package dev.alenajam.monsterdialer.packs

import java.io.File
import java.io.InputStream
import java.util.UUID
import java.util.zip.ZipFile

/**
 * Installs validated packs beneath an app-private directory. The host app supplies [storageRoot]
 * from `Context.filesDir`, so no imported file needs broad storage permission.
 */
class CharacterPackInstaller(
    private val storageRoot: File,
    private val archiveReader: CharacterPackArchiveReader = CharacterPackArchiveReader(),
    private val catalog: CharacterPackCatalog = CharacterPackCatalog(storageRoot)
) {
    fun install(source: InputStream): InstalledCharacterPack {
        val stagingRoot = File(storageRoot, ".staging").apply { mkdirs() }
        val archive = File(stagingRoot, "${UUID.randomUUID()}.zip")
        try {
            source.use { input -> copyWithLimit(input, archive) }
            val pack = archiveReader.read(archive)
            val packageRoot = File(storageRoot, pack.manifest.id)
            val incoming = File(packageRoot, "incoming-${UUID.randomUUID()}")
            extractRequiredFiles(archive, incoming, pack.files)

            val active = File(packageRoot, ActiveDirectory)
            val backup = File(packageRoot, "backup-${UUID.randomUUID()}")
            packageRoot.mkdirs()
            if (active.exists() && !active.renameTo(backup)) fail("Could not prepare existing pack for update")
            if (!incoming.renameTo(active)) {
                if (backup.exists()) backup.renameTo(active)
                fail("Could not install pack")
            }
            backup.deleteRecursively()
            catalog.recordInstallation(pack.manifest)
            return InstalledCharacterPack(pack.manifest, active)
        } finally {
            archive.delete()
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

    private fun fail(message: String): Nothing = throw CharacterPackValidationException(message)

    private companion object {
        const val ActiveDirectory = "active"
        const val BufferSize = 8 * 1024
        const val MaxArchiveBytes = 24L * 1024 * 1024
    }
}
