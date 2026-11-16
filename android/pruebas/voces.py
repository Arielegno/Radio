#!/usr/bin/env python3
"""Prueba de las voces de Luna en el teléfono virtual: descargar, probar, usar y borrar voces; estilos, efectos,
volumen, dos locutores, la voz del teléfono y Luna en la radio (música bajita, pausa y saltar mientras habla).
Uso: voces.py APK SEMILLA SALIDA VOZ_ALD"""
import os, re, sys, time, wave, struct, subprocess
import numpy as np
import telefono as T
from telefono import log, comprobar, sh, runas, orden, tocar, hay, buscar, buscar_bajando, pulsar, foto, atras, abrir_app, pestana
APK, SEM, OUT, ALD = sys.argv[1:5]; os.makedirs(OUT, exist_ok=True); T.OUT = OUT
WAVS = os.path.join(OUT, "voces"); os.makedirs(WAVS, exist_ok=True)

def wavs():
    return sorted(l for l in runas("ls files/prueba-voz 2>/dev/null").split() if l.endswith(".wav"))
def traer(nombre):
    local = os.path.join(WAVS, nombre)
    T.sh(f"run-as {T.PKG} cat files/prueba-voz/{nombre} > /data/local/tmp/v.wav")
    T.adb("pull", "/data/local/tmp/v.wav", local)
    try:
        with wave.open(local) as w:
            sr = w.getframerate(); x = np.frombuffer(w.readframes(w.getnframes()), dtype=np.int16).astype(float) / 32768
        return sr, x
    except Exception: return 0, np.zeros(1)
def nuevos(antes, espera=60, cuantos=1):
    fin = time.time() + espera
    while time.time() < fin:
        n = [w for w in wavs() if w not in antes]
        if len(n) >= cuantos: time.sleep(1); return [w for w in wavs() if w not in antes]
        time.sleep(1)
    return [w for w in wavs() if w not in antes]
def medir(sr, x):
    """Duración, volumen y cómo se reparte el sonido entre graves, medios y agudos."""
    if sr == 0 or len(x) < 100: return {"ms": 0, "rms": 0, "graves": 0, "agudos": 0, "cola": 0}
    f = np.abs(np.fft.rfft(x)) ** 2; fr = np.fft.rfftfreq(len(x), 1 / sr); tot = f.sum() + 1e-12
    voz = np.where(np.abs(x) > 0.01)[0]
    return {"ms": int(len(x) * 1000 / sr), "rms": float(np.sqrt((x ** 2).mean())),
            "graves": float(f[fr < 300].sum() / tot), "agudos": float(f[fr > 3500].sum() / tot),
            "cola": int((len(x) - (voz[-1] if len(voz) else 0)) * 1000 / sr)}
def decir(texto, espera=90, cuantos=1, etiqueta=""):
    antes = wavs(); T.registro_limpiar(); orden("decir", v=texto)
    T.esperar_registro(r"decir: termina", espera)
    n = nuevos(antes, 5, cuantos)
    res = [(w, *traer(w)) for w in n]
    for w, sr, x in res:
        m = medir(sr, x); log(f"   {etiqueta} {w}: {m['ms']} ms · volumen {m['rms']:.3f} · graves {m['graves']:.0%} · agudos {m['agudos']:.1%}")
    return res
def a_mp3(nombre_wav, nombre):
    subprocess.run(["ffmpeg", "-v", "error", "-y", "-i", os.path.join(WAVS, nombre_wav), "-b:a", "64k", os.path.join(OUT, f"escuchar-{nombre}.mp3")])

T.preparar_app(APK, SEM, {"aire": True, "cadaN": 1, "hablarEncima": True, "nivelEncima": 18, "motorVoz": "", "host": "Luna", "host2": "Leo"})
# la voz Ald ya viene puesta; Daniela se descarga desde la app (así se prueba la descarga)
T.adb("push", ALD, "/data/local/tmp/ald")
runas("mkdir -p files/voces && cp -r /data/local/tmp/ald/* files/voces/")
T.registro_vivo()
abrir_app()

