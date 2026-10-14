# ProGuard Rules — iOS 27 Spatial Launcher (com.rafio.launcher27)
# Jaga semua anotasi Compose agar tidak terhapus
-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod

# Jaga Activity & BroadcastReceiver
-keep class com.rafio.launcher27.** { *; }
-keep class * extends android.content.BroadcastReceiver { *; }
-keep class * extends androidx.activity.ComponentActivity { *; }

# Compose — jangan hapus class composable
-keep @androidx.compose.runtime.Composable class * { *; }
-keepclassmembers class * {
    @androidx.compose.runtime.Composable *;
    @androidx.compose.runtime.Immutable *;
    @androidx.compose.runtime.Stable *;
}

# JSON parsing
-keepclassmembers class org.json.** { *; }
-dontwarn org.json.**

# Suppress peringatan umum
-dontwarn java.lang.invoke.**
-dontwarn javax.annotation.**
-dontwarn kotlin.reflect.**
