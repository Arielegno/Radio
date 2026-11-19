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
tapxy() { adb shell input tap $1 $2; echo "TAP XY: $1 $2" >> $OUT/steps.txt; sleep 1; }
frames() {  # cuántos cuadros dibuja la app en 3 s sin tocar nada (debería ser casi 0)
  adb shell dumpsys gfxinfo $PKG reset >/dev/null
  sleep 3
  echo "CUADROS EN REPOSO ($1): $(adb shell dumpsys gfxinfo $PKG | grep 'Total frames rendered' | head -1)" >> $OUT/steps.txt
}
xy() { python3 $F/find.py /tmp/ui.xml "$1" $2; }
hold() {  # mantener el dedo sobre un elemento
  rm -f /tmp/ui.xml; ui; local p; p=$(python3 $F/find.py /tmp/ui.xml "$1" $2)
  if [ -n "$p" ]; then adb shell input swipe $p $p 900; echo "HOLD: $1" >> $OUT/steps.txt; else echo "NO ENCONTRADO: $1" >> $OUT/steps.txt; fi
  sleep 1
}
tapat() { if [ -n "$1" ]; then adb shell input tap $1; echo "TAP AT: $1" >> $OUT/steps.txt; else echo "SIN COORDENADAS" >> $OUT/steps.txt; fi; sleep 1; }
room() {  # abre la sala de estudio de un libro: room "Título" segundos palabra-para-buscar
  adb shell am force-stop $PKG; adb shell am start -W -n $PKG/.MainActivity; sleep 3
  seek "$3"
  tap "Opciones de $1"; sleep 1; tap "Estudiar: tarjetas y resaltados"; sleep "${2:-6}"
}
tap() {   # tap "texto o descripción" [parcial]
  rm -f /tmp/ui.xml
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
seek() {  # busca en la biblioteca para que el libro quede a la vista (en la lista entran menos)
  [ -z "$1" ] && return 0
  tap "Buscar"; sleep 1; adb shell input text "$1"; sleep 1; adb shell input keyevent 4; sleep 1
}
open_book() { adb shell am force-stop $PKG; adb shell am start -W -n $PKG/.MainActivity; sleep 3; seek "$2"; tap "$1"; }

adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell wm size >> $OUT/steps.txt
adb shell settings put system accelerometer_rotation 0
adb shell settings put system user_rotation 0
adb logcat -c

adb shell am start -W -n $PKG/.MainActivity
shot 00_bienvenida 4
tap "Necesito letra grande"
shot 00b_perfil 2
tap "Ahora no"
shot 00c_inicio 3
adb shell am force-stop $PKG

# Biblioteca de prueba: PDF, EPUB, Word, Markdown y FB2
adb shell "run-as $PKG mkdir -p files/books"
put $F/muestra.pdf files/books/t1.pdf
put $F/muestra.epub files/books/t2.epub
put $F/muestra.docx files/books/t3.docx
put $F/muestra.md files/books/t4.md
put $F/muestra.fb2 files/books/t5.fb2
put $F/muestra_ley.txt files/books/t6.txt
put $F/muestra_teoria.txt files/books/t7.txt
put $F/muestra.mobi files/books/t8.mobi
put $F/muestra.cbz files/books/t9.cbz
# La guía de gestos ya vista (se prueba aparte al final)
cat > /tmp/guias.xml <<'X'
<?xml version='1.0' encoding='utf-8' standalone='yes' ?>
<map><boolean name="gestos" value="true" /></map>
X
adb shell "run-as $PKG mkdir -p shared_prefs"
put /tmp/guias.xml shared_prefs/guias.xml
cat > /tmp/lib.json <<'J'
[{"id":"t1","title":"La sociología del conocimiento","author":"Berger y Luckmann","type":"pdf","file":"t1.pdf","added":5,"updated":9,"progress":0.1,"pages":2,"favorite":true},
 {"id":"t2","title":"Libro EPUB de prueba","author":"Berger y Luckmann","type":"epub","file":"t2.epub","added":4},
 {"id":"t3","title":"Documento Word de prueba","author":"Ariel","type":"docx","file":"t3.docx","added":3,"toRead":true},
 {"id":"t4","title":"Apuntes en Markdown","type":"md","file":"t4.md","added":2},
 {"id":"t5","title":"Libro FB2 de prueba","author":"Peter Berger","type":"fb2","file":"t5.fb2","added":1,"collections":["Sociología"]},
 {"id":"t6","title":"Código Procesal de prueba","type":"txt","file":"t6.txt","added":0},
 {"id":"t7","title":"Teoría sociológica de prueba","author":"Varios","type":"txt","file":"t7.txt","added":0},
 {"id":"t8","title":"Libro MOBI de prueba","author":"Émile Durkheim","type":"mobi","file":"t8.mobi","added":0},
 {"id":"t9","title":"Cómic de prueba","type":"comic","file":"t9.cbz","added":0}]
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

# Abrir los otros formatos (en páginas horizontales, modo Día)
open_book "Libro EPUB de prueba" EPUB
shot 13_epub 8
swipe_left
shot 14_epub_pagina_2 3
tap "Contenido y marcadores"
shot 15_indice_epub 3
back
open_book "Documento Word de prueba" Word
shot 16_word 8
open_book "Apuntes en Markdown" Markdown
shot 17_markdown 8
open_book "Libro FB2 de prueba" FB2
shot 18_fb2 8

# Desplazar hacia abajo con la regla
open_book "Libro EPUB de prueba" EPUB
sleep 4
tap "Ajustes de lectura"
sleep 2
tap "Hacia abajo"
back 3
tap "Regla"
shot 19_epub_vertical_regla 4
tap "Siguiente línea"
shot 20_regla_baja 3
tap "Regla"

# Ocultar menús tocando el centro
adb shell input tap 540 1300
shot 21_sin_menus 3

# ---------- Estudio ----------
# Crear una tarjeta para completar
open_book "Libro EPUB de prueba" EPUB
sleep 5
adb shell input swipe 400 900 400 900 900
shot 22_seleccion_menu 2
tap "Más"
shot 22b_seleccion_mas 2
tap "Estudiar: tarjetas y palabras clave"
shot 23_hoja_estudio 3
tap "sociología"
shot 23b_hoja_elegida 2
tap "Completar"
sleep 2
# Subrayar otra frase con su tipo
open_book "Libro EPUB de prueba" EPUB
sleep 5
adb shell input swipe 400 1250 400 1250 900
sleep 2
tap "Subrayar"
shot 24a_colores 2
tap "Definición"
shot 24_resaltado 3
# Buscar en el documento
tap "Contenido y marcadores"
sleep 2
tap "Buscar"
sleep 1
adb shell input text "estandarte"
shot 25_buscar 5
back
back 2
# Sala de estudio del documento

adb shell am force-stop $PKG
adb shell am start -W -n $PKG/.MainActivity
sleep 3
seek EPUB
tap "Opciones de Libro EPUB de prueba"
sleep 1
tap "Estudiar: tarjetas y resaltados"
shot 26_sala_ruta 3
tap "Prueba previa" parcial
shot 26b_prueba_previa 3
back 2
tap "Resaltados"
shot 27_resaltados 2
tap "Apuntes"
shot 28_apuntes 2
tap "Tarjetas"
sleep 1
# Repaso
tap "Repasar 1" parcial
sleep 2
shot 29_repaso_frente 2
tap "Dudo"
shot 30_repaso_respuesta 2
tap "Bien"
shot 31_repaso_fin 2
back

# ---------- Guía, palabras, glosario y mapa ----------
adb shell am force-stop $PKG
adb shell am start -W -n $PKG/.MainActivity
sleep 3
seek EPUB
tap "Opciones de Libro EPUB de prueba"
sleep 1
tap "Estudiar: tarjetas y resaltados"
sleep 2
tap "Guía"
shot 40_guia 5
frames guia
rm -f /tmp/ui.xml; ui
P=$(xy "Palabras"); G=$(xy "Glosario"); M=$(xy "Mapa")
echo "CHIPS: P=$P G=$G M=$M" >> $OUT/steps.txt
tapat "$P"
shot 43_nube_palabras 5
frames palabras
adb shell input swipe 540 2000 540 900 400
sleep 2
shot 44_lista_palabras 2
tap "¿Cómo elige Hilo las palabras?"
shot 44b_como_elige 2
tap "Comunes" parcial
shot 44c_comunes 2
adb shell input swipe 540 2000 540 600 400
sleep 1
shot 44d_todas 2
tap "sociedad"
sleep 1
tap "En contexto"
shot 44e_contexto 2
tap "En el libro"
shot 44f_distribucion 2
tap "Va junto a…"
shot 44g_junto 2
for i in 1 2 3 4; do adb shell input swipe 540 700 540 2000 300; done
sleep 1
rm -f /tmp/ui.xml; ui
G=$(xy "Glosario"); M=$(xy "Mapa")
tapat "$G"
shot 45_glosario 4
frames glosario
tapat "$M"
shot 46_mapa 8
frames mapa
adb shell input tap 540 1500
shot 47_mapa_toque 2
back
back

# ---------- Versión 1.7: MOBI, página de libro, lista, selección, carpetas, novedades ----------
open_book "Libro MOBI de prueba" MOBI
shot 70_mobi_pagina 10
tap "Ajustes de lectura"
sleep 2
shot 71_ajustes_pagina 2
adb shell input swipe 540 1900 540 700 400
shot 72_ajustes_letra 2
adb shell input swipe 540 1900 540 700 400
shot 73_ajustes_mas 2
back 2
adb shell am force-stop $PKG
adb shell am start -W -n $PKG/.MainActivity
sleep 3
shot 74_lista 2
seek Word
hold "Documento Word de prueba"
shot 75_seleccion 2
tap "Todos"
shot 77_todos 2
back 2
tap "Menú"
sleep 2
tap "Carpetas"
shot 78_carpetas 2
back 2
tap "Menú"
sleep 2
adb shell input swipe 300 1800 300 600 400
tap "Novedades" parcial
shot 79_novedades 3
back 2

# ---------- Derecho: ley, artículos enlazados y plazos ----------
room "Código Procesal de prueba" 8 Procesal
shot 50_sala_ley 2
tap "Ley"
shot 51_ley 3
tap "Libro PRIMERO" parcial
sleep 1
tap "Título I" parcial
sleep 1
shot 52_ley_indice 2
tap "Ir al artículo…"
sleep 1
adb shell input text "1"
back 1
tap "Ver"
shot 53_articulo 3
tap "Art. 3"
shot 54_articulo_enlazado 2
tap "Cerrar"
sleep 1
tap "Plazos"
shot 55_plazos 3
tap "Fichas"
sleep 1
tap "+ caso o sentencia"
shot 56_ficha_caso 3
back 2
# ---------- Sociología: autores, cuadro, línea de tiempo, citas, Feynman ----------
room "Teoría sociológica de prueba" 8 Varios
shot 57_sala_teoria 2
tap "Autores"
shot 58_autores 3
tap "Comparar"
sleep 1
adb shell input swipe 540 1800 540 900 500
sleep 1
tap "Comparar"
sleep 1
adb shell input swipe 540 700 540 2000 300
adb shell input swipe 540 700 540 2000 300
sleep 1
tap "Comparar Marx" parcial
shot 59_cuadro 3
tap "Línea de tiempo"
shot 60_linea_tiempo 3
tap "Citas"
shot 61_citas 3
tap "Autores"
sleep 1
adb shell input swipe 540 1800 540 900 500
sleep 1
tap "Explícalo"
sleep 2
adb shell input text "la%sestructura%seconomica%sdetermina%sla%sconciencia%sy%shay%slucha%sde%sclases"
sleep 1
tap "Comparar"
shot 62_feynman 3
tap "Listo"
# Fecha de examen
tap "Pon la fecha" parcial
shot 63_fecha_examen 3
back 2
# ---------- Estudio general: herramientas y mapa entre documentos ----------
adb shell am force-stop $PKG
adb shell am start -W -n $PKG/.MainActivity
sleep 3
tap "Menú"
sleep 2
tap "Estudio"
sleep 2
adb shell input swipe 540 2000 540 500 400
shot 64_herramientas 3
tap "Mapa entre tus documentos"
shot 65_mapa_documentos 20
back 2

# ---------- Documentos del celular (como ReadEra) ----------
adb shell mkdir -p /sdcard/Download
adb push $F/muestra.pdf "/sdcard/Download/Codigo_Civil_prueba.pdf"
adb shell appops set $PKG MANAGE_EXTERNAL_STORAGE allow
adb shell content call --method scan_volume --uri content://media --arg external_primary >/dev/null 2>&1
adb shell am broadcast -a android.intent.action.MEDIA_SCANNER_SCAN_FILE -d "file:///sdcard/Download/Codigo_Civil_prueba.pdf" >/dev/null 2>&1
sleep 4
adb shell am force-stop $PKG
adb shell am start -W -n $PKG/.MainActivity
shot 32_documentos_nuevos 8
tap "VER"
shot 33_nuevos 3

adb shell am force-stop $PKG
adb shell am start -W -n $PKG/.MainActivity
sleep 3
tap "Menú"
sleep 2
tap "Estudio"
shot 34_estudio_general 3
tap "Estadísticas"
shot 35_estadisticas 3
back 2
tap "Simulacro"
shot 36_simulacro 3
back 2
back

# ---------- Versión 2.0: cómic, búsqueda global, filtros, barra inferior, guía de gestos ----------
open_book "Cómic de prueba" Comic
shot 80_comic 6
swipe_left
shot 81_comic_pag2 3
adb shell am force-stop $PKG
adb shell am start -W -n $PKG/.MainActivity
sleep 8
tap "Buscar"; sleep 1; adb shell input text "sociedad"; sleep 3
shot 82_busqueda_global 2
back 1
adb shell am force-stop $PKG
adb shell am start -W -n $PKG/.MainActivity
sleep 3
tap "Filtros"
shot 83_filtros 2
back 2
shot 83b_filtros_cerrados 1
tap "Estudiar"
shot 84_barra_estudiar 3
back 2
adb shell "run-as $PKG rm -f shared_prefs/guias.xml"
open_book "Libro FB2 de prueba" FB2
shot 85_guia_gestos 6
adb shell input tap 540 1300
shot 86_tras_guia 3

# ---------- Versión 2.1: selección en el PDF y guía nueva ----------
open_book "Codigo Civil prueba" Codigo
sleep 5
adb shell input tap 540 1300
sleep 1
adb shell input swipe 500 900 500 900 900
shot 87_pdf_seleccion 3
tap "Subrayar"
tap "Tesis"
shot 88_pdf_subrayado 3
adb shell am force-stop $PKG
adb shell am start -W -n $PKG/.MainActivity
sleep 3
tap "Menú"
sleep 1
adb shell input swipe 300 1800 300 500 400
sleep 1
tap "Cómo usar Hilo"
shot 89_guia_nueva 3
tap "Elegir texto (selección)"
shot 90_guia_tema 3

# ---------- 2.3: ayudas TDAH y botones de ayuda ----------
adb shell am force-stop $PKG
adb shell am start -W -n $PKG/.MainActivity
sleep 3
shot 94_biblioteca_racha 2
tap "Ayuda: cómo se usa"
shot 95_ayuda_hoja 2
back 1
open_book "Libro EPUB de prueba" EPUB
sleep 4
adb shell input tap 540 1300
sleep 1
tap "Ayudas para concentrarte"
shot 96_panel_tdah 2
adb shell input swipe 540 1800 540 700 400
sleep 1
shot 97_panel_tdah_2 2
tap "Cada 3 pág."
adb shell input swipe 540 1800 540 700 400
sleep 1
shot 98_panel_tdah_3 2
tap "Iniciar"
sleep 4
shot 99_teleprompter 2
tap "Detener desplazamiento"
adb shell input tap 540 1300
sleep 1
tap "💭"
sleep 1
adb shell input text "pagar%sla%sluz"
shot 100_libreta 2
tap "Guardar y seguir"
sleep 1
tap "Ayudas para concentrarte"
tap "Foco de un párrafo" parcial
adb shell input tap 980 470
back 1
sleep 1
adb shell input tap 540 1300
sleep 1
shot 101_foco_parrafo 2
adb shell am force-stop $PKG
adb shell am start -W -n $PKG/.MainActivity
sleep 3
tap "Estudiar"
sleep 2
shot 102_estudiar_energia 2
tap "¿Cómo está tu energía?"
tap "Media" parcial
shot 103_energia_media 2
back 1

# ---------- 2.1.1: ocultar formatos ----------
adb shell am force-stop $PKG
adb shell am start -W -n $PKG/.MainActivity
sleep 3
tap "Ajustes"
sleep 1
for i in 1 2 3 4 5 6; do adb shell input swipe 540 1900 540 700 300; sleep 0.5; done
shot 91_formatos_ajuste 2
tap "✓ Markdown"
tap "✓ Texto"
shot 92_formatos_ocultos 2
back 2
shot 93_biblioteca_sin_formatos 3

timeout 60 adb logcat -d > $OUT/logcat.txt
grep -n -A 40 "FATAL EXCEPTION" $OUT/logcat.txt > $OUT/crashes.txt || echo "Sin cierres inesperados" > $OUT/crashes.txt
exit 0
