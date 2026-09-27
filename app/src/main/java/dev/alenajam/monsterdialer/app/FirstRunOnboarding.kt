package dev.alenajam.monsterdialer.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.alenajam.monsterdialer.R
import dev.alenajam.monsterdialer.app.ui.RetroActionButton
import dev.alenajam.monsterdialer.app.ui.RetroDoubleBorderBox
import dev.alenajam.monsterdialer.app.ui.RetroMenuWindow
import dev.alenajam.monsterdialer.app.ui.RetroSelectableRow
import dev.alenajam.monsterdialer.battle.data.BattleEncounterFactory
import dev.alenajam.monsterdialer.battle.data.BattleTiming
import dev.alenajam.monsterdialer.battle.data.EncounterType
import dev.alenajam.monsterdialer.battle.ui.BattleScreen

@Composable
fun FirstRunWelcomeScreen(
    onContinue: () -> Unit,
) {
    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.first_run_welcome_title),
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Start,
            )
            RetroDoubleBorderBox(
                modifier = Modifier.fillMaxWidth(),
                height = 112.dp,
            ) {
                Text(
                    text = stringResource(R.string.first_run_welcome_description),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Start,
                )
            }
            BattlePreviewCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp),
            )
            RetroDoubleBorderBox(
                modifier = Modifier.fillMaxWidth(),
                height = 128.dp,
            ) {
                Text(
                    text = stringResource(R.string.first_run_setup_description),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Start,
                )
            }
            RetroMenuWindow(modifier = Modifier.fillMaxWidth()) {
                RetroSelectableRow(selected = true, onClick = onContinue) {
                    Box(Modifier.weight(1f)) {
                        RetroActionButton(
                            label = stringResource(R.string.first_run_continue),
                            onClick = onContinue,
                            fillWidth = true,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BattlePreviewCard(modifier: Modifier) {
    RetroDoubleBorderBox(modifier = modifier) {
        BattleScreen(
            encounter = BattleEncounterFactory.preview(EncounterType.Trainer),
            timing = BattleTiming.Instant,
            staticPreview = true,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Composable
fun FirstEncounterPrompt(
    onMakeFirstCall: () -> Unit,
    onDismiss: () -> Unit,
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.first_encounter_prompt_title)) },
        text = { Text(stringResource(R.string.first_encounter_prompt_description)) },
        confirmButton = {
            Button(onClick = onMakeFirstCall) {
                Text(stringResource(R.string.first_encounter_prompt_action))
            }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.first_encounter_prompt_later))
            }
        },
    )
}
