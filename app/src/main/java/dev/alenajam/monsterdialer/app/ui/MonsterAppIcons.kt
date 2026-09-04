package dev.alenajam.monsterdialer.app.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.Reorder
import androidx.compose.material.icons.outlined.Shuffle
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import dev.alenajam.monsterdialer.R
import dev.alenajam.opendialer.core.common.ui.IconSource

@Immutable
data class MonsterAppIcons(
    val addCharacter: IconSource,
    val frontSprite: IconSource,
    val backSprite: IconSource,
    val personalizeContact: IconSource,
    val radiant: IconSource,
    val characterPacks: IconSource,
    val importCharacter: IconSource,
    val createPack: IconSource,
    val viewList: IconSource,
    val viewGrid: IconSource,
    val guide: IconSource,
    val battleJournal: IconSource,
    val reorder: IconSource,
    val randomize: IconSource,
)

val DefaultMonsterAppIcons = MonsterAppIcons(
    addCharacter = IconSource.Vector(Icons.Outlined.AddCircleOutline),
    frontSprite = IconSource.Resource(R.drawable.front_sprite, tintable = false),
    backSprite = IconSource.Resource(R.drawable.back_sprite, tintable = false),
    personalizeContact = IconSource.Resource(R.drawable.more, tintable = false),
    radiant = IconSource.Resource(R.drawable.radiant, tintable = false),
    characterPacks = IconSource.Resource(R.drawable.package_icon, tintable = false),
    importCharacter = IconSource.Resource(R.drawable.import_icon, tintable = false),
    createPack = IconSource.Resource(R.drawable.new_package, tintable = false),
    viewList = IconSource.Resource(R.drawable.menu, tintable = false),
    viewGrid = IconSource.Resource(R.drawable.grid, tintable = false),
    guide = IconSource.Resource(R.drawable.guide, tintable = false),
    battleJournal = IconSource.Resource(R.drawable.book, tintable = false),
    reorder = IconSource.Vector(Icons.Outlined.Reorder),
    randomize = IconSource.Vector(Icons.Outlined.Shuffle),
)

val LocalMonsterAppIcons = staticCompositionLocalOf { DefaultMonsterAppIcons }
