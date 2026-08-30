package dev.alenajam.monsterdialer.characters.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.itemsIndexed as gridItemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import dev.alenajam.monsterdialer.R
import dev.alenajam.monsterdialer.app.ui.LocalMonsterAppIcons
import dev.alenajam.opendialer.core.common.ui.AppIcon
import dev.alenajam.opendialer.core.common.ui.LocalAppIcons
import dev.alenajam.monsterdialer.characters.data.BuiltInArtwork
import dev.alenajam.monsterdialer.characters.data.BuiltInCharacter
import dev.alenajam.monsterdialer.packs.data.CharacterReference
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

@Composable
internal fun CharacterTypeTabs(selectedTab: Int, onTabSelected: (Int) -> Unit) {
    val trainerLabel = stringResource(R.string.character_type_trainer)
    val monsterLabel = stringResource(R.string.character_type_monster)

    PrimaryTabRow(selectedTabIndex = selectedTab, modifier = Modifier.fillMaxWidth()) {
        Tab(selected = selectedTab == 0, onClick = { onTabSelected(0) }, text = { Text(trainerLabel) })
        Tab(selected = selectedTab == 1, onClick = { onTabSelected(1) }, text = { Text(monsterLabel) })
    }
}

internal fun LazyListScope.characterTypeItems(
    title: String,
    defaultCharacter: BuiltInCharacter,
    characters: List<InstalledPackCharacter>,
    selected: CharacterReference?,
    defaultArtwork: (BuiltInCharacter) -> BuiltInArtwork,
    packArtwork: (InstalledPackCharacter) -> File,
    onSelect: (CharacterReference?) -> Unit,
    onAddCharacter: () -> Unit,
    addLabel: String,
    isAddEnabled: Boolean = true,
    onDelete: (InstalledPackCharacter) -> Unit = {},
    onEdit: (InstalledPackCharacter) -> Unit = {}
) {
    val availableSelection = selected?.takeIf { reference ->
        characters.any { CharacterReference(it.packId, it.character.id) == reference }
    }
    item(key = "add") {
        AddCharacterButton(
            onClick = onAddCharacter,
            label = addLabel,
            enabled = isAddEnabled
        )
        if (!isAddEnabled) {
            Text(
                text = stringResource(R.string.character_limit_reached_hint),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
            )
        }
    }

    val userCustomPackId = "user.custom"
    val grouped = characters.groupBy { it.packId }
    val userCharacters = grouped[userCustomPackId].orEmpty()
    val otherPacks = grouped.filter { it.key != userCustomPackId }

    item { SectionHeader(stringResource(R.string.built_in_characters_section)) }
    item(key = "default") {
        CharacterOptionCard(
            name = defaultCharacter.name,
            isSelected = availableSelection == null,
            roundTop = true,
            roundBottom = true,
            artwork = {
                Image(
                    painter = painterResource(defaultArtwork(defaultCharacter).resource),
                    contentDescription = stringResource(R.string.default_character_artwork, title.lowercase()),
                    modifier = Modifier.size(72.dp)
                )
            },
            onSelect = { onSelect(null) }
        )
    }

    if (userCharacters.isNotEmpty()) {
        item { SectionHeader(stringResource(R.string.your_characters)) }
        itemsIndexed(items = userCharacters, key = { _, character -> "custom:${character.character.id}" }) { index, installed ->
            val reference = CharacterReference(installed.packId, installed.character.id)
            CharacterOptionCard(
                name = installed.character.name,
                isRadiant = installed.character.isRadiant,
                isSelected = availableSelection == reference,
                roundTop = index == 0,
                roundBottom = index == userCharacters.lastIndex,
                artwork = {
                    AsyncImage(
                        model = packArtwork(installed),
                        contentDescription = stringResource(R.string.character_artwork, installed.character.name),
                        modifier = Modifier.size(72.dp)
                    )
                },
                onSelect = { onSelect(reference) },
                onDelete = { onDelete(installed) },
                onEdit = { onEdit(installed) }
            )
        }
    }

    otherPacks.forEach { (packId, packCharacters) ->
        item { SectionHeader(packCharacters.first().packName) }
        itemsIndexed(items = packCharacters, key = { _, character -> "${packId}:${character.character.id}" }) { index, installed ->
            val reference = CharacterReference(installed.packId, installed.character.id)
            CharacterOptionCard(
                name = installed.character.name,
                isRadiant = installed.character.isRadiant,
                isSelected = availableSelection == reference,
                roundTop = index == 0,
                roundBottom = index == packCharacters.lastIndex,
                artwork = {
                    AsyncImage(
                        model = packArtwork(installed),
                        contentDescription = stringResource(R.string.character_artwork, installed.character.name),
                        modifier = Modifier.size(72.dp)
                    )
                },
                onSelect = { onSelect(reference) }
            )
        }
    }

    if (characters.isEmpty()) item(key = "empty") { NoAdditionalCharacterOptionsCard(title) }
}

