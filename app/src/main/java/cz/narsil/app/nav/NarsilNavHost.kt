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
            // 312: HomeScreen
            PlaceholderScreen(NarsilRoute.Home, onNavigate = navigate, onBack = null)
        }
        composable(NarsilRoute.Contract.route) {
            // 313: ContractScreen
            PlaceholderScreen(NarsilRoute.Contract, onNavigate = navigate, onBack = back)
        }
        composable(NarsilRoute.Squat.route) {
            // 311: SquatScreen
            PlaceholderScreen(NarsilRoute.Squat, onNavigate = navigate, onBack = back)
        }
        composable(NarsilRoute.EchoPreview.route) {
            // 310: EchoScreen (náhled; produkčně v překryvném okně, úkol 113)
            PlaceholderScreen(NarsilRoute.EchoPreview, onNavigate = navigate, onBack = back)
        }
        composable(NarsilRoute.Mirror.route) {
            // 322: MirrorScreen
            PlaceholderScreen(NarsilRoute.Mirror, onNavigate = navigate, onBack = back)
        }
        composable(NarsilRoute.Badges.route) {
            // 323: BadgesScreen
            PlaceholderScreen(NarsilRoute.Badges, onNavigate = navigate, onBack = back)
        }
        composable(NarsilRoute.Onboarding.route) {
            // 320 + 122: OnboardingScreen
            PlaceholderScreen(NarsilRoute.Onboarding, onNavigate = navigate, onBack = back)
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
