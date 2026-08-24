package dev.alenajam.monsterdialer.packs

import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class CharacterPackArchiveReaderTest {
    @get:Rule val temporaryFolder = TemporaryFolder()

    private val reader = CharacterPackArchiveReader()

    @Test
    fun readsAValidPackAndIncludesOnlyReferencedFiles() {
        val archive = archive(
            "manifest.json" to validManifest(),
            "art/mossling.png" to "image",
            "audio/mossling.ogg" to "sound",
            "notes.txt" to "not installed"
        )

        val pack = reader.read(archive)

        assertEquals("com.example.forest", pack.manifest.id)
        assertEquals(setOf("manifest.json", "art/mossling.png", "audio/mossling.ogg"), pack.files)
    }

    @Test(expected = CharacterPackValidationException::class)
    fun rejectsTraversalPathsBeforeExtraction() {
        reader.read(archive("manifest.json" to validManifest(frontImage = "../outside.png")))
    }

    @Test(expected = CharacterPackValidationException::class)
    fun rejectsManifestReferencesToMissingFiles() {
        reader.read(archive("manifest.json" to validManifest()))
    }

    @Test(expected = CharacterPackValidationException::class)
    fun rejectsUnknownManifestFields() {
        reader.read(
            archive(
                "manifest.json" to validManifest().dropLast(1) + ",\"unexpected\":true}",
                "art/mossling.png" to "image",
                "audio/mossling.ogg" to "sound"
            )
        )
    }

    @Test
    fun installerKeepsOnlyValidatedFilesInPrivateStorage() {
        val storage = temporaryFolder.newFolder("character-packs")
        val archive = archive(
            "manifest.json" to validManifest(),
            "art/mossling.png" to "image",
            "audio/mossling.ogg" to "sound",
            "notes.txt" to "not installed"
        )

        val installed = CharacterPackInstaller(storage).install(archive.inputStream())

        assertTrue(File(installed.directory, "art/mossling.png").isFile)
        assertTrue(File(installed.directory, "audio/mossling.ogg").isFile)
        assertTrue(!File(installed.directory, "notes.txt").exists())
        assertEquals("com.example.forest", CharacterPackCatalog(storage).list().single().id)
    }

    private fun archive(vararg entries: Pair<String, String>): File {
        val file = temporaryFolder.newFile("pack-${System.nanoTime()}.zip")
        ZipOutputStream(file.outputStream()).use { zip ->
            entries.forEach { (path, contents) ->
                zip.putNextEntry(ZipEntry(path))
                zip.write(contents.toByteArray())
                zip.closeEntry()
            }
        }
        return file
    }

    private fun validManifest(frontImage: String = "art/mossling.png") = """
        {
          "formatVersion": 1,
          "id": "com.example.forest",
          "name": "Forest Characters",
          "version": "1.0.0",
          "license": "CC-BY-4.0",
          "characters": [{
            "id": "mossling",
            "name": "Mossling",
            "assignableTo": ["contact", "player"],
            "frontImage": "$frontImage",
            "callSound": "audio/mossling.ogg"
          }]
        }
    """.trimIndent()
}
