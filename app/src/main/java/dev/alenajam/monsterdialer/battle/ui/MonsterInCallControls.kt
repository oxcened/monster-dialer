package dev.alenajam.monsterdialer.battle.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.Text
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.alenajam.monsterdialer.R
import dev.alenajam.monsterdialer.app.ui.RetroActionButton
import dev.alenajam.monsterdialer.app.ui.RetroMenuBorder
import dev.alenajam.monsterdialer.app.ui.RetroContextMenuDialog
import dev.alenajam.monsterdialer.app.ui.RetroContextMenuItem
import dev.alenajam.monsterdialer.app.ui.RetroDialog
import dev.alenajam.opendialer.core.common.ui.AppIcons
import dev.alenajam.opendialer.core.common.ui.AppIcon
import dev.alenajam.opendialer.feature.inCall.service.CallAudioRouteUiState
import dev.alenajam.opendialer.core.common.SharedPreferenceHelper
import dev.alenajam.opendialer.feature.inCall.R as InCallR

private val CommandFont = FontFamily(Font(R.font.ui_pixel_font))
private val PixelOperatorFont = FontFamily(Font(R.font.pixel_operator))
private val Ink = Color(0xFF202020)
private val Active = Color(0xFF5B4D8E)
private val RoundFace = Color(0xFF242541)
private val RoundActiveFace = Color(0xFFE5DDF7)

@Composable
internal fun MonsterIncomingCallControls(
    icons: AppIcons,
    controlsEnabled: Boolean,
    onHangup: () -> Unit,
    onAnswer: () -> Unit,
    onMessage: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val quickResponses = remember { SharedPreferenceHelper.getQuickResponses(context) }
    var showQuickResponses by remember { mutableStateOf(false) }
    var showCustomMessage by remember { mutableStateOf(false) }
    var customMessage by remember { mutableStateOf("") }
    val messageLabel = stringResource(InCallR.string.action_message)
    val declineLabel = stringResource(InCallR.string.action_decline)
    val answerLabel = stringResource(InCallR.string.action_answer)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(start = 20.dp, top = 6.dp, end = 20.dp, bottom = 24.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            MinimalCallControl(messageLabel, icons.message, false, controlsEnabled, { showQuickResponses = true }, Modifier.weight(1f))
            MinimalCallControl(declineLabel, icons.hangup, false, controlsEnabled, onHangup, Modifier.weight(1f))
            MinimalCallControl(answerLabel, icons.phone, false, controlsEnabled, onAnswer, Modifier.weight(1f))
        }
    }

    if (showQuickResponses) {
        RetroContextMenuDialog(
            items = quickResponses.map { response ->
                RetroContextMenuItem(response, onClick = {
                    showQuickResponses = false
                    onMessage(response)
                })
            } + RetroContextMenuItem(
                label = stringResource(InCallR.string.write_your_own),
                showCursor = false,
                onClick = {
                    showQuickResponses = false
                    showCustomMessage = true
                },
            ) + RetroContextMenuItem.cancel(stringResource(R.string.call_command_back)) {
                showQuickResponses = false
            },
            fontFamily = PixelOperatorFont,
            fontSize = 16,
            title = messageLabel,
            onDismissRequest = { showQuickResponses = false },
        )
    }

    if (showCustomMessage) {
        AlertDialog(
            onDismissRequest = { showCustomMessage = false },
            title = { Text(stringResource(InCallR.string.reply_with_message)) },
            text = { TextField(value = customMessage, onValueChange = { customMessage = it }) },
            confirmButton = {
                TextButton(
                    enabled = customMessage.isNotBlank(),
                    onClick = {
                        showCustomMessage = false
                        onMessage(customMessage)
                    },
                ) { Text(stringResource(InCallR.string.action_send)) }
            },
            dismissButton = {
                TextButton(onClick = { showCustomMessage = false }) {
                    Text(stringResource(InCallR.string.action_cancel))
                }
            },
        )
    }
}

