package cz.narsil.core.data

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/** Den aplikace začíná ve 4:00 (SPEC 1). Sdílí všechny moduly. */
object DayKey {
    const val DAY_START_HOUR = 4L

    /** Den, do kterého patří okamžik [millis]. */
    fun of(millis: Long, zone: ZoneId = ZoneId.systemDefault()): LocalDate =
        LocalDateTime.ofInstant(Instant.ofEpochMilli(millis), zone).minusHours(DAY_START_HOUR).toLocalDate()

    fun today(zone: ZoneId = ZoneId.systemDefault()): LocalDate = of(System.currentTimeMillis(), zone)

    /** Začátek dne [date] (4:00) v epoch ms. */
    fun startMillis(date: LocalDate, zone: ZoneId = ZoneId.systemDefault()): Long =
        date.atStartOfDay(zone).plusHours(DAY_START_HOUR).toInstant().toEpochMilli()

    /**
     * Pro který den platí smlouva uložená v [millis]: 17:00–3:59 → následující den, jinak dnešní (SPEC 1).
     * [contractToday] = demo přepínač „smlouva platí hned“.
     */
    fun contractDateFor(millis: Long, contractToday: Boolean = false, zone: ZoneId = ZoneId.systemDefault()): LocalDate {
        val day = of(millis, zone)
        if (contractToday) return day
        val hour = LocalDateTime.ofInstant(Instant.ofEpochMilli(millis), zone).hour
        return if (hour >= 17 || hour < DAY_START_HOUR) day.plusDays(1) else day
    }
}
