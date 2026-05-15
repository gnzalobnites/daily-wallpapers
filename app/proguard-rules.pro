# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# Mantener las firmas genéricas (Crucial para Listas y Gson)
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes EnclosingMethod

# Reglas de seguridad para Gson
-keep class sun.misc.Unsafe { *; }
-keep class com.google.gson.stream.** { *; }
-keep class com.google.gson.** { *; }

# Mantener intactos los modelos de datos de tu app
-keep class com.gnzalobnites.dailywallpapers.data.model.** { *; }

# Reglas para Retrofit y Corrutinas (por prevención)
-keep class retrofit2.** { *; }
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}

# Retrofit + Kotlin suspend functions
-keepattributes Signature, InnerClasses, EnclosingMethod

# Mantener servicios Retrofit
-keep interface com.gnzalobnites.dailywallpapers.data.api.** { *; }

# Mantener metadata Kotlin
-keep class kotlin.Metadata { *; }

# Coroutines
-dontwarn kotlinx.coroutines.**
-keep class kotlinx.coroutines.** { *; }

# Mantener respuestas Retrofit/Gson
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# Evita que R8 rompa tipos genéricos
-if interface * { @retrofit2.http.* <methods>; }
-keep,allowobfuscation interface <1>