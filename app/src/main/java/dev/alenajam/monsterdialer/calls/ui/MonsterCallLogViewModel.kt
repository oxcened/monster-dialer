package dev.alenajam.monsterdialer.calls.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import android.telephony.PhoneNumberUtils
import dev.alenajam.monsterdialer.battle.data.BattleJournalEntry
import dev.alenajam.monsterdialer.battle.data.BattleJournalStore
import dev.alenajam.monsterdialer.battle.data.BattleJournalSprite
import dev.alenajam.monsterdialer.characters.data.BuiltInCharacters
import dev.alenajam.monsterdialer.characters.data.CharacterAssignmentRepository
import dev.alenajam.monsterdialer.characters.data.CharactersRepository
import dev.alenajam.monsterdialer.characters.data.ContactArtworkPreferences
import dev.alenajam.monsterdialer.characters.data.ContactArtworkPriority
import dev.alenajam.monsterdialer.packs.data.CharacterAssignmentTarget
import dev.alenajam.monsterdialer.packs.data.CharacterReference
import dev.alenajam.monsterdialer.packs.data.CharacterType
import dev.alenajam.opendialer.data.calls.DialerCall
import dev.alenajam.opendialer.data.contacts.ContactsRepository
import dev.alenajam.opendialer.data.contacts.DialerContactSummary
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@HiltViewModel
class MonsterCallLogViewModel @Inject constructor(
    private val assignments: CharacterAssignmentRepository,
    private val characters: CharactersRepository,
    private val journal: BattleJournalStore,
    private val contactsRepository: ContactsRepository,
    private val artworkPreferences: ContactArtworkPreferences,
) : ViewModel() {
    private val _artworkByCallId = MutableStateFlow<Map<Int, MonsterCallLogArtwork>>(emptyMap())
    val artworkByCallId: StateFlow<Map<Int, MonsterCallLogArtwork>> = _artworkByCallId
    private val _artworkByContactNumber = MutableStateFlow<Map<String, MonsterCallLogArtwork>>(emptyMap())
    val artworkByContactNumber: StateFlow<Map<String, MonsterCallLogArtwork>> = _artworkByContactNumber
    private val _artworkByContactId = MutableStateFlow<Map<Int, MonsterCallLogArtwork>>(emptyMap())
    val artworkByContactId: StateFlow<Map<Int, MonsterCallLogArtwork>> = _artworkByContactId
    private val contactsForArtwork = MutableStateFlow<List<DialerContactSummary>>(emptyList())
    val journalEntries: StateFlow<List<BattleJournalEntry>> = journal.entries
    val assignmentVersion: StateFlow<Long> = assignments.assignmentVersion
    val artworkPriority: StateFlow<ContactArtworkPriority> = artworkPreferences.priority

    init {
        viewModelScope.launch {
            combine(contactsForArtwork, assignments.assignmentVersion, artworkPriority) { contacts, _, priority ->
                contacts to priority
            }
                .collectLatest { (contacts, priority) ->
                    _artworkByContactId.value = withContext(Dispatchers.IO) {
                        resolveContactArtwork(contacts, priority)
                    }
                }
        }
    }

    fun setContactsForArtwork(contacts: List<DialerContactSummary>) {
        contactsForArtwork.value = contacts
    }

    private suspend fun resolveContactArtwork(
        contacts: List<DialerContactSummary>,
        priority: ContactArtworkPriority,
    ): Map<Int, MonsterCallLogArtwork> = buildMap {
        for (contact in contacts) {
            var artwork: MonsterCallLogArtwork? = null
            for (number in contactsRepository.getContactNumbers(contact.id)) {
                artwork = assignedArtworkFor(number, priority)
                if (artwork != null) break
            }
            artwork?.let { put(contact.id, it) }
        }
    }

    suspend fun refresh(
        calls: List<DialerCall>,
        favoriteNumbers: Set<String> = emptySet(),
        priority: ContactArtworkPriority = artworkPriority.value,
    ) {
        val artwork = withContext(Dispatchers.IO) {
            val journalIndex = JournalIndex(journal.entries.value)
            calls.mapNotNull { call ->
                val number = call.contactInfo.number?.takeIf(String::isNotBlank)
                val journalArtwork = journalArtworkFor(call, number, journalIndex)
                val resolvedArtwork = if (call.isAnonymous()) {
                    MonsterCallLogArtwork.anonymous()
                } else {
                    if (number == null) null else assignedArtworkFor(number, priority)
                }
                val artworkForCall = if (call.isAnonymous()) {
                    resolvedArtwork
                } else {
                    resolvedArtwork ?: journalArtwork
                }
                artworkForCall?.let { call.id to it }
            }.toMap()
        }
        _artworkByCallId.value = artwork

        _artworkByContactNumber.value = withContext(Dispatchers.IO) {
            favoriteNumbers.mapNotNull { number ->
                val assignedArtwork = assignedArtworkFor(number, priority)
                assignedArtwork?.let { number to it }
            }.toMap()
        }
    }

    private fun journalArtworkFor(
        call: DialerCall,
        number: String?,
        journalIndex: JournalIndex,
    ): MonsterCallLogArtwork? {
        val entry = journalIndex.byCallId[call.date.time.toString()]
            ?: journalIndex.byNumber[PhoneNumberUtils.normalizeNumber(number.orEmpty())].orEmpty()
            .asSequence()
            .filter { kotlin.math.abs(it.timestampMillis - call.date.time) <= JournalMatchWindowMillis }
            .minByOrNull { kotlin.math.abs(it.timestampMillis - call.date.time) }
        return entry?.opponentSprite?.let(::artworkFor)
    }

    private class JournalIndex(entries: List<BattleJournalEntry>) {
        val byCallId = entries.mapNotNull { entry ->
            entry.callId?.let { it to entry }
        }.toMap()
        val byNumber = entries
            .filter { !it.callerNumber.isNullOrBlank() }
            .groupBy { PhoneNumberUtils.normalizeNumber(it.callerNumber) }
    }

    private fun artworkFor(sprite: BattleJournalSprite): MonsterCallLogArtwork? {
        sprite.drawableResource?.let { return MonsterCallLogArtwork(builtInResource = it) }
        val path = sprite.journalSnapshotPath ?: sprite.localFilePath
        return path?.let(::File)?.takeIf(File::isFile)?.let(::MonsterCallLogArtwork)
    }

    private fun artworkFor(reference: CharacterReference): MonsterCallLogArtwork? {
        return artworkFor(reference, CharacterType.Monster)
    }

    private suspend fun assignedArtworkFor(
        contactKey: String,
        priority: ContactArtworkPriority,
    ): MonsterCallLogArtwork? {
        val types = when (priority) {
            ContactArtworkPriority.TRAINER -> listOf(CharacterType.Trainer, CharacterType.Monster)
            ContactArtworkPriority.MONSTER -> listOf(CharacterType.Monster, CharacterType.Trainer)
        }
        for (type in types) {
            val reference = assignments.getContactCharacterSelection(contactKey, type).character
            val artwork = reference?.let { artworkFor(it, type) }
            if (artwork != null) return artwork
        }
        return null
    }

    private fun artworkFor(reference: CharacterReference, type: CharacterType): MonsterCallLogArtwork? {
        if (reference == BuiltInCharacters.defaultMonsterReference && type == CharacterType.Monster) {
            return MonsterCallLogArtwork(
                builtInResource = BuiltInCharacters.monster.character.contactArtwork.resource,
            )
        }
        if (reference == BuiltInCharacters.defaultTrainerReference && type == CharacterType.Trainer) {
            return MonsterCallLogArtwork(
                builtInResource = BuiltInCharacters.trainer.contactArtwork.resource,
            )
        }
        val installed = characters.findCharacter(
            reference = reference,
            role = CharacterAssignmentTarget.Contact,
            type = type,
        ) ?: return null
        val variant = installed.character.variant(reference.variantId) ?: return null
        val image = variant.frontImage ?: variant.backImage ?: return null
        return MonsterCallLogArtwork(file = installed.imageFile(image))
    }

    private companion object {
        const val JournalMatchWindowMillis = 15 * 60 * 1_000L
    }
}
