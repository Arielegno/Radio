"""Ayudantes para manejar el teléfono virtual desde las pruebas (tocar, buscar textos, capturas, ajustes)."""
import base64, os, re, subprocess, time, json, html, xml.etree.ElementTree as ET
PKG = "io.github.arielegno.radio"
OUT = "."
informe, resultados = [], []
paso = [0]

def log(s): print(s, flush=True); informe.append(s)
def sh(cmd, t=60):
    try: return subprocess.run(["adb", "shell", cmd], capture_output=True, text=True, timeout=t).stdout
    except Exception: return ""
def adb(*a, t=120):
    try: return subprocess.run(["adb", *a], capture_output=True, text=True, timeout=t)
    except Exception as e: return subprocess.CompletedProcess(a, 1, "", str(e))
def runas(cmd): return sh(f"run-as {PKG} sh -c '{cmd}'")
def foto(nombre):
    paso[0] += 1
    try:
        with open(os.path.join(OUT, f"{paso[0]:03d}-{nombre}.png"), "wb") as f:
            f.write(subprocess.run(["adb", "exec-out", "screencap", "-p"], capture_output=True, timeout=60).stdout)
    except Exception: pass
def nodos():
    sh("uiautomator dump /sdcard/ui.xml >/dev/null 2>&1", 40)
    x = sh("cat /sdcard/ui.xml")
    try: raiz = ET.fromstring(x[x.find("<?xml"):] if "<?xml" in x else x)
    except Exception: return []
    out = []
    for n in raiz.iter("node"):
        m = re.match(r"\[(\d+),(\d+)\]\[(\d+),(\d+)\]", n.get("bounds", ""))
        if m: out.append((n.get("text", ""), n.get("content-desc", ""), n.get("class", ""), n.get("package", ""), tuple(map(int, m.groups()))))
    return out
def todos(patron):
    return [b for t, d, c, p, b in nodos() if re.search(patron, t) or re.search(patron, d)]
def buscar(patron, espera=10, indice=0):
    fin = time.time() + espera
    while True:
        l = todos(patron)
        if len(l) > indice: return l[indice]
        if time.time() > fin: return None
        time.sleep(1)
def pulsar(b): sh(f"input tap {(b[0]+b[2])//2} {(b[1]+b[3])//2}"); time.sleep(1.5)
def tocar(patron, espera=10, indice=0):
    b = buscar(patron, espera, indice)
    if not b: return False
    pulsar(b); return True
def hay(patron, espera=5): return buscar(patron, espera) is not None
def tamano():
    m = re.search(r"(\d+)x(\d+)", sh("wm size")); return (int(m.group(1)), int(m.group(2))) if m else (1080, 2400)
def deslizar(veces=1, arriba=True):
    w, h = tamano()
    for _ in range(veces):
        a, b = (int(h * .72), int(h * .30)) if arriba else (int(h * .30), int(h * .72))
        sh(f"input swipe {w//2} {a} {w//2} {b} 350"); time.sleep(1)
def buscar_bajando(patron, intentos=8, indice=0):
    for _ in range(intentos):
        b = buscar(patron, 2, indice)
        if b: return b
        deslizar()
    return None
def atras(): sh("input keyevent 4"); time.sleep(1.5)
def comprobar(nombre, ok, detalle=""):
    resultados.append((nombre, bool(ok))); log(("✅ " if ok else "❌ ") + nombre + (f" — {detalle}" if detalle else ""))
def abrir_app():
    sh(f"monkey -p {PKG} -c android.intent.category.LAUNCHER 1 >/dev/null 2>&1"); time.sleep(6)
    if hay(r"^Saltar$", 4): tocar(r"^Saltar$")
def pestana(n):
    """Toca una pestaña de abajo (la coincidencia más baja de la pantalla)."""
    l = todos("^" + n[:6])
    if not l: return False
    pulsar(max(l, key=lambda b: b[1])); return True
def iniciar_radio():
    """Toca «Iniciar radio» en la portada (el botón de play del teléfono solo sirve cuando ya sonó algo)."""
    pestana("Radio"); time.sleep(1); deslizar(3, arriba=False)
    b = buscar_bajando(r"^Iniciar radio$", 4)
    if b: pulsar(b); time.sleep(3); return True
    return False