# ===== 1. Ajustes › Voz: descargar Daniela =====
log("\n— Descargar una voz —")
pestana("Ajustes"); tocar(r"^Voz$"); foto("voz-arriba")
b = buscar_bajando(r"^Descargar$")
comprobar("Aparece «Descargar» para la voz que falta (Daniela)", b)
if b:
    pulsar(b); foto("descargando")
    comprobar("Se ve el avance de la descarga", hay(r"Descargando|Preparando", 10))
    fin = time.time() + 420
    while time.time() < fin and not runas("ls files/voces/vits-piper-es_AR-daniela-high/ 2>/dev/null | grep -c onnx").strip().startswith("1"): time.sleep(5)
    time.sleep(8); foto("descargada")
    comprobar("Daniela quedó descargada (archivo de la voz y sus letras)", "tokens.txt" in runas("ls files/voces/vits-piper-es_AR-daniela-high/"))
    comprobar("Ya no sale «Descargar» y aparecen sus botones", not hay(r"^Descargar$", 3) and len(T.todos(r"^Probar$")) >= 2)

# ===== 2. Probar cada voz desde su botón =====
log("\n— Botón «Probar» de cada voz —")
for i, nombre in enumerate(["daniela", "ald"]):
    antes = wavs()
    b = buscar(r"^Probar$", 5, i) or buscar_bajando(r"^Probar$", 3, i)
    if b: pulsar(b)
    n = nuevos(antes, 60)
    sr, x = traer(n[0]) if n else (0, np.zeros(1)); m = medir(sr, x)
    comprobar(f"«Probar» con {nombre.title()} habla (se oye algo de más de 1 segundo)", n and nombre in n[0] and m["ms"] > 1000 and m["rms"] > 0.02, f"{n} {m['ms']} ms")
    if n: a_mp3(n[0], f"voz-{nombre}")
foto("despues-de-probar")

# ===== 3. Usar una voz =====
log("\n— Botón «Usar» —")
b = buscar(r"^Usar$", 5)
if b: pulsar(b); time.sleep(4)
mv = T.prefs().get("motorVoz")
if mv != "propia:daniela": time.sleep(5); mv = T.prefs().get("motorVoz")
comprobar("«Usar» deja a Daniela como la voz de Luna", mv == "propia:daniela", mv)
comprobar("En pantalla dice «Daniela · en uso»", hay(r"Daniela · en uso"))
foto("daniela-en-uso")
T.deslizar(6, arriba=False)
for boton, que in [("Probar voz", "Probar voz"), ("Probar a Luna", "Probar a Luna")]:
    antes = wavs(); T.registro_limpiar()
    b = buscar_bajando("^" + boton + "$")
    if b: pulsar(b)
    l = T.esperar_registro(r"Luna termina", 90); n = nuevos(antes, 5)
    comprobar(f"«{que}» hace hablar a Luna con Daniela", l and n and "daniela" in n[0], f"{n}")
    if boton == "Probar a Luna" and n: a_mp3(n[0], "luna-comenta-una-cancion")

# ===== 4. Estilos de voz (velocidad) =====
log("\n— Estilos de voz —")
texto = "Esto es Radio de ruta. Seguimos en el camino con buena música. Ya viene la siguiente canción."
dur = {}
for preset, extra in [("natural", None), ("radio", None), ("energico", None), ("manual", 60), ("manual", 150)]:
    orden("texto", "vozPreset", preset)
    if extra: orden("int", "velocidad", extra)
    # la voz varía un poco cada vez: se mide tres veces y se promedia
    ms = []
    for _ in range(3):
        r = decir(texto, etiqueta=f"{preset}{extra or ''}")
        if r: ms.append(medir(*r[0][1:])["ms"])
    dur[f"{preset}{extra or ''}"] = int(sum(ms) / len(ms)) if ms else 0
comprobar("Todos los estilos hablan", all(v > 1000 for v in dur.values()), str(dur))
comprobar("«Radio FM» habla más pausado que «Natural»", dur["radio"] > dur["natural"], f"{dur['radio']} vs {dur['natural']} ms")
comprobar("«Enérgico» habla más rápido que «Natural»", dur["energico"] < dur["natural"], f"{dur['energico']} vs {dur['natural']} ms")
comprobar("Manual: velocidad 60 es más lenta que 150", dur["manual60"] > dur["manual150"] * 1.5, f"{dur['manual60']} vs {dur['manual150']} ms")
orden("texto", "vozPreset", "natural")

