package dev.alenajam.monsterdialer.app

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.alenajam.monsterdialer.R
import dev.alenajam.monsterdialer.app.ui.RetroActionButton
import dev.alenajam.monsterdialer.app.ui.RetroManualDialogueBox
import dev.alenajam.monsterdialer.battle.data.BattleEncounterFactory
import dev.alenajam.monsterdialer.battle.data.BattleTiming
import dev.alenajam.monsterdialer.battle.data.EncounterType
import dev.alenajam.monsterdialer.battle.ui.BattleScreen

@Composable
fun FirstRunWelcomeScreen(
    onContinue: () -> Unit,
) {
    var dialogueStep by rememberSaveable { mutableIntStateOf(0) }
    val dialogueMessages = listOf(
        stringResource(R.string.first_run_greeting),
        stringResource(R.string.first_run_welcome_description),
        stringResource(R.string.first_run_guide_name),
        stringResource(R.string.first_run_guide_title),
        stringResource(R.string.first_run_call_encounter),
        stringResource(R.string.first_run_call_answer),
        stringResource(R.string.first_run_phone_app_description),
        stringResource(R.string.first_run_setup_description),
    )
    val currentStep = dialogueStep.coerceIn(dialogueMessages.indices)
    val dialogueMessage = dialogueMessages[currentStep]
    val isFinalStep = currentStep == dialogueMessages.lastIndex
    val showBattlePreview = currentStep == 4 || currentStep == 5

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    if (showBattlePreview) {
                        BattleScreen(
                            encounter = BattleEncounterFactory.preview(EncounterType.Trainer),
                            timing = BattleTiming.Instant,
                            staticPreview = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(260.dp),
                        )
                    } else {
                        Image(
                            painter = painterResource(R.drawable.first_run_guide),
                            contentDescription = null,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.size(160.dp),
                        )
                    }
                    RetroManualDialogueBox(
                        message = dialogueMessage,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                RetroActionButton(
                    key = stringResource(R.string.retro_key_a),
                    label = stringResource(
                        if (isFinalStep) R.string.first_run_continue else R.string.first_run_next,
                    ),
                    onClick = {
                        if (isFinalStep) onContinue() else dialogueStep = currentStep + 1
                    },
                )
            }
        }
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
