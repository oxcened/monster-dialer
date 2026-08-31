package dev.alenajam.monsterdialer.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.IntentCompat
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
import dev.alenajam.monsterdialer.characters.ui.CharacterSettingsSummaryViewModel
import dev.alenajam.monsterdialer.characters.ui.CharactersHomeScreen
import dev.alenajam.monsterdialer.characters.ui.PlayerCharacterSettingsContent
import dev.alenajam.monsterdialer.characters.ui.CharacterSharingViewModel
import dev.alenajam.monsterdialer.characters.ui.SharedCharacterImportHandler
import dev.alenajam.monsterdialer.packs.data.CharacterAssignmentTarget
import dev.alenajam.monsterdialer.packs.data.CharacterPackArchive
import dev.alenajam.monsterdialer.packs.data.CharacterType
import dev.alenajam.monsterdialer.packs.ui.CharacterPackSettingsContent
import dev.alenajam.monsterdialer.packs.ui.CharacterPackSettingsViewModel
import dev.alenajam.monsterdialer.packs.ui.CharacterPackImportHandler
import dev.alenajam.opendialer.core.common.DefaultPhoneManager
import dev.alenajam.opendialer.core.common.ui.AppThemeExtension
import dev.alenajam.opendialer.core.common.ui.AppProviders
import dev.alenajam.opendialer.feature.appShell.DialerApp
import dev.alenajam.opendialer.feature.appShell.HomeNavigationItem
import dev.alenajam.opendialer.feature.appShell.HomeScreenConfiguration
import dev.alenajam.opendialer.feature.settings.SettingsSubpage
import dev.alenajam.opendialer.feature.settings.SettingsSubpageDestination
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    @Inject
    lateinit var defaultPhoneManager: DefaultPhoneManager

    private var incomingImport by mutableStateOf<IncomingImport?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        incomingImport = intent.incomingImport()
        enableEdgeToEdge()

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }

        setContent {
            val characterPackSettingsViewModel: CharacterPackSettingsViewModel = hiltViewModel()
            val characterSharingViewModel: CharacterSharingViewModel = hiltViewModel()
            val visiblePacks by characterPackSettingsViewModel.packs.collectAsStateWithLifecycle()
            val characterSettingsSummaryViewModel: CharacterSettingsSummaryViewModel = hiltViewModel()
            val playerCharacterNames by characterSettingsSummaryViewModel.playerCharacterNames.collectAsStateWithLifecycle()
            val assignedContactCount by characterSettingsSummaryViewModel.assignedContactCount.collectAsStateWithLifecycle()

            CompositionLocalProvider(
                LocalMonsterAppIcons provides LocalMonsterAppIcons.current
            ) {
                val appIcons = rememberMonsterIcons()
                val appThemeExtension = AppThemeExtension(
                    typography = rememberMonsterTypography(MaterialTheme.typography)
                )
                AppProviders(icons = appIcons, themeExtension = appThemeExtension) {
                    DialerApp(
                        defaultPhoneManager = defaultPhoneManager,
                        icons = appIcons,
                        themeExtension = appThemeExtension,
                        homeScreenConfiguration = HomeScreenConfiguration(
                        showVoicemailInNavigation = false,
                        showVoicemailInOverflow = true,
                        customNavigationItem = HomeNavigationItem(
                            label = { androidx.compose.material3.Text(stringResource(R.string.characters_navigation_label)) },
                            icon = { _ -> dev.alenajam.opendialer.core.common.ui.AppIcon(dev.alenajam.opendialer.core.common.ui.LocalAppIcons.current.person, null) },
                            content = { onOpenSubpage ->
                                CharactersHomeScreen(
                                    onOpenSubpage = onOpenSubpage,
                                    sharingViewModel = characterSharingViewModel,
                                    showImportUi = false,
                                )
                            }
                        )
                        ),
                        settingsSubpages = listOf(
                        SettingsSubpage(
                            title = stringResource(R.string.settings_player_character_title),
                            description = stringResource(R.string.settings_player_character_description),
                            subtitle = playerCharacterNames
                                .takeIf { it.isNotEmpty() }
                                ?.joinToString(separator = " • ")
                                ?.let { names -> stringResource(R.string.using_characters, names) }
                                ?: stringResource(R.string.player_characters_not_set),
                            content = { PlayerCharacterSettingsContent() },
                            isScrollable = false,
                            topContentPadding = 0.dp,
                            visibleInSettings = false,
                            destinations = listOf(
                                SettingsSubpageDestination(title = stringResource(R.string.add_trainer)) { payload, onNavigateBack ->
                                    AddCharacterScreen(
                                        onNavigateBack,
                                        characterType = CharacterType.Trainer,
                                        characterId = payload,
                                        preferredAssignmentTarget = CharacterAssignmentTarget.Player
                                    )
                                },
                                SettingsSubpageDestination(title = stringResource(R.string.add_monster)) { payload, onNavigateBack ->
                                    AddCharacterScreen(
                                        onNavigateBack,
                                        characterType = CharacterType.Monster,
                                        characterId = payload,
                                        preferredAssignmentTarget = CharacterAssignmentTarget.Player
                                    )
                                }
                            )
                        ),
                        SettingsSubpage(
                            title = stringResource(R.string.settings_contact_characters_title),
                            description = stringResource(R.string.settings_contact_characters_description),
                            subtitle = if (assignedContactCount == 0) {
                                stringResource(R.string.contact_characters_not_set)
                            } else {
                                pluralStringResource(
                                    R.plurals.contact_character_assignment_count,
                                    assignedContactCount,
                                    assignedContactCount
                                )
                            },
                            content = { ContactCharacterSettingsContent() },
                            isScrollable = false,
                            topContentPadding = 0.dp,
                            visibleInSettings = false,
                            destinations = listOf(
                                SettingsSubpageDestination(title = stringResource(R.string.choose_contact)) { _, onNavigateBack ->
                                    ContactPickerDestination(onNavigateBack)
                                },
                                SettingsSubpageDestination(title = stringResource(R.string.add_trainer)) { payload, onNavigateBack ->
                                    AddCharacterScreen(
                                        onNavigateBack,
                                        characterType = CharacterType.Trainer,
                                        characterId = payload,
                                        preferredAssignmentTarget = CharacterAssignmentTarget.Contact
                                    )
                                },
                                SettingsSubpageDestination(title = stringResource(R.string.add_monster)) { payload, onNavigateBack ->
                                    AddCharacterScreen(
                                        onNavigateBack,
                                        characterType = CharacterType.Monster,
                                        characterId = payload,
                                        preferredAssignmentTarget = CharacterAssignmentTarget.Contact
                                    )
                                }
                            )
                        ),
                        SettingsSubpage(
                            title = stringResource(R.string.settings_character_packs_title),
                            description = stringResource(R.string.settings_character_packs_description),
                            subtitle = run {
                                val installed = visiblePacks.size
                                val enabled = visiblePacks.count { it.enabled }
                                val parts = mutableListOf<String>()
                                if (installed > 0) {
                                    parts.add(pluralStringResource(R.plurals.installed_pack_count, installed, installed))
                                }
                                if (enabled > 0) {
                                    parts.add(pluralStringResource(R.plurals.enabled_pack_count, enabled, enabled))
                                }
                                parts.joinToString(" • ")
                            },
                            content = {
                                CharacterPackSettingsContent(
                                    viewModel = characterPackSettingsViewModel,
                                    showImportUi = false,
                                )
                            },
                            isScrollable = visiblePacks.isNotEmpty(),
                            visibleInSettings = false,
                            topContentPadding = 0.dp
                        )
                        )
                    )
                    LaunchedEffect(incomingImport) {
                        incomingImport?.let { incoming ->
                            incomingImport = null
                            when (incoming) {
                                is IncomingImport.SharedCharacter -> {
                                    characterSharingViewModel.preview(this@MainActivity, incoming.uri)
                                }
                                is IncomingImport.CharacterPack -> {
                                    characterPackSettingsViewModel.previewPack(this@MainActivity, incoming.uri)
                                }
                            }
                        }
                    }
                    SharedCharacterImportHandler(characterSharingViewModel)
                    CharacterPackImportHandler(characterPackSettingsViewModel)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        incomingImport = intent.incomingImport()
    }
}

private sealed interface IncomingImport {
    val uri: Uri

    data class SharedCharacter(override val uri: Uri) : IncomingImport
    data class CharacterPack(override val uri: Uri) : IncomingImport
}

private fun Intent.incomingImport(): IncomingImport? {
    val uri = when (action) {
    Intent.ACTION_VIEW -> data
    Intent.ACTION_SEND -> IntentCompat.getParcelableExtra(this, Intent.EXTRA_STREAM, Uri::class.java)
    else -> null
    } ?: return null

    return if (type == CharacterPackArchive.MimeType || uri.lastPathSegment?.endsWith(".${CharacterPackArchive.Extension}", ignoreCase = true) == true) {
        IncomingImport.CharacterPack(uri)
    } else {
        IncomingImport.SharedCharacter(uri)
    }
}
