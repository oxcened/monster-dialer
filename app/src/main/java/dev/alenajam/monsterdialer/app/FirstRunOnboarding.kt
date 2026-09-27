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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.alenajam.monsterdialer.R
import dev.alenajam.monsterdialer.app.ui.RetroActionButton
import dev.alenajam.monsterdialer.app.ui.RetroDoubleBorderTextBox

@Composable
fun FirstRunWelcomeScreen(
    onContinue: () -> Unit,
) {
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
                contentAlignment = Alignment.BottomCenter,
            ) {
                Image(
                    painter = painterResource(R.drawable.first_run_guide),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.size(212.dp),
                )
            }
            RetroDoubleBorderTextBox(
                message = stringResource(R.string.first_run_welcome_description) + "\n" +
                    stringResource(R.string.first_run_setup_description),
                modifier = Modifier.fillMaxWidth(),
                height = 104.dp,
            )
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                RetroActionButton(
                    key = stringResource(R.string.retro_key_a),
                    label = stringResource(R.string.first_run_continue),
                    onClick = onContinue,
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