def orden(o, k="", v=""):
    """Le manda una orden al control remoto de la versión de prueba (cambiar un ajuste, que Luna diga algo…)."""
    b = base64.b64encode(str(v).encode()).decode()
    valor = f"--es b64 {b}" if b else "--ez vacio true"
    sh(f"am broadcast -a {PKG}.PRUEBA -p {PKG} --es orden {o} --es k '{k or '-'}' {valor} >/dev/null"); time.sleep(.6)
def registro_limpiar(): adb("logcat", "-c")
def registro(): return [l.split("RadioPrueba: ", 1)[1] for l in adb("logcat", "-d", "-s", "RadioPrueba:I").stdout.splitlines() if "RadioPrueba: " in l]
def esperar_registro(patron, espera=60):
    fin = time.time() + espera
    while time.time() < fin:
        for l in registro():
            if re.search(patron, l): return l
        time.sleep(1)
    return None
def prefs():
    x = runas("cat shared_prefs/radio.xml"); d = {}
    for m in re.finditer(r'<(string|boolean|int) name="([^"]+)"(?: value="([^"]*)")?\s*/?>(?:(.*?)</string>)?', x, re.S):
        d[m.group(2)] = html.unescape(m.group(4)) if m.group(1) == "string" else m.group(3)
    return d
def preparar_app(apk, semilla, extra_prefs):
    adb("wait-for-device", t=300)
    for k in ["window_animation_scale", "transition_animation_scale", "animator_duration_scale"]: sh(f"settings put global {k} 0")
    r = adb("install", "-r", "-g", apk, t=300); log("Instalar: " + ((r.stdout + r.stderr).strip().splitlines() or ["?"])[-1])
    abrir_app(); sh(f"am force-stop {PKG}")
    adb("push", semilla, "/data/local/tmp/semilla")
    runas("mkdir -p files/music files/covers && cp /data/local/tmp/semilla/music/* files/music/ && cp /data/local/tmp/semilla/covers/* files/covers/ && cp /data/local/tmp/semilla/biblioteca.json files/biblioteca.json")
    x = runas("cat shared_prefs/radio.xml")
    if "<map" not in x: x = "<?xml version='1.0' encoding='utf-8' standalone='yes' ?>\n<map>\n</map>\n"
    for k, v in extra_prefs.items():
        x = re.sub(r'\s*<[a-z]+ name="%s"[^>]*?(/>|>[^<]*</string>)' % re.escape(k), "", x)
        tipo = "boolean" if isinstance(v, bool) else "int" if isinstance(v, int) else "string"
        linea = f'    <string name="{k}">{html.escape(v)}</string>' if tipo == "string" else f'    <{tipo} name="{k}" value="{str(v).lower()}" />'
        x = x.replace("</map>", linea + "\n</map>")
    open(os.path.join(OUT, "radio.xml"), "w").write(x)
    adb("push", os.path.join(OUT, "radio.xml"), "/data/local/tmp/radio.xml")
    runas("mkdir -p shared_prefs && cp /data/local/tmp/radio.xml shared_prefs/radio.xml")
_vivo = []
def registro_vivo():
    """Copia todo el registro de Android a un archivo mientras dura la prueba (para encontrar cierres)."""
    _vivo.append(subprocess.Popen(["adb", "logcat", "-v", "threadtime"], stdout=open(os.path.join(OUT, "logcat.txt"), "w"), stderr=subprocess.STDOUT))
def cierres():
    try: todo = open(os.path.join(OUT, "logcat.txt"), errors="replace").read()
    except Exception: todo = adb("logcat", "-d", "-b", "all").stdout
    return [l for l in todo.splitlines() if re.search(rf"FATAL EXCEPTION|ANR in {PKG}|Fatal signal|Process: {PKG}", l)]
def terminar(titulo):
    bien = sum(ok for _, ok in resultados)
    log(f"\nRESULTADO: {bien} de {len(resultados)} comprobaciones bien")
    open(os.path.join(OUT, "leeme.txt"), "w").write(titulo + "\n\n" + "\n".join(informe) + "\n")
