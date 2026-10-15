package com.rafio.launcher27

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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

// =========================================================================
// CONTAINER HALAMAN APLIKASI LENGKAP (IMAGE 2)
// Mendukung tombol kembali ke Home Screen, dan Dynamic Bar tetap aktif di atas!
// =========================================================================

@Composable
fun AppHeaderBar(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.clickable { onBack() }
        ) {
            Text("‹", color = Color(0xFF0A84FF), fontSize = 32.sp, fontWeight = FontWeight.Light)
            Spacer(modifier = Modifier.width(4.dp))
            Text("Kembali", color = Color(0xFF0A84FF), fontSize = 16.sp)
        }
        Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 17.sp)
        Spacer(modifier = Modifier.width(50.dp))
    }
}

// -------------------------------------------------------------------------
// 1. SETTINGS APP SCREEN (Image 2)
// -------------------------------------------------------------------------
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF000000))
    ) {
        AppHeaderBar(title = "Settings", onBack = onBack)

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                // Search Box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF1C1C1E))
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text("🔍 Search settings", color = Color(0xFF8E8E93), fontSize = 14.sp)
                }
            }

            // User Profile Card
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF1C1C1E))
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(listOf(Color(0xFF0A84FF), Color(0xFFBF5AF2)))),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("R", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text("Rafio", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                        Text("Apple Account, iCloud+ and more", color = Color(0xFF8E8E93), fontSize = 12.5.sp)
                    }
                }
            }

            // Group 1
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF1C1C1E))
                ) {
                    SettingRowItem("📡", "Connections", "Wi-Fi • Bluetooth • SIM manager")
                    Divider(color = Color.White.copy(alpha = 0.08f), thickness = 0.5.dp, modifier = Modifier.padding(start = 50.dp))
                    SettingRowItem("📱", "Connected devices", "Quick Share • Android Auto")
                }
            }

            // Group 2
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF1C1C1E))
                ) {
                    SettingRowItem("🔄", "Modes and Routines", "Modes • Routines")
                    Divider(color = Color.White.copy(alpha = 0.08f), thickness = 0.5.dp, modifier = Modifier.padding(start = 50.dp))
                    SettingRowItem("🔊", "Sounds and vibration", "Sound mode • Ringtone")
                    Divider(color = Color.White.copy(alpha = 0.08f), thickness = 0.5.dp, modifier = Modifier.padding(start = 50.dp))
                    SettingRowItem("🔔", "Notifications", "Status bar • Do not disturb")
                }
            }

            // Group 3
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF1C1C1E))
                ) {
                    SettingRowItem("☀️", "Display", "Brightness • Eye comfort shield")
                    Divider(color = Color.White.copy(alpha = 0.08f), thickness = 0.5.dp, modifier = Modifier.padding(start = 50.dp))
                    SettingRowItem("🎨", "Wallpaper and style", "Wallpapers • Color palette")
                    Divider(color = Color.White.copy(alpha = 0.08f), thickness = 0.5.dp, modifier = Modifier.padding(start = 50.dp))
                    SettingRowItem("✨", "Themes", "Themes • Wallpapers • Icons")
                    Divider(color = Color.White.copy(alpha = 0.08f), thickness = 0.5.dp, modifier = Modifier.padding(start = 50.dp))
                    SettingRowItem("🏠", "Home screen", "Layout • App icon badges")
                }
            }
            item { Spacer(modifier = Modifier.height(20.dp)) }
        }
    }
}

@Composable
fun SettingRowItem(icon: String, title: String, subtitle: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(icon, fontSize = 20.sp)
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = Color(0xFF8E8E93), fontSize = 11.5.sp)
        }
        Text("›", color = Color(0xFF8E8E93), fontSize = 20.sp)
    }
}

