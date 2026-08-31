package dev.alenajam.monsterdialer.packs.data

import java.io.File
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

data class CharacterPackExportRequest(
    val characterIds: Set<String>,
    val id: String,
    val name: String,
    val version: String,
    val creator: String,
    val license: String,
)

@Singleton
class CharacterPackExportRepository @Inject constructor(
    private val customCharacters: CustomCharacterRepository,
) {
    private val json = Json { explicitNulls = false }

    fun export(request: CharacterPackExportRequest, output: OutputStream) {
        val selected = customCharacters.getExportableCharacters()
            .filter { it.character.id in request.characterIds }
        require(selected.isNotEmpty()) { "Select at least one character" }
        require(selected.size == request.characterIds.size) { "One or more selected characters are unavailable" }

        val manifest = CharacterPackManifest(
            formatVersion = CharacterPackValidator.SupportedFormatVersion,
            id = request.id.trim(),
            name = request.name.trim(),
            version = request.version.trim(),
            creator = request.creator.trim().ifBlank { null },
            license = request.license.trim(),
            characters = selected.map { it.character },
        )
        CharacterPackValidator.validate(manifest)

        ZipOutputStream(output).use { zip ->
            zip.putNextEntry(ZipEntry(CharacterPackValidator.ManifestPath))
            zip.write(json.encodeToString(manifest).encodeToByteArray())
            zip.closeEntry()
            selected.flatMap { exported ->
                listOfNotNull(
                    exported.character.frontImage to exported.frontImageFile,
                    exported.character.backImage to exported.backImageFile,
                ).filter { (path, file) -> path != null && file?.isFile == true }
            }.forEach { (path, file) ->
                zip.putNextEntry(ZipEntry(requireNotNull(path)))
                requireNotNull(file).inputStream().use { it.copyTo(zip) }
                zip.closeEntry()
            }
        }
    }
}
