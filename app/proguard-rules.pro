# Room
-dontoptimize
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# Unity Ads official keep rules: the SDK uses reflection and a WebView bridge.
-keepattributes SourceFile,LineNumberTable
-keepattributes JavascriptInterface
-keep class android.webkit.JavascriptInterface { *; }
-keep class com.unity3d.ads.** { *; }
-keep class com.unity3d.services.** { *; }
-keep class com.google.android.gms.ads.initialization.** { *; }
-keep class com.google.android.gms.ads.MobileAds { *; }
-dontwarn com.google.ads.mediation.admob.**
-dontwarn com.google.android.gms.ads.**
