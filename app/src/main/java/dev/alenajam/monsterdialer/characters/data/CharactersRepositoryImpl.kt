package dev.alenajam.monsterdialer.characters.data

import dev.alenajam.monsterdialer.packs.data.CharacterAssignmentTarget
import dev.alenajam.monsterdialer.packs.data.CharacterPackRepository
import dev.alenajam.monsterdialer.packs.data.CharacterReference
import dev.alenajam.monsterdialer.packs.data.CharacterType
import dev.alenajam.monsterdialer.packs.data.CustomCharacterRepository
import dev.alenajam.monsterdialer.packs.data.InstalledPackCharacter
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CharactersRepositoryImpl @Inject constructor(
    private val repository: CharacterPackRepository,
    private val customRepository: CustomCharacterRepository,
    private val assignmentRepository: CharacterAssignmentRepository
) : CharactersRepository {

    override fun observeCharactersAssignableTo(
        role: CharacterAssignmentTarget,
        type: CharacterType?
    ): Flow<List<InstalledPackCharacter>> {
        return repository.observeAssignableCharacters(role, type)
    }

    override fun getCharactersAssignableTo(
        role: CharacterAssignmentTarget,
        type: CharacterType?
    ): List<InstalledPackCharacter> {
        return repository.charactersAssignableTo(role, type)
    }

    override fun findCharacter(
        reference: CharacterReference,
        role: CharacterAssignmentTarget,
        type: CharacterType
    ): InstalledPackCharacter? {
        return repository.find(reference, role, type)
    }

    override fun getCharactersInPack(
        packId: String,
        packName: String
    ): List<InstalledPackCharacter> {
        return repository.charactersInPack(packId, packName)
    }

    override suspend fun deleteCustomCharacter(characterId: String) {
        val reference = CharacterReference(CustomCharacterRepository.CUSTOM_PACK_ID, characterId)
        assignmentRepository.clearAssignmentsForCharacter(reference)
        customRepository.deleteCharacter(characterId)
    }

    override suspend fun isPackInUse(packId: String): Boolean {
        return assignmentRepository.isPackInUse(packId)
    }

    override suspend fun isCharacterInUse(reference: CharacterReference): Boolean {
        return assignmentRepository.isCharacterAssignedToPlayer(reference) ||
            assignmentRepository.isCharacterAssignedToAnyContact(reference)
    }
}
