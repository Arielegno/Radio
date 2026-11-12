#!/usr/bin/env bash
# Prueba de estrés (v2): instala la app en el teléfono virtual, le carga canciones y la usa a lo loco con «monkey».
set -u
PKG=io.github.arielegno.radio
APK=$1; SEM=$2; OUT=$3; mkdir -p "$OUT"
timeout 300 adb wait-for-device
adb shell settings put global window_animation_scale 0; adb shell settings put global transition_animation_scale 0; adb shell settings put global animator_duration_scale 0
adb install -r -g "$APK" || { echo "No se pudo instalar"; exit 1; }
# primera apertura (crea sus carpetas) y se cierra
adb shell monkey -p $PKG -c android.intent.category.LAUNCHER 1 >/dev/null; sleep 8; adb shell am force-stop $PKG
# cargar las canciones de prueba dentro de la app
adb push "$SEM" /data/local/tmp/semilla >/dev/null
adb shell "run-as $PKG sh -c 'mkdir -p files/music files/covers && cp /data/local/tmp/semilla/music/* files/music/ && cp /data/local/tmp/semilla/covers/* files/covers/ && cp /data/local/tmp/semilla/biblioteca.json files/biblioteca.json'" || echo "AVISO: no se pudieron cargar las canciones"
adb shell "run-as $PKG ls files/music | wc -l"
adb logcat -c
total=0
vivo() { [ "$(timeout 10 adb get-state 2>/dev/null)" = "device" ]; }
for semilla in 11 22 33 44 55 66; do
  vivo || { echo "El teléfono virtual se cayó: se termina la prueba con lo que hay"; break; }
  echo "=== Ronda con semilla $semilla ==="
  adb shell monkey -p $PKG --ignore-crashes --ignore-timeouts --ignore-security-exceptions --monitor-native-crashes \
     --pct-syskeys 2 --pct-appswitch 2 --pct-touch 45 --pct-motion 20 --pct-nav 8 --pct-majornav 8 --pct-anyevent 2 \
     -s $semilla --throttle 40 -v 2500 > "$OUT/monkey-$semilla.txt" 2>&1
  grep -E "CRASH|NOT RESPONDING|Monkey aborted|Events injected" "$OUT/monkey-$semilla.txt" | head -20
  # entre rondas: abrir la app y poner la radio con el botón multimedia (play) para que suene y hable
  adb shell monkey -p $PKG -c android.intent.category.LAUNCHER 1 >/dev/null; sleep 2
  adb shell input keyevent 126; sleep 25; adb shell input keyevent 87; sleep 5; adb shell input keyevent 87; sleep 3; adb shell input keyevent 127
done
# ===== Segunda parte: Luna hablando con la voz de la app (Ald) entre TODAS las canciones, mientras se usa la app =====
if [ -d "$SEM/voces" ] && vivo; then
  echo "=== Luna con voz de la app ==="
  adb shell am force-stop $PKG
  adb push "$SEM/voces" /data/local/tmp/voces >/dev/null
  adb shell "run-as $PKG sh -c 'mkdir -p files/voces && cp -r /data/local/tmp/voces/* files/voces/'"
  adb shell "run-as $PKG sh -c 'ls files/voces/*'" | head -5
  # ajustes: voz Ald, Luna al aire, habla en cada canción y encima de la música
  adb shell "run-as $PKG sh -c 'cat shared_prefs/radio.xml'" > "$OUT/prefs-antes.xml" 2>/dev/null
  python3 - "$OUT/prefs-antes.xml" "$OUT/radio.xml" <<'PY'
import sys, re
try: x = open(sys.argv[1]).read()
except Exception: x = ""
if "<map" not in x: x = "<?xml version='1.0' encoding='utf-8' standalone='yes' ?>\n<map>\n</map>\n"
def poner(x, tipo, k, v):
    x = re.sub(r'\s*<[a-z]+ name="%s"[^>]*?(/>|>[^<]*</string>)' % re.escape(k), "", x)
    linea = '    <string name="%s">%s</string>' % (k, v) if tipo == "string" else '    <%s name="%s" value="%s" />' % (tipo, k, v)
    return x.replace("</map>", linea + "\n</map>")
for t, k, v in [("string", "motorVoz", "propia:ald"), ("boolean", "aire", "true"), ("int", "cadaN", "1"), ("boolean", "hablarEncima", "true")]:
    x = poner(x, t, k, v)
open(sys.argv[2], "w").write(x)
PY
  adb push "$OUT/radio.xml" /data/local/tmp/radio.xml >/dev/null
  adb shell "run-as $PKG sh -c 'mkdir -p shared_prefs && cp /data/local/tmp/radio.xml shared_prefs/radio.xml'"
  adb shell monkey -p $PKG -c android.intent.category.LAUNCHER 1 >/dev/null; sleep 4
  for vuelta in 1 2 3; do
    vivo || { echo "El teléfono virtual se cayó"; break; }
    adb shell input keyevent 126            # play: arranca la radio y Luna saluda
    sleep 70                                # varias canciones cortas: Luna habla entre cada una
    adb shell monkey -p $PKG --ignore-crashes --ignore-timeouts --ignore-security-exceptions --monitor-native-crashes \
       --pct-syskeys 0 --pct-appswitch 0 --pct-touch 55 --pct-motion 25 --pct-nav 10 --pct-majornav 10 \
       -s $((100 + vuelta)) --throttle 250 -v 500 > "$OUT/monkey-luna-$vuelta.txt" 2>&1
    grep -E "CRASH|NOT RESPONDING|Events injected" "$OUT/monkey-luna-$vuelta.txt" | head
    adb shell input keyevent 87; sleep 3; adb shell input keyevent 87; sleep 3; adb shell input keyevent 87   # saltar seguido
    sleep 40
    adb shell input keyevent 127; sleep 2; adb shell input keyevent 126; sleep 20   # pausa y sigue
  done
fi

timeout 60 adb logcat -d -b crash > "$OUT/crash.txt" 2>&1
timeout 120 adb logcat -d > "$OUT/logcat.txt" 2>&1
timeout 60 adb shell dumpsys activity exit-info $PKG > "$OUT/salidas.txt" 2>&1
timeout 60 adb shell "run-as $PKG sh -c 'for f in files/reportes/reporte-*.txt; do cat \$f; echo; echo ────────────; done'" > "$OUT/reportes-app.txt" 2>&1
grep -E "FATAL EXCEPTION|ANR in $PKG|Fatal signal|Process: $PKG|Luna no pudo|ERROR ATRAPADO" "$OUT/logcat.txt" > "$OUT/resumen.txt" || true
echo "Cierres y bloqueos encontrados:"; cat "$OUT/resumen.txt"; grep -c "" "$OUT/resumen.txt"
exit 0
