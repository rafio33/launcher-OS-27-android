package com.rafio.launcher27

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Mode Dynamic Island sesuai referensi screenshot iOS:
// 1. MUSIC  -> Now Playing: Beach Bunny - Entropy (Phone 3 / Bawah)
// 2. CALL   -> Panggilan Berlangsung: 📞 0:08 + Waveform Suara (Phone 2 / Tengah)
// 3. IDLE   -> Notch Pill Hitam Kamera Standby (Phone 1 / Atas)
enum class IslandMode {
    MUSIC,
    CALL,
    IDLE
}

// ==========================================
// CONTAINER STATUS BAR + DYNAMIC ISLAND UTAMA
// ==========================================
@Composable
fun IosTopStatusBarContainer(
    batteryPct: Int,
    isCharging: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isExpanded by remember { mutableStateOf(false) }
    var currentMode by remember { mutableStateOf(IslandMode.MUSIC) }
    var timeString by remember { mutableStateOf("9:41") }

    // Update jam real-time HP atau default ke 9:41
    LaunchedEffect(Unit) {
        val sdf = SimpleDateFormat("H:mm", Locale.getDefault())
        while (true) {
            timeString = sdf.format(Date())
            delay(10000)
        }
    }

    // Scrim penutup luar saat expanded (Tap luar untuk menutup Dynamic Island)
    if (isExpanded) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .zIndex(24f)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    triggerHaptic(context)
                    isExpanded = false
                }
        )
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(top = 3.dp)
            .zIndex(26f),
        contentAlignment = Alignment.TopCenter
    ) {
        // LAPISAN 1: STATUS BAR SAMPING (Hanya tampil saat compact)
        AnimatedVisibility(
            visible = !isExpanded,
            enter = fadeIn(tween(180)),
            exit = fadeOut(tween(120))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 22.dp)
                    .height(36.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Jam Kiri ala iOS: 9:41
                Text(
                    text = timeString,
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = (-0.3).sp
                )

                // Ikon Kanan: Sinyal, Wi-Fi, Privacy Dot (jika CALL), dan Baterai
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (currentMode != IslandMode.CALL) {
                        CellularSignalIcon(modifier = Modifier.size(16.dp, 11.dp))
                        WifiIcon(modifier = Modifier.size(15.dp, 11.dp))
                    }

                    // Orange Microphone Privacy Indicator Dot (Persis di Screenshot Handphone Tengah saat CALL)
                    if (currentMode == IslandMode.CALL) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFF9F0A))
                        )
                    }

                    // Kapsul Baterai iOS
                    BatteryPillIcon(
                        pct = batteryPct,
                        isCharging = isCharging,
                        modifier = Modifier.size(24.dp, 11.5.dp)
                    )
                }
            }
        }

        // LAPISAN 2: DYNAMIC ISLAND INTERAKTIF
        DynamicIslandBar(
            batteryPct = batteryPct,
            isCharging = isCharging,
            isExpanded = isExpanded,
            onExpandedChange = { isExpanded = it },
            currentMode = currentMode,
            onModeChange = { currentMode = it }
        )
    }
}

