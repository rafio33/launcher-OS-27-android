package com.rafio.launcher27

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
// LOCK SCREEN SESUAI SCREENSHOT IMAGE 2
// Menampilkan Dynamic Island Panggilan Masuk Raka & Jam 9:41
// =========================================================================
@Composable
fun LockScreenOverlay(
    isLocked: Boolean,
    onUnlock: () -> Unit,
    onOpenApp: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (!isLocked) return
    val context = LocalContext.current

    val transition = rememberInfiniteTransition(label = "unlockPulse")
    val hintAlpha by transition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Reverse),
        label = "alpha"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .zIndex(60f)
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF0F1A30),
                        Color(0xFF28325B),
                        Color(0xFF5A4476),
                        Color(0xFF0D1226)
                    )
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
            .draggable(
                state = rememberDraggableState { delta ->
                    if (delta < -20f) {
                        triggerHaptic(context)
                        onUnlock()
                    }
                },
                orientation = Orientation.Vertical
            )
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Spacer Dynamic Island di Bagian Paling Atas
            Spacer(modifier = Modifier.height(10.dp))

            // Dynamic Island di Lock Screen (Sesuai Image 2: Panggilan Raka)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                InteractiveDynamicIslandBar()
            }

            Spacer(modifier = Modifier.height(34.dp))

            // Tanggal ala iOS
            Text(
                text = "Monday, September 15",
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 17.sp,
                fontWeight = FontWeight.Medium
            )

            // Jam Besar 9:41
            Text(
                text = "9:41",
                color = Color.White,
                fontSize = 82.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-1.5).sp
            )

            Spacer(modifier = Modifier.weight(1f))

            // Teks Geser ke Atas untuk Buka Kunci
            Text(
                text = "Swipe up to unlock",
                color = Color.White.copy(alpha = hintAlpha),
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable {
                        triggerHaptic(context)
                        onUnlock()
                    }
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Baris Bawah: Flashlight & Camera
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 46.dp, vertical = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Flashlight Shortcut
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f))
                        .clickable { triggerHaptic(context) },
                    contentAlignment = Alignment.Center
                ) {
                    Text("🔦", fontSize = 20.sp)
                }

                // Garis Home Indicator
                Box(
                    modifier = Modifier
                        .width(134.dp)
                        .height(5.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(Color.White.copy(alpha = 0.85f))
                        .clickable { onUnlock() }
                )

                // Camera Shortcut
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f))
                        .clickable {
                            triggerHaptic(context)
                            onUnlock()
                            onOpenApp("Camera")
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text("📷", fontSize = 20.sp)
                }
            }
        }
    }
}
