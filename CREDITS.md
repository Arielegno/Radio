# Créditos y avisos de terceros

Radio de ruta usa los siguientes componentes y servicios. Cada uno pertenece a sus autores y se rige por su propia licencia o términos.

## Iconos

Los iconos de la interfaz están basados en **Lucide** (https://lucide.dev), con licencia ISC:

> Copyright (c) for portions of Lucide are held by Cole Bemis 2013-2022 as part of Feather (MIT). All other copyright (c) for Lucide are held by Lucide Contributors 2022.
>
> Permission to use, copy, modify, and/or distribute this software for any purpose with or without fee is hereby granted, provided that the above copyright notice and this permission notice appear in all copies.
>
> THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.

Feather: https://feathericons.com (MIT).

## Tipografías

- **Barlow** y **Barlow Condensed**, de Jeremy Tribby, bajo SIL Open Font License 1.1. Se cargan desde Google Fonts.
- Tipografías de las estéticas de la Revista, todas bajo **SIL Open Font License 1.1** y cargadas desde Google Fonts solo al abrir la Revista: **Playfair Display** (Claus Eggers Sørensen), **Lora** (Cyreal), **Space Mono** (Colophon Foundry), **Caveat** (Impallari Type), **Abril Fatface** (TypeTogether), **Anton** (Vernon Adams), **Shrikhand** (Jonny Pinhorn), **Rampart One** (Fontworks), **Rubik Glitch** y **Archivo Black** (Omnibus-Type).

## Librerías

- **jsmediatags** (MIT), para leer las etiquetas y carátulas de los archivos de audio.
- **Sherpa-ONNX** (Apache 2.0), motor de voz local en el navegador, si activas esa opción. Los modelos de voz que configures (por ejemplo Kokoro o Piper) tienen su propia licencia: revísala antes de usarlos.
- **Leaflet** 1.9.4 (BSD 2-Clause, © Volodymyr Agafonkin y colaboradores), para el mapa de la Agenda. Se descarga de jsDelivr solo al abrir el mapa.

Las librerías se sirven desde **jsDelivr** (CDN público) y las tipografías desde **Google Fonts**; ambos servicios reciben la dirección IP de quien abre la app al descargar esos archivos.

## Servicios y datos externos

La app se conecta a estos servicios según lo que actives en Ajustes. Revisa sus términos antes de publicar la app para otras personas.

- **Spotify Web API:** música, artistas favoritos y "Me gusta". Spotify es una marca de Spotify AB; este proyecto no está afiliado ni respaldado por Spotify.
- **Google Gemini API** y **Anthropic (Claude) API:** generación de textos y voz de la locutora.
- **MusicBrainz** (datos bajo CC0) y **TheAudioDB:** datos y biografías de artistas.
- **Apple iTunes Search API:** búsqueda de datos y carátulas de canciones y álbumes. Apple e iTunes son marcas de Apple Inc.; este proyecto no está afiliado a Apple.
- **Cover Art Archive:** portadas de álbumes. Las portadas pertenecen a sus titulares de derechos.
- **Wikipedia en español:** resúmenes de artistas, festivales y lugares en las fichas de la biblioteca. Textos bajo **CC BY-SA 4.0**, con enlace a su artículo.
- **Bandcamp:** solo se enlaza a su buscador o a páginas de artistas; no se usa su contenido.
- **OpenStreetMap / Nominatim:** nombre de la zona y lugares cercanos. Datos © colaboradores de OpenStreetMap (ODbL).
- **Open-Meteo:** clima (https://open-meteo.com, CC BY 4.0).
- **YouTube, Vimeo e Internet Archive:** los conciertos de la Bóveda del tiempo se ven con el reproductor oficial que cada plataforma ofrece para insertar videos, tal como los publicaron sus dueños. La app no copia ni descarga videos; cada video es de quien lo publicó. YouTube es una marca de Google LLC y Vimeo de Vimeo.com, Inc.; este proyecto no está afiliado a ellas.
- **noembed.com:** comprueba, si hace falta, el título y el canal de un video de YouTube al programarlo.

### Revista

- **Supabase:** base de datos, cuentas y almacén de imágenes de la Revista.
- **Mapa de la Agenda:** mosaicos de **OpenStreetMap** — © colaboradores de OpenStreetMap (ODbL). La ubicación de los eventos se obtiene con **Nominatim**.
- **Wikimedia Commons:** pósters históricos con licencia libre o de dominio público. Cada póster muestra su autor, su licencia y el enlace a su ficha; la licencia de cada imagen es la que indica Commons.
- **Wikipedia en español** («Hoy en la música», vía la API de Wikimedia): textos bajo **CC BY-SA 4.0**, con su fuente indicada en la app.
- **Internet Archive:** solo se enlazan elementos con licencia Creative Commons o de dominio público; no se copian.
- **MusicBrainz** (CC0) y **Cover Art Archive:** discografías y portadas de álbumes. Las portadas pertenecen a sus titulares de derechos.
- **Búsqueda web de pósters:** la hace la IA de cada usuario (Gemini con Búsqueda de Google) con su propia clave. La app solo enlaza a la página de origen y muestra la fuente; nunca copia la imagen.
- **YouTube:** los videos dentro de los artículos usan el reproductor de youtube-nocookie.com y solo cargan al tocarlos.
- **Contenido de la comunidad:** los artículos, comentarios y pósters subidos por usuarios pertenecen a sus autores, que confirman al subirlos que son suyos o que tienen permiso para compartirlos.

## Aviso legal

El descargo de responsabilidad, el aviso de privacidad y cómo pedir que se retire un contenido están en `AVISO_LEGAL.md` (en la app: Guía de uso › Aviso legal y privacidad, o `legal.html`).

## Contenido propio

El código de Radio de ruta es © 2026 Arielegno (seudónimo del autor), bajo PolyForm Noncommercial 1.0.0 (ver `LICENSE`). El logo, los iconos de la app (`icon-192.png`, `icon-512.png`, `logo.png`) y los textos son del autor del proyecto, salvo que se indique lo contrario.
