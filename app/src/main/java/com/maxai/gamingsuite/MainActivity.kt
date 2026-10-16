package com.maxai.gamingsuite

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.provider.Settings
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

private val Void = Color(0xFF070A14)
private val SurfaceDeep = Color(0xFF10182B)
private val SurfaceRaised = Color(0xFF18243B)
private val Cyan = Color(0xFF69F5E2)
private val Purple = Color(0xFF9B86FF)
private val Pink = Color(0xFFFF76C8)
private val TextMuted = Color(0xFFB6C1D7)

class MainActivity : ComponentActivity(), TextToSpeech.OnInitListener {
    private var tts: TextToSpeech? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tts = TextToSpeech(this, this)
        setContent {
            MaxAiApp(::speak, onOpenOverlay = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
                    startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, android.net.Uri.parse("package:$packageName")))
                } else {
                    startService(Intent(this, GamingOverlayService::class.java))
                }
            })
        }
    }
    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val engine = tts ?: return
            val language = Locale.getDefault()
            val result = engine.setLanguage(language)
            // Slightly slower, conversational delivery. Actual voice quality depends on the
            // speech engine and installed Bengali/other language voice data.
            engine.setSpeechRate(0.93f)
            engine.setPitch(1.02f)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                engine.language = Locale.US
            }
        }
    }
    private fun speak(value: String) {
        val spoken = SpeechTextCleaner.forSpeech(value)
        if (spoken.isNotBlank()) tts?.speak(spoken, TextToSpeech.QUEUE_FLUSH, null, "max_ai_reply")
    }
    override fun onDestroy() { tts?.stop(); tts?.shutdown(); super.onDestroy() }
}

/** Removes decorative markup/emojis before TTS, while preserving readable words and sentence pauses. */
internal object SpeechTextCleaner {
    fun forSpeech(input: String): String {
        var text = input
            .replace(Regex("```[\\s\\S]*?```"), " code omitted ")
            .replace(Regex("`([^`]+)`"), "$1")
            .replace(Regex("!\\[([^]]*)]\\([^)]*\\)"), "$1")
            .replace(Regex("\\[([^]]+)]\\([^)]*\\)"), "$1")
            .replace(Regex("https?://\\S+"), " link omitted ")
            .replace(Regex("<[^>]*>"), " ")
            .replace(Regex("[*_~#>|]"), " ")
        // Strip emoji/symbol code points, but keep language letters and natural punctuation.
        text = text.replace(Regex("[\\uD800-\\uDBFF][\\uDC00-\\uDFFF]"), " ")
            .replace(Regex("[\\u2600-\\u27BF]"), " ")
            .replace(Regex("[\\uFE0F\\u200D]"), " ")
            .replace(Regex("[^\\p{L}\\p{N}\\p{M}\\s.,!?;:'’()\\-]"), " ")
            .replace(Regex("\\s+([.,!?;:])"), "$1")
            .replace(Regex("([.,!?;:]){2,}"), "$1")
            .replace(Regex("\\s{2,}"), " ")
        return text.trim()
    }
}

private enum class Page(val label: String) {
    HOME("Home"), CHAT("AI Chat"), GAMES("Games"), MODULES("Modules"), MONITOR("Monitor"), SETTINGS("API & Settings"), VOICE("Voice")
}

private data class ModuleInfo(val name: String, val detail: String, val icon: androidx.compose.ui.graphics.vector.ImageVector, val defaultOn: Boolean = true)
private val moduleCatalog = listOf(
    ModuleInfo("General AI Chat", "Ask questions about any topic", Icons.Default.AutoAwesome),
    ModuleInfo("Voice Assistant", "Speech input and spoken responses", Icons.Default.RecordVoiceOver),
    ModuleInfo("Anime Companion", "Animated companion panel", Icons.Default.Face),
    ModuleInfo("Game Library", "Add and launch installed apps", Icons.Default.SportsEsports),
    ModuleInfo("Device Monitor", "Battery, memory and thermal status", Icons.Default.MonitorHeart),
    ModuleInfo("Gaming Profiles", "Choose balanced, performance or battery guidance", Icons.Default.Speed),
    ModuleInfo("Performance HUD", "Show available device indicators", Icons.Default.Analytics),
    ModuleInfo("Refresh-rate Guide", "Explain supported display options", Icons.Default.DisplaySettings),
    ModuleInfo("Network Helper", "Basic connection guidance; no ping injection", Icons.Default.Wifi),
    ModuleInfo("Focus / DND Guide", "Shortcuts and guidance for focus mode", Icons.Default.DoNotDisturbOn),
    ModuleInfo("Screenshot / Recording Guide", "Use Android's built-in capture controls", Icons.Default.ScreenShare),
    ModuleInfo("Smart Battery Saver", "Battery-friendly gaming recommendations", Icons.Default.BatteryChargingFull),
    ModuleInfo("CPU Frequency Inspector", "Read-only CPU frequency where the device exposes it", Icons.Default.Memory),
    ModuleInfo("GPU Telemetry", "Best-effort GPU frequency reading; OEM access varies", Icons.Default.DeveloperBoard),
    ModuleInfo("Display Refresh Monitor", "Read the active Android display refresh rate", Icons.Default.DisplaySettings),
    ModuleInfo("TCP Latency Probe", "Measure TCP connection time to a host and port", Icons.Default.NetworkCheck),
    ModuleInfo("Keep Screen Awake", "Prevent screen timeout while Monitor is open", Icons.Default.ScreenLockPortrait, defaultOn = false)
)

