package com.example.menstrual.domain.model

import java.time.LocalDate

enum class Confidence { HIGH, MEDIUM, LOW }

/**
 * 单次预测周期。
 * @param predictedStart 预估月经开始日
 * @param predictedEnd   预估月经结束日
 * @param confidence     置信等级（基于历史数据量与离散程度）
 * @param probabilityAtStart 起始日附近的代表性概率（0~1，用于列表展示）
 * @param window80       80% 置信区间（开始日的波动范围）
 * @param ovulationDate  预估排卵日（≈开始日前 14 天）
 * @param fertileStart   易孕窗开始（排卵日 -5）
 * @param fertileEnd     易孕窗结束（排卵日 +1）
 * @param isDefault      是否使用医学默认周期（历史数据不足）
 */
data class Prediction(
    val index: Int,
    val predictedStart: LocalDate,
    val predictedEnd: LocalDate,
    val confidence: Confidence,
    val probabilityAtStart: Double,
    val window80: ClosedRange<LocalDate>,
    val ovulationDate: LocalDate,
    val fertileStart: LocalDate,
    val fertileEnd: LocalDate,
    val isDefault: Boolean
)

/** 预测总结果。 */
data class PredictionResult(
    val predictions: List<Prediction>,
    val isDefault: Boolean,
    val avgCycle: Double,
    val avgPeriod: Double,
    val sigmaCycle: Double,
    val cycleCount: Int
)
