# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod,SourceFile,LineNumberTable
-keep class kotlin.Metadata { *; }

# Keep BuildConfig fields accessed reflectively (e.g., AdConfig)
-keep class com.example.BuildConfig { *; }

# Data models, domain classes, native alarm receivers/services, and widgets
-keep class com.example.data.model.** { *; }
-keep class com.example.domain.** { *; }
-keep class com.example.native.** { *; }
-keep class com.example.widget.** { *; }

# Moshi reflection and generated JsonAdapters
-keepclassmembers class * extends com.squareup.moshi.JsonAdapter {
    public <init>(com.squareup.moshi.Moshi);
}
-keepclassmembers @com.squareup.moshi.JsonClass class * {
    *;
}
-keepclassmembers enum com.example.** {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Compose Rules
-keep class androidx.compose.** { *; }
-keep class androidx.core.splashscreen.** { *; }

# Room
-keep class androidx.room.** { *; }
-keep class * extends androidx.room.RoomDatabase

# Google Mobile Ads
-keep class com.google.android.gms.ads.** { *; }
-keep class com.google.android.ump.** { *; }

# Keep Coroutines DebugProbes safely optional in release builds
-dontwarn kotlinx.coroutines.debug.**


