package dev.alenajam.monsterdialer.characters.data

import dev.alenajam.monsterdialer.packs.data.CharacterAssignmentTarget
import dev.alenajam.monsterdialer.packs.data.CharacterReference
import dev.alenajam.monsterdialer.packs.data.CharacterType
import dev.alenajam.monsterdialer.packs.data.InstalledPackCharacter
import kotlinx.coroutines.flow.Flow

interface CharactersRepository {
    fun observeCharactersAssignableTo(
        role: CharacterAssignmentTarget,
        type: CharacterType? = null
    ): Flow<List<InstalledPackCharacter>>

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

    suspend fun deleteCustomCharacter(characterId: String)

    suspend fun isPackInUse(packId: String): Boolean
}
