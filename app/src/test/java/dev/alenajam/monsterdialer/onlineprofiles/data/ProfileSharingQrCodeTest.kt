package dev.alenajam.monsterdialer.onlineprofiles.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfileSharingQrCodeTest {
    @Test
    fun `encode creates a square matrix with both dark and light modules`() {
        val qrCode = ProfileSharingQrCode.encode("https://monsterdialer.web.app/profile/test-profile-id")

        assertEquals(qrCode.width, qrCode.height)
        val modules = buildList {
            repeat(qrCode.height) { y ->
                repeat(qrCode.width) { x -> add(qrCode[x, y]) }
            }
        }
        assertTrue(modules.any())
        assertTrue(modules.any { !it })
    }
}
