package dev.alenajam.monsterdialer.packs.data

import org.junit.Test

class CharacterPackValidatorTest {
    @Test
    fun validatesValidManifest() {
        val manifest = validManifest()
        CharacterPackValidator.validate(manifest)
    }

    @Test(expected = CharacterPackValidationException::class)
    fun rejectsUnsupportedFormatVersion() {
        val manifest = validManifest(formatVersion = 2)
        CharacterPackValidator.validate(manifest)
    }

    @Test(expected = CharacterPackValidationException::class)
    fun rejectsDuplicateCharacterIds() {
        val manifest = validManifest(characters = listOf(
            validCharacter(id = "c1"),
            validCharacter(id = "c1")
        ))
        CharacterPackValidator.validate(manifest)
    }

    @Test(expected = CharacterPackValidationException::class)
    fun rejectsEmptyCharacters() {
        val manifest = validManifest(characters = emptyList())
        CharacterPackValidator.validate(manifest)
    }

    @Test(expected = CharacterPackValidationException::class)
    fun rejectsContactCharacterWithoutFrontImage() {
        val manifest = validManifest(characters = listOf(
            validCharacter(assignableTo = listOf(CharacterAssignmentTarget.Contact), frontImage = null)
        ))
        CharacterPackValidator.validate(manifest)
    }

    @Test(expected = CharacterPackValidationException::class)
    fun rejectsPlayerCharacterWithoutBackImage() {
        val manifest = validManifest(characters = listOf(
            validCharacter(assignableTo = listOf(CharacterAssignmentTarget.Player), backImage = null)
        ))
        CharacterPackValidator.validate(manifest)
    }

    @Test(expected = CharacterPackValidationException::class)
    fun rejectsRadiantTrainer() {
        val manifest = validManifest(characters = listOf(
            validCharacter(type = CharacterType.Trainer, isRadiant = true)
        ))
        CharacterPackValidator.validate(manifest)
    }

    @Test(expected = CharacterPackValidationException::class)
    fun rejectsUnsafePaths() {
        val manifest = validManifest(characters = listOf(
            validCharacter(frontImage = "../outside.png")
        ))
        CharacterPackValidator.validate(manifest)
    }

    @Test(expected = CharacterPackValidationException::class)
    fun rejectsUnsupportedExtensions() {
        val manifest = validManifest(characters = listOf(
            validCharacter(frontImage = "image.exe")
        ))
        CharacterPackValidator.validate(manifest)
    }

    @Test(expected = CharacterPackValidationException::class)
    fun rejectsInvalidLevel() {
        val manifest = validManifest(characters = listOf(
            validCharacter(level = 0)
        ))
        CharacterPackValidator.validate(manifest)
    }

    @Test(expected = CharacterPackValidationException::class)
    fun rejectsInvalidMaxHp() {
        val manifest = validManifest(characters = listOf(
            validCharacter(maxHp = 1000)
        ))
        CharacterPackValidator.validate(manifest)
    }

    private fun validManifest(
        formatVersion: Int = 1,
        characters: List<PackCharacter> = listOf(validCharacter())
    ) = CharacterPackManifest(
        formatVersion = formatVersion,
        id = "com.example.test",
        name = "Test Pack",
        version = "1.0.0",
        license = "MIT",
        characters = characters
    )

    private fun validCharacter(
        id: String = "c1",
        type: CharacterType = CharacterType.Monster,
        assignableTo: List<CharacterAssignmentTarget> = listOf(CharacterAssignmentTarget.Contact, CharacterAssignmentTarget.Player),
        frontImage: String? = "front.png",
        backImage: String? = "back.png",
        isRadiant: Boolean = false,
        level: Int? = null,
        maxHp: Int? = null
    ) = PackCharacter(
        id = id,
        name = "Character",
        type = type,
        assignableTo = assignableTo,
        frontImage = frontImage,
        backImage = backImage,
        isRadiant = isRadiant,
        level = level,
        maxHp = maxHp
    )
}
