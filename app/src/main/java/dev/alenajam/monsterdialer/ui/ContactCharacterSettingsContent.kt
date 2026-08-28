package dev.alenajam.monsterdialer.ui

import android.provider.ContactsContract
import android.content.ContentUris
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.Image
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import dev.alenajam.monsterdialer.R
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import dev.alenajam.monsterdialer.packs.CharacterAssignmentStore
import dev.alenajam.monsterdialer.packs.CharacterAssignmentTarget
import dev.alenajam.monsterdialer.packs.CharacterPackRepository
import dev.alenajam.monsterdialer.packs.CharacterReference
import dev.alenajam.monsterdialer.packs.CharacterType
import dev.alenajam.monsterdialer.packs.InstalledPackCharacter
import dev.alenajam.monsterdialer.characters.BuiltInCharacter
import dev.alenajam.monsterdialer.characters.BuiltInCharacters
import dev.alenajam.opendialer.feature.settings.LocalSettingsSubpageNavigator
import dev.alenajam.opendialer.feature.contacts.ContactPickerScreen
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private data class PickedContact(val name: String, val numbers: List<String>)

@Composable
fun ColumnScope.ContactCharacterSettingsContent() {
    val context = LocalContext.current
    val root = remember(context.filesDir) { File(context.filesDir, "character-packs") }
    val assignments = remember(root) { CharacterAssignmentStore(root) }
    val repository = remember(root) { CharacterPackRepository(root) }
    val trainers = remember(root) {
        repository.charactersAssignableTo(CharacterAssignmentTarget.Contact, CharacterType.Trainer)
    }
    val monsters = remember(root) {
        repository.charactersAssignableTo(CharacterAssignmentTarget.Contact, CharacterType.Monster)
    }
    var contact by remember {
        mutableStateOf<PickedContact?>(null)
    }
    var assignedTrainer by remember {
        mutableStateOf<CharacterReference?>(null)
    }
    var assignedMonster by remember {
        mutableStateOf<CharacterReference?>(null)
    }
    val lifecycleOwner = LocalLifecycleOwner.current
    val navigator = LocalSettingsSubpageNavigator.current
    val scope = rememberCoroutineScope()

    fun restoreSelectedContact() {
        scope.launch {
            val restored = withContext(Dispatchers.IO) { assignments.selectedContact() }
            val exists = restored == null || withContext(Dispatchers.IO) {
                contactExists(context, restored)
            }
            if (!exists) {
                withContext(Dispatchers.IO) { assignments.clearSelectedContact() }
                contact = null
                assignedTrainer = null
                assignedMonster = null
                return@launch
            }
            contact = restored?.let { PickedContact(it.label, it.contactKeys) }
            assignedTrainer = restored?.contactKeys?.firstNotNullOfOrNull {
                assignments.characterForContact(it, CharacterType.Trainer)
            }
            assignedMonster = restored?.contactKeys?.firstNotNullOfOrNull {
                assignments.characterForContact(it, CharacterType.Monster)
            }
        }
    }

    LaunchedEffect(assignments) { restoreSelectedContact() }
    DisposableEffect(lifecycleOwner, assignments) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                restoreSelectedContact()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    if (trainers.isEmpty() && monsters.isEmpty()) {
        Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
            ContactCharacterTypeSection(
                title = stringResource(R.string.character_type_trainer),
                defaultCharacter = BuiltInCharacters.trainer,
                characters = emptyList(),
                selected = null,
                onSelect = {}
            )
            ContactCharacterTypeSection(
                title = stringResource(R.string.character_type_monster),
                defaultCharacter = BuiltInCharacters.monster.character,
                characters = emptyList(),
                selected = null,
                onSelect = {}
            )
        }
        return
    }

    if (contact == null) {
        ContactChooser(
            hasCharacters = true,
            onChooseContact = { navigator?.navigateTo(0) }
        )
        return
    }

    val selected = requireNotNull(contact)
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    Icons.Outlined.Person,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(selected.name, style = MaterialTheme.typography.titleMedium)
                    Text(
                        selected.numbers.joinToString(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                TextButton(onClick = { navigator?.navigateTo(0) }) { Text(stringResource(R.string.change)) }
            }
        }

        ContactCharacterTypeSection(
            title = stringResource(R.string.character_type_trainer),
            defaultCharacter = BuiltInCharacters.trainer,
            characters = trainers,
            selected = assignedTrainer,
            onSelect = { reference ->
                scope.launch {
                    withContext(Dispatchers.IO) {
                        selected.numbers.forEach {
                            assignments.assignContact(
                                it,
                                CharacterType.Trainer,
                                reference,
                                selected.name
                            )
                        }
                    }
                    assignedTrainer = reference
                }
            }
        )
        ContactCharacterTypeSection(
            title = stringResource(R.string.character_type_monster),
            defaultCharacter = BuiltInCharacters.monster.character,
            characters = monsters,
            selected = assignedMonster,
            onSelect = { reference ->
                scope.launch {
                    withContext(Dispatchers.IO) {
                        selected.numbers.forEach {
                            assignments.assignContact(
                                it,
                                CharacterType.Monster,
                                reference,
                                selected.name
                            )
                        }
                    }
                    assignedMonster = reference
                }
            }
        )
    }
}

