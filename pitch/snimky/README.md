# Snímky obrazovek a slidy (úkol 331)

## Prezentace – `pitch/slidy/index.html`
Samostatný 16:9 pitch deck postavený přesně podle textů od Štěpána (`pitch/slidy.md`, úkol 402) a barevné palety z `docs/ilustrace.md` / `cz.narsil.ui.theme` (`#121110`, `#1C1A17`, `#F2A359`, `#5CC497`, `#9FA8DA`, `#F2C14E`).

- **Otevření:** otevři `pitch/slidy/index.html` v libovolném prohlížeči (Chrome / Edge).
- **Ovládání:** šipky `←` / `→` nebo `Mezerník` přepínají slidy, `N` zobrazí poznámky pro mluvčího s časováním, `F` přepne na celou obrazovku.
- **Export do PDF / Google Slides:** v prohlížeči stiskni `Ctrl + P` → „Uložit jako PDF“ (šablona má `@media print` 16:9).

## Snímky obrazovek – `pitch/snimky/`
Vektorové SVG makety odpovídající reálným Jetpack Compose obrazovkám aplikace Narsil:
1. `01-domu.svg` – Hlavní obrazovka (`HomeScreen`, 312)
2. `02-smlouva.svg` – Večerní smlouva (`ContractScreen` krok 3, 313)
3. `03-ozvena.svg` – Překryvné okno Ozvěny (`EchoScreen`, 310)
4. `04-drepy.svg` – Obrazovka dřepů (`SquatScreen`, 311)
5. `05-zrcadlo-hodnota.svg` – Zrcadlo a mapa hodnoty (`MirrorScreen`, 322)
6. `06-partak-notifikace.svg` – Ukázková notifikace na telefonu Parťáka (MVP simulace)

## Pitch prezentace – `pitch/narsil-pitch.html` (úkol 151)
Jednoduchý světlý styl podle barev aplikace, motiv meče Narsil (zlomený a znovu ukovaný). 11 slidů hlavní linky + 6 záložních slidů s maketami aplikace.

- **Spuštění:** `cd pitch && python3 -m http.server 8000` → Firefox `http://localhost:8000/narsil-pitch.html`, `F` celá obrazovka.
- **Během pitche jen šipky / prezentér:** `→` `↓` `PageDown` mezerník nebo klik myší = dál, `←` `↑` `PageUp` = zpět.
  Kroky na slidu se odkrývají postupně, makety (Ozvěna, dřepy…) běží samy. Na slidech nejsou žádná tlačítka.
- **Pro řečníka:** `N` poznámky, `O` přehled, `T` časomíra, `B` nebo `.` černá obrazovka, `F5` celá obrazovka (nepřenačte stránku).
- Tým, datum a délka pitche jsou v `CONFIG` na začátku skriptu.

### Živý přenos z telefonu (připravit před pitchem)
1. scrcpy je v `~/.local/opt/scrcpy` (oficiální balík z GitHubu Genymobile/scrcpy). Telefon na USB a spustit:
   `ADB=~/Android/Sdk/platform-tools/adb ~/.local/opt/scrcpy/scrcpy --window-title=Narsil-live --no-audio --no-control --max-size=1600`
2. V prezentaci klávesa `S` → **Sdílet okno telefonu** → okno *Narsil-live* → `S` panel zavře.
3. Když přenos běží, slide Demo ukazuje živý telefon a klikem se odkrývají scény dema; 6 záložních slidů se přeskočí.
   Když neběží (nebo spadne), prezentace jimi normálně projde.

Pozor: USB spojení s Nothingem občas samo spadne a scrcpy pak zavře okno – pak spustit znovu a znovu nasdílet (`S`).
