---
status: todo        # todo | in-progress | review | done
owner:              # jméno člověka
agent:              # claude | gemini | codex | human
branch:
pr:
---

# 550 – Test aktuálního stavu aplikace na Redmi Note 11

## Cíl
Připojit vlastní Redmi Note 11, nainstalovat aktuální `main` a projít aplikaci od čistého startu.
Výstupem je přehled, co na Redmi funguje a co ne, a ke každé chybě samostatný task.

## Soubory
- `docs/test-redmi.md` (nový: tabulka funkce → funguje / nefunguje / poznámka)
- `tasks/55x-*.md` (nové, jeden na chybu, z `tasks/_TEMPLATE.md`; čísla od 552)

## Hotovo když
Prošlý celý scénář `pitch/demo.md` a všechny obrazovky (onboarding, home, Ozvěna, dřepy,
smlouva, ověření, zrcadlo, nastavení). `docs/test-redmi.md` je na `main` a každá chyba
má task s kroky, očekávaným a skutečným chováním a screenshotem nebo výřezem logcatu.

## Postup
1. Telefon: Nastavení → O telefonu → 7× verze MIUI → Další nastavení → Možnosti vývojáře:
   zapnout Ladění USB **i „Ladění USB (bezpečnostní nastavení)“** (jinak nejde instalace
   a klikání přes adb). `adb devices` musí ukázat `device`.
2. `git pull` na `main`, `./gradlew installDebug` (s víc telefony `ANDROID_SERIAL=<serial>`,
   ať se nic neinstaluje na demo Nothing).
3. Projít onboarding a udělit všechna oprávnění. MIUI navíc: Autostart, Spořič baterie →
   „Bez omezení“, Další oprávnění → „Zobrazovat vyskakovací okna při běhu na pozadí“
   a „Zobrazit na zamykací obrazovce“. Zapsat, která z nich aplikace sama nevyžádá.
4. Hlavní věci k ověření:
   - Ozvěna se objeví nad Instagramem / TikTokem / YouTube Shorts a v prohlížeči (149),
     vyhledávání se nepočítá (148), služba přístupnosti přežije zamknutí a 10 min na pozadí.
   - Dřepy kamerou (ML Kit) – počítání, plynulost, teplota telefonu.
   - Kroky přes Health Connect (na Redmi možná doinstalovat z Google Play).
   - Smlouva a ověření úkolu (fotka úkolu, fotka prostředí, vlastní popis) přes LLM.
   - Odměny, XP, zrcadlo, žebříček, demo panel, nastavení.
5. Log: `adb logcat -s NarsilA11y NarsilOverlay AndroidRuntime` (pád hledat podle `FATAL`),
   screenshot: `adb exec-out screencap -p > chyba.png`.

## Poznámky
- Pro: Niki (Člen 5) + Codex.
- Nikdy nepoužívat `adb shell am force-stop cz.narsil`, `uiautomator dump`, `adb logcat -c`
  ani `./gradlew --stop` (shazuje buildy ostatních); a nesahat na Nothing Phone.
- API klíče pro LLM do `local.properties` (od Marka), nikdy do gitu.
- Tasky chyb a `docs/test-redmi.md` commitnout rovnou na `main` (jako claim).
- Rozhodnutí 2026-11-28 odložilo Redmi na konec; teď je ten konec – je to záloha pro demo.
