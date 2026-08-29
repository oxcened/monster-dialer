package dev.alenajam.monsterdialer.app

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import dev.alenajam.monsterdialer.R
import dev.alenajam.monsterdialer.app.ui.rememberMonsterIcons
import dev.alenajam.monsterdialer.app.ui.rememberMonsterTypography
import dev.alenajam.monsterdialer.packs.ui.CharacterPackSettingsContent
import dev.alenajam.monsterdialer.packs.ui.CharacterPackSettingsViewModel
import dev.alenajam.monsterdialer.characters.ui.PlayerCharacterSettingsContent
import dev.alenajam.monsterdialer.characters.ui.ContactCharacterSettingsContent
import dev.alenajam.monsterdialer.characters.ui.ContactPickerDestination
import dev.alenajam.opendialer.core.common.DefaultPhoneManager
import dev.alenajam.opendialer.core.common.ui.AppThemeExtension
import dev.alenajam.opendialer.feature.settings.SettingsSubpage
import dev.alenajam.opendialer.feature.settings.SettingsSubpageDestination
import dev.alenajam.opendialer.feature.appShell.DialerApp
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
                        content = { PlayerCharacterSettingsContent() },
                        isScrollable = false,
                        topContentPadding = 0.dp
                    ),
                    SettingsSubpage(
                        title = stringResource(R.string.settings_contact_characters_title),
                        description = stringResource(R.string.settings_contact_characters_description),
                        content = { ContactCharacterSettingsContent() },
                        isScrollable = false,
                        topContentPadding = 0.dp,
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
