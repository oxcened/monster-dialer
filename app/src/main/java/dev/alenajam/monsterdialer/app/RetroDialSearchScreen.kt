package dev.alenajam.monsterdialer.app

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.alenajam.monsterdialer.R
import dev.alenajam.monsterdialer.app.ui.RetroActionButton
import dev.alenajam.monsterdialer.app.ui.RetroContextMenuItem
import dev.alenajam.monsterdialer.app.ui.RetroContextMenuOverlay
import dev.alenajam.monsterdialer.app.ui.RetroFooter
import dev.alenajam.monsterdialer.app.ui.RetroFooterAction
import dev.alenajam.monsterdialer.app.ui.RetroSearchBar
import dev.alenajam.monsterdialer.app.ui.RetroSelectableRow
import dev.alenajam.monsterdialer.app.ui.RetroScreenHorizontalPadding
import dev.alenajam.opendialer.core.common.PermissionUtils
import dev.alenajam.opendialer.core.common.getActivity
import dev.alenajam.opendialer.core.common.telecom.CallAccount
import dev.alenajam.opendialer.core.common.telecom.CallPlacementResult
import dev.alenajam.opendialer.core.common.ui.CallAccountPicker
import dev.alenajam.opendialer.data.contactsSearch.DialerSearchContact
import dev.alenajam.opendialer.feature.contactsSearch.R as SearchR
import dev.alenajam.opendialer.feature.contactsSearch.SearchContactsViewModel

private val DialSearchPaper = Color(0xFFF9F7FC)
private val DialSearchInk = Color(0xFF202020)
private val DialSearchFont = FontFamily(Font(R.font.ui_pixel_font))