@Composable
private fun MaxAiApp(onSpeak: (String) -> Unit, onOpenOverlay: () -> Unit) {
    val context = LocalContext.current
    val normalPrefs = remember { context.getSharedPreferences("max_ai_preferences", Context.MODE_PRIVATE) }
    val securePrefsResult = remember { runCatching { securePreferences(context) } }
    val secureStorageAvailable = securePrefsResult.isSuccess
    val securePrefs = securePrefsResult.getOrElse { context.getSharedPreferences("max_ai_secure_unavailable", Context.MODE_PRIVATE) }
    var page by remember { mutableStateOf(Page.HOME) }
    var modules by remember {
        mutableStateOf(moduleCatalog.associate { it.name to normalPrefs.getBoolean("module_${it.name}", it.defaultOn) })
    }
    var profile by remember { mutableStateOf(normalPrefs.getString("gaming_profile", "Smart Balanced") ?: "Smart Balanced") }
    val scope = rememberCoroutineScope()

    MaterialTheme(colorScheme = darkColorScheme(
        primary = Cyan, secondary = Purple, tertiary = Pink, background = Void, surface = SurfaceDeep,
        onPrimary = Void, onBackground = Color.White, onSurface = Color.White
    )) {
        Scaffold(
            containerColor = Void,
            topBar = {
                Row(Modifier.fillMaxWidth().background(SurfaceDeep).padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(36.dp).background(Brush.linearGradient(listOf(Cyan, Purple)), RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Bolt, null, tint = Void)
                    }
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text("MAX AI", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = Cyan)
                        Text("NEURAL GAMING COMPANION", style = MaterialTheme.typography.labelSmall, color = Purple)
                    }
                    IconButton(onClick = { page = Page.SETTINGS }) { Icon(Icons.Default.Settings, "API settings", tint = TextMuted) }
                }
            },
            bottomBar = {
                NavigationBar(containerColor = SurfaceDeep, contentColor = TextMuted) {
                    listOf(Page.HOME, Page.CHAT, Page.GAMES, Page.MODULES, Page.MONITOR).forEach { destination ->
                        NavigationBarItem(
                            selected = page == destination, onClick = { page = destination },
                            icon = { Icon(pageIcon(destination), contentDescription = destination.label) },
                            label = { Text(destination.label, maxLines = 1) },
                            colors = NavigationBarItemDefaults.colors(selectedIconColor = Cyan, selectedTextColor = Cyan, indicatorColor = Purple.copy(alpha = .18f), unselectedIconColor = TextMuted)
                        )
                    }
                }
            }
        ) { padding ->
            Box(Modifier.fillMaxSize().padding(padding)) {
                when (page) {
                    Page.HOME -> HomePage(
                        enabledCount = modules.values.count { it },
                        apiReady = !securePrefs.getString("api_key", "").isNullOrBlank(),
                        onNavigate = { page = it },
                        modules = modules,
                        onOpenOverlay = onOpenOverlay
                    )
                    Page.CHAT -> if (modules["General AI Chat"] == true) ChatPage(securePrefs, onSpeak) else DisabledPage("General AI Chat", { page = Page.MODULES })
                    Page.GAMES -> if (modules["Game Library"] == true) GamesPage(normalPrefs) else DisabledPage("Game Library", { page = Page.MODULES })
                    Page.MODULES -> ModulesPage(modules) { name, value ->
                        modules = modules + (name to value)
                        normalPrefs.edit().putBoolean("module_$name", value).apply()
                    }
                    Page.MONITOR -> if (modules["Device Monitor"] == true) MonitorPage(profile, onProfile = {
                        profile = it; normalPrefs.edit().putString("gaming_profile", it).apply()
                    }, modules = modules) else DisabledPage("Device Monitor", { page = Page.MODULES })
                    Page.SETTINGS -> SettingsPage(securePrefs, secureStorageAvailable)
                    Page.VOICE -> if (modules["Voice Assistant"] == true) VoicePage(securePrefs, onSpeak) else DisabledPage("Voice Assistant", { page = Page.MODULES })
                }
            }
        }
    }
}

private fun pageIcon(page: Page) = when (page) {
    Page.HOME -> Icons.Default.Dashboard
    Page.CHAT -> Icons.Default.AutoAwesome
    Page.GAMES -> Icons.Default.SportsEsports
    Page.MODULES -> Icons.Default.Tune
    Page.MONITOR -> Icons.Default.MonitorHeart
    Page.SETTINGS -> Icons.Default.Settings
    Page.VOICE -> Icons.Default.Mic
}

