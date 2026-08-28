package dev.alenajam.monsterdialer.contacts.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.alenajam.monsterdialer.contacts.data.ContactSelectionRepository
import dev.alenajam.monsterdialer.contacts.data.ContactSelectionRepositoryImpl
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ContactsModule {
    @Provides
    @Singleton
    fun provideContactSelectionRepository(impl: ContactSelectionRepositoryImpl): ContactSelectionRepository {
        return impl
    }
}
