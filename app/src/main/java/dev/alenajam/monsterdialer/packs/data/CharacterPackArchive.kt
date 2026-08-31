package dev.alenajam.monsterdialer.packs.data

data class CharacterPackPreview(
    val manifest: CharacterPackManifest,
    val previewImage: ByteArray?,
)

object CharacterPackArchive {
    const val MimeType = "application/x-monsterpack"
    const val Extension = "monsterpack"

    val importMimeTypes = arrayOf(
        MimeType,
        "application/zip",
        "application/x-zip-compressed",
        "application/octet-stream",
    )

    fun hasSupportedExtension(fileName: String): Boolean =
        fileName.endsWith(".$Extension", ignoreCase = true) ||
            fileName.endsWith(".zip", ignoreCase = true)
}
