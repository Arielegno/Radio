#!/usr/bin/env python3
"""Prueba en el teléfono virtual: portada propia de una playlist (poner, conservar al reiniciar y al renombrar,
cambiar, quitar y borrar la playlist). Toca la pantalla como una persona y guarda capturas de cada paso.
Uso: portada_playlist.py APK SEMILLA SALIDA"""
import os, re, subprocess, sys, time, json, xml.etree.ElementTree as ET
PKG = "io.github.arielegno.radio"
APK, SEM, OUT = sys.argv[1:4]; os.makedirs(OUT, exist_ok=True)
informe = []
def log(s): print(s, flush=True); informe.append(s)
def sh(cmd, t=60):
    try: return subprocess.run(["adb", "shell", cmd], capture_output=True, text=True, timeout=t).stdout
    except Exception as e: return ""
def adb(*a, t=120): return subprocess.run(["adb", *a], capture_output=True, text=True, timeout=t)
def runas(cmd): return sh(f"run-as {PKG} sh -c '{cmd}'")
paso = [0]
def foto(nombre):
    paso[0] += 1
    with open(os.path.join(OUT, f"{paso[0]:02d}-{nombre}.png"), "wb") as f:
        f.write(subprocess.run(["adb", "exec-out", "screencap", "-p"], capture_output=True, timeout=60).stdout)
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
def buscar(patron, espera=12, pkg=None):
    fin = time.time() + espera
    while time.time() < fin:
        for t, d, c, p, b in nodos():
            if pkg and p != pkg: continue
            if re.search(patron, t) or re.search(patron, d): return b
        time.sleep(1)
    return None
def tocar(patron, espera=12):
    b = buscar(patron, espera)
    if not b: return False
    sh(f"input tap {(b[0]+b[2])//2} {(b[1]+b[3])//2}"); time.sleep(2); return True
def hay(patron, espera=6): return buscar(patron, espera) is not None
resultados = []
def comprobar(nombre, ok, detalle=""):
    resultados.append((nombre, ok)); log(("✅ " if ok else "❌ ") + nombre + (f" — {detalle}" if detalle else ""))
def portadas_en_disco(): return [l for l in runas("ls files/covers").split() if l.startswith("lista_")]
def prefs_playlists():
    x = runas("cat shared_prefs/radio.xml")
    m = re.search(r'<string name="playlists">(.*?)</string>', x, re.S)
    if not m: return {}
    import html
    try: return json.loads(html.unescape(m.group(1)))
    except Exception: return {}
def abrir_app():
    sh(f"monkey -p {PKG} -c android.intent.category.LAUNCHER 1 >/dev/null 2>&1"); time.sleep(6)
    if hay(r"^Saltar$", 4): tocar(r"^Saltar$")   # la bienvenida de la primera vez
def elegir_foto(cual):
    """Del selector de fotos de Android, toca la foto más reciente."""
    time.sleep(4); foto(f"selector-{cual}")
    for t, d, c, p, b in nodos():
        if re.search(r"^(Photo|Foto|Image|Imagen)", d) and b[3] - b[1] > 80:
            sh(f"input tap {(b[0]+b[2])//2} {(b[1]+b[3])//2}"); time.sleep(4); return True
    log("   (no encontré la foto por su descripción; toco la primera casilla debajo de «Recientes»)")
    todos = nodos()
    open(os.path.join(OUT, f"selector-{cual}.txt"), "w").write("\n".join(map(str, todos)))
    for t, d, c, p, b in todos:
        if re.match(r"^(Recent|Recientes)$", t):
            ancho = int(re.search(r"(\d+)x", sh("wm size")).group(1)) if re.search(r"(\d+)x", sh("wm size")) else 1080
            sh(f"input tap {ancho // 8} {b[3] + ancho // 8}"); time.sleep(4); return True
    return False
def poner_imagen(nombre, color):
    local = os.path.join(OUT, nombre)
    subprocess.run(["ffmpeg", "-v", "error", "-y", "-f", "lavfi", "-i", f"color=c={color}:s=800x800", "-frames:v", "1", local], check=True)
    adb("push", local, f"/sdcard/Pictures/{nombre}")
    sh(f"am broadcast -a android.intent.action.MEDIA_SCANNER_SCAN_FILE -d file:///sdcard/Pictures/{nombre} >/dev/null 2>&1")
    sh("content call --uri content://media --method scan_volume --arg external_primary >/dev/null 2>&1", 90)
    time.sleep(3)

# ===== preparar =====
adb("wait-for-device", t=300)
for k in ["window_animation_scale", "transition_animation_scale", "animator_duration_scale"]: sh(f"settings put global {k} 0")
r = adb("install", "-r", "-g", APK, t=300); log("Instalar: " + (r.stdout + r.stderr).strip().splitlines()[-1])
adb("logcat", "-c")
abrir_app(); sh(f"am force-stop {PKG}")
adb("push", SEM, "/data/local/tmp/semilla")
runas("mkdir -p files/music files/covers && cp /data/local/tmp/semilla/music/* files/music/ && cp /data/local/tmp/semilla/covers/* files/covers/ && cp /data/local/tmp/semilla/biblioteca.json files/biblioteca.json")
# una playlist de prueba y la Biblioteca abierta en «Playlists»
x = runas("cat shared_prefs/radio.xml")
if "<map" not in x: x = "<?xml version='1.0' encoding='utf-8' standalone='yes' ?>\n<map>\n</map>\n"
x = re.sub(r'\s*<string name="(playlists|libVista)">.*?</string>', "", x, flags=re.S)
listas = '{&quot;Mi playlist de prueba&quot;:{&quot;canciones&quot;:[1,2,3,4,5]},&quot;Otra playlist&quot;:{&quot;canciones&quot;:[6,7]}}'
x = x.replace("</map>", f'    <string name="playlists">{listas}</string>\n    <string name="libVista">playlists</string>\n</map>')
open(os.path.join(OUT, "radio.xml"), "w").write(x)
adb("push", os.path.join(OUT, "radio.xml"), "/data/local/tmp/radio.xml")
runas("mkdir -p shared_prefs && cp /data/local/tmp/radio.xml shared_prefs/radio.xml")
poner_imagen("rojo.jpg", "red")

