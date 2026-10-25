// SPDX-License-Identifier: GPL-3.0-or-later

package com.likwiid.woop.reminders

import java.time.DayOfWeek
import java.time.ZonedDateTime
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale

/** Pure time maths for reminders, kept free of Android types so it can be unit tested. */
object Schedule {
    const val ALL_DAYS = 0b1111111
    const val WEEKDAYS = 0b0011111
    const val WEEKENDS = 0b1100000

    fun dayBit(day: DayOfWeek): Int = 1 shl (day.value - 1)

    /** Next time a "write a WOOP" prompt should fire, strictly after [from]. */
    fun nextPrompt(from: ZonedDateTime, hour: Int, minute: Int, daysMask: Int): ZonedDateTime? {
        if (daysMask and ALL_DAYS == 0) return null
        for (offset in 0L..7L) {
            val date = from.toLocalDate().plusDays(offset)
            if (daysMask and dayBit(date.dayOfWeek) == 0) continue
            val candidate = date.atTime(hour, minute).atZone(from.zone)
            if (candidate.isAfter(from)) return candidate
        }
        return null
    }

    /**
     * First check-in after [base] (creation or last review). Daily check-ins land the same
     * evening when there is enough time left; longer cadences land N days later.
     */
    fun firstCheckIn(base: ZonedDateTime, everyDays: Int, hour: Int, minute: Int): ZonedDateTime {
        if (everyDays == 1) {
            val sameDay = base.toLocalDate().atTime(hour, minute).atZone(base.zone)
            return if (sameDay.isAfter(base.plusHours(3))) sameDay else sameDay.plusDays(1)
        }
        return base.toLocalDate().plusDays(everyDays.toLong()).atTime(hour, minute).atZone(base.zone)
    }

    fun nextCheckIn(from: ZonedDateTime, base: ZonedDateTime, everyDays: Int, hour: Int, minute: Int): ZonedDateTime? {
        if (everyDays <= 0) return null
        var t = firstCheckIn(base, everyDays, hour, minute)
        while (!t.isAfter(from)) t = t.plusDays(everyDays.toLong())
        return t
    }

    fun isCheckInDue(now: ZonedDateTime, base: ZonedDateTime, everyDays: Int, hour: Int, minute: Int): Boolean =
        everyDays > 0 && !firstCheckIn(base, everyDays, hour, minute).isAfter(now)

    fun describeDays(mask: Int, locale: Locale = Locale.getDefault()): String {
        val days = DayOfWeek.entries.filter { mask and dayBit(it) != 0 }
        return when (mask and ALL_DAYS) {
            ALL_DAYS -> "Every day"
            WEEKDAYS -> "Weekdays"
            WEEKENDS -> "Weekends"
            0 -> "No days picked"
            else -> if (days.size == 1) {
                "Every " + days[0].getDisplayName(TextStyle.FULL, locale)
            } else {
                days.joinToString(", ") { it.getDisplayName(TextStyle.SHORT, locale) }
            }
        }
    }

    fun describeCadence(everyDays: Int): String = when (everyDays) {
        0 -> "Off"
        1 -> "Every day"
        7 -> "Every week"
        14 -> "Every 2 weeks"
        else -> "Every $everyDays days"
    }

    /** "today", "tomorrow", "Wed", or "Oct 30" relative to [now]. */
    fun describeDay(t: ZonedDateTime, now: ZonedDateTime, locale: Locale = Locale.getDefault()): String {
        val days = ChronoUnit.DAYS.between(now.toLocalDate(), t.toLocalDate())
        return when {
            days == 0L -> "today"
            days == 1L -> "tomorrow"
            days in 2..6 -> t.dayOfWeek.getDisplayName(TextStyle.FULL, locale)
            else -> "${t.month.getDisplayName(TextStyle.SHORT, locale)} ${t.dayOfMonth}"
        }
    }
}
