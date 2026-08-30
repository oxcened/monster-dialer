package dev.alenajam.monsterdialer.app

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import dev.alenajam.monsterdialer.R
import dev.alenajam.monsterdialer.app.ui.LocalMonsterAppIcons
import dev.alenajam.monsterdialer.app.ui.rememberMonsterIcons
import dev.alenajam.monsterdialer.app.ui.rememberMonsterTypography
import dev.alenajam.monsterdialer.characters.ui.AddCharacterScreen
import dev.alenajam.monsterdialer.characters.ui.ContactCharacterSettingsContent
import dev.alenajam.monsterdialer.characters.ui.ContactPickerDestination
import dev.alenajam.monsterdialer.characters.ui.PlayerCharacterSettingsContent
import dev.alenajam.monsterdialer.packs.data.CharacterType
import dev.alenajam.monsterdialer.packs.ui.CharacterPackSettingsContent
import dev.alenajam.monsterdialer.packs.ui.CharacterPackSettingsViewModel
import dev.alenajam.opendialer.core.common.DefaultPhoneManager
import dev.alenajam.opendialer.core.common.ui.AppIcon
import dev.alenajam.opendialer.core.common.ui.AppThemeExtension
import dev.alenajam.opendialer.feature.appShell.DialerApp
import dev.alenajam.opendialer.feature.settings.SettingsSubpage
import dev.alenajam.opendialer.feature.settings.SettingsSubpageDestination
import dev.alenajam.opendialer.feature.settings.LocalSettingsSubpageNavigator
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    @Inject
    lateinit var defaultPhoneManager: DefaultPhoneManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }

        setContent {
            val characterPackSettingsViewModel: CharacterPackSettingsViewModel = hiltViewModel()
            val characterPacks by characterPackSettingsViewModel.packs.collectAsStateWithLifecycle()
            var playerCharacterSelectedTab by rememberSaveable { mutableIntStateOf(0) }
            var contactCharactersSelectedTab by rememberSaveable { mutableIntStateOf(0) }

            CompositionLocalProvider(
                LocalMonsterAppIcons provides LocalMonsterAppIcons.current
            ) {
                DialerApp(
                    defaultPhoneManager = defaultPhoneManager,
                    icons = rememberMonsterIcons(),
                    themeExtension = AppThemeExtension(
                        typography = rememberMonsterTypography(MaterialTheme.typography)
                    ),
                    settingsSubpages = listOf(
                        SettingsSubpage(
                            title = stringResource(R.string.settings_character_packs_title),
                            description = stringResource(R.string.settings_character_packs_description),
                            subtitle = pluralStringResource(
                                R.plurals.installed_pack_count,
                                characterPacks.size,
                                characterPacks.size
                            ),
                            content = { CharacterPackSettingsContent(characterPackSettingsViewModel) },
                            topContentPadding = 0.dp
                        ),
                        SettingsSubpage(
                            title = stringResource(R.string.settings_player_character_title),
                            description = stringResource(R.string.settings_player_character_description),
                            content = {
                                PlayerCharacterSettingsContent(
                                    selectedTab = playerCharacterSelectedTab,
                                    onTabSelected = { playerCharacterSelectedTab = it }
                                )
                            },
                            actions = {
                                val navigator = LocalSettingsSubpageNavigator.current
                                IconButton(onClick = {
                                    if (playerCharacterSelectedTab == 0) navigator?.navigateTo(0)
                                    else navigator?.navigateTo(1)
                                }) {
                                    AppIcon(
                                        icon = LocalMonsterAppIcons.current.addCharacter,
                                        contentDescription = stringResource(
                                            if (playerCharacterSelectedTab == 0) R.string.add_trainer
                                            else R.string.add_monster
                                        )
                                    )
                                }
                            },
                            isScrollable = false,
                            topContentPadding = 0.dp,
                            destinations = listOf(
                                SettingsSubpageDestination(title = stringResource(R.string.add_trainer)) { onNavigateBack ->
                                    AddCharacterScreen(onNavigateBack, characterType = CharacterType.Trainer)
                                },
                                SettingsSubpageDestination(title = stringResource(R.string.add_monster)) { onNavigateBack ->
                                    AddCharacterScreen(onNavigateBack, characterType = CharacterType.Monster)
                                }
                            )
                        ),
                        SettingsSubpage(
                            title = stringResource(R.string.settings_contact_characters_title),
                            description = stringResource(R.string.settings_contact_characters_description),
                            content = {
                                ContactCharacterSettingsContent(
                                    selectedTab = contactCharactersSelectedTab,
                                    onTabSelected = { contactCharactersSelectedTab = it }
                                )
                            },
                            actions = {
                                val navigator = LocalSettingsSubpageNavigator.current
                                IconButton(onClick = {
                                    if (contactCharactersSelectedTab == 0) navigator?.navigateTo(1)
                                    else navigator?.navigateTo(2)
                                }) {
                                    AppIcon(
                                        icon = LocalMonsterAppIcons.current.addCharacter,
                                        contentDescription = stringResource(
                                            if (contactCharactersSelectedTab == 0) R.string.add_trainer
                                            else R.string.add_monster
                                        )
                                    )
                                }
                            },
                            isScrollable = false,
                            topContentPadding = 0.dp,
                            destinations = listOf(
                                SettingsSubpageDestination(title = stringResource(R.string.choose_contact)) { onNavigateBack ->
                                    ContactPickerDestination(onNavigateBack)
                                },
                                SettingsSubpageDestination(title = stringResource(R.string.add_trainer)) { onNavigateBack ->
                                    AddCharacterScreen(onNavigateBack, characterType = CharacterType.Trainer)
                                },
                                SettingsSubpageDestination(title = stringResource(R.string.add_monster)) { onNavigateBack ->
                                    AddCharacterScreen(onNavigateBack, characterType = CharacterType.Monster)
                                }
                            )
                        )
                    )
                )
            }
        }
    }
}
