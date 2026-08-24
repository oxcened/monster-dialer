package dev.alenajam.monsterdialer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dagger.hilt.android.AndroidEntryPoint
import dev.alenajam.monsterdialer.ui.MonsterIcons
import dev.alenajam.monsterdialer.ui.CharacterPackSettingsContent
import dev.alenajam.monsterdialer.ui.PlayerCharacterSettingsContent
import dev.alenajam.monsterdialer.ui.ContactCharacterSettingsContent
import dev.alenajam.monsterdialer.ui.ContactPickerDestination
import dev.alenajam.opendialer.feature.settings.SettingsSubpage
import dev.alenajam.opendialer.feature.settings.SettingsSubpageDestination
import dev.alenajam.opendialer.feature.appShell.DialerApp

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }

        setContent {
            DialerApp(
                icons = MonsterIcons,
                settingsSubpages = listOf(
                    SettingsSubpage(
                        title = "Character Packs",
                        description = "Import and manage custom character artwork.",
                        content = { CharacterPackSettingsContent() }
                    ),
                    SettingsSubpage(
                        title = "Player Character",
                        description = "Choose who represents you during calls.",
                        content = { PlayerCharacterSettingsContent() }
                    ),
                    SettingsSubpage(
                        title = "Contact Characters",
                        description = "Assign characters to incoming callers.",
                        content = { ContactCharacterSettingsContent() },
                        destinations = listOf(
                            SettingsSubpageDestination(title = "Choose contact") { onNavigateBack ->
                                ContactPickerDestination(onNavigateBack)
                            }
                        )
                    )
                )
            )
        }
    }
}
