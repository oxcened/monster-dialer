package dev.alenajam.monsterdialer.onlineprofiles.data

import dev.alenajam.monsterdialer.battle.data.BattleMonster
import dev.alenajam.monsterdialer.battle.data.BattleVisualAsset
import kotlinx.serialization.Serializable

@Serializable
data class CachedOnlineProfile(
    val schemaVersion: Int = 1,
    val publicProfileId: String,
    val revision: Long,
    val trainer: SharedTrainer,
    val monster: SharedMonster,
)

@Serializable
data class SharedTrainer(val name: String, val frontSprite: SharedSprite)

@Serializable
data class SharedMonster(
    val name: String,
    val level: Int,
    val maxHp: Int,
    val frontSprite: SharedSprite,
)

@Serializable
data class SharedSprite(
    val storagePath: String,
    val sha256: String,
    val width: Int,
    val height: Int,
    val byteSize: Long,
)

data class RemoteBattleOpponent(
    val profileId: String,
    val trainerName: String,
    val trainerSprite: BattleVisualAsset.LocalFile,
    val monster: BattleMonster,
)
