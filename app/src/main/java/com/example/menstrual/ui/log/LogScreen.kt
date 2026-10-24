package com.example.menstrual.ui.log

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.menstrual.data.local.entity.SymptomEntryEntity
import com.example.menstrual.ui.AppViewModel
import com.example.menstrual.ui.theme.Rose400
import com.example.menstrual.ui.theme.Rose100
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val DATE_FMT = DateTimeFormatter.ofPattern("yyyy年M月d日")

private val SYMPTOMS = listOf("腹痛", "腰酸", "头痛", "乳房胀痛", "疲劳", "情绪波动")
private val MOODS = listOf("开心", "平静", "焦虑", "低落", "易怒")
private val DISCHARGES = listOf("干燥", "正常", "增多", "拉丝")

@Composable
fun LogScreen(vm: AppViewModel) {
    var date by remember { mutableStateOf(LocalDate.now()) }
    val ongoing by vm.ongoing.collectAsStateWithLifecycle()
    val symptoms by vm.symptomsForDate(date).collectAsStateWithLifecycle(initialValue = emptyList())

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 日期选择
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            IconButton(onClick = { date = date.minusDays(1) }) { Icon(Icons.Filled.ChevronLeft, "前一天") }
            Text(date.format(DATE_FMT), style = MaterialTheme.typography.titleMedium)
            IconButton(onClick = { date = date.plusDays(1) }) { Icon(Icons.Filled.ChevronRight, "后一天") }
        }

        // 经期开始 / 结束
        Card(colors = CardDefaults.cardColors(containerColor = Rose100), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("经期记录", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                when {
                    ongoing != null && (date.isEqual(ongoing!!.startDate) || date.isAfter(ongoing!!.startDate)) -> {
                        Text("当前经期进行中（开始于 ${ongoing!!.startDate.format(DATE_FMT)}）", color = Rose400)
                        Button(onClick = { vm.endPeriod() }, modifier = Modifier.fillMaxWidth()) {
                            Text("今天结束了")
                        }
                    }
                    date.isEqual(LocalDate.now()) || date.isBefore(LocalDate.now()) -> {
                        Button(onClick = { vm.startPeriod() }, modifier = Modifier.fillMaxWidth()) {
                            Text("今天来月经了")
                        }
                    }
                    else -> {
                        Text("未来的日期：可回溯记录过去的经期", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        OutlinedButton(onClick = { vm.startPeriod(flowLevel = 2) }, modifier = Modifier.fillMaxWidth()) {
                            Text("标记 ${date.format(DATE_FMT)} 为经期开始")
                        }
                    }
                }
            }
        }

        SymptomGroup(title = "症状", options = SYMPTOMS, type = 0, date = date, symptoms = symptoms, vm = vm)
        SymptomGroup(title = "心情", options = MOODS, type = 1, date = date, symptoms = symptoms, vm = vm)
        SymptomGroup(title = "分泌物", options = DISCHARGES, type = 2, date = date, symptoms = symptoms, vm = vm)
    }
}

@Composable
fun SymptomGroup(
    title: String,
    options: List<String>,
    type: Int,
    date: LocalDate,
    symptoms: List<SymptomEntryEntity>,
    vm: AppViewModel
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEach { opt ->
                val selected = symptoms.any { it.type == type && it.value == opt }
                val bg = if (selected) Rose400 else Color.Transparent
                val fg = if (selected) Color.White else Rose400
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { vm.toggleSymptom(date, type, opt, 1) }
                        .then(if (!selected) Modifier.border(1.dp, Rose400, RoundedCornerShape(16.dp)) else Modifier)
                        .background(bg)
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(opt, color = fg)
                }
            }
        }
    }
}
