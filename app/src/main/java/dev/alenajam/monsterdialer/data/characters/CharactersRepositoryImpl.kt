package dev.alenajam.monsterdialer.data.characters

import dev.alenajam.monsterdialer.packs.CharacterAssignmentTarget
import dev.alenajam.monsterdialer.packs.CharacterPackRepository
import dev.alenajam.monsterdialer.packs.CharacterReference
import dev.alenajam.monsterdialer.packs.CharacterType
import dev.alenajam.monsterdialer.packs.InstalledPackCharacter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CharactersRepositoryImpl @Inject constructor(
    private val repository: CharacterPackRepository
) : CharactersRepository {

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
}
