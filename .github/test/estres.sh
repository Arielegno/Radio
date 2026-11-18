#!/bin/bash
# Prueba de estrés y depuración de Hilo: documentos enormes, miles de tarjetas, toques al azar,
# giros de pantalla, memoria, fluidez y avisos del modo estricto.
set -x
PKG=com.hilo.lector
OUT=shots
F=.github/test
R=$OUT/informe.txt
mkdir -p $OUT
log() { echo "$*" >> $R; }
shot() { sleep "${2:-2}"; timeout 30 adb shell screencap -p /sdcard/s.png && timeout 30 adb pull /sdcard/s.png "$OUT/$1.png" >/dev/null; }
ui() { for i in 1 2 3; do timeout 40 adb shell uiautomator dump /sdcard/ui.xml >/dev/null 2>&1 && timeout 20 adb pull /sdcard/ui.xml /tmp/ui.xml >/dev/null 2>&1 && return 0; sleep 2; done; return 1; }
tap() { rm -f /tmp/ui.xml; ui; local xy; xy=$(python3 $F/find.py /tmp/ui.xml "$1" $2); if [ -n "$xy" ]; then adb shell input tap $xy; else log "  (no encontré «$1»)"; fi; }
has() { rm -f /tmp/ui.xml; ui && [ -n "$(python3 $F/find.py /tmp/ui.xml "$1" $2)" ]; }
put() { adb push "$1" /data/local/tmp/x.bin >/dev/null; adb shell "cat /data/local/tmp/x.bin | run-as $PKG sh -c 'cat > $2'"; }
back() { adb shell input keyevent 4; sleep "${1:-2}"; }
start() { adb shell am force-stop $PKG; adb shell am start -W -n $PKG/.MainActivity | grep -E "TotalTime" ; sleep 3; }
alive() { adb shell pidof $PKG >/dev/null && echo vivo || echo "CERRADA"; }
mem() { log "  Memoria ($1): $(adb shell dumpsys meminfo $PKG | grep -E 'TOTAL PSS|TOTAL:' | head -1 | tr -s ' ')"; }
jank() { log "  Fluidez ($1): $(adb shell dumpsys gfxinfo $PKG | grep -E 'Total frames rendered|Janky frames' | tr -s ' ' | tr '\n' ' ')"; adb shell dumpsys gfxinfo $PKG reset >/dev/null; }
wait_for() {  # wait_for "texto" segundos-máximos → imprime los segundos que tardó
  local t0=$(date +%s)
  while [ $(( $(date +%s) - t0 )) -lt $2 ]; do has "$1" $3 && { echo $(( $(date +%s) - t0 )); return 0; }; sleep 2; done
  echo "más de $2"; return 1
}
room() { start >/dev/null; tap "Opciones de $1"; sleep 1; tap "Estudiar: tarjetas y resaltados"; }

: > $R
log "PRUEBA DE ESTRÉS Y DEPURACIÓN DE HILO — $(date -u '+%Y-%m-%d %H:%M UTC')"
log "Dispositivo: $(adb shell getprop ro.product.model) · Android $(adb shell getprop ro.build.version.release) · $(adb shell wm size | tr -d '\r')"
pip install -q fpdf2 >/dev/null 2>&1
python3 $F/gen_stress.py /tmp/st
ls -la /tmp/st >> $R

adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -W -n $PKG/.MainActivity >/dev/null; sleep 3; adb shell am force-stop $PKG
adb shell "run-as $PKG mkdir -p files/books files/estudio"
adb shell "run-as $PKG touch files/depurar"
put /tmp/st/libro_grande.txt files/books/s1.txt
put /tmp/st/ley_grande.txt files/books/s2.txt
[ -f /tmp/st/pdf_grande.pdf ] && put /tmp/st/pdf_grande.pdf files/books/s3.pdf
put /tmp/st/estudio.json files/estudio/estudio.json
cat > /tmp/lib.json <<'J'
[{"id":"s1","title":"Libro enorme de prueba","author":"Varios","type":"txt","file":"s1.txt","added":3},
 {"id":"s2","title":"Código enorme de prueba","type":"txt","file":"s2.txt","added":2},
 {"id":"s3","title":"PDF de 400 páginas","type":"pdf","file":"s3.pdf","added":1,"pages":400}]
J
put /tmp/lib.json files/library.json
adb logcat -c
adb logcat -G 16M 2>/dev/null

log ""
log "1. ARRANQUE"
for i in 1 2 3; do log "  Arranque en frío $i: $(start)"; done
shot e01_biblioteca 2
mem "biblioteca"

log ""
log "2. LIBRO DE 300.000 PALABRAS"
start >/dev/null; tap "Libro enorme de prueba"
sleep 15; shot e02_libro_abierto; log "  Abierto: $(alive)"
adb shell dumpsys gfxinfo $PKG reset >/dev/null
for i in $(seq 1 40); do adb shell input swipe 540 1900 540 400 120; done
shot e03_libro_desplazado
jank "40 deslizadas rápidas en el libro"
mem "libro abierto"
room "Libro enorme de prueba"
log "  Análisis completo de la sala de estudio: $(wait_for 'Autores' 300) s"
shot e04_sala_libro 1
tap "Palabras"; sleep 4; shot e05_palabras
tap "Mapa"; sleep 8; shot e06_mapa
tap "Línea de tiempo"; sleep 2; shot e07_tiempo
mem "análisis del libro"
log "  Estado: $(alive)"

