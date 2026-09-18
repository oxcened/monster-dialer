package dev.alenajam.monsterdialer.calls.ui

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import android.telephony.PhoneNumberUtils
import dev.alenajam.monsterdialer.battle.data.BattleJournalEntry
import dev.alenajam.monsterdialer.battle.data.BattleJournalStore
import dev.alenajam.monsterdialer.battle.data.BattleJournalSprite
import dev.alenajam.monsterdialer.characters.data.BuiltInCharacters
import dev.alenajam.monsterdialer.characters.data.CharacterAssignmentRepository
import dev.alenajam.monsterdialer.characters.data.CharactersRepository
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
import kotlinx.coroutines.withContext

data class MonsterCallLogArtwork(
    val file: File? = null,
    val builtInResource: Int? = null,
)

@HiltViewModel
class MonsterCallLogViewModel @Inject constructor(
    private val assignments: CharacterAssignmentRepository,
    private val characters: CharactersRepository,
    private val journal: BattleJournalStore,
    private val contactsRepository: ContactsRepository,
) : ViewModel() {
    private val _artworkByCallId = MutableStateFlow<Map<Int, MonsterCallLogArtwork>>(emptyMap())
    val artworkByCallId: StateFlow<Map<Int, MonsterCallLogArtwork>> = _artworkByCallId
    private val _artworkByContactNumber = MutableStateFlow<Map<String, MonsterCallLogArtwork>>(emptyMap())
    val artworkByContactNumber: StateFlow<Map<String, MonsterCallLogArtwork>> = _artworkByContactNumber
    private val _artworkByContactId = MutableStateFlow<Map<Int, MonsterCallLogArtwork>>(emptyMap())
    val artworkByContactId: StateFlow<Map<Int, MonsterCallLogArtwork>> = _artworkByContactId
    val journalEntries: StateFlow<List<BattleJournalEntry>> = journal.entries
    val assignmentVersion: StateFlow<Long> = assignments.assignmentVersion

    suspend fun refreshContacts(contacts: List<DialerContactSummary>) {
        _artworkByContactId.value = withContext(Dispatchers.IO) {
            buildMap {
                for (contact in contacts) {
                var artwork: MonsterCallLogArtwork? = null
                for (number in contactsRepository.getContactNumbers(contact.id)) {
                    val reference = assignments
                        .getContactCharacterSelection(number, CharacterType.Monster)
                        .character
                    artwork = reference?.let(::artworkFor)
                    if (artwork != null) break
                }
                val resolvedArtwork = artwork ?: MonsterCallLogArtwork(
                    builtInResource = BuiltInCharacters.anonymousMonster.enemyArtwork.resource,
                )
                    put(contact.id, resolvedArtwork)
                }
            }
        }
    }

    suspend fun refresh(calls: List<DialerCall>, favoriteNumbers: Set<String> = emptySet()) {
        val artwork = withContext(Dispatchers.IO) {
            val journalIndex = JournalIndex(journal.entries.value)
            calls.mapNotNull { call ->
                val number = call.contactInfo.number?.takeIf(String::isNotBlank)
                val journalArtwork = journalArtworkFor(call, number, journalIndex)
                val fallbackArtwork = number?.let {
                    val reference = assignments
                        .getContactCharacterSelection(it, CharacterType.Monster)
                        .character
                    reference?.let(::artworkFor)
                } ?: MonsterCallLogArtwork(builtInResource = BuiltInCharacters.anonymousMonster.enemyArtwork.resource)
                call.id to (journalArtwork ?: fallbackArtwork)
            }.toMap()
        }
        _artworkByCallId.value = artwork

        _artworkByContactNumber.value = withContext(Dispatchers.IO) {
            favoriteNumbers.mapNotNull { number ->
                val normalizedNumber = PhoneNumberUtils.normalizeNumber(number)
                val latestCall = calls
                    .filter { PhoneNumberUtils.normalizeNumber(it.contactInfo.number.orEmpty()) == normalizedNumber }
                    .maxByOrNull(DialerCall::date)
                val callArtwork = latestCall?.let { artwork[it.id] }
                val assignedArtwork = assignments
                    .getContactCharacterSelection(number, CharacterType.Monster)
                    .character
                    ?.let(::artworkFor)
                val resolvedArtwork = callArtwork
                    ?: assignedArtwork
                    ?: MonsterCallLogArtwork(builtInResource = BuiltInCharacters.anonymousMonster.enemyArtwork.resource)
                number to resolvedArtwork
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
        if (reference == BuiltInCharacters.defaultMonsterReference) {
            return MonsterCallLogArtwork(
                builtInResource = BuiltInCharacters.monster.character.contactArtwork.resource,
            )
        }
        val installed = characters.findCharacter(
            reference = reference,
            role = CharacterAssignmentTarget.Contact,
            type = CharacterType.Monster,
        ) ?: return null
        val variant = installed.character.variant(reference.variantId) ?: return null
        val image = variant.frontImage ?: variant.backImage ?: return null
        return MonsterCallLogArtwork(file = installed.imageFile(image))
    }

    private companion object {
        const val JournalMatchWindowMillis = 15 * 60 * 1_000L
    }
}
