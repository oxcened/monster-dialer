package dev.alenajam.monsterdialer.characters.ui

import dev.alenajam.monsterdialer.packs.data.CharacterReference
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CharacterSelectionStateTest {
    private val bundledMonster = CharacterReference("builtin", "default-monster")

    @Test
    fun `does not select the default option for an empty multi-select pool`() {
        val state = characterSelectionState(
            characters = emptyList(),
            selected = null,
            selectedReferences = emptySet(),
            defaultReference = bundledMonster,
            allowsDeselection = true,
            isRandomSelected = false,
        )

        assertFalse(state.isDefaultSelected)
    }

    @Test
    fun `selects the default option when the multi-select pool includes it`() {
        val state = characterSelectionState(
            characters = emptyList(),
            selected = null,
            selectedReferences = setOf(bundledMonster),
            defaultReference = bundledMonster,
            allowsDeselection = true,
            isRandomSelected = false,
        )

        assertTrue(state.isDefaultSelected)
    }
}
