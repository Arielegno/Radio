#!/bin/bash
# Prueba de humo de Hilo en un emulador: instala, abre un PDF y toma capturas de cada función.
set -x
PKG=com.hilo.lector
OUT=shots
mkdir -p $OUT
F=.github/test

shot() {
  sleep "${2:-3}"
  timeout 30 adb shell screencap -p /sdcard/s.png && timeout 30 adb pull /sdcard/s.png "$OUT/$1.png" >/dev/null
  echo "CAPTURA $1: $?" >> $OUT/steps.txt
}
ui() {
  for i in 1 2 3 4; do
    timeout 40 adb shell uiautomator dump /sdcard/ui.xml >/dev/null 2>&1 && timeout 20 adb pull /sdcard/ui.xml /tmp/ui.xml >/dev/null 2>&1 && return 0
    sleep 2
  done
  return 1
}
tap() {   # tap "texto o descripción" [parcial]
  ui
  local xy; xy=$(python3 $F/find.py /tmp/ui.xml "$1" $2)
  if [ -n "$xy" ]; then adb shell input tap $xy; echo "TAP OK: $1" >> $OUT/steps.txt
  else echo "NO ENCONTRADO: $1" >> $OUT/steps.txt; cp /tmp/ui.xml "$OUT/ui-$(echo $1 | tr ' /' '__').xml"; fi
}

adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell wm size >> $OUT/steps.txt
adb shell settings put system accelerometer_rotation 0
adb shell settings put system user_rotation 0
adb logcat -c

# Primera apertura (crea carpetas) y biblioteca vacía
adb shell am start -W -n $PKG/.MainActivity
shot 00_inicio 4
adb shell am force-stop $PKG

# Copiar el PDF de prueba a la biblioteca de la app
adb push $F/muestra.pdf /data/local/tmp/muestra.pdf
adb shell "run-as $PKG mkdir -p files/books"
adb shell "cat /data/local/tmp/muestra.pdf | run-as $PKG sh -c 'cat > files/books/t1.pdf'"
echo '[{"id":"t1","title":"La sociología del conocimiento","type":"pdf","file":"t1.pdf","added":1,"updated":2,"progress":0.1,"pages":3}]' > /tmp/lib.json
adb push /tmp/lib.json /data/local/tmp/lib.json
adb shell "cat /data/local/tmp/lib.json | run-as $PKG sh -c 'cat > files/library.json'"

adb shell am start -W -n $PKG/.MainActivity
shot 01_biblioteca 4

tap "Seguir leyendo"
shot 02_pdf_colores 8

# Página escaneada (reconocimiento de texto)
adb shell input swipe 540 1900 540 300 300
adb shell input swipe 540 1900 540 300 300
adb shell input swipe 540 1900 540 300 300
shot 03_pagina_escaneada 15
adb shell input swipe 540 400 540 2000 200
adb shell input swipe 540 400 540 2000 200
adb shell input swipe 540 400 540 2000 200
adb shell input swipe 540 400 540 2000 200
sleep 2

# Regla
tap "Regla"
shot 04_regla 3
tap "Siguiente línea"
sleep 1
tap "Siguiente línea"
shot 05_regla_baja 3
tap "Regla"
sleep 1

# Anotar: resaltar y lápiz
tap "Anotar sobre el PDF"
shot 06_anotar 2
adb shell input swipe 150 700 900 705 900
shot 07_resaltado 2
tap "Lápiz"
adb shell input swipe 200 1100 800 1250 700
shot 08_lapiz 2
tap "Listo"
sleep 1
timeout 20 adb shell "run-as $PKG cat files/annot/t1.json" > $OUT/anotaciones.json

# Texto fluido y negrita guía
tap "Texto"
shot 09_texto_fluido 10
tap "Negrita guía"
shot 10_negrita_guia 3
tap "Negrita guía"

# Palabra a palabra
tap "Leer palabra por palabra"
sleep 2
tap "Reproducir"
shot 11_palabra_a_palabra 2
tap "Salir"
sleep 1

# Ajustes
tap "Ajustes de lectura"
shot 12_ajustes 3
adb shell input keyevent 4
sleep 2

# Horizontal
adb shell settings put system user_rotation 1
shot 13_horizontal_texto 5
tap "Página"
shot 14_horizontal_pdf 8
adb shell settings put system user_rotation 0
sleep 3

# Tema oscuro
adb shell cmd uimode night yes
shot 15_oscuro 6
adb shell cmd uimode night no
sleep 2

# Compartir un texto copiado de un PDF (líneas y palabras cortadas)
adb shell input keyevent 4
sleep 2
adb shell am start -a android.intent.action.SEND -t text/plain -n $PKG/.MainActivity \
  --es android.intent.extra.TEXT "'De su enfoque discriminatorio, Stark se halla probablemente\nmás cerca de Scheler que de Mannheim en su concepción\nde las relaciones entre las ideas y su contexto social.\nAsimismo, es evidente que no hemos intentado ofrecer\nun panorama histórico adecuado de la historia de la socio-\nlogía del conocimiento. Además, hasta este momento hemos\npasado por alto hechos que teóricamente podrían ser rele-\nvantes a la sociología del conocimiento.'"
shot 16_texto_compartido 6
adb shell input keyevent 4
shot 17_biblioteca_final 3

timeout 60 adb logcat -d > $OUT/logcat.txt
grep -n -A 30 "FATAL EXCEPTION" $OUT/logcat.txt > $OUT/crashes.txt || echo "Sin cierres inesperados" > $OUT/crashes.txt
timeout 60 adb logcat -d -s AndroidRuntime:E > $OUT/errores.txt
exit 0
