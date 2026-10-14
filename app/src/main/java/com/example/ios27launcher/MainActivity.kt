package com.example.ios27launcher

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class AppModel(
    val label: String,
    val packageName: String,
    val icon: ImageBitmap
)

data class WeatherData(
    val temp: Int = 30,
    val condition: String = "Memuat...",
    val high: Int = 33,
    val low: Int = 25
)

// Enum status fungsi resmi Dynamic Island
enum class IslandMode {
    IDLE,
    CHARGING,
    RECORDING_COMPACT,
    RECORDING_EXPANDED,
    ALARM_COMPACT,
    ALARM_EXPANDED
}

fun drawableToBitmap(drawable: Drawable): ImageBitmap {
    val bmp = Bitmap.createBitmap(140, 140, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)
    drawable.setBounds(0, 0, canvas.width, canvas.height)
    drawable.draw(canvas)
    return bmp.asImageBitmap()
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }

        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isStatusBarContrastEnforced = false
            window.isNavigationBarContrastEnforced = false
        }

        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.isAppearanceLightStatusBars = false
        controller.isAppearanceLightNavigationBars = false

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() { }
        })

        setContent {
            IOS27LauncherScreen()
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun IOS27LauncherScreen() {
    val context = LocalContext.current
    var apps by remember { mutableStateOf<List<AppModel>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }
    var showSpotlight by remember { mutableStateOf(false) }
    var selectedAppMenu by remember { mutableStateOf<AppModel?>(null) }

    // State Baterai Real-Time
    var batteryPct by remember { mutableStateOf(100) }
    var isCharging by remember { mutableStateOf(false) }

    // State Fungsional Dynamic Island
    var islandMode by remember { mutableStateOf(IslandMode.IDLE) }
    var recordSeconds by remember { mutableStateOf(0) }
    var alarmSeconds by remember { mutableStateOf(45) }

    // State Cuaca Real-Time
    var weather by remember { mutableStateOf(WeatherData()) }

    // Broadcast Baterai
    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                if (intent == null) return
                val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                if (level >= 0 && scale > 0) {
                    batteryPct = (level * 100) / scale
                }
                val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                val nowCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                        status == BatteryManager.BATTERY_STATUS_FULL

                // Otomatis memicu animasi cas saat kabel dicolok
                if (nowCharging && !isCharging) {
                    triggerHaptic(context)
                    islandMode = IslandMode.CHARGING
                }
                isCharging = nowCharging
            }
        }
        context.registerReceiver(receiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        onDispose { context.unregisterReceiver(receiver) }
    }

    // Auto-shrink saat cas selesai ditampilkan (3 detik)
    LaunchedEffect(islandMode) {
        if (islandMode == IslandMode.CHARGING) {
            delay(3000)
            if (islandMode == IslandMode.CHARGING) islandMode = IslandMode.IDLE
        }
    }

    // Timer Rekam Suara
    LaunchedEffect(islandMode) {
        if (islandMode == IslandMode.RECORDING_COMPACT || islandMode == IslandMode.RECORDING_EXPANDED) {
            while (islandMode == IslandMode.RECORDING_COMPACT || islandMode == IslandMode.RECORDING_EXPANDED) {
                delay(1000)
                recordSeconds++
            }
        } else {
            recordSeconds = 0
        }
    }

    // Timer Alarm Mundur
    LaunchedEffect(islandMode) {
        if (islandMode == IslandMode.ALARM_COMPACT || islandMode == IslandMode.ALARM_EXPANDED) {
            while ((islandMode == IslandMode.ALARM_COMPACT || islandMode == IslandMode.ALARM_EXPANDED) && alarmSeconds > 0) {
                delay(1000)
                alarmSeconds--
            }
            if (alarmSeconds <= 0) islandMode = IslandMode.IDLE
        }
    }

    // Ambil Cuaca Real
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            try {
                val url = URL("https://api.open-meteo.com/v1/forecast?latitude=-6.2088&longitude=106.8456&current=temperature_2m,weather_code&daily=temperature_2m_max,temperature_2m_min&timezone=auto")
                val conn = url.openConnection() as HttpURLConnection
                conn.connectTimeout = 5000
                if (conn.responseCode == 200) {
                    val res = conn.inputStream.bufferedReader().readText()
                    val json = JSONObject(res)
                    val current = json.getJSONObject("current")
                    val temp = current.getDouble("temperature_2m").toInt()
                    val code = current.getInt("weather_code")
                    val daily = json.getJSONObject("daily")
                    val maxArr = daily.getJSONArray("temperature_2m_max")
                    val minArr = daily.getJSONArray("temperature_2m_min")
                    val high = if (maxArr.length() > 0) maxArr.getDouble(0).toInt() else temp + 3
                    val low = if (minArr.length() > 0) minArr.getDouble(0).toInt() else temp - 4

                    val desc = when (code) {
                        0 -> "Cerah"
                        1, 2, 3 -> "Cerah Berawan"
                        45, 48 -> "Berkabut"
                        51, 53, 55, 61, 63, 65 -> "Hujan"
                        80, 81, 82 -> "Hujan Lebat"
                        95, 96, 99 -> "Badai Petir"
                        else -> "Berawan"
                    }
                    weather = WeatherData(temp = temp, condition = desc, high = high, low = low)
                }
            } catch (_: Exception) {
                weather = WeatherData(temp = 30, condition = "Cerah Berawan", high = 33, low = 25)
            }
        }
    }

    // Ambil Aplikasi Terinstal
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val pm = context.packageManager
            val intent = Intent(Intent.ACTION_MAIN, null).apply { addCategory(Intent.CATEGORY_LAUNCHER) }
            val resolveInfos = pm.queryIntentActivities(intent, 0)
            apps = resolveInfos.mapNotNull { info ->
                val pkg = info.activityInfo.packageName
                if (pkg == context.packageName) return@mapNotNull null
                AppModel(info.loadLabel(pm).toString(), pkg, drawableToBitmap(info.loadIcon(pm)))
            }.sortedBy { it.label.lowercase() }
        }
    }

    val filteredApps = remember(apps, searchQuery) {
        if (searchQuery.isBlank()) apps
        else apps.filter { it.label.contains(searchQuery, ignoreCase = true) }
    }

    // --- ANIMASI SPRING PHYSICS KHAS APPLE iOS (ELASTIS & KENYAL) ---
    val springSpec = spring<Float>(
        dampingRatio = 0.62f, // Efek sedikit membal lembut
        stiffness = Spring.StiffnessMediumLow
    )

    val targetWidth = when (islandMode) {
        IslandMode.IDLE -> 110f
        IslandMode.CHARGING -> 265f
        IslandMode.RECORDING_COMPACT -> 180f
        IslandMode.RECORDING_EXPANDED -> 330f
        IslandMode.ALARM_COMPACT -> 175f
        IslandMode.ALARM_EXPANDED -> 320f
    }
    val animatedWidth by animateFloatAsState(targetValue = targetWidth, animationSpec = springSpec, label = "w")

    val targetHeight = when (islandMode) {
        IslandMode.IDLE -> 32f
        IslandMode.CHARGING -> 46f
        IslandMode.RECORDING_COMPACT -> 38f
        IslandMode.RECORDING_EXPANDED -> 88f
        IslandMode.ALARM_COMPACT -> 38f
        IslandMode.ALARM_EXPANDED -> 86f
    }
    val animatedHeight by animateFloatAsState(targetValue = targetHeight, animationSpec = springSpec, label = "h")

    // Equalizer Suara
    val infiniteTransition = rememberInfiniteTransition(label = "eq")
    val wave1 by infiniteTransition.animateFloat(8f, 22f, infiniteRepeatable(tween(280), RepeatMode.Reverse), label = "")
    val wave2 by infiniteTransition.animateFloat(22f, 9f, infiniteRepeatable(tween(400), RepeatMode.Reverse), label = "")
    val wave3 by infiniteTransition.animateFloat(11f, 26f, infiniteRepeatable(tween(340), RepeatMode.Reverse), label = "")

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0A1128).copy(alpha = 0.40f),
                        Color(0xFF1C2541).copy(alpha = 0.65f),
                        Color(0xFF0B132B).copy(alpha = 0.80f)
                    )
                )
            )
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(6.dp))

            // --- 1. DYNAMIC ISLAND (AUTENTIK APPLE HIG) ---
            Box(
                modifier = Modifier
                    .width(animatedWidth.dp)
                    .height(animatedHeight.dp)
                    .clip(RoundedCornerShape(42.dp))
                    .background(Color.Black)
                    .border(0.7.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(42.dp))
                    .combinedClickable(
                        onClick = {
                            triggerHaptic(context)
                            // Interaktivitas klik fungsi resmi:
                            islandMode = when (islandMode) {
                                IslandMode.RECORDING_COMPACT -> IslandMode.RECORDING_EXPANDED
                                IslandMode.RECORDING_EXPANDED -> IslandMode.RECORDING_COMPACT
                                IslandMode.ALARM_COMPACT -> IslandMode.ALARM_EXPANDED
                                IslandMode.ALARM_EXPANDED -> IslandMode.ALARM_COMPACT
                                IslandMode.IDLE -> IslandMode.RECORDING_COMPACT // Klik saat idle mengaktifkan demo rekam
                                else -> IslandMode.IDLE
                            }
                        },
                        onLongClick = {
                            triggerHaptic(context)
                            // Tekan lama beralih ke simulasi fungsi Alarm
                            islandMode = if (islandMode == IslandMode.ALARM_COMPACT) IslandMode.IDLE else IslandMode.ALARM_COMPACT
                        }
                    )
                    .padding(horizontal = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                AnimatedContent(
                    targetState = islandMode,
                    transitionSpec = {
                        (fadeIn(animationSpec = tween(180, delayMillis = 60)) + scaleIn(initialScale = 0.92f))
                            .togetherWith(fadeOut(animationSpec = tween(120)) + scaleOut(targetScale = 0.92f))
                    },
                    label = "content"
                ) { mode ->
                    when (mode) {
                        IslandMode.IDLE -> {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(modifier = Modifier.size(11.dp).clip(CircleShape).background(Color(0xFF101012)))
                                Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(if (isCharging) Color(0xFF30D158) else Color(0xFF18181A)))
                            }
                        }

                        IslandMode.CHARGING -> {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("⚡", fontSize = 15.sp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Mengisi Daya", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                }
                                Text("$batteryPct%", color = Color(0xFF30D158), fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        // REKAM SUARA (MODE RAMPING)
                        IslandMode.RECORDING_COMPACT -> {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(modifier = Modifier.size(9.dp).clip(CircleShape).background(Color(0xFFFF453A)))
                                Text(
                                    String.format("%02d:%02d", recordSeconds / 60, recordSeconds % 60),
                                    color = Color(0xFFFF453A),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(2.5.dp), verticalAlignment = Alignment.CenterVertically) {
                                    listOf(wave1, wave2, wave3, wave1).forEach { h ->
                                        Box(modifier = Modifier.width(2.5.dp).height(h.dp).clip(RoundedCornerShape(1.dp)).background(Color(0xFFFF9F0A)))
                                    }
                                }
                            }
                        }

                        // REKAM SUARA (MODE MEKAR / DIKLIK)
                        IslandMode.RECORDING_EXPANDED -> {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.Center
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(Color(0xFFFF453A)))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Memo Suara", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                    }
                                    Text(
                                        String.format("%02d:%02d", recordSeconds / 60, recordSeconds % 60),
                                        color = Color(0xFFFF453A),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(3.5.dp), verticalAlignment = Alignment.CenterVertically) {
                                        listOf(wave1, wave2, wave3, wave2, wave1, wave3, wave2).forEach { h ->
                                            Box(modifier = Modifier.width(3.dp).height((h * 1.2).dp).clip(RoundedCornerShape(2.dp)).background(Color(0xFFFF9F0A)))
                                        }
                                    }
                                    Button(
                                        onClick = { islandMode = IslandMode.IDLE },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF453A)),
                                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 2.dp)
                                    ) {
                                        Text("Berhenti", fontSize = 11.sp, color = Color.White)
                                    }
                                }
                            }
                        }

                        // ALARM (MODE RAMPING)
                        IslandMode.ALARM_COMPACT -> {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("🔔", fontSize = 12.sp)
                                Text(
                                    String.format("%02ds", alarmSeconds),
                                    color = Color(0xFFFF9F0A),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // ALARM (MODE MEKAR / DIKLIK)
                        IslandMode.ALARM_EXPANDED -> {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.Center
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("⏰", fontSize = 14.sp)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Alarm Berjalan", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                    }
                                    Text(
                                        String.format("%02ds lagi", alarmSeconds),
                                        color = Color(0xFFFF9F0A),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    Button(
                                        onClick = { islandMode = IslandMode.IDLE },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3A3A3C)),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp)
                                    ) {
                                        Text("Matikan", fontSize = 11.sp, color = Color.White)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // --- 2. WIDGET BARIS ATAS (BATERAI MURNI NON-KLIK) ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(145.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Widget Cuaca
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(26.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFF1D6FA3).copy(alpha = 0.82f), Color(0xFF0F3460).copy(alpha = 0.82f))
                            )
                        )
                        .border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(26.dp))
                        .padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Jakarta", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                            Text("${weather.temp}°", color = Color.White, fontWeight = FontWeight.Light, fontSize = 40.sp)
                        }
                        Text(
                            "${weather.condition}\nT:${weather.high}°  R:${weather.low}°",
                            color = Color.White.copy(alpha = 0.88f),
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )
                    }
                }

                // Widget Baterai Real-Time (MURNI INDIKATOR, KLIK SUDAH DINONAKTIFKAN)
                val batteryColor = when {
                    isCharging -> Color(0xFF30D158)
                    batteryPct <= 20 -> Color(0xFFFF453A)
                    else -> Color(0xFF30D158)
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(26.dp))
                        .background(Color.White.copy(alpha = 0.15f))
                        .border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(26.dp))
                        .padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                if (isCharging) "⚡ Mengisi" else "Baterai",
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp
                            )
                            Text(
                                "$batteryPct%",
                                color = batteryColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp)
                                .clip(RoundedCornerShape(5.dp))
                                .background(Color.White.copy(alpha = 0.2f))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth((batteryPct / 100f).coerceIn(0.05f, 1f))
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(5.dp))
                                    .background(batteryColor)
                            )
                        }
                        Text(
                            if (isCharging) "Pengisian daya aktif" else "Kapasitas baterai saat ini",
                            color = Color.White.copy(alpha = 0.78f),
                            fontSize = 11.sp,
                            lineHeight = 14.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // --- 3. GRID APLIKASI ---
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(18.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredApps, key = { it.packageName }) { app ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .combinedClickable(
                                onClick = { launchApp(context, app.packageName) },
                                onLongClick = {
                                    triggerHaptic(context)
                                    selectedAppMenu = app
                                }
                            )
                    ) {
                        Box(
                            modifier = Modifier
                                .size(62.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color.White.copy(alpha = 0.18f))
                                .border(0.8.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
                                .padding(6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                bitmap = app.icon,
                                contentDescription = app.label,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(12.dp))
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = app.label,
                            color = Color.White,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // --- 4. SPOTLIGHT PILL ---
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White.copy(alpha = 0.18f))
                    .border(0.6.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
                    .clickable { showSpotlight = !showSpotlight }
                    .padding(horizontal = 18.dp, vertical = 6.dp)
            ) {
                Text("🔍 Cari", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            }

            AnimatedVisibility(visible = showSpotlight, enter = fadeIn(), exit = fadeOut()) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Spotlight Search aplikasi...", color = Color.White.copy(0.6f)) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    shape = RoundedCornerShape(18.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // --- 5. GLASS DOCK BAWAH ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(92.dp)
                    .padding(bottom = 8.dp)
                    .clip(RoundedCornerShape(34.dp))
                    .background(Color.White.copy(alpha = 0.18f))
                    .border(1.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(34.dp))
                    .padding(horizontal = 18.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    apps.take(4).forEach { dockApp ->
                        Box(
                            modifier = Modifier
                                .size(58.dp)
                                .clip(RoundedCornerShape(15.dp))
                                .background(Color.White.copy(alpha = 0.2f))
                                .clickable { launchApp(context, dockApp.packageName) }
                                .padding(5.dp)
                        ) {
                            Image(
                                bitmap = dockApp.icon,
                                contentDescription = dockApp.label,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(12.dp))
                            )
                        }
                    }
                }
            }
        }

        selectedAppMenu?.let { app ->
            AlertDialog(
                onDismissRequest = { selectedAppMenu = null },
                containerColor = Color(0xFF1C1C1E).copy(alpha = 0.95f),
                titleContentColor = Color.White,
                textContentColor = Color.White.copy(alpha = 0.8f),
                title = { Text(app.label, fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Paket: ${app.packageName}", fontSize = 12.sp)
                        Text(
                            "📱 Buka Aplikasi",
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    launchApp(context, app.packageName)
                                    selectedAppMenu = null
                                }
                                .padding(vertical = 6.dp)
                        )
                        Text(
                            "⚙️ Info Aplikasi Sistem",
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                        data = android.net.Uri.parse("package:${app.packageName}")
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                    context.startActivity(intent)
                                    selectedAppMenu = null
                                }
                                .padding(vertical = 6.dp)
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = { selectedAppMenu = null }) {
                        Text("Tutup", color = Color(0xFF0A84FF))
                    }
                }
            )
        }
    }
}

fun launchApp(context: Context, packageName: String) {
    val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName)
    if (launchIntent != null) {
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(launchIntent)
    }
}

fun triggerHaptic(context: Context) {
    val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    vibrator?.vibrate(VibrationEffect.createOneShot(25, VibrationEffect.DEFAULT_AMPLITUDE))
}