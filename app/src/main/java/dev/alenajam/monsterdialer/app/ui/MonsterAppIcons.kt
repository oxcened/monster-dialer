package dev.alenajam.monsterdialer.app.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import dev.alenajam.opendialer.core.common.ui.IconSource

@Immutable
data class MonsterAppIcons(
    val addCharacter: IconSource,
    val radiant: IconSource
)

val DefaultMonsterAppIcons = MonsterAppIcons(
    addCharacter = IconSource.Vector(Icons.Outlined.AddCircleOutline),
    radiant = IconSource.Vector(Icons.Outlined.AutoAwesome)
)

val LocalMonsterAppIcons = staticCompositionLocalOf { DefaultMonsterAppIcons }
