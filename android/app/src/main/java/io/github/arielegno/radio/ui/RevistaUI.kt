package io.github.arielegno.radio.ui

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import io.github.arielegno.radio.RadioApp
import io.github.arielegno.radio.Red
import io.github.arielegno.radio.Revista
import io.github.arielegno.radio.core.Json
import io.github.arielegno.radio.launchSafe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Puente entre la Revista web y la app: la Revista ve tu biblioteca de la app,
 * «Escuchar» y «Escuchar artículo» suenan con el reproductor y la voz de la app.
 */
class PuenteRevista(private val web: () -> WebView, private val irRadio: () -> Unit) {
    @JavascriptInterface
    fun biblioteca(): String = Json.write(RadioApp.biblioteca.pistas.map {
        linkedMapOf("id" to it.id, "name" to it.name, "artist" to it.artist, "album" to it.album, "year" to (if (it.year > 0) it.year.toString() else ""), "genre" to it.genre)
    })

    @JavascriptInterface
    fun reproducir(idsJson: String, desde: Double) {
        val ids = Json.arr(idsJson).mapNotNull { (it as? Number)?.toLong() }.toSet()
        if (ids.isEmpty()) return
        RadioApp.scope.launch(Dispatchers.Main) {
            RadioApp.radio.iniciar({ it.id in ids }, desdeId = desde.toLong().takeIf { it in ids }, mezclarEsta = false)
            irRadio()
        }
    }

    @JavascriptInterface
    fun hablar(texto: String, id: Int) {
        RadioApp.scope.launchSafe {
            RadioApp.radio.voz.decir(texto)
            web().evaluateJavascript("window.__rrHablo && window.__rrHablo($id)", null)
        }
    }

    @JavascriptInterface
    fun callar() { RadioApp.scope.launch(Dispatchers.Main) { RadioApp.radio.voz.callar() } }

    @JavascriptInterface
    fun irALaRadio() { RadioApp.scope.launch(Dispatchers.Main) { irRadio() } }
}

/** Se ejecuta en la página: muestra solo la Revista y conecta la biblioteca, la música y la voz con la app. */
private val CAPA_APP = """
(function(){
  try { localStorage.setItem('rr_tourDone','1'); } catch(e) {}
  document.documentElement.classList.add('en-app-android');
  if (!document.getElementById('rrAppCss')) {
    var st = document.createElement('style'); st.id = 'rrAppCss';
    st.textContent = '.topbar,#sidebar,#sidebarOverlay,#ctl,#miniNow,#installBtn,#tourOv{display:none!important}' +
      '.view:not(#view-revista){display:none!important}#view-revista{display:block!important}' +
      'body{padding-top:env(safe-area-inset-top,0)!important}' +
      '#uiDlg{overflow-y:auto!important;padding:16px!important;align-items:flex-start!important}' +
      /* La ventana nunca se aplasta: crece con su contenido hasta el alto real de la pantalla */
      '#uiDlg .dlg-card{max-height:100%!important;height:auto!important;min-height:min-content!important;overflow-y:auto!important;flex-shrink:0!important;margin:auto!important}';
    document.head.appendChild(st);
  }
  var t = document.getElementById('tourOv'); if (t) t.remove();
  // Las medidas «vh» de la página fallan dentro de la app: se calculan con la altura real de la pantalla
  var alto = document.getElementById('rrAppVh'); if (!alto) { alto = document.createElement('style'); alto.id = 'rrAppVh'; document.head.appendChild(alto); }
  var medir = function(){ var h = window.innerHeight || document.documentElement.clientHeight || 700;
    if (h < 320) h = screen.height || 700;
    alto.textContent = '.dlg-opts{max-height:' + Math.round(h * .5) + 'px!important}' +
      '.rv-pview img{max-height:' + Math.round(h * .78) + 'px!important;min-height:120px}.rv-lb img{max-height:' + Math.round(h * .92) + 'px!important}' +
      '.rv-map{height:' + Math.round(h * .6) + 'px!important}body{min-height:' + h + 'px!important}'; };
  medir(); window.addEventListener('resize', medir); setTimeout(medir, 400); setTimeout(medir, 1500);
  // Personalización: el color de acento de la app también en la Revista
  try { if (window.__rrAcento) document.documentElement.style.setProperty('--line', window.__rrAcento); } catch(e) {}
  if (window.__rrApp) return; window.__rrApp = 1;
  // Voz de la app para «Escuchar artículo»
  var cb = {}, n = 0;
  window.__rrHablo = function(id){ var f = cb[id]; delete cb[id]; if (f) f(); };
  try { speak = function(texto){ return new Promise(function(ok){ var id = ++n; cb[id] = ok; RadioNativa.hablar(String(texto || ''), id); }); }; } catch(e) {}
  try { var c0 = speechSynthesis.cancel.bind(speechSynthesis); speechSynthesis.cancel = function(){ try { RadioNativa.callar(); } catch(e) {} try { c0(); } catch(e) {} }; } catch(e) {}
  // Reproducción con el reproductor de la app
  try { playLib = function(filtro, desde){ libAll().then(function(a){ var ids = a.filter(function(r){ try { return filtro ? filtro(r) : true; } catch(e) { return false; } }).map(function(r){ return r.id; }); RadioNativa.reproducir(JSON.stringify(ids), desde == null ? -1 : Number(desde)); }); }; } catch(e) {}
  // La radio web no se usa dentro de la app
  try { var sv = showView; showView = function(nombre, y){ if (nombre !== 'revista') { if (nombre === 'radio') RadioNativa.irALaRadio(); return; } return sv(nombre, y); }; } catch(e) {}
  // Tu biblioteca de la app, para que la Revista sepa qué canciones tienes
  (async function(){
    try {
      var lista = JSON.parse(RadioNativa.biblioteca());
      var d = await dbp; var tr = d.transaction('t', 'readwrite'); var s = tr.objectStore('t');
      s.clear(); lista.forEach(function(r){ s.put(r); });
      await new Promise(function(ok){ tr.oncomplete = ok; tr.onerror = ok; });
      libCache = null;
    } catch(e) {}
    try { showView('revista'); } catch(e) {}
  })();
})();
""".trimIndent()

