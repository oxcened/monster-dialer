package dev.alenajam.monsterdialer.packs.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * A directory of externally hosted packs. A catalog contains metadata and download locations;
 * pack bytes are always validated by the normal character-pack installer before installation.
 */
@Serializable
data class RemotePackCatalog(
    val formatVersion: Int,
    val id: String,
    val name: String,
    val publisher: String? = null,
    val website: String? = null,
    val updatedAt: String? = null,
    val packs: List<RemotePackCatalogPack>,
)

@Serializable
data class RemotePackCatalogPack(
    val id: String,
    val version: String,
    val name: String,
    val license: String,
    val creator: String? = null,
    val description: String? = null,
    val downloadUrl: String,
    val sha256: String,
    val sizeBytes: Long,
    val minAppVersion: String? = null,
)

internal object RemotePackCatalogCodec {
    private val json = Json {
        ignoreUnknownKeys = false
        explicitNulls = false
    }

    fun decode(text: String): RemotePackCatalog = try {
        json.decodeFromString<RemotePackCatalog>(text).also(::validate)
    } catch (exception: CharacterPackValidationException) {
        throw exception
    } catch (exception: Exception) {
        throw CharacterPackValidationException("Catalog is not valid: ${exception.message}")
    }

    private fun validate(catalog: RemotePackCatalog) {
        if (catalog.formatVersion != FormatVersion) fail("Catalog formatVersion must be $FormatVersion")
        if (!Identifier.matches(catalog.id)) fail("Catalog id is invalid")
        if (catalog.name.isBlank() || catalog.name.length > MaxNameLength) fail("Catalog name is invalid")
        if (catalog.packs.size > MaxPackCount) fail("Catalog contains too many packs")
        if (catalog.packs.map { it.id }.toSet().size != catalog.packs.size) fail("Catalog pack ids must be unique")
        catalog.packs.forEach(::validatePack)
    }

    private fun validatePack(pack: RemotePackCatalogPack) {
        if (!Identifier.matches(pack.id)) fail("Catalog pack id is invalid")
        if (pack.name.isBlank() || pack.name.length > MaxNameLength) fail("Catalog pack name is invalid")
        if (pack.version.isBlank() || pack.version.length > MaxVersionLength) fail("Catalog pack version is invalid")
        if (pack.license.isBlank() || pack.license.length > MaxLicenseLength) fail("Catalog pack license is invalid")
        if (pack.description?.length ?: 0 > MaxDescriptionLength) fail("Catalog pack description is too long")
        if (pack.sizeBytes !in 1..MaxPackBytes) fail("Catalog pack size is invalid")
        if (!Sha256.matches(pack.sha256)) fail("Catalog pack SHA-256 is invalid")
        CatalogUrlPolicy.requireHttps(pack.downloadUrl)
    }

    private fun fail(message: String): Nothing = throw CharacterPackValidationException(message)

    private const val FormatVersion = 1
    private const val MaxPackCount = 200
    private const val MaxNameLength = 120
    private const val MaxVersionLength = 64
    private const val MaxLicenseLength = 120
    private const val MaxDescriptionLength = 1_000
    private const val MaxPackBytes = 24L * 1024 * 1024
    private val Identifier = Regex("[a-z0-9][a-z0-9._-]{1,63}")
    private val Sha256 = Regex("[a-fA-F0-9]{64}")
}
