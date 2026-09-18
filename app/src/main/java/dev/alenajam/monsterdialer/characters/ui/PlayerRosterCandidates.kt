package dev.alenajam.monsterdialer.characters.ui

import dev.alenajam.monsterdialer.packs.data.CharacterReference
import dev.alenajam.monsterdialer.packs.data.InstalledPackCharacter

/** Excludes monsters that already occupy another slot in the player's party. */
internal fun List<InstalledPackCharacter>.availableForPlayerRoster(
    roster: List<CharacterReference>,
    targetSlotIndex: Int?,
): List<InstalledPackCharacter> {
    val occupiedElsewhere = roster
        .filterIndexed { index, _ -> index != targetSlotIndex }
        .mapTo(mutableSetOf()) { it.packId to it.characterId }
    return filterNot { character ->
        (character.packId to character.character.id) in occupiedElsewhere
    }
}
