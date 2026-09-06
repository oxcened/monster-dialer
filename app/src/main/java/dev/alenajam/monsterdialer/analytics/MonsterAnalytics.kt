package dev.alenajam.monsterdialer.analytics

import android.content.Context
import android.os.Bundle
import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.FirebaseAnalytics
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Product analytics for the activation funnel.
 *
 * Events intentionally contain only coarse product state. Never add phone numbers, contact
 * names, character names, pack names, or artwork identifiers here.
 */
@Singleton
class MonsterAnalytics @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    private val firebaseAnalytics: FirebaseAnalytics? = runCatching {
        if (FirebaseApp.getApps(context).isEmpty()) return@runCatching null
        FirebaseAnalytics.getInstance(context)
    }.getOrNull()

    fun defaultDialerRequestStarted() = log(EVENT_DEFAULT_DIALER_REQUESTED)

    fun defaultDialerReady() = logOnce(KEY_DEFAULT_DIALER_READY, EVENT_DEFAULT_DIALER_READY)

    fun welcomeCompleted() = log(EVENT_WELCOME_COMPLETED)

    fun callEncounterShown(encounterType: String, hasCustomContent: Boolean) {
        log(EVENT_CALL_ENCOUNTER_SHOWN) {
            param(PARAM_ENCOUNTER_TYPE, encounterType)
            param(PARAM_HAS_CUSTOM_CONTENT, hasCustomContent.toString())
        }
        logOnce(KEY_FIRST_CALL_ENCOUNTER_SHOWN, EVENT_FIRST_CALL_ENCOUNTER_SHOWN)
    }

    fun playerCharacterAssigned(characterType: String) {
        log(EVENT_PLAYER_CHARACTER_ASSIGNED) {
            param(PARAM_CHARACTER_TYPE, characterType)
        }
    }

    fun characterPackImported() = log(EVENT_CHARACTER_PACK_IMPORTED)

    fun battleRecorded(encounterType: String, radiantDiscovered: Boolean) {
        log(EVENT_BATTLE_RECORDED) {
            param(PARAM_ENCOUNTER_TYPE, encounterType)
            param(PARAM_RADIANT_DISCOVERED, radiantDiscovered.toString())
        }
    }

    private fun logOnce(key: String, event: String) {
        if (preferences.getBoolean(key, false)) return
        log(event)
        preferences.edit().putBoolean(key, true).apply()
    }

    private fun log(event: String, parameters: (BundleBuilder.() -> Unit)? = null) {
        val bundle = Bundle().apply {
            parameters?.invoke(BundleBuilder(this))
        }
        firebaseAnalytics?.logEvent(event, bundle)
    }

    private class BundleBuilder(private val bundle: Bundle) {
        fun param(name: String, value: String) = bundle.putString(name, value)
    }

    private companion object {
        const val PREFERENCES_NAME = "monster_analytics"
        const val KEY_DEFAULT_DIALER_READY = "default_dialer_ready"
        const val KEY_FIRST_CALL_ENCOUNTER_SHOWN = "first_call_encounter_shown"

        const val EVENT_DEFAULT_DIALER_REQUESTED = "default_dialer_requested"
        const val EVENT_DEFAULT_DIALER_READY = "default_dialer_ready"
        const val EVENT_WELCOME_COMPLETED = "welcome_completed"
        const val EVENT_CALL_ENCOUNTER_SHOWN = "call_encounter_shown"
        const val EVENT_FIRST_CALL_ENCOUNTER_SHOWN = "first_call_encounter_shown"
        const val EVENT_PLAYER_CHARACTER_ASSIGNED = "player_character_assigned"
        const val EVENT_CHARACTER_PACK_IMPORTED = "character_pack_imported"
        const val EVENT_BATTLE_RECORDED = "battle_recorded"

        const val PARAM_ENCOUNTER_TYPE = "encounter_type"
        const val PARAM_HAS_CUSTOM_CONTENT = "has_custom_content"
        const val PARAM_CHARACTER_TYPE = "character_type"
        const val PARAM_RADIANT_DISCOVERED = "radiant_discovered"
    }
}
