package com.example.menstrual.ui.prediction

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.collectAsState
import com.example.menstrual.domain.model.Confidence
import com.example.menstrual.domain.model.Prediction
import com.example.menstrual.domain.model.PredictionResult
import com.example.menstrual.ui.AppViewModel
import com.example.menstrual.ui.theme.Rose100
import com.example.menstrual.ui.theme.Rose400
import com.example.menstrual.ui.theme.Rose600
import com.example.menstrual.ui.theme.Teal400
import com.example.menstrual.ui.theme.Teal600
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlin.math.roundToLong

private val FMT = DateTimeFormatter.ofPattern("M月d日")
private val FMT_FULL = DateTimeFormatter.ofPattern("yyyy年M月d日")

@Composable
fun PredictionScreen(vm: AppViewModel) {
    val result by vm.predictionsFlow.collectAsState()
    val today = LocalDate.now()
    val upcoming = result.predictions.filter { !it.predictedEnd.isBefore(today) }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("未来经期预测", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Card(colors = CardDefaults.cardColors(containerColor = Rose100)) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("平均周期 ${"%.1f".format(result.avgCycle)} 天，平均经期 ${"%.1f".format(result.avgPeriod)} 天，已统计 ${result.cycleCount} 个周期。")
                if (result.isDefault) {
                    Text("历史数据不足，已使用医学默认周期（28天/5天），记录 2 个以上完整周期后预测更精准。", color = MaterialTheme.colorScheme.tertiary)
                }
            }
        }

        if (upcoming.isEmpty()) {
            Text("暂无可预测周期，请先在「日历」中点击日期标记经期。")
        } else {
            upcoming.forEach { PredictionCard(it, result, today) }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "图例：红色=经期，青色=排卵期（含排卵日），其余为卵泡期/黄体期。下方浅红条为 80% 概率区间，深色竖线为预计来潮日。",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun PredictionCard(p: Prediction, result: PredictionResult, today: LocalDate) {
    val days = ChronoUnit.DAYS.between(today, p.predictedStart)
    val dayHint = when {
        days > 0 -> "还有 ${days} 天"
        days == 0L -> "就是今天"
        else -> "进行中"
    }

    Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(2.dp)) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Column {
                    Text("第 ${p.index} 次预测", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(dayHint, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                ConfidenceChip(p.confidence)
            }

            // 预计日期
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text("预计开始", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(p.predictedStart.format(FMT_FULL), style = MaterialTheme.typography.titleMedium, color = Rose600, fontWeight = FontWeight.Bold)
            }

            // 周期时间轴
            CycleTimeline(p, result.avgCycle)

            // 关键日期
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text("经期：${p.predictedStart.format(FMT)} ~ ${p.predictedEnd.format(FMT)}", style = MaterialTheme.typography.labelMedium)
            }
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text("排卵日：${p.ovulationDate.format(FMT)}", color = Teal600, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                Text("易孕窗：${p.fertileStart.format(FMT)} ~ ${p.fertileEnd.format(FMT)}", color = Teal400, style = MaterialTheme.typography.labelMedium)
            }

            // 80% 概率区间
            Text("80% 概率区间", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            WindowBar(p)
            Text(
                "${p.window80.start.format(FMT)} ~ ${p.window80.endInclusive.format(FMT)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun CycleTimeline(p: Prediction, avgCycle: Double) {
    val periodLen = (p.predictedEnd.toEpochDay() - p.predictedStart.toEpochDay() + 1).toInt().coerceAtLeast(1)
    val nextStart = p.predictedStart.plusDays(avgCycle.roundToLong().coerceAtLeast(periodLen.toLong()))
    val follicular = (p.fertileStart.toEpochDay() - p.predictedEnd.toEpochDay() - 1).toInt().coerceAtLeast(0)
    val ovul = (p.fertileEnd.toEpochDay() - p.fertileStart.toEpochDay() + 1).toInt().coerceAtLeast(1)
    val luteal = (nextStart.toEpochDay() - p.fertileEnd.toEpochDay() - 1).toInt().coerceAtLeast(0)
    val total = (periodLen + follicular + ovul + luteal).toFloat().coerceAtLeast(1f)

    Row(
        modifier = Modifier.fillMaxWidth().height(26.dp).clip(RoundedCornerShape(6.dp)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Segment(periodLen.toFloat(), total, Rose400, "经期", showLabel = periodLen.toFloat() / total >= 0.14f)
        Segment(follicular.toFloat(), total, Rose100, "", showLabel = false)
        Segment(ovul.toFloat(), total, Teal400, "排卵", showLabel = ovul.toFloat() / total >= 0.12f)
        Segment(luteal.toFloat(), total, MaterialTheme.colorScheme.surfaceVariant, "", showLabel = false)
    }
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        LegendDot(Rose400, "经期")
        LegendDot(Rose100, "卵泡期")
        LegendDot(Teal400, "排卵期")
        LegendDot(MaterialTheme.colorScheme.surfaceVariant, "黄体期")
    }
}

@Composable
fun Segment(weight: Float, total: Float, color: Color, label: String, showLabel: Boolean) {
    if (weight <= 0f) return
    Box(
        modifier = Modifier.weight(weight.coerceAtLeast(0.02f)).fillMaxSize()
            .background(color)
            .then(if (label.isEmpty()) Modifier else Modifier),
        contentAlignment = Alignment.Center
    ) {
        if (showLabel) {
            Text(label, color = if (color == Rose100) Rose600 else Color.White, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(10.dp).clip(RoundedCornerShape(3.dp)).background(color))
        Spacer(modifier = Modifier.width(3.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun WindowBar(p: Prediction) {
    val start = p.window80.start
    val end = p.window80.endInclusive
    val total = (end.toEpochDay() - start.toEpochDay() + 1).toDouble().coerceAtLeast(1.0)
    val peak = ((p.predictedStart.toEpochDay() - start.toEpochDay()).toDouble() / total).toFloat().coerceIn(0f, 1f)

    Box(
        modifier = Modifier.fillMaxWidth().height(14.dp).clip(RoundedCornerShape(7.dp)).background(Rose100)
    ) {
        Row(modifier = Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
            Spacer(modifier = Modifier.weight(peak))
            Box(modifier = Modifier.width(3.dp).fillMaxSize().background(Rose600))
            Spacer(modifier = Modifier.weight((1f - peak).coerceAtLeast(0.001f)))
        }
    }
}

@Composable
fun ConfidenceChip(c: Confidence) {
    val (label, bg, fg) = when (c) {
        Confidence.HIGH -> Triple("高置信", Color(0xFFEAF3DE), Color(0xFF2F6B3F))
        Confidence.MEDIUM -> Triple("中置信", Color(0xFFFAEEDA), Color(0xFF8A5A12))
        Confidence.LOW -> Triple("低置信", Color(0xFFF1EFE8), Color(0xFF5A5550))
    }
    Box(modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(bg).padding(horizontal = 10.dp, vertical = 4.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = fg)
    }
}
