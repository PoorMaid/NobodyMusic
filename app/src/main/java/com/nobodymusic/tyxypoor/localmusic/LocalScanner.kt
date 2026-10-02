package com.nobodymusic.tyxypoor.localmusic

import android.content.ContentUris
import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.MediaStore
import com.nobodymusic.tyxypoor.data.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LocalScanner(private val context: Context) {

    suspend fun scan(): List<Song> = withContext(Dispatchers.IO) {
        val out = mutableListOf<Song>()
        val proj = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.IS_MUSIC
        )
        val sel = "${MediaStore.Audio.Media.IS_MUSIC} != 0 AND ${MediaStore.Audio.Media.DURATION} > 1000"
        context.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, proj, sel, null,
            "${MediaStore.Audio.Media.TITLE} ASC"
        )?.use { c ->
            val idCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val durCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val dataCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
            val sizeCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
            while (c.moveToNext()) {
                val id = c.getLong(idCol)
                val uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)
                val path = c.getString(dataCol) ?: ""
                out.add(
                    Song(
                        title = c.getString(titleCol) ?: "未知歌曲",
                        artist = c.getString(artistCol) ?: "未知艺人",
                        album = c.getString(albumCol) ?: "未知专辑",
                        durationMs = c.getLong(durCol),
                        uri = uri.toString(),
                        path = path,
                        size = c.getLong(sizeCol),
                        isLocal = true
                    )
                )
            }
        }
        // 补充：遍历常见音乐目录，补入 MediaStore 未索引的文件
        val seen = out.map { it.path }.toHashSet()
        val exts = setOf("mp3", "flac", "ape", "wav", "m4a", "aac", "ogg", "opus", "wma", "mp4", "dsf", "dff")
        val roots = listOf(
            android.os.Environment.getExternalStorageDirectory(),
            java.io.File("/storage/emulated/0/Music"),
            java.io.File("/storage/emulated/0/Download"),
            java.io.File("/storage/emulated/0/音乐")
        ).filter { it != null && it.exists() }
        for (root in roots) {
            runCatching {
                root.walkTopDown().maxDepth(6).forEach { f ->
                    if (out.size > 20000) return@forEach
                    if (!f.isFile) return@forEach
                    val e = f.extension.lowercase()
                    if (e !in exts) return@forEach
                    if (f.absolutePath in seen) return@forEach
                    seen.add(f.absolutePath)
                    val meta = readMetadata(f.absolutePath)
                    out.add(
                        Song(
                            title = meta["title"] ?: f.nameWithoutExtension,
                            artist = meta["artist"] ?: "未知艺人",
                            album = meta["album"] ?: "未知专辑",
                            durationMs = 0L,
                            uri = f.absolutePath,
                            path = f.absolutePath,
                            size = f.length(),
                            isLocal = true
                        )
                    )
                }
            }
        }
        out
    }

    fun readMetadata(path: String): Map<String, String> {
        val map = mutableMapOf<String, String>()
        try {
            val mmr = MediaMetadataRetriever()
            mmr.setDataSource(path)
            mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)?.let { map["title"] = it }
            mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)?.let { map["artist"] = it }
            mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)?.let { map["album"] = it }
            mmr.release()
        } catch (_: Throwable) {
        }
        return map
    }
}