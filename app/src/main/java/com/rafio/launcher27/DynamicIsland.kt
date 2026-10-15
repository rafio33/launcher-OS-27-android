package com.rafio.launcher27

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.delay

// =========================================================================
// CONTAINER DYNAMIC ISLAND BERSIH (TANPA JAM / SINYAL DI DALAM / SAMPING BAR)
// Sesuai Instruksi: "dynamic bar ada jam sinyal juga jangan diikut sertakan bagian bar"
// =========================================================================
@Composable
fun CleanDynamicIslandContainer(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isExpanded = GlobalIslandState.isExpanded

    // Timer Putar Musik Real-Time
    LaunchedEffect(GlobalIslandState.isPlaying) {
        while (GlobalIslandState.isPlaying) {
            delay(1000)
            GlobalIslandState.trackPositionMs += 1000L
            if (GlobalIslandState.trackPositionMs > GlobalIslandState.currentSong.durationMs) {
                GlobalIslandState.trackPositionMs = 0L
                GlobalIslandState.playNext(context = null)
            }
        }
    }

    // Timer Detik Hitung Mundur (Mode Timer Image 1: 00:14)
    LaunchedEffect(GlobalIslandState.isTimerRunning, GlobalIslandState.currentMode) {
        while (GlobalIslandState.isTimerRunning && GlobalIslandState.currentMode == IslandMode.TIMER) {
            delay(1000)
            if (GlobalIslandState.timerSeconds > 0) {
                GlobalIslandState.timerSeconds--
            } else {
                GlobalIslandState.timerSeconds = 14
            }
        }
    }

    // Timer Rekam Video / Audio (Mode Rekaman Image 1: 00:05)
    LaunchedEffect(GlobalIslandState.isRecordingActive, GlobalIslandState.currentMode) {
        while (GlobalIslandState.currentMode == IslandMode.RECORDING) {
            delay(1000)
            GlobalIslandState.recordingSeconds++
        }
    }

    // Timer Panggilan (0:08)
    LaunchedEffect(GlobalIslandState.currentMode) {
        if (GlobalIslandState.currentMode == IslandMode.CALL && !GlobalIslandState.isIncomingCall) {
            while (true) {
                delay(1000)
                GlobalIslandState.callSeconds++
            }
        }
    }

    // Scrim penutup saat Dynamic Island expanded (tap luar untuk collapse)
    if (isExpanded) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .zIndex(40f)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    triggerHaptic(context)
                    GlobalIslandState.isExpanded = false
                }
        )
    }

    // Dynamic Island Bar Murni di Posisi Atas Layar (Tanpa Jam & Tanpa Sinyal)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(top = 8.dp)
            .zIndex(45f),
        contentAlignment = Alignment.TopCenter
    ) {
        InteractiveDynamicIslandBar()
    }
}

