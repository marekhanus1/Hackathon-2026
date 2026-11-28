package cz.narsil.ui.screens.rating

/**
 * Odpověď na „Co ti to video dalo?“ (156). [rating] jde do `video_ratings.rating` (1–5, mapa hodnoty),
 * [label] do `video_ratings.intent`.
 */
enum class VideoValue(val label: String, val emoji: String, val rating: Int) {
    LEARNED("Naučil jsem se něco", "💡", 5),
    INSPIRED("Inspirace, nápad", "✨", 4),
    RESTED("Odpočinul jsem si", "🛋️", 3),
    NOTHING("Nic, jen čas", "⏳", 2),
    WORSE("Je mi hůř", "🌧️", 1);

    companion object {
        fun ofLabel(label: String?): VideoValue? = entries.firstOrNull { it.label == label }
    }
}

/**
 * Spodní panel po dosledovaném videu nebo zaplaceném čase feedu (156).
 *
 * @param videoTitle co uživatel sledoval („Rust Async…“, „10 minut feedu na TikToku“)
 * @param question otázka v nadpisu
 * @param earnedXp kolik XP odpověď dá (0 = strop dne, štítek se skryje)
 * @param answered vybraná odpověď; panel pak jen poděkuje a sám zmizí
 */
data class VideoRatingUiState(
    val videoTitle: String = "Rust Async – kompletní průvodce pro začátečníky",
    val question: String = "Co ti to video dalo?",
    val earnedXp: Int = 5,
    val answered: VideoValue? = null,
)
