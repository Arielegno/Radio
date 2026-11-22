package com.hilo.lector.data

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.hilo.lector.MainActivity
import com.hilo.lector.R
import org.json.JSONObject
import java.io.File
import java.util.Calendar
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/** Exportar tarjetas a Anki: archivo de texto separado por tabuladores (Anki → Importar). */
object AnkiExport {
    private fun cell(s: String) = s.replace("\t", " ").replace("\r", "").replace("\n", "<br>")

    fun file(ctx: Context, cards: List<StudyCard>, titleOf: (String) -> String, name: String): File {
        val dir = File(ctx.cacheDir, "export").apply { mkdirs() }
        val f = File(dir, name.replace(Regex("[^\\p{L}\\p{N}_-]+"), "_").take(60) + ".txt")
        f.bufferedWriter().use { w ->
            w.write("#separator:tab\n#html:true\n#tags column:3\n")
            for (c in cards) {
                val front = if (c.kind == "cloze") c.front.replace("[ … ]", "_____") else c.front
                val back = buildString {
                    append(c.back)
                    if (c.kind == "cloze" && c.context.isNotBlank()) append("\n\n").append(c.context)
                    if (c.kind == "vocab" && c.context.isNotBlank()) append("\n\n<i>").append(c.context).append("</i>")
                    if (c.source.isNotBlank()) append("\n\n<small>").append(titleOf(c.bookId)).append(" · ").append(c.source).append("</small>")
                }
                val tag = "Hilo " + titleOf(c.bookId).replace(Regex("[^\\p{L}\\p{N}]+"), "_").take(40)
                w.write(cell(front) + "\t" + cell(back) + "\t" + tag + "\n")
            }
        }
        return f
    }

    fun share(ctx: Context, f: File) {
        val uri = FileProvider.getUriForFile(ctx, ctx.packageName + ".files", f)
        val i = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_STREAM, uri)
            .putExtra(Intent.EXTRA_SUBJECT, f.nameWithoutExtension).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        ctx.startActivity(Intent.createChooser(i, "Enviar a Anki").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}

/** Copia de seguridad: tarjetas, resaltados, notas, avance y ajustes (y si quieres, los libros) en un .zip. */
object Backup {
    private const val SKIP_CACHE = "flow/"

    fun write(ctx: Context, out: java.io.OutputStream, withBooks: Boolean): Int {
        var n = 0
        val files = ctx.filesDir
        val prefs = File(ctx.applicationInfo.dataDir, "shared_prefs")
        ZipOutputStream(out.buffered()).use { z ->
            z.putNextEntry(ZipEntry("hilo-copia.txt"))
            z.write("Copia de seguridad de Hilo\n".toByteArray())
            z.closeEntry()
            files.walkTopDown().filter { it.isFile }.forEach { f ->
                val rel = f.relativeTo(files).path.replace('\\', '/')
                // los diccionarios (más de 100 MB) y el índice de búsqueda se vuelven a descargar o armar solos
                if (rel.startsWith(SKIP_CACHE) || rel.startsWith("lines/") || rel.startsWith("diccionarios/") || rel.startsWith("busqueda.db") || rel.endsWith(".tmp")) return@forEach
                if (!withBooks && rel.startsWith("books/")) return@forEach
                z.putNextEntry(ZipEntry("files/$rel"))
                f.inputStream().use { it.copyTo(z) }
                z.closeEntry()
                n++
            }
            prefs.listFiles()?.filter { it.isFile && it.name.endsWith(".xml") }?.forEach { f ->
                z.putNextEntry(ZipEntry("prefs/${f.name}"))
                f.inputStream().use { it.copyTo(z) }
                z.closeEntry()
                n++
            }
        }
        return n
    }

