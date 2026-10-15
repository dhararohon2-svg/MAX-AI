package com.maxai.gamingsuite

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import java.util.Locale

private val Ink = Color(0xFF090D1B)
private val Panel = Color(0xFF151C31)
private val Neon = Color(0xFF70F0E3)
private val Violet = Color(0xFF9A82FF)

class MainActivity : ComponentActivity(), TextToSpeech.OnInitListener {
    private var tts: TextToSpeech? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tts = TextToSpeech(this, this)
        setContent { MaxAiApp(onSpeak = { speak(it) }) }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) tts?.language = Locale.getDefault()
    }

    private fun speak(text: String) {
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "max_ai_reply")
    }

    override fun onDestroy() {
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }
}

private enum class Screen(val title: String) {
    HOME("Dashboard"), CHAT("AI Chat"), VOICE("Voice Assistant"),
    MODULES("Module Hub"), DEVICE("Device Tools"), GAMING("Gaming Suite")
}

@Composable
private fun MaxAiApp(onSpeak: (String) -> Unit) {
    var screen by remember { mutableStateOf(Screen.HOME) }
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("max_ai_modules", 0) }
    var modules by remember {
        mutableStateOf(
            listOf("AI Chat", "Voice Assistant", "Anime Avatar", "Device Tools", "Gaming Optimizer", "Game Library")
                .associateWith { prefs.getBoolean(it, it != "Gaming Optimizer") }
        )
    }

    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Neon, secondary = Violet, background = Ink,
            surface = Panel, onPrimary = Ink, onBackground = Color.White, onSurface = Color.White
        )
    ) {
        Scaffold(
            containerColor = Ink,
            bottomBar = {
                NavigationBar(containerColor = Panel) {
                    val entries = listOf(
                        Screen.HOME to Icons.Default.Home,
                        Screen.CHAT to Icons.Default.Chat,
                        Screen.VOICE to Icons.Default.Mic,
                        Screen.MODULES to Icons.Default.Tune
                    )
                    entries.forEach { (destination, icon) ->
                        NavigationBarItem(
                            selected = screen == destination,
                            onClick = { screen = destination },
                            icon = { Icon(icon, contentDescription = destination.title) },
                            label = { Text(destination.title) }
                        )
                    }
                }
            }
        ) { padding ->
            Box(Modifier.fillMaxSize().padding(padding)) {
                when (screen) {
                    Screen.HOME -> HomeScreen(
                        onOpen = { screen = it },
                        modulesEnabled = modules.values.count { it }
                    )
                    Screen.CHAT -> ChatScreen(onSpeak)
                    Screen.VOICE -> VoiceScreen(onSpeak)
                    Screen.MODULES -> ModuleScreen(modules) { name, enabled ->
                        modules = modules + (name to enabled)
                        prefs.edit().putBoolean(name, enabled).apply()
                    }
                    Screen.DEVICE -> DeviceScreen()
                    Screen.GAMING -> GamingScreen()
                }
            }
        }
    }
}

