package dev.alenajam.monsterdialer.battle.ui

import android.app.Activity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.alenajam.monsterdialer.battle.data.AssignedCharacterEncounterFactory
import dev.alenajam.monsterdialer.analytics.MonsterAnalytics
import dev.alenajam.monsterdialer.characters.data.RadiantVariantUnlockNotifier
import dev.alenajam.monsterdialer.R
import dev.alenajam.monsterdialer.battle.data.BattleEncounter
import dev.alenajam.monsterdialer.battle.ui.BattleScreen
import dev.alenajam.monsterdialer.app.ui.rememberMonsterIcons
import dev.alenajam.monsterdialer.app.ui.rememberMonsterTypography
import dev.alenajam.monsterdialer.onlineprofiles.data.OnlineOpponentResolver
import dev.alenajam.opendialer.core.common.getActivity
import dev.alenajam.opendialer.core.common.ui.AppProviders
import dev.alenajam.opendialer.core.common.ui.AppTheme
import dev.alenajam.opendialer.core.common.ui.AppThemeExtension
import dev.alenajam.opendialer.core.common.ui.InCallUI
import dev.alenajam.opendialer.feature.inCall.ui.CallStatus
import dev.alenajam.opendialer.feature.inCall.ui.InCallDetails
import dev.alenajam.opendialer.feature.inCall.ui.InCallViewModel
import dev.alenajam.opendialer.feature.inCall.ui.ManageConferenceSheet
import dev.alenajam.opendialer.feature.inCall.ui.SecondaryCallBanner
import dev.alenajam.opendialer.feature.inCall.R as InCallR
import javax.inject.Inject
import kotlinx.coroutines.withTimeoutOrNull

