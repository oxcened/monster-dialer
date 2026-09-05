package dev.alenajam.monsterdialer.characters.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.alenajam.monsterdialer.R
import dev.alenajam.monsterdialer.characters.data.BuiltInCharacters
import dev.alenajam.monsterdialer.characters.data.ContactCharacterDefaults
import dev.alenajam.monsterdialer.packs.data.CharacterAssignmentTarget
import dev.alenajam.monsterdialer.packs.data.CharacterReference
import dev.alenajam.monsterdialer.packs.data.CharacterType
import dev.alenajam.monsterdialer.packs.data.InstalledPackCharacter

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
) {
    var selectedType by remember { mutableStateOf(CharacterType.Trainer) }
    val draftPools = remember { mutableStateMapOf<CharacterType, Set<CharacterReference>>() }
    val selectedDefault = defaults.defaults[selectedType]
    val unlockedVariants by viewModel.unlockedVariants.collectAsStateWithLifecycle()
    val effectivePoolMode = selectedDefault == null
    val characters = if (selectedType == CharacterType.Trainer) trainers else monsters
    val characterTitle = stringResource(if (selectedType == CharacterType.Trainer) R.string.character_type_trainer else R.string.character_type_monster)
    val pluralCharacterTitle = stringResource(if (selectedType == CharacterType.Trainer) R.string.character_type_trainers else R.string.character_type_monsters)
    val allPoolReferences = viewModel.allContactPoolReferences(selectedType)
    val savedPool = viewModel.selectedContactPool(selectedType, allPoolReferences)
    val selectedPool = draftPools[selectedType] ?: savedPool
    val hasUnsavedEmptyPool = draftPools.values.any(Set<CharacterReference>::isEmpty)

    LaunchedEffect(defaults.randomPools) {
        draftPools.entries.toList().forEach { (type, draftPool) ->
            if (draftPool.isNotEmpty() && defaults.randomPools[type]?.toSet() == draftPool) draftPools.remove(type)
        }
    }

    RandomPoolEditorBackHandling(hasUnsavedEmptyPool) { draftPools.clear() }

    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ContactDefaultsDropdowns(
                selectedType = selectedType,
                onTypeSelected = { selectedType = it },
                isPoolMode = effectivePoolMode,
                onPoolModeChanged = { isRandomizer ->
                    if (!isRandomizer) draftPools.remove(selectedType)
                    onDefaultChanged(selectedType, if (isRandomizer) null else if (selectedType == CharacterType.Trainer) BuiltInCharacters.defaultTrainerReference else BuiltInCharacters.defaultMonsterReference)
                },
            )
        }
        if (effectivePoolMode) {
            Column(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(modifier = Modifier.weight(1f), onClick = { draftPools.remove(selectedType); onPoolReset(selectedType) }) {
                        Text(stringResource(R.string.contact_random_pool_reset))
                    }
                    OutlinedButton(
                        modifier = Modifier.weight(1f),
                        onClick = {
                            updateRandomPoolDraft(selectedType, if (selectedPool == allPoolReferences) emptySet() else allPoolReferences, draftPools) { type, pool -> onPoolChanged(type, pool.toList()) }
                        },
                    ) {
                        Text(stringResource(if (selectedPool == allPoolReferences) R.string.contact_random_pool_deselect_all else R.string.contact_random_pool_select_all))
                    }
                }
            }
        }
        LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f), contentPadding = PaddingValues(0.dp)) {
            if (effectivePoolMode) {
                item(key = "randomizer-description") {
                    Text(
                        text = stringResource(R.string.contact_random_pool_description),
                        modifier = Modifier.padding(bottom = 8.dp),
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
                filter = MonsterFilter.All,
                onSelected = if (effectivePoolMode) { reference ->
                    updateRandomPoolDraft(selectedType, selectedPool - reference, draftPools) { type, pool -> onPoolChanged(type, pool.toList()) }
                } else null,
            )
        }
    }
}
