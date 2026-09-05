package dev.alenajam.monsterdialer.app

import android.content.Intent
import android.content.ContentResolver
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import dev.alenajam.monsterdialer.characters.ui.ContactCharacterSettingsEntryPoint
import dev.alenajam.monsterdialer.characters.ui.ContactCharacterSettingsContent
import dev.alenajam.monsterdialer.characters.ui.ContactCharacterSettingsViewModel
import dev.alenajam.monsterdialer.characters.ui.ContactPickerDestination
import dev.alenajam.monsterdialer.characters.ui.ContextualGuideButton
import dev.alenajam.monsterdialer.characters.ui.GuideContent
import dev.alenajam.monsterdialer.characters.ui.CharacterSettingsSummaryViewModel
import dev.alenajam.monsterdialer.characters.ui.CharactersHomeScreen
import dev.alenajam.monsterdialer.characters.ui.PlayerCharacterSettingsContent
import dev.alenajam.monsterdialer.characters.ui.CharacterSharingViewModel
import dev.alenajam.monsterdialer.characters.ui.PlayerCharacterSettingsRoute
import dev.alenajam.monsterdialer.characters.ui.SharedCharacterImportHandler
import dev.alenajam.monsterdialer.characters.ui.radiantGuideContents
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
import dev.alenajam.monsterdialer.onlineprofiles.ui.sharedOnlineProfileGuideContents
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
import dev.alenajam.opendialer.feature.contacts.ContactRowTrailingContent
import dev.alenajam.opendialer.feature.settings.LocalSettingsSubpageNavigator
import kotlinx.coroutines.launch
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
                        defaultPhoneManager = remember(defaultPhoneManager) {
                            SafeDefaultPhoneManager(defaultPhoneManager, packageManager)
                        },
                        icons = appIcons,
                        themeExtension = appThemeExtension,
                        homeScreenConfiguration = HomeScreenConfiguration(
                            showVoicemailInNavigation = false,
                            showVoicemailInOverflow = true,
                            contactRowTrailingContent = ContactRowTrailingContent { contact, onOpenSettingsSubpage ->
                                val coroutineScope = rememberCoroutineScope()
                                var expanded by remember(contact.id) { mutableStateOf(false) }
                                Box {
                                    IconButton(onClick = { expanded = true }) {
                                        dev.alenajam.opendialer.core.common.ui.AppIcon(
                                            LocalMonsterAppIcons.current.personalizeContact,
                                            contentDescription = stringResource(R.string.contact_row_actions),
                                        )
                                    }
                                    DropdownMenu(
                                        expanded = expanded,
                                        onDismissRequest = { expanded = false },
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text(stringResource(R.string.contact_characters_action)) },
                                            leadingIcon = {
                                                dev.alenajam.opendialer.core.common.ui.AppIcon(
                                                    dev.alenajam.opendialer.core.common.ui.LocalAppIcons.current.edit,
                                                    contentDescription = null,
                                                )
                                            },
                                            onClick = {
                                                expanded = false
                                                coroutineScope.launch {
                                                    contactCharacterSettingsViewModel.selectContact(contact)
                                                    onOpenSettingsSubpage(1, ContactCharacterSettingsEntryPoint.ContactList.payload)
                                                }
                                            },
                                        )
                                        DropdownMenuItem(
                                            text = { Text(stringResource(R.string.linked_online_profile_title)) },
                                            onClick = {
                                                expanded = false
                                                coroutineScope.launch {
                                                    contactCharacterSettingsViewModel.selectContact(contact)
                                                    onOpenSettingsSubpage(LinkedOnlineProfileSettingsIndex, null)
                                                }
                                            },
                                        )
                                    }
                                }
                            },
                            customNavigationItem = HomeNavigationItem(
                            label = { androidx.compose.material3.Text(stringResource(R.string.characters_navigation_label)) },
                            icon = { _ -> dev.alenajam.opendialer.core.common.ui.AppIcon(dev.alenajam.opendialer.core.common.ui.LocalAppIcons.current.person, null) },
                            content = { onOpenSubpage ->
                                CharactersHomeScreen(
                                    onOpenSubpage = { index, payload ->
                                        val destination = if (index == 1) ToolboxContactCharactersSettingsIndex else index
                                        onOpenSubpage(destination, payload)
                                    },
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
                            subtitle = stringResource(
                                R.string.using_characters,
                                listOf(playerProfile.trainer.name, playerProfile.monster.name)
                                    .joinToString(separator = " • "),
                            ),
                            content = { payload ->
                                PlayerCharacterSettingsContent(
                                    route = PlayerCharacterSettingsRoute.fromPayload(payload),
                                    payload = payload,
                                )
                            },
                            isScrollable = false,
                            topContentPadding = 0.dp,
                            visibleInSettings = false,
                            actions = {
                                ContextualGuideButton(
                                    contents = listOf(
                                        GuideContent(R.string.characters_help_character_list_title, R.string.characters_help_player_character_list_message),
                                        *radiantGuideContents().toTypedArray(),
                                    ),
                                )
                            },
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
                            topBarTitle = {
                                val navigator = LocalSettingsSubpageNavigator.current
                                ContactCharacterTopBarTitle(
                                    contact = selectedContact,
                                    onClick = { navigator?.navigateTo(0) }
                                )
                            },
                            content = { payload ->
                                ContactCharacterSettingsContent(
                                    entryPoint = ContactCharacterSettingsEntryPoint.fromPayload(payload),
                                    viewModel = contactCharacterSettingsViewModel,
                                )
                            },
                            isScrollable = false,
                            topContentPadding = 0.dp,
                            visibleInSettings = false,
                            actions = {
                                ContextualGuideButton(
                                    contents = listOf(
                                        GuideContent(
                                            R.string.characters_help_assignments_title,
                                            R.string.characters_help_assignments_message,
                                            listOf(R.string.characters_help_assignments_contact),
                                        ),
                                        GuideContent(R.string.characters_help_character_list_title, R.string.characters_help_contact_character_list_message),
                                    ),
                                )
                            },
                            destinations = listOf(
                                SettingsSubpageDestination(title = stringResource(R.string.choose_contact)) { _, onNavigateBack ->
                                    ContactPickerDestination(
                                        onNavigateBack = onNavigateBack,
                                        viewModel = contactCharacterSettingsViewModel,
                                    )
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
                            actions = {
                                ContextualGuideButton(
                                    contents = listOf(
                                        GuideContent(R.string.characters_help_packs_import_title, R.string.characters_help_packs_import_message),
                                        GuideContent(R.string.characters_help_packs_title, R.string.characters_help_packs_message),
                                    ),
                                )
                            },
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
                            actions = {
                                ContextualGuideButton(
                                    contents = sharedOnlineProfileGuideContents(),
                                    contentDescription = R.string.open_shared_online_profile_guide,
                                )
                            },
                            isScrollable = false,
                            topContentPadding = 0.dp,
                            visibleInSettings = false,
                        ),
                        ).let { subpages ->
                            // Share the character screen and its destinations.
                            subpages + subpages[1]
                        }
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
private const val ToolboxContactCharactersSettingsIndex = 5

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
private fun ContactCharacterTopBarTitle(
    contact: MonsterContact?,
    onClick: () -> Unit
) {
    val locale = LocalConfiguration.current.locales[0]
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp, horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (contact != null) {
            Box(modifier = Modifier.size(36.dp)) {
                ContactAvatar(
                    name = contact.name,
                    photoUri = contact.photoUri,
                    modifier = Modifier.size(32.dp),
                    initialTextStyle = MaterialTheme.typography.labelLarge,
                )
                Surface(
                    modifier = Modifier
                        .size(18.dp)
                        .align(Alignment.BottomEnd),
                    shape = CircleShape,
                    color = Color.White,
                    tonalElevation = 1.dp,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        dev.alenajam.opendialer.core.common.ui.AppIcon(
                            dev.alenajam.opendialer.core.common.ui.LocalAppIcons.current.edit,
                            contentDescription = null,
                            modifier = Modifier.size(11.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = contact.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = contact.numbers.firstOrNull()?.let { formatPhoneNumber(it, locale) }
                        ?: stringResource(R.string.unknown),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            dev.alenajam.opendialer.core.common.ui.AppIcon(
                dev.alenajam.opendialer.core.common.ui.LocalAppIcons.current.person,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = stringResource(R.string.choose_contact),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
