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

        val assignableTo = mutableListOf<CharacterAssignmentTarget>()
        if (frontImageFileName != null) assignableTo.add(CharacterAssignmentTarget.Contact)
        if (backImageFileName != null) assignableTo.add(CharacterAssignmentTarget.Player)

        val newCharacter = PackCharacter(
            id = characterId,
            name = name.trim(),
            type = type,
            assignableTo = assignableTo,
            frontImage = frontImageFileName,
            backImage = backImageFileName,
            isRadiant = isRadiant && type == CharacterType.Monster,
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
        isRadiant: Boolean = false,
        level: Int? = null,
        maxHp: Int? = null
    ) = withContext(Dispatchers.IO) {
        val currentManifest = readManifest() ?: return@withContext
        val character = currentManifest.characters.find { it.id == characterId } ?: return@withContext

        val frontImageFileName = when {
            frontImageUri != null -> {
                // Delete old front image if exists
                character.frontImage?.let { File(packDirectory, it).delete() }
                saveImage(frontImageUri, "$characterId-front")
            }
            keepExistingFront -> character.frontImage
            else -> {
                character.frontImage?.let { File(packDirectory, it).delete() }
                null
            }
        }

        val backImageFileName = when {
            backImageUri != null -> {
                // Delete old back image if exists
                character.backImage?.let { File(packDirectory, it).delete() }
                saveImage(backImageUri, "$characterId-back")
            }
            keepExistingBack -> character.backImage
            else -> {
                character.backImage?.let { File(packDirectory, it).delete() }
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
                    frontImage = frontImageFileName,
                    backImage = backImageFileName,
                    assignableTo = assignableTo,
                    isRadiant = isRadiant && it.type == CharacterType.Monster,
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
        val tempManifest = File(packDirectory, "${CharacterPackValidator.ManifestPath}.tmp-${UUID.randomUUID()}")
        try {
            tempManifest.writeText(json.encodeToString(manifest))
            if (!tempManifest.renameTo(manifestFile)) {
                manifestFile.writeText(json.encodeToString(manifest))
            }
        } finally {
            if (tempManifest.exists()) tempManifest.delete()
        }
        catalog.recordInstallation(manifest)
    }

    suspend fun deleteCharacter(characterId: String) = withContext(Dispatchers.IO) {
        val currentManifest = readManifest() ?: return@withContext
        val character = currentManifest.characters.find { it.id == characterId } ?: return@withContext

        // Remove images
        listOfNotNull(character.frontImage, character.backImage).distinct().forEach { relativePath ->
            File(packDirectory, relativePath).delete()
        }

        val updatedCharacters = currentManifest.characters.filter { it.id != characterId }
        if (updatedCharacters.isEmpty()) {
            catalog.remove(CUSTOM_PACK_ID)
            File(storageRoot, CUSTOM_PACK_ID).deleteRecursively()
        } else {
            val updatedManifest = currentManifest.copy(characters = updatedCharacters)
            // Write manifest atomically
            val tempManifest = File(packDirectory, "${CharacterPackValidator.ManifestPath}.tmp-${UUID.randomUUID()}")
            try {
                tempManifest.writeText(json.encodeToString(updatedManifest))
                if (!tempManifest.renameTo(manifestFile)) {
                    manifestFile.writeText(json.encodeToString(updatedManifest))
                }
            } finally {
                if (tempManifest.exists()) tempManifest.delete()
            }
            catalog.recordInstallation(updatedManifest)
        }
    }

    fun getCharacterCount(): Int = readManifest()?.characters?.size ?: 0

    fun getCharacter(characterId: String): PackCharacter? =
        readManifest()?.characters?.find { it.id == characterId }

    fun getCharacterFrontImageFile(characterId: String): File? {
        val character = getCharacter(characterId) ?: return null
        val relativePath = character.frontImage ?: return null
        return File(packDirectory, relativePath)
    }

    fun getCharacterBackImageFile(characterId: String): File? {
        val character = getCharacter(characterId) ?: return null
        val relativePath = character.backImage ?: return null
        return File(packDirectory, relativePath)
    }

    data class ExportableCharacter(val character: PackCharacter, val frontImageFile: File?, val backImageFile: File?)

    fun getExportableCharacters(): List<ExportableCharacter> = readManifest()
        ?.characters
        ?.mapNotNull { character -> getExportableCharacter(character.id) }
        .orEmpty()

    fun getExportableCharacter(characterId: String): ExportableCharacter? = getCharacter(characterId)?.let { character ->
        ExportableCharacter(character, character.frontImage?.let { File(packDirectory, it) }?.takeIf(File::isFile), character.backImage?.let { File(packDirectory, it) }?.takeIf(File::isFile))
    }

    suspend fun importSharedCharacter(shared: dev.alenajam.monsterdialer.characters.data.SharedCharacterImport): CharacterReference = withContext(Dispatchers.IO) {
        val manifest = readManifest() ?: CharacterPackManifest(CharacterPackValidator.SupportedFormatVersion, CUSTOM_PACK_ID, "My Characters", "1.0.0", "Created by user", characters = emptyList())
        if (manifest.characters.size >= CharacterPackValidator.MaxCharacters) throw CharacterPackValidationException("Character limit reached (${CharacterPackValidator.MaxCharacters})")
        val id = UUID.randomUUID().toString()
        artDirectory.mkdirs()
        fun writeImage(bytes: ByteArray?, suffix: String) = bytes?.let { File(packDirectory, "art/$id-$suffix.png").apply { writeBytes(it) }.path.substringAfter("$packDirectory/") }
        val front = writeImage(shared.frontImage, "front")
        val back = writeImage(shared.backImage, "back")
        val character = PackCharacter(id, shared.character.name.trim(), shared.character.type, shared.character.assignableTo, front, back, level = shared.character.level, maxHp = shared.character.maxHp, isRadiant = shared.character.isRadiant)
        writeManifest(manifest.copy(characters = manifest.characters + character))
        CharacterReference(CUSTOM_PACK_ID, id)
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
