package com.hilo.lector.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.sp
import com.hilo.lector.HiloViewModel
import com.hilo.lector.data.Book
import com.hilo.lector.data.ReaderSettings
import com.hilo.lector.text.Comic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.max

/** Lector de cómics y mangas: una imagen por página, con zoom y lectura de derecha a izquierda (manga). */
@Composable
fun ComicView(vm: HiloViewModel, book: Book, s: ReaderSettings, mode: ReadMode, bridge: ReaderBridge, onTapPlain: () -> Unit) {
    val c = LocalHilo.current
    val ctx = LocalContext.current
    val file = remember(book.id) { vm.repo.fileOf(book) }
    var prog by remember { mutableStateOf("") }
    // Páginas: nombres dentro del zip, o archivos ya extraídos si es un CBR
    val pages by produceState<List<String>?>(null, book.id) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                if (Comic.isRarFile(file)) {
                    Comic.extractRar(file, File(ctx.cacheDir, "comics/${book.id}")) { n, t -> prog = "Preparando $n de $t páginas…" }.map { it.path }
                } else Comic.pages(file)
            }.getOrDefault(emptyList())
        }
    }
    val list = pages
    if (list == null || list.isEmpty()) {
        Box(Modifier.fillMaxSize().background(mode.paper), contentAlignment = Alignment.Center) {
            if (list == null) Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp())) {
                CircularProgressIndicator(color = c.accent); Text(prog.ifBlank { "Abriendo el cómic…" }, color = mode.ink)
            } else Text("No encontré imágenes en este archivo.", color = mode.ink)
        }
        return
    }
    val rtl = vm.motor.flag("manga:${book.id}")
    val pager = rememberPagerState(initialPage = book.pos.coerceIn(0, list.size - 1)) { list.size }
    val scope = rememberCoroutineScope()
    var zoom by remember { mutableFloatStateOf(1f) }
    var pan by remember { mutableStateOf(Offset.Zero) }

    LaunchedEffect(pager) {
        snapshotFlow { pager.currentPage }.collectLatest { i ->
            zoom = 1f; pan = Offset.Zero
            bridge.position = i
            delay(600)
            vm.repo.update(book.id) { it.copy(pos = i, progress = (i + 1f) / list.size, pages = list.size, updated = System.currentTimeMillis(), done = it.done || i >= list.size - 1) }
        }
    }
    SideEffect {
        bridge.total = list.size
        bridge.unit = "Pág."
        bridge.seek = { p -> pager.scrollToPage(p.coerceIn(0, list.size - 1)) }
        bridge.goPage = { p -> pager.scrollToPage(p.coerceIn(0, list.size - 1)) }
        bridge.goPara = null
        bridge.here = { pager.currentPage to "Página ${pager.currentPage + 1}" }
        bridge.speakFrom = null
        bridge.words = null
        bridge.move = { d -> pager.animateScrollToPage((pager.currentPage + d).coerceIn(0, list.size - 1)) }
    }

    BoxWithConstraints(Modifier.fillMaxSize().background(mode.paper)) {
        val wPx = with(LocalDensity.current) { maxWidth.toPx() }.toInt()
        HorizontalPager(state = pager, reverseLayout = rtl, beyondViewportPageCount = 1, userScrollEnabled = zoom <= 1.01f, modifier = Modifier.fillMaxSize()) { i ->
            val bmp by produceState<Bitmap?>(null, i) {
                value = withContext(Dispatchers.IO) {
                    runCatching {
                        val name = list[i]
                        val bytes = if (name.startsWith("/")) File(name).readBytes() else Comic.page(file, name)
                        val o = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, o)
                        var sample = 1
                        while (o.outWidth / (sample * 2) >= wPx * 1.5f) sample *= 2
                        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, BitmapFactory.Options().apply { inSampleSize = sample })
                    }.getOrNull()
                }
            }
            Box(
                Modifier.fillMaxSize()
                    .pointerInput(i) {
                        detectTapGestures(onDoubleTap = { zoom = if (zoom > 1.2f) 1f else 2.2f; pan = Offset.Zero }) { o ->
                            val f = o.x / size.width
                            val fwd = if (rtl) f < 0.28f else f > 0.72f
                            val back = if (rtl) f > 0.72f else f < 0.28f
                            when {
                                s.tapTurn && fwd -> scope.launch { pager.animateScrollToPage((pager.currentPage + 1).coerceAtMost(list.size - 1)) }
                                s.tapTurn && back -> scope.launch { pager.animateScrollToPage((pager.currentPage - 1).coerceAtLeast(0)) }
                                else -> onTapPlain()
                            }
                        }
                    }
                    .pointerInput(i, zoom > 1.01f) {
                        detectTransformGestures { _, p, z, _ ->
                            zoom = (zoom * z).coerceIn(1f, 5f)
                            val mx = (zoom - 1f) * size.width / 2f; val my = (zoom - 1f) * size.height / 2f
                            pan = if (zoom <= 1.01f) Offset.Zero else Offset((pan.x + p.x).coerceIn(-mx, mx), (pan.y + p.y).coerceIn(-my, my))
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                val b = bmp
                if (b == null) CircularProgressIndicator(color = c.accent)
                else Image(
                    b.asImageBitmap(), "Página ${i + 1}", contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize().graphicsLayer {
                        val z = if (i == pager.currentPage) zoom else 1f
                        scaleX = z; scaleY = z
                        translationX = if (i == pager.currentPage) pan.x else 0f; translationY = if (i == pager.currentPage) pan.y else 0f
                    },
                )
            }
        }
        Text(
            "${pager.currentPage + 1} / ${list.size}" + if (rtl) " · manga" else "", color = mode.muted, fontSize = 12.sp,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

private fun Int.dp() = androidx.compose.ui.unit.Dp(this.toFloat())
