package com.example.menstrual.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.collectAsState
import com.example.menstrual.data.local.entity.PeriodEpisodeEntity
import com.example.menstrual.data.local.entity.SymptomEntryEntity
import com.example.menstrual.domain.model.PredictionResult
import com.example.menstrual.domain.usecase.PredictCyclesUseCase
import com.example.menstrual.ui.AppViewModel
import com.example.menstrual.ui.AppViewModel.Companion.SEX_TYPE
import com.example.menstrual.ui.AppViewModel.Companion.SYMPTOM_TYPE
import com.example.menstrual.ui.isRecordedDay
import com.example.menstrual.ui.theme.Rose100
import com.example.menstrual.ui.theme.Rose400
import com.example.menstrual.ui.theme.Rose600
import com.example.menstrual.ui.theme.Rose800
import com.example.menstrual.ui.theme.Teal400
import com.example.menstrual.ui.theme.Teal600
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val predictor = PredictCyclesUseCase()
private val DATE_FMT = DateTimeFormatter.ofPattern("yyyy年M月d日")
private val WEEK_FMT = DateTimeFormatter.ofPattern("E")
private val SYMPTOMS = listOf("腹痛", "腰酸", "头痛", "乳房胀痛", "疲劳", "情绪波动")

@Composable
fun CalendarScreen(vm: AppViewModel) {
    val episodes by vm.episodes.collectAsState()
    val predictions by vm.predictionsFlow.collectAsState()
    var monthOffset by remember { mutableStateOf(0) }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }

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
                Box(modifier = Modifier.fillMaxWidth(1f), contentAlignment = Alignment.Center) {
                    Text(w, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        cells.chunked(7).forEach { week ->
            Row(modifier = Modifier.fillMaxWidth()) {
                week.forEach { day ->
                    Box(modifier = Modifier.fillMaxWidth(1f).padding(2.dp), contentAlignment = Alignment.Center) {
                        if (day != null) DayCell(
                            day = day,
                            episodes = episodes,
                            predictions = predictions,
                            today = today,
                            selected = day == selectedDate,
                            onSelect = { selectedDate = day }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.size(10.dp))
        Legend()
        Spacer(modifier = Modifier.size(6.dp))
        Text(
            "点击任意日期可查看/编辑当天记录（经期开始结束、症状、性生活）。颜色越深表示来潮概率越高；青色圆点为排卵日，青色小点为易孕日。",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.size(14.dp))
        DayDetail(vm = vm, date = selectedDate, episodes = episodes, predictions = predictions)
    }
}

@Composable
fun DayCell(
    day: LocalDate,
    episodes: List<PeriodEpisodeEntity>,
    predictions: PredictionResult,
    today: LocalDate,
    selected: Boolean,
    onSelect: () -> Unit
) {
    val recorded = isRecordedDay(day, episodes)
    val prob = if (recorded) 1.0 else predictor.dayProbability(day, predictions)
    val fertile = predictions.predictions.any { !day.isBefore(it.fertileStart) && !day.isAfter(it.fertileEnd) }
    val ovulation = predictions.predictions.any { it.ovulationDate == day }

    val bg = when {
        recorded -> Rose400
        prob >= 0.05 -> lerp(Rose100, Rose600, prob.toFloat().coerceIn(0f, 1f))
        else -> MaterialTheme.colorScheme.surface
    }
    val textColor = if (recorded) Color.White else Rose800

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .then(
                if (selected) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp))
                else Modifier
            )
            .then(
                if (day == today && !selected) Modifier.border(2.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                else Modifier
            )
            .clickable { onSelect() },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                day.dayOfMonth.toString(),
                color = textColor,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (recorded || selected) FontWeight.Bold else FontWeight.Normal
            )
            if (!recorded && prob >= 0.05) {
                Text(
                    "%.0f%%".format(prob * 100),
                    color = textColor.copy(alpha = 0.85f),
                    style = MaterialTheme.typography.labelSmall,
                    textAlign = TextAlign.Center
                )
            }
        }
        if (ovulation) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 3.dp)
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(Teal600)
            )
        } else if (fertile) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 4.dp)
                    .size(5.dp)
                    .clip(CircleShape)
                    .background(Teal400)
            )
        }
    }
}

