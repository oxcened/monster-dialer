package dev.alenajam.monsterdialer.characters.data

import dev.alenajam.monsterdialer.packs.data.CharacterReference
import dev.alenajam.monsterdialer.packs.data.CharacterType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

interface CharacterAssignmentRepository {
    val assignmentVersion: StateFlow<Long>
    suspend fun getAssignedCharacter(contactKey: String, type: CharacterType): CharacterReference?
    suspend fun getContactCharacterSelection(contactKey: String, type: CharacterType): ContactCharacterSelection
    suspend fun hasContactOverride(contactKey: String, type: CharacterType): Boolean
    suspend fun clearContactOverride(contactKey: String, type: CharacterType)
    suspend fun getContactCharacterDefaults(): ContactCharacterDefaults
    suspend fun setContactDefault(type: CharacterType, reference: CharacterReference?)
    suspend fun setContactRandomPool(type: CharacterType, references: List<CharacterReference>)
    suspend fun clearContactRandomPool(type: CharacterType)
    suspend fun getContactRandomPool(type: CharacterType): List<CharacterReference>?
    suspend fun getContactRandomPool(contactKey: String, type: CharacterType): List<CharacterReference>?
    suspend fun setContactRandomPool(contactKey: String, type: CharacterType, references: List<CharacterReference>)
    suspend fun clearContactRandomPool(contactKey: String, type: CharacterType)
    suspend fun assignCharacter(
        contactKey: String,
        type: CharacterType,
        reference: CharacterReference?,
        label: String?
    )
    suspend fun randomizeCharacter(contactKey: String, type: CharacterType, label: String?)
    suspend fun getPlayerCharacter(type: CharacterType): CharacterReference?
    suspend fun setPlayerCharacter(type: CharacterType, reference: CharacterReference?)
    suspend fun getPlayerMonsterRoster(): List<CharacterReference>
    suspend fun setPlayerMonsterRoster(roster: List<CharacterReference>)
    suspend fun addPlayerMonsterToRoster(reference: CharacterReference)
    suspend fun removePlayerMonsterFromRoster(reference: CharacterReference)
    suspend fun replacePlayerMonsterInRoster(index: Int, reference: CharacterReference)
    suspend fun assignedContactCount(): Int
    suspend fun isCharacterAssignedToPlayer(reference: CharacterReference): Boolean
    suspend fun isCharacterAssignedToAnyContact(reference: CharacterReference): Boolean
    suspend fun clearAssignmentsForPack(packId: String)
    suspend fun clearAssignmentsForCharacter(reference: CharacterReference)
    suspend fun isPackInUse(packId: String): Boolean
}

