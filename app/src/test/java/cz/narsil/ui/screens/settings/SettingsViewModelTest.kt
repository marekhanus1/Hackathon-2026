package cz.narsil.ui.screens.settings

import cz.narsil.core.ComfortContext
import cz.narsil.core.Exercise
import cz.narsil.core.LlmProvider
import cz.narsil.core.Settings
import cz.narsil.core.TrackedApp
import cz.narsil.core.fake.FakeContextRepository
import cz.narsil.core.fake.FakeSettingsRepository
import cz.narsil.core.fake.FakeStravaRepository
import java.time.LocalDateTime
import java.time.ZoneOffset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val zone = ZoneOffset.UTC
    private val now = LocalDateTime.of(2026, 11, 28, 22, 30).toInstant(zone).toEpochMilli()

    private lateinit var settings: FakeSettingsRepository
    private lateinit var context: FakeContextRepository
    private var partnerPings = 0

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        settings = FakeSettingsRepository()
        context = FakeContextRepository()
        partnerPings = 0
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun vm(strava: kotlinx.coroutines.flow.Flow<StravaCardState>? = null) = SettingsViewModel(
        settings, context, FakeStravaRepository(connected = false), strava,
        llmKeys = mapOf(LlmProvider.GEMINI to true, LlmProvider.CLAUDE to false, LlmProvider.OPENAI to false),
        clock = { now }, zone = zone, onProtectionPaused = { partnerPings++ },
    )

    /** Spustí sběr uiState (WhileSubscribed) po dobu testu. */
    private fun TestScope.collect(vm: SettingsViewModel) = backgroundScope.launch(dispatcher) { vm.uiState.collect {} }

    @Test
    fun stricterChangeAppliesImmediately() = runTest(dispatcher) {
        val vm = vm(); collect(vm)
        vm.setToleranceMinutes(3)
        vm.setSquatsPerUnlock(30)
        vm.setExercise(Exercise.PUSHUPS)
        vm.selectLlmProvider(LlmProvider.CLAUDE)
        runCurrent()
        assertEquals(3, settings.settings.value.toleranceMinutes)
        assertEquals(30, settings.settings.value.squatsPerUnlock)
        assertEquals(Exercise.PUSHUPS, settings.settings.value.exercise)
        assertEquals(LlmProvider.CLAUDE, vm.uiState.value.settings.llmProvider)
        assertNull(vm.uiState.value.pendingChange)
    }

    @Test
    fun looseningWaitsForConfirmationAfterCountdown() = runTest(dispatcher) {
        val vm = vm(); collect(vm)
        vm.setDebtLimitMinutes(60)
        runCurrent()
        assertEquals(30, settings.settings.value.debtLimitMinutes)
        val pending = vm.uiState.value.pendingChange
        assertNotNull(pending)
        assertEquals(SettingsGuard.CONFIRM_DELAY_SECONDS, pending!!.secondsLeft)
        assertFalse(pending.canConfirm)

        // potvrzení před koncem odpočtu nic neudělá
        vm.confirmPendingChange()
        runCurrent()
        assertEquals(30, settings.settings.value.debtLimitMinutes)

        advanceTimeBy(SettingsGuard.CONFIRM_DELAY_SECONDS * 1_000L + 1)
        runCurrent()
        assertTrue(vm.uiState.value.pendingChange!!.canConfirm)
        vm.confirmPendingChange()
        runCurrent()
        assertEquals(60, settings.settings.value.debtLimitMinutes)
        assertNull(vm.uiState.value.pendingChange)
    }

    @Test
    fun cancelKeepsSettings() = runTest(dispatcher) {
        val vm = vm(); collect(vm)
        vm.setAppTracked(TrackedApp.TIKTOK, false)
        runCurrent()
        assertNotNull(vm.uiState.value.pendingChange)
        vm.cancelPendingChange()
        advanceTimeBy(20_000)
        runCurrent()
        assertNull(vm.uiState.value.pendingChange)
        assertTrue(TrackedApp.TIKTOK in settings.settings.value.trackedApps)
        // znovu zapnout je zpřísnění → hned
        settings.update { it.copy(trackedApps = it.trackedApps - TrackedApp.TIKTOK) }
        vm.setAppTracked(TrackedApp.TIKTOK, true)
        runCurrent()
        assertTrue(TrackedApp.TIKTOK in settings.settings.value.trackedApps)
    }

    @Test
    fun pauseUntilFourAmNotifiesPartnerAndResumeIsImmediate() = runTest(dispatcher) {
        val vm = vm(); collect(vm)
        vm.pauseProtectionUntilMorning()
        runCurrent()
        assertNull(settings.settings.value.protectionPausedUntilMillis)
        advanceTimeBy(SettingsGuard.CONFIRM_DELAY_SECONDS * 1_000L + 1)
        vm.confirmPendingChange()
        runCurrent()
        val fourAm = LocalDateTime.of(2026, 11, 29, 4, 0).toInstant(zone).toEpochMilli()
        assertEquals(fourAm, settings.settings.value.protectionPausedUntilMillis)
        assertEquals(1, partnerPings)
        assertTrue(vm.uiState.value.isProtectionPaused)

        vm.resumeProtection()
        runCurrent()
        assertNull(settings.settings.value.protectionPausedUntilMillis)
        assertFalse(vm.uiState.value.isProtectionPaused)
    }

    @Test
    fun pauseWithoutPartnerAlertsSendsNothing() = runTest(dispatcher) {
        settings.update { it.copy(partnerAlerts = false) }
        val vm = vm(); collect(vm)
        vm.pauseProtectionUntilMorning()
        advanceTimeBy(SettingsGuard.CONFIRM_DELAY_SECONDS * 1_000L + 1)
        vm.confirmPendingChange()
        runCurrent()
        assertNotNull(settings.settings.value.protectionPausedUntilMillis)
        assertEquals(0, partnerPings)
    }

    @Test
    fun resetSectionRestoresDefaultsWithGuard() = runTest(dispatcher) {
        // přísnější než výchozí → reset je uvolnění a chce potvrzení
        settings.update { it.copy(toleranceMinutes = 3, videoChainLimit = 2) }
        val vm = vm(); collect(vm)
        vm.resetSection(SettingsSection.PROTECTION)
        runCurrent()
        assertNotNull(vm.uiState.value.pendingChange)
        advanceTimeBy(SettingsGuard.CONFIRM_DELAY_SECONDS * 1_000L + 1)
        vm.confirmPendingChange()
        runCurrent()
        assertEquals(Settings().toleranceMinutes, settings.settings.value.toleranceMinutes)
        assertEquals(Settings().videoChainLimit, settings.settings.value.videoChainLimit)

        // volnější než výchozí → reset je zpřísnění a platí hned
        settings.update { it.copy(debtLimitMinutes = 60, squatsPerUnlock = 10) }
        vm.resetSection(SettingsSection.MOVEMENT)
        runCurrent()
        assertNull(vm.uiState.value.pendingChange)
        assertEquals(30, settings.settings.value.debtLimitMinutes)
        assertEquals(20, settings.settings.value.squatsPerUnlock)
    }

    @Test
    fun manualAwayNeedsConfirmationHomeDoesNot() = runTest(dispatcher) {
        val vm = vm(); collect(vm)
        vm.setManualZone(ManualZoneChoice.HOME)
        runCurrent()
        assertEquals(ComfortContext.HOME, context.manualOverride.value)
        vm.setManualZone(ManualZoneChoice.AWAY)
        runCurrent()
        assertEquals(ComfortContext.HOME, context.manualOverride.value)
        assertNotNull(vm.uiState.value.pendingChange)
        advanceTimeBy(SettingsGuard.CONFIRM_DELAY_SECONDS * 1_000L + 1)
        vm.confirmPendingChange()
        runCurrent()
        assertEquals(ComfortContext.AWAY, context.manualOverride.value)
        assertEquals(ManualZoneChoice.AWAY, vm.uiState.value.manualZone)
    }

    @Test
    fun uiStateCarriesKeysStravaAndPermissions() = runTest(dispatcher) {
        val vm = vm(flowOf(StravaCardState(configured = false, message = "Strava není nastavená.")))
        collect(vm)
        vm.updatePermissionStatuses(accessibility = true, overlay = false, location = true, camera = false, notifications = true)
        runCurrent()
        val s = vm.uiState.value
        assertFalse(s.llmKeys.getValue(LlmProvider.CLAUDE))
        assertTrue(s.llmKeys.getValue(LlmProvider.GEMINI))
        assertFalse(s.strava.configured)
        assertEquals("Strava není nastavená.", s.strava.message)
        assertEquals(
            setOf(SettingsPermissionType.OVERLAY, SettingsPermissionType.CAMERA),
            s.permissions.filterNot { it.isGranted }.map { it.type }.toSet(),
        )
        assertEquals(Settings(), s.settings)
    }

    @Test
    fun scrollProgressToggleIsImmediateAndResettable() = runTest(dispatcher) {
        val vm = vm(); collect(vm)
        vm.setShowScrollProgress(false)
        runCurrent()
        assertNull(vm.uiState.value.pendingChange)
        assertFalse(settings.settings.value.showScrollProgress)
        assertTrue(SettingsGuard.isModified(settings.settings.value, SettingsSection.PROTECTION))
        vm.resetSection(SettingsSection.PROTECTION)
        runCurrent()
        assertTrue(settings.settings.value.showScrollProgress)
    }

    @Test
    fun noOpChangeDoesNothing() = runTest(dispatcher) {
        val vm = vm(); collect(vm)
        vm.setDebtLimitMinutes(30)
        runCurrent()
        assertNull(vm.uiState.value.pendingChange)
    }

    /** 160: „Doma“ → „Automaticky“ mimo domácí Wi-Fi vypne Ozvěnu jako „Venku“, proto potvrzení. */
    @Test
    fun manualHomeToAutoAwayFromHomeWifiNeedsConfirmation() = runTest(dispatcher) {
        settings = FakeSettingsRepository(Settings(homeSsid = "Doma"))
        context.setWifi("Kavarna")
        val vm = vm(); collect(vm)
        vm.setManualZone(ManualZoneChoice.HOME)
        runCurrent()
        vm.setManualZone(ManualZoneChoice.AUTO)
        runCurrent()
        assertEquals(ComfortContext.HOME, context.manualOverride.value)
        assertNotNull(vm.uiState.value.pendingChange)
        advanceTimeBy(SettingsGuard.CONFIRM_DELAY_SECONDS * 1_000L + 1)
        vm.confirmPendingChange()
        runCurrent()
        assertNull(context.manualOverride.value)
    }

    /** 160: na domácí Wi-Fi automatika vyjde doma, nic se neuvolňuje. */
    @Test
    fun manualHomeToAutoOnHomeWifiIsImmediate() = runTest(dispatcher) {
        settings = FakeSettingsRepository(Settings(homeSsid = "\"Doma\""))
        context.setWifi("Doma")
        val vm = vm(); collect(vm)
        vm.setManualZone(ManualZoneChoice.HOME)
        runCurrent()
        vm.setManualZone(ManualZoneChoice.AUTO)
        runCurrent()
        assertNull(context.manualOverride.value)
        assertNull(vm.uiState.value.pendingChange)
    }

    /** 160: jiná síť místo uloženého domova = potvrzení; první uložení a stejná síť hned. */
    @Test
    fun replacingHomeWifiNeedsConfirmation() = runTest(dispatcher) {
        context.setWifi("Doma")
        val first = vm(); collect(first)
        first.saveCurrentWifiAsHome() // domov ještě není uložený
        runCurrent()
        assertEquals("Doma", context.savedHome)
        assertNull(first.uiState.value.pendingChange)

        settings = FakeSettingsRepository(Settings(homeSsid = "Doma"))
        context = cz.narsil.core.fake.FakeContextRepository(wifiSsid = "Kavarna")
        val vm = vm(); collect(vm)
        vm.saveCurrentWifiAsHome()
        runCurrent()
        assertNull(context.savedHome)
        assertNotNull(vm.uiState.value.pendingChange)
        advanceTimeBy(SettingsGuard.CONFIRM_DELAY_SECONDS * 1_000L + 1)
        vm.confirmPendingChange()
        runCurrent()
        assertEquals("Kavarna", context.savedHome)
    }
}
