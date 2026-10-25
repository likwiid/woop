// SPDX-License-Identifier: GPL-3.0-or-later

package com.likwiid.woop

import com.likwiid.woop.reminders.Schedule
import com.likwiid.woop.ui.create.CreateWoopViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.Locale

class ScheduleTest {
    private val zone = ZoneId.of("Europe/Paris")

    // 2026-10-21 is a Wednesday.
    private fun at(day: Int, hour: Int, minute: Int = 0, month: Int = 10) = ZonedDateTime.of(2026, month, day, hour, minute, 0, 0, zone)

    @Test
    fun promptFiresLaterTodayWhenTimeIsAhead() {
        assertEquals(at(21, 8), Schedule.nextPrompt(at(21, 7, 30), 8, 0, Schedule.ALL_DAYS))
    }

    @Test
    fun promptRollsToTomorrowOncePassed() {
        assertEquals(at(22, 8), Schedule.nextPrompt(at(21, 8), 8, 0, Schedule.ALL_DAYS))
    }

    @Test
    fun weekdayPromptSkipsTheWeekend() {
        // Friday 23rd after 7:30 → Monday 26th.
        assertEquals(at(26, 7, 30), Schedule.nextPrompt(at(23, 9), 7, 30, Schedule.WEEKDAYS))
    }

    @Test
    fun weeklyPromptFindsNextSunday() {
        val sunday = Schedule.dayBit(DayOfWeek.SUNDAY)
        assertEquals(at(25, 19), Schedule.nextPrompt(at(21, 12), 19, 0, sunday))
        // On Sunday after the time it waits a full week.
        assertEquals(ZonedDateTime.of(2026, 11, 1, 19, 0, 0, 0, zone), Schedule.nextPrompt(at(25, 20), 19, 0, sunday))
    }

    @Test
    fun promptKeepsWallClockTimeAcrossDstChange() {
        // Clocks go back in Paris on 2026-10-25; the prompt should still fire at 08:00 local.
        val next = Schedule.nextPrompt(at(24, 9), 8, 0, Schedule.ALL_DAYS)!!
        assertEquals(at(25, 8), next)
        assertEquals(3600, next.offset.totalSeconds)
    }

    @Test
    fun promptWithNoDaysNeverFires() {
        assertNull(Schedule.nextPrompt(at(21, 7), 8, 0, 0))
    }

    @Test
    fun dailyCheckInLandsSameEveningWhenThereIsTime() {
        assertEquals(at(21, 19), Schedule.firstCheckIn(at(21, 9), 1, 19, 0))
    }

    @Test
    fun dailyCheckInWaitsUntilTomorrowWhenTooSoon() {
        // Created at 17:30, a 19:00 check-in would be under 3 hours later.
        assertEquals(at(22, 19), Schedule.firstCheckIn(at(21, 17, 30), 1, 19, 0))
    }

    @Test
    fun multiDayCheckInCountsCalendarDays() {
        assertEquals(at(24, 19), Schedule.firstCheckIn(at(21, 23), 3, 19, 0))
    }

    @Test
    fun nextCheckInSkipsMissedOccurrences() {
        // Due on the 24th; asked on the 26th → 27th.
        assertEquals(at(27, 19), Schedule.nextCheckIn(at(26, 12), at(21, 10), 3, 19, 0))
    }

    @Test
    fun checkInsOffNeverFire() {
        assertNull(Schedule.nextCheckIn(at(21, 10), at(21, 9), 0, 19, 0))
        assertFalse(Schedule.isCheckInDue(at(30, 10), at(21, 9), 0, 19, 0))
    }

    @Test
    fun checkInDueOnceTimeHasPassed() {
        assertFalse(Schedule.isCheckInDue(at(24, 18, 59), at(21, 10), 3, 19, 0))
        assertTrue(Schedule.isCheckInDue(at(24, 19), at(21, 10), 3, 19, 0))
    }

    @Test
    fun describesCommonDaySets() {
        assertEquals("Every day", Schedule.describeDays(Schedule.ALL_DAYS))
        assertEquals("Weekdays", Schedule.describeDays(Schedule.WEEKDAYS))
        assertEquals("Weekends", Schedule.describeDays(Schedule.WEEKENDS))
        assertEquals("Every Sunday", Schedule.describeDays(Schedule.dayBit(DayOfWeek.SUNDAY), Locale.US))
    }

    @Test
    fun describesRelativeDays() {
        val now = at(21, 10)
        assertEquals("today", Schedule.describeDay(at(21, 19), now))
        assertEquals("tomorrow", Schedule.describeDay(at(22, 8), now))
    }

    @Test
    fun cueComesFromObstacleWording() {
        assertEquals("I feel too tired after work", CreateWoopViewModel.cueFrom("I'll feel too tired after work."))
        assertEquals("I get distracted by my phone", CreateWoopViewModel.cueFrom("I will get distracted by my phone"))
        assertEquals("my inbox pulls me in", CreateWoopViewModel.cueFrom("my inbox pulls me in!"))
    }
}
