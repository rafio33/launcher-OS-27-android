# Proguard rules for iOS 27 Spatial Launcher
-keepattributes *Annotation*
-keepclassmembers class * {
    @androidx.compose.runtime.Immutable *;
    @androidx.compose.runtime.Stable *;
}
-dontwarn java.lang.invoke.**
