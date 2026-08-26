package dev.alenajam.monsterdialer.ui.battle

import dev.alenajam.monsterdialer.packs.CharacterAssignmentStore
import dev.alenajam.monsterdialer.packs.CharacterAssignmentTarget
import dev.alenajam.monsterdialer.packs.CharacterPackCatalog
import dev.alenajam.monsterdialer.packs.CharacterPackManifest
import dev.alenajam.monsterdialer.packs.CharacterPackRepository
import dev.alenajam.monsterdialer.packs.CharacterReference
import dev.alenajam.monsterdialer.packs.CharacterType
import dev.alenajam.monsterdialer.packs.InstalledPackCharacter
import dev.alenajam.monsterdialer.packs.PackCharacter
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class AssignedCharacterEncounterFactoryTest {
    @get:Rule val temporaryFolder = TemporaryFolder()

    private val storageRoot by lazy { temporaryFolder.newFolder("packs") }
    private val catalog by lazy { CharacterPackCatalog(storageRoot) }
    private val assignmentsRoot by lazy { temporaryFolder.newFolder("assignments") }
    private val store by lazy { CharacterAssignmentStore(assignmentsRoot) }
    private val repository by lazy { CharacterPackRepository(storageRoot, catalog) }
    private val factory by lazy { AssignedCharacterEncounterFactory(store, repository) }

    @Test
    fun forCallPicksAssignedMonsters() {
        val packId = "com.example.test"
        val characterId = "monster1"
        setupPack(packId, characterId, CharacterType.Monster, CharacterAssignmentTarget.Contact)
        
        val reference = CharacterReference(packId, characterId)
        store.assignContact("1234567890", CharacterType.Monster, reference)

        val encounter = factory.forCall("call1", "1234567890", "Alex", isAnonymous = false)

        assertEquals("Monster 1", encounter.enemy?.name)
        assertEquals(EncounterType.Trainer, encounter.type)
    }

    @Test
    fun phoneNumberNormalizationWorksInStore() {
        val character = CharacterReference("pack", "char")
        
        // Test various formats
        store.assignContact("+1 (234) 567-890", character)
        
        assertEquals(character, store.characterForContact("+1234567890"))
        assertEquals(character, store.characterForContact(" +1-234-567-890 "))
        assertEquals(character, store.characterForContact("+1.234.567.890"))
    }

    @Test
    fun fallsBackToBattleEncounterFactoryWhenNoAssignmentFound() {
        val encounter = factory.forCall("call1", "999", "Unknown", isAnonymous = false)
        
        val defaultEncounter = BattleEncounterFactory.forCall("call1", "Unknown", isAnonymous = false)
        assertEquals(defaultEncounter.player.name, encounter.player.name)
        assertEquals(defaultEncounter.enemy?.name, encounter.enemy?.name)
    }

    @Test
    fun handlesAnonymousCallsByIgnoringContactAssignments() {
        val packId = "com.example.test"
        val characterId = "monster1"
        setupPack(packId, characterId, CharacterType.Monster, CharacterAssignmentTarget.Contact)
        
        val reference = CharacterReference(packId, characterId)
        store.assignContact("123", CharacterType.Monster, reference)

        val encounter = factory.forCall("call1", "123", "Alex", isAnonymous = true)

        assertEquals(EncounterType.Anonymous, encounter.type)
        assertNotEquals("Monster 1", encounter.enemy?.name)
    }

    private fun setupPack(
        packId: String,
        characterId: String,
        type: CharacterType,
        vararg assignableTo: CharacterAssignmentTarget
    ) {
        val packDir = File(File(storageRoot, packId), "active")
        packDir.mkdirs()
        
        val manifestObj = CharacterPackManifest(
            formatVersion = 1,
            id = packId,
            name = "Test Pack",
            version = "1.0.0",
            license = "MIT",
            characters = listOf(
                PackCharacter(
                    id = characterId,
                    name = "Monster 1",
                    type = type,
                    assignableTo = assignableTo.toList(),
                    frontImage = "front.png",
                    backImage = "back.png"
                )
            )
        )

        val manifest = """
            {
                "formatVersion": 1,
                "id": "$packId",
                "name": "Test Pack",
                "version": "1.0.0",
                "license": "MIT",
                "characters": [
                    {
                        "id": "$characterId",
                        "name": "Monster 1",
                        "type": "${if (type == CharacterType.Monster) "monster" else "trainer"}",
                        "assignableTo": [${assignableTo.joinToString { "\"${if (it == CharacterAssignmentTarget.Contact) "contact" else "player"}\"" }}],
                        "frontImage": "front.png",
                        "backImage": "back.png"
                    }
                ]
            }
        """.trimIndent()
        
        File(packDir, "manifest.json").writeText(manifest)
        File(packDir, "front.png").writeText("fake image content")
        File(packDir, "back.png").writeText("fake image content")
        
        catalog.recordInstallation(manifestObj)
    }
}
