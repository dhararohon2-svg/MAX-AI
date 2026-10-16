package com.maxai.gamingsuite

import android.app.Service
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast

/** User-invoked, removable in-game utility overlay. Android requires explicit overlay permission. */
class GamingOverlayService : Service() {
    private lateinit var wm: WindowManager
    private var handle: View? = null
    private var panel: View? = null
    private var expanded = false
    private var params: WindowManager.LayoutParams? = null
    private var crosshair: View? = null
    private var panelHeightDp = 480
    private var panelParams: WindowManager.LayoutParams? = null
    private val red = Color.rgb(255, 38, 53)
    private val bg = Color.rgb(7, 7, 12)
    private val line = Color.rgb(66, 27, 34)

    override fun onCreate() {
        super.onCreate()
        if (Build.VERSION.SDK_INT >= 23 && !Settings.canDrawOverlays(this)) { stopSelf(); return }
        startOverlayForeground()
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        showHandle()
    }

    private fun lp(width: Int, height: Int, gravity: Int = Gravity.TOP or Gravity.CENTER_HORIZONTAL): WindowManager.LayoutParams {
        return WindowManager.LayoutParams(width, height, if (Build.VERSION.SDK_INT >= 26) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY else WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT).apply { this.gravity = gravity; y = dp(18) }
    }