    /** Restaura una copia. Devuelve cuántos archivos se recuperaron; la app debe reiniciarse después. */
    fun restore(ctx: Context, input: java.io.InputStream): Int {
        val files = ctx.filesDir.canonicalFile
        val prefs = File(ctx.applicationInfo.dataDir, "shared_prefs").canonicalFile
        var n = 0
        var valid = false
        ZipInputStream(input.buffered()).use { z ->
            while (true) {
                val e = z.nextEntry ?: break
                if (e.isDirectory) continue
                val name = e.name
                val target = when {
                    name == "hilo-copia.txt" -> { valid = true; null }
                    name.startsWith("files/") -> File(files, name.removePrefix("files/"))
                    name.startsWith("prefs/") -> File(prefs, name.removePrefix("prefs/"))
                    else -> null
                } ?: continue
                val canon = target.canonicalFile
                // Evita rutas que salgan de las carpetas de la app
                if (!canon.path.startsWith(files.path + File.separator) && !canon.path.startsWith(prefs.path + File.separator)) continue
                canon.parentFile?.mkdirs()
                canon.outputStream().use { z.copyTo(it) }
                n++
            }
        }
        if (!valid) error("No es una copia de Hilo")
        File(ctx.filesDir, "flow").deleteRecursively()
        return n
    }

    fun restart(ctx: Context) {
        val i = Intent(ctx, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        ctx.startActivity(i)
        Runtime.getRuntime().exit(0)
    }
}

/** Recordatorio diario para repasar. */
object Reminders {
    private const val CHANNEL = "repaso"
    const val EXTRA_STUDY = "abrir_estudio"

    private fun pending(ctx: Context): PendingIntent =
        PendingIntent.getBroadcast(ctx, 7, Intent(ctx, ReminderReceiver::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

    fun needsPermission(ctx: Context) = Build.VERSION.SDK_INT >= 33 &&
        ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED

    fun schedule(ctx: Context) {
        val sp = ctx.getSharedPreferences("estudio", Context.MODE_PRIVATE)
        val am = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        am.cancel(pending(ctx))
        // La alarma diaria también actualiza el widget y hace la copia automática
        if (!sp.getBoolean("remindOn", false) && AutoBackup.folder(ctx) == null) return
        val hour = sp.getInt("remindHour", 19)
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0)
            if (timeInMillis <= System.currentTimeMillis() + 30_000) add(Calendar.DAY_OF_YEAR, 1)
        }
        am.setInexactRepeating(AlarmManager.RTC_WAKEUP, cal.timeInMillis, AlarmManager.INTERVAL_DAY, pending(ctx))
    }

    /** Cuántas tarjetas tocan hoy y qué exámenes se acercan, leyendo el archivo de estudio. */
    fun summary(ctx: Context): Pair<Int, List<Pair<String, Long>>> {
        val f = File(ctx.filesDir, "estudio/estudio.json")
        if (!f.exists()) return 0 to emptyList()
        return try {
            val o = JSONObject(f.readText())
            val t = today()
            var due = 0
            o.optJSONArray("cards")?.let { a -> for (i in 0 until a.length()) if (a.getJSONObject(i).optLong("due") <= t) due++ }
            val ex = ArrayList<Pair<String, Long>>()
            o.optJSONObject("exams")?.let { n -> n.keys().forEach { k -> val d = n.optLong(k) - t; if (d in 0..14) ex.add(k to d) } }
            due to ex.sortedBy { it.second }
        } catch (_: Exception) { 0 to emptyList() }
    }

    /** Exámenes y entregas de las Clases en los próximos 7 días: (título, tipo, días que faltan). */
    fun classEvents(ctx: Context): List<Triple<String, String, Long>> = runCatching {
        val o = JSONObject(File(ctx.filesDir, "clases.json").readText())
        val names = HashMap<String, String>()
        o.optJSONArray("clases")?.let { a -> for (i in 0 until a.length()) a.getJSONObject(i).let { names[it.getString("id")] = it.optString("nombre") } }
        val t = today()
        val out = ArrayList<Triple<String, String, Long>>()
        o.optJSONArray("fechas")?.let { a ->
            for (i in 0 until a.length()) {
                val j = a.getJSONObject(i)
                if (j.optBoolean("hecho")) continue
                val k = j.optString("tipo"); if (k != "examen" && k != "entrega") continue
                val d = j.optLong("dia") - t
                if (d in 0..7) out.add(Triple(j.optString("titulo").ifBlank { names[j.optString("clase")] ?: "tu clase" } + (names[j.optString("clase")]?.let { " ($it)" } ?: ""), k, d))
            }
        }
        out.sortedBy { it.third }
    }.getOrDefault(emptyList())

