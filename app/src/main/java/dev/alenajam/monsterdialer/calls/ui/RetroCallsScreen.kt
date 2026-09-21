package dev.alenajam.monsterdialer.calls.ui

import android.text.format.DateUtils
import android.provider.ContactsContract

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import dev.alenajam.monsterdialer.R
import dev.alenajam.monsterdialer.app.ui.RetroContextMenuItem
import dev.alenajam.monsterdialer.app.ui.RetroContextMenuOverlay
import dev.alenajam.monsterdialer.app.ui.PixelRoundedSquareShape
import dev.alenajam.monsterdialer.app.ui.RetroConfirmationDialog
import dev.alenajam.monsterdialer.app.ui.RetroSearchButton
import dev.alenajam.monsterdialer.app.ui.RetroScreenBottomContentPadding
import dev.alenajam.monsterdialer.app.ui.RetroScreenHorizontalPadding
import dev.alenajam.monsterdialer.app.ui.RetroSelectionArrow
import dev.alenajam.monsterdialer.app.ui.RetroSelectionArrowSize
import dev.alenajam.opendialer.core.common.formatRelativeTime
import dev.alenajam.opendialer.core.common.ui.AppIcon
import dev.alenajam.opendialer.core.common.ui.ContactAvatar
import dev.alenajam.opendialer.core.common.ui.LocalAppIcons
import dev.alenajam.opendialer.core.common.ui.contactAvatarColorKey
import dev.alenajam.opendialer.data.calls.CallType
import dev.alenajam.opendialer.data.calls.DialerCall
import dev.alenajam.opendialer.data.contacts.DialerContact
import dev.alenajam.opendialer.feature.calls.CallsViewModel
import dev.alenajam.opendialer.feature.calls.R as CallsR
import java.time.ZoneId
import java.time.LocalDate

private val CallLogPixelFont = FontFamily(Font(R.font.ui_pixel_font))
private val CallLogInk = Color(0xFF202020)

private enum class RetroCallFilter(val labelRes: Int) {
    All(CallsR.string.filter_all),
    Missed(CallsR.string.filter_missed),
    Contacts(CallsR.string.filter_contacts),
}

