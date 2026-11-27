---
status: todo
owner: 
agent: 
branch: 
pr: 
---

# 324 – Obrazovka ověření úkolu

## Cíl
Obrazovka pro ověření úkolu z plánu dne: vyfocení důkazu nebo krátký text (≤ 300 znaků), stavy čeká / ověřeno / zamítnuto / ověří se později, výsledek s animací odměny. Hotovo, když funguje s `FakeTaskVerifier` i se skutečným `TaskVerifier` (520). Náhledy ve světlém i tmavém režimu.

## Soubory
`app/src/main/java/cz/narsil/ui/screens/verify/`

## Poznámky
- Pravidla: `docs/SPEC.md` sekce 6 a 7. Rozhraní `TaskVerifier` a `Proof` přidá úkol 105, do té doby lokální fake stav.
- Proud B, po úkolu 321.
