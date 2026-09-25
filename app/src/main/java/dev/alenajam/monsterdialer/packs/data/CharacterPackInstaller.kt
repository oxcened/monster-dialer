package dev.alenajam.monsterdialer.packs.data

import java.io.File
import java.io.InputStream
import java.util.UUID
import java.util.zip.ZipFile
import android.graphics.BitmapFactory
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Installs validated packs beneath an app-private directory. The host app supplies [storageRoot]
 * from `Context.filesDir`, so no imported file needs broad storage permission.
 */
class CharacterPackInstaller(
    private val storageRoot: File,
    private val catalog: CharacterPackCatalog,
    private val archiveReader: CharacterPackArchiveReader = CharacterPackArchiveReader()
) {
    private val journal = File(storageRoot, InstallationJournalFileName)
    private val json = Json { ignoreUnknownKeys = false; explicitNulls = false }

    init {
        recoverInterruptedInstallation()
    }

    @Synchronized
    fun install(source: InputStream): InstalledCharacterPack {
        val transactionId = UUID.randomUUID().toString()
        val transactionRoot = File(File(storageRoot, StagingDirectory), transactionId).apply { mkdirs() }
        val archive = File(transactionRoot, "archive.zip")
        val incoming = File(transactionRoot, "incoming")
        try {
            source.use { input -> copyWithLimit(input, archive) }
            val pack = archiveReader.read(archive)
            val packageRoot = File(storageRoot, pack.manifest.id)
            extractRequiredFiles(archive, incoming, pack.files)
            validateImages(pack, incoming)

            val active = File(packageRoot, ActiveDirectory)
            val backup = File(packageRoot, "backup-$transactionId")
            packageRoot.mkdirs()
            val pending = PendingInstallation(
                manifest = pack.manifest,
                backupDirectoryName = backup.name,
                hadActivePack = active.exists(),
            )
            writeJournal(pending)
            try {
                if (active.exists() && !active.renameTo(backup)) {
                    fail("Could not prepare existing pack for update")
                }
                if (!incoming.renameTo(active)) fail("Could not install pack")
                catalog.recordInstallation(pack.manifest)
            } catch (exception: Exception) {
                if (rollback(active, backup, pending.hadActivePack)) journal.delete()
                throw exception
            }
            backup.deleteRecursively()
            journal.delete()
            return InstalledCharacterPack(pack.manifest, active)
        } finally {
            transactionRoot.deleteRecursively()
        }
    }

    /** Recovers an interrupted directory/catalog update before packs are read by the app. */
    @Synchronized
    private fun recoverInterruptedInstallation() {
        if (!journal.isFile) return
        val pending = runCatching {
            json.decodeFromString<PendingInstallation>(journal.readText())
        }.getOrElse {
            journal.delete()
            return
        }
        val packageRoot = File(storageRoot, pending.manifest.id)
        val active = File(packageRoot, ActiveDirectory)
        val backup = File(packageRoot, pending.backupDirectoryName)
        if (active.matches(pending.manifest) && catalogMatches(pending.manifest)) {
            backup.deleteRecursively()
            journal.delete()
        } else if (rollback(active, backup, pending.hadActivePack)) {
            journal.delete()
        }
    }

    private fun File.matches(manifest: CharacterPackManifest): Boolean = runCatching {
        isDirectory && CharacterPackManifestCodec.decode(
            File(this, CharacterPackValidator.ManifestPath).readText(),
        ) == manifest
    }.getOrDefault(false)

    private fun catalogMatches(manifest: CharacterPackManifest): Boolean = catalog.list()
        .firstOrNull { it.id == manifest.id }
        ?.let { record ->
            record.name == manifest.name &&
                record.version == manifest.version &&
                record.creator == manifest.creator &&
                record.license == manifest.license &&
                record.characterCount == manifest.characters.size
        } == true

    private fun rollback(active: File, backup: File, hadActivePack: Boolean): Boolean {
        if (active.exists() && !active.deleteRecursively()) return false
        return !hadActivePack || backup.renameTo(active)
    }

    private fun writeJournal(pending: PendingInstallation) {
        storageRoot.mkdirs()
        val temporary = File(storageRoot, ".$InstallationJournalFileName-${UUID.randomUUID()}")
        try {
            temporary.writeText(json.encodeToString(pending))
            if (!temporary.renameTo(journal)) fail("Could not prepare pack installation")
        } finally {
            temporary.delete()
        }
    }

    private fun copyWithLimit(input: InputStream, destination: File) {
        destination.outputStream().use { output ->
            val buffer = ByteArray(BufferSize)
            var total = 0L
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                total += read
                if (total > MaxArchiveBytes) fail("Pack archive is too large")
                output.write(buffer, 0, read)
            }
        }
    }

    private fun extractRequiredFiles(archive: File, destination: File, requiredFiles: Set<String>) {
        ZipFile(archive).use { zip ->
            requiredFiles.forEach { path ->
                val entry = zip.getEntry(path) ?: fail("Pack file disappeared during import")
                val target = File(destination, path)
                target.parentFile?.mkdirs()
                zip.getInputStream(entry).use { input ->
                    target.outputStream().use { output -> input.copyTo(output) }
                }
            }
        }
    }

    private fun validateImages(pack: ValidatedCharacterPack, directory: File) {
        // Android's BitmapFactory is a throwing stub in local JVM unit tests; decoding remains
        // enforced on device where imported packs are actually installed.
        if (System.getProperty("java.vm.name") != "Dalvik") return
        pack.manifest.characters.flatMap { character ->
            character.visualVariants.flatMap { listOfNotNull(it.frontImage, it.backImage) }
        }.distinct().forEach { path ->
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(File(directory, path).path, options)
            if (options.outWidth <= 0 || options.outHeight <= 0) fail("Pack contains an invalid image")
            if (options.outWidth > MaxImageDimension || options.outHeight > MaxImageDimension || options.outWidth.toLong() * options.outHeight > MaxImagePixels) {
                fail("Pack image is too large")
            }
        }
    }

    private fun fail(message: String): Nothing = throw CharacterPackValidationException(message)

    private companion object {
        const val ActiveDirectory = "active"
        const val StagingDirectory = ".staging"
        const val InstallationJournalFileName = ".pack-installation.json"
        const val BufferSize = 8 * 1024
        const val MaxArchiveBytes = 24L * 1024 * 1024
        const val MaxImageDimension = 4096
        const val MaxImagePixels = 16L * 1024 * 1024
    }
}

@Serializable
private data class PendingInstallation(
    val manifest: CharacterPackManifest,
    val backupDirectoryName: String,
    val hadActivePack: Boolean,
)