@Composable
fun RetroCallsScreen(
    onOpenHistory: (List<Int>) -> Unit,
    onOpenContacts: () -> Unit,
    onAddFavorite: () -> Unit,
    onEditNumberBeforeCall: (String) -> Unit,
    favoritesOnly: Boolean = false,
    viewModel: CallsViewModel = hiltViewModel(),
    monsterCallLogViewModel: MonsterCallLogViewModel = hiltViewModel(),
) {
    val calls by viewModel.calls.collectAsStateWithLifecycle()
    val assignmentVersion by monsterCallLogViewModel.assignmentVersion.collectAsStateWithLifecycle()
    val monsterArtworkByCallId by monsterCallLogViewModel.artworkByCallId.collectAsStateWithLifecycle()
    val monsterArtworkByContactNumber by monsterCallLogViewModel.artworkByContactNumber.collectAsStateWithLifecycle()
    val journalEntries by monsterCallLogViewModel.journalEntries.collectAsStateWithLifecycle()
    val artworkPriority by monsterCallLogViewModel.artworkPriority.collectAsStateWithLifecycle()
    val favorites by viewModel.favorites.collectAsStateWithLifecycle()
    var filter by remember { mutableStateOf(RetroCallFilter.All) }
    val filteredCalls = remember(calls, filter) {
        calls.filter { call ->
            when (filter) {
                RetroCallFilter.All -> true
                RetroCallFilter.Missed -> call.type == CallType.MISSED || call.type == CallType.REJECTED
                RetroCallFilter.Contacts -> call.isContactSaved()
            }
        }
    }
    val callsByDate = remember(filteredCalls) {
        filteredCalls.groupBy { call ->
            call.date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
        }
    }
    val defaultCallId = callsByDate.values.firstOrNull()?.firstOrNull()?.id
    val defaultFavoriteId = favorites.firstOrNull()?.id
    var filterDialogOpen by remember { mutableStateOf(false) }
    var selectedCall by remember { mutableStateOf<DialerCall?>(null) }
    var selectedFavorite by remember { mutableStateOf<DialerContact?>(null) }
    var callMenuOpen by remember { mutableStateOf(false) }
    var favoriteMenuOpen by remember { mutableStateOf(false) }
    val selectedCallId = selectedCall?.id
        ?.takeIf { selectedId -> calls.any { it.id == selectedId } }
        ?: defaultCallId

    androidx.compose.runtime.LaunchedEffect(calls, assignmentVersion, journalEntries, favorites, artworkPriority) {
        monsterCallLogViewModel.refresh(
            calls = calls,
            favoriteNumbers = favorites.map { it.number }.toSet(),
            priority = artworkPriority,
        )
    }

    LifecycleEventEffect(Lifecycle.Event.ON_START) {
        viewModel.startCache()
    }
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) {
        viewModel.stopCache()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF9F7FC)),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = RetroScreenHorizontalPadding,
                top = 0.dp,
                end = RetroScreenHorizontalPadding,
                bottom = RetroScreenBottomContentPadding,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (!favoritesOnly) {
                item {
                    RetroCallLogMenu(
                        filter = filter,
                        onOpenFilterDialog = { filterDialogOpen = true },
                    )
                }
                callsByDate.forEach { (date, callsForDate) ->
                    item(key = "calls-$date") {
                        RetroCallLogGroup(
                            calls = callsForDate,
                            monsterArtworkByCallId = monsterArtworkByCallId,
                            selectedCallId = selectedCallId,
                            onOpenHistory = onOpenHistory,
                            onOpenContextMenu = {
                                selectedCall = it
                                callMenuOpen = true
                            },
                        )
                    }
                }
            } else {
                item {
                    RetroFavoritesList(
                        favorites = favorites,
                        monsterArtworkByContactNumber = monsterArtworkByContactNumber,
                        selectedFavoriteId = selectedFavorite?.id ?: defaultFavoriteId,
                        onOpenContextMenu = {
                            selectedFavorite = it
                            favoriteMenuOpen = true
                        },
                        onAddFavorite = onAddFavorite,
                    )
                }
            }
        }

        if (filterDialogOpen) {
            RetroContextMenuOverlay(
                    modifier = Modifier.fillMaxWidth(0.68f),
                    fontFamily = CallLogPixelFont,
                    onDismissRequest = { filterDialogOpen = false },
                    items = RetroCallFilter.entries.map { item ->
                        RetroContextMenuItem(
                            label = stringResource(item.labelRes),
                            showCursor = filter == item,
                        ) {
                            filter = item
                            filterDialogOpen = false
                        }
                    } + RetroContextMenuItem(
                        label = stringResource(CallsR.string.cancel),
                    ) {
                        filterDialogOpen = false
                    },
            )
        }
        if (callMenuOpen) selectedCall?.let { call ->
            RetroCallActionMenu(
                call = call,
                onDismiss = { callMenuOpen = false },
                onOpenHistory = {
                    callMenuOpen = false
                    onOpenHistory(call.childCalls.map { it.id })
                },
            )
        }
        if (favoriteMenuOpen) selectedFavorite?.let { favorite ->
            RetroFavoriteActionMenu(
                onDismiss = { favoriteMenuOpen = false },
                onCall = {
                    favoriteMenuOpen = false
                    viewModel.makeCall(favorite.number)
                },
                onRemove = {
                    favoriteMenuOpen = false
                    viewModel.unstarContact(favorite.id)
                },
            )
        }
    }
}

@Composable
private fun RetroCallLogGroup(
    calls: List<DialerCall>,
    monsterArtworkByCallId: Map<Int, MonsterCallLogArtwork>,
    selectedCallId: Int?,
    onOpenHistory: (List<Int>) -> Unit,
    onOpenContextMenu: (DialerCall) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        RetroCallText(
            text = retroRelativeTime(calls.first().date, stringResource(R.string.call_log_just_now)),
            size = 13.sp,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        )
        Spacer(Modifier.height(4.dp))
        calls.forEach { call ->
            RetroCallLogRow(
                call = call,
                monsterArtwork = monsterArtworkByCallId[call.id],
                selected = selectedCallId == call.id,
                onOpenHistory = { onOpenHistory(call.childCalls.map { it.id }) },
                onOpenContextMenu = { onOpenContextMenu(call) },
            )
        }
    }
}

