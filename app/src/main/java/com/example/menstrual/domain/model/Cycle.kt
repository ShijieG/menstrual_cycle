package com.example.menstrual.domain.model

import java.time.LocalDate

/** 领域模型：一次经期（与持久化实体解耦）。 */
data class Cycle(
    val startDate: LocalDate,
    val endDate: LocalDate?,
    val flowLevel: Int = 2,
    val note: String = ""
) {
    /** 经期天数（结束日 - 开始日 + 1）；进行中返回 null。 */
    val length: Int?
        get() = endDate?.let { it.toEpochDay().toInt() - startDate.toEpochDay().toInt() + 1 }
}
