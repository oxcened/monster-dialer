package dev.alenajam.monsterdialer.onlineprofiles.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import dev.alenajam.monsterdialer.R
import dev.alenajam.monsterdialer.characters.data.BuiltInCharacters
import dev.alenajam.monsterdialer.characters.data.CharacterAssignmentRepository
import dev.alenajam.monsterdialer.characters.data.CharactersRepository
import dev.alenajam.monsterdialer.packs.data.CharacterAssignmentTarget
import dev.alenajam.monsterdialer.packs.data.CharacterType
import java.io.ByteArrayOutputStream
import java.io.File
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.inject.Inject
import javax.inject.Singleton
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class OwnedOnlineProfile(
    val publicProfileId: String,
    val revision: Long,
    val spritePaths: List<String>,
    val contentFingerprint: String = "",
    val ownerUid: String = "",
)

/** Publishes only the active trainer and monster contact-side artwork. */
@Singleton
class OnlineProfilePublisher @Inject constructor(
    @ApplicationContext private val context: Context,
    private val assignments: CharacterAssignmentRepository,
    private val characters: CharactersRepository,
    private val authentication: OnlineProfileAuthentication,
    private val remoteDataSource: OnlineProfileRemoteDataSource,
) {
    private val _retentionConfirmed = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val retentionConfirmed: SharedFlow<Unit> = _retentionConfirmed.asSharedFlow()

    fun currentProfile(): OwnedOnlineProfile? = ownerStore.read()
        ?.takeIf { it.ownerUid == authentication.currentUserId() }

    fun isSignedIn(): Boolean = authentication.currentUserId() != null

    suspend fun signInWithGoogle(idToken: String): String = authentication.signInWithGoogle(idToken)

    fun signOut() = authentication.signOut()

    suspend fun publish(regenerateId: Boolean = false): OwnedOnlineProfile = withContext(Dispatchers.IO) {
        requireNotNull(publishInternal(regenerateId, skipUnchanged = false))
    }

    /** Restores the signed-in owner's profile metadata from the private Firestore index. */
    suspend fun restoreProfile(): OwnedOnlineProfile? = withContext(Dispatchers.IO) {
        val ownerUid = authentication.requireCurrentUserId()
        val profileId = remoteDataSource.ownedProfileId() ?: run {
            ownerStore.clear()
            return@withContext null
        }
        val profile = remoteDataSource.fetch(profileId, forceServer = true) ?: return@withContext null
        OwnedOnlineProfile(
            publicProfileId = profile.publicProfileId,
            revision = profile.revision,
            spritePaths = spritePaths(profile.publicProfileId),
            ownerUid = ownerUid,
        ).also(ownerStore::write)
    }

    /** Uploads a changed enabled profile, returning null when there is nothing to publish. */
    suspend fun publishIfChanged(): OwnedOnlineProfile? = withContext(Dispatchers.IO) {
        publishInternal(regenerateId = false, skipUnchanged = true)
    }

    private suspend fun publishInternal(regenerateId: Boolean, skipUnchanged: Boolean): OwnedOnlineProfile? {
        val ownerUid = authentication.requireCurrentUserId()
        val cachedProfile = currentProfile()
        val restoredProfile = restoreProfile()
        val previous = restoredProfile?.copy(
            contentFingerprint = cachedProfile
                ?.takeIf { it.publicProfileId == restoredProfile.publicProfileId }
                ?.contentFingerprint
                .orEmpty(),
        )
        if (skipUnchanged && previous == null) return null
        val profileId = if (regenerateId || previous == null) newProfileId() else previous.publicProfileId
        val assets = activeAssets(profileId)
        val fingerprint = assets.contentFingerprint()
        if (skipUnchanged && previous?.contentFingerprint == fingerprint) return null
        val revision = (previous?.revision ?: 0) + 1
        val profile = CachedOnlineProfile(
            publicProfileId = profileId,
            revision = revision,
            trainer = SharedTrainer(assets.trainerName, assets.trainerSprite.shared()),
            monster = SharedMonster(
                name = assets.monsterName,
                level = assets.monsterLevel,
                maxHp = assets.monsterHp,
                frontSprite = assets.monsterSprite.shared(),
                isRadiant = assets.monsterIsRadiant
            ),
        )
        if (regenerateId && previous != null) remoteDataSource.deleteSprites(previous)
        remoteDataSource.publish(
            OnlineProfileUpload(
                profile = profile,
                sprites = assets.all.map { asset -> OnlineProfileUploadSprite(asset.shared(), asset.pngBytes) },
                refreshRetention = previous == null || regenerateId,
                createsOrReplacesOwnerIndex = previous == null || regenerateId,
                previousProfileId = previous?.takeIf { regenerateId }?.publicProfileId,
            ),
        )
        val owned = OwnedOnlineProfile(
            publicProfileId = profileId,
            revision = revision,
            spritePaths = assets.all.map(PreparedSprite::storagePath),
            contentFingerprint = fingerprint,
            ownerUid = ownerUid,
        )
        ownerStore.write(owned)
        return owned
    }

    suspend fun delete(): Unit = withContext(Dispatchers.IO) {
        currentProfile()?.let { owned ->
            remoteDataSource.delete(owned)
            ownerStore.clear()
        }
    }

    suspend fun needsRetentionConfirmation(): Boolean = withContext(Dispatchers.IO) {
        val profile = currentProfile() ?: return@withContext false
        val retention = remoteDataSource.retention(profile.publicProfileId) ?: return@withContext true
        retention.needsConfirmation(System.currentTimeMillis())
    }

    suspend fun confirmRetention(): OwnedOnlineProfile = withContext(Dispatchers.IO) {
        authentication.requireCurrentUserId()
        val profile = requireNotNull(currentProfile())
        remoteDataSource.confirmRetention(profile.publicProfileId)
        _retentionConfirmed.emit(Unit)
        profile
    }

    private suspend fun activeAssets(profileId: String): ActiveAssets {
        val trainer = assignments.getPlayerCharacter(CharacterType.Trainer)
            ?.let { characters.findCharacter(it, CharacterAssignmentTarget.Player, CharacterType.Trainer) }
        val monster = assignments.getPlayerCharacter(CharacterType.Monster)
            ?.let { characters.findCharacter(it, CharacterAssignmentTarget.Player, CharacterType.Monster) }
        val trainerVariant = trainer?.character?.variant(assignments.getPlayerCharacter(CharacterType.Trainer)?.variantId.orEmpty())
        val monsterVariant = monster?.character?.variant(assignments.getPlayerCharacter(CharacterType.Monster)?.variantId.orEmpty())
        val trainerSprite = trainerVariant?.frontImage?.let {
            prepare(File(trainer.directory, it), profileId, TrainerSpriteFileName)
        } ?: prepare(R.drawable.battle_enemy_trainer, profileId, TrainerSpriteFileName)
        val monsterSprite = monsterVariant?.frontImage?.let {
            prepare(File(monster.directory, it), profileId, MonsterSpriteFileName)
        } ?: prepare(R.drawable.battle_enemy_monster, profileId, MonsterSpriteFileName)
        return ActiveAssets(
            trainerName = trainer?.character?.name ?: BuiltInCharacters.trainer.name,
            monsterName = monster?.character?.name ?: BuiltInCharacters.monster.character.name,
            monsterLevel = monster?.character?.level ?: BuiltInCharacters.monster.level,
            monsterHp = monster?.character?.maxHp ?: BuiltInCharacters.monster.maxHp,
            monsterIsRadiant = monsterVariant?.isRadiant == true,
            trainerSprite = trainerSprite,
            monsterSprite = monsterSprite,
        )
    }

    private fun prepare(file: File, profileId: String, fileName: String): PreparedSprite =
        prepare(BitmapFactory.decodeFile(file.path), profileId, fileName)

    private fun prepare(resource: Int, profileId: String, fileName: String): PreparedSprite =
        prepare(BitmapFactory.decodeResource(context.resources, resource), profileId, fileName)

    private fun prepare(source: Bitmap?, profileId: String, fileName: String): PreparedSprite {
        requireNotNull(source) { context.getString(R.string.online_profile_sprite_decode_error) }
        val scale = minOf(1f, MaxDimension.toFloat() / source.width, MaxDimension.toFloat() / source.height)
        val bitmap = if (scale < 1f) Bitmap.createScaledBitmap(source, (source.width * scale).toInt(), (source.height * scale).toInt(), false) else source
        val bytes = ByteArrayOutputStream().use { output -> bitmap.compress(Bitmap.CompressFormat.PNG, 100, output); output.toByteArray() }
        val width = bitmap.width
        val height = bitmap.height
        if (bitmap !== source) bitmap.recycle()
        val hash = bytes.sha256()
        return PreparedSprite("onlineProfiles/$profileId/sprites/$fileName", hash, width, height, bytes)
    }

    private val ownerStore = OwnerStore(File(context.filesDir, "online-profiles/owned-profile.json"))

    private fun spritePaths(profileId: String) = listOf(
        "onlineProfiles/$profileId/sprites/$TrainerSpriteFileName",
        "onlineProfiles/$profileId/sprites/$MonsterSpriteFileName",
    )

    private companion object {
        const val MaxDimension = 1024
        const val TrainerSpriteFileName = "trainer.png"
        const val MonsterSpriteFileName = "monster.png"
        fun newProfileId(): String = Base64.getUrlEncoder().withoutPadding().encodeToString(ByteArray(16).also(SecureRandom()::nextBytes))
    }
}

