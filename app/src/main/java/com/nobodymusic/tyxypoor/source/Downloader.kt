package com.nobodymusic.tyxypoor.source
import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit
object Downloader {
    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()
    fun downloadTo(
        context: Context,
        dirUri: String,
        name: String,
        url: String,
        quality: String = SourceConstants.DEFAULT_QUALITY
    ): Boolean {
        return try {
            val ext: String
            val mime: String
            when (quality) {
                "flac", "flac24bit" -> { ext = "flac"; mime = "audio/flac" }
                else -> { ext = "mp3"; mime = "audio/mpeg" }
            }
            val tree = Uri.parse(dirUri)
            val treeDocId = DocumentsContract.getTreeDocumentId(tree)
            val parentUri = DocumentsContract.buildDocumentUriUsingTree(tree, treeDocId)
            val base = name.removeSuffix(".mp3").removeSuffix(".flac")
            val safe = sanitize(base) + "." + ext
            val newUri = DocumentsContract.createDocument(
                context.contentResolver, parentUri, mime, safe
            ) ?: return false
            val req = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 10)")
                .build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return false
                val body = resp.body ?: return false
                val os = context.contentResolver.openOutputStream(newUri) ?: return false
                os.use { out -> body.byteStream().use { it.copyTo(out) } }
            }
            true
        } catch (e: Exception) {
            false
        }
    }
    fun downloadToPublic(
        context: Context,
        name: String,
        url: String,
        quality: String = SourceConstants.DEFAULT_QUALITY,
        title: String = "",
        artist: String = "",
        album: String = ""
    ): String? {
        val ext = if (quality == "flac" || quality == "flac24bit") "flac" else "mp3"
        val mime = if (ext == "flac") "audio/flac" else "audio/mpeg"
        val base = sanitize(name.removeSuffix(".mp3").removeSuffix(".flac"))
        val fileName = "$base.$ext"
        var ok: java.io.File? = null
        runCatching {
            val musicDir = java.io.File("/sdcard/Music/NobodyMusic")
            if (!musicDir.exists()) musicDir.mkdirs()
            val f = java.io.File(musicDir, fileName)
            val req = Request.Builder().url(url)
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 10)").build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@runCatching
                val body = resp.body ?: return@runCatching
                java.io.FileOutputStream(f).use { out -> body.byteStream().use { it.copyTo(out) } }
            }
            if (f.exists() && f.length() > 0) ok = f
        }
        if (ok == null) {
            runCatching {
                val dir = java.io.File(context.getExternalFilesDir(null), "Music")
                if (!dir.exists()) dir.mkdirs()
                val f = java.io.File(dir, fileName)
                val req = Request.Builder().url(url)
                    .header("User-Agent", "Mozilla/5.0 (Linux; Android 10)").build()
                client.newCall(req).execute().use { resp ->
                    if (!resp.isSuccessful) return@runCatching
                    val body = resp.body ?: return@runCatching
                    java.io.FileOutputStream(f).use { out -> body.byteStream().use { it.copyTo(out) } }
                }
                if (f.exists() && f.length() > 0) ok = f
            }
        }
        return ok?.absolutePath
    }

    private fun sanitize(s: String): String {
        val bad = charArrayOf('/', '\\', ':', '*', '?', '"', '<', '>', '|', '\n', '\r')
        var r = s
        for (c in bad) r = r.replace(c, '_')
        if (r.isBlank()) r = "song_" + System.currentTimeMillis()
        if (r.length > 80) r = r.substring(0, 80)
        return r
    }
}