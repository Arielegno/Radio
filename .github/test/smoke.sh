#!/bin/bash
# Prueba de Hilo en un emulador: instala, abre libros de varios formatos y toma capturas de cada función.
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
  else echo "NO ENCONTRADO: $1" >> $OUT/steps.txt; cp /tmp/ui.xml "$OUT/ui-$(echo $1 | tr ' /' '__').xml" 2>/dev/null; fi
}
put() {   # put archivo-local destino-en-la-app
  adb push "$1" /data/local/tmp/x.bin >/dev/null
  adb shell "cat /data/local/tmp/x.bin | run-as $PKG sh -c 'cat > $2'"
}
back() { adb shell input keyevent 4; sleep "${1:-2}"; }
swipe_left() { adb shell input swipe 950 1300 120 1300 350; }

adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell wm size >> $OUT/steps.txt
adb shell settings put system accelerometer_rotation 0
adb shell settings put system user_rotation 0
adb logcat -c

adb shell am start -W -n $PKG/.MainActivity
shot 00_inicio 4
adb shell am force-stop $PKG

# Biblioteca de prueba: PDF, EPUB, Word, Markdown y FB2
adb shell "run-as $PKG mkdir -p files/books"
put $F/muestra.pdf files/books/t1.pdf
put $F/muestra.epub files/books/t2.epub
put $F/muestra.docx files/books/t3.docx
put $F/muestra.md files/books/t4.md
put $F/muestra.fb2 files/books/t5.fb2
cat > /tmp/lib.json <<'J'
[{"id":"t1","title":"La sociología del conocimiento","author":"Berger y Luckmann","type":"pdf","file":"t1.pdf","added":5,"updated":9,"progress":0.1,"pages":2,"favorite":true},
 {"id":"t2","title":"Libro EPUB de prueba","author":"Berger y Luckmann","type":"epub","file":"t2.epub","added":4},
 {"id":"t3","title":"Documento Word de prueba","author":"Ariel","type":"docx","file":"t3.docx","added":3,"toRead":true},
 {"id":"t4","title":"Apuntes en Markdown","type":"md","file":"t4.md","added":2},
 {"id":"t5","title":"Libro FB2 de prueba","author":"Peter Berger","type":"fb2","file":"t5.fb2","added":1,"collections":["Sociología"]}]
J
put /tmp/lib.json files/library.json

adb shell am start -W -n $PKG/.MainActivity
shot 01_biblioteca 4
tap "Menú"
shot 02_menu_lateral 2
back

# PDF desplazando hacia abajo
tap "Seguir leyendo"
shot 03_pdf_vertical 8

# Ajustes: pasar páginas en horizontal
tap "Ajustes de lectura"
sleep 2
tap "Horizontal"
shot 04_ajustes_horizontal 2
back 3
shot 05_pdf_horizontal 6
swipe_left
shot 06_pdf_pagina_2 8

# Modo sepia
tap "Ajustes de lectura"
sleep 2
tap "Sepia"
back 3
shot 07_pdf_sepia 6

# Marcador e índice
tap "Agregar marcador"
shot 08_marcador 2
tap "Contenido y marcadores"
sleep 2
tap "Marcadores"
shot 09_marcadores 2
back

# Modo noche y texto en páginas
tap "Ajustes de lectura"
sleep 2
tap "Noche"
back 3
tap "Texto"
shot 10_texto_paginas_noche 10
swipe_left
shot 11_texto_pagina_2 3

# Horizontal con dos páginas en PDF
tap "Página"
sleep 3
tap "Ajustes de lectura"
sleep 2
tap "Dos páginas con el celular acostado"
tap "Día"
back 3
adb shell settings put system user_rotation 1
shot 12_dos_paginas 8
adb shell settings put system user_rotation 0
sleep 3

# Volver a la biblioteca y abrir los otros formatos
back 3
tap "Libro EPUB de prueba"
shot 13_epub 8
tap "Contenido y marcadores"
shot 14_indice_epub 3
back
back 3
tap "Documento Word de prueba"
shot 15_word 8
back 3
tap "Apuntes en Markdown"
shot 16_markdown 8
back 3
tap "Libro FB2 de prueba"
shot 17_fb2 8
back 3

# Vertical otra vez con regla en texto
tap "Libro EPUB de prueba"
sleep 4
tap "Ajustes de lectura"
sleep 2
tap "Hacia abajo"
back 3
tap "Regla"
shot 18_epub_vertical_regla 4
tap "Siguiente línea"
shot 19_regla_baja 3
tap "Regla"
back 3

tap "Menú"
sleep 2
tap "Formatos"
shot 20_formatos 2
back

timeout 60 adb logcat -d > $OUT/logcat.txt
grep -n -A 40 "FATAL EXCEPTION" $OUT/logcat.txt > $OUT/crashes.txt || echo "Sin cierres inesperados" > $OUT/crashes.txt
exit 0
