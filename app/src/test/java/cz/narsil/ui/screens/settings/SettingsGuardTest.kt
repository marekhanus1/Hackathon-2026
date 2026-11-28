package cz.narsil.ui.screens.settings

import cz.narsil.core.Exercise
import cz.narsil.core.LlmProvider
import cz.narsil.core.Settings
import cz.narsil.core.TrackedApp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsGuardTest {
    private val d = Settings()

    @Test
    fun stricterChangesAreNotLoosening() {
        listOf(
            d.copy(toleranceMinutes = 3),
            d.copy(videoChainLimit = 2),
            d.copy(squatsPerUnlock = 30),
            d.copy(pushupsPerUnlock = 15),
            d.copy(debtLimitMinutes = 0),
            d.copy(exercise = Exercise.PUSHUPS),
            d.copy(llmProvider = LlmProvider.CLAUDE),
        ).forEach { assertTrue("$it", SettingsGuard.loosenings(d, it).isEmpty()) }
        // přidání aplikace a obnovení ochrany jsou zpřísnění
        val fewer = d.copy(trackedApps = setOf(TrackedApp.YOUTUBE), protectionPausedUntilMillis = 9L, partnerAlerts = false)
        assertTrue(SettingsGuard.loosenings(fewer, d).isEmpty())
    }

    @Test
    fun looseningChangesAreDescribed() {
        assertEquals(1, SettingsGuard.loosenings(d, d.copy(toleranceMinutes = 10)).size)
        assertEquals(1, SettingsGuard.loosenings(d, d.copy(videoChainLimit = 5)).size)
        assertEquals(1, SettingsGuard.loosenings(d, d.copy(squatsPerUnlock = 10)).size)
        assertEquals(1, SettingsGuard.loosenings(d, d.copy(pushupsPerUnlock = 5)).size)
        assertEquals(1, SettingsGuard.loosenings(d, d.copy(debtLimitMinutes = 60)).size)
        assertEquals(1, SettingsGuard.loosenings(d, d.copy(protectionPausedUntilMillis = 1L)).size)
        assertEquals(1, SettingsGuard.loosenings(d, d.copy(partnerAlerts = false)).size)
        val removed = SettingsGuard.loosenings(d, d.copy(trackedApps = d.trackedApps - TrackedApp.TIKTOK))
        assertEquals(listOf("Narsil přestane sledovat: TikTok."), removed)
        assertTrue(SettingsGuard.loosenings(d, d.copy(debtLimitMinutes = 45)).single().contains("45 min místo 30 min"))
    }

    @Test
    fun resetSectionTouchesOnlyThatSection() {
        val custom = d.copy(
            toleranceMinutes = 15, videoChainLimit = 5, trackedApps = emptySet(), squatsPerUnlock = 10,
            exercise = Exercise.PUSHUPS, llmProvider = LlmProvider.OPENAI, partnerAlerts = false,
            homeSsid = "Domov", demoThresholdSec = 20, protectionPausedUntilMillis = 7L,
        )
        val p = SettingsGuard.resetSection(custom, SettingsSection.PROTECTION)
        assertEquals(5, p.toleranceMinutes)
        assertEquals(3, p.videoChainLimit)
        assertEquals(10, p.squatsPerUnlock)
        assertEquals("Domov", p.homeSsid)
        assertEquals(20, p.demoThresholdSec) // demo panel se resetem nastavení nemění
        assertEquals(7L, p.protectionPausedUntilMillis) // pozastavení má vlastní tlačítko
        assertEquals(d.trackedApps, SettingsGuard.resetSection(custom, SettingsSection.APPS).trackedApps)
        val m = SettingsGuard.resetSection(custom, SettingsSection.MOVEMENT)
        assertEquals(Exercise.SQUATS, m.exercise)
        assertEquals(20, m.squatsPerUnlock)
        assertEquals(LlmProvider.GEMINI, SettingsGuard.resetSection(custom, SettingsSection.AI).llmProvider)
        assertTrue(SettingsGuard.resetSection(custom, SettingsSection.NOTIFICATIONS).partnerAlerts)
    }

    @Test
    fun isModified() {
        SettingsSection.entries.forEach { assertFalse(SettingsGuard.isModified(d, it)) }
        assertTrue(SettingsGuard.isModified(d.copy(toleranceMinutes = 3), SettingsSection.PROTECTION))
        assertFalse(SettingsGuard.isModified(d.copy(toleranceMinutes = 3), SettingsSection.MOVEMENT))
    }
}
