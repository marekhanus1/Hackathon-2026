# Důkazy pro demo ověření (522)

Úkoly odpovídají demo smlouvě (`FakeContractRepository.demoContract`): Běh (Strava), Lineární algebra (fotka),
Dopsat report (text), Uklidit pokoj (fotka).

## Textové důkazy

`texty.json` – 2× ANO, 2× NE. Stejné texty jsou v kódu `cz.narsil.verify.demo.DemoProofs`.

## Fotky (nafotit v den dema, do galerie demo telefonu)

| Soubor | Úkol | Co je na fotce | Čekaná odpověď |
| --- | --- | --- | --- |
| `zapisky-algebra.jpg` | Lineární algebra | Sešit s vypočítanými příklady, čitelná „kapitola 3“ | ANO |
| `pokoj-uklizeny.jpg` | Uklidit pokoj | Uklizený stůl a postel | ANO |
| `kocka.jpg` | Lineární algebra | Cokoli nesouvisejícího (kočka, strop) | NE |
| `zapisky-algebra.jpg` podruhé | jiný úkol | Stejná fotka znovu | NE bez LLM (stejný otisk SHA-256) |

Fotky do gitu nedávat (soukromí); stačí je mít v telefonu.

## Strava (MOVEMENT – Běh)

Ráno v den dema nahrát na testovací účet Stravy krátký běh nebo chůzi ≥ 15 min (aktivita od 4:00).
Bez Stravy: úkol Běh ověřit fotkou snímku aktivity z hodinek.

## Scénáře v sále

| Situace | Co nastavit | Co se stane |
| --- | --- | --- |
| Dobrá Wi-Fi | nic | Skutečné ověření přes LLM do 5 s |
| Slabá / žádná Wi-Fi | nic | „Ověří se později“; po připojení se úkol ověří sám (`NetworkRetryTrigger`) |
| Riziko, že síť v sále nepůjde vůbec | demo panel → „ověření vždy ANO“ | Po 1,2 s ANO bez sítě (`DemoAwareTaskVerifier`), označeno jen v demo panelu |
