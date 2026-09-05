package dev.alenajam.monsterdialer.characters.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.itemsIndexed as gridItemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import dev.alenajam.monsterdialer.R
import dev.alenajam.monsterdialer.app.ui.LocalMonsterAppIcons
import dev.alenajam.opendialer.core.common.ui.AppIcon
import dev.alenajam.opendialer.core.common.ui.LocalAppIcons
import dev.alenajam.monsterdialer.characters.data.BuiltInArtwork
import dev.alenajam.monsterdialer.characters.data.BuiltInCharacter
import dev.alenajam.monsterdialer.characters.data.BuiltInCharacters
import dev.alenajam.monsterdialer.characters.data.isBuiltInMonsterRosterReference
import dev.alenajam.monsterdialer.packs.data.CharacterReference
import dev.alenajam.monsterdialer.packs.data.CharacterAssignmentTarget
import dev.alenajam.monsterdialer.packs.data.CharacterVisualVariant
import dev.alenajam.monsterdialer.packs.data.CharacterType
import dev.alenajam.monsterdialer.packs.data.PackCharacter
import dev.alenajam.monsterdialer.packs.data.InstalledPackCharacter
import java.io.File
import kotlinx.coroutines.launch

enum class CharacterLayout { List, Grid }

@Composable
internal fun CharacterLayoutToggle(
    layout: CharacterLayout,
    onLayoutChanged: (CharacterLayout) -> Unit,
    modifier: Modifier = Modifier
) {
    val nextLayout = if (layout == CharacterLayout.List) CharacterLayout.Grid else CharacterLayout.List
    SmallFloatingActionButton(onClick = { onLayoutChanged(nextLayout) }, modifier = modifier) {
        AppIcon(
            icon = if (nextLayout == CharacterLayout.Grid) LocalMonsterAppIcons.current.viewGrid else LocalMonsterAppIcons.current.viewList,
            contentDescription = stringResource(
                if (nextLayout == CharacterLayout.Grid) R.string.show_grid_view else R.string.show_list_view
            )
        )
    }
}