@Composable
private fun HomePage(enabledCount: Int, apiReady: Boolean, onNavigate: (Page) -> Unit, modules: Map<String, Boolean>, onOpenOverlay: () -> Unit) {
    val pulse = rememberInfiniteTransition(label = "hero-pulse")
    val scale by pulse.animateFloat(.96f, 1.04f, infiniteRepeatable(tween(1800, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "scale")
    LazyColumn(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF121E3C), Void))), contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("SYSTEM ONLINE", color = Cyan, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    Text("Your world.\nYour intelligence.", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
                    Text("A personal AI beyond gaming.", color = TextMuted)
                }
                if (modules["Anime Companion"] == true) CompanionOrb(Modifier.size(112.dp).scale(scale))
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatPill("MODULES", "$enabledCount/${moduleCatalog.size}", Icons.Default.Extension, Modifier.weight(1f))
                StatPill("AI API", if (apiReady) "CONNECTED*" else "NOT SET", Icons.Default.Key, Modifier.weight(1f))
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = SurfaceDeep), shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth().border(1.dp, Purple.copy(alpha = .5f), RoundedCornerShape(24.dp))) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AutoAwesome, null, tint = Cyan)
                        Spacer(Modifier.width(8.dp)); Text("MAX AI CORE", color = Cyan, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.weight(1f)); Text("BETA", color = Pink, style = MaterialTheme.typography.labelSmall)
                    }
                    Text("Ask anything — study, coding, writing, everyday questions, or gaming.", color = TextMuted)
                    Button(onClick = { onNavigate(Page.CHAT) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
                        Icon(Icons.Default.Chat, null); Spacer(Modifier.width(8.dp)); Text("Open AI Assistant")
                    }
                }
            }
        }
        item { Text("COMMAND CENTER", color = Purple, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold) }
        item { ActionCard("In-game Floating Panel", "Slim red trigger bar • portrait and landscape", Icons.Default.Games) { onOpenOverlay() } }
        item { ActionCard("Game Library", "Add any launchable installed app", Icons.Default.SportsEsports) { onNavigate(Page.GAMES) } }
        item { ActionCard("Module Hub", "Enable or disable each feature", Icons.Default.Tune) { onNavigate(Page.MODULES) } }
        item { ActionCard("Live Device Monitor", "Memory, battery and thermal state", Icons.Default.MonitorHeart) { onNavigate(Page.MONITOR) } }
        item { ActionCard("Voice & Language", "Talk to MAX AI using Android speech", Icons.Default.RecordVoiceOver) { onNavigate(Page.VOICE) } }
        item { ActionCard("API & Privacy Settings", "Store your API key on this device", Icons.Default.Key) { onNavigate(Page.SETTINGS) } }
        item { Text("*API status means a key is saved locally; it does not guarantee the provider is reachable.", color = TextMuted, style = MaterialTheme.typography.bodySmall) }
    }
}

@Composable
private fun CompanionOrb(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "orb")
    val alpha by transition.animateFloat(.65f, 1f, infiniteRepeatable(tween(1400), RepeatMode.Reverse), label = "orb-alpha")
    Box(modifier.background(Brush.radialGradient(listOf(Cyan.copy(alpha = .8f), Purple.copy(alpha = .65f), Pink.copy(alpha = .15f))), CircleShape).border(2.dp, Cyan.copy(alpha = alpha), CircleShape), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.Face, null, tint = Color.White, modifier = Modifier.size(44.dp))
            Text("MAX", color = Color.White, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun StatPill(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier = Modifier) {
    Card(modifier, colors = CardDefaults.cardColors(containerColor = SurfaceRaised), shape = RoundedCornerShape(16.dp)) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = Cyan, modifier = Modifier.size(24.dp)); Spacer(Modifier.width(8.dp))
            Column { Text(label, style = MaterialTheme.typography.labelSmall, color = TextMuted); Text(value, fontWeight = FontWeight.Bold) }
        }
    }
}

@Composable
private fun ActionCard(title: String, detail: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Card(onClick = onClick, colors = CardDefaults.cardColors(containerColor = SurfaceDeep), shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(44.dp).background(Brush.linearGradient(listOf(Purple.copy(alpha = .4f), Cyan.copy(alpha = .15f))), RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) { Icon(icon, null, tint = Cyan) }
            Spacer(Modifier.width(12.dp)); Column(Modifier.weight(1f)) { Text(title, fontWeight = FontWeight.Bold); Text(detail, color = TextMuted, style = MaterialTheme.typography.bodySmall) }
            Icon(Icons.Default.ChevronRight, null, tint = Purple)
        }
    }
}

