package dev.alenajam.monsterdialer.ui

import android.app.Activity
import java.io.File
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.alenajam.monsterdialer.ui.battle.AssignedCharacterEncounterFactory
import dev.alenajam.monsterdialer.packs.CharacterAssignmentStore
import dev.alenajam.monsterdialer.packs.CharacterPackRepository
import dev.alenajam.monsterdialer.ui.battle.BattleScreen
import dev.alenajam.opendialer.core.common.getActivity
import dev.alenajam.opendialer.core.common.ui.AppProviders
import dev.alenajam.opendialer.core.common.ui.AppThemeExtension
import dev.alenajam.opendialer.core.common.ui.InCallUI
import dev.alenajam.opendialer.feature.inCall.ui.CallStatus
import dev.alenajam.opendialer.feature.inCall.ui.InCallControls
import dev.alenajam.opendialer.feature.inCall.ui.InCallDetails
import dev.alenajam.opendialer.feature.inCall.ui.InCallViewModel
import dev.alenajam.opendialer.feature.inCall.ui.IncomingCallControls
import dev.alenajam.opendialer.feature.inCall.ui.ManageConferenceSheet
import dev.alenajam.opendialer.feature.inCall.ui.SecondaryCallBanner
import javax.inject.Inject

class MonsterInCallUI @Inject constructor() : InCallUI {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val viewModel: InCallViewModel = viewModel()
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        val durationMillis by viewModel.activeCallDuration.collectAsStateWithLifecycle(0L)
        val context = LocalContext.current
        val encounterFactory = remember(context.filesDir) {
            val characterPackRoot = File(context.filesDir, "character-packs")
            AssignedCharacterEncounterFactory(
                assignments = CharacterAssignmentStore(characterPackRoot),
                characters = CharacterPackRepository(characterPackRoot)
            )
        }

        val hasSecondaryCall = uiState.hasSecondaryCall
        val secondaryCallerName = uiState.secondaryCallerName
        val canSwap = hasSecondaryCall
        val canManageConference = uiState.canManageConference
        val canAddCall = uiState.canAddCall && !hasSecondaryCall
        val canHold = uiState.canHold && !canSwap
        val showSplitInManage = canManageConference && !hasSecondaryCall
        var showManageSheet by remember { mutableStateOf(false) }
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

        LaunchedEffect(canManageConference) {
            if (!canManageConference) showManageSheet = false
        }

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

        // Here you provide the MonsterDialer specific UI!
        AppProviders(
            icons = MonsterIcons,
            themeExtension = AppThemeExtension(
                // backgroundPainter = { painterResource(R.drawable.monster_bg) }
            )
        ) {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                bottomBar = {
                    if (uiState.isIncoming) {
                        IncomingCallControls(
                            onHangup = viewModel::hangup,
                            onAnswer = viewModel::answer,
                            onMessage = { viewModel.hangup(it) }
                        )
                    } else {
                        InCallControls(
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
                            onDigit = viewModel::playDtmf
                        )
                    }
                }
            ) { innerPadding ->
                val measuredBottomPadding = innerPadding.calculateBottomPadding()
                val collapsedBottomPadding = remember(uiState.isIncoming) {
                    mutableStateOf(measuredBottomPadding)
                }
                LaunchedEffect(measuredBottomPadding) {
                    if (measuredBottomPadding < collapsedBottomPadding.value) {
                        collapsedBottomPadding.value = measuredBottomPadding
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = collapsedBottomPadding.value)
                ) {
                    if (hasSecondaryCall && secondaryCallerName != null && !uiState.isIncoming) {
                        SecondaryCallBanner(
                            callerName = secondaryCallerName,
                            modifier = Modifier.statusBarsPadding()
                        )
                    }

                    if (uiState.status != CallStatus.IDLE) {
                        InCallDetails(
                            callerName = uiState.callerName,
                            callerNumber = uiState.callerNumber,
                            callerNumberLabel = uiState.callerNumberLabel,
                            status = uiState.status,
                            durationMillis = durationMillis,
                            callerImageUri = uiState.callerImageUri,
                            showCallerImage = false,
                            modifier = Modifier
                                .statusBarsPadding()
                                .padding(start = 16.dp, top = 32.dp, end = 16.dp, bottom = 12.dp)
                                .align(Alignment.CenterHorizontally)
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            BattleScreen(
                                encounter = encounterFactory.forCall(
                                    callId = "${uiState.callerName}:${uiState.callerNumber}",
                                    contactKey = uiState.callerNumber,
                                    callerName = uiState.callerName.ifBlank { uiState.callerNumber.ifBlank { "Unknown" } },
                                    isAnonymous = uiState.callerName.isBlank() && uiState.callerNumber.isBlank()
                                ),
                                modifier = Modifier
                                    .heightIn(max = 355.dp)
                                    .aspectRatio(160f / 144f)
                                    .fillMaxSize()
                            )
                        }
                    }
                }
            }
        }
    }
}
