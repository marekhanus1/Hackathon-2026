package cz.narsil.app.demo

import cz.narsil.gamification.GamificationRules
import cz.narsil.gamification.XpSource
import java.io.File
import java.time.LocalDate
import java.time.ZoneOffset
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** 138: generátor v APK dává stejná data jako mockdata/generuj.py (mirror.json, value_map.json). */
class MockDataGeneratorTest {
    private fun repoFile(name: String): File =
        listOf(File("../mockdata/$name"), File("mockdata/$name")).first { it.exists() }

    @Test
    fun matchesPythonMirrorJson() {
        val mirror = Json.parseToJsonElement(repoFile("mirror.json").readText()).jsonObject
        val expected = mirror.getValue("days").jsonArray.map { it.jsonObject }
        val days = MockDataGenerator.days()
        assertEquals(expected.size, days.size)
        expected.zip(days).forEach { (e, d) ->
            val i = d.index
            assertEquals("datum $i", e.getValue("date").jsonPrimitive.content, d.date.toString())
            assertEquals("feed $i", e.getValue("feed_minutes").jsonPrimitive.int, d.feedMinutes)
            assertEquals("reclaimed $i", e.getValue("reclaimed_minutes").jsonPrimitive.int, d.reclaimedMinutes)
            assertEquals("planned $i", e.getValue("planned_minutes").jsonPrimitive.int, d.plannedMinutes)
            assertEquals("by_app $i", e.getValue("by_app").jsonObject.mapValues { it.value.jsonPrimitive.int }, d.byApp)
            assertEquals("steps $i", e.getValue("steps").jsonPrimitive.int, d.steps)
            assertEquals("squats $i", e.getValue("squats").jsonPrimitive.int, d.squats)
            assertEquals("smlouva $i", e.getValue("contract_fulfilled").jsonPrimitive.booleanOrNull, d.contractFulfilled)
            assertEquals("souboj $i", e.getValue("battle_won").jsonPrimitive.booleanOrNull, d.battleWon)
            assertEquals("xp $i", e.getValue("xp_gained").jsonPrimitive.int, d.xpGained)
            val ints = e.getValue("interventions").jsonArray.map { it.jsonObject }
            assertEquals(
                "zásahy $i",
                ints.map { listOf(it.getValue("time").jsonPrimitive.content, it.getValue("app").jsonPrimitive.content, it.getValue("outcome").jsonPrimitive.content, it["avoiding"]?.jsonPrimitive?.contentOrNull) },
                d.interventions.map { listOf(it.time, it.app, it.outcome, it.avoiding) },
            )
        }
        val vm = Json.parseToJsonElement(repoFile("value_map.json").readText()).jsonObject
        assertEquals(
            vm.getValue("ratings").jsonArray.map { it.jsonObject.getValue("title").jsonPrimitive.content },
            MockDataGenerator.RATINGS.map { it.title },
        )
    }

    /** Konec dat = začátek dema (DemoTuning, 230): XP těsně pod úrovní. */
    @Test
    fun endsJustBelowLevel() {
        val total = MockDataGenerator.days().sumOf { it.xpGained }
        assertEquals(480, total % GamificationRules.XP_PER_LEVEL)
    }

    @Test
    fun rowsAreShiftedToYesterdayAndMatchXp() {
        val today = LocalDate.of(2026, 12, 5)
        val rows = MockDataLoader.rows(today, ZoneOffset.UTC)
        val dates = rows.xp.map { LocalDate.parse(it.date) }
        assertEquals(today.minusDays(1), dates.max())
        assertEquals(today.minusDays(21), dates.min())
        assertEquals(MockDataGenerator.days().sumOf { it.xpGained }, rows.xp.sumOf { it.xp })
        assertEquals(MockDataGenerator.days().sumOf { it.feedMinutes } * 60_000L, rows.consumption.sumOf { it.durationMillis })
        assertEquals(MockDataGenerator.RATINGS.size, rows.ratings.size)
        assertEquals(MockDataGenerator.days().sumOf { it.interventions.size }, rows.interventions.size)
        assertEquals(1, rows.xp.count { it.source == XpSource.JOKER })
        assertTrue(rows.interventions.any { it.packageName == "org.mozilla.firefox" })
        assertTrue(rows.consumption.all { it.context == "HOME" && !it.planned })
    }
}
