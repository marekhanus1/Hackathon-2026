"""
Generátor připravených dat (430) podle mockdata/schema.md.

Spuštění z kořene repa:  python mockdata/generuj.py
Výstup: mockdata/mirror.json, mockdata/value_map.json, mockdata/leaderboard.json

Příběh 21 dní: minut feedu ubývá, „Zavřít“ přibývá, série roste. Čísla XP podle docs/GAMIFICATION.md
(Zavřít 50 – max 5× denně, dřepy 20, kroky 10, půjčka 0, smlouva 100, hodnocení 5 – max 10× denně,
boss 21:00–22:00 +150). Konec dat sedí na začátek dema (DemoTuning, 230): XP těsně pod úrovní
(480/500), série 4, 1 žolík, postava FIT – 20 dřepů v demu spustí novou úroveň.
Data jsou deterministická (žádná náhoda), takže každé spuštění dá stejné soubory.
"""
import datetime as dt
import json
import os

HERE = os.path.dirname(os.path.abspath(__file__))
START = dt.date(2026, 11, 7)  # den 1; den 21 = 27. 11. („včera“ v demu 28. 11.)
CRITICAL_HOUR = 21            # boss 21:00–22:00 (GAMIFICATION.md 7)
XP_IN_LEVEL_AT_END = 480  # 20 XP pod úrovní = 20 dřepů v demu spustí animaci

XP = {"CLOSED": 50, "SQUATS": 20, "PAID_STEPS": 10, "BORROWED": 0}
OUT = {"C": "CLOSED", "S": "SQUATS", "P": "PAID_STEPS", "B": "BORROWED"}
APPS = {"ig": "instagram", "tt": "tiktok", "yt": "youtube", "ff": "firefox"}

# Zásahy: "HH:MM app povrch výsledek čemu_se_vyhýbá" (avoiding: nuda/unava/ukol/samota/-)
INTERVENTIONS = {
    1: ["20:10 ig REELS P nuda", "21:47 tt FEED B unava", "22:30 ig REELS S ukol"],
    2: ["19:40 tt FEED P nuda", "21:15 tt FEED B unava", "22:05 ff SHORTS B unava"],
    3: ["20:30 ig REELS S ukol", "21:30 tt FEED P nuda"],
    4: ["19:55 tt FEED B samota", "21:20 ig REELS B unava", "22:40 tt FEED P nuda"],
    5: ["20:20 tt FEED C nuda", "21:35 ig REELS P unava", "22:50 tt FEED B unava"],
    6: ["20:05 tt FEED S ukol", "21:10 ig REELS C nuda"],
    7: ["19:30 ff VIDEO P nuda", "21:40 tt FEED B samota", "22:15 ig REELS C unava"],
    8: ["20:45 tt FEED C ukol", "21:25 ig REELS S nuda"],
    9: ["20:00 tt FEED C nuda", "21:05 tt FEED P unava", "22:20 ig REELS S ukol"],
    10: ["19:50 ig REELS C ukol", "21:30 tt FEED C nuda"],
    11: ["20:15 tt FEED C nuda", "21:45 ff SHORTS S unava"],
    12: ["19:45 tt FEED P samota", "21:20 tt FEED C nuda", "22:35 ig REELS B unava"],
    13: ["20:30 ig REELS C ukol", "21:15 tt FEED C nuda", "22:10 tt FEED S unava"],
    14: ["20:05 tt FEED C nuda", "21:50 ig REELS P samota"],
    15: ["19:40 tt FEED C ukol", "21:00 tt FEED C nuda", "22:25 yt SHORTS S unava"],
    16: ["20:20 ig REELS C nuda", "21:35 tt FEED C ukol"],
    17: ["20:10 tt FEED S unava", "21:25 tt FEED B unava", "22:45 ig REELS C nuda"],  # relaps
    18: ["20:00 tt FEED C ukol", "21:10 ig REELS C nuda", "22:00 tt FEED S unava"],
    19: ["19:35 ig REELS C nuda", "21:20 tt FEED C ukol"],
    20: ["20:25 tt FEED C nuda", "21:40 yt SHORTS C ukol"],
    21: ["20:10 ig REELS C ukol", "21:30 tt FEED C nuda", "22:20 tt FEED C unava"],
}
FULFILLED = {3, 6, 8, 10, 11, 13, 15, 16, 18, 19, 20, 21}  # série na konci 4 (18–21), 17 relaps
EXTRA_SQUATS = {19: 50}  # 50 dřepů navíc = žolík (GAMIFICATION.md 4)
FEED = [142, 135, 118, 128, 110, 95, 104, 88, 82, 70, 66, 72, 58, 61, 49, 45, 63, 41, 38, 36, 33]
STEPS = [4200, 3900, 6400, 4500, 5200, 6800, 4900, 6900, 5600, 7200, 7400, 5800, 7600, 6100,
         8100, 7900, 5300, 8400, 8600, 8800, 8900]
