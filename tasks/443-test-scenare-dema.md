---
status: todo        # todo | in-progress | review | done
owner:              # jméno člověka
agent:              # claude | gemini | codex | human
branch:
pr:
---

# 443 – Ruční test scénáře dema a hlášení chyb

## Cíl
Projít scénář dema na Nothing Phone 3a krok po kroku a každou chybu zapsat jako samostatný úkol `13x` (viz 130), aby ji Marek mohl hned opravit.

## Soubory
- tasks/13x-*.md (nové, jeden na chybu, z šablony tasks/_TEMPLATE.md)

## Hotovo když
Scénář je projitý celý. Každá chyba má task s kroky, očekávaným a skutečným chováním a ideálně screenshotem nebo výřezem logcatu.

## Postup
- Scénář: `pitch/demo.md` (případně aktuální demo panel v aplikaci).
- Screenshot: `adb exec-out screencap -p > chyba.png`. Log: `adb logcat -s NarsilA11y NarsilOverlay`, případně celý `adb logcat` uložit do souboru.
- Jeden task = jedna chyba. Do `Soubory` napiš, kde chybu tušíš, pokud to víš. Jinak to nech Markovi.

## Poznámky
- Pro: Štěpán (bez AI, nebo GPT na formulaci).
- Telefon se sdílí s Markem: domluvit se, kdy je volný, a neinstalovat vlastní build.
- Task chyby commitni rovnou na `main` (je to jen nový soubor v tasks/, jako claim).
