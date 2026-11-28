---
status: in-progress # todo | in-progress | review | done
owner: jan          # jméno člověka
agent: gemini       # claude | gemini | human
branch: jan/gemini/334-sova-mazlicek
pr:
---

# 334 – Sova Narsilka (mazlíček a AI podpora)

## Cíl
Sova-mazlíček „Narsilka“ s chatem, náladami podle `AvatarState` (+ noční spánek), offline hláškami a volitelným napojením na LLM přes `MultiProviderLlmClient`, navržená tak, aby nikdy nebyla otravná (max 2 proaktivní změny denně, tlačítko „Teď ne“, žádné moralizování).

## Soubory
- app/src/main/java/cz/narsil/ui/screens/pet/
- app/src/test/java/cz/narsil/ui/screens/pet/
- app/src/main/java/cz/narsil/ui/screens/home/HomeScreen.kt (slot `topSlot` pod XP barem)
- app/src/main/java/cz/narsil/app/nav/ (samostatný commit – vyžaduje souhlas Marka)

## Poznámky
- Zapojení do navigace sahá na Markovy soubory v `cz.narsil.app.nav` (samostatný commit, nutný souhlas Marka).
- Nepushnuto na přání Jana (vše pouze lokálně na větvi `jan/gemini/334-sova-mazlicek`).
- Jméno mazlíčka není „Parťák“, to je ve SPEC kamarád-člověk.
- Build ani testy neběžely: na Janově PC chybí JDK 17 a Android SDK; ověřit `./gradlew testDebugUnitTest assembleDebug` v Android Studiu.