# ===== 1. poner la portada =====
abrir_app(); foto("inicio")
tocar(r"^Bibliot"); foto("biblioteca-playlists")
comprobar("La playlist aparece en la Biblioteca", hay(r"Mi playlist de prueba"))
tocar(r"^Mi playlist de prueba$"); foto("playlist-abierta")
comprobar("Dentro de la playlist sale «Poner portada»", hay(r"^Poner portada$"))
tocar(r"^Poner portada$")
comprobar("Se abre el selector de fotos y se elige una", elegir_foto("rojo"))
foto("con-portada-roja")
comprobar("Después de elegir, el botón dice «Cambiar portada»", hay(r"^Cambiar portada$", 10))
comprobar("Aparece el aviso «Portada cambiada.»", hay(r"Portada cambiada"))
f1 = portadas_en_disco(); p = prefs_playlists()
comprobar("La foto quedó guardada en la app (1 archivo)", len(f1) == 1, str(f1))
comprobar("La playlist recuerda su portada", p.get("Mi playlist de prueba", {}).get("portada") in f1, str(p))
comprobar("La otra playlist no se tocó", "portada" not in p.get("Otra playlist", {}))
sh("input keyevent 4"); time.sleep(2); foto("lista-con-portada")

# ===== 2. se conserva al cerrar y abrir la app =====
sh(f"am force-stop {PKG}"); abrir_app(); tocar(r"^Bibliot"); tocar(r"^Mi playlist de prueba$"); foto("tras-reiniciar")
comprobar("Al cerrar y abrir la app, la portada sigue", hay(r"^Cambiar portada$"))

# ===== 3. se conserva al cambiarle el nombre =====
tocar(r"^Cambiar nombre$"); foto("dialogo-nombre")
b = buscar(r"Mi playlist de prueba", 6)
campo = [n for n in nodos() if "EditText" in n[2]]
if campo:
    bb = campo[0][4]; sh(f"input tap {(bb[0]+bb[2])//2} {(bb[1]+bb[3])//2}"); time.sleep(1)
    sh("input keyevent KEYCODE_MOVE_END"); sh(" ; ".join(["input keyevent 67"] * 30)); sh("input text 'Viaje%sal%smar'"); time.sleep(1)
foto("nombre-nuevo"); tocar(r"^Guardar$"); foto("renombrada")
p = prefs_playlists()
comprobar("La playlist ahora se llama «Viaje al mar»", "Viaje al mar" in p and "Mi playlist de prueba" not in p, str(list(p)))
comprobar("Con el nombre nuevo conserva la portada", p.get("Viaje al mar", {}).get("portada") in f1 and hay(r"^Cambiar portada$"))

# ===== 4. cambiar por otra foto: la vieja se borra =====
poner_imagen("azul.jpg", "blue")
tocar(r"^Cambiar portada$")
comprobar("Se elige una segunda foto", elegir_foto("azul")); foto("con-portada-azul")
f2 = portadas_en_disco()
comprobar("Queda una sola foto (la vieja se borró)", len(f2) == 1 and f2 != f1, f"{f1} → {f2}")

# ===== 5. quitar la portada =====
tocar(r"^Quitar portada$"); foto("sin-portada")
comprobar("«Quitar portada» vuelve a «Poner portada»", hay(r"^Poner portada$"))
comprobar("Aparece el aviso de la carátula de la primera canción", hay(r"primera canción"))
comprobar("La foto se borró de la app", portadas_en_disco() == [], str(portadas_en_disco()))
comprobar("La playlist ya no tiene portada guardada", "portada" not in prefs_playlists().get("Viaje al mar", {}))

# ===== 6. borrar la playlist con portada: se borra la foto también =====
tocar(r"^Poner portada$"); elegir_foto("rojo-otra-vez")
comprobar("Se pone portada otra vez", len(portadas_en_disco()) == 1)
tocar(r"^Borrar playlist$"); foto("dialogo-borrar"); tocar(r"^Borrar$"); foto("borrada")
p = prefs_playlists()
comprobar("La playlist se borró y la otra sigue", "Viaje al mar" not in p and "Otra playlist" in p, str(list(p)))
comprobar("Su foto también se borró", portadas_en_disco() == [], str(portadas_en_disco()))
comprobar("Las carátulas de las canciones siguen ahí", len(runas("ls files/covers").split()) >= 30)

# ===== cierres =====
crash = adb("logcat", "-d", "-b", "crash").stdout
todo = adb("logcat", "-d").stdout
open(os.path.join(OUT, "logcat.txt"), "w").write(todo)
cierres = [l for l in todo.splitlines() if re.search(rf"FATAL EXCEPTION|ANR in {PKG}|Fatal signal", l)]
comprobar("La app no se cerró ni se trabó", not cierres and PKG not in crash, "; ".join(cierres[:3]))
bien = sum(ok for _, ok in resultados)
log(f"\nRESULTADO: {bien} de {len(resultados)} comprobaciones bien")
open(os.path.join(OUT, "leeme.txt"), "w").write("PRUEBA · PORTADA PROPIA DE PLAYLISTS\n\n" + "\n".join(informe) + "\n")
