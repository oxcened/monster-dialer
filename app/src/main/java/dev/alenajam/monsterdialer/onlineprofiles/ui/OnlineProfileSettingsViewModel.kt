package dev.alenajam.monsterdialer.onlineprofiles.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.alenajam.monsterdialer.onlineprofiles.data.OnlineProfilePublisher
import dev.alenajam.monsterdialer.onlineprofiles.data.OwnedOnlineProfile
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

enum class OnlineProfileOperation {
    SignIn,
    Enable,
    Regenerate,
    KeepOnline,
    Delete,
}

@HiltViewModel
class OnlineProfileSettingsViewModel @Inject constructor(
    private val publisher: OnlineProfilePublisher,
) : ViewModel() {
    private val _profile = MutableStateFlow(publisher.currentProfile())
    val profile: StateFlow<OwnedOnlineProfile?> = _profile.asStateFlow()
    private val _isSignedIn = MutableStateFlow(publisher.isSignedIn())
    val isSignedIn: StateFlow<Boolean> = _isSignedIn.asStateFlow()
    private val _signInRequests = MutableSharedFlow<Unit>()
    val signInRequests: SharedFlow<Unit> = _signInRequests
    private val _isWorking = MutableStateFlow(false)
    val isWorking: StateFlow<Boolean> = _isWorking.asStateFlow()
    private val _operation = MutableStateFlow<OnlineProfileOperation?>(null)
    val operation: StateFlow<OnlineProfileOperation?> = _operation.asStateFlow()
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    private val _showRetentionCheckIn = MutableStateFlow(false)
    val showRetentionCheckIn: StateFlow<Boolean> = _showRetentionCheckIn.asStateFlow()

    init {
        viewModelScope.launch {
            if (publisher.isSignedIn()) _profile.value = runCatching { publisher.restoreProfile() }.getOrNull()
        }
        viewModelScope.launch {
            _showRetentionCheckIn.value = runCatching { publisher.needsRetentionConfirmation() }.getOrDefault(false)
        }
        viewModelScope.launch {
            publisher.retentionConfirmed.collect {
                _showRetentionCheckIn.value = false
            }
        }
    }

    fun enable() {
        if (publisher.isSignedIn()) {
            enableProfile()
        } else {
            viewModelScope.launch { _signInRequests.emit(Unit) }
        }
    }

    fun completeGoogleSignIn(idToken: String) = viewModelScope.launch {
        _isWorking.value = true
        _operation.value = OnlineProfileOperation.SignIn
        try {
            publisher.signInWithGoogle(idToken)
            _isSignedIn.value = true
            _profile.value = publisher.publish()
        } catch (exception: Exception) {
            _error.value = exception.message
        } finally {
            _operation.value = null
            _isWorking.value = false
        }
    }

    fun failGoogleSignIn(message: String?) {
        _error.value = message
    }

    private fun enableProfile() = viewModelScope.launch {
        _isWorking.value = true
        _operation.value = OnlineProfileOperation.Enable
        try {
            _profile.value = publisher.publish()
        } catch (exception: Exception) {
            _error.value = exception.message
        } finally {
            _operation.value = null
            _isWorking.value = false
        }
    }

    fun signOut() {
        publisher.signOut()
        _isSignedIn.value = false
        _profile.value = null
    }

    fun regenerate() = publishInBackground(OnlineProfileOperation.Regenerate) { publisher.publish(regenerateId = true) }
    fun keepOnline() = publishInBackground(OnlineProfileOperation.KeepOnline) {
        publisher.confirmRetention().also { _showRetentionCheckIn.value = false }
    }
    fun delete() = viewModelScope.launch {
        _isWorking.value = true
        _operation.value = OnlineProfileOperation.Delete
        try { publisher.delete(); _profile.value = null } catch (exception: Exception) { _error.value = exception.message }
        finally { _operation.value = null; _isWorking.value = false }
    }
    fun clearError() { _error.value = null }

    private fun publishInBackground(
        operation: OnlineProfileOperation,
        action: suspend () -> OwnedOnlineProfile,
    ) = viewModelScope.launch {
        _isWorking.value = true
        _operation.value = operation
        try { _profile.value = action() } catch (exception: Exception) { _error.value = exception.message }
        finally { _operation.value = null; _isWorking.value = false }
    }
}
