package com.example.menstrual.ui.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import com.example.menstrual.ui.AppViewModel
import com.example.menstrual.ui.theme.Rose400
import com.example.menstrual.ui.theme.Rose100
import kotlin.math.sqrt

@Composable
fun StatsScreen(vm: AppViewModel) {
    val episodes by vm.episodes.collectAsState()
    val sorted = episodes.sortedBy { it.startDate }
    val starts = sorted.map { it.startDate }
    val intervals = starts.zipWithNext { a, b -> (b.toEpochDay() - a.toEpochDay()).toInt() }
    val completed = sorted.filter { it.endDate != null }
    val lengths = completed.mapNotNull { (it.endDate!!.toEpochDay() - it.startDate.toEpochDay() + 1).toInt() }

    val avgCycle = if (intervals.isNotEmpty()) intervals.average() else null
    val sigma = if (intervals.size >= 2) {
        val m = intervals.average()
        sqrt(intervals.sumOf { (it - m) * (it - m) } / (intervals.size - 1))
    } else null
    val avgLen = if (lengths.isNotEmpty()) lengths.average() else null

    val regularity = when {
        sigma == null -> "数据不足"
        sigma <= 3 -> "规律"
        sigma <= 6 -> "基本规律"
        else -> "不规律"
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("周期统计", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard("平均周期", avgCycle?.let { "%.1f 天".format(it) } ?: "—", Modifier.weight(1f))
            StatCard("平均经期", avgLen?.let { "%.1f 天".format(it) } ?: "—", Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard("记录次数", "${intervals.size} 个", Modifier.weight(1f))
            StatCard("规律性", regularity, Modifier.weight(1f))
        }

        if (intervals.isNotEmpty()) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("近期周期长度（天）", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    val max = intervals.maxOf { it }.toFloat().coerceAtLeast(1f)
                    intervals.takeLast(8).forEachIndexed { i, v ->
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            Text("${i + 1}", modifier = Modifier.width(24.dp), style = MaterialTheme.typography.labelMedium)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth((v / max).coerceIn(0.05f, 1f))
                                    .height(18.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Rose400)
                            )
                            Text(" $v", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        } else {
            Text("记录至少 2 个完整周期后即可查看统计与趋势。")
        }
    }
}

@Composable
fun StatCard(title: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier, colors = CardDefaults.cardColors(containerColor = Rose100)) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
    }
}
