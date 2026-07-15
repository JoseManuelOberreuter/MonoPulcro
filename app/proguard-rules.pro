-keep class com.josem.monopulcro.widget.** { *; }
-keepclassmembers class * extends androidx.glance.appwidget.GlanceAppWidget { *; }
-keepclassmembers class * extends androidx.glance.appwidget.GlanceAppWidgetReceiver { *; }

# ViewModels (evita crash al instanciar en release)
-keep class * extends androidx.lifecycle.ViewModel { *; }
-keep class * extends androidx.lifecycle.AndroidViewModel { *; }

# Gson: preservar clases de datos serializadas
-keep class com.josem.monopulcro.data.Task { *; }
-keep class com.josem.monopulcro.data.DustMote { *; }
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn sun.misc.**
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# AdMob
-keep class com.google.android.gms.ads.** { *; }
-dontwarn com.google.android.gms.ads.**
