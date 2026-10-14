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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

@Immutable
data class AppModel(
    val label: String,
    val packageName: String,
    val icon: ImageBitmap
)

@Immutable
data class WeatherData(
    val temp: Int = 30,
    val condition: String = "Cerah Berawan",
    val high: Int = 33,
    val low: Int = 25
)

enum class IslandMode {
    IDLE,
    CHARGING,
    RECORDING_COMPACT,
    RECORDING_EXPANDED,
    ALARM_COMPACT,
    ALARM_EXPANDED
}

// Ukuran bitmap dioptimalkan ke 112px agar render GPU sangat ringan & smooth saat swipe
fun drawableToBitmap(drawable: Drawable): ImageBitmap {
    val bmp = Bitmap.createBitmap(112, 112, Bitmap.Config.ARGB_8888)
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
    val focusManager = LocalFocusManager.current
    val coroutineScope = rememberCoroutineScope()

    var apps by remember { mutableStateOf<List<AppModel>>(emptyList()) }
    var selectedAppMenu by remember { mutableStateOf<AppModel?>(null) }

    // State Baterai Real-Time
    var batteryPct by remember { mutableIntStateOf(100) }
    var isCharging by remember { mutableStateOf(false) }

    // State Dynamic Island
    var islandMode by remember { mutableStateOf(IslandMode.IDLE) }
    var recordSeconds by remember { mutableIntStateOf(0) }
    var alarmSeconds by remember { mutableIntStateOf(45) }

    // State Cuaca Real-Time
    var weather by remember { mutableStateOf(WeatherData()) }

    // Sensor Baterai
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

    LaunchedEffect(islandMode) {
        if (islandMode == IslandMode.CHARGING) {
            delay(3000)
            if (islandMode == IslandMode.CHARGING) islandMode = IslandMode.IDLE
        }
    }

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

    LaunchedEffect(islandMode) {
        if (islandMode == IslandMode.ALARM_COMPACT || islandMode == IslandMode.ALARM_EXPANDED) {
            while ((islandMode == IslandMode.ALARM_COMPACT || islandMode == IslandMode.ALARM_EXPANDED) && alarmSeconds > 0) {
                delay(1000)
                alarmSeconds--
            }
            if (alarmSeconds <= 0) islandMode = IslandMode.IDLE
        }
    }

    // Ambil Data Cuaca Live
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
            } catch (_: Exception) { }
        }
    }

    // Muat Aplikasi Sekali di Background Thread
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

    // Pembagian Halaman:
    // Page 0 = Halaman Search & App Library (Paling Kiri Mentok)
    // Page 1 = Widget Atas + 12 Aplikasi (3 baris x 4 kolom)
    // Page 2..N = Masing-masing 20 Aplikasi (5 baris x 4 kolom)
    val firstPageApps by remember(apps) {
        derivedStateOf { apps.take(12) }
    }
    val extraPages by remember(apps) {
        derivedStateOf {
            if (apps.size > 12) apps.drop(12).chunked(20) else emptyList()
        }
    }
    val totalPages = 2 + extraPages.size

    // Mulai di Page 1 (Halaman Utama Pertama), sehingga kalau swipe mentok ke kiri masuk ke Page 0 (Search)
    val pagerState = rememberPagerState(initialPage = 1, pageCount = { totalPages })

    // Tutup keyboard otomatis jika keluar dari halaman Search (Page 0)
    LaunchedEffect(pagerState.currentPage) {
        if (pagerState.currentPage != 0) {
            focusManager.clearFocus()
        }
    }

    // Animasi Pegas Dynamic Island
    val springSpec = spring<Float>(dampingRatio = 0.62f, stiffness = Spring.StiffnessMediumLow)
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
                        Color(0xFF0A1128).copy(alpha = 0.38f),
                        Color(0xFF1C2541).copy(alpha = 0.62f),
                        Color(0xFF0B132B).copy(alpha = 0.78f)
                    )
                )
            )
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(6.dp))

            // 1. DYNAMIC ISLAND DI KAMERA ATAS
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
                            islandMode = when (islandMode) {
                                IslandMode.RECORDING_COMPACT -> IslandMode.RECORDING_EXPANDED
                                IslandMode.RECORDING_EXPANDED -> IslandMode.RECORDING_COMPACT
                                IslandMode.ALARM_COMPACT -> IslandMode.ALARM_EXPANDED
                                IslandMode.ALARM_EXPANDED -> IslandMode.ALARM_COMPACT
                                IslandMode.IDLE -> IslandMode.RECORDING_COMPACT
                                else -> IslandMode.IDLE
                            }
                        },
                        onLongClick = {
                            triggerHaptic(context)
                            islandMode = if (islandMode == IslandMode.ALARM_COMPACT) IslandMode.IDLE else IslandMode.ALARM_COMPACT
                        }
                    )
                    .padding(horizontal = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                AnimatedContent(
                    targetState = islandMode,
                    transitionSpec = {
                        (fadeIn(tween(160, delayMillis = 50)) + scaleIn(initialScale = 0.92f))
                            .togetherWith(fadeOut(tween(110)) + scaleOut(targetScale = 0.92f))
                    },
                    label = "island"
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
                        IslandMode.RECORDING_EXPANDED -> {
                            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.Center) {
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
                        IslandMode.ALARM_COMPACT -> {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("🔔", fontSize = 12.sp)
                                Text(String.format("%02ds", alarmSeconds), color = Color(0xFFFF9F0A), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        IslandMode.ALARM_EXPANDED -> {
                            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.Center) {
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
                                    Text(String.format("%02ds lagi", alarmSeconds), color = Color(0xFFFF9F0A), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
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

            Spacer(modifier = Modifier.height(14.dp))

            // 2. HORIZONTAL PAGER (SWIPE KANAN-KIRI & SWIPE KIRI MENTOK UNTUK SEARCH)
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                beyondBoundsPageCount = 1
            ) { pageIndex ->
                when (pageIndex) {
                    // PAGE 0 (MENTOK PALING KIRI): HALAMAN SPOTLIGHT SEARCH & APP LIBRARY
                    0 -> {
                        LeftSearchPage(
                            allApps = apps,
                            onLaunchApp = { pkg -> launchApp(context, pkg) },
                            onLongClickApp = { app ->
                                triggerHaptic(context)
                                selectedAppMenu = app
                            }
                        )
                    }

                    // PAGE 1: HALAMAN UTAMA PERTAMA (WIDGET CUACA + BATERAI + 12 APLIKASI)
                    1 -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 18.dp),
                            verticalArrangement = Arrangement.spacedBy(18.dp)
                        ) {
                            // Baris Widget Atas
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(142.dp),
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
                                                listOf(Color(0xFF1D6FA3).copy(alpha = 0.84f), Color(0xFF0F3460).copy(alpha = 0.84f))
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
                                            Text("${weather.temp}°", color = Color.White, fontWeight = FontWeight.Light, fontSize = 38.sp)
                                        }
                                        Text(
                                            "${weather.condition}\nT:${weather.high}°  R:${weather.low}°",
                                            color = Color.White.copy(alpha = 0.88f),
                                            fontSize = 12.sp,
                                            lineHeight = 15.sp
                                        )
                                    }
                                }

                                // Widget Baterai (Pasif Non-Klik)
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
                                            if (isCharging) "Pengisian daya aktif" else "Kapasitas daya saat ini",
                                            color = Color.White.copy(alpha = 0.78f),
                                            fontSize = 11.sp,
                                            lineHeight = 14.sp
                                        )
                                    }
                                }
                            }

                            // 3 Baris x 4 Kolom Aplikasi di Halaman 1
                            StaticAppPageGrid(
                                pageApps = firstPageApps,
                                onLaunchApp = { pkg -> launchApp(context, pkg) },
                                onLongClickApp = { app ->
                                    triggerHaptic(context)
                                    selectedAppMenu = app
                                }
                            )
                        }
                    }

                    // PAGE 2..N: HALAMAN APLIKASI PENUH (5 BARIS x 4 KOLOM TANPA SCROLL VERTIKAL)
                    else -> {
                        val pageApps = extraPages.getOrNull(pageIndex - 2) ?: emptyList()
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 18.dp)
                        ) {
                            StaticAppPageGrid(
                                pageApps = pageApps,
                                onLaunchApp = { pkg -> launchApp(context, pkg) },
                                onLongClickApp = { app ->
                                    triggerHaptic(context)
                                    selectedAppMenu = app
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // 3. INDIKATOR TITIK HALAMAN ALA iOS (Menggantikan tombol Cari di atas Dock)
            Row(
                horizontalArrangement = Arrangement.spacedBy(7.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black.copy(alpha = 0.22f))
                    .padding(horizontal = 12.dp, vertical = 5.dp)
            ) {
                for (i in 0 until totalPages) {
                    val isSelected = pagerState.currentPage == i
                    Box(
                        modifier = Modifier
                            .size(if (i == 0) 8.dp else 7.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    isSelected -> Color.White
                                    i == 0 -> Color(0xFF64D2FF).copy(alpha = 0.55f) // Titik kiri penanda Search Page
                                    else -> Color.White.copy(alpha = 0.32f)
                                }
                            )
                            .clickable {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(i)
                                }
                            }
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 4. GLASS DOCK BAWAH
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp)
                    .height(90.dp)
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

        // Context Menu saat ikon ditekan lama
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

// Grid Statis Ringan (Anti-Lag) untuk Setiap Halaman Geser Kanan-Kiri
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun StaticAppPageGrid(
    pageApps: List<AppModel>,
    onLaunchApp: (String) -> Unit,
    onLongClickApp: (AppModel) -> Unit
) {
    val rows = remember(pageApps) { pageApps.chunked(4) }
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        rows.forEach { rowApps ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                for (i in 0 until 4) {
                    val app = rowApps.getOrNull(i)
                    if (app != null) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .weight(1f)
                                .combinedClickable(
                                    onClick = { onLaunchApp(app.packageName) },
                                    onLongClick = { onLongClickApp(app) }
                                )
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(60.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color.White.copy(alpha = 0.18f))
                                    .border(0.8.dp, Color.White.copy(alpha = 0.32f), RoundedCornerShape(16.dp))
                                    .padding(5.dp),
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
                            Spacer(modifier = Modifier.height(5.dp))
                            Text(
                                text = app.label,
                                color = Color.White,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

// Halaman Paling Kiri Mentok: Pencarian Aplikasi (Spotlight & App Library)
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LeftSearchPage(
    allApps: List<AppModel>,
    onLaunchApp: (String) -> Unit,
    onLongClickApp: (AppModel) -> Unit
) {
    var query by remember { mutableStateOf("") }
    val filtered by remember(allApps, query) {
        derivedStateOf {
            if (query.isBlank()) allApps
            else allApps.filter { it.label.contains(query, ignoreCase = true) }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 18.dp)
    ) {
        // Kotak Pencarian Spotlight Khas iOS di Atas
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = {
                Text("🔍 Perpustakaan App & Cari...", color = Color.White.copy(alpha = 0.7f), fontSize = 14.sp)
            },
            singleLine = true,
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White.copy(alpha = 0.14f), RoundedCornerShape(20.dp)),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = Color.White.copy(alpha = 0.45f),
                unfocusedBorderColor = Color.White.copy(alpha = 0.22f),
                cursorColor = Color(0xFF0A84FF)
            )
        )

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = if (query.isBlank()) "Semua Aplikasi (${allApps.size})" else "Hasil Pencarian (${filtered.size})",
            color = Color.White.copy(alpha = 0.8f),
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(start = 4.dp, bottom = 10.dp)
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(filtered, key = { it.packageName }) { app ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.combinedClickable(
                        onClick = { onLaunchApp(app.packageName) },
                        onLongClick = { onLongClickApp(app) }
                    )
                ) {
                    Box(
                        modifier = Modifier
                            .size(58.dp)
                            .clip(RoundedCornerShape(15.dp))
                            .background(Color.White.copy(alpha = 0.18f))
                            .border(0.8.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(15.dp))
                            .padding(5.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            bitmap = app.icon,
                            contentDescription = app.label,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(11.dp))
                        )
                    }
                    Spacer(modifier = Modifier.height(5.dp))
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