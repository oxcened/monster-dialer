package dev.alenajam.monsterdialer.onlineprofiles.data

import java.io.File
import java.util.UUID
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
private data class OnlineProfileLinksDocument(
    val schemaVersion: Int = 1,
    val links: Map<String, String> = emptyMap(),
)

/** App-private storage. Phone numbers and contact associations never leave this file. */
class OnlineProfileLinkStore(
    private val root: File,
    private val json: Json = Json { ignoreUnknownKeys = false; explicitNulls = false },
) {
    @Synchronized fun profileIdFor(e164: String): String? = read().links[e164]

    @Synchronized fun link(e164Numbers: Collection<String>, publicProfileId: String) {
        require(PublicProfileId.isValid(publicProfileId)) { "Profile ID is invalid" }
        val updated = read().links.toMutableMap()
        e164Numbers.forEach { updated[it] = publicProfileId }
        write(OnlineProfileLinksDocument(links = updated))
    }

    @Synchronized fun unlink(e164Numbers: Collection<String>) {
        val updated = read().links.toMutableMap()
        e164Numbers.forEach(updated::remove)
        write(OnlineProfileLinksDocument(links = updated))
    }

    private fun read(): OnlineProfileLinksDocument {
        val file = File(root, FileName)
        if (!file.exists()) return OnlineProfileLinksDocument()
        return runCatching { json.decodeFromString<OnlineProfileLinksDocument>(file.readText()) }
            .getOrElse { OnlineProfileLinksDocument() }
    }

    private fun write(document: OnlineProfileLinksDocument) {
        root.mkdirs()
        val destination = File(root, FileName)
        val temporary = File(root, ".${FileName}-${UUID.randomUUID()}")
        temporary.writeText(json.encodeToString(document))
        if (!temporary.renameTo(destination)) {
            temporary.delete()
            error("Could not update online profile links")
        }
    }

    private companion object { const val FileName = "online-profile-links.json" }
}

object PublicProfileId {
    private val pattern = Regex("[A-Za-z0-9_-]{22,128}")
    fun isValid(value: String) = pattern.matches(value)
}
