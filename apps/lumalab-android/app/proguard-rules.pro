# kotlinx.serialization: keep generated serializers for persisted library/edit documents.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class com.lumalab.** {
    *** Companion;
}
-keepclasseswithmembers class com.lumalab.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.lumalab.**$$serializer { *; }
