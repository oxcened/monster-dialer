package dev.alenajam.monsterdialer.characters.data

import dev.alenajam.monsterdialer.packs.data.CharacterReference
import dev.alenajam.monsterdialer.packs.di.CharacterPacksDir
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** Persists unlockable character variants, including radiant discoveries and future variant types. */
@Singleton
class VariantUnlockStore @Inject constructor(
    @CharacterPacksDir private val storageRoot: File,
) {
    private val file = File(storageRoot, "variant-unlocks.json")
    private val json = Json { ignoreUnknownKeys = false; explicitNulls = false }
    private val mutableUnlocked = MutableStateFlow(read())
    val unlocked: StateFlow<Set<CharacterReference>> = mutableUnlocked.asStateFlow()

    fun hasStoredData(): Boolean = file.isFile

    @Synchronized
    fun reload() {
        mutableUnlocked.value = read()
    }

    @Synchronized
    fun unlock(reference: CharacterReference): Boolean {
        if (reference in mutableUnlocked.value) return false
        persist(mutableUnlocked.value + reference)
        return true
    }

    /** Adds restored unlocks without ever removing a local discovery. */
    @Synchronized
    fun merge(references: Set<CharacterReference>): Boolean {
        val updated = mutableUnlocked.value + references
        if (updated == mutableUnlocked.value) return false
        persist(updated)
        return true
    }

    private fun persist(updated: Set<CharacterReference>) {
        file.parentFile?.mkdirs()
        file.writeText(json.encodeToString(VariantUnlockDocument(updated.toList())))
        mutableUnlocked.value = updated
    }

    private fun read(): Set<CharacterReference> = runCatching {
        json.decodeFromString<VariantUnlockDocument>(file.readText()).unlocked.toSet()
    }.getOrDefault(emptySet())

    @Serializable
    private data class VariantUnlockDocument(
        val unlocked: List<CharacterReference> = emptyList(),
    )
}
