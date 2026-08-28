package dev.alenajam.monsterdialer.characters.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ViewList
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import dev.alenajam.monsterdialer.R
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
        Icon(
            imageVector = if (nextLayout == CharacterLayout.Grid) Icons.Outlined.GridView else Icons.AutoMirrored.Outlined.ViewList,
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
    onSelect: (CharacterReference?) -> Unit
) {
    val availableSelection = selected?.takeIf { reference ->
        characters.any { CharacterReference(it.packId, it.character.id) == reference }
    }
    item(key = "default") {
        CharacterOptionCard(
            name = defaultCharacter.name,
            subtitle = stringResource(R.string.built_in_character),
            isSelected = availableSelection == null,
            roundTop = true,
            roundBottom = false,
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
    itemsIndexed(items = characters, key = { _, character -> "${character.packId}:${character.character.id}" }) { index, installed ->
        val reference = CharacterReference(installed.packId, installed.character.id)
        CharacterOptionCard(
            name = installed.character.name,
            subtitle = installed.packName,
            isRadiant = installed.character.isRadiant,
            isSelected = availableSelection == reference,
            roundTop = false,
            roundBottom = index == characters.lastIndex,
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
    if (characters.isEmpty()) item(key = "empty") { NoAdditionalCharacterOptionsCard(title) }
}

internal fun LazyGridScope.characterTypeGridItems(
    title: String,
    defaultCharacter: BuiltInCharacter,
    characters: List<InstalledPackCharacter>,
    selected: CharacterReference?,
    defaultArtwork: (BuiltInCharacter) -> BuiltInArtwork,
    packArtwork: (InstalledPackCharacter) -> File,
    onSelect: (CharacterReference?) -> Unit
) {
    val availableSelection = selected?.takeIf { reference ->
        characters.any { CharacterReference(it.packId, it.character.id) == reference }
    }
    item(key = "default") {
        CharacterGridItem(
            name = defaultCharacter.name,
            subtitle = stringResource(R.string.built_in_character),
            isSelected = availableSelection == null,
            shape = gridItemShape(index = 0, itemCount = if (characters.isEmpty()) 2 else characters.size + 1),
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
    gridItemsIndexed(items = characters, key = { _, character -> "${character.packId}:${character.character.id}" }) { index, installed ->
        val reference = CharacterReference(installed.packId, installed.character.id)
        val itemIndex = index + 1
        CharacterGridItem(
            name = installed.character.name,
            subtitle = installed.packName,
            isRadiant = installed.character.isRadiant,
            isSelected = availableSelection == reference,
            shape = gridItemShape(index = itemIndex, itemCount = characters.size + 1),
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
): Int = selected?.let { selectedReference ->
    characters.indexOfFirst { character ->
        CharacterReference(character.packId, character.character.id) == selectedReference
    }.takeIf { it >= 0 }?.plus(1)
} ?: 0

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
    val topContentPadding = with(LocalDensity.current) { 8.dp.roundToPx() }

    if (isSelectedItemOffScreen) {
        SmallFloatingActionButton(
            onClick = { coroutineScope.launch { listState.animateScrollToItem(selectedItemIndex, topContentPadding) } },
            modifier = modifier
        ) {
            Icon(
                imageVector = if (isSelectedItemAbove) {
                    Icons.Outlined.KeyboardArrowUp
                } else {
                    Icons.Outlined.KeyboardArrowDown
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
    val topContentPadding = with(LocalDensity.current) { 8.dp.roundToPx() }

    if (isSelectedItemOffScreen) {
        SmallFloatingActionButton(
            onClick = { coroutineScope.launch { gridState.animateScrollToItem(selectedItemIndex, topContentPadding) } },
            modifier = modifier
        ) {
            Icon(
                imageVector = if (isSelectedItemAbove) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown,
                contentDescription = stringResource(R.string.jump_to_selected_character)
            )
        }
    }
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

@Composable
private fun CharacterOptionCard(
    name: String, subtitle: String, isRadiant: Boolean = false, isSelected: Boolean,
    roundTop: Boolean, roundBottom: Boolean, artwork: @Composable () -> Unit, onSelect: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp),
        shape = RoundedCornerShape(
            topStart = if (roundTop) 20.dp else 2.dp, topEnd = if (roundTop) 20.dp else 2.dp,
            bottomStart = if (roundBottom) 20.dp else 2.dp, bottomEnd = if (roundBottom) 20.dp else 2.dp
        ),
        colors = CardDefaults.cardColors(containerColor = if (isSelected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
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
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (isSelected) Text(stringResource(R.string.selected), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            }
            if (!isSelected) OutlinedButton(onClick = onSelect) { Text(stringResource(R.string.select)) }
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
            Icon(
                imageVector = Icons.Outlined.AutoAwesome,
                contentDescription = stringResource(R.string.radiant),
                modifier = Modifier.size(15.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun CharacterGridItem(
    name: String,
    subtitle: String,
    isRadiant: Boolean = false,
    isSelected: Boolean,
    shape: Shape,
    artwork: @Composable () -> Unit,
    onSelect: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onSelect),
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerLow
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(modifier = Modifier.size(88.dp)) {
                artwork()
                if (isRadiant) RadiantBadge(Modifier.align(Alignment.TopEnd))
            }
            Text(name, style = MaterialTheme.typography.titleSmall, textAlign = TextAlign.Center, maxLines = 2)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center, maxLines = 1)
            Box(modifier = Modifier.height(20.dp), contentAlignment = Alignment.Center) {
                if (isSelected) Text(stringResource(R.string.selected), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
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
