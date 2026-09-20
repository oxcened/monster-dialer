package dev.alenajam.monsterdialer.app

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import dev.alenajam.monsterdialer.R
import dev.alenajam.monsterdialer.app.ui.RetroActionButton
import dev.alenajam.monsterdialer.app.ui.RetroContextMenuItem
import dev.alenajam.monsterdialer.app.ui.RetroContextMenuOverlay
import dev.alenajam.monsterdialer.app.ui.RetroHomeTabs
import dev.alenajam.monsterdialer.app.ui.MonsterHomeTab
import dev.alenajam.monsterdialer.app.ui.RetroSearchBar
import dev.alenajam.monsterdialer.app.ui.RetroScreenFooterVerticalPadding
import dev.alenajam.monsterdialer.app.ui.RetroScreenHorizontalPadding
import dev.alenajam.monsterdialer.calls.ui.RetroCallsScreen
import dev.alenajam.monsterdialer.characters.ui.CharacterSettingsPage
import dev.alenajam.monsterdialer.characters.ui.CharacterSettingsSummaryViewModel
import dev.alenajam.monsterdialer.characters.ui.CharactersHomeScreen
import dev.alenajam.monsterdialer.characters.ui.ContactCharacterSettingsViewModel
import dev.alenajam.monsterdialer.characters.ui.PlayerProfile
import dev.alenajam.monsterdialer.characters.ui.ProfileMetrics
import dev.alenajam.monsterdialer.characters.ui.CharacterSharingViewModel
import dev.alenajam.monsterdialer.contacts.ui.RetroContactsScreen
import dev.alenajam.opendialer.feature.appShell.HomeScreenCallbacks

