package com.estudio.musical.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

data class Cancion(
    val id: String = UUID.randomUUID().toString(),
    val titulo: String,
    val letra: String,
    val tonalidad: String = "",
    val actualizada: Long = System.currentTimeMillis()
)

/** Guarda las letras en un archivo JSON privado de la app (funciona sin internet). */
class LyricsRepository(context: Context) {
    private val file = File(context.filesDir, "letras.json")

    fun all(): List<Cancion> {
        if (!file.exists()) return emptyList()
        val arr = JSONArray(file.readText())
        return (0 until arr.length()).map {
            val o = arr.getJSONObject(it)
            Cancion(
                o.getString("id"), o.getString("titulo"), o.getString("letra"),
                o.optString("tonalidad"), o.optLong("actualizada")
            )
        }.sortedByDescending { it.actualizada }
    }

    fun save(c: Cancion) {
        val list = all().filter { it.id != c.id } + c.copy(actualizada = System.currentTimeMillis())
        write(list)
    }

    fun delete(id: String) = write(all().filter { it.id != id })

    private fun write(list: List<Cancion>) {
        val arr = JSONArray()
        list.forEach {
            arr.put(JSONObject().apply {
                put("id", it.id); put("titulo", it.titulo); put("letra", it.letra)
                put("tonalidad", it.tonalidad); put("actualizada", it.actualizada)
            })
        }
        file.writeText(arr.toString())
    }
}
