package com.hilo.lector.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.AccountTree
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.BorderColor
import androidx.compose.material.icons.rounded.Brush
import androidx.compose.material.icons.rounded.CenterFocusStrong
import androidx.compose.material.icons.rounded.Collections
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.FormatQuote
import androidx.compose.material.icons.rounded.Gavel
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.Quiz
import androidx.compose.material.icons.rounded.Route
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Style
import androidx.compose.material.icons.rounded.Swipe
import androidx.compose.material.icons.rounded.TextFields
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hilo.lector.HiloViewModel
import com.hilo.lector.Screen
import java.text.Normalizer

/** Un tema de la guía: para qué sirve, cómo se usa paso a paso y consejos. */
internal class Topic(
    val group: String,
    val icon: ImageVector,
    val title: String,
    val what: String,
    val steps: List<String>,
    val tips: List<String> = emptyList(),
    val key: String = "",
)

internal val GROUPS = listOf("Empezar", "Ayudas TDAH", "Leer", "Elegir texto y subrayar", "Estudiar", "Derecho", "Sociología", "Biblioteca", "Ajustes y ayudas")

private val BASE_TOPICS = listOf(
    // ---------------- Empezar ----------------
    Topic(
        "Empezar", Icons.Rounded.TouchApp, "Primeros pasos (léelo primero)",
        "Hilo es un lector pensado para que no pierdas el hilo al leer y para estudiar lo que lees. En cinco minutos ya lo dominas.",
        listOf(
            "Agrega tus libros: en la Biblioteca toca «Permitir» para que Hilo encuentre solo tus PDF, EPUB y Word, o toca el botón «+ Agregar».",
            "Abre un libro tocándolo. La primera vez verás una guía de gestos: tócala para seguir.",
            "Toca el centro de la página para mostrar u ocultar los menús. Toca el borde derecho para avanzar y el izquierdo para volver.",
            "Mantén el dedo sobre una palabra: queda elegida con dos manijas azules y aparece un menú pequeño (Copiar, Subrayar, Nota, Traducir, Diccionario, Más).",
            "Toca el birrete 🎓 arriba para entrar a la sala de estudio de ese documento: ahí están tu ruta de estudio, tus tarjetas, subrayados, mapa y más.",
            "Repasa cada día unos minutos en «Estudiar» (barra de abajo). Hilo te dice qué tarjetas tocan hoy.",
        ),
        listOf(
            "¿Te sientes perdido? Activa Ajustes → Modo simple: solo verás lo esencial.",
            "Puedes volver a elegir tu perfil de lectura (atención, dislexia, letra grande o estudio) en Ajustes → Perfil de lectura.",
        ),
    ),
    Topic(
        "Empezar", Icons.Rounded.Swipe, "Gestos de la página",
        "Todo se hace con toques sencillos, sin buscar botones.",
        listOf(
            "Toque en el centro: muestra u oculta los menús de arriba y de abajo.",
            "Toque en el borde derecho: página siguiente. Borde izquierdo: página anterior.",
            "Esquina de arriba a la izquierda: cambia entre día y noche.",
            "Esquina de arriba a la derecha: guarda un marcador en esa página.",
            "Mantener el dedo sobre el texto: elige una palabra (ver «Elegir texto»).",
            "Dos dedos (pellizcar): acercar o alejar en los PDF y en los cómics. Dos toques en un cómic: zoom rápido.",
        ),
        listOf("Si prefieres deslizar en vez de tocar los bordes, desactiva «Tocar los bordes para pasar» en Ajustes (Aa)."),
    ),

    // ---------------- Leer ----------------
    Topic(
        "Leer", Icons.AutoMirrored.Rounded.MenuBook, "Página original o Texto",
        "Los PDF se pueden leer tal cual son (Página) o convertidos en texto que se acomoda a tu pantalla (Texto).",
        listOf(
            "Arriba, en el lector, toca «Página» o «Texto».",
            "Página: el PDF como es, con sus imágenes y su diseño. Pellizca para hacer zoom.",
            "Texto: el contenido como la página de un libro impreso, justificado, con sangría y con la letra que elijas. Funciona en EPUB, Word, MOBI, textos pegados y PDF.",
            "En PDF, Ajustes → «Recortar márgenes» quita el borde blanco para que la letra se vea más grande.",
        ),
        listOf("Si un PDF es escaneado (una foto), activa «Reconocer texto en escaneados» en Ajustes para poder elegir y estudiar su texto."),
    ),
    Topic(
        "Leer", Icons.Rounded.TextFields, "Letra, márgenes y forma del texto",
        "Puedes dejar el texto a tu gusto, como en un libro de papel.",
        listOf(
            "Toca el botón «Aa» arriba en el lector.",
            "Elige la fuente (Atkinson y Lexend ayudan con la dislexia), el tamaño y el grosor.",
            "Ajusta el interlineado, el espacio entre párrafos, la alineación (justificado o a la izquierda) y los márgenes.",
            "«Forma del texto: Libro» pone sangría y quita el espacio entre párrafos; «Separado» deja aire entre ellos.",
            "En «Más ajustes» encuentras el resto: PDF, toques, vibración, modo enfoque y modo simple.",
        ),
    ),
    Topic(
        "Leer", Icons.Rounded.AutoStories, "Pasar páginas como en un libro",
        "Puedes leer deslizando hacia abajo (como una web) o pasando páginas hacia el lado.",
        listOf(
            "En Ajustes (Aa) → Pasar páginas, elige Vertical u Horizontal.",
            "En Horizontal la página se desliza sobre la siguiente, en PDF y en todos los formatos.",
            "Con el celular acostado y «Dos páginas» activado, ves dos páginas como un libro abierto.",
            "Abajo está la barra de avance: arrastra el punto o toca «Pág. 27 de 237» para saltar a otra página.",
        ),
    ),
    Topic(
        "Leer", Icons.Rounded.VolumeUp, "Marcadores, índice y lectura en voz alta",
        "Para volver a un lugar, moverte por capítulos o escuchar el libro.",
        listOf(
            "Marcador: toca la esquina de arriba a la derecha o el ícono de marcador abajo.",
            "El botón de marcadores (abajo a la izquierda) abre el índice del libro, tus marcadores y tus notas.",
            "El botón de la voz lee en voz alta desde donde estás; la página avanza sola.",
            "El botón ▶ redondo arriba muestra el texto palabra por palabra en un solo punto, a la velocidad que elijas.",
        ),
    ),
    Topic(
        "Leer", Icons.Rounded.Collections, "Cómics y mangas",
        "Hilo abre cómics en CBZ y CBR, una imagen por página.",
        listOf(
            "Agrégalos como cualquier libro.",
            "Toca los bordes para pasar de página; dos toques para acercar; pellizca para un zoom fino.",
            "Para mangas (de derecha a izquierda): en la Biblioteca, menú ⋮ del cómic → «Leer como manga».",
        ),
    ),

    // ---------------- Elegir texto ----------------
    Topic(
        "Elegir texto y subrayar", Icons.Rounded.TouchApp, "Elegir texto (selección)",
        "Como en ReadEra: eliges una palabra o una frase y decides qué hacer con ella.",
        listOf(
            "Mantén el dedo sobre una palabra hasta que se pinte de azul.",
            "Aparecen dos manijas (gotas azules) al inicio y al final. Arrástralas para elegir más o menos texto.",
            "Sobre el texto elegido aparece un menú pequeño. Si no te cabe arriba, sale abajo.",
            "Toca en cualquier otro lado de la página para soltar la selección.",
            "En «Más» puedes elegir toda la oración o todo el párrafo de una vez.",
        ),
        listOf(
            "En el texto fluido la selección queda dentro de un párrafo; para tomar varios párrafos usa «Más → Elegir todo el párrafo» y hazlo por partes.",
            "En la página original del PDF puedes elegir varias líneas seguidas. La posición de cada letra es aproximada: ajusta las manijas si hace falta.",
        ),
    ),
    Topic(
        "Elegir texto y subrayar", Icons.Rounded.BorderColor, "El menú del texto elegido",
        "Cada botón hace una sola cosa, sin abrir pantallas grandes.",
        listOf(
            "Copiar: lo copia para pegarlo en otra app.",
            "Subrayar: muestra los colores. «Resaltar» es el amarillo común; los demás son tipos de idea (ver «Subrayar con colores»).",
            "Nota: escribe lo que piensas. Se guarda en tus Apuntes del documento (en PDF, queda pegada en la página) y el texto queda resaltado.",
            "Traducir y Diccionario: abren el traductor o la RAE con lo que elegiste.",
            "Más → Estudiar: abre el panel completo para crear tarjetas de repaso (ver «Estudiar un fragmento»).",
            "Más → Guardar como cita (APA), Buscar en Wikipedia, Buscar en internet y Compartir.",
        ),
    ),
    Topic(
        "Elegir texto y subrayar", Icons.Rounded.Style, "Subrayar con colores (lectura activa)",
        "Subrayar por tipo de idea te obliga a pensar mientras lees, y después puedes repasar solo las definiciones o solo las tesis.",
        listOf(
            "Elige el texto y toca Subrayar.",
            "Azul = Definición: qué significa un concepto.",
            "Amarillo = Tesis: la idea principal que defiende el autor.",
            "Verde = Ejemplo: un caso que ilustra la idea.",
            "Rosa = Crítica: una objeción o un límite.",
            "Morado = Dato: cifras, fechas, nombres.",
            "Naranja = Duda: algo que no entendiste. Las dudas aparecen en tu ruta de estudio para resolverlas después.",
            "Para borrar un subrayado: sala de estudio → Resaltados. Si te equivocas, toca «Deshacer» en el aviso de abajo.",
        ),
        listOf("No subrayes todo: una o dos ideas por página bastan. Si todo es importante, nada lo es."),
    ),

    // ---------------- Estudiar ----------------
    Topic(
        "Estudiar", Icons.Rounded.School, "Estudiar un fragmento (crear tarjetas)",
        "Convierte lo que lees en preguntas para repasar. Es lo que más ayuda a recordar.",
        listOf(
            "Elige el texto y toca Más → Estudiar.",
            "Arriba ves la oración completa. Toca las palabras clave (las que vienen elegidas ya están marcadas).",
            "Completar: oculta esas palabras y tú debes recordarlas. Ideal para definiciones, autores, fechas y artículos.",
            "Pregunta: escribe tu propia pregunta y su respuesta, por ejemplo «¿Qué es la anomia?».",
            "Vocabulario: para idiomas, la palabra y su significado.",
            "Explícalo: escribe o di con tus palabras qué significa; Hilo te muestra qué ideas usaste y cuáles te faltaron.",
            "Más abajo puedes subrayar con su tipo, buscar el término (Diccionario, Wikipedia, Jurídico, Traducir) y guardar como cita o en tus apuntes.",
        ),
    ),
    Topic(
        "Estudiar", Icons.Rounded.Route, "La ruta de estudio (6 fases)",
        "Una guía de qué hacer antes, durante y después de leer, basada en cómo aprende el cerebro. Es la primera pestaña de la sala de estudio.",
        listOf(
            "Abre la sala de estudio (birrete 🎓) y toca «Ruta».",
            "1. Activar: haz la prueba previa aunque falles. Así el cerebro sabe qué buscar.",
            "2. Leer activo: subraya con colores y marca tus dudas.",
            "3. Consolidar: notas Cornell, resumen con tus palabras y tu propio mapa.",
            "4. Recuperar: recuerdo libre (escribe todo lo que recuerdes sin mirar), tarjetas y preguntas por niveles (Bloom).",
            "5. Aplicar: usa lo aprendido en un ejemplo o un caso.",
            "6. Repasar y cerrar: repaso espaciado, simulacro de examen y cierre del día.",
            "El semáforo te dice qué fase toca. Marca cada fase como hecha cuando la termines.",
        ),
        listOf("No hace falta hacer todo el mismo día: una fase por sesión de 15 minutos está muy bien."),
    ),
    Topic(
        "Estudiar", Icons.Rounded.Quiz, "Repasar las tarjetas",
        "Hilo programa cada tarjeta para mostrártela justo antes de que la olvides (método FSRS, el del Anki moderno).",
        listOf(
            "Toca «Estudiar» en la barra de abajo o «Repasar» en la sala de estudio.",
            "Lee la pregunta y piensa la respuesta.",
            "Antes de verla, di qué tan seguro estás: «Lo sé», «Dudo» o «No lo sé».",
            "Mira la respuesta y elige: Otra vez, Difícil, Bien o Fácil. Debajo de cada botón ves cuándo volverá.",
            "Las tarjetas que te cuestan mucho se marcan como frágiles y salen primero.",
            "Las tarjetas de varios temas se mezclan: así aprendes a distinguirlos.",
        ),
        listOf(
            "Es mejor repasar 10 minutos cada día que dos horas un solo día.",
            "Examen oral: en el repaso toca el micrófono, responde hablando y Hilo te dice qué ideas clave te faltaron.",
        ),
    ),
    Topic(
        "Estudiar", Icons.Rounded.Insights, "Estadísticas y simulacro",
        "Para saber cuánto recuerdas de verdad y practicar como en el examen.",
        listOf(
            "En Estudiar toca «Estadísticas»: ves repasos por día, aciertos, retención por documento y tu calibración.",
            "Calibración: compara lo seguro que dijiste estar con lo que acertaste. Si dices «Lo sé» y fallas mucho, te sobreestimas.",
            "Toca «Simulacro»: un examen con tiempo, mezclando preguntas. Al final ves tu nota y las tarjetas falladas vuelven pronto.",
        ),
    ),
    Topic(
        "Estudiar", Icons.Rounded.Map, "Mapa de conceptos y frecuencias",
        "Muestra de qué trata un documento, como los programas de análisis cualitativo (MAXQDA, ATLAS.ti).",
        listOf(
            "Sala de estudio → Mapa.",
            "Cada círculo es una palabra importante del documento; el número dentro es cuántas veces aparece.",
            "Las líneas unen palabras que salen en la misma oración. Más gruesa = más veces juntas.",
            "Toca un círculo para ver con qué se relaciona, el porcentaje del texto y oraciones donde aparece.",
            "Si una palabra no es un concepto, tócala y elige «Quitar del mapa».",
            "Debajo está la tabla de frecuencias con las veces y el porcentaje de cada palabra.",
            "En Estudiar → «Mapa entre tus documentos» ves qué conceptos comparten varios textos.",
        ),
        listOf("Se cuentan juntos el singular y el plural. No se cuentan artículos, conectores ni palabras como «precisamente»."),
    ),
    Topic(
        "Estudiar", Icons.Rounded.Event, "Fecha de examen y recordatorio",
        "Hilo reparte lo que te falta entre los días que quedan.",
        listOf(
            "En la sala de estudio toca «¿Tienes examen? Pon la fecha y te armo un plan».",
            "Elige la fecha. Hilo divide el documento en partes por día y adelanta tus tarjetas para que lleguen frescas.",
            "En Estudiar activa el «Recordatorio diario» y elige la hora. Te avisa si tienes tarjetas o un examen cerca.",
        ),
    ),
    Topic(
        "Estudiar", Icons.Rounded.Insights, "Burbujas de palabras",
        "Las palabras que más se repiten, como burbujas: cuanto más grande, más veces aparece. Sirve para ver de un vistazo de qué trata un texto.",
        listOf(
            "Sala de estudio → Palabras.",
            "Hilo cuenta todas las palabras, junta singular y plural, y deja aparte las comunes (el, la, de, que, ser, haber…) porque no dicen de qué trata el texto.",
            "Arriba eliges cuántas burbujas ver (25, 45 u 80). «Comunes» las incluye.",
            "Abajo está la lista de TODAS las palabras con su número. El ojo encendido = está en las burbujas; tócalo para ponerla o sacarla.",
            "Toca una burbuja: «En contexto» muestra cada aparición con lo que va antes y después; «Va junto a…» las palabras de la misma oración; «En el libro» dónde se concentra.",
            "«Unir con…» junta variantes (trabajo, trabajador) para que cuenten como una sola. «Ocultar» la saca de las burbujas.",
            "«Exportar» comparte la lista con veces y porcentaje para usarla en Excel o en tu trabajo.",
        ),
        listOf("Para análisis de entrevistas o documentos: une las variantes, oculta lo que no es concepto y mira «En contexto» antes de interpretar una palabra."),
    ),
    Topic(
        "Estudiar", Icons.Rounded.Psychology, "Más herramientas de estudio",
        "Cada pestaña de la sala de estudio sirve para algo distinto.",
        listOf(
            "Guía: preguntas sobre cada parte del documento para leer con un propósito.",
            "Palabras: las palabras más repetidas en burbujas; toca una para ver cuántas veces aparece.",
            "Glosario: definiciones que Hilo encontró en el texto («La anomia es…»). Corrígelas y pásalas a tarjetas.",
            "Fichas: plantillas para ordenar lo que lees (lectura, autor, concepto, caso, IRAC…).",
            "Citas: tus citas con su página y la referencia en APA; se exportan a Word o BibTeX.",
            "Tarjetas, Resaltados y Apuntes: todo lo que guardaste de ese documento.",
            "Importar de Anki: Estudiar → Herramientas → Anki (.apkg, .txt o .csv).",
        ),
    ),

    // ---------------- Derecho ----------------
    Topic(
        "Derecho", Icons.Rounded.Gavel, "Leyes y códigos",
        "Si el documento es una ley o un código, Hilo lo reconoce y lo ordena.",
        listOf(
            "Sala de estudio → Ley: índice por libros, títulos y capítulos, y el buscador «Ir al artículo».",
            "Al abrir un artículo ves a qué artículos remite y cuáles lo mencionan. Tócalos para saltar.",
            "Si eliges un texto que dice «conforme al artículo 12», aparece «Ver art. 12».",
            "Tarjetas de artículos: «¿Qué establece el artículo 45?», de a una, por capítulo o de toda la ley.",
            "Pirámide normativa: dónde está esa norma (Constitución, tratado, ley, reglamento…).",
        ),
    ),
    Topic(
        "Derecho", Icons.Rounded.Timer, "Plazos",
        "Todos los plazos del texto y una calculadora.",
        listOf(
            "Sala de estudio → Plazos: la tabla de plazos del documento («dentro de tres días hábiles») con su artículo.",
            "Calculadora de plazos: pon la fecha de inicio y la cantidad; Hilo cuenta días hábiles saltando fines de semana y feriados de Honduras (también Semana Santa).",
            "Pasa los plazos a tarjetas para memorizarlos.",
        ),
        listOf("Verifica siempre el cómputo con la norma aplicable y el calendario oficial del tribunal."),
    ),
    Topic(
        "Derecho", Icons.Rounded.FormatQuote, "Fichas de caso, IRAC y sentencias",
        "Para analizar casos y sentencias con orden.",
        listOf(
            "Sala de estudio → Fichas → «+ Caso con método IRAC»: Problema (Issue), Regla, Análisis y Conclusión. Hilo revisa que cites un artículo.",
            "«+ sentencia»: hechos, problema jurídico, norma, argumentos, fallo y votos.",
            "«+ norma» y «+ caso» para resumir leyes y casos prácticos.",
            "Jurídico (en Buscar): abre el Diccionario panhispánico del español jurídico de la RAE.",
        ),
    ),

    // ---------------- Sociología ----------------
    Topic(
        "Sociología", Icons.Rounded.Groups, "Autores, paradigmas y comparador",
        "Hilo reconoce a los autores clásicos y te ayuda a compararlos.",
        listOf(
            "Sala de estudio → Autores: los autores que aparecen en el texto y los conceptos que se tratan junto a cada uno.",
            "Marca dos o tres autores y toca Comparar: un cuadro con lo que dice cada uno.",
            "Métodos para Sociología: 18 autores clave, paradigmas, escuelas y una línea de tiempo.",
            "Línea de tiempo: las fechas y siglos del texto, en orden.",
        ),
    ),
    Topic(
        "Sociología", Icons.Rounded.AccountTree, "Fichas para tus trabajos",
        "Plantillas para leer y escribir en ciencias sociales.",
        listOf(
            "Fichas → «+ autor», «+ concepto», «+ método», «+ lectura» y «+ Análisis de texto clásico».",
            "Cada ficha tiene sus campos (tesis, argumentos, conceptos, citas, tu crítica) y se comparte como texto.",
            "Citas: elige el texto → Más → Guardar como cita. En Citas tienes la referencia APA del libro y cada cita con su página.",
        ),
    ),

    // ---------------- Biblioteca ----------------
    Topic(
        "Biblioteca", Icons.Rounded.Add, "Agregar libros",
        "Hilo abre PDF, EPUB, MOBI y AZW3 sin DRM, Word, ODT, FB2, HTML, Markdown, TXT y cómics.",
        listOf(
            "Toca «Permitir» en la biblioteca: Hilo busca solo los documentos del celular, sin copiarlos, y avisa cuando hay nuevos.",
            "«+ Agregar» → Archivos: elige uno o varios.",
            "«+ Agregar» → Buscar en una carpeta: por ejemplo Descargas; Hilo la revisa cada vez que abres la app.",
            "«+ Agregar» → Fotos de páginas: se unen en un PDF y Hilo reconoce su texto sin internet.",
            "«+ Agregar» → Pegar texto: para artículos o apuntes copiados.",
            "Desde otra app: Compartir → Hilo, o Abrir con → Hilo.",
        ),
        listOf("En el menú ☰ → Carpetas puedes elegir carpetas que Hilo no debe leer.", "Los libros con DRM (Kindle, Google Play Libros) no se pueden abrir."),
    ),
    Topic(
        "Biblioteca", Icons.Rounded.FilterList, "Ordenar, filtrar y elegir varios",
        "Para encontrar rápido lo que buscas.",
        listOf(
            "Barra de abajo: Biblioteca, Leyendo, Estudiar y Ajustes.",
            "El botón de filtros combina estado (Leyendo, Por leer, Leídos, Favoritos, Nuevos, Con examen), formato, colección y etiquetas.",
            "Mantén el dedo sobre un libro para elegir varios: luego puedes compartirlos, marcarlos como leídos, pasarlos a favoritos o a la papelera.",
            "Menú ☰ → Duplicados encuentra libros repetidos aunque tengan otro nombre.",
            "La vista en lista o en cuadrícula se cambia en Ajustes.",
            "Para no ver un formato nunca (por ejemplo HTML): Ajustes → Formatos que se ven, y toca ese formato. Los archivos no se borran; vuelven al tocarlo otra vez.",
        ),
    ),
    Topic(
        "Biblioteca", Icons.Rounded.Search, "Buscar en todos tus libros",
        "Encuentra una palabra dentro de todos tus documentos a la vez.",
        listOf(
            "Toca la lupa en la biblioteca y escribe. Puedes escribir sin tildes.",
            "Arriba ves «Dentro de tus libros»: los párrafos donde aparece. Tócalos para abrir el libro justo ahí.",
            "Debajo ves lo que coincide en tus tarjetas, apuntes y escritos.",
            "Para una frase exacta, escríbela entre comillas: \"capital cultural\".",
        ),
        listOf("Los PDF se suman a la búsqueda después de abrirlos una vez en modo Texto o en su sala de estudio."),
    ),
    Topic(
        "Biblioteca", Icons.Rounded.Save, "Copias, Novedades y datos",
        "Tus cosas se guardan solo en este celular.",
        listOf(
            "Menú ☰ → Copia de seguridad: guarda tarjetas, subrayados, fichas, citas y avance en un .zip y restáuralo en otro celular.",
            "Exportar a Anki (en Estudiar → Herramientas): un archivo con tus tarjetas para importarlo en Anki.",
            "Menú ☰ → Novedades: qué cambió en cada versión.",
            "Si desinstalas Hilo se borra lo que guardaste: haz una copia antes.",
        ),
    ),

    // ---------------- Ajustes y ayudas ----------------
    Topic(
        "Ajustes y ayudas", Icons.Rounded.Lightbulb, "Ayudas de lectura para el TDAH",
        "Pequeñas ayudas para que el ojo no se pierda entre líneas. Prueba una a la vez.",
        listOf(
            "Colores: cada línea pasa de un color a otro y la siguiente empieza en ese mismo color. El ojo encuentra el inicio de la línea sin saltársela.",
            "Regla: oscurece todo menos la línea que lees. Tócala para moverla o usa las flechas.",
            "Negrita guía (modo Texto): resalta el inicio de cada palabra.",
            "Palabra a palabra (▶): el texto aparece en un solo punto, a tu velocidad.",
            "Si pierdes el hilo: baja el interlineado, sube un poco el tamaño y los márgenes para líneas más cortas.",
        ),
    ),
    Topic(
        "Ajustes y ayudas", Icons.Rounded.CenterFocusStrong, "Modo enfoque y temporizador",
        "Para leer sin interrupciones en bloques cortos.",
        listOf(
            "Ajustes → Más ajustes → Modo enfoque: pantalla completa, sin porcentaje y notificaciones en silencio mientras lees. La primera vez Android te pide permiso.",
            "Toca el reloj y elige 10, 15, 25 o 45 minutos. Al terminar, descansa 5.",
            "Al final de la sesión, el cierre del día te muestra lo que hiciste y lo que toca mañana.",
        ),
    ),
    Topic(
        "Ajustes y ayudas", Icons.Rounded.DarkMode, "Modos de color",
        "Día, Noche, Noche contraste, Sepia, Sepia contraste, Crepúsculo y Consola.",
        listOf(
            "En Ajustes (Aa) elige el modo. Cambia el papel y la letra, también en los PDF.",
            "Toca la esquina de arriba a la izquierda de la página para pasar de día a noche.",
        ),
    ),
    Topic(
        "Ajustes y ayudas", Icons.Rounded.Brush, "Anotar sobre el PDF",
        "Escribe y dibuja encima de la página original.",
        listOf(
            "En la página original toca el pincel arriba.",
            "Resaltar: arrastra sobre el texto; el resaltado se ajusta a las líneas.",
            "Lápiz para escribir o subrayar, Borrar para quitar trazos y Nota para dejar un comentario en un punto.",
            "El botón «Guardar» de esa barra crea una copia del PDF con tus anotaciones para enviarla o abrirla en otra app.",
        ),
    ),
    Topic(
        "Ajustes y ayudas", Icons.Rounded.Tune, "Modo simple, vibración y movimiento",
        "Para una app más tranquila.",
        listOf(
            "Modo simple: solo leer, resaltar y repasar; el resto queda en «Más herramientas».",
            "Vibración suave: un toque al pasar página y al responder tarjetas. Se apaga en Más ajustes.",
            "Si tu celular tiene las animaciones desactivadas, Hilo tampoco anima.",
            "Si Hilo se cierra de golpe, al abrirlo otra vez te ofrece enviar el informe.",
        ),
    ),
)

