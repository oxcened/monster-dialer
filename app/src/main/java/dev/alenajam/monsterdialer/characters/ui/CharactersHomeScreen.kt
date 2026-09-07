package dev.alenajam.monsterdialer.characters.ui

import android.graphics.BitmapFactory
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.annotation.DrawableRes
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import dev.alenajam.monsterdialer.R
import dev.alenajam.monsterdialer.app.ui.LocalMonsterAppIcons
import dev.alenajam.monsterdialer.app.ui.RetroScreenHorizontalPadding
import dev.alenajam.monsterdialer.characters.data.DefaultMonsterLevel
import dev.alenajam.monsterdialer.characters.data.MaxPlayerMonsterTeamSize
import dev.alenajam.monsterdialer.characters.data.SharedCharacterImport
import dev.alenajam.monsterdialer.packs.data.CharacterReference
import dev.alenajam.monsterdialer.packs.data.CharacterType
import dev.alenajam.opendialer.core.common.ui.AppIcon
import dev.alenajam.opendialer.core.common.ui.LocalAppIcons
import dev.alenajam.monsterdialer.app.ui.RetroSelectableRow
import dev.alenajam.monsterdialer.app.ui.RetroTextBox
import dev.alenajam.monsterdialer.app.ui.RetroActionButton
import dev.alenajam.monsterdialer.app.ui.RetroFooter
import dev.alenajam.monsterdialer.app.ui.RetroScreenBottomContentPadding
import dev.alenajam.monsterdialer.app.ui.RetroScreenTopContentPadding
import dev.alenajam.monsterdialer.app.ui.RetroProfilePanel
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

private val ProfilePixelFont = FontFamily(Font(R.font.ui_pixel_font))
private val ProfilePixelTextStyle = androidx.compose.ui.text.TextStyle(
    fontFamily = ProfilePixelFont,
)

private enum class ProfileCharacterSection { Trainer, Monster }

@Composable
fun CharactersHomeScreen(
    onOpenSettings: () -> Unit,
    onOpenSubpage: (Int, String?) -> Unit,
    sharingViewModel: CharacterSharingViewModel = hiltViewModel(),
    playerProfile: PlayerProfile,
    profileMetrics: ProfileMetrics,
    onReorderRoster: (List<CharacterReference>) -> Unit,
    onRemoveRosterMonster: (CharacterReference) -> Unit,
    showImportUi: Boolean = true,
    onSetBackAction: (Boolean, () -> Unit) -> Unit = { _, _ -> },
) {
    if (showImportUi) {
        SharedCharacterImportHandler(sharingViewModel)
    }

    val pageModifier = Modifier
        .fillMaxSize()
        .background(MaterialTheme.colorScheme.background)
        .then(
            Modifier.verticalScroll(rememberScrollState()),
        )
        .padding(
            start = RetroScreenHorizontalPadding,
            top = RetroScreenTopContentPadding,
            end = RetroScreenHorizontalPadding,
            bottom = RetroScreenBottomContentPadding,
        )

    Column(
        modifier = pageModifier,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        GameBoyProfileLayout(
                playerProfile = playerProfile,
                profileMetrics = profileMetrics,
                onChangeTrainer = { onOpenSubpage(CharacterSettingsPage.PlayerCharacter.index, PlayerCharacterSettingsRoute.ChangeTrainer.payload) },
                onChangeMonster = { onOpenSubpage(CharacterSettingsPage.PlayerCharacter.index, "${PlayerCharacterSettingsRoute.AddToRoster.payload}:0") },
                onOpenRoster = {
                    onOpenSubpage(CharacterSettingsPage.PlayerCharacter.index, PlayerCharacterSettingsRoute.AddToRoster.payload)
                },
                onOpenOnlineProfile = { onOpenSubpage(CharacterSettingsPage.ProfileLink.index, null) },
                onOpenToolbox = { onOpenSubpage(CharacterSettingsPage.Toolbox.index, null) },
                onOpenOptions = onOpenSettings,
        )
    }
}

