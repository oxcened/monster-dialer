package dev.alenajam.monsterdialer.characters.data

import dev.alenajam.monsterdialer.packs.di.CharacterPacksDir
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** Persists player-profile statistics that cannot be derived from current character data. */
@Singleton
class PlayerProfileStatsStore @Inject constructor(
    @CharacterPacksDir private val storageRoot: File,
) {
    private val file = File(storageRoot, "player-profile-stats.json")
    private val json = Json { ignoreUnknownKeys = false; explicitNulls = false }
    private val mutableCallsBattled = MutableStateFlow(read().callsBattled)

    val callsBattled: StateFlow<Int> = mutableCallsBattled.asStateFlow()

    @Synchronized
    fun recordBattle() {
        val updated = mutableCallsBattled.value + 1
        file.parentFile?.mkdirs()
        file.writeText(json.encodeToString(PlayerProfileStatsDocument(callsBattled = updated)))
        mutableCallsBattled.value = updated
    }

    private fun read(): PlayerProfileStatsDocument = runCatching {
        json.decodeFromString<PlayerProfileStatsDocument>(file.readText())
    }.getOrDefault(PlayerProfileStatsDocument())
}

@Serializable
private data class PlayerProfileStatsDocument(
    val callsBattled: Int = 0,
)
