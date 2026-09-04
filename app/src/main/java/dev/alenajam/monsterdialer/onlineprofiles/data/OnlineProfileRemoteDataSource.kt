package dev.alenajam.monsterdialer.onlineprofiles.data

import java.io.File

/** Network boundary for shared profiles; local policy stays outside Firebase-specific code. */
interface OnlineProfileRemoteDataSource {
    suspend fun publish(profile: OnlineProfileUpload)
    suspend fun ownedProfileId(): String?
    suspend fun retention(publicProfileId: String): OnlineProfileRetention?
    suspend fun confirmRetention(publicProfileId: String)
    suspend fun fetch(publicProfileId: String, forceServer: Boolean = false): CachedOnlineProfile?
    suspend fun downloadSprite(sprite: SharedSprite, destination: File): Boolean
    suspend fun deleteSprites(profile: OwnedOnlineProfile)
    suspend fun delete(profile: OwnedOnlineProfile)
}

data class OnlineProfileUpload(
    val profile: CachedOnlineProfile,
    val sprites: List<OnlineProfileUploadSprite>,
    val refreshRetention: Boolean,
    val createsOrReplacesOwnerIndex: Boolean,
    val previousProfileId: String?,
)

data class OnlineProfileUploadSprite(
    val sprite: SharedSprite,
    val pngBytes: ByteArray,
)
