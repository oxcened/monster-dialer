package dev.alenajam.monsterdialer.data.characters

import dev.alenajam.opendialer.data.contacts.ContactsRepository
import dev.alenajam.monsterdialer.packs.CharacterAssignmentStore
import dev.alenajam.opendialer.data.contacts.DialerContactSummary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

interface ContactSelectionRepository {
    suspend fun getSelectedContact(): MonsterContact?
    suspend fun setSelectedContact(selectedContact: DialerContactSummary)
    suspend fun clearSelectedContact()
}

@Singleton
class ContactSelectionRepositoryImpl @Inject constructor(
    private val assignments: CharacterAssignmentStore,
    private val contactsRepository: ContactsRepository
) : ContactSelectionRepository {

    override suspend fun getSelectedContact(): MonsterContact? = withContext(Dispatchers.IO) {
        val restored = assignments.selectedContact()
        val exists = restored == null || contactsRepository.contactExists(restored.contactId, restored.contactKeys)
        if (!exists) {
            assignments.clearSelectedContact()
            null
        } else {
            restored?.let { MonsterContact(it.label, it.contactKeys) }
        }
    }

    override suspend fun setSelectedContact(selectedContact: DialerContactSummary) = withContext(Dispatchers.IO) {
        val numbers = contactsRepository.getContactNumbers(selectedContact.id)
        assignments.setSelectedContact(
            label = selectedContact.name,
            contactKeys = numbers,
            contactId = selectedContact.id
        )
    }

    override suspend fun clearSelectedContact() = withContext(Dispatchers.IO) {
        assignments.clearSelectedContact()
    }
}
