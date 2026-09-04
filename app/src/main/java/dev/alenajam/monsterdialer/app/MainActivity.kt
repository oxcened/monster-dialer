package dev.alenajam.monsterdialer.app

import android.content.Intent
import android.content.ContentResolver
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
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
import dev.alenajam.monsterdialer.characters.ui.ContactCharacterSettingsViewModel
import dev.alenajam.monsterdialer.characters.ui.ContactPickerDestination
import dev.alenajam.monsterdialer.characters.ui.CharacterSettingsSummaryViewModel
import dev.alenajam.monsterdialer.characters.ui.CharactersHomeScreen
import dev.alenajam.monsterdialer.characters.ui.PlayerCharacterSettingsContent
import dev.alenajam.monsterdialer.characters.ui.PlayerCharacterSettingsRoute
import dev.alenajam.monsterdialer.characters.ui.CharacterSharingViewModel
import dev.alenajam.monsterdialer.characters.ui.SharedCharacterImportHandler
import dev.alenajam.monsterdialer.battle.ui.BattleJournalScreen
import dev.alenajam.monsterdialer.battle.ui.BattleJournalOverflowMenu
import dev.alenajam.monsterdialer.contacts.data.MonsterContact
import dev.alenajam.monsterdialer.contacts.ui.formatPhoneNumber
import dev.alenajam.monsterdialer.packs.data.CharacterAssignmentTarget
import dev.alenajam.monsterdialer.packs.data.CharacterPackArchive
import dev.alenajam.monsterdialer.packs.data.CharacterType
import dev.alenajam.monsterdialer.onlineprofiles.data.ProfileSharingLink
import dev.alenajam.monsterdialer.onlineprofiles.ui.LinkedOnlineProfileContent
import dev.alenajam.monsterdialer.onlineprofiles.ui.SharedProfileImportScreen
import dev.alenajam.monsterdialer.packs.ui.CharacterPackSettingsContent
import dev.alenajam.monsterdialer.packs.ui.CharacterPackSettingsViewModel
import dev.alenajam.monsterdialer.packs.ui.CharacterPackImportHandler
import dev.alenajam.monsterdialer.packs.ui.CreateCharacterPackScreen
import dev.alenajam.opendialer.core.common.DefaultPhoneManager
import dev.alenajam.opendialer.core.common.ui.AppThemeExtension
import dev.alenajam.opendialer.core.common.ui.AppProviders
import dev.alenajam.opendialer.core.common.ui.ContactAvatar
import dev.alenajam.opendialer.feature.appShell.DialerApp
import dev.alenajam.opendialer.feature.appShell.HomeNavigationItem
import dev.alenajam.opendialer.feature.appShell.HomeScreenConfiguration
import dev.alenajam.opendialer.feature.contacts.ContactRowOverflowAction
import dev.alenajam.opendialer.feature.contacts.ContactRowOverflowMenu
import dev.alenajam.opendialer.feature.settings.SettingsSubpage
import dev.alenajam.opendialer.feature.settings.SettingsSubpageDestination
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    @Inject
    lateinit var defaultPhoneManager: DefaultPhoneManager

    private var incomingImport by mutableStateOf<IncomingImport?>(null)
    private var sharedProfileImportId by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        incomingImport = intent.incomingImport(contentResolver)
        enableEdgeToEdge()

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }

        setContent {
            val characterPackSettingsViewModel: CharacterPackSettingsViewModel = hiltViewModel()
            val characterSharingViewModel: CharacterSharingViewModel = hiltViewModel()
            val contactCharacterSettingsViewModel: ContactCharacterSettingsViewModel = hiltViewModel()
            val visiblePacks by characterPackSettingsViewModel.packs.collectAsStateWithLifecycle()
            val characterSettingsSummaryViewModel: CharacterSettingsSummaryViewModel = hiltViewModel()
            val playerCharacterNames by characterSettingsSummaryViewModel.playerCharacterNames.collectAsStateWithLifecycle()
            val playerProfile by characterSettingsSummaryViewModel.playerProfile.collectAsStateWithLifecycle()
            val profileMetrics by characterSettingsSummaryViewModel.profileMetrics.collectAsStateWithLifecycle()
            val selectedContact by contactCharacterSettingsViewModel.contact.collectAsStateWithLifecycle()

            CompositionLocalProvider(
                LocalMonsterAppIcons provides LocalMonsterAppIcons.current
            ) {
                val appIcons = rememberMonsterIcons()
                val appThemeExtension = AppThemeExtension(
                    typography = rememberMonsterTypography(MaterialTheme.typography)
                )
                AppProviders(icons = appIcons, themeExtension = appThemeExtension) {
                    if (sharedProfileImportId != null) {
                        SharedProfileImportScreen(
                            viewModel = contactCharacterSettingsViewModel,
                            onNavigateBack = {
                                contactCharacterSettingsViewModel.clearPendingOnlineProfile()
                                sharedProfileImportId = null
                            },
                            onProfileLinked = { sharedProfileImportId = null },
                        )
                    } else DialerApp(
                        defaultPhoneManager = defaultPhoneManager,
                        icons = appIcons,
                        themeExtension = appThemeExtension,
                        homeScreenConfiguration = HomeScreenConfiguration(
                            showVoicemailInNavigation = false,
                            showVoicemailInOverflow = true,
                            contactRowOverflowMenu = ContactRowOverflowMenu(
                                actions = listOf(
                                    ContactRowOverflowAction(
                                        settingsSubpageIndex = 1,
                                        onClick = contactCharacterSettingsViewModel::selectContact,
                                        content = { Text(stringResource(R.string.choose_team)) },
                                    ),
                                    ContactRowOverflowAction(
                                        settingsSubpageIndex = LinkedOnlineProfileSettingsIndex,
                                        onClick = { contact ->
                                            contactCharacterSettingsViewModel.selectContact(contact)
                                        },
                                        content = { Text(stringResource(R.string.linked_online_profile_title)) },
                                    ),
                                ),
                                content = { actions, expanded, onExpandedChange, onActionClick ->
                                    IconButton(onClick = { onExpandedChange(true) }) {
                                        dev.alenajam.opendialer.core.common.ui.AppIcon(
                                            LocalMonsterAppIcons.current.personalizeContact,
                                            contentDescription = stringResource(R.string.contact_row_actions),
                                        )
                                    }
                                    DropdownMenu(
                                        expanded = expanded,
                                        onDismissRequest = { onExpandedChange(false) },
                                    ) {
                                        actions.forEach { action ->
                                            DropdownMenuItem(
                                                text = action.content,
                                                onClick = { onActionClick(action) },
                                            )
                                        }
                                    }
                                },
                            ),
                            customNavigationItem = HomeNavigationItem(
                            label = { androidx.compose.material3.Text(stringResource(R.string.characters_navigation_label)) },
                            icon = { _ -> dev.alenajam.opendialer.core.common.ui.AppIcon(dev.alenajam.opendialer.core.common.ui.LocalAppIcons.current.person, null) },
                            content = { onOpenSubpage ->
                                CharactersHomeScreen(
                                    onOpenSubpage = onOpenSubpage,
                                    sharingViewModel = characterSharingViewModel,
                                    playerProfile = playerProfile,
                                    profileMetrics = profileMetrics,
                                    onReorderRoster = characterSettingsSummaryViewModel::reorderPlayerMonsterRoster,
                                    onRemoveRosterMonster = characterSettingsSummaryViewModel::removePlayerMonsterFromRoster,
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
                            content = { payload ->
                                PlayerCharacterSettingsContent(
                                    route = PlayerCharacterSettingsRoute.fromPayload(payload),
                                    payload = payload,
                                )
                            },
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
                            title = selectedContact?.name ?: stringResource(R.string.settings_contact_characters_title),
                            description = stringResource(R.string.settings_contact_characters_description),
                            topBarTitle = selectedContact?.let { contact ->
                                { ContactCharacterTopBarTitle(contact) }
                            },
                            content = { _ -> ContactCharacterSettingsContent() },
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
                            content = { _ ->
                                CharacterPackSettingsContent(
                                    viewModel = characterPackSettingsViewModel,
                                    showImportUi = false,
                                )
                            },
                            isScrollable = visiblePacks.isNotEmpty(),
                            visibleInSettings = false,
                            topContentPadding = 0.dp,
                            destinations = listOf(
                                SettingsSubpageDestination(title = stringResource(R.string.create_character_pack)) { _, onNavigateBack ->
                                    CreateCharacterPackScreen(onNavigateBack)
                                }
                            )
                        ),
                        SettingsSubpage(
                            title = stringResource(R.string.battle_journal_title),
                            description = stringResource(R.string.battle_journal_description),
                            content = { _ -> BattleJournalScreen() },
                            actions = { BattleJournalOverflowMenu() },
                            visibleInSettings = false,
                            isScrollable = false,
                            topContentPadding = 0.dp,
                        ),
                        SettingsSubpage(
                            title = stringResource(R.string.linked_online_profile_title),
                            description = null,
                            content = { LinkedOnlineProfileContent(contactCharacterSettingsViewModel) },
                            isScrollable = false,
                            topContentPadding = 0.dp,
                            visibleInSettings = false,
                        ),
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
                                is IncomingImport.OnlineProfile -> {
                                    contactCharacterSettingsViewModel.prepareOnlineProfileLink(incoming.publicProfileId)
                                    sharedProfileImportId = incoming.publicProfileId
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
        incomingImport = intent.incomingImport(contentResolver)
    }
}

private const val LinkedOnlineProfileSettingsIndex = 4

private sealed interface IncomingImport {
    val uri: Uri

    data class SharedCharacter(override val uri: Uri) : IncomingImport
    data class CharacterPack(override val uri: Uri) : IncomingImport
    data class OnlineProfile(override val uri: Uri, val publicProfileId: String) : IncomingImport
}

private fun Intent.incomingImport(contentResolver: ContentResolver): IncomingImport? {
    val uri = when (action) {
    Intent.ACTION_VIEW -> data
    Intent.ACTION_SEND -> IntentCompat.getParcelableExtra(this, Intent.EXTRA_STREAM, Uri::class.java)
    else -> null
    } ?: return null

    val onlineProfileId = ProfileSharingLink.profileIdFrom(uri.toString())
    return when {
        onlineProfileId != null -> IncomingImport.OnlineProfile(uri, onlineProfileId)
        type == CharacterPackArchive.MimeType || uri.displayName(contentResolver).endsWith(".${CharacterPackArchive.Extension}", ignoreCase = true) -> {
        IncomingImport.CharacterPack(uri)
        }
        else -> IncomingImport.SharedCharacter(uri)
    }
}

private fun Uri.displayName(contentResolver: ContentResolver): String {
    contentResolver.query(this, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
        val column = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (column >= 0 && cursor.moveToFirst()) {
            cursor.getString(column)?.takeIf(String::isNotBlank)?.let { return it }
        }
    }
    return lastPathSegment.orEmpty()
}

@Composable
private fun ContactCharacterTopBarTitle(contact: MonsterContact) {
    val locale = LocalConfiguration.current.locales[0]
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ContactAvatar(
            name = contact.name,
            photoUri = contact.photoUri,
            modifier = Modifier.size(32.dp),
            initialTextStyle = MaterialTheme.typography.labelLarge,
        )
        Column {
            Text(contact.name, style = MaterialTheme.typography.titleMedium)
            Text(
                text = contact.numbers.firstOrNull()?.let { formatPhoneNumber(it, locale) }
                    ?: stringResource(R.string.unknown),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
