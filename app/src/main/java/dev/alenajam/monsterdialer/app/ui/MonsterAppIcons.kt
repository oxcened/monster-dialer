package dev.alenajam.monsterdialer.app.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.automirrored.outlined.ViewList
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import dev.alenajam.opendialer.core.common.ui.IconSource

@Immutable
data class MonsterAppIcons(
    val addCharacter: IconSource,
    val radiant: IconSource,
    val importPacks: IconSource,
    val viewList: IconSource,
    val viewGrid: IconSource,
    val help: IconSource
)

val DefaultMonsterAppIcons = MonsterAppIcons(
    addCharacter = IconSource.Vector(Icons.Outlined.AddCircleOutline),
    radiant = IconSource.Vector(Icons.Outlined.AutoAwesome),
    importPacks = IconSource.Vector(Icons.Outlined.FolderOpen),
    viewList = IconSource.Vector(Icons.AutoMirrored.Outlined.ViewList),
    viewGrid = IconSource.Vector(Icons.Outlined.GridView),
    help = IconSource.Vector(Icons.AutoMirrored.Outlined.HelpOutline)
)

val LocalMonsterAppIcons = staticCompositionLocalOf { DefaultMonsterAppIcons }
