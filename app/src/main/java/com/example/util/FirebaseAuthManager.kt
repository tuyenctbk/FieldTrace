package com.example.util

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Handles Firebase Authentication and User Profile state management.
 * Provides anonymous guest sign-in fallback for cloud syncing.
 */
object FirebaseAuthManager {

    private val _currentUser = MutableStateFlow<FirebaseUser?>(null)
    val currentUser: StateFlow<FirebaseUser?> = _currentUser.asStateFlow()

    private val _isAnonymous = MutableStateFlow(true)
    val isAnonymous: StateFlow<Boolean> = _isAnonymous.asStateFlow()

    private val _userId = MutableStateFlow<String?>(null)
    val userId: StateFlow<String?> = _userId.asStateFlow()

    private val _userEmail = MutableStateFlow<String?>(null)
    val userEmail: StateFlow<String?> = _userEmail.asStateFlow()

    private val _authStatusMessage = MutableStateFlow("Initializing Cloud Auth...")
    val authStatusMessage: StateFlow<String> = _authStatusMessage.asStateFlow()

    fun init(context: Context) {
        try {
            val auth = FirebaseAuth.getInstance()
            updateUserState(auth.currentUser)

            auth.addAuthStateListener { firebaseAuth ->
                updateUserState(firebaseAuth.currentUser)
            }

            // Ensure we have a valid Firebase user (Anonymous or Authenticated)
            if (auth.currentUser == null) {
                signInAnonymously()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            _authStatusMessage.value = "Auth unavailable (Offline mode)"
        }
    }

    private fun updateUserState(user: FirebaseUser?) {
        _currentUser.value = user
        _userId.value = user?.uid
        _userEmail.value = user?.email
        _isAnonymous.value = user?.isAnonymous ?: true

        _authStatusMessage.value = when {
            user == null -> "Not Signed In (Offline)"
            user.isAnonymous -> "Signed in as Guest (${user.uid.take(8)})"
            else -> "Signed in as ${user.email ?: user.uid.take(8)}"
        }
    }

    fun signInAnonymously(onComplete: ((Boolean) -> Unit)? = null) {
        try {
            val auth = FirebaseAuth.getInstance()
            _authStatusMessage.value = "Authenticating Guest Session..."
            auth.signInAnonymously()
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        updateUserState(task.result?.user)
                        onComplete?.invoke(true)
                    } else {
                        _authStatusMessage.value = "Guest sign-in error: ${task.exception?.localizedMessage}"
                        onComplete?.invoke(false)
                    }
                }
        } catch (e: Exception) {
            e.printStackTrace()
            _authStatusMessage.value = "Cloud Auth Error: ${e.localizedMessage}"
            onComplete?.invoke(false)
        }
    }

    fun signInWithEmailAndPassword(email: String, pword: String, onComplete: (Boolean, String?) -> Unit) {
        try {
            val auth = FirebaseAuth.getInstance()
            _authStatusMessage.value = "Signing in..."
            auth.signInWithEmailAndPassword(email, pword)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        updateUserState(task.result?.user)
                        onComplete(true, null)
                    } else {
                        val errMsg = task.exception?.localizedMessage ?: "Unknown error"
                        _authStatusMessage.value = "Sign-in error: $errMsg"
                        onComplete(false, errMsg)
                    }
                }
        } catch (e: Exception) {
            e.printStackTrace()
            _authStatusMessage.value = "Cloud Auth Error: ${e.localizedMessage}"
            onComplete(false, e.localizedMessage)
        }
    }

    fun registerWithEmailAndPassword(email: String, pword: String, onComplete: (Boolean, String?) -> Unit) {
        try {
            val auth = FirebaseAuth.getInstance()
            _authStatusMessage.value = "Registering user..."
            auth.createUserWithEmailAndPassword(email, pword)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        updateUserState(task.result?.user)
                        onComplete(true, null)
                    } else {
                        val errMsg = task.exception?.localizedMessage ?: "Unknown error"
                        _authStatusMessage.value = "Registration error: $errMsg"
                        onComplete(false, errMsg)
                    }
                }
        } catch (e: Exception) {
            e.printStackTrace()
            _authStatusMessage.value = "Cloud Auth Error: ${e.localizedMessage}"
            onComplete(false, e.localizedMessage)
        }
    }

    fun signOut() {
        try {
            FirebaseAuth.getInstance().signOut()
            signInAnonymously()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
