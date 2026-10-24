package com.example.menstrual.reminder

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.menstrual.MenstrualApplication
import com.example.menstrual.domain.model.Cycle
import com.example.menstrual.domain.usecase.PredictCyclesUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * 周期提醒 Worker：每日运行一次，依据预测结果在经期临近/排卵日推送系统通知。
 * 无日历权限、无联网，仅使用设备系统日期与本地数据。
 */
class ReminderWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val app = applicationContext as MenstrualApplication
        val periodOn = app.settings.periodReminderOn.first()
        val ovulationOn = app.settings.ovulationReminderOn.first()
        if (!periodOn && !ovulationOn) return@withContext Result.success()

        val episodes = app.repository.getAllEpisodes().map {
            Cycle(it.startDate, it.endDate, it.flowLevel, it.note)
        }
        val dc = app.settings.defaultCycleLength.first()
        val dp = app.settings.defaultPeriodLength.first()
        val result = PredictCyclesUseCase().invoke(episodes, dc, dp)
        val today = LocalDate.now()

        val nm = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createChannel(nm)

        if (periodOn) {
            val upcoming = result.predictions
                .filter { !it.predictedEnd.isBefore(today) }
                .minByOrNull { ChronoUnit.DAYS.between(today, it.predictedStart) }
            upcoming?.let {
                val days = ChronoUnit.DAYS.between(today, it.predictedStart)
                val msg = if (days <= 0L) "预计今天来月经"
                else "预计 ${days} 天后来月经（${it.predictedStart}）"
                notify(nm, ID_PERIOD, "经期提醒", msg)
            }
        }

        if (ovulationOn) {
            result.predictions.firstOrNull { it.ovulationDate == today }?.let {
                notify(nm, ID_OVULATION, "排卵期提醒", "今天是排卵期，注意易孕窗口")
            }
        }
        Result.success()
    }

    private fun createChannel(nm: NotificationManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(CHANNEL_ID, "经期提醒", NotificationManager.IMPORTANCE_DEFAULT)
            nm.createNotificationChannel(ch)
        }
    }

    private fun notify(nm: NotificationManager, id: Int, title: String, text: String) {
        val n = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        nm.notify(id, n)
    }

    companion object {
        const val CHANNEL_ID = "menstrual_reminder"
        private const val ID_PERIOD = 1
        private const val ID_OVULATION = 2
    }
}
