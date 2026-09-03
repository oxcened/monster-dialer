package dev.alenajam.monsterdialer.characters.data

import dev.alenajam.monsterdialer.packs.data.CharacterReference

/** A persisted roster entry for the bundled monster, which is not provided by a character pack. */
val BuiltInMonsterRosterReference = CharacterReference(
    packId = "builtin",
    characterId = "monster",
)

fun CharacterReference.isBuiltInMonsterRosterReference(): Boolean = this == BuiltInMonsterRosterReference
