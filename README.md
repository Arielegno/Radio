# Hilo · lector para TDAH (Android, Kotlin)

Lector de PDF, EPUB y texto con ayudas para mantener la atención:

- **Página original**: el PDF con su formato (imágenes, tablas, columnas). Encima se pintan
  los **colores por línea** y la **regla de enfoque**. En páginas escaneadas o fotos, Hilo
  reconoce el texto sin internet (ML Kit).
- **Texto fluido**: el libro como párrafos de corrido, con **interlineado compacto (1.35)**,
  negrita guía y colores por línea. Todo ajustable en *Aa*.
- **Anotar sobre el PDF**: resaltador que se ajusta a las líneas, lápiz, borrador, notas,
  deshacer y **Guardar PDF** (copia con tus anotaciones, se abre en cualquier lector).
- Palabra a palabra, temporizador de enfoque, zoom con dos dedos, vertical y horizontal.
- Biblioteca con portadas, búsqueda y avance. Recibe archivos desde *Compartir* y *Abrir con*.

## Instalarlo en tu celular

### Opción A: con Android Studio (recomendada)
1. Instala [Android Studio](https://developer.android.com/studio) (gratis).
2. *File → Open* y elige la carpeta `Hilo`. Espera a que termine "Gradle sync"
   (la primera vez descarga librerías, necesita internet; tarda unos minutos).
3. En el celular: *Ajustes → Acerca del teléfono →* toca 7 veces *Número de compilación*.
   Luego en *Opciones de desarrollador* activa *Depuración USB*.
4. Conecta el celular con cable, elígelo arriba en Android Studio y presiona ▶ **Run**.

Para tener un archivo instalable: *Build → Build App Bundle(s) / APK(s) → Build APK(s)*.
El APK queda en `app/build/outputs/apk/debug/app-debug.apk`; pásalo al celular y ábrelo
(acepta "instalar apps de origen desconocido").

### Opción B: sin instalar nada (GitHub)
1. Crea un repositorio en GitHub y sube esta carpeta.
2. En la pestaña **Actions** se compila solo. Al terminar, abre la ejecución y descarga
   **Hilo-apk**. Dentro está `app-debug.apk` para instalar en el celular.

## Estructura
- `pdf/PdfEngine.kt`: dibuja páginas, encuentra cada línea (PdfBox) o la reconoce (ML Kit)
  y pinta los degradados sobre la página sin tocar el formato.
- `pdf/Annotations.kt`: trazos, notas y exportación a PDF.
- `ui/PdfView.kt`: visor con zoom, regla y herramientas de anotación.
- `ui/FlowView.kt`: texto fluido, negrita guía y palabra a palabra.
- `text/TextTools.kt`: une líneas y palabras cortadas ("socio-" + "logía").
- `data/`: biblioteca y ajustes.