@Composable
private fun HomeScreen(onOpen: (Screen) -> Unit, modulesEnabled: Int) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(Color(0xFF111B38), Ink))
        ),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("MAX AI", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Black, color = Neon)
            Text("ULTIMATE GAMING & AI COMPANION", style = MaterialTheme.typography.labelMedium, color = Violet)
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(28.dp)) {
                Column(Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        Modifier.size(116.dp).background(
                            Brush.linearGradient(listOf(Violet, Neon)), CircleShape
                        ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("✦", style = MaterialTheme.typography.displayLarge, color = Ink)
                    }
                    Spacer(Modifier.height(12.dp))
                    Text("Your AI companion", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("Anime avatar placeholder • Personalize later", color = Color.LightGray)
                    Spacer(Modifier.height(8.dp))
                    Text("$modulesEnabled modules enabled", color = Neon)
                }
            }
        }
        item { Text("Quick access", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
        item { FeatureCard("AI Chat", "Ask questions and get a demo response", Icons.Default.Chat) { onOpen(Screen.CHAT) } }
        item { FeatureCard("Voice Assistant", "Speak and listen using Android voice services", Icons.Default.Mic) { onOpen(Screen.VOICE) } }
        item { FeatureCard("Module Hub", "Toggle app modules individually", Icons.Default.Tune) { onOpen(Screen.MODULES) } }
        item { FeatureCard("Device Tools", "View basic device information", Icons.Default.PhoneAndroid) { onOpen(Screen.DEVICE) } }
        item { FeatureCard("Gaming Suite", "Safe optimization guidance and game shortcuts", Icons.Default.SportsEsports) { onOpen(Screen.GAMING) } }
        item {
            Text(
                "Privacy-first: MAX AI only uses features you open and permissions you approve.",
                color = Color.LightGray,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun FeatureCard(title: String, description: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = Panel),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = Neon, modifier = Modifier.size(30.dp))
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold)
                Text(description, color = Color.LightGray, style = MaterialTheme.typography.bodySmall)
            }
            Icon(Icons.Default.ChevronRight, null, tint = Violet)
        }
    }
}

@Composable
private fun ChatScreen(onSpeak: (String) -> Unit) {
    var message by remember { mutableStateOf("") }
    var transcript by remember { mutableStateOf(listOf("MAX AI: Hello! I’m ready. This is demo mode until an AI provider is connected.")) }
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("AI Chat", style = MaterialTheme.typography.headlineMedium, color = Neon, fontWeight = FontWeight.Bold)
        LazyColumn(Modifier.weight(1f).fillMaxWidth(), contentPadding = PaddingValues(vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(transcript) { line ->
                Card(colors = CardDefaults.cardColors(containerColor = Panel)) {
                    Text(line, Modifier.padding(14.dp))
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = message, onValueChange = { message = it }, modifier = Modifier.weight(1f),
                placeholder = { Text("Type a message…") }, shape = RoundedCornerShape(18.dp), maxLines = 3
            )
            Spacer(Modifier.width(8.dp))
            IconButton(onClick = {
                if (message.isNotBlank()) {
                    val reply = DemoReplyEngine.reply(message)
                    transcript = transcript + "You: $message" + "MAX AI: $reply"
                    onSpeak(reply)
                    message = ""
                }
            }) { Icon(Icons.Default.Send, contentDescription = "Send", tint = Neon) }
        }
    }
}

@Composable
private fun VoiceScreen(onSpeak: (String) -> Unit) {
    val context = LocalContext.current
    var heard by remember { mutableStateOf("Tap the microphone and speak. Language depends on your device speech service.") }
    var permissionGranted by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED)
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { permissionGranted = it }
    val speechLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val spoken = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
        if (!spoken.isNullOrBlank()) {
            heard = "You said: $spoken\n\nMAX AI: ${DemoReplyEngine.reply(spoken)}"
            onSpeak(DemoReplyEngine.reply(spoken))
        } else if (result.resultCode != android.app.Activity.RESULT_OK) {
            heard = "No speech was captured. Please try again."
        }
    }

    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Voice Assistant", style = MaterialTheme.typography.headlineMedium, color = Neon, fontWeight = FontWeight.Bold)
        Text("Speech recognition + Android TextToSpeech", color = Color.LightGray)
        Card(colors = CardDefaults.cardColors(containerColor = Panel)) {
            Text(heard, Modifier.padding(18.dp))
        }
        Button(onClick = {
            if (!permissionGranted) {
                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            } else if (SpeechRecognizer.isRecognitionAvailable(context)) {
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_PROMPT, "Talk to MAX AI")
                }
                try {
                    speechLauncher.launch(intent)
                    heard = "Listening… speak clearly into your phone microphone."
                } catch (_: Exception) {
                    heard = "Speech input could not be opened on this device."
                }
            } else {
                heard = "No speech recognition service is available. Install or enable a compatible speech service."
            }
        }, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.Mic, null)
            Spacer(Modifier.width(8.dp))
            Text("Start voice input")
        }
        OutlinedButton(onClick = { onSpeak("Hello! I am MAX AI. How can I help you today?") }, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.VolumeUp, null)
            Spacer(Modifier.width(8.dp))
            Text("Test assistant voice")
        }
        Text("Note: speech recognition availability and Bengali voice quality depend on installed Android speech services.", color = Color.LightGray, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun ModuleScreen(modules: Map<String, Boolean>, onToggle: (String, Boolean) -> Unit) {
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Text("Module Hub", style = MaterialTheme.typography.headlineMedium, color = Neon, fontWeight = FontWeight.Bold)
            Text("Switch modules on or off individually.", color = Color.LightGray)
        }
        items(modules.keys.toList()) { name ->
            Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(18.dp)) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(name, fontWeight = FontWeight.SemiBold)
                        Text(if (modules[name] == true) "Enabled" else "Disabled", color = Color.LightGray, style = MaterialTheme.typography.bodySmall)
                    }
                    Switch(checked = modules[name] == true, onCheckedChange = { onToggle(name, it) })
                }
            }
        }
    }
}

