package com.rafio.launcher27

import android.content.Context
import android.media.AudioManager
import android.net.Uri
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.KeyEvent
import androidx.compose.runtime.*

// 9 Mode Dynamic Island sesuai Screenshot Image 1 & 2:
// 1. MUSIC          -> Sweater Weather - The Neighbourhood (atau Adele Someone Like You)
// 2. NAVIGATION     -> 200 m Turn right + Maps Icon
// 3. TIMER          -> Timer 00:14 + Tombol Pause/Play
// 4. RECORDING      -> 🔴 00:05 + Gelombang Suara (Kamera/Perekam)
// 5. LIVE_ACTIVITY  -> GoFood: Pesanan dalam proses, 12 min
// 6. FACE_ID        -> Face ID: Look at your iPhone
// 7. CHARGING       -> ⚡ Charging 78% + Baterai
// 8. TRANSLATE      -> Live Translate: English ↔ Indonesian
// 9. FOCUS          -> Focus: Do Not Disturb
// 10. CALL          -> Raka: Incoming call... / 0:08 Panggilan Berlangsung
// 11. IDLE          -> Standby Pill Minimalis
enum class IslandMode {
    MUSIC,
    NAVIGATION,
    TIMER,
    RECORDING,
    LIVE_ACTIVITY,
    FACE_ID,
    CHARGING,
    TRANSLATE,
    FOCUS,
    CALL,
    IDLE
}

data class SongItem(
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val albumColorHex: Long = 0xFF2C1A4D
)

object GlobalIslandState {
    // Mode Island saat ini
    var currentMode by mutableStateOf(IslandMode.MUSIC)
    var isExpanded by mutableStateOf(false)

    // Mode yang terakhir dibuka di latar belakang
    var backgroundTaskMode by mutableStateOf<IslandMode?>(null)

    // Lagu-lagu referensi persis dari Screenshot Image 1 & 2
    val playlist = listOf(
        SongItem("Sweater Weather", "The Neighbourhood", "I Love You.", 240000L, 0xFF1B2430),
        SongItem("Someone Like You", "Adele", "21", 285000L, 0xFF2C1A4D),
        SongItem("Entropy", "Beach Bunny", "Emotional Creature", 221000L, 0xFF311E43),
        SongItem("Die For You", "The Weeknd", "Starboy", 260000L, 0xFF3D1624),
        SongItem("Perfect", "Ed Sheeran", "÷ (Divide)", 263000L, 0xFF142B3B),
        SongItem("Until I Found You", "Stephen Sanchez", "Easy on My Eyes", 178000L, 0xFF2A231C)
    )

    var currentSongIndex by mutableIntStateOf(0)
    val currentSong: SongItem
        get() = playlist.getOrElse(currentSongIndex) { playlist[0] }

    // State Pemutaran Musik
    var isPlaying by mutableStateOf(true)
    var trackPositionMs by mutableLongStateOf(74000L) // Sedang berjalan 1:14

    // State Navigasi (Screenshot Image 1: 200 m Turn right)
    var navDistance by mutableStateOf("200 m")
    var navInstruction by mutableStateOf("Turn right")
    var navDestination by mutableStateOf("Bundaran HI")

    // State Timer (Screenshot Image 1: Timer 00:14)
    var timerSeconds by mutableIntStateOf(14)
    var isTimerRunning by mutableStateOf(true)

    // State Rekaman Video/Audio (Screenshot Image 1: 🔴 00:05)
    var recordingSeconds by mutableIntStateOf(5)
    var isRecordingActive by mutableStateOf(false)

    // State Live Activity GoFood (Screenshot Image 1: GoFood Pesanan dalam proses 12 min)
    var foodOrderTitle by mutableStateOf("GoFood")
    var foodOrderStatus by mutableStateOf("Pesanan dalam proses")
    var foodOrderDesc by mutableStateOf("Pesanan kamu sedang diproses di restoran.")
    var foodOrderEta by mutableStateOf("12 min")
    var foodProgress by mutableFloatStateOf(0.65f)

