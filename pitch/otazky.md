# Otázky poroty s odpověďmi (410)

Každá odpověď do 30 s (cca 60–80 slov). **[T]** = technická otázka, odpovídá Marek (odpovědi s ním zkontrolovat); ostatní Štěpán.
Zásada: odpovídat poctivě – co je v MVP postavené, co nasimulované a co je roadmapa.

## Soukromí a bezpečnost

**1. Služba přístupnosti vidí celou obrazovku. Proč bych vám měl věřit? [T]**
Čteme jen to, co potřebujeme: která aplikace je v popředí a jestli je to feed, Reels, zprávy nebo video – podle struktury obrazovky, ne podle obsahu. Chatovací aplikace nesledujeme vůbec, uvnitř Instagramu se obrazovka zpráv nepočítá. Nic z toho neopouští telefon. Jediné, co jde ven, je název YouTube videa a váš plán do AI, aby šlo poznat plánovaný obsah.

**2. Proč chcete polohu, když hlídáte jen Wi-Fi? [T]**
Android od verze 10 vrací název Wi-Fi sítě jen aplikacím s oprávněním k poloze. Polohu samotnou nečteme ani neukládáme – potřebujeme jen poznat, že jste na domácí síti. Bez oprávnění aplikace bere stav jako „nevím“ a nezasahuje. Proto taky ruční přepínač „jsem doma“.

**3. Kam jdou moje večerní videa?**
Nikam. Video zůstává v úložišti aplikace v telefonu a přehraje ho jen Ozvěna. Žádný cloud, žádný server. Když aplikaci smažete, smažou se i videa.

**4. Co přesně posíláte do AI? [T]**
Dvě věci a jen když je potřeba: název videa a seznam plánovaného obsahu – pro párování „je to v plánu?“. A při ověření úkolu fotku nebo text důkazu zmenšený na minimum. Nikdy obsah zpráv, nikdy video ze smlouvy. Bez sítě funguje záloha klíčovými slovy a ověření počká.

**5. Co vidí Parťák?**
Nic z vašich dat. Dostane jen zprávu: aplikace se přestala hlásit, nebo dluh přerostl limit. Žádné minuty, žádné aplikace, žádná videa. V MVP je to ukázková notifikace, skutečné hlídání výpadku je v roadmapě.

## Obcházení

**6. Prostě to vypnu, ne?**
Ano, a to je v pořádku – jsme pomocník, ne dozorce. Ale smlouvu jste si napsal sám se sebou a o vypnutí se dozví váš Parťák. Kód se dá obejít, lidi ne. A protože nic neblokujeme, není moc důvodu aplikaci obcházet: pokračovat jde vždycky, jen to něco stojí.

**7. Můžu třást telefonem místo kroků nebo podvádět dřepy?**
Můžete – a podvádíte sám sebe. Smlouvu píšete vy pro sebe, ne pro nás. Dřepy počítá kamera podle úhlu v kolenou a pohybu kyčlí, takže mávání telefonem nepomůže, ale nehrajeme si na policii. Rozhodnutí 11 v briefu: podvádění vědomě neřešíme.

**8. Co když otevřu Instagram v prohlížeči?**
Detekce hlídá chování, ne seznam aplikací: nekonečné scrollování a řetězení videí. Instagram, TikTok a YouTube v Chromu poznáme podle adresy. To je přesně důvod, proč blokovací aplikace prohrávají a my ne.

## Produkt a trh

**9. Čím se lišíte od one sec nebo Opalu?**
Jsou to dobré aplikace, ale stojí na zábraně: one sec přidá před otevřením aplikace pauzu, Opal blokuje aplikace podle času a rozvrhu. Narsil nikde nic neblokuje a zasahuje jen doma, u nekonečného obsahu, po pěti minutách. Místo zdi používá vaše vlastní video, místo trestu pohyb a místo limitu plán: plánovaný obsah je zdarma.

**10. Proč jen Android? Co iOS? [T]**
iOS nedovolí aplikaci rozpoznat, jestli jste ve feedu, nebo ve zprávách, a nepustí překryvné okno přes jinou aplikaci. Screen Time API umí jen blokovat celé aplikace – a to je přesně model, který nechceme. Android má službu přístupnosti, takže tam jde postavit pomocník místo zámku.

