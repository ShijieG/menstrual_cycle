package com.example.menstrual.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
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
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
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
import com.example.menstrual.ui.theme.Teal100
import com.example.menstrual.ui.theme.Teal300
import com.example.menstrual.ui.theme.Teal400
import com.example.menstrual.ui.theme.Teal600
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val predictor = PredictCyclesUseCase()
private val DATE_FMT = DateTimeFormatter.ofPattern("yyyy年M月d日")
private val WEEK_FMT = DateTimeFormatter.ofPattern("E")
private val SYMPTOMS = listOf("腹痛", "腰酸", "头痛", "乳房胀痛", "疲劳", "情绪波动")

/**
 * 日历页（参考系统日历交互）：
 * 默认整屏只展示月历（格子随屏幕宽度自适应为正方形），
 * 点击任意日期后，才在下方展开当天的记录面板（经期起止、症状、性生活、预测信息）。
 */
@Composable
fun CalendarScreen(vm: AppViewModel) {
    val episodes by vm.episodes.collectAsState()
    val predictions by vm.predictionsFlow.collectAsState()
    var monthOffset by remember { mutableStateOf(0) }
    var selectedDate by remember { mutableStateOf<LocalDate?>(null) }

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
        // ---- 月份切换 ----
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(onClick = { monthOffset-- }) { Icon(Icons.Filled.ChevronLeft, "上月") }
            Text("${base.year}年${base.monthValue}月", style = MaterialTheme.typography.titleMedium)
            IconButton(onClick = { monthOffset++ }) { Icon(Icons.Filled.ChevronRight, "下月") }
        }

        // ---- 月历（确定性布局）：BoxWithConstraints 只负责算格子宽度，内部用 Column 纵向排布各行 ----
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val cell = maxWidth / 7
            Column(modifier = Modifier.fillMaxWidth()) {
                // 星期表头
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    listOf("一", "二", "三", "四", "五", "六", "日").forEach { w ->
                        Box(modifier = Modifier.width(cell), contentAlignment = Alignment.Center) {
                            Text(w, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                // 日期网格：每行一个 Row，纵向依次排列
                cells.chunked(7).forEach { week ->
                    Row(modifier = Modifier.fillMaxWidth()) {
                        week.forEach { day ->
                            Box(
                                modifier = Modifier.width(cell).height(cell).padding(2.dp),
                                contentAlignment = Alignment.Center
                            ) {
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
            }
        }

        // ---- 点击后才展开：图例 + 说明 + 当天记录面板 ----
        val sel = selectedDate
        if (sel == null) {
            Spacer(modifier = Modifier.height(32.dp))
            Text(
                "点击任意日期\n查看 / 记录当天的经期、症状与预测",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            Spacer(modifier = Modifier.height(14.dp))
            Legend()
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                "红色=已记录经期/预测经期（90%~99% 浅红，100% 深红）；浅青色块=易孕日，青色块=排卵日。",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(14.dp))
            DayDetail(vm = vm, date = sel, episodes = episodes, predictions = predictions)
        }
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
    val ovulation = predictions.predictions.any { it.ovulationDate == day }
    // 易孕日 = 落入易孕窗且非排卵日
    val fertile = !ovulation && predictions.predictions.any { !day.isBefore(it.fertileStart) && !day.isAfter(it.fertileEnd) }
    // 预测经期：仅在来潮概率 ≥ 90% 时显示与染色；90%~99% 浅色块，100% 深色块
    val showProb = !recorded && prob >= 0.9
    val probDark = prob >= 0.999

    val bg = when {
        recorded -> Rose400                                                  // 已记录经期：红色块
        ovulation -> Teal300                                                 // 排卵日：浅青色块（比易孕日深一点）
        fertile -> Teal100                                                   // 易孕日：极浅青色块
        showProb && probDark -> Rose600                                      // 预测经期 =100%：深红块
        showProb -> Rose100                                                  // 预测经期 90%~99%：浅红块
        else -> MaterialTheme.colorScheme.surface
    }
    val textColor = when {
        recorded -> Color.White
        showProb && probDark -> Color.White
        ovulation -> Teal800
        fertile -> Teal800
        showProb -> Rose800
        else -> MaterialTheme.colorScheme.onSurface
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .then(
                if (selected) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(10.dp))
                else Modifier
            )
            .then(
                if (day == today && !selected) Modifier.border(2.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(10.dp))
                else Modifier
            )
            .clickable { onSelect() },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                day.dayOfMonth.toString(),
                color = textColor,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = if (recorded || ovulation || fertile || selected) FontWeight.Bold else FontWeight.Normal
            )
            // 预测经期（>90%）显示百分比；排卵/易孕/已记录不显示百分比
            if (showProb) {
                Text(
                    "%.0f%%".format(prob * 100),
                    color = textColor.copy(alpha = 0.9f),
                    style = MaterialTheme.typography.labelSmall,
                    textAlign = TextAlign.Center
                )
            }
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
                        val ep = episode!!
                        val dayNum = (date.toEpochDay() - ep.startDate.toEpochDay() + 1).toInt().coerceAtLeast(1)
                        Text(
                            if (ep.endDate == null)
                                "经期进行中（开始于 ${ep.startDate.format(DATE_FMT)}）· 第 ${dayNum} 天"
                            else
                                "已记录经期 ${ep.startDate.format(DATE_FMT)} ~ ${ep.endDate!!.format(DATE_FMT)} · 第 ${dayNum} 天",
                            color = Rose400
                        )
                        Button(onClick = { vm.setEpisodeEnd(ep, date) }, modifier = Modifier.fillMaxWidth()) {
                            Text("设为经期于此日结束")
                        }
                        OutlinedButton(onClick = { vm.deleteEpisode(ep) }, modifier = Modifier.fillMaxWidth()) {
                            Text("删除这条经期")
                        }
                    } else {
                        val isFuture = date.isAfter(today)
                        Text(
                            if (isFuture) "未来的日期：可预标记为经期开始" else "这一天未记录为经期",
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
                        val selSym = selectedSymptoms.any { it.value == opt }
                        SymptomChip(selected = selSym, label = opt) { vm.toggleSymptom(date, SYMPTOM_TYPE, opt, 1) }
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
        LegendItem(Rose100, "预测经期 90%~99%")
        LegendItem(Rose600, "预测经期 100%")
        LegendItem(Teal300, "排卵日")
        LegendItem(Teal100, "易孕日")
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
