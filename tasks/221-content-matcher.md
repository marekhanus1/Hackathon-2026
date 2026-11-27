---
status: review
owner: simon
agent: claude
branch: simon/claude/221-content-matcher
pr: https://github.com/marekhanus1/Hackathon-2026/pull/39
---

# 221 – RewardService: odměny za ověřené úkoly

## Cíl
`RewardService.reward(task)` podle `docs/SPEC.md` 7 a 11.2: po ověření úkolu (`VERIFIED`) přičte `task.rewardMinutes` do zůstatku (`MovementRepository.addEarnedMinutes`) a +30 XP (`GamificationEngine.onTaskVerified`). Každý úkol se odmění jen jednou (i po restartu – podle `PlanTaskEntity.verifiedAtMillis` nebo záznamu v ledgeru). Unit testy.

## Soubory
- app/src/main/java/cz/narsil/gamification/reward/
- app/src/test/java/cz/narsil/gamification/reward/

## Hotovo když
Ověřený úkol zvýší zůstatek i XP právě jednou; test pokrývá dvojí volání.

## Poznámky
- Pro: Šimon (Claude Pro č. 2).
- Fáze 2, proud B. Volá ho ověření Člena 5 (520), odměnu ukazuje Jan (321).
- Do napojení `FakeRewardService` v `cz.narsil.core.fake`.
- `ContentMatcher` (Claude API, plánovaný obsah) přešel na Člena 5, úkol 512.
