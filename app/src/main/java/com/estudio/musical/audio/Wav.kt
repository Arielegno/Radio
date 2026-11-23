package com.estudio.musical.audio

import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder

/** Audio en memoria: muestras mono en rango -1..1 */
class AudioData(val samples: FloatArray, val sampleRate: Int) {
    val durationSec: Float get() = samples.size / sampleRate.toFloat()
}

/** Lectura/escritura de WAV PCM 16 bits (formato sin pérdida, máxima calidad). */
object Wav {

    fun writeHeader(raf: RandomAccessFile, sampleRate: Int, channels: Int, dataBytes: Int) {
        val bb = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN)
        bb.put("RIFF".toByteArray()); bb.putInt(36 + dataBytes)
        bb.put("WAVE".toByteArray())
        bb.put("fmt ".toByteArray()); bb.putInt(16)
        bb.putShort(1)                       // PCM
        bb.putShort(channels.toShort())
        bb.putInt(sampleRate)
        bb.putInt(sampleRate * channels * 2) // byte rate
        bb.putShort((channels * 2).toShort())
        bb.putShort(16)                      // bits
        bb.put("data".toByteArray()); bb.putInt(dataBytes)
        raf.seek(0)
        raf.write(bb.array())
    }

    /** Datos del PCM sin convertir (para codificar a M4A) */
    data class InfoPcm(val channels: Int, val sampleRate: Int, val dataStart: Int, val dataLen: Int)

    fun info(file: File): InfoPcm {
        val head = ByteArray(minOf(file.length(), 4096L).toInt())
        file.inputStream().use { it.read(head) }
        val bb = ByteBuffer.wrap(head).order(ByteOrder.LITTLE_ENDIAN)
        var ch = 1; var sr = 48000; var pos = 12
        while (pos + 8 <= head.size) {
            val id = String(head, pos, 4); val size = bb.getInt(pos + 4)
            if (id == "fmt ") { ch = bb.getShort(pos + 10).toInt(); sr = bb.getInt(pos + 12) }
            else if (id == "data") return InfoPcm(ch, sr, pos + 8, minOf(size.toLong(), file.length() - pos - 8).toInt())
            pos += 8 + size + (size and 1)
        }
        return InfoPcm(ch, sr, 44, (file.length() - 44).toInt())
    }

    fun read(file: File): AudioData {
        val bytes = file.readBytes()
        val bb = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        var channels = 1
        var sampleRate = 48000
        var pos = 12
        var dataStart = 44
        var dataLen = bytes.size - 44
        while (pos + 8 <= bytes.size) {
            val id = String(bytes, pos, 4)
            val size = bb.getInt(pos + 4)
            if (id == "fmt ") {
                channels = bb.getShort(pos + 10).toInt()
                sampleRate = bb.getInt(pos + 12)
            } else if (id == "data") {
                dataStart = pos + 8
                dataLen = minOf(size, bytes.size - dataStart)
                break
            }
            pos += 8 + size + (size and 1)
        }
        val frames = dataLen / (2 * channels)
        val out = FloatArray(frames)
        for (i in 0 until frames) {
            var sum = 0f
            for (c in 0 until channels) {
                sum += bb.getShort(dataStart + (i * channels + c) * 2) / 32768f
            }
            out[i] = sum / channels
        }
        return AudioData(out, sampleRate)
    }

    /** WAV estéreo (para mezclas con paneo izquierda/derecha) */
    fun writeStereo(file: File, left: FloatArray, right: FloatArray, sampleRate: Int) {
        RandomAccessFile(file, "rw").use { raf ->
            raf.setLength(0)
            val frames = minOf(left.size, right.size)
            val dataBytes = frames * 4
            writeHeader(raf, sampleRate, 2, dataBytes)
            val bb = ByteBuffer.allocate(dataBytes).order(ByteOrder.LITTLE_ENDIAN)
            for (i in 0 until frames) {
                bb.putShort((left[i].coerceIn(-1f, 1f) * 32767f).toInt().toShort())
                bb.putShort((right[i].coerceIn(-1f, 1f) * 32767f).toInt().toShort())
            }
            raf.seek(44)
            raf.write(bb.array())
        }
    }

    /** Lee un WAV y lo convierte a [targetRate] si hace falta (p. ej. 44,1 kHz → 48 kHz) */
    fun readAt(file: File, targetRate: Int): AudioData {
        val a = read(file)
        if (a.sampleRate == targetRate) return a
        val ratio = a.sampleRate.toDouble() / targetRate
        val n = (a.samples.size / ratio).toInt()
        val out = FloatArray(n) {
            val pos = it * ratio
            val i = pos.toInt()
            val f = (pos - i).toFloat()
            val s0 = a.samples[i.coerceAtMost(a.samples.size - 1)]
            val s1 = a.samples[(i + 1).coerceAtMost(a.samples.size - 1)]
            s0 + (s1 - s0) * f
        }
        return AudioData(out, targetRate)
    }

    fun write(file: File, audio: AudioData) {
        RandomAccessFile(file, "rw").use { raf ->
            raf.setLength(0)
            val dataBytes = audio.samples.size * 2
            writeHeader(raf, audio.sampleRate, 1, dataBytes)
            val bb = ByteBuffer.allocate(dataBytes).order(ByteOrder.LITTLE_ENDIAN)
            for (s in audio.samples) {
                bb.putShort((s.coerceIn(-1f, 1f) * 32767f).toInt().toShort())
            }
            raf.seek(44)
            raf.write(bb.array())
        }
    }
}
