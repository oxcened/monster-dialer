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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.ContentScale
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
import dev.alenajam.monsterdialer.app.ui.RetroSearchButton
import dev.alenajam.monsterdialer.characters.data.CharactersRepository
import dev.alenajam.monsterdialer.characters.data.RadiantVariantUnlockStore
import dev.alenajam.monsterdialer.packs.data.CharacterAssignmentTarget
import dev.alenajam.monsterdialer.packs.data.CharacterReference
import dev.alenajam.monsterdialer.packs.data.CharacterType
import dev.alenajam.opendialer.core.common.ui.AppIcon
import coil.compose.AsyncImage
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class RadiantCollectionViewModel @Inject constructor(
    charactersRepository: CharactersRepository,
    radiantUnlocks: RadiantVariantUnlockStore,
) : ViewModel() {
    val entries: StateFlow<List<RadiantCollectionEntry>> = combine(
        charactersRepository.observeCharactersAssignableTo(
            CharacterAssignmentTarget.Player,
            CharacterType.Monster,
        ),
        radiantUnlocks.unlocked,
    ) { characters, unlocked ->
        characters.flatMap { installed ->
            installed.character.visualVariants
                .mapNotNull { variant ->
                    val imagePath = variant.frontImage ?: variant.backImage ?: return@mapNotNull null
                    val imageFile = installed.imageFile(imagePath).takeIf { it.isFile } ?: return@mapNotNull null
                    val reference = CharacterReference(installed.packId, installed.character.id, variant.id)
                    RadiantCollectionEntry(
                        name = installed.character.name,
                        level = installed.character.level,
                        reference = reference,
                        imageFile = imageFile,
                        isRadiant = variant.isRadiant,
                        isUnlocked = !variant.isRadiant || reference in unlocked,
                    )
                }
        }.sortedWith(compareBy({ it.name.lowercase() }, { it.isRadiant }))
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList(),
    )
}

data class RadiantCollectionEntry(
    val name: String,
    val level: Int?,
    val reference: CharacterReference,
    val imageFile: File,
    val isRadiant: Boolean,
    val isUnlocked: Boolean,
)

@Composable
fun RadiantCollectionScreen(viewModel: RadiantCollectionViewModel = hiltViewModel()) {
    val entries by viewModel.entries.collectAsStateWithLifecycle()
    val navigator = dev.alenajam.opendialer.feature.settings.LocalSettingsSubpageNavigator.current

    Column(modifier = Modifier.fillMaxSize()) {
        RetroSearchButton(
            label = stringResource(R.string.radiant_collection_browse_packs),
            onClick = { navigator?.navigateTo(1) },
            modifier = Modifier.align(Alignment.End),
        )
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(top = 2.dp, bottom = 12.dp),
        ) {
            items(entries.size, key = { index ->
                val entry = entries[index]
                "${entry.reference.packId}:${entry.reference.characterId}:${entry.reference.variantId}"
            }) { index ->
                CollectionMonsterRow(entries[index])
            }
        }
        RetroFooter(
            backKey = stringResource(R.string.retro_key_b),
            backLabel = stringResource(R.string.retro_action_back_label),
            onBack = { navigator?.navigateBack() },
        )
    }
}

@Composable
private fun CollectionMonsterRow(entry: RadiantCollectionEntry) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 2.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.size(48.dp), contentAlignment = Alignment.Center) {
            AsyncImage(
                model = entry.imageFile,
                contentDescription = stringResource(
                    if (entry.isRadiant) R.string.radiant_collection_sprite else R.string.collection_sprite,
                    entry.name,
                ),
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize().alpha(if (entry.isUnlocked) 1f else 0.4f),
            )
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
