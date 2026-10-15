package com.rafio.launcher27

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex

// =========================================================================
// RECENT APPS / MULTITASKING SESUAI SCREENSHOT IMAGE 2
// Kartu Melayang dengan Efek Blur & Tombol Close All
// =========================================================================
@Composable
fun RecentAppsOverlay(
    isOpen: Boolean,
    onDismiss: () -> Unit,
    onSelectApp: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (!isOpen) return
    val context = LocalContext.current

    val recentApps = remember {
        mutableStateListOf(
            Triple("Chrome", "The Future of Smartphones", "google.com"),
            Triple("Music", "Good Vibes Playlist", "Adele"),
            Triple("Camera", "Photo Viewfinder", "Ready"),
            Triple("Settings", "Connections", "Wi-Fi")
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .zIndex(55f)
            .background(Color.Black.copy(alpha = 0.72f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                triggerHaptic(context)
                onDismiss()
            }
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(vertical = 20.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Text(
                text = "${recentApps.size} active app${if (recentApps.size > 1) "s" else ""}",
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Carousel Kartu Melayang
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(horizontal = 40.dp)
            ) {
                items(recentApps) { (appName, previewTitle, previewDesc) ->
                    Box(
                        modifier = Modifier
                            .width(260.dp)
                            .fillMaxHeight(0.85f)
                            .clip(RoundedCornerShape(28.dp))
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color(0xFF1C2237), Color(0xFF0F1424))
                                )
                            )
                            .border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(28.dp))
                            .clickable {
                                triggerHaptic(context)
                                onSelectApp(appName)
                                onDismiss()
                            }
                            .padding(16.dp)
                    ) {
                        Column(modifier = Modifier.fillMaxSize()) {
                            // Header Kartu
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = when (appName) {
                                            "Chrome" -> "🌐"
                                            "Music" -> "🎵"
                                            "Camera" -> "📷"
                                            else -> "⚙️"
                                        },
                                        fontSize = 16.sp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(appName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                }

                                Text(
                                    text = "✕",
                                    color = Color.White.copy(alpha = 0.6f),
                                    fontSize = 14.sp,
                                    modifier = Modifier.clickable {
                                        triggerHaptic(context)
                                        recentApps.remove(Triple(appName, previewTitle, previewDesc))
                                        if (recentApps.isEmpty()) onDismiss()
                                    }
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Konten Preview Kartu
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(Color.White.copy(alpha = 0.08f))
                                    .padding(14.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(previewTitle, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(previewDesc, color = Color(0xFF8E8E93), fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Tombol "Close all" Glass
            Box(
                modifier = Modifier
                    .width(160.dp)
                    .height(44.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(Color.White.copy(alpha = 0.18f))
                    .border(1.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(22.dp))
                    .clickable {
                        triggerHaptic(context)
                        recentApps.clear()
                        onDismiss()
                    },
                contentAlignment = Alignment.Center
            ) {
                Text("Close all", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            }
        }
    }
}
