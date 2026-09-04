package dev.alenajam.monsterdialer.characters.data

import dev.alenajam.monsterdialer.packs.data.CharacterPackValidationException
import dev.alenajam.monsterdialer.packs.data.CharacterReference
import dev.alenajam.monsterdialer.packs.data.CharacterType
import java.io.File
import java.util.UUID
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

internal const val MaxPlayerMonsterBenchSize = 5

/** The active monster plus its bench: the full team shown in the roster, active first. */
internal const val MaxPlayerMonsterTeamSize = MaxPlayerMonsterBenchSize + 1

@Serializable
private data class CharacterAssignmentsDocument(
    /** Legacy v1 monster assignment. */
    val player: CharacterReference? = null,
    /** Legacy v1 monster assignments. */
    val contacts: Map<String, CharacterReference> = emptyMap(),
    val playerByType: Map<CharacterType, CharacterReference> = emptyMap(),
    val playerMonsterRoster: List<CharacterReference> = emptyList(),
    val activePlayerMonster: CharacterReference? = null,
    val contactsByType: Map<String, Map<CharacterType, CharacterReference>> = emptyMap(),
    val contactModes: Map<String, Map<CharacterType, ContactCharacterMode>> = emptyMap(),
    val contactLabels: Map<String, String> = emptyMap(),
    val selectedContact: StoredSelectedContact? = null
)

@Serializable
enum class ContactCharacterMode {
    Default,
    Random,
}

data class ContactCharacterSelection(
    val character: CharacterReference?,
    val mode: ContactCharacterMode,
)

@Serializable
private data class StoredSelectedContact(
    val label: String,
    val contactKeys: List<String>,
    val contactId: Int? = null,
    val photoUri: String? = null
)

data class ContactCharacterAssignment(
    val contactKey: String,
    val label: String,
    val type: CharacterType,
    val character: CharacterReference
)

data class SelectedContact(
    val label: String,
    val contactKeys: List<String>,
    val contactId: Int?,
    val photoUri: String?
)

