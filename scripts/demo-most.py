#!/usr/bin/env python3
"""Demo most (161): telefon ↔ prezentace. Jen standardní knihovna Pythonu.

  python3 scripts/demo-most.py            # prezentace + události z telefonu
  python3 scripts/demo-most.py --scrcpy   # navíc zrcadlení telefonu ve smyčce (po výpadku USB znovu)

Pak ve Firefoxu http://localhost:8000/narsil-pitch.html → S → „Sdílet okno telefonu“ (Narsil-live).

- servíruje složku pitch/ (místo python3 -m http.server),
- GET  /demo/events  – Server-Sent Events: řádky z `adb logcat -v raw -s NarsilDemo:I` (JSON z aplikace)
                       a stav mostu {"e":"bridge","adb":true|false,"device":"…"},
- POST /demo/cmd/<c> – `adb shell am broadcast -a cz.narsil.DEMO -p cz.narsil --es cmd <c>`
                       (prepare | home | away | auto | echo | xp | steps | reset | state),
- GET  /demo/status  – stav mostu jako JSON.

Pravidla pro demo telefon (viz paměť týmu): logcat se jen čte živě (žádné -c), žádný force-stop ani reconnect.
"""
import argparse
import json
import os
import queue
import shutil
import subprocess
import threading
import time
from http.server import SimpleHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
PITCH = ROOT / "pitch"
COMMANDS = {"prepare", "home", "away", "auto", "echo", "xp", "steps", "reset", "state"}


def find_adb() -> str:
    env = os.environ.get("ADB")
    if env:
        return env
    sdk = Path.home() / "Android/Sdk/platform-tools/adb"
    if sdk.exists():
        return str(sdk)
    return shutil.which("adb") or "adb"


ADB = find_adb()


class Hub:
    """Rozesílá události všem otevřeným prezentacím."""

    def __init__(self):
        self.lock = threading.Lock()
        self.clients: list[queue.Queue] = []
        self.status = {"e": "bridge", "adb": False, "device": None}

    def subscribe(self) -> queue.Queue:
        q: queue.Queue = queue.Queue(maxsize=200)
        with self.lock:
            self.clients.append(q)
        q.put(json.dumps(self.status))
        return q

    def unsubscribe(self, q):
        with self.lock:
            if q in self.clients:
                self.clients.remove(q)

    def publish(self, line: str):
        with self.lock:
            for q in list(self.clients):
                try:
                    q.put_nowait(line)
                except queue.Full:
                    pass

    def set_status(self, adb: bool, device):
        new = {"e": "bridge", "adb": adb, "device": device}
        if new != self.status:
            self.status = new
            self.publish(json.dumps(new))
            log(f"telefon: {'připojený ' + str(device) if adb else 'nepřipojený'}")


HUB = Hub()


def log(msg: str):
    print(time.strftime("%H:%M:%S"), msg, flush=True)


def device_name():
    try:
        out = subprocess.run([ADB, "devices", "-l"], capture_output=True, text=True, timeout=5).stdout
    except (OSError, subprocess.TimeoutExpired):
        return None
    for line in out.splitlines()[1:]:
        parts = line.split()
        if len(parts) >= 2 and parts[1] == "device":
            model = next((p.split(":", 1)[1] for p in parts if p.startswith("model:")), parts[0])
            return model
    return None


def logcat_loop():
    """Živě čte události aplikace. Po výpadku USB počká na telefon a začne znovu (jen nové řádky)."""
    while True:
        dev = device_name()
        HUB.set_status(dev is not None, dev)
        if dev is None:
            time.sleep(2)
            continue
        try:
            # Jen nové řádky: od teď podle hodin telefonu (staré „level“ by v prezentaci znovu slavilo).
            since = subprocess.run([ADB, "shell", "date", "+%m-%d %H:%M:%S.000"],
                                   capture_output=True, text=True, timeout=5).stdout.strip()
            proc = subprocess.Popen(
                [ADB, "logcat", "-v", "raw", "-T", since or "1", "-s", "NarsilDemo:I"],
                stdout=subprocess.PIPE, stderr=subprocess.DEVNULL, text=True, bufsize=1,
            )
        except (OSError, subprocess.TimeoutExpired) as e:
            log(f"adb nejde spustit ({e}) – nastav ADB=/cesta/k/adb")
            time.sleep(5)
            continue
        send_cmd("state")  # srovnat prezentaci se stavem telefonu
        for line in proc.stdout:
            line = line.strip()
            if line.startswith("{"):
                HUB.publish(line)
                log(f"← {line}")
        proc.wait()
        HUB.set_status(False, None)
        time.sleep(1)


