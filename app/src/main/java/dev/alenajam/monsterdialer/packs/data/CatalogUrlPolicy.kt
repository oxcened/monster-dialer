package dev.alenajam.monsterdialer.packs.data

import java.net.URI

/** Limits catalogs to direct HTTPS endpoints; catalog servers cannot redirect the app elsewhere. */
internal object CatalogUrlPolicy {
    fun requireHttps(value: String): URI {
        val uri = try {
            URI(value)
        } catch (exception: Exception) {
            throw CharacterPackValidationException("Catalog URL is invalid")
        }
        if (uri.scheme != "https" || uri.host.isNullOrBlank() || uri.userInfo != null || uri.fragment != null) {
            throw CharacterPackValidationException("Catalog URLs must be direct HTTPS URLs")
        }
        return uri
    }
}
