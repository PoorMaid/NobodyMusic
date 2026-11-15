# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /usr/local/Cellar/android-sdk/24.3.3/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

-keep class com.reactnativenavigation.views.element.animators.** { *; }
# -keepclassmembers class com.reactnativenavigation.views.element.animators.** { *; }


-keep class org.jaudiotagger.tag.** { *; }


-keep public class com.dylanvann.fastimage.* {*;}
-keep public class com.dylanvann.fastimage.** {*;}
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep public class * extends com.bumptech.glide.module.AppGlideModule
-keep public enum com.bumptech.glide.load.ImageHeaderParser$** {
  **[] $VALUES;
  public *;
}

# Keep all cn.toside.music.mobile app classes and native module registrations

-keep class cn.toside.music.mobile.** { *; }
-keepclassmembers class cn.toside.music.mobile.** { *; }
-keep class com.reactnativenavigation.** { *; }
-keepclassmembers class com.reactnativenavigation.** { *; }
-keep class com.whl.quickjs.** { *; }
-keepclassmembers class com.whl.quickjs.** { *; }
-keep class com.masteratul.exceptionhandler.** { *; }

-keep class cn.toside.music.mobile.BootLog { *; }
-dontwarn cn.toside.music.mobile.BootLog