// ==========================================
// DYNAMIC ISLAND BAR (COMPACT & EXPANDED)
// ==========================================
@Composable
fun DynamicIslandBar(
    batteryPct: Int,
    isCharging: Boolean,
    isExpanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    currentMode: IslandMode,
    onModeChange: (IslandMode) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showAirPlaySheet by remember { mutableStateOf(false) }

    // State Lagu Spotify / Media
    val isPlaying = SpotifyMediaState.isPlaying
    val trackTitle = SpotifyMediaState.trackTitle
    val trackArtist = SpotifyMediaState.trackArtist
    val durationMs = SpotifyMediaState.trackDurationMs

    // State Panggilan Aktif (Mulai dari 0:08 sesuai screenshot)
    var callSeconds by remember { mutableIntStateOf(8) }

    // State Banner Charger
    var showChargingBanner by remember { mutableStateOf(false) }

    // Timer Putar Musik
    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            delay(1000)
            SpotifyMediaState.trackPositionMs += 1000L
            if (SpotifyMediaState.trackPositionMs > SpotifyMediaState.trackDurationMs) {
                SpotifyMediaState.trackPositionMs = 0L
            }
        }
    }

    // Timer Panggilan Telepon (0:08, 0:09, 0:10...)
    LaunchedEffect(currentMode) {
        if (currentMode == IslandMode.CALL) {
            while (true) {
                delay(1000)
                callSeconds++
            }
        }
    }

    // Deteksi Charger
    LaunchedEffect(isCharging) {
        if (isCharging) {
            triggerHaptic(context)
            showChargingBanner = true
            delay(3500)
            showChargingBanner = false
        }
    }

    // Pegas Fisika Animasi iOS Dynamic Island
    val springSpec = spring<Float>(
        dampingRatio = 0.76f,
        stiffness = Spring.StiffnessMediumLow
    )

    val targetWidth = when {
        isExpanded -> 362f
        showChargingBanner -> 250f
        currentMode == IslandMode.CALL -> 220f
        currentMode == IslandMode.MUSIC -> if (isPlaying) 205f else 170f
        else -> 126f  // Idle notch pill
    }
    val animatedWidth by animateFloatAsState(targetValue = targetWidth, animationSpec = springSpec, label = "islandW")

    val targetHeight = when {
        isExpanded -> 188f
        showChargingBanner -> 38f
        else -> 36f
    }
    val animatedHeight by animateFloatAsState(targetValue = targetHeight, animationSpec = springSpec, label = "islandH")

    val cornerRadius = if (isExpanded) 42.dp else 20.dp

    Box(
        modifier = modifier
            .width(animatedWidth.dp)
            .height(animatedHeight.dp)
            .clip(RoundedCornerShape(cornerRadius))
            .background(Color.Black)
            .border(
                0.7.dp,
                Color.White.copy(alpha = if (isExpanded) 0.16f else 0.22f),
                RoundedCornerShape(cornerRadius)
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                triggerHaptic(context)
                onExpandedChange(!isExpanded)
            },
        contentAlignment = Alignment.Center
    ) {
        if (!isExpanded) {
            // ==========================================
            // MODE COMPACT (Kecil di Atas Kamera)
            // ==========================================
            when {
                showChargingBanner -> {
                    // Indikator Pengisian Baterai
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("⚡", fontSize = 13.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Mengisi Daya", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                        Text("$batteryPct%", color = Color(0xFF30D158), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }

                currentMode == IslandMode.CALL -> {
                    // Sesuai Screenshot Handphone Tengah: 📞 0:08 (Hijau) & Waveform Suara
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            PhoneHandsetIcon(
                                modifier = Modifier.size(15.dp),
                                tint = Color(0xFF30D158)
                            )
                            Spacer(modifier = Modifier.width(7.dp))
                            Text(
                                text = String.format("%d:%02d", callSeconds / 60, callSeconds % 60),
                                color = Color(0xFF30D158),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Kamera Cutout Spacer Tengah
                        Spacer(modifier = Modifier.width(30.dp))

                        // Waveform Multi-Warna Suara
                        CallEqualizerWaveform(
                            modifier = Modifier.size(28.dp, 16.dp)
                        )
                    }
                }

                currentMode == IslandMode.MUSIC -> {
                    // Musik Sedang Diputar
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Thumbnail Cover Beach Bunny Mini
                        BeachBunnyArtwork(
                            modifier = Modifier
                                .size(22.dp)
                                .clip(RoundedCornerShape(6.dp))
                        )

                        // Kamera Cutout Spacer Tengah
                        Spacer(modifier = Modifier.width(24.dp))

                        // Equalizer Ungu/Magenta
                        MusicEqualizerWaveform(
                            isPlaying = isPlaying,
                            modifier = Modifier.size(24.dp, 15.dp),
                            barColor = Color(0xFFD177E5)
                        )
                    }
                }

                else -> {
                    // Sesuai Screenshot Handphone Atas: IDLE PILL
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(11.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF0F1424).copy(alpha = 0.6f))
                        )
                    }
                }
            }
        } else {
            // ==========================================
            // MODE EXPANDED (Sesuai Screenshot Bawah)
            // ==========================================
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Notch Cutout Kamera Depan di Bagian Atas
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .width(115.dp)
                            .height(28.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFF0D0D11).copy(alpha = 0.5f))
                    )
                }

                // BARIS 1: Album Art + Judul Lagu & Artis + Equalizer Ungu
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Cover Album Beach Bunny "Entropy"
                    BeachBunnyArtwork(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(RoundedCornerShape(13.dp))
                            .border(0.6.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(13.dp))
                            .clickable { SpotifyMediaState.openSpotify(context) }
                    )

                    Spacer(modifier = Modifier.width(14.dp))

                    // Title: "Entropy" | Artist: "Beach Bunny"
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { SpotifyMediaState.openSpotify(context) }
                    ) {
                        Text(
                            text = trackTitle,
                            color = Color.White,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = trackArtist,
                            color = Color(0xFF8E8E93),
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.Normal,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Equalizer Magenta / Lilac di Kanan
                    MusicEqualizerWaveform(
                        isPlaying = isPlaying,
                        modifier = Modifier.size(30.dp, 20.dp),
                        barColor = Color(0xFFD177E5)
                    )
                }

                // BARIS 2: Timeline Scrubber (2:50 - bar putih interaktif - -0:51)
                IosScrubber(
                    currentMs = SpotifyMediaState.trackPositionMs,
                    totalMs = durationMs,
                    onSeek = { newPos -> SpotifyMediaState.trackPositionMs = newPos }
                )

                // BARIS 3: Tombol Kontrol Musik (Previous <<, Play/Pause, Next >>, AirPlay)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Previous Track (<<)
                        IconButton(
                            onClick = {
                                triggerHaptic(context)
                                SpotifyMediaState.previous(context)
                                SpotifyMediaState.trackPositionMs = 0L
                            },
                            modifier = Modifier.size(40.dp)
                        ) {
                            RewindIcon(modifier = Modifier.size(24.dp))
                        }

                        // Play / Pause (|| atau ▶)
                        IconButton(
                            onClick = {
                                triggerHaptic(context)
                                SpotifyMediaState.playPause(context)
                            },
                            modifier = Modifier.size(46.dp)
                        ) {
                            PlayPauseIcon(isPlaying = isPlaying, modifier = Modifier.size(28.dp))
                        }

                        // Next Track (>>)
                        IconButton(
                            onClick = {
                                triggerHaptic(context)
                                SpotifyMediaState.next(context)
                                SpotifyMediaState.trackPositionMs = 0L
                            },
                            modifier = Modifier.size(40.dp)
                        ) {
                            FastForwardIcon(modifier = Modifier.size(24.dp))
                        }
                    }

                    // Ikon AirPlay di Kanan Bawah
                    IconButton(
                        onClick = {
                            triggerHaptic(context)
                            showAirPlaySheet = true
                        },
                        modifier = Modifier.size(40.dp)
                    ) {
                        AirPlayIcon(modifier = Modifier.size(22.dp), tint = Color(0xFFEBEBF5))
                    }
                }

                // BARIS 4: Pengganti Mode Demo Cepat (Bisa Menguji 3 Tampilan Screenshot Sekaligus)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.08f))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "🎵 Musik",
                            fontSize = 10.sp,
                            color = if (currentMode == IslandMode.MUSIC) Color.White else Color(0xFF8E8E93),
                            fontWeight = if (currentMode == IslandMode.MUSIC) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier.clickable {
                                triggerHaptic(context)
                                onModeChange(IslandMode.MUSIC)
                            }
                        )
                        Text(
                            text = "•",
                            fontSize = 10.sp,
                            color = Color.White.copy(alpha = 0.3f)
                        )
                        Text(
                            text = "📞 Panggilan (0:08)",
                            fontSize = 10.sp,
                            color = if (currentMode == IslandMode.CALL) Color(0xFF30D158) else Color(0xFF8E8E93),
                            fontWeight = if (currentMode == IslandMode.CALL) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier.clickable {
                                triggerHaptic(context)
                                onModeChange(IslandMode.CALL)
                            }
                        )
                        Text(
                            text = "•",
                            fontSize = 10.sp,
                            color = Color.White.copy(alpha = 0.3f)
                        )
                        Text(
                            text = "⬛ Standby",
                            fontSize = 10.sp,
                            color = if (currentMode == IslandMode.IDLE) Color.White else Color(0xFF8E8E93),
                            fontWeight = if (currentMode == IslandMode.IDLE) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier.clickable {
                                triggerHaptic(context)
                                onModeChange(IslandMode.IDLE)
                                onExpandedChange(false)
                            }
                        )
                    }
                }
            }
        }
    }

    // Modal Output Audio AirPlay
    if (showAirPlaySheet) {
        AirPlayOutputSheet(onDismiss = { showAirPlaySheet = false })
    }
}