PLANNED = [45, 30, 50, 20, 40, 55, 25, 60, 35, 50, 45, 30, 55, 40, 60, 50, 20, 55, 60, 45, 50]
# Minuty odměn ověřených úkolů (souboj s algoritmem: reclaimed = Zavřít × 15 + odměny)
TASK_REWARDS = [0, 0, 20, 0, 10, 30, 0, 40, 20, 40, 50, 20, 40, 30, 60, 50, 10, 60, 50, 50, 60]

# Mapa hodnoty: hodnocení videí (funkce zapnutá od 8. dne). Kanály jsou vymyšlené.
CHANNELS = {
    "Rust za kafe": ("LONG", 22),
    "Fyzika v kostce": ("LONG", 15),
    "Kuchyně za minutu": ("SHORT", 1),
    "Drama Recap CZ": ("SHORT", 1),
    "Memes denně": ("SHORT", 1),
    "Kočičí svět": ("SHORT", 1),
}
# (den, kanál, název, záměr, plánované, % dokoukáno, hodnocení)
RATINGS = [
    (8, "Rust za kafe", "Ownership za 20 minut", "pochopit borrow checker", True, 95, 5),
    (8, "Drama Recap CZ", "Kdo s kým TENTOKRÁT", "", False, 90, 2),
    (9, "Memes denně", "Když je pondělí #312", "", False, 100, 2),
    (9, "Kuchyně za minutu", "Těstoviny za 60 s", "nápad na večeři", False, 80, 4),
    (10, "Rust za kafe", "Lifetimes bez bolesti", "lifetimes", True, 88, 5),
    (10, "Drama Recap CZ", "Rozchod roku?!", "", False, 85, 1),
    (11, "Fyzika v kostce", "Proč je nebe modré", "", True, 92, 4),
    (11, "Kočičí svět", "Kočka vs. okurka", "", False, 100, 3),
    (12, "Drama Recap CZ", "Další skandál", "", False, 95, 1),
    (12, "Memes denně", "Když je pondělí #318", "", False, 90, 2),
    (12, "Rust za kafe", "Traity v praxi", "traity", True, 70, 4),
    (13, "Kuchyně za minutu", "Polévka za 60 s", "", False, 75, 4),
    (13, "Drama Recap CZ", "Kdo lže?", "", False, 90, 2),
    (14, "Fyzika v kostce", "Černé díry za 15 minut", "na odpočinek", True, 96, 5),
    (14, "Memes denně", "Když je pondělí #321", "", False, 85, 1),
    (15, "Rust za kafe", "Async Rust", "async", True, 90, 5),
    (15, "Kočičí svět", "Kočka a krabice", "", False, 60, 3),
    (16, "Drama Recap CZ", "Shrnutí týdne", "", False, 88, 2),
    (16, "Rust za kafe", "Chybové typy", "error handling", True, 85, 4),
    (17, "Drama Recap CZ", "Konec přátelství?", "", False, 98, 1),
    (17, "Memes denně", "Když je pondělí #330", "", False, 100, 2),
    (17, "Drama Recap CZ", "Reakce na reakci", "", False, 92, 1),
    (18, "Fyzika v kostce", "Kvantové tunelování", "", True, 90, 5),
    (19, "Rust za kafe", "Makra pro začátečníky", "makra", True, 93, 5),
    (19, "Kuchyně za minutu", "Rizoto za 60 s", "", False, 70, 3),
    (20, "Fyzika v kostce", "Entropie lidsky", "", True, 88, 4),
    (18, "Kuchyně za minutu", "Kari za 60 s", "", False, 65, 4),
    (18, "Drama Recap CZ", "Kdo vyhrál hádku", "", False, 80, 2),
    (18, "Rust za kafe", "Iterátory", "iterátory", True, 91, 5),
    (19, "Fyzika v kostce", "Proč padají letadla? (nepadají)", "", True, 89, 4),
    (19, "Memes denně", "Když je pondělí #335", "", False, 70, 2),
    (19, "Kočičí svět", "Kočka na klávesnici", "", False, 55, 3),
    (20, "Rust za kafe", "Unsafe Rust", "unsafe", True, 86, 4),
    (20, "Kuchyně za minutu", "Palačinky za 60 s", "", False, 72, 4),
    (20, "Drama Recap CZ", "Usmíření?", "", False, 75, 2),
    (21, "Rust za kafe", "Přednáška: Rust v embedded", "přednáška o Rustu", True, 97, 5),
    (21, "Fyzika v kostce", "Teplota a pohyb molekul", "", True, 90, 5),
    (21, "Memes denně", "Když je pondělí #340", "", False, 60, 2),
]

