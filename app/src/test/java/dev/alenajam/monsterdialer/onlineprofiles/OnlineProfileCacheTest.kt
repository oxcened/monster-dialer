package dev.alenajam.monsterdialer.onlineprofiles

import dev.alenajam.monsterdialer.onlineprofiles.data.OnlineProfileCache
import java.io.File
import java.nio.file.Files
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OnlineProfileCacheTest {
    @Test fun `reports profile freshness from its cached timestamp`() {
        val root = Files.createTempDirectory("online-profile-cache-test").toFile()
        try {
            val profileId = "shared-profile"
            val profileFile = File(root, "profiles/$profileId.json")
            profileFile.parentFile?.mkdirs()
            profileFile.writeText("{}")
            val refreshInterval = 24L * 60 * 60 * 1000
            val now = refreshInterval * 2

            profileFile.setLastModified(now - refreshInterval + 1)
            assertTrue(OnlineProfileCache(root).isFresh(profileId, refreshInterval, now))

            profileFile.setLastModified(now - refreshInterval - 1)
            assertFalse(OnlineProfileCache(root).isFresh(profileId, refreshInterval, now))
        } finally {
            root.deleteRecursively()
        }
    }
}
