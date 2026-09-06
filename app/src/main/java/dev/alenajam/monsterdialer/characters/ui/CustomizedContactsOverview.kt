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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import dev.alenajam.monsterdialer.R
import dev.alenajam.monsterdialer.app.ui.LocalMonsterAppIcons
import dev.alenajam.monsterdialer.app.ui.RetroContextMenu
import dev.alenajam.monsterdialer.app.ui.RetroContextMenuItem
import dev.alenajam.monsterdialer.app.ui.RetroActionButton
import dev.alenajam.monsterdialer.app.ui.RetroSelectionArrow
import dev.alenajam.monsterdialer.app.ui.RetroSelectionArrowSize
import dev.alenajam.monsterdialer.battle.data.BattleTiming
import dev.alenajam.monsterdialer.battle.ui.BattleDialogue
import dev.alenajam.monsterdialer.characters.data.BuiltInCharacters
import dev.alenajam.monsterdialer.characters.data.ContactCharacterMode
import dev.alenajam.monsterdialer.characters.data.ContactCharacterOverview
import dev.alenajam.monsterdialer.packs.data.CharacterReference
import dev.alenajam.monsterdialer.packs.data.CharacterType
import dev.alenajam.monsterdialer.packs.data.InstalledPackCharacter
import dev.alenajam.opendialer.core.common.ui.AppIcon
import java.io.File

private val ContactRosterPixelFont = FontFamily(Font(R.font.ui_pixel_font))

@Composable
internal fun CustomizedContactsOverview(
    contacts: List<ContactCharacterOverview>,
    trainers: List<InstalledPackCharacter>,
    monsters: List<InstalledPackCharacter>,
    highlightedContactKey: String?,
    isAddEnabled: Boolean,
    onAddContact: () -> Unit,
    onBack: () -> Unit,
    onContactMenuOpened: (ContactCharacterOverview) -> Unit,
    onContactSelected: (ContactCharacterOverview, CharacterType) -> Unit,
) {
    var menuContact by remember { mutableStateOf<ContactCharacterOverview?>(null) }
    Box(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
        val cursorContactKey = contacts.firstOrNull { it.contactKey == highlightedContactKey }?.contactKey
            ?: contacts.firstOrNull()?.contactKey
        if (contacts.isEmpty()) {
            Spacer(modifier = Modifier.weight(1f))
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(vertical = 2.dp),
            ) {
                itemsIndexed(contacts, key = { _, contact -> contact.contactKey }) { index, contact ->
                    ContactRosterRow(
                        contact = contact,
                        trainers = trainers,
                        monsters = monsters,
                        showCursor = contact.contactKey == cursorContactKey,
                        onClick = {
                            onContactMenuOpened(contact)
                            menuContact = contact
                        },
                    )
                }
            }
        }

        BattleDialogue(
            message = stringResource(R.string.customized_contacts_prompt),
            dialogueId = 0,
            isTyping = false,
            timing = BattleTiming.Instant,
            onCompleted = {},
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(vertical = 2.dp),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            RetroActionButton(
                key = stringResource(R.string.retro_key_a),
                label = stringResource(R.string.customized_contacts_assign_action),
                enabled = isAddEnabled,
                onClick = onAddContact,
                arrowSize = 14.dp,
            )
            RetroActionButton(
                key = stringResource(R.string.retro_key_b),
                label = stringResource(R.string.customized_contacts_back_action),
                onClick = onBack,
                arrowSize = 14.dp,
            )
        }
        }
        menuContact?.let { contact ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable { menuContact = null },
                contentAlignment = Alignment.Center,
            ) {
                RetroContextMenu(
                    modifier = Modifier.fillMaxWidth(0.72f),
                    fontFamily = ContactRosterPixelFont,
                    items = listOf(
                        RetroContextMenuItem(
                            label = stringResource(R.string.contact_choose_trainer),
                            showCursor = true,
                        ) {
                        menuContact = null
                        onContactSelected(contact, CharacterType.Trainer)
                        },
                        RetroContextMenuItem(
                            label = stringResource(R.string.contact_choose_monster),
                        ) {
                        menuContact = null
                        onContactSelected(contact, CharacterType.Monster)
                        },
                        RetroContextMenuItem(label = stringResource(R.string.cancel)) { menuContact = null },
                    ),
                )
            }
        }
    }
}

