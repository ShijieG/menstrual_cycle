package com.example.menstrual.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.collectAsState
import com.example.menstrual.data.local.entity.PeriodEpisodeEntity
import com.example.menstrual.domain.model.PredictionResult
import com.example.menstrual.domain.usecase.PredictCyclesUseCase
import com.example.menstrual.ui.AppViewModel
import com.example.menstrual.ui.isRecordedDay
import com.example.menstrual.ui.theme.Rose100
import com.example.menstrual.ui.theme.Rose400
import com.example.menstrual.ui.theme.Rose600
import com.example.menstrual.ui.theme.Rose800
import com.example.menstrual.ui.theme.Teal400
import com.example.menstrual.ui.theme.Teal600
import java.time.LocalDate

private val predictor = PredictCyclesUseCase()

@Composable
fun CalendarScreen(vm: AppViewModel) {
    val episodes by vm.episodes.collectAsState()
    val predictions by vm.predictionsFlow.collectAsState()
    var monthOffset by remember { mutableStateOf(0) }
    val today = LocalDate.now()
    val base = today.withDayOfMonth(1).plusMonths(monthOffset.toLong())

    val leading = (base.dayOfWeek.value - 1 + 7) % 7
    val daysInMonth = base.lengthOfMonth()
    val cells = mutableListOf<LocalDate?>()
    repeat(leading) { cells.add(null) }
    for (d in 1..daysInMonth) cells.add(base.plusDays((d - 1).toLong()))
    while (cells.size % 7 != 0) cells.add(null)

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(onClick = { monthOffset-- }) { Icon(Icons.Filled.ChevronLeft, "上月") }
            Text("${base.year}年${base.monthValue}月", style = MaterialTheme.typography.titleMedium)
            IconButton(onClick = { monthOffset++ }) { Icon(Icons.Filled.ChevronRight, "下月") }
        }

        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
            listOf("一", "二", "三", "四", "五", "六", "日").forEach { w ->
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Text(w, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        cells.chunked(7).forEach { week ->
            Row(modifier = Modifier.fillMaxWidth()) {
                week.forEach { day ->
                    Box(modifier = Modifier.weight(1f).padding(2.dp), contentAlignment = Alignment.Center) {
                        if (day != null) DayCell(day, episodes, predictions, today)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.size(12.dp))
        Legend()
        Spacer(modifier = Modifier.size(8.dp))
        Text(
            "提示：颜色越深表示当天来月经的概率越高；青色圆点为排卵日，青色小点为易孕日。",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun DayCell(day: LocalDate, episodes: List<PeriodEpisodeEntity>, predictions: PredictionResult, today: LocalDate) {
    val recorded = isRecordedDay(day, episodes)
    val prob = if (recorded) 1.0 else predictor.dayProbability(day, predictions)
    val fertile = predictions.predictions.any { !day.isBefore(it.fertileStart) && !day.isAfter(it.fertileEnd) }
    val ovulation = predictions.predictions.any { it.ovulationDate == day }

    val bg = when {
        recorded -> Rose400
        prob >= 0.08 -> lerp(Rose100, Rose600, prob.toFloat())
        else -> MaterialTheme.colorScheme.surface
    }
    val textColor = if (recorded) Color.White else Rose800

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .border(
                width = if (day == today) 2.dp else 0.dp,
                color = if (day == today) MaterialTheme.colorScheme.primary else Color.Transparent,
                shape = RoundedCornerShape(8.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(day.dayOfMonth.toString(), color = textColor, style = MaterialTheme.typography.bodyMedium)
        if (fertile || ovulation) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 4.dp)
                    .size(if (ovulation) 8.dp else 5.dp)
                    .clip(CircleShape)
                    .background(if (ovulation) Teal600 else Teal400)
            )
        }
    }
}

@Composable
fun Legend() {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        LegendItem(Rose400, "已记录经期")
        LegendItem(Rose100, "预测（浅→深=概率低→高）")
        LegendItem(Teal400, "易孕/排卵")
    }
}

@Composable
fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(end = 12.dp)) {
        Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(color))
        Spacer(modifier = Modifier.size(4.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Normal)
    }
}
