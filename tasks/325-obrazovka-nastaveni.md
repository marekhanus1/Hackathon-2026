---
status: todo        # todo | in-progress | review | done
owner:              # jméno člověka
agent:              # claude | gemini | human
branch:
pr:
---

# 325 – Obrazovka nastavení

## Cíl
Přehledná a klidná obrazovka nastavení v designu Narsilu (301, 302), česky, bez technických výrazů:
- **Domov:** aktuální Wi-Fi, uložený domov, tlačítko „Tohle je můj domov“, ruční přepínač Doma / Venku / Automaticky (platí do 4:00) a jedna věta, co to znamená („Ozvěna zasahuje jen doma“).
- **Ověření úkolů:** poskytovatel AI (Claude, Gemini, ChatGPT).
- **Strava:** připojeno / připojit (nepovinné).
- **Oprávnění:** stav služby přístupnosti, překrytí, polohy; u chybějícího tlačítko do systému.
- **Parťák:** zatím jen ukázka (MVP simulace).
- Skrytý vstup do demo panelu (např. dlouhé podržení verze aplikace).

## Soubory
- app/src/main/java/cz/narsil/ui/screens/settings/

## Hotovo když
Obrazovka v náhledu (světlý i tmavý režim) s fake daty; po napojení (Marek, 125/124) mění skutečná nastavení.

## Poznámky
- Pro: Jan (Gemini Antigravity Pro).
- ViewModel bere jen rozhraní z `cz.narsil.core`: `SettingsRepository` (`Settings.llmProvider`, `homeSsid`), `ContextRepository` (`context`, `manualOverride`, `setManualOverride`, `currentWifiSsid`, `saveCurrentWifiAsHome()` – doplní 125), `StravaRepository.isConnected`. Do napojení fake z `cz.narsil.core.fake`.
- Oprávnění jen jako stav + callback `onOpenSystemSettings(…)`, logiku dodá 122.
- Trasa `NarsilRoute.Settings` (125); Jan vymění zástupnou obrazovku za svou.
