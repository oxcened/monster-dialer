package dev.alenajam.monsterdialer.battle.ui

import java.io.File
import java.text.DateFormat
import java.time.ZoneId
import java.util.Date
import javax.inject.Inject
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.Image
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.alenajam.monsterdialer.R
import dev.alenajam.monsterdialer.app.ui.LocalMonsterAppIcons
import dev.alenajam.monsterdialer.app.ui.RetroConfirmationDialog
import dev.alenajam.monsterdialer.app.ui.RetroContextMenuItem
import dev.alenajam.monsterdialer.app.ui.RetroContextMenuOverlay
import dev.alenajam.monsterdialer.app.ui.RetroFooter
import dev.alenajam.monsterdialer.app.ui.RetroSearchButton
import dev.alenajam.monsterdialer.app.ui.RetroScreenHorizontalPadding
import dev.alenajam.monsterdialer.battle.data.BattleJournalEntry
import dev.alenajam.monsterdialer.battle.data.BattleJournalStore
import dev.alenajam.monsterdialer.battle.data.BattleJournalSprite
import dev.alenajam.monsterdialer.battle.data.EncounterType
import dev.alenajam.monsterdialer.characters.data.RadiantVariantUnlockNotifier
import dev.alenajam.opendialer.core.common.ui.AppIcon
import dev.alenajam.opendialer.core.common.ui.LocalAppIcons
import dev.alenajam.opendialer.feature.settings.LocalSettingsSubpageNavigator
import coil.compose.AsyncImage
import kotlinx.coroutines.launch

private val BattleJournalPixelFont = FontFamily(Font(R.font.ui_pixel_font))
private val BattleJournalInk = Color(0xFF202020)

private enum class BattleJournalFilter(val labelRes: Int) {
    All(R.string.filter_all),
    RadiantFound(R.string.radiant),
    Battles(R.string.battle_journal_battle_label),
}

@Composable
fun BattleJournalScreen(viewModel: BattleJournalViewModel = hiltViewModel()) {
    val entries by viewModel.entries.collectAsStateWithLifecycle()
    val navigator = LocalSettingsSubpageNavigator.current
    var selectedFilter by rememberSaveable { mutableStateOf(BattleJournalFilter.All) }
    var isFilterMenuVisible by rememberSaveable { mutableStateOf(false) }
    var isClearConfirmationVisible by rememberSaveable { mutableStateOf(false) }
    val filteredEntries = when (selectedFilter) {
        BattleJournalFilter.All -> entries
        BattleJournalFilter.RadiantFound -> entries.filter { entry ->
            entry.encounterType == EncounterType.RadiantWild
        }
        BattleJournalFilter.Battles -> entries.filter { entry ->
            entry.encounterType != EncounterType.RadiantWild
        }
    }
    val entriesByDate = filteredEntries.groupBy { entry ->
        Date(entry.timestampMillis).toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF9F7FC)),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(modifier = Modifier.fillMaxWidth()) {
                RetroSearchButton(
                    label = stringResource(selectedFilter.labelRes),
                    onClick = { isFilterMenuVisible = true },
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.Start,
                )
                RetroSearchButton(
                    label = stringResource(R.string.battle_journal_clear_short),
                    onClick = { isClearConfirmationVisible = true },
                    modifier = Modifier.weight(1f),
                )
            }
            Box(modifier = Modifier.weight(1f)) {
                when {
                    entries.isEmpty() -> JournalEmptyState(
                        title = stringResource(R.string.battle_journal_empty_title),
                        description = stringResource(R.string.battle_journal_empty_description),
                    )
                    filteredEntries.isEmpty() -> JournalEmptyState(
                        title = stringResource(R.string.battle_journal_no_matches_title),
                        description = stringResource(R.string.battle_journal_no_matches_description),
                    )
                    else -> LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(
                            start = RetroScreenHorizontalPadding,
                            top = 2.dp,
                            end = RetroScreenHorizontalPadding,
                            bottom = 12.dp,
                        ),
                    ) {
                        entriesByDate.forEach { (_, entriesForDate) ->
                            item(key = "header-${entriesForDate.first().timestampMillis}") {
                                JournalDateHeader(entriesForDate.first().timestampMillis)
                            }
                            itemsIndexed(entriesForDate, key = { _, entry -> entry.id }) { _, entry ->
                                BattleJournalEntryRow(
                                    entry = entry,
                                    onClick = { viewModel.shareRadiantDiscovery(entry) },
                                )
                            }
                        }
                    }
                }
            }
            RetroFooter(
                backKey = stringResource(R.string.retro_key_b),
                backLabel = stringResource(R.string.retro_action_back_label),
                onBack = { navigator?.navigateBack() },
            )
        }

        if (isFilterMenuVisible) {
            RetroContextMenuOverlay(
                modifier = Modifier.fillMaxWidth(0.68f),
                fontFamily = BattleJournalPixelFont,
                onDismissRequest = { isFilterMenuVisible = false },
                items = BattleJournalFilter.entries.map { filter ->
                    RetroContextMenuItem(
                        label = stringResource(filter.labelRes),
                        showCursor = selectedFilter == filter,
                    ) {
                        selectedFilter = filter
                        isFilterMenuVisible = false
                    }
                } + RetroContextMenuItem.cancel(stringResource(R.string.cancel)) {
                    isFilterMenuVisible = false
                },
            )
        }
        if (isClearConfirmationVisible) {
            RetroConfirmationDialog(
                title = stringResource(R.string.battle_journal_clear_confirmation_title),
                noLabel = stringResource(R.string.cancel),
                yesLabel = stringResource(R.string.battle_journal_clear),
                fontFamily = BattleJournalPixelFont,
                onDismissRequest = { isClearConfirmationVisible = false },
                onConfirm = {
                    viewModel.clear()
                    isClearConfirmationVisible = false
                },
            )
        }
    }
}

