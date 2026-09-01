package dev.alenajam.monsterdialer.battle.data

import androidx.annotation.DrawableRes

sealed interface BattleVisualAsset {
    data class AppDrawable(
        @param:DrawableRes val resource: Int,
        val fallbackName: String? = null
    ) : BattleVisualAsset

    /** A density-independent illustration rendered directly by Compose. */
    data class VectorDrawable(@param:DrawableRes val resource: Int) : BattleVisualAsset

    /** A file extracted by [dev.alenajam.monsterdialer.packs.data.CharacterPackInstaller]. */
    data class LocalFile(val path: String) : BattleVisualAsset
}

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

enum class EncounterType { Trainer, RadiantWild, Anonymous }

enum class BattlePanel { Hidden, Roster, Monster }

data class BattleMonster(
    val name: String,
    val level: Int,
    val hp: Int,
    val maxHp: Int,
    val frontSprite: BattleVisualAsset,
    /** Only player-controlled monsters need a rear sprite. */
    val backSprite: BattleVisualAsset? = null,
    val isRadiant: Boolean = false
)

data class BattleEncounter(
    val id: String,
    val type: EncounterType,
    val player: BattleMonster,
    val enemy: BattleMonster?,
    val enemyTrainerName: String?,
    val playerTrainerSprite: BattleVisualAsset,
    val enemyTrainerSprite: BattleVisualAsset,
    /** Present only when this encounter discovers a radiant variant for the first time. */
    val unlockedRadiantName: String? = null,
    val unlockedRadiantFrontSpritePath: String? = null,
)

data class BattleUiState(
    val runId: Long = 0,
    val phase: BattlePhase = BattlePhase.Idle,
    val encounter: BattleEncounter? = null,
    val message: String = "",
    val dialogueId: Long = 0,
    val isTyping: Boolean = false,
    val playerPanel: BattlePanel = BattlePanel.Hidden,
    val enemyPanel: BattlePanel = BattlePanel.Hidden,
    val enemyRevealFrame: Int = 0,
    val playerRevealFrame: Int = 0,
    val showEnemyRadiance: Boolean = false,
    val showPlayerRadiance: Boolean = false
)

data class BattleTiming(
    val trainerEnterMillis: Int = 1_400,
    val trainerExitMillis: Int = 500,
    val colorizeMillis: Int = 450,
    val characterMillis: Long = 8,
    val dialoguePageHoldMillis: Long = 900,
    val introHoldMillis: Long = 1_500,
    val panelHoldMillis: Long = 700,
    val revealFrameMillis: Long = 70,
    val radianceMillis: Long = 480,
    val readyHoldMillis: Long = 700
) {
    companion object {
        val Instant = BattleTiming(
            trainerEnterMillis = 0,
            trainerExitMillis = 0,
            colorizeMillis = 0,
            characterMillis = 0,
            dialoguePageHoldMillis = 0,
            introHoldMillis = 0,
            panelHoldMillis = 0,
            revealFrameMillis = 0,
            radianceMillis = 0,
            readyHoldMillis = 0
        )
    }
}
