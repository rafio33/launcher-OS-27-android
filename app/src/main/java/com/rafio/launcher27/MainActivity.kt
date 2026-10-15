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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
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

@Immutable
data class InstalledAppModel(
    val label: String,
    val packageName: String,
    val icon: ImageBitmap
)

fun drawableToBitmap(drawable: Drawable): ImageBitmap {
    val bmp = Bitmap.createBitmap(112, 112, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bmp)
    drawable.setBounds(0, 0, canvas.width, canvas.height)
    drawable.draw(canvas)
    return bmp.asImageBitmap()
}

fun triggerHaptic(context: Context?) {
    if (context == null) return
    try {
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        vibrator?.vibrate(VibrationEffect.createOneShot(25, VibrationEffect.DEFAULT_AMPLITUDE))
    } catch (_: Exception) {}
}

fun launchApp(context: Context, packageName: String) {
    val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName)
    if (launchIntent != null) {
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(launchIntent)
    }
}

class MainActivity : ComponentActivity() {
    private var backPressedCallback: (() -> Boolean)? = null

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
            override fun handleOnBackPressed() {
                val handled = backPressedCallback?.invoke() ?: false
                if (!handled) {
                    // Berada di layar utama, abaikan atau pertahankan di launcher
                }
            }
        })

        setContent {
            IOS27LauncherApp(
                registerBackCallback = { cb -> backPressedCallback = cb }
            )
        }
    }
}