/** MonsterDialer-specific call controls; the base OpenDialer footer remains Material-themed. */
@Composable
internal fun MonsterInCallControls(
    icons: AppIcons,
    isMuted: Boolean,
    isSpeaker: Boolean,
    audioRoutes: List<CallAudioRouteUiState>,
    isHolding: Boolean,
    canManageConference: Boolean,
    canMerge: Boolean,
    canSwap: Boolean,
    canHold: Boolean,
    showAddCall: Boolean,
    canAddCall: Boolean,
    controlsEnabled: Boolean = true,
    canHangup: Boolean = true,
    dialpadLabel: String,
    muteLabel: String,
    speakerLabel: String,
    moreLabel: String,
    endCallLabel: String,
    addCallLabel: String,
    holdLabel: String,
    mergeLabel: String,
    swapLabel: String,
    manageLabel: String,
    onHangup: () -> Unit,
    onMute: () -> Unit,
    onSpeaker: () -> Unit,
    onAudioRouteSelected: (CallAudioRouteUiState) -> Unit,
    onHold: () -> Unit,
    onAddCall: () -> Unit,
    onMerge: () -> Unit,
    onSwap: () -> Unit,
    onManageConference: () -> Unit,
    onDigitPress: (digit: Char) -> Unit,
    onDigitRelease: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val expanded = remember { mutableStateOf<CommandSection?>(null) }
    val dialpadInput = remember { mutableStateOf("") }
    val externalRoutes = audioRoutes.filter {
        it.type == android.telecom.CallAudioState.ROUTE_BLUETOOTH ||
            it.type == android.telecom.CallAudioState.ROUTE_WIRED_HEADSET
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(start = 20.dp, top = 6.dp, end = 20.dp, bottom = 24.dp),
    ) {
        GameBoyControlDeck(
            icons = icons,
            dialpadLabel = dialpadLabel,
            muteLabel = muteLabel,
            speakerLabel = speakerLabel,
            moreLabel = moreLabel,
            dialpadActive = expanded.value == CommandSection.DIALPAD,
            moreActive = expanded.value == CommandSection.MORE,
            muteActive = isMuted,
            speakerActive = isSpeaker || externalRoutes.isNotEmpty(),
            enabled = controlsEnabled,
            onDialpad = { expanded.value = CommandSection.DIALPAD },
            onMute = onMute,
            onSpeaker = {
                if (externalRoutes.isNotEmpty()) expanded.value = CommandSection.ROUTES else onSpeaker()
            },
            onMore = { expanded.value = CommandSection.MORE },
            endCallLabel = endCallLabel,
            canHangup = canHangup,
            onHangup = onHangup,
        )
    }

    when (expanded.value) {
        CommandSection.DIALPAD -> RetroKeypadDialog(
            label = dialpadLabel,
            number = dialpadInput.value,
            onDigit = { digit ->
                dialpadInput.value += digit
            },
            onDigitPress = onDigitPress,
            onDigitRelease = onDigitRelease,
            onDismissRequest = { onDigitRelease(); expanded.value = null },
        )
        CommandSection.MORE -> RetroContextMenuDialog(
            items = buildList {
                if (showAddCall) add(RetroContextMenuItem(addCallLabel, onClick = { expanded.value = null; onAddCall() }))
                if (canHold) add(RetroContextMenuItem(holdLabel, onClick = { expanded.value = null; onHold() }))
                if (canMerge) add(RetroContextMenuItem(mergeLabel, onClick = { expanded.value = null; onMerge() }))
                if (canSwap) add(RetroContextMenuItem(swapLabel, onClick = { expanded.value = null; onSwap() }))
                if (canManageConference) add(RetroContextMenuItem(manageLabel, onClick = { expanded.value = null; onManageConference() }))
                add(RetroContextMenuItem.cancel(stringResource(R.string.call_command_back)) { expanded.value = null })
            },
            fontFamily = CommandFont,
            title = moreLabel,
            onDismissRequest = { expanded.value = null },
        )
        CommandSection.ROUTES -> RetroContextMenuDialog(
            items = externalRoutes.map { route ->
                RetroContextMenuItem(route.label, onClick = {
                    expanded.value = null
                    onAudioRouteSelected(route)
                })
            } + RetroContextMenuItem.cancel(stringResource(R.string.call_command_back)) {
                expanded.value = null
            },
            fontFamily = CommandFont,
            title = speakerLabel,
            onDismissRequest = { expanded.value = null },
        )
        null -> Unit
    }
}

