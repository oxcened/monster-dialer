package dev.alenajam.monsterdialer.ui.battle

import androidx.compose.runtime.Composable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Preview(name = "Trainer battle", showBackground = true, widthDp = 400, heightDp = 600)
@Composable
private fun TrainerBattlePreview() {
    val encounter = BattleEncounterFactory.preview(EncounterType.Trainer)
    BattleScene(
        BattleUiState(
            runId = 1,
            phase = BattlePhase.EnemyReady,
            encounter = encounter,
            message = "Trainer Alex sent out SHELKURL!",
            enemyPanel = BattlePanel.Monster,
            enemyRevealFrame = 4
        )
    )
}

@Preview(name = "Radiant encounter", showBackground = true, widthDp = 400, heightDp = 600)
@Composable
private fun RadiantBattlePreview() {
    val encounter = BattleEncounterFactory.preview(EncounterType.RadiantWild)
    BattleScene(
        BattleUiState(
            runId = 1,
            phase = BattlePhase.IntroMessage,
            encounter = encounter,
            message = "Wild radiant SHELKURL appeared!",
            playerPanel = BattlePanel.Roster,
            showEnemyRadiance = true
        )
    )
}

@Preview(name = "Unknown encounter", showBackground = true, widthDp = 400, heightDp = 600)
@Composable
private fun UnknownBattlePreview() {
    val encounter = BattleEncounterFactory.preview(EncounterType.Anonymous)
    BattleScene(
        BattleUiState(
            runId = 1,
            phase = BattlePhase.Ready,
            encounter = encounter,
            message = "What will you do?",
            playerPanel = BattlePanel.Monster,
            enemyPanel = BattlePanel.Monster,
            playerRevealFrame = 4
        )
    )
}

@Preview(name = "Enemy status panel", showBackground = true, backgroundColor = 0xFFF8F8F0)
@Composable
private fun EnemyStatusPanelPreview() {
    BattlePanelView(
        monster = BattleEncounterFactory.preview(EncounterType.Trainer).enemy,
        panel = BattlePanel.Monster,
        isEnemy = true,
        modifier = Modifier.padding(16.dp)
    )
}

@Preview(name = "Player status panel", showBackground = true, backgroundColor = 0xFFF8F8F0)
@Composable
private fun PlayerStatusPanelPreview() {
    BattlePanelView(
        monster = BattleEncounterFactory.preview(EncounterType.Trainer).player,
        panel = BattlePanel.Monster,
        isEnemy = false,
        modifier = Modifier.padding(16.dp)
    )
}

@Preview(name = "Roster panels", showBackground = true, backgroundColor = 0xFFF8F8F0)
@Composable
private fun RosterPanelsPreview() {
    val encounter = BattleEncounterFactory.preview(EncounterType.Trainer)
    Column(
        modifier = Modifier
            .background(Color(0xFFF8F8F0))
            .padding(16.dp)
    ) {
        BattlePanelView(
            monster = encounter.enemy,
            panel = BattlePanel.Roster,
            isEnemy = true
        )
        BattlePanelView(
            monster = encounter.player,
            panel = BattlePanel.Roster,
            isEnemy = false,
            modifier = Modifier.padding(top = 16.dp)
        )
    }
}
