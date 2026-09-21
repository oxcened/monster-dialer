package dev.alenajam.monsterdialer.characters.data

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** Records the player's explicit choice to back up unlockable character variants privately. */
@Singleton
class VariantBackupSettingsStore @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val file = File(context.filesDir, "variant-backup/settings.json")
    private val json = Json { ignoreUnknownKeys = false; explicitNulls = false }

    @Synchronized
    fun isEnabled(): Boolean = read().enabled

    @Synchronized
    fun enable() {
        file.parentFile?.mkdirs()
        file.writeText(json.encodeToString(VariantBackupSettings(enabled = true)))
    }

    @Synchronized
    fun disable() {
        file.parentFile?.mkdirs()
        file.writeText(json.encodeToString(VariantBackupSettings(enabled = false)))
    }

    private fun read(): VariantBackupSettings = runCatching {
        json.decodeFromString<VariantBackupSettings>(file.readText())
    }.getOrDefault(VariantBackupSettings())
}

@Serializable
private data class VariantBackupSettings(
    val enabled: Boolean = false,
)
