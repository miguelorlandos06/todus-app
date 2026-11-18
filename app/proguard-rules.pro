# Hilt
-keep class dagger.hilt.** { *; }
-keep,allowobfuscation,allowshrinking @dagger.hilt.* class *
-keep,allowobfuscation,allowshrinking @javax.inject.* class *

# Kotlinx Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class **$$serializer { *; }
-keepclasseswithmembers,allowshrinking,allowobfuscation class * {
    @kotlinx.serialization.SerialName <fields>;
}
-keep,includedescriptorclasses class cu.todus.android.**$$serializer { *; }
-keepclassmembers class cu.todus.android.** {
    *** Companion;
}

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# Compose
-keep class androidx.compose.runtime.** { *; }

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**

# App
-keep class cu.todus.android.protocol.** { *; }
-keep class cu.todus.android.data.** { *; }
-keep class cu.todus.android.domain.** { *; }