/** MonsterDialer's GSC-styled dialpad, search results, and number actions. */
@Composable
internal fun RetroDialSearchScreen(
    prefilledNumber: String,
    onOpenHistory: (List<Int>) -> Unit,
    onDialpadCallStarted: () -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: SearchContactsViewModel = hiltViewModel(),
) {
    var query by rememberSaveable(prefilledNumber) { mutableStateOf(prefilledNumber) }
    val result by viewModel.result.collectAsStateWithLifecycle()
    val hasPermission by viewModel.hasRuntimePermission.collectAsStateWithLifecycle()
    var selectedContact by remember { mutableStateOf<DialerSearchContact?>(null) }
    var selectedResultId by remember { mutableStateOf<Long?>(null) }
    var showModifiers by remember { mutableStateOf(false) }
    var pendingNumber by remember { mutableStateOf<String?>(null) }
    var callAccounts by remember { mutableStateOf<List<CallAccount>?>(null) }
    val context = LocalContext.current
    val requestCallPermissions = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { permissions ->
        if (PermissionUtils.makeCallPermissions.all { permissions[it] == true }) {
            viewModel.handleCallRuntimePermissionGranted()
            pendingNumber?.let { number ->
                when (val placement = viewModel.makeCall(number)) {
                    CallPlacementResult.Placed -> onDialpadCallStarted()
                    is CallPlacementResult.AccountSelectionRequired -> callAccounts = placement.accounts
                    else -> pendingNumber = null
                }
            }
        } else {
            pendingNumber = null
        }
    }

    fun updateQuery(value: String) {
        query = value
        if (hasPermission) viewModel.searchContactsByDialpad(value)
    }

    fun makeCall(number: String) {
        when (val placement = viewModel.makeCall(number)) {
            CallPlacementResult.Placed -> onDialpadCallStarted()
            CallPlacementResult.PermissionRequired -> {
                pendingNumber = number
                requestCallPermissions.launch(PermissionUtils.makeCallPermissions)
            }
            is CallPlacementResult.AccountSelectionRequired -> {
                pendingNumber = number
                callAccounts = placement.accounts
            }
            CallPlacementResult.Unavailable -> Unit
        }
    }

    LaunchedEffect(hasPermission, query) {
        if (hasPermission) viewModel.searchContactsByDialpad(query)
    }

    callAccounts?.let { accounts ->
        CallAccountPicker(
            accounts = accounts,
            onAccountSelected = { account ->
                pendingNumber?.let { number ->
                    if (viewModel.makeCall(number, account) == CallPlacementResult.Placed) onDialpadCallStarted()
                }
                callAccounts = null
                pendingNumber = null
            },
            onDismiss = {
                callAccounts = null
                pendingNumber = null
            },
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DialSearchPaper)
            .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        RetroSearchBar(
            label = stringResource(R.string.dial_search_number_prefix),
            query = query,
            focusRequester = remember { androidx.compose.ui.focus.FocusRequester() },
            onQueryChanged = ::updateQuery,
            modifier = Modifier.padding(horizontal = RetroScreenHorizontalPadding, vertical = 8.dp),
        )

        if (!hasPermission) {
            androidx.compose.foundation.layout.Box(modifier = Modifier.weight(1f)) {
                RetroPermissionPrompt(onPermissionGranted = {
                    viewModel.handleRuntimePermissionGranted(query)
                })
            }
        } else {
            RetroSearchMatches(
                contacts = result?.contacts.orEmpty(),
                query = query,
                selectedId = selectedResultId ?: result?.contacts?.firstOrNull()?.dataId,
                onSelect = {
                    selectedResultId = it.dataId
                    selectedContact = it
                },
                modifier = Modifier.weight(1f),
            )
        }

        RetroDialpad(
            onDigit = { updateQuery(query + it) },
            onBackspace = { clear ->
                if (clear) updateQuery("")
                else if (query.isNotEmpty()) updateQuery(query.dropLast(1))
            },
            onMore = { showModifiers = true },
            moreEnabled = query.isNotEmpty(),
            modifier = Modifier.padding(horizontal = RetroScreenHorizontalPadding),
        )

        RetroFooter(
            animationKey = "dial-search:$query",
            leftAction = RetroFooterAction(
                key = stringResource(R.string.retro_key_a),
                label = stringResource(R.string.call),
                enabled = true,
                onClick = {
                    if (query.isBlank()) {
                        viewModel.getLastOutgoingNumber()?.let(::updateQuery)
                    } else {
                        makeCall(query)
                    }
                },
            ),
            onBack = onNavigateBack,
            backKey = stringResource(R.string.retro_key_b),
            backLabel = stringResource(R.string.customized_contacts_back_action),
            modifier = Modifier.padding(horizontal = RetroScreenHorizontalPadding),
        )
    }

    selectedContact?.let { contact ->
        RetroContextMenuOverlay(
            fontFamily = DialSearchFont,
            title = contact.name.ifBlank { contact.number },
            onDismissRequest = { selectedContact = null },
            items = listOf(
                RetroContextMenuItem(stringResource(R.string.call)) {
                    selectedContact = null
                    makeCall(contact.number)
                },
                RetroContextMenuItem(stringResource(SearchR.string.send_message)) {
                    selectedContact = null
                    viewModel.sendMessage(context.getActivity() as Activity, contact.number)
                },
                RetroContextMenuItem(stringResource(SearchR.string.contact_history)) {
                    selectedContact = null
                    viewModel.getHistoryIds(contact.number).takeIf { it.isNotEmpty() }?.let(onOpenHistory)
                },
                RetroContextMenuItem.cancel(stringResource(R.string.cancel)) { selectedContact = null },
            ),
        )
    }
    if (showModifiers) {
        RetroContextMenuOverlay(
            fontFamily = DialSearchFont,
            onDismissRequest = { showModifiers = false },
            items = listOf(
                RetroContextMenuItem(stringResource(SearchR.string.create_new_contact)) {
                    showModifiers = false
                    viewModel.createContact(context.getActivity() as Activity, query)
                },
                RetroContextMenuItem(stringResource(SearchR.string.add_to_a_contact)) {
                    showModifiers = false
                    viewModel.addToContact(context.getActivity() as Activity, query)
                },
                RetroContextMenuItem(stringResource(SearchR.string.send_message)) {
                    showModifiers = false
                    viewModel.sendMessage(context.getActivity() as Activity, query)
                },
                RetroContextMenuItem(
                    label = stringResource(SearchR.string.add_2_second_pause),
                    dividerBefore = true,
                ) {
                    showModifiers = false
                    updateQuery(query + ',')
                },
                RetroContextMenuItem(stringResource(SearchR.string.add_wait)) {
                    showModifiers = false
                    updateQuery(query + ';')
                },
                RetroContextMenuItem.cancel(stringResource(R.string.cancel)) { showModifiers = false },
            ),
        )
    }
}

