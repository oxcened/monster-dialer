package dev.alenajam.monsterdialer.packs

import java.io.File
import java.util.Base64
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
            "art/mossling-back.png" to "image",
            "audio/mossling.ogg" to "sound",
            "notes.txt" to "not installed"
        )

        val pack = reader.read(archive)

        assertEquals("com.example.forest", pack.manifest.id)
        assertEquals(12, pack.manifest.characters.single().level)
        assertEquals(45, pack.manifest.characters.single().maxHp)
        assertEquals(
            setOf("manifest.json", "art/mossling.png", "art/mossling-back.png", "audio/mossling.ogg"),
            pack.files
        )
    }

    @Test
    fun acceptsCharactersWithoutOptionalBattleStats() {
        val archive = archive(
            "manifest.json" to validManifest(battleStats = ""),
            "art/mossling.png" to "image",
            "art/mossling-back.png" to "image",
            "audio/mossling.ogg" to "sound"
        )

        val character = reader.read(archive).manifest.characters.single()

        assertEquals(null, character.level)
        assertEquals(null, character.maxHp)
    }

    @Test(expected = CharacterPackValidationException::class)
    fun rejectsInvalidBattleStats() {
        reader.read(
            archive(
                "manifest.json" to validManifest(battleStats = "\"level\": 0,"),
                "art/mossling.png" to "image",
                "art/mossling-back.png" to "image",
                "audio/mossling.ogg" to "sound"
            )
        )
    }

    @Test(expected = CharacterPackValidationException::class)
    fun rejectsBattleStatsLongerThanThreeDigits() {
        reader.read(
            archive(
                "manifest.json" to validManifest(battleStats = "\"level\": 12, \"maxHp\": 1000,"),
                "art/mossling.png" to "image",
                "art/mossling-back.png" to "image",
                "audio/mossling.ogg" to "sound"
            )
        )
    }

    @Test(expected = CharacterPackValidationException::class)
    fun rejectsTraversalPathsBeforeExtraction() {
        reader.read(archive("manifest.json" to validManifest(frontImage = "../outside.png")))
    }

    @Test
    fun acceptsPlayerOnlyCharacterWithoutFrontImage() {
        val archive = archive(
            "manifest.json" to validManifest(
                frontImage = null,
                assignableTo = "[\"player\"]"
            ),
            "art/mossling-back.png" to "image",
            "audio/mossling.ogg" to "sound"
        )

        val character = reader.read(archive).manifest.characters.single()

        assertEquals(null, character.frontImage)
        assertEquals("art/mossling-back.png", character.backImage)
    }

    @Test(expected = CharacterPackValidationException::class)
    fun rejectsContactCharacterWithoutFrontImage() {
        reader.read(
            archive(
                "manifest.json" to validManifest(
                    frontImage = null,
                    assignableTo = "[\"contact\"]"
                ),
                "art/mossling-back.png" to "image",
                "audio/mossling.ogg" to "sound"
            )
        )
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
                "art/mossling-back.png" to "image",
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
            "art/mossling-back.png" to "image",
            "audio/mossling.ogg" to "sound",
            "notes.txt" to "not installed"
        )

        val installed = CharacterPackInstaller(storage).install(archive.inputStream())

        assertTrue(File(installed.directory, "art/mossling.png").isFile)
        assertTrue(File(installed.directory, "audio/mossling.ogg").isFile)
        assertTrue(!File(installed.directory, "notes.txt").exists())
        assertEquals("com.example.forest", CharacterPackCatalog(storage).list().single().id)
    }

    @Test
    fun repositoryExposesOnlyEnabledCharactersForTheirAllowedRoles() {
        val storage = temporaryFolder.newFolder("character-packs")
        val archive = archive(
            "manifest.json" to validManifest(),
            "art/mossling.png" to "image",
            "art/mossling-back.png" to "image",
            "audio/mossling.ogg" to "sound"
        )
        CharacterPackInstaller(storage).install(archive.inputStream())
        val catalog = CharacterPackCatalog(storage)
        val repository = CharacterPackRepository(storage, catalog)

        assertEquals(1, repository.charactersAssignableTo(CharacterAssignmentTarget.Contact).size)
        assertEquals(1, repository.charactersAssignableTo(CharacterAssignmentTarget.Player).size)

        catalog.setEnabled("com.example.forest", enabled = false)
        assertTrue(repository.charactersAssignableTo(CharacterAssignmentTarget.Contact).isEmpty())
    }

    @Test
    fun assignmentsPersistPlayerAndContactCharactersSeparately() {
        val storage = temporaryFolder.newFolder("character-assignments")
        val store = CharacterAssignmentStore(storage)
        val player = CharacterReference("com.example.forest", "mossling")
        val contact = CharacterReference("com.example.forest", "fernfox")

        store.setPlayer(player)
        store.assignContact("tel:+390000000", contact, label = "Alex")
        store.setSelectedContact("Alex", listOf("tel:+390000000"))

        val restored = CharacterAssignmentStore(storage)
        assertEquals(player, restored.player())
        assertEquals(contact, restored.characterForContact("tel:+390000000"))
        assertEquals("Alex", restored.contactAssignments().single().label)
        assertEquals("Alex", restored.selectedContact()?.label)
        assertEquals(listOf("390000000"), restored.selectedContact()?.contactKeys)

        restored.assignContact("tel:+390000000", null)
        assertEquals(null, restored.characterForContact("tel:+390000000"))
    }

    @Test
    fun assignmentsNormalizePhoneNumberFormatting() {
        val store = CharacterAssignmentStore(temporaryFolder.newFolder("normalized-assignments"))
        val character = CharacterReference("com.example.forest", "mossling")

        store.assignContact("+39 000 000", character)

        assertEquals(character, store.characterForContact("+39000000"))
    }

    @Test
    fun trainerAndMonsterAssignmentsPersistIndependently() {
        val store = CharacterAssignmentStore(temporaryFolder.newFolder("typed-assignments"))
        val trainer = CharacterReference("com.example.forest", "ranger")
        val monster = CharacterReference("com.example.forest", "mossling")

        store.setPlayer(CharacterType.Trainer, trainer)
        store.setPlayer(CharacterType.Monster, monster)
        store.assignContact("123", CharacterType.Trainer, trainer)
        store.assignContact("123", CharacterType.Monster, monster)

        assertEquals(trainer, store.player(CharacterType.Trainer))
        assertEquals(monster, store.player(CharacterType.Monster))
        assertEquals(trainer, store.characterForContact("123", CharacterType.Trainer))
        assertEquals(monster, store.characterForContact("123", CharacterType.Monster))
    }

    private fun archive(vararg entries: Pair<String, String>): File {
        val file = temporaryFolder.newFile("pack-${System.nanoTime()}.zip")
        ZipOutputStream(file.outputStream()).use { zip ->
            entries.forEach { (path, contents) ->
                zip.putNextEntry(ZipEntry(path))
                zip.write(if (contents == "image") tinyPng else contents.toByteArray())
                zip.closeEntry()
            }
        }
        return file
    }

    private fun validManifest(
        frontImage: String? = "art/mossling.png",
        assignableTo: String = "[\"contact\", \"player\"]",
        battleStats: String = "\"level\": 12, \"maxHp\": 45,"
    ): String {
        val frontImageField = frontImage?.let { "\"frontImage\": \"$it\"," }.orEmpty()
        return """
        {
          "formatVersion": 1,
          "id": "com.example.forest",
          "name": "Forest Characters",
          "version": "1.0.0",
          "license": "CC-BY-4.0",
          "characters": [{
            "id": "mossling",
            "name": "Mossling",
            "type": "monster",
            $battleStats
            "assignableTo": $assignableTo,
            $frontImageField
            "backImage": "art/mossling-back.png",
            "callSound": "audio/mossling.ogg"
          }]
        }
    """.trimIndent()
    }

    private companion object {
        val tinyPng = Base64.getDecoder().decode(
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVQIHWP4z8DwHwAFgAI/ScL8jQAAAABJRU5ErkJggg=="
        )
    }
}
