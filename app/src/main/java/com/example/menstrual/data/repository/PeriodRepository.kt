package com.example.menstrual.data.repository

import com.example.menstrual.data.local.AppDatabase
import com.example.menstrual.data.local.dao.PeriodDao
import com.example.menstrual.data.local.dao.SymptomDao
import com.example.menstrual.data.local.entity.PeriodEpisodeEntity
import com.example.menstrual.data.local.entity.SymptomEntryEntity
import kotlinx.coroutines.flow.Flow
import org.json.JSONArray
import org.json.JSONObject
import java.io.InputStream
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

    /**
     * 从导出的 JSON 流导入数据（换机迁移用）。
     * 解析 episodes / symptoms 两个数组，批量 REPLACE 写入，不覆盖设备上既有但文件里没有的记录。
     * @return 导入的经期条数与症状条数
     */
    suspend fun importData(input: InputStream): Pair<Int, Int> {
        val text = input.bufferedReader().use { it.readText() }
        val root = JSONObject(text)
        val episodes = mutableListOf<PeriodEpisodeEntity>()
        val symptoms = mutableListOf<SymptomEntryEntity>()
        root.optJSONArray("episodes")?.let { arr: JSONArray ->
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                episodes.add(
                    PeriodEpisodeEntity(
                        id = o.optLong("id", 0L),
                        startDate = LocalDate.parse(o.getString("startDate")),
                        endDate = if (o.isNull("endDate")) null else LocalDate.parse(o.getString("endDate")),
                        flowLevel = o.optInt("flowLevel", 2),
                        note = o.optString("note", "")
                    )
                )
            }
        }
        root.optJSONArray("symptoms")?.let { arr: JSONArray ->
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                symptoms.add(
                    SymptomEntryEntity(
                        id = o.optLong("id", 0L),
                        date = LocalDate.parse(o.getString("date")),
                        type = o.optInt("type"),
                        value = o.optString("value"),
                        intensity = o.optInt("intensity")
                    )
                )
            }
        }
        if (episodes.isNotEmpty()) periodDao.insertAll(episodes)
        if (symptoms.isNotEmpty()) symptomDao.insertAll(symptoms)
        return episodes.size to symptoms.size
    }
}
