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
    /** Ověření úkolu; s `?taskId=` rovnou pro konkrétní úkol (334), bez něj → [VerifyPick]. */
    data object Verify : NarsilRoute("verify", "Ověření úkolu") {
        const val ARG_TASK_ID = "taskId"
        const val PATTERN = "verify?$ARG_TASK_ID={$ARG_TASK_ID}"
        fun forTask(taskId: Long): String = "verify?$ARG_TASK_ID=$taskId"
    }
    /** Výběr úkolu k ověření (334). */
    data object VerifyPick : NarsilRoute("verify_pick", "Který úkol ověříš?")
    data object Settings : NarsilRoute("settings", "Nastavení")
    data object Demo : NarsilRoute("demo", "Demo panel")

    companion object {
        val start: NarsilRoute get() = Home
        val all: List<NarsilRoute>
            get() = listOf(Home, Contract, Squat, Verify, VerifyPick, EchoPreview, Mirror, Badges, Onboarding, Settings, Demo)
    }
}
