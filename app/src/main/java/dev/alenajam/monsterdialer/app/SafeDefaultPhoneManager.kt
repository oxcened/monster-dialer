package dev.alenajam.monsterdialer.app

import android.content.Intent
import android.content.pm.PackageManager
import android.telecom.TelecomManager
import android.util.Log
import dev.alenajam.monsterdialer.analytics.MonsterAnalytics
import dev.alenajam.opendialer.core.common.DefaultPhoneManager

/**
 * Prevents the legacy default-dialer request from being launched on devices that do not provide
 * a telecom settings activity, such as Wi-Fi-only tablets.
 */
class SafeDefaultPhoneManager(
    private val delegate: DefaultPhoneManager,
    private val packageManager: PackageManager,
    private val analytics: MonsterAnalytics? = null,
    private val logger: (String) -> Unit = { message -> Log.w(TAG, message) },
) : DefaultPhoneManager {

    private var lastDefaultState: Boolean? = null

    override fun isDefaultDialer(): Boolean {
        val isDefault = delegate.isDefaultDialer()
        if (isDefault && lastDefaultState != true) analytics?.defaultDialerReady()
        lastDefaultState = isDefault
        return isDefault
    }

    override fun createRequestDefaultDialerIntent(): Intent? {
        val intent = delegate.createRequestDefaultDialerIntent() ?: return null
        if (intent.action != TelecomManager.ACTION_CHANGE_DEFAULT_DIALER) return intent

        val canRequestDefaultDialer = packageManager.hasSystemFeature(PackageManager.FEATURE_TELEPHONY) &&
            packageManager.resolveActivity(intent, 0) != null
        if (canRequestDefaultDialer) {
            analytics?.defaultDialerRequestStarted()
            return intent
        }

        logger("Default dialer request is unavailable on this device")
        return null
    }

    private companion object {
        const val TAG = "DefaultDialer"
    }
}
