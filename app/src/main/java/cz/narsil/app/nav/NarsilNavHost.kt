package cz.narsil.app.nav

import androidx.compose.foundation.background
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
import cz.narsil.ui.theme.NeuColors

/** Záložka spodního menu pro trasu; null = obrazovka bez menu (334). */
internal fun tabFor(route: String?): NeuTab? = when (route) {
    NarsilRoute.Home.route -> NeuTab.HOME
    NarsilRoute.Badges.route -> NeuTab.BADGES
    NarsilRoute.Settings.route -> NeuTab.SETTINGS
    else -> null
}

private fun NeuTab.route(): NarsilRoute = when (this) {
    NeuTab.HOME -> NarsilRoute.Home
    NeuTab.CONTRACT -> NarsilRoute.Contract
    NeuTab.BADGES -> NarsilRoute.Badges
    NeuTab.SETTINGS -> NarsilRoute.Settings
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
        navController.navigate(tab.route().route) {
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

    Column(modifier.fillMaxSize().background(NeuColors.Base)) {
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
                BadgesRoute(onOpenMirror = { navigate(NarsilRoute.Mirror) })
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
            NeuTabBar(tab, openTab, Modifier.padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 16.dp))
        }
    }
}
