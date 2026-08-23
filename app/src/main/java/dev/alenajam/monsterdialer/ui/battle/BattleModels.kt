package dev.alenajam.monsterdialer.ui.battle

import androidx.annotation.DrawableRes

enum class BattlePhase {
    Idle,
    TrainersEntering,
    TrainersColorizing,
    IntroMessage,
    EnemyTrainerLeaving,
    EnemyRevealing,
    EnemyReady,
    PlayerTrainerLeaving,
    PlayerRevealing,
    Ready
}

enum class EncounterType { Trainer, ShinyWild, Anonymous }

enum class BattlePanel { Hidden, Pokeballs, Pokemon }

data class BattlePokemon(
    val name: String,
    val level: Int,
    val hp: Int,
    val maxHp: Int,
    @param:DrawableRes val frontSprite: Int,
    @param:DrawableRes val backSprite: Int,
    val frontSpriteName: String? = null,
    val backSpriteName: String? = null,
    val isShiny: Boolean = false
)

data class BattleEncounter(
    val id: String,
    val type: EncounterType,
    val player: BattlePokemon,
    val enemy: BattlePokemon?,
    val enemyTrainerName: String?,
    @param:DrawableRes val playerTrainerSprite: Int,
    @param:DrawableRes val enemyTrainerSprite: Int,
    val playerTrainerSpriteName: String? = null,
    val enemyTrainerSpriteName: String? = null
)

data class BattleUiState(
    val runId: Long = 0,
    val phase: BattlePhase = BattlePhase.Idle,
    val encounter: BattleEncounter? = null,
    val message: String = "",
    val isTyping: Boolean = false,
    val playerPanel: BattlePanel = BattlePanel.Hidden,
    val enemyPanel: BattlePanel = BattlePanel.Hidden,
    val enemyRevealFrame: Int = 0,
    val playerRevealFrame: Int = 0,
    val showEnemySparkles: Boolean = false,
    val showPlayerSparkles: Boolean = false
)

data class BattleTiming(
    val trainerEnterMillis: Int = 1_400,
    val trainerExitMillis: Int = 500,
    val colorizeMillis: Int = 450,
    val characterMillis: Long = 8,
    val introHoldMillis: Long = 1_500,
    val panelHoldMillis: Long = 700,
    val revealFrameMillis: Long = 70,
    val sparkleMillis: Long = 480,
    val readyHoldMillis: Long = 700
) {
    companion object {
        val Instant = BattleTiming(
            trainerEnterMillis = 0,
            trainerExitMillis = 0,
            colorizeMillis = 0,
            characterMillis = 0,
            introHoldMillis = 0,
            panelHoldMillis = 0,
            revealFrameMillis = 0,
            sparkleMillis = 0,
            readyHoldMillis = 0
        )
    }
}
