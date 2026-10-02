package com.nobodymusic.tyxypoor.player
import android.media.audiofx.Equalizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
class EqController {
    private var eq: Equalizer? = null
    private var bands: Short = 0
    private var minLevel: Short = 0
    private var maxLevel: Short = 0
    private val _enabled = MutableStateFlow(false)
    val enabled: StateFlow<Boolean> = _enabled
    private val _ready = MutableStateFlow(false)
    val ready: StateFlow<Boolean> = _ready
    private val _levels = MutableStateFlow<List<Float>>(emptyList())
    val levels: StateFlow<List<Float>> = _levels
    private val _preset = MutableStateFlow(0)
    val preset: StateFlow<Int> = _preset
    val bandLabels: List<String>
        get() {
            if (!_ready.value) return emptyList()
            val e = eq ?: return emptyList()
            val n = e.numberOfBands.toInt()
            val out = mutableListOf<String>()
            for (i in 0 until n) {
                val hz = e.getCenterFreq(i.toShort()) / 1000
                out += if (hz >= 1000) "${hz / 1000}kHz" else "${hz}Hz"
            }
            return out
        }
    fun attachGlobal() {
        attach(0, force = true)
    }

    fun attach(sessionId: Int) { attach(sessionId, force = false) }

    fun attach(sessionId: Int, force: Boolean) {
        if (sessionId == 0 && !force) return
        release()
        try {
            val e = Equalizer(0, sessionId)
            val n = e.numberOfBands
            if (n <= 0) { e.release(); return }
            bands = n
            minLevel = e.bandLevelRange[0]
            maxLevel = e.bandLevelRange[1]
            eq = e
            val cur = mutableListOf<Float>()
            for (i in 0 until n.toInt()) cur += e.getBandLevel(i.toShort()).toFloat()
            _levels.value = cur
            e.enabled = _enabled.value
            _ready.value = true
        } catch (t: Throwable) {
            _ready.value = false
            eq = null
        }
    }
    fun setEnabled(on: Boolean) {
        _enabled.value = on
        eq?.enabled = on
    }
    fun setBand(index: Int, level: Float) {
        val e = eq ?: return
        if (index < 0 || index >= bands) return
        val clamped = level.coerceIn(minLevel.toFloat(), maxLevel.toFloat())
        e.setBandLevel(index.toShort(), clamped.toInt().toShort())
        val cur = _levels.value.toMutableList()
        if (index < cur.size) cur[index] = clamped
        _levels.value = cur
    }
    fun usePreset(p: Int) {
        val e = eq ?: return
        if (p < 0 || p >= e.numberOfPresets) return
        runCatching {
            e.usePreset(p.toShort())
            _preset.value = p
            val cur = mutableListOf<Float>()
            for (i in 0 until bands.toInt()) cur += e.getBandLevel(i.toShort()).toFloat()
            _levels.value = cur
        }
    }
    fun presetNames(): List<String> {
        val e = eq ?: return emptyList()
        val out = mutableListOf<String>()
        runCatching {
            for (i in 0 until e.numberOfPresets) out += e.getPresetName(i.toShort())
        }
        return out
    }
    fun release() {
        runCatching { eq?.release() }
        eq = null
        _ready.value = false
        _levels.value = emptyList()
    }
}