@Composable
private fun ChatPage(prefs: android.content.SharedPreferences, onSpeak: (String) -> Unit) {
    var message by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var transcript by remember { mutableStateOf(listOf(ChatLine(false, "Hi! I’m MAX AI. Add your own API key in Settings to get general-purpose AI answers."))) }
    val scope = rememberCoroutineScope()
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("AI CONVERSATION", color = Purple, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        Text("Ask anything", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
        LazyColumn(Modifier.weight(1f).fillMaxWidth(), contentPadding = PaddingValues(vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(transcript) { line ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = if (line.user) Arrangement.End else Arrangement.Start) {
                    Card(colors = CardDefaults.cardColors(containerColor = if (line.user) Color(0xFF263A59) else SurfaceDeep), shape = RoundedCornerShape(18.dp), modifier = Modifier.widthIn(max = 320.dp)) {
                        Column(Modifier.padding(14.dp)) {
                            Text(if (line.user) "YOU" else "MAX AI", color = if (line.user) Cyan else Purple, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            Text(line.text)
                        }
                    }
                }
            }
            if (busy) item { Row(verticalAlignment = Alignment.CenterVertically) { CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp); Spacer(Modifier.width(10.dp)); Text("Thinking…", color = Cyan) } }
            error?.let { msg -> item { Text(msg, color = Pink, style = MaterialTheme.typography.bodySmall) } }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(value = message, onValueChange = { message = it }, modifier = Modifier.weight(1f), placeholder = { Text("Ask anything…") }, shape = RoundedCornerShape(18.dp), maxLines = 4)
            Spacer(Modifier.width(8.dp))
            IconButton(enabled = !busy && message.isNotBlank(), onClick = {
                val question = message.trim(); message = ""; error = null
                val history = transcript + ChatLine(true, question)
                transcript = history; busy = true
                scope.launch {
                    try {
                        val answer = AiApi.chat(prefs, history.filter { it.text.isNotBlank() })
                        transcript = transcript + ChatLine(false, answer)
                        onSpeak(answer)
                    } catch (e: Exception) {
                        error = e.message ?: "AI request failed. Check the API key, model, endpoint, and internet connection."
                        transcript = transcript + ChatLine(false, "I couldn't reach the AI provider. Open API & Settings and verify your configuration.")
                    } finally { busy = false }
                }
            }) { Icon(Icons.Default.Send, "Send", tint = Cyan) }
        }
    }
}

private data class ChatLine(val user: Boolean, val text: String)

private object AiApi {
    suspend fun chat(prefs: android.content.SharedPreferences, messages: List<ChatLine>): String = withContext(Dispatchers.IO) {
        val key = prefs.getString("api_key", "")?.trim().orEmpty()
        val base = prefs.getString("api_base", "https://openrouter.ai/api/v1")?.trim().orEmpty().trimEnd('/')
        val model = prefs.getString("api_model", "openrouter/auto")?.trim().orEmpty()
        require(key.isNotBlank()) { "No API key saved. Open API & Settings first." }
        require(base.startsWith("https://") || base.startsWith("http://10.") || base.startsWith("http://192.168.")) { "Endpoint must use HTTPS (private LAN endpoints are also allowed)." }
        require(model.isNotBlank()) { "Enter a model name in API settings." }
        val payloadMessages = JSONArray().apply {
            put(JSONObject().put("role", "system").put("content", "You are MAX AI, a helpful general-purpose assistant. Be accurate, clear, and safe."))
            messages.takeLast(24).forEach { put(JSONObject().put("role", if (it.user) "user" else "assistant").put("content", it.text)) }
        }
        val payload = JSONObject().put("model", model).put("messages", payloadMessages).put("temperature", 0.7)
        val connection = (URL("$base/chat/completions").openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"; connectTimeout = 20000; readTimeout = 60000; doOutput = true
            setRequestProperty("Authorization", "Bearer $key")
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
        }
        try {
            connection.outputStream.use { it.write(payload.toString().toByteArray(Charsets.UTF_8)) }
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val body = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (status !in 200..299) {
                val providerMessage = runCatching { JSONObject(body).optJSONObject("error")?.optString("message") }.getOrNull().orEmpty()
                throw IllegalStateException("AI provider returned HTTP $status${if (providerMessage.isNotBlank()) ": $providerMessage" else ". Check key/model/endpoint."}")
            }
            JSONObject(body).optJSONArray("choices")?.optJSONObject(0)?.optJSONObject("message")?.optString("content")?.takeIf { it.isNotBlank() }
                ?: throw IllegalStateException("Provider response did not contain choices[0].message.content. Use an OpenAI-compatible chat-completions endpoint.")
        } finally { connection.disconnect() }
    }
}

@Composable
private fun SettingsPage(prefs: android.content.SharedPreferences, secureStorageAvailable: Boolean) {
    var key by remember { mutableStateOf(prefs.getString("api_key", "") ?: "") }
    var base by remember { mutableStateOf(prefs.getString("api_base", "https://openrouter.ai/api/v1") ?: "https://openrouter.ai/api/v1") }
    var model by remember { mutableStateOf(prefs.getString("api_model", "openrouter/auto") ?: "openrouter/auto") }
    var showKey by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("") }
    LazyColumn(Modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Text("PERSONAL AI CONFIGURATION", color = Purple, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            Text("Your key. Your choice.", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
            Text("MAX AI stores your key in Android Keystore-backed encrypted preferences. The key is sent to your configured HTTPS endpoint only when you send a chat message.", color = TextMuted)
            if (!secureStorageAvailable) Text("Secure storage could not initialize on this device. Saving is disabled; no plaintext fallback will be used.", color = Pink)
        }
        item { OutlinedTextField(value = base, onValueChange = { base = it }, modifier = Modifier.fillMaxWidth(), label = { Text("API base URL") }, placeholder = { Text("https://openrouter.ai/api/v1") }, singleLine = true) }
        item { OutlinedTextField(value = model, onValueChange = { model = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Model ID") }, placeholder = { Text("openrouter/auto") }, singleLine = true) }
        item {
            OutlinedTextField(value = key, onValueChange = { key = it }, modifier = Modifier.fillMaxWidth(), label = { Text("API key") }, placeholder = { Text("Paste your own API key") }, singleLine = true, visualTransformation = if (showKey) androidx.compose.ui.text.input.VisualTransformation.None else PasswordVisualTransformation(), trailingIcon = { IconButton(onClick = { showKey = !showKey }) { Icon(if (showKey) Icons.Default.VisibilityOff else Icons.Default.Visibility, "Toggle API key visibility") } })
        }
        item { Button(enabled = secureStorageAvailable, onClick = {
            if (!base.trim().startsWith("https://")) {
                status = "Use an HTTPS endpoint to protect your API key in transit."
            } else if (key.isBlank() || model.isBlank()) {
                status = "Enter an API key and model ID before saving."
            } else {
                prefs.edit().putString("api_key", key.trim()).putString("api_base", base.trim()).putString("api_model", model.trim()).apply()
                status = "Saved using encrypted local storage. Test it by opening AI Chat."
            }
        }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) { Icon(Icons.Default.Lock, null); Spacer(Modifier.width(8.dp)); Text("Save securely on device") } }
        item { OutlinedButton(onClick = {
            key = ""; prefs.edit().remove("api_key").apply(); status = "API key removed from this device."
        }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) { Icon(Icons.Default.DeleteOutline, null); Spacer(Modifier.width(8.dp)); Text("Delete saved API key") } }
        if (status.isNotBlank()) item { Text(status, color = Cyan) }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = SurfaceDeep), shape = RoundedCornerShape(18.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Compatible providers", color = Cyan, fontWeight = FontWeight.Bold)
                    Text("Use an OpenAI-compatible /chat/completions endpoint. Enter the provider's base URL, model ID, and your own key. Provider usage fees or free-tier limits still apply.", color = TextMuted)
                    Text("Never share your key or commit it to GitHub. Local encryption protects storage on this device, but does not make a compromised device risk-free.", color = TextMuted, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun ModulesPage(modules: Map<String, Boolean>, onToggle: (String, Boolean) -> Unit) {
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { Text("MODULE CONTROL CENTER", color = Purple, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold); Text("Modules", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black); Text("These switches persist across restarts and disable their associated app screens.", color = TextMuted) }
        items(moduleCatalog) { module ->
            val enabled = modules[module.name] == true
            Card(colors = CardDefaults.cardColors(containerColor = if (enabled) SurfaceDeep else Color(0xFF10131E)), shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(42.dp).background(if (enabled) Cyan.copy(alpha = .12f) else Color.Gray.copy(alpha = .12f), RoundedCornerShape(13.dp)), contentAlignment = Alignment.Center) { Icon(module.icon, null, tint = if (enabled) Cyan else TextMuted) }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) { Text(module.name, fontWeight = FontWeight.Bold); Text(module.detail, color = TextMuted, style = MaterialTheme.typography.bodySmall); Text(if (enabled) "ACTIVE" else "OFFLINE", color = if (enabled) Cyan else TextMuted, style = MaterialTheme.typography.labelSmall) }
                    Switch(checked = enabled, onCheckedChange = { onToggle(module.name, it) })
                }
            }
        }
        item { Text("Note: Android restricts direct CPU/GPU clock control, forced process killing, and universal FPS/temperature readings. Unsupported actions are presented as guidance rather than falsely reported as active controls.", color = TextMuted, style = MaterialTheme.typography.bodySmall) }
    }
}

private data class LaunchableApp(val label: String, val packageName: String, val icon: Drawable)

@Suppress("DEPRECATION")
private fun installedLaunchableApps(context: Context): List<LaunchableApp> {
    val pm = context.packageManager
    val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
    val resolved = if (Build.VERSION.SDK_INT >= 33) pm.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(0)) else pm.queryIntentActivities(intent, 0)
    return resolved.mapNotNull { info ->
        val pkg = info.activityInfo?.packageName ?: return@mapNotNull null
        if (pkg == context.packageName) return@mapNotNull null
        LaunchableApp(info.loadLabel(pm).toString(), pkg, info.loadIcon(pm))
    }.distinctBy { it.packageName }.sortedBy { it.label.lowercase(Locale.getDefault()) }
}

@Composable
private fun GamesPage(prefs: android.content.SharedPreferences) {
    val context = LocalContext.current
    var allApps by remember { mutableStateOf(emptyList<LaunchableApp>()) }
    var saved by remember { mutableStateOf(prefs.getStringSet("game_packages", emptySet())?.toSet() ?: emptySet()) }
    var showPicker by remember { mutableStateOf(false) }
    var search by remember { mutableStateOf("") }
    var notice by remember { mutableStateOf("") }
    fun persist(packages: Set<String>) { saved = packages.toSet(); prefs.edit().putStringSet("game_packages", saved).apply() }
    LaunchedEffect(Unit) { allApps = installedLaunchableApps(context) }
    val chosen = allApps.filter { it.packageName in saved }
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) { Text("GAME LIBRARY", color = Purple, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold); Text("Your games. Your rules.", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black) }
                Button(onClick = { showPicker = true }, shape = RoundedCornerShape(14.dp)) { Icon(Icons.Default.Add, null); Text(" Add") }
            }
            Text("Add any app installed on your device that has a launcher icon, including games and emulators.", color = TextMuted)
            if (notice.isNotBlank()) Text(notice, color = Cyan)
        }
        if (chosen.isEmpty()) item { EmptyState(Icons.Default.SportsEsports, "No apps added yet", "Tap Add to choose installed games or other launchable apps.") }
        items(chosen) { app ->
            AppRow(app, actionText = "PLAY", actionIcon = Icons.Default.PlayArrow, onAction = {
                val launch = context.packageManager.getLaunchIntentForPackage(app.packageName)
                if (launch != null) context.startActivity(launch) else notice = "This app does not expose a launch activity."
            }, onRemove = { persist(saved - app.packageName); notice = "Removed ${app.label} from your library." })
        }
    }
    if (showPicker) {
        var query by remember { mutableStateOf("") }
        val filtered = allApps.filter { it.label.contains(query, true) || it.packageName.contains(query, true) }
        AlertDialog(onDismissRequest = { showPicker = false }, title = { Text("Add installed app") }, text = {
            Column(Modifier.heightIn(max = 480.dp)) {
                OutlinedTextField(value = query, onValueChange = { query = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Search apps") }, singleLine = true)
                Spacer(Modifier.height(8.dp))
                LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    items(filtered) { app ->
                        Row(Modifier.fillMaxWidth().clickable {
                            persist(saved + app.packageName); notice = "Added ${app.label}."; showPicker = false
                        }.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            AppIcon(app.icon, Modifier.size(38.dp)); Spacer(Modifier.width(10.dp)); Column(Modifier.weight(1f)) { Text(app.label, fontWeight = FontWeight.SemiBold); Text(app.packageName, color = TextMuted, style = MaterialTheme.typography.labelSmall) }; Icon(Icons.Default.AddCircleOutline, null, tint = Cyan)
                        }
                    }
                }
            }
        }, confirmButton = { TextButton(onClick = { showPicker = false }) { Text("Done") } })
    }
}

