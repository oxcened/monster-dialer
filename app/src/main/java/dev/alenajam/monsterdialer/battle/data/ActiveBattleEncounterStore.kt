package dev.alenajam.monsterdialer.battle.data

import dev.alenajam.monsterdialer.packs.data.CharacterReference
import dev.alenajam.monsterdialer.packs.di.CharacterPacksDir
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** Retains the current call's outcome if the app process needs to be recreated mid-call. */
@Singleton
class ActiveBattleEncounterStore @Inject constructor(
    @CharacterPacksDir private val storageRoot: File,
) {
    private val file = File(storageRoot, "active-battle-encounter.json")
    private val json = Json { ignoreUnknownKeys = false; explicitNulls = false }

    @Synchronized
    internal fun restore(call: ActiveCallKey): ActiveBattleEncounter? =
        read().encounters.firstOrNull { it.call == call }

    @Synchronized
    fun save(call: ActiveCallKey, radiantReference: CharacterReference?) {
        val existing = read().encounters.filterNot { it.call.callId == call.callId }
        file.parentFile?.mkdirs()
        file.writeText(
            json.encodeToString(
                ActiveBattleEncounterDocument(existing + ActiveBattleEncounter(call, radiantReference)),
            ),
        )
    }

    @Synchronized
    fun clear() {
        file.delete()
    }

    private fun read(): ActiveBattleEncounterDocument = runCatching {
        json.decodeFromString<ActiveBattleEncounterDocument>(file.readText())
    }.getOrDefault(ActiveBattleEncounterDocument())
}

@Serializable
data class ActiveCallKey(
    val callId: String,
    val contactKey: String,
    val callerName: String,
    val isAnonymous: Boolean,
)

@Serializable
private data class ActiveBattleEncounterDocument(
    val encounters: List<ActiveBattleEncounter> = emptyList(),
)

@Serializable
internal data class ActiveBattleEncounter(
    val call: ActiveCallKey,
    val radiantReference: CharacterReference? = null,
)
