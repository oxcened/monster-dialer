package dev.alenajam.monsterdialer.characters.data

import dev.alenajam.monsterdialer.onlineprofiles.data.OnlineProfileAuthentication
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Restores and backs up unlockable variants with additive, multi-device-safe merging. */
@Singleton
class VariantBackupSynchronizer @Inject constructor(
    private val settings: VariantBackupSettingsStore,
    private val unlocks: VariantUnlockStore,
    private val remote: VariantBackupRemoteDataSource,
    private val authentication: OnlineProfileAuthentication,
) {
    sealed interface Status {
        data object Disabled : Status
        data object Idle : Status
        data object Synchronizing : Status
        data class Failed(val message: String?) : Status
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val synchronizationMutex = Mutex()
    private var started = false
    private val _status = MutableStateFlow<Status>(if (settings.isEnabled()) Status.Idle else Status.Disabled)
    val status: StateFlow<Status> = _status.asStateFlow()

    fun isEnabled(): Boolean = settings.isEnabled()

    fun start() {
        if (started) return
        started = true
        authentication.observeAuthState { signedIn ->
            if (signedIn) synchronizeInBackground()
        }
        unlocks.unlocked
            .drop(1)
            .onEach { synchronizeInBackground() }
            .launchIn(scope)
        synchronizeInBackground()
    }

    suspend fun enable() {
        settings.enable()
        _status.value = Status.Idle
        synchronize()
    }

    suspend fun deleteBackup() = synchronizationMutex.withLock {
        remote.deleteAll()
        settings.disable()
        _status.value = Status.Disabled
    }

    fun retry() = synchronizeInBackground()

    suspend fun synchronize() = synchronizationMutex.withLock {
        if (!settings.isEnabled()) {
            _status.value = Status.Disabled
            return
        }
        if (!remote.isSignedIn()) return
        _status.value = Status.Synchronizing
        try {
            unlocks.merge(remote.restore())
            remote.backup(unlocks.unlocked.value)
            _status.value = Status.Idle
        } catch (exception: Exception) {
            _status.value = Status.Failed(exception.message)
            throw exception
        }
    }

    private fun synchronizeInBackground() {
        scope.launch { runCatching { synchronize() } }
    }
}
