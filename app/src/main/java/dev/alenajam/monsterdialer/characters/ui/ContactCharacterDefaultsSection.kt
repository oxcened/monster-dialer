package dev.alenajam.monsterdialer.characters.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.alenajam.monsterdialer.R
import dev.alenajam.monsterdialer.app.ui.RetroFastScroller
import dev.alenajam.monsterdialer.app.ui.RetroContextMenuItem
import dev.alenajam.monsterdialer.app.ui.RetroContextMenuOverlay
import dev.alenajam.monsterdialer.app.ui.RetroFooter
import dev.alenajam.monsterdialer.app.ui.RetroSearchButton
import dev.alenajam.monsterdialer.characters.data.BuiltInCharacters
import dev.alenajam.monsterdialer.characters.data.ContactCharacterDefaults
import dev.alenajam.monsterdialer.packs.data.CharacterAssignmentTarget
import dev.alenajam.monsterdialer.packs.data.CharacterReference
import dev.alenajam.monsterdialer.packs.data.CharacterType
import dev.alenajam.monsterdialer.packs.data.InstalledPackCharacter
import dev.alenajam.opendialer.feature.settings.LocalSettingsBackInterceptor
import dev.alenajam.opendialer.feature.settings.LocalSettingsSubpageNavigator

private enum class DefaultsOptionsMenu {
    Root,
    Mode,
    Characters,
    RandomizerPool,
}

