package com.rafio.launcher27

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex

// =========================================================================
// QUICK PANEL (CONTROL CENTER) SESUAI SCREENSHOT IMAGE 2
// =========================================================================
@Composable
fun QuickPanelOverlay(
    isOpen: Boolean,
    onDismiss: () -> Unit,
    onOpenApp: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (!isOpen) return
    val context = LocalContext.current

    var wifiEnabled by remember { mutableStateOf(true) }
    var bluetoothEnabled by remember { mutableStateOf(true) }
    var airplaneEnabled by remember { mutableStateOf(false) }
    var cellularEnabled by remember { mutableStateOf(true) }
    var brightnessLevel by remember { mutableFloatStateOf(0.75f) }
    var volumeLevel by remember { mutableFloatStateOf(0.60f) }

    val song = GlobalIslandState.currentSong
    val isPlaying = GlobalIslandState.isPlaying

    // Scrim latar belakang dengan efek glassmorphism blur
    Box(
        modifier = modifier
            .fillMaxSize()
            .zIndex(50f)
            .background(Color.Black.copy(alpha = 0.55f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                triggerHaptic(context)
                onDismiss()
            }
            .statusBarsPadding()
            .padding(horizontal = 18.dp, vertical = 12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { /* Mencegah klik tembus */ },
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header: Jam & Baterai Quick Panel
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("9:41", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    Text("Mon, Sep 15", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    QuickPanelWifiIcon(modifier = Modifier.size(15.dp, 12.dp))
                    Text("⚡ 100%", color = Color(0xFF30D158), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Baris 1: 4 Toggles Kiri (Wi-Fi, Bluetooth, Airplane, Cellular) + 2 Sliders Kanan
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(148.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // 4-Grid Connectivity Box
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(26.dp))
                        .background(Color.White.copy(alpha = 0.16f))
                        .border(1.dp, Color.White.copy(alpha = 0.28f), RoundedCornerShape(26.dp))
                        .padding(12.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            // Wi-Fi
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(CircleShape)
                                    .background(if (wifiEnabled) Color(0xFF0A84FF) else Color.White.copy(alpha = 0.2f))
                                    .clickable {
                                        triggerHaptic(context)
                                        wifiEnabled = !wifiEnabled
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                QuickPanelWifiIcon(modifier = Modifier.size(24.dp))
                            }
                            // Bluetooth
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(CircleShape)
                                    .background(if (bluetoothEnabled) Color(0xFF0A84FF) else Color.White.copy(alpha = 0.2f))
                                    .clickable {
                                        triggerHaptic(context)
                                        bluetoothEnabled = !bluetoothEnabled
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                QuickPanelBluetoothIcon(modifier = Modifier.size(24.dp))
                            }
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            // Airplane
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(CircleShape)
                                    .background(if (airplaneEnabled) Color(0xFFFF9F0A) else Color.White.copy(alpha = 0.2f))
                                    .clickable {
                                        triggerHaptic(context)
                                        airplaneEnabled = !airplaneEnabled
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                QuickPanelAirplaneIcon(modifier = Modifier.size(22.dp))
                            }
                            // Cellular
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(CircleShape)
                                    .background(if (cellularEnabled) Color(0xFF30D158) else Color.White.copy(alpha = 0.2f))
                                    .clickable {
                                        triggerHaptic(context)
                                        cellularEnabled = !cellularEnabled
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text("📶", fontSize = 18.sp)
                            }
                        }
                    }
                }

                // 2 Slider Vertikal: Kecerahan & Volume
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Brightness Slider
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(26.dp))
                            .background(Color.White.copy(alpha = 0.16f))
                            .border(1.dp, Color.White.copy(alpha = 0.28f), RoundedCornerShape(26.dp))
                            .clickable {
                                triggerHaptic(context)
                                brightnessLevel = if (brightnessLevel > 0.8f) 0.35f else 0.85f
                            },
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(brightnessLevel)
                                .background(Color.White)
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(bottom = 14.dp),
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            Text("☀️", fontSize = 20.sp)
                        }
                    }

                    // Volume Slider
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(26.dp))
                            .background(Color.White.copy(alpha = 0.16f))
                            .border(1.dp, Color.White.copy(alpha = 0.28f), RoundedCornerShape(26.dp))
                            .clickable {
                                triggerHaptic(context)
                                volumeLevel = if (volumeLevel > 0.7f) 0.25f else 0.75f
                            },
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(volumeLevel)
                                .background(Color.White)
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(bottom = 14.dp),
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            Text("🔊", fontSize = 20.sp)
                        }
                    }
                }
            }

            // Baris 2: Kartu Pemutar Musik Quick Panel (Adele / Current Song)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(96.dp)
                    .clip(RoundedCornerShape(26.dp))
                    .background(Color.White.copy(alpha = 0.16f))
                    .border(1.dp, Color.White.copy(alpha = 0.28f), RoundedCornerShape(26.dp))
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        AlbumArtThumbnail(
                            song = song,
                            modifier = Modifier
                                .size(54.dp)
                                .clip(RoundedCornerShape(12.dp))
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(song.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text(song.artist, color = Color(0xFF8E8E93), fontSize = 12.sp)
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("⏮", color = Color.White, fontSize = 18.sp, modifier = Modifier.clickable { GlobalIslandState.playPrevious(context) })
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                                .clickable {
                                    GlobalIslandState.togglePlayPause(context)
                                    GlobalIslandState.currentMode = IslandMode.MUSIC
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            PlayPauseMiniIcon(isPlaying = isPlaying, modifier = Modifier.size(14.dp), tint = Color.Black)
                        }
                        Text("⏭", color = Color.White, fontSize = 18.sp, modifier = Modifier.clickable { GlobalIslandState.playNext(context) })
                    }
                }
            }

            // Baris 3: Device Control & Smart View Pills
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp)
                        .clip(RoundedCornerShape(23.dp))
                        .background(Color.White.copy(alpha = 0.16f))
                        .border(1.dp, Color.White.copy(alpha = 0.28f), RoundedCornerShape(23.dp))
                        .clickable { },
                    contentAlignment = Alignment.Center
                ) {
                    Text("💡 Device control", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp)
                        .clip(RoundedCornerShape(23.dp))
                        .background(Color.White.copy(alpha = 0.16f))
                        .border(1.dp, Color.White.copy(alpha = 0.28f), RoundedCornerShape(23.dp))
                        .clickable { },
                    contentAlignment = Alignment.Center
                ) {
                    Text("📺 Smart View", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            // Baris 4: 4 Tombol Cepat (Flashlight, Timer, Calculator, Camera)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Flashlight
                Box(
                    modifier = Modifier
                        .size(62.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.16f))
                        .border(1.dp, Color.White.copy(alpha = 0.28f), CircleShape)
                        .clickable { triggerHaptic(context) },
                    contentAlignment = Alignment.Center
                ) {
                    Text("🔦", fontSize = 22.sp)
                }

                // Timer (Memicu Dynamic Island Mode Timer)
                Box(
                    modifier = Modifier
                        .size(62.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.16f))
                        .border(1.dp, Color.White.copy(alpha = 0.28f), CircleShape)
                        .clickable {
                            triggerHaptic(context)
                            GlobalIslandState.currentMode = IslandMode.TIMER
                            GlobalIslandState.isTimerRunning = true
                            GlobalIslandState.timerSeconds = 14
                            onDismiss()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text("⏱", fontSize = 22.sp)
                }

                // Calculator
                Box(
                    modifier = Modifier
                        .size(62.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.16f))
                        .border(1.dp, Color.White.copy(alpha = 0.28f), CircleShape)
                        .clickable {
                            triggerHaptic(context)
                            onOpenApp("Calculator")
                            onDismiss()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text("🧮", fontSize = 22.sp)
                }

                // Camera
                Box(
                    modifier = Modifier
                        .size(62.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.16f))
                        .border(1.dp, Color.White.copy(alpha = 0.28f), CircleShape)
                        .clickable {
                            triggerHaptic(context)
                            onOpenApp("Camera")
                            onDismiss()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text("📷", fontSize = 22.sp)
                }
            }
        }
    }
}
