package dev.alenajam.monsterdialer.onlineprofiles.data

import dev.alenajam.monsterdialer.battle.data.BattleMonster
import dev.alenajam.monsterdialer.battle.data.BattleVisualAsset
import java.io.File
import java.util.UUID
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class OnlineProfileCache(
    private val root: File,
    private val json: Json = Json { ignoreUnknownKeys = false; explicitNulls = false },
) {
    fun profile(profileId: String): CachedOnlineProfile? {
        return runCatching { json.decodeFromString<CachedOnlineProfile>(profileFile(profileId).readText()) }.getOrNull()
    }

    fun save(profile: CachedOnlineProfile) {
        atomicWrite(profileFile(profile.publicProfileId), json.encodeToString(profile).encodeToByteArray())
    }

    fun isFresh(profileId: String, maxAgeMillis: Long, nowMillis: Long = System.currentTimeMillis()): Boolean {
        val cachedAt = profileFile(profileId).lastModified()
        val age = nowMillis - cachedAt
        return cachedAt > 0 && age in 0..maxAgeMillis
    }

    fun hasSprites(profile: CachedOnlineProfile): Boolean =
        hasSprite(profile.trainer.frontSprite.sha256) && hasSprite(profile.monster.frontSprite.sha256)

    fun spriteFile(hash: String): File = File(spritesDirectory, "$hash.png")

    fun hasSprite(hash: String): Boolean = spriteFile(hash).isFile

    fun toOpponent(profile: CachedOnlineProfile): RemoteBattleOpponent? {
        val trainerFile = spriteFile(profile.trainer.frontSprite.sha256)
        val monsterFile = spriteFile(profile.monster.frontSprite.sha256)
        if (!trainerFile.isFile || !monsterFile.isFile) return null
        return RemoteBattleOpponent(
            profileId = profile.publicProfileId,
            trainerName = profile.trainer.name,
            trainerSprite = BattleVisualAsset.LocalFile(trainerFile.path),
            monster = BattleMonster(
                name = profile.monster.name,
                level = profile.monster.level,
                hp = profile.monster.maxHp,
                maxHp = profile.monster.maxHp,
                frontSprite = BattleVisualAsset.LocalFile(monsterFile.path),
            ),
        )
    }

    fun atomicWrite(destination: File, bytes: ByteArray) {
        destination.parentFile?.mkdirs()
        val temporary = File(destination.parentFile, ".${destination.name}-${UUID.randomUUID()}")
        temporary.outputStream().use { it.write(bytes) }
        if (!temporary.renameTo(destination)) {
            temporary.delete()
            error("Could not update online profile cache")
        }
    }

    private val profilesDirectory get() = File(root, "profiles")
    private val spritesDirectory get() = File(root, "sprites")
    private fun profileFile(profileId: String) = File(profilesDirectory, "$profileId.json")
}
