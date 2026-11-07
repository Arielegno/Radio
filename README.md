# Radio de ruta

Una radio personal que corre en el navegador (PWA). Reproduce tu música y, entre canción y canción, una locutora con IA (por defecto «Luna») comenta lo que suena: datos del artista, el clima, la zona por la que viajas, efemérides y más.

## Qué hace

- **Dos fuentes de música:** tu propia biblioteca local (archivos de audio guardados en el navegador) o tu cuenta de Spotify (requiere Premium para controlar la reproducción).
- **Locutora con IA:** escribe intervenciones con Gemini o Claude y las lee con voz en la nube, voz local o la del teléfono.
- **Contexto en vivo:** ubicación (opcional), clima, lugares cercanos y los artistas que más escuchas en Spotify.
- **Biblioteca:** artistas, álbumes, años, géneros, playlists y canciones, con carátulas y vista de artista con biografía. Se puede ordenar (por año, título, artista, más escuchadas…) y filtrar (década, género, calidad, favoritas); los álbumes de cada artista se ven en mosaico, carrusel o lista.
- **Sonido:** reproduce el archivo original sin comprimir. Ecualizador y nivelador de volumen opcionales (apagados por defecto) e información de calidad de cada archivo.
- **Favoritas y estadísticas:** canciones y artistas favoritos, y lo más escuchado por ubicación.
- **Instalable y sin conexión:** se instala como app y guarda una copia para abrir sin internet.
- **Revista de la comunidad:** artículos con estéticas de revista, perfiles, pósters (originales con licencia libre y de la comunidad), perfiles de artistas y bandas verificadas, debates, retos, agenda de conciertos con mapa, ediciones mensuales, notificaciones y moderación. Usa Supabase y se carga solo cuando la abres.
- **La bóveda del tiempo:** cartelera curada de conciertos y festivales históricos dentro de la Revista. Cada concierto se queda 30 días y rota; cada concierto tiene sinopsis obligatoria; hay concierto del día, ciclos, lista de canciones con saltos al minuto, nota del curador y comentarios. Los videos se ven con el reproductor oficial de YouTube, Vimeo o Internet Archive (nunca se descargan) y siempre se muestra la fuente. La vista de artista avisa cuando su concierto o un festival de su biografía está en cartelera, y la locutora puede recomendarlo.

## Archivos

| Archivo | Para qué sirve |
|---|---|
| `index.html` | Toda la app (interfaz, estilos y lógica) |
| `revista.js` | La Revista (se descarga solo al abrirla) |
| `sw.js` | Service worker: caché y modo sin conexión |
| `revista_supabase_completo.sql` | Base de datos de la Revista (se ejecuta en Supabase, no hace falta subirlo) |
| `legal.html` | Aviso legal, descargo de responsabilidad y privacidad (se abre desde la app) |
| `AVISO_LEGAL.md` | El mismo aviso legal, para leerlo en el repositorio |
| `CREDITS.md` | Créditos y licencias de terceros |
| `LICENSE` | Licencia del proyecto (PolyForm Noncommercial 1.0.0) |
| `_headers` | Cabeceras de seguridad (solo se usan si publicas en Netlify o Cloudflare Pages; GitHub Pages las ignora) |
| `manifest.json` | Datos para instalarla como app |
| `icon-192.png`, `icon-512.png`, `logo.png` | Iconos y logo (nombres exactos, en minúsculas) |

## Puesta en marcha

1. Sube estos archivos a un hosting estático con HTTPS (por ejemplo GitHub Pages).
2. Abre la página desde el navegador del teléfono y, si quieres, instálala desde el menú.
3. En **Ajustes** configura tus claves:
   - **Gemini o Claude** para la locutora.
   - **Spotify**, si quieres usarlo como fuente: crea una app en el panel de desarrolladores de Spotify, añade la URL de tu página como *Redirect URI* y pega tu *Client ID*.