@Composable
private fun GameBoyProfileLayout(
    playerProfile: PlayerProfile,
    profileMetrics: ProfileMetrics,
    onChangeTrainer: () -> Unit,
    onChangeMonster: () -> Unit,
    onOpenRoster: () -> Unit,
    onOpenOnlineProfile: () -> Unit,
    onOpenToolbox: () -> Unit,
    onOpenOptions: () -> Unit,
) {
    val trainer = playerProfile.trainer
    val monster = playerProfile.monster
    val trainerTitle = stringResource(R.string.character_type_trainer)
    val monsterTitle = stringResource(R.string.character_type_monster)
    var selectedSection by remember { mutableStateOf(ProfileCharacterSection.Trainer) }
    Box(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            GameBoyPanel {
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable {
                            selectedSection = ProfileCharacterSection.Trainer
                            onChangeTrainer()
                        },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            GameBoyText(
                                text = stringResource(R.string.profile_trainer_field, trainer.name)
                                    .withProfileCursor(selectedSection == ProfileCharacterSection.Trainer),
                                16.sp,
                            )
                            GameBoyStat(stringResource(R.string.profile_calls_label), profileMetrics.callsBattled.toString())
                            GameBoyStat(stringResource(R.string.profile_collection_label), profileMetrics.charactersCollected.toString())
                            GameBoyStat(stringResource(R.string.profile_radiants_label), profileMetrics.radiantsFound.toString())
                        }
                        TeamArtwork(
                            trainer.artwork,
                            trainer.fallbackArtwork,
                            stringResource(R.string.default_character_artwork, trainerTitle.lowercase()),
                            Modifier.size(96.dp),
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable {
                            selectedSection = ProfileCharacterSection.Monster
                            onChangeMonster()
                        },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            GameBoyText(
                                text = stringResource(R.string.profile_monster_field, monster.name)
                                    .withProfileCursor(selectedSection == ProfileCharacterSection.Monster),
                                16.sp,
                            )
                            GameBoyText(stringResource(R.string.profile_level_field, monster.level ?: DefaultMonsterLevel), 16.sp)
                        }
                        TeamArtwork(
                            monster.artwork,
                            monster.fallbackArtwork,
                            stringResource(R.string.default_character_artwork, monsterTitle.lowercase()),
                            Modifier.size(72.dp),
                        )
                    }
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Top,
            ) {
                GameBoyPanel(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    GameBoyText(stringResource(R.string.profile_roster_description), 16.sp)
                }
                GameBoyPanel(modifier = Modifier.width(220.dp).fillMaxHeight(), fillWidth = false) {
                    Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                        GameBoyMenuItem(stringResource(R.string.profile_menu_roster), true, onOpenRoster)
                        GameBoyMenuItem(stringResource(R.string.profile_menu_online), false, onOpenOnlineProfile)
                        GameBoyMenuItem(stringResource(R.string.profile_menu_toolbox), false, onOpenToolbox)
                        GameBoyMenuItem(stringResource(R.string.profile_menu_options), false, onOpenOptions)
                    }
                }
            }
        }
    }
}

@Composable
private fun String.withProfileCursor(selected: Boolean): String =
    takeIf { selected }?.let { stringResource(R.string.profile_menu_cursor, it) } ?: this

@Composable
private fun GameBoyPanel(
    modifier: Modifier = Modifier,
    fillWidth: Boolean = true,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    RetroProfilePanel(
        modifier = modifier
            .then(if (fillWidth) Modifier.fillMaxWidth() else Modifier)
            .then(if (onClick == null) Modifier else Modifier.clickable(onClick = onClick)),
        content = content,
    )
}

@Composable
private fun GameBoyText(
    text: String,
    size: androidx.compose.ui.unit.TextUnit,
    color: Color = RetroInk,
) {
    Text(text, style = androidx.compose.ui.text.TextStyle(fontFamily = ProfilePixelFont, fontSize = size, lineHeight = size * 1.15f), color = color)
}

@Composable
private fun GameBoyStat(label: String, value: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        GameBoyText(label, 16.sp)
        GameBoyText(value, 16.sp)
    }
}

@Composable
private fun GameBoyMenuItem(text: String, selected: Boolean = false, onClick: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 1.dp),
    ) {
        GameBoyText(stringResource(R.string.profile_menu_cursor, text).takeIf { selected } ?: text, 15.sp)
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

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.profile_roster_title),
                style = MaterialTheme.typography.titleMedium.merge(ProfilePixelTextStyle),
                color = RetroInk,
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
        RetroPanel(
            modifier = Modifier
                .width(92.dp)
                .then(modifier),
            selected = monster.isActive,
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
                Text(monster.character.name, style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis, fontFamily = ProfilePixelFont)
                Text(
                    text = stringResource(R.string.roster_monster_level, monster.character.level ?: DefaultMonsterLevel),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontFamily = ProfilePixelFont,
                )
            }
        }
    }
}

