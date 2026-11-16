# Ofuscação agressiva (R8). Só manter o que Room, Compose e serialização exigem.
-repackageclasses ftp
-allowaccessmodification
-overloadaggressively
-dontusemixedcaseclassnames
-renamesourcefileattribute SourceFile
-keepattributes RuntimeVisibleAnnotations, AnnotationDefault, InnerClasses, EnclosingMethod, Signature, *Annotation*
-dontnote kotlinx.serialization.AnnotationsKt
-dontwarn org.jetbrains.annotations.**
-dontwarn androidx.room.paging.**

# Remove logs em release
-assumenosideeffects class android.util.Log {
    public static *** v(...);
    public static *** d(...);
    public static *** i(...);
    public static *** w(...);
}
-assumenosideeffects class kotlin.io.ConsoleKt {
    public static *** println(...);
}

# kotlinx.serialization
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.fittrainer.pro.**$$serializer { *; }
-keepclassmembers class com.fittrainer.pro.** {
    *** Companion;
}
-keepclasseswithmembers class com.fittrainer.pro.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep @kotlinx.serialization.Serializable class com.fittrainer.pro.** { *; }

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao interface * { *; }

# FileProvider / manifesto
-keep class androidx.core.content.FileProvider { *; }
-keep class com.fittrainer.pro.FitTrainerApplication { *; }
-keep class com.fittrainer.pro.MainActivity { *; }
