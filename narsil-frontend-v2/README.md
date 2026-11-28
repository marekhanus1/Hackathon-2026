# Narsil Frontend (Frutiger Aero)

Samostatný, lehký frontend bez externích závislostí (čisté HTML + CSS + Vanilla JS), připravený k napojení na backend nebo mobilní aplikaci (WebView).

## Struktura balíčku

- `index.html` – struktura všech obrazovek (`Úkoly`, `Zrcadlo`, `Odznaky`, `Plán`, `Ověření úkolu`)
- `styles.css` – kompletní Frutiger Aero styly
- `api.js` – integrační vrstva (`window.NarsilAPI`) s výchozími mock daty a podporou REST / WebView Bridge
- `app.js` – logika vykreslování a interakcí (`window.NarsilFrontend`)
- `assets/aero-background.png` – pozadí obrazovky
- `INTEGRATION.md` – detailní návod, jak frontend napojit na zbytek aplikace (API endpointy, JSON formát, WebView bridge, události)

## Rychlé spuštění

Otevři složku libovolným statickým serverem, například:

```bash
python -m http.server 4173
```

Pak otevři `http://127.0.0.1:4173/`. Bez dalšího nastavení běží v režimu `mock` s ukázkovými daty.
