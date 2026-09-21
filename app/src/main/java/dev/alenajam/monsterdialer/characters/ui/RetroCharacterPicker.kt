package dev.alenajam.monsterdialer.characters.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import dev.alenajam.monsterdialer.R
import dev.alenajam.monsterdialer.app.ui.LocalMonsterAppIcons
import dev.alenajam.monsterdialer.app.ui.RetroSelectableRow
import dev.alenajam.monsterdialer.app.ui.RetroContextMenuItem
import dev.alenajam.monsterdialer.app.ui.RetroContextMenuOverlay
import dev.alenajam.monsterdialer.app.ui.RetroActionButton
import dev.alenajam.monsterdialer.app.ui.RetroSearchButton
import dev.alenajam.monsterdialer.app.ui.RetroDoubleBorderTextBox
import dev.alenajam.monsterdialer.app.ui.RetroFooter
import dev.alenajam.monsterdialer.app.ui.RetroFooterAction
import dev.alenajam.monsterdialer.app.ui.RetroFastScroller
import dev.alenajam.monsterdialer.characters.data.BuiltInCharacter
import dev.alenajam.monsterdialer.characters.data.BuiltInCharacters
import dev.alenajam.monsterdialer.packs.data.CharacterAssignmentTarget
import dev.alenajam.monsterdialer.packs.data.CharacterReference
import dev.alenajam.monsterdialer.packs.data.CharacterType
import dev.alenajam.monsterdialer.packs.data.InstalledPackCharacter
import dev.alenajam.monsterdialer.packs.data.CharacterVisualVariant
import dev.alenajam.opendialer.core.common.ui.AppIcon
import java.io.File

internal val RetroPickerFont = FontFamily(Font(R.font.ui_pixel_font))

private data class RetroCharacterEntry(
    val key: String,
    val reference: CharacterReference?,
    val name: String,
    val level: Int? = null,
    val isRadiant: Boolean = false,
    val artwork: File? = null,
    val builtInArtwork: Int? = null,
    val section: String,
)

