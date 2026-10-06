package com.bro.assistant

import android.app.Application
import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import java.io.File
import kotlin.math.PI
import kotlin.math.sin
import org.json.JSONArray
import org.json.JSONObject



class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )
        setContent { BroApp() }
    }
}



class ChatViewModel(app: Application) : AndroidViewModel(app) {
    private val store = HistoryStore(app)
    private var currentId = 0L

    val conversations = mutableStateListOf<Conversation>()
    val messages = mutableStateListOf<ChatMessage>()

    init {
        conversations.addAll(store.load())
        startNew()
    }

    fun startNew() {
        currentId = System.currentTimeMillis()
        messages.clear()
    }

    fun send(text: String) {
        messages.add(ChatMessage(true, text))
        // Placeholder reply. The real brain arrives in later stages.
        messages.add(ChatMessage(false, "I heard you. My brain is not connected yet, that comes in a later stage."))
        persist()
    }

    fun open(c: Conversation) {
        currentId = c.id
        messages.clear()
        messages.addAll(c.messages)
    }

    fun delete(c: Conversation) {
        conversations.remove(c)
        if (c.id == currentId) startNew()
        store.save(conversations.toList())
    }

    private fun persist() {
        if (messages.isEmpty()) return
        val title = messages.firstOrNull { it.fromUser }?.text?.take(40) ?: "Chat"
        val conv = Conversation(currentId, title, messages.toList())
        val idx = conversations.indexOfFirst { it.id == currentId }
        if (idx >= 0) conversations[idx] = conv else conversations.add(0, conv)
        store.save(conversations.toList())
    }
}



data class ChatMessage(val fromUser: Boolean, val text: String)

data class Conversation(val id: Long, val title: String, val messages: List<ChatMessage>)

/** Saves chat history as a JSON file inside the app's private storage. */
class HistoryStore(context: Context) {
    private val file = File(context.filesDir, "history.json")

    fun load(): List<Conversation> {
        if (!file.exists()) return emptyList()
        return try {
            val arr = JSONArray(file.readText())
            List(arr.length()) { i ->
                val o = arr.getJSONObject(i)
                val ms = o.getJSONArray("messages")
                Conversation(
                    id = o.getLong("id"),
                    title = o.getString("title"),
                    messages = List(ms.length()) { j ->
                        val m = ms.getJSONObject(j)
                        ChatMessage(m.getBoolean("user"), m.getString("text"))
                    }
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun save(list: List<Conversation>) {
        val arr = JSONArray()
        for (c in list) {
            val ms = JSONArray()
            for (m in c.messages) {
                ms.put(JSONObject().put("user", m.fromUser).put("text", m.text))
            }
            arr.put(JSONObject().put("id", c.id).put("title", c.title).put("messages", ms))
        }
        file.writeText(arr.toString())
    }
}



object BroColors {
    val Bg = Color(0xFF05070D)
    val Panel = Color(0xFF0E1420)
    val Cyan = Color(0xFF22D3EE)
    val Blue = Color(0xFF3B82F6)
    val Violet = Color(0xFF8B5CF6)
    val Text = Color(0xFFE6EDF7)
    val Muted = Color(0xFF8A97AB)
}

@Composable
fun BroTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = BroColors.Cyan,
            background = BroColors.Bg,
            surface = BroColors.Panel,
            onSurface = BroColors.Text,
            onBackground = BroColors.Text
        ),
        content = content
    )
}



enum class BroState(val label: String) {
    IDLE("Navi"),
    LISTENING("Listening"),
    THINKING("Thinking"),
    EXECUTING("Working"),
    SUCCESS("Done"),
    ERROR("Error")
}

@Composable
fun BroOrb(state: BroState, height: Dp, modifier: Modifier = Modifier) {
    val t = rememberInfiniteTransition(label = "orb")
    val spin by t.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(16000, easing = LinearEasing)),
        label = "spin"
    )
    val drop by t.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(3600, easing = LinearEasing)),
        label = "drop"
    )
    val wave by t.animateFloat(
        initialValue = 0f,
        targetValue = (2.0 * PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(2400, easing = LinearEasing)),
        label = "wave"
    )

    Column(modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Canvas(Modifier.fillMaxWidth().height(height)) {
            drawNavi(spin, drop, wave)
        }
        Text(
            text = state.label,
            color = BroColors.Cyan,
            fontSize = 20.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 2.sp
        )
    }
}

/**
 * Water orb: a drop forms on the tap, falls into the orb, the water rocks
 * and ripples spread, while two pairs of rings rotate around it.
 */