// =========================================================================
// INTERACTIVE DYNAMIC ISLAND BAR (COMPACT & EXPANDED)
// =========================================================================
@Composable
fun InteractiveDynamicIslandBar(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val mode = GlobalIslandState.currentMode
    val isExpanded = GlobalIslandState.isExpanded
    val isPlaying = GlobalIslandState.isPlaying
    val song = GlobalIslandState.currentSong

    // Pegas Fisika Animasi iOS Dynamic Island
    val springSpec = spring<Float>(
        dampingRatio = 0.78f,
        stiffness = Spring.StiffnessMediumLow
    )

    // Lebar Dynamic Island otomatis menyesuaikan mode (Sesuai Screenshot Image 1)
    val targetWidth = when {
        isExpanded -> 364f
        mode == IslandMode.MUSIC -> if (isPlaying) 296f else 260f
        mode == IslandMode.NAVIGATION -> 280f
        mode == IslandMode.TIMER -> 220f
        mode == IslandMode.RECORDING -> 200f
        mode == IslandMode.LIVE_ACTIVITY -> 295f
        mode == IslandMode.FACE_ID -> 270f
        mode == IslandMode.CHARGING -> 240f
        mode == IslandMode.TRANSLATE -> 310f
        mode == IslandMode.FOCUS -> 240f
        mode == IslandMode.CALL -> if (GlobalIslandState.isIncomingCall) 290f else 210f
        else -> 130f // Standby pill
    }
    val animatedWidth by animateFloatAsState(targetValue = targetWidth, animationSpec = springSpec, label = "islandW")

    val targetHeight = when {
        isExpanded -> 210f
        else -> 38f
    }
    val animatedHeight by animateFloatAsState(targetValue = targetHeight, animationSpec = springSpec, label = "islandH")

    val cornerRadius = if (isExpanded) 42.dp else 22.dp

    Box(
        modifier = modifier
            .width(animatedWidth.dp)
            .height(animatedHeight.dp)
            .clip(RoundedCornerShape(cornerRadius))
            .background(Color.Black)
            .border(
                0.8.dp,
                Color.White.copy(alpha = if (isExpanded) 0.16f else 0.22f),
                RoundedCornerShape(cornerRadius)
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                triggerHaptic(context)
                GlobalIslandState.isExpanded = !isExpanded
            },
        contentAlignment = Alignment.Center
    ) {
        if (!isExpanded) {
            // =============================================================
            // TAMPILAN COMPACT SESUAI SCREENSHOT IMAGE 1 & 2
            // =============================================================
            when (mode) {
                // 1. PUTAR MUSIK (Sweater Weather - The Neighbourhood + Waveform + Tombol Play/Stop)
                IslandMode.MUSIC -> {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Kiri: Album Art Mini
                        AlbumArtThumbnail(
                            song = song,
                            modifier = Modifier
                                .size(24.dp)
                                .clip(RoundedCornerShape(6.dp))
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        // Tengah: Judul Lagu & Artis
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = song.title,
                                color = Color.White,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = song.artist,
                                color = Color(0xFF9E9EA7),
                                fontSize = 9.5.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        // Kanan: Tombol Play/Stop Cepat Langsung di Dynamic Bar
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.12f))
                                .clickable {
                                    GlobalIslandState.togglePlayPause(context)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            PlayPauseMiniIcon(
                                isPlaying = isPlaying,
                                modifier = Modifier.size(11.dp),
                                tint = Color.White
                            )
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        // Waveform Equalizer Suara
                        LiveEqualizerWaveform(
                            isPlaying = isPlaying,
                            modifier = Modifier.size(20.dp, 13.dp),
                            barColor = Color(0xFF64D2FF)
                        )
                    }
                }

                // 2. NAVIGASI (200 m Turn right + Ikon Maps)
                IslandMode.NAVIGATION -> {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Kiri: Panah Arah Biru
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TurnArrowIcon(
                                modifier = Modifier.size(17.dp),
                                tint = Color(0xFF0A84FF)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = GlobalIslandState.navDistance,
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = GlobalIslandState.navInstruction,
                                    color = Color(0xFF8E8E93),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        // Kanan: Ikon Pin Peta
                        AppleMapsMiniIcon(modifier = Modifier.size(20.dp))
                    }
                }

                // 3. TIMER & STOPWATCH (Timer 00:14 + Tombol Pause/Play Oranye)
                IslandMode.TIMER -> {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Kiri: Ikon Stopwatch Oranye
                        StopwatchMiniIcon(
                            modifier = Modifier.size(18.dp),
                            tint = Color(0xFFFF9F0A)
                        )

                        // Tengah: Waktu Hitung Mundur 00:14
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "Timer",
                                color = Color(0xFFFF9F0A),
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = String.format("00:%02d", GlobalIslandState.timerSeconds),
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Kanan: Tombol Pause / Play Oranye yang bisa ditekan
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFF9F0A).copy(alpha = 0.25f))
                                .border(1.dp, Color(0xFFFF9F0A), CircleShape)
                                .clickable {
                                    GlobalIslandState.toggleTimer(context)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            PlayPauseMiniIcon(
                                isPlaying = GlobalIslandState.isTimerRunning,
                                modifier = Modifier.size(10.dp),
                                tint = Color(0xFFFF9F0A)
                            )
                        }
                    }
                }

                // 4. REKAM VIDEO / AUDIO (🔴 00:05 + Gelombang Suara)
                IslandMode.RECORDING -> {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Titik Merah Berkedip
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            PulsingRedDot(modifier = Modifier.size(9.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = String.format("00:%02d", GlobalIslandState.recordingSeconds),
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Gelombang Suara Perekam
                        LiveEqualizerWaveform(
                            isPlaying = true,
                            modifier = Modifier.size(24.dp, 14.dp),
                            barColor = Color(0xFFFF453A)
                        )
                    }
                }

                // 5. LIVE ACTIVITIES (GoFood: Pesanan dalam proses, 12 min)
                IslandMode.LIVE_ACTIVITY -> {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            GoFoodIconBadge(modifier = Modifier.size(22.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = GlobalIslandState.foodOrderTitle,
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = GlobalIslandState.foodOrderStatus,
                                    color = Color(0xFF8E8E93),
                                    fontSize = 9.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Text(
                            text = GlobalIslandState.foodOrderEta,
                            color = Color(0xFF64D2FF),
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // 6. FACE ID (Face ID: Look at your iPhone)
                IslandMode.FACE_ID -> {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 14.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FaceIdBracketIcon(
                            modifier = Modifier.size(20.dp),
                            tint = Color(0xFF0A84FF)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Face ID",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Look at your iPhone",
                                color = Color(0xFF8E8E93),
                                fontSize = 9.sp
                            )
                        }
                    }
                }

                // 7. PENGISIAN DAYA (⚡ Charging 78%)
                IslandMode.CHARGING -> {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("⚡", fontSize = 12.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Charging",
                                color = Color.White,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "${GlobalIslandState.chargingPct}%",
                                color = Color(0xFF30D158),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            BatteryPillIcon(
                                pct = GlobalIslandState.chargingPct,
                                isCharging = true,
                                modifier = Modifier.size(22.dp, 11.dp)
                            )
                        }
                    }
                }

                // 8. TERJEMAHAN LANGSUNG (Live Translate: English ↔ Indonesian)
                IslandMode.TRANSLATE -> {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LiveEqualizerWaveform(
                            isPlaying = true,
                            modifier = Modifier.size(18.dp, 12.dp),
                            barColor = Color(0xFF64D2FF)
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "Live Translate",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "English ↔ Indonesian",
                                color = Color(0xFF8E8E93),
                                fontSize = 9.sp
                            )
                        }

                        TranslateChatBubbleIcon(
                            modifier = Modifier.size(18.dp),
                            tint = Color(0xFF64D2FF)
                        )
                    }
                }

                // 9. FOCUS MODE (Focus: Do Not Disturb)
                IslandMode.FOCUS -> {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CrescentMoonIcon(
                                modifier = Modifier.size(15.dp),
                                tint = Color(0xFFBF5AF2)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Focus",
                                    color = Color(0xFF8E8E93),
                                    fontSize = 8.5.sp
                                )
                                Text(
                                    text = GlobalIslandState.focusName,
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        Text("•••", color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
                    }
                }

                // 10. CALL (Raka: Incoming call... atau 0:08)
                IslandMode.CALL -> {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            PhoneHandsetIcon(modifier = Modifier.size(14.dp), tint = Color(0xFF30D158))
                            Spacer(modifier = Modifier.width(7.dp))
                            Text(
                                text = if (GlobalIslandState.isIncomingCall) "Raka" else String.format("%d:%02d", GlobalIslandState.callSeconds / 60, GlobalIslandState.callSeconds % 60),
                                color = Color(0xFF30D158),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        if (GlobalIslandState.isIncomingCall) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFFF453A))
                                        .clickable {
                                            triggerHaptic(context)
                                            GlobalIslandState.isIncomingCall = false
                                            GlobalIslandState.onReturnToHomeScreen()
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("✕", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF30D158))
                                        .clickable {
                                            triggerHaptic(context)
                                            GlobalIslandState.isIncomingCall = false
                                            GlobalIslandState.callSeconds = 8
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    PhoneHandsetIcon(modifier = Modifier.size(12.dp), tint = Color.White)
                                }
                            }
                        } else {
                            CallEqualizerWaveform(modifier = Modifier.size(26.dp, 14.dp))
                        }
                    }
                }

                // 11. IDLE
                else -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF0F1424).copy(alpha = 0.5f))
                        )
                    }
                }
            }
        } else {
            // =============================================================
            // TAMPILAN EXPANDED (KARTU KONTROL LENGKAP SAAT DIKETUK)
            // =============================================================
            ExpandedIslandContent()
        }
    }
}

