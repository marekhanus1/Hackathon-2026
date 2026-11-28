---
status: done
owner: marek
agent: claude
branch: marek/claude/334-neumorfni-redesign
pr: https://github.com/marekhanus1/Hackathon-2026/pull/132
---

# 334 – Neumorfní redesign (koncept E: tmavý, všechno vnořené)

## Cíl
Převést schválený koncept E do Compose: téma, sdílený `Modifier.neuInset`,
vnořená spodní lišta se 4 záložkami, domov bez posouvání s todo listem úkolů,
nová obrazovka výběru úkolu k ověření, ověření se záložkami úkolů, nastavení,
smlouva, odznaky a Ozvěna ve stylu E.

## Soubory
- app/src/main/java/cz/narsil/ui/** (téma, komponenty, obrazovky, navigace)
- app/src/main/java/cz/narsil/app/** (navigace, pokud je tam)
- případně overlay (Ozvěna)

## Poznámky
- Nahrazuje draft PR #128 (úkol 332, flat home) a #130 (úkol 333, task-first
  contract) – obě sahají na HomeScreen.kt / ContractScreen.kt.
- Hotovo v PR #132: téma E (`neuInset`), spodní menu, domov s todo listem, výběr úkolu
  (`verify_pick`), ověření `verify?taskId=` se záložkami, smlouva na jedné obrazovce,
  nastavení přehled + detaily, odznaky (Zrcadlo odtud), Ozvěna. Drafty #128 a #130
  jsou tím nahrazené – doporučeno je po sloučení zavřít. Na telefonu zatím neověřeno.
