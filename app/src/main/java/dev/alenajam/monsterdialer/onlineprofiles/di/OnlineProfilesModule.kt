package dev.alenajam.monsterdialer.onlineprofiles.di

import android.content.Context
import android.telephony.TelephonyManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.alenajam.monsterdialer.onlineprofiles.data.LibPhoneNumberNormalizer
import dev.alenajam.monsterdialer.onlineprofiles.data.FirebaseOnlineProfileRemoteDataSource
import dev.alenajam.monsterdialer.onlineprofiles.data.OnlineProfileCache
import dev.alenajam.monsterdialer.onlineprofiles.data.OnlineProfileLinkStore
import dev.alenajam.monsterdialer.onlineprofiles.data.PhoneNumberNormalizer
import dev.alenajam.monsterdialer.onlineprofiles.data.OnlineProfileRemoteDataSource
import java.io.File
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object OnlineProfilesModule {
    @Provides @Singleton
    fun providePhoneNumberNormalizer(@ApplicationContext context: Context): PhoneNumberNormalizer {
        val telephony = context.getSystemService(TelephonyManager::class.java)
        return LibPhoneNumberNormalizer(LibPhoneNumberNormalizer.regionProvider(telephony))
    }

    @Provides @Singleton
    fun provideOnlineProfileLinkStore(@ApplicationContext context: Context): OnlineProfileLinkStore =
        OnlineProfileLinkStore(File(context.filesDir, "online-profiles"))

    @Provides @Singleton
    fun provideOnlineProfileCache(@ApplicationContext context: Context): OnlineProfileCache =
        OnlineProfileCache(File(context.filesDir, "online-profiles/cache"))

    @Provides @Singleton
    fun provideOnlineProfileRemoteDataSource(
        impl: FirebaseOnlineProfileRemoteDataSource,
    ): OnlineProfileRemoteDataSource = impl
}