**11. Projde to přes Google Play? [T]**
Služba přístupnosti má na Google Play přísná pravidla a vyžaduje zdůvodnění. Pro hackathon distribuujeme APK mimo Play (rozhodnutí 16). Do budoucna je cesta podat žádost s jasným účelem – pomoc uživateli s jeho vlastním cílem – a minimem čtených dat, což náš návrh splňuje.

**12. Je to jen pro studenty?**
Ne. Mechanismus je univerzální: komfortní zóna, nekonečný obsah, vlastní hlas. Pro studenty to začíná, protože tam je bolest největší. Stejně to funguje na home office – vlastní zóny jsou v roadmapě – a stejný mechanismus jde rozšířit na Netflix, hry nebo zprávy.

**13. Jak vyděláte peníze?**
Na hackathonu to neřešíme do detailu. Přirozený model je freemium: jádro zdarma, placené rozšíření pro party a výzvy, pokročilé zóny a statistiky. Žádné reklamy – aplikace proti feedu, která by žila z pozornosti, by popírala sama sebe.

## Věda a etika

**14. Má to nějaký vědecký základ?**
Opíráme se o tři známé principy. Prokrastinace jako regulace emocí – útěk před nepříjemným pocitem, proto otázka „Čemu se vyhýbáš?“. Předběžný závazek: rozhodnutí v klidu večer místo v pokušení. A tření: malá cena v okamžiku impulzu stačí, aby se člověk zastavil. Pohyb navíc mění fyzický stav, který chuť scrollovat často vyvolává.

**15. Není gamifikace jen další droga?**
Proto ji stavíme obráceně: nejvíc bodů je za odchod od feedu, ne za čas v aplikaci. Nic nikdy body nebere, takže nevzniká strach o sérii – a série má žolíky. Strop na body za „Zavřít“ zabraňuje farmení. Cílem je, abyste v aplikaci trávili co nejméně času.

**16. Nemůže pohyb jako cena někomu ublížit?**
Pohyb je lehký: 20 dřepů nebo kroky, které stejně během dne ujdete. Vždycky jde i půjčka bez pohybu, aplikace nikdy nic nezamkne. Do budoucna: volba typu aktivity, den volna a horní strop – je to v rizicích briefu.

**17. Co když mi video z minulosti přitíží – mám špatný den?**
Video natáčíte vy, večer, s vědomím, k čemu slouží – a můžete ho kdykoli přetočit. Otázka „Čemu se vyhýbáš?“ není výčitka, odpověď je nepovinná. A když videa nechcete, Ozvěna ukáže jen text vašeho dnešního plánu.

## Technika

**18. Jak poznáte feed od zpráv? [T]**
Služba přístupnosti dává strukturu obrazovky: identifikátory prvků a typy oken. Pro Instagram, TikTok a YouTube máme seznam obrazovek – Pro tebe, Reels, Shorts proti Direct a chatům. V nejistotě bereme obrazovku jako zprávy, tedy nezasahujeme. Raději zásah propásnout než otravovat.

**19. Jak spolehlivé je počítání dřepů? [T]**
ML Kit Pose Detection přímo v telefonu, bez sítě. Počítáme hloubku dřepu ze dvou signálů: úhel v koleni a zkrácení stehna vůči trupu – to funguje i čelem ke kameře. Zákmity tlumíme vyhlazením a minimálním časem mezi dřepy. Prahy jsou záměrně tolerantní kvůli horšímu světlu a před demem je ověřujeme v sále (3 lidé × 20 dřepů).

**20. Co jste za 20 hodin opravdu postavili a co je nasimulované?**
Postavené a běží spolu: večerní smlouva s videem, detekce domova a feedu, Ozvěna, dřepy kamerou, kroky, dluh, XP, úrovně, odznaky a souboj s algoritmem. Postavené a otestované, napojení do aplikace se dokončuje: párování videa s plánem a ověření úkolů přes AI, Strava. Nasimulované na připravených datech: Zrcadlo, mapa hodnoty, žebříček party a notifikace Parťáka. Roadmapa: vlastní zóny a předpověď kritické hodiny.
*(Před pitchem aktualizovat podle stavu G3/G4.)*
