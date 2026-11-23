package com.estudio.musical.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

private class Pagina(val icono: ImageVector, val titulo: String, val texto: String)

private val PAGINAS = listOf(
    Pagina(Icons.Filled.Mic, "Bienvenido a Tonalia",
        "Tu estudio de música de bolsillo. Graba con calidad de estudio usando el micrófono del teléfono, uno USB tipo C o Bluetooth."),
    Pagina(Icons.Filled.LibraryMusic, "Crea pistas en segundos",
        "Elige tonalidad y estilo, y Tonalia arma la batería, el bajo y los acordes. Luego graba tu voz encima y mezcla."),
    Pagina(Icons.Filled.Piano, "Instrumentos y tus propios sonidos",
        "Toca pianos, cuerdas y sintetizadores. Graba cualquier sonido y conviértelo en un instrumento afinado."),
    Pagina(Icons.Filled.EditNote, "Escribe y aprende",
        "Guarda tus letras con acordes, dicta versos, busca rimas y aprende teoría musical paso a paso."),
    Pagina(Icons.Filled.Lock, "Todo se queda contigo",
        "Tonalia funciona sin internet. Tus canciones nunca salen de tu teléfono, salvo que tú las compartas.")
)

@Composable
fun Bienvenida(onTerminar: () -> Unit) {
    val estado = rememberPagerState { PAGINAS.size }
    val scope = rememberCoroutineScope()
    val ultima = estado.currentPage == PAGINAS.lastIndex
    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF2A1360), Marca.Fondo, Marca.Fondo)))) {
        Column(Modifier.fillMaxSize().systemBarsPadding().padding(24.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                if (!ultima) TextButton(onClick = onTerminar) { Text("Saltar") }
            }
            HorizontalPager(estado, Modifier.weight(1f)) { i ->
                val p = PAGINAS[i]
                Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Box(Modifier.size(150.dp).clip(RoundedCornerShape(44.dp)).background(Marca.degradado), contentAlignment = Alignment.Center) {
                        Icon(p.icono, null, tint = Color.White, modifier = Modifier.size(76.dp))
                    }
                    Spacer(Modifier.height(40.dp))
                    Text(p.titulo, style = MaterialTheme.typography.headlineLarge, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(14.dp))
                    Text(p.texto, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Row(Modifier.fillMaxWidth().padding(vertical = 20.dp), horizontalArrangement = Arrangement.Center) {
                repeat(PAGINAS.size) { i ->
                    Box(Modifier.padding(4.dp).height(8.dp).width(if (i == estado.currentPage) 24.dp else 8.dp).clip(CircleShape)
                        .background(if (i == estado.currentPage) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant))
                }
            }
            Button(
                onClick = { if (ultima) onTerminar() else scope.launch { estado.animateScrollToPage(estado.currentPage + 1) } },
                modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(18.dp)
            ) { Text(if (ultima) "Empezar" else "Siguiente", style = MaterialTheme.typography.titleMedium) }
        }
    }
}