// =========================================================================
// KONTEN EXPANDED DYNAMIC ISLAND
// =========================================================================
@Composable
fun ExpandedIslandContent() {
    val context = LocalContext.current
    val mode = GlobalIslandState.currentMode
    val song = GlobalIslandState.currentSong
    val isPlaying = GlobalIslandState.isPlaying

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Notch Cutout Spacer
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .width(110.dp)
                    .height(24.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF0E0E12).copy(alpha = 0.6f))
            )
        }

        when (mode) {
            IslandMode.MUSIC -> {
                // Header Lagu
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AlbumArtThumbnail(
                        song = song,
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .border(0.6.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = song.title,
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${song.artist} — ${song.album}",
                            color = Color(0xFF8E8E93),
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    LiveEqualizerWaveform(
                        isPlaying = isPlaying,
                        modifier = Modifier.size(28.dp, 18.dp),
                        barColor = Color(0xFF64D2FF)
                    )
                }

                // Scrubber Timeline
                IosScrubber(
                    currentMs = GlobalIslandState.trackPositionMs,
                    totalMs = song.durationMs,
                    onSeek = { newPos -> GlobalIslandState.trackPositionMs = newPos }
                )

                // Tombol Kontrol Musik
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { GlobalIslandState.playPrevious(context) },
                        modifier = Modifier.size(38.dp)
                    ) {
                        RewindIcon(modifier = Modifier.size(22.dp))
                    }

                    IconButton(
                        onClick = { GlobalIslandState.togglePlayPause(context) },
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.15f))
                    ) {
                        PlayPauseIcon(isPlaying = isPlaying, modifier = Modifier.size(24.dp))
                    }

                    IconButton(
                        onClick = { GlobalIslandState.playNext(context) },
                        modifier = Modifier.size(38.dp)
                    ) {
                        FastForwardIcon(modifier = Modifier.size(22.dp))
                    }

                    AirPlayIcon(modifier = Modifier.size(20.dp), tint = Color(0xFF8E8E93))
                }
            }

            IslandMode.NAVIGATION -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TurnArrowIcon(modifier = Modifier.size(36.dp), tint = Color(0xFF0A84FF))
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Dalam ${GlobalIslandState.navDistance} ${GlobalIslandState.navInstruction}",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Menuju ${GlobalIslandState.navDestination} • Estimasi 14 mnt",
                            color = Color(0xFF8E8E93),
                            fontSize = 13.sp
                        )
                    }
                }
                TextButton(
                    onClick = {
                        triggerHaptic(context)
                        GlobalIslandState.onReturnToHomeScreen()
                        GlobalIslandState.isExpanded = false
                    },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Keluar Navigasi", color = Color(0xFFFF453A))
                }
            }

            IslandMode.TIMER -> {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = String.format("00:%02d", GlobalIslandState.timerSeconds),
                        color = Color.White,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text("Hitung Mundur Sisa Waktu", color = Color(0xFFFF9F0A), fontSize = 12.sp)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Button(
                        onClick = { GlobalIslandState.resetTimer(context) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.15f)),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Text("Reset", color = Color.White)
                    }
                    Button(
                        onClick = { GlobalIslandState.toggleTimer(context) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9F0A)),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Text(if (GlobalIslandState.isTimerRunning) "Jeda" else "Lanjutkan", color = Color.Black)
                    }
                }
            }

            IslandMode.LIVE_ACTIVITY -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    GoFoodIconBadge(modifier = Modifier.size(34.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("GoFood Delivery", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text(GlobalIslandState.foodOrderDesc, color = Color(0xFF8E8E93), fontSize = 12.sp)
                    }
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(Color.White.copy(alpha = 0.2f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(GlobalIslandState.foodProgress)
                            .fillMaxHeight()
                            .background(Color(0xFF30D158))
                    )
                }
                Text("Estimasi Tiba: ${GlobalIslandState.foodOrderEta}", color = Color(0xFF64D2FF), fontSize = 12.sp)
            }

            else -> {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Dynamic Island Aktif", color = Color.White, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Ketuk untuk kembali ke mode musik", color = Color(0xFF8E8E93), fontSize = 12.sp)
                    Button(
                        onClick = {
                            GlobalIslandState.currentMode = IslandMode.MUSIC
                            GlobalIslandState.isPlaying = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0A84FF)),
                        modifier = Modifier.padding(top = 10.dp)
                    ) {
                        Text("Buka Musik", color = Color.White)
                    }
                }
            }
        }

        // Quick Mode Switcher (Memungkinkan pengguna langsung menguji semua 9 mode di gambar!)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(Color.White.copy(alpha = 0.08f))
                .padding(vertical = 4.dp, horizontal = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Mode Uji:", color = Color(0xFF8E8E93), fontSize = 9.sp)
            listOf(
                "🎵" to IslandMode.MUSIC,
                "🧭" to IslandMode.NAVIGATION,
                "⏱" to IslandMode.TIMER,
                "🔴" to IslandMode.RECORDING,
                "🍔" to IslandMode.LIVE_ACTIVITY,
                "⚡" to IslandMode.CHARGING,
                "🗣" to IslandMode.TRANSLATE,
                "🌙" to IslandMode.FOCUS
            ).forEach { (icon, targetMode) ->
                Text(
                    text = icon,
                    fontSize = 11.sp,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(if (mode == targetMode) Color.White.copy(alpha = 0.25f) else Color.Transparent)
                        .clickable {
                            triggerHaptic(context)
                            GlobalIslandState.currentMode = targetMode
                        }
                        .padding(4.dp)
                )
            }
        }
    }
}

