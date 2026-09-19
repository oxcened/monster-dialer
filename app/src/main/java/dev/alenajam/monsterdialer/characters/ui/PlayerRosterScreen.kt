package dev.alenajam.monsterdialer.characters.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import dev.alenajam.monsterdialer.R
import dev.alenajam.monsterdialer.app.ui.RetroContextMenuItem
import dev.alenajam.monsterdialer.app.ui.RetroContextMenuOverlay
import dev.alenajam.monsterdialer.app.ui.RetroConfirmationDialog
import dev.alenajam.monsterdialer.app.ui.RetroFooter
import dev.alenajam.monsterdialer.app.ui.RetroSelectableRow
import dev.alenajam.monsterdialer.characters.data.DefaultMonsterLevel
import dev.alenajam.monsterdialer.characters.data.MaxPlayerMonsterTeamSize

private val RosterPixelFont = FontFamily(Font(R.font.ui_pixel_font))

/** A six-position party view that mirrors the compact Game Boy roster layout. */
@Composable
internal fun PlayerRosterScreen(
    roster: List<PlayerRosterMonster>,
    onSelectSlot: (Int) -> Unit,
    onRemoveMonster: (PlayerRosterMonster) -> Unit,
    onSwitchSlots: (sourceSlot: Int, targetSlot: Int) -> Unit,
    onBack: () -> Unit,
) {
    var selectedSlot by remember { mutableIntStateOf(0) }
    var menuSlot by remember { mutableStateOf<Int?>(null) }
    var switchSourceSlot by remember { mutableStateOf<Int?>(null) }
    var confirmationSlot by remember { mutableStateOf<Int?>(null) }
    val selectedMonster = roster.getOrNull(selectedSlot)
    val prompt = if (switchSourceSlot != null) {
        stringResource(R.string.roster_switch_target_message)
    } else if (selectedMonster == null) {
        stringResource(R.string.roster_empty_slot_message)
    } else {
        stringResource(R.string.roster_choose_monster_message)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxWidth()) {
            repeat(MaxPlayerMonsterTeamSize) { slotIndex ->
                val monster = roster.getOrNull(slotIndex)
                val isSelectable = monster != null || (switchSourceSlot == null && slotIndex == roster.size)
                RosterSlotRow(
                    monster = monster,
                    selected = if (switchSourceSlot == null) selectedSlot == slotIndex else switchSourceSlot == slotIndex,
                    enabled = isSelectable,
                    onClick = {
                        selectedSlot = slotIndex
                        val sourceSlot = switchSourceSlot
                        when {
                            sourceSlot == null -> menuSlot = slotIndex
                            sourceSlot == slotIndex -> switchSourceSlot = null
                            monster != null -> {
                                switchSourceSlot = null
                                onSwitchSlots(sourceSlot, slotIndex)
                            }
                        }
                    },
                )
            }
        }
        RetroFooter(
            message = prompt,
            animationKey = selectedSlot to switchSourceSlot,
            backKey = stringResource(R.string.retro_key_b),
            backLabel = stringResource(
                if (switchSourceSlot == null) {
                    R.string.retro_action_back_label
                } else {
                    R.string.customized_contacts_cancel_action
                },
            ),
            onBack = {
                if (switchSourceSlot != null) switchSourceSlot = null else onBack()
            },
            modifier = Modifier.align(Alignment.BottomCenter),
        )
        menuSlot?.let { slotIndex ->
            val monster = roster.getOrNull(slotIndex)
            RetroContextMenuOverlay(
                    modifier = Modifier.fillMaxWidth(0.72f),
                    onDismissRequest = { menuSlot = null },
                    items = if (monster == null) {
                        listOf(
                            RetroContextMenuItem(label = stringResource(R.string.retro_picker_add)) {
                                menuSlot = null
                                onSelectSlot(slotIndex)
                            },
                            RetroContextMenuItem.cancel(stringResource(R.string.cancel)) {
                                menuSlot = null
                            },
                        )
                    } else {
                        listOf(
                            RetroContextMenuItem(label = stringResource(R.string.roster_switch)) {
                                menuSlot = null
                                switchSourceSlot = slotIndex
                            },
                            RetroContextMenuItem(label = stringResource(R.string.change)) {
                                menuSlot = null
                                onSelectSlot(slotIndex)
                            },
                        ) + if (roster.size > 1) {
                            listOf(
                            RetroContextMenuItem(label = stringResource(R.string.remove)) {
                                menuSlot = null
                                confirmationSlot = slotIndex
                                },
                            )
                        } else {
                            emptyList()
                        } + listOf(
                            RetroContextMenuItem.cancel(stringResource(R.string.cancel)) {
                                menuSlot = null
                            },
                        )
                    },
                    fontFamily = RosterPixelFont,
            )
        }
        confirmationSlot?.let { slotIndex ->
            roster.getOrNull(slotIndex)?.let { monster ->
                RetroConfirmationDialog(
                    title = stringResource(R.string.roster_release_title),
                    message = monster.character.name,
                    noLabel = stringResource(R.string.roster_release_no),
                    yesLabel = stringResource(R.string.roster_release_yes),
                    fontFamily = RosterPixelFont,
                    onDismissRequest = { confirmationSlot = null },
                    onConfirm = {
                        confirmationSlot = null
                        onRemoveMonster(monster)
                    },
                )
            }
        }
    }
}

@Composable
private fun RosterSlotRow(
    monster: PlayerRosterMonster?,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    RetroSelectableRow(
        selected = selected,
        onClick = onClick,
        enabled = enabled,
    ) {
        if (monster == null) {
            Text(
                text = stringResource(R.string.roster_empty_slot),
                fontFamily = RosterPixelFont,
                fontSize = 18.sp,
                color = RetroInk,
                modifier = Modifier.padding(vertical = 14.dp),
            )
        } else {
            RosterMonsterRow(monster)
        }
    }
}

@Composable
private fun RosterMonsterRow(monster: PlayerRosterMonster) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val artwork = monster.character.artwork
        val fallbackArtwork = monster.character.fallbackArtwork
        if (artwork != null) {
            AsyncImage(
                model = artwork,
                contentDescription = monster.character.name,
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(48.dp),
            )
        } else if (fallbackArtwork != null) {
            Image(
                painter = painterResource(fallbackArtwork),
                contentDescription = monster.character.name,
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(48.dp),
            )
        } else {
            Spacer(modifier = Modifier.size(48.dp))
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 4.dp),
            verticalArrangement = Arrangement.spacedBy(1.dp),
        ) {
            Text(
                text = monster.character.name.uppercase(),
                fontFamily = RosterPixelFont,
                fontSize = 18.sp,
                color = RetroInk,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = stringResource(
                    R.string.roster_monster_level,
                    monster.character.level ?: DefaultMonsterLevel,
                ),
                fontFamily = RosterPixelFont,
                fontSize = 13.sp,
                color = RetroInk.copy(alpha = 0.75f),
            )
        }
    }
}
