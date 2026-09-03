package dev.alenajam.monsterdialer.ui.battle

import dev.alenajam.monsterdialer.characters.data.CharacterAssignmentStore
import dev.alenajam.monsterdialer.characters.data.CharactersRepositoryImpl
import dev.alenajam.monsterdialer.characters.data.RadiantVariantUnlockStore
import dev.alenajam.monsterdialer.characters.data.PlayerProfileStatsStore
import dev.alenajam.monsterdialer.characters.data.CharacterAssignmentRepositoryImpl
import dev.alenajam.monsterdialer.battle.data.AssignedCharacterEncounterFactory
import dev.alenajam.monsterdialer.battle.data.ActiveBattleEncounterStore
import dev.alenajam.monsterdialer.battle.data.ActiveCallKey
import dev.alenajam.monsterdialer.battle.data.BattleEncounterFactory
import dev.alenajam.monsterdialer.battle.data.EncounterType
import dev.alenajam.monsterdialer.packs.data.CharacterAssignmentTarget
import dev.alenajam.monsterdialer.packs.data.CharacterPackCatalog
import dev.alenajam.monsterdialer.packs.data.CharacterPackManifest
import dev.alenajam.monsterdialer.packs.data.CharacterPackRepository
import dev.alenajam.monsterdialer.packs.data.CharacterReference
import dev.alenajam.monsterdialer.packs.data.CharacterType
import dev.alenajam.monsterdialer.packs.data.CharacterVisualVariant
import dev.alenajam.monsterdialer.packs.data.InstalledPackCharacter
import dev.alenajam.monsterdialer.packs.data.PackCharacter
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotSame
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import kotlin.random.Random

class AssignedCharacterEncounterFactoryTest {
    @get:Rule val temporaryFolder = TemporaryFolder()

    private val storageRoot by lazy { temporaryFolder.newFolder("packs") }
    private val catalog by lazy { CharacterPackCatalog(storageRoot) }
    private val assignmentsRoot by lazy { temporaryFolder.newFolder("assignments") }
    private val store by lazy { CharacterAssignmentStore(assignmentsRoot) }
    private val repository by lazy { CharacterPackRepository(storageRoot, catalog) }
    private val assignmentRepository by lazy { CharacterAssignmentRepositoryImpl(store) }
    private val charactersRepository by lazy { CharactersRepositoryImpl(repository, assignmentRepository) }
    private val radiantUnlocks by lazy { RadiantVariantUnlockStore(storageRoot) }
    private val activeEncounterStore by lazy { ActiveBattleEncounterStore(storageRoot) }
    private val profileStatsStore by lazy { PlayerProfileStatsStore(storageRoot) }
    private val factory by lazy {
        AssignedCharacterEncounterFactory(charactersRepository, assignmentRepository, radiantUnlocks, activeEncounterStore, profileStatsStore)
    }

    @Test
    fun forCallPicksAssignedMonsters() {
        val packId = "com.example.test"
        val characterId = "monster1"
        setupPack(packId, characterId, CharacterType.Monster, CharacterAssignmentTarget.Contact)
        
        val reference = CharacterReference(packId, characterId)
        store.assignContact("1234567890", CharacterType.Monster, reference)

        val encounter = factory.forCall("call1", "1234567890", "Alex", isAnonymous = false)

        assertEquals("Monster 1", encounter.enemy?.name)
        assertEquals(EncounterType.Trainer, encounter.type)
    }

    @Test
    fun phoneNumberNormalizationWorksInStore() {
        val character = CharacterReference("pack", "char")
        
        // Test various formats
        store.assignContact("+1 (234) 567-890", character)
        
        assertEquals(character, store.characterForContact("+1234567890"))
        assertEquals(character, store.characterForContact(" +1-234-567-890 "))
        assertEquals(character, store.characterForContact("+1.234.567.890"))
    }

    @Test
    fun fallsBackToBattleEncounterFactoryWhenNoAssignmentFound() {
        val encounter = factory.forCall("call1", "999", "Unknown", isAnonymous = false)
        
        val defaultEncounter = BattleEncounterFactory.forCall("call1", "Unknown", isAnonymous = false)
        assertEquals(defaultEncounter.player.name, encounter.player.name)
        assertEquals(defaultEncounter.enemy?.name, encounter.enemy?.name)
    }

