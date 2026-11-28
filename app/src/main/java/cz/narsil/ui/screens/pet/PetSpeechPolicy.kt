package cz.narsil.ui.screens.pet

import android.content.Context
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

/** Jednoduché úložiště pro paměť sovy (oddělené kvůli unit testům bez Androidu). */
interface PetMemoryStore {
    fun getInt(key: String, def: Int): Int
    fun putInt(key: String, v: Int)
    fun getLong(key: String, def: Long): Long
    fun putLong(key: String, v: Long)
}

/** Implementace [PetMemoryStore] nad SharedPreferences `"narsil_pet"`. */
class SharedPrefsPetMemoryStore(context: Context) : PetMemoryStore {
    private val prefs = context.applicationContext.getSharedPreferences("narsil_pet", Context.MODE_PRIVATE)

    override fun getInt(key: String, def: Int): Int = prefs.getInt(key, def)
    override fun putInt(key: String, v: Int) {
        prefs.edit().putInt(key, v).apply()
    }

    override fun getLong(key: String, def: Long): Long = prefs.getLong(key, def)
    override fun putLong(key: String, v: Long) {
        prefs.edit().putLong(key, v).apply()
    }
}

/** Epoch day posunutý o −4 h podle SPEC 1 (den začíná ve 4:00 ráno). */
fun petDayKey(millis: Long, zone: ZoneId = ZoneId.systemDefault()): Long =
    LocalDateTime.ofInstant(Instant.ofEpochMilli(millis), zone)
        .minusHours(4L)
        .toLocalDate()
        .toEpochDay()

/**
 * Hlídá, aby sova nebyla otravná: max [maxUnsolicitedPerDay] samovolných hlášek za den
 * a možnost ztlumení „Teď ne“ do zítřka.
 */
class PetSpeechPolicy(
    private val store: PetMemoryStore,
    private val maxUnsolicitedPerDay: Int = 2,
) {
    fun canSpeakUnprompted(dayKey: Long): Boolean {
        if (isMuted(dayKey)) return false
        val savedDay = store.getLong(KEY_SPOKEN_DAY, Long.MIN_VALUE)
        if (savedDay != dayKey) return maxUnsolicitedPerDay > 0
        return store.getInt(KEY_SPOKEN_COUNT, 0) < maxUnsolicitedPerDay
    }

    fun markSpoken(dayKey: Long) {
        val savedDay = store.getLong(KEY_SPOKEN_DAY, Long.MIN_VALUE)
        val nextCount = if (savedDay != dayKey) 1 else store.getInt(KEY_SPOKEN_COUNT, 0) + 1
        store.putLong(KEY_SPOKEN_DAY, dayKey)
        store.putInt(KEY_SPOKEN_COUNT, nextCount)
    }

    fun muteUntilTomorrow(dayKey: Long) {
        store.putLong(KEY_MUTED_DAY, dayKey)
    }

    fun unmute() {
        store.putLong(KEY_MUTED_DAY, Long.MIN_VALUE)
    }

    fun isMuted(dayKey: Long): Boolean =
        store.getLong(KEY_MUTED_DAY, Long.MIN_VALUE) == dayKey

    private companion object {
        const val KEY_SPOKEN_DAY = "spoken_day"
        const val KEY_SPOKEN_COUNT = "spoken_count"
        const val KEY_MUTED_DAY = "muted_day"
    }
}
