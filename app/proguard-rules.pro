# Keep Gson TypeToken and generic ReflectiveTypeAdapterFactory
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.google.gson.** { *; }
-keep class * extends com.google.gson.TypeAdapter
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer
-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
# Keep BrewMethodStep for Gson deserialization
-keep class com.coffeelab.coffeenotes.data.entity.BrewMethodStep { *; }
-keep class com.coffeelab.coffeenotes.data.entity.** { *; }
