package cz.narsil.core.fake

import cz.narsil.core.History
import cz.narsil.core.HistoryRepository

/** Fake historie (náhledy, testy). */
class FakeHistoryRepository(var history: History = History()) : HistoryRepository {
    override suspend fun load(days: Int): History = history
}
