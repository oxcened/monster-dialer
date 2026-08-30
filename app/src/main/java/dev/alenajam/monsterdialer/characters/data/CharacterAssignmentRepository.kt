package dev.alenajam.monsterdialer.characters.data

import dev.alenajam.monsterdialer.packs.data.CharacterReference
import dev.alenajam.monsterdialer.packs.data.CharacterType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

interface CharacterAssignmentRepository {
    val assignmentVersion: StateFlow<Long>
    suspend fun getAssignedCharacter(contactKey: String, type: CharacterType): CharacterReference?
    suspend fun assignCharacter(
        contactKey: String,
        type: CharacterType,
        reference: CharacterReference?,
        label: String?
    )
    suspend fun getPlayerCharacter(type: CharacterType): CharacterReference?
    suspend fun setPlayerCharacter(type: CharacterType, reference: CharacterReference?)
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

    override suspend fun assignCharacter(
        contactKey: String,
        type: CharacterType,
        reference: CharacterReference?,
        label: String?
    ) = withContext(Dispatchers.IO) {
        assignments.assignContact(contactKey, type, reference, label)
        _assignmentVersion.value += 1
    }

    override suspend fun getPlayerCharacter(type: CharacterType): CharacterReference? = withContext(Dispatchers.IO) {
        assignments.player(type)
    }

    override suspend fun setPlayerCharacter(
        type: CharacterType,
        reference: CharacterReference?
    ) = withContext(Dispatchers.IO) {
        assignments.setPlayer(type, reference)
        _assignmentVersion.value += 1
    }

    override suspend fun assignedContactCount(): Int = withContext(Dispatchers.IO) {
        assignments.assignedContactCount()
    }

    override suspend fun isCharacterAssignedToPlayer(
        reference: CharacterReference
    ): Boolean = withContext(Dispatchers.IO) {
        CharacterType.entries.any { assignments.player(it) == reference }
    }

    override suspend fun isCharacterAssignedToAnyContact(
        reference: CharacterReference
    ): Boolean = withContext(Dispatchers.IO) {
        assignments.contactAssignments().any { it.character == reference }
    }

    override suspend fun clearAssignmentsForPack(
        packId: String
    ) = withContext(Dispatchers.IO) {
        assignments.clearAssignmentsForPack(packId)
        _assignmentVersion.value += 1
    }

    override suspend fun clearAssignmentsForCharacter(
        reference: CharacterReference
    ) = withContext(Dispatchers.IO) {
        assignments.clearAssignmentsForCharacter(reference)
        _assignmentVersion.value += 1
    }

    override suspend fun isPackInUse(
        packId: String
    ): Boolean = withContext(Dispatchers.IO) {
        val playerInUse = CharacterType.entries.any { assignments.player(it)?.packId == packId }
        val contactsInUse = assignments.contactAssignments().any { it.character.packId == packId }
        playerInUse || contactsInUse
    }
}