internal fun LazyListScope.characterTypeItems(
    title: String,
    pluralTitle: String,
    defaultCharacter: BuiltInCharacter,
    characters: List<InstalledPackCharacter>,
    selected: CharacterReference?,
    defaultArtwork: (BuiltInCharacter) -> BuiltInArtwork,
    artworkTarget: CharacterAssignmentTarget,
    onSelect: (CharacterReference?) -> Unit,
    defaultReference: CharacterReference? = null,
    unlockedVariants: Set<CharacterReference> = emptySet(),
    onDelete: (InstalledPackCharacter) -> Unit = {},
    onEdit: (InstalledPackCharacter) -> Unit = {},
    onShare: (InstalledPackCharacter) -> Unit = {},
    isRandomSelected: Boolean = false,
    onRandomize: (() -> Unit)? = null,
    showRandomize: Boolean = true,
    selectedReferences: Set<CharacterReference> = emptySet(),
    onSelected: ((CharacterReference) -> Unit)? = null,
    hideSelected: Boolean = false,
    filter: MonsterFilter = MonsterFilter.All,
) {
    val type = if (defaultCharacter == BuiltInCharacters.trainer) CharacterType.Trainer else CharacterType.Monster
    val selectionState = characterSelectionState(
        characters = characters,
        selected = selected,
        selectedReferences = selectedReferences,
        defaultReference = defaultReference,
        allowsDeselection = onSelected != null,
        isRandomSelected = isRandomSelected,
    )
    fun isReferenceSelected(reference: CharacterReference): Boolean =
        selectionState.isReferenceSelected(reference, selectedReferences)

    fun CharacterSelection.matchesFilter(): Boolean {
        if (type != CharacterType.Monster) return true
        val reference = CharacterReference(installed.packId, installed.character.id, variant.id)
        return when (filter) {
            MonsterFilter.All -> true
            MonsterFilter.Regular -> !variant.isRadiant
            MonsterFilter.RadiantUnlocked -> variant.isRadiant && reference in unlockedVariants
            MonsterFilter.RadiantLocked -> variant.isRadiant && reference !in unlockedVariants
        }
    }

    val hasSelectableCharacter = characters.any { installed ->
        installed.selectionVariants().any { selection ->
            selection.matchesFilter() && (!hideSelected || !isReferenceSelected(CharacterReference(selection.installed.packId, selection.installed.character.id, selection.variant.id)))
        }
    }
    val effectiveRandomize = onRandomize?.takeIf { showRandomize }
    val hasNoCharacterOptions = effectiveRandomize == null && hideSelected && selectionState.isDefaultSelected && !hasSelectableCharacter

    if (effectiveRandomize != null) {
        item(key = "random") {
            CharacterOptionCard(
                name = stringResource(R.string.randomize),
                modifier = Modifier.padding(top = 8.dp),
                type = type,
                isSelected = isRandomSelected,
                showTypeSubtitle = false,
                roundTop = true,
                roundBottom = true,
                artwork = {
                    AppIcon(
                        icon = LocalMonsterAppIcons.current.randomize,
                        contentDescription = stringResource(R.string.randomize),
                        modifier = Modifier.size(40.dp),
                    )
                },
                onSelect = effectiveRandomize,
            )
        }
    }

    val userCharacters = characters.filter { it.isEditable }
    val otherPacks = characters.filter { !it.isEditable }.groupBy { it.packId }

    if (filter.allowsBuiltInMonster && (!hideSelected || !selectionState.isDefaultSelected)) {
        item { SectionHeader(stringResource(R.string.built_in_characters_section, pluralTitle)) }
        item(key = "default") {
            CharacterOptionCard(
                name = defaultCharacter.name,
                type = type,
                isSelected = selectionState.isDefaultSelected,
                roundTop = true,
                roundBottom = true,
                artwork = {
                    Image(
                        painter = painterResource(defaultArtwork(defaultCharacter).resource),
                        contentDescription = stringResource(R.string.default_character_artwork, title.lowercase()),
                        modifier = Modifier.size(72.dp)
                    )
                },
                onSelect = { onSelect(defaultReference) },
                onSelected = onSelected?.let { callback ->
                    { defaultReference?.let(callback) }
                },
            )
        }
    }

    if (userCharacters.isNotEmpty()) {
        val selections = userCharacters.flatMap(InstalledPackCharacter::selectionVariants)
            .filter { selection ->
                val reference = CharacterReference(selection.installed.packId, selection.installed.character.id, selection.variant.id)
                selection.matchesFilter() && (!hideSelected || !isReferenceSelected(reference))
            }
        if (selections.isNotEmpty()) {
            item { SectionHeader(stringResource(R.string.your_characters, pluralTitle)) }
            itemsIndexed(items = selections, key = { _, selection -> "custom:${selection.installed.character.id}:${selection.variant.id}" }) { index, selection ->
                val installed = selection.installed
                val reference = CharacterReference(installed.packId, installed.character.id, selection.variant.id)
                val isUnlocked = !selection.variant.isRadiant || reference in unlockedVariants
                CharacterOptionCard(
                    name = installed.character.name,
                    type = installed.character.type,
                    level = installed.character.level,
                    isRadiant = selection.variant.isRadiant,
                    isSelected = isReferenceSelected(reference),
                    isUnlocked = isUnlocked,
                    roundTop = index == 0,
                    roundBottom = index == selections.lastIndex,
                    artwork = {
                        AsyncImage(
                            model = selection.previewArtwork(artworkTarget),
                            contentDescription = stringResource(R.string.character_artwork, installed.character.name),
                            modifier = Modifier.size(72.dp)
                        )
                    },
                    onSelect = { if (isUnlocked) onSelect(reference) },
                    onSelected = onSelected?.let { callback -> { callback(reference) } },
                    onDelete = if (installed.isDeletable) { { onDelete(installed) } } else null,
                    onEdit = if (installed.isEditable) { { onEdit(installed) } } else null,
                    onShare = if (installed.isEditable) { { onShare(installed) } } else null
                )
            }
        }
    }

    otherPacks.forEach { (packId, packCharacters) ->
        val selections = packCharacters.flatMap(InstalledPackCharacter::selectionVariants)
            .filter { selection ->
                val reference = CharacterReference(selection.installed.packId, selection.installed.character.id, selection.variant.id)
                selection.matchesFilter() && (!hideSelected || !isReferenceSelected(reference))
            }
        if (selections.isNotEmpty()) {
            item { SectionHeader(packCharacters.first().packName) }
            itemsIndexed(items = selections, key = { _, selection -> "${packId}:${selection.installed.character.id}:${selection.variant.id}" }) { index, selection ->
                val installed = selection.installed
                val reference = CharacterReference(installed.packId, installed.character.id, selection.variant.id)
                val isUnlocked = !selection.variant.isRadiant || reference in unlockedVariants
                CharacterOptionCard(
                    name = installed.character.name,
                    type = installed.character.type,
                    level = installed.character.level,
                    isRadiant = selection.variant.isRadiant,
                    isSelected = isReferenceSelected(reference),
                    isUnlocked = isUnlocked,
                    roundTop = index == 0,
                    roundBottom = index == selections.lastIndex,
                    artwork = {
                        AsyncImage(
                            model = selection.previewArtwork(artworkTarget),
                            contentDescription = stringResource(R.string.character_artwork, installed.character.name),
                            modifier = Modifier.size(72.dp)
                        )
                    },
                    onSelect = { if (isUnlocked) onSelect(reference) },
                    onSelected = onSelected?.let { callback -> { callback(reference) } }
                )
            }
        }
    }

    if (hasNoCharacterOptions) {
        item(key = "no-options") { NoCharacterOptionsPlaceholder(pluralTitle) }
    }

}