@Composable
private fun ContactCharacterTypeSection(
    title: String,
    defaultCharacter: BuiltInCharacter,
    characters: List<InstalledPackCharacter>,
    selected: CharacterReference?,
    onSelect: (CharacterReference?) -> Unit
) {
    val availableSelection = selected?.takeIf { reference ->
        characters.any { CharacterReference(it.packId, it.character.id) == reference }
    }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Column {
            CharacterOptionCard(
                name = defaultCharacter.name,
                subtitle = stringResource(R.string.built_in_character),
                isSelected = availableSelection == null,
                roundTop = true,
                roundBottom = false,
                artwork = {
                    Image(
                        painter = painterResource(defaultCharacter.contactArtwork.resource),
                        contentDescription = stringResource(R.string.default_character_artwork, title.lowercase()),
                        modifier = Modifier.size(72.dp)
                    )
                },
                onSelect = { onSelect(null) }
            )
            characters.forEachIndexed { index, item ->
                val reference = CharacterReference(item.packId, item.character.id)
                CharacterOptionCard(
                    name = item.character.name,
                    subtitle = item.packName,
                    isRadiant = item.character.isRadiant,
                    isSelected = availableSelection == reference,
                    roundTop = false,
                    roundBottom = index == characters.lastIndex,
                    artwork = {
                        AsyncImage(
                            model = item.imageFile(requireNotNull(item.character.frontImage)),
                            contentDescription = stringResource(R.string.character_artwork, item.character.name),
                            modifier = Modifier.size(72.dp)
                        )
                    },
                    onSelect = { onSelect(reference) }
                )
            }
            if (characters.isEmpty()) {
                NoAdditionalCharacterOptionsCard(title)
            }
        }
    }
}

@Composable
private fun ContactChooser(hasCharacters: Boolean, onChooseContact: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                Icons.Outlined.Person,
                contentDescription = null,
                modifier = Modifier.size(56.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    stringResource(R.string.contact_chooser_title),
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center
                )
                Text(
                    if (hasCharacters) {
                        stringResource(R.string.contact_chooser_prompt)
                    } else {
                        stringResource(R.string.contact_chooser_empty_prompt)
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (hasCharacters) {
                Button(onClick = onChooseContact) {
                    Text(stringResource(R.string.choose_contact))
                }
            }
        }
    }
}

private fun readContactNumbers(context: android.content.Context, contactId: Int): List<String> {
    val numbers = mutableListOf<String>()
    context.contentResolver.query(
        ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
        arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER),
        "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?",
        arrayOf(contactId.toString()),
        null
    )?.use { cursor ->
        while (cursor.moveToNext()) {
            cursor.getString(0)?.takeIf { it.isNotBlank() }?.let(numbers::add)
        }
    }
    return numbers.distinct()
}

private fun contactExists(context: android.content.Context, contact: dev.alenajam.monsterdialer.packs.SelectedContact): Boolean {
    return try {
        contact.contactId?.let { contactId ->
            context.contentResolver.query(
                ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, contactId.toLong()),
                arrayOf(ContactsContract.Contacts._ID),
                null,
                null,
                null
            )?.use { it.moveToFirst() } ?: false
        } ?: contact.contactKeys.any { number ->
            context.contentResolver.query(
                Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number)),
                arrayOf(ContactsContract.PhoneLookup._ID),
                null,
                null,
                null
            )?.use { it.moveToFirst() } == true
        }
    } catch (_: SecurityException) {
        // Without contacts permission, the contact's existence cannot be determined.
        true
    }
}

@Composable
fun ContactPickerDestination(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    val root = remember(context.filesDir) { File(context.filesDir, "character-packs") }
    val assignments = remember(root) { CharacterAssignmentStore(root) }
    val scope = rememberCoroutineScope()
    ContactPickerScreen(
        onNavigateBack = onNavigateBack,
        onContactSelected = { selectedContact ->
            scope.launch {
                withContext(Dispatchers.IO) {
                    assignments.setSelectedContact(
                        label = selectedContact.name,
                        contactKeys = readContactNumbers(context, selectedContact.id),
                        contactId = selectedContact.id
                    )
                }
                onNavigateBack()
            }
        }
    )
}