@Singleton
class CharacterAssignmentRepositoryImpl @Inject constructor(
    private val assignments: CharacterAssignmentStore
) : CharacterAssignmentRepository {

    private val _assignmentVersion = MutableStateFlow(0L)
    override val assignmentVersion: StateFlow<Long> = _assignmentVersion

    override suspend fun getAssignedCharacter(
        contactKey: String,
        type: CharacterType
    ): CharacterReference? = withContext(Dispatchers.IO) {
        assignments.characterForContact(contactKey, type)
    }

    override suspend fun getContactCharacterSelection(
        contactKey: String,
        type: CharacterType,
    ): ContactCharacterSelection = withContext(Dispatchers.IO) {
        assignments.selectionForContact(contactKey, type)
    }

    override suspend fun hasContactOverride(contactKey: String, type: CharacterType): Boolean = withContext(Dispatchers.IO) {
        assignments.hasContactOverride(contactKey, type)
    }

    override suspend fun clearContactOverride(contactKey: String, type: CharacterType) = withContext(Dispatchers.IO) {
        assignments.clearContactOverride(contactKey, type)
        notifyAssignmentsChanged()
    }

    override suspend fun getContactCharacterDefaults(): ContactCharacterDefaults = withContext(Dispatchers.IO) {
        assignments.contactCharacterDefaults()
    }

    override suspend fun setContactDefault(type: CharacterType, reference: CharacterReference?) = withContext(Dispatchers.IO) {
        assignments.setContactDefault(type, reference)
        notifyAssignmentsChanged()
    }

    override suspend fun setContactRandomPool(type: CharacterType, references: List<CharacterReference>) = withContext(Dispatchers.IO) {
        assignments.setContactRandomPool(type, references)
        notifyAssignmentsChanged()
    }

    override suspend fun clearContactRandomPool(type: CharacterType) = withContext(Dispatchers.IO) {
        assignments.clearContactRandomPool(type)
        notifyAssignmentsChanged()
    }

    override suspend fun getContactRandomPool(type: CharacterType): List<CharacterReference>? = withContext(Dispatchers.IO) {
        assignments.contactCharacterDefaults().randomPools[type]
    }

    override suspend fun getContactRandomPool(contactKey: String, type: CharacterType): List<CharacterReference>? = withContext(Dispatchers.IO) {
        assignments.contactRandomPool(contactKey, type)
    }

    override suspend fun setContactRandomPool(contactKey: String, type: CharacterType, references: List<CharacterReference>) = withContext(Dispatchers.IO) {
        assignments.setContactRandomPool(contactKey, type, references)
        notifyAssignmentsChanged()
    }

    override suspend fun clearContactRandomPool(contactKey: String, type: CharacterType) = withContext(Dispatchers.IO) {
        assignments.clearContactRandomPool(contactKey, type)
        notifyAssignmentsChanged()
    }

    override suspend fun assignCharacter(
        contactKey: String,
        type: CharacterType,
        reference: CharacterReference?,
        label: String?
    ) = withContext(Dispatchers.IO) {
        assignments.assignContact(contactKey, type, reference, label)
        notifyAssignmentsChanged()
    }

    override suspend fun randomizeCharacter(contactKey: String, type: CharacterType, label: String?) = withContext(Dispatchers.IO) {
        assignments.randomizeContact(contactKey, type, label)
        notifyAssignmentsChanged()
    }

    override suspend fun getPlayerCharacter(type: CharacterType): CharacterReference? = withContext(Dispatchers.IO) {
        assignments.player(type)
    }

    override suspend fun setPlayerCharacter(
        type: CharacterType,
        reference: CharacterReference?
    ) = withContext(Dispatchers.IO) {
        assignments.setPlayer(type, reference)
        notifyAssignmentsChanged()
    }

    override suspend fun getPlayerMonsterRoster(): List<CharacterReference> = withContext(Dispatchers.IO) {
        assignments.playerMonsterRoster()
    }

    override suspend fun setPlayerMonsterRoster(roster: List<CharacterReference>) = withContext(Dispatchers.IO) {
        assignments.setPlayerMonsterRoster(roster)
        notifyAssignmentsChanged()
    }

    override suspend fun addPlayerMonsterToRoster(reference: CharacterReference) = withContext(Dispatchers.IO) {
        assignments.addPlayerMonsterToRoster(reference)
        notifyAssignmentsChanged()
    }

    override suspend fun removePlayerMonsterFromRoster(reference: CharacterReference) = withContext(Dispatchers.IO) {
        assignments.removePlayerMonsterFromRoster(reference)
        notifyAssignmentsChanged()
    }

    override suspend fun replacePlayerMonsterInRoster(index: Int, reference: CharacterReference) = withContext(Dispatchers.IO) {
        assignments.replacePlayerMonsterInRoster(index, reference)
        notifyAssignmentsChanged()
    }

    override suspend fun assignedContactCount(): Int = withContext(Dispatchers.IO) {
        assignments.assignedContactCount()
    }

    override suspend fun isCharacterAssignedToPlayer(
        reference: CharacterReference
    ): Boolean = withContext(Dispatchers.IO) {
        CharacterType.entries.any { assignments.player(it)?.sameCharacterAs(reference) == true } ||
            assignments.playerMonsterRoster().any { it.sameCharacterAs(reference) }
    }

    override suspend fun isCharacterAssignedToAnyContact(
        reference: CharacterReference
    ): Boolean = withContext(Dispatchers.IO) {
        assignments.contactAssignments().any { it.character.sameCharacterAs(reference) }
    }

    override suspend fun clearAssignmentsForPack(
        packId: String
    ) = withContext(Dispatchers.IO) {
        assignments.clearAssignmentsForPack(packId)
        notifyAssignmentsChanged()
    }

    override suspend fun clearAssignmentsForCharacter(
        reference: CharacterReference
    ) = withContext(Dispatchers.IO) {
        assignments.clearAssignmentsForCharacter(reference)
        notifyAssignmentsChanged()
    }

    override suspend fun isPackInUse(
        packId: String
    ): Boolean = withContext(Dispatchers.IO) {
        val playerInUse = CharacterType.entries.any { assignments.player(it)?.packId == packId } ||
            assignments.playerMonsterRoster().any { it.packId == packId }
        val contactsInUse = assignments.contactAssignments().any { it.character.packId == packId }
        playerInUse || contactsInUse
    }

    private fun CharacterReference.sameCharacterAs(other: CharacterReference): Boolean =
        packId == other.packId && characterId == other.characterId

    private fun notifyAssignmentsChanged() {
        _assignmentVersion.update { it + 1 }
    }
}
