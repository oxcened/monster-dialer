package dev.alenajam.monsterdialer.characters.data

import dev.alenajam.monsterdialer.packs.data.CharacterReference

/** A persisted roster entry for the bundled monster, which is not provided by a character pack. */
val BuiltInMonsterRosterReference = BuiltInCharacters.defaultMonsterReference

private val LegacyBuiltInMonsterRosterReference = CharacterReference(
    packId = "builtin",
    characterId = "monster",
)

fun CharacterReference.isBuiltInMonsterRosterReference(): Boolean =
    this == BuiltInMonsterRosterReference || this == LegacyBuiltInMonsterRosterReference

fun CharacterReference.normalizedBuiltInMonsterRosterReference(): CharacterReference =
    if (isBuiltInMonsterRosterReference()) BuiltInMonsterRosterReference else this
