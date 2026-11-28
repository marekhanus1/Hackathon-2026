# Jak připravit prezentaci na 17:00 (`pitch/slidy/index.html`)

Prezentace je připravená jako čistá grafická šablona (9 širokoúhlých 16:9 slidů). Všechny texty a obrázky si můžete upravit podle toho, co se stihne dotáhnout.

## 1. Otevření
Stačí v prohlížeči otevřít soubor `pitch/slidy/index.html` (funguje i bez internetu).

## 2. Úprava přímo v prohlížeči (nejrychlejší)
1. V horní liště klikni na **`✎ Upravit`**.
2. **Texty:** Klikni na jakýkoliv nadpis, podtitul nebo kartu a rovnou piš.
3. **Screenshoty z telefonu (Slide 5):** Klikni na rámeček telefonu (nebo přetáhni `.png`/`.jpg` obrázek) a nahraj reálný snímek obrazovky.
   *(Alternativně stačí vložit soubory `screen-1.png`, `screen-2.png`, `screen-3.png` do složky `pitch/slidy/obrazky/`.)*
4. **Pořadí a počet slidů:** V režimu úprav můžeš slidy přesouvat (`←` / `→`), skrýt ty, co nebudete ukazovat, nebo přidat nový přes **`+ Přidat slide`**.
5. **Barvy:** Nahoře přepni mezi **Meruňková**, **Lososová** a **Noční** (tmavý režim pro projektor).
6. **Uložení před prezentací:** Klikni na **`⬇ Uložit HTML`** – stáhne se hotový samostatný `index.html` včetně všech textů a nahraných obrázků (připraveno offline pro projektor v 17:00; můžeš ho i rovnou commitnout zpět do `pitch/slidy/index.html`).

## 3. Úprava v kódu / přes AI agenta
Každý slide je jeden přehledný blok `<section class="slide">…</section>` v `pitch/slidy/index.html`. Stačí popsat Claude / Gemini / Codexu, který slide a jak má upravit.

## 4. Ovládání při prezentování
- `←` / `→` nebo `Mezerník` – přepínání slidů
- `G` – přehledová mřížka všech slidů
- `F` – celá obrazovka
- `N` – poznámky pro mluvčího
- `Ctrl + P` – export do PDF
