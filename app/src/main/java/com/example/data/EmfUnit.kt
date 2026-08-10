package com.example.data

import java.util.Locale

enum class EmfUnit(
    val label: String,
    val symbol: String,
    val scaleFactor: Float
) {
    MICROTESLA("Microtesla", "µT", 1.0f),
    MILLIGAUSS("Milligauss", "mG", 10.0f);

    fun convert(valInMicrotesla: Float): Float {
        return valInMicrotesla * scaleFactor
    }

    fun format(valInMicrotesla: Float, decimals: Int = 1): String {
        val converted = convert(valInMicrotesla)
        return String.format(Locale.US, "%.${decimals}f %s", converted, symbol)
    }

    fun formatValueOnly(valInMicrotesla: Float, decimals: Int = 1): String {
        val converted = convert(valInMicrotesla)
        return String.format(Locale.US, "%.${decimals}f", converted)
    }
}