// -------------------------------------------------------------------------
// 2. PHONE / DIALER SCREEN (Image 2)
// -------------------------------------------------------------------------
@Composable
fun PhoneScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var dialedNumber by remember { mutableStateOf("") }
    var selectedTab by remember { mutableStateOf("Keypad") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF000000))
    ) {
        AppHeaderBar(title = "Phone", onBack = onBack)

        // Top Tabs
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 40.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            listOf("Keypad", "Recents", "Contacts").forEach { tab ->
                val isSelected = selectedTab == tab
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable { selectedTab = tab }
                ) {
                    Text(
                        tab,
                        color = if (isSelected) Color.White else Color(0xFF8E8E93),
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 15.sp
                    )
                    if (isSelected) {
                        Box(
                            modifier = Modifier
                                .padding(top = 4.dp)
                                .width(24.dp)
                                .height(2.dp)
                                .background(Color(0xFF0A84FF))
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Display Angka yang diketik
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .padding(horizontal = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = dialedNumber.ifEmpty { " " },
                color = Color.White,
                fontSize = 32.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        // Keypad Grid 1-9, *, 0, #
        val keys = listOf(
            listOf("1" to "", "2" to "A B C", "3" to "D E F"),
            listOf("4" to "G H I", "5" to "J K L", "6" to "M N O"),
            listOf("7" to "P Q R S", "8" to "T U V", "9" to "W X Y Z"),
            listOf("*" to "", "0" to "+", "#" to "")
        )

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            keys.forEach { row ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    row.forEach { (num, letters) ->
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF1C1C1E))
                                .clickable {
                                    triggerHaptic(context)
                                    dialedNumber += num
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(num, color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Normal)
                                if (letters.isNotEmpty()) {
                                    Text(letters, color = Color(0xFF8E8E93), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            // Tombol Panggil Hijau
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(modifier = Modifier.size(72.dp))
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF30D158))
                        .clickable {
                            triggerHaptic(context)
                            // Memulai panggilan simulated ke Raka -> memicu Dynamic Island!
                            GlobalIslandState.callerName = if (dialedNumber.isNotBlank()) dialedNumber else "Raka"
                            GlobalIslandState.currentMode = IslandMode.CALL
                            GlobalIslandState.isIncomingCall = false
                            GlobalIslandState.callSeconds = 0
                            onBack()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    PhoneHandsetIcon(modifier = Modifier.size(32.dp), tint = Color.White)
                }

                // Tombol Hapus Angka
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clickable {
                            if (dialedNumber.isNotEmpty()) {
                                triggerHaptic(context)
                                dialedNumber = dialedNumber.dropLast(1)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (dialedNumber.isNotEmpty()) {
                        Text("⌫", color = Color(0xFF8E8E93), fontSize = 24.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}

// -------------------------------------------------------------------------
// 3. MESSAGES SCREEN (Image 2)
// -------------------------------------------------------------------------
@Composable
fun MessagesScreen(onBack: () -> Unit) {
    val messages = listOf(
        Triple("Raka", "See you tomorrow!", "09:21"),
        Triple("Dinda", "Oke, makasih yaa!", "08:45"),
        Triple("Budi", "Meeting jam 11 ya?", "07:30"),
        Triple("Mama", "Jangan lupa makan", "Yesterday"),
        Triple("Rina", "Foto nya keren!", "Yesterday"),
        Triple("Rizky", "Udah sampai?", "Yesterday")
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF000000))
    ) {
        AppHeaderBar(title = "Messages", onBack = onBack)

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF1C1C1E))
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text("🔍 Search messages", color = Color(0xFF8E8E93), fontSize = 14.sp)
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            items(messages) { (sender, snippet, time) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { }
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(listOf(Color(0xFF32D74B), Color(0xFF0A84FF)))),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(sender.take(1), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(sender, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text(time, color = Color(0xFF8E8E93), fontSize = 12.sp)
                        }
                        Text(snippet, color = Color(0xFF8E8E93), fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                Divider(color = Color.White.copy(alpha = 0.08f), thickness = 0.5.dp, modifier = Modifier.padding(start = 60.dp))
            }
        }
    }
}

// -------------------------------------------------------------------------
// 4. GALLERY SCREEN (Image 2)
// -------------------------------------------------------------------------
@Composable
fun GalleryScreen(onBack: () -> Unit) {
    var selectedTab by remember { mutableStateOf("Photos") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF000000))
    ) {
        AppHeaderBar(title = "Gallery", onBack = onBack)

        // Tabs
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            listOf("Photos", "Albums", "Stories").forEach { tab ->
                val isSelected = selectedTab == tab
                Text(
                    tab,
                    color = if (isSelected) Color.White else Color(0xFF8E8E93),
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier.clickable { selectedTab = tab },
                    fontSize = 15.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Grid Foto Estetik Canvas
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(3) { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    for (col in 0..1) {
                        val colorSeed = (row * 2 + col)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(160.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    Brush.linearGradient(
                                        when (colorSeed % 4) {
                                            0 -> listOf(Color(0xFF2C3E50), Color(0xFFFD746C))
                                            1 -> listOf(Color(0xFF141E30), Color(0xFF243B55))
                                            2 -> listOf(Color(0xFF0F2027), Color(0xFF203A43), Color(0xFF2C5364))
                                            else -> listOf(Color(0xFF4B1248), Color(0xFFF0C27B))
                                        }
                                    )
                                )
                                .border(0.6.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(16.dp)),
                            contentAlignment = Alignment.BottomStart
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color.Black.copy(alpha = 0.35f))
                                    .padding(8.dp)
                            ) {
                                Text("Sept 2025 • Wallpaper #${colorSeed + 1}", color = Color.White, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------------------
// 5. CAMERA SCREEN (Image 2 - Memicu Mode Rekaman di Dynamic Island)
// -------------------------------------------------------------------------
@Composable
fun CameraScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var selectedMode by remember { mutableStateOf("Photo") }
    var isRecording by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF000000))
    ) {
        AppHeaderBar(title = "Camera", onBack = onBack)

        // Top Toolbar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("⚡", color = Color.White, fontSize = 16.sp)
            Text("HDR", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Text("16:9", color = Color.White, fontSize = 13.sp)
            Text("⚙️", color = Color.White, fontSize = 16.sp)
        }

        // Viewfinder Simulasi Sunset View
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 14.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0xFF0D1B2A),
                            Color(0xFF1B263B),
                            Color(0xFF415A77),
                            Color(0xFFE0A96D)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            // Bingkai Fokus Kamera
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .border(1.dp, Color(0xFFFFD60A), RoundedCornerShape(6.dp))
            )

            if (isRecording) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 16.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Red.copy(alpha = 0.7f))
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text("REC 4K • 60FPS", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Mode Switcher: Portrait, Photo, Video, More
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            listOf("Portrait", "Photo", "Video", "More").forEach { mode ->
                val isSelected = selectedMode == mode
                Text(
                    text = mode,
                    color = if (isSelected) Color(0xFFFFD60A) else Color.White,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    fontSize = 14.sp,
                    modifier = Modifier.clickable {
                        selectedMode = mode
                        if (mode == "Video") {
                            GlobalIslandState.currentMode = IslandMode.RECORDING
                            GlobalIslandState.isRecordingActive = true
                        }
                    }
                )
            }
        }

        // Tombol Shutter Kamera / Rekam
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 30.dp, top = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Galeri Thumbnail
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF333333))
            )

            // Shutter Button
            Box(
                modifier = Modifier
                    .size(76.dp)
                    .clip(CircleShape)
                    .border(4.dp, Color.White, CircleShape)
                    .padding(5.dp)
                    .clip(CircleShape)
                    .background(if (selectedMode == "Video") Color(0xFFFF3B30) else Color.White)
                    .clickable {
                        triggerHaptic(context)
                        if (selectedMode == "Video") {
                            isRecording = !isRecording
                            GlobalIslandState.isRecordingActive = isRecording
                            GlobalIslandState.currentMode = if (isRecording) IslandMode.RECORDING else IslandMode.MUSIC
                        }
                    }
            )

            // Kamera Flip
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF222222)),
                contentAlignment = Alignment.Center
            ) {
                Text("🔄", fontSize = 18.sp)
            }
        }
    }
}