@Composable
private fun ContactRosterRow(
    contact: ContactCharacterOverview,
    trainers: List<InstalledPackCharacter>,
    monsters: List<InstalledPackCharacter>,
    showCursor: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(66.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (showCursor) {
            RetroSelectionArrow(
                tint = RetroInk,
            )
        } else {
            Spacer(modifier = Modifier.size(RetroSelectionArrowSize))
        }
        Spacer(modifier = Modifier.size(2.dp))
        AssignmentSprite(
            selection = contact.trainer,
            characters = trainers,
            modifier = Modifier.size(42.dp),
        )
        Column(
            modifier = Modifier.weight(1f).padding(horizontal = 5.dp),
            verticalArrangement = Arrangement.spacedBy(1.dp),
        ) {
            Text(
                text = contact.label.uppercase(),
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontFamily = ContactRosterPixelFont,
                    fontSize = 18.sp,
                ),
                color = RetroInk,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssignmentName(
                    selection = contact.trainer,
                    characters = trainers,
                    modifier = Modifier.weight(1f),
                )
                AssignmentName(
                    selection = contact.monster,
                    characters = monsters,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.End,
                )
            }
        }
        AssignmentSprite(
            selection = contact.monster,
            characters = monsters,
            modifier = Modifier.size(42.dp),
        )
    }
}

@Composable
private fun AssignmentSprite(
    selection: dev.alenajam.monsterdialer.characters.data.ContactCharacterSelection,
    characters: List<InstalledPackCharacter>,
    modifier: Modifier,
) {
    val character = selection.character
    val installed = character?.let { reference ->
        characters.firstOrNull { it.packId == reference.packId && it.character.id == reference.characterId }
    }
    val artwork = character?.let { reference -> installed?.artworkFile(reference) }
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        when {
            selection.mode == ContactCharacterMode.Random -> AppIcon(
                icon = LocalMonsterAppIcons.current.randomize,
                contentDescription = null,
                tint = RetroInk,
                modifier = Modifier.size(32.dp),
            )
            character == BuiltInCharacters.defaultTrainerReference -> Image(
                painter = painterResource(BuiltInCharacters.trainer.contactArtwork.resource),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
            )
            character == BuiltInCharacters.defaultMonsterReference -> Image(
                painter = painterResource(BuiltInCharacters.monster.character.contactArtwork.resource),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
            )
            artwork != null -> AsyncImage(
                model = artwork,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
private fun AssignmentName(
    selection: dev.alenajam.monsterdialer.characters.data.ContactCharacterSelection,
    characters: List<InstalledPackCharacter>,
    modifier: Modifier = Modifier,
    textAlign: TextAlign = TextAlign.Start,
) {
    val character = selection.character
    val installed = character?.let { reference ->
        characters.firstOrNull { it.packId == reference.packId && it.character.id == reference.characterId }
    }
    val name = when {
        selection.mode == ContactCharacterMode.Random -> stringResource(R.string.randomize)
        character == BuiltInCharacters.defaultTrainerReference -> BuiltInCharacters.trainer.name
        character == BuiltInCharacters.defaultMonsterReference -> BuiltInCharacters.monster.character.name
        installed != null -> installed.character.name
        else -> stringResource(R.string.contact_assignment_unavailable)
    }
    Text(
        text = name.uppercase(),
        modifier = modifier,
        style = MaterialTheme.typography.bodyMedium.copy(
            fontFamily = ContactRosterPixelFont,
            fontSize = 14.sp,
        ),
        color = RetroInk,
        textAlign = textAlign,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

private fun InstalledPackCharacter.artworkFile(reference: CharacterReference): File? {
    val variant = character.variant(reference.variantId) ?: return null
    val image = variant.frontImage ?: variant.backImage ?: return null
    return image.let(::imageFile)
}
