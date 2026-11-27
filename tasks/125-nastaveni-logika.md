---
status: todo        # todo | in-progress | review | done
owner:              # jméno člověka
agent:              # claude | gemini | human
branch:
pr:
---

# 125 – Nastavení: logika a napojení

## Cíl
Podklad pro obrazovku nastavení (325): rozšířit `ContextRepository` o aktuální Wi-Fi a „tohle je můj domov“ (`currentWifiSsid`, `saveCurrentWifiAsHome()`), fake, trasa `NarsilRoute.Settings` v navigaci se zástupnou obrazovkou, dokud Jan nedodá UI. Demo panel (123) přestylovat do `NarsilTheme`, ať nevyčnívá.

## Soubory
`app/src/main/java/cz/narsil/core/ContextRepository.kt`, `core/fake/FakeContextRepository.kt`, `detection/context/`, `app/nav/`, `app/demo/`, `docs/contracts.md`

## Poznámky
- Pro: Marek (Claude Pro č. 1). UI dělá Jan v 325.
