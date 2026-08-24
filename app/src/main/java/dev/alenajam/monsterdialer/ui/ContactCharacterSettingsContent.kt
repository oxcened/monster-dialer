package dev.alenajam.monsterdialer.ui

import android.provider.ContactsContract
import java.io.File
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import dev.alenajam.monsterdialer.packs.*
import kotlinx.coroutines.*

private data class PickedContact(val name: String, val numbers: List<String>)

@Composable
fun ColumnScope.ContactCharacterSettingsContent() {
    val context = LocalContext.current
    val root = remember(context.filesDir) { File(context.filesDir, "character-packs") }
    val assignments = remember(root) { CharacterAssignmentStore(root) }
    val characters = remember(root) { CharacterPackRepository(root).charactersAssignableTo(CharacterAssignmentTarget.Contact) }
    var contact by remember { mutableStateOf<PickedContact?>(null) }
    var assigned by remember { mutableStateOf<CharacterReference?>(null) }
    val scope = rememberCoroutineScope()
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickContact()) { uri ->
        if (uri != null) scope.launch {
            contact = withContext(Dispatchers.IO) { readContact(context, uri) }
            assigned = contact?.numbers?.firstNotNullOfOrNull(assignments::characterForContact)
        }
    }
    if (contact == null) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Box(modifier = Modifier.fillMaxWidth().heightIn(min = 160.dp), contentAlignment = androidx.compose.ui.Alignment.Center) {
                androidx.compose.foundation.layout.Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                    Icon(Icons.Outlined.Person, contentDescription = null)
                    Text(
                        if (characters.isEmpty()) {
                            "Import and enable a pack containing a contact-assignable character first."
                        } else {
                            "Choose a contact to assign a character."
                        },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                        textAlign = TextAlign.Center
                    )
                    if (characters.isNotEmpty()) {
                        Button(onClick = { picker.launch(null) }) { Text("Choose contact") }
                    }
                }
            }
        }
    }
    contact?.let { selected ->
        Text(selected.name)
        Text(selected.numbers.joinToString())
        characters.forEach { item ->
            val ref = CharacterReference(item.packId, item.character.id)
            Card(Modifier.fillMaxWidth()) { androidx.compose.foundation.layout.Column(Modifier.padding(16.dp)) {
                AsyncImage(model = item.imageFile(item.character.frontImage), contentDescription = item.character.name)
                Text(item.character.name)
                if (assigned == ref) Text("Assigned") else Button(onClick = { scope.launch {
                    withContext(Dispatchers.IO) { selected.numbers.forEach { assignments.assignContact(it, ref) } }; assigned = ref
                } }) { Text("Assign") }
            } }
        }
        if (assigned != null) TextButton(onClick = { scope.launch {
            withContext(Dispatchers.IO) { selected.numbers.forEach { assignments.assignContact(it, null) } }; assigned = null
        } }) { Text("Clear assignment") }
    }
}

private fun readContact(context: android.content.Context, uri: android.net.Uri): PickedContact? {
    val contact = context.contentResolver.query(uri, arrayOf(ContactsContract.Contacts._ID, ContactsContract.Contacts.DISPLAY_NAME), null, null, null)
        ?.use { if (it.moveToFirst()) it.getString(0) to it.getString(1) else null } ?: return null
    val numbers = context.contentResolver.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER), "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?", arrayOf(contact.first), null)
        ?.use { cursor -> buildList { while (cursor.moveToNext()) add(cursor.getString(0)) } }.orEmpty()
    return PickedContact(contact.second.orEmpty(), numbers.filter(String::isNotBlank).distinct()).takeIf { it.numbers.isNotEmpty() }
}
