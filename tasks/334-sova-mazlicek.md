---
status: review      # todo | in-progress | review | done
owner: jan          # jméno člověka
agent: gemini       # claude | gemini | human
branch: jan/gemini/334-sova-mazlicek
pr: https://github.com/marekhanus1/Hackathon-2026/pull/97
---

# 334 – Sova Narsilka (mazlíček a AI podpora)

## Cíl
Sova-mazlíček „Narsilka“ s chatem, náladami podle `AvatarState` (+ noční spánek), offline hláškami a volitelným napojením na LLM přes `MultiProviderLlmClient`, navržená tak, aby nikdy nebyla otravná (max 2 proaktivní změny denně, tlačítko „Teď ne“, žádné moralizování).

## Soubory
- app/src/main/java/cz/narsil/ui/screens/pet/
- app/src/test/java/cz/narsil/ui/screens/pet/
- app/src/main/java/cz/narsil/ui/screens/home/HomeScreen.kt (slot `topSlot` pod XP barem)
- app/src/main/java/cz/narsil/app/nav/ (samostatný commit – vyžaduje souhlas Marka)
- app/build.gradle.kts (`PET_GEMINI_API_KEY`, samostatný commit – vyžaduje souhlas Marka)

## Poznámky
- Zapojení do navigace sahá na Markovy soubory v `cz.narsil.app.nav` a `app/build.gradle.kts` (samostatné commity, nutný souhlas Marka).
- Otevřeno PR #97 na větvi `jan/gemini/334-sova-mazlicek` (rebasnuto na `origin/main` 2816111).
- Jméno mazlíčka není „Parťák“, to je ve SPEC kamarád-člověk.
- Sova používá vlastní klíč `PET_GEMINI_API_KEY` (Google Gemini Free API, `gemini-3.5-flash-lite`) s pádem zpět na `GEMINI_API_KEY` a pak offline.
- Ochrana free kvóty a proti sekání: lokální odpovědi pro rychlé volby/pozdravy/krizi, cache, rate-limiter (min 6 s, max 25 volání/den, 60 s cooldown po 429), timeout 5 s, max 160 tokenů.
- Build ani testy neběžely: na Janově PC chybí JDK 17 a Android SDK; ověřit `./gradlew testDebugUnitTest assembleDebug` v Android Studiu.
