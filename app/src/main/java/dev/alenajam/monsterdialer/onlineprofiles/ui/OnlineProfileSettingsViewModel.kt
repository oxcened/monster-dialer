package dev.alenajam.monsterdialer.onlineprofiles.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.alenajam.monsterdialer.characters.data.VariantBackupSynchronizer
import dev.alenajam.monsterdialer.onlineprofiles.data.OnlineProfilePublisher
import dev.alenajam.monsterdialer.onlineprofiles.data.OnlineAccountDataDeletion
import dev.alenajam.monsterdialer.onlineprofiles.data.OwnedOnlineProfile
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
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
    DeleteVariantBackup,
    DeleteAccount,
}

/** A credential prompt remains pending until the UI completes or cancels it. */
enum class OnlineProfileCredentialRequest {
    SignIn,
    DeleteAccount,
}

@HiltViewModel
class OnlineProfileSettingsViewModel @Inject constructor(
    private val publisher: OnlineProfilePublisher,
    private val variantBackup: VariantBackupSynchronizer,
    private val accountDataDeletion: OnlineAccountDataDeletion,
) : ViewModel() {
    private val _profile = MutableStateFlow(publisher.currentProfile())
    val profile: StateFlow<OwnedOnlineProfile?> = _profile.asStateFlow()
    private val _isSignedIn = MutableStateFlow(publisher.isSignedIn())
    val isSignedIn: StateFlow<Boolean> = _isSignedIn.asStateFlow()
    private val removeAuthStateListener = publisher.observeAuthState { signedIn ->
        _isSignedIn.value = signedIn
    }
    private val _credentialRequest = MutableStateFlow<OnlineProfileCredentialRequest?>(null)
    val credentialRequest: StateFlow<OnlineProfileCredentialRequest?> = _credentialRequest.asStateFlow()
    private val _isWorking = MutableStateFlow(false)
    val isWorking: StateFlow<Boolean> = _isWorking.asStateFlow()
    private val _operation = MutableStateFlow<OnlineProfileOperation?>(null)
    val operation: StateFlow<OnlineProfileOperation?> = _operation.asStateFlow()
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    private val _showRetentionCheckIn = MutableStateFlow(false)
    val showRetentionCheckIn: StateFlow<Boolean> = _showRetentionCheckIn.asStateFlow()
    private val _variantBackupEnabled = MutableStateFlow(variantBackup.isEnabled())
    val variantBackupEnabled: StateFlow<Boolean> = _variantBackupEnabled.asStateFlow()
    val variantBackupStatus: StateFlow<VariantBackupSynchronizer.Status> = variantBackup.status
    private var pendingVariantBackupSignIn = false

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
        if (_isSignedIn.value) {
            enableProfile()
        } else {
            signIn()
        }
    }

    fun signIn() = viewModelScope.launch {
        if (_isWorking.value) return@launch
        _isWorking.value = true
        _operation.value = OnlineProfileOperation.SignIn
        _credentialRequest.value = OnlineProfileCredentialRequest.SignIn
    }

    fun requestAccountDeletion() {
        _credentialRequest.value = OnlineProfileCredentialRequest.DeleteAccount
    }

    fun deleteAccount(idToken: String) = viewModelScope.launch {
        _credentialRequest.value = null
        _isWorking.value = true
        _operation.value = OnlineProfileOperation.DeleteAccount
        try {
            accountDataDeletion.deleteAll(idToken)
            _isSignedIn.value = false
            _profile.value = null
            _variantBackupEnabled.value = false
        } catch (exception: Exception) {
            _error.value = exception.message
        } finally {
            _operation.value = null
            _isWorking.value = false
        }
    }

    fun completeGoogleSignIn(idToken: String) = viewModelScope.launch {
        _credentialRequest.value = null
        _isWorking.value = true
        _operation.value = OnlineProfileOperation.SignIn
        try {
            publisher.signInWithGoogle(idToken)
            _isSignedIn.value = publisher.isSignedIn()
            refreshProfile()
            if (pendingVariantBackupSignIn) {
                pendingVariantBackupSignIn = false
                variantBackup.enable()
                _variantBackupEnabled.value = true
            }
        } catch (exception: Exception) {
            _error.value = exception.message
        } finally {
            _operation.value = null
            _isWorking.value = false
        }
    }

    fun failGoogleSignIn(message: String?) {
        _credentialRequest.value = null
        pendingVariantBackupSignIn = false
        _error.value = message
        _operation.value = null
        _isWorking.value = false
    }

    fun enableVariantBackup() {
        if (publisher.isSignedIn()) {
            viewModelScope.launch {
                _isWorking.value = true
                try {
                    variantBackup.enable()
                    _variantBackupEnabled.value = true
                } catch (exception: Exception) {
                    _error.value = exception.message
                } finally {
                    _isWorking.value = false
                }
            }
        } else {
            pendingVariantBackupSignIn = true
            signIn()
        }
    }

    fun deleteVariantBackup() = viewModelScope.launch {
        _isWorking.value = true
        _operation.value = OnlineProfileOperation.DeleteVariantBackup
        try {
            variantBackup.deleteBackup()
            _variantBackupEnabled.value = false
        } catch (exception: Exception) {
            _error.value = exception.message
        } finally {
            _operation.value = null
            _isWorking.value = false
        }
    }

    fun retryVariantBackup() = viewModelScope.launch {
        _isWorking.value = true
        try {
            variantBackup.retry()
        } finally {
            _isWorking.value = false
        }
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
        try {
            publisher.delete()
            refreshProfile()
        } catch (exception: Exception) { _error.value = exception.message }
        finally { _operation.value = null; _isWorking.value = false }
    }
    fun clearError() { _error.value = null }

    private suspend fun refreshProfile() {
        _profile.value = if (publisher.isSignedIn()) {
            runCatching { publisher.restoreProfile() }.getOrNull()
        } else {
            publisher.currentProfile()
        }
    }

    override fun onCleared() {
        removeAuthStateListener()
        super.onCleared()
    }

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
