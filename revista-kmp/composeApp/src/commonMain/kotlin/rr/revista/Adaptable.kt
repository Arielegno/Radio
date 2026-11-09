package rr.revista

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/* Pantallas de todos los tamaños: teléfono, tableta y computadora (Chrome, Edge, Firefox).
   Con más de 900 dp de ancho la barra de abajo pasa a ser un menú lateral y las listas
   se vuelven cuadrículas que agregan columnas según el espacio. */

/** Ancho disponible de la ventana (para que cada pantalla decida cuántas columnas usar). */
val LocalAncho = compositionLocalOf { 400.dp }

val Dp.esAncha: Boolean get() = this >= 900.dp

/** Manita del ratón al pasar por encima de algo que se puede tocar (en computadora). */
fun Modifier.mano(): Modifier = pointerHoverIcon(PointerIcon.Hand)

val SECCIONES = listOf(Triple("inicio", "casa", "Inicio"), Triple("boveda", "pelicula", "Bóveda"), Triple("posters", "imagen", "Pósters"), Triple("artistas", "nota", "Artistas"), Triple("mas", "menu", "Más"))

/** Menú lateral para pantallas anchas. */
@Composable
fun BarraLateral(actual: String, ir: (String) -> Unit) {
    Column(Modifier.width(230.dp).fillMaxHeight().background(Color(0xFF1B1E23)).verticalScroll(rememberScrollState()).padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(Modifier.padding(bottom = 18.dp, top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(38.dp).clip(RoundedCornerShape(12.dp)).background(Amarillo), contentAlignment = Alignment.Center) { Icono("nota", Color(0xFF1B1E23), 24.dp) }
            Spacer(Modifier.width(10.dp))
            Text("Revista", fontFamily = Fuentes.de("condensada"), fontWeight = FontWeight.ExtraBold, fontSize = 28.sp)
        }
        SECCIONES.forEach { (k, ic, n) ->
            val on = actual == k
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(if (on) Amarillo.copy(alpha = .16f) else Color.Transparent).mano().clickable { ir(k) }.padding(horizontal = 12.dp, vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) {
                Icono(ic, if (on) Amarillo else Tenue, 22.dp); Spacer(Modifier.width(12.dp))
                Text(n, color = if (on) Amarillo else TextoClaro, fontWeight = if (on) FontWeight.Bold else FontWeight.Medium, fontSize = 16.sp)
            }
        }
        Spacer(Modifier.height(10.dp))
        listOf(Triple("lupa", "Buscar", Destino.Buscar), Triple("chat", "Chats", Destino.Chats), Triple("libro", "Ediciones", Destino.Ediciones), Triple("guardar", "Guardados", Destino.Guardados)).forEach { (ic, n, d) ->
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).mano().clickable { if (d == Destino.Chats && !Nav.necesitaCuenta()) return@clickable; Nav.ir(d) }.padding(horizontal = 12.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
                Icono(ic, Tenue, 20.dp); Spacer(Modifier.width(12.dp)); Text(n, color = TextoClaro, fontSize = 15.sp)
            }
        }
        Spacer(Modifier.height(14.dp))
        Text("Escribir artículo", color = Color(0xFF1B1E23), fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
            .background(Brush.linearGradient(listOf(Color(0xFFFF8A3D), Color(0xFFFF5F9E)))).mano().clickable {
                if (Nav.necesitaCuenta()) { if (Cuenta.suspendida) Aviso.mostrar("Tu cuenta está suspendida: puedes leer, pero no publicar.", true) else Nav.ir(Destino.Editar(null)) }
            }.padding(horizontal = 14.dp, vertical = 12.dp))
    }
}

/** Esc cierra lo que esté abierto (visor, ventana o pantalla), como en cualquier página. */
fun teclaEscape(e: KeyEvent): Boolean {
    if (e.type != KeyEventType.KeyDown || e.key != Key.Escape) return false
    when {
        Visor.url != null -> Visor.url = null
        EditorFotos.actual != null -> EditorFotos.cerrar()
        Nav.dialogo != null -> Nav.dialogo = null
        Nav.pila.isNotEmpty() -> Nav.atras()
        else -> return false
    }
    return true
}