    @Test
    fun randomContactMonsterChangesForEachCall() {
        setupPack("com.example.first", "first", CharacterType.Monster, CharacterAssignmentTarget.Contact, name = "First Monster")
        setupPack("com.example.second", "second", CharacterType.Monster, CharacterAssignmentTarget.Contact, name = "Second Monster")
        store.randomizeContact("123", CharacterType.Monster)

        val first = AssignedCharacterEncounterFactory(
            charactersRepository, assignmentRepository, radiantUnlocks, activeEncounterStore, profileStatsStore, FirstRandom,
        ).forCall("call1", "123", "Alex", isAnonymous = false)
        val second = AssignedCharacterEncounterFactory(
            charactersRepository, assignmentRepository, radiantUnlocks, activeEncounterStore, profileStatsStore, LastRandom,
        ).forCall("call2", "123", "Alex", isAnonymous = false)

        assertEquals("First Monster", first.enemy?.name)
        assertEquals("Second Monster", second.enemy?.name)
    }

    @Test
    fun handlesAnonymousCallsByIgnoringContactAssignments() {
        val packId = "com.example.test"
        val characterId = "monster1"
        setupPack(packId, characterId, CharacterType.Monster, CharacterAssignmentTarget.Contact)
        
        val reference = CharacterReference(packId, characterId)
        store.assignContact("123", CharacterType.Monster, reference)

        val encounter = factory.forCall("call1", "123", "Alex", isAnonymous = true)

        assertEquals(EncounterType.Anonymous, encounter.type)
        assertNotEquals("Monster 1", encounter.enemy?.name)
    }

    @Test
    fun radiantRollReplacesAnyCallWithARandomRadiantWildMonster() {
        setupRadiantPack()
        val radiantFactory = AssignedCharacterEncounterFactory(
            charactersRepository,
            assignmentRepository,
            radiantUnlocks,
            activeEncounterStore,
            profileStatsStore,
            random = AlwaysRadiantRandom,
        )

        val encounter = radiantFactory.forCall("call1", "123", "Alex", isAnonymous = false)

        assertEquals(EncounterType.RadiantWild, encounter.type)
        assertEquals("Radiant Monster", encounter.enemy?.name)
        assertEquals(true, encounter.enemy?.isRadiant)
    }

    @Test
    fun radiantEncounterSurvivesFactoryRecreationForTheSameCall() {
        setupRadiantPack()
        val firstFactory = AssignedCharacterEncounterFactory(
            charactersRepository,
            assignmentRepository,
            radiantUnlocks,
            activeEncounterStore,
            profileStatsStore,
            random = AlwaysRadiantRandom,
        )
        firstFactory.forCall("telecom-call-1", "123", "Alex", isAnonymous = false)

        val recreatedFactory = AssignedCharacterEncounterFactory(
            charactersRepository,
            assignmentRepository,
            radiantUnlocks,
            activeEncounterStore,
            profileStatsStore,
            random = NeverRadiantRandom,
        )
        val restoredEncounter = recreatedFactory.forCall("telecom-call-1", "123", "Alex", isAnonymous = false)

        assertEquals(EncounterType.RadiantWild, restoredEncounter.type)
        assertEquals("Radiant Monster", restoredEncounter.enemy?.name)
        assertEquals(1, profileStatsStore.callsBattled.value)
    }

    @Test
    fun encountersRemainIndependentWhenCallsAreSwitched() {
        setupRadiantPack()
        AssignedCharacterEncounterFactory(
            charactersRepository,
            assignmentRepository,
            radiantUnlocks,
            activeEncounterStore,
            profileStatsStore,
            random = AlwaysRadiantRandom,
        ).forCall("telecom-call-1", "123", "Alex", isAnonymous = false)
        activeEncounterStore.save(
            ActiveCallKey("telecom-call-2", "456", "Bea", isAnonymous = false),
            radiantReference = null,
        )

        val recreatedFactory = AssignedCharacterEncounterFactory(
            charactersRepository,
            assignmentRepository,
            radiantUnlocks,
            activeEncounterStore,
            profileStatsStore,
            random = NeverRadiantRandom,
        )

        assertEquals(
            EncounterType.RadiantWild,
            recreatedFactory.forCall("telecom-call-1", "123", "Alex", isAnonymous = false).type,
        )
        assertEquals(
            EncounterType.Trainer,
            recreatedFactory.forCall("telecom-call-2", "456", "Bea", isAnonymous = false).type,
        )
    }

