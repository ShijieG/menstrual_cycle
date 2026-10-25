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
 *  1. 统计窗口限定为【过去 windowMonths(默认 36) 个月】内的记录（仅 today 及之前）；
 *  2. 由窗口内相邻开始日求周期长度序列，统计均值 μ_c 与标准差 σ_c；
 *  3. 由窗口内已结束经期求经期长度序列，统计 μ_p 与 σ_p；
 *  4. 窗口内数据不足（<2 个完整区间）时回落医学默认 28 天 / 5 天，并标记为低置信；
 *  5. 预测窗口为【未来 horizonMonths(默认 36) 个月】：从最后一次记录的下一次月经起，
 *     按 μ_c 递推各周期起点（仅生成 today 之后的预测），套高斯分布给出概率窗口。
 */
class PredictCyclesUseCase {

    operator fun invoke(
        episodes: List<Cycle>,
        defaultCycle: Int,
        defaultPeriod: Int,
        horizonMonths: Int = 36,
        windowMonths: Int = 36,
        today: LocalDate = LocalDate.now()
    ): PredictionResult {
        // 仅纳入“今天及之前”的记录，并限定到【过去 windowMonths 个月】作为统计窗口
        val sorted = episodes.filter { !it.startDate.isAfter(today) }.sortedBy { it.startDate }
        val windowStart = today.minusMonths(windowMonths.toLong())
        val inWindow = sorted.filter { !it.startDate.isBefore(windowStart) }

        // 统计仅基于过去 windowMonths 个月内的记录
        val starts = inWindow.map { it.startDate }
        val intervals = starts.zipWithNext { a, b -> (b.toEpochDay() - a.toEpochDay()).toDouble() }
        val completed = inWindow.filter { it.endDate != null }
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
        // 从最后一次记录的下一次月经起推算，并保证只预测“今天之后”的月经（跳过已过去的推算点）
        var cursor = lastStart.plusDays(round(muC).roundToLong())
        while (cursor < today) cursor = cursor.plusDays(round(muC).roundToLong())
        while (cursor <= horizonEnd) {
            val start = cursor
            val end = start.plusDays(maxOf(1L, round(muP).roundToLong()) - 1)
            val windowStart = start.plusDays(floor(-half).toLong())
            val windowEnd = start.plusDays(ceil(half).toLong())
            val ovulation = start.minusDays(14)
            // 易孕窗：偏保守地放宽——排卵前 6 天到排卵后 2 天（含精子存活与卵子存活的缓冲）
            val fertileStart = ovulation.minusDays(6)
            val fertileEnd = ovulation.plusDays(2)

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
