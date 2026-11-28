package cz.narsil.app.nav

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import cz.narsil.app.demo.DemoPanelScreen
import cz.narsil.app.onboarding.OnboardingPrefs
import cz.narsil.app.onboarding.OnboardingRoute
import cz.narsil.ui.components.NeuTab
import cz.narsil.ui.components.NeuTabBar

/** Záložka spodního menu pro trasu; null = obrazovka bez menu (334). */
internal fun tabFor(route: String?): NeuTab? = when (route) {
    NarsilRoute.Home.route -> NeuTab.HOME
    NarsilRoute.Mirror.route -> NeuTab.MIRROR
    NarsilRoute.Badges.route -> NeuTab.BADGES
    else -> null
}

private fun NeuTab.route(): NarsilRoute = when (this) {
    NeuTab.HOME -> NarsilRoute.Home
    NeuTab.MIRROR -> NarsilRoute.Mirror
    NeuTab.BADGES -> NarsilRoute.Badges
    NeuTab.CONTRACT -> NarsilRoute.Contract
}

/**
 * Navigace aplikace. Domov, odznaky a nastavení mají vespod vnořené menu se 4 záložkami (334);
 * smlouva, ověření a ostatní jsou bez menu s tlačítkem zpět.
 */
@Composable
fun NarsilNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    val navigate: (NarsilRoute) -> Unit = { target ->
        navController.navigate(target.route) {
            launchSingleTop = true
            if (target == NarsilRoute.Home) popUpTo(NarsilRoute.Home.route) { inclusive = false }
        }
    }
    val openTab: (NeuTab) -> Unit = { tab ->
        // Plán (smlouva) je průvodce přes celou obrazovku se zpět, ne záložka.
        if (tab == NeuTab.CONTRACT) navigate(NarsilRoute.Contract) else navController.navigate(tab.route().route) {
            launchSingleTop = true
            popUpTo(NarsilRoute.Home.route) { inclusive = false }
        }
    }
    val openVerify: (Long?) -> Unit = { taskId ->
        navController.navigate(if (taskId == null) NarsilRoute.VerifyPick.route else NarsilRoute.Verify.forTask(taskId)) {
            launchSingleTop = true
        }
    }
    val back: () -> Unit = { navController.popBackStack() }
    val entry by navController.currentBackStackEntryAsState()
    val tab = tabFor(entry?.destination?.route)

    // Frutiger Aero: pozadí (nebe, ryby, tráva) kreslí MainActivity přes celý displej, obrazovky jsou průhledné.
    Column(modifier.fillMaxSize()) {
        NavHost(
            navController = navController,
            // Při prvním spuštění průvodce oprávněními (122).
            startDestination = if (OnboardingPrefs.isDone(LocalContext.current)) NarsilRoute.start.route else NarsilRoute.Onboarding.route,
            modifier = Modifier.weight(1f),
        ) {
            composable(NarsilRoute.Home.route) {
                HomeRoute(onNavigate = navigate, onVerify = openVerify)
            }
            composable(NarsilRoute.Contract.route) {
                ContractRoute(onBack = back)
            }
            composable(NarsilRoute.Squat.route) {
                SquatRoute(onBack = back)
            }
            composable(NarsilRoute.EchoPreview.route) {
                // 310: EchoScreen (náhled; produkčně v překryvném okně, úkol 113)
                PlaceholderScreen(NarsilRoute.EchoPreview, onNavigate = navigate, onBack = back)
            }
            composable(NarsilRoute.Mirror.route) {
                MirrorRoute()
            }
            composable(NarsilRoute.Badges.route) {
                BadgesRoute(onOpenMirror = null) // Zrcadlo je v doku
            }
            composable(NarsilRoute.Onboarding.route) {
                // 122: logika + funkční obrazovka; Janova 320 ji později nahradí
                OnboardingRoute(onDone = {
                    navController.navigate(NarsilRoute.Home.route) {
                        popUpTo(NarsilRoute.Onboarding.route) { inclusive = true }
                        launchSingleTop = true
                    }
                })
            }
            composable(NarsilRoute.VerifyPick.route) {
                TaskPickerRoute(
                    onBack = back,
                    onContinue = { id ->
                        navController.navigate(NarsilRoute.Verify.forTask(id)) {
                            popUpTo(NarsilRoute.VerifyPick.route) { inclusive = true }
                        }
                    },
                    onOpenContract = { navigate(NarsilRoute.Contract) },
                )
            }
            composable(
                NarsilRoute.Verify.PATTERN,
                arguments = listOf(navArgument(NarsilRoute.Verify.ARG_TASK_ID) { type = NavType.LongType; defaultValue = -1L }),
            ) { backStackEntry ->
                val taskId = backStackEntry.arguments?.getLong(NarsilRoute.Verify.ARG_TASK_ID)?.takeIf { it >= 0 }
                VerifyRoute(onBack = back, initialTaskId = taskId)
            }
            composable(NarsilRoute.Settings.route) {
                SettingsRoute(onNavigate = navigate, onBack = back)
            }
            composable(NarsilRoute.Demo.route) {
                DemoPanelScreen(onBack = back)
            }
        }
        if (tab != null) {
            NeuTabBar(tab, openTab, Modifier.padding(start = 14.dp, end = 14.dp, top = 8.dp, bottom = 12.dp))
        }
    }
}
