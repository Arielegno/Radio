package com.hilo.lector.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hilo.lector.HiloViewModel
import com.hilo.lector.Screen

private val SECTIONS = listOf(
    "Tu biblioteca" to listOf(
        "Se muestra en lista, como ReadEra: portada, formato, tamaño y avance. Debajo de cada libro hay botones rápidos: favorito, por leer, leído, colecciones y más opciones.",
        "¿Prefieres ver solo las portadas? Ajustes → Vista de la biblioteca → Portadas.",
        "Para elegir varios libros, mantén el dedo sobre uno y toca los demás. Abajo aparecen Compartir, Favoritos, Colección, Leídos y Papelera. «Todos» los marca a todos.",
        "Menú → Carpetas: apaga las carpetas que no quieres que Hilo lea (por ejemplo, WhatsApp). Sus documentos salen de Hilo, pero no se borran del celular.",
        "En Nuevos puedes tocar «Marcar todos como vistos».",
        "Menú → Novedades: lo que cambió en cada versión.",
    ),
    "Dos formas de leer" to listOf(
        "Página: el PDF tal como es, con su formato, imágenes y tablas. Los colores y la regla se pintan encima de cada línea.",
        "Texto: el libro como la página de un libro impreso, justificado, con guiones y sangría. Sirve para EPUB, MOBI, Word, textos pegados y PDF cuando el formato estorba.",
        "En Ajustes (Aa) eliges la fuente, el tamaño, el grosor, el interlineado, la alineación y los márgenes. «Forma del texto: Separado» vuelve a los párrafos con espacio entre ellos.",
        "En PDF, «Recortar márgenes» quita el borde blanco de la página para que la letra se vea más grande.",
        "Cambia entre las dos con el botón Página / Texto arriba.",
    ),
    "Estudiar con tus documentos" to listOf(
        "Mientras lees, mantén el dedo sobre una palabra o frase. Se abre una hoja con esa oración: toca las palabras clave.",
        "Completar: crea una tarjeta que oculta esas palabras. Ideal para definiciones, autores, fechas y artículos de una ley.",
        "Vocabulario: para idiomas. Escribe el significado; la tarjeta guarda la oración donde la viste y la puede leer en voz alta.",
        "Pregunta: escribe tu propia pregunta, por ejemplo «¿Qué es la socialización primaria?».",
        "Diccionario, Wikipedia y Traducir: busca la palabra que tocaste (o las que elegiste) en la RAE, en Wikipedia o en el traductor, en el idioma del documento.",
        "Resaltar: marca la oración. Tus resaltados quedan en la sala de estudio del documento y puedes compartirlos como resumen.",
        "El botón del birrete (arriba) abre la sala de estudio: tarjetas, resaltados y apuntes de ese documento.",
        "Repasa cada día con rondas cortas de 15 (o solo 5). Si respondes bien, la tarjeta tarda más en volver; si fallas, vuelve pronto.",
        "En Contenido → Buscar encuentras cualquier palabra; en Artículos, los artículos de leyes y códigos.",
    ),
    "Para Derecho" to listOf(
        "Si el documento es una ley o un código, la sala de estudio muestra la sección Ley: el índice por libros, títulos y capítulos, y un buscador «Ir al artículo».",
        "Al abrir un artículo ves a qué artículos remite y cuáles lo mencionan. Tócalos para saltar de uno a otro sin perder tu lugar.",
        "Mientras lees, si mantienes el dedo sobre una oración que dice «conforme al artículo 12», aparece «Ver art. 12».",
        "Tarjetas de artículos: «¿Qué establece el artículo 45?». Créalas de a una, por capítulo o de toda la ley.",
        "Plazos: una tabla con todos los plazos del texto («dentro de tres días hábiles», «prescribe a los diez años») y su artículo. Pásalos a tarjetas para memorizarlos.",
        "Fichas de caso: hechos, problema jurídico, norma aplicable, argumentos y fallo. Puedes llenar los campos con tus resaltados.",
        "Jurídico: busca la palabra en el Diccionario panhispánico del español jurídico de la RAE (latinismos, términos técnicos).",
    ),
    "Para Sociología y tus trabajos" to listOf(
        "Autores: Hilo reconoce a Marx, Weber, Durkheim, Bourdieu y muchos más, y junta los conceptos que se tratan junto a cada uno.",
        "Marca dos o tres autores y toca Comparar: un cuadro con lo que dice cada uno sobre los mismos conceptos.",
        "Línea de tiempo: las fechas y siglos del texto, en orden.",
        "Fichas de lectura y de autor: tesis, argumentos, conceptos, citas y tu crítica. Se comparten como texto.",
        "Citar: mantén el dedo sobre el texto y toca Citar. En la sección Citas tienes la referencia APA del libro y cada cita con su página, listas para copiar.",
        "En Estudio → Mapa entre tus documentos ves qué conceptos comparten tus textos.",
    ),
    "Repasar mejor" to listOf(
        "Fecha de examen: ponla en la sala de estudio del documento. Hilo reparte las partes que te faltan entre los días que quedan y hace que tus tarjetas vuelvan antes del examen.",
        "Recordatorio diario: en Estudio, actívalo y elige la hora. Te avisa si tienes tarjetas o un examen cerca.",
        "Examen oral: en el repaso toca el micrófono. Hilo lee la pregunta en voz alta, respondes hablando y te dice qué ideas clave te faltaron.",
        "Explícalo (modo Feynman): explica un concepto o un capítulo con tus palabras, escribiendo o hablando. Hilo te muestra qué ideas usaste y cuáles te faltaron.",
        "Exportar a Anki: crea un archivo con tus tarjetas (todas o las de un documento). En Anki: Archivo → Importar.",
        "Copia de seguridad: guarda tus tarjetas, resaltados, fichas, citas y avance en un archivo .zip, y restáuralo en otro celular.",
    ),
    "Pasar páginas como en un libro" to listOf(
        "En Ajustes (Aa) → Pasar páginas, elige Horizontal. La página se desliza sobre la siguiente, en PDF y también en EPUB, Word y los demás formatos.",
        "Toca el borde derecho para avanzar, el izquierdo para volver y el centro para mostrar u ocultar los menús.",
        "Con el celular acostado y «Dos páginas» activado, se ven dos páginas como un libro abierto, en PDF y en los demás formatos.",
        "Abajo está la barra de páginas: arrastra el punto o toca «Pág. 27 de 237» para saltar.",
    ),
    "Modos de color" to listOf(
        "Día, Noche, Noche contraste, Sepia, Sepia contraste, Crepúsculo y Consola. Cambian el papel y la letra, también en los PDF.",
        "Toca la esquina superior izquierda de la página para pasar de día a noche y al revés.",
    ),
    "Marcadores, índice y voz" to listOf(
        "Toca la esquina superior derecha o el marcador de abajo para guardar la página.",
        "El botón de marcadores abre el índice del libro, tus marcadores y tus notas.",
        "El botón de la voz lee en voz alta desde donde estás y la página avanza sola.",
        "Si activas «Brillo deslizando», sube o baja el dedo por el borde izquierdo.",
    ),
    "Ayudas de lectura" to listOf(
        "Colores: cada línea pasa de un color a otro y la siguiente empieza en ese mismo color. El ojo encuentra el inicio de la línea sin saltársela.",
        "Regla: oscurece todo menos la línea que lees. Tócala para moverla o usa las flechas redondas.",
        "Negrita guía (modo Texto): resalta el inicio de cada palabra.",
        "Palabra a palabra (botón ▶): el texto aparece en un solo punto, a tu velocidad. Empieza donde está la regla.",
        "Prueba una ayuda a la vez. A mucha gente le sirve una sola, no todas juntas.",
    ),
    "Si pierdes el hilo entre líneas" to listOf(
        "En Ajustes (Aa) baja el Interlineado a Compacto y el Espacio entre párrafos.",
        "Sube un poco el Tamaño y los Márgenes para que las líneas sean más cortas.",
        "Gira el celular: en horizontal ves más texto por línea en el modo Página.",
    ),
    "Trabajar sobre el PDF" to listOf(
        "Toca el pincel arriba para anotar.",
        "Resaltar: arrastra sobre el texto y el resaltado se ajusta a las líneas, como al seleccionar.",
        "Lápiz para escribir o subrayar, Borrar para quitar trazos y Nota para dejar un comentario en un punto.",
        "Todo se guarda solo. Con Guardar PDF creas una copia con tus anotaciones para enviarla o abrirla en otra app.",
        "Pellizca con dos dedos para hacer zoom. Toca el número de página para saltar a otra.",
    ),
    "Agregar libros" to listOf(
        "Como ReadEra: toca Permitir en la biblioteca y Hilo encuentra solo los PDF, EPUB y Word de tu celular, sin copiarlos. Avisa cuando hay documentos nuevos.",
        "Agregar → Archivos: PDF, EPUB, MOBI y AZW3 (sin DRM), Word (DOCX), ODT, FB2, HTML, Markdown o TXT.",
        "Agregar → Buscar en una carpeta: elige, por ejemplo, Descargas. Hilo agrega lo que encuentre y revisa esa carpeta cada vez que abres la app.",
        "En el menú ☰ de la biblioteca: Leyendo ahora, Favoritos, Por leer, Leídos, Autores, Colecciones, Formatos y Papelera.",
        "Agregar → Fotos de páginas: elige varias en orden. Se unen en un PDF y Hilo reconoce su texto sin internet.",
        "Agregar → Pegar texto: para artículos, apuntes o texto copiado de un PDF. Las líneas cortadas se unen solas.",
        "Desde otra app: Compartir → Hilo, o Abrir con → Hilo.",
        "Los libros con DRM (Kindle, Google Play Libros) no se pueden abrir.",
    ),
    "Temporizador" to listOf(
        "Toca el reloj y elige 10, 15, 25 o 45 minutos. Cuando suene, descansa 5 minutos. Bloques cortos rinden más que una sesión larga que se abandona.",
    ),
    "Dónde se guarda todo" to listOf(
        "Tus libros, tu avance y tus anotaciones se guardan solo en este celular. Si desinstalas Hilo se borran: guarda tus archivos originales.",
    ),
)

@Composable
fun GuideScreen(vm: HiloViewModel) {
    val c = LocalHilo.current
    BackHandler { vm.screen = Screen.Library }
    Box(Modifier.fillMaxSize().background(c.bg).windowInsetsPadding(WindowInsets.safeDrawing), contentAlignment = Alignment.TopCenter) {
        Column(Modifier.widthIn(max = 720.dp).fillMaxSize()) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(4.dp)) {
                IconButton(onClick = { vm.screen = Screen.Library }) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Volver", tint = c.ink) }
                Text("Cómo usar Hilo", style = MaterialTheme.typography.titleLarge, color = c.ink)
            }
            Column(
                Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                SECTIONS.forEach { (title, items) ->
                    Column(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(c.surface).padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(title, color = c.accent, fontFamily = Fonts.display, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                        items.forEach { line ->
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("•", color = c.muted, fontSize = 15.sp)
                                Text(line, color = c.ink, fontSize = 15.sp, lineHeight = 21.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
