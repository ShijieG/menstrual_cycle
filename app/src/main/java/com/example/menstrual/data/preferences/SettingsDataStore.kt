package com.example.menstrual.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

/**
 * 应用设置（DataStore）。保存预测默认值与提醒开关，纯本地、无联网。
 */
class SettingsDataStore(private val context: Context) {
    private val ds = context.dataStore

    val defaultCycleLength: Flow<Int> = ds.data.map { it[KEY_CYCLE] ?: 28 }
    val defaultPeriodLength: Flow<Int> = ds.data.map { it[KEY_PERIOD] ?: 5 }
    val periodReminderOn: Flow<Boolean> = ds.data.map { it[KEY_REMIND_PERIOD] ?: true }
    val periodRemindDaysBefore: Flow<Int> = ds.data.map { it[KEY_REMIND_DAYS] ?: 1 }
    val ovulationReminderOn: Flow<Boolean> = ds.data.map { it[KEY_REMIND_OVULATION] ?: false }

    suspend fun setDefaultCycleLength(v: Int) = ds.edit { it[KEY_CYCLE] = v.coerceIn(20, 45) }
    suspend fun setDefaultPeriodLength(v: Int) = ds.edit { it[KEY_PERIOD] = v.coerceIn(2, 15) }
    suspend fun setPeriodReminderOn(v: Boolean) = ds.edit { it[KEY_REMIND_PERIOD] = v }
    suspend fun setPeriodRemindDaysBefore(v: Int) = ds.edit { it[KEY_REMIND_DAYS] = v.coerceIn(0, 7) }
    suspend fun setOvulationReminderOn(v: Boolean) = ds.edit { it[KEY_REMIND_OVULATION] = v }

    companion object {
        private val KEY_CYCLE = intPreferencesKey("default_cycle_length")
        private val KEY_PERIOD = intPreferencesKey("default_period_length")
        private val KEY_REMIND_PERIOD = booleanPreferencesKey("period_reminder_on")
        private val KEY_REMIND_DAYS = intPreferencesKey("period_remind_days_before")
        private val KEY_REMIND_OVULATION = booleanPreferencesKey("ovulation_reminder_on")
    }
}
