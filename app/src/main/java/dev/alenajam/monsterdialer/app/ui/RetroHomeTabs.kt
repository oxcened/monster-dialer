package dev.alenajam.monsterdialer.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsIgnoringVisibility
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import dev.alenajam.monsterdialer.R
import dev.alenajam.opendialer.core.common.ui.AppIcon
import dev.alenajam.opendialer.core.common.ui.LocalAppIcons

private val RetroTabViolet = Color(0xFF7355A5)
private val RetroTabFace = Color(0xFF9A7BC4)
private val RetroTabHighlight = Color(0xFFEDE5F7)
private val RetroTabShadow = Color(0xFFB8B0C2)
private val RetroTabInk = Color(0xFF202020)

internal enum class MonsterHomeTab {
    FAVORITES,
    CALLS,
    CONTACTS,
    PROFILE,
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
internal fun RetroHomeTabs(
    currentTab: MonsterHomeTab,
    onFavorites: () -> Unit,
    onCalls: () -> Unit,
    onContacts: () -> Unit,
    onProfile: () -> Unit,
) {
    val tabs = listOf(
        RetroHomeTab(R.string.favorites, MonsterHomeTab.FAVORITES, onFavorites) {
            AppIcon(
                icon = LocalAppIcons.current.favorite,
                contentDescription = null,
                modifier = Modifier.size(28.dp),
                tint = RetroTabInk,
            )
        },
        RetroHomeTab(R.string.recents, MonsterHomeTab.CALLS, onCalls) { selected ->
            AppIcon(
                icon = if (selected) LocalAppIcons.current.recentsSelected else LocalAppIcons.current.recents,
                contentDescription = null,
                modifier = Modifier.size(28.dp),
                tint = RetroTabInk,
            )
        },
        RetroHomeTab(R.string.contacts, MonsterHomeTab.CONTACTS, onContacts) { selected ->
            AppIcon(
                icon = if (selected) LocalAppIcons.current.contactsSelected else LocalAppIcons.current.contacts,
                contentDescription = null,
                modifier = Modifier.size(28.dp),
                tint = RetroTabInk,
            )
        },
        RetroHomeTab(R.string.characters_navigation_label, MonsterHomeTab.PROFILE, onProfile) {
            AppIcon(
                icon = LocalAppIcons.current.person,
                contentDescription = null,
                modifier = Modifier.size(28.dp),
                tint = RetroTabInk,
            )
        },
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(RetroTabFace)
            .windowInsetsPadding(WindowInsets.statusBarsIgnoringVisibility)
            .padding(start = 8.dp, end = 8.dp),
    ) {
        Box(modifier = Modifier.height(62.dp)) {
            Row(
                modifier = Modifier
                    .padding(top = 2.dp)
                    .height(54.dp)
                    .background(RetroTabViolet)
                    .padding(3.dp),
                horizontalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                tabs.forEach { tab ->
                    val selected = currentTab == tab.destination
                    val accessibleLabel = stringResource(tab.label)
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clickable(onClick = tab.onClick)
                            .semantics { contentDescription = accessibleLabel },
                        contentAlignment = Alignment.Center,
                    ) {
                        // The original control uses only a white top/left keyline and a gray
                        // lower/right drop edge; keep the face otherwise flat and violet.
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .background(RetroTabHighlight)
                                .padding(start = 3.dp, top = 3.dp)
                                .background(RetroTabShadow)
                                .padding(end = 3.dp, bottom = 3.dp)
                                .background(if (selected) RetroTabFace else RetroTabViolet),
                            contentAlignment = Alignment.Center,
                        ) {
                            tab.icon(selected)
                        }
                        if (selected) {
                            RetroTabSelectionArrow(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .offset(y = 12.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RetroTabSelectionArrow(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(24.dp)) {
        val unit = size.minDimension / 8f
        val outer = androidx.compose.ui.graphics.Path().apply {
            moveTo(3f * unit, 0f)
            lineTo(5f * unit, 0f)
            lineTo(5f * unit, unit)
            lineTo(6f * unit, unit)
            lineTo(6f * unit, 2f * unit)
            lineTo(7f * unit, 2f * unit)
            lineTo(7f * unit, 3f * unit)
            lineTo(8f * unit, 3f * unit)
            lineTo(8f * unit, 5f * unit)
            lineTo(6f * unit, 5f * unit)
            lineTo(6f * unit, 8f * unit)
            lineTo(2f * unit, 8f * unit)
            lineTo(2f * unit, 5f * unit)
            lineTo(0f, 5f * unit)
            lineTo(0f, 3f * unit)
            lineTo(unit, 3f * unit)
            lineTo(unit, 2f * unit)
            lineTo(2f * unit, 2f * unit)
            lineTo(2f * unit, unit)
            lineTo(3f * unit, unit)
            close()
        }
        drawPath(outer, RetroTabInk)
        val inner = androidx.compose.ui.graphics.Path().apply {
            moveTo(3.5f * unit, unit)
            lineTo(4.5f * unit, unit)
            lineTo(4.5f * unit, 2f * unit)
            lineTo(5.5f * unit, 2f * unit)
            lineTo(5.5f * unit, 3f * unit)
            lineTo(6.5f * unit, 3f * unit)
            lineTo(6.5f * unit, 4f * unit)
            lineTo(5f * unit, 4f * unit)
            lineTo(5f * unit, 7f * unit)
            lineTo(3f * unit, 7f * unit)
            lineTo(3f * unit, 4f * unit)
            lineTo(1.5f * unit, 4f * unit)
            lineTo(1.5f * unit, 3f * unit)
            lineTo(2.5f * unit, 3f * unit)
            lineTo(2.5f * unit, 2f * unit)
            lineTo(3.5f * unit, 2f * unit)
            close()
        }
        drawPath(inner, RetroTabFace)
    }
}

private data class RetroHomeTab(
    val label: Int,
    val destination: MonsterHomeTab,
    val onClick: () -> Unit,
    val icon: @Composable (selected: Boolean) -> Unit,
)
