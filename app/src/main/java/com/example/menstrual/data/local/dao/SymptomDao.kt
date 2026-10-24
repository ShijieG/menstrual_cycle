package com.example.menstrual.data.local.dao

import androidx.room.*
import com.example.menstrual.data.local.entity.SymptomEntryEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface SymptomDao {
    @Query("SELECT * FROM symptom_entries WHERE date = :date ORDER BY type ASC")
    fun observeByDate(date: LocalDate): Flow<List<SymptomEntryEntity>>

    @Query("SELECT * FROM symptom_entries ORDER BY date ASC")
    suspend fun getAll(): List<SymptomEntryEntity>

    @Query("SELECT * FROM symptom_entries WHERE date BETWEEN :start AND :end ORDER BY date ASC")
    suspend fun getBetween(start: LocalDate, end: LocalDate): List<SymptomEntryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: SymptomEntryEntity): Long

    @Delete
    suspend fun delete(entry: SymptomEntryEntity)

    @Query("DELETE FROM symptom_entries WHERE date = :date AND type = :type AND value = :value")
    suspend fun deleteByKey(date: LocalDate, type: Int, value: String)
}
