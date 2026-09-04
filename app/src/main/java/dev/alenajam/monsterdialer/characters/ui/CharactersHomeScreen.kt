package dev.alenajam.monsterdialer.characters.ui

import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.annotation.DrawableRes
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import dev.alenajam.monsterdialer.R
import dev.alenajam.monsterdialer.app.ui.LocalMonsterAppIcons
import dev.alenajam.monsterdialer.characters.data.DefaultMonsterLevel
import dev.alenajam.monsterdialer.characters.data.MaxPlayerMonsterTeamSize
import dev.alenajam.monsterdialer.characters.data.SharedCharacterImport
import dev.alenajam.monsterdialer.onlineprofiles.ui.OnlineProfileSection
import dev.alenajam.monsterdialer.packs.data.CharacterReference
import dev.alenajam.monsterdialer.packs.data.CharacterType
import dev.alenajam.opendialer.core.common.ui.AppIcon
import dev.alenajam.opendialer.core.common.ui.IconSource
import dev.alenajam.opendialer.core.common.ui.LocalAppIcons
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@Composable
fun CharactersHomeScreen(
    onOpenSubpage: (Int, String?) -> Unit,
    sharingViewModel: CharacterSharingViewModel = hiltViewModel(),
    playerProfile: PlayerProfile,
    profileMetrics: ProfileMetrics,
    onReorderRoster: (List<CharacterReference>) -> Unit,
    onRemoveRosterMonster: (CharacterReference) -> Unit,
    showImportUi: Boolean = true,
) {
    val context = LocalContext.current
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) sharingViewModel.preview(context, uri)
    }

    if (showImportUi) {
        SharedCharacterImportHandler(sharingViewModel)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        TeamProfileCard(
            playerProfile = playerProfile,
            profileMetrics = profileMetrics,
            onChangeTrainer = { onOpenSubpage(0, PlayerCharacterSettingsRoute.ChangeTrainer.payload) },
            onChangeMonster = { onOpenSubpage(0, "${PlayerCharacterSettingsRoute.AddToRoster.payload}:0") }
        )
        RosterSection(
            roster = playerProfile.roster,
            onOpenRoster = { onOpenSubpage(0, PlayerCharacterSettingsRoute.AddToRoster.payload) },
            onOpenRosterSlot = { index ->
                onOpenSubpage(0, "${PlayerCharacterSettingsRoute.AddToRoster.payload}:$index")
            },
            onReorderRoster = onReorderRoster,
            onRemoveRosterMonster = onRemoveRosterMonster,
        )
        OnlineProfileSection()
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = stringResource(R.string.character_tools_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            CharacterToolsGroup(
                onOpenJournal = { onOpenSubpage(3, null) },
                onOpenPacks = { onOpenSubpage(2, null) },
                onImport = { picker.launch(arrayOf("*/*")) },
            )
        }
    }
}

