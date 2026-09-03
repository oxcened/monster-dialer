package dev.alenajam.monsterdialer.battle.ui

import java.text.DateFormat
import java.util.Date
import java.time.ZoneId
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.material3.Surface
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.IconButton
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.alenajam.monsterdialer.R
import dev.alenajam.monsterdialer.app.ui.LocalMonsterAppIcons
import dev.alenajam.monsterdialer.battle.data.BattleJournalEntry
import dev.alenajam.monsterdialer.battle.data.BattleJournalStore
import dev.alenajam.monsterdialer.battle.data.BattleJournalSprite
import dev.alenajam.monsterdialer.battle.data.EncounterType
import dev.alenajam.monsterdialer.characters.data.RadiantVariantUnlockNotifier
import dev.alenajam.opendialer.core.common.ui.AppIcon
import dev.alenajam.opendialer.core.common.ui.LocalAppIcons
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

private enum class BattleJournalFilter(val labelRes: Int) {
    All(R.string.filter_all),
    RadiantFound(R.string.radiant),
    Battles(R.string.battle_journal_battle_label),
}

@Composable
fun BattleJournalScreen(viewModel: BattleJournalViewModel = hiltViewModel()) {
    val entries by viewModel.entries.collectAsStateWithLifecycle()
    var selectedFilter by rememberSaveable { mutableStateOf(BattleJournalFilter.All) }
    val filteredEntries = when (selectedFilter) {
        BattleJournalFilter.All -> entries
        BattleJournalFilter.RadiantFound -> entries.filter { entry ->
            entry.encounterType == EncounterType.RadiantWild
        }
        BattleJournalFilter.Battles -> entries.filterNot(BattleJournalEntry::isRadiantDiscovery)
    }
    val entriesByDate = filteredEntries.groupBy { entry ->
        Date(entry.timestampMillis).toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
    }

    if (entries.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                AppIcon(
                    icon = LocalMonsterAppIcons.current.battleJournal,
                    contentDescription = null,
                    modifier = Modifier.size(56.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = stringResource(R.string.battle_journal_empty_title),
                        style = MaterialTheme.typography.headlineSmall,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        text = stringResource(R.string.battle_journal_empty_description),
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    } else {
        Column(modifier = Modifier.fillMaxSize()) {
            BattleJournalFilterChips(
                selectedFilter = selectedFilter,
                onFilterSelected = { selectedFilter = it },
            )
            if (filteredEntries.isEmpty()) {
                JournalFilterEmptyState()
            } else {
                LazyColumn(
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 100.dp),
                ) {
                    entriesByDate.forEach { (_, entriesForDate) ->
                        item(key = "header-${entriesForDate.first().timestampMillis}") {
                            Text(
                                text = DateFormat.getDateInstance(DateFormat.MEDIUM)
                                    .format(Date(entriesForDate.first().timestampMillis)),
                                modifier = Modifier.padding(top = 4.dp, bottom = 7.dp),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        itemsIndexed(entriesForDate, key = { _, entry -> entry.id }) { index, entry ->
                            BattleJournalEntryRow(
                                entry = entry,
                                roundTop = index == 0,
                                roundBottom = index == entriesForDate.lastIndex,
                                onClick = { viewModel.shareRadiantDiscovery(entry) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BattleJournalFilterChips(
    selectedFilter: BattleJournalFilter,
    onFilterSelected: (BattleJournalFilter) -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
    ) {
        BattleJournalFilter.entries.forEach { filter ->
            FilterChip(
                selected = selectedFilter == filter,
                onClick = { onFilterSelected(filter) },
                label = { Text(stringResource(filter.labelRes)) },
            )
        }
    }
}

@Composable
private fun JournalFilterEmptyState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(R.string.battle_journal_no_matches_title),
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
            )
            Text(
                text = stringResource(R.string.battle_journal_no_matches_description),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun BattleJournalOverflowMenu(viewModel: BattleJournalViewModel = hiltViewModel()) {
    var isMenuExpanded by rememberSaveable { mutableStateOf(false) }
    var isClearConfirmationVisible by rememberSaveable { mutableStateOf(false) }

    IconButton(onClick = { isMenuExpanded = true }) {
        AppIcon(
            icon = LocalAppIcons.current.more,
            contentDescription = stringResource(R.string.battle_journal_more_options),
        )
    }
    DropdownMenu(
        expanded = isMenuExpanded,
        onDismissRequest = { isMenuExpanded = false },
    ) {
        DropdownMenuItem(
            text = { Text(stringResource(R.string.battle_journal_clear)) },
            onClick = {
                isMenuExpanded = false
                isClearConfirmationVisible = true
            },
            leadingIcon = {
                AppIcon(
                    icon = LocalAppIcons.current.delete,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
            },
        )
    }
    if (isClearConfirmationVisible) {
        AlertDialog(
            onDismissRequest = { isClearConfirmationVisible = false },
            title = { Text(stringResource(R.string.battle_journal_clear_confirmation_title)) },
            text = { Text(stringResource(R.string.battle_journal_clear_confirmation_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clear()
                        isClearConfirmationVisible = false
                    },
                ) {
                    Text(stringResource(R.string.battle_journal_clear))
                }
            },
            dismissButton = {
                TextButton(onClick = { isClearConfirmationVisible = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}

@Composable
private fun BattleJournalEntryRow(
    entry: BattleJournalEntry,
    roundTop: Boolean,
    roundBottom: Boolean,
    onClick: () -> Unit,
) {
    val locale = LocalLocale.current.platformLocale
    val isRadiantEncounter = entry.encounterType == EncounterType.RadiantWild
    val cardShape = RoundedCornerShape(
        topStart = if (roundTop) 20.dp else 2.dp,
        topEnd = if (roundTop) 20.dp else 2.dp,
        bottomStart = if (roundBottom) 20.dp else 2.dp,
        bottomEnd = if (roundBottom) 20.dp else 2.dp,
    )
    Surface(
        modifier = Modifier
            .padding(vertical = 1.dp)
            .then(
                if (entry.isRadiantDiscovery) {
                    Modifier.clickable(onClick = onClick)
                } else {
                    Modifier
                }
            ),
        shape = cardShape,
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        shadowElevation = 0.5.dp,
    ) {
        Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
            if (entry.isRadiantDiscovery) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(4.dp)
                        .background(MaterialTheme.colorScheme.primary),
                )
            } else {
                Spacer(modifier = Modifier.width(4.dp))
            }
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                JournalSprites(entry)
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
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isRadiantEncounter) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
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
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text(
                        text = opponentName,
                        style = MaterialTheme.typography.titleMedium,
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
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text = trainerName,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun JournalSprites(entry: BattleJournalEntry) {
    Row(
        modifier = Modifier.width(100.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        JournalSprite(
            sprite = entry.playerSprite,
            fallbackResource = R.drawable.back_sprite,
            contentDescription = stringResource(R.string.character_artwork, entry.playerMonsterName),
        )
        JournalSprite(
            sprite = entry.opponentSprite,
            fallbackResource = R.drawable.battle_unknown_monster,
            contentDescription = entry.opponentMonsterName?.let { name ->
                stringResource(R.string.character_artwork, name)
            },
        )
    }
}

@Composable
private fun JournalSprite(
    sprite: BattleJournalSprite?,
    fallbackResource: Int,
    contentDescription: String?,
) {
    val modifier = Modifier.size(44.dp)
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
