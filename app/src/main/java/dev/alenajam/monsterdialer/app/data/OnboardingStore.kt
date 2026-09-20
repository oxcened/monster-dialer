package dev.alenajam.monsterdialer.app.data

import android.content.Context
import dev.alenajam.monsterdialer.battle.data.ActiveBattleEncounterStore
import dev.alenajam.monsterdialer.battle.data.BattleJournalStore
import dev.alenajam.monsterdialer.characters.data.CharacterAssignmentStore
import dev.alenajam.monsterdialer.characters.data.PlayerProfileStatsStore
import dev.alenajam.monsterdialer.characters.data.VariantUnlockStore
import dev.alenajam.monsterdialer.onlineprofiles.data.OnlineProfileLinkStore
import dev.alenajam.monsterdialer.packs.data.CharacterPackCatalog
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OnboardingStore @Inject constructor(
    @ApplicationContext context: Context,
    private val assignments: CharacterAssignmentStore,
    private val packCatalog: CharacterPackCatalog,
    private val battleJournal: BattleJournalStore,
    private val profileStats: PlayerProfileStatsStore,
    private val radiantUnlocks: VariantUnlockStore,
    private val activeBattle: ActiveBattleEncounterStore,
    private val onlineProfileLinks: OnlineProfileLinkStore,
) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun shouldShowWelcome(): Boolean {
        migrateExistingInstallIfNeeded()
        return !preferences.getBoolean(KEY_FIRST_RUN_WELCOME_COMPLETED, false)
    }

    fun markWelcomeCompleted() {
        preferences.edit().putBoolean(KEY_FIRST_RUN_WELCOME_COMPLETED, true).apply()
    }

    fun shouldShowFirstEncounterPrompt(): Boolean =
        !preferences.getBoolean(KEY_FIRST_ENCOUNTER_PROMPT_SHOWN, false)

    fun markFirstEncounterPromptShown() {
        preferences.edit().putBoolean(KEY_FIRST_ENCOUNTER_PROMPT_SHOWN, true).apply()
    }

    private fun migrateExistingInstallIfNeeded() {
        if (preferences.getBoolean(KEY_ONBOARDING_MIGRATED, false)) return

        preferences.edit()
            .putBoolean(KEY_FIRST_RUN_WELCOME_COMPLETED, hasExistingInstallData())
            .putBoolean(KEY_ONBOARDING_MIGRATED, true)
            .apply()
    }

    private fun hasExistingInstallData(): Boolean =
        assignments.hasStoredData() ||
            packCatalog.hasStoredData() ||
            battleJournal.hasStoredData() ||
            profileStats.hasStoredData() ||
            radiantUnlocks.hasStoredData() ||
            activeBattle.hasStoredData() ||
            onlineProfileLinks.hasStoredData()

    private companion object {
        const val PREFERENCES_NAME = "monster_onboarding"
        const val KEY_FIRST_RUN_WELCOME_COMPLETED = "first_run_welcome_completed"
        const val KEY_FIRST_ENCOUNTER_PROMPT_SHOWN = "first_encounter_prompt_shown"
        const val KEY_ONBOARDING_MIGRATED = "onboarding_migrated"
    }
}
