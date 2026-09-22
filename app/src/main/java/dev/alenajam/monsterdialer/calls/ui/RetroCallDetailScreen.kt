package dev.alenajam.monsterdialer.calls.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import dev.alenajam.monsterdialer.R
import dev.alenajam.monsterdialer.app.ui.RetroActionButton
import dev.alenajam.monsterdialer.app.ui.RetroConfirmationDialog
import dev.alenajam.monsterdialer.app.ui.RetroContextMenuItem
import dev.alenajam.monsterdialer.app.ui.RetroContextMenuOverlay
import dev.alenajam.opendialer.core.common.CommonUtils
import dev.alenajam.opendialer.core.common.PermissionUtils
import dev.alenajam.opendialer.core.common.formatRelativeTime
import dev.alenajam.opendialer.core.common.functional.EventObserver
import dev.alenajam.opendialer.core.common.telecom.CallAccount
import dev.alenajam.opendialer.core.common.telecom.CallPlacementResult
import dev.alenajam.opendialer.core.common.ui.AppIcon
import dev.alenajam.opendialer.core.common.ui.CallAccountPicker
import dev.alenajam.opendialer.core.common.ui.LocalAppIcons
import dev.alenajam.opendialer.data.calls.CallOption
import dev.alenajam.opendialer.data.calls.CallType
import dev.alenajam.opendialer.data.calls.DetailCall
import dev.alenajam.opendialer.data.calls.DialerCall
import dev.alenajam.opendialer.feature.callDetail.DialerViewModel
import dev.alenajam.opendialer.feature.callDetail.R as CallDetailR
import dev.alenajam.opendialer.feature.calls.R as CallsR

private val CallDetailPixelFont = FontFamily(Font(R.font.ui_pixel_font))
private val CallDetailInk = Color(0xFF202020)
private val CallDetailMutedInk = CallDetailInk.copy(alpha = 0.72f)