private fun DrawScope.drawNavi(spin: Float, drop: Float, wave: Float) {
    val c = Offset(size.width / 2f, size.height * 0.58f)
    val r = minOf(size.width, size.height) * 0.25f
    val ir = r * 0.95f

    // soft glow
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(BroColors.Cyan.copy(alpha = 0.22f), Color.Transparent),
            center = c,
            radius = r * 2f
        ),
        radius = r * 2f,
        center = c
    )

    // rotating rings
    fun ring(rad: Float, deg: Float, sweep: Float, w: Float, color: Color) {
        rotate(degrees = deg, pivot = c) {
            drawArc(
                color = color,
                startAngle = 0f,
                sweepAngle = sweep,
                useCenter = false,
                topLeft = Offset(c.x - rad, c.y - rad),
                size = Size(rad * 2f, rad * 2f),
                style = Stroke(width = w, cap = StrokeCap.Round)
            )
        }
    }
    drawCircle(color = BroColors.Cyan.copy(alpha = 0.10f), radius = r * 1.28f, center = c, style = Stroke(1.5f))
    ring(r * 1.28f, spin, 110f, 5f, BroColors.Cyan)
    ring(r * 1.28f, spin + 180f, 50f, 5f, BroColors.Blue)
    ring(r * 1.48f, -spin * 1.6f, 150f, 3f, BroColors.Violet.copy(alpha = 0.85f))
    ring(r * 1.48f, -spin * 1.6f + 200f, 40f, 3f, BroColors.Cyan.copy(alpha = 0.7f))

    // water state
    val level = c.y + ir * 0.12f
    val impact = if (drop > 0.5f) (drop - 0.5f) * 2f else 0f
    val kick = if (drop > 0.5f) (1f - impact) * (1f - impact) else 0f
    val amp = ir * 0.035f + ir * 0.11f * kick

    fun wavePath(phase: Float, a: Float): Path {
        val p = Path()
        val steps = 48
        p.moveTo(c.x - ir, c.y + ir)
        for (i in 0..steps) {
            val x = c.x - ir + (2f * ir) * i / steps
            val y = level + sin(i * 0.30f + phase) * a
            p.lineTo(x, y)
        }
        p.lineTo(c.x + ir, c.y + ir)
        p.close()
        return p
    }

    val clip = Path().apply { addOval(Rect(center = c, radius = ir)) }
    clipPath(clip) {
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFF0B1730), Color(0xFF060C18)),
                startY = c.y - ir,
                endY = c.y + ir
            ),
            topLeft = Offset(c.x - ir, c.y - ir),
            size = Size(ir * 2f, ir * 2f)
        )
        drawPath(
            path = wavePath(wave + 2f, amp * 0.8f),
            brush = Brush.verticalGradient(
                colors = listOf(BroColors.Blue.copy(alpha = 0.55f), BroColors.Violet.copy(alpha = 0.35f)),
                startY = level - amp,
                endY = c.y + ir
            )
        )
        drawPath(
            path = wavePath(wave, amp),
            brush = Brush.verticalGradient(
                colors = listOf(BroColors.Cyan.copy(alpha = 0.85f), BroColors.Blue.copy(alpha = 0.5f)),
                startY = level - amp,
                endY = c.y + ir
            )
        )
        // ripples spreading on the surface after the drop lands
        for (k in 0..2) {
            val p = (impact - k * 0.15f) / (1f - k * 0.15f)
            if (p > 0f && p < 1f) {
                drawOval(
                    color = Color.White.copy(alpha = 0.55f * (1f - p)),
                    topLeft = Offset(c.x - ir * 0.95f * p, level - ir * 0.13f * p),
                    size = Size(ir * 1.9f * p, ir * 0.26f * p),
                    style = Stroke(2.5f)
                )
            }
        }
    }
    drawCircle(color = BroColors.Cyan.copy(alpha = 0.65f), radius = ir, center = c, style = Stroke(3f))

    // tap and falling drop
    val tapY = c.y - r * 1.78f
    drawRoundRect(
        color = Color(0xFF2B3A55),
        topLeft = Offset(c.x - r * 0.09f, tapY - r * 0.12f),
        size = Size(r * 0.18f, r * 0.12f),
        cornerRadius = CornerRadius(8f, 8f)
    )
    if (drop <= 0.5f) {
        val p = drop / 0.5f
        val swell = minOf(1f, p / 0.18f)
        val fall = if (p < 0.18f) 0f else (p - 0.18f) / 0.82f
        val y = tapY + (level - tapY) * fall * fall
        drawCircle(
            color = BroColors.Cyan,
            radius = r * 0.035f * (0.4f + 0.6f * swell),
            center = Offset(c.x, y)
        )
    }
}



enum class Screen { CHAT, HISTORY, SETTINGS }

@Composable
fun BroApp(vm: ChatViewModel = viewModel()) {
    var screen by remember { mutableStateOf(Screen.CHAT) }
    BackHandler(enabled = screen != Screen.CHAT) { screen = Screen.CHAT }

    BroTheme {
        Box(
            Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(Color(0xFF0B1530), BroColors.Bg)))
                .systemBarsPadding()
                .imePadding()
        ) {
            when (screen) {
                Screen.CHAT -> ChatScreen(
                    vm = vm,
                    onHistory = { screen = Screen.HISTORY },
                    onSettings = { screen = Screen.SETTINGS }
                )
                Screen.HISTORY -> HistoryScreen(vm = vm, onBack = { screen = Screen.CHAT })
                Screen.SETTINGS -> SettingsScreen(onBack = { screen = Screen.CHAT })
            }
        }
    }
}

