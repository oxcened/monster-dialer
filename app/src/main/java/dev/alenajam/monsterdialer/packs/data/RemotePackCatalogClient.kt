package dev.alenajam.monsterdialer.packs.data

import java.net.HttpURLConnection
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RemotePackCatalogClient @Inject constructor() {
    fun fetch(url: String): RemotePackCatalog {
        val connection = open(url)
        try {
            if (connection.responseCode !in 200..299) {
                throw CharacterPackValidationException("Catalog could not be loaded (${connection.responseCode})")
            }
            if (connection.contentLengthLong > MaxCatalogBytes) {
                throw CharacterPackValidationException("Catalog is too large")
            }
            val body = connection.inputStream.use { input ->
                input.readWithLimit(MaxCatalogBytes).decodeToString()
            }
            return RemotePackCatalogCodec.decode(body)
        } finally {
            connection.disconnect()
        }
    }

    fun download(pack: RemotePackCatalogPack, output: java.io.File) {
        val connection = open(pack.downloadUrl)
        try {
            if (connection.responseCode !in 200..299) {
                throw CharacterPackValidationException("Pack could not be downloaded (${connection.responseCode})")
            }
            if (connection.contentLengthLong > pack.sizeBytes || connection.contentLengthLong > MaxPackBytes) {
                throw CharacterPackValidationException("Pack download is too large")
            }
            val digest = MessageDigest.getInstance("SHA-256")
            var downloaded = 0L
            connection.inputStream.use { input ->
                output.outputStream().use { outputStream ->
                    val buffer = ByteArray(BufferSize)
                    while (true) {
                        val read = input.read(buffer)
                        if (read < 0) break
                        downloaded += read
                        if (downloaded > pack.sizeBytes || downloaded > MaxPackBytes) {
                            throw CharacterPackValidationException("Pack download is too large")
                        }
                        digest.update(buffer, 0, read)
                        outputStream.write(buffer, 0, read)
                    }
                }
            }
            if (downloaded != pack.sizeBytes) throw CharacterPackValidationException("Pack download size does not match its catalog entry")
            val actualHash = digest.digest().joinToString("") { byte -> "%02x".format(byte) }
            if (!actualHash.equals(pack.sha256, ignoreCase = true)) {
                throw CharacterPackValidationException("Pack download does not match its catalog entry")
            }
        } finally {
            connection.disconnect()
        }
    }

    private fun open(value: String): HttpURLConnection {
        val uri = CatalogUrlPolicy.requireHttps(value)
        return (uri.toURL().openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = TimeoutMillis
            readTimeout = TimeoutMillis
            instanceFollowRedirects = false
            setRequestProperty("Accept", "application/json, application/octet-stream")
        }
    }

    private fun java.io.InputStream.readWithLimit(limit: Long): ByteArray {
        val output = java.io.ByteArrayOutputStream()
        val buffer = ByteArray(BufferSize)
        var total = 0L
        while (true) {
            val read = read(buffer)
            if (read < 0) return output.toByteArray()
            total += read
            if (total > limit) throw CharacterPackValidationException("Catalog is too large")
            output.write(buffer, 0, read)
        }
    }

    private companion object {
        const val TimeoutMillis = 10_000
        const val BufferSize = 8 * 1024
        const val MaxCatalogBytes = 1L * 1024 * 1024
        const val MaxPackBytes = 24L * 1024 * 1024
    }
}
