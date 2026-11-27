package cz.narsil.app.nav

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import cz.narsil.app.demo.DemoPanelScreen

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
        startDestination = NarsilRoute.start.route,
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
            // 320 + 122: OnboardingScreen
            PlaceholderScreen(NarsilRoute.Onboarding, onNavigate = navigate, onBack = back)
        }
        composable(NarsilRoute.Verify.route) {
            VerifyRoute(onBack = back)
        }
        composable(NarsilRoute.Settings.route) {
            // 325: SettingsScreen (Jan)
            PlaceholderScreen(NarsilRoute.Settings, onNavigate = navigate, onBack = back)
        }
        composable(NarsilRoute.Demo.route) {
            DemoPanelScreen(onBack = back)
        }
    }
}
