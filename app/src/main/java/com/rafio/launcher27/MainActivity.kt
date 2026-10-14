package com.rafio.launcher27

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.media.AudioManager
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.KeyEvent
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
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

enum class IslandActiveFeature {
    SPOTIFY,
    VOICE_MEMO,
    ALARM
}

// Controller Spotify & Media (Aman, Tanpa Izin Rahasia / Bebas Cekal Play Protect)
object SpotifyMediaState {
    var isPlaying by mutableStateOf(false)
    var trackTitle by mutableStateOf("Spotify Musik")
    var trackArtist by mutableStateOf("Sentuh untuk buka Spotify")
    var albumTitle by mutableStateOf("")
    var trackDurationMs by mutableLongStateOf(210000L)
    var trackPositionMs by mutableLongStateOf(0L)
    var hasTrackInfo by mutableStateOf(false)

    fun openSpotify(context: Context) {
        val launchIntent = context.packageManager.getLaunchIntentForPackage("com.spotify.music")
        if (launchIntent != null) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(launchIntent)
        } else {
            val storeIntent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.spotify.music")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try { context.startActivity(storeIntent) } catch (_: Exception) {}
        }
    }

    fun playPause(context: Context) {
        sendMediaKeyEvent(context, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
        isPlaying = !isPlaying
    }

    fun next(context: Context) {
        sendMediaKeyEvent(context, KeyEvent.KEYCODE_MEDIA_NEXT)
        isPlaying = true
    }

    fun previous(context: Context) {
        sendMediaKeyEvent(context, KeyEvent.KEYCODE_MEDIA_PREVIOUS)
        isPlaying = true
    }

    private fun sendMediaKeyEvent(context: Context, keyCode: Int) {
        try {
            val am = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            am?.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, keyCode))
            am?.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, keyCode))
        } catch (_: Exception) {}
    }
}

fun drawableToBitmap(drawable: Drawable): ImageBitmap {
    val bmp = Bitmap.createBitmap(112, 112, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bmp)
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
                isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                        status == BatteryManager.BATTERY_STATUS_FULL
            }
        }
        context.registerReceiver(receiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        onDispose { context.unregisterReceiver(receiver) }
    }

    // Spotify Broadcast Receiver (Membaca info lagu asli saat Spotify berputar)
    DisposableEffect(context) {
        val spotifyReceiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                if (intent == null) return
                when (intent.action) {
                    "com.spotify.music.metadatachanged" -> {
                        val track = intent.getStringExtra("track")
                        val artist = intent.getStringExtra("artist") ?: "Spotify"
                        val album = intent.getStringExtra("album") ?: ""
                        val length = intent.getIntExtra("length", 0)
                        if (!track.isNullOrBlank()) {
                            SpotifyMediaState.trackTitle = track
                            SpotifyMediaState.trackArtist = artist
                            SpotifyMediaState.albumTitle = album
                            if (length > 0) SpotifyMediaState.trackDurationMs = length.toLong()
                            SpotifyMediaState.hasTrackInfo = true
                        }
                    }
                    "com.spotify.music.playbackstatechanged" -> {
                        val playing = intent.getBooleanExtra("playing", false)
                        val position = intent.getIntExtra("playbackPosition", 0)
                        SpotifyMediaState.isPlaying = playing
                        SpotifyMediaState.trackPositionMs = position.toLong()
                        SpotifyMediaState.hasTrackInfo = true
                    }
                }
            }
        }
        val filter = IntentFilter().apply {
            addAction("com.spotify.music.metadatachanged")
            addAction("com.spotify.music.playbackstatechanged")
            addAction("com.spotify.music.queuechanged")
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(spotifyReceiver, filter, Context.RECEIVER_EXPORTED)
        } else {
            context.registerReceiver(spotifyReceiver, filter)
        }
        onDispose {
            try { context.unregisterReceiver(spotifyReceiver) } catch (_: Exception) {}
        }
    }

    // Cek apakah sistem audio HP sedang memainkan musik
    LaunchedEffect(Unit) {
        val am = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        if (am?.isMusicActive == true) {
            SpotifyMediaState.isPlaying = true
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

    val firstPageApps by remember(apps) {
        derivedStateOf { apps.take(12) }
    }
    val extraPages by remember(apps) {
        derivedStateOf {
            if (apps.size > 12) apps.drop(12).chunked(20) else emptyList()
        }
    }
    val totalPages = 2 + extraPages.size

    val pagerState = rememberPagerState(initialPage = 1, pageCount = { totalPages })

    // Tutup keyboard otomatis jika keluar dari halaman Search
    LaunchedEffect(pagerState.currentPage) {
        if (pagerState.currentPage != 0) {
            focusManager.clearFocus()
        }
    }

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
        // LAYER 1: KONTEN UTAMA YANG DAPAT DIGESER (HORIZONTAL PAGER)
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Spacer pas di bawah Dynamic Island
            Spacer(modifier = Modifier.height(56.dp))

            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                beyondBoundsPageCount = 1
            ) { pageIndex ->
                when (pageIndex) {
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

                                // Widget Baterai
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

            // Indikator Titik Halaman ala iOS
            PageIndicator(
                currentPage = pagerState.currentPage,
                totalPages = totalPages,
                onPageSelected = { i ->
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(i)
                    }
                }
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Glass Dock Bawah
            GlassDock(
                dockApps = apps.take(4),
                onLaunchApp = { pkg -> launchApp(context, pkg) }
            )
        }

        // LAYER 2: DYNAMIC ISLAND INTERAKTIF (MUDAH DIKLIK & LENGKAP DENGAN MENU)
        // Posisi top = 10.dp menjamin sentuhan jari langsung terdeteksi tanpa terhalang gesture bar status Android
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp)
                .zIndex(20f),
            contentAlignment = Alignment.TopCenter
        ) {
            DynamicIslandBar(
                batteryPct = batteryPct,
                isCharging = isCharging
            )
        }

        // Context Menu saat ikon ditekan lama
        selectedAppMenu?.let { app ->
            AppContextMenu(
                app = app,
                onDismiss = { selectedAppMenu = null },
                onLaunch = { launchApp(context, app.packageName) }
            )
        }
    }
}

