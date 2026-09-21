package dev.alenajam.monsterdialer.app

import androidx.activity.compose.BackHandler
import android.content.ActivityNotFoundException
import android.content.Context
import android.provider.BlockedNumberContract
import android.telecom.TelecomManager
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBarsIgnoringVisibility
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.alenajam.monsterdialer.R
import dev.alenajam.monsterdialer.app.ui.RetroFastScroller
import dev.alenajam.monsterdialer.app.ui.RetroFooter
import dev.alenajam.monsterdialer.app.ui.RetroSelectionArrow
import dev.alenajam.monsterdialer.app.ui.RetroSelectionArrowSize
import dev.alenajam.monsterdialer.calls.ui.ContactArtworkPreferencesViewModel
import dev.alenajam.monsterdialer.characters.data.ContactArtworkPriority
import dev.alenajam.monsterdialer.characters.ui.CharacterSettingsPage
import dev.alenajam.opendialer.core.common.SharedPreferenceHelper
import dev.alenajam.opendialer.feature.appShell.SettingsScreenCallbacks
import dev.alenajam.opendialer.feature.settings.R as SettingsR

private val SettingsPaper = Color(0xFFF9F7FC)
private val SettingsInk = Color(0xFF202020)
private val SettingsFont = FontFamily(Font(R.font.pixel_operator))

@Composable
@OptIn(ExperimentalLayoutApi::class)
internal fun MonsterSettingsScreen(
    callbacks: SettingsScreenCallbacks,
    artworkPreferencesViewModel: ContactArtworkPreferencesViewModel = hiltViewModel(),
) {
    val artworkPriority by artworkPreferencesViewModel.priority.collectAsStateWithLifecycle()
    var cursor by rememberSaveable { mutableIntStateOf(1) }
    val listState = rememberLazyListState()
    val context = androidx.compose.ui.platform.LocalContext.current
    var updateChecksEnabled by rememberSaveable {
        mutableIntStateOf(if (SharedPreferenceHelper.isUpdateCheckEnabled(context)) 1 else 0)
    }
    val entries = buildList {
        add(MonsterSettingsEntry.Section(stringResource(R.string.settings_section_general)))
        add(
        MonsterSettingsEntry.Action(
            title = stringResource(SettingsR.string.display_options),
            description = stringResource(SettingsR.string.display_options_description, stringResource(R.string.app_name)),
            onClick = callbacks.onOpenDisplayOptions,
        )
        )
        add(
        MonsterSettingsEntry.Action(
            title = stringResource(SettingsR.string.customize_quick_responses),
            description = stringResource(SettingsR.string.customize_quick_responses_description),
            onClick = callbacks.onOpenQuickResponses,
        )
        )
        if (BlockedNumberContract.canCurrentUserBlockNumbers(context)) {
            add(
                MonsterSettingsEntry.Action(
                    title = stringResource(SettingsR.string.manageBlockedNumbers),
                    description = stringResource(SettingsR.string.manage_blocked_numbers_description),
                    onClick = {
                        try {
                            val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager
                            val intent = telecomManager?.createManageBlockedNumbersIntent()
                                ?: throw ActivityNotFoundException()
                            context.startActivity(intent)
                        } catch (_: ActivityNotFoundException) {
                            Toast.makeText(
                                context,
                                SettingsR.string.manage_blocked_numbers_unavailable,
                                Toast.LENGTH_SHORT,
                            ).show()
                        }
                    },
                ),
            )
        }
        add(
            MonsterSettingsEntry.Toggle(
                title = stringResource(SettingsR.string.check_for_updates),
                description = stringResource(SettingsR.string.check_for_updates_description),
                enabled = updateChecksEnabled == 1,
                onToggle = { enabled ->
                    updateChecksEnabled = if (enabled) 1 else 0
                    SharedPreferenceHelper.setUpdateCheckEnabled(context, enabled)
                },
            ),
        )
        add(MonsterSettingsEntry.Section(stringResource(R.string.settings_section_characters)))
        add(
        MonsterSettingsEntry.Page(
            title = stringResource(R.string.contact_defaults_toolbox_title),
            description = stringResource(R.string.contact_defaults_description),
            pageId = CharacterSettingsPage.ContactDefaults.id,
        )
        )
        add(
        MonsterSettingsEntry.Toggle(
            title = stringResource(R.string.prefer_monster_artwork),
            description = stringResource(R.string.prefer_monster_artwork_description),
            enabled = artworkPriority == ContactArtworkPriority.MONSTER,
            onToggle = { enabled ->
                artworkPreferencesViewModel.setPriority(
                    if (enabled) ContactArtworkPriority.MONSTER else ContactArtworkPriority.TRAINER,
                )
            },
        )
        )
        add(
        MonsterSettingsEntry.Page(
            title = stringResource(R.string.settings_catalogs_title),
            description = stringResource(R.string.settings_catalogs_description),
            pageId = "catalogs",
        )
        )
        add(
        MonsterSettingsEntry.Page(
            title = stringResource(R.string.local_backup_title),
            description = stringResource(R.string.local_backup_settings_description),
            pageId = CharacterSettingsPage.LocalBackup.id,
        )
        )
    }

    BackHandler(onBack = callbacks.onNavigateBack)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SettingsPaper)
            .windowInsetsPadding(WindowInsets.statusBarsIgnoringVisibility),
    ) {
        androidx.compose.foundation.layout.Box(modifier = Modifier.weight(1f)) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 8.dp),
            ) {
                itemsIndexed(entries) { index, entry ->
                    when (entry) {
                        is MonsterSettingsEntry.Section -> SettingsSectionLabel(entry.title)
                        else -> RetroSettingsRow(
                            entry = entry,
                            selected = cursor == index,
                            onClick = {
                                cursor = index
                                when (entry) {
                                    is MonsterSettingsEntry.Action -> entry.onClick()
                                    is MonsterSettingsEntry.Page -> callbacks.onOpenSubpage(entry.pageId, null)
                                    is MonsterSettingsEntry.Toggle -> entry.onToggle(!entry.enabled)
                                    is MonsterSettingsEntry.Section -> Unit
                                }
                            },
                        )
                    }
                }
            }
            RetroFastScroller(
                listState = listState,
                contentDescription = stringResource(R.string.monster_settings_title),
                modifier = Modifier.align(Alignment.CenterEnd).padding(vertical = 8.dp),
            )
        }
        RetroFooter(
            onBack = callbacks.onNavigateBack,
            backKey = stringResource(R.string.retro_key_b),
            backLabel = stringResource(R.string.back),
            modifier = Modifier.navigationBarsPadding().padding(horizontal = 2.dp),
        )
    }
}