internal fun LazyGridScope.characterTypeGridItems(
    title: String,
    pluralTitle: String,
    defaultCharacter: BuiltInCharacter,
    characters: List<InstalledPackCharacter>,
    selected: CharacterReference?,
    defaultArtwork: (BuiltInCharacter) -> BuiltInArtwork,
    artworkTarget: CharacterAssignmentTarget,
    onSelect: (CharacterReference?) -> Unit,
    defaultReference: CharacterReference? = null,
    unlockedVariants: Set<CharacterReference> = emptySet(),
    onDelete: (InstalledPackCharacter) -> Unit = {},
    onEdit: (InstalledPackCharacter) -> Unit = {},
    onShare: (InstalledPackCharacter) -> Unit = {},
    isRandomSelected: Boolean = false,
    onRandomize: (() -> Unit)? = null,
    showRandomize: Boolean = true,
    selectedReferences: Set<CharacterReference> = emptySet(),
    onSelected: ((CharacterReference) -> Unit)? = null,
    hideSelected: Boolean = false,
    filter: MonsterFilter = MonsterFilter.All,
) {
    val type = if (defaultCharacter == BuiltInCharacters.trainer) CharacterType.Trainer else CharacterType.Monster
    val selectionState = characterSelectionState(
        characters = characters,
        selected = selected,
        selectedReferences = selectedReferences,
        defaultReference = defaultReference,
        allowsDeselection = onSelected != null,
        isRandomSelected = isRandomSelected,
    )
    fun isReferenceSelected(reference: CharacterReference): Boolean =
        selectionState.isReferenceSelected(reference, selectedReferences)

    fun CharacterSelection.matchesFilter(): Boolean {
        if (type != CharacterType.Monster) return true
        val reference = CharacterReference(installed.packId, installed.character.id, variant.id)
        return when (filter) {
            MonsterFilter.All -> true
            MonsterFilter.Regular -> !variant.isRadiant
            MonsterFilter.RadiantUnlocked -> variant.isRadiant && reference in unlockedVariants
            MonsterFilter.RadiantLocked -> variant.isRadiant && reference !in unlockedVariants
        }
    }

    val hasSelectableCharacter = characters.any { installed ->
        installed.selectionVariants().any { selection ->
            selection.matchesFilter() && (!hideSelected || !isReferenceSelected(CharacterReference(selection.installed.packId, selection.installed.character.id, selection.variant.id)))
        }
    }
    val effectiveRandomize = onRandomize?.takeIf { showRandomize }
    val hasNoCharacterOptions = effectiveRandomize == null && hideSelected && selectionState.isDefaultSelected && !hasSelectableCharacter

    if (effectiveRandomize != null) {
        item(key = "random") {
            CharacterGridItem(
                name = stringResource(R.string.randomize),
                modifier = Modifier.padding(top = 8.dp),
                type = type,
                isSelected = isRandomSelected,
                showTypeSubtitle = false,
                shape = gridItemShape(index = 0, itemCount = 1),
                artwork = {
                    AppIcon(
                        icon = LocalMonsterAppIcons.current.randomize,
                        contentDescription = stringResource(R.string.randomize),
                        modifier = Modifier.size(48.dp),
                    )
                },
                onSelect = effectiveRandomize,
            )
        }
    }

    val userCharacters = characters.filter { it.isEditable }
    val otherPacks = characters.filter { !it.isEditable }.groupBy { it.packId }

    if (filter.allowsBuiltInMonster && (!hideSelected || !selectionState.isDefaultSelected)) {
        item(span = { GridItemSpan(2) }) { SectionHeader(stringResource(R.string.built_in_characters_section, pluralTitle)) }
        item(key = "default") {
            CharacterGridItem(
                name = defaultCharacter.name,
                type = type,
                isSelected = selectionState.isDefaultSelected,
                shape = gridItemShape(index = 0, itemCount = 1),
                artwork = {
                    Image(
                        painter = painterResource(defaultArtwork(defaultCharacter).resource),
                        contentDescription = stringResource(R.string.default_character_artwork, title.lowercase()),
                        modifier = Modifier.size(88.dp)
                    )
                },
                onSelect = { onSelect(defaultReference) },
                onSelected = onSelected?.let { callback ->
                    { defaultReference?.let(callback) }
                },
            )
        }
    }

    if (userCharacters.isNotEmpty()) {
        val selections = userCharacters.flatMap(InstalledPackCharacter::selectionVariants)
            .filter { selection ->
                val reference = CharacterReference(selection.installed.packId, selection.installed.character.id, selection.variant.id)
                selection.matchesFilter() && (!hideSelected || !isReferenceSelected(reference))
            }
        if (selections.isNotEmpty()) {
            item(span = { GridItemSpan(2) }) { SectionHeader(stringResource(R.string.your_characters, pluralTitle)) }
            gridItemsIndexed(items = selections, key = { _, selection -> "grid:custom:${selection.installed.character.id}:${selection.variant.id}" }) { index, selection ->
                val installed = selection.installed
                val reference = CharacterReference(installed.packId, installed.character.id, selection.variant.id)
                val isUnlocked = !selection.variant.isRadiant || reference in unlockedVariants
                CharacterGridItem(
                    name = installed.character.name,
                    type = installed.character.type,
                    level = installed.character.level,
                    isRadiant = selection.variant.isRadiant,
                    isSelected = isReferenceSelected(reference),
                    isUnlocked = isUnlocked,
                    shape = gridItemShape(index = index, itemCount = selections.size),
                    artwork = {
                        AsyncImage(
                            model = selection.previewArtwork(artworkTarget),
                            contentDescription = stringResource(R.string.character_artwork, installed.character.name),
                            modifier = Modifier.size(88.dp)
                        )
                    },
                    onSelect = { if (isUnlocked) onSelect(reference) },
                    onSelected = onSelected?.let { callback -> { callback(reference) } },
                    onDelete = if (installed.isDeletable) { { onDelete(installed) } } else null,
                    onEdit = if (installed.isEditable) { { onEdit(installed) } } else null,
                    onShare = if (installed.isEditable) { { onShare(installed) } } else null
                )
            }
        }
    }

    otherPacks.forEach { (packId, packCharacters) ->
        val selections = packCharacters.flatMap(InstalledPackCharacter::selectionVariants)
            .filter { selection ->
                val reference = CharacterReference(selection.installed.packId, selection.installed.character.id, selection.variant.id)
                selection.matchesFilter() && (!hideSelected || !isReferenceSelected(reference))
            }
        if (selections.isNotEmpty()) {
            item(span = { GridItemSpan(2) }) { SectionHeader(packCharacters.first().packName) }
            gridItemsIndexed(items = selections, key = { _, selection -> "grid:${packId}:${selection.installed.character.id}:${selection.variant.id}" }) { index, selection ->
                val installed = selection.installed
                val reference = CharacterReference(installed.packId, installed.character.id, selection.variant.id)
                val isUnlocked = !selection.variant.isRadiant || reference in unlockedVariants
                CharacterGridItem(
                    name = installed.character.name,
                    type = installed.character.type,
                    level = installed.character.level,
                    isRadiant = selection.variant.isRadiant,
                    isSelected = isReferenceSelected(reference),
                    isUnlocked = isUnlocked,
                    shape = gridItemShape(index = index, itemCount = selections.size),
                    artwork = {
                        AsyncImage(
                            model = selection.previewArtwork(artworkTarget),
                            contentDescription = stringResource(R.string.character_artwork, installed.character.name),
                            modifier = Modifier.size(88.dp)
                        )
                    },
                    onSelect = { if (isUnlocked) onSelect(reference) },
                    onSelected = onSelected?.let { callback -> { callback(reference) } },
                )
            }
        }
    }

    if (hasNoCharacterOptions) {
        item(key = "no-options", span = { GridItemSpan(2) }) { NoCharacterOptionsPlaceholder(pluralTitle) }
    }

}

