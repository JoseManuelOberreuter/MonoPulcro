-keep class com.josem.monopulcro.widget.** { *; }
-keepclassmembers class * extends androidx.glance.appwidget.GlanceAppWidget { *; }
-keepclassmembers class * extends androidx.glance.appwidget.GlanceAppWidgetReceiver { *; }

# Gson: preservar clases de datos serializadas
-keep class com.josem.monopulcro.data.Task { *; }
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn sun.misc.**
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer
