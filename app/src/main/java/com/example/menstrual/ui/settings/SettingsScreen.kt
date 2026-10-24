package com.example.menstrual.ui.settings

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.input.KeyboardOptions
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.compose.runtime.collectAsState
import com.example.menstrual.MenstrualApplication
import com.example.menstrual.reminder.ReminderScheduler
import com.example.menstrual.ui.AppViewModel
import com.example.menstrual.ui.theme.Rose100
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(vm: AppViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val defaultCycle by vm.defaultCycleLength.collectAsState()
    val defaultPeriod by vm.defaultPeriodLength.collectAsState()
    val periodReminderOn by vm.periodReminderOn.collectAsState()
    val periodRemindDaysBefore by vm.periodRemindDaysBefore.collectAsState()
    val ovulationReminderOn by vm.ovulationReminderOn.collectAsState()

    val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { granted ->
        val app = context.applicationContext as MenstrualApplication
        if (granted) ReminderScheduler.schedule(app)
    }

    fun toggleReminder(on: Boolean) {
        vm.setPeriodReminderOn(on)
        val app = context.applicationContext as MenstrualApplication
        if (on) {
            if (Build.VERSION.SDK_INT >= 33 &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
            ) {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                ReminderScheduler.schedule(app)
            }
        } else {
            ReminderScheduler.cancel(app)
        }
    }

    fun shareExport() {
        scope.launch {
            val file = vm.export()
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/json"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "导出数据备份"))
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("设置", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("默认周期参数", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                NumberRow("默认周期长度", defaultCycle, 20, 45, "天") { vm.setDefaultCycleLength(it) }
                NumberRow("默认经期长度", defaultPeriod, 2, 15, "天") { vm.setDefaultPeriodLength(it) }
                Text("历史数据不足时使用以上默认值进行预测。", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text("经期来临提醒", style = MaterialTheme.typography.titleMedium)
                    Switch(checked = periodReminderOn, onCheckedChange = { toggleReminder(it) })
                }
                if (periodReminderOn) {
                    NumberRow("提前提醒天数", periodRemindDaysBefore, 0, 7, "天") { vm.setPeriodRemindDaysBefore(it) }
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text("排卵期提醒", style = MaterialTheme.typography.titleMedium)
                    Switch(checked = ovulationReminderOn, onCheckedChange = {
                        vm.setOvulationReminderOn(it)
                        val app = context.applicationContext as MenstrualApplication
                        if (it) ReminderScheduler.schedule(app) else if (!periodReminderOn) ReminderScheduler.cancel(app)
                    })
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Rose100)) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text("数据导出 / 备份", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("将所有经期与症状记录导出为 JSON 文件并分享保存。", style = MaterialTheme.typography.labelMedium)
                Button(onClick = { shareExport() }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                    Text("导出数据")
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text("隐私说明", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    "本应用完全在本地运行：仅使用设备系统日期（无日历权限），数据保存在本机数据库，不联网、不上传任何服务器。提醒仅依赖系统通知。",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/** 数字输入 + 滑块组合：可直接键入数值，也可用滑块微调，二者实时同步。 */
@Composable
fun NumberRow(label: String, value: Int, min: Int, max: Int, unit: String, onSet: (Int) -> Unit) {
    var text by remember(value) { mutableStateOf(value.toString()) }
    LaunchedEffect(value) {
        val parsed = text.toIntOrNull()
        if (parsed != null && parsed != value) text = value.toString()
    }

    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text(label, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        OutlinedTextField(
            value = text,
            onValueChange = { s ->
                val filtered = s.filter { it.isDigit() }
                text = filtered
                val v = filtered.toIntOrNull()
                if (v != null) onSet(v.coerceIn(min, max))
            },
            modifier = Modifier.width(88.dp),
            singleLine = true,
            textStyle = TextStyle(textAlign = TextAlign.Center),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            label = { Text(unit) }
        )
    }
    Slider(
        value = value.toFloat(),
        onValueChange = { onSet(it.toInt()) },
        valueRange = min.toFloat()..max.toFloat(),
        steps = (max - min - 1).coerceAtLeast(0)
    )
}
