#!/usr/bin/env bash
# Prueba de estrés: instala la app en el teléfono virtual, le carga canciones y la usa a lo loco con «monkey».
set -u
PKG=io.github.arielegno.radio
APK=$1; SEM=$2; OUT=$3; mkdir -p "$OUT"
adb wait-for-device
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
for semilla in 11 22 33 44 55 66; do
  echo "=== Ronda con semilla $semilla ==="
  adb shell monkey -p $PKG --ignore-crashes --ignore-timeouts --ignore-security-exceptions --monitor-native-crashes \
     --pct-syskeys 2 --pct-appswitch 2 --pct-touch 45 --pct-motion 20 --pct-nav 8 --pct-majornav 8 --pct-anyevent 2 \
     -s $semilla --throttle 40 -v 2500 > "$OUT/monkey-$semilla.txt" 2>&1
  grep -E "CRASH|NOT RESPONDING|Monkey aborted|Events injected" "$OUT/monkey-$semilla.txt" | head -20
  # entre rondas: abrir la app y poner la radio con el botón multimedia (play) para que suene y hable
  adb shell monkey -p $PKG -c android.intent.category.LAUNCHER 1 >/dev/null; sleep 2
  adb shell input keyevent 126; sleep 25; adb shell input keyevent 87; sleep 5; adb shell input keyevent 87; sleep 3; adb shell input keyevent 127
done
adb logcat -d -b crash > "$OUT/crash.txt" 2>&1
adb logcat -d > "$OUT/logcat.txt" 2>&1
adb shell dumpsys activity exit-info $PKG > "$OUT/salidas.txt" 2>&1
adb shell "run-as $PKG sh -c 'for f in files/reportes/reporte-*.txt; do cat \$f; echo; echo ────────────; done'" > "$OUT/reportes-app.txt" 2>&1
grep -E "FATAL EXCEPTION|ANR in $PKG|Fatal signal" "$OUT/logcat.txt" > "$OUT/resumen.txt" || true
echo "Cierres y bloqueos encontrados:"; cat "$OUT/resumen.txt"; grep -c "" "$OUT/resumen.txt"
exit 0
