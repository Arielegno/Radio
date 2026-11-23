# Estudio Musical · Android (Kotlin + Jetpack Compose)

Estudio de grabación, instrumentos digitales y cuaderno de canciones. **Todo funciona en el teléfono, sin nube ni cuentas.**

## Pestañas
| | Qué hace |
|---|---|
| **Grabar** | WAV 48 kHz/16 bits. Micrófono USB tipo C, cable o Bluetooth (se detecta al conectarlo). Metrónomo con cuenta de entrada, **grabar encima de una base (overdub)** con compensación de latencia, **monitoreo** en audífonos, vúmetro y lista de tomas. |
| **Editar** | Forma de onda, **deshacer/rehacer**, recortar, ganancia, normalizar, fundidos, **reducción de ruido**, puerta de ruido, quitar retumbe, **ecualizador de 3 bandas**, **compresor**, **eco**, **reverb**, análisis de tono y BPM, guardar WAV y **exportar M4A** para compartir. |
| **Estudio** | Proyectos **multipista**: volumen, paneo, mute/solo y desplazamiento por pista; mezcla estéreo, exportar WAV + M4A, “Grabar encima” de la mezcla. **Crear pista**: eliges tonalidad, estilo (pop, balada, rock, ranchera, cumbia, bachata, reggaetón, blues, jazz, electrónica, adoración), tempo y progresión, y genera **batería, bajo, acordes y melodía** como pistas separadas. Cuadrícula de batería editable. |
| **Tocar** | **Teclado multitáctil** con 14 instrumentos sintetizados (pianos, órgano, pad, cuerdas, metales, lead, flauta, guitarra, arpa, bajos, campanas, marimba) y **grabación de lo que tocas**. **Pads de batería** (11 piezas sintetizadas). **Sampler**: graba cualquier sonido, detecta su nota y lo toca afinado en cualquier tono (con bucle opcional); tus sonidos sirven en Crear pista. **Crear sonido**: editor de sintetizador (sustractiva, FM, órgano, cuerda pulsada) para guardar sonidos propios. |
| **Letras** | Acordes sobre la letra (`[Do]Hoy te [Sol]vi`), botones con los acordes del tono, **transponer**, **contador de sílabas** (gramaticales y métricas), **buscador de rimas** consonantes/asonantes, **dictado por voz**, **notas de voz** por canción, generador de progresiones y **exportar a PDF**. |
| **Más** | **Afinador** cromático, **metrónomo** con tap tempo, detector de **tonalidad y BPM**, **entrenamiento de oído** (intervalos y acordes), teoría con **piano y diapasón de guitarra** interactivos, guía de grabación paso a paso, calibración de latencia y **respaldo local (.zip)**. |

## Cómo abrirlo
1. Instala **Android Studio** (Ladybug 2024.2 o más reciente).
2. *File → Open* → carpeta `EstudioMusical`. Acepta descargar Gradle 8.9 y las dependencias (solo la primera vez).
3. Activa *Depuración USB* en tu teléfono, conéctalo y pulsa ▶ **Run**.

Requisitos: Android 8.0 (API 26) o superior.

## Privacidad
- Sin permisos de internet: la app no puede enviar nada a ningún servidor.
- `allowBackup=false`: Android tampoco sube los datos a la nube.
- El respaldo es un `.zip` que tú guardas donde quieras (memoria, USB, computadora).
- El dictado usa el reconocedor de voz del teléfono; descarga el paquete de Español en Ajustes de Android para usarlo sin conexión.

## Estructura
```
audio/        grabador, motor en tiempo real, metrónomo, afinador, WAV, exportar M4A
audio/dsp/    FFT, YIN (altura), BPM, filtros/EQ, compresor, reverb, reducción de ruido
audio/synth/  osciladores, envolventes, instrumentos, batería, sampler
music/        teoría (escalas, grados, acordes), generador de pistas, mezclador, sílabas y rimas
data/         letras, proyectos, muestras, sonidos, ajustes, PDF y respaldo
ui/           pantallas Compose
```

## Notas
- Bluetooth graba con el perfil de llamada (máx. 16 kHz). Para calidad real, USB tipo C.
- Para grabar encima de una base usa audífonos y calibra la latencia una vez (Más → Ajustes).
- MP3 no viene en Android; se exporta M4A (AAC), que suena igual o mejor con menor tamaño y lo abre cualquier teléfono.
