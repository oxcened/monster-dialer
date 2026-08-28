package dev.alenajam.monsterdialer.packs.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.alenajam.monsterdialer.packs.data.CharacterPackCatalog
import dev.alenajam.monsterdialer.packs.data.CharacterPackRepository
import dev.alenajam.monsterdialer.packs.data.PacksRepository
import dev.alenajam.monsterdialer.packs.data.PacksRepositoryImpl
import java.io.File
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class CharacterPacksDir

@Module
@InstallIn(SingletonComponent::class)
object PacksModule {
    @Provides
    @CharacterPacksDir
    fun provideCharacterPacksDir(@ApplicationContext context: Context): File {
        return File(context.filesDir, "character-packs")
    }

    @Provides
    @Singleton
    fun provideCharacterPackRepository(@CharacterPacksDir root: File): CharacterPackRepository {
        return CharacterPackRepository(root)
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
