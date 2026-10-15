package com.rafio.launcher27

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// =========================================================================
// IKON BATERAI KAPSUL RESMI iOS
// =========================================================================
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

        drawRoundRect(
            color = Color.White.copy(alpha = 0.40f),
            topLeft = Offset(0f, 0f),
            size = Size(bodyW, h),
            cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx()),
            style = Stroke(width = 1.2.dp.toPx())
        )

        val nubW = 1.8.dp.toPx()
        val nubH = h * 0.40f
        drawRoundRect(
            color = Color.White.copy(alpha = 0.40f),
            topLeft = Offset(bodyW + 0.8.dp.toPx(), (h - nubH) / 2),
            size = Size(nubW, nubH),
            cornerRadius = CornerRadius(1.dp.toPx(), 1.dp.toPx())
        )

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

// =========================================================================
// IKON TELEPON HANDSET HIJAU
// =========================================================================
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

// =========================================================================
// EQUALIZER MULTI-WARNA PANGGILAN
// =========================================================================
@Composable
fun CallEqualizerWaveform(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "callEq")
    val c1 by transition.animateFloat(0.35f, 0.95f, infiniteRepeatable(tween(250, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "c1")
    val c2 by transition.animateFloat(0.85f, 0.30f, infiniteRepeatable(tween(330, easing = LinearOutSlowInEasing), RepeatMode.Reverse), label = "c2")
    val c3 by transition.animateFloat(0.40f, 1.00f, infiniteRepeatable(tween(280, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "c3")
    val c4 by transition.animateFloat(0.95f, 0.20f, infiniteRepeatable(tween(350, easing = LinearOutSlowInEasing), RepeatMode.Reverse), label = "c4")
    val c5 by transition.animateFloat(0.50f, 0.90f, infiniteRepeatable(tween(300, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "c5")

    val barColors = listOf(
        Color(0xFF30D158),
        Color(0xFF30D158),
        Color(0xFFFFD60A),
        Color(0xFFFF9F0A),
        Color(0xFFFF453A)
    )

    Canvas(modifier = modifier) {
        val barW = 2.4.dp.toPx()
        val spacing = 2.dp.toPx()
        val bars = listOf(c1, c2, c3, c4, c5)
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

// =========================================================================
// SCRUBBER & TIMELINE AUDIO iOS
// =========================================================================
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
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.5.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0xFF38383A))
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth(activeRatio)
                    .height(3.5.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.White)
            )
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

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = String.format("%d:%02d", currentSec / 60, currentSec % 60),
                color = Color(0xFF8E8E93),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = String.format("-%d:%02d", remainingSec / 60, remainingSec % 60),
                color = Color(0xFF8E8E93),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

// =========================================================================
// IKON-IKON MEDIA KONTROL (REWIND, FAST FORWARD, PLAY/PAUSE, AIRPLAY)
// =========================================================================
@Composable
fun RewindIcon(modifier: Modifier = Modifier, tint: Color = Color.White) {
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

@Composable
fun FastForwardIcon(modifier: Modifier = Modifier, tint: Color = Color.White) {
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

@Composable
fun PlayPauseIcon(isPlaying: Boolean, modifier: Modifier = Modifier, tint: Color = Color.White) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        if (isPlaying) {
            val barW = 3.8.dp.toPx()
            val barH = h * 0.70f
            val gap = 5.dp.toPx()
            val startX = (w - (2 * barW + gap)) / 2
            val startY = (h - barH) / 2
            drawRoundRect(tint, Offset(startX, startY), Size(barW, barH), CornerRadius(2.dp.toPx(), 2.dp.toPx()))
            drawRoundRect(tint, Offset(startX + barW + gap, startY), Size(barW, barH), CornerRadius(2.dp.toPx(), 2.dp.toPx()))
        } else {
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

@Composable
fun AirPlayIcon(modifier: Modifier = Modifier, tint: Color = Color.White) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val triangle = Path().apply {
            moveTo(w * 0.50f, h * 0.56f)
            lineTo(w * 0.72f, h * 0.88f)
            lineTo(w * 0.28f, h * 0.88f)
            close()
        }
        drawPath(triangle, color = tint)
        val strokeW = 1.6.dp.toPx()
        drawArc(
            color = tint,
            startAngle = 210f,
            sweepAngle = 120f,
            useCenter = false,
            topLeft = Offset(w * 0.24f, h * 0.24f),
            size = Size(w * 0.52f, h * 0.52f),
            style = Stroke(width = strokeW, cap = StrokeCap.Round)
        )
    }
}

// Ikon Sinyal Seluler & Wi-Fi untuk Quick Panel
@Composable
fun QuickPanelWifiIcon(modifier: Modifier = Modifier, tint: Color = Color.White) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val stroke = 1.6.dp.toPx()
        drawArc(
            color = tint,
            startAngle = 225f,
            sweepAngle = 90f,
            useCenter = false,
            topLeft = Offset(w * 0.1f, h * 0.1f),
            size = Size(w * 0.8f, h * 0.8f),
            style = Stroke(width = stroke, cap = StrokeCap.Round)
        )
        drawArc(
            color = tint,
            startAngle = 225f,
            sweepAngle = 90f,
            useCenter = false,
            topLeft = Offset(w * 0.28f, h * 0.35f),
            size = Size(w * 0.44f, h * 0.44f),
            style = Stroke(width = stroke, cap = StrokeCap.Round)
        )
        drawCircle(color = tint, radius = 1.5.dp.toPx(), center = Offset(w * 0.5f, h * 0.85f))
    }
}

@Composable
fun QuickPanelBluetoothIcon(modifier: Modifier = Modifier, tint: Color = Color.White) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val stroke = 1.8.dp.toPx()
        val path = Path().apply {
            moveTo(w * 0.5f, 0f)
            lineTo(w * 0.5f, h)
            lineTo(w * 0.75f, h * 0.75f)
            lineTo(w * 0.25f, h * 0.25f)
            moveTo(w * 0.25f, h * 0.75f)
            lineTo(w * 0.75f, h * 0.25f)
            lineTo(w * 0.5f, 0f)
        }
        drawPath(path, color = tint, style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

@Composable
fun QuickPanelAirplaneIcon(modifier: Modifier = Modifier, tint: Color = Color.White) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val p = Path().apply {
            moveTo(w * 0.5f, 0f)
            lineTo(w * 0.6f, h * 0.35f)
            lineTo(w * 0.95f, h * 0.55f)
            lineTo(w * 0.95f, h * 0.65f)
            lineTo(w * 0.58f, h * 0.60f)
            lineTo(w * 0.58f, h * 0.82f)
            lineTo(w * 0.72f, h * 0.92f)
            lineTo(w * 0.72f, h)
            lineTo(w * 0.5f, h * 0.92f)
            lineTo(w * 0.28f, h)
            lineTo(w * 0.28f, h * 0.92f)
            lineTo(w * 0.42f, h * 0.82f)
            lineTo(w * 0.42f, h * 0.60f)
            lineTo(w * 0.05f, h * 0.65f)
            lineTo(w * 0.05f, h * 0.55f)
            lineTo(w * 0.4f, h * 0.35f)
            close()
        }
        drawPath(p, color = tint)
    }
}
