package dev.alenajam.monsterdialer.contacts.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.alenajam.monsterdialer.R
import dev.alenajam.monsterdialer.calls.ui.MonsterCallLogArtwork
import dev.alenajam.monsterdialer.calls.ui.MonsterCallLogAvatar
import dev.alenajam.monsterdialer.calls.ui.MonsterCallLogViewModel
import dev.alenajam.monsterdialer.characters.ui.ContactCharacterSettingsEntryPoint
import dev.alenajam.monsterdialer.characters.ui.ContactCharacterSettingsViewModel
import dev.alenajam.monsterdialer.characters.ui.CharacterSettingsPage
import dev.alenajam.monsterdialer.app.ui.RetroContextMenuItem
import dev.alenajam.monsterdialer.app.ui.RetroContextMenuOverlay
import dev.alenajam.monsterdialer.app.ui.RetroFastScroller
import dev.alenajam.monsterdialer.app.ui.RetroSelectionArrow
import dev.alenajam.opendialer.core.common.CommonUtils
import dev.alenajam.opendialer.core.common.PermissionUtils
import dev.alenajam.opendialer.core.common.ui.AppIcon
import dev.alenajam.opendialer.core.common.ui.ContactAvatar
import dev.alenajam.opendialer.core.common.ui.LocalAppIcons
import dev.alenajam.opendialer.core.common.ui.contactAvatarColorKey
import dev.alenajam.opendialer.data.contacts.DialerContactSummary
import dev.alenajam.opendialer.feature.contacts.ContactsViewModel
import dev.alenajam.opendialer.feature.contacts.R as ContactsR
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private val RetroContactsPaper = androidx.compose.ui.graphics.Color(0xFFF9F7FC)
private val RetroContactsInk = androidx.compose.ui.graphics.Color(0xFF202020)
private val RetroContactsFont = FontFamily(Font(R.font.ui_pixel_font))