// =========================================================================
// GRAFIKA & IKON KUSTOM CANVAS (IMAGE 1 & IMAGE 2)
// =========================================================================

// Thumbnail Album Art Menggunakan Canvas Indah
@Composable
fun AlbumArtThumbnail(
    song: SongItem,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Background Gradien Kosmik
        drawRect(
            brush = Brush.linearGradient(
                colors = listOf(
                    Color(song.albumColorHex),
                    Color(0xFF0C101A)
                ),
                start = Offset(0f, 0f),
                end = Offset(w, h)
            )
        )

        // Bintang / Pola Artistik
        drawCircle(
            color = Color(0xFF64D2FF).copy(alpha = 0.4f),
            radius = w * 0.35f,
            center = Offset(w * 0.3f, h * 0.35f)
        )
        drawCircle(
            color = Color(0xFFD177E5).copy(alpha = 0.35f),
            radius = w * 0.4f,
            center = Offset(w * 0.75f, h * 0.7f)
        )

        // Ikon Not Balok Elegan di Tengah
        val noteColor = Color.White.copy(alpha = 0.9f)
        drawCircle(noteColor, radius = w * 0.12f, center = Offset(w * 0.4f, h * 0.65f))
        drawCircle(noteColor, radius = w * 0.12f, center = Offset(w * 0.65f, h * 0.55f))
        drawLine(
            color = noteColor,
            start = Offset(w * 0.5f, h * 0.65f),
            end = Offset(w * 0.5f, h * 0.25f),
            strokeWidth = 1.6.dp.toPx()
        )
        drawLine(
            color = noteColor,
            start = Offset(w * 0.75f, h * 0.55f),
            end = Offset(w * 0.75f, h * 0.20f),
            strokeWidth = 1.6.dp.toPx()
        )
        drawLine(
            color = noteColor,
            start = Offset(w * 0.5f, h * 0.25f),
            end = Offset(w * 0.75f, h * 0.20f),
            strokeWidth = 2.4.dp.toPx()
        )
    }
}

