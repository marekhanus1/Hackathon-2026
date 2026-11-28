#!/usr/bin/env python3
"""Demo most (161): telefon ↔ prezentace. Jen standardní knihovna Pythonu.

  python3 scripts/demo-most.py --scrcpy             # prezentace + události + okno Narsil-live ve smyčce
  python3 scripts/demo-most.py --scrcpy --adb-video # záloha: obraz telefonu i přes adb screencap (~5 snímků/s)

Obraz pro prezentace v síti posílá záložka na localhostu, která sdílí okno Narsil-live. Záloha --adb-video
bere obraz přes adb bez sdílení (trhanější, zatěžuje telefon – při dřepech může zpomalit kameru).

Pak ve Firefoxu http://localhost:8000/narsil-pitch.html → S → „Sdílet okno telefonu“ (Narsil-live).
Prezentace jde otevřít i z jiného počítače v síti (http://<IP notebooku>:8000/): obraz telefonu sem přeposílá
záložka na localhostu, která okno sdílí (MJPEG /demo/live.mjpg). Sdílet okno jde jen na localhostu (Firefox
getDisplayMedia na http://IP nepovolí). Příkazy do telefonu a snímky jen z localhostu (--allow-remote-cmd povolí příkazy z LAN).

- servíruje složku pitch/ (místo python3 -m http.server),
- GET  /demo/events  – Server-Sent Events: řádky z `adb logcat -v raw -s NarsilDemo:I` (JSON z aplikace)
                       a stav mostu {"e":"bridge","adb":true|false,"device":"…"},
- POST /demo/cmd/<c> – `adb shell am broadcast -a cz.narsil.DEMO -p cz.narsil --es cmd <c>`
                       (prepare | home | away | auto | echo | xp | steps | reset | state),
- GET  /demo/status  – stav mostu jako JSON,
- POST /demo/frame   – JPEG snímek telefonu ze záložky, která sdílí okno (jen localhost),
- GET  /demo/live.mjpg – živý obraz telefonu pro prezentace bez vlastního sdílení.

Pravidla pro demo telefon (viz paměť týmu): logcat se jen čte živě (žádné -c), žádný force-stop ani reconnect.
"""
import argparse
import atexit
import signal
import json
import os
import queue
import shutil
import sys
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
        self.live = False
        self.last_state = None

    def subscribe(self) -> queue.Queue:
        q: queue.Queue = queue.Queue(maxsize=200)
        with self.lock:
            self.clients.append(q)
        q.put(json.dumps(self.status))
        if self.last_state:
            q.put(self.last_state)
        if self.live:
            q.put(json.dumps({"e": "live", "on": True}))
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

    def set_live(self, on: bool):
        if on != self.live:
            self.live = on
            self.publish(json.dumps({"e": "live", "on": on}))
            log(f"obraz telefonu pro ostatní prezentace: {'běží' if on else 'neběží'}")

    def set_status(self, adb: bool, device):
        new = {"e": "bridge", "adb": adb, "device": device}
        if new != self.status:
            self.status = new
            self.publish(json.dumps(new))
            log(f"telefon: {'připojený ' + str(device) if adb else 'nepřipojený'}")


HUB = Hub()


class Frames:
    """Poslední snímek telefonu (JPEG) – z adb nebo ze záložky, která sdílí okno; MJPEG klienti se probudí."""

    TIMEOUT = 3.0

    def __init__(self):
        self.cond = threading.Condition()
        self.jpeg = None
        self.seq = 0
        self.at = 0.0
        self.browser_at = 0.0

    def put(self, data: bytes, browser: bool = False):
        if browser:
            self.browser_at = time.time()
        elif time.time() - self.browser_at < 2:
            return  # sdílené okno z prohlížeče je plynulejší, má přednost
        with self.cond:
            self.jpeg, self.seq, self.at = data, self.seq + 1, time.time()
            self.cond.notify_all()
        HUB.set_live(True)

    def wait(self, seq: int, timeout: float = 5.0):
        with self.cond:
            self.cond.wait_for(lambda: self.seq != seq, timeout)
            return self.seq, self.jpeg

    def watchdog(self):
        while True:
            time.sleep(1)
            if HUB.live and time.time() - self.at > self.TIMEOUT:
                HUB.set_live(False)