@Composable
fun RetroContactsScreen(
    searchQuery: String,
    onOpenSettingsSubpage: (Int, String?) -> Unit,
    characterSettingsViewModel: ContactCharacterSettingsViewModel,
    viewModel: ContactsViewModel = hiltViewModel(),
    artworkViewModel: MonsterCallLogViewModel = hiltViewModel(),
) {
    val requestPermissions = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { result ->
        if (PermissionUtils.contactsPermissions.all { result[it] == true }) {
            viewModel.handleRuntimePermissionGranted()
        }
    }
    val contacts by viewModel.contacts.collectAsStateWithLifecycle()
    val profileContact by viewModel.profileContact.collectAsStateWithLifecycle()
    val hasPermission by viewModel.hasRuntimePermission.collectAsStateWithLifecycle()
    val assignmentVersion by artworkViewModel.assignmentVersion.collectAsStateWithLifecycle()
    val artworkByContactId by artworkViewModel.artworkByContactId.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val allContactsLabel = stringResource(ContactsR.string.all_contacts)
    val favoritesLabel = stringResource(ContactsR.string.favorites)
    val filteredContacts = remember(contacts, searchQuery) {
        if (searchQuery.isBlank()) contacts else contacts.filter {
            it.name.contains(searchQuery.trim(), ignoreCase = true)
        }
    }
    val listItems = remember(filteredContacts, searchQuery) {
        buildRetroContactListItems(filteredContacts, searchQuery.isBlank(), allContactsLabel, favoritesLabel)
    }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    var selectedContact by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<DialerContactSummary?>(null) }
    var cursorRowKey by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<String?>(null) }
    val cursorIndex = listItems.indexOfFirst { item ->
        val rowKey = when (item) {
            is RetroContactListItem.Header -> "header-${item.label}"
            is RetroContactListItem.Contact -> "contact-${item.section}-${item.contact.id}"
        }
        rowKey == cursorRowKey
    }
        .takeIf { it >= 0 }
        ?: listItems.indexOfFirst { item -> item is RetroContactListItem.Contact }

    LaunchedEffect(filteredContacts, assignmentVersion) {
        artworkViewModel.refreshContacts(filteredContacts)
    }

    Surface(modifier = Modifier.fillMaxSize(), color = RetroContactsPaper) {
        if (!hasPermission) {
            Column(
                modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
            ) {
                RetroContactsText(
                    stringResource(ContactsR.string.placeholder_contacts),
                    16.sp,
                    textAlign = TextAlign.Center,
                )
                RetroContactsCommand(stringResource(ContactsR.string.turn_on)) {
                    requestPermissions.launch(PermissionUtils.contactsPermissions)
                }
            }
            return@Surface
        }

        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(start = 2.dp, top = 0.dp, end = 2.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp),
            ) {
                if (searchQuery.isBlank()) {
                    item("new-contact") {
                        RetroContactsCommand(stringResource(R.string.new_contact_short)) {
                            CommonUtils.createContact(context, null)
                        }
                    }
                    profileContact?.let { profile ->
                        item("profile-contact") {
                            RetroProfileContactRow(
                                contact = profile,
                                onOpenProfile = viewModel::openProfileContact,
                                onShareProfile = { viewModel.shareProfileContact(profile.id) },
                            )
                        }
                    }
                }
                itemsIndexed(
                    items = listItems,
                    key = { _, item ->
                        when (item) {
                            is RetroContactListItem.Header -> "header-${item.label}"
                            is RetroContactListItem.Contact -> "contact-${item.section}-${item.contact.id}"
                        }
                    },
                ) { index, item ->
                    when (item) {
                        is RetroContactListItem.Header -> RetroContactSectionHeader(item.label, item.favorite)
                        is RetroContactListItem.Contact -> RetroContactRow(
                            contact = item.contact,
                            artwork = artworkByContactId[item.contact.id],
                            selected = index == cursorIndex,
                            onOpenContact = {
                                cursorRowKey = "contact-${item.section}-${item.contact.id}"
                                selectedContact = item.contact
                            },
                        )
                    }
                }
            }
            RetroFastScroller(
                listState = listState,
                contentDescription = stringResource(ContactsR.string.fast_scroll_contacts),
                modifier = Modifier.align(Alignment.CenterEnd).padding(vertical = 8.dp),
            )
            selectedContact?.let { contact ->
                RetroContextMenuOverlay(
                        modifier = Modifier.fillMaxWidth(0.82f),
                        fontFamily = RetroContactsFont,
                        onDismissRequest = { selectedContact = null },
                        items = listOf(
                            RetroContextMenuItem(stringResource(R.string.contact_details_action)) {
                                selectedContact = null
                                viewModel.openContact(contact.id)
                            },
                            RetroContextMenuItem(stringResource(R.string.character_type_trainer)) {
                                selectedContact = null
                                coroutineScope.launch {
                                    characterSettingsViewModel.selectContact(contact)
                                    characterSettingsViewModel.setSelectedTab(0)
                                    onOpenSettingsSubpage(
                                        CharacterSettingsPage.ContactCharacters.index,
                                        ContactCharacterSettingsEntryPoint.ContactList.payload,
                                    )
                                }
                            },
                            RetroContextMenuItem(stringResource(R.string.character_type_monster)) {
                                selectedContact = null
                                coroutineScope.launch {
                                    characterSettingsViewModel.selectContact(contact)
                                    characterSettingsViewModel.setSelectedTab(1)
                                    onOpenSettingsSubpage(
                                        CharacterSettingsPage.ContactCharacters.index,
                                        ContactCharacterSettingsEntryPoint.ContactList.payload,
                                    )
                                }
                            },
                            RetroContextMenuItem(stringResource(R.string.linked_online_profile_title)) {
                                selectedContact = null
                                coroutineScope.launch {
                                    characterSettingsViewModel.selectContact(contact)
                                    onOpenSettingsSubpage(CharacterSettingsPage.LinkedOnlineProfile.index, null)
                                }
                            },
                            RetroContextMenuItem.cancel(stringResource(R.string.cancel), onClick = { selectedContact = null }),
                        ),
                )
            }
        }
    }
}

