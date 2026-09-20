package dev.alenajam.monsterdialer.characters.data

import dev.alenajam.monsterdialer.packs.data.CharacterReference
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class VariantUnlockStoreTest {
    @get:Rule val temporaryFolder = TemporaryFolder()

    @Test
    fun mergeRetainsLocalUnlocksAndAddsRestoredVariants() {
        val store = VariantUnlockStore(temporaryFolder.newFolder("variants"))
        val radiant = CharacterReference("com.example.forest", "mossling", "radiant")
        val seasonal = CharacterReference("com.example.coast", "tidescale", "winter")

        store.unlock(radiant)

        assertTrue(store.merge(setOf(seasonal)))
        assertEquals(setOf(radiant, seasonal), store.unlocked.value)
        assertFalse(store.merge(setOf(radiant, seasonal)))
    }

    @Test
    fun unlockedVariantsPersistForTheNextAppSession() {
        val directory = temporaryFolder.newFolder("variants")
        val variant = CharacterReference("com.example.forest", "mossling", "winter")

        VariantUnlockStore(directory).merge(setOf(variant))

        assertEquals(setOf(variant), VariantUnlockStore(directory).unlocked.value)
    }
}
