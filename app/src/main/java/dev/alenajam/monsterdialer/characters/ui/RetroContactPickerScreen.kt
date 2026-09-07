package dev.alenajam.monsterdialer.characters.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.alenajam.monsterdialer.R
import dev.alenajam.monsterdialer.app.ui.RetroActionButton
import dev.alenajam.monsterdialer.app.ui.RetroSearchBar
import dev.alenajam.monsterdialer.app.ui.RetroSearchButton
import dev.alenajam.monsterdialer.app.ui.RetroSelectableRow
import dev.alenajam.monsterdialer.app.ui.RetroScreenHorizontalPadding
import dev.alenajam.monsterdialer.app.ui.RetroTextBox
import dev.alenajam.monsterdialer.app.ui.RetroFooter
import dev.alenajam.monsterdialer.app.ui.RetroFooterAction
import dev.alenajam.opendialer.core.common.PermissionUtils
import dev.alenajam.opendialer.data.contacts.DialerContactSummary
import dev.alenajam.opendialer.feature.contacts.ContactsViewModel

private val ContactPickerPixelFont = FontFamily(Font(R.font.ui_pixel_font))

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
    var searchSelectionId by remember { mutableStateOf<Int?>(null) }
    var selectedContactId by remember { mutableStateOf<Int?>(null) }
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
            .sortedBy { it.name }
    }
    val listItems = remember(availableContacts, favoritesLabel, isSearching) {
        buildList {
            if (!isSearching) {
                val favorites = availableContacts.filter { it.starred }
                if (favorites.isNotEmpty()) {
                    add(RetroContactPickerItem.Header(favoritesLabel))
                    addAll(favorites.map { RetroContactPickerItem.Contact(it) })
                }
            }
            if (isSearching) {
                addAll(availableContacts.map { RetroContactPickerItem.Contact(it) })
            } else {
                availableContacts
                    .groupBy { it.name.firstOrNull()?.uppercaseChar() ?: '#' }
                    .toSortedMap()
                    .forEach { (letter, contactsForLetter) ->
                        add(RetroContactPickerItem.Header(letter.toString()))
                        addAll(contactsForLetter.map { RetroContactPickerItem.Contact(it) })
                    }
            }
        }
    }
    val listState = rememberLazyListState()
    LaunchedEffect(isSearching, searchQuery, availableContacts) {
        if (isSearching && searchSelectionId !in availableContacts.map { it.id }) {
            searchSelectionId = availableContacts.firstOrNull()?.id
        }
    }
    LaunchedEffect(availableContacts) {
        if (selectedContactId !in availableContacts.map { it.id }) {
            selectedContactId = availableContacts.firstOrNull()?.id
        }
    }
    LaunchedEffect(isSearching) {
        if (isSearching) {
            searchFocusRequester.requestFocus()
            keyboardController?.show()
        } else {
            focusManager.clearFocus()
            keyboardController?.hide()
        }
    }
    val cursorId = if (isSearching) searchSelectionId else selectedContactId
    val cursorIndex = listItems.indexOfFirst { item ->
        item is RetroContactPickerItem.Contact && item.contact.id == cursorId
    }
    val selectedSearchContact = availableContacts.firstOrNull { it.id == searchSelectionId }

    Scaffold { contentPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .padding(horizontal = RetroScreenHorizontalPadding)
                .background(MaterialTheme.colorScheme.background),
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                if (!hasPermission) {
                    PermissionPrompt(
                        onRequestPermission = {
                            requestPermissions.launch(PermissionUtils.contactsPermissions)
                        },
                    )
                } else {
                    if (isSearching) {
                        RetroSearchBar(
                            label = stringResource(R.string.contact_picker_search_name_prefix),
                            query = searchQuery,
                            focusRequester = searchFocusRequester,
                            onQueryChanged = { searchQuery = it },
                        )
                    } else {
                        RetroSearchButton(
                            label = stringResource(R.string.contact_picker_search),
                            onClick = { isSearching = true },
                        )
                    }
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        contentPadding = PaddingValues(vertical = 4.dp),
                    ) {
                        itemsIndexed(listItems, key = { index, item -> "${item.key}:$index" }) { index, item ->
                            when (item) {
                                is RetroContactPickerItem.Header -> RetroContactPickerHeaderRow(item.label)
                                is RetroContactPickerItem.Contact -> RetroContactPickerRow(
                                    contact = item.contact,
                                    showCursor = index == cursorIndex,
                                    onClick = {
                                        selectedContactId = item.contact.id
                                        if (isSearching) searchSelectionId = item.contact.id
                                        onContactSelected(item.contact)
                                    },
                                )
                            }
                        }
                    }
                    RetroFooter(
                        message = when {
                            !isSearching -> stringResource(R.string.contact_picker_prompt)
                            searchQuery.isBlank() -> stringResource(R.string.contact_picker_search_empty_prompt)
                            else -> pluralStringResource(
                                R.plurals.contact_picker_found_count,
                                availableContacts.size,
                                availableContacts.size,
                            )
                        },
                        animationKey = "contact-picker:$isSearching:$searchQuery:${availableContacts.size}",
                        backKey = stringResource(R.string.retro_key_b),
                        backLabel = stringResource(R.string.customized_contacts_back_action),
                        onBack = {
                            if (isSearching) {
                                isSearching = false
                                searchQuery = ""
                                searchSelectionId = null
                            } else {
                                onNavigateBack()
                            }
                        },
                        leftAction = if (isSearching) RetroFooterAction(
                            key = stringResource(R.string.retro_key_a),
                            label = stringResource(R.string.contact_picker_open_action),
                            enabled = selectedSearchContact != null,
                            onClick = { selectedSearchContact?.let(onContactSelected) },
                        ) else null,
                    )
                }
            }
        }
    }
}

@Composable
private fun RetroContactPickerHeaderRow(label: String) {
    Text(
        text = label.uppercase(),
        modifier = Modifier.fillMaxWidth().padding(top = 5.dp, bottom = 2.dp),
        fontFamily = ContactPickerPixelFont,
        fontSize = 13.sp,
        color = RetroInk,
    )
}

@Composable
private fun RetroContactPickerRow(
    contact: DialerContactSummary,
    showCursor: Boolean,
    onClick: () -> Unit,
) {
    RetroSelectableRow(
        selected = showCursor,
        onClick = onClick,
    ) {
        Text(
            text = contact.name.uppercase(),
            fontFamily = ContactPickerPixelFont,
            fontSize = 18.sp,
            color = RetroInk,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun PermissionPrompt(onRequestPermission: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(R.string.favorites_permission_prompt),
            fontFamily = ContactPickerPixelFont,
            fontSize = 16.sp,
            color = Color(0xFF202020),
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.turn_on).uppercase(),
            modifier = Modifier.clickable(onClick = onRequestPermission),
            fontFamily = ContactPickerPixelFont,
            fontSize = 16.sp,
            color = Color(0xFF202020),
        )
    }
}

private sealed class RetroContactPickerItem(val key: String) {
    data class Header(val label: String) : RetroContactPickerItem("header:$label")
    data class Contact(val contact: DialerContactSummary) : RetroContactPickerItem("contact:${contact.id}")
}
