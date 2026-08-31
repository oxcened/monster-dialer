package dev.alenajam.monsterdialer.packs.data

import android.content.Context
import android.net.Uri
import dev.alenajam.monsterdialer.packs.di.CharacterPacksDir
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CustomCharacterRepository @Inject constructor(
    private val context: Context,
    @CharacterPacksDir private val storageRoot: File,
    private val catalog: CharacterPackCatalog
) {
    private val packDirectory = File(File(storageRoot, CUSTOM_PACK_ID), "active")
    private val artDirectory = File(packDirectory, "art")
    private val manifestFile = File(packDirectory, CharacterPackValidator.ManifestPath)

    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        prettyPrint = true
    }

    suspend fun addCharacter(
        name: String,
        type: CharacterType,
        frontImageUri: Uri?,
        backImageUri: Uri?,
        radiantFrontImageUri: Uri? = null,
        radiantBackImageUri: Uri? = null,
        isRadiant: Boolean = false,
        level: Int? = null,
        maxHp: Int? = null
    ): CharacterReference = withContext(Dispatchers.IO) {
        if (frontImageUri == null && backImageUri == null) {
            throw CharacterPackValidationException("At least one image is required")
        }

        // Check limit
        val currentManifest = readManifest() ?: CharacterPackManifest(
            formatVersion = CharacterPackValidator.SupportedFormatVersion,
            id = CUSTOM_PACK_ID,
            name = "My Characters",
            version = "1.0.0",
            license = "Created by user",
            characters = emptyList()
        )

        if (currentManifest.characters.size >= CharacterPackValidator.MaxCharacters) {
            throw CharacterPackValidationException("Character limit reached (${CharacterPackValidator.MaxCharacters})")
        }

        val characterId = UUID.randomUUID().toString()
        
        // Ensure directories exist
        artDirectory.mkdirs()

        val frontImageFileName = frontImageUri?.let { uri ->
            saveImage(uri, "$characterId-front")
        }
        val backImageFileName = backImageUri?.let { uri ->
            saveImage(uri, "$characterId-back")
        }
        val radiantFrontImageFileName = radiantFrontImageUri?.let { uri ->
            saveImage(uri, "$characterId-radiant-front")
        }
        val radiantBackImageFileName = radiantBackImageUri?.let { uri ->
            saveImage(uri, "$characterId-radiant-back")
        }

        val assignableTo = mutableListOf<CharacterAssignmentTarget>()
        if (frontImageFileName != null) assignableTo.add(CharacterAssignmentTarget.Contact)
        if (backImageFileName != null) assignableTo.add(CharacterAssignmentTarget.Player)

        val newCharacter = PackCharacter(
            id = characterId,
            name = name.trim(),
            type = type,
            assignableTo = assignableTo,
            variants = visualVariants(
                frontImageFileName,
                backImageFileName,
                radiantFrontImageFileName,
                radiantBackImageFileName,
                isRadiant && type == CharacterType.Monster,
            ),
            level = level,
            maxHp = maxHp
        )

        val updatedManifest = currentManifest.copy(
            characters = currentManifest.characters + newCharacter
        )

        writeManifest(updatedManifest)

        CharacterReference(CUSTOM_PACK_ID, characterId)
    }

    suspend fun updateCharacter(
        characterId: String,
        name: String,
        frontImageUri: Uri?,
        keepExistingFront: Boolean,
        backImageUri: Uri?,
        keepExistingBack: Boolean,
        radiantFrontImageUri: Uri? = null,
        keepExistingRadiantFront: Boolean = false,
        radiantBackImageUri: Uri? = null,
        keepExistingRadiantBack: Boolean = false,
        isRadiant: Boolean = false,
        level: Int? = null,
        maxHp: Int? = null
    ) = withContext(Dispatchers.IO) {
        val currentManifest = readManifest() ?: return@withContext
        val character = currentManifest.characters.find { it.id == characterId } ?: return@withContext

        val frontImageFileName = when {
            frontImageUri != null -> {
                // Delete old front image if exists
                character.variant(PackCharacter.DefaultVariantId)?.frontImage?.let { File(packDirectory, it).delete() }
                saveImage(frontImageUri, "$characterId-front")
            }
            keepExistingFront -> character.variant(PackCharacter.DefaultVariantId)?.frontImage
            else -> {
                character.variant(PackCharacter.DefaultVariantId)?.frontImage?.let { File(packDirectory, it).delete() }
                null
            }
        }

        val backImageFileName = when {
            backImageUri != null -> {
                // Delete old back image if exists
                character.variant(PackCharacter.DefaultVariantId)?.backImage?.let { File(packDirectory, it).delete() }
                saveImage(backImageUri, "$characterId-back")
            }
            keepExistingBack -> character.variant(PackCharacter.DefaultVariantId)?.backImage
            else -> {
                character.variant(PackCharacter.DefaultVariantId)?.backImage?.let { File(packDirectory, it).delete() }
                null
            }
        }

        val radiantFrontImageFileName = when {
            radiantFrontImageUri != null -> {
                character.variant(PackCharacter.RadiantVariantId)?.frontImage?.let { File(packDirectory, it).delete() }
                saveImage(radiantFrontImageUri, "$characterId-radiant-front")
            }
            keepExistingRadiantFront -> character.variant(PackCharacter.RadiantVariantId)?.frontImage
            else -> {
                character.variant(PackCharacter.RadiantVariantId)?.frontImage?.let { File(packDirectory, it).delete() }
                null
            }
        }

        val radiantBackImageFileName = when {
            radiantBackImageUri != null -> {
                character.variant(PackCharacter.RadiantVariantId)?.backImage?.let { File(packDirectory, it).delete() }
                saveImage(radiantBackImageUri, "$characterId-radiant-back")
            }
            keepExistingRadiantBack -> character.variant(PackCharacter.RadiantVariantId)?.backImage
            else -> {
                character.variant(PackCharacter.RadiantVariantId)?.backImage?.let { File(packDirectory, it).delete() }
                null
            }
        }

        val assignableTo = mutableListOf<CharacterAssignmentTarget>()
        if (frontImageFileName != null) assignableTo.add(CharacterAssignmentTarget.Contact)
        if (backImageFileName != null) assignableTo.add(CharacterAssignmentTarget.Player)

        val updatedCharacters = currentManifest.characters.map {
            if (it.id == characterId) {
                it.copy(
                    name = name.trim(),
                    frontImage = null,
                    backImage = null,
                    radiantFrontImage = null,
                    radiantBackImage = null,
                    variants = visualVariants(
                        frontImageFileName,
                        backImageFileName,
                        radiantFrontImageFileName,
                        radiantBackImageFileName,
                        isRadiant && it.type == CharacterType.Monster,
                    ),
                    assignableTo = assignableTo,
                    isRadiant = false,
                    level = level,
                    maxHp = maxHp
                )
            } else it
        }

        val updatedManifest = currentManifest.copy(characters = updatedCharacters)
        writeManifest(updatedManifest)
    }

    private fun saveImage(uri: Uri, nameWithoutExtension: String): String {
        val extension = context.contentResolver.getType(uri)?.substringAfterLast('/') ?: "png"
        val fileName = "art/$nameWithoutExtension.$extension"
        val targetFile = File(packDirectory, fileName)

        val tempFile = File(artDirectory, ".$nameWithoutExtension.$extension.tmp")
        context.contentResolver.openInputStream(uri)?.use { input ->
            tempFile.outputStream().use { output ->
                input.copyTo(output)
            }
        } ?: throw CharacterPackValidationException("Could not read image")

        if (!tempFile.renameTo(targetFile)) {
            tempFile.delete()
            throw CharacterPackValidationException("Could not save image")
        }
        return fileName
    }

    private fun writeManifest(manifest: CharacterPackManifest) {
        val currentManifest = manifest.copy(
            formatVersion = CharacterPackValidator.CurrentFormatVersion,
            characters = manifest.characters.map { character ->
                if (character.variants.isEmpty()) character.copy(variants = character.visualVariants) else character
            }
        )
        val tempManifest = File(packDirectory, "${CharacterPackValidator.ManifestPath}.tmp-${UUID.randomUUID()}")
        try {
            tempManifest.writeText(json.encodeToString(currentManifest))
            if (!tempManifest.renameTo(manifestFile)) {
                manifestFile.writeText(json.encodeToString(currentManifest))
            }
        } finally {
            if (tempManifest.exists()) tempManifest.delete()
        }
        catalog.recordInstallation(currentManifest)
    }

    suspend fun deleteCharacter(characterId: String) = withContext(Dispatchers.IO) {
        val currentManifest = readManifest() ?: return@withContext
        val character = currentManifest.characters.find { it.id == characterId } ?: return@withContext

        // Remove images
        character.visualVariants.flatMap { listOfNotNull(it.frontImage, it.backImage) }.distinct().forEach { relativePath ->
            File(packDirectory, relativePath).delete()
        }

        val updatedCharacters = currentManifest.characters.filter { it.id != characterId }
        if (updatedCharacters.isEmpty()) {
            catalog.remove(CUSTOM_PACK_ID)
            File(storageRoot, CUSTOM_PACK_ID).deleteRecursively()
        } else {
            writeManifest(currentManifest.copy(characters = updatedCharacters))
        }
    }

    fun getCharacterCount(): Int = readManifest()?.characters?.size ?: 0

    fun getCharacter(characterId: String): PackCharacter? =
        readManifest()?.characters?.find { it.id == characterId }

    fun getCharacterFrontImageFile(characterId: String): File? {
        val character = getCharacter(characterId) ?: return null
        val relativePath = character.variant(PackCharacter.DefaultVariantId)?.frontImage ?: return null
        return File(packDirectory, relativePath)
    }

    fun getCharacterBackImageFile(characterId: String): File? {
        val character = getCharacter(characterId) ?: return null
        val relativePath = character.variant(PackCharacter.DefaultVariantId)?.backImage ?: return null
        return File(packDirectory, relativePath)
    }

    fun getCharacterRadiantFrontImageFile(characterId: String): File? {
        val character = getCharacter(characterId) ?: return null
        val relativePath = character.variant(PackCharacter.RadiantVariantId)?.frontImage ?: return null
        return File(packDirectory, relativePath)
    }

    fun getCharacterRadiantBackImageFile(characterId: String): File? {
        val character = getCharacter(characterId) ?: return null
        val relativePath = character.variant(PackCharacter.RadiantVariantId)?.backImage ?: return null
        return File(packDirectory, relativePath)
    }

    data class ExportableCharacter(
        val character: PackCharacter,
        val frontImageFile: File?,
        val backImageFile: File?,
        val radiantFrontImageFile: File?,
        val radiantBackImageFile: File?,
    )

    fun getExportableCharacters(): List<ExportableCharacter> = readManifest()
        ?.characters
        ?.mapNotNull { character -> getExportableCharacter(character.id) }
        .orEmpty()

    fun getExportableCharacter(characterId: String): ExportableCharacter? = getCharacter(characterId)?.let { character ->
        ExportableCharacter(
            character = character,
            frontImageFile = character.variant(PackCharacter.DefaultVariantId)?.frontImage?.let { File(packDirectory, it) }?.takeIf(File::isFile),
            backImageFile = character.variant(PackCharacter.DefaultVariantId)?.backImage?.let { File(packDirectory, it) }?.takeIf(File::isFile),
            radiantFrontImageFile = character.variant(PackCharacter.RadiantVariantId)?.frontImage?.let { File(packDirectory, it) }?.takeIf(File::isFile),
            radiantBackImageFile = character.variant(PackCharacter.RadiantVariantId)?.backImage?.let { File(packDirectory, it) }?.takeIf(File::isFile),
        )
    }

    suspend fun importSharedCharacter(shared: dev.alenajam.monsterdialer.characters.data.SharedCharacterImport): CharacterReference = withContext(Dispatchers.IO) {
        val manifest = readManifest() ?: CharacterPackManifest(CharacterPackValidator.SupportedFormatVersion, CUSTOM_PACK_ID, "My Characters", "1.0.0", "Created by user", characters = emptyList())
        if (manifest.characters.size >= CharacterPackValidator.MaxCharacters) throw CharacterPackValidationException("Character limit reached (${CharacterPackValidator.MaxCharacters})")
        val id = UUID.randomUUID().toString()
        artDirectory.mkdirs()
        fun writeImage(bytes: ByteArray?, suffix: String) = bytes?.let { File(packDirectory, "art/$id-$suffix.png").apply { writeBytes(it) }.path.substringAfter("$packDirectory/") }
        val front = writeImage(shared.frontImage, "front")
        val back = writeImage(shared.backImage, "back")
        val radiantFront = writeImage(shared.radiantFrontImage, "radiant-front")
        val radiantBack = writeImage(shared.radiantBackImage, "radiant-back")
        val character = PackCharacter(
            id = id,
            name = shared.character.name.trim(),
            type = shared.character.type,
            assignableTo = shared.character.assignableTo,
            variants = visualVariants(front, back, radiantFront, radiantBack, shared.character.isRadiant),
            level = shared.character.level,
            maxHp = shared.character.maxHp,
        )
        writeManifest(manifest.copy(characters = manifest.characters + character))
        CharacterReference(CUSTOM_PACK_ID, id)
    }

    private fun visualVariants(
        frontImage: String?,
        backImage: String?,
        radiantFrontImage: String?,
        radiantBackImage: String?,
        isLegacyRadiant: Boolean,
    ): List<CharacterVisualVariant> = buildList {
        add(
            CharacterVisualVariant(
                id = PackCharacter.DefaultVariantId,
                name = PackCharacter.DefaultVariantName,
                frontImage = frontImage,
                backImage = backImage,
                isRadiant = isLegacyRadiant && radiantFrontImage == null && radiantBackImage == null,
            )
        )
        if (radiantFrontImage != null || radiantBackImage != null) {
            add(
                CharacterVisualVariant(
                    id = PackCharacter.RadiantVariantId,
                    name = PackCharacter.RadiantVariantName,
                    frontImage = radiantFrontImage,
                    backImage = radiantBackImage,
                    isRadiant = true,
                )
            )
        }
    }

    private fun readManifest(): CharacterPackManifest? {
        if (!manifestFile.exists()) return null
        return try {
            json.decodeFromString<CharacterPackManifest>(manifestFile.readText())
        } catch (e: Exception) {
            null
        }
    }

    companion object {
        const val CUSTOM_PACK_ID = "user.custom"
    }
}
