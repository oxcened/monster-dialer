package dev.alenajam.monsterdialer.data.characters

import dev.alenajam.monsterdialer.packs.CharacterAssignmentTarget
import dev.alenajam.monsterdialer.packs.CharacterReference
import dev.alenajam.monsterdialer.packs.CharacterType
import dev.alenajam.monsterdialer.packs.InstalledPackCharacter
import dev.alenajam.opendialer.data.contacts.DialerContactSummary
import kotlinx.coroutines.flow.Flow

interface CharactersRepository {
    fun getCharactersAssignableTo(
        role: CharacterAssignmentTarget,
        type: CharacterType? = null
    ): List<InstalledPackCharacter>

    suspend fun getSelectedContact(): MonsterContact?

    suspend fun setSelectedContact(selectedContact: DialerContactSummary)

    suspend fun clearSelectedContact()

    suspend fun getAssignedCharacter(contactKey: String, type: CharacterType): CharacterReference?

    suspend fun assignCharacter(
        contactKey: String,
        type: CharacterType,
        reference: CharacterReference?,
        label: String?
    )

    fun findCharacter(
        reference: CharacterReference,
        role: CharacterAssignmentTarget,
        type: CharacterType
    ): InstalledPackCharacter?

    suspend fun getPlayerCharacter(type: CharacterType): CharacterReference?

    suspend fun setPlayerCharacter(type: CharacterType, reference: CharacterReference?)
}
