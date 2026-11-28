---
status: todo        # todo | in-progress | review | done
owner:              # jméno člověka
agent:              # claude | gemini | codex | human
branch:
pr:
---

# 551 – Opravy chyb z testu na Redmi

## Cíl
Opravit chyby nalezené v 550. Každou chybu zvlášť: claimnout její task `55x`, vlastní větev
a worktree, malé PR. Co se opravit nedá, popsat v tasku chyby (příčina, návrh) a nechat Markovi.

## Soubory
- Podle chyby. Volně: `verify/`, `integrations/`, `llm/`.
- `core`, `detection`, `overlay`, `app`, `ui`, `res/` a databáze jen po domluvě s vlastníkem
  (viz AGENTS.md) – napsat do tasku chyby a do popisu PR.

## Hotovo když
Chyby, které brání demu na Redmi (pád, Ozvěna se neukáže, chybí oprávnění, nejde ověření),
jsou opravené nebo předané s popsanou příčinou. Scénář `pitch/demo.md` na Redmi projde
2× za sebou. `docs/test-redmi.md` aktualizovaný.

## Postup
- Pořadí: pády → Ozvěna a služba přístupnosti → oprávnění MIUI → ověření/LLM → vzhled.
- Opravu ověřit na Redmi a `./gradlew testDebugUnitTest` před PR.
- Specifické pro MIUI řešit podmínkou na výrobce (`Build.MANUFACTURER`), ať se nerozbije Nothing.

## Poznámky
- Pro: Niki (Člen 5) + Codex. Navazuje na 550.
- Nové závislosti nepřidávat.
