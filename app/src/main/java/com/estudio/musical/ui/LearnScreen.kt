package com.estudio.musical.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.estudio.musical.audio.MotorAudio
import com.estudio.musical.audio.synth.Instrumentos
import com.estudio.musical.music.Acorde
import com.estudio.musical.music.Teoria
import androidx.compose.foundation.verticalScroll
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private data class Leccion(val titulo: String, val texto: String)

private val LECCIONES = listOf(
    Leccion("Las 12 notas",
        "En la música occidental hay 12 notas que se repiten: Do, Do#, Re, Re#, Mi, Fa, Fa#, Sol, Sol#, La, La#, Si. " +
        "La distancia entre una y la siguiente es un semitono. Dos semitonos forman un tono. " +
        "Entre Mi–Fa y Si–Do solo hay medio tono (por eso no existe Mi# ni Si# en la práctica)."),
    Leccion("¿Qué es una escala?",
        "Una escala es una selección de 7 de esas 12 notas siguiendo un patrón.\n" +
        "Escala mayor (alegre): T – T – S – T – T – T – S.\n" +
        "Escala menor natural (melancólica): T – S – T – T – S – T – T.\n" +
        "Ejemplo: Do mayor = Do Re Mi Fa Sol La Si (solo teclas blancas del piano)."),
    Leccion("¿Qué es la tonalidad?",
        "La tonalidad es la escala sobre la que está construida tu canción y su nota \"casa\" (tónica). " +
        "Si tu canción está en Sol mayor, la melodía y los acordes usan sobre todo las notas de esa escala " +
        "y la canción se siente resuelta al volver a Sol."),
    Leccion("Acordes",
        "Un acorde es tocar 3 o más notas a la vez. El acorde básico (tríada) se forma saltando notas de la escala: " +
        "1ª, 3ª y 5ª. Do + Mi + Sol = Do mayor. La + Do + Mi = La menor.\n" +
        "Mayor: 4 semitonos + 3 semitonos. Menor: 3 + 4."),
    Leccion("Grados y progresiones",
        "Cada acorde de la tonalidad se numera: I, ii, iii, IV, V, vi, vii°. Las progresiones más usadas:\n" +
        "• I – V – vi – IV (pop: Do – Sol – Lam – Fa)\n" +
        "• I – IV – V (rock, ranchera, blues)\n" +
        "• vi – IV – I – V (baladas)\n" +
        "• ii – V – I (jazz)\n" +
        "Usa el explorador de abajo para verlas en cualquier tono."),
    Leccion("Relativa mayor/menor",
        "Cada tonalidad mayor comparte notas con una menor, 3 semitonos abajo: Do mayor ↔ La menor, " +
        "Sol mayor ↔ Mi menor. Por eso el detector a veces duda entre las dos: escucha cuál suena a \"final\"."),
    Leccion("Ritmo y compás",
        "El compás agrupa los pulsos. 4/4 (cuatro tiempos) es el más común en pop y rock. " +
        "3/4 es de vals y muchas rancheras. 6/8 se siente \"balanceado\" (baladas, bachata en algunos casos). " +
        "El tempo se mide en BPM (pulsos por minuto): 60–80 lento, 90–110 medio, 120+ rápido."),
    Leccion("Melodía y letra",
        "Para que la letra cante bien: pon las sílabas acentuadas en los tiempos fuertes (1 y 3 en 4/4). " +
        "El coro suele estar más agudo que el verso y repetir la frase principal (el gancho). " +
        "Las notas 1, 3 y 5 de la escala suenan estables; las demás crean tensión que quiere resolver.")
)

private val PASOS_GRABACION = listOf(
    "Prepara la canción" to "Escribe la letra en Letras con sus acordes ([Do], [Sol]…), define la tonalidad y el tempo. Usa el Metrónomo con “Tap tempo” si no sabes el BPM.",
    "Crea el proyecto" to "En Estudio toca + y crea un proyecto con el nombre, tono y tempo de tu canción.",
    "Haz la base" to "En Estudio → Crear pista elige estilo y progresión: la app genera batería, bajo y acordes en tu tonalidad. O graba tu guitarra con el metrónomo activado.",
    "Elige el lugar" to "Un cuarto pequeño con muebles, cortinas o ropa absorbe el eco. Evita baños y cocinas. Apaga ventiladores.",
    "Conecta el micrófono" to "Micro USB tipo C o interfaz de audio con adaptador OTG. Selecciónalo en Grabar → Entrada de audio. Bluetooth solo para maquetas.",
    "Calibra la latencia" to "Una sola vez: Más → Ajustes → Calibrar automáticamente. Así lo que grabes encima de la base queda en tiempo.",
    "Posición y nivel" to "Voz a 15–20 cm del micro con filtro antipop. Canta la parte más fuerte mirando el vúmetro: 60–80 % sin llegar al rojo.",
    "Graba la voz encima" to "En Estudio toca “Grabar encima”. Con audífonos, graba 3–4 tomas completas y agrega la mejor al proyecto (botón de lista en Mis tomas).",
    "Edita la voz" to "En Editar: recorta, limpia ruido, quita retumbe, ecualiza (agudos +2), comprime (umbral −18, 4:1) y añade reverb suave (25 %) o eco (300 ms).",
    "Mezcla" to "En Estudio ajusta volúmenes: la voz debe entenderse por encima de todo. Abre un poco los acordes a un lado y la melodía al otro con el paneo.",
    "Revisa y exporta" to "Escucha en audífonos, en la bocina y en el carro. Luego Exportar: obtienes WAV (calidad máxima) y M4A para compartir.",
    "Guarda un respaldo" to "Más → Ajustes → Crear respaldo. Guarda el .zip en una USB o tu computadora: tu música queda solo contigo."
)

