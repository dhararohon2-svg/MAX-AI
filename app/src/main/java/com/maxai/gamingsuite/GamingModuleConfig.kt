package com.maxai.gamingsuite

import android.content.Context

/**
 * MAX AI unified Gaming Module configuration.
 * Stores user-selected preferences only. It deliberately does not write hidden system properties,
 * spoof device identity, change CPU/GPU governors, bypass thermal limits, or alter another game's
 * private graphics settings: those require OEM/root/game-specific privileges and cannot be safely
 * promised by a normal Android application.
 */
object GamingModuleConfig {
    const val PREFS = "max_ai_gaming_module_config"
    const val MASTER_ENABLED = "master_enabled"

    data class Choice(val key: String, val title: String, val options: List<String>, val defaultValue: String, val note: String)
    data class Toggle(val key: String, val title: String, val defaultValue: Boolean, val note: String)
    data class Group(val title: String, val choices: List<Choice> = emptyList(), val toggles: List<Toggle> = emptyList())

    val groups = listOf(
        Group("CPU & Governors", listOf(
            Choice("cpu_profile", "CPU profile", listOf("Battery Saver", "Balanced", "Performance guidance"), "Balanced", "Advisory profile only; CPU clocks/governor are OEM/root controlled."),
            Choice("cpu_frequency_cap", "Desired CPU frequency cap", listOf("Auto", "Low", "Medium", "High", "Maximum available"), "Auto", "A target preference only; this app cannot force hardware frequency."),
            Choice("core_policy", "Core scheduling preference", listOf("System default", "Efficiency first", "Performance first"), "System default", "Does not pin cores or change scheduler policy.")
        ), listOf(
            Toggle("realtime_ui_thread", "Realtime UI thread priority", false, "Not applied: elevated scheduler priority is restricted."),
            Toggle("keep_big_cores_awake", "Keep performance cores awake", false, "Not applied: ordinary apps cannot control core online state.")
        )),
        Group("GPU & Rendering", listOf(
            Choice("gpu_profile", "GPU profile", listOf("System default", "Efficiency", "Performance guidance"), "System default", "GPU governor/clock changes are not available to normal apps."),
            Choice("gpu_level", "Desired GPU performance level", listOf("Auto", "Level 1", "Level 2", "Level 3", "Level 4"), "Auto", "Saved as a per-game preference; does not change GPU clocks."),
            Choice("render_api", "Graphics API preference", listOf("Game default", "OpenGL ES", "Vulkan (if game supports it)"), "Game default", "The game itself chooses its rendering API."),
            Choice("anti_aliasing", "Anti-aliasing preference", listOf("Game default", "Off / performance", "Low", "High quality"), "Game default", "Cannot override another app's graphics settings universally."),
            Choice("shadow_quality", "Shadow quality preference", listOf("Game default", "Low", "Balanced", "High"), "Game default", "Guidance only; configure inside the game when available."),
            Choice("texture_filtering", "Texture filtering preference", listOf("Game default", "Bilinear", "Trilinear", "Anisotropic"), "Game default", "Not injected into game rendering."),
            Choice("render_thread", "Render thread preference", listOf("System default", "Balanced", "High priority guidance"), "System default", "No privileged thread-priority injection is performed."),
            Choice("shader_cache", "Shader cache preference", listOf("Game default", "Default caching", "Precompile if game supports it"), "Game default", "Shader compilation is controlled by the game/driver.")
        ), listOf(
            Toggle("force_hw_2d", "Force HW 2D composition", false, "Does not write debug/system properties."),
            Toggle("zero_lag", "Zero-lag render-buffer mode", false, "No render queue changes are injected."),
            Toggle("hardware_frame_pacing", "Hardware frame-pacing preference", true, "Guidance only; V-Sync/frame pacing is game-controlled."),
            Toggle("force_32bit_color", "32-bit color preference", false, "Cannot override a game's surface format."),
            Toggle("vulkan_subpass", "Vulkan subpass merging preference", false, "No Vulkan driver flags are injected."),
            Toggle("surfaceflinger_queue", "SurfaceFlinger queue bypass", false, "Unsupported for ordinary apps; no bypass is attempted."),
            Toggle("shader_precompile", "Aggressive shader pre-compilation", false, "Enabled only as a preference; no background injection.")
        )),
        Group("GFX Engine Pro", listOf(
            Choice("resolution_scale", "Resolution scale preference", listOf("Game default", "50%", "60%", "75%", "100%"), "Game default", "Applies only if the game offers its own resolution option."),
            Choice("fps_target", "FPS target preference", listOf("Game default", "30 FPS", "45 FPS", "60 FPS", "90 FPS", "120 FPS"), "Game default", "The panel cannot unlock unsupported FPS modes."),
            Choice("refresh_target", "Display refresh target", listOf("System default", "60 Hz", "90 Hz", "120 Hz", "Highest supported"), "System default", "Actual refresh rate depends on device settings and supported modes."),
            Choice("gfx_backend", "Graphics backend", listOf("Game default", "OpenGL ES", "Vulkan if supported"), "Game default", "Requires the game to expose this setting."),
            Choice("msaa", "MSAA preference", listOf("Game default", "Disabled", "2x", "4x", "8x"), "Game default", "Not applied to other apps by this module."),
            Choice("anisotropic", "Anisotropic filtering", listOf("Game default", "Bilinear 2x", "4x", "8x", "16x"), "Game default", "Game/driver support required."),
            Choice("resolution_mode", "Resolution mode", listOf("Native", "720p preference", "1080p preference", "Dynamic resolution preference"), "Native", "Saved profile only; no display-size override is executed.")
        ), listOf(
            Toggle("adaptive_profile", "Adaptive performance recommendations", true, "Recommendations use available battery/thermal information."),
            Toggle("frame_pacing_guide", "Frame pacing guidance", true, "Displays guidance, not a game FPS modification."),
            Toggle("fps_monitor", "FPS / refresh-rate monitor", true, "Refresh rate is measurable; exact third-party game FPS may not be."),
            Toggle("crosshair", "Custom crosshair overlay", false, "Requires Android Display over other apps permission."),
            Toggle("edge_gestures", "Edge trigger / gesture helpers", false, "Only gestures received by this overlay can be handled; no touch injection."),
            Toggle("draggable_panel", "Resizable / draggable panel", true, "Moves/resizes this app's overlay only.")
        )),
        Group("RAM & VM Tuning", listOf(
            Choice("memory_profile", "Memory profile", listOf("System default", "Battery friendly", "Balanced", "Performance guidance"), "Balanced", "Android manages process memory; no force-cleaning other apps."),
            Choice("background_policy", "Background app guidance", listOf("System default", "Keep apps as-is", "Review unused apps"), "System default", "No process killing or hidden app restrictions are applied.")
        ), listOf(
            Toggle("memory_monitor", "RAM usage monitor", true, "Shows available process/system memory data only."),
            Toggle("ram_clean", "RAM clean shortcut", false, "Opens Android settings; does not kill other apps."),
            Toggle("storage_monitor", "Storage monitor", true, "Reads available storage statistics.")
        )),
        Group("Touch Latency & Sensitivity", listOf(
            Choice("touch_sensitivity", "Sensitivity profile", listOf("1 - Very Low", "2", "3 - Low", "4", "5", "6 - Balanced", "7", "8", "9 - High", "10 - Very High"), "6 - Balanced", "A saved personal preset; actual in-game sensitivity is configured in each game."),
            Choice("touch_response", "Touch response preference", listOf("Default", "Precision", "Fast response"), "Default", "Cannot change digitizer sampling rate on all devices."),
            Choice("aim_profile", "Aim / camera profile", listOf("General", "FPS", "MOBA", "Racing"), "General", "Per-game recommendation only.")
        ), listOf(
            Toggle("touch_latency_monitor", "Touch/latency guidance", true, "No touch-event interception outside the overlay."),
            Toggle("edge_trigger", "Edge trigger helper", false, "Does not suppress system navigation gestures."),
            Toggle("gesture_controls", "Panel gesture controls", true, "Only controls this overlay panel.")
        )),
        Group("Thermal Manager & Battery", listOf(
            Choice("thermal_policy", "Thermal response", listOf("Notify only", "Balanced recommendations", "Battery saver recommendations"), "Balanced recommendations", "Cannot disable thermal throttling or safety limits."),
            Choice("battery_policy", "Battery profile", listOf("System default", "Battery Saver", "Balanced", "Performance guidance"), "Balanced", "Can guide/open system settings; does not force OEM performance mode.")
        ), listOf(
            Toggle("thermal_guard", "Thermal guard alerts", true, "Uses Android thermal status when available."),
            Toggle("battery_guard", "Low battery guard", true, "Uses battery level/status and shows recommendations."),
            Toggle("temperature_monitor", "Temperature monitor", true, "Available sensor readings vary by OEM."),
            Toggle("screen_record_shortcut", "Screen recording shortcut", true, "Launches Android/system capture UI; user consent is required."),
            Toggle("network_latency", "Network latency monitor", true, "Measures TCP connection time, not universal game-server ping.")
        )),
        Group("Device Profile & Compatibility", listOf(
            Choice("device_profile", "Compatibility profile", listOf("Actual device", "Generic compatibility notes", "Game-specific notes"), "Actual device", "A UI profile only; does not spoof model, GPU vendor, or build properties.")
        ), listOf(
            Toggle("spoof_gpu_vendor", "GPU vendor/model spoofing", false, "Unsupported and intentionally not implemented; avoids false device identity."),
            Toggle("bypass_game_throttle", "Bypass game SDK throttling", false, "Unsupported; no vendor flags or throttling bypass are applied."),
            Toggle("override_game_mode_flags", "Override system game-mode flags", false, "Unsupported without system/OEM privileges; use Android/OEM Game Mode.")
        ))
    )

    fun isEnabled(context: Context): Boolean = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(MASTER_ENABLED, true)
    fun setEnabled(context: Context, enabled: Boolean) { context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(MASTER_ENABLED, enabled).apply() }
    fun getChoice(context: Context, choice: Choice): String = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(choice.key, choice.defaultValue) ?: choice.defaultValue
    fun setChoice(context: Context, choice: Choice, value: String) {
        if (value in choice.options) context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(choice.key, value).apply()
    }
    fun isToggleOn(context: Context, toggle: Toggle): Boolean = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(toggle.key, toggle.defaultValue)
    fun setToggle(context: Context, toggle: Toggle, enabled: Boolean) { context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(toggle.key, enabled).apply() }
    fun reset(context: Context) { context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().clear().apply() }
}
