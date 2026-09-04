package dev.alenajam.monsterdialer.app

import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.telecom.TelecomManager
import dev.alenajam.opendialer.core.common.DefaultPhoneManager
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

class SafeDefaultPhoneManagerTest {

    @Test
    fun `does not return legacy request when device has no telephony`() {
        val intent = legacyDefaultDialerIntent()
        val delegate = mock<DefaultPhoneManager> {
            on { createRequestDefaultDialerIntent() } doReturn intent
        }
        val packageManager = mock<PackageManager> {
            on { hasSystemFeature(PackageManager.FEATURE_TELEPHONY) } doReturn false
        }

        val manager = SafeDefaultPhoneManager(delegate, packageManager, logger = {})

        assertNull(manager.createRequestDefaultDialerIntent())
    }

    @Test
    fun `does not return legacy request without a handler`() {
        val intent = legacyDefaultDialerIntent()
        val delegate = mock<DefaultPhoneManager> {
            on { createRequestDefaultDialerIntent() } doReturn intent
        }
        val packageManager = mock<PackageManager> {
            on { hasSystemFeature(PackageManager.FEATURE_TELEPHONY) } doReturn true
            on { resolveActivity(intent, 0) } doReturn null
        }

        val manager = SafeDefaultPhoneManager(delegate, packageManager, logger = {})

        assertNull(manager.createRequestDefaultDialerIntent())
    }

    @Test
    fun `returns resolvable legacy request`() {
        val intent = legacyDefaultDialerIntent()
        val delegate = mock<DefaultPhoneManager> {
            on { createRequestDefaultDialerIntent() } doReturn intent
        }
        val packageManager = mock<PackageManager> {
            on { hasSystemFeature(PackageManager.FEATURE_TELEPHONY) } doReturn true
            on { resolveActivity(intent, 0) } doReturn mock<ResolveInfo>()
        }

        val manager = SafeDefaultPhoneManager(delegate, packageManager, logger = {})

        assertSame(intent, manager.createRequestDefaultDialerIntent())
    }

    private fun legacyDefaultDialerIntent(): Intent = mock {
        on { action } doReturn TelecomManager.ACTION_CHANGE_DEFAULT_DIALER
    }
}
