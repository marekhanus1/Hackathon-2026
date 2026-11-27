package cz.narsil.core

import kotlinx.coroutines.flow.StateFlow

/** Komfortní zóna: zasahujeme jen doma (HOME). V nejistotě (UNKNOWN) nezasahujeme. */
enum class ComfortContext { HOME, AWAY, UNKNOWN }

/**
 * Kde uživatel je. Implementuje 112 (detection/context) přes domácí Wi-Fi.
 *
 * [context] je výsledný stav: ruční přepínač má přednost před Wi-Fi.
 */
interface ContextRepository {
    /** Výsledný stav (ruční přepínač, jinak detekce). */
    val context: StateFlow<ComfortContext>

    /** Ruční přepínač „jsem doma / venku“; null = automatika. */
    val manualOverride: StateFlow<ComfortContext?>

    /** Nastaví ruční přepínač; null vrátí automatickou detekci. Platí do konce dne (4:00). */
    suspend fun setManualOverride(context: ComfortContext?)

    /** Název aktuální Wi-Fi; null = žádná Wi-Fi nebo název nejde zjistit (chybí poloha). */
    val currentWifiSsid: StateFlow<String?>

    /** „Tohle je můj domov“: uloží aktuální Wi-Fi do `Settings.homeSsid`. false = není co uložit. */
    suspend fun saveCurrentWifiAsHome(): Boolean
}
