package dev.alenajam.monsterdialer.packs.data

data class MonsterPack(
    val id: String,
    val name: String,
    val version: String,
    val creator: String?,
    val license: String,
    val enabled: Boolean
)
