package dev.alenajam.monsterdialer.characters.data

import dev.alenajam.monsterdialer.packs.data.CharacterAssignmentTarget
import dev.alenajam.monsterdialer.packs.data.CharacterPackValidationException
import dev.alenajam.monsterdialer.packs.data.CharacterType
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class SharedCharacter(
    val formatVersion: Int = 1,
    val name: String,
    val creator: String,
    val license: String,
    val type: CharacterType,
    val assignableTo: List<CharacterAssignmentTarget>,
    val frontImage: String? = null,
    val backImage: String? = null,
    val isRadiant: Boolean = false,
    val level: Int? = null,
    val maxHp: Int? = null,
)

data class SharedCharacterImport(
    val character: SharedCharacter,
    val frontImage: ByteArray?,
    val backImage: ByteArray?,
)

object SharedCharacterArchive {
    const val MimeType = "application/x-monstercharacter"
    const val Extension = "monstercharacter"
    private const val ManifestFile = "character.json"
    private val json = Json { ignoreUnknownKeys = false; explicitNulls = false }

    fun write(character: SharedCharacter, images: Map<String, File>, output: OutputStream) {
        ZipOutputStream(output).use { zip ->
            zip.putNextEntry(ZipEntry(ManifestFile))
            zip.write(json.encodeToString(character).encodeToByteArray())
            zip.closeEntry()
            images.forEach { (name, file) ->
                zip.putNextEntry(ZipEntry(name))
                file.inputStream().use { it.copyTo(zip) }
                zip.closeEntry()
            }
        }
    }

    fun read(input: InputStream): SharedCharacterImport {
        var character: SharedCharacter? = null
        val files = mutableMapOf<String, ByteArray>()
        ZipInputStream(input).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                require(!entry.isDirectory && entry.name !in files) { "Invalid character archive" }
                val bytes = zip.readBytes()
                if (entry.name == ManifestFile) character = json.decodeFromString(SharedCharacter.serializer(), bytes.decodeToString())
                else files[entry.name] = bytes
                zip.closeEntry()
            }
        }
        val shared = requireNotNull(character) { "Character archive has no manifest" }
        require(
            shared.formatVersion == 1 &&
                shared.name.isNotBlank() &&
                shared.creator.isNotBlank() &&
                shared.license.isNotBlank()
        ) { "Character archive is not supported" }
        return SharedCharacterImport(shared, shared.frontImage?.let { requireNotNull(files[it]) }, shared.backImage?.let { requireNotNull(files[it]) })
    }
}
