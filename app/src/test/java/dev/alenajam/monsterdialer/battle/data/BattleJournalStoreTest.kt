package dev.alenajam.monsterdialer.battle.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class BattleJournalStoreTest {
    @get:Rule val temporaryFolder = TemporaryFolder()

    @Test
    fun retainsOnlyTheLatestFiveThousandRegularEntriesAndAllRadiantDiscoveries() {
        val journal = BattleJournalStore(temporaryFolder.newFolder("journal"))

        repeat(5_001) { index ->
            journal.record(BattleEncounterFactory.forCall("regular-$index", "Caller", isAnonymous = false), false)
        }
        journal.record(
            BattleEncounterFactory.forCall("radiant", "Caller", isAnonymous = false),
            isRadiantDiscovery = true,
        )

        assertEquals(5_001, journal.entries.value.size)
        assertEquals(5_000, journal.entries.value.count { !it.isRadiantDiscovery })
        assertTrue(journal.entries.value.any { it.isRadiantDiscovery })
        assertTrue(journal.entries.value.first().isRadiantDiscovery)
    }

    @Test
    fun recordsExistingArtworkReferencesWithoutCopyingSpriteFiles() {
        val journal = BattleJournalStore(temporaryFolder.newFolder("sprite-references"))

        journal.record(BattleEncounterFactory.forCall("sprite-call", "Caller", isAnonymous = false), false)

        val entry = journal.entries.value.single()
        assertTrue(entry.playerSprite?.drawableResource != null)
        assertTrue(entry.opponentSprite?.drawableResource != null)
    }

    @Test
    fun clearsEntriesAndTheirPersistedJournal() {
        val storageRoot = temporaryFolder.newFolder("clear-journal")
        val journal = BattleJournalStore(storageRoot)
        journal.record(BattleEncounterFactory.forCall("journal-entry", "Caller", isAnonymous = false), false)

        journal.clear()

        assertTrue(journal.entries.value.isEmpty())
        assertTrue(!java.io.File(storageRoot, "battle-journal.json").exists())
    }
}
