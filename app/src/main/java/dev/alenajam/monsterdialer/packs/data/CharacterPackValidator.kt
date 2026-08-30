package dev.alenajam.monsterdialer.packs.data

class CharacterPackValidationException(message: String) : IllegalArgumentException(message)

object CharacterPackValidator {
    const val SupportedFormatVersion = 1
    const val ManifestPath = "manifest.json"
    const val MaxCharacters = 200
    const val MaxLevel = 999
    const val MaxHp = 999

    private val idPattern = Regex("[a-z0-9][a-z0-9._-]{1,63}")
    private val mediaExtensions = setOf("png", "webp", "jpg", "jpeg")
    private val audioExtensions = setOf("ogg")

    fun validate(manifest: CharacterPackManifest): ValidatedCharacterPack {
        requireThat(
            manifest.formatVersion == SupportedFormatVersion,
            "Unsupported pack format version: pack uses ${manifest.formatVersion}, but this app supports $SupportedFormatVersion"
        )
        requireThat(idPattern.matches(manifest.id), "Pack id must be 2–64 lowercase letters, digits, dots, dashes, or underscores")
        requireText(manifest.name, "Pack name")
        requireText(manifest.version, "Pack version")
        requireText(manifest.license, "Pack license")
        requireThat(manifest.version.length <= 64, "Pack version is too long")
        requireThat(manifest.characters.isNotEmpty(), "Pack must contain at least one character")
        requireThat(manifest.characters.size <= MaxCharacters, "Pack contains too many characters")

        val characterIds = mutableSetOf<String>()
        val files = linkedSetOf(ManifestPath)
        manifest.characters.forEach { character ->
            requireThat(idPattern.matches(character.id), "Character id '${character.id}' is invalid")
            requireThat(characterIds.add(character.id), "Character ids must be unique")
            requireText(character.name, "Character name")
            requireThat(character.assignableTo.isNotEmpty(), "Character assignableTo must not be empty")
            requireThat(character.assignableTo.distinct().size == character.assignableTo.size, "Character assignableTo contains duplicates")
            requireThat(
                !character.isRadiant || character.type == CharacterType.Monster,
                "Only monster character '${character.id}' may be radiant"
            )
            requireThat(
                CharacterAssignmentTarget.Contact !in character.assignableTo || character.frontImage != null,
                "Contact-assignable character '${character.id}' must provide frontImage"
            )
            requireThat(
                CharacterAssignmentTarget.Player !in character.assignableTo || character.backImage != null,
                "Player-assignable character '${character.id}' must provide backImage"
            )
            character.level?.let {
                requireThat(it in 1..MaxLevel, "Character '${character.id}' level must be between 1 and $MaxLevel")
            }
            character.maxHp?.let {
                requireThat(it in 1..MaxHp, "Character '${character.id}' maxHp must be between 1 and $MaxHp")
            }
            character.frontImage?.let { files += validatePath(it, mediaExtensions, "frontImage") }
            character.backImage?.let { files += validatePath(it, mediaExtensions, "backImage") }
            character.callSound?.let { files += validatePath(it, audioExtensions, "callSound") }
        }
        return ValidatedCharacterPack(manifest, files)
    }

    /** Validates a relative ZIP entry path before it is ever resolved onto disk. */
    fun validateArchivePath(path: String): String {
        requireThat(path.isNotBlank(), "Archive contains an empty path")
        requireThat(!path.startsWith('/') && !path.startsWith('\\'), "Archive path must be relative")
        requireThat(!path.contains('\\'), "Archive paths must use forward slashes")
        val segments = path.split('/')
        requireThat(segments.none { it.isEmpty() || it == "." || it == ".." }, "Archive path is unsafe")
        return path
    }

    private fun validatePath(path: String, allowedExtensions: Set<String>, field: String): String {
        val validated = validateArchivePath(path)
        val extension = validated.substringAfterLast('.', missingDelimiterValue = "").lowercase()
        requireThat(extension in allowedExtensions, "$field has an unsupported file type")
        return validated
    }

    private fun requireText(value: String, field: String) {
        requireThat(value.isNotBlank() && value.length <= 120, "$field must be between 1 and 120 characters")
    }

    private fun requireThat(condition: Boolean, message: String) {
        if (!condition) throw CharacterPackValidationException(message)
    }
}
