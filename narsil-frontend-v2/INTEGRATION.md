# Návod k napojení frontendu na aplikaci (`NarsilAPI` & `NarsilFrontend`)

Frontend je navržený tak, aby nevyžadoval žádný build krok (Vite/Webpack/npm) a dal se napojit třemi způsoby:

1. **REST API (`mode: 'rest'`)** – klasický webový backend
2. **Nativní WebView Bridge (`mode: 'bridge'`)** – např. Android `JavascriptInterface` / iOS `WKScriptMessageHandler`
3. **Vlastní JS handlery (`handlers`)** – přímé napojení na existující JS vrstvu

---

## 1. Napojení na REST API

Před načtením `api.js` (nebo hned po něm přes `NarsilAPI.configure(...)`) nastavte konfiguraci:

```html
<script>
  window.NARSIL_CONFIG = {
    mode: 'rest',
    baseUrl: 'https://vas-backend.cz/api',
    headers: {
      Authorization: 'Bearer VAS_TOKEN',
    },
  };
</script>
<script src="api.js"></script>
<script src="app.js"></script>
```

### Očekávané endpointy

| Akce | Metoda & Cesta | Request | Očekávaná odpověď |
| --- | --- | --- | --- |
| Načtení stavu | `GET /state` | – | JSON objekt dle schématu níže |
| Ověření úkolu (text) | `POST /tasks/:taskId/verify` | `{ "proofType": "text", "proofText": "..." }` | `{ "ok": true, "rewardMinutes": 30 }` |
| Ověření úkolu (fotka) | `POST /tasks/:taskId/verify` | `multipart/form-data` (`taskId`, `proofType`, `file`) | `{ "ok": true, "rewardMinutes": 30 }` |
| Přidání úkolu do plánu | `POST /plan/tasks` | `{ "title": "...", "rewardMinutes": 20 }` | `{ "id": "...", "title": "...", "rewardMinutes": 20 }` |
| Podpis plánu na zítra | `POST /plan/sign` | `{ "stepGoal": 6000, "tasks": [...] }` | `{ "ok": true, "message": "Plán uložen" }` |

---

## 2. Napojení přes Android / iOS WebView Bridge

Pokud běží frontend uvnitř mobilní aplikace (např. Android `WebView`), vystavte objekt `window.NarsilBridge` a nastavte `mode: 'bridge'`:

```js
window.NARSIL_CONFIG = { mode: 'bridge' };
```

Frontend pak volá metody na `window.NarsilBridge`:
- `getState()` → vrací JSON string (nebo objekt) se stavem aplikace
- `verifyTask(jsonString)` → `{ taskId, taskTitle, rewardMinutes, proofType, proofText }`
- `addPlanTask(jsonString)` → `{ title, rewardMinutes }`
- `signPlan(jsonString)` → `{ stepGoal, tasks }`
- `openSquats()` → otevře nativní obrazovku / počítadlo dřepů
- `openSettings()` → otevře nativní nastavení

---

## 3. Přímé JS napojení a události

### Vlastní handlery
```js
window.NarsilFrontend.init({
  handlers: {
    async getState() {
      return myStore.loadState();
    },
    async verifyTask(payload) {
      return myStore.verify(payload);
    },
    async signPlan(plan) {
      return myStore.savePlan(plan);
    },
  },
});
```

### Aktualizace dat zvenčí za běhu
```js
window.NarsilFrontend.setState({
  home: {
    earnedMinutes: 90,
    streakDays: 7,
    stepsToday: 6200,
    tasks: [/* ... */],
  },
});
```

### DOM události na `window`
Frontend také vysílá `CustomEvent` události, na které lze naslouchat bez úpravy `app.js`:
- `narsil:navigate` (`detail: { screen }`)
- `narsil:verify-task` (`detail: { taskId, taskTitle, rewardMinutes, proofType, proofText, hasFile }`)
- `narsil:add-plan-task` (`detail: { title, rewardMinutes }`)
- `narsil:sign-plan` (`detail: { stepGoal, tasks }`)
- `narsil:open-squats`
- `narsil:open-settings`

---

## 4. JSON schéma stavu (`GET /state` / `setState`)

```json
{
  "protection": {
    "active": true,
    "label": "Ochrana aktivní",
    "clockText": "5:54",
    "statusIcons": "● 5G ︽"
  },
  "home": {
    "earnedMinutes": 45,
    "streakDays": 6,
    "stepsToday": 4820,
    "tasks": [
      {
        "id": "task-1",
        "title": "Dokončit kapitolu 3 z fyziky",
        "rewardMinutes": 30,
        "done": false
      }
    ]
  },
  "mirror": {
    "reclaimedHours": 13,
    "reclaimedTotalMinutes": 780,
    "feedTimeDelta": "−58 %",
    "squatsPerDay": 24,
    "bossDefeatedDays": 5,
    "criticalHour": "22–23 h",
    "weeklyTrend": [
      { "day": "Po", "percent": 42 },
      { "day": "Út", "percent": 58 }
    ]
  },
  "badges": {
    "items": [
      { "id": "badge-1", "icon": "↪", "title": "Odešel jsem sám 10×", "status": "Získáno" }
    ],
    "partyGoal": {
      "title": "Společný cíl",
      "currentSteps": 74200,
      "targetSteps": 100000
    }
  },
  "plan": {
    "stepGoal": 6000,
    "minSteps": 1000,
    "maxSteps": 20000,
    "stepIncrement": 1000,
    "tasks": [
      { "id": "plan-1", "title": "Dokončit kapitolu 3 z fyziky", "rewardMinutes": 30 }
    ]
  }
}
```
