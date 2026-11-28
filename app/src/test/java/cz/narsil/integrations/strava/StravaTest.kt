package cz.narsil.integrations.strava

import cz.narsil.core.StravaRepository
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StravaTest {
    private val now = Instant.parse("2026-11-28T10:00:00Z")
    private val clock = object : Clock() {
        override fun getZone(): ZoneId = ZoneOffset.UTC
        override fun withZone(zone: ZoneId): Clock = this
        override fun instant(): Instant = now
    }
    private val config = StravaConfig("123", "tajne")

    private class FakeHttp : StravaHttp {
        val posts = mutableListOf<Map<String, String>>()
        val gets = mutableListOf<Pair<String, String>>()
        var tokenReply: String? = """{"access_token":"A2","refresh_token":"R2","expires_at":1900000000}"""
        var activitiesReply: String? = """[
            {"id":1,"type":"Run","sport_type":"TrailRun","start_date":"2026-11-28T06:30:00Z","moving_time":1700,"distance":5200.5},
            {"id":2,"type":"Walk","sport_type":"Walk","start_date":"2026-11-28T03:00:00Z","moving_time":2000,"distance":2000},
            {"id":3,"type":"Swim","sport_type":"Swim","start_date":"2026-11-28T08:00:00Z","moving_time":1800,"distance":1000}
        ]"""
        override suspend fun postForm(url: String, form: Map<String, String>): String? { posts += form; return tokenReply }
        override suspend fun get(url: String, bearer: String): String? { gets += url to bearer; return activitiesReply }
    }

    @Test
    fun authorizeUrlAndRedirect() {
        val url = StravaApi.authorizeUrl(config)
        assertTrue(url.startsWith("https://www.strava.com/oauth/mobile/authorize?client_id=123"))
        assertTrue(url.contains("redirect_uri=narsil%3A%2F%2Fstrava") && url.contains("scope=activity%3Aread"))
        assertEquals("abc", StravaApi.codeFromRedirect("narsil://strava?state=&code=abc&scope=read,activity:read"))
        assertNull(StravaApi.codeFromRedirect("narsil://strava?error=access_denied"))
        assertNull(StravaApi.codeFromRedirect("narsil://strava"))
        assertNull(StravaApi.codeFromRedirect("narsil://strava?code=abc&scope=read")) // bez přístupu k aktivitám
        assertEquals("abc", StravaApi.codeFromRedirect("narsil://strava?code=abc&scope=read,activity:read_all"))
    }

    @Test
    fun connectAndReadTodayActivities() = runTest {
        val http = FakeHttp()
        val store = InMemoryStravaTokenStore()
        val repo = HttpStravaRepository(config, store, http, clock)
        assertFalse(repo.isConnected.value)
        assertTrue(repo.todayActivities().isEmpty())

        assertTrue(repo.handleRedirect("narsil://strava?code=kod"))
        assertEquals("authorization_code", http.posts.single()["grant_type"])
        assertTrue(repo.isConnected.value)

        val acts = repo.todayActivities()
        // aktivita ve 3:00 je ze včerejška (den od 4:00)
        assertEquals(listOf(1L, 3L), acts.map { it.id })
        assertEquals("Run", acts.first().type) // TrailRun → Run
        val dayStartSec = Instant.parse("2026-11-28T04:00:00Z").epochSecond
        assertTrue(http.gets.single().first.contains("after=$dayStartSec"))
        assertEquals("A2", http.gets.single().second)
        // pohybový úkol: běh 28 min splní, plavání ne
        assertTrue(acts.any { it.type in StravaRepository.MOVEMENT_TYPES && it.movingTimeSec >= StravaRepository.MIN_MOVING_TIME_SEC })
    }

    @Test
    fun expiredTokenIsRefreshed() = runTest {
        val http = FakeHttp()
        val store = InMemoryStravaTokenStore(StravaToken("A1", "R1", expiresAtSec = now.epochSecond - 10))
        val repo = HttpStravaRepository(config, store, http, clock)
        repo.todayActivities()
        assertEquals("refresh_token", http.posts.single()["grant_type"])
        assertEquals("R1", http.posts.single()["refresh_token"])
        assertEquals("A2", store.load()!!.accessToken)
    }

    @Test
    fun errorsGiveEmptyList() = runTest {
        val http = FakeHttp().apply { activitiesReply = null }
        val repo = HttpStravaRepository(config, InMemoryStravaTokenStore(StravaToken("A", "R", 1_900_000_000)), http, clock)
        assertTrue(repo.todayActivities().isEmpty())
        assertTrue(StravaApi.parseActivities("není json").isEmpty())
        http.tokenReply = null
        assertFalse(repo.handleRedirect("narsil://strava?code=x"))
    }

    @Test
    fun disconnect() {
        val store = InMemoryStravaTokenStore(StravaToken("A", "R", 1))
        val repo = HttpStravaRepository(config, store, FakeHttp(), clock)
        assertTrue(repo.isConnected.value)
        repo.disconnect()
        assertFalse(repo.isConnected.value)
        assertNull(store.load())
    }
}
