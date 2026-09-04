package dev.alenajam.monsterdialer.onlineprofiles.data

import android.graphics.BitmapFactory
import java.io.File
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Cached-first profile lookup. Network work is deliberately never performed by the call UI. */
@Singleton
class OnlineOpponentResolver @Inject constructor(
    private val normalizer: PhoneNumberNormalizer,
    private val links: OnlineProfileLinkStore,
    private val cache: OnlineProfileCache,
    private val remoteDataSource: OnlineProfileRemoteDataSource,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _cacheVersion = MutableStateFlow(0L)
    val cacheVersion: StateFlow<Long> = _cacheVersion.asStateFlow()

    fun cachedOpponentForNumber(number: String): RemoteBattleOpponent? {
        val e164 = normalizer.toE164OrNull(number) ?: return null
        val profileId = links.profileIdFor(e164) ?: return null
        return cache.profile(profileId)?.let(cache::toOpponent)
    }

    fun linkedProfileId(phoneNumbers: Collection<String>): String? =
        phoneNumbers.asSequence()
            .mapNotNull(normalizer::toE164OrNull)
            .mapNotNull(links::profileIdFor)
            .firstOrNull()

    fun refreshForNumber(number: String, force: Boolean = false) {
        refreshForNumberAsync(number, force)
    }

    /**
     * Starts a linked profile refresh and exposes its completion for callers that can wait briefly.
     * Returning null means this phone number has no linked online profile.
     */
    fun refreshForNumberAsync(number: String, force: Boolean = false): Deferred<Unit>? {
        val e164 = normalizer.toE164OrNull(number) ?: return null
        val profileId = links.profileIdFor(e164) ?: return null
        return scope.async { refresh(profileId, force) }
    }

    suspend fun link(phoneNumbers: Collection<String>, publicProfileId: String): Boolean {
        if (!PublicProfileId.isValid(publicProfileId)) return false
        val canonical = phoneNumbers.mapNotNull(normalizer::toE164OrNull).distinct()
        if (canonical.isEmpty()) return false
        links.link(canonical, publicProfileId)
        refreshForNumber(canonical.first(), force = true)
        return true
    }

    fun unlink(phoneNumbers: Collection<String>) {
        links.unlink(phoneNumbers.mapNotNull(normalizer::toE164OrNull).distinct())
        _cacheVersion.value += 1
    }

    private suspend fun refresh(profileId: String, force: Boolean = false) = withContext(Dispatchers.IO) {
        runCatching {
            val cachedProfile = cache.profile(profileId)
            if (!force && cachedProfile != null && cache.hasSprites(cachedProfile) && cache.isFresh(profileId, ProfileRefreshIntervalMillis)) {
                return@runCatching
            }
            val profile = remoteDataSource.fetch(profileId, forceServer = force) ?: return@runCatching
            profile.sprites().forEach { sprite ->
                if (!cache.hasSprite(sprite.sha256)) downloadSprite(sprite)
            }
            cache.save(profile)
            _cacheVersion.value += 1
        }
    }

    private suspend fun downloadSprite(sprite: SharedSprite) {
        val target = cache.spriteFile(sprite.sha256)
        val temporary = File(target.parentFile, ".${target.name}.download")
        target.parentFile?.mkdirs()
        try {
            if (!remoteDataSource.downloadSprite(sprite, temporary)) return
            if (temporary.length() != sprite.byteSize ||
                temporary.sha256() != sprite.sha256 ||
                !temporary.isExpectedPng(sprite) ||
                !temporary.renameTo(target)
            ) return
        } finally {
            temporary.delete()
        }
    }

    private fun CachedOnlineProfile.sprites() = listOf(trainer.frontSprite, monster.frontSprite)

    private fun File.sha256(): String = inputStream().use { input ->
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(8 * 1024)
        while (true) {
            val count = input.read(buffer)
            if (count < 0) break
            digest.update(buffer, 0, count)
        }
        digest.digest().joinToString("") { byte -> "%02x".format(byte) }
    }

    /** Reads only image bounds, so untrusted images are rejected before pixel allocation. */
    private fun File.isExpectedPng(sprite: SharedSprite): Boolean {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, options)
        return options.outMimeType == PngMimeType &&
            options.outWidth == sprite.width && options.outHeight == sprite.height &&
            options.outWidth in 1..MaxSpriteDimension && options.outHeight in 1..MaxSpriteDimension
    }

    private companion object {
        const val ProfileRefreshIntervalMillis = 1L * 60 * 60 * 1000
        const val MaxSpriteDimension = 1024
        const val PngMimeType = "image/png"
    }
}
