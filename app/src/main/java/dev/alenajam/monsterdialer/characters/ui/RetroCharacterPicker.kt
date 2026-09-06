package dev.alenajam.monsterdialer.characters.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import dev.alenajam.monsterdialer.R
import dev.alenajam.monsterdialer.app.ui.RetroSelectionArrow
import dev.alenajam.monsterdialer.app.ui.RetroSelectionArrowSize
import dev.alenajam.monsterdialer.battle.ui.BattleDialogue
import dev.alenajam.monsterdialer.characters.data.BuiltInCharacter
import dev.alenajam.monsterdialer.characters.data.BuiltInCharacters
import dev.alenajam.monsterdialer.packs.data.CharacterAssignmentTarget
import dev.alenajam.monsterdialer.packs.data.CharacterReference
import dev.alenajam.monsterdialer.packs.data.CharacterType
import dev.alenajam.monsterdialer.packs.data.InstalledPackCharacter
import dev.alenajam.monsterdialer.packs.data.CharacterVisualVariant
import java.io.File

private val RetroPickerFont = FontFamily(Font(R.font.ui_pixel_font))

private data class RetroCharacterEntry(
    val key: String,
    val reference: CharacterReference?,
    val name: String,
    val level: Int? = null,
    val isRadiant: Boolean = false,
    val isUnlocked: Boolean = true,
    val artwork: File? = null,
    val builtInArtwork: Int? = null,
    val section: String,
)

@Composable
internal fun RetroCharacterPicker(
    type: CharacterType,
    selected: CharacterReference?,
    characters: List<InstalledPackCharacter>,
    unlockedVariants: Set<CharacterReference>,
    filter: MonsterFilter,
    defaultCharacter: BuiltInCharacter,
    defaultArtwork: BuiltInCharacter.() -> Int,
    onAssign: (CharacterReference?) -> Unit,
    onBack: () -> Unit,
) {
    val entries = remember(type, characters, unlockedVariants, filter, defaultCharacter) {
        retroCharacterEntries(type, characters, unlockedVariants, filter, defaultCharacter, defaultArtwork)
    }
    var pendingSelection by remember(selected, entries) { mutableStateOf(selected) }
    val selectedEntry = entries.firstOrNull { it.reference == pendingSelection }
    val prompt = stringResource(
        if (type == CharacterType.Trainer) R.string.contact_choose_trainer else R.string.contact_choose_monster,
    )

    LaunchedEffect(selected) { pendingSelection = selected }

    Column(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
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
                RetroCharacterRow(entry, entry.reference == pendingSelection) {
                    if (entry.isUnlocked) pendingSelection = entry.reference
                }
            }
        }
        BattleDialogue(
            message = prompt,
            dialogueId = 0,
            isTyping = false,
            timing = dev.alenajam.monsterdialer.battle.data.BattleTiming.Instant,
            onCompleted = {},
            modifier = Modifier.align(Alignment.CenterHorizontally).padding(vertical = 2.dp),
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            RetroPickerAction(
                key = stringResource(R.string.retro_key_a),
                label = stringResource(R.string.customized_contacts_assign_action),
                enabled = selectedEntry != null,
                onClick = { onAssign(pendingSelection) },
            )
            RetroPickerAction(
                key = stringResource(R.string.retro_key_b),
                label = stringResource(R.string.customized_contacts_back_action),
                onClick = onBack,
            )
        }
    }
}

@Composable
internal fun RetroPickerModeBar(
    selectedType: CharacterType,
    mode: ContactAssignmentMode,
    onModeChanged: (ContactAssignmentMode) -> Unit,
) {
    val chooseLabel = stringResource(
        if (selectedType == CharacterType.Trainer) R.string.contact_choose_trainer else R.string.contact_choose_monster,
    )
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 2.dp)) {
        Row(
            modifier = Modifier.padding(top = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            RetroPickerChoice(
                label = stringResource(R.string.contact_character_source_global),
                selected = mode == ContactAssignmentMode.Global,
                onClick = { onModeChanged(ContactAssignmentMode.Global) },
            )
            RetroPickerChoice(
                label = chooseLabel,
                selected = mode == ContactAssignmentMode.Custom,
                onClick = { onModeChanged(ContactAssignmentMode.Custom) },
            )
            RetroPickerChoice(
                label = stringResource(R.string.randomize),
                selected = mode == ContactAssignmentMode.Random,
                onClick = { onModeChanged(ContactAssignmentMode.Random) },
            )
        }
    }
}

@Composable
private fun RetroPickerChoice(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier.clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        if (selected) {
            RetroSelectionArrow(
                tint = RetroInk,
            )
        } else {
            Spacer(modifier = Modifier.size(RetroSelectionArrowSize))
        }
        Text(label.uppercase(), fontFamily = RetroPickerFont, fontSize = 12.sp, color = RetroInk)
    }
}

@Composable
private fun RetroCharacterRow(
    entry: RetroCharacterEntry,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 1.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (isSelected) {
            RetroSelectionArrow(
                tint = RetroInk,
            )
        } else {
            Spacer(modifier = Modifier.size(RetroSelectionArrowSize))
        }
        Box(modifier = Modifier.size(52.dp), contentAlignment = Alignment.Center) {
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
                text = buildString {
                    append(entry.name.uppercase())
                    if (entry.isRadiant) append(" ✦")
                },
                fontFamily = RetroPickerFont,
                fontSize = 16.sp,
                color = if (entry.isUnlocked) RetroInk else RetroInk.copy(alpha = 0.65f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (entry.level != null) {
                val variant = stringResource(if (entry.isRadiant) R.string.radiant else R.string.regular)
                val level = stringResource(R.string.roster_monster_level, entry.level)
                Text(
                    text = stringResource(R.string.monster_variant_and_level, variant, level),
                    fontFamily = RetroPickerFont,
                    fontSize = 13.sp,
                    color = RetroInk.copy(alpha = 0.75f),
                )
            }
            if (!entry.isUnlocked) {
                Text(
                    text = stringResource(R.string.locked).uppercase(),
                    fontFamily = RetroPickerFont,
                    fontSize = 13.sp,
                    color = RetroInk.copy(alpha = 0.7f),
                )
            }
        }
    }
}

@Composable
private fun RetroPickerAction(
    key: String,
    label: String,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier.clickable(enabled = enabled, onClick = onClick).padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(modifier = Modifier.background(if (enabled) RetroInk else RetroInk.copy(alpha = 0.35f)).padding(horizontal = 8.dp, vertical = 3.dp)) {
            Text(key, fontFamily = RetroPickerFont, fontSize = 14.sp, color = Color.White)
        }
        Text(label, fontFamily = RetroPickerFont, fontSize = 18.sp, color = if (enabled) RetroInk else RetroInk.copy(alpha = 0.35f))
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
            if (type == CharacterType.Monster && !filter.matches(variant, reference, unlockedVariants)) return@forEach
            val image = if (type == CharacterType.Trainer) variant.frontImage ?: variant.backImage else variant.frontImage ?: variant.backImage
            result += RetroCharacterEntry(
                key = "${installed.packId}:${installed.character.id}:${variant.id}",
                reference = reference,
                name = installed.character.name,
                level = installed.character.level,
                isRadiant = variant.isRadiant,
                isUnlocked = !variant.isRadiant || reference in unlockedVariants,
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
    MonsterFilter.RadiantLocked -> variant.isRadiant && reference !in unlockedVariants
}