    /** Minutos leídos hoy (para avisar antes de perder la racha). */
    fun minutesToday(ctx: Context): Int = runCatching {
        JSONObject(File(ctx.filesDir, "estudio/motor.json").readText()).optJSONObject("kv")?.optString("min:${today()}")?.toIntOrNull() ?: 0
    }.getOrDefault(0)

    fun notify(ctx: Context) {
        HiloWidget.refresh(ctx)
        AutoBackup.runIfDue(ctx)
        if (!ctx.getSharedPreferences("estudio", Context.MODE_PRIVATE).getBoolean("remindOn", false)) return
        if (needsPermission(ctx)) return
        val (due, exams) = summary(ctx)
        val evs = classEvents(ctx)
        val noReading = minutesToday(ctx) == 0
        if (due == 0 && exams.isEmpty() && evs.isEmpty() && !noReading) return
        val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= 26 && nm.getNotificationChannel(CHANNEL) == null) {
            nm.createNotificationChannel(NotificationChannel(CHANNEL, "Recordatorio de repaso", NotificationManager.IMPORTANCE_DEFAULT))
        }
        val titles = runCatching { org.json.JSONArray(File(ctx.filesDir, "library.json").readText()) }.getOrNull()
        fun titleOf(id: String): String {
            val a = titles ?: return "tu examen"
            for (i in 0 until a.length()) { val j = a.optJSONObject(i) ?: continue; if (j.optString("id") == id) return j.optString("title") }
            return "tu examen"
        }
        val text = buildString {
            if (due > 0) append(if (due == 1) "Tienes 1 tarjeta para hoy. Es un minuto." else "Tienes $due tarjetas para hoy. Una ronda de 5 minutos alcanza.")
            exams.firstOrNull()?.let { (id, d) ->
                if (isNotEmpty()) append(" ")
                append(when (d) { 0L -> "¡Hoy es el examen de «${titleOf(id)}»!"; 1L -> "Mañana es el examen de «${titleOf(id)}»."; else -> "Faltan $d días para el examen de «${titleOf(id)}»." })
            }
            evs.take(2).forEach { (title, kind, d) ->
                if (isNotEmpty()) append(" ")
                val what = if (kind == "examen") "el examen" else "la entrega"
                append(when (d) { 0L -> "Hoy es $what: $title."; 1L -> "Mañana es $what: $title."; else -> "En $d días es $what: $title." })
            }
            if (noReading && due == 0) { if (isNotEmpty()) append(" "); append("Hoy todavía no leíste: con 2 minutos mantienes la racha.") }
        }
        val open = PendingIntent.getActivity(
            ctx, 8, Intent(ctx, MainActivity::class.java).putExtra(EXTRA_STUDY, true).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val n = NotificationCompat.Builder(ctx, CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_hilo)
            .setContentTitle(if (due > 0 || exams.isNotEmpty() || evs.isNotEmpty()) "Hora de repasar" else "Tu racha te espera")
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        if (Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            try { NotificationManagerCompat.from(ctx).notify(71, n) } catch (_: SecurityException) {}
        }
    }
}

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_MY_PACKAGE_REPLACED -> Reminders.schedule(ctx)
            else -> Reminders.notify(ctx)
        }
    }
}

@Suppress("unused")
private fun Uri.isZip() = toString().endsWith(".zip")


