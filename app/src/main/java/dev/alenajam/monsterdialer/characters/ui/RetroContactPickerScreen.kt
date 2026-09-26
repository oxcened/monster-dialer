package dev.alenajam.monsterdialer.characters.ui

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
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
import dev.alenajam.monsterdialer.app.ui.PixelRoundedSquareShape
import dev.alenajam.monsterdialer.app.ui.RetroActionButton
import dev.alenajam.monsterdialer.app.ui.RetroFastScroller
import dev.alenajam.monsterdialer.app.ui.RetroSearchBar
import dev.alenajam.monsterdialer.app.ui.RetroSelectionArrow
import dev.alenajam.monsterdialer.app.ui.RetroScreenFooterVerticalPadding
import dev.alenajam.opendialer.core.common.PermissionUtils
import dev.alenajam.opendialer.core.common.ui.AppIcon
import dev.alenajam.opendialer.core.common.ui.ContactAvatar
import dev.alenajam.opendialer.core.common.ui.LocalAppIcons
import dev.alenajam.opendialer.core.common.ui.contactAvatarColorKey
import dev.alenajam.opendialer.data.contacts.DialerContactSummary
import dev.alenajam.opendialer.feature.contacts.ContactsViewModel

private val ContactPickerPaper = Color(0xFFF9F7FC)
private val ContactPickerInk = Color(0xFF202020)
private val ContactPickerFont = FontFamily(Font(R.font.ui_pixel_font))

@Composable
internal fun RetroContactPickerScreen(
    onNavigateBack: () -> Unit,
    onContactSelected: (DialerContactSummary) -> Unit,
    excludedContactIds: Set<Int> = emptySet(),
    viewModel: ContactsViewModel = hiltViewModel(),
) {
    val contacts by viewModel.contacts.collectAsStateWithLifecycle()
    val hasPermission by viewModel.hasRuntimePermission.collectAsStateWithLifecycle()
    var isSearching by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var cursorRowKey by remember { mutableStateOf<String?>(null) }
    val searchFocusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val requestPermissions = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { result ->
        if (PermissionUtils.contactsPermissions.all { result[it] == true }) {
            viewModel.handleRuntimePermissionGranted()
        }
    }
    val favoritesLabel = stringResource(R.string.favorites)
    val availableContacts = remember(contacts, excludedContactIds, isSearching, searchQuery) {
        contacts
            .filterNot { it.id in excludedContactIds }
            .filter { !isSearching || it.name.contains(searchQuery.trim(), ignoreCase = true) }
            .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name })
    }
    val listItems = remember(availableContacts, favoritesLabel, isSearching) {
        buildContactPickerItems(availableContacts, isSearching, favoritesLabel)
    }
    val listState = rememberLazyListState()
    val cursorIndex = listItems.indexOfFirst { it.key == cursorRowKey }
        .takeIf { it >= 0 }
        ?: listItems.indexOfFirst { it is ContactPickerItem.Contact }
    LaunchedEffect(isSearching) {
        if (isSearching) {
            searchFocusRequester.requestFocus()
            keyboardController?.show()
        } else {
            focusManager.clearFocus()
            keyboardController?.hide()
        }
    }
    BackHandler {
        if (isSearching) {
            isSearching = false
            searchQuery = ""
        } else {
            onNavigateBack()
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = ContactPickerPaper) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing),
        ) {
            if (!hasPermission) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 24.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        ContactPickerText(
                            text = stringResource(R.string.favorites_permission_prompt),
                            size = 16.sp,
                            textAlign = TextAlign.Center,
                        )
                    }
                    ContactPickerCommand(stringResource(R.string.turn_on)) {
                        requestPermissions.launch(PermissionUtils.contactsPermissions)
                    }
                    ContactPickerBackAction(onClick = onNavigateBack)
                }
            }
            else {
                Column(modifier = Modifier.fillMaxSize()) {
                if (isSearching) {
                    RetroSearchBar(
                        label = stringResource(R.string.contact_picker_search_name_prefix),
                        query = searchQuery,
                        focusRequester = searchFocusRequester,
                        onQueryChanged = { searchQuery = it },
                    )
                } else {
                    ContactPickerCommand(stringResource(R.string.contact_picker_search)) {
                        isSearching = true
                    }
                }

                    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(start = 2.dp, top = 0.dp, end = 2.dp, bottom = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(0.dp),
                        ) {
                            itemsIndexed(listItems, key = { _, item -> item.key }) { index, item ->
                                when (item) {
                                    is ContactPickerItem.Header -> ContactPickerSectionHeader(item.label, item.favorite)
                                    is ContactPickerItem.Contact -> ContactPickerRow(
                                        contact = item.contact,
                                        selected = index == cursorIndex,
                                        onSelect = {
                                            cursorRowKey = item.key
                                            onContactSelected(item.contact)
                                        },
                                    )
                                }
                            }
                        }
                        RetroFastScroller(
                            listState = listState,
                            contentDescription = stringResource(R.string.contact_picker_fast_scroller),
                            modifier = Modifier.align(Alignment.CenterEnd).padding(vertical = 8.dp),
                        )
                    }
                    ContactPickerBackAction(
                        onClick = {
                            if (isSearching) {
                                isSearching = false
                                searchQuery = ""
                            } else {
                                onNavigateBack()
                            }
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun ContactPickerBackAction(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 2.dp, vertical = RetroScreenFooterVerticalPadding),
        contentAlignment = Alignment.CenterEnd,
    ) {
        RetroActionButton(
            key = stringResource(R.string.retro_key_b),
            label = stringResource(R.string.customized_contacts_back_action),
            onClick = onClick,
        )
    }
}

@Composable
internal fun RetroAddFavoriteScreen(
    onNavigateBack: () -> Unit,
    viewModel: ContactsViewModel = hiltViewModel(),
) {
    val contacts by viewModel.contacts.collectAsStateWithLifecycle()
    val favoritedContactIds = remember(contacts) {
        contacts.asSequence().filter { it.starred }.map { it.id }.toSet()
    }
    RetroContactPickerScreen(
        onNavigateBack = onNavigateBack,
        onContactSelected = { contact ->
            viewModel.toggleFavorite(contact.id, true)
            onNavigateBack()
        },
        excludedContactIds = favoritedContactIds,
        viewModel = viewModel,
    )
}

@Composable
private fun ContactPickerRow(
    contact: DialerContactSummary,
    selected: Boolean,
    onSelect: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth()
            .clickable(onClick = onSelect)
            .padding(horizontal = 8.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (selected) {
            RetroSelectionArrow(tint = ContactPickerInk, size = 14.dp)
            Spacer(Modifier.size(2.dp))
        } else {
            Spacer(Modifier.size(16.dp))
        }
        ContactAvatar(
            name = contact.name,
            photoUri = contact.image,
            colorKey = contactAvatarColorKey(contact.name),
            modifier = Modifier.size(42.dp).clip(PixelRoundedSquareShape),
            shape = PixelRoundedSquareShape,
        )
        ContactPickerText(
            text = contact.name,
            size = 16.sp,
            modifier = Modifier.weight(1f).padding(horizontal = 10.dp),
            maxLines = 1,
        )
    }
}

@Composable
private fun ContactPickerSectionHeader(label: String, favorite: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (favorite) {
            AppIcon(
                icon = LocalAppIcons.current.favorite,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = ContactPickerInk,
            )
            Spacer(Modifier.size(8.dp))
        }
        ContactPickerText(label, 13.sp, ContactPickerInk.copy(alpha = 0.75f))
    }
}