@Composable
private fun RosterSection(
    roster: List<PlayerRosterMonster>,
    onOpenRoster: () -> Unit,
    onOpenRosterSlot: (Int) -> Unit,
    onReorderRoster: (List<CharacterReference>) -> Unit,
    onRemoveRosterMonster: (CharacterReference) -> Unit,
) {
    var orderedRoster by remember(roster) { mutableStateOf(roster) }
    var hasReordered by remember { mutableStateOf(false) }
    val lazyListState = rememberLazyListState()
    val reorderableState = rememberReorderableLazyListState(lazyListState) { from, to ->
        orderedRoster = orderedRoster.toMutableList().apply {
            add(to.index, removeAt(from.index))
        }
        hasReordered = true
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.your_roster),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            ContextualGuideButton(
                contents = listOf(GuideContent(R.string.characters_help_roster_title, R.string.characters_help_roster_message)),
            )
        }
        LazyRow(
            state = lazyListState,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            itemsIndexed(
                items = orderedRoster,
                key = { _, monster -> monster.reference?.rosterKey() ?: "built-in-monster" },
            ) { index, monster ->
                val reference = monster.reference
                if (reference == null) {
                    val interactionSource = remember { MutableInteractionSource() }
                    RosterMonsterTile(
                        monster = monster,
                        isDragged = false,
                        onRemove = {},
                        isRemoveEnabled = false,
                        onClick = { onOpenRosterSlot(index) },
                        dragHandle = {},
                        interactionSource = interactionSource,
                    )
                } else {
                    ReorderableItem(reorderableState, key = reference.rosterKey()) { isDragging ->
                        val interactionSource = remember { MutableInteractionSource() }
                        RosterMonsterTile(
                            monster = monster,
                            isDragged = isDragging,
                            onRemove = { onRemoveRosterMonster(reference) },
                            isRemoveEnabled = orderedRoster.size > 1,
                            onClick = { onOpenRosterSlot(index) },
                            interactionSource = interactionSource,
                            dragHandle = {
                                AppIcon(
                                    LocalMonsterAppIcons.current.reorder,
                                    contentDescription = stringResource(R.string.reorder_monster),
                                    modifier = Modifier
                                        .size(24.dp)
                                        .padding(4.dp)
                                        .longPressDraggableHandle(
                                            onDragStarted = {},
                                            onDragStopped = {
                                                if (hasReordered) {
                                                    onReorderRoster(orderedRoster.mapNotNull(PlayerRosterMonster::reference))
                                                }
                                                hasReordered = false
                                            },
                                            interactionSource = interactionSource,
                                        )
                                )
                            },
                        )
                    }
                }
            }
            item(key = "add-monster") {
                val context = androidx.compose.ui.platform.LocalContext.current
                val rosterFullMessage = stringResource(R.string.roster_full_message)
                RosterAddTile(
                    onClick = {
                        if (orderedRoster.size < MaxPlayerMonsterTeamSize) {
                            onOpenRoster()
                        } else {
                            android.widget.Toast.makeText(context, rosterFullMessage, android.widget.Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }
        }
    }
}

private fun CharacterReference.rosterKey(): String = "$packId:$characterId:$variantId"

private val RosterAddTileHeight = 132.dp

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RosterMonsterTile(
    monster: PlayerRosterMonster,
    isDragged: Boolean,
    onRemove: () -> Unit,
    isRemoveEnabled: Boolean,
    onClick: () -> Unit,
    dragHandle: @Composable () -> Unit,
    interactionSource: MutableInteractionSource,
    modifier: Modifier = Modifier,
) {
    var showMenu by remember { mutableStateOf(false) }

    Box {
        Card(
            modifier = Modifier
                .width(82.dp)
                .then(modifier),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (monster.isActive) {
                    MaterialTheme.colorScheme.secondaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceContainerLow
                }
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = if (isDragged) 8.dp else 0.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .indication(
                        interactionSource = interactionSource,
                        indication = androidx.compose.foundation.LocalIndication.current,
                    ),
            ) {
                RosterMonsterContent(
                    monster = monster,
                    dragHandle = dragHandle,
                    interactionSource = interactionSource,
                    onClick = onClick,
                    onLongClick = { showMenu = true },
                )
            }
        }

        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false }
        ) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.remove_roster_monster)) },
                onClick = {
                    showMenu = false
                    onRemove()
                },
                enabled = isRemoveEnabled,
                leadingIcon = {
                    AppIcon(LocalAppIcons.current.delete, contentDescription = null, modifier = Modifier.size(18.dp))
                }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RosterMonsterContent(
    monster: PlayerRosterMonster,
    dragHandle: @Composable () -> Unit,
    interactionSource: MutableInteractionSource,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(24.dp),
            contentAlignment = Alignment.Center
        ) {
            dragHandle()
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick,
                    onLongClick = onLongClick,
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            TeamArtwork(
                artwork = monster.character.artwork,
                fallbackArtwork = monster.character.fallbackArtwork,
                contentDescription = monster.character.name,
                modifier = Modifier.padding(vertical = 4.dp).size(54.dp),
            )
            Spacer(Modifier.height(4.dp))
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                horizontalAlignment = Alignment.Start,
            ) {
                Text(monster.character.name, style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    text = stringResource(R.string.roster_monster_level, monster.character.level ?: DefaultMonsterLevel),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun RosterAddTile(onClick: () -> Unit) {
    val outlineColor = MaterialTheme.colorScheme.outlineVariant
    val containerColor = MaterialTheme.colorScheme.surfaceContainerLow
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .width(82.dp)
            .height(RosterAddTileHeight)
            .clip(RoundedCornerShape(18.dp))
            .background(containerColor, RoundedCornerShape(18.dp))
            .drawBehind {
                val strokeWidth = 1.dp.toPx()
                drawRoundRect(
                    color = outlineColor,
                    topLeft = androidx.compose.ui.geometry.Offset(strokeWidth / 2, strokeWidth / 2),
                    size = size.copy(width = size.width - strokeWidth, height = size.height - strokeWidth),
                    cornerRadius = CornerRadius(18.dp.toPx()),
                    style = Stroke(
                        width = strokeWidth,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx())),
                    ),
                )
            }
            .clickable(onClick = onClick),
    ) {
        AppIcon(
            LocalMonsterAppIcons.current.addCharacter,
            contentDescription = stringResource(R.string.add_monster),
            modifier = Modifier.size(32.dp),
        )
    }
}


