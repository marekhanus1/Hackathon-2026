# Rozhodnutí

Jeden řádek na rozhodnutí: `YYYY-MM-DD – kdo – co a proč`.


2026-11-27 – marek – Projekt Narsil: Android aplikace proti prokrastinaci, nativně Kotlin + Jetpack Compose (Accessibility a překryvné okno potřebují nativní služby).
2026-11-27 – marek – Aplikace nic tvrdě neblokuje; zasahuje jen doma (domácí Wi-Fi), u nekonečného obsahu, po 5 minutách. Zprávy a chaty se nikdy nesledují.
2026-11-27 – marek – Práce dělená podle balíčků cz.narsil.* a číselných řad úkolů 1xx–4xx (viz AGENTS.md, sekce Projekt).
2026-11-27 – marek – Demo telefon Nothing Phone 3a, záloha a telefon Parťáka Redmi Note 11.
2026-11-27 – marek – Jedna Room databáze AppDatabase v core/data (úkol 121); ostatní moduly (např. 212) přidávají jen vlastní entity a DAO a registraci v AppDatabase hlásí Markovi.
2026-11-27 – marek – Změny AndroidManifest.xml a app/build.gradle.kts drží proudy minimální; konflikty řeší Marek. Klíč Claude je v BuildConfig.CLAUDE_API_KEY z local.properties (už v kostře 101).
2026-11-27 – marek – Sdílená databáze je NarsilDatabase v cz.narsil.core.data (105, nahrazuje AppDatabase/121): všechny entity a DAO předem, ostatní moduly vlastní entity nezakládají.
2026-11-27 – marek – DailyContract.isFulfilled je funkce isFulfilled(stepsToday), ne vlastnost: kroky smlouva nezná (105). Den od 4:00 počítá sdílený DayKey.
2026-11-28 – marek – Ověření úkolů přes Claude (výchozí poskytovatel) používá claude-haiku-5-5 bez parametru temperature (model ho odmítá); místo „teplota 0“ ze SPEC 7 je thinking vypnuté a effort low. Gemini a OpenAI dál s teplotou 0.
2026-11-28 – marek – Klíče GEMINI_API_KEY, OPENAI_API_KEY, STRAVA_CLIENT_ID, STRAVA_CLIENT_SECRET (vedle CLAUDE_API_KEY) jen v local.properties → BuildConfig; chybějící klíč = poskytovatel vypnutý (ověření pak VERIFY_LATER, párování obsahu přes klíčová slova).
2026-11-28 – marek – Výchozí poskytovatel LLM pro ověření úkolů a párování obsahu je GEMINI (gemini-2.5-flash-lite, temperature 0) místo CLAUDE: Gemini API má bezplatnou úroveň. Záloha pro demo bez klíče je přepínač „vždy ANO“. Poskytovatel jde přepnout v demo panelu (do obrazovky nastavení 325).
2026-11-28 – marek – Aplikace se ladí a testuje jen na Nothing Phone 3a; Redmi Note 11 (záloha, Parťák) je odložený úplně na konec, nic se na něm teď neověřuje.
2026-11-28 – marek – Web se sleduje ve Firefoxu (org.mozilla.firefox), ne v Chrome: na demo telefonu je Firefox. Detekce URL (110/111) a název videa YouTube na webu (120) se čtou z adresního řádku Firefoxu. Kód zatím zná jen Chrome (WatchedApps, WindowReader, EchoStateBuilder, MockDataLoader) – přepnutí je samostatná změna.
