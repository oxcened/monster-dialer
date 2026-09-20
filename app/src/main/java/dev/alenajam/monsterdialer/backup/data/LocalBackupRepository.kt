package dev.alenajam.monsterdialer.backup.data

import android.app.Application
import android.net.Uri
import dev.alenajam.monsterdialer.packs.di.CharacterPacksDir
import dev.alenajam.monsterdialer.packs.data.CharacterPackCatalog
import dev.alenajam.monsterdialer.characters.data.PlayerProfileStatsStore
import dev.alenajam.monsterdialer.characters.data.VariantUnlockStore
import dev.alenajam.monsterdialer.battle.data.BattleJournalStore
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** Creates and restores portable copies of the app's character collection and local progress. */
@Singleton
class LocalBackupRepository @Inject constructor(
    private val application: Application,
    @CharacterPacksDir private val storageRoot: File,
    private val catalog: CharacterPackCatalog,
    private val profileStats: PlayerProfileStatsStore,
    private val unlocks: VariantUnlockStore,
    private val journal: BattleJournalStore,
) {
    suspend fun export(destination: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            application.contentResolver.openOutputStream(destination)?.use { output ->
                ZipOutputStream(output.buffered()).use { zip ->
                    zip.putNextEntry(ZipEntry(ManifestPath))
                    zip.write(json.encodeToString(LocalBackupManifest()).encodeToByteArray())
                    zip.closeEntry()
                    storageRoot.takeIf(File::isDirectory)?.walkTopDown()
                        ?.filter(File::isFile)
                        ?.forEach { file ->
                            val path = file.relativeTo(storageRoot).invariantSeparatorsPath
                            zip.putNextEntry(ZipEntry("$DataDirectory/$path"))
                            file.inputStream().use { it.copyTo(zip) }
                            zip.closeEntry()
                        }
                }
            } ?: error("Could not open the backup destination")
            Unit
        }
    }

    /** Restores a validated archive, replacing only MonsterDialer's collection and progress data. */
    suspend fun import(source: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val staging = File.createTempFile("monster-backup-", "", application.cacheDir).apply {
                delete()
                mkdirs()
            }
            try {
                application.contentResolver.openInputStream(source)?.use { input -> unpack(input, staging) }
                    ?: error("Could not open the selected backup")
                require(File(staging, ManifestPath).isFile) { "This is not a MonsterDialer backup" }
                json.decodeFromString<LocalBackupManifest>(File(staging, ManifestPath).readText()).also {
                    require(it.formatVersion == FormatVersion) { "This backup is from an unsupported version" }
                }
                val restoredData = File(staging, DataDirectory)
                require(restoredData.isDirectory) { "The backup does not contain any data" }
                val previous = File(storageRoot.parentFile, ".character-packs-previous")
                previous.deleteRecursively()
                if (storageRoot.exists() && !storageRoot.renameTo(previous)) error("Could not prepare local data for restore")
                if (!restoredData.renameTo(storageRoot)) {
                    previous.renameTo(storageRoot)
                    error("Could not restore the backup")
                }
                previous.deleteRecursively()
                catalog.reload()
                profileStats.reload()
                unlocks.reload()
                journal.reload()
            } finally {
                staging.deleteRecursively()
            }
            Unit
        }
    }

    private fun unpack(input: java.io.InputStream, destination: File) {
        var manifestFound = false
        var totalBytes = 0L
        var entryCount = 0
        ZipInputStream(input.buffered()).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                val name = entry.name
                require(!entry.isDirectory && name.isSafeBackupPath()) { "The backup contains an invalid file path" }
                require(name == ManifestPath || name.startsWith("$DataDirectory/")) { "The backup contains an unsupported file" }
                require(++entryCount <= MaxEntryCount) { "The backup contains too many files" }
                val target = File(destination, name)
                target.parentFile?.mkdirs()
                target.outputStream().use { output ->
                    val buffer = ByteArray(BufferSize)
                    while (true) {
                        val count = zip.read(buffer)
                        if (count < 0) break
                        totalBytes += count
                        require(totalBytes <= MaxUncompressedBytes) { "The backup is too large" }
                        output.write(buffer, 0, count)
                    }
                }
                if (name == ManifestPath) manifestFound = true
                zip.closeEntry()
            }
        }
        require(manifestFound) { "This is not a MonsterDialer backup" }
    }

    private fun String.isSafeBackupPath() = !startsWith('/') && !contains("\\") && split('/').none { it == ".." || it.isBlank() }

    @Serializable
    private data class LocalBackupManifest(val formatVersion: Int = FormatVersion)

    private companion object {
        const val FormatVersion = 1
        const val ManifestPath = "backup.json"
        const val DataDirectory = "data"
        const val BufferSize = 8 * 1024
        const val MaxEntryCount = 4_096
        const val MaxUncompressedBytes = 128L * 1024 * 1024
        val json = Json { ignoreUnknownKeys = false; explicitNulls = false }
    }
}