@Composable
private fun RosterAddTile(onClick: () -> Unit) {
    val outlineColor = RetroInk
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .width(92.dp)
            .height(RosterAddTileHeight)
            .background(RetroPaper, androidx.compose.ui.graphics.RectangleShape)
            .drawBehind {
                val strokeWidth = 1.dp.toPx()
                drawRoundRect(
                    color = outlineColor,
                    topLeft = androidx.compose.ui.geometry.Offset(strokeWidth / 2, strokeWidth / 2),
                    size = size.copy(width = size.width - strokeWidth, height = size.height - strokeWidth),
                    cornerRadius = CornerRadius(0.dp.toPx()),
                    style = Stroke(
                        width = strokeWidth,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx())),
                    ),
                )
            }
            .clickable(onClick = onClick),
    ) {
        Text(stringResource(R.string.profile_add_roster), style = MaterialTheme.typography.displaySmall, color = RetroInk, fontFamily = ProfilePixelFont)
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
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .border(2.dp, RetroInk, androidx.compose.ui.graphics.RectangleShape)
            .background(RetroPaper, androidx.compose.ui.graphics.RectangleShape)
            .padding(14.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.profile_player_label),
                    style = MaterialTheme.typography.headlineSmall.merge(ProfilePixelTextStyle),
                    color = RetroInk,
                )
                Spacer(Modifier.weight(1f))
                ContextualGuideButton(
                    contents = listOf(GuideContent(R.string.characters_help_team_title, R.string.characters_help_team_message)),
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth().clickable(onClick = onChangeTrainer),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    RetroProfileLine(stringResource(R.string.profile_name_label), trainer.name)
                    RetroProfileLine(stringResource(R.string.profile_status_title), stringResource(R.string.character_type_trainer))
                    RetroProfileLine(stringResource(R.string.profile_calls_label), profileMetrics.callsBattled.toString())
                    RetroProfileLine(stringResource(R.string.profile_collection_label), profileMetrics.charactersCollected.toString())
                }
                TeamArtwork(
                    artwork = trainer.artwork,
                    fallbackArtwork = trainer.fallbackArtwork,
                    contentDescription = stringResource(R.string.default_character_artwork, trainerTitle.lowercase()),
                    modifier = Modifier.size(112.dp),
                )
            }
            HorizontalDivider(color = RetroInk, thickness = 1.dp)
            Row(
                modifier = Modifier.fillMaxWidth().clickable(onClick = onChangeMonster),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    RetroProfileLine(stringResource(R.string.active_monster_label).uppercase(), monster.name)
                    RetroProfileLine(
                        stringResource(R.string.roster_monster_level, monster.level ?: DefaultMonsterLevel),
                        stringResource(if (monster.isRadiant) R.string.radiant else R.string.regular),
                    )
                }
                TeamArtwork(
                    artwork = monster.artwork,
                    fallbackArtwork = monster.fallbackArtwork,
                    contentDescription = stringResource(R.string.default_character_artwork, monsterTitle.lowercase()),
                    modifier = Modifier.size(76.dp),
                )
            }
        }
    }
}

@Composable
private fun RetroProfileLine(label: String, value: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium.merge(ProfilePixelTextStyle),
            color = RetroInk,
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium.merge(ProfilePixelTextStyle),
            color = RetroInk,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
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
                Text(value.toString(), style = MaterialTheme.typography.titleSmall, color = RetroInk, fontFamily = ProfilePixelFont)
                }
                Text(metric.label.uppercase(), style = MaterialTheme.typography.labelSmall, color = RetroInk, maxLines = 1, fontFamily = ProfilePixelFont)
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
internal fun CharacterToolsContent(
    onOpenContactCharacters: () -> Unit,
    onOpenContactDefaults: () -> Unit,
    onOpenJournal: () -> Unit,
    onOpenPacks: () -> Unit,
    onImport: () -> Unit,
    onBack: () -> Unit,
) {
    val actions = listOf(
        CharacterToolAction(stringResource(R.string.contact_defaults_toolbox_title), onOpenContactDefaults),
        CharacterToolAction(stringResource(R.string.settings_contact_characters_title), onOpenContactCharacters),
        CharacterToolAction(stringResource(R.string.settings_character_packs_title), onOpenPacks),
        CharacterToolAction(stringResource(R.string.import_character), onImport),
        CharacterToolAction(stringResource(R.string.battle_journal_title), onOpenJournal),
    )
    var selectedIndex by remember { mutableStateOf(0) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxWidth()) {
            actions.forEachIndexed { index, action ->
                CharacterToolRow(
                    title = action.title,
                    selected = index == selectedIndex,
                    onClick = {
                        selectedIndex = index
                        action.onClick()
                    },
                )
            }
        }
        RetroFooter(
            message = stringResource(R.string.character_tools_prompt),
            animationKey = selectedIndex,
            backKey = stringResource(R.string.retro_key_b),
            backLabel = stringResource(R.string.customized_contacts_back_action),
            onBack = onBack,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

private data class CharacterToolAction(
    val title: String,
    val onClick: () -> Unit,
)

@Composable
private fun CharacterToolRow(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    RetroSelectableRow(selected = selected, onClick = onClick) {
        Text(
            text = title.uppercase(),
            fontFamily = ProfilePixelFont,
            fontSize = 16.sp,
            color = RetroInk,
        )
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