private data class CharacterSelection(
    val installed: InstalledPackCharacter,
    val variant: CharacterVisualVariant,
)

private fun InstalledPackCharacter.selectionVariants(): List<CharacterSelection> =
    character.visualVariants.map { CharacterSelection(this, it) }

private val MonsterFilter.allowsBuiltInMonster: Boolean
    get() = this == MonsterFilter.All || this == MonsterFilter.Regular

internal fun InstalledPackCharacter.matches(reference: CharacterReference): Boolean =
    packId == reference.packId &&
        character.id == reference.characterId &&
        character.variant(reference.variantId) != null

private fun CharacterSelection.previewArtwork(artworkTarget: CharacterAssignmentTarget): File {
    val image = when (artworkTarget) {
        CharacterAssignmentTarget.Player -> variant.backImage ?: variant.frontImage
        CharacterAssignmentTarget.Contact -> variant.frontImage ?: variant.backImage
    }
    return installed.imageFile(requireNotNull(image))
}

private fun gridItemShape(index: Int, itemCount: Int): RoundedCornerShape {
    val isLeftColumn = index % 2 == 0
    val isRightColumn = !isLeftColumn
    val isTopRow = index < 2
    val isBottomRow = index / 2 == (itemCount - 1) / 2
    val isOnlyInRow = isLeftColumn && index == itemCount - 1

    return RoundedCornerShape(
        topStart = if (isTopRow && isLeftColumn) 20.dp else 2.dp,
        topEnd = if (isTopRow && (isRightColumn || isOnlyInRow)) 20.dp else 2.dp,
        bottomStart = if (isBottomRow && isLeftColumn) 20.dp else 2.dp,
        bottomEnd = if (isBottomRow && (isRightColumn || isOnlyInRow)) 20.dp else 2.dp
    )
}

