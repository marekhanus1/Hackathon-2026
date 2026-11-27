package cz.narsil.core.fake

import cz.narsil.core.EchoVideo
import cz.narsil.core.EchoVideoRepository
import java.time.LocalDate

/** Fake: vrací [video] (výchozí demo URI ze včerejška); null = bez videa. */
class FakeEchoVideoRepository(
    var video: EchoVideo? = EchoVideo(DEMO_URI, LocalDate.now().minusDays(1)),
) : EchoVideoRepository {
    override suspend fun pickEchoVideo(): EchoVideo? = video

    companion object {
        /** Demo video v res/raw (dodá 4xx/302); dokud neexistuje, přehrávač ukáže chybu a okno jen otázku. */
        const val DEMO_URI = "android.resource://cz.narsil/raw/echo_demo"
    }
}
