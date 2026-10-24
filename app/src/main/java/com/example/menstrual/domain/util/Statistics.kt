package com.example.menstrual.domain.util

import kotlin.math.exp
import kotlin.math.sqrt

/** 纯 Kotlin 统计工具，可单元测试，不依赖 Android。 */
object Statistics {
    fun mean(xs: List<Double>): Double = if (xs.isEmpty()) 0.0 else xs.sum() / xs.size

    fun std(xs: List<Double>, mu: Double = mean(xs)): Double {
        if (xs.size < 2) return 0.0
        val variance = xs.sumOf { (it - mu) * (it - mu) } / (xs.size - 1)
        return sqrt(variance)
    }

    /** 高斯概率密度（未归一化），sigma<=0 时退化为点对点判断。 */
    fun gaussian(x: Double, mu: Double, sigma: Double): Double {
        if (sigma <= 0.0) return if (kotlin.math.abs(x - mu) < 0.5) 1.0 else 0.0
        return exp(-((x - mu) * (x - mu)) / (2 * sigma * sigma))
    }

    /** 80% 置信区间半宽（正态分布 z≈1.2816）。 */
    fun ci80HalfWidth(sigma: Double): Double = 1.2816 * sigma
}
