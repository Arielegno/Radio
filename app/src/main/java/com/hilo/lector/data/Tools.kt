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
                if (rel.startsWith(SKIP_CACHE) || rel.startsWith("lines/") || rel.endsWith(".tmp")) return@forEach
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
        if (!sp.getBoolean("remindOn", false)) return
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

    fun notify(ctx: Context) {
        if (needsPermission(ctx)) return
        val (due, exams) = summary(ctx)
        if (due == 0 && exams.isEmpty()) return
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
        }
        val open = PendingIntent.getActivity(
            ctx, 8, Intent(ctx, MainActivity::class.java).putExtra(EXTRA_STUDY, true).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val n = NotificationCompat.Builder(ctx, CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_hilo)
            .setContentTitle("Hora de repasar")
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