@Composable
private fun RetroCallLogMenu(
    filter: RetroCallFilter,
    onOpenFilterDialog: () -> Unit,
) {
    RetroSearchButton(
        label = stringResource(R.string.contact_picker_options),
        onClick = onOpenFilterDialog,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun RetroFavoritesList(
    favorites: List<DialerContact>,
    monsterArtworkByContactNumber: Map<String, MonsterCallLogArtwork>,
    selectedFavoriteId: Int?,
    onOpenContextMenu: (DialerContact) -> Unit,
    onAddFavorite: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RetroSearchButton(
                label = stringResource(CallsR.string.add),
                onClick = onAddFavorite,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        favorites.forEach { favorite ->
            RetroFavoriteRow(
                favorite = favorite,
                monsterArtwork = monsterArtworkByContactNumber[favorite.number],
                selected = selectedFavoriteId == favorite.id,
                onOpenContextMenu = { onOpenContextMenu(favorite) },
            )
        }
    }
}

@Composable
private fun RetroFavoriteRow(
    favorite: DialerContact,
    monsterArtwork: MonsterCallLogArtwork?,
    selected: Boolean,
    onOpenContextMenu: () -> Unit,
) {
    val phoneType = ContactsContract.CommonDataKinds.Phone.getTypeLabel(
        LocalResources.current,
        favorite.phoneType,
        favorite.phoneLabel,
    ).toString()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpenContextMenu)
            .padding(horizontal = 8.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (selected) {
            RetroSelectionArrow(tint = CallLogInk, size = 14.dp)
            Spacer(Modifier.size(2.dp))
        } else {
            Spacer(Modifier.size(16.dp))
        }
        if (monsterArtwork != null) {
            MonsterCallLogAvatar(monsterArtwork)
        } else {
            ContactAvatar(
                name = favorite.name,
                photoUri = favorite.image,
                colorKey = contactAvatarColorKey(favorite.name, favorite.number),
                fallbackIcon = LocalAppIcons.current.person,
                shape = PixelRoundedSquareShape,
                modifier = Modifier.size(42.dp),
            )
        }
        Column(modifier = Modifier.weight(1f).padding(horizontal = 10.dp)) {
            RetroCallText(favorite.name, 16.sp, maxLines = 1)
            RetroCallText(
                stringResource(R.string.favorite_phone_type_number, phoneType, favorite.number),
                13.sp,
                maxLines = 1,
                color = CallLogInk.copy(alpha = 0.75f),
            )
        }
    }

}

@Composable
private fun RetroFavoriteActionMenu(
    onDismiss: () -> Unit,
    onCall: () -> Unit,
    onRemove: () -> Unit,
) {
    RetroContextMenuOverlay(
            modifier = Modifier.fillMaxWidth(0.68f),
            fontFamily = CallLogPixelFont,
            onDismissRequest = onDismiss,
            items = listOf(
                RetroContextMenuItem(stringResource(R.string.call), onClick = onCall),
                RetroContextMenuItem(stringResource(CallsR.string.remove), onClick = onRemove),
                RetroContextMenuItem.cancel(stringResource(CallsR.string.cancel), onDismiss),
            ),
    )
}

@Composable
private fun RetroCallLogRow(
    call: DialerCall,
    monsterArtwork: MonsterCallLogArtwork?,
    selected: Boolean,
    onOpenHistory: () -> Unit,
    onOpenContextMenu: () -> Unit,
) {
    val viewModel: CallsViewModel = hiltViewModel()
    var isBlocked by remember(call.id) { mutableStateOf(false) }
    val title = call.contactInfo.name?.takeIf { it.isNotBlank() }
        ?: call.contactInfo.formattedNumber
        ?: call.contactInfo.number.orEmpty()
    val resources = LocalResources.current
    val locale = LocalConfiguration.current.locales[0]
    val callDate = call.date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
    val relativeTime = if (callDate == LocalDate.now()) {
        retroRelativeTime(call.date, stringResource(R.string.call_log_just_now))
    } else {
        java.text.DateFormat.getTimeInstance(java.text.DateFormat.SHORT, locale).format(call.date)
    }
    val callLabel = if (isBlocked) {
        stringResource(CallsR.string.blocked)
    } else {
        call.contactInfo.type
            ?.takeIf { call.isContactSaved() }
            ?.let {
                ContactsContract.CommonDataKinds.Phone.getTypeLabel(
                    resources,
                    it,
                    call.contactInfo.label,
                ).toString()
            }
            .orEmpty()
    }
    val subtitle = if (callLabel.isBlank()) {
        relativeTime
    } else {
        stringResource(CallsR.string.call_log_type_time, callLabel, relativeTime)
    }
    val number = call.contactInfo.number

    androidx.compose.runtime.LaunchedEffect(call.id) {
        viewModel.getBlockStatus(call) { isBlocked = it }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                number?.let { viewModel.getBlockStatus(call) { isBlocked = it } }
                onOpenContextMenu()
            }
            .padding(horizontal = 8.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (selected) {
            RetroSelectionArrow(tint = CallLogInk, size = 14.dp)
            Spacer(Modifier.size(2.dp))
        } else {
            Spacer(Modifier.size(16.dp))
        }
        if (monsterArtwork != null) {
            MonsterCallLogAvatar(monsterArtwork)
        } else {
            ContactAvatar(
                name = call.contactInfo.name,
                photoUri = call.contactInfo.photoUri,
                colorKey = contactAvatarColorKey(call.contactInfo.name, call.contactInfo.number),
                fallbackIcon = LocalAppIcons.current.person,
                shape = PixelRoundedSquareShape,
                modifier = Modifier.size(42.dp),
            )
        }
        Column(modifier = Modifier.weight(1f).padding(horizontal = 10.dp)) {
            RetroCallText(title, 16.sp, maxLines = 1)
            RetroCallText(
                subtitle,
                13.sp,
                maxLines = 1,
                color = CallLogInk.copy(alpha = 0.75f),
            )
        }
    }

}

@Composable
private fun RetroCallActionMenu(
    call: DialerCall,
    onDismiss: () -> Unit,
    onOpenHistory: () -> Unit,
) {
    val viewModel: CallsViewModel = hiltViewModel()
    var deleteConfirmationOpen by remember { mutableStateOf(false) }
    val number = call.contactInfo.number

    if (deleteConfirmationOpen) {
        RetroConfirmationDialog(
            title = stringResource(CallsR.string.delete_call_title),
            message = stringResource(CallsR.string.delete_call_message),
            noLabel = stringResource(CallsR.string.cancel),
            yesLabel = stringResource(CallsR.string.delete),
            fontFamily = CallLogPixelFont,
            onDismissRequest = { deleteConfirmationOpen = false },
            onConfirm = {
                deleteConfirmationOpen = false
                onDismiss()
                viewModel.deleteCall(call)
            },
        )
    } else {
        val menuItems = buildList {
            if (number != null) {
                add(RetroContextMenuItem(label = stringResource(R.string.call), onClick = {
                    onDismiss()
                    viewModel.makeCall(number)
                }))
            }
            add(RetroContextMenuItem(label = stringResource(CallsR.string.history), onClick = { onOpenHistory() }))
            add(RetroContextMenuItem(label = stringResource(CallsR.string.delete), onClick = { deleteConfirmationOpen = true }))
            add(RetroContextMenuItem.cancel(stringResource(CallsR.string.cancel), onDismiss))
        }
        RetroContextMenuOverlay(
                modifier = Modifier.fillMaxWidth(0.82f),
                fontFamily = CallLogPixelFont,
                onDismissRequest = onDismiss,
                items = menuItems,
        )
    }
}

@Composable
private fun RetroCallText(
    text: String,
    size: androidx.compose.ui.unit.TextUnit,
    modifier: Modifier = Modifier,
    maxLines: Int = Int.MAX_VALUE,
    color: Color = CallLogInk,
) {
    androidx.compose.material3.Text(
        text = text.uppercase(),
        modifier = modifier,
        fontFamily = CallLogPixelFont,
        fontSize = size,
        lineHeight = size * 1.15f,
        color = color,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
    )
}

private fun retroRelativeTime(date: java.util.Date, justNowLabel: String): String {
    val age = System.currentTimeMillis() - date.time
    return if (age in 0 until DateUtils.MINUTE_IN_MILLIS) {
        justNowLabel
    } else {
        formatRelativeTime(date)
    }
}