class MonsterInCallUI @Inject constructor(
    private val encounterFactory: AssignedCharacterEncounterFactory,
    private val radiantUnlockNotifier: RadiantVariantUnlockNotifier,
    private val onlineOpponentResolver: OnlineOpponentResolver,
    private val analytics: MonsterAnalytics,
) : InCallUI {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val viewModel: InCallViewModel = viewModel()
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        val durationMillis by viewModel.activeCallDuration.collectAsStateWithLifecycle(0L)
        val onlineOpponentCacheVersion by onlineOpponentResolver.cacheVersion.collectAsStateWithLifecycle()
        val context = LocalContext.current
        val unknownCallerName = stringResource(R.string.unknown)
        val hasSecondaryCall = uiState.hasSecondaryCall
        val secondaryCallerName = uiState.secondaryCallerName
        val canSwap = hasSecondaryCall
        val canManageConference = uiState.canManageConference
        val canAddCall = uiState.canAddCall && !hasSecondaryCall
        val canHold = uiState.canHold && !canSwap
        val showSplitInManage = canManageConference && !hasSecondaryCall
        var showManageSheet by remember { mutableStateOf(false) }
        var hasObservedCall by remember { mutableStateOf(false) }
        var preparedCallId by remember { mutableStateOf<String?>(null) }
        var encounter by remember { mutableStateOf<BattleEncounter?>(null) }
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val radiantUnlockSnackbar = remember { SnackbarHostState() }
        val canStartBattle = true

        LaunchedEffect(uiState.status) {
            if (uiState.status == CallStatus.IDLE) {
                if (hasObservedCall) {
                    encounterFactory.clearCachedEncounter()
                }
                preparedCallId = null
                encounter = null
            } else {
                hasObservedCall = true
            }
        }

        LaunchedEffect(uiState.status, uiState.callId, uiState.callerNumber, onlineOpponentCacheVersion) {
            if (!canStartBattle || uiState.status == CallStatus.IDLE) return@LaunchedEffect
            if (preparedCallId != uiState.callId) {
                encounter = null
                onlineOpponentResolver.refreshForNumberAsync(uiState.callerNumber)?.let { refresh ->
                    withTimeoutOrNull(OnlineProfileRefreshWaitMillis) { refresh.await() }
                }
                preparedCallId = uiState.callId
            }
            encounter = encounterFactory.forCall(
                callId = uiState.callId,
                contactKey = uiState.callerNumber,
                callerName = uiState.callerName.ifBlank {
                    uiState.callerNumber.ifBlank { unknownCallerName }
                },
                isAnonymous = uiState.callerName.isBlank() && uiState.callerNumber.isBlank(),
            )
        }

        LaunchedEffect(encounter?.id) {
            encounter?.let { preparedEncounter ->
                analytics.callEncounterShown(
                    encounterType = preparedEncounter.type.name.lowercase(),
                    hasCustomContent = preparedEncounter.player.frontSprite is dev.alenajam.monsterdialer.battle.data.BattleVisualAsset.LocalFile ||
                        preparedEncounter.enemy?.frontSprite is dev.alenajam.monsterdialer.battle.data.BattleVisualAsset.LocalFile,
                )
            }
        }

        LaunchedEffect(canManageConference) {
            if (!canManageConference) showManageSheet = false
        }

        // Here you provide the MonsterDialer specific UI!
        AppProviders(
            icons = rememberMonsterIcons(forceLightDialpad = true),
            themeExtension = AppThemeExtension(
                typography = rememberMonsterTypography(MaterialTheme.typography)
                // backgroundPainter = { painterResource(R.drawable.monster_bg) }
            )
        ) {
            AppTheme(darkTheme = false) {
            if (showManageSheet && canManageConference) {
                ModalBottomSheet(
                    onDismissRequest = { showManageSheet = false },
                    sheetState = sheetState
                ) {
                    ManageConferenceSheet(
                        participants = uiState.conferenceParticipants,
                        showSplit = showSplitInManage,
                        onSplit = viewModel::split,
                        onHangup = { call -> viewModel.hangup(call) }
                    )
                }
            }

            Scaffold(
                modifier = Modifier.fillMaxSize(),
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
            ) { innerPadding ->
                val configuration = LocalConfiguration.current
                val isCompactLayout = configuration.screenHeightDp <= 720 ||
                    configuration.screenWidthDp <= 360
                val callerDetailsBottomPadding = if (isCompactLayout) {
                    0.dp
                } else {
                    12.dp
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        if (uiState.status != CallStatus.IDLE) {
                            InCallDetails(
                                callerName = uiState.callerName,
                                callerNumber = uiState.callerNumber,
                                callerNumberLabel = uiState.callerNumberLabel,
                                status = uiState.status,
                                durationMillis = durationMillis,
                                    callerImageUri = uiState.callerImageUri,
                                    showCallerImage = false,
                                    useCompactCallerText = isCompactLayout,
                                modifier = Modifier
                                    .statusBarsPadding()
                                    .padding(
                                        start = 16.dp,
                                        top = 32.dp,
                                        end = 16.dp,
                                        bottom = callerDetailsBottomPadding,
                                    )
                                    .align(Alignment.CenterHorizontally)
                            )

                            val preparedEncounter = encounter?.takeIf { canStartBattle }
                            if (preparedEncounter != null) {
                                val radiantUnlockMessage = preparedEncounter.unlockedRadiantName?.let { name ->
                                    stringResource(R.string.radiant_variant_unlocked_message, name)
                                }
                                LaunchedEffect(preparedEncounter.id, radiantUnlockMessage) {
                                    radiantUnlockMessage?.let { message ->
                                        radiantUnlockNotifier.show(
                                            characterName = requireNotNull(preparedEncounter.unlockedRadiantName),
                                            frontSpritePath = preparedEncounter.unlockedRadiantFrontSpritePath,
                                        )
                                        radiantUnlockSnackbar.showSnackbar(message)
                                    }
                                }
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(400.dp)
                                            .padding(horizontal = 4.dp, vertical = 4.dp),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        BattleScreen(
                                            encounter = preparedEncounter,
                                            modifier = Modifier.fillMaxSize(),
                                            framed = false,
                                        )
                                    }
                                }
                            } else {
                                Spacer(modifier = Modifier.weight(1f))
                            }

                            if (uiState.isIncoming) {
                                MonsterIncomingCallControls(
                                    icons = rememberMonsterIcons(forceLightDialpad = true),
                                    controlsEnabled = true,
                                    onHangup = viewModel::hangup,
                                    onAnswer = viewModel::answer,
                                    onMessage = { viewModel.hangup(it) },
                                )
                            } else {
                                MonsterInCallControls(
                                    icons = rememberMonsterIcons(forceLightDialpad = true),
                                    isMuted = uiState.isMuted,
                                    isSpeaker = uiState.isSpeaker,
                                    audioRoutes = uiState.audioRoutes,
                                    isHolding = uiState.isHolding,
                                    canManageConference = canManageConference,
                                    canMerge = uiState.canMerge,
                                    canSwap = canSwap,
                                    canHold = canHold,
                                    showAddCall = !hasSecondaryCall,
                                    canAddCall = canAddCall,
                                    onHangup = viewModel::hangup,
                                    onMute = viewModel::turnMute,
                                    onSpeaker = viewModel::turnSpeaker,
                                    onAudioRouteSelected = viewModel::selectAudioRoute,
                                    onHold = viewModel::hold,
                                    onAddCall = { viewModel.addCall(context.getActivity() as Activity) },
                                    onMerge = viewModel::merge,
                                    onSwap = viewModel::swap,
                                    onManageConference = { showManageSheet = true },
                                    onDigitPress = viewModel::startDtmf,
                                    onDigitRelease = viewModel::stopDtmf,
                                    dialpadLabel = stringResource(R.string.call_command_keys),
                                    muteLabel = stringResource(R.string.call_command_shush),
                                    speakerLabel = stringResource(R.string.call_command_hear),
                                    moreLabel = stringResource(R.string.call_command_bag),
                                    endCallLabel = stringResource(R.string.call_command_run),
                                    addCallLabel = stringResource(InCallR.string.action_add_call),
                                    holdLabel = stringResource(InCallR.string.action_hold),
                                    mergeLabel = stringResource(InCallR.string.conference_merge),
                                    swapLabel = stringResource(InCallR.string.conference_swap),
                                    manageLabel = stringResource(InCallR.string.conference_manage),
                                )
                            }
                        }
                    }

                    if (hasSecondaryCall && secondaryCallerName != null && !uiState.isIncoming) {
                        SecondaryCallBanner(
                            callerName = secondaryCallerName,
                            modifier = Modifier
                                .statusBarsPadding()
                                .align(Alignment.TopCenter)
                        )
                    }

                    SnackbarHost(
                        hostState = radiantUnlockSnackbar,
                        modifier = Modifier
                            .statusBarsPadding()
                            .align(Alignment.TopCenter)
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }
            }
        }
    }

    private companion object {
        const val OnlineProfileRefreshWaitMillis = 500L
    }
}
