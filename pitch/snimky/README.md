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

## Prezentace ve stylu Pána prstenů – `pitch/narsil-pitch.html` (úkol 151)
Jedna samostatná stránka (pevná scéna 1920×1080): Prolog → Kniha I Kraj → Kniha II Cesta (funkce aplikace) → Kniha III Společenstvo → Epilog.
Vpravo nahoře se meč Narsil slide po slidu skládá, na posledním slidu je celý.

- **Spuštění:** kvůli živému přenosu z telefonu otevírej přes localhost, ne jako soubor:
  `cd pitch && python3 -m http.server 8000` → Firefox `http://localhost:8000/narsil-pitch.html`
- **Ovládání:** `←` `→` / mezerník (kroky i slidy), `N` poznámky řečníka, `O` přehled, `F` celá obrazovka,
  `1–5` kapitoly, `L` živý obraz telefonu i v maketách, `D` dřep na slidu 2.4, klik na časomíru ji vynuluje.
  Tým, datum a délka pitche jsou v objektu `CONFIG` na začátku skriptu.
- **Makety obrazovek** (smlouva, Ozvěna, dřepy, hodnocení videa, Zrcadlo, Parťák) jsou interaktivní a texty odpovídají aplikaci.
  Slouží jako záloha, když živé demo selže.

### Živý přenos z telefonu (slide 2.1)
1. Nainstalovat scrcpy: `sudo apt install scrcpy`, případně oficiální balík z GitHubu (Genymobile/scrcpy, linux-x86_64).
   Když chybí `adb` v PATH: `export ADB=~/Android/Sdk/platform-tools/adb`.
2. Telefon na USB s laděním USB, spustit:
   `scrcpy --window-title=Narsil-live --no-audio --stay-awake --max-size=1600`
3. V prezentaci na slidu 2.1 kliknout **Sdílet okno telefonu** a vybrat okno *Narsil-live* (na Waylandu přes systémový dialog).
   Okno scrcpy pak může zůstat schované za prezentací.
4. Klávesa `L` přepne živý obraz i do telefonů na dalších slidech.

Varianta bez výběru okna: virtuální kamera (`sudo apt install v4l2loopback-dkms` + hlavičky jádra,
`sudo modprobe v4l2loopback video_nr=10 card_label=scrcpy`, `scrcpy --v4l2-sink=/dev/video10 --no-video-playback`)
a v prezentaci tlačítko **Virtuální kamera**.
