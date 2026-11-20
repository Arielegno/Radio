package com.hilo.lector.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.automirrored.rounded.Notes
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.CenterFocusStrong
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.FormatQuote
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.HourglassBottom
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.Quiz
import androidx.compose.material.icons.rounded.RecordVoiceOver
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.SlowMotionVideo
import androidx.compose.material.icons.rounded.Style
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.ListAlt
import androidx.compose.material.icons.rounded.Calculate
import androidx.compose.material.icons.rounded.ElectricBolt
import androidx.compose.material.icons.rounded.Functions
import androidx.compose.material.icons.rounded.Gavel
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.LibraryBooks
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.hilo.lector.HiloViewModel
import com.hilo.lector.Screen

/** Temas de la guía que se abren con el botón «?» de cada apartado. */
internal val EXTRA_TOPICS: List<Topic> get() = EXTRA_LIST + CLASS_TOPICS + NEW_TOPICS

private val EXTRA_LIST = listOf(
    // ---------------- Ayudas TDAH ----------------
    Topic(
        "Ayudas TDAH", Icons.Rounded.Psychology, "Ayudas para concentrarte (TDAH)",
        "Un panel con todo lo que ayuda a empezar, mantener la atención, manejar el tiempo y no castigarte. Está en el lector, en el botón del cerebro 🧠 de arriba.",
        listOf(
            "Abre un libro y toca el centro para ver los menús.",
            "Toca el cerebro 🧠 arriba: se abre «Ayudas para concentrarte».",
            "Está ordenado en cinco partes: Empezar, Mientras lees, Tiempo, Ambiente y Motivación.",
            "Cada ayuda tiene su «?» con la explicación. Activa una o dos, no todas a la vez.",
        ),
        listOf("Prueba cada ayuda durante tres o cuatro días antes de decidir si te sirve. Lo que funciona cambia de persona a persona."),
        key = "tdah",
    ),
    Topic(
        "Ayudas TDAH", Icons.Rounded.PlayCircle, "Solo 2 minutos",
        "Con TDAH lo más difícil es empezar. Comprometerse a solo 2 minutos baja la barrera, y casi siempre sigues.",
        listOf(
            "En la biblioteca, en «Sigue leyendo», toca «Solo 2 min». También está en el panel 🧠.",
            "Se abre el libro donde lo dejaste con un temporizador de 2 minutos.",
            "Al terminar, Hilo te felicita y pregunta si sigues 10 minutos más. Puedes decir que no sin culpa.",
        ),
        key = "dos-min",
    ),
    Topic(
        "Ayudas TDAH", Icons.Rounded.History, "¿Dónde me quedé?",
        "Al volver a un libro después de varias horas, Hilo te muestra un resumen corto de lo último que leíste y tus subrayados de esa parte, para no tener que releer.",
        listOf(
            "Aparece solo al abrir un libro que no tocabas hace más de 4 horas.",
            "Lee las 3 ideas y toca «Seguir leyendo».",
            "Si no recuerdas nada, toca «Volver un poco» para empezar unas páginas antes.",
            "Para verlo cuando quieras: panel 🧠 → ¿Dónde me quedé?",
        ),
        listOf("Las ideas las elige Hilo con las palabras que más se repiten en esa parte; no es un resumen escrito por una persona."),
        key = "donde-quede",
    ),
    Topic(
        "Ayudas TDAH", Icons.Rounded.Flag, "Meta de hoy",
        "Una meta pequeña y visible («hoy: 15 minutos») es más fácil de cumplir que «terminar el capítulo». La barra se llena mientras lees.",
        listOf(
            "Elige tu meta en el panel 🧠 o tocando la tarjeta de la racha en la biblioteca: 5, 10, 15, 20 o 30 minutos.",
            "Hilo cuenta los minutos en que realmente lees (si no pasas de página en 3 minutos, no cuenta).",
            "Al cumplirla aparece una pequeña celebración.",
        ),
        key = "meta",
    ),
    Topic(
        "Ayudas TDAH", Icons.Rounded.CenterFocusStrong, "Foco de un párrafo",
        "Todo se atenúa menos el párrafo que estás leyendo. Es como la regla, pero por bloques de ideas.",
        listOf(
            "Funciona en el modo Texto. Actívalo en el panel 🧠 → Foco de un párrafo.",
            "Toca el centro de la pantalla para pasar al párrafo siguiente; el borde izquierdo vuelve al anterior.",
            "Con el foco activo, el borde derecho de la pantalla muestra u oculta los menús.",
        ),
        key = "foco-parrafo",
    ),
    Topic(
        "Ayudas TDAH", Icons.Rounded.RecordVoiceOver, "Voz con resaltado",
        "Hilo lee en voz alta y marca la palabra que está diciendo, como un karaoke. Leer y escuchar a la vez ayuda a sostener la atención.",
        listOf(
            "Toca el botón de la voz abajo, o panel 🧠 → Voz con resaltado.",
            "La palabra que suena se marca en el texto y la página avanza sola.",
            "La velocidad se cambia en Ajustes (Aa) → Voz.",
        ),
        listOf("El resaltado de palabras funciona en el modo Texto. En la página original del PDF la voz lee, pero sin marcar cada palabra.", "Si tu celular no tiene voz en español, instálala en Ajustes del sistema → Texto a voz."),
        key = "voz-resaltado",
    ),
    Topic(
        "Ayudas TDAH", Icons.Rounded.SlowMotionVideo, "Desplazamiento automático",
        "El texto avanza solo a la velocidad que elijas, como un teleprompter. Sirve cuando te cuesta mantener el ritmo.",
        listOf(
            "Panel 🧠 → Desplazamiento automático → Empezar.",
            "Abajo aparece un control: − y + cambian la velocidad, ⏸ lo detiene.",
            "En modo vertical el texto sube suave; en modo por páginas, la página pasa sola cada cierto tiempo.",
        ),
        key = "teleprompter",
    ),
    Topic(
        "Ayudas TDAH", Icons.Rounded.Quiz, "Preguntas de control",
        "Cada pocas páginas Hilo te pregunta de qué trataba lo que leíste. Detecta cuando tus ojos leían pero tu mente estaba en otro lado.",
        listOf(
            "Panel 🧠 → Preguntas de control → elige cada 3, 5 o 10 páginas.",
            "Cuando aparezca, escribe o piensa en una frase de qué trataba.",
            "Toca «Ver ideas clave» para comparar y di si lo entendiste.",
            "Si no, «Volver a leer esta parte» te lleva al inicio de ese tramo.",
        ),
        key = "control",
    ),
    Topic(
        "Ayudas TDAH", Icons.Rounded.EditNote, "Libreta de distracciones",
        "Cuando se te cruza un pensamiento («pagar la luz»), anótalo en 3 segundos y sigue leyendo. Ya no tienes que sostenerlo en la cabeza.",
        listOf(
            "En el lector, con los menús a la vista, toca la nube 💭 abajo a la izquierda (o panel 🧠 → Libreta).",
            "Escribe y toca «Guardar y seguir».",
            "Al terminar, abre la libreta en el panel 🧠: marca lo hecho o compártelo.",
        ),
        key = "distracciones",
    ),
    Topic(
        "Ayudas TDAH", Icons.Rounded.HourglassBottom, "Temporizador visual",
        "Un círculo que se va vaciando en vez de números. Ayuda a «ver» el tiempo, que con TDAH cuesta sentir.",
        listOf(
            "Empieza un bloque con el reloj de abajo o en el panel 🧠.",
            "Arriba a la derecha de la página aparece el círculo que se vacía.",
            "Se activa o apaga en panel 🧠 → Temporizador visual.",
        ),
        key = "timer-visual",
    ),
    Topic(
        "Ayudas TDAH", Icons.Rounded.Timer, "Cuánto falta",
        "Hilo mide tu velocidad real de lectura y calcula cuánto te falta para terminar el capítulo y el libro.",
        listOf(
            "Lee unos minutos para que Hilo aprenda tu ritmo.",
            "Mira el panel 🧠 → Tiempo: «Te faltan ~12 min de este capítulo y ~3 h del libro».",
        ),
        key = "tiempo-falta",
    ),
    Topic(
        "Ayudas TDAH", Icons.Rounded.Alarm, "Aviso de pausa (hiperfoco)",
        "El TDAH también trae hiperfoco: horas sin parar que luego cansan. Hilo te avisa con suavidad para tomar agua o estirarte.",
        listOf(
            "Panel 🧠 → Aviso de pausa → elige 45, 60 o 90 minutos (o apagado).",
            "Cuando llegue, elige «Pausa de 5 min» o «Sigo 30 min más».",
        ),
        key = "hiperfoco",
    ),
    Topic(
        "Ayudas TDAH", Icons.Rounded.BatteryChargingFull, "Plan según tu energía",
        "No todos los días se puede lo mismo. Eliges cómo está tu energía y Hilo te propone qué hacer.",
        listOf(
            "En Estudiar (barra de abajo) o en el panel 🧠, toca «¿Cómo está tu energía?».",
            "Poca: repasar pocas tarjetas fáciles o escuchar el libro.",
            "Media: leer 15 minutos o repasar tus tarjetas.",
            "Mucha: material nuevo 25 minutos o un simulacro de examen.",
            "Toca la propuesta y empieza.",
        ),
        key = "energia",
    ),
    Topic(
        "Ayudas TDAH", Icons.Rounded.GraphicEq, "Sonidos de fondo",
        "Ruido marrón, rosa, blanco o lluvia, hechos por el celular sin internet. A muchas personas con TDAH les ayuda a concentrarse.",
        listOf(
            "Panel 🧠 → Sonidos de fondo → elige uno y ajusta el volumen.",
            "Suena mientras Hilo está abierto; al salir de la app se apaga.",
        ),
        listOf("El ruido marrón es el más grave y suave; prueba primero ese. Usa audífonos y volumen bajo."),
        key = "sonidos",
    ),
    Topic(
        "Ayudas TDAH", Icons.Rounded.Group, "Leer con un amigo",
        "Leer «acompañado» (body doubling) ayuda mucho. Quedas con un amigo para leer a la misma hora, cada uno en su lugar.",
        listOf(
            "Panel 🧠 → Leer con un amigo.",
            "Elige cuánto tiempo y cuándo empiezan (ahora, en 5 o en 10 minutos).",
            "Hilo arma el mensaje para enviarlo por WhatsApp u otra app y empieza el temporizador a esa hora.",
            "Al terminar puedes avisarle a tu amigo con un toque.",
        ),
        listOf("Hilo no se conecta a internet: tu amigo no necesita la app, solo leer a la misma hora."),
        key = "amigo",
    ),
    Topic(
        "Ayudas TDAH", Icons.Rounded.LocalFireDepartment, "Racha amable",
        "Cuenta los días seguidos que lees o repasas, sin castigos: tienes 2 días de descanso por semana que no rompen la racha.",
        listOf(
            "La tarjeta de la racha está arriba en la biblioteca y en Estudiar.",
            "Cuenta un día si lees 5 minutos o repasas al menos una tarjeta.",
            "Si un día no puedes, se usa un día de descanso solo. Tócala para ver tus últimas dos semanas y cambiar tu meta.",
        ),
        key = "racha",
    ),

    // ---------------- Estudiar: cada pestaña ----------------
    Topic(
        "Estudiar", Icons.Rounded.School, "La sala de estudio",
        "Cada documento tiene su sala de estudio con pestañas: Ruta, Guía, Palabras, Glosario, Mapa, Fichas, Citas, Tarjetas, Resaltados, Apuntes y, si corresponde, Ley, Plazos, Autores y Línea de tiempo.",
        listOf(
            "Entra desde el lector (birrete 🎓 arriba) o desde la biblioteca (menú ⋮ del libro → Estudiar).",
            "Arriba están las pestañas. El «?» al lado explica la pestaña que estás viendo.",
            "En modo simple ves pocas pestañas; toca «Más herramientas…» para el resto.",
        ),
        key = "sala",
    ),
    Topic(
        "Estudiar", Icons.AutoMirrored.Rounded.MenuBook, "Pestaña Guía",
        "Para cada capítulo: cuántas palabras tiene, sus ideas principales, los conceptos clave y preguntas para leer con un propósito.",
        listOf(
            "Lee las preguntas antes del capítulo: te dicen qué buscar.",
            "Toca «Ir» para abrir el libro en esa parte.",
            "Pasa las preguntas a tarjetas para repasarlas.",
        ),
        key = "guia-tab",
    ),
    Topic(
        "Estudiar", Icons.AutoMirrored.Rounded.Notes, "Glosario",
        "Las definiciones que Hilo encontró en el texto («La anomia es…») y las que tú agregues.",
        listOf(
            "Revisa cada definición: corrígela con el lápiz si no es exacta.",
            "Toca «Tarjeta» para repasarla, o «Todas a tarjetas».",
            "Desde Palabras, «Al glosario» agrega una palabra con su oración.",
        ),
        key = "glosario",
    ),
    Topic(
        "Estudiar", Icons.Rounded.Style, "Fichas",
        "Plantillas para ordenar lo que lees: lectura, autor, concepto, caso, IRAC, sentencia, norma, método o texto clásico.",
        listOf(
            "Toca «+» y elige el tipo de ficha.",
            "Llena los campos con tus palabras; no copies el texto.",
            "Compártela como texto o expórtala a Word.",
        ),
        key = "fichas",
    ),
    Topic(
        "Estudiar", Icons.Rounded.FormatQuote, "Citas",
        "Tus citas textuales con su página y la referencia en APA, listas para tu trabajo.",
        listOf(
            "Para guardar una: elige el texto → Más → Guardar como cita.",
            "En Citas completa el autor, el año y la editorial una sola vez.",
            "Exporta a Word o a BibTeX (para Zotero o Mendeley).",
        ),
        key = "citas",
    ),
    Topic(
        "Estudiar", Icons.Rounded.ListAlt, "Tarjetas, resaltados y apuntes",
        "Todo lo que guardaste de este documento.",
        listOf(
            "Tarjetas: edita o borra tus tarjetas y toca «Repasar» para practicar solo las de este documento.",
            "Resaltados: tus subrayados por color y tipo; toca uno para ir a esa parte o bórralo.",
            "Apuntes: tus notas libres y las que guardaste desde el texto elegido.",
        ),
        key = "tarjetas-tab",
    ),
    Topic(
        "Estudiar", Icons.Rounded.AutoStories, "Las actividades de la ruta",
        "Cada fase de la ruta tiene actividades cortas. Esta es la lista y para qué sirve cada una.",
        listOf(
            "Prueba previa: preguntas antes de leer, aunque falles. Prepara al cerebro.",
            "Lectura guiada: preguntas por capítulo para leer buscando algo.",
            "Notas Cornell: a la izquierda preguntas, a la derecha notas, abajo un resumen.",
            "Dudas: lo que marcaste como «Duda» para resolverlo.",
            "Resumen con tus palabras: escribe lo esencial sin mirar.",
            "Mapa propio: arma tu mapa de conceptos y compáralo con el de Hilo.",
            "Recuerdo libre: escribe todo lo que recuerdes en 3 minutos y mira qué olvidaste.",
            "Preguntas Bloom: de recordar a crear, cada vez más profundas.",
            "Aplicar: usa lo aprendido en un caso o ejemplo.",
            "Simulacro y cierre: examen con tiempo y resumen del día.",
        ),
        key = "actividades",
    ),
)


