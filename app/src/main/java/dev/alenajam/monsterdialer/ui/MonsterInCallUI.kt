package dev.alenajam.monsterdialer.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import dev.alenajam.opendialer.core.common.ui.AppProviders
import dev.alenajam.opendialer.core.common.ui.AppThemeExtension
import dev.alenajam.opendialer.core.common.ui.InCallUI
import javax.inject.Inject

class MonsterInCallUI @Inject constructor() : InCallUI {
    @Composable
    override fun Content() {
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
                Text(text = "MonsterDialer In-Call Screen")
                // TODO: Re-use components from :feature:inCall or build entirely new ones!
            }
        }
    }
}
