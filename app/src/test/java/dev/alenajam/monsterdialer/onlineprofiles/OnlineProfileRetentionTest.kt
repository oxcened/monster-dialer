package dev.alenajam.monsterdialer.onlineprofiles

import dev.alenajam.monsterdialer.onlineprofiles.data.OnlineProfileRetention
import dev.alenajam.monsterdialer.onlineprofiles.data.needsConfirmation
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OnlineProfileRetentionTest {
    @Test fun `requests confirmation after 330 days`() {
        val day = 24L * 60 * 60 * 1000
        val now = 400L * day

        assertFalse(OnlineProfileRetention(now - 329L * day).needsConfirmation(now))
        assertTrue(OnlineProfileRetention(now - 330L * day).needsConfirmation(now))
    }
}
