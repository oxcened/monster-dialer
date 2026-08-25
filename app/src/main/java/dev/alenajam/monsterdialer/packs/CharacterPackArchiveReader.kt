package dev.alenajam.monsterdialer.packs

import java.io.File
import java.util.zip.ZipFile

/** Reads and validates archive structure without extracting untrusted paths to disk. */
class CharacterPackArchiveReader {
    fun read(archive: File): ValidatedCharacterPack {
        ZipFile(archive).use { zip ->
            val entries = zip.entries().asSequence().toList()
            if (entries.size > MaxArchiveEntries) fail("Pack contains too many files")

            val names = mutableSetOf<String>()
            var uncompressedBytes = 0L
            entries.filterNot { it.isDirectory }.forEach { entry ->
                val name = CharacterPackValidator.validateArchivePath(entry.name)
                if (!names.add(name)) fail("Pack contains duplicate file paths")
                if (entry.size < 0 || entry.size > MaxSingleFileBytes) {
                    fail("Pack contains a file that is too large")
                }
                uncompressedBytes += entry.size
                if (uncompressedBytes > MaxUncompressedBytes) fail("Pack expands to too much data")
            }

            val manifestEntry = zip.getEntry(CharacterPackValidator.ManifestPath)
                ?: fail("Pack is missing manifest.json")
            if (manifestEntry.isDirectory || manifestEntry.size > MaxManifestBytes) {
                fail("Pack manifest is too large")
            }

            val manifestText = zip.getInputStream(manifestEntry).bufferedReader().use { reader ->
                reader.readText().also {
                    if (it.toByteArray(Charsets.UTF_8).size > MaxManifestBytes) fail("Pack manifest is too large")
                }
            }
            val manifest = CharacterPackManifestCodec.decode(manifestText)
            val validated = CharacterPackValidator.validate(manifest)
            if (!names.containsAll(validated.files)) fail("Pack manifest refers to missing files")
            if (names != validated.files) fail("Pack contains files that are not referenced by manifest.json")
            return validated
        }
    }

    private fun fail(message: String): Nothing = throw CharacterPackValidationException(message)

    private companion object {
        const val MaxArchiveEntries = 512
        const val MaxManifestBytes = 256 * 1024L
        const val MaxSingleFileBytes = 8 * 1024 * 1024L
        const val MaxUncompressedBytes = 48 * 1024 * 1024L
    }
}