def send_cmd(cmd: str) -> bool:
    try:
        r = subprocess.run(
            [ADB, "shell", "am", "broadcast", "-a", "cz.narsil.DEMO", "-p", "cz.narsil", "--es", "cmd", cmd],
            capture_output=True, text=True, timeout=10,
        )
    except (OSError, subprocess.TimeoutExpired):
        return False
    ok = r.returncode == 0
    log(f"→ {cmd} ({'ok' if ok else 'chyba: ' + (r.stderr or r.stdout).strip()})")
    return ok


def scrcpy_loop(binary: str, extra: list[str]):
    """Zrcadlení telefonu pro sdílení okna v prezentaci; po výpadku USB ho nahodí znovu."""
    env = dict(os.environ, ADB=ADB)  # stejné adb → scrcpy nerestartuje adb server
    args = [binary, "--window-title=Narsil-live", "--no-audio", "--no-control", "--max-size=1600", *extra]
    while True:
        if device_name() is None:
            time.sleep(2)
            continue
        log("scrcpy: start (okno Narsil-live – po výpadku ho ve Firefoxu nasdílej znovu)")
        try:
            subprocess.run(args, env=env, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
        except OSError as e:
            log(f"scrcpy nejde spustit ({e})")
            return
        log("scrcpy: skončilo, za 2 s znovu")
        time.sleep(2)


class Handler(SimpleHTTPRequestHandler):
    def __init__(self, *a, **kw):
        super().__init__(*a, directory=str(PITCH), **kw)

    def log_message(self, fmt, *args):
        pass  # do konzole jen události dema

    def _json(self, code: int, obj):
        body = json.dumps(obj).encode()
        self.send_response(code)
        self.send_header("Content-Type", "application/json")
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)

    def do_GET(self):
        if self.path == "/demo/status":
            return self._json(200, HUB.status)
        if self.path == "/demo/events":
            return self._events()
        if self.path in ("/", ""):
            self.path = "/narsil-pitch.html"
        return super().do_GET()

    def do_POST(self):
        if self.path.startswith("/demo/cmd/"):
            cmd = self.path.rsplit("/", 1)[1]
            if cmd not in COMMANDS:
                return self._json(400, {"ok": False, "error": "neznámý příkaz"})
            ok = send_cmd(cmd)
            return self._json(200 if ok else 503, {"ok": ok})
        self.send_error(404)

    def _events(self):
        self.send_response(200)
        self.send_header("Content-Type", "text/event-stream")
        self.send_header("Cache-Control", "no-cache")
        self.end_headers()
        q = HUB.subscribe()
        try:
            while True:
                try:
                    line = q.get(timeout=15)
                    self.wfile.write(f"data: {line}\n\n".encode())
                except queue.Empty:
                    self.wfile.write(b": ping\n\n")
                self.wfile.flush()
        except (BrokenPipeError, ConnectionResetError):
            pass
        finally:
            HUB.unsubscribe(q)


def main():
    ap = argparse.ArgumentParser(description="Demo most Narsilu: telefon ↔ prezentace")
    ap.add_argument("--port", type=int, default=8000)
    ap.add_argument("--scrcpy", action="store_true", help="spustit zrcadlení telefonu ve smyčce")
    ap.add_argument("--scrcpy-bin", default=str(Path.home() / ".local/opt/scrcpy/scrcpy"))
    ap.add_argument("--scrcpy-arg", action="append", default=[], help="další argument pro scrcpy")
    a = ap.parse_args()

    threading.Thread(target=logcat_loop, daemon=True).start()
    if a.scrcpy:
        binary = a.scrcpy_bin if Path(a.scrcpy_bin).exists() else (shutil.which("scrcpy") or a.scrcpy_bin)
        threading.Thread(target=scrcpy_loop, args=(binary, a.scrcpy_arg), daemon=True).start()

    srv = ThreadingHTTPServer(("127.0.0.1", a.port), Handler)
    srv.daemon_threads = True
    log(f"prezentace: http://localhost:{a.port}/narsil-pitch.html  (adb: {ADB})")
    try:
        srv.serve_forever()
    except KeyboardInterrupt:
        log("konec")


if __name__ == "__main__":
    main()