// -------------------------------------------------------------------------
// 6. MUSIC APP SCREEN (Image 2 - Terintegrasi Penuh dengan Dynamic Island)
// -------------------------------------------------------------------------
@Composable
fun MusicScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val song = GlobalIslandState.currentSong
    val isPlaying = GlobalIslandState.isPlaying

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF000000))
    ) {
        AppHeaderBar(title = "Music", onBack = onBack)

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text("Good Vibes Playlist", color = Color(0xFF8E8E93), fontSize = 13.sp)
                Text("Tech & Lifestyle", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            }

            // Big Album Card
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp)
                        .clip(RoundedCornerShape(26.dp))
                        .background(
                            Brush.verticalGradient(
                                listOf(Color(song.albumColorHex), Color(0xFF0B0D13))
                            )
                        )
                        .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(26.dp))
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        AlbumArtThumbnail(
                            song = song,
                            modifier = Modifier
                                .size(110.dp)
                                .clip(RoundedCornerShape(20.dp))
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(song.title, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Text(song.artist, color = Color(0xFF8E8E93), fontSize = 14.sp)

                        Spacer(modifier = Modifier.height(10.dp))
                        LiveEqualizerWaveform(
                            isPlaying = isPlaying,
                            modifier = Modifier.size(50.dp, 22.dp),
                            barColor = Color(0xFF64D2FF)
                        )
                    }
                }
            }

            // Scrubber
            item {
                IosScrubber(
                    currentMs = GlobalIslandState.trackPositionMs,
                    totalMs = song.durationMs,
                    onSeek = { newPos -> GlobalIslandState.trackPositionMs = newPos }
                )
            }

            // Media Controls
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { GlobalIslandState.playPrevious(context) }) {
                        RewindIcon(modifier = Modifier.size(28.dp))
                    }

                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                            .clickable {
                                GlobalIslandState.togglePlayPause(context)
                                GlobalIslandState.currentMode = IslandMode.MUSIC
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        PlayPauseIcon(isPlaying = isPlaying, modifier = Modifier.size(28.dp), tint = Color.Black)
                    }

                    IconButton(onClick = { GlobalIslandState.playNext(context) }) {
                        FastForwardIcon(modifier = Modifier.size(28.dp))
                    }
                }
            }

            // Tracklist Items
            item {
                Text("Daftar Putar", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 17.sp)
            }

            items(GlobalIslandState.playlist) { itemSong ->
                val isCurrent = itemSong.title == song.title
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isCurrent) Color.White.copy(alpha = 0.12f) else Color.Transparent)
                        .clickable {
                            val idx = GlobalIslandState.playlist.indexOf(itemSong)
                            if (idx >= 0) {
                                GlobalIslandState.currentSongIndex = idx
                                GlobalIslandState.trackPositionMs = 0L
                                GlobalIslandState.isPlaying = true
                                GlobalIslandState.currentMode = IslandMode.MUSIC
                            }
                        }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AlbumArtThumbnail(song = itemSong, modifier = Modifier.size(40.dp).clip(RoundedCornerShape(8.dp)))
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = itemSong.title,
                            color = if (isCurrent) Color(0xFF64D2FF) else Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.5.sp
                        )
                        Text(text = itemSong.artist, color = Color(0xFF8E8E93), fontSize = 12.sp)
                    }
                    if (isCurrent && isPlaying) {
                        LiveEqualizerWaveform(isPlaying = true, modifier = Modifier.size(20.dp, 14.dp))
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------------------
// 7. BROWSER SCREEN (Image 2)
// -------------------------------------------------------------------------
@Composable
fun BrowserScreen(onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF000000))
    ) {
        AppHeaderBar(title = "Safari", onBack = onBack)

        // Address Bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .height(42.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF1C1C1E))
                .border(0.8.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                .padding(horizontal = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("🔒 google.com", color = Color.White, fontSize = 14.sp)
                Text("↻", color = Color(0xFF8E8E93), fontSize = 16.sp)
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                Text("Google", color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(14.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color(0xFF242426))
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text("🔍 Search or type URL", color = Color(0xFF8E8E93), fontSize = 14.sp)
                }
            }

            // Shortcuts
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    listOf("YouTube", "Gmail", "Maps", "Photos").forEach { sc ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF1C1C1E)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(sc.take(1), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(sc, color = Color(0xFF8E8E93), fontSize = 11.sp)
                        }
                    }
                }
            }

            // Discover Feed Card (Image 2)
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color(0xFF1C1C1E))
                        .padding(16.dp)
                ) {
                    Text("Discover", color = Color(0xFF8E8E93), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Brush.linearGradient(listOf(Color(0xFF0F2027), Color(0xFF203A43))))
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        "The Future of Smartphones in 2027",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "More intelligent. More connected. More you.",
                        color = Color(0xFF8E8E93),
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------------------
// 8. WEATHER SCREEN (Image 2)
// -------------------------------------------------------------------------
@Composable
fun WeatherScreen(onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF1E3C72), Color(0xFF2A5298), Color(0xFF0B1021))
                )
            )
    ) {
        AppHeaderBar(title = "Weather", onBack = onBack)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Jakarta", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.SemiBold)
            Text("28°", color = Color.White, fontSize = 72.sp, fontWeight = FontWeight.Thin)
            Text("Mostly Cloudy", color = Color.White.copy(alpha = 0.9f), fontSize = 16.sp)
            Text("H:32°  L:24°", color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp)
        }

        // Hourly Forecast Horizontal Row
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color.White.copy(alpha = 0.12f))
                .padding(14.dp)
        ) {
            Text("PRAKIRAAN PER JAM", color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(10.dp))
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                val hours = listOf("Now" to 28, "12PM" to 31, "3PM" to 29, "6PM" to 27, "9PM" to 25, "12AM" to 24)
                items(hours) { (time, temp) ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(time, color = Color.White, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("☁️", fontSize = 16.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("$temp°", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 5-Day Forecast Card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color.White.copy(alpha = 0.12f))
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("PRAKIRAAN 5 HARI", color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
            listOf("Tue" to "24° - 32°", "Wed" to "24° - 31°", "Thu" to "23° - 30°", "Fri" to "24° - 31°").forEach { (day, range) ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(day, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    Text("⛅", fontSize = 15.sp)
                    Text(range, color = Color.White, fontSize = 14.sp)
                }
            }
        }
    }
}

// -------------------------------------------------------------------------
// 9. CALENDAR SCREEN (Image 2)
// -------------------------------------------------------------------------
@Composable
fun CalendarScreen(onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF000000))
    ) {
        AppHeaderBar(title = "Calendar", onBack = onBack)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("September 2025", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)

            // Header Hari: S M T W T F S
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                listOf("S", "M", "T", "W", "T", "F", "S").forEach { d ->
                    Text(d, color = Color(0xFF8E8E93), fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(36.dp), textAlign = TextAlign.Center)
                }
            }

            // Grid Tanggal 1 - 30
            val dates = (1..30).toList()
            dates.chunked(7).forEach { week ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    week.forEach { date ->
                        val isToday = date == 15
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(if (isToday) Color(0xFF0A84FF) else Color.Transparent),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = date.toString(),
                                color = if (isToday) Color.White else Color.White.copy(alpha = 0.85f),
                                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }

            Divider(color = Color.White.copy(alpha = 0.1f))

            // Agenda List Card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color(0xFF1C1C1E))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.width(3.dp).height(30.dp).background(Color(0xFF0A84FF)))
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text("Team Sync", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text("10:00 - 11:00", color = Color(0xFF8E8E93), fontSize = 12.sp)
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.width(3.dp).height(30.dp).background(Color(0xFFFF9F0A)))
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text("Lunch with Raka", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text("12:30 - 13:30", color = Color(0xFF8E8E93), fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------------------
// 10. FILE MANAGER / FILES SCREEN (Image 2)
// -------------------------------------------------------------------------
@Composable
fun FilesScreen(onBack: () -> Unit) {
    val categories = listOf(
        Triple("🖼 Images", "1.2 GB", Color(0xFF34C759)),
        Triple("🎥 Videos", "4.8 GB", Color(0xFF007AFF)),
        Triple("📄 Documents", "2.1 GB", Color(0xFFFF9500)),
        Triple("🎵 Audio", "1.3 GB", Color(0xFFAF52DE)),
        Triple("⬇️ Downloads", "856 MB", Color(0xFFFF2D55))
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF000000))
    ) {
        AppHeaderBar(title = "Files", onBack = onBack)

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF1C1C1E))
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text("🔍 Search files", color = Color(0xFF8E8E93), fontSize = 14.sp)
                }
            }

            items(categories) { (name, sizeStr, tintColor) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF1C1C1E))
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(name, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, modifier = Modifier.weight(1f))
                    Text(sizeStr, color = Color(0xFF8E8E93), fontSize = 13.sp)
                }
            }
        }
    }
}

