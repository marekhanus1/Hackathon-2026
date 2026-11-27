# Vizuální styl ilustrací Narsil (zadání pro Štěpána – úkoly 420 a 421)

Tento dokument sjednocuje vzhled ilustrací postavy **Budoucího já**, **odznaků** a **obrázků na slidy**, aby vše ladilo s Material 3 design systémem v `cz.narsil.ui.theme`.

---

## 1. Umělecký směr a tón
- **Styl:** klidná moderní editoriální vektorová ilustrace (čisté tvary, jemné přechody, žádné ostré neonové hrany ani komiksový chaos).
- **Metafora:** znovu ukovaný meč (*kintsugi* – zlatý/jantarový svár na tmavém či teplém podkladu), klidné soustředění, laskavý pomocník místo přísného dozorce.
- **Pozadí:** všechny assety pro aplikaci (`assets/postava/`, `assets/odznaky/`) exportuj jako **průhledné PNG** (nebo čisté SVG/WebP), aby fungovaly ve **světlém (`#FAF7F2`) i tmavém (`#121110` / `#1C1A17`) režimu**.

---

## 2. Barevná paleta (přesné hex kódy ze systému 301)

| Role | Světlý režim | Tmavý režim | Použití |
| --- | --- | --- | --- |
| **Pozadí (plátno)** | `#FAF7F2` | `#121110` | Teplý alabastr / hluboký obsidián |
| **Karty (povrch)** | `#FFFFFF` / `#F2ECE1` | `#1C1A17` / `#26231F` | Podklad karet pod ilustracemi |
| **Jiskra / kovaný jantar (Primary)** | `#C86D2D` | `#F2A359` | Hlavní akcent, pohyb, svár meče |
| **Klidná šalvěj (Positive / XP)** | `#2E7D5B` | `#5CC497` | Výhry nad algoritmem, tlačítko Zavřít, XP |
| **Soumrakové indigo (Ozvěna)** | `#5465B8` | `#9FA8DA` | Ozvěna, večerní klid a sebereflexe |
| **Zlato (Odznaky a úroveň)** | `#D49B27` | `#F2C14E` | Odznaky, žolíky, odměna za úroveň |
| **Tlumený algoritmus** | `#7B6B8D` | `#B5A5C9` | Neutrální/tlumený kontrast pro „feed“ |

---

## 3. Úkol 420: Postava „Budoucí já“ v 5 stavech (`assets/postava/`)

Všechny ilustrace mají stejnou siluetu/postavu (středový ořez, poměr **1:1**, ideálně **512×512 px**, průhledné pozadí), mění se držení těla, světlo a výraz:

| Soubor | Stav (`AvatarState`) | Vizuální popis |
| --- | --- | --- |
| `assets/postava/avatar_state_1_tired.png` | 1 – Unavené já | Postava sedí opřená, tlumené soumrakové světlo (`#9FA8DA`), malá pohasínající jiskra vedle ní. Žádný trest nebo smutná karikatura – jen klidná únava. |
| `assets/postava/avatar_state_2_resting.png` | 2 – Klidné já | Postava se zvedá, hluboký nádech, teplé jemné světlo v dlaních (`#F2A359` tlumeně). |
| `assets/postava/avatar_state_3_balanced.png` | 3 – Vyrovnané já | Vzpřímený klidný postoj, vyvážený kruh ze šalvějové (`#5CC497`) a jantarové (`#F2A359`) linky kolem ní. |
| `assets/postava/avatar_state_4_energized.png` | 4 – Soustředěné já | Dynamický krok vpřed, jasnější teplý jantarový lem a klidný pohled. |
| `assets/postava/avatar_state_5_thriving.png` | 5 – V plné kondici | Zářivá, sebevědomá postava se zlatým/jantarovým symbolem znovu ukované síly (`#F2C14E` + `#5CC497`). |

### Šablona promptu pro Gemini (postava)
```text
Flat editorial vector illustration on a completely transparent background, 1:1 square centered character portrait of a calm minimalist person ("Future Self"), warm obsidian outlines (#26231F), soft amber (#F2A359) and calm sage green (#5CC497) and twilight indigo (#9FA8DA) accents. No text, no watermark, clean shapes compatible with both dark (#1C1A17) and light (#FAF7F2) UI cards. State: [DOPLŇ POPIS STAVU Z TABULKY].
```

---

## 4. Úkol 420: 5 ikon odznaků (`assets/odznaky/`)

Poměr **1:1**, **256×256 px**, průhledné pozadí, kulatý medailonový základ se zlatým/jantarovým okrajem (`#F2C14E` / `#F2A359`) a jednoduchým čitelným symbolem uprostřed:

| Soubor | Název odznaku | Symbol |
| --- | --- | --- |
| `assets/odznaky/badge_closed_10x.png` | Odešel jsem sám 10× | Zavřené dveře / vypnutá vlna v šalvějové zelené (`#5CC497`) |
| `assets/odznaky/badge_week_no_debt.png` | Týden bez dluhu | Vyvážené váhy nebo klidný štít se 7 tečkami (`#F2C14E`) |
| `assets/odznaky/badge_1000_squats.png` | 1 000 dřepů | Stylizovaný pohyb / plamen kovaného jantaru (`#F2A359`) |
| `assets/odznaky/badge_echo_keeper.png` | Strážce Ozvěny | Soumrakové indigo (`#9FA8DA`) se zvukovou vlnou a jiskrou |
| `assets/odznaky/badge_reforged.png` | Znovu ukovaný | Minimalistický meč se zlatým kintsugi svárem (`#F2C14E`) |

---

## 5. Úkol 421: Obrázky na slidy (`assets/slidy/`)

- Formát **16:9** nebo průhledné ilustrace na tmavé pozadí prezentace (`#121110` / `#1C1A17`) s vysokým kontrastem pro projektor.
- Používej stejnou trojici akcentů: **Jantar (`#F2A359`)** pro pohyb a akci, **Šalvěj (`#5CC497`)** pro výhru/zavření, **Indigo (`#9FA8DA`)** pro Ozvěnu.
- Doporučené názvy: `slide_problem_wall.png`, `slide_echo_concept.png`, `slide_reforged_sword.png`.
