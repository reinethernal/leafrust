package com.leafrust.util

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.ShareCompat
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentLinkedQueue

/** In-app debug log (file + memory). Share from Settings in debug builds. */
object AppLog {
    private const val TAG = "LeafRust"
    private const val MAX_LINES = 2000
    private val lines = ConcurrentLinkedQueue<String>()
    @Volatile private var logFile: File? = null

    fun init(context: Context) {
        val dir = File(context.cacheDir, "logs").apply { mkdirs() }
        logFile = File(dir, "leafrust-debug.log")
        i("App", "Log init · ${context.packageName}")
    }

    fun i(tag: String, message: String) = append("I", tag, message, null)

    fun w(tag: String, message: String) = append("W", tag, message, null)

    fun e(tag: String, message: String, error: Throwable? = null) =
        append("E", tag, message, error)

    private fun append(level: String, tag: String, message: String, error: Throwable?) {
        val ts = SimpleDateFormat("HH:mm:ss.SSS", Locale.US).format(Date())
        val err = error?.let { " · ${it.javaClass.simpleName}: ${it.message}" }.orEmpty()
        val line = "$ts $level/$tag: $message$err"
        when (level) {
            "E" -> Log.e(TAG, "$tag: $message", error)
            "W" -> Log.w(TAG, "$tag: $message")
            else -> Log.i(TAG, "$tag: $message")
        }
        lines.add(line)
        while (lines.size > MAX_LINES) lines.poll()
        try {
            logFile?.appendText(line + "\n")
        } catch (_: Exception) {
        }
    }

    fun snapshot(): String = buildString {
        appendLine("LeafRust debug log")
        appendLine(SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date()))
        appendLine("---")
        lines.forEach { appendLine(it) }
    }

    fun share(context: Context) {
        val file = logFile ?: File(context.cacheDir, "logs/leafrust-debug.log").also {
            it.parentFile?.mkdirs()
            logFile = it
        }
        file.writeText(snapshot())
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file,
        )
        val intent = ShareCompat.IntentBuilder(context)
            .setType("text/plain")
            .setStream(uri)
            .setSubject("LeafRust debug log")
            .setText("Логи LeafRust (debug)")
            .setChooserTitle("Отправить логи")
            .createChooserIntent()
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        context.startActivity(intent)
        i("App", "Log share opened · ${file.length()} bytes")
    }

    fun clear() {
        lines.clear()
        logFile?.writeText("")
        i("App", "Log cleared")
    }
}