// -------------------------------------------------------------------------
// 11. NOTES SCREEN (Image 2)
// -------------------------------------------------------------------------
@Composable
fun NotesScreen(onBack: () -> Unit) {
    val notes = listOf(
        Triple("Tugas Skripsi", "Revisi bab 3 - metode penelitian", "Today"),
        Triple("Ide Project", "Website portofolio + blog", "Yesterday"),
        Triple("Shopping List", "Hoodie • Sepatu", "Sep 12"),
        Triple("Catatan Kuliah", "Sistem Informasi • Database", "Sep 10")
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF000000))
    ) {
        AppHeaderBar(title = "Notes", onBack = onBack)

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF1C1C1E))
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text("🔍 Search notes", color = Color(0xFF8E8E93), fontSize = 14.sp)
                }
            }

            items(notes) { (title, desc, date) ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF1C1C1E))
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text(date, color = Color(0xFF8E8E93), fontSize = 12.sp)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(desc, color = Color(0xFF8E8E93), fontSize = 13.sp)
                }
            }
        }
    }
}

// -------------------------------------------------------------------------
// 12. CALCULATOR SCREEN (Image 2 - Fungsionalitas Nyata)
// -------------------------------------------------------------------------
@Composable
fun CalculatorScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var display by remember { mutableStateOf("0") }
    var currentOp by remember { mutableStateOf<String?>(null) }
    var firstOperand by remember { mutableDoubleStateOf(0.0) }
    var clearOnNext by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF000000))
    ) {
        AppHeaderBar(title = "Calculator", onBack = onBack)

        Spacer(modifier = Modifier.weight(1f))

        // Display Angka Besar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp),
            contentAlignment = Alignment.BottomEnd
        ) {
            Text(display, color = Color.White, fontSize = 64.sp, fontWeight = FontWeight.Light, maxLines = 1)
        }

        // Layout Tombol Kalkulator iOS
        val rows = listOf(
            listOf("AC" to Color(0xFFA5A5A5), "+/-" to Color(0xFFA5A5A5), "%" to Color(0xFFA5A5A5), "÷" to Color(0xFFFF9F0A)),
            listOf("7" to Color(0xFF333333), "8" to Color(0xFF333333), "9" to Color(0xFF333333), "×" to Color(0xFFFF9F0A)),
            listOf("4" to Color(0xFF333333), "5" to Color(0xFF333333), "6" to Color(0xFF333333), "-" to Color(0xFFFF9F0A)),
            listOf("1" to Color(0xFF333333), "2" to Color(0xFF333333), "3" to Color(0xFF333333), "+" to Color(0xFFFF9F0A))
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            rows.forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    row.forEach { (label, bg) ->
                        val isOp = bg == Color(0xFFFF9F0A)
                        val isLight = bg == Color(0xFFA5A5A5)
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(bg)
                                .clickable {
                                    triggerHaptic(context)
                                    when (label) {
                                        "AC" -> {
                                            display = "0"
                                            firstOperand = 0.0
                                            currentOp = null
                                            clearOnNext = false
                                        }
                                        "+/-" -> {
                                            display = if (display.startsWith("-")) display.drop(1) else "-$display"
                                        }
                                        "%" -> {
                                            val v = display.toDoubleOrNull() ?: 0.0
                                            display = (v / 100.0).toString()
                                        }
                                        "÷", "×", "-", "+" -> {
                                            firstOperand = display.toDoubleOrNull() ?: 0.0
                                            currentOp = label
                                            clearOnNext = true
                                        }
                                        else -> {
                                            if (display == "0" || clearOnNext) {
                                                display = label
                                                clearOnNext = false
                                            } else {
                                                display += label
                                            }
                                        }
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(label, color = if (isLight) Color.Black else Color.White, fontSize = 28.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }

            // Baris Terakhir: 0, ,, =
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                // Tombol 0 Panjang
                Box(
                    modifier = Modifier
                        .width(156.dp)
                        .height(72.dp)
                        .clip(RoundedCornerShape(36.dp))
                        .background(Color(0xFF333333))
                        .clickable {
                            triggerHaptic(context)
                            if (display != "0" && !clearOnNext) {
                                display += "0"
                            } else {
                                display = "0"
                                clearOnNext = false
                            }
                        }
                        .padding(start = 28.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text("0", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Medium)
                }

                // Koma / Desimal
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF333333))
                        .clickable {
                            triggerHaptic(context)
                            if (!display.contains(".")) display += "."
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(",", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Medium)
                }

                // Sama Dengan (=)
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFF9F0A))
                        .clickable {
                            triggerHaptic(context)
                            val second = display.toDoubleOrNull() ?: 0.0
                            val res = when (currentOp) {
                                "+" -> firstOperand + second
                                "-" -> firstOperand - second
                                "×" -> firstOperand * second
                                "÷" -> if (second != 0.0) firstOperand / second else 0.0
                                else -> second
                            }
                            display = if (res % 1.0 == 0.0) res.toLong().toString() else String.format("%.2f", res)
                            clearOnNext = true
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text("=", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

// -------------------------------------------------------------------------
// 13. AI ASSISTANT (GEMINI) SCREEN (Image 2)
// -------------------------------------------------------------------------
@Composable
fun AIAssistantScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var inputQuery by remember { mutableStateOf("") }
    var chatMessages by remember {
        mutableStateOf(
            listOf(
                "Gemini" to "Halo! Saya Gemini, asisten AI cerdas Anda. Ada yang bisa saya bantu hari ini?",
            )
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF000000))
    ) {
        AppHeaderBar(title = "Gemini AI", onBack = onBack)

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(listOf(Color(0xFF0A84FF), Color(0xFFBF5AF2)))),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("✨", fontSize = 28.sp)
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("Hi, I'm Gemini", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    Text("How can I help you today?", color = Color(0xFF8E8E93), fontSize = 14.sp)
                }
            }

            // Quick Chips
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("Ringkas halaman", "Rencana belajar", "Terjemahkan").forEach { chip ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xFF1C1C1E))
                                .border(0.8.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                                .clickable {
                                    triggerHaptic(context)
                                    inputQuery = chip
                                }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(chip, color = Color(0xFF64D2FF), fontSize = 11.5.sp)
                        }
                    }
                }
            }

            // Chat Bubbles
            items(chatMessages) { (sender, msg) ->
                val isMe = sender == "Me"
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start
                ) {
                    Box(
                        modifier = Modifier
                            .widthIn(max = 280.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (isMe) Color(0xFF0A84FF) else Color(0xFF1C1C1E))
                            .padding(12.dp)
                    ) {
                        Text(msg, color = Color.White, fontSize = 14.sp)
                    }
                }
            }
        }

        // Input Field
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(Color(0xFF1C1C1E))
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                OutlinedTextField(
                    value = inputQuery,
                    onValueChange = { inputQuery = it },
                    placeholder = { Text("Ask anything...", color = Color(0xFF8E8E93), fontSize = 13.sp) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent
                    ),
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF0A84FF))
                    .clickable {
                        if (inputQuery.isNotBlank()) {
                            triggerHaptic(context)
                            val q = inputQuery
                            chatMessages = chatMessages + ("Me" to q)
                            chatMessages = chatMessages + ("Gemini" to "Jawaban AI untuk '$q': Fitur ini siap digunakan dengan koneksi Gemini.")
                            inputQuery = ""
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Text("➤", color = Color.White, fontSize = 16.sp)
            }
        }
    }
}
