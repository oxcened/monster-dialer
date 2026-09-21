package dev.alenajam.monsterdialer.characters.data

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

enum class ContactArtworkPriority {
    TRAINER,
    MONSTER,
}

@Singleton
class ContactArtworkPreferences @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val preferences = context.getSharedPreferences(PreferencesName, Context.MODE_PRIVATE)
    private val _priority = MutableStateFlow(readPriority())
    val priority: StateFlow<ContactArtworkPriority> = _priority

    fun setPriority(priority: ContactArtworkPriority) {
        preferences.edit().putString(PriorityKey, priority.name).apply()
        _priority.value = priority
    }

    private fun readPriority(): ContactArtworkPriority =
        preferences.getString(PriorityKey, null)
            ?.let { value -> runCatching { ContactArtworkPriority.valueOf(value) }.getOrNull() }
            ?: ContactArtworkPriority.TRAINER

    private companion object {
        const val PreferencesName = "contact_artwork"
        const val PriorityKey = "priority"
    }
}