@Composable
private fun ContactPickerCommand(label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.End,
    ) {
        ContactPickerText(
            text = label,
            size = 18.sp,
            modifier = Modifier.clickable(onClick = onClick)
                .padding(start = 16.dp, top = 14.dp, end = 16.dp, bottom = 6.dp),
        )
    }
}

@Composable
private fun ContactPickerText(
    text: String,
    size: TextUnit,
    color: Color = ContactPickerInk,
    modifier: Modifier = Modifier,
    maxLines: Int = Int.MAX_VALUE,
    textAlign: TextAlign? = null,
) {
    Text(
        text = text.uppercase(),
        modifier = modifier,
        fontFamily = ContactPickerFont,
        fontSize = size,
        lineHeight = size * 1.15f,
        color = color,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
        textAlign = textAlign,
    )
}

private sealed class ContactPickerItem(val key: String) {
    data class Header(val label: String, val favorite: Boolean = false) : ContactPickerItem("header:$label")
    data class Contact(val contact: DialerContactSummary, val section: String) :
        ContactPickerItem("contact-${section}-${contact.id}")
}

private fun buildContactPickerItems(
    contacts: List<DialerContactSummary>,
    searching: Boolean,
    favoritesLabel: String,
): List<ContactPickerItem> = buildList {
    fun addSection(label: String, sectionContacts: List<DialerContactSummary>, favorite: Boolean = false) {
        if (sectionContacts.isEmpty()) return
        add(ContactPickerItem.Header(label, favorite))
        sectionContacts.forEach { add(ContactPickerItem.Contact(it, label)) }
    }
    if (searching) {
        contacts.forEach { add(ContactPickerItem.Contact(it, "search")) }
    } else {
        addSection(favoritesLabel, contacts.filter { it.starred }, favorite = true)
        contacts.groupBy { it.name.firstOrNull()?.uppercaseChar()?.toString() ?: "#" }
            .toSortedMap()
            .forEach { (initial, sectionContacts) -> addSection(initial, sectionContacts) }
    }
}