// =========================================================================
// APLIKASI UTAMA iOS 27 SPATIAL LAUNCHER
// =========================================================================
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun IOS27LauncherApp(
    registerBackCallback: (() -> Boolean) -> Unit
) {
    val context = LocalContext.current
    var activeApp by remember { mutableStateOf<String?>(null) }
    var isQuickPanelOpen by remember { mutableStateOf(false) }
    var isLockScreenOpen by remember { mutableStateOf(false) }
    var isRecentAppsOpen by remember { mutableStateOf(false) }

    // Registrasi handler tombol kembali
    LaunchedEffect(activeApp, isQuickPanelOpen, isLockScreenOpen, isRecentAppsOpen) {
        registerBackCallback {
            when {
                isQuickPanelOpen -> {
                    isQuickPanelOpen = false
                    true
                }
                isLockScreenOpen -> {
                    isLockScreenOpen = false
                    true
                }
                isRecentAppsOpen -> {
                    isRecentAppsOpen = false
                    true
                }
                activeApp != null -> {
                    // Kembali ke Menu Awal:
                    // Sesuai instruksi: "jika kembali kemenu awal dan ketika play musik maka dynamic bar juga sedang play music dan bisa di play dan stop"
                    activeApp = null
                    GlobalIslandState.onReturnToHomeScreen()
                    true
                }
                else -> false
            }
        }
    }

    // Broadcast Receiver untuk Baterai Nyata
    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                if (intent == null) return
                val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                if (level >= 0 && scale > 0) {
                    GlobalIslandState.chargingPct = (level * 100) / scale
                }
                val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                val charging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
                if (charging && GlobalIslandState.currentMode != IslandMode.CHARGING) {
                    GlobalIslandState.currentMode = IslandMode.CHARGING
                }
            }
        }
        context.registerReceiver(receiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        onDispose { context.unregisterReceiver(receiver) }
    }

    // Spotify Receiver Resmi
    DisposableEffect(context) {
        val spotifyReceiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                if (intent == null) return
                when (intent.action) {
                    "com.spotify.music.metadatachanged" -> {
                        val track = intent.getStringExtra("track")
                        val artist = intent.getStringExtra("artist") ?: "Spotify"
                        val album = intent.getStringExtra("album") ?: ""
                        if (!track.isNullOrBlank()) {
                            // Sinkronkan lagu ke Dynamic Island
                            GlobalIslandState.isPlaying = true
                            GlobalIslandState.currentMode = IslandMode.MUSIC
                        }
                    }
                    "com.spotify.music.playbackstatechanged" -> {
                        val playing = intent.getBooleanExtra("playing", false)
                        GlobalIslandState.isPlaying = playing
                        if (playing) GlobalIslandState.currentMode = IslandMode.MUSIC
                    }
                }
            }
        }
        val filter = IntentFilter().apply {
            addAction("com.spotify.music.metadatachanged")
            addAction("com.spotify.music.playbackstatechanged")
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0F172A),
                        Color(0xFF1E293B),
                        Color(0xFF0A0F1D)
                    )
                )
            )
    ) {
        // LAYER 1: TAMPILAN HOME SCREEN ATAU APLIKASI AKTIF
        if (activeApp == null) {
            HomeScreenContent(
                onOpenApp = { appName ->
                    triggerHaptic(context)
                    activeApp = appName
                    // Reaksi Dynamic Island terhadap aplikasi yang dibuka
                    GlobalIslandState.onAppOpened(appName, context)
                },
                onOpenQuickPanel = {
                    triggerHaptic(context)
                    isQuickPanelOpen = true
                },
                onOpenLockScreen = {
                    triggerHaptic(context)
                    isLockScreenOpen = true
                },
                onOpenRecentApps = {
                    triggerHaptic(context)
                    isRecentAppsOpen = true
                }
            )
        } else {
            // Menampilkan Aplikasi Aktif (Image 2)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 46.dp) // Sisakan ruang untuk Dynamic Island di atas
            ) {
                when (activeApp) {
                    "Settings" -> SettingsScreen(onBack = {
                        activeApp = null
                        GlobalIslandState.onReturnToHomeScreen()
                    })
                    "Phone" -> PhoneScreen(onBack = {
                        activeApp = null
                        GlobalIslandState.onReturnToHomeScreen()
                    })
                    "Messages" -> MessagesScreen(onBack = {
                        activeApp = null
                        GlobalIslandState.onReturnToHomeScreen()
                    })
                    "Gallery" -> GalleryScreen(onBack = {
                        activeApp = null
                        GlobalIslandState.onReturnToHomeScreen()
                    })
                    "Camera" -> CameraScreen(onBack = {
                        activeApp = null
                        GlobalIslandState.onReturnToHomeScreen()
                    })
                    "Music" -> MusicScreen(onBack = {
                        activeApp = null
                        GlobalIslandState.onReturnToHomeScreen()
                    })
                    "Browser", "Chrome" -> BrowserScreen(onBack = {
                        activeApp = null
                        GlobalIslandState.onReturnToHomeScreen()
                    })
                    "Weather" -> WeatherScreen(onBack = {
                        activeApp = null
                        GlobalIslandState.onReturnToHomeScreen()
                    })
                    "Calendar" -> CalendarScreen(onBack = {
                        activeApp = null
                        GlobalIslandState.onReturnToHomeScreen()
                    })
                    "Files" -> FilesScreen(onBack = {
                        activeApp = null
                        GlobalIslandState.onReturnToHomeScreen()
                    })
                    "Notes" -> NotesScreen(onBack = {
                        activeApp = null
                        GlobalIslandState.onReturnToHomeScreen()
                    })
                    "Calculator" -> CalculatorScreen(onBack = {
                        activeApp = null
                        GlobalIslandState.onReturnToHomeScreen()
                    })
                    "AIAssistant", "Gemini" -> AIAssistantScreen(onBack = {
                        activeApp = null
                        GlobalIslandState.onReturnToHomeScreen()
                    })
                    else -> {
                        // Aplikasi eksternal Android jika ada
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Button(onClick = {
                                activeApp = null
                                GlobalIslandState.onReturnToHomeScreen()
                            }) {
                                Text("Kembali ke Home")
                            }
                        }
                    }
                }
            }
        }

        // LAYER 2: DYNAMIC ISLAND MURNI DI BAGIAN ATAS
        // Sesuai Instruksi User: "dynamic bar ada jam sinyal juga jangan diikut sertakan bagian bar"
        // Dynamic Island berdiri sendiri secara elegan di atas, tanpa jam atau sinyal yang mengapit bar!
        CleanDynamicIslandContainer()

        // LAYER 3: KARTU FLOATING SPESIAL SESUAI SCREENSHOT IMAGE 1
        FloatingLiveActivityCards(activeApp = activeApp)

        // LAYER 4: OVERLAY QUICK PANEL (CONTROL CENTER) SESUAI IMAGE 2
        QuickPanelOverlay(
            isOpen = isQuickPanelOpen,
            onDismiss = { isQuickPanelOpen = false },
            onOpenApp = { appName ->
                isQuickPanelOpen = false
                activeApp = appName
                GlobalIslandState.onAppOpened(appName, context)
            }
        )

        // LAYER 5: OVERLAY LOCK SCREEN SESUAI IMAGE 2
        LockScreenOverlay(
            isLocked = isLockScreenOpen,
            onUnlock = { isLockScreenOpen = false },
            onOpenApp = { appName ->
                isLockScreenOpen = false
                activeApp = appName
                GlobalIslandState.onAppOpened(appName, context)
            }
        )

        // LAYER 6: OVERLAY RECENT APPS SESUAI IMAGE 2
        RecentAppsOverlay(
            isOpen = isRecentAppsOpen,
            onDismiss = { isRecentAppsOpen = false },
            onSelectApp = { appName ->
                isRecentAppsOpen = false
                activeApp = appName
                GlobalIslandState.onAppOpened(appName, context)
            }
        )
    }
}

