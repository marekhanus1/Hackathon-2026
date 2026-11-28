package cz.narsil.app.nav

/**
 * Trasy aplikace. Jan (cz.narsil.ui.screens.*) jen vymění zástupnou obrazovku
 * v [NarsilNavHost] za svou; trasy a jejich názvy zůstávají.
 */
sealed class NarsilRoute(val route: String, val title: String) {
    data object Home : NarsilRoute("home", "Domů")
    data object Contract : NarsilRoute("contract", "Večerní smlouva")
    data object Squat : NarsilRoute("squat", "Dřepy")
    /** Jen náhled; v produkci se Ozvěna ukazuje v překryvném okně (úkol 113). */
    data object EchoPreview : NarsilRoute("echo_preview", "Ozvěna (náhled)")
    data object Mirror : NarsilRoute("mirror", "Zrcadlo")
    data object Badges : NarsilRoute("badges", "Odznaky")
    data object Onboarding : NarsilRoute("onboarding", "Onboarding")
    data object Verify : NarsilRoute("verify", "Ověření úkolu")
    data class VerifyTask(val taskId: Long) : NarsilRoute("verify/$taskId", "Ověření úkolu")
    data object Settings : NarsilRoute("settings", "Nastavení")
    data object Demo : NarsilRoute("demo", "Demo panel")

    companion object {
        const val VERIFY_TASK_PATTERN = "verify/{taskId}"
        const val TASK_ID_ARGUMENT = "taskId"
        val start: NarsilRoute get() = Home
        val all: List<NarsilRoute>
            get() = listOf(Home, Contract, Squat, Verify, EchoPreview, Mirror, Badges, Onboarding, Settings, Demo)
    }
}
