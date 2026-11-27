# PULSE Smart Gym — R8 / ProGuard Release Rules

# Preserve Kotlinx Serialization metadata & generated serializers for Navigation 3 NavKeys
-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod
-keep @kotlinx.serialization.Serializable class com.ayasheta.pulsesmartgym.** { *; }
-keepclassmembers class com.ayasheta.pulsesmartgym.** {
    *** Companion;
}
-keepclasseswithmembers class com.ayasheta.pulsesmartgym.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keepclassmembers class com.ayasheta.pulsesmartgym.**$$serializer {
    *;
}

# Preserve Room entities, DAOs, and Database implementations
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }

# Keep BLE / ParcelUuid / GATT callbacks intact
-keepclassmembers class * extends android.bluetooth.BluetoothGattCallback { *; }
-keepclassmembers class * extends android.bluetooth.le.ScanCallback { *; }
