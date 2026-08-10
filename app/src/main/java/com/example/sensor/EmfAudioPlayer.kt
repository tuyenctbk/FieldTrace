package com.example.sensor

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.asin
import kotlin.math.floor
import kotlin.math.sin

enum class WaveformType(val displayName: String, val description: String) {
    SINE("SINE", "Smooth Pure Tone"),
    SQUARE("SQUARE", "8-Bit Digital Chiptune"),
    TRIANGLE("TRIANGLE", "Acoustic Harmonic"),
    SAWTOOTH("SAW", "Rich Harmonic Saw"),
    GEIGER("GEIGER", "Geiger Click Counter"),
    THEREMIN("THEREMIN", "Vibrato Sci-Fi Pitch")
}

class EmfAudioPlayer {
    private var audioTrack: AudioTrack? = null
    private var playJob: Job? = null
    private val sampleRate = 22050
    @Volatile private var targetFrequency = 0f
    @Volatile private var volume = 0f

    private val _currentFrequency = MutableStateFlow(0f)
    val currentFrequency: StateFlow<Float> = _currentFrequency.asStateFlow()

    private val _isSonificationActive = MutableStateFlow(true)
    val isSonificationActive: StateFlow<Boolean> = _isSonificationActive.asStateFlow()

    private val _waveformType = MutableStateFlow(WaveformType.SINE)
    val waveformType: StateFlow<WaveformType> = _waveformType.asStateFlow()

    fun setWaveform(type: WaveformType) {
        _waveformType.value = type
    }

    fun start() {
        if (playJob != null) return
        
        val minBufferSize = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        
        try {
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()

            val audioFormat = AudioFormat.Builder()
                .setSampleRate(sampleRate)
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                .build()

            audioTrack = AudioTrack.Builder()
                .setAudioAttributes(audioAttributes)
                .setAudioFormat(audioFormat)
                .setBufferSizeInBytes(minBufferSize.coerceAtLeast(1024))
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()

            audioTrack?.play()
        } catch (e: Exception) {
            e.printStackTrace()
            return
        }

        playJob = CoroutineScope(Dispatchers.Default).launch {
            val bufferSize = 512
            val buffer = ShortArray(bufferSize)
            var angle = 0.0
            var vibratoPhase = 0.0
            var geigerPhase = 0.0
            var geigerBurstSamples = 0
            
            while (isActive) {
                val freq = targetFrequency
                val vol = volume
                val wave = _waveformType.value
                
                if (vol > 0.01f && freq > 10f) {
                    for (i in 0 until bufferSize) {
                        val rawSample = when (wave) {
                            WaveformType.SINE -> sin(angle)
                            WaveformType.SQUARE -> if (sin(angle) >= 0.0) 0.7 else -0.7
                            WaveformType.TRIANGLE -> (2.0 / Math.PI) * asin(sin(angle))
                            WaveformType.SAWTOOTH -> {
                                val normAngle = angle / (2.0 * Math.PI)
                                (2.0 * (normAngle - floor(normAngle + 0.5))) * 0.7
                            }
                            WaveformType.GEIGER -> {
                                // Geiger click rate scales with frequency
                                geigerPhase += (freq / 30f) / sampleRate
                                if (geigerPhase >= 1.0) {
                                    geigerPhase -= 1.0
                                    geigerBurstSamples = 35 // short ~1.5ms click
                                }
                                if (geigerBurstSamples > 0) {
                                    geigerBurstSamples--
                                    (sin(geigerBurstSamples * 0.6) * (geigerBurstSamples / 35.0)) * 0.9
                                } else {
                                    0.0
                                }
                            }
                            WaveformType.THEREMIN -> {
                                vibratoPhase += 6.0 * 2.0 * Math.PI / sampleRate
                                val vibratoMod = sin(vibratoPhase) * (freq * 0.06f)
                                val modAngle = angle + vibratoMod / sampleRate
                                sin(modAngle)
                            }
                        }
                        val sample = (rawSample * 32767 * vol).toInt().toShort()
                        buffer[i] = sample

                        angle += 2.0 * Math.PI * freq / sampleRate
                        if (angle > 2.0 * Math.PI) {
                            angle -= 2.0 * Math.PI
                        }
                    }
                    audioTrack?.write(buffer, 0, bufferSize)
                } else {
                    buffer.fill(0)
                    audioTrack?.write(buffer, 0, bufferSize)
                }
            }
        }
    }

    fun setSonificationEnabled(enabled: Boolean) {
        _isSonificationActive.value = enabled
        if (!enabled) {
            volume = 0f
            _currentFrequency.value = 0f
        }
    }

    fun updateSound(emfMagnitude: Float, isMuted: Boolean, warningThreshold: Float = 80f) {
        if (isMuted || !_isSonificationActive.value) {
            volume = 0f
            _currentFrequency.value = 0f
            return
        }

        // Map EMF 0uT - 300uT -> 120Hz - 1800Hz audio frequency curve
        val clampedEmf = emfMagnitude.coerceIn(0f, 300f)
        val normalized = clampedEmf / 300f
        var freq = 120f + (normalized * normalized) * 1680f
        
        // Visual/Audible threshold alarm: introduce siren pitch modulation when threshold is surpassed
        if (emfMagnitude > warningThreshold) {
            val systemTime = System.currentTimeMillis()
            val sirenMod = if ((systemTime / 200) % 2 == 0L) 400f else -200f
            freq += sirenMod
        }
        
        targetFrequency = freq.coerceIn(50f, 3000f)
        _currentFrequency.value = freq
        volume = (0.25f + normalized * 0.6f).coerceIn(0f, 0.95f)
    }

    fun stop() {
        playJob?.cancel()
        playJob = null
        try {
            audioTrack?.stop()
            audioTrack?.release()
            audioTrack = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
