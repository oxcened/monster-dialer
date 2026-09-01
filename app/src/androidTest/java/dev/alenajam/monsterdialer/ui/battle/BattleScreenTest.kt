package dev.alenajam.monsterdialer.ui.battle

import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.alenajam.monsterdialer.battle.data.BattleEncounter
import dev.alenajam.monsterdialer.battle.data.BattleMonster
import dev.alenajam.monsterdialer.battle.data.BattleTiming
import dev.alenajam.monsterdialer.battle.data.BattleVisualAsset
import dev.alenajam.monsterdialer.battle.data.EncounterType
import dev.alenajam.monsterdialer.battle.ui.BattleScreen
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BattleScreenTest {
    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun rendersTrainerBattleAndShowsInitialText() {
        val encounter = encounter(EncounterType.Trainer, enemyTrainerName = "Alex")
        // Use a slight delay for introHoldMillis to capture the initial text
        val timing = BattleTiming.Instant.copy(introHoldMillis = 2000)
        
        composeTestRule.setContent {
            BattleScreen(encounter = encounter, timing = timing)
        }

        composeTestRule.onNodeWithText("Trainer Alex wants to battle!").assertIsDisplayed()
    }

    @Test
    fun rendersAnonymousBattleAndShowsInitialText() {
        val encounter = encounter(EncounterType.Anonymous)
        val timing = BattleTiming.Instant.copy(introHoldMillis = 2000)
        
        composeTestRule.setContent {
            BattleScreen(encounter = encounter, timing = timing)
        }

        composeTestRule.onNodeWithText("Wild FERNFOX appeared!").assertIsDisplayed()
    }

    @Test
    fun progressesToReadyState() {
        val encounter = encounter(EncounterType.Trainer, enemyTrainerName = "Alex")
        
        composeTestRule.setContent {
            BattleScreen(encounter = encounter, timing = BattleTiming.Instant)
        }

        composeTestRule.onNodeWithContentDescription("Monster battle scene").assertIsDisplayed()
        composeTestRule.onNodeWithText("What will you do?").assertIsDisplayed()
        composeTestRule.onNodeWithText("MOSSLING").assertIsDisplayed()
        composeTestRule.onNodeWithText("FERNFOX").assertIsDisplayed()
    }

    @Test
    fun handlesMissingSpritesGracefully() {
        val encounter = encounter(EncounterType.Trainer, enemyTrainerName = "Alex").copy(
            player = BattleMonster(
                "Mossling", 5, 20, 20, 
                BattleVisualAsset.AppDrawable(dev.alenajam.monsterdialer.R.drawable.battle_player_monster), 
                backSprite = null
            ),
            enemy = BattleMonster(
                "Fernfox", 5, 20, 20,
                BattleVisualAsset.AppDrawable(dev.alenajam.monsterdialer.R.drawable.battle_enemy_monster),
                backSprite = null
            )
        )

        composeTestRule.setContent {
            BattleScreen(encounter = encounter, timing = BattleTiming.Instant)
        }

        composeTestRule.onNodeWithText("What will you do?").assertIsDisplayed()
    }

    @Test
    fun handlesZeroWidthBattleLayout() {
        composeTestRule.setContent {
            BattleScreen(
                encounter = encounter(EncounterType.Trainer, enemyTrainerName = "Alex"),
                timing = BattleTiming.Instant,
                modifier = Modifier.width(0.dp),
            )
        }

        composeTestRule.waitForIdle()
    }

    private fun encounter(type: EncounterType, enemyTrainerName: String? = null) = BattleEncounter(
        id = "test",
        type = type,
        player = BattleMonster("Mossling", 5, 20, 20, BattleVisualAsset.AppDrawable(dev.alenajam.monsterdialer.R.drawable.battle_player_monster)),
        enemy = BattleMonster("Fernfox", 5, 20, 20, BattleVisualAsset.AppDrawable(dev.alenajam.monsterdialer.R.drawable.battle_enemy_monster)),
        enemyTrainerName = enemyTrainerName,
        playerTrainerSprite = BattleVisualAsset.AppDrawable(dev.alenajam.monsterdialer.R.drawable.battle_player_trainer),
        enemyTrainerSprite = BattleVisualAsset.AppDrawable(dev.alenajam.monsterdialer.R.drawable.battle_enemy_trainer)
    )
}
