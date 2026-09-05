package dev.alenajam.monsterdialer.characters.ui

import dev.alenajam.monsterdialer.characters.data.isBuiltInMonsterRosterReference
import dev.alenajam.monsterdialer.packs.data.CharacterReference
import dev.alenajam.monsterdialer.packs.data.InstalledPackCharacter

internal data class CharacterSelectionState(
    val availableSelection: CharacterReference?,
    val isDefaultSelected: Boolean,
) {
    fun isReferenceSelected(reference: CharacterReference, selectedReferences: Set<CharacterReference>): Boolean =
        if (selectedReferences.isEmpty()) availableSelection == reference else reference in selectedReferences
}

internal fun characterSelectionState(
    characters: List<InstalledPackCharacter>,
    selected: CharacterReference?,
    selectedReferences: Set<CharacterReference>,
    defaultReference: CharacterReference?,
    allowsDeselection: Boolean,
    isRandomSelected: Boolean,
): CharacterSelectionState {
    val availableSelection = selected?.takeIf { reference -> characters.any { it.matches(reference) } }
    val isDefaultSelected = (
        selected?.isBuiltInMonsterRosterReference() == true ||
            selectedReferences.any(CharacterReference::isBuiltInMonsterRosterReference) ||
            defaultReference in selectedReferences ||
            (selectedReferences.isEmpty() && availableSelection == null && !allowsDeselection)
        ) && (!isRandomSelected || selectedReferences.isNotEmpty())
    return CharacterSelectionState(availableSelection, isDefaultSelected)
}
