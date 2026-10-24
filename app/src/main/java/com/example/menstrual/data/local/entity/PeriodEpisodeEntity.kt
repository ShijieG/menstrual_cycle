package com.example.menstrual.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

/**
 * 一次经期事件。
 * @param startDate 来月经的第一天（必填）
 * @param endDate   结束日；null 表示「进行中」
 * @param flowLevel 流量强度 1=少 2=中 3=多
 */
@Entity(tableName = "period_episodes")
data class PeriodEpisodeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startDate: LocalDate,
    val endDate: LocalDate?,
    val flowLevel: Int = 2,
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
