package io.github.arielegno.radio.ui

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.ViewGroup
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
import io.github.arielegno.radio.Red
import io.github.arielegno.radio.Revista

/** Una sola vista web para la Revista, que se conserva al cambiar de pestaña. */
class VistaRevista(ctx: Context) {
    var elegirArchivos: ((ValueCallback<Array<Uri>>, WebChromeClient.FileChooserParams) -> Boolean)? = null
    var cargada = false

    @SuppressLint("SetJavaScriptEnabled")
    val web: WebView = WebView(ctx).apply {
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.mediaPlaybackRequiresUserGesture = true
        webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                val u = request.url
                val propia = u.host == "arielegno.github.io" || (u.host ?: "").endsWith(".supabase.co")
                if (propia || !request.isForMainFrame) return false
                return try { view.context.startActivity(Intent(Intent.ACTION_VIEW, u).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); true } catch (e: Exception) { false }
            }
        }
        webChromeClient = object : WebChromeClient() {
            override fun onShowFileChooser(webView: WebView?, filePathCallback: ValueCallback<Array<Uri>>?, fileChooserParams: FileChooserParams?): Boolean {
                val cb = filePathCallback ?: return false; val ps = fileChooserParams ?: return false
                return elegirArchivos?.invoke(cb, ps) ?: false
            }
        }
    }

    fun abrir(destino: String) {
        val url = if (destino.startsWith("#")) Revista.WEB.substringBefore('#') + destino else Revista.WEB
        web.loadUrl(url); cargada = true
    }
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
    LaunchedEffect(destino) { if (destino.isNotEmpty() || !vr.cargada) vr.abrir(destino) }
    BackHandler { if (vr.web.canGoBack()) vr.web.goBack() else alSalir() }
    AndroidView(factory = { (vr.web.parent as? ViewGroup)?.removeView(vr.web); vr.web }, modifier = Modifier.fillMaxSize())
}