// ==========================================
// ARTWORK BEACH BUNNY "ENTROPY" (PERSIS DARI FOTO)
// ==========================================
@Composable
fun BeachBunnyArtwork(
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // 1. Background Galaksi Kosmik Ungu/Biru Tua
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFF2C1A4D),
                    Color(0xFF140F2E),
                    Color(0xFF080918)
                ),
                center = Offset(w * 0.5f, h * 0.4f),
                radius = w * 0.9f
            )
        )

        // 2. Bintang & Kilauan Cahaya di Angkasa
        val stars = listOf(
            Offset(w * 0.15f, h * 0.20f) to 1.4.dp.toPx(),
            Offset(w * 0.85f, h * 0.22f) to 1.8.dp.toPx(),
            Offset(w * 0.22f, h * 0.72f) to 1.0.dp.toPx(),
            Offset(w * 0.80f, h * 0.68f) to 1.4.dp.toPx(),
            Offset(w * 0.50f, h * 0.10f) to 1.5.dp.toPx(),
            Offset(w * 0.10f, h * 0.45f) to 1.0.dp.toPx(),
            Offset(w * 0.90f, h * 0.42f) to 1.2.dp.toPx()
        )
        stars.forEach { (pos, r) ->
            drawCircle(Color(0xFFE2D6FF).copy(alpha = 0.85f), radius = r, center = pos)
        }

        // Bintang 4-Sisi Kilau Cantik
        val starX = w * 0.80f
        val starY = h * 0.16f
        val starS = w * 0.07f
        val starPath = Path().apply {
            moveTo(starX, starY - starS)
            quadraticBezierTo(starX, starY, starX + starS, starY)
            quadraticBezierTo(starX, starY, starX, starY + starS)
            quadraticBezierTo(starX, starY, starX - starS, starY)
            quadraticBezierTo(starX, starY, starX, starY - starS)
            close()
        }
        drawPath(starPath, color = Color.White.copy(alpha = 0.9f))

        // 3. Badan & Baju Gadis
        val torsoPath = Path().apply {
            moveTo(w * 0.12f, h)
            cubicTo(w * 0.18f, h * 0.74f, w * 0.38f, h * 0.70f, w * 0.50f, h * 0.70f)
            cubicTo(w * 0.62f, h * 0.70f, w * 0.82f, h * 0.74f, w * 0.88f, h)
            close()
        }
        drawPath(torsoPath, color = Color(0xFF382362))

        // Leher
        drawRect(
            color = Color(0xFFFFD5C2),
            topLeft = Offset(w * 0.42f, h * 0.54f),
            size = Size(w * 0.16f, h * 0.20f)
        )

        // 4. Rambut Pirang Bob (Blonde Hair)
        drawCircle(
            color = Color(0xFFFCD36A),
            radius = w * 0.33f,
            center = Offset(w * 0.50f, h * 0.44f)
        )

        // 5. Wajah Cantik Oval
        drawOval(
            color = Color(0xFFFFDFC4),
            topLeft = Offset(w * 0.27f, h * 0.24f),
            size = Size(w * 0.46f, h * 0.44f)
        )

        // Blush Pipi Merah Muda Lembut
        drawCircle(Color(0xFFFF7E98).copy(alpha = 0.32f), radius = w * 0.065f, center = Offset(w * 0.35f, h * 0.51f))
        drawCircle(Color(0xFFFF7E98).copy(alpha = 0.32f), radius = w * 0.065f, center = Offset(w * 0.65f, h * 0.51f))

        // Mata Berbinar
        drawOval(Color(0xFF2B2240), topLeft = Offset(w * 0.35f, h * 0.42f), size = Size(w * 0.065f, h * 0.085f))
        drawOval(Color(0xFF2B2240), topLeft = Offset(w * 0.585f, h * 0.42f), size = Size(w * 0.065f, h * 0.085f))
        drawCircle(Color.White, radius = 1.3.dp.toPx(), center = Offset(w * 0.365f, h * 0.435f))
        drawCircle(Color.White, radius = 1.3.dp.toPx(), center = Offset(w * 0.600f, h * 0.435f))

        // Bibir Senyum Tipis
        val lips = Path().apply {
            moveTo(w * 0.45f, h * 0.57f)
            quadraticBezierTo(w * 0.50f, h * 0.60f, w * 0.55f, h * 0.57f)
        }
        drawPath(lips, color = Color(0xFFDE6D74), style = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round))

        // 6. Poni Pirang Lurus Rapi
        val bangs = Path().apply {
            moveTo(w * 0.24f, h * 0.33f)
            cubicTo(w * 0.30f, h * 0.17f, w * 0.70f, h * 0.17f, w * 0.76f, h * 0.33f)
            lineTo(w * 0.74f, h * 0.37f)
            cubicTo(w * 0.60f, h * 0.35f, w * 0.40f, h * 0.35f, w * 0.26f, h * 0.37f)
            close()
        }
        drawPath(bangs, color = Color(0xFFFFE082))

        // 7. Sarung Tangan Lilac / Ungu Menopang Dagu (Pose Beach Bunny)
        val leftGlove = Path().apply {
            moveTo(w * 0.18f, h * 0.88f)
            lineTo(w * 0.33f, h * 0.54f)
            cubicTo(w * 0.37f, h * 0.49f, w * 0.43f, h * 0.51f, w * 0.45f, h * 0.61f)
            lineTo(w * 0.37f, h * 0.88f)
            close()
        }
        drawPath(leftGlove, color = Color(0xFFB388FF))

        val rightGlove = Path().apply {
            moveTo(w * 0.82f, h * 0.88f)
            lineTo(w * 0.67f, h * 0.54f)
            cubicTo(w * 0.63f, h * 0.49f, w * 0.57f, h * 0.51f, w * 0.55f, h * 0.61f)
            lineTo(w * 0.63f, h * 0.88f)
            close()
        }
        drawPath(rightGlove, color = Color(0xFF9E6DE8))

        drawCircle(Color(0xFFD3B8FF), radius = 2.2.dp.toPx(), center = Offset(w * 0.38f, h * 0.57f))
        drawCircle(Color(0xFFD3B8FF), radius = 2.2.dp.toPx(), center = Offset(w * 0.62f, h * 0.57f))
    }
}

