package dev.alenajam.monsterdialer.app

import android.app.Application
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import dagger.hilt.android.HiltAndroidApp
import dev.alenajam.monsterdialer.characters.data.VariantBackupSynchronizer
import dev.alenajam.monsterdialer.onlineprofiles.data.OnlineProfileAutoPublisher
import javax.inject.Inject

@HiltAndroidApp
class MonsterApp : Application() {
    @Inject lateinit var onlineProfileAutoPublisher: OnlineProfileAutoPublisher
    @Inject lateinit var variantBackupSynchronizer: VariantBackupSynchronizer

    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)?.let { app ->
            FirebaseAppCheck.getInstance(app).installAppCheckProviderFactory(appCheckProviderFactory())
        }
        onlineProfileAutoPublisher.start()
        variantBackupSynchronizer.start()
    }
}