/** Copia de seguridad automática a una carpeta elegida (Drive, la tarjeta SD o donde quieras). Sin servidores. */
object AutoBackup {
    private fun sp(ctx: Context) = ctx.getSharedPreferences("copia", Context.MODE_PRIVATE)
    fun folder(ctx: Context): Uri? = sp(ctx).getString("carpeta", null)?.let { Uri.parse(it) }
    fun every(ctx: Context) = sp(ctx).getInt("cada", 7)
    fun last(ctx: Context) = sp(ctx).getLong("ultima", 0L)

    fun setFolder(ctx: Context, uri: Uri?, days: Int) {
        val e = sp(ctx).edit()
        if (uri == null) e.remove("carpeta") else {
            runCatching { ctx.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION) }
            e.putString("carpeta", uri.toString())
        }
        e.putInt("cada", days).apply()
    }

    /** Hace la copia si ya pasaron los días elegidos. Guarda las 3 últimas y borra las más viejas. */
    fun runIfDue(ctx: Context, force: Boolean = false): Boolean {
        val tree = folder(ctx) ?: return false
        if (!force && System.currentTimeMillis() - last(ctx) < every(ctx) * 86_400_000L - 3_600_000L) return false
        return runCatching {
            val dir = androidx.documentfile.provider.DocumentFile.fromTreeUri(ctx, tree) ?: error("sin carpeta")
            val name = "Hilo-copia-" + java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date()) + ".zip"
            dir.findFile(name)?.delete()
            val f = dir.createFile("application/zip", name) ?: error("no pude crear")
            ctx.contentResolver.openOutputStream(f.uri)?.use { Backup.write(ctx, it, false) } ?: error("sin salida")
            dir.listFiles().filter { it.name?.startsWith("Hilo-copia-") == true }.sortedByDescending { it.name }.drop(3).forEach { it.delete() }
            sp(ctx).edit().putLong("ultima", System.currentTimeMillis()).apply()
            true
        }.getOrDefault(false)
    }
}

/** Widget de la pantalla de inicio: tarjetas para hoy, racha y próximo examen. */
class HiloWidget : android.appwidget.AppWidgetProvider() {
    override fun onUpdate(ctx: Context, mgr: android.appwidget.AppWidgetManager, ids: IntArray) = refresh(ctx)

    companion object {
        fun refresh(ctx: Context) {
            runCatching {
                val mgr = android.appwidget.AppWidgetManager.getInstance(ctx)
                val ids = mgr.getAppWidgetIds(android.content.ComponentName(ctx, HiloWidget::class.java))
                if (ids.isEmpty()) return
                val (due, exams) = Reminders.summary(ctx)
                val evs = Reminders.classEvents(ctx)
                val min = Reminders.minutesToday(ctx)
                val v = android.widget.RemoteViews(ctx.packageName, R.layout.widget_hilo)
                v.setTextViewText(R.id.w_due, when (due) { 0 -> "Al día ✓"; 1 -> "1 tarjeta para hoy"; else -> "$due tarjetas para hoy" })
                val next = evs.firstOrNull()?.let { (t, k, d) -> (if (k == "examen") "Examen" else "Entrega") + ": " + t + when (d) { 0L -> " · hoy"; 1L -> " · mañana"; else -> " · en $d días" } }
                    ?: exams.firstOrNull()?.let { (_, d) -> "Examen " + when (d) { 0L -> "hoy"; 1L -> "mañana"; else -> "en $d días" } }
                    ?: if (min > 0) "Hoy leíste $min min" else "Hoy todavía no leíste"
                v.setTextViewText(R.id.w_next, next)
                v.setTextViewText(R.id.w_streak, if (min > 0) "🔥 $min min" else "")
                v.setTextViewText(R.id.w_go, if (due > 0) "Repasar" else "Abrir")
                val open = PendingIntent.getActivity(
                    ctx, 9, Intent(ctx, MainActivity::class.java).putExtra(Reminders.EXTRA_STUDY, due > 0).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )
                v.setOnClickPendingIntent(R.id.w_root, open)
                v.setOnClickPendingIntent(R.id.w_go, open)
                mgr.updateAppWidget(ids, v)
            }
        }
    }
}
