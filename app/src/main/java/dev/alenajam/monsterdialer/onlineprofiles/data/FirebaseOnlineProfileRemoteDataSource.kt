package dev.alenajam.monsterdialer.onlineprofiles.data

import android.content.Context
import dev.alenajam.monsterdialer.R
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.Source
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.tasks.await

@Singleton
class FirebaseOnlineProfileRemoteDataSource @Inject constructor(
    @ApplicationContext private val context: Context,
) : OnlineProfileRemoteDataSource {
    override suspend fun publish(profile: OnlineProfileUpload) {
        val app = firebaseApp()
        val auth = FirebaseAuth.getInstance(app)
        val user = auth.currentUser ?: auth.signInAnonymously().await().user
        requireNotNull(user) { context.getString(R.string.online_profile_anonymous_auth_error) }
        val firestore = FirebaseFirestore.getInstance(app)
        val storage = FirebaseStorage.getInstance(app)
        val profileRef = firestore.collection(Collection).document(profile.profile.publicProfileId)
        val ownershipRef = profileRef.collection(PrivateCollection).document(OwnershipDocument)
        firestore.runBatch { batch ->
            batch.set(profileRef, profile.profile.asFirestoreDocument(profile.refreshRetention), SetOptions.merge())
            batch.set(ownershipRef, mapOf(OwnerUid to user.uid))
        }.await()
        profile.sprites.forEach { upload ->
            storage.reference.child(upload.sprite.storagePath).putBytes(
                upload.pngBytes,
                StorageMetadata.Builder().setContentType(PngContentType).build(),
            ).await()
        }
    }

    override suspend fun retention(publicProfileId: String): OnlineProfileRetention? {
        val app = FirebaseApp.initializeApp(context) ?: return null
        val timestamp = FirebaseFirestore.getInstance(app)
            .collection(Collection)
            .document(publicProfileId)
            .get()
            .await()
            .getTimestamp(RetentionConfirmedAt)
            ?: return null
        return OnlineProfileRetention(timestamp.toDate().time)
    }

    override suspend fun confirmRetention(publicProfileId: String) {
        val app = firebaseApp()
        FirebaseFirestore.getInstance(app)
            .collection(Collection)
            .document(publicProfileId)
            .update(RetentionConfirmedAt, FieldValue.serverTimestamp())
            .await()
    }

    override suspend fun fetch(publicProfileId: String, forceServer: Boolean): CachedOnlineProfile? {
        val app = FirebaseApp.initializeApp(context) ?: return null
        val snapshot = FirebaseFirestore.getInstance(app)
            .collection(Collection)
            .document(publicProfileId)
            .get(if (forceServer) Source.SERVER else Source.DEFAULT)
            .await()
        return snapshot.takeIf(DocumentSnapshot::exists)?.toCachedProfile(publicProfileId)
    }

    override suspend fun downloadSprite(sprite: SharedSprite, destination: File): Boolean {
        val app = FirebaseApp.initializeApp(context) ?: return false
        FirebaseStorage.getInstance(app).reference.child(sprite.storagePath).getFile(destination).await()
        return true
    }

    override suspend fun delete(profile: OwnedOnlineProfile) {
        val app = FirebaseApp.initializeApp(context) ?: return
        val storage = FirebaseStorage.getInstance(app)
        profile.spritePaths.forEach { path -> runCatching { storage.reference.child(path).delete().await() } }
        val firestore = FirebaseFirestore.getInstance(app)
        val profileRef = firestore.collection(Collection).document(profile.publicProfileId)
        firestore.runBatch { batch ->
            batch.delete(profileRef)
            batch.delete(profileRef.collection(PrivateCollection).document(OwnershipDocument))
        }.await()
    }

    private fun firebaseApp(): FirebaseApp = requireNotNull(FirebaseApp.initializeApp(context)) {
        context.getString(R.string.online_profile_firebase_not_configured)
    }

    private fun CachedOnlineProfile.asFirestoreDocument(refreshRetention: Boolean): Map<String, Any> = buildMap {
        put("schemaVersion", schemaVersion)
        put("publicProfileId", publicProfileId)
        put("revision", revision)
        put("updatedAt", FieldValue.serverTimestamp())
        if (refreshRetention) put(RetentionConfirmedAt, FieldValue.serverTimestamp())
        put("trainer", mapOf("name" to trainer.name, "frontSprite" to trainer.frontSprite.asFirestoreDocument()))
        put("monster", mapOf(
            "name" to monster.name,
            "level" to monster.level,
            "maxHp" to monster.maxHp,
            "frontSprite" to monster.frontSprite.asFirestoreDocument(),
        ))
    }

    private fun SharedSprite.asFirestoreDocument(): Map<String, Any> = mapOf(
        "storagePath" to storagePath,
        "sha256" to sha256,
        "width" to width,
        "height" to height,
        "byteSize" to byteSize,
    )

    private fun DocumentSnapshot.toCachedProfile(profileId: String): CachedOnlineProfile? {
        val trainer = get("trainer") as? Map<*, *> ?: return null
        val monster = get("monster") as? Map<*, *> ?: return null
        val trainerSprite = trainer.sprite("frontSprite") ?: return null
        val monsterSprite = monster.sprite("frontSprite") ?: return null
        val revision = getLong("revision") ?: return null
        val trainerName = trainer["name"] as? String ?: return null
        val monsterName = monster["name"] as? String ?: return null
        val level = (monster["level"] as? Number)?.toInt() ?: return null
        val maxHp = (monster["maxHp"] as? Number)?.toInt() ?: return null
        if (level !in 1..999 || maxHp !in 1..999) return null
        return CachedOnlineProfile(
            publicProfileId = profileId,
            revision = revision,
            trainer = SharedTrainer(trainerName.take(MaxNameLength), trainerSprite),
            monster = SharedMonster(monsterName.take(MaxNameLength), level, maxHp, monsterSprite),
        )
    }

    private fun Map<*, *>.sprite(key: String): SharedSprite? {
        val value = get(key) as? Map<*, *> ?: return null
        val path = value["storagePath"] as? String ?: return null
        val hash = value["sha256"] as? String ?: return null
        val width = (value["width"] as? Number)?.toInt() ?: return null
        val height = (value["height"] as? Number)?.toInt() ?: return null
        val size = (value["byteSize"] as? Number)?.toLong() ?: return null
        if (!path.startsWith("onlineProfiles/") || !Hash.matches(hash) || width !in 1..1024 || height !in 1..1024 || size !in 1..MaxSpriteBytes) return null
        return SharedSprite(path, hash, width, height, size)
    }

    private companion object {
        const val Collection = "onlineProfiles"
        const val PrivateCollection = "private"
        const val OwnershipDocument = "ownership"
        const val OwnerUid = "ownerUid"
        const val RetentionConfirmedAt = "retentionConfirmedAt"
        const val PngContentType = "image/png"
        const val MaxNameLength = 120
        const val MaxSpriteBytes = 2L * 1024 * 1024
        val Hash = Regex("[a-f0-9]{64}")
    }
}