/** Temas de las Clases. */
internal val CLASS_TOPICS = listOf(
    Topic(
        "Clases", Icons.Rounded.School, "Clases: estudiar por materia",
        "Una clase junta todo lo de una materia: libros, documentos, fotos de la pizarra, tus apuntes y las fechas. Con eso Hilo arma un mapa de conceptos de toda la clase, una síntesis por temas y preguntas de examen.",
        listOf(
            "Toca «Clases» en la barra de abajo (o en Estudiar).",
            "Toca «Nueva clase», ponle nombre, profesor y color.",
            "En Materiales agrega lo de esa materia: de tu biblioteca, archivos o fotos.",
            "Mira el Mapa y la Síntesis, repasa en Estudiar y anota cada clase en el Diario.",
            "Pon los exámenes en Calendario: Hilo arma el plan de lectura hasta esa fecha.",
        ),
        listOf("Un mismo libro puede estar en varias clases. Si quitas un material de la clase, no se borra de tu biblioteca."),
        key = "clases",
    ),
    Topic(
        "Clases", Icons.AutoMirrored.Rounded.MenuBook, "Materiales de la clase",
        "Todo lo que la clase usa: libros, PDF del profesor, artículos, leyes y fotos de la pizarra o de tus apuntes.",
        listOf(
            "«+ De tu biblioteca»: marca los documentos que ya tienes.",
            "«+ Archivos»: agrega PDF, Word, EPUB… y quedan en la clase y en tu biblioteca.",
            "«+ Fotos»: elige fotos de la pizarra o de tus cuadernos. Hilo reconoce el texto sin internet y lo suma al mapa.",
            "Toca un material para leerlo, «Estudiar» para su sala de estudio, o la X para quitarlo de la clase.",
        ),
        key = "clase-materiales",
    ),
    Topic(
        "Clases", Icons.Rounded.Psychology, "Mapa de la clase",
        "Una red con los conceptos más importantes de todos los materiales juntos. Hilo los agrupa en temas, cada uno con su color.",
        listOf(
            "Cada círculo es un concepto. Es más grande si pesa más en la clase (se repite mucho y en varios materiales). El número es cuántas veces aparece.",
            "Las líneas unen conceptos que salen en la misma oración; más gruesas, más veces juntos.",
            "Arriba están los temas: toca uno para ver solo sus conceptos.",
            "Toca un concepto: ves en qué materiales aparece, cómo lo define cada uno (para comparar autores), con qué se relaciona y tus subrayados.",
            "«Crear tarjeta» hace una pregunta «¿Qué es…?» con la definición.",
        ),
        listOf("Los temas se forman solos según qué conceptos aparecen juntos. Si un tema no tiene sentido, quita palabras que no son conceptos desde la pestaña Palabras de ese libro."),
        key = "clase-mapa",
    ),
    Topic(
        "Clases", Icons.Rounded.AutoStories, "Síntesis y hoja de repaso",
        "Un resumen de toda la clase, ordenado por temas: conceptos clave, definiciones, las ideas centrales de cada texto y tus subrayados.",
        listOf(
            "Abre la clase y toca «Síntesis».",
            "Cada tema muestra sus conceptos, definiciones e ideas centrales con el material de donde salen. Toca una idea para ir a esa parte.",
            "Lo que marcaste con ★ en el Diario aparece arriba: «Va para el examen».",
            "«Exportar a Word» crea el documento para imprimir o completar. «Hoja de repaso» es la versión de una página.",
        ),
        listOf("Hilo elige las ideas centrales con un método que busca las frases más conectadas con el resto del texto (TextRank). No inventa texto: todo sale de tus materiales."),
        key = "clase-sintesis",
    ),
    Topic(
        "Clases", Icons.Rounded.Quiz, "Estudiar la clase",
        "Repaso, avance y simulacro de toda la materia junta.",
        listOf(
            "Mazo de la clase: las tarjetas de todos los materiales, mezcladas. «Repasar las de hoy» abre el repaso.",
            "Cómo vas en cada tema: verde lo dominas, amarillo a medias, rojo repásalo, gris sin tarjetas. «Crear» hace tarjetas con las definiciones del tema.",
            "Simulacro: preguntas de opción múltiple armadas con tus materiales (definiciones, relaciones, autores, en qué texto está cada tema). Lo que falles puede pasar a tarjetas.",
            "Plan hasta el examen: cuántas páginas leer por día de cada material, dejando los últimos días para repasar.",
        ),
        key = "clase-estudiar",
    ),
    Topic(
        "Clases", Icons.Rounded.EditNote, "Diario de clase",
        "Tus apuntes de cada clase, por fecha, con grabación de audio.",
        listOf(
            "Escribe lo que se vio o lo que dijo el profe y toca «Guardar».",
            "Marca ★ «Va para el examen» para que aparezca en la síntesis.",
            "El micrófono graba la clase; toca ■ para terminar y se guarda con la entrada. La primera vez Android pide permiso.",
            "«Tarjeta» convierte la entrada en una tarjeta de repaso.",
        ),
        listOf("Pide permiso a tu profesor antes de grabar la clase.", "Las grabaciones quedan solo en tu celular; no se pasan a texto."),
        key = "clase-diario",
    ),
    Topic(
        "Clases", Icons.Rounded.Alarm, "Calendario de la clase",
        "Exámenes, entregas y lecturas con su fecha.",
        listOf(
            "Elige el tipo (Examen, Entrega, Lectura u Otro), escribe qué es y la fecha.",
            "Las próximas 7 días se ven en rojo. Márcalas como hechas con la casilla.",
            "Con un examen puesto, la pestaña Estudiar muestra el plan de lectura por día y la lista de clases lo muestra primero.",
        ),
        key = "clase-calendario",
    ),
)

