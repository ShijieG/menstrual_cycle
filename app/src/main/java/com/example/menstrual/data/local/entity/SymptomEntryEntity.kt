package com.example.menstrual.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

/**
 * 某天的症状 / 性生活记录。
 * @param type 0=症状 3=性生活（心情/分泌物已移除）
 * @param value 名称（如「腹痛」「有」）
 * @param intensity 0~3 程度
 */
@Entity(tableName = "symptom_entries")
data class SymptomEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: LocalDate,
    val type: Int,
    val value: String,
    val intensity: Int = 0
)