FRAMES = Frames()
CHILDREN: list = []


def kill_children(*_):
    for c in list(CHILDREN):
        try:
            c.terminate()
        except OSError:
            pass
ALLOW_REMOTE_CMD = False
MAX_FRAME = 4 * 1024 * 1024


def is_local(addr: str) -> bool:
    return addr in ("127.0.0.1", "::1", "::ffff:127.0.0.1")


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
                if '"e":"state"' in line:
                    HUB.last_state = line
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


def read_png(stream):
    """Jeden PNG z proudu zřetězených PNG (screencap -p ve smyčce). None = konec proudu."""
    sig = stream.read(8)
    if len(sig) < 8:
        return None
    if sig != b"\x89PNG\r\n\x1a\n":
        raise ValueError("není PNG")
    parts = [sig]
    while True:
        head = stream.read(8)
        if len(head) < 8:
            return None
        n = int.from_bytes(head[:4], "big")
        body = stream.read(n + 4)
        if len(body) < n + 4:
            return None
        parts += [head, body]
        if head[4:8] == b"IEND":
            return b"".join(parts)


def capture_loop(fps: float, height: int):
    """Obraz telefonu pro prezentaci: screencap v jedné stálé adb relaci, PNG → JPEG (PIL) → MJPEG."""
    try:
        from io import BytesIO
        from PIL import Image
    except ImportError:
        log("obraz telefonu: chybí PIL (python3-pil) – zbývá sdílení okna ve Firefoxu")
        return
    pause = max(0.0, 1.0 / fps - 0.15)
    while True:
        if device_name() is None:
            time.sleep(2)
            continue
        log(f"obraz telefonu: snímám přes adb (~{fps:g} snímků/s)")
        proc = subprocess.Popen(
            [ADB, "exec-out", "sh", "-c", f"while true; do screencap -p; sleep {pause:.2f}; done"],
            stdout=subprocess.PIPE, stderr=subprocess.DEVNULL, bufsize=0,
        )
        CHILDREN.append(proc)
        try:
            while True:
                png = read_png(proc.stdout)
                if png is None:
                    break
                im = Image.open(BytesIO(png)).convert("RGB")
                if im.height > height:
                    im = im.resize((round(im.width * height / im.height), height), Image.BILINEAR)
                out = BytesIO()
                im.save(out, "JPEG", quality=72)
                FRAMES.put(out.getvalue())
        except (ValueError, OSError) as e:
            log(f"obraz telefonu: {e}")
        finally:
            proc.kill()
            CHILDREN.remove(proc)
        log("obraz telefonu: přerušeno (USB?), za 2 s znovu")
        time.sleep(2)


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
            proc = subprocess.Popen(args, env=env, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
            CHILDREN.append(proc)
            proc.wait()
            CHILDREN.remove(proc)
        except OSError as e:
            log(f"scrcpy nejde spustit ({e})")
            return
        log("scrcpy: skončilo, za 2 s znovu")
        time.sleep(2)


class Handler(SimpleHTTPRequestHandler):
    def __init__(self, *a, **kw):
        super().__init__(*a, directory=str(PITCH), **kw)

    def end_headers(self):
        if not self.path.startswith("/demo/"):
            self.send_header("Cache-Control", "no-store")
        super().end_headers()

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
            return self._json(200, {**HUB.status, "live": HUB.live, "local": is_local(self.client_address[0])})
        if self.path.split("?")[0] == "/demo/live.mjpg":
            return self._mjpeg()
        if self.path == "/demo/events":
            return self._events()
        if self.path in ("/", ""):
            self.path = "/narsil-pitch.html"
        return super().do_GET()

    def do_POST(self):
        local = is_local(self.client_address[0])
        if self.path == "/demo/frame":
            n = int(self.headers.get("Content-Length") or 0)
            if not local or not 0 < n <= MAX_FRAME:
                return self._json(403 if not local else 400, {"ok": False})
            FRAMES.put(self.rfile.read(n), browser=True)
            return self._json(200, {"ok": True})
        if self.path.startswith("/demo/cmd/") and not (local or ALLOW_REMOTE_CMD):
            return self._json(403, {"ok": False, "error": "příkazy jen z localhostu (--allow-remote-cmd)"})
        if self.path.startswith("/demo/cmd/"):
            cmd = self.path.rsplit("/", 1)[1]
            if cmd not in COMMANDS:
                return self._json(400, {"ok": False, "error": "neznámý příkaz"})
            ok = send_cmd(cmd)
            return self._json(200 if ok else 503, {"ok": ok})
        self.send_error(404)

    def _mjpeg(self):
        self.send_response(200)
        self.send_header("Content-Type", "multipart/x-mixed-replace; boundary=frame")
        self.send_header("Cache-Control", "no-cache")
        self.end_headers()
        seq = -1
        try:
            while True:
                seq, jpeg = FRAMES.wait(seq)
                if jpeg is None:
                    continue
                self.wfile.write(b"--frame\r\nContent-Type: image/jpeg\r\nContent-Length: %d\r\n\r\n" % len(jpeg))
                self.wfile.write(jpeg)
                self.wfile.write(b"\r\n")
                self.wfile.flush()
        except (BrokenPipeError, ConnectionResetError):
            pass

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


def lan_ips():
    try:
        out = subprocess.run(["hostname", "-I"], capture_output=True, text=True, timeout=3).stdout.split()
    except (OSError, subprocess.TimeoutExpired):
        return []
    return [ip for ip in out if ":" not in ip and not ip.startswith(("172.17.", "127."))]


def main():
    ap = argparse.ArgumentParser(description="Demo most Narsilu: telefon ↔ prezentace")
    ap.add_argument("--port", type=int, default=8000)
    ap.add_argument("--host", default="0.0.0.0", help="127.0.0.1 = jen tento počítač")
    ap.add_argument("--allow-remote-cmd", action="store_true", help="příkazy do telefonu i z jiných počítačů v síti")
    ap.add_argument("--scrcpy", action="store_true", help="spustit i okno scrcpy (pro sdílení okna ve Firefoxu)")
    ap.add_argument("--fps", type=float, default=5, help="obraz telefonu přes adb: snímků za sekundu")
    ap.add_argument("--height", type=int, default=1280, help="obraz telefonu přes adb: výška v px")
    ap.add_argument("--adb-video", action="store_true", help="záloha: obraz telefonu přes adb screencap")
    ap.add_argument("--scrcpy-bin", default=str(Path.home() / ".local/opt/scrcpy/scrcpy"))
    ap.add_argument("--scrcpy-arg", action="append", default=[], help="další argument pro scrcpy")
    a = ap.parse_args()

    global ALLOW_REMOTE_CMD
    atexit.register(kill_children)
    signal.signal(signal.SIGTERM, lambda *_: sys.exit(0))
    ALLOW_REMOTE_CMD = a.allow_remote_cmd
    threading.Thread(target=logcat_loop, daemon=True).start()
    threading.Thread(target=FRAMES.watchdog, daemon=True).start()
    if a.adb_video:
        threading.Thread(target=capture_loop, args=(a.fps, a.height), daemon=True).start()
    if a.scrcpy:
        binary = a.scrcpy_bin if Path(a.scrcpy_bin).exists() else (shutil.which("scrcpy") or a.scrcpy_bin)
        threading.Thread(target=scrcpy_loop, args=(binary, a.scrcpy_arg), daemon=True).start()

    srv = ThreadingHTTPServer((a.host, a.port), Handler)
    srv.daemon_threads = True
    log(f"prezentace: http://localhost:{a.port}/narsil-pitch.html  (adb: {ADB})")
    if a.host != "127.0.0.1":
        for ip in lan_ips():
            log(f"v síti:     http://{ip}:{a.port}/narsil-pitch.html")
    try:
        srv.serve_forever()
    except KeyboardInterrupt:
        log("konec")


if __name__ == "__main__":
    main()
