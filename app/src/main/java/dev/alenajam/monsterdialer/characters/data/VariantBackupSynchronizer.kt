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
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val synchronizationMutex = Mutex()
    private var started = false

    fun isEnabled(): Boolean = settings.isEnabled()

    fun start() {
        if (started) return
        started = true
        authentication.observeAuthState { signedIn ->
            if (signedIn) scope.launch { runCatching { synchronize() } }
        }
        unlocks.unlocked
            .drop(1)
            .onEach { runCatching { synchronize() } }
            .launchIn(scope)
        scope.launch { runCatching { synchronize() } }
    }

    suspend fun enable() {
        settings.enable()
        synchronize()
    }

    suspend fun deleteBackup() = synchronizationMutex.withLock {
        remote.deleteAll()
        settings.disable()
    }

    suspend fun synchronize() = synchronizationMutex.withLock {
        if (!settings.isEnabled() || !remote.isSignedIn()) return
        unlocks.merge(remote.restore())
        remote.backup(unlocks.unlocked.value)
    }
}
