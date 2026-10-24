package com.example.menstrual.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.EventRepeat
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.menstrual.MenstrualApplication
import com.example.menstrual.ui.calendar.CalendarScreen
import com.example.menstrual.ui.log.LogScreen
import com.example.menstrual.ui.prediction.PredictionScreen
import com.example.menstrual.ui.settings.SettingsScreen
import com.example.menstrual.ui.stats.StatsScreen
import java.time.LocalDate

enum class Screen(val label: String, val icon: ImageVector) {
    CALENDAR("日历", Icons.Filled.CalendarMonth),
    LOG("记录", Icons.Filled.EditNote),
    PREDICTION("预测", Icons.Filled.EventRepeat),
    STATS("统计", Icons.Filled.BarChart),
    SETTINGS("设置", Icons.Filled.Settings)
}

@Composable
fun MainApp() {
    val app = androidx.compose.ui.platform.LocalContext.current.applicationContext as MenstrualApplication
    val vm: AppViewModel = viewModel(factory = AppViewModelFactory(app))
    var selected by remember { mutableStateOf(Screen.CALENDAR) }

    Scaffold(
        bottomBar = {
            NavigationBar {
                Screen.values().forEach { s ->
                    NavigationBarItem(
                        selected = selected == s,
                        onClick = { selected = s },
                        icon = { Icon(s.icon, contentDescription = s.label) },
                        label = { Text(s.label) }
                    )
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (selected) {
                Screen.CALENDAR -> CalendarScreen(vm)
                Screen.LOG -> LogScreen(vm)
                Screen.PREDICTION -> PredictionScreen(vm)
                Screen.STATS -> StatsScreen(vm)
                Screen.SETTINGS -> SettingsScreen(vm)
            }
        }
    }
}

/** 计算某天是否落在已记录经期内（进行中按结束日=今天）。 */
fun isRecordedDay(day: LocalDate, episodes: List<com.example.menstrual.data.local.entity.PeriodEpisodeEntity>): Boolean {
    val today = LocalDate.now()
    return episodes.any { e ->
        val end = e.endDate ?: today
        !day.isBefore(e.startDate) && !day.isAfter(end)
    }
}
