#!/usr/bin/env python3
"""Druhá verze pitche ve stylu Frutiger Aero (narsil-frontend-v2).

Vezme pitch/narsil-pitch.html a vyrobí pitch/narsil-pitch-aero.html:
- barvy prezentace přepne na Aero (nebe, sklo, zelený lesk),
- na slidech s maketami telefonu ukáže skutečné screenshoty z pitch/snimky-aero/*.png
  (původní HTML makety zůstanou v DOM jen skryté, aby jejich skripty dál běžely),
- přidá galerii obrazovek nového vzhledu za slide Demo.

Spuštění: python3 pitch/make-aero-pitch.py  (znovu po každé změně původního pitche nebo screenshotů)
"""
from pathlib import Path
import re

ROOT = Path(__file__).resolve().parent
src = (ROOT / "narsil-pitch.html").read_text(encoding="utf-8")

# slide -> screenshot (cesty relativní k pitch/)
# Ozvěna, dřepy a hodnocení videa zatím z původních maket (screenshot by zasáhl do dat / ukázal kameru).
SHOTS = {
    "s06": "snimky-aero/01-ukoly.jpg",
    "s07": "snimky-aero/02-plan.jpg",
    "s11": "snimky-aero/06-zrcadlo.jpg",
}
GALLERY = [
    ("snimky-aero/01-ukoly.jpg", "Úkoly"),
    ("snimky-aero/07-overeni.jpg", "Ověření úkolu"),
    ("snimky-aero/02-plan.jpg", "Plán na zítra"),
    ("snimky-aero/06-zrcadlo.jpg", "Zrcadlo"),
    ("snimky-aero/08-odznaky.jpg", "Odznaky"),
]

AERO_CSS = """
<style>
/* ===== Frutiger Aero (narsil-frontend-v2) – přebíjí barvy původního pitche ===== */
:root{
  --bg:#EAF8FF; --surface:rgba(255,255,255,.86); --ink:#063B5C; --muted:#3F6B78; --line:rgba(255,255,255,.95);
  --amber:#0A7FC0; --amberD:#36BB45; --sage:#168231; --sageD:#7EE06A; --tw:#2B6CC4; --twD:#8EC9FF;
  --gold:#E0A500; --gold2:#FFD166; --debt:#D2861A; --alg:#6E5BA8;
  --f:'Trebuchet MS','Manrope','Segoe UI',system-ui,sans-serif;
}
#stage{background:
  radial-gradient(900px 520px at 12% 8%,rgba(255,255,255,.95),transparent 60%),
  radial-gradient(800px 600px at 92% 95%,rgba(150,240,120,.45),transparent 60%),
  linear-gradient(170deg,#c9efff 0%,#eefbff 45%,#e6ffe0 100%)}
.card{background:linear-gradient(145deg,rgba(255,255,255,.95),rgba(207,250,255,.8));border:1.5px solid #fff;
  box-shadow:inset 0 2px #fff,0 8px 22px rgba(0,66,112,.16)}
#prog{background:linear-gradient(#adf87a,#22af42)}
.phone{background:linear-gradient(160deg,#ffffff,#cdeeff 55%,#7cc8f0);box-shadow:0 0 0 2px #fff,0 30px 70px rgba(0,60,120,.32)}
.phone .scr{background:#159de9}
.phone.shot .scr > :not(.shot):not(.livev){visibility:hidden}
.phone .scr img.shot{position:absolute;inset:0;width:100%;height:100%;object-fit:cover;z-index:20;border-radius:44px}
.phone.live .scr img.shot{display:none}
/* galerie nového vzhledu */
#sAero .row{position:absolute;left:110px;right:110px;top:300px;display:flex;justify-content:space-between}
#sAero figure{margin:0;display:flex;flex-direction:column;align-items:center;gap:22px}
#sAero .mini{width:300px;height:640px;border-radius:44px;padding:11px;background:linear-gradient(160deg,#fff,#cdeeff 55%,#7cc8f0);
  box-shadow:0 0 0 2px #fff,0 22px 50px rgba(0,60,120,.28)}
#sAero .mini img{width:100%;height:100%;object-fit:cover;border-radius:34px;display:block;background:#159de9}
#sAero figcaption{font:800 28px/1 var(--f);color:var(--ink)}
</style>
"""

out = src.replace("<title>Narsil · pitch</title>", "<title>Narsil · pitch (Aero)</title>")
out = out.replace("</head>", AERO_CSS + "</head>", 1)

for sid, img in SHOTS.items():
    # první telefon na slidu: přidat třídu shot a obrázek hned za <div class="scr">
    m = re.search(rf'<section class="sl[^"]*" id="{sid}"', out)
    assert m, sid
    i = out.index('<div class="phone', m.end())
    j = out.index('<div class="scr">', i) + len('<div class="scr">')
    head = out[i:j].replace('class="phone', 'class="phone shot', 1)
    out = out[:i] + head + f'<img class="shot" src="{img}" alt="">' + out[j:]

gallery = "\n".join(
    f'    <figure data-s="{n + 1}"><div class="mini"><img src="{img}" alt="{cap}"></div><figcaption>{cap}</figcaption></figure>'
    for n, (img, cap) in enumerate(GALLERY)
)
slide = f'''<!-- NOVÝ VZHLED (Aero) -->
<section class="sl" id="sAero" data-steps="{len(GALLERY)}" data-title="Nový vzhled">
  <div class="kick">Frutiger Aero</div>
  <h2 class="t">Sklo, bubliny a <em>modré nebe</em></h2>
  <div class="row">
{gallery}
  </div>
</section>

'''
k = out.index('<section class="sl feat" id="s07"')
k = out.rindex("<!--", 0, k) if out.rfind("<!--", 0, k) > out.rfind("</section>", 0, k) else k
out = out[:k] + slide + out[k:]

(ROOT / "narsil-pitch-aero.html").write_text(out, encoding="utf-8")
print("hotovo:", ROOT / "narsil-pitch-aero.html")