    @Test
    fun clearingTheCacheBuildsAFreshEncounterForTheNextCall() {
        val firstEncounter = factory.forCall("same-call-key", "123", "Alex", isAnonymous = false)

        factory.clearCachedEncounter()

        val nextEncounter = factory.forCall("same-call-key", "123", "Alex", isAnonymous = false)
        assertNotSame(firstEncounter, nextEncounter)
    }

    private fun setupRadiantPack() {
        val packId = "com.example.radiant"
        val packDir = File(File(storageRoot, packId), "active")
        packDir.mkdirs()
        val manifest = CharacterPackManifest(
            formatVersion = 2,
            id = packId,
            name = "Radiant Pack",
            version = "1.0.0",
            license = "MIT",
            characters = listOf(
                PackCharacter(
                    id = "radiant-monster",
                    name = "Radiant Monster",
                    type = CharacterType.Monster,
                    assignableTo = listOf(CharacterAssignmentTarget.Contact),
                    variants = listOf(
                        CharacterVisualVariant("default", "Default", frontImage = "front.png"),
                        CharacterVisualVariant("radiant", "Radiant", frontImage = "radiant-front.png", isRadiant = true),
                    )
                )
            )
        )
        File(packDir, "manifest.json").writeText(
            """{"formatVersion":2,"id":"$packId","name":"Radiant Pack","version":"1.0.0","license":"MIT","characters":[{"id":"radiant-monster","name":"Radiant Monster","type":"monster","assignableTo":["contact"],"variants":[{"id":"default","name":"Default","frontImage":"front.png"},{"id":"radiant","name":"Radiant","frontImage":"radiant-front.png","isRadiant":true}]}]}"""
        )
        File(packDir, "front.png").writeText("fake image content")
        File(packDir, "radiant-front.png").writeText("fake image content")
        catalog.recordInstallation(manifest)
    }

    private fun setupPack(
        packId: String,
        characterId: String,
        type: CharacterType,
        vararg assignableTo: CharacterAssignmentTarget,
        name: String = "Monster 1",
    ) {
        val packDir = File(File(storageRoot, packId), "active")
        packDir.mkdirs()
        
        val manifestObj = CharacterPackManifest(
            formatVersion = 1,
            id = packId,
            name = "Test Pack",
            version = "1.0.0",
            license = "MIT",
            characters = listOf(
                PackCharacter(
                    id = characterId,
                    name = name,
                    type = type,
                    assignableTo = assignableTo.toList(),
                    frontImage = "front.png",
                    backImage = "back.png"
                )
            )
        )

        val manifest = """
            {
                "formatVersion": 1,
                "id": "$packId",
                "name": "Test Pack",
                "version": "1.0.0",
                "license": "MIT",
                "characters": [
                    {
                        "id": "$characterId",
                        "name": "$name",
                        "type": "${if (type == CharacterType.Monster) "monster" else "trainer"}",
                        "assignableTo": [${assignableTo.joinToString { "\"${if (it == CharacterAssignmentTarget.Contact) "contact" else "player"}\"" }}],
                        "frontImage": "front.png",
                        "backImage": "back.png"
                    }
                ]
            }
        """.trimIndent()
        
        File(packDir, "manifest.json").writeText(manifest)
        File(packDir, "front.png").writeText("fake image content")
        File(packDir, "back.png").writeText("fake image content")
        
        catalog.recordInstallation(manifestObj)
    }

    private object AlwaysRadiantRandom : Random() {
        override fun nextBits(bitCount: Int): Int = 0
    }

    private object NeverRadiantRandom : Random() {
        override fun nextBits(bitCount: Int): Int = when (bitCount) {
            0 -> 0
            Int.SIZE_BITS -> -1
            else -> (1 shl bitCount) - 1
        }
    }

    private object FirstRandom : Random() {
        override fun nextBits(bitCount: Int): Int = 0
    }

    private object LastRandom : Random() {
        override fun nextBits(bitCount: Int): Int = when (bitCount) {
            0 -> 0
            Int.SIZE_BITS -> -1
            else -> (1 shl bitCount) - 1
        }
    }
}