private data class ActiveAssets(
    val trainerName: String,
    val monsterName: String,
    val monsterLevel: Int,
    val monsterHp: Int,
    val monsterIsRadiant: Boolean,
    val trainerSprite: PreparedSprite,
    val monsterSprite: PreparedSprite
) {
    val all get() = listOf(trainerSprite, monsterSprite)

    fun contentFingerprint(): String = listOf(
        trainerName,
        monsterName,
        monsterLevel.toString(),
        monsterHp.toString(),
        monsterIsRadiant.toString(),
        trainerSprite.sha256,
        monsterSprite.sha256,
    ).joinToString("\u0000").encodeToByteArray().sha256()
}
private data class PreparedSprite(val storagePath: String, val sha256: String, val width: Int, val height: Int, val pngBytes: ByteArray) {
    fun shared() = SharedSprite(storagePath, sha256, width, height, pngBytes.size.toLong())
}
private fun ByteArray.sha256() = MessageDigest.getInstance("SHA-256").digest(this).joinToString("") { "%02x".format(it) }
private class OwnerStore(private val file: File) {
    private val json = Json { explicitNulls = false }
    fun read() = runCatching { json.decodeFromString<OwnedOnlineProfile>(file.readText()) }.getOrNull()
    fun write(value: OwnedOnlineProfile) { file.parentFile?.mkdirs(); file.writeText(json.encodeToString(value)) }
    fun clear() { file.delete() }
}
