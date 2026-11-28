package cz.narsil.app.nav

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.compose.ui.platform.LocalContext
import cz.narsil.app.demo.DemoPanelScreen
import cz.narsil.app.onboarding.OnboardingPrefs
import cz.narsil.app.onboarding.OnboardingRoute

/**
 * Navigace aplikace. Každá trasa zatím ukazuje [PlaceholderScreen];
 * skutečné obrazovky z cz.narsil.ui.screens.* se dosadí místo ní.
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
    val back: () -> Unit = { navController.popBackStack() }

    NavHost(
        navController = navController,
        // Při prvním spuštění průvodce oprávněními (122).
        startDestination = if (OnboardingPrefs.isDone(LocalContext.current)) NarsilRoute.start.route else NarsilRoute.Onboarding.route,
        modifier = modifier,
    ) {
        composable(NarsilRoute.Home.route) {
            HomeRoute(onNavigate = navigate)
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
            BadgesRoute()
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
        composable(NarsilRoute.Verify.route) {
            VerifyRoute(onBack = back)
        }
        composable(
            route = NarsilRoute.VERIFY_TASK_PATTERN,
            arguments = listOf(navArgument(NarsilRoute.TASK_ID_ARGUMENT) { type = NavType.LongType }),
        ) { entry ->
            VerifyRoute(
                onBack = back,
                initialTaskId = entry.arguments?.getLong(NarsilRoute.TASK_ID_ARGUMENT),
            )
        }
        composable(NarsilRoute.Settings.route) {
            SettingsRoute(onNavigate = navigate, onBack = back)
        }
        composable(NarsilRoute.Demo.route) {
            DemoPanelScreen(onBack = back)
        }
    }
}