@Composable
private fun JournalDateHeader(timestampMillis: Long) {
    Text(
        text = DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(timestampMillis)).uppercase(),
        modifier = Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 2.dp),
        fontFamily = BattleJournalPixelFont,
        fontSize = 13.sp,
        color = BattleJournalInk,
    )
}

@Composable
private fun JournalEmptyState(title: String, description: String) {
    Box(
        modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AppIcon(
                icon = LocalMonsterAppIcons.current.battleJournal,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = BattleJournalInk,
            )
            Text(
                text = title.uppercase(),
                fontFamily = BattleJournalPixelFont,
                fontSize = 18.sp,
                textAlign = TextAlign.Center,
                color = BattleJournalInk,
            )
            Text(
                text = description.uppercase(),
                fontFamily = BattleJournalPixelFont,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                color = BattleJournalInk.copy(alpha = 0.75f),
            )
        }
    }
}

@Composable
private fun BattleJournalEntryRow(
    entry: BattleJournalEntry,
    onClick: () -> Unit,
) {
    val locale = LocalLocale.current.platformLocale
    val isRadiantEncounter = entry.encounterType == EncounterType.RadiantWild
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (entry.isRadiantDiscovery) {
                    Modifier.clickable(onClick = onClick)
                } else {
                    Modifier
                }
            )
            .padding(horizontal = 2.dp, vertical = 7.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        JournalSprite(
            sprite = entry.opponentSprite,
            fallbackResource = R.drawable.battle_unknown_monster,
            contentDescription = entry.opponentMonsterName?.let { name ->
                stringResource(R.string.character_artwork, name)
            },
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(1.dp),
        ) {
            val opponentName = entry.opponentMonsterName ?: stringResource(R.string.unknown)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(
                            if (entry.isRadiantDiscovery) {
                                R.string.battle_journal_radiant_label
                            } else if (isRadiantEncounter) {
                                R.string.battle_journal_radiant_encounter_label
                            } else {
                                R.string.battle_journal_battle_label
                            },
                        ).uppercase(locale),
                        fontFamily = BattleJournalPixelFont,
                        fontSize = 13.sp,
                        color = if (isRadiantEncounter) {
                            BattleJournalInk
                        } else {
                            BattleJournalInk.copy(alpha = 0.75f)
                        },
                    )
                    if (isRadiantEncounter) {
                        AppIcon(
                            icon = LocalMonsterAppIcons.current.radiant,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                        )
                    }
                }
                Text(
                    text = DateFormat.getTimeInstance(DateFormat.SHORT)
                        .format(Date(entry.timestampMillis)),
                    fontFamily = BattleJournalPixelFont,
                    fontSize = 13.sp,
                    color = BattleJournalInk.copy(alpha = 0.75f),
                )
            }
            Text(
                text = opponentName.uppercase(locale),
                fontFamily = BattleJournalPixelFont,
                fontSize = 16.sp,
                color = BattleJournalInk,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            entry.opponentTrainerName?.let { trainerName ->
                val trainerDescription = stringResource(
                    R.string.battle_journal_opponent_trainer,
                    trainerName,
                )
                Row(
                    modifier = Modifier.clearAndSetSemantics {
                        contentDescription = trainerDescription
                    },
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AppIcon(
                        icon = LocalAppIcons.current.person,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = BattleJournalInk.copy(alpha = 0.75f),
                    )
                    Text(
                        text = trainerName,
                        fontFamily = BattleJournalPixelFont,
                        fontSize = 13.sp,
                        color = BattleJournalInk.copy(alpha = 0.75f),
                    )
                }
            }
        }
    }
}

@Composable
private fun JournalSprite(
    sprite: BattleJournalSprite?,
    fallbackResource: Int,
    contentDescription: String?,
) {
    val modifier = Modifier.size(42.dp)
    val filePath = sprite?.journalSnapshotPath ?: sprite?.localFilePath
    if (filePath != null) {
        AsyncImage(
            model = File(filePath),
            contentDescription = contentDescription,
            contentScale = ContentScale.Fit,
            error = painterResource(fallbackResource),
            modifier = modifier,
        )
    } else {
        Image(
            painter = painterResource(sprite?.drawableResource ?: fallbackResource),
            contentDescription = contentDescription,
            contentScale = ContentScale.Fit,
            modifier = modifier,
        )
    }
}

@HiltViewModel
class BattleJournalViewModel @Inject constructor(
    private val journalStore: BattleJournalStore,
    private val radiantUnlockNotifier: RadiantVariantUnlockNotifier,
) : ViewModel() {
    val entries = journalStore.entries

    fun clear() = journalStore.clear()

    fun shareRadiantDiscovery(entry: BattleJournalEntry) {
        if (!entry.isRadiantDiscovery) return
        val name = entry.opponentMonsterName ?: return
        val spritePath = entry.opponentSprite?.localFilePath
            ?: entry.opponentSprite?.journalSnapshotPath
            ?: return

        viewModelScope.launch {
            radiantUnlockNotifier.share(name, spritePath)
        }
    }
}
