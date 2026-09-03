package dev.alenajam.monsterdialer.battle.data

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import dev.alenajam.monsterdialer.packs.di.CharacterPacksDir
import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** Keeps a compact, local history of battle encounters. */
@Singleton
class BattleJournalStore @Inject constructor(
    @param:CharacterPacksDir private val storageRoot: File,
) {
    private val file = File(storageRoot, "battle-journal.json")
    private val spriteDirectory = File(storageRoot, "battle-journal-sprites")
    private val json = Json { ignoreUnknownKeys = false; explicitNulls = false }
    private val mutableEntries = MutableStateFlow(read())

    val entries: StateFlow<List<BattleJournalEntry>> = mutableEntries.asStateFlow()

    @Synchronized
    fun record(encounter: BattleEncounter, isRadiantDiscovery: Boolean) {
        val entry = BattleJournalEntry(
            id = UUID.randomUUID().toString(),
            timestampMillis = System.currentTimeMillis(),
            encounterType = encounter.type,
            playerMonsterName = encounter.player.name,
            playerSprite = (encounter.player.backSprite ?: encounter.player.frontSprite).toJournalSprite(),
            opponentMonsterName = encounter.enemy?.name,
            opponentSprite = encounter.enemy?.frontSprite?.toJournalSprite(isRadiantDiscovery),
            opponentTrainerName = encounter.enemyTrainerName,
            isRadiantDiscovery = isRadiantDiscovery,
        )
        val updated = (listOf(entry) + mutableEntries.value)
            .let(::retainEntries)
        write(updated)
        removeUnreferencedSnapshots(updated)
        mutableEntries.value = updated
    }

    @Synchronized
    fun clear() {
        file.delete()
        spriteDirectory.listFiles()?.forEach(File::delete)
        spriteDirectory.delete()
        mutableEntries.value = emptyList()
    }

    private fun retainEntries(entries: List<BattleJournalEntry>): List<BattleJournalEntry> {
        var regularEntries = 0
        return entries.filter { entry ->
            entry.isRadiantDiscovery || regularEntries++ < MaxRegularEntries
        }
    }

    private fun read(): List<BattleJournalEntry> = runCatching {
        json.decodeFromString<BattleJournalDocument>(file.readText()).entries
            .let(::retainEntries)
    }.getOrDefault(emptyList())

    private fun write(entries: List<BattleJournalEntry>) {
        file.parentFile?.mkdirs()
        file.writeText(json.encodeToString(BattleJournalDocument(entries)))
    }

    private fun BattleVisualAsset.toJournalSprite(isRadiant: Boolean = false): BattleJournalSprite = when (this) {
        is BattleVisualAsset.LocalFile -> {
            val snapshotPath = archiveSprite(path, isRadiant)
            BattleJournalSprite(
                journalSnapshotPath = snapshotPath,
                localFilePath = path,
            )
        }
        is BattleVisualAsset.AppDrawable -> BattleJournalSprite(drawableResource = resource)
        is BattleVisualAsset.VectorDrawable -> BattleJournalSprite(drawableResource = resource)
    }

    private fun archiveSprite(path: String, isRadiant: Boolean): String? = runCatching {
        val source = File(path).takeIf(File::isFile) ?: return null
        val suffix = if (isRadiant) "-radiant" else ""
        val destination = File(spriteDirectory, "${source.sha256()}$suffix.png")
        if (!destination.isFile) {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(source.path, bounds)
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
            val bitmap = BitmapFactory.decodeFile(
                source.path,
                BitmapFactory.Options().apply { inSampleSize = sampleSizeFor(bounds) },
            ) ?: return null
            val scaledBitmap = bitmap.scaleToJournalThumbnail(isRadiant)
            spriteDirectory.mkdirs()
            destination.outputStream().buffered().use { output ->
                check(scaledBitmap.compress(Bitmap.CompressFormat.PNG, 100, output))
            }
            if (scaledBitmap !== bitmap) scaledBitmap.recycle()
            bitmap.recycle()
        }
        destination.path
    }.getOrNull()

    private fun sampleSizeFor(bounds: BitmapFactory.Options): Int {
        var sampleSize = 1
        while (bounds.outWidth / sampleSize > DecodeSize || bounds.outHeight / sampleSize > DecodeSize) {
            sampleSize *= 2
        }
        return sampleSize
    }

    private fun Bitmap.scaleToJournalThumbnail(isRadiant: Boolean): Bitmap {
        val targetSize = if (isRadiant) RadiantThumbnailSize else JournalThumbnailSize
        if (width <= targetSize && height <= targetSize) return this
        val scale = minOf(targetSize.toFloat() / width, targetSize.toFloat() / height)
        return Bitmap.createScaledBitmap(this, (width * scale).toInt(), (height * scale).toInt(), false)
    }

    private fun File.sha256(): String = FileInputStream(this).use { input ->
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(8_192)
        while (true) {
            val count = input.read(buffer)
            if (count < 0) break
            digest.update(buffer, 0, count)
        }
        digest.digest().joinToString(separator = "") { byte -> "%02x".format(byte.toInt() and 0xff) }
    }

    private fun removeUnreferencedSnapshots(entries: List<BattleJournalEntry>) {
        val referencedPaths = entries.flatMap { entry ->
            listOfNotNull(
                entry.playerSprite?.journalSnapshotPath,
                entry.opponentSprite?.journalSnapshotPath,
            )
        }.toSet()
        spriteDirectory.listFiles()?.filter(File::isFile)?.forEach { snapshot ->
            if (snapshot.path !in referencedPaths) snapshot.delete()
        }
    }

    private companion object {
        const val MaxRegularEntries = 5_000
        const val JournalThumbnailSize = 96
        const val RadiantThumbnailSize = 768
        const val DecodeSize = RadiantThumbnailSize * 2
    }
}

@Serializable
data class BattleJournalEntry(
    val id: String,
    val timestampMillis: Long,
    val encounterType: EncounterType,
    val playerMonsterName: String,
    val playerSprite: BattleJournalSprite? = null,
    val opponentMonsterName: String? = null,
    val opponentSprite: BattleJournalSprite? = null,
    val opponentTrainerName: String? = null,
    /** Discovery entries are retained permanently; routine encounters are capped. */
    val isRadiantDiscovery: Boolean = false,
)

/** A lightweight reference to artwork that is already stored by the app or its packs. */
@Serializable
data class BattleJournalSprite(
    /** A journal-owned thumbnail that survives removal of the original character pack. */
    val journalSnapshotPath: String? = null,
    val localFilePath: String? = null,
    val drawableResource: Int? = null,
)

@Serializable
private data class BattleJournalDocument(
    val entries: List<BattleJournalEntry> = emptyList(),
)