@Composable
fun DayDetail(vm: AppViewModel, date: LocalDate, episodes: List<PeriodEpisodeEntity>, predictions: PredictionResult) {
    val symptoms by vm.symptomsForDate(date).collectAsState(initial = emptyList())
    val today = LocalDate.now()
    val episode = episodes.find { !date.isBefore(it.startDate) && !date.isAfter(it.endDate ?: today) }
    val recorded = episode != null
    val sex = symptoms.any { it.type == SEX_TYPE }
    val selectedSymptoms = symptoms.filter { it.type == SYMPTOM_TYPE }
    val prob = if (recorded) 1.0 else predictor.dayProbability(date, predictions)
    val matched = predictions.predictions.firstOrNull {
        !date.isBefore(it.window80.start) && !date.isAfter(it.window80.endInclusive)
    }

    Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(2.dp)) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Column {
                    Text(date.format(DATE_FMT), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(date.format(WEEK_FMT), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                // 状态摘要
                val status = when {
                    recorded -> "经期"
                    sex -> "有性生活"
                    else -> "无特殊记录"
                }
                val statusColor = when {
                    recorded -> Rose400
                    sex -> Teal400
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
                Text(status, color = statusColor, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            }

            // ---- 经期操作 ----
            Card(colors = CardDefaults.cardColors(containerColor = Rose100), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("经期记录", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    if (recorded) {
                        val dayNum = (date.toEpochDay() - episode!!.startDate.toEpochDay() + 1).toInt().coerceAtLeast(1)
                        val ongoing = episode!!.endDate == null
                        Text(
                            if (ongoing) "经期进行中（开始于 ${episode!!.startDate.format(DATE_FMT)}）· 第 ${dayNum} 天"
                            else "已记录经期 · 第 ${dayNum} 天",
                            color = Rose400
                        )
                        Button(onClick = { vm.endPeriodOn(date) }, modifier = Modifier.fillMaxWidth()) {
                            Text("设为经期于此日结束")
                        }
                        OutlinedButton(onClick = { vm.deleteEpisode(episode!!) }, modifier = Modifier.fillMaxWidth()) {
                            Text("删除这条经期")
                        }
                    } else {
                        val isFuture = date.isAfter(today)
                        Text(
                            if (isFuture) "未来的日期：可回溯标记为经期开始" else "这一天未记录为经期",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(onClick = { vm.startPeriodOn(date) }, modifier = Modifier.fillMaxWidth()) {
                            Text("标记为经期开始于这天")
                        }
                    }
                }
            }

            // ---- 症状 ----
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("经期症状", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                if (selectedSymptoms.isEmpty()) {
                    Text("暂无记录", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SYMPTOMS.forEach { opt ->
                        val sel = selectedSymptoms.any { it.value == opt }
                        SymptomChip(selected = sel, label = opt) { vm.toggleSymptom(date, SYMPTOM_TYPE, opt, 1) }
                    }
                }
            }

            // ---- 性生活 ----
            Card(
                colors = CardDefaults.cardColors(containerColor = if (sex) Teal400.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth().clickable { vm.toggleSex(date) }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth().padding(12.dp)
                ) {
                    Text("性生活", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text(
                        if (sex) "✓ 已记录：有" else "点击记录：有",
                        color = if (sex) Teal600 else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // ---- 预测信息 ----
            if (!date.isBefore(today) || matched != null) {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("预测信息", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("这一天来月经的概率", style = MaterialTheme.typography.labelMedium)
                            Text("%.0f%%".format(prob * 100), color = Rose400, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        }
                        Box(
                            modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp))
                                .background(Rose100)
                        ) {
                            Box(
                                modifier = Modifier.fillMaxWidth((prob.toFloat()).coerceIn(0f, 1f)).height(8.dp)
                                    .clip(RoundedCornerShape(4.dp)).background(Rose400)
                            )
                        }
                        if (matched != null) {
                            Text(
                                "落在第 ${matched.index} 次预测的 80% 概率区间内（${matched.window80.start.format(DATE_FMT)} ~ ${matched.window80.endInclusive.format(DATE_FMT)}）",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else if (date.isAfter(today)) {
                            Text("暂无明确预测（可能超出已推算范围）", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SymptomChip(selected: Boolean, label: String, onClick: () -> Unit) {
    val bg = if (selected) Rose400 else Color.Transparent
    val fg = if (selected) Color.White else Rose400
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .then(if (!selected) Modifier.border(1.dp, Rose400, RoundedCornerShape(16.dp)) else Modifier)
            .background(bg)
            .padding(horizontal = 12.dp, vertical = 7.dp)
    ) {
        Text(label, color = fg, style = MaterialTheme.typography.labelMedium)
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
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(end = 10.dp)) {
        Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(color))
        Spacer(modifier = Modifier.size(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Normal)
    }
}