@Composable
private fun ChatScreen(vm: ChatViewModel, onHistory: () -> Unit, onSettings: () -> Unit) {
    var input by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val context = LocalContext.current
    val imeUp = WindowInsets.ime.getBottom(LocalDensity.current) > 0

    LaunchedEffect(vm.messages.size) {
        if (vm.messages.isNotEmpty()) listState.animateScrollToItem(vm.messages.lastIndex)
    }

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onHistory) { Text("History", color = BroColors.Cyan) }
            Text("BRO", color = BroColors.Text, fontSize = 18.sp, fontWeight = FontWeight.Bold, letterSpacing = 4.sp)
            Row {
                TextButton(onClick = { vm.startNew() }) { Text("+ New", color = BroColors.Cyan) }
                TextButton(onClick = onSettings) { Text("Settings", color = BroColors.Muted) }
            }
        }

        BroOrb(state = BroState.IDLE, height = if (imeUp) 130.dp else 280.dp)

        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (vm.messages.isEmpty()) {
                item {
                    Text(
                        "Type a message to start talking with Navi.",
                        color = BroColors.Muted,
                        fontSize = 14.sp,
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                    )
                }
            }
            items(vm.messages) { m -> Bubble(m) }
        }

        Row(
            Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircleButton(label = "Mic", color = Color(0xFF1B2640), tint = BroColors.Text) {
                Toast.makeText(context, "Voice input arrives in Stage 3", Toast.LENGTH_SHORT).show()
            }
            Spacer(Modifier.width(8.dp))
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Message BRO", color = BroColors.Muted) },
                maxLines = 3,
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = BroColors.Text,
                    unfocusedTextColor = BroColors.Text,
                    focusedBorderColor = BroColors.Cyan,
                    unfocusedBorderColor = Color(0xFF26324A),
                    cursorColor = BroColors.Cyan
                )
            )
            Spacer(Modifier.width(8.dp))
            CircleButton(label = "Send", color = BroColors.Cyan, tint = Color(0xFF04101A)) {
                val text = input.trim()
                if (text.isNotEmpty()) {
                    vm.send(text)
                    input = ""
                }
            }
        }
    }
}

@Composable
private fun CircleButton(label: String, color: Color, tint: Color, onClick: () -> Unit) {
    Box(
        Modifier
            .size(52.dp)
            .clip(CircleShape)
            .background(color)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = tint, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun Bubble(m: ChatMessage) {
    val shape = RoundedCornerShape(18.dp)
    val border = if (m.fromUser) BroColors.Cyan.copy(alpha = 0.5f) else Color(0xFF1E2A40)
    val fill = if (m.fromUser) BroColors.Cyan.copy(alpha = 0.16f) else BroColors.Panel
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = if (m.fromUser) Arrangement.End else Arrangement.Start
    ) {
        Box(
            Modifier
                .widthIn(max = 290.dp)
                .clip(shape)
                .background(fill)
                .border(1.dp, border, shape)
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Text(m.text, color = BroColors.Text, fontSize = 15.sp)
        }
    }
}

@Composable
private fun SubTopBar(title: String, onBack: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextButton(onClick = onBack) { Text("Back", color = BroColors.Cyan) }
        Text(title, color = BroColors.Text, fontSize = 18.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun HistoryScreen(vm: ChatViewModel, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        SubTopBar("History", onBack)
        if (vm.conversations.isEmpty()) {
            Text(
                "No saved chats yet. Send a message and the chat appears here.",
                color = BroColors.Muted,
                fontSize = 14.sp,
                modifier = Modifier.padding(20.dp)
            )
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(vm.conversations, key = { it.id }) { c ->
                val shape = RoundedCornerShape(16.dp)
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(shape)
                        .background(BroColors.Panel)
                        .border(1.dp, Color(0xFF1E2A40), shape)
                        .clickable {
                            vm.open(c)
                            onBack()
                        }
                        .padding(start = 16.dp, top = 6.dp, bottom = 6.dp, end = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(c.title, color = BroColors.Text, fontSize = 15.sp, maxLines = 1)
                        Text("${c.messages.size} messages", color = BroColors.Muted, fontSize = 12.sp)
                    }
                    TextButton(onClick = { vm.delete(c) }) {
                        Text("Delete", color = Color(0xFFFF6B6B))
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsScreen(onBack: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        SubTopBar("Settings", onBack)
        Text(
            "Voice, wake phrase and AI settings arrive in later stages.",
            color = BroColors.Muted,
            fontSize = 14.sp,
            modifier = Modifier.padding(20.dp)
        )
        Text(
            "BRO 0.1.0 (Stage 1)",
            color = BroColors.Muted,
            fontSize = 12.sp,
            modifier = Modifier.padding(horizontal = 20.dp)
        )
    }
}
