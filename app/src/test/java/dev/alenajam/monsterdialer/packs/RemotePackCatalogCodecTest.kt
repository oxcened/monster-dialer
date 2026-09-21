package dev.alenajam.monsterdialer.packs

import dev.alenajam.monsterdialer.packs.data.CharacterPackValidationException
import dev.alenajam.monsterdialer.packs.data.RemotePackCatalogCodec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class RemotePackCatalogCodecTest {
    @Test
    fun `decodes a catalog with an externally hosted pack`() {
        val catalog = RemotePackCatalogCodec.decode(
            """
            {
              "formatVersion": 1,
              "id": "org.example.catalog",
              "name": "Example catalog",
              "packs": [{
                "id": "org.example.mossling",
                "version": "1.0.0",
                "name": "Mossling",
                "license": "CC BY 4.0",
                "downloadUrl": "https://example.org/mossling.monsterpack",
                "sha256": "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef",
                "sizeBytes": 12
              }]
            }
            """.trimIndent(),
        )

        assertEquals("org.example.catalog", catalog.id)
        assertEquals("org.example.mossling", catalog.packs.single().id)
    }

    @Test
    fun `rejects a pack served over http`() {
        assertThrows(CharacterPackValidationException::class.java) {
            RemotePackCatalogCodec.decode(
                """
                {
                  "formatVersion": 1,
                  "id": "org.example.catalog",
                  "name": "Example catalog",
                  "packs": [{
                    "id": "org.example.mossling",
                    "version": "1.0.0",
                    "name": "Mossling",
                    "license": "CC BY 4.0",
                    "downloadUrl": "http://example.org/mossling.monsterpack",
                    "sha256": "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef",
                    "sizeBytes": 12
                  }]
                }
                """.trimIndent(),
            )
        }
    }
}