@Composable
internal fun RetroCharacterPicker(
    modifier: Modifier = Modifier,
    type: CharacterType,
    selectionVersion: Int = 0,
    selected: CharacterReference?,
    characters: List<InstalledPackCharacter>,
    unlockedVariants: Set<CharacterReference>,
    filter: MonsterFilter,
    defaultCharacter: BuiltInCharacter,
    defaultArtwork: BuiltInCharacter.() -> Int,
    onAssign: (CharacterReference?) -> Unit,
    onBack: () -> Unit,
    isRandomMode: Boolean = false,
    isGuidedFirstStep: Boolean = false,
    randomPool: Set<CharacterReference> = emptySet(),
    defaultRandomPool: Set<CharacterReference> = randomPool,
    onRandomPoolDone: ((Set<CharacterReference>) -> Unit)? = null,
    onClear: (() -> Unit)? = null,
    showOptions: Boolean = true,
    onAddCharacter: (() -> Unit)? = null,
    addCharacterLabel: String? = null,
    onFilterSelected: ((MonsterFilter) -> Unit)? = null,
    showFilterOptions: Boolean = true,
    guideContents: List<GuideContent>? = null,
) {
    val entries = remember(type, characters, unlockedVariants, filter, defaultCharacter) {
        retroCharacterEntries(type, characters, unlockedVariants, filter, defaultCharacter, defaultArtwork)
    }
    var pendingSelection by remember(selected, selectionVersion) { mutableStateOf(selected) }
    var optionsOpen by remember(type, selectionVersion) { mutableStateOf(false) }
    var randomPoolOpen by remember(type, selectionVersion) { mutableStateOf(isRandomMode) }
    val enteredFromRandomMode = remember(type, selectionVersion) { isRandomMode }
    var poolCursor by remember(type, randomPool, selectionVersion) { mutableStateOf(randomPool.firstOrNull()) }
    var poolDraft by remember(type, randomPool, selectionVersion) { mutableStateOf(randomPool) }
    var assignmentCleared by remember(type, selectionVersion) { mutableStateOf(false) }
    var assignmentRandomized by remember(type, selectionVersion) { mutableStateOf(false) }
    var hasMadeSelection by remember(type, selectionVersion) { mutableStateOf(false) }
    var guideOpen by remember(type, selectionVersion) { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val selectedEntry = entries.firstOrNull { it.reference == pendingSelection }
    val hasConfirmedSelection = selected != null || hasMadeSelection
    val prompt = if (randomPoolOpen) {
        stringResource(if (type == CharacterType.Trainer) R.string.contact_picker_pool_trainers else R.string.contact_picker_pool_monsters)
    } else if (hasConfirmedSelection && pendingSelection != null) {
        stringResource(R.string.contact_picker_ready, selectedEntry?.name?.uppercase().orEmpty())
    } else {
        stringResource(
            when {
                type == CharacterType.Trainer && assignmentCleared -> R.string.contact_picker_no_trainer_assigned
                type == CharacterType.Monster && assignmentCleared -> R.string.contact_picker_no_monster_assigned
                type == CharacterType.Trainer && assignmentRandomized -> R.string.contact_picker_random_trainer
                type == CharacterType.Monster && assignmentRandomized -> R.string.contact_picker_random_monster
                type == CharacterType.Trainer -> R.string.contact_picker_choose_trainer
                else -> R.string.contact_picker_choose_monster
            },
        )
    }

    LaunchedEffect(selected, entries, selectionVersion) {
        pendingSelection = selected
        hasMadeSelection = false
    }
    BackHandler(enabled = optionsOpen) { optionsOpen = false }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            if (onAddCharacter != null && addCharacterLabel != null) {
                RetroSearchButton(
                    label = addCharacterLabel,
                    onClick = onAddCharacter,
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.Start,
                )
            }
            if (showOptions) {
                RetroSearchButton(
                    label = stringResource(R.string.contact_picker_options),
                    onClick = { optionsOpen = true },
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.End,
                )
            }
        }
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 2.dp, bottom = 2.dp),
            ) {
                itemsIndexed(entries, key = { _, entry -> entry.key }) { index, entry ->
                    if (index == 0 || entries[index - 1].section != entry.section) {
                        Text(
                            text = entry.section,
                            modifier = Modifier.fillMaxWidth().padding(top = 5.dp, bottom = 2.dp),
                            fontFamily = RetroPickerFont,
                            fontSize = 13.sp,
                            color = RetroInk,
                        )
                    }
                    RetroCharacterRow(
                        entry = entry,
                        isSelected = if (randomPoolOpen) {
                            entry.reference == poolCursor
                        } else {
                            entry.reference == pendingSelection
                        },
                        poolIncluded = if (randomPoolOpen) entry.reference in poolDraft else null,
                    ) {
                        if (randomPoolOpen) {
                            entry.reference?.let { reference ->
                                poolCursor = reference
                                poolDraft = if (reference in poolDraft) poolDraft - reference else poolDraft + reference
                            }
                        } else {
                            pendingSelection = entry.reference
                            hasMadeSelection = true
                            assignmentCleared = false
                            assignmentRandomized = false
                        }
                    }
                }
            }
            RetroFastScroller(
                listState = listState,
                contentDescription = stringResource(R.string.character_fast_scroller),
                modifier = Modifier.align(Alignment.CenterEnd).padding(vertical = 8.dp),
            )
        }
        RetroFooter(
            message = prompt,
            animationKey = "$type:$selectionVersion:$prompt",
            backKey = stringResource(R.string.retro_key_b),
            backLabel = stringResource(R.string.customized_contacts_back_action),
            onBack = {
                if (randomPoolOpen) {
                    if (enteredFromRandomMode) {
                        onBack()
                    } else {
                        randomPoolOpen = false
                        poolDraft = randomPool
                        poolCursor = randomPool.firstOrNull()
                    }
                } else if (hasMadeSelection) {
                    pendingSelection = selected
                    hasMadeSelection = false
                    assignmentCleared = false
                    assignmentRandomized = false
                    onBack()
                } else {
                    onBack()
                }
            },
            modifier = Modifier.align(Alignment.CenterHorizontally),
            leftAction = RetroFooterAction(
                key = stringResource(R.string.retro_key_a),
                label = stringResource(
                    when {
                        randomPoolOpen -> R.string.contact_picker_pool_done
                        isGuidedFirstStep -> R.string.contact_picker_next
                        else -> R.string.customized_contacts_assign_action
                    },
                ),
                enabled = if (randomPoolOpen) poolDraft.isNotEmpty() else hasConfirmedSelection && selectedEntry != null,
                onClick = {
                    if (randomPoolOpen) {
                        randomPoolOpen = false
                        onRandomPoolDone?.invoke(poolDraft)
                    } else {
                        onAssign(pendingSelection)
                    }
                },
            )
        )
    }
    if (optionsOpen) {
        Box(
            modifier = Modifier.fillMaxSize().clickable { optionsOpen = false },
            contentAlignment = Alignment.Center,
        ) {
            RetroContextMenuOverlay(
                modifier = Modifier.fillMaxWidth(0.68f),
                fontFamily = RetroPickerFont,
                onDismissRequest = { optionsOpen = false },
                items = (if (randomPoolOpen) {
                    listOf(
                        RetroContextMenuItem(label = stringResource(R.string.contact_picker_choose)) {
                            randomPoolOpen = false
                            poolDraft = randomPool
                            poolCursor = randomPool.firstOrNull()
                            optionsOpen = false
                        },
                        RetroContextMenuItem(label = stringResource(R.string.contact_random_pool_select_all)) {
                            poolDraft = entries.mapNotNull { it.reference }.toSet()
                            poolCursor = poolDraft.firstOrNull()
                            optionsOpen = false
                        },
                        RetroContextMenuItem(label = stringResource(R.string.contact_random_pool_deselect_all)) {
                            poolDraft = emptySet()
                            poolCursor = null
                            optionsOpen = false
                        },
                        RetroContextMenuItem(label = stringResource(R.string.contact_random_pool_reset)) {
                            poolDraft = defaultRandomPool
                            poolCursor = poolDraft.firstOrNull()
                            optionsOpen = false
                        },
                        RetroContextMenuItem.cancel(stringResource(R.string.cancel)) {
                            optionsOpen = false
                        },
                    )
                } else if (onFilterSelected != null && showFilterOptions) {
                    listOf(
                        MonsterFilter.All to R.string.filter_all,
                        MonsterFilter.Regular to R.string.filter_regular,
                        MonsterFilter.RadiantUnlocked to R.string.filter_unlocked_radiant,
                    ).map { (filterOption, labelRes) ->
                        RetroContextMenuItem(
                            label = stringResource(labelRes),
                            showCursor = filter == filterOption,
                        ) {
                            onFilterSelected(filterOption)
                            optionsOpen = false
                        }
                    } + RetroContextMenuItem.cancel(stringResource(R.string.cancel)) {
                        optionsOpen = false
                    }
                } else if (onClear != null) {
                    listOf(
                        RetroContextMenuItem(
                            label = stringResource(R.string.contact_default_random),
                        ) {
                            optionsOpen = false
                            randomPoolOpen = true
                            poolDraft = randomPool
                            poolCursor = randomPool.firstOrNull()
                        },
                        RetroContextMenuItem(label = stringResource(R.string.contact_picker_default)) {
                            optionsOpen = false
                            pendingSelection = null
                            assignmentCleared = true
                            assignmentRandomized = false
                            onClear?.invoke()
                        },
                        RetroContextMenuItem.cancel(stringResource(R.string.cancel)) {
                            optionsOpen = false
                        },
                    )
                } else {
                    listOf(
                        RetroContextMenuItem.cancel(stringResource(R.string.cancel)) {
                            optionsOpen = false
                        },
                    )
                }).let { items ->
                    if (guideContents == null) {
                        items
                    } else {
                        items.dropLast(1) + RetroContextMenuItem(
                            label = stringResource(R.string.retro_picker_guide),
                            dividerBefore = true,
                        ) {
                            optionsOpen = false
                            guideOpen = true
                        } + items.last()
                    }
                },
            )
        }
    }
    if (guideOpen && guideContents != null) {
        ContextualGuideDialog(contents = guideContents, onDismiss = { guideOpen = false })
    }
}
}

