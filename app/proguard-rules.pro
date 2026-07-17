# Keep JNI-bound classes/methods
-keepclasseswithmembernames class * {
    native <methods>;
}
-keep class com.aashutarminal.terminal.** { *; }
-keep class com.aashutarminal.tools.Tool { *; }
-dontwarn kotlinx.serialization.**