internal fun LazyGridScope.characterTypeGridItems(
    title: String,
    defaultCharacter: BuiltInCharacter,
    characters: List<InstalledPackCharacter>,
    selected: CharacterReference?,
    defaultArtwork: (BuiltInCharacter) -> BuiltInArtwork,
    packArtwork: (InstalledPackCharacter) -> File,
    onSelect: (CharacterReference?) -> Unit,
    onAddCharacter: () -> Unit,
    addLabel: String,
    isAddEnabled: Boolean = true,
    onDelete: (InstalledPackCharacter) -> Unit = {},
    onEdit: (InstalledPackCharacter) -> Unit = {}
) {
    val availableSelection = selected?.takeIf { reference ->
        characters.any { CharacterReference(it.packId, it.character.id) == reference }
    }
    item(key = "add", span = { GridItemSpan(2) }) {
        Column {
            AddCharacterButton(
                onClick = onAddCharacter,
                label = addLabel,
                enabled = isAddEnabled
            )
            if (!isAddEnabled) {
                Text(
                    text = stringResource(R.string.character_limit_reached_hint),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                )
            }
        }
    }

    val userCustomPackId = "user.custom"
    val grouped = characters.groupBy { it.packId }
    val userCharacters = grouped[userCustomPackId].orEmpty()
    val otherPacks = grouped.filter { it.key != userCustomPackId }

    item(span = { GridItemSpan(2) }) { SectionHeader(stringResource(R.string.built_in_characters_section)) }
    item(key = "default") {
        CharacterGridItem(
            name = defaultCharacter.name,
            isSelected = availableSelection == null,
            shape = gridItemShape(index = 0, itemCount = 1),
            artwork = {
                Image(
                    painter = painterResource(defaultArtwork(defaultCharacter).resource),
                    contentDescription = stringResource(R.string.default_character_artwork, title.lowercase()),
                    modifier = Modifier.size(88.dp)
                )
            },
            onSelect = { onSelect(null) }
        )
    }

    if (userCharacters.isNotEmpty()) {
        item(span = { GridItemSpan(2) }) { SectionHeader(stringResource(R.string.your_characters)) }
        gridItemsIndexed(items = userCharacters, key = { _, character -> "custom:${character.character.id}" }) { index, installed ->
            val reference = CharacterReference(installed.packId, installed.character.id)
            CharacterGridItem(
                name = installed.character.name,
                isRadiant = installed.character.isRadiant,
                isSelected = availableSelection == reference,
                shape = gridItemShape(index = index, itemCount = userCharacters.size),
                artwork = {
                    AsyncImage(
                        model = packArtwork(installed),
                        contentDescription = stringResource(R.string.character_artwork, installed.character.name),
                        modifier = Modifier.size(88.dp)
                    )
                },
                onSelect = { onSelect(reference) },
                onDelete = { onDelete(installed) },
                onEdit = { onEdit(installed) }
            )
        }
    }

    otherPacks.forEach { (packId, packCharacters) ->
        item(span = { GridItemSpan(2) }) { SectionHeader(packCharacters.first().packName) }
        gridItemsIndexed(items = packCharacters, key = { _, character -> "${packId}:${character.character.id}" }) { index, installed ->
            val reference = CharacterReference(installed.packId, installed.character.id)
            CharacterGridItem(
                name = installed.character.name,
                isRadiant = installed.character.isRadiant,
                isSelected = availableSelection == reference,
                shape = gridItemShape(index = index, itemCount = packCharacters.size),
                artwork = {
                    AsyncImage(
                        model = packArtwork(installed),
                        contentDescription = stringResource(R.string.character_artwork, installed.character.name),
                        modifier = Modifier.size(88.dp)
                    )
                },
                onSelect = { onSelect(reference) }
            )
        }
    }

    if (characters.isEmpty()) {
        item(key = "empty") {
            NoAdditionalCharacterGridItem(
                title = title,
                shape = gridItemShape(index = 1, itemCount = 2)
            )
        }
    }
}

private fun gridItemShape(index: Int, itemCount: Int): RoundedCornerShape {
    val isLeftColumn = index % 2 == 0
    val isTopRow = index < 2
    val isBottomRow = index / 2 == (itemCount - 1) / 2
    return RoundedCornerShape(
        topStart = if (isTopRow && isLeftColumn) 20.dp else 2.dp,
        topEnd = if (isTopRow && !isLeftColumn) 20.dp else 2.dp,
        bottomStart = if (isBottomRow && isLeftColumn) 20.dp else 2.dp,
        bottomEnd = if (isBottomRow && !isLeftColumn) 20.dp else 2.dp
    )
}