/** Áreas de referencia, diccionario, herramientas y fórmulas (2.5). */
internal val NEW_TOPICS = listOf(
    Topic(
        "Áreas y diccionario", Icons.Rounded.LibraryBooks, "Áreas de referencia",
        "Un área junta TODOS los documentos de un campo (por ejemplo «Derecho hondureño»: Constitución, códigos, leyes, reglamentos). Hilo los lee y aprende su estructura: qué normas hay y en qué nivel, cómo se citan entre sí, qué definen, qué plazos y sanciones ponen, quién es la autoridad y qué palabras son propias del campo.",
        listOf(
            "Ve a Clases y toca «Áreas de referencia» arriba.",
            "Toca «Nueva área», ponle nombre y elige el campo (Derecho, Psicología, Eléctrica…).",
            "En Materiales agrega todos los documentos de ese campo.",
            "Usa las pestañas: Buscar, Glosario, Vocabulario y, según el campo, Normas, Plazos, Plantillas, Fórmulas o Herramientas.",
            "En una clase, en Materiales → «Áreas de referencia que usa», conecta el área: así la clase busca también ahí.",
            "Mientras lees, elige un texto → Más → «Buscar en mis áreas de referencia»: si nombra un artículo de una ley que tienes, lo abre directo; si no, muestra los párrafos más parecidos.",
        ),
        listOf(
            "Hilo aprende la estructura y las palabras del campo; no «entiende» el derecho ni da consejo legal. Revisa siempre la norma vigente.",
            "Cuantos más documentos tenga el área, mejor encuentra referencias y vocabulario propio.",
        ),
        key = "areas",
    ),
    Topic(
        "Áreas y diccionario", Icons.Rounded.Search, "Buscar en un área o clase",
        "Busca en todos los documentos del área (y en las áreas que usa la clase) a la vez.",
        listOf(
            "«Palabras»: encuentra las páginas que contienen lo que escribes.",
            "«Párrafos parecidos»: pega un párrafo o una idea y Hilo trae los párrafos que hablan de lo mismo, aunque usen otras palabras cercanas.",
            "Toca un resultado para abrir el documento en ese lugar.",
        ),
        listOf("La primera vez tarda un poco porque prepara el índice de cada documento."),
        key = "clase-buscar",
    ),
    Topic(
        "Áreas y diccionario", Icons.AutoMirrored.Rounded.MenuBook, "Glosario y vocabulario del área",
        "El Glosario junta las definiciones que aparecen en los documentos («Para efectos de esta ley se entiende por…», «X es…»). El Vocabulario muestra las palabras que este campo usa mucho más que el español común: las que tienes que dominar.",
        listOf(
            "Glosario: toca un término para ver su definición y de qué documento viene; «Tarjeta» la manda a tu repaso.",
            "Vocabulario: las primeras son las más propias del campo. Toca una para ver dónde aparece o buscarla en el diccionario.",
        ),
        listOf("Si una «definición» no lo es, bórrala: Hilo usa reglas, no inteligencia artificial, y a veces se equivoca."),
        key = "clase-glosario",
    ),
    Topic(
        "Áreas y diccionario", Icons.Rounded.Gavel, "Normas, plazos y plantillas (Derecho)",
        "Para áreas de Derecho: Normas ordena los documentos por jerarquía (Constitución, tratados, códigos, leyes, reglamentos…) y muestra qué normas citan a cuáles. Plazos junta los plazos, sanciones y autoridades. Plantillas guarda modelos de escritos.",
        listOf(
            "Normas: toca una norma para ver a quién cita y quién la cita.",
            "Plazos: cada plazo con su artículo; toca para abrirlo.",
            "Plantillas: guarda un escrito modelo con «Usar como modelo», genera una plantilla de Word con los campos para llenar y revisa un borrador contra el modelo (qué partes le faltan).",
        ),
        listOf("La jerarquía se deduce del título del documento; si se equivoca, el orden igual sirve de guía."),
        key = "clase-normas",
    ),
    Topic(
        "Áreas y diccionario", Icons.Rounded.Translate, "Diccionario sin internet",
        "Diccionarios completos de español e inglés del Wikcionario: definiciones, ejemplos, pronunciación, origen y traducción entre los dos idiomas. Se descargan una vez dentro de Hilo y después funcionan sin internet.",
        listOf(
            "Ve a Ajustes → «Diccionarios sin internet» y toca «Descargar» (usa wifi: español 27 MB, inglés 88 MB).",
            "Al leer, elige una palabra → «Traducir» o Más → «Diccionario».",
            "Si la palabra está conjugada o en plural (corrieron, leyes), Hilo busca su forma base.",
            "«+ A mis tarjetas de vocabulario» la manda al repaso.",
        ),
        listOf("El Diccionario de la RAE no se puede guardar en la app por derechos de autor; el botón «RAE» lo abre en internet."),
        key = "diccionario",
    ),
    Topic(
        "Herramientas", Icons.Rounded.Calculate, "Herramientas para tu carrera",
        "Calculadoras y creadores de tarjetas que funcionan sin internet. Están en Estudiar → Herramientas y, dentro de cada clase o área, en la pestaña Herramientas (con las de su campo primero).",
        listOf(
            "Toca una herramienta: se abre a pantalla completa. La X o «atrás» la cierra.",
            "Los valores aceptan prefijos: 4.7k = 4700, 20m = 0,02, 100n = 0,0000001, 1M = 1 000 000.",
            "Las tarjetas que creas (código, problema, fórmula, imagen tapada) van a la clase donde estás o, si no, al mazo «herramientas».",
        ),
        listOf("Las calculadoras ayudan a comprobar; en el examen tienes que saber el procedimiento. Úsalas para verificar tus resultados."),
        key = "herramientas",
    ),
    Topic(
        "Herramientas", Icons.Rounded.ElectricBolt, "Electricidad y electrónica",
        "Para Ingeniería eléctrica y electrónica.",
        listOf(
            "Ley de Ohm y potencia: escribe dos de V, I, R, P.",
            "Código de colores: elige las bandas (4 o 5) y ve el valor, o escribe el valor y ve los colores.",
            "Serie y paralelo para R, C y L; divisor de tensión (con carga y R2 comercial).",
            "RC y RL: τ y frecuencia de corte. RLC: reactancias, |Z|, ángulo, resonancia y Q.",
            "Potencia en alterna mono y trifásica, con el capacitor para corregir el factor de potencia.",
            "Resistencia para LED (valor E12), 555 astable, decibeles, calibre AWG con caída de tensión.",
            "Bases numéricas con complemento a 2 y tabla de verdad con suma de productos.",
        ),
        listOf("Tabla de verdad: AB significa A·B, C' es NO C, + es O."),
        key = "tools-electrica",
    ),
    Topic(
        "Herramientas", Icons.Rounded.Functions, "Matemáticas y fórmulas",
        "Calculadora científica, conversor de unidades (incluye la manzana hondureña), ecuación de segundo grado y sistemas 2×2 y 3×3. En las clases de Eléctrica y Matemáticas hay además un Formulario.",
        listOf(
            "Calculadora: escribe toda la operación (2·sin(30)+√16). El interruptor cambia grados/radianes.",
            "Formulario: «Poner las básicas» agrega las fórmulas típicas del campo, o «+ Fórmula» para las tuyas.",
            "Cada fórmula se ve como en el libro. Si le pones «Para calcular» (por ejemplo 1/(2*pi*sqrt(L*C))), el botón Calcular te pide los valores.",
            "«Tarjeta» la manda al repaso: de un lado el nombre, del otro la fórmula.",
        ),
        listOf("Para escribir fórmulas se usa LaTeX: \\frac{a}{b}, x^2, x_1, \\sqrt{x}, \\pi. Debajo del campo hay una ayuda."),
        key = "clase-formulas",
    ),
    Topic(
        "Herramientas", Icons.Rounded.BarChart, "Psicología: estadística, APA y corrientes",
        "Herramientas para Psicología (y Sociología): estadística descriptiva, correlación de Pearson, prueba t de dos grupos, referencias APA 7 y un resumen de corrientes y autores. Además hay fichas nuevas: investigación, caso clínico y teoría.",
        listOf(
            "Pega tus datos (separados por espacio, coma o renglón).",
            "Cada resultado trae cómo escribirlo en APA, por ejemplo t(18) = 2.31, p = .033.",
            "APA 7: elige libro, artículo, capítulo o web y llena los campos; copia la referencia y la cita.",
            "Corrientes: busca por escuela, autor o concepto (apego, refuerzo, Gestalt…).",
        ),
        listOf("La prueba t es la de Welch (no supone varianzas iguales). Para trabajos formales confirma con SPSS, JASP o R."),
        key = "tools-psicologia",
    ),
    Topic(
        "Herramientas", Icons.Rounded.Code, "Programación y tarjetas especiales",
        "Probador de expresiones regulares y tarjetas que se ven bien en el repaso: código con colores, problemas paso a paso, fórmulas e imagen tapada.",
        listOf(
            "Tarjeta de código: pregunta + código. En el repaso el código se ve con colores y en letra de máquina.",
            "Tarjeta de problema: enunciado y pasos. Al repasar, los pasos se muestran de a uno: intenta el siguiente antes de verlo.",
            "En cualquier tarjeta puedes escribir fórmulas entre \$…\$ y se dibujan bien.",
        ),
        listOf("Bases numéricas y tabla de verdad también sirven para programación."),
        key = "tools-programacion",
    ),
    Topic(
        "Herramientas", Icons.Rounded.Image, "Imagen tapada",
        "Toma una foto o imagen (un circuito, el cerebro, un mapa, un diagrama) y tapa sus partes. Al repasar, una parte se ve en rojo y tienes que decir qué es.",
        listOf(
            "Herramientas → Imagen tapada → «Elegir imagen».",
            "Arrastra el dedo sobre la imagen para tapar cada parte (se numeran).",
            "Escribe qué es cada parte y toca «Crear tarjeta»: se crea una por parte.",
        ),
        listOf("Hilo guarda una copia de la imagen: aunque la borres de la galería, la tarjeta sigue funcionando."),
        key = "imagen-tapada",
    ),
)