@Composable
private fun SettingsSectionLabel(label: String) {
    androidx.compose.material3.Text(
        text = label.uppercase(),
        fontFamily = SettingsFont,
        fontSize = 16.sp,
        color = SettingsInk,
        modifier = Modifier.padding(start = 8.dp, top = 10.dp, end = 8.dp, bottom = 4.dp),
    )
}

@Composable
private fun RetroSettingsRow(
    entry: MonsterSettingsEntry,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 8.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (selected) {
            RetroSelectionArrow(tint = SettingsInk, size = 14.dp)
            Spacer(Modifier.size(2.dp))
        } else {
            Spacer(Modifier.size(RetroSelectionArrowSize))
        }
        Column(modifier = Modifier.weight(1f).padding(horizontal = 10.dp)) {
            androidx.compose.material3.Text(
                text = entry.title,
                fontFamily = SettingsFont,
                fontSize = 20.sp,
                color = SettingsInk,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            androidx.compose.material3.Text(
                text = entry.description,
                fontFamily = SettingsFont,
                fontSize = 16.sp,
                color = SettingsInk.copy(alpha = 0.75f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (entry is MonsterSettingsEntry.Toggle) {
            Switch(
                checked = entry.enabled,
                onCheckedChange = { entry.onToggle(it) },
            )
        }
    }
}

private sealed interface MonsterSettingsEntry {
    val title: String
    val description: String

    data class Action(
        override val title: String,
        override val description: String,
        val onClick: () -> Unit,
    ) : MonsterSettingsEntry

    data class Section(
        override val title: String,
        override val description: String = "",
    ) : MonsterSettingsEntry

    data class Page(
        override val title: String,
        override val description: String,
        val pageId: String,
    ) : MonsterSettingsEntry

    data class Toggle(
        override val title: String,
        override val description: String,
        val enabled: Boolean,
        val onToggle: (Boolean) -> Unit,
    ) : MonsterSettingsEntry
}
