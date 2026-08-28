package dev.alenajam.monsterdialer.characters.data

import dev.alenajam.monsterdialer.packs.data.CharacterReference
import dev.alenajam.monsterdialer.packs.data.CharacterType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

interface CharacterAssignmentRepository {
    suspend fun getAssignedCharacter(contactKey: String, type: CharacterType): CharacterReference?
    suspend fun assignCharacter(
        contactKey: String,
        type: CharacterType,
        reference: CharacterReference?,
        label: String?
    )
    suspend fun getPlayerCharacter(type: CharacterType): CharacterReference?
    suspend fun setPlayerCharacter(type: CharacterType, reference: CharacterReference?)
}

@Singleton
class CharacterAssignmentRepositoryImpl @Inject constructor(
    private val assignments: CharacterAssignmentStore
) : CharacterAssignmentRepository {

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
    }

    override suspend fun getPlayerCharacter(type: CharacterType): CharacterReference? = withContext(Dispatchers.IO) {
        assignments.player(type)
    }

    override suspend fun setPlayerCharacter(
        type: CharacterType,
        reference: CharacterReference?
    ) = withContext(Dispatchers.IO) {
        assignments.setPlayer(type, reference)
    }
}
