package dev.alenajam.monsterdialer.app

import com.google.firebase.appcheck.AppCheckProviderFactory
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory

internal fun appCheckProviderFactory(): AppCheckProviderFactory =
    DebugAppCheckProviderFactory.getInstance()