/** Ajustes de la app que la Revista también respeta (color de acento y tamaño de letra). */
private fun capaConAjustes(web: WebView): String {
    val p = RadioApp.prefs
    val hex = p.str("acentoColor", "#f2c230").lowercase().takeIf { Regex("^#[0-9a-f]{6}$").matches(it) } ?: "#f2c230"
    web.settings.textZoom = (p.int("letra", 17) * 100 / 17).coerceIn(85, 145)
    return "window.__rrAcento='$hex';" + CAPA_APP
}

/** Una sola vista web para la Revista, que se conserva al cambiar de pestaña. */
class VistaRevista(ctx: Context, irRadio: () -> Unit) {
    var elegirArchivos: ((ValueCallback<Array<Uri>>, WebChromeClient.FileChooserParams) -> Boolean)? = null
    var cargada = false

    @SuppressLint("SetJavaScriptEnabled", "JavascriptInterface")
    val web: WebView = WebView(ctx).apply {
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.mediaPlaybackRequiresUserGesture = false
        settings.userAgentString = settings.userAgentString + " RadioDeRutaApp/1.0"
        setBackgroundColor(android.graphics.Color.parseColor("#14161A"))
        // Ocupa toda la pantalla de verdad (si no, la página mide mal la altura y las ventanas salen aplastadas)
        layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                val u = request.url
                val propia = u.host == "arielegno.github.io" || (u.host ?: "").endsWith(".supabase.co")
                if (propia || !request.isForMainFrame) return false
                return try { view.context.startActivity(Intent(Intent.ACTION_VIEW, u).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); true } catch (e: Exception) { false }
            }
            override fun onPageFinished(view: WebView, url: String?) {
                if (url != null && Uri.parse(url).host == "arielegno.github.io") view.evaluateJavascript(capaConAjustes(view), null)
            }
        }
        webChromeClient = object : WebChromeClient() {
            override fun onShowFileChooser(webView: WebView?, filePathCallback: ValueCallback<Array<Uri>>?, fileChooserParams: FileChooserParams?): Boolean {
                val cb = filePathCallback ?: return false; val ps = fileChooserParams ?: return false
                return elegirArchivos?.invoke(cb, ps) ?: false
            }
        }
    }

    init { web.addJavascriptInterface(PuenteRevista({ web }, irRadio), "RadioNativa") }

    fun abrir(destino: String) {
        val url = if (destino.startsWith("#")) Revista.WEB.substringBefore('#') + destino else Revista.WEB
        web.loadUrl(url); cargada = true
    }

    /** Al volver a la pestaña: vuelve a aplicar la capa (por si la biblioteca cambió). */
    fun refrescar() { if (cargada) web.evaluateJavascript("window.__rrApp=0;" + capaConAjustes(web), null) }
}

@Composable
fun RevistaPantalla(vr: VistaRevista, destino: String, alSalir: () -> Unit) {
    var pendiente by remember { mutableStateOf<ValueCallback<Array<Uri>>?>(null) }
    val selector = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { res ->
        val uris = if (res.resultCode == Activity.RESULT_OK) WebChromeClient.FileChooserParams.parseResult(res.resultCode, res.data) else null
        pendiente?.onReceiveValue(uris); pendiente = null
    }
    vr.elegirArchivos = { cb, ps ->
        pendiente?.onReceiveValue(null); pendiente = cb
        try { selector.launch(ps.createIntent()); true } catch (e: Exception) { pendiente = null; cb.onReceiveValue(null); false }
    }
    val hay = remember { Red.hayInternet(vr.web.context) }
    if (!hay && !vr.cargada) {
        Column(Modifier.fillMaxSize().padding(24.dp)) {
            Text("La Revista necesita internet. Conéctate y vuelve a intentarlo.")
            Button(onClick = alSalir, modifier = Modifier.padding(top = 12.dp)) { Text("Volver a la radio") }
        }
        return
    }
    LaunchedEffect(destino) {
        // Espera a que la vista tenga su tamaño antes de cargar la página
        var n = 0; while ((vr.web.width == 0 || vr.web.height == 0) && n++ < 60) kotlinx.coroutines.delay(25)
        if (destino.isNotEmpty() || !vr.cargada) vr.abrir(destino) else vr.refrescar()
    }
    BackHandler { if (vr.web.canGoBack()) vr.web.goBack() else alSalir() }
    AndroidView(factory = { (vr.web.parent as? ViewGroup)?.removeView(vr.web); vr.web }, modifier = Modifier.fillMaxSize())
}