@Composable
private fun RetroPermissionPrompt(onPermissionGranted: () -> Unit) {
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        if (PermissionUtils.contactsPermissions.all { result[it] == true }) onPermissionGranted()
    }
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(stringResource(SearchR.string.placeholder_search_permissions), fontFamily = DialSearchFont, fontSize = 16.sp, color = DialSearchInk)
        RetroActionButton(label = stringResource(SearchR.string.turn_on), onClick = {
            launcher.launch(PermissionUtils.searchPermissions)
        })
    }
}

@Composable
private fun RetroSearchMatches(
    contacts: List<DialerSearchContact>,
    query: String,
    selectedId: Long?,
    onSelect: (DialerSearchContact) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier = modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        if (contacts.isNotEmpty()) {
            item { Text(stringResource(R.string.dial_search_results), fontFamily = DialSearchFont, fontSize = 16.sp, color = DialSearchInk, modifier = Modifier.fillMaxWidth().padding(start = 24.dp, top = 6.dp)) }
        }
        items(contacts, key = { it.dataId }) { contact ->
            RetroSelectableRow(
                selected = selectedId == contact.dataId,
                onClick = { onSelect(contact) },
                modifier = Modifier.padding(horizontal = 14.dp),
            ) {
                Column(modifier = Modifier.weight(1f).padding(vertical = 4.dp)) {
                    Text(contact.name.ifBlank { contact.number }, fontFamily = DialSearchFont, fontSize = 17.sp, color = DialSearchInk, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (contact.name.isNotBlank()) Text(contact.number, fontFamily = DialSearchFont, fontSize = 13.sp, color = DialSearchInk.copy(alpha = .72f), maxLines = 1)
                }
            }
        }
        if (query.isNotBlank() && contacts.isEmpty()) item { Text(query, fontFamily = DialSearchFont, fontSize = 16.sp, color = DialSearchInk.copy(alpha = .6f), modifier = Modifier.padding(top = 24.dp)) }
    }
}

@Composable
private fun RetroDialpad(
    onDigit: (Char) -> Unit,
    onBackspace: (clear: Boolean) -> Unit,
    onMore: () -> Unit,
    moreEnabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val rows = listOf(
        listOf('1' to "", '2' to stringResource(R.string.dialpad_letters_2), '3' to stringResource(R.string.dialpad_letters_3)),
        listOf('4' to stringResource(R.string.dialpad_letters_4), '5' to stringResource(R.string.dialpad_letters_5), '6' to stringResource(R.string.dialpad_letters_6)),
        listOf('7' to stringResource(R.string.dialpad_letters_7), '8' to stringResource(R.string.dialpad_letters_8), '9' to stringResource(R.string.dialpad_letters_9)),
        listOf('*' to "", '0' to stringResource(R.string.dialpad_letters_0), '#' to ""),
    )
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        rows.forEach { row ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                row.forEach { (digit, subtitle) ->
                    RetroDialKey(
                        label = digit.toString(),
                        subtitle = subtitle,
                        onClick = { onDigit(digit) },
                        modifier = Modifier.weight(1f),
                        onLongClick = if (digit == '0') {
                            { onDigit('+') }
                        } else {
                            null
                        },
                    )
                }
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            RetroDialKey(stringResource(R.string.dial_search_more), "", onMore, Modifier.weight(1f), moreEnabled)
            RetroDialKey(
                label = "⌫",
                subtitle = "",
                onClick = { onBackspace(false) },
                modifier = Modifier.weight(1f),
                enabled = moreEnabled,
                onLongClick = { onBackspace(true) },
            )
        }
    }
}

@Composable
private fun RetroDialKey(
    label: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier,
    enabled: Boolean = true,
    onLongClick: (() -> Unit)? = null,
) {
    Box(
        modifier = modifier
            .padding(vertical = 6.dp)
            .combinedClickable(
                enabled = enabled,
                onClick = onClick,
                onLongClick = onLongClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = label,
                fontFamily = DialSearchFont,
                fontSize = 18.sp,
                color = if (enabled) DialSearchInk else DialSearchInk.copy(alpha = 0.35f),
            )
            if (subtitle.isNotEmpty()) {
                Text(
                    text = subtitle,
                    fontFamily = DialSearchFont,
                    fontSize = 10.sp,
                    color = if (enabled) DialSearchInk.copy(alpha = 0.72f) else DialSearchInk.copy(alpha = 0.25f),
                )
            }
        }
    }
}
