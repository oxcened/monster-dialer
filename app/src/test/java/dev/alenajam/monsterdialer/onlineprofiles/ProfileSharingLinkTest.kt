package dev.alenajam.monsterdialer.onlineprofiles

import dev.alenajam.monsterdialer.onlineprofiles.data.ProfileSharingLink
import dev.alenajam.monsterdialer.BuildConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ProfileSharingLinkTest {
    private val profileId = "7FK2A9QXk1p3R5s7T9v2W4y6"

    @Test
    fun `creates the canonical HTTPS profile link`() {
        assertEquals(
            "https://${BuildConfig.PROFILE_SHARING_HOST}/p/$profileId",
            ProfileSharingLink.urlFor(profileId),
        )
    }

    @Test
    fun `reads only the expected HTTPS profile path`() {
        assertEquals(profileId, ProfileSharingLink.profileIdFrom("https://${BuildConfig.PROFILE_SHARING_HOST}/p/$profileId"))
        assertNull(ProfileSharingLink.profileIdFrom("https://${BuildConfig.PROFILE_SHARING_HOST}/p/$profileId/extra"))
        assertNull(ProfileSharingLink.profileIdFrom("http://${BuildConfig.PROFILE_SHARING_HOST}/p/$profileId"))
    }
}
