package dev.alenajam.monsterdialer.onlineprofiles.data

import dev.alenajam.monsterdialer.characters.data.VariantBackupSynchronizer
import javax.inject.Inject
import javax.inject.Singleton

/** Deletes all Firebase data owned by the signed-in player before removing their app account. */
@Singleton
class OnlineAccountDataDeletion @Inject constructor(
    private val profiles: OnlineProfilePublisher,
    private val variantBackup: VariantBackupSynchronizer,
    private val authentication: OnlineProfileAuthentication,
) {
    /**
     * Requires a fresh Google ID token so Firebase can verify the account holder before deletion.
     * If account deletion fails, the exception leaves the caller signed in to surface the failure.
     */
    suspend fun deleteAll(idToken: String) {
        profiles.delete()
        variantBackup.deleteBackup()
        authentication.reauthenticateAndDeleteCurrentUser(idToken)
    }
}
