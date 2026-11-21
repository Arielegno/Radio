package com.hilo.lector.data

/** Qué cambió en cada versión de Hilo. La primera es la más nueva. */
object Changelog {
    class Version(val code: Int, val name: String, val date: String, val title: String, val added: List<String>, val fixed: List<String>)

    val versions = listOf(
        Version(
            31, "2.6", "21 nov 2026", "Más herramientas, widget y copia automática",
            added = listOf(
                "Foto a texto: saca una foto a la pizarra o a una página y Hilo la convierte en texto sin internet; la guardas como documento o en el diario de una clase.",
                "Graficador de funciones con raíces, máximos, mínimos y cortes entre dos curvas.",
                "Complejos y fasores: operaciones en forma polar y rectangular, paralelo, diagrama de fasores e impedancias de R, L y C.",
                "Matrices hasta 6×6: determinante, rango, inversa, transpuesta y sistemas A·x = b.",
                "Comparar dos versiones de una ley: artículos iguales, cambiados (palabra por palabra), nuevos y quitados.",
                "Plazos: agrega días inhábiles (los que declare el Poder Judicial o la Semana Morazánica de este año).",
                "Widget para la pantalla de inicio: tarjetas para hoy, próximo examen o entrega y minutos leídos.",
                "Copia de seguridad automática a la carpeta que elijas (también Drive), cada día, 3 días o semana.",
                "El recordatorio diario ahora avisa también de exámenes y entregas de tus Clases, y si todavía no leíste para cuidar la racha.",
                "Simulacro escribiendo: respondes con tus palabras y Hilo revisa qué ideas clave nombraste.",
                "En tablets y con el celular acostado, Estudiar y Clases quedan centradas en vez de estirarse de borde a borde.",
            ),
            fixed = listOf("El botón de la libreta (💭) ahora se anuncia con su nombre en TalkBack."),
        ),
        Version(
            30, "2.5.3", "21 nov 2026", "Revisión visual",
            added = listOf("Fichas: primero las de la materia del libro (Derecho, Psicología, Ingeniería…); el resto con «Más tipos»."),
            fixed = listOf(
                "«¿Dónde me quedé?» decía «hace 20778 días» cuando no sabía la fecha.",
                "El botón 💭 (libreta) ya no tapa el texto: pasó a la barra de abajo.",
                "Las herramientas a pantalla completa quedaban debajo de la barra de estado.",
                "Al buscar con el teclado abierto, la barra de abajo y «Agregar» tapaban los resultados.",
                "Mapas: los nombres ya no se enciman y el mapa de la clase se ve entero y centrado (antes quedaba amontonado en un borde).",
                "Mapa entre documentos: sin palabras de relleno ni de los títulos, y sin la tabla con porcentajes rotos.",
                "Burbujas: las palabras largas se parten con guion en vez de salirse del círculo.",
                "Sala de estudio: el resumen de hoy se achica en las pestañas para dejar más lugar.",
                "Ajustes: «Con guiones» ya no sale cortado; márgenes y espacio entre párrafos con nombres claros.",
                "Estadísticas: mensaje cuando todavía no hay datos en vez de un gráfico vacío.",
                "Lista de palabras: primero las importantes y al final las comunes (la, de, el…).",
            ),
        ),
        Version(
            29, "2.5.2", "20 nov 2026", "Conceptos más certeros y editables",
            added = listOf(
                "Mapa y burbujas: «+ Agregar concepto» para meter uno que Hilo dejó afuera (por ejemplo «comunismo» o «lucha de clases»); lo cuenta en todo el texto y queda fijado.",
                "Al tocar un concepto: Fijar, Cambiar nombre, Unir con otro y Quitar. «Deshacer mis cambios» deja el mapa como lo armó Hilo.",
            ),
            fixed = listOf(
                "El mapa ya no se llena de palabras de relleno como «siguiente», «anterior», «paso», «total», «número» o «resultado».",
                "Los conceptos con forma de concepto (comunismo, democracia, alienación, sociología) suben; los verbos y participios bajan.",
                "Las frases clave ya no salen cortadas («intervalo de» → «intervalo de confianza»).",
            ),
        ),
        Version(
            28, "2.5.1", "20 nov 2026", "Arreglos",
            added = listOf("Ajustes → «Color en las letras»: interruptor para apagar el color de las líneas y leer con letra de un solo color. La intensidad ahora baja hasta 20 %."),
            fixed = listOf(
                "Con la letra grande del celular, la barra de abajo se partía letra por letra y el botón «Agregar» la tapaba.",
                "Las versiones nuevas se instalan encima de la anterior sin desinstalar (firma fija).",
            ),
        ),
        Version(
            27, "2.5", "20 nov 2026", "Áreas, diccionarios y herramientas",
            added = listOf(
                "Áreas de referencia: junta todos los documentos de un campo (por ejemplo Derecho hondureño) y Hilo aprende su estructura: jerarquía de normas, quién cita a quién, definiciones, plazos, sanciones, autoridades y el vocabulario propio del campo. Las clases pueden usar áreas.",
                "«Buscar en mis áreas» al elegir un texto: si nombra un artículo de una ley que tienes, lo abre directo; si no, trae los párrafos más parecidos.",
                "Plantillas de escritos para Derecho: guarda modelos, genera una plantilla de Word y revisa un borrador contra el modelo.",
                "Diccionarios de español (210 000 palabras) e inglés (665 000) del Wikcionario, descargables dentro de Hilo y sin internet después: definiciones, ejemplos, pronunciación, origen y traducción. Reconoce palabras conjugadas y plurales.",
                "Herramientas para la carrera: 14 de electricidad y electrónica (Ohm, código de colores, serie/paralelo, divisor, RC/RL, RLC y resonancia, potencia mono y trifásica con corrección del factor de potencia, LED, 555, dB, AWG, prefijos, bases y complemento a 2, tabla de verdad), calculadora científica, unidades, ecuaciones y sistemas.",
                "Psicología: estadística descriptiva, correlación de Pearson y prueba t con el resultado en formato APA, generador de referencias APA 7, resumen de corrientes y autores, y fichas de investigación, caso clínico y teoría.",
                "Programación: probador de expresiones regulares y tarjetas de código con colores.",
                "Formulario por clase con fórmulas dibujadas como en el libro (sin internet) y que se pueden calcular.",
                "Tarjetas nuevas: problema paso a paso, fórmula e imagen tapada (tapas partes de un circuito, del cerebro o de un mapa y repasas cada una).",
                "Glosario más bonito y fácil de leer, con búsqueda, y sin «definiciones» falsas como «Esta valoración es…».",
            ),
            fixed = listOf("El glosario ya no toma como término frases que empiezan con «esta», «esto», «la situación» y parecidas."),
        ),
        Version(
            26, "2.4", "20 nov 2026", "Clases",
            added = listOf(
                "Clases: crea una por materia y agrégale libros, documentos y fotos de la pizarra o de tus apuntes (Hilo reconoce su texto).",
                "Mapa de la clase: los conceptos de todos los materiales en una sola red, agrupados solos en temas de colores. Toca un concepto para ver en qué materiales aparece y cómo lo define cada uno.",
                "Síntesis por temas: conceptos, definiciones, ideas centrales de cada texto y tus subrayados; se exporta a Word. También una hoja de repaso de una página.",
                "Estudiar la clase: mazo con todas sus tarjetas, semáforo por tema, simulacro con preguntas armadas de tus materiales y plan de lectura hasta el examen.",
                "Diario de clase con fecha, grabación de audio y ★ «Va para el examen», y calendario de exámenes, entregas y lecturas.",
                "Más inteligencia sin internet: los resúmenes («¿Dónde me quedé?», preguntas de control, síntesis) eligen las frases centrales del texto con TextRank, y los conceptos de una clase se ordenan por su peso en todos los materiales.",
            ),
            fixed = emptyList(),
        ),
        Version(
            25, "2.3", "19 nov 2026", "Ayudas para concentrarte",
            added = listOf(
                "Botón del cerebro 🧠 en el lector: todas las ayudas para el TDAH en un solo panel, ordenadas en Empezar, Mientras lees, Tiempo, Ambiente y Motivación.",
                "Empezar: «Solo 2 min» en Sigue leyendo; «¿Dónde me quedé?» con las ideas de lo último que leíste al volver a un libro; meta diaria en minutos con celebración.",
                "Mientras lees: foco de un párrafo, voz que marca cada palabra (karaoke), desplazamiento automático tipo teleprompter, preguntas de control cada pocas páginas y libreta de distracciones (💭).",
                "Tiempo: temporizador visual que se vacía, cuánto falta para terminar el capítulo y el libro según tu ritmo, y aviso suave de pausa tras mucho rato seguido.",
                "Plan según tu energía: eliges poca, media o mucha y Hilo te propone qué hacer.",
                "Ambiente: ruido marrón, rosa, blanco o lluvia hechos por el celular, sin internet; y «Leer con un amigo»: arma la invitación y empieza el temporizador a la misma hora.",
                "Racha amable: días seguidos leyendo o repasando, con 2 días de descanso por semana que no la rompen.",
                "Botón «?» en cada apartado (biblioteca, lector, ajustes, cada pestaña de la sala de estudio, repaso, actividades y menús) que abre la explicación de esa parte.",
                "Guía: nueva sección «Ayudas TDAH» y temas para cada pestaña de estudio.",
            ),
            fixed = emptyList(),
        ),
        Version(
            24, "2.2", "19 nov 2026", "Todas las palabras",
            added = listOf(
                "Palabras: ahora están TODAS las palabras del documento en una lista con buscador, también las comunes (de, la, que…).",
                "Toca el ojo de una palabra para ponerla o sacarla de las burbujas. Las fijadas quedan siempre, con borde blanco.",
                "Elige cuántas burbujas ver (25, 45 u 80) y activa «Comunes» para incluir artículos y conectores.",
                "«¿Cómo elige Hilo las palabras?»: la explicación de qué cuenta y qué deja aparte.",
                "Al tocar una palabra: En contexto (cada aparición con lo que va antes y después), Va junto a… (palabras que salen en la misma oración), En el libro (dónde se concentra, del inicio al final) y Dónde aparece.",
                "Unir con…: junta variantes (trabajo, trabajador, laboral) para que cuenten como una sola.",
                "Exportar la lista de palabras con sus veces y porcentaje (para Excel o tus trabajos).",
            ),
            fixed = listOf("Las burbujas mostraban solo las 45 palabras más repetidas y las demás no se podían ver."),
        ),
        Version(
            23, "2.1.1", "19 nov 2026", "Elige qué formatos ver",
            added = listOf(
                "Ajustes → Formatos que se ven: oculta de la biblioteca los formatos que no quieres (por ejemplo HTML). No se borra nada; vuelven al tocarlos otra vez.",
                "La biblioteca avisa cuántos documentos están ocultos por su formato, y los filtros tienen un atajo a este ajuste.",
            ),
            fixed = listOf(
                "Desplazarse rápido por párrafos enormes ya no traba la app.",
            ),
        ),
        Version(
            22, "2.1", "19 nov 2026", "Elegir texto como en ReadEra",
            added = listOf(
                "Mantén el dedo sobre una palabra: queda elegida con dos manijas que puedes arrastrar, en el texto y en la página original del PDF.",
                "Menú pequeño junto al texto elegido: Copiar, Subrayar (con los colores por tipo), Nota, Traducir, Diccionario y Más (estudiar, citar, Wikipedia, buscar en internet, compartir, elegir la oración o el párrafo).",
                "El panel «Estudiar este fragmento» ya no se abre solo: está en Más → Estudiar, y viene con lo que elegiste ya marcado.",
                "Panel de estudio rediseñado: el fragmento arriba, tarjetas (Completar, Pregunta, Vocabulario, Explícalo), subrayar, buscar y guardar en secciones claras.",
                "Nota: escribe lo que piensas sobre el texto elegido; se guarda en tus Apuntes (o pegada en la página del PDF).",
                "Guía nueva y detallada: buscador, temas por sección y, en cada uno, para qué sirve, el paso a paso y consejos.",
            ),
            fixed = listOf(
                "El panel de estudio ya no tapa la página cada vez que mantienes el dedo sobre el texto.",
            ),
        ),
        Version(
            21, "2.0.1", "19 nov 2026", "Mapa de conceptos con cifras",
            added = listOf(
                "Cada círculo del mapa muestra cuántas veces aparece la palabra en el documento.",
                "Tabla de frecuencias debajo del mapa: veces y porcentaje del texto, como en los programas de análisis cualitativo.",
                "Al tocar un concepto: «Quitar del mapa» si no es un concepto (se puede deshacer).",
            ),
            fixed = listOf(
                "La sala de estudio mostraba el mapa del primer libro que abriste aunque entraras desde otro. Ahora cada documento tiene el suyo.",
                "El mapa ya no cuenta palabras de relleno como «Véase», «precisamente» o «claramente».",
            ),
        ),
        Version(
            20, "2.0", "18 nov 2026", "Estudiar con método",
            added = listOf(
                "Ruta de estudio en 6 pasos (prueba previa, lectura guiada, notas Cornell, dudas, resumen con tus palabras, mapa propio, recuerdo libre, preguntas Bloom, aplicar, simulacro y cierre). Es la primera pestaña de la sala de estudio y un semáforo te dice qué toca.",
                "Repaso con FSRS: el mismo método de Anki moderno, que calcula cuándo vas a olvidar cada tarjeta. Las tarjetas que te cuestan se marcan como frágiles.",
                "Calibración: antes de ver la respuesta dices qué tan seguro estás, y Hilo te muestra si te sobreestimas.",
                "Repaso intercalado: las tarjetas de varios temas se mezclan para que el cerebro aprenda a distinguirlos.",
                "Simulacro de examen con tiempo y estadísticas de lectura, repaso, aciertos y retención por día.",
                "Subrayado con tipo: definición, tesis, ejemplo, crítica, dato o duda, cada uno con su color.",
                "Importar mazos de Anki (.apkg) o listas .txt/.csv.",
                "Derecho: fichas IRAC, de sentencia, norma y caso; pirámide normativa; calculadora de plazos con feriados de Honduras.",
                "Sociología: fichas de autor, concepto, método y clásico; 18 autores clave, paradigmas, escuelas y línea de tiempo; comparador de autores.",
                "Exportar citas en BibTeX y apuntes a Word.",
                "Cómics y mangas CBZ y CBR, con zoom y lectura de derecha a izquierda.",
                "Buscar una palabra en todos tus libros a la vez, aunque escribas sin tildes.",
                "Filtros combinados en la biblioteca (formato, estado, favoritos, colección) y aviso de libros duplicados.",
                "Bienvenida con perfiles de lectura (atención, dislexia, letra grande, estudio). Cámbialo en Ajustes → Perfil de lectura.",
                "Modo simple: solo lo esencial a la vista; lo demás en «Más herramientas».",
                "Modo enfoque: pantalla completa, sin porcentaje y con las notificaciones silenciadas mientras lees.",
                "Barra inferior en la biblioteca: Biblioteca, Leyendo, Estudiar y Ajustes.",
                "Deshacer al borrar tarjetas, subrayados, notas y libros.",
                "Vibración suave al pasar de página y al responder tarjetas.",
                "Guía de gestos la primera vez que abres un libro.",
            ),
            fixed = listOf(
                "Si el celular tiene las animaciones desactivadas, Hilo tampoco anima las burbujas de palabras.",
                "Si Hilo se cierra de golpe, al volver a abrirlo te ofrece enviar el informe. Toca 5 veces el título de Novedades para ver el diagnóstico.",
                "Los PDF preparan la página siguiente antes de que llegues.",
            ),
        ),
        Version(
            17, "1.7", "18 nov 2026", "Leer como en un libro",
            added = listOf(
                "EPUB, MOBI, Word y los demás formatos se ven como la página de un libro: texto justificado, guiones al cortar palabras, sangría y sin huecos entre párrafos. Puedes volver a los párrafos separados en Ajustes → Forma del texto.",
                "Fuentes nuevas: Literata, Comfortaa, Lexend y Atkinson Hyperlegible (pensada para leer con menos esfuerzo). Cada una se muestra con su propia letra.",
                "Grosor de la letra: de fina a gruesa.",
                "Libros MOBI y AZW3 de Kindle (sin DRM).",
                "Dos páginas con el celular acostado también en el texto (EPUB, MOBI, Word…).",
                "PDF sin franjas negras: el fondo toma el color del papel y «Recortar márgenes» quita el borde blanco para que la letra se vea más grande.",
                "Biblioteca en lista, como ReadEra: portada, formato, tamaño, avance y botones rápidos. La vista de portadas sigue en Ajustes → Vista de la biblioteca.",
                "Elegir varios libros: mantén el dedo sobre uno y marca otros para compartirlos, pasarlos a favoritos, a una colección, marcarlos como leídos o mandarlos a la papelera.",
                "Carpetas que no leer: en Menú → Carpetas apaga, por ejemplo, la carpeta de WhatsApp. Sus documentos salen de Hilo sin borrarse del celular.",
                "Este apartado de Novedades.",
            ),
            fixed = listOf(
                "Los libros cuyo nombre era un número (como «4 57714757040215178 40») toman el título guardado dentro del PDF.",
                "La primera búsqueda en el celular ya no marca cientos de documentos como «Nuevo». Si ya los tienes así, en Nuevos toca «Marcar todos como vistos».",
                "El texto en páginas ya no corta un párrafo en dos trozos distintos: la línea siguiente continúa en la otra página con el mismo formato.",
                "Ajustes de lectura ordenados por secciones: Página, Letra, Ayudas, PDF, Biblioteca y Toques.",
            ),
        ),
        Version(
            16, "1.6", "17 nov 2026", "Más estable y más rápido",
            added = listOf("Pruebas automáticas de depuración y de estrés con documentos enormes, miles de tarjetas y miles de toques al azar."),
            fixed = listOf(
                "Una oración con una lista muy larga de artículos («artículos 1, 2, 3…») cerraba la app.",
                "En Android 11 o más nuevo, el sistema bloqueaba la voz: la lectura en voz alta y el examen oral podían no funcionar.",
                "Al analizar libros muy grandes la pantalla se trababa unos segundos.",
                "El análisis de un libro enorme pasó de no terminar a tardar unos 9 segundos; un PDF de 400 páginas, unos 23.",
                "Guardar la biblioteca, crear muchas tarjetas de una vez y desplazar el texto ya no traban la pantalla.",
                "El plan de examen ya no tapa las secciones de la sala de estudio.",
            ),
        ),
        Version(
            15, "1.5", "17 nov 2026", "Derecho y Sociología",
            added = listOf(
                "Leyes y códigos: índice por libros, títulos y capítulos, «Ir al artículo», artículos enlazados entre sí y tarjetas de artículos.",
                "Tabla de plazos de la ley.",
                "Autores con sus conceptos y cuadro comparativo entre autores.",
                "Línea de tiempo con las fechas del texto.",
                "Fichas de caso, de lectura y de autor.",
                "Citas en formato APA con su página.",
                "Modo Feynman (explícalo con tus palabras) y examen oral.",
                "Fecha de examen con plan diario, recordatorio diario, exportar a Anki y copia de seguridad.",
                "Mapa de conceptos entre tus documentos y diccionario jurídico de la RAE.",
            ),
            fixed = emptyList(),
        ),
        Version(
            14, "1.4", "17 nov 2026", "Entender el texto",
            added = listOf(
                "Nube de palabras flotante: quita las que no importan.",
                "Glosario automático, mapa de conceptos y guía de estudio con preguntas por capítulo.",
                "Buscar una palabra en el diccionario, en Wikipedia o en el traductor.",
            ),
            fixed = listOf("El mapa de conceptos ya no amontona los círculos."),
        ),
        Version(
            13, "1.3", "17 nov 2026", "Sala de estudio",
            added = listOf(
                "Tarjetas con repaso espaciado (como Anki) para cada documento.",
                "Resaltados, apuntes, buscar en el libro y artículos de leyes.",
                "Hilo encuentra solo los documentos del celular, como ReadEra.",
            ),
            fixed = listOf("El texto en páginas a veces se quedaba en «Armando las páginas…»."),
        ),
        Version(
            12, "1.2", "17 nov 2026", "Como un lector de verdad",
            added = listOf(
                "Pasar páginas en horizontal, como un libro.",
                "Modos de color: Día, Noche, Sepia, Crepúsculo, Consola y más.",
                "Marcadores, índice, voz, pantalla completa y brillo deslizando.",
                "Biblioteca con secciones y formatos nuevos: Word, ODT, FB2, HTML y Markdown.",
            ),
            fixed = emptyList(),
        ),
        Version(
            11, "1.1", "16 nov 2026", "Detalles",
            added = emptyList(),
            fixed = listOf(
                "Los títulos ya no se pegan al primer párrafo en el modo Texto.",
                "Los textos cortos ya no aparecen como «Terminado» sin leerlos.",
                "La barra para anotar el PDF entra completa en la pantalla.",
            ),
        ),
        Version(
            10, "1.0", "16 nov 2026", "Hilo nace",
            added = listOf(
                "Colores que pasan de una línea a la siguiente para no perder el hilo, y la regla de lectura.",
                "PDF con su formato original y anotaciones encima: resaltar, lápiz y notas.",
                "Modo Texto con interlineado compacto, negrita guía y palabra a palabra.",
                "Temporizador de lectura.",
            ),
            fixed = emptyList(),
        ),
    )

    val current get() = versions.first()
}