log ""
log "3. CÓDIGO DE 3000 ARTÍCULOS"
room "Código enorme de prueba"
log "  Índice de la ley listo: $(wait_for 'Ley' 240) s"
tap "Ley"; sleep 3; shot e08_ley
tap "Ir al artículo…"; sleep 1; adb shell input text "2999"; back 1; tap "Ver"; sleep 2; shot e09_art_2999
back 1
tap "Plazos"; sleep 3; shot e10_plazos
mem "código"
log "  Estado: $(alive)"

log ""
log "4. PDF DE 400 PÁGINAS"
start >/dev/null; tap "PDF de 400 páginas"; sleep 10; shot e11_pdf
adb shell dumpsys gfxinfo $PKG reset >/dev/null
for i in $(seq 1 60); do adb shell input swipe 540 1900 540 300 100; done
shot e12_pdf_desplazado
jank "60 deslizadas en el PDF"
mem "PDF"
room "PDF de 400 páginas"
log "  Texto de las 400 páginas analizado: $(wait_for 'Autores' 600) s"
shot e13_sala_pdf 1
log "  Estado: $(alive)"

log ""
log "5. 5000 TARJETAS"
start >/dev/null; tap "Menú"; sleep 2
t0=$(date +%s); tap "Estudio"; sleep 1; log "  Abrir Estudio con 5000 tarjetas: ~$(( $(date +%s) - t0 )) s"
shot e14_estudio
tap "Repasar 15" parcial; sleep 2
t0=$(date +%s)
for i in $(seq 1 15); do tap "Mostrar respuesta"; tap "Bien"; done
log "  Repasar 15 tarjetas (con lectura de pantalla entre toques): $(( $(date +%s) - t0 )) s"
shot e15_fin_repaso
mem "repaso"
log "  Estado: $(alive)"

log ""
log "6. GIROS DE PANTALLA"
start >/dev/null; tap "Libro enorme de prueba"; sleep 6
adb shell settings put system accelerometer_rotation 0
for i in $(seq 1 10); do adb shell settings put system user_rotation 1; sleep 2; adb shell settings put system user_rotation 0; sleep 2; done
shot e16_tras_giros
log "  10 giros en el lector: $(alive)"
room "Libro enorme de prueba"; sleep 20
for i in $(seq 1 6); do adb shell settings put system user_rotation 1; sleep 2; adb shell settings put system user_rotation 0; sleep 2; done
log "  6 giros en la sala de estudio: $(alive)"

log ""
log "7. TOQUES AL AZAR (monkey, 8000 eventos)"
start >/dev/null
timeout 900 adb shell monkey -p $PKG -s 42 --throttle 60 --pct-syskeys 0 --pct-appswitch 0 --pct-anyevent 0 \
  --ignore-crashes --ignore-timeouts --ignore-security-exceptions --monitor-native-crashes -v 8000 > $OUT/monkey.txt 2>&1
log "  $(grep -E 'Events injected' $OUT/monkey.txt | tail -1)"
log "  Cierres detectados por monkey: $(grep -c 'CRASH' $OUT/monkey.txt)"
log "  Bloqueos (ANR) detectados por monkey: $(grep -c 'NOT RESPONDING' $OUT/monkey.txt)"
shot e17_tras_monkey
mem "tras monkey"

log ""
log "8. REGISTRO DEL SISTEMA"
timeout 60 adb logcat -d > $OUT/logcat.txt
grep -n -A 30 "FATAL EXCEPTION" $OUT/logcat.txt > $OUT/cierres.txt
grep -n -B 2 -A 20 "ANR in $PKG" $OUT/logcat.txt > $OUT/anr.txt
log "  Cierres inesperados (FATAL): $(grep -c 'FATAL EXCEPTION' $OUT/logcat.txt)"
log "  Bloqueos (ANR): $(grep -c "ANR in $PKG" $OUT/logcat.txt)"
log "  Falta de memoria (OutOfMemory): $(grep -c 'OutOfMemoryError' $OUT/logcat.txt)"
log "  Avisos del modo estricto: $(grep -c 'StrictMode policy violation' $OUT/logcat.txt)"
python3 - "$OUT/logcat.txt" >> $R <<'P'
import re, sys, collections
lines = open(sys.argv[1], errors="ignore").read().split("\n")
c = collections.Counter()
for i, l in enumerate(lines):
    if "StrictMode policy violation" in l:
        kind = re.search(r"violation[^:]*: (?:android\.os\.strictmode\.)?(\w+)", l)
        where = "?"
        for k in lines[i + 1:i + 40]:
            m = re.search(r"at (com\.hilo\.lector\.[\w.$]+)\(", k)
            if m: where = m.group(1); break
        c[(kind.group(1) if kind else "?", where)] += 1
for (k, w), n in c.most_common(25):
    print(f"    {n:4d} × {k} en {w}")
P
log "  Errores de Hilo en el registro: $(grep -cE ' E (AndroidRuntime|Hilo|com.hilo)' $OUT/logcat.txt)"
exit 0