@Composable
private fun AppRow(app: LaunchableApp, actionText: String, actionIcon: androidx.compose.ui.graphics.vector.ImageVector, onAction: () -> Unit, onRemove: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = SurfaceDeep), shape = RoundedCornerShape(16.dp)) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            AppIcon(app.icon, Modifier.size(46.dp)); Spacer(Modifier.width(12.dp)); Column(Modifier.weight(1f)) { Text(app.label, fontWeight = FontWeight.Bold); Text(app.packageName, color = TextMuted, style = MaterialTheme.typography.labelSmall, maxLines = 1) }
            IconButton(onClick = onAction) { Icon(actionIcon, actionText, tint = Cyan) }; IconButton(onClick = onRemove) { Icon(Icons.Default.RemoveCircleOutline, "Remove", tint = Pink) }
        }
    }
}

@Composable
private fun AppIcon(drawable: Drawable, modifier: Modifier = Modifier) {
    val bitmap = remember(drawable) { drawableToBitmap(drawable).asImageBitmap() }
    Image(bitmap, contentDescription = "App icon", modifier = modifier)
}

private fun drawableToBitmap(drawable: Drawable): Bitmap {
    val width = drawable.intrinsicWidth.coerceIn(1, 256)
    val height = drawable.intrinsicHeight.coerceIn(1, 256)
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    drawable.setBounds(0, 0, canvas.width, canvas.height); drawable.draw(canvas)
    return bitmap
}