AVATAR_STATE = {"EXHAUSTED": 1, "TIRED": 2, "OK": 3, "FIT": 4, "THRIVING": 5}


def avatar_for(fulfilled_7):
    if fulfilled_7 <= 1:
        return "EXHAUSTED"
    if fulfilled_7 == 2:
        return "TIRED"
    if fulfilled_7 <= 4:
        return "OK"
    if fulfilled_7 <= 6:
        return "FIT"
    return "THRIVING"


def parse(entry):
    time, app, surface, outcome, avoiding = entry.split()
    return {"time": time, "app": APPS[app], "surface": surface, "outcome": OUT[outcome],
            "avoiding": None if avoiding == "-" else avoiding}


def build():
    days = []
    total_xp = 0
    streak = jokers = 0
    won_days = 0
    closes_total = 0
    for i in range(1, 22):
        date = START + dt.timedelta(days=i - 1)
        ints = [parse(e) for e in INTERVENTIONS[i]]
        closes = [x for x in ints if x["outcome"] == "CLOSED"]
        closes_total += len(closes)
        xp = sum(XP[x["outcome"]] for x in ints if x["outcome"] != "CLOSED") + XP["CLOSED"] * min(5, len(closes))
        if i in FULFILLED:
            xp += 100
        in_boss = [x for x in ints if int(x["time"][:2]) == CRITICAL_HOUR]
        boss = all(x["outcome"] == "CLOSED" for x in in_boss)
        if boss:
            xp += 150
        rated = [r for r in RATINGS if r[0] == i]
        xp += 5 * min(10, len(rated))
        squats = 20 * sum(1 for x in ints if x["outcome"] == "SQUATS") + EXTRA_SQUATS.get(i, 0)
        jokers = min(3, jokers + EXTRA_SQUATS.get(i, 0) // 50)
        if i in FULFILLED:
            streak += 1
        elif jokers > 0:
            jokers -= 1
        else:
            streak = 0
        reclaimed = 15 * len(closes) + TASK_REWARDS[i - 1]
        feed = FEED[i - 1]
        won = reclaimed > feed
        won_days += won
        weights = {}
        for x in ints:
            weights[x["app"]] = weights.get(x["app"], 0) + 1
        apps = sorted(weights)
        by_app, left = {}, feed
        for n, a in enumerate(apps):
            share = left if n == len(apps) - 1 else round(feed * weights[a] / len(ints))
            by_app[a] = share
            left -= share
        days.append({
            "day_index": i,
            "date": date.isoformat(),
            "feed_minutes": feed,
            "reclaimed_minutes": reclaimed,
            "planned_minutes": PLANNED[i - 1],
            "by_app": by_app,
            "interventions": ints,
            "steps": STEPS[i - 1],
            "squats": squats,
            "debt_minutes": 10 * sum(1 for x in ints if x["outcome"] == "BORROWED"),
            "contract_fulfilled": i in FULFILLED,
            "battle_won": won,
            "xp_gained": xp,
        })
        total_xp += xp
    last7 = [d for d in days if d["day_index"] >= 15]
    reclaimed_week = sum(d["reclaimed_minutes"] for d in last7)
    insights = [
        f"Nejčastěji utíkáš na TikTok kolem {CRITICAL_HOUR}:00, když se ti nechce do úkolů.",
        f"Za poslední týden sis vzal zpátky {reclaimed_week // 60} h {reclaimed_week % 60} min.",
        f"Od feedu jsi {closes_total}× odešel sám.",
        f"Minut feedu ubylo z {FEED[0]} na {FEED[-1]} denně.",
        "Relaps 23. 11. nevadí – další den jsi navázal a série zase roste.",
    ]
    mirror = {"critical_hour": f"{CRITICAL_HOUR:02d}:00", "insights": insights, "days": days}
    stats = {
        "total_xp": total_xp, "level": 1 + total_xp // 500, "xp_in_level": total_xp % 500,
        "streak": streak, "jokers": jokers, "won_days": won_days,
        "avatar": avatar_for(sum(1 for d in last7 if d["contract_fulfilled"])),
        "reclaimed_week": reclaimed_week, "steps_week": sum(d["steps"] for d in last7),
    }
    return mirror, stats


def value_map():
    ratings = []
    for day, ch, title, intent, planned, watched, rating in RATINGS:
        ratings.append({
            "date": (START + dt.timedelta(days=day - 1)).isoformat(),
            "title": title, "channel": ch, "format": CHANNELS[ch][0], "intent": intent,
            "planned": planned, "watched_percent": watched, "rating": rating,
        })
    channels = []
    for ch, (fmt, length) in CHANNELS.items():
        rs = [r for r in ratings if r["channel"] == ch]
        if not rs:
            continue
        avg_w = round(sum(r["watched_percent"] for r in rs) / len(rs))
        avg_r = round(sum(r["rating"] for r in rs) / len(rs), 1)
        # SHORT = typicky víc zhlédnutí než hodnocení (hodnotí se jen některá)
        views = len(rs) * (8 if fmt == "SHORT" else 1)
        minutes = round(views * length * avg_w / 100)
        verdict = "TRAP" if avg_w >= 70 and avg_r <= 2 else ("KEEP" if avg_r >= 4 else "NEUTRAL")
        channels.append({"channel": ch, "format": fmt, "views": views, "minutes": minutes,
                         "avg_watched_percent": avg_w, "avg_rating": avg_r, "verdict": verdict})
    channels.sort(key=lambda c: (-c["minutes"], c["channel"]))
    return {"ratings": ratings, "channels": channels}


def leaderboard(stats):
    me = {"name": "Ty", "is_me": True, "reclaimed_minutes": stats["reclaimed_week"], "steps": stats["steps_week"],
          "streak_days": stats["streak"], "level": stats["level"], "avatar_state": AVATAR_STATE[stats["avatar"]]}
    others = [
        {"name": "Bára", "is_me": False, "reclaimed_minutes": 610, "steps": 61200, "streak_days": 9, "level": 11, "avatar_state": 5},
        {"name": "Ondra", "is_me": False, "reclaimed_minutes": 240, "steps": 38900, "streak_days": 2, "level": 4, "avatar_state": 2},
        {"name": "Kuba", "is_me": False, "reclaimed_minutes": 410, "steps": 47300, "streak_days": 5, "level": 7, "avatar_state": 4},
    ]
    members = sorted(others + [me], key=lambda m: -m["reclaimed_minutes"])
    goal = 200_000
    return {
        "party_name": "Kovárna",
        "challenge": {"title": "200 000 kroků za týden", "steps_goal": goal,
                      "steps_done": sum(m["steps"] for m in members), "ends": "2026-11-30"},
        "members": members,
    }


def validate(mirror, vm, lb, stats):
    days = mirror["days"]
    allowed_apps = {"instagram", "tiktok", "youtube", "firefox"}
    assert len(days) == 21 and [d["day_index"] for d in days] == list(range(1, 22))
    for d in days:
        assert sum(d["by_app"].values()) == d["feed_minutes"], d["day_index"]
        assert set(d["by_app"]) <= allowed_apps
        assert d["battle_won"] == (d["reclaimed_minutes"] > d["feed_minutes"])
        for x in d["interventions"]:
            assert x["app"] in allowed_apps
            assert x["surface"] in {"FEED", "REELS", "SHORTS", "MESSAGES", "VIDEO"}
            assert x["outcome"] in {"CLOSED", "PAID_STEPS", "SQUATS", "BORROWED", "PLANNED_FREE"}
            assert x["avoiding"] in {"nuda", "unava", "ukol", "samota", None}
    assert days[-1]["feed_minutes"] < days[0]["feed_minutes"] / 3, "příběh: feedu ubývá"
    assert any(x["app"] == "firefox" for d in days for x in d["interventions"]), "Firefox musí být v demu vidět"
    assert 3 <= len(mirror["insights"]) <= 5
    for r in vm["ratings"]:
        assert 1 <= r["rating"] <= 5 and 0 <= r["watched_percent"] <= 100 and r["format"] in {"SHORT", "LONG"}
    assert any(c["verdict"] == "TRAP" for c in vm["channels"]) and any(c["verdict"] == "KEEP" for c in vm["channels"])
    ms = lb["members"]
    assert sum(m["is_me"] for m in ms) == 1
    assert [m["reclaimed_minutes"] for m in ms] == sorted((m["reclaimed_minutes"] for m in ms), reverse=True)
    assert lb["challenge"]["steps_done"] == sum(m["steps"] for m in ms)
    # Konec dat = začátek dema (DemoTuning, 230)
    assert stats["xp_in_level"] == XP_IN_LEVEL_AT_END, stats
    assert stats["streak"] == 4 and stats["jokers"] == 1 and stats["avatar"] == "FIT", stats


def main():
    mirror, stats = build()
    vm = value_map()
    lb = leaderboard(stats)
    validate(mirror, vm, lb, stats)
    for name, data in (("mirror.json", mirror), ("value_map.json", vm), ("leaderboard.json", lb)):
        with open(os.path.join(HERE, name), "w", encoding="utf-8", newline="\n") as f:
            json.dump(data, f, ensure_ascii=False, indent=2)
            f.write("\n")
    print("OK", stats)


if __name__ == "__main__":
    main()
