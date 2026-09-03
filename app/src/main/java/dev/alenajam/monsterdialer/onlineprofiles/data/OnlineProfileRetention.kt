package dev.alenajam.monsterdialer.onlineprofiles.data

private const val MillisPerDay = 24L * 60 * 60 * 1000
private const val ConfirmationWarningDays = 330L

data class OnlineProfileRetention(
    val confirmedAtEpochMillis: Long,
)

fun OnlineProfileRetention.needsConfirmation(nowEpochMillis: Long): Boolean =
    nowEpochMillis - confirmedAtEpochMillis >= ConfirmationWarningDays * MillisPerDay