/** Edits the global contact-character defaults and randomizer pools. */
@Composable
internal fun ContactCharacterDefaultsSection(
    viewModel: ContactCharacterSettingsViewModel,
    trainers: List<InstalledPackCharacter>,
    monsters: List<InstalledPackCharacter>,
    defaults: ContactCharacterDefaults,
    onDefaultChanged: (CharacterType, CharacterReference?) -> Unit,
    onPoolChanged: (CharacterType, List<CharacterReference>) -> Unit,
    onPoolReset: (CharacterType) -> Unit,
    onAddCharacter: (CharacterType) -> Unit,
    isAddEnabled: Boolean,
) {
    val settingsNavigator = LocalSettingsSubpageNavigator.current
    val settingsBackInterceptor = LocalSettingsBackInterceptor.current
    var selectedType by remember { mutableStateOf(CharacterType.Trainer) }
    val draftPools = remember { mutableStateMapOf<CharacterType, Set<CharacterReference>>() }
    var resetPools by remember { mutableStateOf(emptySet<CharacterType>()) }
    var optionsMenu by remember { mutableStateOf<DefaultsOptionsMenu?>(null) }
    val selectedDefault = defaults.defaults[selectedType]
    val unlockedVariants by viewModel.unlockedVariants.collectAsStateWithLifecycle()
    val filter by viewModel.filter.collectAsStateWithLifecycle()
    val effectivePoolMode = selectedDefault == null
    val characters = if (selectedType == CharacterType.Trainer) trainers else monsters
    val characterTitle = stringResource(if (selectedType == CharacterType.Trainer) R.string.character_type_trainer else R.string.character_type_monster)
    val pluralCharacterTitle = stringResource(if (selectedType == CharacterType.Trainer) R.string.character_type_trainers else R.string.character_type_monsters)
    val allPoolReferences = viewModel.allContactPoolReferences(selectedType)
    val savedPool = viewModel.selectedContactPool(selectedType, allPoolReferences)
    val selectedPool = if (selectedType in resetPools) allPoolReferences else draftPools[selectedType] ?: savedPool
    val hasUnsavedEmptyPool = draftPools.values.any(Set<CharacterReference>::isEmpty)
    val listState = rememberLazyListState()
    LaunchedEffect(defaults.randomPools) {
        draftPools.entries.toList().forEach { (type, draftPool) ->
            if (draftPool.isNotEmpty() && defaults.randomPools[type]?.toSet() == draftPool) draftPools.remove(type)
        }
        resetPools = resetPools - resetPools.filter { type -> defaults.randomPools[type] == null }.toSet()
    }

    RandomPoolEditorBackHandling(hasUnsavedEmptyPool) { draftPools.clear() }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.White,
    ) {
    Box(modifier = Modifier.fillMaxSize()) {
    Column(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ContactDefaultsDropdowns(
                    selectedType = selectedType,
                    onTypeSelected = { selectedType = it },
                    onOpenOptions = { optionsMenu = DefaultsOptionsMenu.Root },
                )
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clipToBounds(),
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize(),
                contentPadding = PaddingValues(bottom = 72.dp),
            ) {
                if (effectivePoolMode) {
                    item(key = "randomizer-description") {
                        Text(
                            text = stringResource(R.string.contact_random_pool_description),
                            modifier = Modifier.padding(top = 8.dp, bottom = 8.dp),
                            style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                            color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                characterTypeItems(
                    title = characterTitle,
                    pluralTitle = pluralCharacterTitle,
                    defaultCharacter = if (selectedType == CharacterType.Trainer) BuiltInCharacters.trainer else BuiltInCharacters.monster.character,
                    characters = characters,
                    selected = if (effectivePoolMode) null else selectedDefault,
                    defaultReference = if (selectedType == CharacterType.Trainer) BuiltInCharacters.defaultTrainerReference else BuiltInCharacters.defaultMonsterReference,
                    defaultArtwork = { it.contactArtwork },
                    artworkTarget = CharacterAssignmentTarget.Contact,
                    unlockedVariants = unlockedVariants,
                    onSelect = { reference ->
                        if (effectivePoolMode) updateRandomPoolDraft(selectedType, if (reference == null) selectedPool else selectedPool + reference, draftPools) { type, pool -> onPoolChanged(type, pool.toList()) }
                        else onDefaultChanged(selectedType, reference)
                    },
                    isRandomSelected = false,
                    onRandomize = null,
                    showRandomize = false,
                    selectedReferences = if (effectivePoolMode) selectedPool else emptySet(),
                    filter = if (selectedType == CharacterType.Monster) filter else MonsterFilter.All,
                    hideLockedVariants = true,
                    onSelected = if (effectivePoolMode) { reference ->
                        updateRandomPoolDraft(selectedType, selectedPool - reference, draftPools) { type, pool -> onPoolChanged(type, pool.toList()) }
                    } else null,
                )
            }
            RetroFastScroller(
                listState = listState,
                contentDescription = stringResource(R.string.character_fast_scroller),
                modifier = Modifier.align(Alignment.CenterEnd).padding(vertical = 8.dp),
            )
        }
        RetroFooter(
            backKey = stringResource(R.string.retro_key_b),
            backLabel = stringResource(R.string.customized_contacts_back_action),
            onBack = {
                if (settingsBackInterceptor?.consumesBackNavigation() != true) {
                    settingsNavigator?.navigateBack()
                }
            },
        )
    }
    optionsMenu?.let { menu ->
        val modeItems = listOf(
            RetroContextMenuItem(
                label = stringResource(
                    if (selectedType == CharacterType.Trainer) {
                        R.string.contact_choose_trainer
                    } else {
                        R.string.contact_choose_monster
                    },
                ),
                showCursor = !effectivePoolMode,
            ) {
                draftPools.remove(selectedType)
                onDefaultChanged(
                    selectedType,
                    if (selectedType == CharacterType.Trainer) {
                        BuiltInCharacters.defaultTrainerReference
                    } else {
                        BuiltInCharacters.defaultMonsterReference
                    },
                )
                optionsMenu = null
            },
            RetroContextMenuItem(
                label = stringResource(R.string.randomize),
                showCursor = effectivePoolMode,
            ) {
                onDefaultChanged(selectedType, null)
                optionsMenu = null
            },
        )
        val filterItems = if (selectedType == CharacterType.Monster) {
            listOf(
                RetroContextMenuItem(
                    label = stringResource(R.string.filter_all),
                    showCursor = filter == MonsterFilter.All,
                    dividerBefore = true,
                ) {
                    viewModel.setFilter(MonsterFilter.All)
                    optionsMenu = null
                },
                RetroContextMenuItem(
                    label = stringResource(R.string.filter_regular),
                    showCursor = filter == MonsterFilter.Regular,
                ) {
                    viewModel.setFilter(MonsterFilter.Regular)
                    optionsMenu = null
                },
                RetroContextMenuItem(
                    label = stringResource(R.string.filter_unlocked_radiant),
                    showCursor = filter == MonsterFilter.RadiantUnlocked,
                ) {
                    viewModel.setFilter(MonsterFilter.RadiantUnlocked)
                    optionsMenu = null
                },
            )
        } else {
            emptyList()
        }
        val addItem = if (isAddEnabled) {
            RetroContextMenuItem(
                label = stringResource(R.string.add),
                dividerBefore = true,
            ) {
                optionsMenu = null
                onAddCharacter(selectedType)
            }
        } else {
            null
        }
        val poolItems = if (effectivePoolMode) {
            listOf(
                RetroContextMenuItem(
                    label = stringResource(R.string.contact_random_pool_select_all),
                    dividerBefore = true,
                ) {
                    resetPools = resetPools - selectedType
                    updateRandomPoolDraft(selectedType, allPoolReferences, draftPools) { type, pool -> onPoolChanged(type, pool.toList()) }
                    optionsMenu = null
                },
                RetroContextMenuItem(stringResource(R.string.contact_random_pool_deselect_all)) {
                    resetPools = resetPools - selectedType
                    updateRandomPoolDraft(selectedType, emptySet(), draftPools) { type, pool -> onPoolChanged(type, pool.toList()) }
                    optionsMenu = null
                },
                RetroContextMenuItem(stringResource(R.string.contact_random_pool_reset)) {
                    draftPools.remove(selectedType)
                    resetPools = resetPools + selectedType
                    onPoolReset(selectedType)
                    optionsMenu = null
                },
            )
        } else {
            emptyList()
        }
        RetroContextMenuOverlay(
            modifier = Modifier.fillMaxWidth(0.82f),
            fontFamily = RetroPickerFont,
            onDismissRequest = { optionsMenu = null },
            title = when (menu) {
                DefaultsOptionsMenu.Root -> null
                DefaultsOptionsMenu.Mode -> stringResource(R.string.contact_defaults_menu_mode)
                DefaultsOptionsMenu.Characters -> stringResource(R.string.contact_defaults_menu_characters)
                DefaultsOptionsMenu.RandomizerPool -> stringResource(R.string.contact_defaults_menu_pool)
            },
            items = when (menu) {
                DefaultsOptionsMenu.Root -> buildList {
                    add(RetroContextMenuItem(stringResource(R.string.contact_defaults_menu_mode)) { optionsMenu = DefaultsOptionsMenu.Mode })
                    add(RetroContextMenuItem(stringResource(R.string.contact_defaults_menu_characters)) { optionsMenu = DefaultsOptionsMenu.Characters })
                    if (effectivePoolMode) {
                        add(RetroContextMenuItem(stringResource(R.string.contact_defaults_menu_pool)) { optionsMenu = DefaultsOptionsMenu.RandomizerPool })
                    }
                    add(RetroContextMenuItem.cancel(stringResource(R.string.cancel)) { optionsMenu = null })
                }
                DefaultsOptionsMenu.Mode -> modeItems + RetroContextMenuItem.cancel(stringResource(R.string.back)) { optionsMenu = DefaultsOptionsMenu.Root }
                DefaultsOptionsMenu.Characters -> filterItems + listOfNotNull(addItem) + RetroContextMenuItem.cancel(stringResource(R.string.back)) { optionsMenu = DefaultsOptionsMenu.Root }
                DefaultsOptionsMenu.RandomizerPool -> poolItems + RetroContextMenuItem.cancel(stringResource(R.string.back)) { optionsMenu = DefaultsOptionsMenu.Root }
            },
        )
    }
    }
    }
}