    private fun startOverlayForeground() {
        val channelId = "max_ai_overlay"
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= 26) {
            manager.createNotificationChannel(NotificationChannel(channelId, "Gaming overlay", NotificationManager.IMPORTANCE_LOW).apply {
                description = "Keeps the user-enabled gaming control bar available"
            })
        }
        val openApp = PendingIntent.getActivity(this, 10, Intent(this, MainActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        @Suppress("DEPRECATION")
        val notification = if (Build.VERSION.SDK_INT >= 26) Notification.Builder(this, channelId) else Notification.Builder(this)
        notification.setSmallIcon(android.R.drawable.ic_menu_manage)
            .setContentTitle("MAX AI gaming panel is active")
            .setContentText("Tap the slim red bar to open controls. Tap × to stop the overlay.")
            .setContentIntent(openApp).setOngoing(true)
        startForeground(7104, notification.build())
    }

    private fun showHandle() {
        val bar = TextView(this).apply {
            text = "━"; textSize = 17f; gravity = Gravity.CENTER; setTextColor(Color.WHITE)
            background = shape(red, red, 99)
            contentDescription = "Open MAX AI gaming panel"
            setOnClickListener { if (expanded) collapse() else expand() }
        }
        val p = lp(dp(92), dp(13), Gravity.TOP or Gravity.CENTER_HORIZONTAL).apply { y = dp(8) }
        bar.setOnTouchListener(object : View.OnTouchListener {
            var downX = 0f; var downY = 0f; var startX = 0; var startY = 0; var dragged = false
            override fun onTouch(v: View, e: MotionEvent): Boolean {
                when (e.action) {
                    MotionEvent.ACTION_DOWN -> { downX = e.rawX; downY = e.rawY; startX = p.x; startY = p.y; dragged = false; return true }
                    MotionEvent.ACTION_MOVE -> {
                        val dx = (e.rawX - downX).toInt(); val dy = (e.rawY - downY).toInt()
                        if (kotlin.math.abs(dx) + kotlin.math.abs(dy) > dp(5)) dragged = true
                        p.x = startX - dx; p.y = (startY + dy).coerceAtLeast(0)
                        runCatching { wm.updateViewLayout(bar, p) }; return true
                    }
                    MotionEvent.ACTION_UP -> { if (!dragged) { if (expanded) collapse() else expand() }; return true }
                }
                return false
            }
        })
        handle = bar; params = p
        runCatching { wm.addView(bar, p) }.onFailure { stopSelf() }
    }

    private fun expand() {
        if (expanded || handle == null) return
        expanded = true
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; setPadding(dp(12), dp(12), dp(12), dp(12))
            background = shape(bg, red, 18)
        }
        val header = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        header.addView(label("ZX ELITE  //  MAX AI", 16, red, true), LinearLayout.LayoutParams(0, -2, 1f))
        header.addView(button("−") { collapse() }, LinearLayout.LayoutParams(dp(42), dp(38)))
        header.addView(button("×") { stopSelf() }, LinearLayout.LayoutParams(dp(42), dp(38)))
        // Drag the panel by its title bar; the small red handle remains independently draggable.
        header.setOnTouchListener(object : View.OnTouchListener {
            var downX = 0f; var downY = 0f; var startX = 0; var startY = 0
            override fun onTouch(v: View, e: MotionEvent): Boolean {
                val pp = panelParams ?: return false
                when (e.action) {
                    MotionEvent.ACTION_DOWN -> { downX = e.rawX; downY = e.rawY; startX = pp.x; startY = pp.y; return true }
                    MotionEvent.ACTION_MOVE -> { pp.x = startX + (e.rawX - downX).toInt(); pp.y = (startY + (e.rawY - downY).toInt()).coerceAtLeast(0); runCatching { panel?.let { wm.updateViewLayout(it, pp) } }; return true }
                    MotionEvent.ACTION_UP -> return true
                }
                return false
            }
        })
        root.addView(header)
        root.addView(label("GAMING PROFILE", 11, Color.LTGRAY, true).apply { setPadding(0, dp(8), 0, dp(5)) })
        val modes = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        listOf("BATTERY", "BALANCE", "TURBO").forEach { mode ->
            modes.addView(button(mode) {
                getSharedPreferences("max_ai_preferences", MODE_PRIVATE).edit().putString("gaming_profile", when (mode) { "BATTERY" -> "Battery Saver"; "TURBO" -> "Performance Guidance"; else -> "Smart Balanced" }).apply()
                Toast.makeText(this, "$mode profile selected. Android/OEM may not permit direct CPU/GPU clock changes.", Toast.LENGTH_LONG).show()
            }, LinearLayout.LayoutParams(0, dp(42), 1f).apply { setMargins(dp(2), 0, dp(2), 0) })
        }
        root.addView(modes)
        root.addView(label("SENSITIVITY PRESET", 12, Color.WHITE, true).apply { setPadding(0, dp(12), 0, dp(4)) })
        root.addView(label("Personal preset only — this cannot change sensitivity inside every game automatically.", 10, Color.LTGRAY, false))
        val sensitivityValue = label("Sensitivity: ${getSharedPreferences("max_ai_preferences", MODE_PRIVATE).getInt("sensitivity_level", 6)}/10", 12, red, true)
        root.addView(sensitivityValue)
        val sensitivity = SeekBar(this).apply {
            max = 9
            progress = (getSharedPreferences("max_ai_preferences", MODE_PRIVATE).getInt("sensitivity_level", 6) - 1).coerceIn(0, 9)
            progressTintList = android.content.res.ColorStateList.valueOf(red)
            thumbTintList = android.content.res.ColorStateList.valueOf(red)
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    val level = progress + 1
                    sensitivityValue.text = "Sensitivity: $level/10 — ${when { level <= 3 -> "Low"; level <= 7 -> "Balanced"; else -> "High" }}"
                    if (fromUser) getSharedPreferences("max_ai_preferences", MODE_PRIVATE).edit().putInt("sensitivity_level", level).apply()
                }
                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            })
        }
        root.addView(sensitivity, LinearLayout.LayoutParams(-1, dp(40)))
        val sensitivityPresets = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        listOf("LOW" to 3, "BALANCED" to 6, "HIGH" to 9).forEach { (name, level) ->
            sensitivityPresets.addView(button(name) {
                getSharedPreferences("max_ai_preferences", MODE_PRIVATE).edit().putInt("sensitivity_level", level).apply()
                sensitivity.progress = level - 1
                sensitivityValue.text = "Sensitivity: $level/10 — $name"
            }, LinearLayout.LayoutParams(0, dp(36), 1f).apply { setMargins(dp(2), dp(2), dp(2), dp(2)) })
        }
        root.addView(sensitivityPresets)
        root.addView(label("ADVANCED TOUCH PROFILE", 12, red, true).apply { setPadding(0, dp(10), 0, dp(4)) })
        val touchPrefs = getSharedPreferences("max_ai_preferences", MODE_PRIVATE)
        listOf("FPS / REFRESH MONITOR", "NETWORK LATENCY MONITOR", "THERMAL & BATTERY GUARD", "CUSTOM CROSSHAIR", "EDGE TRIGGER / GESTURES", "PER-GAME PROFILE", "SCREEN RECORDING SHORTCUT").forEach { feature ->
            val key = "gaming_feature_" + feature.lowercase().replace(Regex("[^a-z0-9]+"), "_")
            val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
            row.addView(label(feature, 10, Color.WHITE, false), LinearLayout.LayoutParams(0, dp(34), 1f))
            row.addView(button(if (touchPrefs.getBoolean(key, feature == "THERMAL & BATTERY GUARD")) "ON" else "OFF") {
                val next = !touchPrefs.getBoolean(key, feature == "THERMAL & BATTERY GUARD")
                touchPrefs.edit().putBoolean(key, next).apply()
                Toast.makeText(this, "$feature ${if (next) "enabled" else "disabled"}", Toast.LENGTH_SHORT).show()
                if (feature == "CUSTOM CROSSHAIR") toggleCrosshair(next)
                if (feature == "SCREEN RECORDING SHORTCUT" && next) openScreenRecording()
            }, LinearLayout.LayoutParams(dp(68), dp(32)))
            root.addView(row)
        }
        root.addView(label("PER-GAME PROFILE CONFIG", 11, red, true).apply { setPadding(0, dp(8), 0, dp(3)) })
        val packageInput = EditText(this).apply {
            hint = "Game package name (e.g. com.example.game)"; textSize = 11f; setSingleLine(true)
            setTextColor(Color.WHITE); setHintTextColor(Color.GRAY); background = shape(Color.rgb(18, 12, 16), line, 8)
            setPadding(dp(8), dp(2), dp(8), dp(2))
        }
        root.addView(packageInput, LinearLayout.LayoutParams(-1, dp(42)))
        val profileButtons = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        listOf("BATTERY", "BALANCE", "PERFORMANCE").forEach { mode -> profileButtons.addView(button(mode) {
            val pkg = packageInput.text.toString().trim()
            if (pkg.isBlank() || !pkg.contains('.')) Toast.makeText(this, "Enter a valid game package name first", Toast.LENGTH_SHORT).show()
            else { touchPrefs.edit().putString("game_profile_$pkg", mode).apply(); Toast.makeText(this, "Saved $mode profile for $pkg. Profile is advisory; it cannot force game graphics/FPS settings.", Toast.LENGTH_LONG).show() }
        }, LinearLayout.LayoutParams(0, dp(34), 1f).apply { setMargins(dp(2), dp(3), dp(2), dp(3)) }) }
        root.addView(profileButtons)
        root.addView(label("PANEL SIZE", 11, Color.LTGRAY, true).apply { setPadding(0, dp(8), 0, 0) })
        val sizeValue = label("Height: ${panelHeightDp} dp", 10, red, false)
        root.addView(sizeValue)
        root.addView(SeekBar(this).apply {
            max = 520; progress = (panelHeightDp - 300).coerceIn(0, 520)
            progressTintList = android.content.res.ColorStateList.valueOf(red); thumbTintList = android.content.res.ColorStateList.valueOf(red)
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    panelHeightDp = 300 + progress; sizeValue.text = "Height: ${panelHeightDp} dp"
                    if (fromUser) { panelParams?.height = dp(panelHeightDp); runCatching { panel?.let { view -> panelParams?.let { params -> wm.updateViewLayout(view, params) } } } }
                }
                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            })
        }, LinearLayout.LayoutParams(-1, dp(34)))
        root.addView(label("QUICK GAMING TOOLS", 12, Color.WHITE, true).apply { gravity = Gravity.CENTER; setPadding(0, dp(10), 0, dp(7)) })
        val tools = listOf(
            "SPEED BOOST" to { openSettings(Settings.ACTION_BATTERY_SAVER_SETTINGS, "Battery saver settings unavailable") },
            "GAME MODE" to { Toast.makeText(this, "Game Mode selected. Use your phone's built-in Game Space/Game Turbo if available.", Toast.LENGTH_LONG).show() },
            "PING FIX" to { openWifi() },
            "TOUCH FIX" to { Toast.makeText(this, "Touch sensitivity is controlled by Android/device settings; no hidden touch injection is used.", Toast.LENGTH_LONG).show() },
            "FPS METER" to { Toast.makeText(this, "Android does not let ordinary apps read another game's exact FPS universally. Use OEM/game FPS tools if available.", Toast.LENGTH_LONG).show() },
            "RAM CLEAN" to { openSettings(Settings.ACTION_INTERNAL_STORAGE_SETTINGS, "Storage settings unavailable") },
            "STABILIZE" to { openSettings(Settings.ACTION_DISPLAY_SETTINGS, "Display settings unavailable") },
            "SENSI" to { openSettings(Settings.ACTION_DISPLAY_SETTINGS, "Display settings unavailable") },
            "CROSSHAIR" to { toggleCrosshair(crosshair == null) },
            "CPU STATS" to { showCpuStats() },
            "NETWORK CHECK" to { checkLatency() },
            "REFRESH RATE" to { showRefreshRate() },
            "THERMAL GUARD" to { showThermalBattery() },
            "SCREEN RECORD" to { openScreenRecording() },
            "SCREEN LOCK" to { Toast.makeText(this, "Use Android screen timeout settings to prevent unwanted sleep; this panel does not lock game controls.", Toast.LENGTH_LONG).show() },
            "NOTIFICATIONS" to { openSettings("android.settings.APP_NOTIFICATION_SETTINGS", "Notification settings unavailable") },
            "BRIGHTNESS" to { openSettings(Settings.ACTION_DISPLAY_SETTINGS, "Display settings unavailable") }
        )
        tools.chunked(2).forEach { pair ->
            val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            pair.forEach { (name, action) -> row.addView(button(name, action), LinearLayout.LayoutParams(0, dp(46), 1f).apply { setMargins(dp(3), dp(3), dp(3), dp(3)) }) }
            if (pair.size == 1) row.addView(View(this), LinearLayout.LayoutParams(0, dp(46), 1f))
            root.addView(row)
        }
        root.addView(label("Read-only utilities • device support varies", 10, Color.LTGRAY, false).apply { gravity = Gravity.CENTER; setPadding(0, dp(6), 0, 0) })
        val scroll = ScrollView(this).apply { isFillViewport = true; addView(root) }
        val metrics = resources.displayMetrics
        val width = (metrics.widthPixels - dp(24)).coerceAtMost(dp(410))
        val height = (metrics.heightPixels - dp(90)).coerceAtMost(dp(480))
        panel = scroll
        panelParams = lp(width, dp(panelHeightDp).coerceAtMost(height), Gravity.TOP or Gravity.CENTER_HORIZONTAL).apply { y = dp(28) }
        runCatching { wm.addView(scroll, panelParams) }
    }

    private fun collapse() {
        expanded = false
        panel?.let { runCatching { wm.removeView(it) } }; panel = null; panelParams = null
    }

    private fun toggleCrosshair(enable: Boolean) {
        if (enable && crosshair == null) {
            val v = TextView(this).apply { text = "+"; textSize = 30f; gravity = Gravity.CENTER; setTextColor(red); setShadowLayer(dp(5).toFloat(), 0f, 0f, red); contentDescription = "MAX AI custom crosshair; drag to reposition" }
            val p = lp(dp(64), dp(64), Gravity.CENTER)
            v.setOnTouchListener(object : View.OnTouchListener {
                var x = 0f; var y = 0f; var sx = 0; var sy = 0
                override fun onTouch(view: View, e: MotionEvent): Boolean { when (e.action) { MotionEvent.ACTION_DOWN -> { x=e.rawX; y=e.rawY; sx=p.x; sy=p.y; return true }; MotionEvent.ACTION_MOVE -> { p.x=sx+(e.rawX-x).toInt(); p.y=sy+(e.rawY-y).toInt(); runCatching { wm.updateViewLayout(v,p) }; return true }; MotionEvent.ACTION_UP -> return true }; return false }
            })
            crosshair = v; runCatching { wm.addView(v,p) }.onFailure { crosshair = null; Toast.makeText(this, "Overlay permission required", Toast.LENGTH_SHORT).show() }
            Toast.makeText(this, "Crosshair shown. Some games prohibit overlays; disable it if it conflicts with game rules.", Toast.LENGTH_LONG).show()
        } else if (!enable) { crosshair?.let { runCatching { wm.removeView(it) } }; crosshair = null }
    }
    private fun showRefreshRate() {
        @Suppress("DEPRECATION")
        val rate = runCatching { (getSystemService(WINDOW_SERVICE) as WindowManager).defaultDisplay.refreshRate }.getOrDefault(0f)
        Toast.makeText(this, "Display refresh rate: ${"%.1f".format(rate)} Hz. This is not the game's measured FPS.", Toast.LENGTH_LONG).show()
    }
    private fun showThermalBattery() {
        val battery = getSystemService(BATTERY_SERVICE) as BatteryManager
        val pct = battery.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        val thermal = if (Build.VERSION.SDK_INT >= 29) { val pm = getSystemService(POWER_SERVICE) as android.os.PowerManager; when (pm.currentThermalStatus) { 0 -> "none"; 1 -> "light"; 2 -> "moderate"; 3 -> "severe"; 4 -> "critical"; 5 -> "emergency"; else -> "unknown" } } else "not available on this Android version"
        Toast.makeText(this, "Battery: ${if (pct in 0..100) "$pct%" else "N/A"} • Thermal status: $thermal. Guard gives warnings; Android thermal protection remains in control.", Toast.LENGTH_LONG).show()
    }
    private fun checkLatency() {
        Toast.makeText(this, "Checking TCP connection latency to 1.1.1.1:443…", Toast.LENGTH_SHORT).show()
        Thread {
            val ms = runCatching { val start = System.nanoTime(); java.net.Socket().use { it.connect(java.net.InetSocketAddress("1.1.1.1", 443), 2500) }; (System.nanoTime()-start)/1_000_000 }.getOrNull()
            android.os.Handler(mainLooper).post { Toast.makeText(this, ms?.let { "TCP connect: ${it} ms (not game-server ping)" } ?: "Connection test failed or timed out", Toast.LENGTH_LONG).show() }
        }.start()
    }
    private fun openScreenRecording() {
        // Android does not expose a universal public API to start recording another app silently.
        val intents = listOf("android.settings.SCREEN_RECORDING_SETTINGS", "com.android.settings.SCREEN_RECORDING_SETTINGS")
        for (action in intents) { if (runCatching { startActivity(Intent(action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); true }.getOrDefault(false)) return }
        Toast.makeText(this, "Open Quick Settings and choose Screen Record. Android requires the system capture consent flow.", Toast.LENGTH_LONG).show()
        openSettings(Settings.ACTION_SETTINGS, "Open Quick Settings and use Screen Record")
    }

    private fun showCpuStats() {
        val battery = getSystemService(BATTERY_SERVICE) as BatteryManager
        val pct = battery.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        val cpu = runCatching { java.io.File("/sys/devices/system/cpu/cpu0/cpufreq/scaling_cur_freq").readText().trim().toLong() / 1000 }.getOrNull()
        Toast.makeText(this, "Battery: ${if (pct in 0..100) "$pct%" else "N/A"} • CPU0: ${cpu?.let { "$it MHz" } ?: "not exposed"} • GPU/FPS may be restricted", Toast.LENGTH_LONG).show()
    }
    private fun openWifi() = openSettings(Settings.ACTION_WIFI_SETTINGS, "Wi-Fi settings unavailable")
    private fun openSettings(action: String, fallback: String) {
        runCatching { startActivity(Intent(action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }.onFailure { Toast.makeText(this, fallback, Toast.LENGTH_SHORT).show() }
    }
    private fun button(text: String, action: () -> Unit): Button = Button(this).apply {
        this.text = text; textSize = 10f; isAllCaps = false; setTextColor(Color.WHITE); setOnClickListener { action() }
        background = shape(Color.rgb(24, 12, 17), line, 10)
        minHeight = 0; minimumHeight = 0; setPadding(dp(3), 0, dp(3), 0)
    }
    private fun label(text: String, size: Int, color: Int, bold: Boolean) = TextView(this).apply {
        this.text = text; textSize = size.toFloat(); setTextColor(color); if (bold) typeface = Typeface.DEFAULT_BOLD
    }
    private fun shape(fill: Int, stroke: Int, radius: Int) = GradientDrawable().apply {
        setColor(fill); cornerRadius = dp(radius).toFloat(); setStroke(dp(1), stroke)
    }
    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt().coerceAtLeast(1)
    override fun onDestroy() { collapse(); crosshair?.let { runCatching { wm.removeView(it) } }; crosshair = null; handle?.let { runCatching { wm.removeView(it) } }; handle = null; super.onDestroy() }
    override fun onBind(intent: Intent?): IBinder? = null
}
