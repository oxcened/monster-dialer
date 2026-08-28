package dev.alenajam.monsterdialer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.ui.res.stringResource
import dagger.hilt.android.AndroidEntryPoint
import dev.alenajam.monsterdialer.ui.MonsterIcons
import dev.alenajam.monsterdialer.packs.ui.CharacterPackSettingsContent
import dev.alenajam.monsterdialer.characters.ui.PlayerCharacterSettingsContent
import dev.alenajam.monsterdialer.characters.ui.ContactCharacterSettingsContent
import dev.alenajam.monsterdialer.characters.ui.ContactPickerDestination
import dev.alenajam.opendialer.core.common.DefaultPhoneManager
import dev.alenajam.opendialer.feature.settings.SettingsSubpage
import dev.alenajam.opendialer.feature.settings.SettingsSubpageDestination
import dev.alenajam.opendialer.feature.appShell.DialerApp
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var defaultPhoneManager: DefaultPhoneManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }

        setContent {
            DialerApp(
                defaultPhoneManager = defaultPhoneManager,
                icons = MonsterIcons,
                settingsSubpages = listOf(
                    SettingsSubpage(
                        title = stringResource(R.string.settings_character_packs_title),
                        description = stringResource(R.string.settings_character_packs_description),
                        content = { CharacterPackSettingsContent() }
                    ),
                    SettingsSubpage(
                        title = stringResource(R.string.settings_player_character_title),
                        description = stringResource(R.string.settings_player_character_description),
                        content = { PlayerCharacterSettingsContent() }
                    ),
                    SettingsSubpage(
                        title = stringResource(R.string.settings_contact_characters_title),
                        description = stringResource(R.string.settings_contact_characters_description),
                        content = { ContactCharacterSettingsContent() },
                        destinations = listOf(
                            SettingsSubpageDestination(title = stringResource(R.string.choose_contact)) { onNavigateBack ->
                                ContactPickerDestination(onNavigateBack)
                            }
                        )
                    )
                )
            )
        }
    }
}
