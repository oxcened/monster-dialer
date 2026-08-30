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
        val characterId = UUID.randomUUID().toString()
        val extension = context.contentResolver.getType(imageUri)?.substringAfterLast('/') ?: "png"
        val fileName = "art/$characterId.$extension"

        // Ensure directories exist
        artDirectory.mkdirs()

        // Copy image
        val targetFile = File(packDirectory, fileName)
        context.contentResolver.openInputStream(imageUri)?.use { input ->
            targetFile.outputStream().use { output ->
                input.copyTo(output)
            }
        } ?: throw CharacterPackValidationException("Could not read image")

        // Update manifest
        val currentManifest = readManifest() ?: CharacterPackManifest(
            formatVersion = CharacterPackValidator.SupportedFormatVersion,
            id = CUSTOM_PACK_ID,
            name = "My Characters",
            version = "1.0.0",
            license = "Created by user",
            characters = emptyList()
        )

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

        manifestFile.writeText(json.encodeToString(updatedManifest))

        // Update catalog
        catalog.recordInstallation(updatedManifest)

        CharacterReference(CUSTOM_PACK_ID, characterId)
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
