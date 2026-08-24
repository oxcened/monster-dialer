package dev.alenajam.monsterdialer.packs

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * The on-disk format for a local, user-provided character pack.
 *
 * Packs deliberately contain data and media only. Network locations, executable code, and
 * franchise-specific fields are not part of this format.
 */
@Serializable
data class CharacterPackManifest(
    val formatVersion: Int,
    val id: String,
    val name: String,
    val version: String,
    val license: String,
    val creator: String? = null,
    val characters: List<PackCharacter>
)

@Serializable
data class PackCharacter(
    val id: String,
    val name: String,
    val type: CharacterType,
    val assignableTo: List<CharacterAssignmentTarget>,
    val frontImage: String? = null,
    val backImage: String? = null,
    val callSound: String? = null,
    val level: Int? = null,
    val maxHp: Int? = null
)

@Serializable
enum class CharacterType {
    @SerialName("trainer") Trainer,
    @SerialName("monster") Monster
}

@Serializable
enum class CharacterAssignmentTarget {
    @SerialName("contact") Contact,
    @SerialName("player") Player
}

data class ValidatedCharacterPack(
    val manifest: CharacterPackManifest,
    /** Archive entries needed by this pack, including its manifest. */
    val files: Set<String>
)

data class InstalledCharacterPack(
    val manifest: CharacterPackManifest,
    val directory: java.io.File
)

/** One validated character together with the private directory that owns its media. */
data class InstalledPackCharacter(
    val packId: String,
    val packName: String,
    val character: PackCharacter,
    val directory: java.io.File
) {
    fun imageFile(relativePath: String): java.io.File = java.io.File(directory, relativePath)
}

@Serializable
data class CharacterReference(
    val packId: String,
    val characterId: String
)

internal object CharacterPackManifestCodec {
    private val json = Json {
        ignoreUnknownKeys = false
        explicitNulls = false
    }

    fun decode(text: String): CharacterPackManifest = try {
        json.decodeFromString<CharacterPackManifest>(text)
    } catch (exception: Exception) {
        throw CharacterPackValidationException("Pack manifest is not valid: ${exception.message}")
    }
}
