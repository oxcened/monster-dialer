package dev.alenajam.monsterdialer.characters.data

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.Source
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.alenajam.monsterdialer.packs.data.CharacterReference
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.tasks.await

/** Private, append-only cloud storage for unlockable character variants. */
@Singleton
class FirebaseVariantBackupRemoteDataSource @Inject constructor(
    @ApplicationContext private val context: Context,
) : VariantBackupRemoteDataSource {
    override suspend fun restore(): Set<CharacterReference> {
        val userId = currentUserId() ?: return emptySet()
        val firestore = FirebaseFirestore.getInstance(firebaseApp())
        return firestore.collection(Collection)
            .document(userId)
            .collection(UnlocksCollection)
            .get(Source.SERVER)
            .await()
            .documents
            .mapNotNull(::referenceOrNull)
            .toSet()
    }

    override suspend fun backup(references: Set<CharacterReference>) {
        if (references.isEmpty()) return
        val userId = requireNotNull(currentUserId())
        val firestore = FirebaseFirestore.getInstance(firebaseApp())
        references.chunked(MaxBatchSize).forEach { chunk ->
            firestore.runBatch { batch ->
                chunk.forEach { reference ->
                    val document = firestore.collection(Collection)
                        .document(userId)
                        .collection(UnlocksCollection)
                        .document(reference.documentId())
                    batch.set(document, reference.asDocument(), SetOptions.merge())
                }
            }.await()
        }
    }

    override suspend fun deleteAll() {
        val userId = requireNotNull(currentUserId())
        val firestore = FirebaseFirestore.getInstance(firebaseApp())
        val unlocks = firestore.collection(Collection)
            .document(userId)
            .collection(UnlocksCollection)
            .get(Source.SERVER)
            .await()
            .documents
        unlocks.chunked(MaxBatchSize).forEach { chunk ->
            firestore.runBatch { batch ->
                chunk.forEach { document -> batch.delete(document.reference) }
            }.await()
        }
    }

    override fun isSignedIn(): Boolean = currentUserId() != null

    private fun currentUserId(): String? = FirebaseApp.initializeApp(context)
        ?.let(FirebaseAuth::getInstance)
        ?.currentUser
        ?.uid

    private fun firebaseApp(): FirebaseApp = requireNotNull(FirebaseApp.initializeApp(context))

    private fun referenceOrNull(snapshot: com.google.firebase.firestore.DocumentSnapshot): CharacterReference? {
        if (snapshot.getLong(SchemaVersion) != Schema.toLong()) return null
        val packId = snapshot.getString(PackId) ?: return null
        val characterId = snapshot.getString(CharacterId) ?: return null
        val variantId = snapshot.getString(VariantId) ?: return null
        return CharacterReference(packId, characterId, variantId).takeIf(::isValidReference)
    }

    private fun CharacterReference.asDocument(): Map<String, Any> = mapOf(
        SchemaVersion to Schema,
        PackId to packId,
        CharacterId to characterId,
        VariantId to variantId,
    )

    private fun CharacterReference.documentId(): String = "$packId:$characterId:$variantId"

    private fun isValidReference(reference: CharacterReference): Boolean = listOf(
        reference.packId,
        reference.characterId,
        reference.variantId,
    ).all { Identifier.matches(it) }

    private companion object {
        const val Collection = "variantBackups"
        const val UnlocksCollection = "unlocks"
        const val SchemaVersion = "schemaVersion"
        const val PackId = "packId"
        const val CharacterId = "characterId"
        const val VariantId = "variantId"
        const val Schema = 1
        const val MaxBatchSize = 400
        val Identifier = Regex("[a-z0-9][a-z0-9._-]{1,63}")
    }
}
