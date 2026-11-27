---
status: review
owner: marek
agent: claude
branch: marek/claude/101-kostra
pr: https://github.com/marekhanus1/Hackathon-2026/pull/3
---

# 101 – Kostra projektu

## Cíl
Gradle projekt s jedním modulem `app`, balíčky `cz.narsil.*`, minSdk 29,
`gradle/libs.versions.toml` se **všemi** závislostmi ze stacku.
Hotovo, když `./gradlew assembleDebug` projde a prázdná aplikace běží na Nothing.

## Soubory
Kořen (`settings.gradle.kts`, `build.gradle.kts`, `gradle.properties`, `gradlew*`),
`gradle/`, `app/build.gradle.kts`, `app/src/main/AndroidManifest.xml`,
`app/src/main/java/cz/narsil/app/`, prázdné balíčky `cz.narsil.*`.

## Poznámky