@Composable
fun LearnScreen(modifier: Modifier) {
    var tab by remember { mutableIntStateOf(0) }
    Column(modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = tab) {
            Tab(tab == 0, { tab = 0 }, text = { Text("Teoría") })
            Tab(tab == 1, { tab = 1 }, text = { Text("Explorador") })
            Tab(tab == 2, { tab = 2 }, text = { Text("Cómo grabar") })
        }
        when (tab) {
            0 -> LazyColumn(Modifier.padding(16.dp)) {
                items(LECCIONES) { ExpandCard(it.titulo, it.texto) }
            }
            1 -> ScaleExplorer()
            2 -> LazyColumn(Modifier.padding(16.dp)) {
                item { SectionTitle("Graba tu canción paso a paso") }
                items(PASOS_GRABACION.withIndex().toList()) { (i, p) ->
                    var hecho by remember { mutableStateOf(false) }
                    Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Row(Modifier.padding(12.dp)) {
                            Checkbox(checked = hecho, onCheckedChange = { hecho = it })
                            Column {
                                Text("${i + 1}. ${p.first}", fontWeight = FontWeight.Bold)
                                Text(p.second, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ExpandCard(title: String, body: String) {
    var open by remember { mutableStateOf(false) }
    Card(Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { open = !open }.animateContentSize()) {
        Column(Modifier.padding(12.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            if (open) Text(body, Modifier.padding(top = 6.dp), style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun ScaleExplorer() {
    val scope = rememberCoroutineScope()
    var tonica by remember { mutableIntStateOf(0) }
    var mayor by remember { mutableStateOf(true) }
    val escala = Teoria.escala(tonica, mayor)
    Column(Modifier.padding(16.dp).verticalScroll(rememberScrollState())) {
        Row(Modifier.horizontalScroll(rememberScrollState())) {
            Teoria.NOTAS.forEachIndexed { i, n ->
                FilterChip(selected = tonica == i, onClick = { tonica = i }, label = { Text(n) }, modifier = Modifier.padding(end = 4.dp))
            }
        }
        Row {
            FilterChip(mayor, { mayor = true }, label = { Text("Mayor") }, modifier = Modifier.padding(end = 8.dp))
            FilterChip(!mayor, { mayor = false }, label = { Text("Menor") })
        }
        Text(Teoria.nombreTono(tonica, mayor), style = MaterialTheme.typography.headlineMedium)
        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Text("Escala: " + escala.joinToString(" – ") { Teoria.NOTAS[it] }, Modifier.weight(1f))
            TextButton(onClick = {
                scope.launch {
                    val notas = (if (mayor) Teoria.MAYOR else Teoria.MENOR).map { 60 + tonica + it } + (72 + tonica)
                    for (n in notas) { MotorAudio.notaOn(Instrumentos.PIANO, n); delay(330); MotorAudio.notaOff(n) }
                }
            }) { Text("▶ Escuchar") }
        }
        PianoTeclado(desde = 48, octavas = 2, resaltadas = escala.toSet(), raiz = tonica,
            onNotaOn = { MotorAudio.notaOn(Instrumentos.PIANO, it) }, onNotaOff = { MotorAudio.notaOff(it) })
        Spacer(Modifier.height(8.dp))
        Text("En la guitarra (toca el diapasón para oír cada nota):", style = MaterialTheme.typography.labelLarge)
        Diapason(escala.toSet(), tonica) { MotorAudio.notaOn(Instrumentos.GUITARRA, it) }
        Spacer(Modifier.height(8.dp))
        Text("Acordes de la tonalidad (toca para escuchar):", fontWeight = FontWeight.Bold)
        Row(Modifier.horizontalScroll(rememberScrollState())) {
            Teoria.acordes(tonica, mayor).forEach { g ->
                val nombre = g.substringAfter("=")
                SuggestionChip(onClick = {
                    Acorde.parse(nombre)?.notas(60)?.forEach { MotorAudio.notaOn(Instrumentos.GUITARRA, it, 0.6f) }
                }, label = { Text(g.replace("=", "  ")) }, modifier = Modifier.padding(end = 4.dp))
            }
        }
        val relativa = if (mayor) "${Teoria.NOTAS[(tonica + 9) % 12]} menor" else "${Teoria.NOTAS[(tonica + 3) % 12]} mayor"
        Spacer(Modifier.height(8.dp))
        Text("Relativa: $relativa")
        val a = Teoria.acordes(tonica, mayor).map { it.substringAfter("=") }
        if (mayor) {
            Text("Pop (I–V–vi–IV): ${a[0]} – ${a[4]} – ${a[5]} – ${a[3]}")
            Text("Ranchera/rock (I–IV–V): ${a[0]} – ${a[3]} – ${a[4]}")
        } else {
            Text("Balada menor (i–VI–III–VII): ${a[0]} – ${a[5]} – ${a[2]} – ${a[6]}")
            Text("Andaluza (i–VII–VI–V): ${a[0]} – ${a[6]} – ${a[5]} – ${Teoria.NOTAS[(tonica + 7) % 12]}")
        }
    }
}
