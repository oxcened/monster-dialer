package dev.alenajam.monsterdialer.characters.data

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CharacterLayoutPreferences @Inject constructor(
    @ApplicationContext context: Context
) {
    private val preferences = context.getSharedPreferences(PreferencesName, Context.MODE_PRIVATE)

    fun isGridLayout(): Boolean = preferences.getBoolean(GridLayoutKey, false)

    fun setGridLayout(isGridLayout: Boolean) {
        preferences.edit().putBoolean(GridLayoutKey, isGridLayout).apply()
    }

    private companion object {
        const val PreferencesName = "character_layout"
        const val GridLayoutKey = "grid_layout"
    }
}
