package dev.alenajam.monsterdialer.characters.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
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
import dev.alenajam.monsterdialer.app.ui.RetroFooter
import dev.alenajam.monsterdialer.app.ui.RetroFastScroller
import dev.alenajam.monsterdialer.app.ui.RetroContextMenu
import dev.alenajam.monsterdialer.app.ui.RetroContextMenuItem
import dev.alenajam.monsterdialer.app.ui.RetroSearchButton
import dev.alenajam.monsterdialer.characters.data.BuiltInCharacters
import dev.alenajam.monsterdialer.characters.data.CharactersRepository
import dev.alenajam.monsterdialer.characters.data.RadiantVariantUnlockStore
import dev.alenajam.monsterdialer.packs.data.CharacterAssignmentTarget
import dev.alenajam.monsterdialer.packs.data.CharacterReference
import dev.alenajam.monsterdialer.packs.data.CharacterType
import dev.alenajam.opendialer.core.common.ui.AppIcon
import dev.alenajam.opendialer.feature.settings.LocalSettingsSubpageNavigator
import coil.compose.AsyncImage
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class RadiantCollectionViewModel @Inject constructor(
    private val charactersRepository: CharactersRepository,
    radiantUnlocks: RadiantVariantUnlockStore,
) : ViewModel() {
    val entries: StateFlow<List<RadiantCollectionEntry>> = combine(
        charactersRepository.observeCharactersAssignableTo(
            CharacterAssignmentTarget.Player,
            CharacterType.Monster,
        ),
        radiantUnlocks.unlocked,
    ) { characters, unlocked ->
        val builtIn = RadiantCollectionEntry(
            name = BuiltInCharacters.monster.character.name,
            packName = "",
            level = BuiltInCharacters.monster.level,
            reference = BuiltInCharacters.defaultMonsterReference,
            imageFile = null,
            fallbackArtwork = BuiltInCharacters.monster.character.playerArtwork.resource,
            isRadiant = false,
            isUnlocked = true,
            isEditable = false,
            hasRadiantVariant = false,
        )
        (listOf(builtIn) + characters.flatMap { installed ->
            installed.character.visualVariants
                .mapNotNull { variant ->
                    val imagePath = variant.frontImage ?: variant.backImage ?: return@mapNotNull null
                    val imageFile = installed.imageFile(imagePath).takeIf { it.isFile } ?: return@mapNotNull null
                    val reference = CharacterReference(installed.packId, installed.character.id, variant.id)
                    RadiantCollectionEntry(
                        name = installed.character.name,
                        packName = installed.packName,
                        level = installed.character.level,
                        reference = reference,
                        imageFile = imageFile,
                        fallbackArtwork = null,
                        isRadiant = variant.isRadiant,
                        isUnlocked = !variant.isRadiant || reference in unlocked,
                        isEditable = installed.isEditable,
                        hasRadiantVariant = installed.character.hasRadiantVariant,
                    )
                }
        }).sortedWith(compareBy({ it.name.lowercase() }, { it.isRadiant }))
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList(),
    )

    fun deleteCustomCharacter(characterId: String) {
        viewModelScope.launch {
            charactersRepository.deleteCustomCharacter(characterId)
        }
    }

    suspend fun isCharacterInUse(reference: CharacterReference): Boolean =
        charactersRepository.isCharacterInUse(reference)
}

data class RadiantCollectionEntry(
    val name: String,
    val packName: String,
    val level: Int?,
    val reference: CharacterReference,
    val imageFile: File?,
    val fallbackArtwork: Int?,
    val isRadiant: Boolean,
    val isUnlocked: Boolean,
    val isEditable: Boolean,
    val hasRadiantVariant: Boolean,
)

