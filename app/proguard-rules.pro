# Room Proguard
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# Jsoup & OkHttp
-keepattributes Signature
-keepattributes *Annotation*
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn org.jsoup.**