@Composable
private fun DeviceScreen() {
    val context = LocalContext.current
    val info = remember {
        listOf(
            "Android version" to android.os.Build.VERSION.RELEASE.orEmpty(),
            "SDK level" to android.os.Build.VERSION.SDK_INT.toString(),
            "Device" to "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}",
            "Device codename" to android.os.Build.DEVICE
        )
    }
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("Device Tools", style = MaterialTheme.typography.headlineMedium, color = Neon, fontWeight = FontWeight.Bold) }
        items(info) { (label, value) ->
            Card(colors = CardDefaults.cardColors(containerColor = Panel)) {
                Column(Modifier.fillMaxWidth().padding(16.dp)) {
                    Text(label, color = Violet, style = MaterialTheme.typography.labelLarge)
                    Text(value.ifBlank { "Not reported" }, style = MaterialTheme.typography.titleMedium)
                }
            }
        }
        item {
            Text("MAX AI does not change protected system settings or force CPU/GPU clocks. Such operations require vendor/system privileges.", color = Color.LightGray)
        }
    }
}

@Composable
private fun GamingScreen() {
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("Gaming Suite", style = MaterialTheme.typography.headlineMedium, color = Neon, fontWeight = FontWeight.Bold) }
        item { Text("Capability-aware tools", style = MaterialTheme.typography.titleMedium) }
        item { TipCard("Before gaming", "Close apps you are not using, enable your phone’s built-in Game Mode, and keep vents unobstructed.") }
        item { TipCard("Thermal safety", "If the device becomes hot, pause play and let it cool. MAX AI will not disable thermal protection.") }
        item { TipCard("Performance", "Use the game’s own graphics/FPS controls. Available refresh rates are managed by Android and the device manufacturer.") }
        item { TipCard("Game library", "A future iteration can list launchable installed games after adding the required package-visibility design and user controls.") }
    }
}

@Composable
private fun TipCard(title: String, body: String) {
    Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, fontWeight = FontWeight.Bold, color = Neon)
            Text(body, color = Color.LightGray)
        }
    }
}

internal object DemoReplyEngine {
    fun reply(input: String): String {
        val text = input.trim()
        if (text.isEmpty()) return "Please type a message first."
        val lower = text.lowercase()
        return when {
            lower.contains("hello") || lower.contains("hi") || text.contains("হ্যালো") || text.contains("নমস্কার") ->
                "Hello! I’m MAX AI. Tell me what you’d like to do."
            lower.contains("game") || lower.contains("gaming") || text.contains("গেম") ->
                "For safer gaming, use your phone’s built-in Game Mode, monitor heat, and adjust graphics inside the game."
            lower.contains("voice") || text.contains("ভয়েস") || text.contains("কথা") ->
                "You can test Android TextToSpeech on the Voice Assistant screen. Available languages depend on installed speech services."
            else -> "I received: \"$text\". This starter uses a local demo engine. Connect an AI provider to enable intelligent cloud responses."
        }
    }
}