internal fun selectedCharacterIndex(
    characters: List<InstalledPackCharacter>,
    selected: CharacterReference?,
    hasRandomize: Boolean = false,
): Int {
    if (selected == null) return 0 // Keep the first section visible for the default selection.

    val userCharacters = characters.filter { it.isEditable }
    val otherPacks = characters.filter { !it.isEditable }.groupBy { it.packId }

    // Logic follows the order in characterTypeItems and characterTypeGridItems:
    // 0: Built-in header
    // 1: Default character
    // 2: Next section header
    var currentIndex = if (hasRandomize) 3 else 2

    if (userCharacters.isNotEmpty()) {
            currentIndex++ // "Your characters" header
        val userIdx = userCharacters.flatMap(InstalledPackCharacter::selectionVariants).indexOfFirst {
            it.installed.matches(selected) && it.variant.id == selected.variantId
        }
        if (userIdx != -1) {
            val target = currentIndex + userIdx
            // If it's the very first user character, index 0 might still be better 
            // to keep the "Add" button and headers visible.
            return if (target <= 4) 0 else target
        }
        currentIndex += userCharacters.sumOf { it.selectionVariants().size }
    }

    for (packCharacters in otherPacks.values) {
        currentIndex++ // Pack header
        val packIdx = packCharacters.flatMap(InstalledPackCharacter::selectionVariants).indexOfFirst {
            it.installed.matches(selected) && it.variant.id == selected.variantId
        }
        if (packIdx != -1) return currentIndex + packIdx
        currentIndex += packCharacters.sumOf { it.selectionVariants().size }
    }

    return 0
}