// ==========================================
// EQUALIZER UNGU/MAGENTA (MUSIK AKTIF)
// ==========================================
@Composable
fun MusicEqualizerWaveform(
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    barColor: Color = Color(0xFFD177E5)
) {
    if (!isPlaying) {
        Canvas(modifier = modifier) {
            val barW = 3.dp.toPx()
            val spacing = 2.5.dp.toPx()
            val staticHeights = listOf(0.35f, 0.70f, 0.45f, 0.85f, 0.40f)
            staticHeights.forEachIndexed { i, r ->
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

    val transition = rememberInfiniteTransition(label = "musicEq")
    val b1 by transition.animateFloat(0.20f, 0.95f, infiniteRepeatable(tween(260, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "1")
    val b2 by transition.animateFloat(0.90f, 0.30f, infiniteRepeatable(tween(340, easing = LinearOutSlowInEasing), RepeatMode.Reverse), label = "2")
    val b3 by transition.animateFloat(0.35f, 1.00f, infiniteRepeatable(tween(280, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "3")
    val b4 by transition.animateFloat(0.85f, 0.25f, infiniteRepeatable(tween(360, easing = LinearOutSlowInEasing), RepeatMode.Reverse), label = "4")
    val b5 by transition.animateFloat(0.40f, 0.90f, infiniteRepeatable(tween(310, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "5")

    Canvas(modifier = modifier) {
        val barW = 3.dp.toPx()
        val spacing = 2.5.dp.toPx()
        val bars = listOf(b1, b2, b3, b4, b5)
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
// EQUALIZER MULTI-WARNA PANGGILAN AKTIF
// ==========================================
@Composable
fun CallEqualizerWaveform(
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "callEq")
    val c1 by transition.animateFloat(0.35f, 0.95f, infiniteRepeatable(tween(250, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "c1")
    val c2 by transition.animateFloat(0.85f, 0.30f, infiniteRepeatable(tween(330, easing = LinearOutSlowInEasing), RepeatMode.Reverse), label = "c2")
    val c3 by transition.animateFloat(0.40f, 1.00f, infiniteRepeatable(tween(280, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "c3")
    val c4 by transition.animateFloat(0.95f, 0.20f, infiniteRepeatable(tween(350, easing = LinearOutSlowInEasing), RepeatMode.Reverse), label = "c4")
    val c5 by transition.animateFloat(0.50f, 0.90f, infiniteRepeatable(tween(300, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "c5")
    val c6 by transition.animateFloat(0.80f, 0.35f, infiniteRepeatable(tween(320, easing = LinearOutSlowInEasing), RepeatMode.Reverse), label = "c6")

    val barColors = listOf(
        Color(0xFF30D158),
        Color(0xFF30D158),
        Color(0xFFFFD60A),
        Color(0xFFFFD60A),
        Color(0xFFFF9F0A),
        Color(0xFFFF9F0A)
    )

    Canvas(modifier = modifier) {
        val barW = 2.5.dp.toPx()
        val spacing = 2.dp.toPx()
        val bars = listOf(c1, c2, c3, c4, c5, c6)
        bars.forEachIndexed { i, factor ->
            val x = i * (barW + spacing)
            val h = size.height * factor
            val y = (size.height - h) / 2
            drawRoundRect(
                color = barColors[i % barColors.size],
                topLeft = Offset(x, y),
                size = Size(barW, h),
                cornerRadius = CornerRadius(barW / 2, barW / 2)
            )
        }
    }
}

// ==========================================
// SCRUBBER & TIMELINE ALA iOS (2:50 - bar - -0:51)
// ==========================================
@Composable
fun IosScrubber(
    currentMs: Long,
    totalMs: Long,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val progress = (currentMs.toFloat() / totalMs.toFloat().coerceAtLeast(1f)).coerceIn(0f, 1f)
    var isDragging by remember { mutableStateOf(false) }
    var dragProgress by remember { mutableFloatStateOf(0f) }

    val activeRatio = if (isDragging) dragProgress else progress
    val effectiveMs = if (isDragging) (dragProgress * totalMs).toLong() else currentMs
    val currentSec = (effectiveMs / 1000L).coerceAtLeast(0L)
    val remainingSec = ((totalMs - effectiveMs) / 1000L).coerceAtLeast(0L)

    Column(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(18.dp)
                .pointerInput(totalMs) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        isDragging = true
                        dragProgress = (down.position.x / size.width.toFloat()).coerceIn(0f, 1f)

                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull() ?: break
                            if (change.pressed) {
                                dragProgress = (change.position.x / size.width.toFloat()).coerceIn(0f, 1f)
                                change.consume()
                            } else {
                                isDragging = false
                                onSeek((dragProgress * totalMs).toLong())
                                break
                            }
                        }
                    }
                },
            contentAlignment = Alignment.CenterStart
        ) {
            // Background Track Abu-Abu iOS
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.5.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0xFF38383A))
            )

            // Active Track Putih Penuh
            Box(
                modifier = Modifier
                    .fillMaxWidth(activeRatio)
                    .height(3.5.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.White)
            )

            // Scrubber Thumb Bulat Putih
            Box(
                modifier = Modifier
                    .fillMaxWidth(activeRatio)
                    .wrapContentWidth(Alignment.End)
            ) {
                Box(
                    modifier = Modifier
                        .size(if (isDragging) 9.dp else 5.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                )
            }
        }

        Spacer(modifier = Modifier.height(1.dp))

        // Label Menit & Detik: "2:50" di Kiri, "-0:51" di Kanan
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = String.format("%d:%02d", currentSec / 60, currentSec % 60),
                color = Color(0xFF8E8E93),
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = String.format("-%d:%02d", remainingSec / 60, remainingSec % 60),
                color = Color(0xFF8E8E93),
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

// ==========================================
// IKON-IKON VEKTOR CANVAS iOS RESMI
// ==========================================

// Ikon AirPlay Apple Resmi
@Composable
fun AirPlayIcon(
    modifier: Modifier = Modifier,
    tint: Color = Color.White
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Segitiga di Bawah
        val triangle = Path().apply {
            moveTo(w * 0.50f, h * 0.56f)
            lineTo(w * 0.72f, h * 0.88f)
            lineTo(w * 0.28f, h * 0.88f)
            close()
        }
        drawPath(triangle, color = tint)

        val strokeW = 1.8.dp.toPx()
        // Lengkungan Busur Dalam
        drawArc(
            color = tint,
            startAngle = 210f,
            sweepAngle = 120f,
            useCenter = false,
            topLeft = Offset(w * 0.24f, h * 0.24f),
            size = Size(w * 0.52f, h * 0.52f),
            style = Stroke(width = strokeW, cap = StrokeCap.Round)
        )

        // Lengkungan Busur Luar
        drawArc(
            color = tint,
            startAngle = 215f,
            sweepAngle = 110f,
            useCenter = false,
            topLeft = Offset(w * 0.08f, h * 0.08f),
            size = Size(w * 0.84f, h * 0.84f),
            style = Stroke(width = strokeW, cap = StrokeCap.Round)
        )
    }
}

// Ikon Rewind (<<)
@Composable
fun RewindIcon(
    modifier: Modifier = Modifier,
    tint: Color = Color.White
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val p1 = Path().apply {
            moveTo(w * 0.46f, h * 0.20f)
            lineTo(w * 0.06f, h * 0.50f)
            lineTo(w * 0.46f, h * 0.80f)
            close()
        }
        val p2 = Path().apply {
            moveTo(w * 0.94f, h * 0.20f)
            lineTo(w * 0.54f, h * 0.50f)
            lineTo(w * 0.94f, h * 0.80f)
            close()
        }
        drawPath(p1, color = tint)
        drawPath(p2, color = tint)
    }
}

// Ikon Fast Forward (>>)
@Composable
fun FastForwardIcon(
    modifier: Modifier = Modifier,
    tint: Color = Color.White
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val p1 = Path().apply {
            moveTo(w * 0.06f, h * 0.20f)
            lineTo(w * 0.46f, h * 0.50f)
            lineTo(w * 0.06f, h * 0.80f)
            close()
        }
        val p2 = Path().apply {
            moveTo(w * 0.54f, h * 0.20f)
            lineTo(w * 0.94f, h * 0.50f)
            lineTo(w * 0.54f, h * 0.80f)
            close()
        }
        drawPath(p1, color = tint)
        drawPath(p2, color = tint)
    }
}

// Ikon Play / Pause (|| atau ▶)
@Composable
fun PlayPauseIcon(
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    tint: Color = Color.White
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        if (isPlaying) {
            // Dua Batang Pause Tegak
            val barW = 4.2.dp.toPx()
            val barH = h * 0.72f
            val gap = 6.dp.toPx()
            val startX = (w - (2 * barW + gap)) / 2
            val startY = (h - barH) / 2
            drawRoundRect(
                color = tint,
                topLeft = Offset(startX, startY),
                size = Size(barW, barH),
                cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
            )
            drawRoundRect(
                color = tint,
                topLeft = Offset(startX + barW + gap, startY),
                size = Size(barW, barH),
                cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
            )
        } else {
            // Segitiga Play
            val path = Path().apply {
                moveTo(w * 0.28f, h * 0.18f)
                lineTo(w * 0.82f, h * 0.50f)
                lineTo(w * 0.28f, h * 0.82f)
                close()
            }
            drawPath(path, color = tint)
        }
    }
}

// Ikon Telepon Hijau Handset
@Composable
fun PhoneHandsetIcon(
    modifier: Modifier = Modifier,
    tint: Color = Color(0xFF30D158)
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val path = Path().apply {
            moveTo(w * 0.22f, h * 0.35f)
            cubicTo(w * 0.22f, h * 0.25f, w * 0.35f, h * 0.15f, w * 0.45f, h * 0.22f)
            lineTo(w * 0.52f, h * 0.38f)
            cubicTo(w * 0.55f, h * 0.44f, w * 0.50f, h * 0.50f, w * 0.44f, h * 0.54f)
            cubicTo(w * 0.50f, h * 0.65f, w * 0.60f, h * 0.75f, w * 0.70f, h * 0.80f)
            cubicTo(w * 0.74f, h * 0.74f, w * 0.80f, h * 0.70f, w * 0.86f, h * 0.72f)
            lineTo(w * 0.98f, h * 0.82f)
            cubicTo(w * 1.05f, h * 0.90f, w * 0.95f, h * 1.05f, w * 0.85f, h * 1.05f)
            cubicTo(w * 0.50f, h * 1.05f, w * 0.15f, h * 0.70f, w * 0.15f, h * 0.42f)
            close()
        }
        drawPath(path, color = tint)
    }
}

// Ikon Sinyal Seluler 4 Bar Tangga
@Composable
fun CellularSignalIcon(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val barW = 2.4.dp.toPx()
        val spacing = 1.8.dp.toPx()
        val ratios = listOf(0.35f, 0.58f, 0.80f, 1.0f)
        ratios.forEachIndexed { i, r ->
            val barH = h * r
            val x = i * (barW + spacing)
            val y = h - barH
            drawRoundRect(
                color = Color.White,
                topLeft = Offset(x, y),
                size = Size(barW, barH),
                cornerRadius = CornerRadius(0.8.dp.toPx(), 0.8.dp.toPx())
            )
        }
    }
}

// Ikon Wi-Fi 3 Busur Konsentris
@Composable
fun WifiIcon(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val stroke = 1.5.dp.toPx()

        drawArc(
            color = Color.White,
            startAngle = 225f,
            sweepAngle = 90f,
            useCenter = false,
            topLeft = Offset(w * 0.05f, h * 0.05f),
            size = Size(w * 0.90f, h * 0.90f),
            style = Stroke(width = stroke, cap = StrokeCap.Round)
        )
        drawArc(
            color = Color.White,
            startAngle = 225f,
            sweepAngle = 90f,
            useCenter = false,
            topLeft = Offset(w * 0.25f, h * 0.35f),
            size = Size(w * 0.50f, h * 0.50f),
            style = Stroke(width = stroke, cap = StrokeCap.Round)
        )
        drawCircle(
            color = Color.White,
            radius = 1.4.dp.toPx(),
            center = Offset(w * 0.50f, h * 0.88f)
        )
    }
}

// Ikon Baterai Kapsul iOS
@Composable
fun BatteryPillIcon(
    pct: Int,
    isCharging: Boolean,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val bodyW = w - 2.5.dp.toPx()

        // Kerangka luar baterai
        drawRoundRect(
            color = Color.White.copy(alpha = 0.40f),
            topLeft = Offset(0f, 0f),
            size = Size(bodyW, h),
            cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx()),
            style = Stroke(width = 1.2.dp.toPx())
        )

        // Terminal kutub positif baterai di kanan
        val nubW = 1.8.dp.toPx()
        val nubH = h * 0.40f
        drawRoundRect(
            color = Color.White.copy(alpha = 0.40f),
            topLeft = Offset(bodyW + 0.8.dp.toPx(), (h - nubH) / 2),
            size = Size(nubW, nubH),
            cornerRadius = CornerRadius(1.dp.toPx(), 1.dp.toPx())
        )

        // Isi Baterai (Fill)
        val fillPadding = 2.dp.toPx()
        val maxFillW = bodyW - (fillPadding * 2)
        val fillW = (maxFillW * (pct.coerceIn(5, 100) / 100f)).coerceAtLeast(2.dp.toPx())
        val fillColor = when {
            isCharging -> Color(0xFF30D158)
            pct <= 20 -> Color(0xFFFF453A)
            else -> Color.White
        }

        drawRoundRect(
            color = fillColor,
            topLeft = Offset(fillPadding, fillPadding),
            size = Size(fillW, h - (fillPadding * 2)),
            cornerRadius = CornerRadius(1.5.dp.toPx(), 1.5.dp.toPx())
        )
    }
}

// ==========================================
// MODAL AIRPLAY OUTPUT SELECTOR
// ==========================================
@Composable
fun AirPlayOutputSheet(onDismiss: () -> Unit) {
    val context = LocalContext.current
    var selectedDevice by remember { mutableStateOf("iPhone Speaker") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1C1C1E).copy(alpha = 0.96f),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AirPlayIcon(modifier = Modifier.size(20.dp), tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Output Audio", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 17.sp)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                listOf(
                    "iPhone Speaker (Bawaan)",
                    "AirPods Pro (Spatial Audio)",
                    "Perangkat Bluetooth Lain"
                ).forEach { device ->
                    val isSelected = selectedDevice.startsWith(device.take(6))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) Color.White.copy(alpha = 0.15f) else Color.Transparent)
                            .clickable {
                                triggerHaptic(context)
                                selectedDevice = device
                            }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(device, color = Color.White, fontSize = 14.sp)
                        if (isSelected) {
                            Text("✓", color = Color(0xFF0A84FF), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Selesai", color = Color(0xFF0A84FF), fontWeight = FontWeight.SemiBold)
            }
        }
    )
}
