# Proguard rules for Hindi Movies & South Dubbed

# Keep Room database entities and DAOs
-keep class androidx.room.** { *; }
-dontwarn androidx.room.**

# Keep kotlinx.serialization models
-keepattributes *Annotation*,InnerClasses
-dontnote kotlinx.serialization.SerializationKt
-keepclassmembers class * {
    *** Companion;
}
-keepclasseswithmembers class * {
    kotlinx.serialization.KSerializer serializer();
}

# Pierfrancesco Soffritti YouTube Player
-keep class com.pierfrancescosoffritti.androidyoutubeplayer.** { *; }
-dontwarn com.pierfrancescosoffritti.androidyoutubeplayer.**

# Coil
-keep class coil.** { *; }
-dontwarn coil.**

# Keep app data models and local Room entities intact
-keep class com.hindimovies.app.data.model.** { *; }
-keep class com.hindimovies.app.data.local.** { *; }

