package com.example.communityvoice.data

import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Small text helpers shared by every screen.
 *
 * Timestamps arrive from the backend as ISO-8601 text, like
 * "2026-10-08T09:15:00Z". java.time is only available from Android 8 (API 26)
 * and this app supports API 24, so rather than switch on desugaring for one
 * feature we parse the handful of characters we actually need.
 */
object Formatters {

    private val MONTHS = listOf(
        "Jan", "Feb", "Mar", "Apr", "May", "Jun",
        "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
    )

    /**
     * "just now", "14m ago", "3h ago", "2d ago", or a date once it is over a
     * week old - which is how long anybody reads a relative time as useful.
     */
    fun relativeTime(isoTimestamp: String, nowMillis: Long = System.currentTimeMillis()): String {
        val then = parseEpochMillis(isoTimestamp) ?: return shortDate(isoTimestamp)
        val elapsed = nowMillis - then

        // A clock skew between phone and server can make this slightly
        // negative. Treat anything in the future as "just now".
        if (elapsed < TimeUnit.MINUTES.toMillis(1)) return "just now"

        val minutes = TimeUnit.MILLISECONDS.toMinutes(elapsed)
        if (minutes < 60) return "${minutes}m ago"

        val hours = TimeUnit.MILLISECONDS.toHours(elapsed)
        if (hours < 24) return "${hours}h ago"

        val days = TimeUnit.MILLISECONDS.toDays(elapsed)
        if (days < 7) return "${days}d ago"

        return shortDate(isoTimestamp)
    }

    /** "2026-10-08T09:15:00Z" -> "8 Oct 2026". */
    fun shortDate(isoTimestamp: String): String {
        if (isoTimestamp.length < 10) return isoTimestamp
        val year = isoTimestamp.substring(0, 4)
        val month = isoTimestamp.substring(5, 7).toIntOrNull() ?: return isoTimestamp
        val day = isoTimestamp.substring(8, 10).toIntOrNull() ?: return isoTimestamp
        val monthName = MONTHS.getOrNull(month - 1) ?: return isoTimestamp
        return "$day $monthName $year"
    }

    /** "October 2026", for "Member since ...". */
    fun monthAndYear(isoTimestamp: String): String {
        if (isoTimestamp.length < 7) return isoTimestamp
        val year = isoTimestamp.substring(0, 4)
        val month = isoTimestamp.substring(5, 7).toIntOrNull() ?: return isoTimestamp
        val monthName = MONTHS.getOrNull(month - 1) ?: return isoTimestamp
        return "$monthName $year"
    }

    /** 1 -> "1", 1200 -> "1.2k", 45000 -> "45k". Keeps vote pills narrow. */
    fun compactCount(value: Int): String = when {
        value < 1_000 -> value.toString()
        value < 10_000 -> String.format(Locale.US, "%.1fk", value / 1000.0).replace(".0k", "k")
        else -> "${value / 1000}k"
    }

    fun compactCount(value: Long): String = compactCount(value.coerceAtMost(Int.MAX_VALUE.toLong()).toInt())

    /** "1 person supports this" / "12 people support this". */
    fun supportSentence(count: Int): String =
        if (count == 1) "1 person supports this" else "$count people support this"

    /**
     * Reads the leading "yyyy-MM-ddTHH:mm:ss" of an ISO timestamp as UTC
     * milliseconds. Returns null for anything that does not look like one.
     *
     * Fractional seconds and the trailing "Z" are ignored - neither changes a
     * relative time by anything a person would notice.
     */
    private fun parseEpochMillis(isoTimestamp: String): Long? {
        if (isoTimestamp.length < 19) return null
        return runCatching {
            val year = isoTimestamp.substring(0, 4).toInt()
            val month = isoTimestamp.substring(5, 7).toInt()
            val day = isoTimestamp.substring(8, 10).toInt()
            val hour = isoTimestamp.substring(11, 13).toInt()
            val minute = isoTimestamp.substring(14, 16).toInt()
            val second = isoTimestamp.substring(17, 19).toInt()

            val calendar = java.util.Calendar.getInstance(
                java.util.TimeZone.getTimeZone("UTC")
            ).apply {
                clear()
                set(year, month - 1, day, hour, minute, second)
            }
            calendar.timeInMillis
        }.getOrNull()
    }
}
