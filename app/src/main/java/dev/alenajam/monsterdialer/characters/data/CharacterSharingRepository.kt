package dev.alenajam.monsterdialer.characters.data

import dev.alenajam.monsterdialer.packs.data.CustomCharacterRepository
import java.io.InputStream
import java.io.OutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CharacterSharingRepository @Inject constructor(
    private val customCharacters: CustomCharacterRepository
) {
    fun export(characterId: String, creator: String, license: String, output: OutputStream) {
        val exported = requireNotNull(customCharacters.getExportableCharacter(characterId))
        val character = exported.character
        val frontName = exported.frontImageFile?.name
        val backName = exported.backImageFile?.name
        val radiantFrontName = exported.radiantFrontImageFile?.name
        val radiantBackName = exported.radiantBackImageFile?.name
        SharedCharacterArchive.write(
            SharedCharacter(
                name = character.name,
                creator = creator.trim(),
                license = license.trim(),
                type = character.type,
                assignableTo = character.assignableTo,
                frontImage = frontName,
                backImage = backName,
                radiantFrontImage = radiantFrontName,
                radiantBackImage = radiantBackName,
                isRadiant = character.isRadiant,
                level = character.level,
                maxHp = character.maxHp
            ),
            buildMap {
                exported.frontImageFile?.let { put(requireNotNull(frontName), it) }
                exported.backImageFile?.let { put(requireNotNull(backName), it) }
                exported.radiantFrontImageFile?.let { put(requireNotNull(radiantFrontName), it) }
                exported.radiantBackImageFile?.let { put(requireNotNull(radiantBackName), it) }
            },
            output
        )
    }

    fun preview(input: InputStream): SharedCharacterImport = SharedCharacterArchive.read(input)

    suspend fun import(shared: SharedCharacterImport) = customCharacters.importSharedCharacter(shared)
}