@Composable
fun RadiantCollectionScreen(viewModel: RadiantCollectionViewModel = hiltViewModel()) {
    val entries by viewModel.entries.collectAsStateWithLifecycle()
    val navigator = LocalSettingsSubpageNavigator.current
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    var selectedEntry by remember { mutableStateOf<RadiantCollectionEntry?>(null) }
    var pendingDeletion by remember { mutableStateOf<RadiantCollectionEntry?>(null) }
    var isPendingDeletionInUse by remember { mutableStateOf(false) }
    var pendingShare by remember { mutableStateOf<RadiantCollectionEntry?>(null) }
    val listState = rememberLazyListState()
    val sections = buildCollectionSections(
        entries = entries,
        builtInTitle = stringResource(
            R.string.built_in_characters_section,
            stringResource(R.string.character_type_monsters),
        ),
        userTitle = stringResource(
            R.string.your_characters,
            stringResource(R.string.character_type_monsters),
        ),
    )

    Column(modifier = Modifier.fillMaxSize()) {
        RetroSearchButton(
            label = stringResource(R.string.radiant_collection_browse_packs),
            onClick = { navigator?.navigateTo(1) },
            modifier = Modifier.align(Alignment.End),
        )
        Box(modifier = Modifier.weight(1f)) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                state = listState,
                contentPadding = PaddingValues(top = 2.dp, bottom = 12.dp),
            ) {
                sections.forEach { section ->
                    item(key = "header:${section.title}") {
                        CollectionSectionHeader(section.title)
                    }
                    items(section.entries.size, key = { index ->
                        val entry = section.entries[index]
                        "${entry.reference.packId}:${entry.reference.characterId}:${entry.reference.variantId}"
                    }) { index ->
                        val entry = section.entries[index]
                        CollectionMonsterRow(
                            entry = entry,
                            onClick = if (entry.isEditable && !entry.isRadiant) {
                                { selectedEntry = entry }
                            } else {
                                null
                            },
                        )
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
            backKey = stringResource(R.string.retro_key_b),
            backLabel = stringResource(R.string.retro_action_back_label),
            onBack = { navigator?.navigateBack() },
        )
    }

    pendingDeletion?.let { entry ->
        CustomCharacterDeletionConfirmationDialog(
            characterName = entry.name,
            hasRadiantVariant = entry.hasRadiantVariant,
            isInUse = isPendingDeletionInUse,
            onConfirm = {
                viewModel.deleteCustomCharacter(entry.reference.characterId)
                pendingDeletion = null
            },
            onDismiss = { pendingDeletion = null },
        )
    }

    pendingShare?.let { entry ->
        ShareCharacterDialog(
            characterId = entry.reference.characterId,
            characterName = entry.name,
            onDismiss = { pendingShare = null },
        )
    }

    selectedEntry?.let { entry ->
        Box(
            modifier = Modifier.fillMaxSize().then(Modifier),
            contentAlignment = Alignment.Center,
        ) {
            RetroContextMenu(
                modifier = Modifier.fillMaxWidth(0.82f),
                fontFamily = RetroPickerFont,
                onDismissRequest = { selectedEntry = null },
                items = listOf(
                    RetroContextMenuItem(stringResource(R.string.edit)) {
                        selectedEntry = null
                        navigator?.navigateTo(2, entry.reference.characterId)
                    },
                    RetroContextMenuItem(stringResource(R.string.delete_action)) {
                        selectedEntry = null
                        scope.launch {
                            isPendingDeletionInUse = viewModel.isCharacterInUse(entry.reference)
                            pendingDeletion = entry
                        }
                    },
                    RetroContextMenuItem(stringResource(R.string.share)) {
                        selectedEntry = null
                        pendingShare = entry
                    },
                    RetroContextMenuItem(stringResource(R.string.cancel)) { selectedEntry = null },
                ),
            )
        }
    }
}

private data class CollectionSection(
    val title: String,
    val entries: List<RadiantCollectionEntry>,
)

private fun buildCollectionSections(
    entries: List<RadiantCollectionEntry>,
    builtInTitle: String,
    userTitle: String,
): List<CollectionSection> {
    val builtIn = entries.filter { it.reference == BuiltInCharacters.defaultMonsterReference }
    val userCreated = entries.filter { it.isEditable }
    val importedPacks = entries
        .filter { !it.isEditable && it.reference != BuiltInCharacters.defaultMonsterReference }
        .groupBy { it.reference.packId }
        .values
        .sortedBy { it.firstOrNull()?.packName?.lowercase() }

    return buildList {
        if (builtIn.isNotEmpty()) add(CollectionSection(builtInTitle, builtIn))
        if (userCreated.isNotEmpty()) add(CollectionSection(userTitle, userCreated))
        importedPacks.forEach { packEntries ->
            add(CollectionSection(packEntries.first().packName, packEntries))
        }
    }
}

@Composable
private fun CollectionSectionHeader(title: String) {
    Text(
        text = title.uppercase(),
        modifier = Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 2.dp),
        fontFamily = RetroPickerFont,
        fontSize = 13.sp,
        color = RetroInk,
    )
}

@Composable
private fun CollectionMonsterRow(
    entry: RadiantCollectionEntry,
    onClick: (() -> Unit)?,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 2.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        androidx.compose.foundation.layout.Box(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        ) {
            CollectionMonsterRowContent(entry)
        }
    }
}

@Composable
private fun CollectionMonsterRowContent(entry: RadiantCollectionEntry) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.size(48.dp), contentAlignment = Alignment.Center) {
            val contentDescription = stringResource(
                if (entry.isRadiant) R.string.radiant_collection_sprite else R.string.collection_sprite,
                entry.name,
            )
            if (entry.imageFile != null) {
                AsyncImage(
                    model = entry.imageFile,
                    contentDescription = contentDescription,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize().alpha(if (entry.isUnlocked) 1f else 0.4f),
                )
            } else if (entry.fallbackArtwork != null) {
                Image(
                    painter = painterResource(entry.fallbackArtwork),
                    contentDescription = contentDescription,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
        Column(
            modifier = Modifier.padding(start = 4.dp),
            verticalArrangement = Arrangement.spacedBy(1.dp),
        ) {
            Text(
                text = entry.name.uppercase(),
                fontFamily = RetroPickerFont,
                fontSize = 16.sp,
                color = RetroInk,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            entry.level?.let { level ->
                val levelText = stringResource(R.string.roster_monster_level, level)
                val metadata = if (entry.isRadiant) {
                    val variant = stringResource(R.string.radiant)
                    stringResource(R.string.retro_picker_variant_and_level, variant, levelText)
                } else {
                    levelText
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