private fun fold(s: String) = Normalizer.normalize(s.lowercase(), Normalizer.Form.NFD).replace(Regex("\\p{M}+"), "")

@Composable
fun GuideScreen(vm: HiloViewModel) {
    val c = LocalHilo.current
    BackHandler { vm.screen = Screen.Library }
    var query by rememberSaveable { mutableStateOf("") }
    var group by rememberSaveable { mutableStateOf("") }
    var open by rememberSaveable { mutableStateOf(setOf(BASE_TOPICS.first().title)) }
    val q = fold(query.trim())
    val shown = ALL_TOPICS.filter { t ->
        (group.isEmpty() || t.group == group) &&
            (q.isEmpty() || fold(t.title + " " + t.what + " " + t.steps.joinToString(" ") + " " + t.tips.joinToString(" ")).contains(q))
    }

    Box(Modifier.fillMaxSize().background(c.bg).windowInsetsPadding(WindowInsets.safeDrawing), contentAlignment = Alignment.TopCenter) {
        Column(Modifier.widthIn(max = 720.dp).fillMaxSize()) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(4.dp)) {
                IconButton(onClick = { vm.screen = Screen.Library }) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Volver", tint = c.ink) }
                Text("Cómo usar Hilo", fontFamily = Fonts.display, fontWeight = FontWeight.Bold, fontSize = 22.sp, color = c.ink)
            }
            OutlinedTextField(
                query, { query = it },
                placeholder = { Text("Busca: subrayar, tarjetas, PDF, plazos…") },
                leadingIcon = { Icon(Icons.Rounded.Search, null, tint = c.muted) },
                singleLine = true, shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = c.line, focusedBorderColor = c.accent),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            )
            Row(
                Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                (listOf("") + GROUPS).forEach { g ->
                    val on = g == group
                    Text(
                        g.ifEmpty { "Todo" }, color = if (on) c.accentInk else c.ink, fontSize = 14.sp,
                        modifier = Modifier.clip(RoundedCornerShape(50)).background(if (on) c.accent else c.surface)
                            .clickable { group = g }.padding(horizontal = 14.dp, vertical = 8.dp),
                    )
                }
            }
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 16.dp, end = 16.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (shown.isEmpty()) item { Text("No encontré ese tema. Prueba con otra palabra.", color = c.muted, modifier = Modifier.padding(16.dp)) }
                var last = ""
                shown.forEach { t ->
                    if (t.group != last && group.isEmpty() && q.isEmpty()) {
                        val g = t.group
                        item(key = "g:$g") {
                            Text(g.uppercase(), color = c.muted, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, modifier = Modifier.padding(top = 8.dp, start = 4.dp))
                        }
                    }
                    last = t.group
                    item(key = t.title) {
                        val on = t.title in open || q.isNotEmpty()
                        TopicCard(t, on) { open = if (t.title in open) open - t.title else open + t.title }
                    }
                }
            }
        }
    }
}