    // State Face ID (Screenshot Image 1: Face ID Look at your iPhone)
    var isFaceIdScanning by mutableStateOf(false)

    // State Pengisian Daya (Screenshot Image 1: ⚡ Charging 78%)
    var chargingPct by mutableIntStateOf(78)

    // State Terjemahan Langsung (Screenshot Image 1: Live Translate English ↔ Indonesian)
    var translateSource by mutableStateOf("Hello, how are you?")
    var translateTarget by mutableStateOf("Halo, apa kabar?")

    // State Focus Mode (Screenshot Image 1: Focus Do Not Disturb)
    var focusName by mutableStateOf("Do Not Disturb")
    var isFocusActive by mutableStateOf(true)

    // State Panggilan (Screenshot Image 2: Raka Incoming call...)
    var callerName by mutableStateOf("Raka")
    var isIncomingCall by mutableStateOf(false)
    var callSeconds by mutableIntStateOf(8)

    // Kontrol Musik Interaktif
    fun togglePlayPause(context: Context?) {
        isPlaying = !isPlaying
        if (context != null) {
            triggerHaptic(context)
            sendMediaKeyEvent(context, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
        }
    }

    fun playNext(context: Context?) {
        currentSongIndex = (currentSongIndex + 1) % playlist.size
        trackPositionMs = 0L
        isPlaying = true
        if (context != null) {
            triggerHaptic(context)
            sendMediaKeyEvent(context, KeyEvent.KEYCODE_MEDIA_NEXT)
        }
    }

    fun playPrevious(context: Context?) {
        if (trackPositionMs > 5000L) {
            trackPositionMs = 0L
        } else {
            currentSongIndex = if (currentSongIndex > 0) currentSongIndex - 1 else playlist.size - 1
            trackPositionMs = 0L
        }
        isPlaying = true
        if (context != null) {
            triggerHaptic(context)
            sendMediaKeyEvent(context, KeyEvent.KEYCODE_MEDIA_PREVIOUS)
        }
    }

    fun toggleTimer(context: Context?) {
        isTimerRunning = !isTimerRunning
        if (context != null) triggerHaptic(context)
    }

    fun resetTimer(context: Context?) {
        timerSeconds = 14
        isTimerRunning = true
        if (context != null) triggerHaptic(context)
    }

    // Dipanggil saat aplikasi di gambar dibuka
    fun onAppOpened(appName: String, context: Context?) {
        when (appName.lowercase()) {
            "camera", "kamera" -> {
                isRecordingActive = true
                currentMode = IslandMode.RECORDING
            }
            "maps", "navigasi" -> {
                currentMode = IslandMode.NAVIGATION
            }
            "music", "musik", "spotify" -> {
                currentMode = IslandMode.MUSIC
            }
            "calculator", "kalkulator" -> {
                // Kalkulator bisa mempertahankan musik atau idle
                if (isPlaying) currentMode = IslandMode.MUSIC
            }
            "phone", "telepon" -> {
                currentMode = IslandMode.CALL
            }
            "ai assistant", "gemini", "translate" -> {
                currentMode = IslandMode.TRANSLATE
            }
            "gofood", "live activities" -> {
                currentMode = IslandMode.LIVE_ACTIVITY
            }
            else -> {
                if (isPlaying) {
                    currentMode = IslandMode.MUSIC
                }
            }
        }
    }

    // Sesuai Instruksi User:
    // "tetapi jika kembali kemenu awal dan ketika play musik maka dynamic bar juga sedang play music dan bisa di play dan stop"
    fun onReturnToHomeScreen() {
        if (isPlaying) {
            currentMode = IslandMode.MUSIC
        } else if (isTimerRunning && timerSeconds > 0) {
            currentMode = IslandMode.TIMER
        } else if (isRecordingActive) {
            currentMode = IslandMode.RECORDING
        }
    }

    private fun sendMediaKeyEvent(context: Context, keyCode: Int) {
        try {
            val am = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            am?.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, keyCode))
            am?.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, keyCode))
        } catch (_: Exception) {}
    }
}
