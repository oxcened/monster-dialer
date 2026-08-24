package dev.alenajam.monsterdialer.packs

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

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
    val assignableTo: List<CharacterAssignmentTarget>,
    val frontImage: String,
    val backImage: String? = null,
    val callSound: String? = null
)

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
