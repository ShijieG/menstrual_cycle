package com.example.menstrual.domain.usecase

import com.example.menstrual.domain.model.Confidence
import com.example.menstrual.domain.model.Cycle
import com.example.menstrual.domain.model.Prediction
import com.example.menstrual.domain.model.PredictionResult
import com.example.menstrual.domain.util.Statistics
import java.time.LocalDate
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.round
import kotlin.math.roundToLong

/**
 * 核心预测用例：根据历史经期，预测未来 [horizonMonths] 个月内每一次月经的开始/结束，
 * 并给出概率（置信等级 + 80% 置信区间）以及排卵期 / 易孕日。
 *
 * 算法：
 *  1. 由相邻开始日求周期长度序列，统计均值 μ_c 与标准差 σ_c；
 *  2. 由已结束经期求经期长度序列，统计 μ_p 与 σ_p；
 *  3. 历史不足（<2 个完整区间）时回落医学默认 28 天 / 5 天，并标记为低置信；
 *  4. 从最后一次记录起，按 μ_c 递推未来各周期起点，套高斯分布给出概率窗口。
 */
class PredictCyclesUseCase {

    operator fun invoke(
        episodes: List<Cycle>,
        defaultCycle: Int,
        defaultPeriod: Int,
        horizonMonths: Int = 36,
        today: LocalDate = LocalDate.now()
    ): PredictionResult {
        val sorted = episodes.sortedBy { it.startDate }
        val starts = sorted.map { it.startDate }

        val intervals = starts.zipWithNext { a, b -> (b.toEpochDay() - a.toEpochDay()).toDouble() }
        val completed = sorted.filter { it.endDate != null }
        val lengths = completed.mapNotNull { it.length?.toDouble() }

        val (muC, sigC, isDefault) = if (intervals.size >= 2) {
            val m = Statistics.mean(intervals)
            Triple(m, Statistics.std(intervals, m), false)
        } else {
            Triple(defaultCycle.toDouble(), 3.0, true)
        }

        val (muP, sigP) = if (lengths.isNotEmpty()) {
            val m = Statistics.mean(lengths)
            Pair(m, Statistics.std(lengths, m))
        } else {
            Pair(defaultPeriod.toDouble(), 1.0)
        }

        val lastStart = starts.maxOrNull() ?: today
        val horizonEnd = today.plusMonths(horizonMonths.toLong())
        val half = Statistics.ci80HalfWidth(sigC)

        val predictions = mutableListOf<Prediction>()
        var k = 1
        var cursor = lastStart.plusDays(round(muC).roundToLong())
        while (cursor <= horizonEnd) {
            val start = cursor
            val end = start.plusDays(maxOf(1L, round(muP).roundToLong()) - 1)
            val windowStart = start.plusDays(floor(-half).toLong())
            val windowEnd = start.plusDays(ceil(half).toLong())
            val ovulation = start.minusDays(14)
            val fertileStart = ovulation.minusDays(5)
            val fertileEnd = ovulation.plusDays(1)

            val confidence = when {
                !isDefault && intervals.size >= 6 && sigC <= 3.0 -> Confidence.HIGH
                !isDefault && intervals.size >= 2 -> Confidence.MEDIUM
                else -> Confidence.LOW
            }

            predictions.add(
                Prediction(
                    index = k,
                    predictedStart = start,
                    predictedEnd = end,
                    confidence = confidence,
                    probabilityAtStart = confidenceFactor(confidence),
                    window80 = windowStart..windowEnd,
                    ovulationDate = ovulation,
                    fertileStart = fertileStart,
                    fertileEnd = fertileEnd,
                    isDefault = isDefault
                )
            )
            cursor = cursor.plusDays(round(muC).roundToLong())
            k++
        }

        return PredictionResult(
            predictions = predictions,
            isDefault = isDefault,
            avgCycle = muC,
            avgPeriod = muP,
            sigmaCycle = sigC,
            cycleCount = intervals.size
        )
    }

    /** 某天处于经期的概率（取所有预测周期高斯最大值，0~1），供日历着色使用。 */
    fun dayProbability(day: LocalDate, result: PredictionResult): Double {
        if (result.predictions.isEmpty()) return 0.0
        val sigma = if (result.sigmaCycle > 0.0) result.sigmaCycle else 2.0
        var max = 0.0
        for (p in result.predictions) {
            val span = (p.predictedEnd.toEpochDay() - p.predictedStart.toEpochDay()).toDouble()
            val center = p.predictedStart.toEpochDay() + span / 2.0
            val d = day.toEpochDay() - center
            val v = Statistics.gaussian(d.toDouble(), 0.0, sigma + 1.5)
            if (v > max) max = v
        }
        return max.coerceIn(0.0, 1.0)
    }

    private fun confidenceFactor(c: Confidence): Double = when (c) {
        Confidence.HIGH -> 0.9
        Confidence.MEDIUM -> 0.7
        Confidence.LOW -> 0.4
    }
}