/** Persists the user's selected player and contact mappings without putting personal data in packs. */
class CharacterAssignmentStore(
    private val storageRoot: File,
    private val json: Json = Json { ignoreUnknownKeys = false; explicitNulls = false }
) {
    @Synchronized
    fun player(type: CharacterType): CharacterReference? {
        val document = read()
        if (type == CharacterType.Monster) {
            return activeMonster(document)
        }
        return document.playerByType[type] ?: document.player.takeIf { type == CharacterType.Monster }
    }

    fun player(): CharacterReference? = player(CharacterType.Monster)

    @Synchronized
    fun setPlayer(type: CharacterType, character: CharacterReference?) {
        character?.validate()
        val document = read()
        if (type == CharacterType.Monster) {
            val active = activeMonster(document)
            val shouldReplaceInitialDefault =
                active == BuiltInCharacters.defaultMonsterReference && benchRoster(document).isEmpty()
            val roster = character?.let { selected ->
                when {
                    selected == active || shouldReplaceInitialDefault -> benchRoster(document)
                    else -> benchRoster(document).filterNot { it == selected }.plus(selected).take(MaxPlayerMonsterBenchSize)
                }
            }.orEmpty()
            write(document.copy(
                player = null,
                playerByType = document.playerByType - CharacterType.Monster,
                playerMonsterRoster = roster,
                activePlayerMonster = character?.let { selected ->
                    if (shouldReplaceInitialDefault) selected else active ?: selected
                } ?: BuiltInCharacters.defaultMonsterReference,
            ))
            return
        }
        val updated = document.playerByType.toMutableMap().apply {
            if (character == null) remove(type) else put(type, character)
        }
        write(document.copy(
            player = if (type == CharacterType.Monster) null else document.player,
            playerByType = updated
        ))
    }

    fun setPlayer(character: CharacterReference?) = setPlayer(CharacterType.Monster, character)

    /** The full team, active monster first followed by the bench in order. */
    @Synchronized
    fun playerMonsterRoster(): List<CharacterReference> {
        val document = read()
        return listOfNotNull(activeMonster(document)) + benchRoster(document)
    }

    @Synchronized
    fun addPlayerMonsterToRoster(character: CharacterReference) {
        character.validate()
        val document = read()
        val team = listOfNotNull(activeMonster(document)) + benchRoster(document)
        if (character in team) return
        require(team.size < MaxPlayerMonsterTeamSize) { "Roster is full" }
        setPlayerMonsterRoster(team + character)
    }

    /**
     * Replaces the full team with [roster]: the first entry becomes the active monster and the
     * rest become the bench, in order. Used both to persist a drag-and-drop reorder (whichever
     * monster ends up at index 0 becomes active) and to append newly added monsters.
     */
    @Synchronized
    fun setPlayerMonsterRoster(roster: List<CharacterReference>) {
        require(roster.size <= MaxPlayerMonsterTeamSize) { "Roster is too large" }
        require(roster.distinct().size == roster.size) { "Roster contains duplicate monsters" }
        roster.forEach { reference -> reference.validate() }
        val document = read()
        write(document.copy(
            player = null,
            playerByType = document.playerByType - CharacterType.Monster,
            playerMonsterRoster = roster.drop(1),
            activePlayerMonster = roster.firstOrNull()
                ?: BuiltInCharacters.defaultMonsterReference,
        ))
    }

    @Synchronized
    fun removePlayerMonsterFromRoster(character: CharacterReference) {
        setPlayerMonsterRoster(playerMonsterRoster().filterNot { it == character })
    }

    @Synchronized
    fun replacePlayerMonsterInRoster(index: Int, character: CharacterReference) {
        character.validate()
        val roster = playerMonsterRoster().toMutableList()
        val current = roster.getOrNull(index)
        if (current == null) {
            if (roster.isEmpty() && index == 0) {
                setPlayerMonsterRoster(listOf(character))
            }
            return
        }
        if (current == character) return

        // Ensure the character is not already in the roster elsewhere
        val existingIndex = roster.indexOfFirst { it.sameCharacterAs(character) }
        if (existingIndex == index) {
            roster[index] = character
        } else if (existingIndex != -1) {
            roster.removeAt(existingIndex)
            // Adjust index if we removed something before it
            val finalIndex = if (existingIndex < index) index - 1 else index
            if (finalIndex in roster.indices) {
                roster[finalIndex] = character
            } else {
                roster.add(character)
            }
        } else {
            roster[index] = character
        }

        setPlayerMonsterRoster(roster)
    }

    @Synchronized
    fun characterForContact(contactKey: String, type: CharacterType): CharacterReference? {
        val document = read()
        val normalizedKey = normalizeContactKeyOrNull(contactKey) ?: return null
        return document.contactsByType[normalizedKey]?.get(type)
            ?: document.contacts[normalizedKey].takeIf { type == CharacterType.Monster }
    }

    @Synchronized
    fun selectionForContact(contactKey: String, type: CharacterType): ContactCharacterSelection {
        val document = read()
        val normalizedKey = normalizeContactKeyOrNull(contactKey)
            ?: return ContactCharacterSelection(null, ContactCharacterMode.Default)
        val character = document.contactsByType[normalizedKey]?.get(type)
            ?: document.contacts[normalizedKey].takeIf { type == CharacterType.Monster }
        return ContactCharacterSelection(
            character = character,
            // Missing modes are intentionally random so existing, uncustomized contacts gain
            // the new default behavior after updating.
            mode = document.contactModes[normalizedKey]?.get(type)
                ?: if (character == null) ContactCharacterMode.Random else ContactCharacterMode.Default,
        )
    }

    fun characterForContact(contactKey: String): CharacterReference? =
        characterForContact(contactKey, CharacterType.Monster)

    @Synchronized
    fun contactAssignments(): List<ContactCharacterAssignment> {
        val document = read()
        val typed = document.contactsByType.flatMap { (contactKey, assignments) ->
            assignments.map { (type, character) ->
                ContactCharacterAssignment(
                    contactKey = contactKey,
                    label = document.contactLabels[contactKey] ?: contactKey,
                    type = type,
                    character = character
                )
            }
        }.toMutableList()
        document.contacts.forEach { (contactKey, character) ->
            if (typed.none { it.contactKey == contactKey && it.type == CharacterType.Monster }) {
                typed += ContactCharacterAssignment(
                    contactKey = contactKey,
                    label = document.contactLabels[contactKey] ?: contactKey,
                    type = CharacterType.Monster,
                    character = character
                )
            }
        }
        return typed.sortedWith(compareBy({ it.label.lowercase() }, { it.type.name }))
    }

    @Synchronized
    fun assignedContactCount(): Int {
        val document = read()
        return (document.contacts.keys + document.contactsByType.keys + document.contactModes.keys)
            .distinct()
            .size
    }

    @Synchronized
    fun selectedContact(): SelectedContact? = read().selectedContact?.let { selected ->
        SelectedContact(selected.label, selected.contactKeys, selected.contactId, selected.photoUri)
    }

    @Synchronized
    fun setSelectedContact(
        label: String,
        contactKeys: List<String>,
        contactId: Int? = null,
        photoUri: String? = null
    ): Boolean {
        val normalizedKeys = contactKeys.mapNotNull(::normalizeContactKeyOrNull).distinct()
        if (normalizedKeys.isEmpty()) return false
        val selected = StoredSelectedContact(
            label = label.trim().ifBlank { normalizedKeys.first() }.take(MaxContactLabelLength),
            contactKeys = normalizedKeys,
            contactId = contactId,
            photoUri = photoUri
        )
        val document = read()
        write(document.copy(selectedContact = selected))
        return true
    }

    @Synchronized
    fun clearSelectedContact() {
        val document = read()
        if (document.selectedContact != null) write(document.copy(selectedContact = null))
    }

    @Synchronized
    fun assignContact(
        contactKey: String,
        type: CharacterType,
        character: CharacterReference?,
        label: String? = null
    ) {
        val normalizedKey = normalizeContactKey(contactKey)
        character?.validate()
        val document = read()
        val assignmentsForContact = document.contactsByType[normalizedKey].orEmpty().toMutableMap().apply {
            if (character == null) remove(type) else put(type, character)
        }
        val updated = document.contactsByType.toMutableMap().apply {
            if (assignmentsForContact.isEmpty()) remove(normalizedKey) else put(normalizedKey, assignmentsForContact)
        }
        val legacyContacts = document.contacts.toMutableMap().apply {
            if (type == CharacterType.Monster) remove(normalizedKey)
        }
        val updatedLabels = document.contactLabels.toMutableMap().apply {
            if (character == null && assignmentsForContact.isEmpty() && normalizedKey !in legacyContacts) {
                remove(normalizedKey)
            } else if (!label.isNullOrBlank()) {
                put(normalizedKey, label.trim().take(MaxContactLabelLength))
            }
        }
        val updatedModes = document.contactModes.toMutableMap().apply {
            val modes = document.contactModes[normalizedKey].orEmpty().toMutableMap().apply {
                if (character == null) put(type, ContactCharacterMode.Default) else remove(type)
            }
            if (modes.isEmpty()) remove(normalizedKey) else put(normalizedKey, modes)
        }
        write(document.copy(
            contacts = legacyContacts,
            contactsByType = updated,
            contactModes = updatedModes,
            contactLabels = updatedLabels
        ))
    }

    @Synchronized
    fun randomizeContact(contactKey: String, type: CharacterType, label: String? = null) {
        val normalizedKey = normalizeContactKey(contactKey)
        val document = read()
        val assignments = document.contactsByType[normalizedKey].orEmpty().toMutableMap().apply { remove(type) }
        val updatedAssignments = document.contactsByType.toMutableMap().apply {
            if (assignments.isEmpty()) remove(normalizedKey) else put(normalizedKey, assignments)
        }
        val updatedModes = document.contactModes.toMutableMap().apply {
            val modes = document.contactModes[normalizedKey].orEmpty().toMutableMap().apply {
                put(type, ContactCharacterMode.Random)
            }
            put(normalizedKey, modes)
        }
        val updatedLabels = document.contactLabels.toMutableMap().apply {
            if (!label.isNullOrBlank()) put(normalizedKey, label.trim().take(MaxContactLabelLength))
        }
        write(document.copy(
            contacts = if (type == CharacterType.Monster) document.contacts - normalizedKey else document.contacts,
            contactsByType = updatedAssignments,
            contactModes = updatedModes,
            contactLabels = updatedLabels,
        ))
    }

    fun assignContact(contactKey: String, character: CharacterReference?, label: String? = null) =
        assignContact(contactKey, CharacterType.Monster, character, label)

    @Synchronized
    fun clearAssignmentsForPack(packId: String) {
        val document = read()
        val updatedPlayerByType = document.playerByType.filterValues { it.packId != packId }
        val updatedRoster = benchRoster(document).filter { it.packId != packId }
        val updatedActiveMonster = activeMonster(document)?.takeIf { it.packId != packId }
            ?: updatedRoster.firstOrNull()
        val updatedBench = updatedRoster.filterNot { it == updatedActiveMonster }
        
        val updatedContactsByType = document.contactsByType.mapValues { (_, assignments) ->
            assignments.filterValues { it.packId != packId }
        }.filterValues { it.isNotEmpty() }

        val legacyContacts = document.contacts.filterValues { it.packId != packId }
        
        val updatedModes = document.contactModes.mapValues { (_, modes) -> modes.toMutableMap() }.toMutableMap()
        document.contactsByType.forEach { (key, assignments) ->
            assignments.filterValues { it.packId == packId }.keys.forEach { type ->
                updatedModes.getOrPut(key) { mutableMapOf() }[type] = ContactCharacterMode.Random
            }
        }
        val cleanedModes = updatedModes.filterValues { it.isNotEmpty() }
        val updatedLabels = document.contactLabels.filterKeys { key ->
            key in updatedContactsByType || key in legacyContacts || key in cleanedModes
        }

        write(document.copy(
            player = if (document.player?.packId == packId) null else document.player,
            contacts = legacyContacts,
            playerByType = updatedPlayerByType,
            playerMonsterRoster = updatedBench,
            activePlayerMonster = updatedActiveMonster,
            contactsByType = updatedContactsByType,
            contactModes = cleanedModes,
            contactLabels = updatedLabels
        ))
    }

    @Synchronized
    fun clearAssignmentsForCharacter(reference: CharacterReference) {
        val document = read()
        val updatedPlayerByType = document.playerByType.filterValues { !it.sameCharacterAs(reference) }
        val updatedRoster = benchRoster(document).filter { !it.sameCharacterAs(reference) }
        val updatedActiveMonster = activeMonster(document)?.takeUnless { it.sameCharacterAs(reference) }
            ?: updatedRoster.firstOrNull()
        val updatedBench = updatedRoster.filterNot { it == updatedActiveMonster }
        
        val updatedContactsByType = document.contactsByType.mapValues { (_, assignments) ->
            assignments.filterValues { !it.sameCharacterAs(reference) }
        }.filterValues { it.isNotEmpty() }

        val legacyContacts = document.contacts.filterValues { !it.sameCharacterAs(reference) }
        
        val updatedModes = document.contactModes.mapValues { (_, modes) -> modes.toMutableMap() }.toMutableMap()
        document.contactsByType.forEach { (key, assignments) ->
            assignments.filterValues { it.sameCharacterAs(reference) }.keys.forEach { type ->
                updatedModes.getOrPut(key) { mutableMapOf() }[type] = ContactCharacterMode.Random
            }
        }
        val cleanedModes = updatedModes.filterValues { it.isNotEmpty() }
        val updatedLabels = document.contactLabels.filterKeys { key ->
            key in updatedContactsByType || key in legacyContacts || key in cleanedModes
        }

        write(document.copy(
            player = if (document.player?.sameCharacterAs(reference) == true) null else document.player,
            contacts = legacyContacts,
            playerByType = updatedPlayerByType,
            playerMonsterRoster = updatedBench,
            activePlayerMonster = updatedActiveMonster,
            contactsByType = updatedContactsByType,
            contactModes = cleanedModes,
            contactLabels = updatedLabels
        ))
    }

    private fun CharacterReference.validate() {
        require(packId.isNotBlank() && packId.length <= MaxIdentifierLength) { "Pack id is invalid" }
        require(characterId.isNotBlank() && characterId.length <= MaxIdentifierLength) { "Character id is invalid" }
    }

    private fun CharacterReference.sameCharacterAs(other: CharacterReference): Boolean =
        packId == other.packId && characterId == other.characterId

    /**
     * Converts the released single-monster assignment and the earlier roster format on read.
     * The bundled monster is always the initial active roster member, even before a user saves
     * any character assignment.
     */
    private fun activeMonster(document: CharacterAssignmentsDocument): CharacterReference =
        document.activePlayerMonster ?: BuiltInCharacters.defaultMonsterReference

    /** The roster holds the team members that are not currently active. */
    private fun benchRoster(document: CharacterAssignmentsDocument): List<CharacterReference> =
        document.playerMonsterRoster
            .filterNot { it == activeMonster(document) }
            .distinct()
            .take(MaxPlayerMonsterBenchSize)

    private fun normalizeContactKey(value: String): String {
        return requireNotNull(normalizeContactKeyOrNull(value)) { "Contact key is invalid" }
    }

    /**
     * Incoming call providers can expose labels such as "Private number" instead of a phone
     * number. Those values cannot have an assignment and must not crash the in-call UI.
     */
    private fun normalizeContactKeyOrNull(value: String): String? {
        val trimmed = value.trim()
        val normalized = buildString {
            trimmed.forEachIndexed { index, character ->
                if (character.isDigit() || (character == '+' && index == 0)) append(character)
            }
        }
        return normalized.takeIf { it.isNotBlank() && it.length <= MaxContactKeyLength }
    }

    private fun read(): CharacterAssignmentsDocument {
        val file = File(storageRoot, FileName)
        val document = if (!file.exists()) {
            CharacterAssignmentsDocument()
        } else try {
            json.decodeFromString<CharacterAssignmentsDocument>(file.readText())
        } catch (exception: Exception) {
            throw CharacterPackValidationException("Character assignments are unreadable: ${exception.message}")
        }
        val referencesNormalized = document.copy(
            player = document.player?.normalizedBuiltInMonsterRosterReference(),
            contacts = document.contacts.mapValues { (_, reference) ->
                reference.normalizedBuiltInMonsterRosterReference()
            },
            playerByType = document.playerByType.mapValues { (_, reference) ->
                reference.normalizedBuiltInMonsterRosterReference()
            },
            playerMonsterRoster = document.playerMonsterRoster
                .map(CharacterReference::normalizedBuiltInMonsterRosterReference)
                .distinct(),
            activePlayerMonster = document.activePlayerMonster?.normalizedBuiltInMonsterRosterReference(),
            contactsByType = document.contactsByType.mapValues { (_, assignments) ->
                assignments.mapValues { (_, reference) -> reference.normalizedBuiltInMonsterRosterReference() }
            },
        )
        val activeMonster = referencesNormalized.activePlayerMonster
            ?: referencesNormalized.playerByType[CharacterType.Monster]
            ?: referencesNormalized.player
            ?: referencesNormalized.playerMonsterRoster.firstOrNull()
            ?: BuiltInCharacters.defaultMonsterReference
        val normalizedDocument = referencesNormalized.copy(
            // Player monsters used to be represented by null or one of these legacy fields.
            // Persist a single roster model instead: an explicit active monster plus its bench.
            player = null,
            playerByType = referencesNormalized.playerByType - CharacterType.Monster,
            playerMonsterRoster = referencesNormalized.playerMonsterRoster
                .filterNot { it == activeMonster }
                .distinct()
                .take(MaxPlayerMonsterBenchSize),
            activePlayerMonster = activeMonster,
        )
        if (normalizedDocument != document) {
            write(normalizedDocument)
        }
        return normalizedDocument
    }

    private fun write(document: CharacterAssignmentsDocument) {
        storageRoot.mkdirs()
        val destination = File(storageRoot, FileName)
        val temporary = File(storageRoot, ".$FileName-${UUID.randomUUID()}")
        val backup = File(storageRoot, ".$FileName-backup-${UUID.randomUUID()}")
        try {
            temporary.writeText(json.encodeToString(document))
            if (destination.exists() && !destination.renameTo(backup)) fail("Could not update character assignments")
            if (!temporary.renameTo(destination)) {
                if (backup.exists()) backup.renameTo(destination)
                fail("Could not update character assignments")
            }
            backup.delete()
        } finally {
            temporary.delete()
        }
    }

    private fun fail(message: String): Nothing = throw CharacterPackValidationException(message)

    private companion object {
        const val FileName = "character-assignments.json"
        const val MaxIdentifierLength = 128
        const val MaxContactKeyLength = 512
        const val MaxContactLabelLength = 120
    }
}
