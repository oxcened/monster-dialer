package dev.alenajam.monsterdialer.ui

import android.app.Activity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.alenajam.monsterdialer.ui.battle.BattleEncounterFactory
import dev.alenajam.monsterdialer.ui.battle.BattleScreen
import dev.alenajam.opendialer.core.common.getActivity
import dev.alenajam.opendialer.core.common.ui.AppProviders
import dev.alenajam.opendialer.core.common.ui.AppThemeExtension
import dev.alenajam.opendialer.core.common.ui.InCallUI
import dev.alenajam.opendialer.feature.inCall.ui.InCallControls
import dev.alenajam.opendialer.feature.inCall.ui.InCallDetails
import dev.alenajam.opendialer.feature.inCall.ui.InCallViewModel
import dev.alenajam.opendialer.feature.inCall.ui.IncomingCallControls
import javax.inject.Inject

class MonsterInCallUI @Inject constructor() : InCallUI {
    @Composable
    override fun Content() {
        val viewModel: InCallViewModel = viewModel()
        val call = viewModel.primaryCall.observeAsState().value
        val isHolding = viewModel.isHolding.observeAsState().value
        val isSpeaker = viewModel.isSpeaker.observeAsState().value
        val isMuted = viewModel.isMuted.observeAsState().value
        val isIncoming = viewModel.isIncoming.observeAsState(false).value
        val stateLabel = viewModel.stateLabel.observeAsState("").value
        val callerName = viewModel.callerName.observeAsState("").value
        val callerNumber = viewModel.callerNumber.observeAsState("").value
        val callerImageUri = viewModel.callerImageUri.observeAsState().value
        val activity = LocalContext.current.getActivity() as Activity

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
                    if (isIncoming) {
                        IncomingCallControls(
                            onHangup = viewModel::hangup,
                            onAnswer = viewModel::answer
                        )
                    } else {
                        InCallControls(
                            isMuted = isMuted,
                            isSpeaker = isSpeaker,
                            isHolding = isHolding,
                            onHangup = viewModel::hangup,
                            onMute = viewModel::turnMute,
                            onSpeaker = viewModel::turnSpeaker,
                            onHold = viewModel::hold,
                            onAddCall = { viewModel.addCall(activity) },
                            onDigit = viewModel::playDtmf
                        )
                    }
                }
            ) { innerPadding ->
                val measuredBottomPadding = innerPadding.calculateBottomPadding()
                val collapsedBottomPadding = remember(isIncoming) {
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
                    call?.let { ongoingCall ->
                        InCallDetails(
                            callerName = callerName,
                            callerNumber = callerNumber,
                            stateLabel = stateLabel,
                            callerImageUri = callerImageUri,
                            showCallerImage = false,
                            modifier = Modifier
                                .statusBarsPadding()
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                                .align(Alignment.CenterHorizontally)
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            BattleScreen(
                                encounter = BattleEncounterFactory.forCall(
                                    callId = "${System.identityHashCode(ongoingCall)}:${ongoingCall.callerNumber}",
                                    callerName = callerName.ifBlank { "Unknown" },
                                    isAnonymous = ongoingCall.isAnonymous
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
