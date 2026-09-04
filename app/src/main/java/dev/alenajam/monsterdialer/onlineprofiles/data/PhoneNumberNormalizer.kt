package dev.alenajam.monsterdialer.onlineprofiles.data

import android.telephony.TelephonyManager
import com.google.i18n.phonenumbers.PhoneNumberUtil
import java.util.Locale

/** Provides the only key format used for local phone-to-profile associations. */
interface PhoneNumberNormalizer {
    fun toE164OrNull(number: String): String?
}

class LibPhoneNumberNormalizer(
    private val defaultRegion: () -> String?,
    private val phoneNumberUtil: PhoneNumberUtil = PhoneNumberUtil.getInstance(),
) : PhoneNumberNormalizer {
    override fun toE164OrNull(number: String): String? = runCatching {
        val trimmed = number.trim()
        if (trimmed.isBlank()) return null
        val region = if (trimmed.startsWith('+')) null else defaultRegion()
        val parsed = phoneNumberUtil.parse(trimmed, region)
        if (!phoneNumberUtil.isValidNumber(parsed)) return null
        phoneNumberUtil.format(parsed, PhoneNumberUtil.PhoneNumberFormat.E164)
    }.getOrNull()

    companion object {
        fun regionProvider(telephonyManager: TelephonyManager): () -> String? = {
            telephonyManager.networkCountryIso.takeIf { it.length == 2 }?.uppercase(Locale.ROOT)
                ?: telephonyManager.simCountryIso.takeIf { it.length == 2 }?.uppercase(Locale.ROOT)
                ?: Locale.getDefault().country.takeIf { it.length == 2 }?.uppercase(Locale.ROOT)
        }
    }
}