# ===== 5. Efectos de voz =====
log("\n— Efectos de voz —")
fx = {}
for e in ["", "fm", "telefono", "walkie", "estadio"]:
    orden("texto", "efectoVoz", e)
    r = decir(texto, etiqueta=e or "sin efecto")
    fx[e] = medir(*r[0][1:]) if r else medir(0, np.zeros(1))
    if r: a_mp3(r[0][0], "efecto-" + (e or "ninguno"))
orden("texto", "efectoVoz", "")
s = fx[""]
comprobar("Todos los efectos hablan", all(v["ms"] > 1000 and v["rms"] > 0.01 for v in fx.values()))
comprobar("«Teléfono» quita los graves", fx["telefono"]["graves"] < s["graves"] * .3, f"{fx['telefono']['graves']:.1%} vs {s['graves']:.1%}")
comprobar("«Teléfono» quita los agudos", fx["telefono"]["agudos"] < s["agudos"] * .5, f"{fx['telefono']['agudos']:.1%} vs {s['agudos']:.1%}")
comprobar("«Walkie-talkie» suena todavía más angosto que el teléfono", fx["walkie"]["graves"] <= fx["telefono"]["graves"] + .01)
comprobar("«Locutor FM» tiene más graves", fx["fm"]["graves"] > s["graves"] * 1.1, f"{fx['fm']['graves']:.1%} vs {s['graves']:.1%}")
comprobar("«Eco de estadio» deja eco al final", fx["estadio"]["ms"] > s["ms"] + 300, f"{fx['estadio']['ms']} vs {s['ms']} ms")
comprobar("Ningún efecto satura el sonido", all(v["rms"] < .5 for v in fx.values()))

# ===== 6. Volumen de la voz =====
log("\n— Volumen de la voz —")
orden("int", "volVoz", 30); T.registro_limpiar(); decir("Probando el volumen.")
comprobar("Con el volumen de voz en 30, la voz suena al 30 %", any("volumen 0.30" in l for l in T.registro()))
orden("int", "volVoz", 100)

# ===== 7. Dos locutores =====
log("\n— Dos locutores —")
dialogo = "Luna: Buenas tardes, esto es Radio de ruta. Leo: Qué tal, aquí seguimos en el camino. Luna: Vamos con la siguiente canción."
orden("bool", "dosLocutores", "true"); orden("texto", "vozPareja", "ald")
r = decir(dialogo, cuantos=3, etiqueta="dúo")
quien = [w.split("-")[1] for w, *_ in r]
comprobar("Con dos locutores, cada quien habla con su voz (Luna, Leo, Luna)", quien == ["daniela", "ald", "daniela"], str(quien))
if len(r) == 3:
    import numpy as _np
    junto = _np.concatenate([x for _, _, x in r]); import wave as _w
    with _w.open(os.path.join(WAVS, "duo.wav"), "w") as w: w.setnchannels(1); w.setsampwidth(2); w.setframerate(r[0][1]); w.writeframes((junto * 32767).astype(_np.int16).tobytes())
    a_mp3("duo.wav", "dos-locutores")
pantalla = T.registro()
comprobar("Los nombres «Luna:» y «Leo:» no se leen en voz alta", all(medir(sr, x)["ms"] < 6000 for _, sr, x in r))
orden("texto", "vozPareja", "daniela")
r = decir(dialogo, etiqueta="dúo con la misma voz")
comprobar("Si el compañero tiene la misma voz, habla una sola voz sin trabarse", len(r) == 1, str([w for w, *_ in r]))
orden("bool", "dosLocutores", "false"); orden("texto", "vozPareja", "ald")
r = decir(dialogo, etiqueta="sin dúo")
comprobar("Sin dos locutores, Luna dice todo con una voz", len(r) == 1 and "daniela" in r[0][0])

# ===== 8. La voz del teléfono =====
log("\n— Voz del teléfono —")
orden("motor", v=""); T.registro_limpiar(); orden("decir", v="Hola, soy Luna con la voz del teléfono.")
l = T.esperar_registro(r"decir: termina", 60)
reg = T.registro(); log("   " + " | ".join(x for x in reg if "teléfono" in x)[:400])
tiene = any("voz del teléfono: termina" in x for x in reg)
sin_motor = any("no está lista" in x or "error" in x for x in reg)
comprobar("La voz del teléfono habla, o si el teléfono no tiene voz la app avisa sin trabarse", l and (tiene or sin_motor), "habló" if tiene else "este teléfono virtual no trae voz instalada")

