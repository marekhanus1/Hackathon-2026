package cz.narsil.movement

import cz.narsil.core.data.LedgerDao
import cz.narsil.core.data.LedgerEntryEntity
import cz.narsil.movement.currency.LedgerStore
import cz.narsil.movement.currency.MinuteEntry
import java.time.LocalDate

/** Pohyby minut ve sdílené tabulce `ledger` (`LedgerDao`, SPEC 11.5). */
class RoomLedgerStore(private val dao: LedgerDao) : LedgerStore {
    override suspend fun entries(date: LocalDate): List<MinuteEntry> =
        dao.forDate(date.toString()).map { MinuteEntry(it.kind, it.minutes, it.atMillis) }

    override suspend fun add(date: LocalDate, entry: MinuteEntry) {
        dao.insert(
            LedgerEntryEntity(
                date = date.toString(),
                atMillis = entry.atMillis,
                kind = entry.kind,
                minutes = entry.minutes.coerceAtLeast(0),
            ),
        )
    }
}
