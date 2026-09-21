package dev.alenajam.monsterdialer.calls.ui

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.alenajam.monsterdialer.characters.data.ContactArtworkPreferences
import dev.alenajam.monsterdialer.characters.data.ContactArtworkPriority
import javax.inject.Inject
import kotlinx.coroutines.flow.StateFlow

@HiltViewModel
class ContactArtworkPreferencesViewModel @Inject constructor(
    private val preferences: ContactArtworkPreferences,
) : ViewModel() {
    val priority: StateFlow<ContactArtworkPriority> = preferences.priority

    fun setPriority(priority: ContactArtworkPriority) = preferences.setPriority(priority)
}
