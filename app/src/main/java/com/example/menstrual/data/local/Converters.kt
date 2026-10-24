package com.example.menstrual.data.local

import androidx.room.TypeConverter
import java.time.LocalDate

/**
 * Room 类型转换器：LocalDate <-> epoch day (Long)。
 * 仅依赖设备系统日期，无需日历权限。
 */
class Converters {
    @TypeConverter
    fun fromLocalDate(date: LocalDate?): Long? = date?.toEpochDay()

    @TypeConverter
    fun toLocalDate(epochDay: Long?): LocalDate? = epochDay?.let { LocalDate.ofEpochDay(it) }
}
