# Radio de ruta

Una radio personal que corre en el navegador (PWA). Reproduce tu música y, entre canción y canción, una locutora con IA (por defecto «Luna») comenta lo que suena: datos del artista, el clima, la zona por la que viajas, efemérides y más.

## Qué hace

- **Dos fuentes de música:** tu propia biblioteca local (archivos de audio guardados en el navegador) o tu cuenta de Spotify (requiere Premium para controlar la reproducción).
- **Locutora con IA:** escribe intervenciones con Gemini o Claude y las lee con voz en la nube, voz local o la del teléfono.
- **Contexto en vivo:** ubicación (opcional), clima, lugares cercanos y los artistas que más escuchas en Spotify.
- **Biblioteca:** artistas, álbumes, años, géneros, playlists y canciones, con carátulas y vista de artista con biografía.
- **Favoritas y estadísticas:** canciones y artistas favoritos, y lo más escuchado por ubicación.
- **Instalable y sin conexión:** se instala como app y guarda una copia para abrir sin internet.

## Archivos

| Archivo | Para qué sirve |
|---|---|
| `index.html` | Toda la app (interfaz, estilos y lógica) |
| `sw.js` | Service worker: caché y modo sin conexión |
| `manifest.json` | Datos para instalarla como app |
| `icon-192.png`, `icon-512.png`, `logo.png` | Iconos y logo (nombres exactos, en minúsculas) |

## Puesta en marcha

1. Sube estos archivos a un hosting estático con HTTPS (por ejemplo GitHub Pages).
2. Abre la página desde el navegador del teléfono y, si quieres, instálala desde el menú.
3. En **Ajustes** configura tus claves:
   - **Gemini o Claude** para la locutora.
   - **Spotify**, si quieres usarlo como fuente: crea una app en el panel de desarrolladores de Spotify, añade la URL de tu página como *Redirect URI* y pega tu *Client ID*.
4. Las claves se guardan solo en tu dispositivo. **Nunca las escribas dentro del código ni las subas al repositorio.**

## Actualizar la app

Cada vez que cambies `index.html`, sube el número de `CACHE` en `sw.js` (por ejemplo `rr14` → `rr15`). Si sigues viendo la versión vieja, cierra la pestaña y vuelve a abrirla.

## Música y contenido de terceros

La app no incluye música. Cada persona usa sus propios archivos o su cuenta de Spotify. **No subas música ni carátulas con derechos de autor a un repositorio público.**

## Licencia

Copyright (c) 2026 Oscar Ariel Vallecillo Casalegno.

Este proyecto usa la **PolyForm Noncommercial License 1.0.0** (ver `LICENSE`). En palabras simples:

- Puedes **usarlo, copiarlo, modificarlo y compartirlo** gratis para cualquier fin **no comercial** (personal, estudio, hobby, educación, organizaciones sin fines de lucro).
- **No puedes comercializarlo** ni usarlo para ganar dinero (venderlo, incluirlo en un producto o servicio de pago, usarlo en un negocio) sin una licencia comercial aparte.
- Si lo compartes o publicas tu versión modificada, debes incluir el archivo `LICENSE` y el aviso `Required Notice`.
- El autor conserva el derecho de explotarlo comercialmente y de otorgar licencias comerciales a quien las pida.

Esta licencia es de *código disponible*: no se considera "software libre u open source" según la definición de la OSI, porque restringe el uso comercial.

Componentes de terceros: ver `CREDITS.md`.
