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
            message = "Trainer Red sent out BULBASAUR!",
            enemyPanel = BattlePanel.Pokemon,
            enemyRevealFrame = 4
        )
    )
}

@Preview(name = "Shiny encounter", showBackground = true, widthDp = 400, heightDp = 600)
@Composable
private fun ShinyBattlePreview() {
    val encounter = BattleEncounterFactory.preview(EncounterType.ShinyWild)
    BattleScene(
        BattleUiState(
            runId = 1,
            phase = BattlePhase.IntroMessage,
            encounter = encounter,
            message = "Wild shiny BULBASAUR appeared!",
            playerPanel = BattlePanel.Pokeballs,
            showEnemySparkles = true
        )
    )
}

@Preview(name = "MissingNo encounter", showBackground = true, widthDp = 400, heightDp = 600)
@Composable
private fun MissingNoBattlePreview() {
    val encounter = BattleEncounterFactory.preview(EncounterType.Anonymous)
    BattleScene(
        BattleUiState(
            runId = 1,
            phase = BattlePhase.Ready,
            encounter = encounter,
            message = "What will you do?",
            playerPanel = BattlePanel.Pokemon,
            enemyPanel = BattlePanel.Pokemon,
            playerRevealFrame = 4
        )
    )
}

@Preview(name = "Enemy status panel", showBackground = true, backgroundColor = 0xFFF8F8F0)
@Composable
private fun EnemyStatusPanelPreview() {
    BattlePanelView(
        pokemon = BattleEncounterFactory.preview(EncounterType.Trainer).enemy,
        panel = BattlePanel.Pokemon,
        isEnemy = true,
        modifier = Modifier.padding(16.dp)
    )
}

@Preview(name = "Player status panel", showBackground = true, backgroundColor = 0xFFF8F8F0)
@Composable
private fun PlayerStatusPanelPreview() {
    BattlePanelView(
        pokemon = BattleEncounterFactory.preview(EncounterType.Trainer).player,
        panel = BattlePanel.Pokemon,
        isEnemy = false,
        modifier = Modifier.padding(16.dp)
    )
}

@Preview(name = "Pokéball panels", showBackground = true, backgroundColor = 0xFFF8F8F0)
@Composable
private fun PokeballPanelsPreview() {
    val encounter = BattleEncounterFactory.preview(EncounterType.Trainer)
    Column(
        modifier = Modifier
            .background(Color(0xFFF8F8F0))
            .padding(16.dp)
    ) {
        BattlePanelView(
            pokemon = encounter.enemy,
            panel = BattlePanel.Pokeballs,
            isEnemy = true
        )
        BattlePanelView(
            pokemon = encounter.player,
            panel = BattlePanel.Pokeballs,
            isEnemy = false,
            modifier = Modifier.padding(top = 16.dp)
        )
    }
}