@Composable
fun RetroCallDetailScreen(
    onNavigateBack: () -> Unit,
    viewModel: DialerViewModel = hiltViewModel(),
) {
    val call by viewModel.call.observeAsState()
    val detailOptions by viewModel.detailOptions.observeAsState(emptyList())
    var actionMenuOpen by remember { mutableStateOf(false) }
    var blockConfirmationOpen by remember { mutableStateOf(false) }
    var deleteConfirmationOpen by remember { mutableStateOf(false) }
    var pendingCallNumber by remember { mutableStateOf<String?>(null) }
    var callAccounts by remember { mutableStateOf<List<CallAccount>?>(null) }

    val requestCallPermissions = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { result ->
        if (PermissionUtils.makeCallPermissions.all { result[it] == true }) {
            pendingCallNumber?.let { number ->
                when (val placementResult = viewModel.makeCall(number)) {
                    is CallPlacementResult.AccountSelectionRequired -> callAccounts = placementResult.accounts
                    else -> pendingCallNumber = null
                }
            }
        } else {
            pendingCallNumber = null
        }
    }

    fun placeCall(number: String) {
        when (val result = viewModel.makeCall(number)) {
            CallPlacementResult.PermissionRequired -> {
                pendingCallNumber = number
                requestCallPermissions.launch(PermissionUtils.makeCallPermissions)
            }
            is CallPlacementResult.AccountSelectionRequired -> {
                pendingCallNumber = number
                callAccounts = result.accounts
            }
            else -> Unit
        }
    }

    LaunchedEffect(call) { call?.let(viewModel::getDetailOptions) }

    viewModel.deletedDetailCalls.observe(
        LocalLifecycleOwner.current,
        EventObserver { onNavigateBack() },
    )
    viewModel.blockedCaller.observe(
        LocalLifecycleOwner.current,
        EventObserver { call?.let(viewModel::getDetailOptions) },
    )
    viewModel.unblockedCaller.observe(
        LocalLifecycleOwner.current,
        EventObserver { call?.let(viewModel::getDetailOptions) },
    )

    callAccounts?.let { accounts ->
        CallAccountPicker(
            accounts = accounts,
            onAccountSelected = { account ->
                val number = pendingCallNumber ?: return@CallAccountPicker
                callAccounts = null
                when (val result = viewModel.makeCall(number, account)) {
                    is CallPlacementResult.AccountSelectionRequired -> callAccounts = result.accounts
                    else -> pendingCallNumber = null
                }
            },
            onDismiss = {
                callAccounts = null
                pendingCallNumber = null
            },
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF9F7FC)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 8.dp, vertical = 6.dp),
        ) {
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(call?.childCalls.orEmpty(), key = DetailCall::id) { detailCall ->
                    RetroCallDetailRow(
                        call = detailCall,
                        isVoicemailNumber = call?.isVoicemailNumber == true,
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RetroActionButton(
                    key = stringResource(R.string.call_detail_options_key),
                    label = stringResource(R.string.contact_picker_options),
                    onClick = { actionMenuOpen = true },
                )
                RetroActionButton(
                    key = stringResource(R.string.call_detail_back_key),
                    label = stringResource(R.string.back),
                    onClick = onNavigateBack,
                )
            }
        }

        if (blockConfirmationOpen) {
            val caller = call?.contactInfo?.name?.takeIf { it.isNotBlank() }
                ?: call?.contactInfo?.number.orEmpty()
            RetroConfirmationDialog(
                title = stringResource(CallDetailR.string.block_confirmation_title, caller),
                message = stringResource(CallDetailR.string.block_confirmation_message),
                noLabel = stringResource(CallDetailR.string.cancel),
                yesLabel = stringResource(CallDetailR.string.blockThisCaller),
                fontFamily = CallDetailPixelFont,
                onDismissRequest = { blockConfirmationOpen = false },
                onConfirm = {
                    blockConfirmationOpen = false
                    call?.let(viewModel::blockCaller)
                },
            )
        } else if (deleteConfirmationOpen) {
            RetroConfirmationDialog(
                title = stringResource(CallsR.string.delete_call_title),
                message = stringResource(CallsR.string.delete_call_message),
                noLabel = stringResource(CallDetailR.string.cancel),
                yesLabel = stringResource(CallDetailR.string.delete),
                fontFamily = CallDetailPixelFont,
                onDismissRequest = { deleteConfirmationOpen = false },
                onConfirm = {
                    deleteConfirmationOpen = false
                    call?.let(viewModel::deleteCalls)
                },
            )
        } else if (actionMenuOpen) {
            RetroCallDetailActionMenu(
                call = call,
                options = detailOptions,
                onDismiss = { actionMenuOpen = false },
                onCall = { number ->
                    actionMenuOpen = false
                    placeCall(number)
                },
                onSendMessage = {
                    actionMenuOpen = false
                    viewModel.sendMessage()
                },
                onCopyNumber = {
                    actionMenuOpen = false
                    call?.let(viewModel::copyNumber)
                },
                onEditNumber = {
                    actionMenuOpen = false
                    call?.let(viewModel::editNumberBeforeCall)
                },
                onBlock = {
                    actionMenuOpen = false
                    blockConfirmationOpen = true
                },
                onUnblock = {
                    actionMenuOpen = false
                    call?.let(viewModel::unblockCaller)
                },
                onDelete = {
                    actionMenuOpen = false
                    deleteConfirmationOpen = true
                },
            )
        }
    }
}