// =========================================================================
// KONTEN HOME SCREEN PERSIS SESUAI SCREENSHOT IMAGE 2
// Dynamic Bar di Atas, Jam 9:41 Besar di Bawahnya, 2 Widget Glass, Grid Ikon & Dock
// =========================================================================
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreenContent(
    onOpenApp: (String) -> Unit,
    onOpenQuickPanel: () -> Unit,
    onOpenLockScreen: () -> Unit,
    onOpenRecentApps: () -> Unit
) {
    val context = LocalContext.current
    var installedApps by remember { mutableStateOf<List<InstalledAppModel>>(emptyList()) }

    // Memuat aplikasi bawaan HP di background untuk halaman pencarian
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val pm = context.packageManager
            val intent = Intent(Intent.ACTION_MAIN, null).apply { addCategory(Intent.CATEGORY_LAUNCHER) }
            val resolveInfos = pm.queryIntentActivities(intent, 0)
            installedApps = resolveInfos.mapNotNull { info ->
                val pkg = info.activityInfo.packageName
                if (pkg == context.packageName) return@mapNotNull null
                InstalledAppModel(info.loadLabel(pm).toString(), pkg, drawableToBitmap(info.loadIcon(pm)))
            }.sortedBy { it.label.lowercase() }
        }
    }

    val pagerState = rememberPagerState(initialPage = 0, pageCount = { 2 })

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Ruang di bawah Dynamic Island
        Spacer(modifier = Modifier.height(52.dp))

        // Toolbar Cepat Panel Atas (Geser/Ketuk untuk Quick Panel, Lock Screen, Recents)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "🔒 Lock",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onOpenLockScreen() }
                    .padding(4.dp)
            )

            Text(
                "📑 Recents",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onOpenRecentApps() }
                    .padding(4.dp)
            )

            Text(
                "⚙️ Control",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onOpenQuickPanel() }
                    .padding(4.dp)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Tanggal ala iOS: "Monday, September 15" (Sesuai Image 2)
        Text(
            text = "Monday, September 15",
            color = Color.White.copy(alpha = 0.85f),
            fontSize = 14.5.sp,
            fontWeight = FontWeight.Medium
        )

        // Jam Besar: "9:41" (Sesuai Image 2)
        Text(
            text = "9:41",
            color = Color.White,
            fontSize = 72.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-1.5).sp,
            lineHeight = 76.sp
        )

        Spacer(modifier = Modifier.height(12.dp))

        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) { page ->
            if (page == 0) {
                // HALAMAN 1: 2 WIDGET GLASS + GRID 8 IKON PERSIS SCREENSHOT IMAGE 2
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // BARIS 2 WIDGET GLASS BESAR:
                    // Kiri: Jakarta 28° | Kanan: Today Team Sync / Lunch with Raka
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Widget Cuaca Jakarta 28°
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(26.dp))
                                .background(
                                    Brush.linearGradient(
                                        listOf(Color(0xFF1D5A8A).copy(alpha = 0.8f), Color(0xFF0F2B48).copy(alpha = 0.85f))
                                    )
                                )
                                .border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(26.dp))
                                .clickable { onOpenApp("Weather") }
                                .padding(14.dp)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("Jakarta", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                    Text("28°", color = Color.White, fontWeight = FontWeight.Light, fontSize = 38.sp)
                                }
                                Column {
                                    Text("Mostly Cloudy", color = Color.White.copy(alpha = 0.88f), fontSize = 11.5.sp)
                                    Text("H:32°  L:24°", color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
                                }
                            }
                        }

                        // Widget Kalender / Agenda "Today"
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(26.dp))
                                .background(Color.White.copy(alpha = 0.14f))
                                .border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(26.dp))
                                .clickable { onOpenApp("Calendar") }
                                .padding(14.dp)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Today", color = Color(0xFFFF9F0A), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text("Team Sync", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                                    Text("10:00 - 11:00", color = Color.White.copy(alpha = 0.7f), fontSize = 10.sp)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text("Lunch with Raka", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                                    Text("12:30 - 13:30", color = Color.White.copy(alpha = 0.7f), fontSize = 10.sp)
                                }
                            }
                        }
                    }

                    // GRID APLIKASI PERSIS SCREENSHOT IMAGE 2
                    // Baris 1: Gallery, Play Store, Maps, YouTube
                    // Baris 2: Gmail, Spotify, Instagram, WhatsApp
                    val homeApps = listOf(
                        listOf("Gallery" to "🖼️", "Play Store" to "▶️", "Maps" to "🧭", "YouTube" to "▶️"),
                        listOf("Gmail" to "✉️", "Spotify" to "🎵", "Instagram" to "📷", "WhatsApp" to "💬")
                    )

                    homeApps.forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            row.forEach { (name, fallback) ->
                                IosAppGridIcon(
                                    name = name,
                                    fallbackEmoji = fallback,
                                    onClick = { onOpenApp(name) }
                                )
                            }
                        }
                    }
                }
            } else {
                // HALAMAN 2: APLIKASI SISTEM iOS 27 LENGKAP (IMAGE 2 ROW 2 & ROW 3)
                // Settings, Phone, Messages, Camera, Music, Browser, Weather, Calendar, Files, Notes, Calculator, AI Assistant
                val allApps = listOf(
                    "Settings" to "⚙️", "Phone" to "📞", "Messages" to "💬", "Camera" to "📷",
                    "Music" to "🎵", "Browser" to "🌐", "Weather" to "⛅", "Calendar" to "📅",
                    "Files" to "📁", "Notes" to "📝", "Calculator" to "🧮", "Gemini" to "✨"
                )

                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(allApps) { (name, emoji) ->
                        IosAppGridIcon(
                            name = name,
                            fallbackEmoji = emoji,
                            onClick = { onOpenApp(name) }
                        )
                    }
                }
            }
        }

        // Indikator Titik Halaman
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(vertical = 8.dp)
        ) {
            repeat(2) { i ->
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(if (pagerState.currentPage == i) Color.White else Color.White.copy(alpha = 0.3f))
                )
            }
        }

        // GLASS DOCK BAWAH PERSIS SCREENSHOT IMAGE 2 (Phone, Messages, Chrome, Camera)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 6.dp)
                .height(84.dp)
                .clip(RoundedCornerShape(32.dp))
                .background(Color.White.copy(alpha = 0.16f))
                .border(1.dp, Color.White.copy(alpha = 0.28f), RoundedCornerShape(32.dp))
                .padding(horizontal = 20.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. Phone
                DockItemIcon(bgGradient = listOf(Color(0xFF34C759), Color(0xFF30D158)), emoji = "📞") {
                    onOpenApp("Phone")
                }
                // 2. Messages
                DockItemIcon(bgGradient = listOf(Color(0xFF34C759), Color(0xFF007AFF)), emoji = "💬") {
                    onOpenApp("Messages")
                }
                // 3. Browser
                DockItemIcon(bgGradient = listOf(Color(0xFF0A84FF), Color(0xFF5E5CE6)), emoji = "🌐") {
                    onOpenApp("Browser")
                }
                // 4. Camera
                DockItemIcon(bgGradient = listOf(Color(0xFF48484A), Color(0xFF1C1C1E)), emoji = "📷") {
                    onOpenApp("Camera")
                }
            }
        }
    }
}