@Composable
private fun MonitorPage(profile: String, onProfile: (String) -> Unit, modules: Map<String, Boolean>) {
    val context = LocalContext.current
    var refresh by remember { mutableIntStateOf(0) }
    var keepAwake by remember { mutableStateOf(false) }
    var host by remember { mutableStateOf("1.1.1.1") }
    var port by remember { mutableStateOf("443") }
    var probeResult by remember { mutableStateOf("Enter a host and port, then test TCP connection latency.") }
    var probing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    DisposableEffect(keepAwake, context) {
        val activity = context as? Activity
        if (keepAwake && modules["Keep Screen Awake"] == true) {
            activity?.window?.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            activity?.window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose { activity?.window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
    }

    LaunchedEffect(Unit) { while (true) { kotlinx.coroutines.delay(3000); refresh++ } }
    val memory = remember(refresh) {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
        val mi = android.app.ActivityManager.MemoryInfo(); activityManager.getMemoryInfo(mi)
        val total = mi.totalMem / (1024 * 1024); val avail = mi.availMem / (1024 * 1024)
        Triple(total, avail, ((total - avail).toFloat() / total.coerceAtLeast(1) * 100).toInt())
    }
    val battery = remember(refresh) {
        val bm = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY).takeIf { it in 0..100 } ?: -1
    }
    val thermal = remember(refresh) {
        if (Build.VERSION.SDK_INT >= 29) {
            val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            when (pm.currentThermalStatus) {
                PowerManager.THERMAL_STATUS_NONE -> "Normal"
                PowerManager.THERMAL_STATUS_LIGHT -> "Light"
                PowerManager.THERMAL_STATUS_MODERATE -> "Moderate"
                PowerManager.THERMAL_STATUS_SEVERE -> "Severe"
                PowerManager.THERMAL_STATUS_CRITICAL -> "Critical"
                PowerManager.THERMAL_STATUS_EMERGENCY -> "Emergency"
                PowerManager.THERMAL_STATUS_SHUTDOWN -> "Shutdown"
                else -> "Unknown"
            }
        } else "Unavailable on Android below 10"
    }
    val networkType = remember(refresh, modules["Network Helper"]) {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val caps = cm.getNetworkCapabilities(cm.activeNetwork)
        when {
            caps == null -> "Offline / no active network"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Connected via Wi-Fi"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Connected via mobile data"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Connected via Ethernet"
            else -> "Connected (network type unknown)"
        }
    }
    val cpu = remember(refresh, modules["CPU Frequency Inspector"]) {
        if (modules["CPU Frequency Inspector"] == true) DeviceCapabilityEngine.cpuFrequency() else null
    }
    val gpuMHz = remember(refresh, modules["GPU Telemetry"]) {
        if (modules["GPU Telemetry"] == true) DeviceCapabilityEngine.gpuFrequencyMHz() else null
    }
    val refreshRate = remember(refresh, modules["Display Refresh Monitor"]) {
        if (modules["Display Refresh Monitor"] == true) DeviceCapabilityEngine.displayRefreshRate(context) else null
    }

    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text("LIVE DEVICE TELEMETRY", color = Purple, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            Text("System monitor", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
            Text("Refreshes about every 3 seconds while this screen is open. Restricted metrics show as unavailable instead of guessed values.", color = TextMuted)
        }
        if (modules["Performance HUD"] == true) {
            item { Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricCard("MEMORY USED", "${memory.third}%", "${memory.first - memory.second} / ${memory.first} MB", Icons.Default.Memory, Modifier.weight(1f))
                MetricCard("BATTERY", if (battery >= 0) "$battery%" else "N/A", "Current charge", Icons.Default.BatteryFull, Modifier.weight(1f))
            } }
            item { MetricCard("THERMAL STATUS", thermal, if (Build.VERSION.SDK_INT >= 29) "Android thermal service" else "API limitation", Icons.Default.DeviceThermostat, Modifier.fillMaxWidth()) }
        } else item { TipCard("Performance HUD is off", "Turn it on in Module Hub to display memory, battery, and thermal indicators.") }

        if (modules["CPU Frequency Inspector"] == true) {
            item { Text("CPU FREQUENCY (READ ONLY)", color = Purple, fontWeight = FontWeight.Bold) }
            item { Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricCard("CURRENT AVG", DeviceCapabilityEngine.formatMHz(cpu?.currentMHz), "Readable cores: ${cpu?.readableCores ?: 0}", Icons.Default.Memory, Modifier.weight(1f))
                MetricCard("SUPPORTED RANGE", "${DeviceCapabilityEngine.formatMHz(cpu?.minMHz)} – ${DeviceCapabilityEngine.formatMHz(cpu?.maxMHz)}", "OEM sysfs access may be blocked", Icons.Default.Speed, Modifier.weight(1f))
            } }
        }
        if (modules["GPU Telemetry"] == true) item {
            MetricCard("GPU FREQUENCY", DeviceCapabilityEngine.formatMHz(gpuMHz), if (gpuMHz == null) "This firmware does not expose a readable GPU frequency node to this app." else "Best-effort read-only OEM telemetry", Icons.Default.DeveloperBoard, Modifier.fillMaxWidth())
        }
        if (modules["Display Refresh Monitor"] == true) item {
            MetricCard("DISPLAY REFRESH RATE", refreshRate?.let { "%.1f Hz".format(Locale.US, it) } ?: "Unavailable", "Active Android display mode; this is not the game's measured FPS.", Icons.Default.DisplaySettings, Modifier.fillMaxWidth())
        }
        if (modules["Gaming Profiles"] == true) {
            item { Text("GAMING PROFILE", color = Purple, fontWeight = FontWeight.Bold) }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Extreme Performance", "Smart Balanced", "Ultra Battery Saver").forEach { option ->
                        Row(Modifier.fillMaxWidth().background(SurfaceDeep, RoundedCornerShape(14.dp)).clickable { onProfile(option) }.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(when (option) { "Extreme Performance" -> Icons.Default.Bolt; "Ultra Battery Saver" -> Icons.Default.BatterySaver; else -> Icons.Default.Balance }, null, tint = if (profile == option) Cyan else TextMuted)
                            Spacer(Modifier.width(10.dp)); Text(option, Modifier.weight(1f), fontWeight = FontWeight.SemiBold); RadioButton(selected = profile == option, onClick = { onProfile(option) })
                        }
                    }
                    Text(profileDescription(profile), color = TextMuted, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        if (modules["TCP Latency Probe"] == true) {
            item { Text("TCP LATENCY PROBE", color = Purple, fontWeight = FontWeight.Bold) }
            item {
                Card(colors = CardDefaults.cardColors(containerColor = SurfaceDeep), shape = RoundedCornerShape(18.dp)) {
                    Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Measures the time to establish a TCP connection to the selected host/port. This is not ICMP ping and may differ from the game's actual server latency.", color = TextMuted, style = MaterialTheme.typography.bodySmall)
                        OutlinedTextField(value = host, onValueChange = { host = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Host or IP") }, singleLine = true)
                        OutlinedTextField(value = port, onValueChange = { port = it.filter(Char::isDigit).take(5) }, modifier = Modifier.fillMaxWidth(), label = { Text("TCP port") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                        Button(enabled = !probing, onClick = {
                            probing = true; probeResult = "Connecting…"
                            scope.launch {
                                try {
                                    val ms = withContext(Dispatchers.IO) { DeviceCapabilityEngine.tcpConnectLatencyMs(host, port.toIntOrNull() ?: 0) }
                                    probeResult = "TCP connect: ${ms} ms to ${host.trim()}:${port.trim()}"
                                } catch (e: Exception) { probeResult = "Probe failed: ${e.message ?: "host unreachable or connection blocked"}" }
                                finally { probing = false }
                            }
                        }, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.NetworkCheck, null); Spacer(Modifier.width(8.dp)); Text(if (probing) "Testing…" else "Measure TCP latency") }
                        Text(probeResult, color = if (probeResult.startsWith("Probe failed")) Pink else Cyan)
                    }
                }
            }
        }
        if (modules["Keep Screen Awake"] == true) item {
            Card(colors = CardDefaults.cardColors(containerColor = SurfaceDeep), shape = RoundedCornerShape(18.dp)) {
                Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) { Text("Keep screen awake", color = Cyan, fontWeight = FontWeight.Bold); Text("Only while the Monitor screen is open", color = TextMuted, style = MaterialTheme.typography.bodySmall) }
                    Switch(checked = keepAwake, onCheckedChange = { keepAwake = it })
                }
            }
        }
        if (modules["Refresh-rate Guide"] == true) item { TipCard("Refresh-rate guide", "Open Android Settings → Display → Refresh rate. MAX AI reads the active rate but cannot force unsupported display modes.") }
        if (modules["Network Helper"] == true) item { TipCard("Network helper", "$networkType. Use TCP Latency Probe for a host/port connection timing measurement; it is not a guaranteed game-server ping.") }
        if (modules["Focus / DND Guide"] == true) item {
            Card(colors = CardDefaults.cardColors(containerColor = SurfaceDeep), shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.padding(15.dp)) {
                    Text("Focus / Do Not Disturb", color = Cyan, fontWeight = FontWeight.Bold)
                    Text("Manage interruptions using Android's own DND settings. Access remains under your control.", color = TextMuted)
                    TextButton(onClick = { runCatching { context.startActivity(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } }) { Text("Open DND settings") }
                }
            }
        }
        if (modules["Screenshot / Recording Guide"] == true) item { TipCard("Screenshot / recording", "Use your phone's built-in screenshot shortcut or Quick Settings screen recorder. Android will request capture consent when required.") }
        if (modules["Smart Battery Saver"] == true) item { TipCard("Smart battery saver", "Lower screen brightness, choose a moderate FPS cap inside the game, and avoid playing while the device is excessively hot.") }
        item { Text("Limits: ordinary apps cannot disable thermal protection or universally force CPU/GPU clocks. Display refresh rate is not game FPS. CPU/GPU frequency nodes are read-only and may be blocked by OEM firmware.", color = TextMuted, style = MaterialTheme.typography.bodySmall) }
    }
}