internal fun selectedCharacterIndex(
    characters: List<InstalledPackCharacter>,
    selected: CharacterReference?
): Int {
    if (selected == null) return 0 // Keep Add button visible for Default selection

    val userCustomPackId = "user.custom"
    val grouped = characters.groupBy { it.packId }
    val userCharacters = grouped[userCustomPackId].orEmpty()
    val otherPacks = grouped.filter { it.key != userCustomPackId }

    // Logic follows the order in characterTypeItems
    // 0: Add button
    // 1: Built-in header
    // 2: Default character
    var currentIndex = 3

    if (userCharacters.isNotEmpty()) {
        currentIndex++ // "Your characters" header
        val userIdx = userCharacters.indexOfFirst {
            CharacterReference(it.packId, it.character.id) == selected
        }
        if (userIdx != -1) {
            val target = currentIndex + userIdx
            // If it's the very first user character, index 0 might still be better 
            // to keep the "Add" button and headers visible.
            return if (target <= 4) 0 else target
        }
        currentIndex += userCharacters.size
    }

    for (packCharacters in otherPacks.values) {
        currentIndex++ // Pack header
        val packIdx = packCharacters.indexOfFirst {
            CharacterReference(it.packId, it.character.id) == selected
        }
        if (packIdx != -1) return currentIndex + packIdx
        currentIndex += packCharacters.size
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

@Composable
private fun AddCharacterButton(
    onClick: () -> Unit,
    label: String,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        AppIcon(
            LocalMonsterAppIcons.current.addCharacter,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimary
        )
        Text(
            text = label,
            modifier = Modifier.padding(start = 8.dp),
        )
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
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.remove_character_pack_title, characterName)) },
        text = { Text(stringResource(R.string.remove_character_pack_message, characterName)) },
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
private fun NoAdditionalCharacterOptionsCard(title: String) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp),
        shape = RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp, bottomStart = 20.dp, bottomEnd = 20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxWidth().heightIn(min = 104.dp).padding(horizontal = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(stringResource(R.string.no_other_character_options, title.lowercase()), style = MaterialTheme.typography.titleSmall, textAlign = TextAlign.Center)
                Text(stringResource(R.string.import_and_enable_character_pack), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CharacterOptionCard(
    name: String, isRadiant: Boolean = false, isSelected: Boolean,
    roundTop: Boolean, roundBottom: Boolean, artwork: @Composable () -> Unit, onSelect: () -> Unit,
    onDelete: (() -> Unit)? = null,
    onEdit: (() -> Unit)? = null
) {
    var showMenu by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(
        topStart = if (roundTop) 20.dp else 2.dp, topEnd = if (roundTop) 20.dp else 2.dp,
        bottomStart = if (roundBottom) 20.dp else 2.dp, bottomEnd = if (roundBottom) 20.dp else 2.dp
    )
    Box {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 1.dp),
            shape = shape,
            colors = CardDefaults.cardColors(containerColor = if (isSelected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerLow),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
        ) {
            Row(
                modifier = Modifier
                    .combinedClickable(
                        onClick = onSelect,
                        onLongClick = if (onDelete != null || onEdit != null) { { showMenu = true } } else null
                    )
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(modifier = Modifier.size(72.dp)) {
                    artwork()
                    if (isRadiant) {
                        RadiantBadge(Modifier.align(Alignment.TopEnd))
                    }
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(name, style = MaterialTheme.typography.titleMedium)
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
                    OutlinedButton(onClick = onSelect) { Text(stringResource(R.string.select)) }
                }
            }
        }

        if (onDelete != null || onEdit != null) {
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
private fun RadiantBadge(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.size(24.dp),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.65f),
        shadowElevation = 2.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            AppIcon(
                icon = LocalMonsterAppIcons.current.radiant,
                contentDescription = stringResource(R.string.radiant),
                modifier = Modifier.size(15.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CharacterGridItem(
    name: String,
    isRadiant: Boolean = false,
    isSelected: Boolean,
    shape: Shape,
    artwork: @Composable () -> Unit,
    onSelect: () -> Unit,
    onDelete: (() -> Unit)? = null,
    onEdit: (() -> Unit)? = null
) {
    var showMenu by remember { mutableStateOf(false) }
    Box {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = shape,
            colors = CardDefaults.cardColors(
                containerColor = if (isSelected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerLow
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
        ) {
            Column(
                modifier = Modifier
                    .combinedClickable(
                        onClick = onSelect,
                        onLongClick = if (onDelete != null || onEdit != null) { { showMenu = true } } else null
                    )
                    .fillMaxWidth()
                    .heightIn(min = 184.dp)
                    .padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(modifier = Modifier.size(88.dp)) {
                    artwork()
                    if (isRadiant) RadiantBadge(Modifier.align(Alignment.TopEnd))
                }
                Text(name, style = MaterialTheme.typography.titleSmall, textAlign = TextAlign.Center, maxLines = 2)
                Box(modifier = Modifier.height(20.dp), contentAlignment = Alignment.Center) {
                    if (isSelected) Text(stringResource(R.string.selected), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                }
            }
        }

        if (onDelete != null || onEdit != null) {
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
private fun NoAdditionalCharacterGridItem(title: String, shape: Shape) {
    Card(
        modifier = Modifier.fillMaxWidth().height(184.dp),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(R.string.no_other_character_options, title.lowercase()), style = MaterialTheme.typography.titleSmall, textAlign = TextAlign.Center)
                Text(stringResource(R.string.import_and_enable_character_pack), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
            }
        }
    }
}