// ==========================================
// DYNAMIC ISLAND BAR (INTERAKTIF & RINGAN)
// ==========================================
@Composable
fun DynamicIslandBar(
    batteryPct: Int,
    isCharging: Boolean
) {
    val context = LocalContext.current
    var isExpanded by remember { mutableStateOf(false) }
    var activeTab by remember { mutableStateOf(IslandActiveFeature.SPOTIFY) }

    // State Spotify
    val isPlaying = SpotifyMediaState.isPlaying
    val trackTitle = SpotifyMediaState.trackTitle
    val trackArtist = SpotifyMediaState.trackArtist
    val durationMs = SpotifyMediaState.trackDurationMs
    var localPositionMs by remember { mutableLongStateOf(0L) }

    // State Perekam Suara (Voice Memo)
    var isRecording by remember { mutableStateOf(false) }
    var recordSeconds by remember { mutableIntStateOf(0) }

    // State Alarm & Timer
    var isAlarmRunning by remember { mutableStateOf(false) }
    var alarmSeconds by remember { mutableIntStateOf(45) }

    // State Animasi Charger
    var showChargingBanner by remember { mutableStateOf(false) }

    // Deteksi kabel charger dicolok
    LaunchedEffect(isCharging) {
        if (isCharging) {
            triggerHaptic(context)
            showChargingBanner = true
            delay(3500)
            showChargingBanner = false
        }
    }

    // Timer Putar Spotify
    LaunchedEffect(isPlaying, SpotifyMediaState.trackPositionMs) {
        localPositionMs = SpotifyMediaState.trackPositionMs
        while (isPlaying) {
            delay(1000)
            localPositionMs += 1000L
        }
    }

    // Timer Perekam Suara
    LaunchedEffect(isRecording) {
        while (isRecording) {
            delay(1000)
            recordSeconds++
        }
    }

    // Timer Alarm
    LaunchedEffect(isAlarmRunning) {
        while (isAlarmRunning && alarmSeconds > 0) {
            delay(1000)
            alarmSeconds--
        }
        if (alarmSeconds <= 0 && isAlarmRunning) {
            isAlarmRunning = false
            triggerHaptic(context)
        }
    }

    // Pegas Animasi Dynamic Island
    val springSpec = spring<Float>(dampingRatio = 0.72f, stiffness = Spring.StiffnessMediumLow)

    val targetWidth = when {
        isExpanded -> 344f
        showChargingBanner -> 268f
        isRecording -> 180f
        isAlarmRunning -> 175f
        isPlaying -> 215f
        else -> 150f
    }
    val animatedWidth by animateFloatAsState(targetValue = targetWidth, animationSpec = springSpec, label = "w")

    val targetHeight = when {
        isExpanded -> 210f
        showChargingBanner -> 44f
        else -> 38f
    }
    val animatedHeight by animateFloatAsState(targetValue = targetHeight, animationSpec = springSpec, label = "h")

    // Kontainer Dynamic Island
    Box(
        modifier = Modifier
            .width(animatedWidth.dp)
            .height(animatedHeight.dp)
            .clip(RoundedCornerShape(if (isExpanded) 34.dp else 40.dp))
            .background(Color.Black)
            .border(
                0.8.dp,
                Color.White.copy(alpha = 0.25f),
                RoundedCornerShape(if (isExpanded) 34.dp else 40.dp)
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                triggerHaptic(context)
                if (!isExpanded) {
                    isExpanded = true
                }
            }
            .padding(horizontal = if (isExpanded) 14.dp else 12.dp, vertical = if (isExpanded) 10.dp else 0.dp),
        contentAlignment = Alignment.Center
    ) {
        if (!isExpanded) {
            // MODE COMPACT (Kecil / Menempel di Kamera Atas)
            when {
                showChargingBanner -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("⚡", fontSize = 14.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Mengisi Daya", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                        Text("$batteryPct%", color = Color(0xFF30D158), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }

                isRecording -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(9.dp).clip(CircleShape).background(Color(0xFFFF453A)))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Memo", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                        Text(
                            String.format("%02d:%02d", recordSeconds / 60, recordSeconds % 60),
                            color = Color(0xFFFF453A),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        MiniEqualizer(isPlaying = true, barColor = Color(0xFFFF9F0A), modifier = Modifier.size(18.dp, 12.dp))
                    }
                }

                isAlarmRunning -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("🔔", fontSize = 12.sp)
                        Text(String.format("%02ds", alarmSeconds), color = Color(0xFFFF9F0A), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                isPlaying -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("🟢", fontSize = 10.sp)
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = trackTitle,
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.widthIn(max = 110.dp)
                            )
                        }
                        MiniEqualizer(isPlaying = true, barColor = Color(0xFF1DB954), modifier = Modifier.size(20.dp, 13.dp))
                    }
                }

                else -> {
                    // Siaga Normal (Kamera Punchhole Kiri + Teks Kanan)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.size(11.dp).clip(CircleShape).background(Color(0xFF101014)))
                        Text("iOS 27 • Buka", color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp, fontWeight = FontWeight.Medium)
                        Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(Color(0xFF30D158)))
                    }
                }
            }
        } else {
            // MODE EXPANDED: MENU LENGKAP (SPOTIFY, PEREKAM SUARA, ALARM)
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Header Baris 1: Tab Pilihan Menu Interaktif (Bisa Langsung Diklik)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        // Tab Spotify
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (activeTab == IslandActiveFeature.SPOTIFY) Color(0xFF1DB954) else Color.White.copy(0.12f))
                                .clickable {
                                    triggerHaptic(context)
                                    activeTab = IslandActiveFeature.SPOTIFY
                                }
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                "🎵 Spotify",
                                color = if (activeTab == IslandActiveFeature.SPOTIFY) Color.Black else Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Tab Perekam Suara
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (activeTab == IslandActiveFeature.VOICE_MEMO) Color(0xFFFF453A) else Color.White.copy(0.12f))
                                .clickable {
                                    triggerHaptic(context)
                                    activeTab = IslandActiveFeature.VOICE_MEMO
                                }
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                "🎙️ Perekam",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Tab Alarm
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (activeTab == IslandActiveFeature.ALARM) Color(0xFFFF9F0A) else Color.White.copy(0.12f))
                                .clickable {
                                    triggerHaptic(context)
                                    activeTab = IslandActiveFeature.ALARM
                                }
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                "⏰ Alarm",
                                color = if (activeTab == IslandActiveFeature.ALARM) Color.Black else Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Tombol Kecilkan / Tutup
                    IconButton(
                        onClick = {
                            triggerHaptic(context)
                            isExpanded = false
                        },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Text("▾", color = Color.White.copy(alpha = 0.8f), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Konten Sesuai Tab yang Dipilih
                when (activeTab) {
                    IslandActiveFeature.SPOTIFY -> {
                        // KONTEN SPOTIFY
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF1DB954))
                                    .clickable { SpotifyMediaState.openSpotify(context) },
                                contentAlignment = Alignment.Center
                            ) {
                                Text("🎧", fontSize = 22.sp)
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = trackTitle,
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = trackArtist,
                                    color = Color.White.copy(alpha = 0.72f),
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            ExpandedWaveformVisualizer(isPlaying = isPlaying, barColor = Color(0xFF1DB954), modifier = Modifier.size(38.dp, 20.dp))
                        }

                        // Timeline Durasi
                        val currentSec = (localPositionMs / 1000L).coerceAtLeast(0L)
                        val totalSec = (durationMs / 1000L).coerceAtLeast(1L)
                        val progressRatio = (currentSec.toFloat() / totalSec.toFloat()).coerceIn(0f, 1f)

                        Column(modifier = Modifier.fillMaxWidth()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(3.5.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(Color.White.copy(0.2f))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(progressRatio)
                                        .fillMaxHeight()
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(Color(0xFF1DB954))
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(String.format("%02d:%02d", currentSec / 60, currentSec % 60), color = Color.White.copy(0.6f), fontSize = 10.sp)
                                Text(String.format("%02d:%02d", totalSec / 60, totalSec % 60), color = Color.White.copy(0.6f), fontSize = 10.sp)
                            }
                        }

                        // Tombol Kontrol Musik
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = { SpotifyMediaState.previous(context); triggerHaptic(context) }, modifier = Modifier.size(34.dp)) {
                                Text("⏮", color = Color.White, fontSize = 16.sp)
                            }
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(Color.White)
                                    .clickable { SpotifyMediaState.playPause(context); triggerHaptic(context) },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(if (isPlaying) "⏸" else "▶", color = Color.Black, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            }
                            IconButton(onClick = { SpotifyMediaState.next(context); triggerHaptic(context) }, modifier = Modifier.size(34.dp)) {
                                Text("⏭", color = Color.White, fontSize = 16.sp)
                            }
                            Button(
                                onClick = { SpotifyMediaState.openSpotify(context); triggerHaptic(context) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1DB954)),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                            ) {
                                Text("Buka Spotify", fontSize = 11.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    IslandActiveFeature.VOICE_MEMO -> {
                        // KONTEN PEREKAM SUARA (VOICE MEMO)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(if (isRecording) Color(0xFFFF453A) else Color.Gray))
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text("Perekam Suara iOS", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    Text(if (isRecording) "Sedang merekam suara..." else "Siap merekam", color = Color.White.copy(0.7f), fontSize = 11.sp)
                                }
                            }
                            Text(
                                String.format("%02d:%02d", recordSeconds / 60, recordSeconds % 60),
                                color = Color(0xFFFF453A),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Waveform Gelombang Suara Oranye
                        ExpandedWaveformVisualizer(isPlaying = isRecording, barColor = Color(0xFFFF9F0A), modifier = Modifier.fillMaxWidth().height(26.dp))

                        // Tombol Mulai / Berhenti / Reset
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = {
                                    triggerHaptic(context)
                                    isRecording = !isRecording
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = if (isRecording) Color(0xFFFF453A) else Color(0xFF30D158)),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)
                            ) {
                                Text(if (isRecording) "⏹ Berhenti" else "● Mulai Rekam", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = {
                                    triggerHaptic(context)
                                    isRecording = false
                                    recordSeconds = 0
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3A3A3C)),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp)
                            ) {
                                Text("🔄 Reset", fontSize = 12.sp, color = Color.White)
                            }
                        }
                    }

                    IslandActiveFeature.ALARM -> {
                        // KONTEN ALARM & TIMER
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("⏰", fontSize = 18.sp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text("Timer Alarm", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    Text(if (isAlarmRunning) "Hitungan mundur aktif" else "Siap dimulai", color = Color.White.copy(0.7f), fontSize = 11.sp)
                                }
                            }
                            Text(
                                String.format("%02ds", alarmSeconds),
                                color = Color(0xFFFF9F0A),
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Pilihan Waktu Cepat
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            listOf(30 to "+30s", 60 to "+1m", 300 to "+5m").forEach { (sec, label) ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color.White.copy(0.15f))
                                        .clickable {
                                            triggerHaptic(context)
                                            alarmSeconds += sec
                                        }
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(label, color = Color.White, fontSize = 11.sp)
                                }
                            }
                        }

                        // Tombol Kontrol Alarm
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = {
                                    triggerHaptic(context)
                                    isAlarmRunning = !isAlarmRunning
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9F0A)),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)
                            ) {
                                Text(if (isAlarmRunning) "⏸ Jeda" else "▶ Mulai Timer", fontSize = 12.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = {
                                    triggerHaptic(context)
                                    isAlarmRunning = false
                                    alarmSeconds = 45
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3A3A3C)),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp)
                            ) {
                                Text("🔄 Reset", fontSize = 12.sp, color = Color.White)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// HARDWARE-ACCELERATED EQUALIZER & VISUALIZER
// ==========================================
@Composable
fun MiniEqualizer(
    isPlaying: Boolean,
    barColor: Color,
    modifier: Modifier = Modifier
) {
    if (!isPlaying) {
        Canvas(modifier = modifier) {
            val barW = 2.5.dp.toPx()
            val spacing = 2.dp.toPx()
            val staticRatios = listOf(0.4f, 0.7f, 0.5f, 0.3f)
            staticRatios.forEachIndexed { i, r ->
                val x = i * (barW + spacing)
                val h = size.height * r
                val y = (size.height - h) / 2
                drawRoundRect(
                    color = barColor.copy(alpha = 0.5f),
                    topLeft = Offset(x, y),
                    size = Size(barW, h),
                    cornerRadius = CornerRadius(barW / 2, barW / 2)
                )
            }
        }
        return
    }

    val transition = rememberInfiniteTransition(label = "miniEq")
    val b1 by transition.animateFloat(
        initialValue = 0.25f, targetValue = 0.95f,
        animationSpec = infiniteRepeatable(tween(260, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "1"
    )
    val b2 by transition.animateFloat(
        initialValue = 0.85f, targetValue = 0.30f,
        animationSpec = infiniteRepeatable(tween(340, easing = LinearOutSlowInEasing), RepeatMode.Reverse), label = "2"
    )
    val b3 by transition.animateFloat(
        initialValue = 0.35f, targetValue = 1.00f,
        animationSpec = infiniteRepeatable(tween(300, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "3"
    )
    val b4 by transition.animateFloat(
        initialValue = 0.90f, targetValue = 0.20f,
        animationSpec = infiniteRepeatable(tween(380, easing = LinearOutSlowInEasing), RepeatMode.Reverse), label = "4"
    )

    Canvas(modifier = modifier) {
        val barW = 2.5.dp.toPx()
        val spacing = 2.dp.toPx()
        val bars = listOf(b1, b2, b3, b4)
        bars.forEachIndexed { i, factor ->
            val x = i * (barW + spacing)
            val h = size.height * factor
            val y = (size.height - h) / 2
            drawRoundRect(
                color = barColor,
                topLeft = Offset(x, y),
                size = Size(barW, h),
                cornerRadius = CornerRadius(barW / 2, barW / 2)
            )
        }
    }
}

@Composable
fun ExpandedWaveformVisualizer(
    isPlaying: Boolean,
    barColor: Color,
    modifier: Modifier = Modifier
) {
    if (!isPlaying) {
        Canvas(modifier = modifier) {
            val barW = 3.dp.toPx()
            val spacing = 3.dp.toPx()
            val ratios = listOf(0.3f, 0.5f, 0.4f, 0.6f, 0.3f, 0.5f)
            ratios.forEachIndexed { i, r ->
                val x = i * (barW + spacing)
                val h = size.height * r
                val y = (size.height - h) / 2
                drawRoundRect(
                    color = barColor.copy(alpha = 0.4f),
                    topLeft = Offset(x, y),
                    size = Size(barW, h),
                    cornerRadius = CornerRadius(barW / 2, barW / 2)
                )
            }
        }
        return
    }

    val transition = rememberInfiniteTransition(label = "expWave")
    val w1 by transition.animateFloat(0.2f, 0.9f, infiniteRepeatable(tween(280), RepeatMode.Reverse), label = "")
    val w2 by transition.animateFloat(0.9f, 0.3f, infiniteRepeatable(tween(400), RepeatMode.Reverse), label = "")
    val w3 by transition.animateFloat(0.4f, 1.0f, infiniteRepeatable(tween(320), RepeatMode.Reverse), label = "")
    val w4 by transition.animateFloat(0.8f, 0.2f, infiniteRepeatable(tween(360), RepeatMode.Reverse), label = "")
    val w5 by transition.animateFloat(0.3f, 0.85f, infiniteRepeatable(tween(290), RepeatMode.Reverse), label = "")
    val w6 by transition.animateFloat(0.7f, 0.4f, infiniteRepeatable(tween(370), RepeatMode.Reverse), label = "")

    Canvas(modifier = modifier) {
        val barW = 3.dp.toPx()
        val spacing = 3.dp.toPx()
        val bars = listOf(w1, w2, w3, w4, w5, w6)
        bars.forEachIndexed { i, factor ->
            val x = i * (barW + spacing)
            val h = size.height * factor
            val y = (size.height - h) / 2
            drawRoundRect(
                color = barColor,
                topLeft = Offset(x, y),
                size = Size(barW, h),
                cornerRadius = CornerRadius(barW / 2, barW / 2)
            )
        }
    }
}

// ==========================================
// GRID APLIKASI STATIS TEROPTIMASI
// ==========================================
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
                        AppItemCell(
                            app = app,
                            modifier = Modifier.weight(1f),
                            onLaunchApp = onLaunchApp,
                            onLongClickApp = onLongClickApp
                        )
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppItemCell(
    app: AppModel,
    modifier: Modifier = Modifier,
    onLaunchApp: (String) -> Unit,
    onLongClickApp: (AppModel) -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.combinedClickable(
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
}

// ==========================================
// INDIKATOR HALAMAN (DOTS) ALA iOS
// ==========================================
@Composable
fun PageIndicator(
    currentPage: Int,
    totalPages: Int,
    onPageSelected: (Int) -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(7.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color.Black.copy(alpha = 0.22f))
            .padding(horizontal = 12.dp, vertical = 5.dp)
    ) {
        for (i in 0 until totalPages) {
            val isSelected = currentPage == i
            Box(
                modifier = Modifier
                    .size(if (i == 0) 8.dp else 7.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            isSelected -> Color.White
                            i == 0 -> Color(0xFF64D2FF).copy(alpha = 0.65f)
                            else -> Color.White.copy(alpha = 0.32f)
                        }
                    )
                    .clickable { onPageSelected(i) }
            )
        }
    }
}

// ==========================================
// GLASS DOCK BAWAH
// ==========================================
@Composable
fun GlassDock(
    dockApps: List<AppModel>,
    onLaunchApp: (String) -> Unit
) {
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
            dockApps.forEach { dockApp ->
                Box(
                    modifier = Modifier
                        .size(58.dp)
                        .clip(RoundedCornerShape(15.dp))
                        .background(Color.White.copy(alpha = 0.2f))
                        .clickable { onLaunchApp(dockApp.packageName) }
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

// ==========================================
// HALAMAN SPOTLIGHT SEARCH & APP LIBRARY
// ==========================================
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
                AppItemCell(
                    app = app,
                    onLaunchApp = onLaunchApp,
                    onLongClickApp = onLongClickApp
                )
            }
        }
    }
}

// ==========================================
// CONTEXT MENU (LONG PRESS APP)
// ==========================================
@Composable
fun AppContextMenu(
    app: AppModel,
    onDismiss: () -> Unit,
    onLaunch: () -> Unit
) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
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
                            onLaunch()
                            onDismiss()
                        }
                        .padding(vertical = 6.dp)
                )
                Text(
                    "⚙️ Info Aplikasi Sistem",
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.parse("package:${app.packageName}")
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(intent)
                            onDismiss()
                        }
                        .padding(vertical = 6.dp)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Tutup", color = Color(0xFF0A84FF))
            }
        }
    )
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