@Composable
private fun RetroCharacterRow(
    entry: RetroCharacterEntry,
    isSelected: Boolean,
    poolIncluded: Boolean? = null,
    onClick: () -> Unit,
) {
    RetroSelectableRow(selected = isSelected, onClick = onClick) {
        poolIncluded?.let {
            Text(
                text = stringResource(if (it) R.string.contact_picker_pool_included else R.string.contact_picker_pool_excluded),
                fontFamily = RetroPickerFont,
                fontSize = 16.sp,
                color = RetroInk,
                modifier = Modifier.padding(end = 2.dp),
            )
        }
        Box(modifier = Modifier.size(48.dp), contentAlignment = Alignment.Center) {
            when {
                entry.builtInArtwork != null -> Image(
                    painter = painterResource(entry.builtInArtwork),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                )
                entry.artwork != null -> AsyncImage(
                    model = entry.artwork,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
        Column(modifier = Modifier.padding(start = 4.dp), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(
                text = entry.name.uppercase(),
                fontFamily = RetroPickerFont,
                fontSize = 16.sp,
                color = RetroInk,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (entry.level != null) {
                val level = stringResource(R.string.roster_monster_level, entry.level)
                val metadata = if (entry.isRadiant) {
                    val variant = stringResource(R.string.radiant)
                    stringResource(R.string.retro_picker_variant_and_level, variant, level)
                } else {
                    level
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (entry.isRadiant) {
                        AppIcon(
                            icon = LocalMonsterAppIcons.current.radiant,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                        )
                    }
                    Text(
                        text = metadata,
                        fontFamily = RetroPickerFont,
                        fontSize = 13.sp,
                        color = RetroInk.copy(alpha = 0.75f),
                    )
                }
            }
        }
    }
}

private fun retroCharacterEntries(
    type: CharacterType,
    characters: List<InstalledPackCharacter>,
    unlockedVariants: Set<CharacterReference>,
    filter: MonsterFilter,
    defaultCharacter: BuiltInCharacter,
    defaultArtwork: BuiltInCharacter.() -> Int,
): List<RetroCharacterEntry> {
    val result = mutableListOf<RetroCharacterEntry>()
    if (type == CharacterType.Trainer || filter == MonsterFilter.All || filter == MonsterFilter.Regular) {
        result += RetroCharacterEntry(
            key = "default",
            reference = if (type == CharacterType.Trainer) BuiltInCharacters.defaultTrainerReference else BuiltInCharacters.defaultMonsterReference,
            name = defaultCharacter.name,
            artwork = null,
            builtInArtwork = defaultCharacter.defaultArtwork(),
            section = "BUILT-IN",
        )
    }
    characters.forEach { installed ->
        installed.character.visualVariants.forEach { variant ->
            val reference = CharacterReference(installed.packId, installed.character.id, variant.id)
            if (
                type == CharacterType.Monster &&
                (!filter.matches(variant, reference, unlockedVariants) ||
                    (variant.isRadiant && reference !in unlockedVariants))
            ) return@forEach
            val image = if (type == CharacterType.Trainer) variant.frontImage ?: variant.backImage else variant.frontImage ?: variant.backImage
            result += RetroCharacterEntry(
                key = "${installed.packId}:${installed.character.id}:${variant.id}",
                reference = reference,
                name = installed.character.name,
                level = installed.character.level,
                isRadiant = variant.isRadiant,
                artwork = image?.let(installed::imageFile),
                section = if (installed.isEditable) "YOUR ${if (type == CharacterType.Trainer) "TRAINERS" else "MONSTERS"}"
                else installed.packName.uppercase(),
            )
        }
    }
    return result
}

private fun MonsterFilter.matches(
    variant: CharacterVisualVariant,
    reference: CharacterReference,
    unlockedVariants: Set<CharacterReference>,
): Boolean = when (this) {
    MonsterFilter.All -> true
    MonsterFilter.Regular -> !variant.isRadiant
    MonsterFilter.RadiantUnlocked -> variant.isRadiant && reference in unlockedVariants
}