@Composable
internal fun MonsterHomeScreen(
    callbacks: HomeScreenCallbacks,
    contactCharacterSettingsViewModel: ContactCharacterSettingsViewModel,
    characterSharingViewModel: CharacterSharingViewModel,
    playerProfile: PlayerProfile,
    profileMetrics: ProfileMetrics,
    characterSettingsSummaryViewModel: CharacterSettingsSummaryViewModel,
) {
    var currentTab by rememberSaveable { mutableStateOf(MonsterHomeTab.CALLS) }
    var searchActive by rememberSaveable { mutableStateOf(false) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var menuOpen by remember { mutableStateOf(false) }
    var profileBackAction by remember { mutableStateOf<(() -> Unit)?>(null) }

    BackHandler(enabled = searchActive || profileBackAction != null) {
        when {
            searchActive -> {
                searchActive = false
                searchQuery = ""
            }
            else -> profileBackAction?.invoke()
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        RetroHomeTabs(
            currentTab = currentTab,
            onFavorites = { currentTab = MonsterHomeTab.FAVORITES },
            onCalls = { currentTab = MonsterHomeTab.CALLS },
            onContacts = { currentTab = MonsterHomeTab.CONTACTS },
            onProfile = { currentTab = MonsterHomeTab.PROFILE },
        )

        if (currentTab != MonsterHomeTab.PROFILE && searchActive) {
            MonsterSearchControl(
                active = searchActive,
                query = searchQuery,
                onQueryChanged = { searchQuery = it },
            )
        }

        Box(modifier = Modifier.weight(1f)) {
            when {
                searchActive -> RetroContactsScreen(
                    searchQuery = searchQuery,
                    onOpenSettingsSubpage = callbacks.onOpenSettingsSubpage,
                    characterSettingsViewModel = contactCharacterSettingsViewModel,
                )
                currentTab == MonsterHomeTab.FAVORITES -> RetroCallsScreen(
                    onOpenHistory = callbacks.onOpenHistory,
                    onOpenContacts = { currentTab = MonsterHomeTab.CONTACTS },
                    onAddFavorite = callbacks.onAddFavorite,
                    onEditNumberBeforeCall = callbacks.onOpenDialpad,
                    favoritesOnly = true,
                )
                currentTab == MonsterHomeTab.CALLS -> RetroCallsScreen(
                    onOpenHistory = callbacks.onOpenHistory,
                    onOpenContacts = { currentTab = MonsterHomeTab.CONTACTS },
                    onAddFavorite = callbacks.onAddFavorite,
                    onEditNumberBeforeCall = callbacks.onOpenDialpad,
                )
                currentTab == MonsterHomeTab.CONTACTS -> RetroContactsScreen(
                    searchQuery = searchQuery,
                    onOpenSettingsSubpage = callbacks.onOpenSettingsSubpage,
                    characterSettingsViewModel = contactCharacterSettingsViewModel,
                )
                else -> CharactersHomeScreen(
                    onOpenSettings = callbacks.onOpenSettings,
                    onOpenSubpage = { index, payload ->
                        val destination = if (index == CharacterSettingsPage.ContactCharacters.index) {
                            CharacterSettingsPage.ToolboxContactCharacters.index
                        } else {
                            index
                        }
                        callbacks.onOpenSettingsSubpage(destination, payload)
                    },
                    sharingViewModel = characterSharingViewModel,
                    playerProfile = playerProfile,
                    profileMetrics = profileMetrics,
                    onOpenAbout = callbacks.onOpenAbout,
                    onReorderRoster = characterSettingsSummaryViewModel::reorderPlayerMonsterRoster,
                    onRemoveRosterMonster = characterSettingsSummaryViewModel::removePlayerMonsterFromRoster,
                    showImportUi = false,
                    onSetBackAction = { enabled, action -> profileBackAction = action.takeIf { enabled } },
                )
            }
        }

        MonsterHomeActions(
            currentTab = currentTab,
            searchActive = searchActive,
            profileBackAction = profileBackAction,
            onSearch = { searchActive = true },
            onDial = { callbacks.onOpenDialpad("") },
            onBack = {
                if (searchActive) {
                    searchActive = false
                    searchQuery = ""
                } else {
                    profileBackAction?.invoke()
                }
            },
        )
    }

    if (menuOpen) {
        RetroContextMenuOverlay(
            modifier = Modifier.fillMaxWidth(0.78f),
            fontFamily = FontFamily(Font(R.font.ui_pixel_font)),
            onDismissRequest = { menuOpen = false },
            items = listOf(
                RetroContextMenuItem(stringResource(R.string.favorites), currentTab == MonsterHomeTab.FAVORITES) {
                    currentTab = MonsterHomeTab.FAVORITES
                    menuOpen = false
                },
                RetroContextMenuItem(stringResource(R.string.recents), currentTab == MonsterHomeTab.CALLS) {
                    currentTab = MonsterHomeTab.CALLS
                    menuOpen = false
                },
                RetroContextMenuItem(stringResource(R.string.contacts), currentTab == MonsterHomeTab.CONTACTS) {
                    currentTab = MonsterHomeTab.CONTACTS
                    menuOpen = false
                },
                RetroContextMenuItem(stringResource(R.string.characters_navigation_label), currentTab == MonsterHomeTab.PROFILE) {
                    currentTab = MonsterHomeTab.PROFILE
                    menuOpen = false
                },
                RetroContextMenuItem.cancel(stringResource(R.string.cancel)) { menuOpen = false },
            ),
        )
    }
}

@Composable
private fun MonsterSearchControl(
    active: Boolean,
    query: String,
    onQueryChanged: (String) -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    LaunchedEffect(active) {
        if (active) {
            focusRequester.requestFocus()
            keyboardController?.show()
        } else {
            keyboardController?.hide()
        }
    }
    RetroSearchBar(
        label = stringResource(R.string.contact_picker_search_name_prefix),
        query = query,
        focusRequester = focusRequester,
        onQueryChanged = onQueryChanged,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
    )
}

@Composable
private fun MonsterHomeActions(
    currentTab: MonsterHomeTab,
    searchActive: Boolean,
    profileBackAction: (() -> Unit)?,
    onSearch: () -> Unit,
    onDial: () -> Unit,
    onBack: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(
                horizontal = RetroScreenHorizontalPadding,
                vertical = RetroScreenFooterVerticalPadding,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (currentTab != MonsterHomeTab.PROFILE) {
            Box(modifier = Modifier.weight(1f)) {
                if (!searchActive) {
                    RetroActionButton(
                        key = stringResource(R.string.retro_key_a),
                        label = stringResource(R.string.contact_picker_search),
                        onClick = onSearch,
                    )
                }
            }
            RetroActionButton(
                key = stringResource(R.string.retro_key_b),
                label = stringResource(if (searchActive) R.string.customized_contacts_back_action else R.string.retro_action_dial_label),
                onClick = if (searchActive) onBack else onDial,
            )
        } else {
            Box(modifier = Modifier.weight(1f)) {
                if (profileBackAction != null) {
                    RetroActionButton(
                        key = stringResource(R.string.retro_key_a),
                        label = stringResource(R.string.customized_contacts_back_action),
                        onClick = onBack,
                    )
                }
            }
            RetroActionButton(
                key = stringResource(R.string.retro_key_b),
                label = stringResource(R.string.retro_action_dial_label),
                onClick = onDial,
            )
        }
    }
}
