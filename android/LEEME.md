# Radio de ruta para Android (Kotlin)

Versión nativa de Radio de ruta escrita en **Kotlin** con **Jetpack Compose** y **Media3**.

## Qué trae

- **Radio:** tu biblioteca con Luna entre canciones (Gemini gratis con tu clave, o frases sencillas sin clave), voz del teléfono, modos de escucha, programas de Luna («La hora del recuerdo» y «El resumen del domingo»), dedicatoria, efemérides propias, fundido, temporizador para dormir, orden aleatorio y repetir.
- **Reproducción en segundo plano** con controles en la notificación y en la pantalla bloqueada.
- **Portada personalizable:** saludo, clima y zona, vinilo que gira o carátula cuadrada con color de fondo, «Seguir escuchando», modos con un toque, Luna con cara, tarjetas «Para ti», «Hoy en la música» (con el concierto del día de la Bóveda), «Tu mes» y «Club de escucha».
- **Biblioteca:** agrega archivos de audio (se copian a la app y suenan sin internet), lee etiquetas y carátulas, busca, ordena, vista por artistas y álbumes, edita datos.
- **Copia de tu música (.rrlib):** el mismo formato de la versión web: una copia hecha en la web se restaura en la app y al revés.
- **Modo carro** con botones grandes, pantalla encendida y comandos de voz («siguiente», «pausa», «pon Soda Stereo», «solo música», «Luna»).
- **Estadísticas:** favoritas, más escuchadas, «Tu resumen» y el mapa de tus viajes musicales.
- **Revista:** se abre tu Revista web dentro de la app (con tu sesión, subida de imágenes y enlaces).
- Tema claro, oscuro o automático. Recorrido de bienvenida.

## Cómo se compila

No hace falta Android Studio: GitHub compila la app solo con **GitHub Actions** (`.github/workflows/android.yml`).
Cada vez que cambia algo en `android/`, se ejecutan las pruebas, se compila el APK y se publica en **Releases › Radio de ruta para Android**.
Desde tu celular Android abre esa página, descarga `radio-de-ruta.apk` e instálala (Android te pedirá permitir «instalar apps desconocidas» para tu navegador).

La firma (`app/firma-debug.keystore`) es fija para que cada versión nueva se instale encima de la anterior sin borrar tu música. Sirve para instalarla tú; para publicarla en Google Play se usaría otra firma.

## Pendiente para próximas versiones

- La Revista nativa (por ahora es la web dentro de la app).
- Spotify (necesita el SDK de Spotify y registrar la app).
- Dedicatorias del muro leídas por Luna y «Escuchando ahora» desde la app.
- Voces en la nube y la «nube» de datos de artistas de la versión web.
