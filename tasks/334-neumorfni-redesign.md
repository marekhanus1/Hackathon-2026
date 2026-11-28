---
status: in-progress
owner: marek
agent: claude
branch: marek/claude/334-neumorfni-redesign
pr:
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