@Composable
internal fun TopicCard(t: Topic, expanded: Boolean, onToggle: () -> Unit) {
    val c = LocalHilo.current
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(c.surface)
            .border(1.dp, if (expanded) c.accent.copy(alpha = 0.5f) else c.line, RoundedCornerShape(18.dp))
            .animateContentSize(),
    ) {
        Row(
            Modifier.fillMaxWidth().clickable(onClick = onToggle).padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(40.dp).clip(CircleShape).background(c.band), contentAlignment = Alignment.Center) {
                Icon(t.icon, null, tint = c.accent, modifier = Modifier.size(22.dp))
            }
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(t.title, color = c.ink, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                if (!expanded) Text(t.what, color = c.muted, fontSize = 13.sp, maxLines = 2, lineHeight = 17.sp)
            }
            Icon(if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore, if (expanded) "Cerrar" else "Abrir", tint = c.muted)
        }
        if (expanded) {
            Column(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(t.what, color = c.ink, fontSize = 15.sp, lineHeight = 21.sp)
                Text("PASO A PASO", color = c.muted, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                t.steps.forEachIndexed { i, s ->
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(Modifier.size(24.dp).clip(CircleShape).background(c.accent), contentAlignment = Alignment.Center) {
                            Text("${i + 1}", color = c.accentInk, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Text(s, color = c.ink, fontSize = 15.sp, lineHeight = 21.sp, modifier = Modifier.weight(1f))
                    }
                }
                t.tips.forEach { tip ->
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(c.band).padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(Icons.Rounded.Lightbulb, null, tint = c.accent, modifier = Modifier.size(18.dp))
                        Text(tip, color = c.ink, fontSize = 14.sp, lineHeight = 19.sp, modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}


/** Todos los temas, ordenados por sección. */
internal val ALL_TOPICS: List<Topic> by lazy { (BASE_TOPICS + EXTRA_TOPICS).sortedBy { GROUPS.indexOf(it.group).let { i -> if (i < 0) 99 else i } } }
