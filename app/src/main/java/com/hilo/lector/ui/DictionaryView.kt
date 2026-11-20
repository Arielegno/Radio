package com.hilo.lector.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hilo.lector.HiloViewModel
import com.hilo.lector.data.Dict
import com.hilo.lector.data.DictEntry
import com.hilo.lector.data.StudyCard
import com.hilo.lector.data.today
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Para abrir el diccionario desde cualquier botón «Diccionario» de la app. */
object DictUi {
    var open: ((word: String, preferEn: Boolean) -> Unit)? = null
}

/** Ficha del diccionario dentro de Hilo, sin internet. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DictionarySheet(vm: HiloViewModel, start: String, preferEn: Boolean, onDismiss: () -> Unit) {
    val c = LocalHilo.current
    val ctx = LocalContext.current
    var word by remember(start) { mutableStateOf(start.trim().split(Regex("\\s+")).take(3).joinToString(" ")) }
    var query by remember(start) { mutableStateOf(word) }
    val ready = vm.dictVersion.let { Dict.any(ctx) }
    val results by produceState<List<DictEntry>?>(null, query, vm.dictVersion) {
        value = withContext(Dispatchers.IO) { if (Dict.any(ctx)) Dict.lookupAny(ctx, query, preferEn) else emptyList() }
    }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), containerColor = c.surface) {
        Column(
            Modifier.padding(horizontal = 18.dp).padding(bottom = 28.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row {
                Text("Diccionario", fontFamily = Fonts.display, fontWeight = FontWeight.Bold, fontSize = 22.sp, color = c.ink, modifier = Modifier.weight(1f))
                HelpButton(vm, "diccionario")
            }
            OutlinedTextField(
                word, { word = it }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Escribe una palabra") },
                trailingIcon = { TextButton(onClick = { query = word }) { Text("Buscar") } },
            )
            if (!ready) {
                DictDownloads(vm)
            } else {
                val r = results
                when {
                    r == null -> LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    r.isEmpty() -> {
                        Text("No encontré «$query» en los diccionarios descargados.", color = c.muted, fontSize = 14.sp)
                        val sug by produceState(emptyList<String>(), query) {
                            value = withContext(Dispatchers.IO) { (Dict.suggest(ctx, query.take(max(2, query.length - 2)), "es", 8) + Dict.suggest(ctx, query.take(max(2, query.length - 2)), "en", 6)).distinct() }
                        }
                        if (sug.isNotEmpty()) {
                            Text("¿Quisiste decir?", color = c.muted, fontSize = 13.sp)
                            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                sug.forEach { s -> Text(s, color = c.ink, modifier = Modifier.clip(RoundedCornerShape(50)).background(c.chip).clickable { word = s; query = s }.padding(horizontal = 12.dp, vertical = 6.dp)) }
                            }
                        }
                    }
                    else -> r.forEach { e -> EntryCard(vm, e) { w -> word = w; query = w } }
                }
                if (!Dict.has(ctx, "es") || !Dict.has(ctx, "en")) TextButton(onClick = { vm.sheet = "settings" }) {
                    Text("Descargar también el de ${if (!Dict.has(ctx, "es")) "español" else "inglés"} (Ajustes → Diccionarios)")
                }
            }
            Text("En internet:", color = c.muted, fontSize = 12.sp)
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { lookup(ctx, query, "web-dic", if (preferEn) "en" else "es") }) { Text(if (preferEn) "Cambridge" else "RAE", color = c.ink) }
                OutlinedButton(onClick = { lookup(ctx, query, "jur", "es") }) { Text("Jurídico (RAE)", color = c.ink) }
                OutlinedButton(onClick = { lookup(ctx, query, "wiki", if (preferEn) "en" else "es") }) { Text("Wikipedia", color = c.ink) }
            }
            Text("Diccionarios del Wikcionario (CC BY-SA 4.0).", color = c.muted, fontSize = 11.sp)
        }
    }
}

private fun max(a: Int, b: Int) = if (a > b) a else b

@Composable
private fun EntryCard(vm: HiloViewModel, e: DictEntry, onWord: (String) -> Unit) {
    val c = LocalHilo.current
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(c.bg).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row {
            Text(e.word, color = c.ink, fontWeight = FontWeight.Bold, fontSize = 20.sp, modifier = Modifier.weight(1f))
            Text(if (e.lang == "en") "inglés" else "español", color = c.accent, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp))
        }
        val sub = listOfNotNull(e.pos.takeIf { it.isNotBlank() }, e.ipa.takeIf { it.isNotBlank() }).joinToString(" · ")
        if (sub.isNotBlank()) Text(sub, color = c.muted, fontSize = 13.sp, fontStyle = FontStyle.Italic)
        if (e.from != null) Text("«${e.from}» es una forma de «${e.word}».", color = c.muted, fontSize = 13.sp)
        e.senses.forEachIndexed { i, s ->
            Column {
                Text(
                    "${i + 1}. " + (if (s.tags.isNotEmpty()) "(${s.tags.joinToString(", ")}) " else "") + s.def,
                    color = c.ink, fontSize = 15.sp, lineHeight = 21.sp,
                )
                if (s.example.isNotBlank()) Text("«${s.example}»", color = c.muted, fontSize = 13.sp, fontStyle = FontStyle.Italic, modifier = Modifier.padding(start = 14.dp))
            }
        }
        if (e.tr.isNotBlank()) {
            Text(if (e.lang == "en") "En español:" else "En inglés:", color = c.accent, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Text(e.tr.split(" | ").take(4).joinToString("; "), color = c.ink, fontSize = 14.sp)
            if (e.lang == "en") Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                e.tr.split(", ").take(6).forEach { t -> Text(t, color = c.ink, fontSize = 13.sp, modifier = Modifier.clip(RoundedCornerShape(50)).background(c.chip).clickable { onWord(t) }.padding(horizontal = 10.dp, vertical = 5.dp)) }
            }
        }
        if (e.ety.isNotBlank()) Text("Origen: ${e.ety.take(260)}", color = c.muted, fontSize = 12.sp)
        TextButton(onClick = {
            val back = e.senses.take(2).joinToString("\n") { it.def }.ifBlank { e.tr }
            vm.study.add(StudyCard(vm.study.newId(), "vocabulario", "vocab", e.word, back, lang = e.lang, source = "Diccionario", due = today(), created = System.currentTimeMillis()))
            vm.toast = "Agregada a tu vocabulario."
        }) { Text("+ A mis tarjetas de vocabulario") }
    }
}

/** Descargar o borrar los diccionarios. */
@Composable
fun DictDownloads(vm: HiloViewModel) {
    val c = LocalHilo.current
    val ctx = LocalContext.current
    @Suppress("UNUSED_VARIABLE") val v = vm.dictVersion
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            "Descárgalos una vez y funcionan sin internet. Vienen del Wikcionario: definiciones, ejemplos, pronunciación, origen y traducción entre español e inglés.",
            color = c.muted, fontSize = 13.sp, lineHeight = 18.sp,
        )
        Dict.NAMES.forEach { (lang, name) ->
            val prog = vm.dictProgress[lang]
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(c.band).padding(12.dp)) {
                Column(Modifier.weight(1f)) {
                    Text("Diccionario de $name", color = c.ink, fontWeight = FontWeight.SemiBold)
                    Text(
                        when {
                            prog != null -> "Descargando… ${(prog * 100).toInt()} %"
                            Dict.has(ctx, lang) -> "Listo · ${Dict.sizeMb(ctx, lang)} MB en el celular"
                            else -> if (lang == "es") "210 000 palabras · descarga de 27 MB" else "665 000 palabras · descarga de 88 MB"
                        },
                        color = c.muted, fontSize = 12.sp,
                    )
                    if (prog != null) LinearProgressIndicator(progress = { prog }, modifier = Modifier.fillMaxWidth().height(4.dp).padding(top = 4.dp))
                }
                when {
                    prog != null -> {}
                    Dict.has(ctx, lang) -> TextButton(onClick = { Dict.delete(ctx, lang); vm.dictVersion++ }) { Text("Borrar", color = c.lines[0]) }
                    else -> Button(onClick = { vm.downloadDict(lang) }, colors = ButtonDefaults.buttonColors(containerColor = c.accent, contentColor = c.accentInk)) { Text("Descargar") }
                }
            }
        }
        Text("Usa wifi: son archivos grandes. Si se corta, vuelve a tocar «Descargar».", color = c.muted, fontSize = 12.sp)
    }
}