@Composable
private fun RetroProfileContactRow(
    contact: DialerContactSummary,
    onOpenProfile: () -> Unit,
    onShareProfile: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onOpenProfile).padding(horizontal = 8.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ContactAvatar(contact.name, contact.image, colorKey = contactAvatarColorKey(contact.name), modifier = Modifier.size(42.dp))
        Column(modifier = Modifier.weight(1f).padding(horizontal = 10.dp)) {
            RetroContactsText(stringResource(ContactsR.string.your_info), 13.sp, RetroContactsInk.copy(alpha = 0.75f))
            RetroContactsText(contact.name, 16.sp, maxLines = 1)
        }
        AppIcon(
            icon = LocalAppIcons.current.share,
            contentDescription = stringResource(ContactsR.string.share_contact),
            modifier = Modifier.size(24.dp).clickable(onClick = onShareProfile),
            tint = RetroContactsInk,
        )
    }
}

@Composable
private fun RetroContactRow(
    contact: DialerContactSummary,
    artwork: MonsterCallLogArtwork?,
    selected: Boolean,
    onOpenContact: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onOpenContact).padding(horizontal = 8.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (selected) {
            RetroSelectionArrow(tint = RetroContactsInk, size = 14.dp)
            androidx.compose.foundation.layout.Spacer(Modifier.size(2.dp))
        } else {
            androidx.compose.foundation.layout.Spacer(Modifier.size(16.dp))
        }
        if (artwork != null) {
            MonsterCallLogAvatar(artwork)
        } else {
            ContactAvatar(contact.name, contact.image, colorKey = contactAvatarColorKey(contact.name), modifier = Modifier.size(42.dp))
        }
        RetroContactsText(contact.name, 16.sp, modifier = Modifier.weight(1f).padding(horizontal = 10.dp), maxLines = 1)
    }
}

@Composable
private fun RetroContactSectionHeader(label: String, favorite: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (favorite) {
            AppIcon(LocalAppIcons.current.favorite, contentDescription = null, modifier = Modifier.size(16.dp), tint = RetroContactsInk)
            androidx.compose.foundation.layout.Spacer(Modifier.size(8.dp))
        }
        RetroContactsText(label, 13.sp, RetroContactsInk.copy(alpha = 0.75f))
    }
}

@Composable
private fun RetroContactsCommand(label: String, onClick: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 2.dp), horizontalArrangement = Arrangement.End) {
        RetroContactsText(label, 18.sp, modifier = Modifier.clickable(onClick = onClick).padding(start = 16.dp, top = 14.dp, end = 16.dp, bottom = 6.dp))
    }
}

@Composable
private fun RetroContactsText(
    text: String,
    size: TextUnit,
    color: androidx.compose.ui.graphics.Color = RetroContactsInk,
    modifier: Modifier = Modifier,
    maxLines: Int = Int.MAX_VALUE,
    textAlign: TextAlign? = null,
) {
    Text(
        text = text.uppercase(),
        modifier = modifier,
        fontFamily = RetroContactsFont,
        fontSize = size,
        lineHeight = size * 1.15f,
        color = color,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
        textAlign = textAlign,
    )
}

private sealed class RetroContactListItem {
    data class Header(val label: String, val favorite: Boolean) : RetroContactListItem()
    data class Contact(val contact: DialerContactSummary, val section: String) : RetroContactListItem()
}

private fun buildRetroContactListItems(
    contacts: List<DialerContactSummary>,
    groupBySection: Boolean,
    allContactsLabel: String,
    favoritesLabel: String,
): List<RetroContactListItem> = buildList {
    val sorted = contacts.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name })
    fun addSection(label: String, sectionContacts: List<DialerContactSummary>, favorite: Boolean = false) {
        if (sectionContacts.isEmpty()) return
        add(RetroContactListItem.Header(label, favorite))
        sectionContacts.forEach { add(RetroContactListItem.Contact(it, label)) }
    }
    if (!groupBySection) {
        addSection(allContactsLabel, sorted)
    } else {
        addSection(favoritesLabel, sorted.filter { it.starred }, favorite = true)
        sorted.groupBy { it.name.firstOrNull()?.uppercaseChar()?.toString() ?: "#" }
            .toSortedMap()
            .forEach { (initial, sectionContacts) -> addSection(initial, sectionContacts) }
    }
}
