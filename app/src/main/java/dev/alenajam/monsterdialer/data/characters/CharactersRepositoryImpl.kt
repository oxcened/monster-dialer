package dev.alenajam.monsterdialer.data.characters

import android.app.Application
import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.ContactsContract
import dev.alenajam.monsterdialer.packs.CharacterAssignmentStore
import dev.alenajam.monsterdialer.packs.CharacterAssignmentTarget
import dev.alenajam.monsterdialer.packs.CharacterPackRepository
import dev.alenajam.monsterdialer.packs.CharacterReference
import dev.alenajam.monsterdialer.packs.CharacterType
import dev.alenajam.monsterdialer.packs.InstalledPackCharacter
import dev.alenajam.monsterdialer.packs.SelectedContact
import dev.alenajam.opendialer.data.contacts.DialerContactSummary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CharactersRepositoryImpl @Inject constructor(
    private val app: Application,
    private val assignments: CharacterAssignmentStore,
    private val repository: CharacterPackRepository
) : CharactersRepository {

    override fun getCharactersAssignableTo(
        role: CharacterAssignmentTarget,
        type: CharacterType?
    ): List<InstalledPackCharacter> {
        return repository.charactersAssignableTo(role, type)
    }

    override suspend fun getSelectedContact(): MonsterContact? = withContext(Dispatchers.IO) {
        val restored = assignments.selectedContact()
        val exists = restored == null || contactExists(app, restored)
        if (!exists) {
            assignments.clearSelectedContact()
            null
        } else {
            restored?.let { MonsterContact(it.label, it.contactKeys) }
        }
    }

    override suspend fun setSelectedContact(selectedContact: DialerContactSummary) = withContext(Dispatchers.IO) {
        val numbers = readContactNumbers(app, selectedContact.id)
        assignments.setSelectedContact(
            label = selectedContact.name,
            contactKeys = numbers,
            contactId = selectedContact.id
        )
    }

    override suspend fun clearSelectedContact() = withContext(Dispatchers.IO) {
        assignments.clearSelectedContact()
    }

    override suspend fun getAssignedCharacter(
        contactKey: String,
        type: CharacterType
    ): CharacterReference? = withContext(Dispatchers.IO) {
        assignments.characterForContact(contactKey, type)
    }

    override suspend fun assignCharacter(
        contactKey: String,
        type: CharacterType,
        reference: CharacterReference?,
        label: String?
    ) = withContext(Dispatchers.IO) {
        assignments.assignContact(contactKey, type, reference, label)
    }

    override fun findCharacter(
        reference: CharacterReference,
        role: CharacterAssignmentTarget,
        type: CharacterType
    ): InstalledPackCharacter? {
        return repository.find(reference, role, type)
    }

    override suspend fun getPlayerCharacter(type: CharacterType): CharacterReference? = withContext(Dispatchers.IO) {
        assignments.player(type)
    }

    override suspend fun setPlayerCharacter(
        type: CharacterType,
        reference: CharacterReference?
    ) = withContext(Dispatchers.IO) {
        assignments.setPlayer(type, reference)
    }

    private fun readContactNumbers(context: Context, contactId: Int): List<String> {
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

    private fun contactExists(context: Context, contact: SelectedContact): Boolean {
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
            true
        }
    }
}