@Composable
fun DockItemIcon(
    bgGradient: List<Color>,
    emoji: String,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    Box(
        modifier = Modifier
            .size(56.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.linearGradient(bgGradient))
            .border(0.8.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
            .clickable {
                triggerHaptic(context)
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {
        Text(emoji, fontSize = 24.sp)
    }
}

// Komponen Ikon Aplikasi Grid Bergaya iOS 27
@Composable
fun IosAppGridIcon(
    name: String,
    fallbackEmoji: String,
    onClick: () -> Unit
) {
    val context = LocalContext.current

    val bgGradient = when (name.lowercase()) {
        "gallery" -> listOf(Color(0xFFFF2D55), Color(0xFFFF9500), Color(0xFFFFCC00))
        "play store" -> listOf(Color(0xFF00C9FF), Color(0xFF92FE9D))
        "maps" -> listOf(Color(0xFF34C759), Color(0xFF007AFF))
        "youtube" -> listOf(Color(0xFFFF0000), Color(0xFFCC0000))
        "gmail" -> listOf(Color(0xFFEA4335), Color(0xFFB31412))
        "spotify", "music" -> listOf(Color(0xFF1DB954), Color(0xFF191414))
        "instagram" -> listOf(Color(0xFF833AB4), Color(0xFFFD1D1D), Color(0xFFFCB045))
        "whatsapp" -> listOf(Color(0xFF25D366), Color(0xFF128C7E))
        "camera" -> listOf(Color(0xFF636366), Color(0xFF2C2C2E))
        "settings" -> listOf(Color(0xFF8E8E93), Color(0xFF48484A))
        "phone" -> listOf(Color(0xFF34C759), Color(0xFF30D158))
        "messages" -> listOf(Color(0xFF30D158), Color(0xFF0A84FF))
        "browser", "chrome" -> listOf(Color(0xFF0A84FF), Color(0xFF5E5CE6))
        "weather" -> listOf(Color(0xFF5AC8FA), Color(0xFF007AFF))
        "calendar" -> listOf(Color(0xFFFF3B30), Color(0xFFFF453A))
        "files" -> listOf(Color(0xFF007AFF), Color(0xFF5856D6))
        "notes" -> listOf(Color(0xFFFFCC00), Color(0xFFFF9500))
        "calculator" -> listOf(Color(0xFFFF9F0A), Color(0xFFFF453A))
        "gemini", "aiassistant" -> listOf(Color(0xFF0A84FF), Color(0xFFBF5AF2))
        else -> listOf(Color(0xFF3A3A3C), Color(0xFF1C1C1E))
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(68.dp)
            .clickable {
                triggerHaptic(context)
                onClick()
            }
    ) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Brush.linearGradient(bgGradient))
                .border(0.8.dp, Color.White.copy(alpha = 0.32f), RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(fallbackEmoji, fontSize = 26.sp)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = name,
            color = Color.White,
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

// =========================================================================
// KARTU FLOATING SPESIAL SESUAI SCREENSHOT IMAGE 1
// (GoFood Live Activity, MagSafe Charging Ring, Live Translate Dialog)
// =========================================================================
@Composable
fun FloatingLiveActivityCards(
    activeApp: String?
) {
    if (activeApp != null) return
    val mode = GlobalIslandState.currentMode

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 80.dp),
        contentAlignment = Alignment.Center
    ) {
        when (mode) {
            // Live Activity GoFood (Sesuai Screenshot Image 1 Tengah Baris 2)
            IslandMode.LIVE_ACTIVITY -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color.Black.copy(alpha = 0.65f))
                        .border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(24.dp))
                        .padding(18.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                GoFoodIconBadge(modifier = Modifier.size(32.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text("GoFood", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    Text("Pesanan kamu sedang diproses di restoran.", color = Color(0xFF8E8E93), fontSize = 12.sp)
                                }
                            }
                            Text("12 min", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }

                        // Progress Slider
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(Color.White.copy(alpha = 0.2f))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.65f)
                                    .fillMaxHeight()
                                    .background(Color(0xFF30D158))
                            )
                        }
                    }
                }
            }

            // Pengisian Daya MagSafe (Sesuai Screenshot Image 1 Kiri Baris 3)
            IslandMode.CHARGING -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(160.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF30D158).copy(alpha = 0.15f))
                            .border(4.dp, Color(0xFF30D158), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("⚡", color = Color(0xFF30D158), fontSize = 54.sp)
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        "${GlobalIslandState.chargingPct}% Charged",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Live Translate Dialog (Sesuai Screenshot Image 1 Tengah Baris 3)
            IslandMode.TRANSLATE -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color.Black.copy(alpha = 0.7f))
                        .border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(24.dp))
                        .padding(18.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            GlobalIslandState.translateSource,
                            color = Color(0xFF8E8E93),
                            fontSize = 16.sp
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                GlobalIslandState.translateTarget,
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text("🔊", fontSize = 18.sp)
                        }
                    }
                }
            }

            else -> {}
        }
    }
}