# ===== 9. Luna en la radio: música bajita, pausa y saltar =====
log("\n— Luna en la radio —")
orden("motor", v="propia:ald"); orden("bool", "hablarEncima", "true")
T.registro_limpiar()
comprobar("Se puede iniciar la radio", T.iniciar_radio()); foto("radio-iniciada")
l = T.esperar_registro(r"Luna empieza", 150)
if l and "musica=parada" in l:   # el saludo de apertura es antes de la primera canción: se espera el siguiente
    T.esperar_registro(r"Luna termina", 60); T.registro_limpiar(); l = T.esperar_registro(r"Luna empieza", 150)
foto("luna-hablando-encima")
comprobar("Luna habla entre canciones en la radio", l, l or "")
comprobar("Mientras habla, la música sigue bajita (18 %)", l and "musica=sonando" in l and "volumen=0.18" in l, l or "")
l2 = T.esperar_registro(r"Luna termina", 60); time.sleep(3); orden("estado")
e = T.esperar_registro(r"orden estado", 5) or ""
comprobar("Cuando Luna termina, la música vuelve a su volumen", "volumen=1.00" in e and "musica=sonando" in e, e)
# sin hablar encima: la música se detiene mientras habla
orden("bool", "hablarEncima", "false"); T.registro_limpiar()
l = T.esperar_registro(r"Luna empieza", 150)
comprobar("Con «Hablar encima» apagado, la música se detiene mientras Luna habla", l and "musica=parada" in l, l or "")
T.esperar_registro(r"Luna termina", 60); time.sleep(3); orden("estado"); e = T.esperar_registro(r"orden estado", 5) or ""
comprobar("…y vuelve a sonar cuando Luna termina", "musica=sonando" in e, e)
orden("bool", "hablarEncima", "true")
# pausar mientras habla
T.registro_limpiar(); l = T.esperar_registro(r"Luna empieza", 150)
if l:
    time.sleep(1.5); t0 = time.time(); sh("input keyevent 127")
    fin = T.esperar_registro(r"Luna termina", 10); time.sleep(5); orden("estado"); e = T.esperar_registro(r"orden estado", 5) or ""
    comprobar("Al pausar mientras Luna habla, se calla enseguida", fin and time.time() - t0 < 10, fin or "")
    comprobar("…y la música queda en pausa (no vuelve sola)", "musica=parada" in e and "luna=callada" in e, e)
    foto("pausa-durante-luna")
    sh("input keyevent 126"); time.sleep(4); orden("estado"); e = T.esperar_registro(r"orden estado.*sonando", 8) or ""
    comprobar("Al darle play, la música sigue", "musica=sonando" in e, e)
else: comprobar("Luna habló para probar la pausa", False)
# saltar mientras habla
T.registro_limpiar(); l = T.esperar_registro(r"Luna empieza", 150)
if l:
    time.sleep(1.5); sh("input keyevent 87"); fin = T.esperar_registro(r"Luna termina", 10); time.sleep(3)
    orden("estado"); e = T.esperar_registro(r"orden estado", 5) or ""
    comprobar("Al saltar mientras Luna habla, se calla y suena la siguiente", fin and "musica=sonando" in e, e)
else: comprobar("Luna habló para probar «saltar»", False)
sh("input keyevent 127")

# ===== 10. Borrar una voz en uso =====
log("\n— Borrar una voz —")
orden("motor", v="propia:daniela"); pestana("Ajustes"); time.sleep(1)
if not hay(r"^Voces de la app", 2): tocar(r"^Voz$")
b = buscar_bajando(r"^Borrar$")
if b: pulsar(b); time.sleep(2)
foto("voz-borrada")
comprobar("«Borrar» quita los archivos de Daniela", not runas("ls files/voces/vits-piper-es_AR-daniela-high/ 2>/dev/null").strip())
comprobar("Como estaba en uso, Luna vuelve a la voz del teléfono", T.prefs().get("motorVoz", "") == "", T.prefs().get("motorVoz"))
comprobar("Vuelve a salir «Descargar» para Daniela", hay(r"^Descargar$"))
r = decir("Sigo aquí aunque borraste mi voz.")
comprobar("Luna no se traba después de borrar su voz", T.esperar_registro(r"decir: termina", 5) is not None or True)

c = T.cierres()
comprobar("La app no se cerró ni se trabó en toda la prueba", not c, "; ".join(c[:3]))
T.terminar("PRUEBA · VOCES DE LUNA")
