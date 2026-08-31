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
    fun restore(call: ActiveCallKey): ActiveBattleEncounter? = read()?.takeIf { it.call == call }

    @Synchronized
    fun save(call: ActiveCallKey, radiantReference: CharacterReference?) {
        file.parentFile?.mkdirs()
        file.writeText(json.encodeToString(ActiveBattleEncounter(call, radiantReference)))
    }

    @Synchronized
    fun clear() {
        file.delete()
    }

    private fun read(): ActiveBattleEncounter? = runCatching {
        json.decodeFromString<ActiveBattleEncounter>(file.readText())
    }.getOrNull()
}

@Serializable
data class ActiveCallKey(
    val callId: String,
    val contactKey: String,
    val callerName: String,
    val isAnonymous: Boolean,
)

@Serializable
data class ActiveBattleEncounter(
    val call: ActiveCallKey,
    val radiantReference: CharacterReference? = null,
)