@Composable
private fun TeamProfileCard(
    playerProfile: PlayerProfile,
    profileMetrics: ProfileMetrics,
    onChangeTrainer: () -> Unit,
    onChangeMonster: () -> Unit,
) {
    val trainer = playerProfile.trainer
    val monster = playerProfile.monster
    val trainerTitle = stringResource(R.string.character_type_trainer)
    val monsterTitle = stringResource(R.string.character_type_monster)
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(30.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(
            modifier = Modifier
                .background(
                    Brush.linearGradient(
                        listOf(
                            MaterialTheme.colorScheme.primaryContainer,
                            MaterialTheme.colorScheme.tertiaryContainer,
                            MaterialTheme.colorScheme.secondaryContainer,
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .clickable(onClick = onChangeTrainer)
                        .padding(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    TeamArtwork(
                        artwork = trainer.artwork,
                        fallbackArtwork = trainer.fallbackArtwork,
                        contentDescription = stringResource(R.string.default_character_artwork, trainerTitle.lowercase()),
                        modifier = Modifier.size(128.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.profile_trainer_label),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.72f)
                        )
                        Text(
                            text = trainer.name,
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            maxLines = 1,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        ProfileMetricColumn(profileMetrics)
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.16f))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .clickable(onClick = onChangeMonster)
                        .padding(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.active_monster_label),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.72f),
                        )
                        Text(
                            text = monster.name,
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = stringResource(
                                R.string.active_monster_variant_and_level,
                                stringResource(if (monster.isRadiant) R.string.radiant else R.string.regular),
                                stringResource(
                                    R.string.roster_monster_level,
                                    monster.level ?: DefaultMonsterLevel,
                                ),
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.72f),
                        )
                    }
                    TeamArtwork(
                        artwork = monster.artwork,
                        fallbackArtwork = monster.fallbackArtwork,
                        contentDescription = stringResource(R.string.default_character_artwork, monsterTitle.lowercase()),
                        modifier = Modifier.size(80.dp),
                    )
                }
            }
            ContextualGuideButton(
                contents = listOf(GuideContent(R.string.characters_help_team_title, R.string.characters_help_team_message)),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp),
            )
        }
    }
}

@Composable
private fun ProfileMetricColumn(metrics: ProfileMetrics) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        listOf(
            ProfileMetric(label = pluralStringResource(R.plurals.calls_battled, metrics.callsBattled, metrics.callsBattled)),
            ProfileMetric(label = pluralStringResource(R.plurals.characters_collected, metrics.charactersCollected, metrics.charactersCollected)),
            ProfileMetric(label = pluralStringResource(R.plurals.radiants_found, metrics.radiantsFound, metrics.radiantsFound)),
        ).forEach { metric ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                metric.value?.let { value ->
                    Text(value.toString(), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                }
                Text(metric.label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.76f), maxLines = 1)
            }
        }
    }
}

private data class ProfileMetric(val value: Int? = null, val label: String)