@Composable
internal fun JumpToSelectedCharacterButton(
    listState: LazyListState,
    selectedItemIndex: Int,
    modifier: Modifier = Modifier
) {
    val isSelectedItemOffScreen by remember(listState, selectedItemIndex) {
        derivedStateOf {
            val visibleItems = listState.layoutInfo.visibleItemsInfo
            visibleItems.isNotEmpty() && visibleItems.none { it.index == selectedItemIndex }
        }
    }
    val isSelectedItemAbove by remember(listState, selectedItemIndex) {
        derivedStateOf {
            listState.layoutInfo.visibleItemsInfo.firstOrNull()?.index?.let { firstVisibleIndex ->
                selectedItemIndex < firstVisibleIndex
            } ?: false
        }
    }
    val coroutineScope = rememberCoroutineScope()

    if (isSelectedItemOffScreen) {
        SmallFloatingActionButton(
            onClick = { coroutineScope.launch { listState.animateScrollToItem(selectedItemIndex) } },
            modifier = modifier
        ) {
            AppIcon(
                icon = if (isSelectedItemAbove) {
                    LocalAppIcons.current.arrowUp
                } else {
                    LocalAppIcons.current.arrowDown
                },
                contentDescription = stringResource(R.string.jump_to_selected_character)
            )
        }
    }
}

@Composable
internal fun JumpToSelectedCharacterButton(
    gridState: LazyGridState,
    selectedItemIndex: Int,
    modifier: Modifier = Modifier
) {
    val isSelectedItemOffScreen by remember(gridState, selectedItemIndex) {
        derivedStateOf {
            val visibleItems = gridState.layoutInfo.visibleItemsInfo
            visibleItems.isNotEmpty() && visibleItems.none { it.index == selectedItemIndex }
        }
    }
    val isSelectedItemAbove by remember(gridState, selectedItemIndex) {
        derivedStateOf {
            gridState.layoutInfo.visibleItemsInfo.firstOrNull()?.index?.let { firstVisibleIndex ->
                selectedItemIndex < firstVisibleIndex
            } ?: false
        }
    }
    val coroutineScope = rememberCoroutineScope()

    if (isSelectedItemOffScreen) {
        SmallFloatingActionButton(
            onClick = { coroutineScope.launch { gridState.animateScrollToItem(selectedItemIndex) } },
            modifier = modifier
        ) {
            AppIcon(
                icon = if (isSelectedItemAbove) LocalAppIcons.current.arrowUp else LocalAppIcons.current.arrowDown,
                contentDescription = stringResource(R.string.jump_to_selected_character)
            )
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CharacterSelectionActions(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    isAddEnabled: Boolean,
    onAddCharacter: () -> Unit,
    showCharacterTypeTabs: Boolean = true,
    modifier: Modifier = Modifier,
    filter: MonsterFilter? = null,
    onFilterSelected: ((MonsterFilter) -> Unit)? = null,
) {
    var filterExpanded by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()
    val addButton: @Composable () -> Unit = {
        FilledIconButton(
            onClick = onAddCharacter,
            enabled = isAddEnabled,
            modifier = Modifier.size(48.dp),
            shape = CircleShape,
        ) {
            AppIcon(
                icon = LocalMonsterAppIcons.current.addCharacter,
                contentDescription = stringResource(R.string.add),
            )
        }
    }
    val filterButton: @Composable () -> Unit = {
        if (filter != null && onFilterSelected != null) {
            Box {
                TextButton(onClick = { filterExpanded = true }) {
                    AppIcon(
                        icon = LocalMonsterAppIcons.current.filter,
                        contentDescription = null,
                    )
                    Text(
                        text = when (filter) {
                            MonsterFilter.All -> stringResource(R.string.filter_all)
                            MonsterFilter.Regular -> stringResource(R.string.filter_regular)
                            MonsterFilter.RadiantUnlocked -> stringResource(R.string.filter_unlocked_radiant)
                            MonsterFilter.RadiantLocked -> stringResource(R.string.filter_locked_radiant)
                        },
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
                DropdownMenu(
                    expanded = filterExpanded,
                    onDismissRequest = { filterExpanded = false },
                ) {
                    MonsterFilter.entries.forEach { entry ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = when (entry) {
                                        MonsterFilter.All -> stringResource(R.string.filter_all)
                                        MonsterFilter.Regular -> stringResource(R.string.filter_regular)
                                        MonsterFilter.RadiantUnlocked -> stringResource(R.string.filter_unlocked_radiant)
                                        MonsterFilter.RadiantLocked -> stringResource(R.string.filter_locked_radiant)
                                    },
                                )
                            },
                            onClick = {
                                onFilterSelected(entry)
                                filterExpanded = false
                            },
                        )
                    }
                }
            }
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (showCharacterTypeTabs) Modifier.horizontalScroll(scrollState) else Modifier)
                .padding(top = 4.dp),
            horizontalArrangement = if (showCharacterTypeTabs) Arrangement.spacedBy(8.dp) else Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (showCharacterTypeTabs) {
                SingleChoiceSegmentedButtonRow {
                    SegmentedButton(
                        selected = selectedTab == 0,
                        onClick = { onTabSelected(0) },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                        label = { Text(stringResource(R.string.character_type_trainer)) },
                    )
                    SegmentedButton(
                        selected = selectedTab == 1,
                        onClick = { onTabSelected(1) },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                        label = { Text(stringResource(R.string.character_type_monster)) },
                    )
                }
            }
            if (showCharacterTypeTabs) {
                addButton()
                filterButton()
            } else {
                filterButton()
                Spacer(modifier = Modifier.weight(1f))
                addButton()
            }
        }
        if (!isAddEnabled) {
            Text(
                text = stringResource(R.string.character_limit_reached_hint),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Start,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    )
}

@Composable
fun CustomCharacterDeletionConfirmationDialog(
    characterName: String,
    hasRadiantVariant: Boolean,
    isInUse: Boolean = false,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.remove_character_pack_title, characterName)) },
        text = { 
            Text(
                stringResource(
                    when {
                        hasRadiantVariant && isInUse -> R.string.remove_character_with_radiant_variant_in_use_message
                        hasRadiantVariant -> R.string.remove_character_with_radiant_variant_message
                        isInUse -> R.string.remove_character_in_use_message
                        else -> R.string.remove_character_pack_message
                    },
                    characterName
                )
            ) 
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                )
            ) {
                Text(stringResource(R.string.remove))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}

