---
status: in-progress
owner: simon
agent: claude
branch: simon/claude/520-overeni-v-aplikaci
pr: 
---

# 520 – Ověření fotkou a textem v aplikaci

## Cíl
Napojit `TaskVerifier` na úkoly plánu (`ContractRepository.updateTask`): stavy čeká, ověřeno, zamítnuto, ověří se později, opakování při další síti (nejvýš 3 pokusy). Hotovo, když fotka zápisků ověří úkol Škola do 5 s.

## Soubory
`app/src/main/java/cz/narsil/verify/`

## Poznámky
-
