package dev.alenajam.monsterdialer.characters.ui

import androidx.activity.compose.BackHandler
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import dev.alenajam.monsterdialer.R
import dev.alenajam.monsterdialer.packs.data.CharacterReference
import dev.alenajam.monsterdialer.packs.data.CharacterType
import dev.alenajam.opendialer.feature.settings.LocalSettingsBackInterceptor
import dev.alenajam.opendialer.feature.settings.LocalSettingsSubpageNavigator

@Composable
internal fun RandomPoolEditorBackHandling(
    hasUnsavedEmptyPool: Boolean,
    onDiscard: () -> Unit,
) {
    val navigator = LocalSettingsSubpageNavigator.current
    val backInterceptor = LocalSettingsBackInterceptor.current
    var showWarning by remember { mutableStateOf(false) }
    val requestWarning = { showWarning = true }

    BackHandler(enabled = hasUnsavedEmptyPool, onBack = requestWarning)
    SideEffect {
        backInterceptor?.onNavigateBack = if (hasUnsavedEmptyPool) {
            { requestWarning(); true }
        } else null
    }
    DisposableEffect(backInterceptor) { onDispose { backInterceptor?.onNavigateBack = null } }

    if (showWarning) {
        AlertDialog(
            onDismissRequest = { showWarning = false },
            title = { Text(stringResource(R.string.contact_random_pool_empty_title)) },
            text = { Text(stringResource(R.string.contact_random_pool_empty_exit_message)) },
            confirmButton = {
                TextButton(onClick = { showWarning = false }) {
                    Text(stringResource(R.string.contact_random_pool_continue_editing))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    onDiscard()
                    showWarning = false
                    navigator?.navigateBack()
                }) {
                    Text(stringResource(R.string.contact_random_pool_discard_changes))
                }
            },
        )
    }
}

internal fun updateRandomPoolDraft(
    type: CharacterType,
    pool: Set<CharacterReference>,
    drafts: MutableMap<CharacterType, Set<CharacterReference>>,
    onValidPoolChanged: (CharacterType, Set<CharacterReference>) -> Unit,
) {
    drafts[type] = pool
    if (pool.isNotEmpty()) onValidPoolChanged(type, pool)
}
