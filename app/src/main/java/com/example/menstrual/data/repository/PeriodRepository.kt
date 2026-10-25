package com.example.menstrual.data.repository

import com.example.menstrual.data.local.AppDatabase
import com.example.menstrual.data.local.dao.PeriodDao
import com.example.menstrual.data.local.dao.SymptomDao
import com.example.menstrual.data.local.entity.PeriodEpisodeEntity
import com.example.menstrual.data.local.entity.SymptomEntryEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/**
 * 数据仓储：封装 Room DAO，向上暴露 Flow 与挂起操作。
 * 所有日期均取自设备系统日期（LocalDate.now()），无日历集成。
 */
class PeriodRepository(private val db: AppDatabase) {
    private val periodDao: PeriodDao = db.periodDao()
    private val symptomDao: SymptomDao = db.symptomDao()

    val episodes: Flow<List<PeriodEpisodeEntity>> = periodDao.observeAll()
    fun symptomsForDate(date: LocalDate): Flow<List<SymptomEntryEntity>> = symptomDao.observeByDate(date)

    suspend fun getAllEpisodes(): List<PeriodEpisodeEntity> = periodDao.getAll()
    suspend fun getAllSymptoms(): List<SymptomEntryEntity> = symptomDao.getAll()
    suspend fun getEpisodesBetween(start: LocalDate, end: LocalDate): List<PeriodEpisodeEntity> =
        periodDao.getBetween(start, end)

    suspend fun getOngoing(): PeriodEpisodeEntity? = periodDao.getOngoing()

    /**
     * 记录一次经期开始。
     * @param lengthDays 经期时长（天）；提供时自动写 endDate = startDate + lengthDays - 1，
     *                   仅该区间着色，而不是一直"进行中"染到今天。
     */
    suspend fun startPeriod(startDate: LocalDate = LocalDate.now(), flowLevel: Int = 2, lengthDays: Int? = null): Long {
        periodDao.getOngoing()?.let { ongoing ->
            val close = if (startDate.isBefore(ongoing.startDate)) ongoing.startDate else startDate.minusDays(1)
            periodDao.update(ongoing.copy(endDate = close))
        }
        val autoEnd = lengthDays?.let { startDate.plusDays((it - 1).coerceAtLeast(0).toLong()) }
        return periodDao.insert(
            PeriodEpisodeEntity(startDate = startDate, endDate = autoEnd, flowLevel = flowLevel)
        )
    }

    /** 记录「今天结束了」：把进行中的经期结束日设为给定日期（不早于开始日）。 */
    suspend fun endPeriod(date: LocalDate = LocalDate.now()) {
        periodDao.getOngoing()?.let { ongoing ->
            val end = if (date.isBefore(ongoing.startDate)) ongoing.startDate else date
            periodDao.update(ongoing.copy(endDate = end))
        }
    }

    suspend fun addSymptom(entry: SymptomEntryEntity): Long = symptomDao.insert(entry)
    suspend fun removeSymptom(entry: SymptomEntryEntity) = symptomDao.delete(entry)
    suspend fun removeSymptomByKey(date: LocalDate, type: Int, value: String) =
        symptomDao.deleteByKey(date, type, value)

    suspend fun deleteEpisode(episode: PeriodEpisodeEntity) = periodDao.delete(episode)
    suspend fun updateEpisode(episode: PeriodEpisodeEntity) = periodDao.update(episode)
}
