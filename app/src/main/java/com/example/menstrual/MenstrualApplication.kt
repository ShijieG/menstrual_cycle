package com.example.menstrual

import android.app.Application
import androidx.room.Room
import com.example.menstrual.data.export.ExportManager
import com.example.menstrual.data.local.AppDatabase
import com.example.menstrual.data.preferences.SettingsDataStore
import com.example.menstrual.data.repository.PeriodRepository
import com.example.menstrual.domain.usecase.PredictCyclesUseCase
import com.example.menstrual.reminder.ReminderScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MenstrualApplication : Application() {

    val database by lazy {
        Room.databaseBuilder(this, AppDatabase::class.java, AppDatabase.NAME)
            .fallbackToDestructiveMigration()
            .build()
    }

    val repository by lazy { PeriodRepository(database) }
    val settings by lazy { SettingsDataStore(this) }
    val predictUseCase by lazy { PredictCyclesUseCase() }
    val exportManager by lazy { ExportManager(this) }

    override fun onCreate() {
        super.onCreate()
        // 若提醒开关开启，则在应用启动时重新登记周期提醒
        CoroutineScope(Dispatchers.IO).launch {
            if (settings.periodReminderOn.first() || settings.ovulationReminderOn.first()) {
                ReminderScheduler.schedule(this@MenstrualApplication)
            }
        }
    }
}
