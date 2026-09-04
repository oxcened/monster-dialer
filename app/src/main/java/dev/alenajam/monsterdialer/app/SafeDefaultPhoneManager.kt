package dev.alenajam.monsterdialer.app

import android.content.Intent
import android.content.pm.PackageManager
import android.telecom.TelecomManager
import android.util.Log
import dev.alenajam.opendialer.core.common.DefaultPhoneManager

/**
 * Prevents the legacy default-dialer request from being launched on devices that do not provide
 * a telecom settings activity, such as Wi-Fi-only tablets.
 */
class SafeDefaultPhoneManager(
    private val delegate: DefaultPhoneManager,
    private val packageManager: PackageManager,
    private val logger: (String) -> Unit = { message -> Log.w(TAG, message) },
) : DefaultPhoneManager {

    override fun isDefaultDialer(): Boolean = delegate.isDefaultDialer()

    override fun createRequestDefaultDialerIntent(): Intent? {
        val intent = delegate.createRequestDefaultDialerIntent() ?: return null
        if (intent.action != TelecomManager.ACTION_CHANGE_DEFAULT_DIALER) return intent

        val canRequestDefaultDialer = packageManager.hasSystemFeature(PackageManager.FEATURE_TELEPHONY) &&
            packageManager.resolveActivity(intent, 0) != null
        if (canRequestDefaultDialer) return intent

        logger("Default dialer request is unavailable on this device")
        return null
    }

    private companion object {
        const val TAG = "DefaultDialer"
    }
}
