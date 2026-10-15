package com.maxai.gamingsuite

import android.content.Context
import android.hardware.display.DisplayManager
import android.view.Display
import java.io.File
import java.net.InetSocketAddress
import java.net.Socket
import java.util.Locale

/** Best-effort Android telemetry. Values are omitted rather than fabricated when OEMs block access. */
internal object DeviceCapabilityEngine {
    data class FrequencyReading(val currentMHz: Double?, val minMHz: Double?, val maxMHz: Double?, val readableCores: Int)

    fun cpuFrequency(): FrequencyReading {
        val count = Runtime.getRuntime().availableProcessors().coerceIn(1, 256)
        val current = mutableListOf<Double>()
        val mins = mutableListOf<Double>()
        val maxs = mutableListOf<Double>()
        for (i in 0 until count) {
            val root = "/sys/devices/system/cpu/cpu$i/cpufreq"
            readKHz("$root/scaling_cur_freq")?.let { current += it / 1000.0 }
            readKHz("$root/cpuinfo_min_freq")?.let { mins += it / 1000.0 }
            readKHz("$root/cpuinfo_max_freq")?.let { maxs += it / 1000.0 }
        }
        return FrequencyReading(current.takeIf { it.isNotEmpty() }?.average(), mins.minOrNull(), maxs.maxOrNull(), current.size)
    }

    fun gpuFrequencyMHz(): Double? {
        val paths = listOf(
            "/sys/class/kgsl/kgsl-3d0/gpuclk",
            "/sys/class/devfreq/gpu/cur_freq",
            "/sys/devices/platform/18500000.mali/devfreq/18500000.mali/cur_freq",
            "/sys/class/devfreq/17000000.gpu/cur_freq"
        )
        for (path in paths) {
            val raw = runCatching { File(path).readText().trim().toLongOrNull() }.getOrNull() ?: continue
            if (raw <= 0) continue
            // Qualcomm gpuclk is commonly Hz; other devfreq nodes may be Hz or kHz.
            return when {
                raw >= 100_000_000L -> raw / 1_000_000.0
                raw >= 100_000L -> raw / 1000.0
                else -> raw.toDouble()
            }
        }
        return null
    }

    fun displayRefreshRate(context: Context): Float? = runCatching {
        val manager = context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
        manager.getDisplay(Display.DEFAULT_DISPLAY)?.refreshRate?.takeIf { it > 0f }
    }.getOrNull()

    fun tcpConnectLatencyMs(host: String, port: Int, timeoutMs: Int = 4000): Long {
        val cleanHost = host.trim().removePrefix("https://").removePrefix("http://").substringBefore('/').substringBefore(':')
        require(cleanHost.isNotBlank()) { "Enter a host name or IP address." }
        require(port in 1..65535) { "Port must be between 1 and 65535." }
        val started = System.nanoTime()
        Socket().use { socket -> socket.connect(InetSocketAddress(cleanHost, port), timeoutMs) }
        return ((System.nanoTime() - started) / 1_000_000L).coerceAtLeast(0L)
    }

    fun formatMHz(value: Double?): String = value?.let { String.format(Locale.US, "%.0f MHz", it) } ?: "Not exposed"

    private fun readKHz(path: String): Double? = runCatching {
        File(path).readText().trim().toDoubleOrNull()?.takeIf { it > 0 }
    }.getOrNull()
}
