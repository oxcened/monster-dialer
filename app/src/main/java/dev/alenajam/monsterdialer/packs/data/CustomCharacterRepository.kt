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
        imageUri: Uri
    ): CharacterReference = withContext(Dispatchers.IO) {
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
        val extension = context.contentResolver.getType(imageUri)?.substringAfterLast('/') ?: "png"
        val fileName = "art/$characterId.$extension"

        // Ensure directories exist
        artDirectory.mkdirs()

        // Copy image atomically
        val tempImage = File(artDirectory, ".${characterId}.${extension}.tmp")
        val targetFile = File(packDirectory, fileName)
        context.contentResolver.openInputStream(imageUri)?.use { input ->
            tempImage.outputStream().use { output ->
                input.copyTo(output)
            }
        } ?: throw CharacterPackValidationException("Could not read image")

        if (!tempImage.renameTo(targetFile)) {
            tempImage.delete()
            throw CharacterPackValidationException("Could not save image")
        }

        val newCharacter = PackCharacter(
            id = characterId,
            name = name.trim(),
            type = type,
            assignableTo = listOf(CharacterAssignmentTarget.Contact, CharacterAssignmentTarget.Player),
            frontImage = fileName,
            backImage = fileName // Reuse front image for back for MVP
        )

        val updatedManifest = currentManifest.copy(
            characters = currentManifest.characters + newCharacter
        )

        // Write manifest atomically
        val tempManifest = File(packDirectory, "${CharacterPackValidator.ManifestPath}.tmp-${UUID.randomUUID()}")
        try {
            tempManifest.writeText(json.encodeToString(updatedManifest))
            if (!tempManifest.renameTo(manifestFile)) {
                // Fallback: if rename fails (e.g. across different filesystems, though unlikely here), 
                // try direct write
                manifestFile.writeText(json.encodeToString(updatedManifest))
            }
        } finally {
            if (tempManifest.exists()) tempManifest.delete()
        }

        // Update catalog
        catalog.recordInstallation(updatedManifest)

        CharacterReference(CUSTOM_PACK_ID, characterId)
    }

    suspend fun updateCharacter(
        characterId: String,
        name: String,
        imageUri: Uri?
    ) = withContext(Dispatchers.IO) {
        val currentManifest = readManifest() ?: return@withContext
        val character = currentManifest.characters.find { it.id == characterId } ?: return@withContext

        var updatedFrontImage = character.frontImage
        var updatedBackImage = character.backImage

        if (imageUri != null) {
            val extension = context.contentResolver.getType(imageUri)?.substringAfterLast('/') ?: "png"
            val fileName = "art/$characterId.$extension"
            val targetFile = File(packDirectory, fileName)

            // Copy image atomically
            val tempImage = File(artDirectory, ".${characterId}.${extension}.tmp")
            context.contentResolver.openInputStream(imageUri)?.use { input ->
                tempImage.outputStream().use { output ->
                    input.copyTo(output)
                }
            } ?: throw CharacterPackValidationException("Could not read image")

            if (!tempImage.renameTo(targetFile)) {
                tempImage.delete()
                throw CharacterPackValidationException("Could not save image")
            }
            updatedFrontImage = fileName
            updatedBackImage = fileName
        }

        val updatedCharacters = currentManifest.characters.map {
            if (it.id == characterId) {
                it.copy(
                    name = name.trim(),
                    frontImage = updatedFrontImage,
                    backImage = updatedBackImage
                )
            } else it
        }

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

    fun getCharacterImageFile(characterId: String): File? {
        val character = getCharacter(characterId) ?: return null
        val relativePath = character.frontImage ?: character.backImage ?: return null
        return File(packDirectory, relativePath)
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