@Composable
private fun NoCharacterOptionsPlaceholder(pluralTitle: String) {
    Text(
        text = stringResource(R.string.no_available_character_options, pluralTitle.lowercase()),
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 32.dp),
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CharacterOptionCard(
    name: String,
    type: CharacterType,
    level: Int? = null,
    isRadiant: Boolean = false,
    isSelected: Boolean,
    isUnlocked: Boolean = true,
    showTypeSubtitle: Boolean = true,
    roundTop: Boolean,
    roundBottom: Boolean,
    artwork: @Composable () -> Unit,
    onSelect: () -> Unit,
    onSelected: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
    onEdit: (() -> Unit)? = null,
    onShare: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }
    var showRadiantUnlockDialog by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(
        topStart = if (roundTop) 20.dp else 2.dp, topEnd = if (roundTop) 20.dp else 2.dp,
        bottomStart = if (roundBottom) 20.dp else 2.dp, bottomEnd = if (roundBottom) 20.dp else 2.dp
    )
    Box {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 1.dp)
                .then(modifier),
            shape = shape,
            colors = CardDefaults.cardColors(containerColor = if (isSelected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerLow),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
        ) {
            Row(
                modifier = Modifier
                    .combinedClickable(
                        onClick = {
                            if (isSelected) {
                                onSelected?.invoke()
                                return@combinedClickable
                            }
                            if (isUnlocked) onSelect() else showRadiantUnlockDialog = true
                        },
                        onLongClick = if (!isSelected && (onDelete != null || onEdit != null || onShare != null)) { { showMenu = true } } else null
                    )
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier.size(60.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    artwork()
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(name, style = MaterialTheme.typography.titleMedium)
                    }

                    if (showTypeSubtitle && type == CharacterType.Monster) {
                        val variant = stringResource(if (isRadiant) R.string.radiant else R.string.regular)
                        val levelText = stringResource(R.string.roster_monster_level, level ?: dev.alenajam.monsterdialer.characters.data.DefaultMonsterLevel)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.monster_variant_and_level, variant, levelText),
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isRadiant) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (isRadiant) {
                                AppIcon(
                                    icon = LocalMonsterAppIcons.current.radiant,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
                if (isSelected) {
                    Text(
                        stringResource(R.string.selected),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                } else {
                    OutlinedButton(onClick = onSelect, enabled = isUnlocked) {
                        Text(stringResource(if (isUnlocked) R.string.select else R.string.locked))
                    }
                }
            }
        }

        if (showRadiantUnlockDialog) {
            RadiantVariantUnlockDialog(
                characterName = name,
                onDismiss = { showRadiantUnlockDialog = false }
            )
        }

        if (onDelete != null || onEdit != null || onShare != null) {
            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false }
            ) {
                if (onEdit != null) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.edit)) },
                        onClick = {
                            showMenu = false
                            onEdit()
                        },
                        leadingIcon = {
                            AppIcon(LocalAppIcons.current.edit, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                    )
                }
                if (onShare != null) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.share)) },
                        onClick = {
                            showMenu = false
                            onShare()
                        },
                        leadingIcon = {
                            AppIcon(LocalAppIcons.current.share, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                    )
                }
                if (onDelete != null) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.remove)) },
                        onClick = {
                            showMenu = false
                            onDelete()
                        },
                        leadingIcon = {
                            AppIcon(LocalAppIcons.current.delete, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CharacterGridItem(
    name: String,
    type: CharacterType,
    level: Int? = null,
    isRadiant: Boolean = false,
    isSelected: Boolean,
    isUnlocked: Boolean = true,
    showTypeSubtitle: Boolean = true,
    shape: Shape,
    artwork: @Composable () -> Unit,
    onSelect: () -> Unit,
    onSelected: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
    onEdit: (() -> Unit)? = null,
    onShare: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }
    var showRadiantUnlockDialog by remember { mutableStateOf(false) }
    Box {
        Card(
            modifier = Modifier.fillMaxWidth().then(modifier),
            shape = shape,
            colors = CardDefaults.cardColors(
                containerColor = if (isSelected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerLow
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
        ) {
            Column(
                modifier = Modifier
                    .combinedClickable(
                        onClick = {
                            if (isSelected) {
                                onSelected?.invoke()
                            } else if (isUnlocked) {
                                onSelect()
                            } else {
                                showRadiantUnlockDialog = true
                            }
                        },
                        onLongClick = if (onDelete != null || onEdit != null || onShare != null) { { showMenu = true } } else null
                    )
                    .fillMaxWidth()
                    .height(172.dp)
                    .padding(12.dp),
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(72.dp),
                    contentAlignment = Alignment.Center
                ) {
                    artwork()
                }
                
                Spacer(modifier = Modifier.height(2.dp))
                
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Box(modifier = Modifier.height(20.dp)) {
                    if (showTypeSubtitle && type == CharacterType.Monster) {
                        val variant = stringResource(if (isRadiant) R.string.radiant else R.string.regular)
                        val levelText = stringResource(R.string.roster_monster_level, level ?: dev.alenajam.monsterdialer.characters.data.DefaultMonsterLevel)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.monster_variant_and_level, variant, levelText),
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isRadiant) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                            if (isRadiant) {
                                AppIcon(
                                    icon = LocalMonsterAppIcons.current.radiant,
                                    contentDescription = null,
                                    modifier = Modifier.size(12.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.weight(1f))
                
                Box(modifier = Modifier.height(24.dp)) {
                    if (isSelected) {
                        Text(
                            text = stringResource(R.string.selected),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else if (!isUnlocked) {
                        Text(
                            text = stringResource(R.string.locked),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        if (showRadiantUnlockDialog) {
            RadiantVariantUnlockDialog(
                characterName = name,
                onDismiss = { showRadiantUnlockDialog = false }
            )
        }

        if (onDelete != null || onEdit != null || onShare != null) {
            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false }
            ) {
                if (onEdit != null) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.edit)) },
                        onClick = {
                            showMenu = false
                            onEdit()
                        },
                        leadingIcon = {
                            AppIcon(LocalAppIcons.current.edit, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                    )
                }
                if (onShare != null) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.share)) },
                        onClick = {
                            showMenu = false
                            onShare()
                        },
                        leadingIcon = {
                            AppIcon(LocalAppIcons.current.share, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                    )
                }
                if (onDelete != null) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.remove)) },
                        onClick = {
                            showMenu = false
                            onDelete()
                        },
                        leadingIcon = {
                            AppIcon(LocalAppIcons.current.delete, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun RadiantVariantUnlockDialog(characterName: String, onDismiss: () -> Unit) {
    var showGuide by remember { mutableStateOf(false) }
    if (showGuide) {
        ContextualGuideDialog(
            contents = radiantGuideContents(),
            onDismiss = { showGuide = false },
        )
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.radiant_variant_locked_title)) },
        text = { Text(stringResource(R.string.radiant_variant_locked_message, characterName)) },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
        dismissButton = {
            TextButton(onClick = {
                showGuide = true
            }) { Text(stringResource(R.string.learn_about_radiants)) }
        },
    )
}
