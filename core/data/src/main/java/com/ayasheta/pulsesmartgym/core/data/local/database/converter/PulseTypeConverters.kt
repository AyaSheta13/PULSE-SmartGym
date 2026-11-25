package com.ayasheta.pulsesmartgym.core.data.local.database.converter

import androidx.room.TypeConverter
import com.ayasheta.pulsesmartgym.core.model.device.MeasurementOrigin
import com.ayasheta.pulsesmartgym.core.model.device.RepClassification
import com.ayasheta.pulsesmartgym.core.model.device.TrackingMode
import com.ayasheta.pulsesmartgym.core.model.exercise.EquipmentCategory
import com.ayasheta.pulsesmartgym.core.model.exercise.MuscleGroup
import com.ayasheta.pulsesmartgym.core.model.exercise.TrackingCategory
import com.ayasheta.pulsesmartgym.core.model.session.SessionStatus

class PulseTypeConverters {

    @TypeConverter
    fun fromMuscleGroup(value: MuscleGroup): String = value.name

    @TypeConverter
    fun toMuscleGroup(value: String): MuscleGroup = MuscleGroup.valueOf(value)

    @TypeConverter
    fun fromEquipmentCategory(value: EquipmentCategory): String = value.name

    @TypeConverter
    fun toEquipmentCategory(value: String): EquipmentCategory = EquipmentCategory.valueOf(value)

    @TypeConverter
    fun fromTrackingCategory(value: TrackingCategory): String = value.name

    @TypeConverter
    fun toTrackingCategory(value: String): TrackingCategory = TrackingCategory.valueOf(value)

    @TypeConverter
    fun fromSessionStatus(value: SessionStatus): String = value.name

    @TypeConverter
    fun toSessionStatus(value: String): SessionStatus = SessionStatus.valueOf(value)

    @TypeConverter
    fun fromTrackingMode(value: TrackingMode): String = value.name

    @TypeConverter
    fun toTrackingMode(value: String): TrackingMode = TrackingMode.valueOf(value)

    @TypeConverter
    fun fromMeasurementOrigin(value: MeasurementOrigin): String = value.name

    @TypeConverter
    fun toMeasurementOrigin(value: String): MeasurementOrigin = MeasurementOrigin.valueOf(value)

    @TypeConverter
    fun fromRepClassification(value: RepClassification): String = value.name

    @TypeConverter
    fun toRepClassification(value: String): RepClassification = RepClassification.valueOf(value)
}