internal fun profileDescription(profile: String) = when (profile) {
    "Extreme Performance" -> "Guidance: use your phone's native performance/game mode and in-game FPS controls. May increase heat and battery use."
    "Ultra Battery Saver" -> "Guidance: lower graphics and brightness, cap FPS in-game, and close unneeded apps."
    else -> "Guidance: balance graphics and FPS, monitor heat, and use the manufacturer's game tools where available."
}

@Composable
private fun TipCard(title: String, detail: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SurfaceDeep),
        shape = RoundedCornerShape(18.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(15.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(Icons.Default.Info, contentDescription = null, tint = Cyan)
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(title, color = Cyan, fontWeight = FontWeight.Bold)
                Text(detail, color = TextMuted, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun MetricCard(title: String, value: String, detail: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier = Modifier) {
    Card(modifier, colors = CardDefaults.cardColors(containerColor = SurfaceDeep), shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) { Icon(icon, null, tint = Cyan); Text(title, color = TextMuted, style = MaterialTheme.typography.labelSmall); Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black); Text(detail, color = TextMuted, style = MaterialTheme.typography.bodySmall) }
    }
}

@Composable
private fun VoicePage(prefs: android.content.SharedPreferences, onSpeak: (String) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var recognizedText by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var resultText by remember { mutableStateOf("Tap the microphone and speak. The Android speech service determines language availability.") }
    var permission by remember { mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) }
    val permissionRequest = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { permission = it }
    val speech = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val spoken = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
        if (!spoken.isNullOrBlank()) {
            recognizedText = spoken
            resultText = "You said: $spoken\n\nTap Ask MAX AI to get an answer."
        } else if (result.resultCode != Activity.RESULT_OK) resultText = "No speech was captured. Please try again."
    }
    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        CompanionOrb(Modifier.size(100.dp).align(Alignment.CenterHorizontally))
        Text("VOICE INTERFACE", color = Purple, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        Text("Talk naturally", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
        Card(colors = CardDefaults.cardColors(containerColor = SurfaceDeep), shape = RoundedCornerShape(20.dp)) { Text(resultText, Modifier.padding(18.dp)) }
        Button(onClick = {
            if (!permission) permissionRequest.launch(Manifest.permission.RECORD_AUDIO)
            else if (SpeechRecognizer.isRecognitionAvailable(context)) {
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM).putExtra(RecognizerIntent.EXTRA_PROMPT, "Talk to MAX AI")
                speech.launch(intent); resultText = "Listening…"
            } else resultText = "No compatible speech recognition service is available on this device."
        }, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.Mic, null); Spacer(Modifier.width(8.dp)); Text("Start voice input") }
        Button(enabled = recognizedText.isNotBlank() && !busy, onClick = {
            val question = recognizedText
            busy = true
            scope.launch {
                try {
                    val answer = AiApi.chat(prefs, listOf(ChatLine(true, question)))
                    resultText = "You said: $question\n\nMAX AI: $answer"
                    onSpeak(answer)
                } catch (e: Exception) {
                    resultText = e.message ?: "AI request failed. Check API settings and internet connection."
                } finally { busy = false }
            }
        }, modifier = Modifier.fillMaxWidth()) {
            if (busy) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp) else Icon(Icons.Default.AutoAwesome, null)
            Spacer(Modifier.width(8.dp)); Text(if (busy) "Thinking…" else "Ask MAX AI")
        }
        OutlinedButton(onClick = { onSpeak("Hello! I am MAX AI. How can I help you today?") }, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.VolumeUp, null); Spacer(Modifier.width(8.dp)); Text("Test assistant voice") }
        Text("Device voice quality and language availability depend on installed speech engines. Real AI answers require a saved API configuration.", color = TextMuted)
    }
}