/** Claves de los temas que ya estaban en la guía. */
private val KEY_TITLES = mapOf(
    "clase-vocabulario" to "Glosario y vocabulario del área",
    "clase-plantillas" to "Normas, plazos y plantillas (Derecho)",
    "clase-plazos" to "Normas, plazos y plantillas (Derecho)",
    "clase-herramientas" to "Herramientas para tu carrera",
    "primeros" to "Primeros pasos (léelo primero)",
    "gestos" to "Gestos de la página",
    "modo" to "Página original o Texto",
    "letra" to "Letra, márgenes y forma del texto",
    "paginas" to "Pasar páginas como en un libro",
    "marcadores" to "Marcadores, índice y lectura en voz alta",
    "comics" to "Cómics y mangas",
    "seleccion" to "Elegir texto (selección)",
    "menu-sel" to "El menú del texto elegido",
    "subrayar" to "Subrayar con colores (lectura activa)",
    "fragmento" to "Estudiar un fragmento (crear tarjetas)",
    "ruta" to "La ruta de estudio (6 fases)",
    "repaso" to "Repasar las tarjetas",
    "stats" to "Estadísticas y simulacro",
    "palabras" to "Burbujas de palabras",
    "mapa" to "Mapa de conceptos y frecuencias",
    "examen" to "Fecha de examen y recordatorio",
    "herramientas" to "Más herramientas de estudio",
    "ley" to "Leyes y códigos",
    "plazos" to "Plazos",
    "casos" to "Fichas de caso, IRAC y sentencias",
    "autores" to "Autores, paradigmas y comparador",
    "fichas-soc" to "Fichas para tus trabajos",
    "agregar" to "Agregar libros",
    "filtros" to "Ordenar, filtrar y elegir varios",
    "buscar" to "Buscar en todos tus libros",
    "copias" to "Copias, Novedades y datos",
    "ayudas-lectura" to "Ayudas de lectura para el TDAH",
    "enfoque" to "Modo enfoque y temporizador",
    "colores" to "Modos de color",
    "anotar" to "Anotar sobre el PDF",
    "simple" to "Modo simple, vibración y movimiento",
)

