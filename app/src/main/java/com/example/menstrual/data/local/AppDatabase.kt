package com.example.menstrual.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.menstrual.data.local.dao.PeriodDao
import com.example.menstrual.data.local.dao.SymptomDao
import com.example.menstrual.data.local.entity.PeriodEpisodeEntity
import com.example.menstrual.data.local.entity.SymptomEntryEntity

@Database(
    entities = [PeriodEpisodeEntity::class, SymptomEntryEntity::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun periodDao(): PeriodDao
    abstract fun symptomDao(): SymptomDao

    companion object {
        const val NAME = "menstrual.db"
    }
}
