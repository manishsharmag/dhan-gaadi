# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

-keep class kotlinx.serialization.** { *; }
-keepclassmembers class * {
    @kotlinx.serialization.Serializable *;
}

# Keep our data models so kotlinx.serialization can find them by name
-keep class com.example.haryanaroadways.data.api.** { *; }
-keepclassmembers class com.example.haryanaroadways.data.api.** { *; }

# ZXing barcode scanner
-keep class com.journeyapps.barcodescanner.** { *; }
-dontwarn com.journeyapps.barcodescanner.**
-keep class com.google.zxing.** { *; }
-dontwarn com.google.zxing.**

# OSMDroid map
-keep class org.osmdroid.** { *; }
-dontwarn org.osmdroid.**

# Socket.IO / Engine.IO
-keep class io.socket.** { *; }
-dontwarn io.socket.**
