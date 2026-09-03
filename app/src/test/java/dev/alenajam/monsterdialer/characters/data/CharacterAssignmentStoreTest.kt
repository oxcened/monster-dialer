package dev.alenajam.monsterdialer.characters.data

import dev.alenajam.monsterdialer.packs.data.CharacterReference
import dev.alenajam.monsterdialer.packs.data.CharacterType
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
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
    fun `a pack is in use when only an inactive roster monster belongs to it`() = runTest {
        val store = CharacterAssignmentStore(temporaryFolder.newFolder("pack-in-use-roster"))
        val activeMonster = CharacterReference("com.example.coast", "tidescale")
        val inactiveMonster = CharacterReference("com.example.forest", "mossling")
        store.setPlayerMonsterRoster(listOf(activeMonster, inactiveMonster))

        val repository = CharacterAssignmentRepositoryImpl(store)

        assertTrue(repository.isPackInUse(inactiveMonster.packId))
    }
}
