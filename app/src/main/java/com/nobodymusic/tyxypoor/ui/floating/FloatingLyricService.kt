package com.nobodymusic.tyxypoor.ui.floating

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import com.nobodymusic.tyxypoor.MainActivity
import com.nobodymusic.tyxypoor.data.LyricLine
import com.nobodymusic.tyxypoor.data.Lyrics
import com.nobodymusic.tyxypoor.ui.AppState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class FloatingLyricService : Service() {

    companion object {
        const val MODE_DESKTOP = "desktop"
        const val MODE_ISLAND = "island"
        const val CHANNEL_ID = "nobody_float"
        const val NOTIF_ID = 4001
        private const val PREFS = "float_prefs"

        fun start(ctx: Context, mode: String) {
            val i = Intent(ctx, FloatingLyricService::class.java).putExtra("mode", mode)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) ctx.startForegroundService(i) else ctx.startService(i)
        }

        fun stop(ctx: Context) {
            ctx.stopService(Intent(ctx, FloatingLyricService::class.java))
        }
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var wm: WindowManager? = null
    private var root: View? = null
    private var lp: WindowManager.LayoutParams? = null
    private var mainText: TextView? = null
    private var subText: TextView? = null
    private var titleText: TextView? = null
    private var job: Job? = null
    private var mode = MODE_DESKTOP
    private var lastLineIdx = -1

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        startForegroundNotif()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        mode = intent?.getStringExtra("mode") ?: prefs().getString("mode", MODE_DESKTOP) ?: MODE_DESKTOP
        prefs().edit().putString("mode", mode).apply()
        buildView()
        startTick()
        return START_STICKY
    }

    private fun prefs(): SharedPreferences = getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun dp(v: Float): Int = (v * resources.displayMetrics.density).toInt()

    private fun buildView() {
        removeView()
        val mgr = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        wm = mgr
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14f), dp(8f), dp(14f), dp(8f))
        }
        val app = AppState.instance
        if (mode == MODE_ISLAND) {
            container.background = android.graphics.drawable.GradientDrawable().apply {
                setColor(Color.parseColor("#E6101010"))
                cornerRadius = dp(24f).toFloat()
                setStroke(dp(1f), Color.parseColor("#33FFFFFF"))
            }
            titleText = TextView(this).apply {
                setTextColor(Color.WHITE)
                textSize = 11f
                maxLines = 1
                text = "Nobody Music"
            }
            mainText = TextView(this).apply {
                setTextColor(Color.parseColor("#80D8FF"))
                textSize = 14f
                maxLines = 1
                ellipsize = android.text.TextUtils.TruncateAt.MARQUEE
                marqueeRepeatLimit = -1
                isSingleLine = true
                text = "暂无歌词"
            }
            container.addView(titleText)
            container.addView(mainText)
        } else {
            container.background = android.graphics.drawable.GradientDrawable().apply {
                setColor(Color.parseColor("#CC000000"))
                cornerRadius = dp(16f).toFloat()
            }
            mainText = TextView(this).apply {
                setTextColor(Color.WHITE)
                textSize = 18f
                maxLines = 1
                ellipsize = android.text.TextUtils.TruncateAt.MARQUEE
                marqueeRepeatLimit = -1
                isSingleLine = true
                setTypeface(typeface, Typeface.BOLD)
                text = "桌面歌词已开启"
            }
            subText = TextView(this).apply {
                setTextColor(Color.parseColor("#AAAAAA"))
                textSize = 12f
                maxLines = 1
                text = ""
            }
            container.addView(mainText)
            container.addView(subText)
        }

        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else WindowManager.LayoutParams.TYPE_PHONE

        val w = if (mode == MODE_ISLAND) WindowManager.LayoutParams.WRAP_CONTENT else dp(300f)
        val h = WindowManager.LayoutParams.WRAP_CONTENT
        val p = WindowManager.LayoutParams(w, h, type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT)
        p.gravity = if (mode == MODE_ISLAND) Gravity.TOP or Gravity.CENTER_HORIZONTAL else Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
        p.y = if (mode == MODE_ISLAND) dp(40f) else dp(140f)
        lp = p
        attachDrag(container)
        container.setOnClickListener {
            runCatching {
                startActivity(Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
        }
        try {
            mgr.addView(container, p)
            root = container
        } catch (e: Exception) {
            android.util.Log.e("FloatLyric", "addView failed: ${e.message}", e)
            stopSelf()
        }
    }

    private fun attachDrag(v: View) {
        var downX = 0f; var downY = 0f; var ox = 0; var oy = 0
        v.setOnTouchListener { _, e ->
            val p = lp ?: return@setOnTouchListener false
            when (e.action) {
                MotionEvent.ACTION_DOWN -> {
                    downX = e.rawX; downY = e.rawY; ox = p.x; oy = p.y; false
                }
                MotionEvent.ACTION_MOVE -> {
                    p.x = ox + (e.rawX - downX).toInt()
                    p.y = if (mode == MODE_ISLAND) oy + (e.rawY - downY).toInt()
                    else oy - (e.rawY - downY).toInt()
                    runCatching { wm?.updateViewLayout(root, p) }
                    true
                }
                else -> false
            }
        }
    }

    private fun startTick() {
        job?.cancel()
        job = scope.launch {
            while (true) {
                try { update() } catch (_: Exception) { }
                delay(500)
            }
        }
    }

    private fun update() {
        val app = AppState.instance ?: return
        val song = app.player.current.value
        if (mode == MODE_DESKTOP && subText != null) {
            subText?.text = song?.let { "${it.title} - ${it.artist}" } ?: ""
        }
        if (mode == MODE_ISLAND && titleText != null) {
            titleText?.text = song?.let { it.title } ?: "Nobody Music"
        }
        val lines: List<LyricLine> = app.lyric.value
        val text = if (lines.isEmpty()) {
            (if (song != null) "♪ ${song.title}" else "暂无歌词")
        } else {
            val pos = app.player.position.value
            val idx = Lyrics.indexAt(lines, pos).coerceIn(0, lines.size - 1)
            if (idx != lastLineIdx) { lastLineIdx = idx }
            lines[idx].text.ifBlank { "♪" }
        }
        if (mainText?.text?.toString() != text) mainText?.text = text
    }

    private fun removeView() {
        root?.let { runCatching { wm?.removeView(it) } }
        root = null
    }

    private fun startForegroundNotif() {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(CHANNEL_ID, "悬浮歌词", NotificationManager.IMPORTANCE_LOW)
            ch.setShowBadge(false)
            nm.createNotificationChannel(ch)
        }
        val pi = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val n = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
                .setContentTitle("悬浮歌词运行中")
                .setContentText("点击返回 Nobody Music")
                .setSmallIcon(android.R.drawable.ic_media_play)
                .setContentIntent(pi)
                .build()
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
                .setContentTitle("悬浮歌词运行中")
                .setContentText("点击返回 Nobody Music")
                .setSmallIcon(android.R.drawable.ic_media_play)
                .setContentIntent(pi)
                .build()
        }
        startForeground(NOTIF_ID, n)
    }

    override fun onDestroy() {
        job?.cancel()
        removeView()
        super.onDestroy()
    }
}
