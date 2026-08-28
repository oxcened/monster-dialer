package dev.alenajam.monsterdialer.data.contacts

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.ContactsContract
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.alenajam.monsterdialer.packs.SelectedContact
import javax.inject.Inject
import javax.inject.Singleton

interface ContactsRepository {
    fun readContactNumbers(contactId: Int): List<String>
    fun contactExists(contact: SelectedContact): Boolean
}

@Singleton
class ContactsRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : ContactsRepository {

    override fun readContactNumbers(contactId: Int): List<String> {
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

    override fun contactExists(contact: SelectedContact): Boolean {
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
