package dev.alenajam.monsterdialer.contacts.ui

import android.telephony.PhoneNumberUtils
import java.util.Locale

fun formatPhoneNumber(number: String, locale: Locale): String =
    PhoneNumberUtils.formatNumber(number, locale.country) ?: number