@Composable
private fun RetroCallDetailRow(
    call: DetailCall,
    isVoicemailNumber: Boolean,
) {
    val label = if (isVoicemailNumber) {
        stringResource(CallDetailR.string.voicemail_call)
    } else when (call.type) {
        CallType.OUTGOING -> stringResource(CallDetailR.string.outgoing_call)
        CallType.INCOMING, CallType.ANSWERED_EXTERNALLY -> stringResource(CallDetailR.string.incoming_call)
        CallType.MISSED -> stringResource(CallDetailR.string.missed_call)
        CallType.VOICEMAIL -> stringResource(CallDetailR.string.voicemail_call)
        CallType.REJECTED -> stringResource(CallDetailR.string.rejected_call)
        CallType.BLOCKED -> stringResource(CallDetailR.string.blocked_call)
    }
    val icon = when (call.type) {
        CallType.INCOMING, CallType.ANSWERED_EXTERNALLY -> LocalAppIcons.current.callReceived
        CallType.OUTGOING -> LocalAppIcons.current.callMade
        CallType.MISSED, CallType.REJECTED -> LocalAppIcons.current.callMissed
        CallType.VOICEMAIL -> LocalAppIcons.current.voicemail
        CallType.BLOCKED -> LocalAppIcons.current.blockCall
    }
    val color = if (!isVoicemailNumber && call.type == CallType.MISSED) Color(0xFFB3261E) else CallDetailInk

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 2.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppIcon(icon = icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
        Column(modifier = Modifier.weight(1f).padding(start = 10.dp, end = 8.dp)) {
            RetroDetailText(label, 16.sp, color = color, maxLines = 1)
            RetroDetailText(formatRelativeTime(call.date), 13.sp, color = CallDetailMutedInk, maxLines = 1)
        }
        if (call.duration > 0) {
            RetroDetailText(
                CommonUtils.getDurationTimeStringMinimal(call.duration * 1000),
                13.sp,
                color = CallDetailMutedInk,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun RetroCallDetailActionMenu(
    call: DialerCall?,
    options: List<CallOption>,
    onDismiss: () -> Unit,
    onCall: (String) -> Unit,
    onSendMessage: () -> Unit,
    onCopyNumber: () -> Unit,
    onEditNumber: () -> Unit,
    onBlock: () -> Unit,
    onUnblock: () -> Unit,
    onDelete: () -> Unit,
) {
    val number = call?.number
    val isAnonymous = call?.isAnonymous() == true
    val canBlock = options.any { it.id == CallOption.ID_BLOCK_CALLER }
    val canUnblock = options.any { it.id == CallOption.ID_UNBLOCK_CALLER }
    val items = buildList {
        if (!isAnonymous && number != null) {
            add(RetroContextMenuItem(stringResource(R.string.call)) { onCall(number) })
            add(RetroContextMenuItem(stringResource(CallDetailR.string.send_message), onClick = onSendMessage))
            add(RetroContextMenuItem(stringResource(CallDetailR.string.copy_number), onClick = onCopyNumber))
            add(RetroContextMenuItem(stringResource(CallDetailR.string.edit_number_before_call), onClick = onEditNumber))
        }
        if (canBlock) add(RetroContextMenuItem(stringResource(CallDetailR.string.blockThisCaller), onClick = onBlock))
        if (canUnblock) add(RetroContextMenuItem(stringResource(CallDetailR.string.unblockThisCaller), onClick = onUnblock))
        add(RetroContextMenuItem(stringResource(CallDetailR.string.delete), onClick = onDelete))
        add(RetroContextMenuItem.cancel(stringResource(CallDetailR.string.cancel), onDismiss))
    }
    RetroContextMenuOverlay(
        items = items,
        fontFamily = CallDetailPixelFont,
        onDismissRequest = onDismiss,
    )
}

@Composable
private fun RetroDetailText(
    text: String,
    size: androidx.compose.ui.unit.TextUnit,
    modifier: Modifier = Modifier,
    color: Color = CallDetailInk,
    maxLines: Int = Int.MAX_VALUE,
) {
    androidx.compose.material3.Text(
        text = text.uppercase(),
        modifier = modifier,
        fontFamily = CallDetailPixelFont,
        fontSize = size,
        lineHeight = size * 1.15f,
        color = color,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
    )
}