@Composable
private fun TeamArtwork(
    artwork: java.io.File?,
    fallbackArtwork: Int?,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        val artworkModifier = Modifier.fillMaxSize()
        if (artwork == null) {
            Image(
                painter = painterResource(requireNotNull(fallbackArtwork)),
                contentDescription = contentDescription,
                contentScale = ContentScale.Fit,
                modifier = artworkModifier
            )
        } else {
            AsyncImage(
                model = artwork,
                contentDescription = contentDescription,
                contentScale = ContentScale.Fit,
                modifier = artworkModifier
            )
        }
    }
}

@Composable
private fun CharacterToolsGroup(
    onOpenJournal: () -> Unit,
    onOpenPacks: () -> Unit,
    onImport: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
        ) {
            CharacterToolRow(
                title = stringResource(R.string.battle_journal_title),
                icon = LocalMonsterAppIcons.current.battleJournal,
                onClick = onOpenJournal,
            )
            HorizontalDivider(
                modifier = Modifier.padding(start = 56.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )
            CharacterToolRow(
                title = stringResource(R.string.settings_character_packs_title),
                icon = LocalMonsterAppIcons.current.characterPacks,
                onClick = onOpenPacks,
            )
            HorizontalDivider(
                modifier = Modifier.padding(start = 56.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )
            CharacterToolRow(
                title = stringResource(R.string.import_character),
                icon = LocalMonsterAppIcons.current.importCharacter,
                onClick = onImport,
            )
        }
    }
}

@Composable
private fun CharacterToolRow(
    title: String,
    icon: IconSource,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        AppIcon(icon, contentDescription = null, modifier = Modifier.size(24.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
        }
        AppIcon(LocalAppIcons.current.arrowRight, contentDescription = null, modifier = Modifier.size(18.dp))
    }
}

@Composable
fun SharedCharacterImportHandler(sharingViewModel: CharacterSharingViewModel) {
    val preview by sharingViewModel.preview.collectAsStateWithLifecycle()
    val hasImportError by sharingViewModel.hasImportError.collectAsStateWithLifecycle()

    preview?.let { shared -> SharedCharacterImportDialog(shared, sharingViewModel::importPreview, sharingViewModel::dismissPreview) }
    if (hasImportError) {
        AlertDialog(
            onDismissRequest = sharingViewModel::dismissImportError,
            title = { Text(stringResource(R.string.shared_character_import_failed_title)) },
            text = { Text(stringResource(R.string.shared_character_import_failed_message)) },
            confirmButton = {
                TextButton(onClick = sharingViewModel::dismissImportError) {
                    Text(stringResource(R.string.close))
                }
            }
        )
    }
}

@Composable
private fun SharedCharacterImportDialog(
    shared: SharedCharacterImport,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val characterType = stringResource(
        if (shared.character.type == CharacterType.Trainer) R.string.character_type_trainer
        else R.string.character_type_monster
    ).lowercase()
    val artwork = remember(shared.frontImage, shared.backImage) {
        (shared.frontImage ?: shared.backImage)?.let { BitmapFactory.decodeByteArray(it, 0, it.size)?.asImageBitmap() }
    }
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 6.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Text(
                    stringResource(R.string.shared_character_import_title, shared.character.name),
                    style = MaterialTheme.typography.headlineSmall
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (artwork != null) {
                        Image(artwork, contentDescription = stringResource(R.string.character_artwork, shared.character.name), modifier = Modifier.size(72.dp))
                    } else {
                        AppIcon(LocalMonsterAppIcons.current.frontSprite, contentDescription = null, modifier = Modifier.size(48.dp))
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(stringResource(R.string.creator_label), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(shared.character.creator, style = MaterialTheme.typography.bodyLarge)
                        Text(stringResource(R.string.license_label), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(shared.character.license, style = MaterialTheme.typography.bodyLarge)
                    }
                }
                Text(
                    stringResource(R.string.shared_character_import_description, characterType),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)) {
                    TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
                    TextButton(onClick = onConfirm) { Text(stringResource(R.string.add_to_your_characters)) }
                }
            }
        }
    }
}
