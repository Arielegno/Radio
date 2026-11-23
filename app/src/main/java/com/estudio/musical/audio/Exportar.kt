package com.estudio.musical.audio

import android.content.Context
import android.content.Intent
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import androidx.core.content.FileProvider
import java.io.File
import java.io.RandomAccessFile

object Exportar {

    /** Convierte un WAV a M4A (AAC), que pesa ~10 veces menos. Todo se hace en el teléfono. */
    fun wavAM4a(wav: File, salida: File, bitrate: Int = 192_000) {
        val info = Wav.info(wav)
        val formato = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, info.sampleRate, info.channels).apply {
            setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
            setInteger(MediaFormat.KEY_BIT_RATE, bitrate)
            setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 16384)
        }
        val codec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC)
        codec.configure(formato, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        codec.start()
        val muxer = MediaMuxer(salida.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        var pista = -1
        var muxerIniciado = false
        val bi = MediaCodec.BufferInfo()
        val bytesPorFrame = 2 * info.channels

        RandomAccessFile(wav, "r").use { raf ->
            raf.seek(info.dataStart.toLong())
            var leidos = 0L
            var finEntrada = false
            var finSalida = false
            val tmp = ByteArray(8192)
            while (!finSalida) {
                if (!finEntrada) {
                    val idx = codec.dequeueInputBuffer(10_000)
                    if (idx >= 0) {
                        val buf = codec.getInputBuffer(idx)!!
                        buf.clear()
                        val quiero = minOf(buf.capacity(), tmp.size, (info.dataLen - leidos).toInt())
                        val n = if (quiero > 0) raf.read(tmp, 0, quiero) else -1
                        val tiempoUs = leidos / bytesPorFrame * 1_000_000L / info.sampleRate
                        if (n <= 0) {
                            codec.queueInputBuffer(idx, 0, 0, tiempoUs, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            finEntrada = true
                        } else {
                            buf.put(tmp, 0, n)
                            codec.queueInputBuffer(idx, 0, n, tiempoUs, 0)
                            leidos += n
                        }
                    }
                }
                val out = codec.dequeueOutputBuffer(bi, 10_000)
                when {
                    out == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                        pista = muxer.addTrack(codec.outputFormat); muxer.start(); muxerIniciado = true
                    }
                    out >= 0 -> {
                        val data = codec.getOutputBuffer(out)!!
                        if ((bi.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) bi.size = 0
                        if (bi.size > 0 && muxerIniciado) {
                            data.position(bi.offset); data.limit(bi.offset + bi.size)
                            muxer.writeSampleData(pista, data, bi)
                        }
                        codec.releaseOutputBuffer(out, false)
                        if ((bi.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) finSalida = true
                    }
                }
            }
        }
        codec.stop(); codec.release()
        if (muxerIniciado) muxer.stop()
        muxer.release()
    }

    /** Abre el menú de compartir de Android (WhatsApp, Bluetooth, guardar en archivos…) */
    fun compartir(ctx: Context, archivo: File, tipo: String = tipoDe(archivo)) {
        val uri = FileProvider.getUriForFile(ctx, ctx.packageName + ".archivos", archivo)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = tipo
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        ctx.startActivity(Intent.createChooser(intent, "Compartir ${archivo.name}").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    private fun tipoDe(f: File) = when (f.extension.lowercase()) {
        "wav" -> "audio/wav"; "m4a" -> "audio/mp4"; "pdf" -> "application/pdf"; "zip" -> "application/zip"
        else -> "*/*"
    }
}