@Composable
private fun RetroKeypadDialog(
    label: String,
    number: String,
    onDigit: (Char) -> Unit,
    onDigitPress: (Char) -> Unit,
    onDigitRelease: () -> Unit,
    onDismissRequest: () -> Unit,
) {
    RetroDialog(
        onDismissRequest = onDismissRequest,
        modifier = Modifier.fillMaxWidth(0.82f),
    ) {
        val inputScrollState = rememberScrollState()
        LaunchedEffect(number) {
            inputScrollState.scrollTo(inputScrollState.maxValue)
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(label.uppercase(), fontFamily = CommandFont, fontSize = 18.sp, color = Ink)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(42.dp)
                    .border(2.dp, Ink, RectangleShape)
                    .padding(horizontal = 8.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                Row(
                    modifier = Modifier.horizontalScroll(inputScrollState),
                ) {
                    Text(
                        text = number,
                        fontFamily = CommandFont,
                        fontSize = 20.sp,
                        color = Ink,
                        maxLines = 1,
                        softWrap = false,
                    )
                }
            }
            stringResource(R.string.call_command_dialpad_digits).lines().forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    row.forEach { digit ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(54.dp)
                                .border(2.dp, Ink, RectangleShape)
                                .retroDtmfKey(
                                    digit = digit,
                                    onDigit = onDigit,
                                    onDigitPress = onDigitPress,
                                    onDigitRelease = onDigitRelease,
                                )
                                .semantics { role = Role.Button },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(digit.toString(), fontFamily = CommandFont, fontSize = 20.sp, color = Ink)
                        }
                    }
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(34.dp)
                    .clickable(onClick = onDismissRequest)
                    .semantics { role = Role.Button },
                contentAlignment = Alignment.Center,
            ) {
                Text(stringResource(R.string.call_command_back), fontFamily = CommandFont, fontSize = 16.sp, color = Ink)
            }
        }
    }
}

@Composable
private fun Modifier.retroDtmfKey(
    digit: Char,
    onDigit: (Char) -> Unit,
    onDigitPress: (Char) -> Unit,
    onDigitRelease: () -> Unit,
): Modifier {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    var handledOnPress by remember { mutableStateOf(false) }
    LaunchedEffect(isPressed) {
        if (isPressed) {
            handledOnPress = true
            onDigit(digit)
            onDigitPress(digit)
        } else {
            onDigitRelease()
        }
    }
    return combinedClickable(
        interactionSource = interactionSource,
        onClick = {
            if (!handledOnPress) onDigit(digit)
            handledOnPress = false
        },
    )
}

@Composable
private fun GameBoyControlDeck(
    icons: AppIcons,
    dialpadLabel: String,
    muteLabel: String,
    speakerLabel: String,
    moreLabel: String,
    dialpadActive: Boolean,
    moreActive: Boolean,
    muteActive: Boolean,
    speakerActive: Boolean,
    enabled: Boolean,
    onDialpad: () -> Unit,
    onMute: () -> Unit,
    onSpeaker: () -> Unit,
    onMore: () -> Unit,
    endCallLabel: String,
    canHangup: Boolean,
    onHangup: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            MinimalCallControl(speakerLabel, icons.speaker, speakerActive, enabled, onSpeaker, Modifier.weight(1f))
            MinimalCallControl(muteLabel, icons.mute, muteActive, enabled, onMute, Modifier.weight(1f))
            MinimalCallControl(dialpadLabel, icons.dialpad, dialpadActive, enabled, onDialpad, Modifier.weight(1f))
            MinimalCallControl(moreLabel, icons.more, moreActive, enabled, onMore, Modifier.weight(1f))
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .background(Color(0xFFC8323C), CircleShape)
                    .clickable(enabled = canHangup, onClick = onHangup)
                    .semantics { role = Role.Button },
                contentAlignment = Alignment.Center,
            ) {
                AppIcon(
                    icons.hangup,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = Color.White,
                )
            }
            Text(
                text = endCallLabel.uppercase(),
                fontFamily = CommandFont,
                fontSize = 11.sp,
                color = Ink,
            )
        }
    }
}

