# Rozhodnutí

Jeden řádek na rozhodnutí: `YYYY-MM-DD – kdo – co a proč`.


2026-11-27 – marek – Projekt Narsil: Android aplikace proti prokrastinaci, nativně Kotlin + Jetpack Compose (Accessibility a překryvné okno potřebují nativní služby).
2026-11-27 – marek – Aplikace nic tvrdě neblokuje; zasahuje jen doma (domácí Wi-Fi), u nekonečného obsahu, po 5 minutách. Zprávy a chaty se nikdy nesledují.
2026-11-27 – marek – Práce dělená podle balíčků cz.narsil.* a číselných řad úkolů 1xx–4xx (viz AGENTS.md, sekce Projekt).
2026-11-27 – marek – Demo telefon Nothing Phone 3a, záloha a telefon Parťáka Redmi Note 11.
2026-11-27 – marek – Jedna Room databáze AppDatabase v core/data (úkol 121); ostatní moduly (např. 212) přidávají jen vlastní entity a DAO a registraci v AppDatabase hlásí Markovi.
2026-11-27 – marek – Změny AndroidManifest.xml a app/build.gradle.kts drží proudy minimální; konflikty řeší Marek. Klíč Claude je v BuildConfig.CLAUDE_API_KEY z local.properties (už v kostře 101).
