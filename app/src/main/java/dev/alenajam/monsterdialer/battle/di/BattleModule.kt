package dev.alenajam.monsterdialer.battle.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.alenajam.opendialer.core.common.ui.InCallUI
import dev.alenajam.monsterdialer.battle.ui.MonsterInCallUI
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class BattleModule {
    @Binds
    @Singleton
    abstract fun bindInCallUI(impl: MonsterInCallUI): InCallUI
}
