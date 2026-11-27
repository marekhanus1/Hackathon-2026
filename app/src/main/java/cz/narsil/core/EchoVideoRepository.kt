package cz.narsil.core

import java.time.LocalDate

/** Video pro Ozvěnu. [uri] přehraje ExoPlayer (content://, file:// nebo android.resource://). */
data class EchoVideo(
    val uri: String,
    val recordedOn: LocalDate,
)

/** Výběr videa pro Ozvěnu (212): typicky včerejší video ze smlouvy, jinak nejnovější. */
interface EchoVideoRepository {
    /** null = žádné video; okno pak ukáže jen otázku. */
    suspend fun pickEchoVideo(): EchoVideo?
}
