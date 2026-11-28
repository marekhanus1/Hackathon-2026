package cz.narsil.ui.screens.settings

import cz.narsil.core.Settings
import cz.narsil.core.TrackedApp

/** Sekce nastavení s tlačítkem „Obnovit výchozí“ (133). */
enum class SettingsSection(val title: String) {
    PROTECTION("Ozvěna"),
    APPS("Sledované aplikace"),
    MOVEMENT("Pohyb a odemykání"),
    AI("Ověření úkolů"),
    NOTIFICATIONS("Upozornění"),
}

/**
 * Pravidla, aby nastavení nesloužilo jako únik z Narsilu (133).
 *
 * Zpřísnění (kratší tolerance, víc dřepů, nižší dluh, víc aplikací) platí hned. Uvolnění
 * vrací [loosenings] neprázdné a ViewModel ho pustí až po potvrzení s odpočtem [CONFIRM_DELAY_SECONDS].
 */
object SettingsGuard {
    /** Odpočet, než jde uvolnění potvrdit: krátká chvíle na rozmyšlenou, ne trest. */
    const val CONFIRM_DELAY_SECONDS = 10

    fun appLabel(app: TrackedApp): String = when (app) {
        TrackedApp.INSTAGRAM -> "Instagram"
        TrackedApp.TIKTOK -> "TikTok"
        TrackedApp.YOUTUBE -> "YouTube"
        TrackedApp.FIREFOX -> "Web ve Firefoxu"
        TrackedApp.X -> "X (Twitter)"
        TrackedApp.REDDIT -> "Reddit"
        TrackedApp.FACEBOOK -> "Facebook"
        TrackedApp.THREADS -> "Threads"
        TrackedApp.PINTEREST -> "Pinterest"
        TrackedApp.NINEGAG -> "9GAG"
        TrackedApp.TUMBLR -> "Tumblr"
        TrackedApp.LINKEDIN -> "LinkedIn"
        TrackedApp.BLUESKY -> "Bluesky"
    }

    /** Aplikace s vlastními pravidly; ostatní mají obecnou detekci podle scrollu (149). */
    val CORE_APPS = setOf(TrackedApp.INSTAGRAM, TrackedApp.TIKTOK, TrackedApp.YOUTUBE, TrackedApp.FIREFOX)

    /** Česky popsaná uvolnění mezi [old] a [new]; prázdné = změna je neutrální nebo přísnější. */
    fun loosenings(old: Settings, new: Settings): List<String> = buildList {
        if (new.toleranceMinutes > old.toleranceMinutes) {
            add("Ozvěna přijde později: po ${new.toleranceMinutes} min místo ${old.toleranceMinutes} min.")
        }
        if (new.videoChainLimit > old.videoChainLimit) {
            add("Víc neplánovaných videí po sobě: ${new.videoChainLimit}. místo ${old.videoChainLimit}.")
        }
        if (new.squatsPerUnlock < old.squatsPerUnlock) {
            add("Méně dřepů na odemčení: ${new.squatsPerUnlock} místo ${old.squatsPerUnlock}.")
        }
        if (new.pushupsPerUnlock < old.pushupsPerUnlock) {
            add("Méně kliků na odemčení: ${new.pushupsPerUnlock} místo ${old.pushupsPerUnlock}.")
        }
        if (new.debtLimitMinutes > old.debtLimitMinutes) {
            add("Vyšší limit dluhu: ${new.debtLimitMinutes} min místo ${old.debtLimitMinutes} min.")
        }
        val removed = old.trackedApps - new.trackedApps
        if (removed.isNotEmpty()) {
            add("Narsil přestane sledovat: ${removed.sortedBy { it.ordinal }.joinToString { appLabel(it) }}.")
        }
        if (old.trackOtherFeedApps && !new.trackOtherFeedApps) {
            add("Narsil přestane hlídat další aplikace s nekonečným feedem.")
        }
        val oldPause = old.protectionPausedUntilMillis ?: 0L
        val newPause = new.protectionPausedUntilMillis ?: 0L
        if (newPause > oldPause) add("Ochrana se pozastaví do 4:00 ráno. Ozvěna do té doby nepřijde.")
        if (old.partnerAlerts && !new.partnerAlerts) {
            add("Parťák se nedozví o dluhu nad limit ani o pozastavení ochrany.")
        }
    }

    /** [current] s výchozími hodnotami jen v [section]; ostatní sekce a demo přepínače zůstanou. */
    fun resetSection(current: Settings, section: SettingsSection): Settings {
        val d = Settings()
        return when (section) {
            SettingsSection.PROTECTION -> current.copy(
                toleranceMinutes = d.toleranceMinutes,
                videoChainLimit = d.videoChainLimit,
                showScrollProgress = d.showScrollProgress,
            )
            SettingsSection.APPS -> current.copy(trackedApps = d.trackedApps, trackOtherFeedApps = d.trackOtherFeedApps)
            SettingsSection.MOVEMENT -> current.copy(
                exercise = d.exercise,
                squatsPerUnlock = d.squatsPerUnlock,
                pushupsPerUnlock = d.pushupsPerUnlock,
                debtLimitMinutes = d.debtLimitMinutes,
            )
            SettingsSection.AI -> current.copy(llmProvider = d.llmProvider)
            SettingsSection.NOTIFICATIONS -> current.copy(partnerAlerts = d.partnerAlerts)
        }
    }

    /** Má sekce jiné než výchozí hodnoty? (tlačítko „Obnovit výchozí“ je jen tehdy aktivní) */
    fun isModified(current: Settings, section: SettingsSection): Boolean = resetSection(current, section) != current
}
