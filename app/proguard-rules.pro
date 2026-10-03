# Nimbus JOSE JWT
-keep class com.nimbusds.** { *; }
-dontwarn com.nimbusds.**

# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** { kotlinx.serialization.KSerializer serializer(...); }
-keep,includedescriptorclasses class com.evgenykon.travelguide.**$$serializer { *; }
-keepclassmembers class com.evgenykon.travelguide.** { *** Companion; }
-keepclasseswithmembers class com.evgenykon.travelguide.** { kotlinx.serialization.KSerializer serializer(...); }

# Retrofit / OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn retrofit2.**
-keepattributes Signature, Exceptions
