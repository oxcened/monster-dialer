package dev.alenajam.monsterdialer.packs.data

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
    /**
     * Named visual forms for this character. New packs use this field; the image fields below
     * remain readable solely for previously released always-radiant character data.
     */
    val variants: List<CharacterVisualVariant> = emptyList(),
    val radiantFrontImage: String? = null,
    val radiantBackImage: String? = null,
    val callSound: String? = null,
    val level: Int? = null,
    val maxHp: Int? = null,
    /** Whether this monster plays the radiant animation when it enters battle. */
    val isRadiant: Boolean = false
) {
    val visualVariants: List<CharacterVisualVariant>
        get() = variants.ifEmpty {
            buildList {
                add(CharacterVisualVariant(DefaultVariantId, DefaultVariantName, frontImage, backImage, isRadiant))
                if (radiantFrontImage != null || radiantBackImage != null) {
                    add(CharacterVisualVariant(RadiantVariantId, RadiantVariantName, radiantFrontImage, radiantBackImage, true))
                }
            }
        }

    val hasRadiantVariant: Boolean
        get() = visualVariants.any { it.isRadiant }

    fun variant(id: String): CharacterVisualVariant? = visualVariants.find { it.id == id }

    companion object {
        const val DefaultVariantId = "default"
        const val DefaultVariantName = "Default"
        const val RadiantVariantId = "radiant"
        const val RadiantVariantName = "Radiant"
    }
}

@Serializable
data class CharacterVisualVariant(
    val id: String,
    val name: String,
    val frontImage: String? = null,
    val backImage: String? = null,
    /** Whether this form receives radiant battle effects. */
    val isRadiant: Boolean = false,
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
    val directory: java.io.File,
    val isEditable: Boolean = false,
    val isDeletable: Boolean = false
) {
    fun imageFile(relativePath: String): java.io.File = java.io.File(directory, relativePath)
}

@Serializable
data class CharacterReference(
    val packId: String,
    val characterId: String,
    @SerialName("variant") val variantId: String = PackCharacter.DefaultVariantId,
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
