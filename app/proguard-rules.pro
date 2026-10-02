-keep class org.mozilla.javascript.** { *; }
-dontwarn org.mozilla.javascript.**
-keep class com.nobodymusic.tyxypoor.source.js.** { *; }
-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**
-keep class kotlinx.serialization.** { *; }
-keepclassmembers class **$$serializer { *; }
-keepclasseswithmembers class * {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.nobodymusic.tyxypoor.**$$serializer { *; }
-keepclassmembers class com.nobodymusic.tyxypoor.** {
    *** Companion;
}
-keepclasseswithmembers class com.nobodymusic.tyxypoor.** {
    kotlinx.serialization.KSerializer serializer(...);
}