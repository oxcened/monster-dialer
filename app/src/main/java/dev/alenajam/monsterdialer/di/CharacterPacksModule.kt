package dev.alenajam.monsterdialer.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.alenajam.monsterdialer.data.characters.CharactersRepository
import dev.alenajam.monsterdialer.data.characters.CharactersRepositoryImpl
import dev.alenajam.monsterdialer.data.characters.PacksRepository
import dev.alenajam.monsterdialer.data.characters.PacksRepositoryImpl
import dev.alenajam.monsterdialer.packs.CharacterAssignmentStore
import dev.alenajam.monsterdialer.packs.CharacterPackCatalog
import dev.alenajam.monsterdialer.packs.CharacterPackRepository
import java.io.File
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class CharacterPacksDir

@Module
@InstallIn(SingletonComponent::class)
object CharacterPacksModule {
    @Provides
    @CharacterPacksDir
    fun provideCharacterPacksDir(@ApplicationContext context: Context): File {
        return File(context.filesDir, "character-packs")
    }

    @Provides
    @Singleton
    fun provideCharacterAssignmentStore(@CharacterPacksDir root: File): CharacterAssignmentStore {
        return CharacterAssignmentStore(root)
    }

    @Provides
    @Singleton
    fun provideCharacterPackRepository(@CharacterPacksDir root: File): CharacterPackRepository {
        return CharacterPackRepository(root)
    }

    @Provides
    @Singleton
    fun provideCharactersRepository(impl: CharactersRepositoryImpl): CharactersRepository {
        return impl
    }

    @Provides
    @Singleton
    fun provideCharacterPackCatalog(@CharacterPacksDir root: File): CharacterPackCatalog {
        return CharacterPackCatalog(root)
    }

    @Provides
    @Singleton
    fun providePacksRepository(impl: PacksRepositoryImpl): PacksRepository {
        return impl
    }
}