internal fun topicFor(key: String): Topic? =
    ALL_TOPICS.firstOrNull { it.key == key } ?: KEY_TITLES[key]?.let { t -> ALL_TOPICS.firstOrNull { it.title == t } }

/** El «?» que abre la explicación de este apartado. */
@Composable
fun HelpButton(vm: HiloViewModel, key: String, size: Dp = 20.dp, tint: Color? = null) {
    val c = LocalHilo.current
    IconButton(onClick = { vm.help = key }, modifier = Modifier.size(size + 20.dp)) {
        Icon(Icons.AutoMirrored.Rounded.HelpOutline, "Ayuda: cómo se usa", tint = tint ?: c.muted, modifier = Modifier.size(size))
    }
}

/** Hoja con la explicación de un apartado (el mismo texto de la guía). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpSheet(vm: HiloViewModel, key: String) {
    val c = LocalHilo.current
    val t = topicFor(key) ?: topicFor("primeros") ?: return
    ModalBottomSheet(onDismissRequest = { vm.help = null }, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), containerColor = c.bg) {
        Column(
            Modifier.padding(horizontal = 16.dp).padding(bottom = 28.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            TopicCard(t, expanded = true) {}
            Button(
                onClick = { vm.help = null; vm.screen = Screen.Guide },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = c.surface, contentColor = c.ink),
            ) { Text("Ver toda la guía") }
        }
    }
}
