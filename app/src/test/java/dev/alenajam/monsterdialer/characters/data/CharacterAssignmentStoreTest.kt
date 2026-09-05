package dev.alenajam.monsterdialer.characters.data

import dev.alenajam.monsterdialer.packs.data.CharacterReference
import dev.alenajam.monsterdialer.packs.data.CharacterType
import java.io.File
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class CharacterAssignmentStoreTest {
    @get:Rule val temporaryFolder = TemporaryFolder()

    @Test
    fun initializesTheBundledMonsterAsTheActiveRosterMember() {
        val store = CharacterAssignmentStore(temporaryFolder.newFolder("assignments"))

        assertEquals(BuiltInCharacters.defaultMonsterReference, store.player(CharacterType.Monster))
        assertEquals(listOf(BuiltInCharacters.defaultMonsterReference), store.playerMonsterRoster())
    }

    @Test
    fun addsNewMonstersBehindTheBundledActiveMonster() {
        val store = CharacterAssignmentStore(temporaryFolder.newFolder("assignments"))
        val customMonster = CharacterReference("com.example.custom", "mossling")

        store.addPlayerMonsterToRoster(customMonster)

        assertEquals(BuiltInCharacters.defaultMonsterReference, store.player(CharacterType.Monster))
        assertEquals(
            listOf(BuiltInCharacters.defaultMonsterReference, customMonster),
            store.playerMonsterRoster(),
        )
    }

    @Test
    fun `normalizes the legacy bundled monster roster entry`() {
        val assignmentsDirectory = temporaryFolder.newFolder("legacy-bundled-monster")
        File(assignmentsDirectory, "character-assignments.json").writeText(
            """{"playerMonsterRoster":[{"packId":"builtin","characterId":"monster"}],"activePlayerMonster":{"packId":"builtin","characterId":"default-monster"}}"""
        )

        val store = CharacterAssignmentStore(assignmentsDirectory)

        assertEquals(listOf(BuiltInCharacters.defaultMonsterReference), store.playerMonsterRoster())
        assertEquals(1, File(assignmentsDirectory, "character-assignments.json").readText().split("default-monster").size - 1)
    }

    @Test
    fun `migrates a partial legacy document to an explicit bundled active monster`() {
        val assignmentsDirectory = temporaryFolder.newFolder("partial-legacy-document")
        val assignmentsFile = File(assignmentsDirectory, "character-assignments.json")
        assignmentsFile.writeText(
            """{"contacts":{"123":{"packId":"com.example.forest","characterId":"mossling"}}}"""
        )

        val store = CharacterAssignmentStore(assignmentsDirectory)

        assertEquals(BuiltInCharacters.defaultMonsterReference, store.player(CharacterType.Monster))
        assertTrue(assignmentsFile.readText().contains("default-monster"))
    }

    @Test
    fun `migrates the legacy single player monster into the roster`() {
        val assignmentsDirectory = temporaryFolder.newFolder("legacy-single-monster")
        val assignmentsFile = File(assignmentsDirectory, "character-assignments.json")
        val legacyMonster = CharacterReference("com.example.forest", "mossling")
        assignmentsFile.writeText(
            """{"player":{"packId":"${legacyMonster.packId}","characterId":"${legacyMonster.characterId}"}}"""
        )

        val store = CharacterAssignmentStore(assignmentsDirectory)

        assertEquals(legacyMonster, store.player(CharacterType.Monster))
        assertEquals(listOf(legacyMonster), store.playerMonsterRoster())
        assertFalse(assignmentsFile.readText().contains("\"player\":"))
    }

    @Test
    fun `removes a duplicated active monster from the persisted bench`() {
        val assignmentsDirectory = temporaryFolder.newFolder("duplicated-active-monster")
        val assignmentsFile = File(assignmentsDirectory, "character-assignments.json")
        assignmentsFile.writeText(
            """{"playerMonsterRoster":[{"packId":"builtin","characterId":"default-monster"}],"activePlayerMonster":{"packId":"builtin","characterId":"default-monster"}}"""
        )

        val store = CharacterAssignmentStore(assignmentsDirectory)

        assertEquals(listOf(BuiltInCharacters.defaultMonsterReference), store.playerMonsterRoster())
        assertEquals(1, assignmentsFile.readText().split("default-monster").size - 1)
        assertFalse(assignmentsFile.readText().contains("\"playerMonsterRoster\""))
    }

    @Test
    fun `replacing the built-in fallback creates the first roster entry`() {
        val store = CharacterAssignmentStore(temporaryFolder.newFolder("empty-roster"))
        val monster = CharacterReference("com.example.forest", "mossling")

        store.replacePlayerMonsterInRoster(0, monster)

        assertEquals(listOf(monster), store.playerMonsterRoster())
    }

    @Test
    fun `replacing a roster monster variant keeps its roster position`() {
        val store = CharacterAssignmentStore(temporaryFolder.newFolder("variant-roster"))
        val original = CharacterReference("com.example.forest", "mossling", "default")
        val replacement = CharacterReference("com.example.forest", "mossling", "sunset")
        store.setPlayerMonsterRoster(listOf(original))

        store.replacePlayerMonsterInRoster(0, replacement)

        assertEquals(listOf(replacement), store.playerMonsterRoster())
    }

    @Test
    fun `ignores a replacement outside the roster bounds`() {
        val store = CharacterAssignmentStore(temporaryFolder.newFolder("invalid-roster-index"))
        val replacement = CharacterReference("com.example.forest", "mossling")

        store.replacePlayerMonsterInRoster(index = 1, character = replacement)

        assertEquals(listOf(BuiltInCharacters.defaultMonsterReference), store.playerMonsterRoster())
    }

    @Test
    fun `a pack is in use when only an inactive roster monster belongs to it`() = runTest {
        val store = CharacterAssignmentStore(temporaryFolder.newFolder("pack-in-use-roster"))
        val activeMonster = CharacterReference("com.example.coast", "tidescale")
        val inactiveMonster = CharacterReference("com.example.forest", "mossling")
        store.setPlayerMonsterRoster(listOf(activeMonster, inactiveMonster))

        val repository = CharacterAssignmentRepositoryImpl(store)

        assertTrue(repository.isPackInUse(inactiveMonster.packId))
    }

    @Test
    fun `rejects a selected contact without a usable phone number`() {
        val store = CharacterAssignmentStore(temporaryFolder.newFolder("contact-without-phone-number"))

        val selected = store.setSelectedContact("Alex", listOf("", "Private number"), contactId = 42)

        assertFalse(selected)
        assertEquals(null, store.selectedContact())
    }

    @Test
    fun `unassigned contacts use the configured contact default`() {
        val store = CharacterAssignmentStore(temporaryFolder.newFolder("contact-defaults"))
        val trainer = CharacterReference("builtin", "trainer")

        store.setContactDefault(CharacterType.Trainer, trainer)

        assertEquals(
            ContactCharacterSelection(trainer, ContactCharacterMode.Default),
            store.selectionForContact("123", CharacterType.Trainer),
        )

        store.randomizeContact("123", CharacterType.Trainer)

        assertEquals(
            ContactCharacterSelection(null, ContactCharacterMode.Random),
            store.selectionForContact("123", CharacterType.Trainer),
        )
    }

    @Test
    fun `contact customization can inherit global defaults again`() {
        val store = CharacterAssignmentStore(temporaryFolder.newFolder("contact-inheritance"))
        val globalTrainer = CharacterReference("builtin", "trainer")
        val customTrainer = CharacterReference("com.example.forest", "mossling")

        store.setContactDefault(CharacterType.Trainer, globalTrainer)
        store.assignContact("123", CharacterType.Trainer, customTrainer)

        assertTrue(store.hasContactOverride("123", CharacterType.Trainer))
        assertEquals(
            ContactCharacterSelection(customTrainer, ContactCharacterMode.Default),
            store.selectionForContact("123", CharacterType.Trainer),
        )

        store.clearContactOverride("123", CharacterType.Trainer)

        assertFalse(store.hasContactOverride("123", CharacterType.Trainer))
        assertEquals(
            ContactCharacterSelection(globalTrainer, ContactCharacterMode.Default),
            store.selectionForContact("123", CharacterType.Trainer),
        )
    }

    @Test
    fun `contact randomizer pools are stored separately from player assignments`() {
        val store = CharacterAssignmentStore(temporaryFolder.newFolder("contact-pools"))
        val monster = CharacterReference("com.example.forest", "mossling")

        store.setContactRandomPool(CharacterType.Monster, listOf(monster))

        assertEquals(listOf(monster), store.contactCharacterDefaults().randomPools[CharacterType.Monster])
        assertEquals(BuiltInCharacters.defaultMonsterReference, store.player(CharacterType.Monster))

        store.setContactRandomPool(CharacterType.Monster, emptyList())
        assertEquals(emptyList<CharacterReference>(), store.contactCharacterDefaults().randomPools[CharacterType.Monster])

        store.clearContactRandomPool(CharacterType.Monster)
        assertFalse(store.contactCharacterDefaults().randomPools.containsKey(CharacterType.Monster))
    }
}
