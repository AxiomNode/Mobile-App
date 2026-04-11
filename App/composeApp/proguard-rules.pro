# AxiomNode Proguard Rules
# Keep Kotlin serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep app serializable data classes
-keep,includedescriptorclasses class es.sebas1705.axiomnode.**$$serializer { *; }
-keepclassmembers class es.sebas1705.axiomnode.** {
    *** Companion;
}
-keepclasseswithmembers class es.sebas1705.axiomnode.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Ktor
-keep class io.ktor.** { *; }
-dontwarn io.ktor.**

# Room
-keep class androidx.room.** { *; }
-dontwarn androidx.room.**

# Firebase
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**

# Credential Manager
-keep class androidx.credentials.** { *; }
-dontwarn androidx.credentials.**

