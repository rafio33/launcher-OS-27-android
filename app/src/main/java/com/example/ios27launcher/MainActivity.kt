package com.example.ios27launcher

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.core.view.WindowCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class AppModel(
    val label: String,
    val packageName: String,
    val icon: ImageBitmap
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() { /* Tetap di Home Screen */ }
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
    var apps by remember { mutableStateOf<List<AppModel>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }
    var showSpotlight by remember { mutableStateOf(false) }
    var islandExpanded by remember { mutableStateOf(false) }
    var iconMode by remember { mutableStateOf(0) } // 0: Light, 1: Dark, 2: Tinted Blue
    var selectedAppMenu by remember { mutableStateOf<AppModel?>(null) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val pm = context.packageManager
            val intent = Intent(Intent.ACTION_MAIN, null).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }
            val resolveInfos = pm.queryIntentActivities(intent, 0)
            val loadedApps = resolveInfos.mapNotNull { info ->
                val pkg = info.activityInfo.packageName
                if (pkg == context.packageName) return@mapNotNull null
                AppModel(
                    label = info.loadLabel(pm).toString(),
                    packageName = pkg,
                    icon = info.loadIcon(pm).toBitmap(140, 140).asImageBitmap()
                )
            }.sortedBy { it.label.lowercase() }
            apps = loadedApps
        }
    }

    val filteredApps = remember(apps, searchQuery) {
        if (searchQuery.isBlank()) apps
        else apps.filter { it.label.contains(searchQuery, ignoreCase = true) }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0A1128).copy(alpha = 0.72f),
                        Color(0xFF1C2541).copy(alpha = 0.78f),
                        Color(0xFF0B132B).copy(alpha = 0.85f)
                    )
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // 1. DYNAMIC ISLAND INTERAKTIF
            val islandWidth by animateDpAsState(if (islandExpanded) 330.dp else 136.dp, label = "")
            val islandHeight by animateDpAsState(if (islandExpanded) 88.dp else 35.dp, label = "")

            Box(
                modifier = Modifier
                    .width(islandWidth)
                    .height(islandHeight)
                    .clip(RoundedCornerShape(30.dp))
                    .background(Color.Black.copy(alpha = 0.9f))
                    .border(0.8.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(30.dp))
                    .clickable {
                        triggerHaptic(context)
                        islandExpanded = !islandExpanded
                    }
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                if (!islandExpanded) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF30D158))
                        )
                        Text("iOS 27 Spatial", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Apple HIG Spatial", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            val modeName = when(iconMode) {
                                1 -> "Dark Glass"
                                2 -> "Cyan Tinted"
                                else -> "Default Light"
                            }
                            Text("Tema Ikon: $modeName", color = Color.White.copy(0.7f), fontSize = 11.sp)
                        }
                        Button(
                            onClick = { iconMode = (iconMode + 1) % 3 },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0A84FF)),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Text("Ganti Tema", fontSize = 12.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // 2. WIDGET BARIS ATAS ALA APPLE HIG
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(145.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Cupertino Weather Widget
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(26.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFF1D6FA3).copy(alpha = 0.85f), Color(0xFF0F3460).copy(alpha = 0.85f))
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
                            Text("29°", color = Color.White, fontWeight = FontWeight.Light, fontSize = 40.sp)
                        }
                        Text("Cerah Berawan\nT:32°  R:24°", color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp, lineHeight = 16.sp)
                    }
                }

                // Spatial Battery Widget
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(26.dp))
                        .background(Color.White.copy(alpha = 0.14f))
                        .border(1.dp, Color.White.copy(alpha = 0.24f), RoundedCornerShape(26.dp))
                        .padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Android 14", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                            Text("94%", color = Color(0xFF30D158), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                        LinearProgressIndicator(
                            progress = 0.94f,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = Color(0xFF30D158),
                            trackColor = Color.White.copy(alpha = 0.2f),
                        )
                        Text(
                            "Ketuk Dynamic Island untuk mode ikon iOS 18/27",
                            color = Color.White.copy(alpha = 0.75f),
                            fontSize = 11.sp,
                            lineHeight = 14.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 3. GRID APLIKASI UTAMA (APPLE SQUIRCLE)
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(18.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredApps, key = { it.packageName }) { app ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .combinedClickable(
                                onClick = { launchApp(context, app.packageName) },
                                onLongClick = {
                                    triggerHaptic(context)
                                    selectedAppMenu = app
                                }
                            )
                    ) {
                        Box(
                            modifier = Modifier
                                .size(62.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    when (iconMode) {
                                        1 -> Color(0xFF1C1C1E).copy(alpha = 0.92f)
                                        2 -> Color(0xFF0A84FF).copy(alpha = 0.25f)
                                        else -> Color.White.copy(alpha = 0.18f)
                                    }
                                )
                                .border(
                                    0.8.dp,
                                    Color.White.copy(alpha = if (iconMode == 1) 0.15f else 0.35f),
                                    RoundedCornerShape(16.dp)
                                )
                                .padding(6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                bitmap = app.icon,
                                contentDescription = app.label,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(12.dp)),
                                colorFilter = when (iconMode) {
                                    1 -> ColorFilter.lighting(Color(0xFFCCCCCC), Color(0xFF111111))
                                    2 -> ColorFilter.lighting(Color(0xFF64D2FF), Color(0xFF002244))
                                    else -> null
                                }
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
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

            Spacer(modifier = Modifier.height(8.dp))

            // 4. KAPSUL SPOTLIGHT SEARCH DI ATAS DOCK ("Cari")
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White.copy(alpha = 0.18f))
                    .border(0.6.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
                    .clickable { showSpotlight = !showSpotlight }
                    .padding(horizontal = 18.dp, vertical = 6.dp)
            ) {
                Text("🔍 Cari", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            }

            AnimatedVisibility(visible = showSpotlight, enter = fadeIn(), exit = fadeOut()) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Spotlight Search aplikasi...", color = Color.White.copy(0.6f)) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF0A84FF),
                        unfocusedBorderColor = Color.White.copy(0.3f)
                    )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 5. GLASS DOCK BAWAH
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(92.dp)
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

        // 6. 3D HAPTIC TOUCH CONTEXT MENU
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
                        Divider(color = Color.White.copy(0.15f))
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
                            "⚙️ Info Aplikasi Sistem (Android 14)",
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
