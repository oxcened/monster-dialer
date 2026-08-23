package dev.alenajam.monsterdialer.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.alenajam.monsterdialer.R
import dev.alenajam.monsterdialer.ui.battle.BattleEncounterFactory
import dev.alenajam.monsterdialer.ui.battle.BattleScreen
import dev.alenajam.opendialer.core.common.ui.AppProviders
import dev.alenajam.opendialer.core.common.ui.AppThemeExtension
import dev.alenajam.opendialer.core.common.ui.InCallUI
import dev.alenajam.opendialer.core.common.ui.LocalAppIcons
import dev.alenajam.opendialer.feature.inCall.ui.InCallViewModel
import javax.inject.Inject

class MonsterInCallUI @Inject constructor() : InCallUI {
    @Composable
    override fun Content() {
        val viewModel: InCallViewModel = viewModel()
        val call = viewModel.primaryCall.observeAsState().value

        // Here you provide the MonsterDialer specific UI!
        AppProviders(
            icons = MonsterIcons,
            themeExtension = AppThemeExtension(
                // backgroundPainter = { painterResource(R.drawable.monster_bg) }
            )
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                call?.let {
                    BattleScreen(
                        encounter = BattleEncounterFactory.forCall(
                            callId = "${System.identityHashCode(it)}:${it.callerNumber}",
                            callerName = it.callerName.orEmpty().ifBlank { "Unknown" },
                            isAnonymous = it.isAnonymous
                        ),
                        modifier = Modifier
                            .heightIn(max = 355.dp)
                            .aspectRatio(160f / 144f)
                            .fillMaxSize()
                    )
                }

                IconButton(
                    onClick = viewModel::hangup,
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    ),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(bottom = 32.dp)
                        .size(72.dp)
                ) {
                    Icon(
                        imageVector = LocalAppIcons.current.hangup,
                        contentDescription = stringResource(R.string.hang_up),
                        tint = Color.White
                    )
                }
            }
        }
    }
}
