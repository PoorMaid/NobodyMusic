package com.nobodymusic.tyxypoor.data

data class LyricLine(val timeMs: Long, val text: String)

object Lyrics {
    private val timeTag = Regex("\\[(\\d{1,2}):(\\d{1,2})[.:](\\d{1,3})\\]")

    fun parse(raw: String?): List<LyricLine> {
        if (raw.isNullOrBlank()) return emptyList()
        val out = mutableListOf<LyricLine>()
        raw.split('\n').forEach { line ->
            val ms = timeTag.findAll(line).map {
                val m = it.groupValues[1].toLongOrNull() ?: 0L
                val s = it.groupValues[2].toLongOrNull() ?: 0L
                val frac = it.groupValues[3]
                val f = when (frac.length) {
                    1 -> frac.toLongOrNull()?.times(100) ?: 0L
                    2 -> frac.toLongOrNull()?.times(10) ?: 0L
                    else -> frac.toLongOrNull() ?: 0L
                }
                m * 60000L + s * 1000L + f
            }.toList()
            val text = timeTag.replace(line, "").trim()
            if (text.isBlank()) return@forEach
            if (ms.isEmpty()) return@forEach
            ms.forEach { out += LyricLine(it, text) }
        }
        return out.sortedBy { it.timeMs }
    }

    fun indexAt(lines: List<LyricLine>, posMs: Long): Int {
        if (lines.isEmpty()) return -1
        var lo = 0
        var hi = lines.size - 1
        var ans = -1
        while (lo <= hi) {
            val mid = (lo + hi) / 2
            if (lines[mid].timeMs <= posMs) {
                ans = mid
                lo = mid + 1
            } else hi = mid - 1
        }
        return ans
    }
}
