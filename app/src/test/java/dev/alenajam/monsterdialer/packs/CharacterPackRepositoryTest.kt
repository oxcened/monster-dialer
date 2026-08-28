package dev.alenajam.monsterdialer.packs.data

import java.io.File
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class CharacterPackRepositoryTest {
    @get:Rule val temporaryFolder = TemporaryFolder()

    private val storageRoot by lazy { temporaryFolder.newFolder("packs") }
    private val catalog by lazy { CharacterPackCatalog(storageRoot) }
    private val repository by lazy { CharacterPackRepository(storageRoot, catalog) }

    @Test
    fun charactersAssignableToReturnsOnlyEnabledCharacters() {
        setupPack("pack1", "c1", enabled = true)
        setupPack("pack2", "c2", enabled = false)

        val contactChars = repository.charactersAssignableTo(CharacterAssignmentTarget.Contact)
        assertEquals(1, contactChars.size)
        assertEquals("c1", contactChars[0].character.id)
    }

    @Test
    fun charactersInPackReturnsCharactersWhenPackIsDisabled() {
        setupPack("pack1", "c1", enabled = false)

        val characters = repository.charactersInPack("pack1", "pack1 Pack")

        assertEquals(1, characters.size)
        assertEquals("c1", characters.single().character.id)
    }

    @Test
    fun findReturnsCorrectCharacter() {
        setupPack("pack1", "c1")
        val reference = CharacterReference("pack1", "c1")
        
        val found = repository.find(reference, CharacterAssignmentTarget.Contact, CharacterType.Monster)
        assertNotNull(found)
        assertEquals("c1", found?.character?.id)
    }

    @Test
    fun findReturnsNullForMissingCharacter() {
        setupPack("pack1", "c1")
        val reference = CharacterReference("pack1", "missing")
        
        val found = repository.find(reference, CharacterAssignmentTarget.Contact, CharacterType.Monster)
        assertNull(found)
    }

    @Test
    fun ignoresCorruptedPacks() {
        // Missing manifest
        val corruptedDir = File(File(storageRoot, "corrupted"), "active")
        corruptedDir.mkdirs()
        catalog.recordInstallation(CharacterPackManifest(1, "corrupted", "Corrupted", "1.0", "MIT", characters = listOf(
            PackCharacter("id", "Name", CharacterType.Monster, listOf(CharacterAssignmentTarget.Contact), frontImage = "front.png")
        )))

        // Pack with missing images
        setupPack("missing-images", "c3")
        File(File(File(storageRoot, "missing-images"), "active"), "front.png").delete()

        val allChars = repository.charactersAssignableTo(CharacterAssignmentTarget.Contact)
        assertTrue("Should ignore pack with missing manifest file", allChars.none { it.packId == "corrupted" })
        assertTrue("Should ignore character with missing image file", allChars.none { it.packId == "missing-images" })
    }

    private fun setupPack(packId: String, characterId: String, enabled: Boolean = true) {
        val packDir = File(File(storageRoot, packId), "active")
        packDir.mkdirs()
        
        val manifest = CharacterPackManifest(
            formatVersion = 1,
            id = packId,
            name = "$packId Pack",
            version = "1.0.0",
            license = "MIT",
            characters = listOf(
                PackCharacter(
                    id = characterId,
                    name = "Character $characterId",
                    type = CharacterType.Monster,
                    assignableTo = listOf(CharacterAssignmentTarget.Contact, CharacterAssignmentTarget.Player),
                    frontImage = "front.png",
                    backImage = "back.png"
                )
            )
        )
        
        val manifestJson = """
            {
                "formatVersion": 1,
                "id": "$packId",
                "name": "$packId Pack",
                "version": "1.0.0",
                "license": "MIT",
                "characters": [
                    {
                        "id": "$characterId",
                        "name": "Character $characterId",
                        "type": "monster",
                        "assignableTo": ["contact", "player"],
                        "frontImage": "front.png",
                        "backImage": "back.png"
                    }
                ]
            }
        """.trimIndent()
        
        File(packDir, "manifest.json").writeText(manifestJson)
        File(packDir, "front.png").writeText("fake image content")
        File(packDir, "back.png").writeText("fake image content")
        
        catalog.recordInstallation(manifest)
        if (!enabled) {
            catalog.setEnabled(packId, false)
        }
    }
}
