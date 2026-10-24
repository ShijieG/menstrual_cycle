package com.example.menstrual.ui.prediction

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import com.example.menstrual.ui.AppViewModel
import com.example.menstrual.ui.theme.Rose100
import com.example.menstrual.ui.theme.Teal400
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val FMT = DateTimeFormatter.ofPattern("yyyy/M/d")

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
            Text("暂无可预测周期，请先在「记录」中标记经期。")
        } else {
            upcoming.forEach { PredictionCard(it) }
        }
    }
}

@Composable
fun PredictionCard(p: Prediction) {
    Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(2.dp)) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text("第 ${p.index} 次预测", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                ConfidenceChip(p.confidence)
            }
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text("预估开始：${p.predictedStart.format(FMT)}")
                Text("预估结束：${p.predictedEnd.format(FMT)}")
            }
            Text("80% 概率区间：${p.window80.start.format(FMT)} ~ ${p.window80.endInclusive.format(FMT)}", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text("排卵日：${p.ovulationDate.format(FMT)}", color = Teal400)
                Text("易孕窗：${p.fertileStart.format(FMT)} ~ ${p.fertileEnd.format(FMT)}", color = Teal400)
            }
        }
    }
}

@Composable
fun ConfidenceChip(c: Confidence) {
    val (label, bg) = when (c) {
        Confidence.HIGH -> "高置信" to Color(0xFFEAF3DE)
        Confidence.MEDIUM -> "中置信" to Color(0xFFFAEEDA)
        Confidence.LOW -> "低置信" to Color(0xFFF1EFE8)
    }
    Box(modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(bg).padding(horizontal = 10.dp, vertical = 4.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
    }
}
