package com.example.menstrual.data.local.dao

import androidx.room.*
import com.example.menstrual.data.local.entity.PeriodEpisodeEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface PeriodDao {
    @Query("SELECT * FROM period_episodes ORDER BY startDate ASC")
    fun observeAll(): Flow<List<PeriodEpisodeEntity>>

    @Query("SELECT * FROM period_episodes ORDER BY startDate ASC")
    suspend fun getAll(): List<PeriodEpisodeEntity>

    @Query("SELECT * FROM period_episodes WHERE endDate IS NULL ORDER BY startDate DESC LIMIT 1")
    suspend fun getOngoing(): PeriodEpisodeEntity?

    @Query("SELECT * FROM period_episodes WHERE startDate <= :end AND (endDate IS NULL OR endDate >= :start) ORDER BY startDate ASC")
    suspend fun getBetween(start: LocalDate, end: LocalDate): List<PeriodEpisodeEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(episode: PeriodEpisodeEntity): Long

    @Update
    suspend fun update(episode: PeriodEpisodeEntity)

    @Delete
    suspend fun delete(episode: PeriodEpisodeEntity)

    @Query("DELETE FROM period_episodes WHERE id = :id")
    suspend fun deleteById(id: Long)
}
