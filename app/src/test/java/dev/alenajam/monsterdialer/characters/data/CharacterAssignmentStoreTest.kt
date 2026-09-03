package dev.alenajam.monsterdialer.characters.data

import dev.alenajam.monsterdialer.packs.data.CharacterReference
import dev.alenajam.monsterdialer.packs.data.CharacterType
import org.junit.Assert.assertEquals
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
}