@Composable
private fun DisabledPage(name: String, onModules: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Icon(Icons.Default.PowerOff, null, tint = Purple, modifier = Modifier.size(56.dp))
        Spacer(Modifier.height(16.dp)); Text("$name is disabled", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("Enable this module in Module Hub to use it.", color = TextMuted)
        Spacer(Modifier.height(16.dp)); Button(onClick = onModules) { Text("Open Module Hub") }
    }
}

@Composable
private fun EmptyState(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, detail: String) {
    Card(colors = CardDefaults.cardColors(containerColor = SurfaceDeep), shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(icon, null, tint = Cyan, modifier = Modifier.size(42.dp)); Text(title, fontWeight = FontWeight.Bold); Text(detail, color = TextMuted)
        }
    }
}

private fun securePreferences(context: Context): android.content.SharedPreferences {
    val masterKey = MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()
    val encrypted = EncryptedSharedPreferences.create(
        context,
        "max_ai_secure_preferences",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )
    // Migrate any key left by older builds' private fallback, then clear the plaintext fallback file.
    val legacy = context.getSharedPreferences("max_ai_secure_fallback", Context.MODE_PRIVATE)
    val oldKey = legacy.getString("api_key", null)
    val migrationSucceeded = if (!oldKey.isNullOrBlank() && encrypted.getString("api_key", null).isNullOrBlank()) {
        encrypted.edit()
            .putString("api_key", oldKey)
            .putString("api_base", legacy.getString("api_base", "https://openrouter.ai/api/v1"))
            .putString("api_model", legacy.getString("api_model", "openrouter/auto"))
            .commit()
    } else true
    if (!migrationSucceeded) throw IllegalStateException("Could not migrate the legacy API key into encrypted storage")
    legacy.edit().clear().commit()
    return encrypted
}
