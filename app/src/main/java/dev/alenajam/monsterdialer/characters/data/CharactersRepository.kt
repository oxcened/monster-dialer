package dev.alenajam.monsterdialer.characters.data

import dev.alenajam.monsterdialer.packs.data.CharacterAssignmentTarget
import dev.alenajam.monsterdialer.packs.data.CharacterReference
import dev.alenajam.monsterdialer.packs.data.CharacterType
import dev.alenajam.monsterdialer.packs.data.InstalledPackCharacter

interface CharactersRepository {
    fun getCharactersAssignableTo(
        role: CharacterAssignmentTarget,
        type: CharacterType? = null
    ): List<InstalledPackCharacter>

    fun findCharacter(
        reference: CharacterReference,
        role: CharacterAssignmentTarget,
        type: CharacterType
    ): InstalledPackCharacter?

    /** Returns a pack's valid installed characters, including when the pack is disabled. */
    fun getCharactersInPack(packId: String, packName: String): List<InstalledPackCharacter>
}