4. Las claves se guardan solo en tu dispositivo. **Nunca las escribas dentro del código ni las subas al repositorio.** (La única excepción es la clave *pública* «publishable/anon» de Supabase, que está hecha para ir en la app; la seguridad la dan las reglas de la base de datos. Nunca uses la clave *secret* ni *service_role*.)

### Revista (Supabase)

1. En supabase.com crea un proyecto. En **SQL Editor › New query** pega todo `revista_supabase_completo.sql` y toca **Run**.
2. En **Authentication › Sign In / Providers › Email** apaga «Confirm email» (o déjalo, y cada persona confirmará su correo).
3. En **Authentication › URL Configuration** pon como **Site URL** la dirección de tu página (para que funcione «Olvidé mi contraseña»).
4. En `index.html` pon la URL del proyecto y la clave *publishable* en `REV_URL` y `REV_KEY`, y la dirección de tu página en `REV_HOME` (o, sin tocar el código, pégalas en la app: Ajustes › Revista).
5. Crea tu cuenta en la Revista y hazte administrador con la línea del final del archivo SQL.
6. **Curadores de la Bóveda:** el administrador siempre es curador y puede todo. En **Admin › Bóveda** gestionas la cartelera completa (editar, ocultar, borrar), decides quién es curador y qué puede hacer cada uno (programar, editar, borrar) y ves el **historial de cambios**, donde puedes deshacer una edición o recuperar un concierto borrado. Solo la cuenta de administrador puede cambiar permisos: lo comprueba la base de datos.
7. Para borrar la cuenta de alguien que lo pida: Supabase › Authentication › Users › eliminar. Se borra también su contenido.

### Cada revista es de su dueño

La revista de la página original de Radio de ruta es privada de ese proyecto. Si copias este código, eres libre de usarlo y modificarlo según la licencia, pero **crea tu propia revista**:

- La app solo usa la revista original en la dirección de la página original (`REV_HOME` en `index.html`). En cualquier otra dirección, la Revista pide un proyecto de Supabase propio.
- Además, la base de datos original solo acepta publicaciones hechas desde su página (tabla `rr_origins` del SQL). Una copia puede leer lo público, pero no escribir en ella.
- Para tu revista: crea tu proyecto en Supabase, ejecuta `revista_supabase_completo.sql` y, en la parte del candado, cambia `https://arielegno.github.io` por la dirección de tu página.

## Actualizar la app

Cada vez que cambies `index.html` o `revista.js`, sube el número de `CACHE` en `sw.js` (por ejemplo `rr59` → `rr60`). Si cambias `revista.js`, pon el mismo número en `REV_VER` dentro de `index.html`. Sube los archivos en **un solo commit** para que GitHub Pages publique una sola vez. Si sigues viendo la versión vieja, cierra la app y vuelve a abrirla.

## Música y contenido de terceros

La app no incluye música. Cada persona usa sus propios archivos o su cuenta de Spotify. **No subas música ni carátulas con derechos de autor a un repositorio público.**

## Licencia

Copyright (c) 2026 Arielegno (seudónimo del autor).

Este proyecto usa la **PolyForm Noncommercial License 1.0.0** (ver `LICENSE`). En palabras simples:

- Puedes **usarlo, copiarlo, modificarlo y compartirlo** gratis para cualquier fin **no comercial** (personal, estudio, hobby, educación, organizaciones sin fines de lucro).
- **No puedes comercializarlo** ni usarlo para ganar dinero (venderlo, incluirlo en un producto o servicio de pago, usarlo en un negocio) sin una licencia comercial aparte.
- Si lo compartes o publicas tu versión modificada, debes incluir el archivo `LICENSE` y el aviso `Required Notice`.
- El autor conserva el derecho de explotarlo comercialmente y de otorgar licencias comerciales a quien las pida.

Esta licencia es de *código disponible*: no se considera "software libre u open source" según la definición de la OSI, porque restringe el uso comercial.

Componentes de terceros: ver `CREDITS.md`. Aviso legal, descargo de responsabilidad y privacidad: ver `AVISO_LEGAL.md`.

**Al volante:** no manipules la app mientras conduces; configúrala antes de salir.
