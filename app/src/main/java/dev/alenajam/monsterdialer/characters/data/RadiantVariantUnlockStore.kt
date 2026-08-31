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

/** Persists radiant forms discovered in wild encounters. */
@Singleton
class RadiantVariantUnlockStore @Inject constructor(
    @CharacterPacksDir private val storageRoot: File,
) {
    private val file = File(storageRoot, "radiant-variant-unlocks.json")
    private val json = Json { ignoreUnknownKeys = false; explicitNulls = false }
    private val mutableUnlocked = MutableStateFlow(read())
    val unlocked: StateFlow<Set<CharacterReference>> = mutableUnlocked.asStateFlow()

    @Synchronized
    fun unlock(reference: CharacterReference) {
        if (reference in mutableUnlocked.value) return
        val updated = mutableUnlocked.value + reference
        file.parentFile?.mkdirs()
        file.writeText(json.encodeToString(RadiantVariantUnlockDocument(updated.toList())))
        mutableUnlocked.value = updated
    }

    private fun read(): Set<CharacterReference> = runCatching {
        json.decodeFromString<RadiantVariantUnlockDocument>(file.readText()).unlocked.toSet()
    }.getOrDefault(emptySet())

    @Serializable
    private data class RadiantVariantUnlockDocument(
        val unlocked: List<CharacterReference> = emptyList(),
    )
}