// Gelombang Equalizer Animasi Real-Time
@Composable
fun LiveEqualizerWaveform(
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    barColor: Color = Color(0xFF64D2FF)
) {
    if (!isPlaying) {
        Canvas(modifier = modifier) {
            val barW = 2.6.dp.toPx()
            val spacing = 2.2.dp.toPx()
            val staticRatios = listOf(0.35f, 0.65f, 0.40f, 0.75f)
            staticRatios.forEachIndexed { i, r ->
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

    val transition = rememberInfiniteTransition(label = "eqAnim")
    val b1 by transition.animateFloat(0.20f, 0.95f, infiniteRepeatable(tween(260, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "1")
    val b2 by transition.animateFloat(0.90f, 0.25f, infiniteRepeatable(tween(340, easing = LinearOutSlowInEasing), RepeatMode.Reverse), label = "2")
    val b3 by transition.animateFloat(0.35f, 1.00f, infiniteRepeatable(tween(280, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "3")
    val b4 by transition.animateFloat(0.85f, 0.20f, infiniteRepeatable(tween(360, easing = LinearOutSlowInEasing), RepeatMode.Reverse), label = "4")

    Canvas(modifier = modifier) {
        val barW = 2.6.dp.toPx()
        val spacing = 2.2.dp.toPx()
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

// Ikon Mini Play/Pause untuk Dynamic Bar
@Composable
fun PlayPauseMiniIcon(
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    tint: Color = Color.White
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        if (isPlaying) {
            val barW = 2.8.dp.toPx()
            val barH = h * 0.85f
            val gap = 3.dp.toPx()
            val startX = (w - (2 * barW + gap)) / 2
            val startY = (h - barH) / 2
            drawRoundRect(tint, Offset(startX, startY), Size(barW, barH), CornerRadius(1.dp.toPx(), 1.dp.toPx()))
            drawRoundRect(tint, Offset(startX + barW + gap, startY), Size(barW, barH), CornerRadius(1.dp.toPx(), 1.dp.toPx()))
        } else {
            val path = Path().apply {
                moveTo(w * 0.25f, h * 0.15f)
                lineTo(w * 0.85f, h * 0.50f)
                lineTo(w * 0.25f, h * 0.85f)
                close()
            }
            drawPath(path, color = tint)
        }
    }
}

// Panah Belok Navigasi (Image 1: 200 m Turn right)
@Composable
fun TurnArrowIcon(
    modifier: Modifier = Modifier,
    tint: Color = Color(0xFF0A84FF)
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val stroke = 3.dp.toPx()

        val stem = Path().apply {
            moveTo(w * 0.35f, h * 0.85f)
            lineTo(w * 0.35f, h * 0.45f)
            quadraticBezierTo(w * 0.35f, h * 0.25f, w * 0.58f, h * 0.25f)
            lineTo(w * 0.75f, h * 0.25f)
        }
        drawPath(stem, color = tint, style = Stroke(width = stroke, cap = StrokeCap.Round))

        val arrowHead = Path().apply {
            moveTo(w * 0.65f, h * 0.12f)
            lineTo(w * 0.85f, h * 0.25f)
            lineTo(w * 0.65f, h * 0.38f)
            close()
        }
        drawPath(arrowHead, color = tint)
    }
}

// Ikon Mini Apple Maps
@Composable
fun AppleMapsMiniIcon(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        drawRoundRect(
            brush = Brush.linearGradient(listOf(Color(0xFF34C759), Color(0xFF007AFF))),
            size = Size(w, h),
            cornerRadius = CornerRadius(5.dp.toPx(), 5.dp.toPx())
        )

        // Jalan raya putih & pin merah
        val road = Path().apply {
            moveTo(w * 0.2f, h)
            lineTo(w * 0.8f, 0f)
        }
        drawPath(road, color = Color.White.copy(alpha = 0.85f), style = Stroke(width = 2.dp.toPx()))

        drawCircle(Color(0xFFFF3B30), radius = 3.dp.toPx(), center = Offset(w * 0.5f, h * 0.5f))
    }
}

// Ikon Mini Stopwatch (Image 1: Timer 00:14)
@Composable
fun StopwatchMiniIcon(
    modifier: Modifier = Modifier,
    tint: Color = Color(0xFFFF9F0A)
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val stroke = 1.8.dp.toPx()

        drawCircle(
            color = tint,
            radius = w * 0.38f,
            center = Offset(w * 0.5f, h * 0.55f),
            style = Stroke(width = stroke)
        )

        // Tombol atas stopwatch
        drawLine(
            color = tint,
            start = Offset(w * 0.5f, h * 0.17f),
            end = Offset(w * 0.5f, h * 0.05f),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )

        // Jarum jam di dalam
        drawLine(
            color = tint,
            start = Offset(w * 0.5f, h * 0.55f),
            end = Offset(w * 0.68f, h * 0.42f),
            strokeWidth = stroke * 0.9f,
            cap = StrokeCap.Round
        )
    }
}

// Titik Merah Berkedip untuk Mode Rekam Video/Audio
@Composable
fun PulsingRedDot(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "pulse")
    val alpha by transition.animateFloat(
        initialValue = 1f,
        targetValue = 0.2f,
        animationSpec = infiniteRepeatable(tween(500, easing = LinearEasing), RepeatMode.Reverse),
        label = "dotAlpha"
    )

    Canvas(modifier = modifier) {
        drawCircle(Color(0xFFFF453A).copy(alpha = alpha), radius = size.minDimension / 2)
    }
}

// Badge GoFood (Image 1: GoFood Pesanan dalam proses)
@Composable
fun GoFoodIconBadge(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        drawCircle(Color(0xFFEE2737), radius = w / 2)

        // Garpu & Sendok Putih
        val stroke = 1.6.dp.toPx()
        // Garpu
        drawLine(Color.White, Offset(w * 0.38f, h * 0.3f), Offset(w * 0.38f, h * 0.72f), strokeWidth = stroke, cap = StrokeCap.Round)
        drawLine(Color.White, Offset(w * 0.32f, h * 0.3f), Offset(w * 0.44f, h * 0.3f), strokeWidth = stroke, cap = StrokeCap.Round)

        // Sendok
        drawOval(Color.White, topLeft = Offset(w * 0.56f, h * 0.26f), size = Size(w * 0.18f, h * 0.22f))
        drawLine(Color.White, Offset(w * 0.65f, h * 0.46f), Offset(w * 0.65f, h * 0.72f), strokeWidth = stroke, cap = StrokeCap.Round)
    }
}

// Ikon Face ID (Image 1: Face ID Look at your iPhone)
@Composable
fun FaceIdBracketIcon(
    modifier: Modifier = Modifier,
    tint: Color = Color(0xFF0A84FF)
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val stroke = 1.8.dp.toPx()
        val corner = 4.dp.toPx()

        // 4 Sudut Bracket
        // Kiri Atas
        val tl = Path().apply {
            moveTo(0f, corner * 2)
            lineTo(0f, corner)
            quadraticBezierTo(0f, 0f, corner, 0f)
            lineTo(corner * 2, 0f)
        }
        drawPath(tl, color = tint, style = Stroke(width = stroke, cap = StrokeCap.Round))

        // Kanan Atas
        val tr = Path().apply {
            moveTo(w - corner * 2, 0f)
            lineTo(w - corner, 0f)
            quadraticBezierTo(w, 0f, w, corner)
            lineTo(w, corner * 2)
        }
        drawPath(tr, color = tint, style = Stroke(width = stroke, cap = StrokeCap.Round))

        // Kiri Bawah
        val bl = Path().apply {
            moveTo(0f, h - corner * 2)
            lineTo(0f, h - corner)
            quadraticBezierTo(0f, h, corner, h)
            lineTo(corner * 2, h)
        }
        drawPath(bl, color = tint, style = Stroke(width = stroke, cap = StrokeCap.Round))

        // Kanan Bawah
        val br = Path().apply {
            moveTo(w - corner * 2, h)
            lineTo(w - corner, h)
            quadraticBezierTo(w, h, w, h - corner)
            lineTo(w, h - corner * 2)
        }
        drawPath(br, color = tint, style = Stroke(width = stroke, cap = StrokeCap.Round))

        // Mata & Senyum di Dalam
        drawCircle(tint, radius = 1.2.dp.toPx(), center = Offset(w * 0.36f, h * 0.42f))
        drawCircle(tint, radius = 1.2.dp.toPx(), center = Offset(w * 0.64f, h * 0.42f))

        val smile = Path().apply {
            moveTo(w * 0.35f, h * 0.64f)
            quadraticBezierTo(w * 0.50f, h * 0.74f, w * 0.65f, h * 0.64f)
        }
        drawPath(smile, color = tint, style = Stroke(width = stroke * 0.9f, cap = StrokeCap.Round))
    }
}

// Ikon Gelembung Terjemahan (Image 1: Live Translate)
@Composable
fun TranslateChatBubbleIcon(
    modifier: Modifier = Modifier,
    tint: Color = Color(0xFF64D2FF)
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        drawRoundRect(
            color = tint,
            topLeft = Offset(0f, 0f),
            size = Size(w * 0.85f, h * 0.75f),
            cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx()),
            style = Stroke(width = 1.5.dp.toPx())
        )

        // Garis panah terjemahan
        drawLine(tint, Offset(w * 0.2f, h * 0.37f), Offset(w * 0.65f, h * 0.37f), strokeWidth = 1.2.dp.toPx())
    }
}

// Ikon Bulan Sabit Focus Mode (Image 1: Focus Do Not Disturb)
@Composable
fun CrescentMoonIcon(
    modifier: Modifier = Modifier,
    tint: Color = Color(0xFFBF5AF2)
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        val moonPath = Path().apply {
            moveTo(w * 0.5f, 0f)
            cubicTo(w * 0.2f, 0f, 0f, h * 0.25f, 0f, h * 0.55f)
            cubicTo(0f, h * 0.85f, w * 0.25f, h, w * 0.65f, h)
            cubicTo(w * 0.35f, h * 0.85f, w * 0.35f, h * 0.25f, w * 0.5f, 0f)
            close()
        }
        drawPath(moonPath, color = tint)
    }
}
