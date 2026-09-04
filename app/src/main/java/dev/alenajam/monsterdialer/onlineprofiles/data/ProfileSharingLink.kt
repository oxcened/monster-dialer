package dev.alenajam.monsterdialer.onlineprofiles.data

import dev.alenajam.monsterdialer.BuildConfig
import java.net.URI

/** The canonical public URL used to share and open an Online Profile. */
object ProfileSharingLink {
    private const val ProfilePathSegment = "p"

    fun urlFor(publicProfileId: String): String {
        require(PublicProfileId.isValid(publicProfileId)) { "Profile ID is invalid" }
        return "https://${BuildConfig.PROFILE_SHARING_HOST}/$ProfilePathSegment/$publicProfileId"
    }

    fun profileIdFrom(rawUri: String): String? {
        val uri = runCatching { URI(rawUri) }.getOrNull() ?: return null
        if (uri.rawQuery != null || uri.rawFragment != null) return null
        val isHttpsProfileLink = uri.scheme.equals("https", ignoreCase = true) &&
            uri.host.equals(BuildConfig.PROFILE_SHARING_HOST, ignoreCase = true) &&
            uri.port == -1
        val isLegacyProfileLink = uri.scheme == "monsterdialer" && uri.host == "profile"
        if (!isHttpsProfileLink && !isLegacyProfileLink) return null

        val pathSegments = uri.rawPath.orEmpty().split('/').filter(String::isNotEmpty)
        val profileId = if (isHttpsProfileLink) {
            pathSegments.takeIf { it.size == 2 && it.first() == ProfilePathSegment }?.last()
        } else {
            pathSegments.singleOrNull()
        }
        return profileId?.takeIf(PublicProfileId::isValid)
    }
}