@Composable
private fun MinimalCallControl(
    label: String,
    icon: dev.alenajam.opendialer.core.common.ui.IconSource,
    active: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clickable(enabled = enabled, onClick = onClick)
            .semantics { role = Role.Button },
        contentAlignment = Alignment.Center,
    ) {
        val color = if (active) Active else Ink
        Column(
            modifier = Modifier.padding(vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            AppIcon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(22.dp),
                tint = if (enabled) color else color.copy(alpha = 0.35f),
            )
            Text(
                text = label.uppercase(),
                fontFamily = CommandFont,
                fontSize = 9.sp,
                color = if (enabled) color else color.copy(alpha = 0.35f),
                maxLines = 1,
            )
            if (active) {
                Box(
                    modifier = Modifier
                        .width(24.dp)
                        .height(3.dp)
                        .background(Active),
                )
            }
        }
    }
}

private enum class CommandSection { DIALPAD, MORE, ROUTES }

private data class CommandAction(
    val label: String,
    val icon: dev.alenajam.opendialer.core.common.ui.IconSource,
    val onClick: () -> Unit,
    val enabled: Boolean,
    val active: Boolean = false,
)

@Composable
private fun CommandButton(
    label: String,
    icon: dev.alenajam.opendialer.core.common.ui.IconSource,
    active: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    RetroCommandButton(label, icon, enabled, if (active) Active else Ink, onClick)
}

@Composable
private fun RetroCommandButton(
    label: String,
    icon: dev.alenajam.opendialer.core.common.ui.IconSource,
    enabled: Boolean,
    color: Color,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(38.dp)
            .clickable(enabled = enabled, onClick = onClick)
            .semantics { role = Role.Button },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = if (color == Active) {
                stringResource(R.string.call_command_cursor, label.uppercase())
            } else {
                label.uppercase()
            },
            fontFamily = CommandFont,
            fontSize = 15.sp,
            color = if (enabled) color else Ink.copy(alpha = 0.35f),
        )
    }
}

@Composable
private fun RetroCommandList(
    label: String,
    backIcon: dev.alenajam.opendialer.core.common.ui.IconSource,
    actions: List<CommandAction>,
    onClose: () -> Unit,
) {
    RetroMenuBorder(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(stringResource(R.string.call_command_section, label), fontFamily = CommandFont, fontSize = 16.sp, color = Ink)
            actions.forEach { action ->
                RetroCommandButton(action.label, action.icon, action.enabled, if (action.active) Active else Ink, {
                    action.onClick()
                    onClose()
                })
            }
            RetroCommandButton(stringResource(R.string.call_command_back), backIcon, true, Ink, onClose)
        }
    }
}

@Composable
private fun RetroDialpadPanel(
    label: String,
    backIcon: dev.alenajam.opendialer.core.common.ui.IconSource,
    onDigitPress: (digit: Char) -> Unit,
    onDigitRelease: () -> Unit,
    onClose: () -> Unit,
) {
    RetroMenuBorder(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text(stringResource(R.string.call_command_section, label), fontFamily = CommandFont, fontSize = 16.sp, color = Ink)
            Spacer(modifier = Modifier.height(8.dp))
            stringResource(R.string.call_command_dialpad_digits).lines().forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    row.forEach { digit ->
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clickable {
                                    onDigitPress(digit)
                                    onDigitRelease()
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(digit.toString(), fontFamily = CommandFont, fontSize = 20.sp, color = Ink)
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            RetroCommandButton(stringResource(R.string.call_command_back), backIcon, true, Ink, onClose)
        }
    }
}
