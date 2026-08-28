package dev.alenajam.monsterdialer.characters.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.alenajam.monsterdialer.characters.data.CharacterAssignmentRepository
import dev.alenajam.monsterdialer.characters.data.CharacterAssignmentRepositoryImpl
import dev.alenajam.monsterdialer.characters.data.CharactersRepository
import dev.alenajam.monsterdialer.characters.data.CharactersRepositoryImpl
import dev.alenajam.monsterdialer.characters.data.CharacterAssignmentStore
import dev.alenajam.monsterdialer.packs.di.CharacterPacksDir
import java.io.File
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object CharactersModule {
    @Provides
    @Singleton
    fun provideCharacterAssignmentStore(@CharacterPacksDir root: File): CharacterAssignmentStore {
        return CharacterAssignmentStore(root)
    }

    @Provides
    @Singleton
    fun provideCharactersRepository(impl: CharactersRepositoryImpl): CharactersRepository {
        return impl
    }

    @Provides
    @Singleton
    fun provideCharacterAssignmentRepository(impl: CharacterAssignmentRepositoryImpl): CharacterAssignmentRepository {
        return impl
    }
}
