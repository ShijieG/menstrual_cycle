package com.example.menstrual.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.menstrual.data.export.ExportManager
import com.example.menstrual.data.local.entity.PeriodEpisodeEntity
import com.example.menstrual.data.local.entity.SymptomEntryEntity
import com.example.menstrual.data.preferences.SettingsDataStore
import com.example.menstrual.data.repository.PeriodRepository
import com.example.menstrual.MenstrualApplication
import com.example.menstrual.domain.model.Cycle
import com.example.menstrual.domain.model.PredictionResult
import com.example.menstrual.domain.usecase.PredictCyclesUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.time.LocalDate

class AppViewModel(
    app: Application,
    private val repository: PeriodRepository,
    private val settings: SettingsDataStore,
    private val predictUseCase: PredictCyclesUseCase,
    private val exportManager: ExportManager
) : AndroidViewModel(app) {

    val episodes = repository.episodes.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val defaultCycleLength = settings.defaultCycleLength.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 28)
    val defaultPeriodLength = settings.defaultPeriodLength.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 5)
    val periodReminderOn = settings.periodReminderOn.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
    val periodRemindDaysBefore = settings.periodRemindDaysBefore.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 1)
    val ovulationReminderOn = settings.ovulationReminderOn.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val predictions: PredictionResult = predictUseCase(emptyList<Cycle>(), 28, 5)

    val predictionsFlow = combine(episodes, defaultCycleLength, defaultPeriodLength) { eps, dc, dp ->
        predictUseCase(
            episodes = eps.map { Cycle(it.startDate, it.endDate, it.flowLevel, it.note) },
            defaultCycle = dc,
            defaultPeriod = dp
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), predictions)

    val ongoing = episodes.map { list ->
        list.filter { it.endDate == null }.maxByOrNull { it.startDate }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // ---- 经期记录（按具体日期标记，供日历点击使用） ----
    fun startPeriodOn(date: LocalDate) = viewModelScope.launch { repository.startPeriod(startDate = date) }
    fun endPeriodOn(date: LocalDate) = viewModelScope.launch { repository.endPeriod(date = date) }
    fun deleteEpisode(episode: PeriodEpisodeEntity) = viewModelScope.launch { repository.deleteEpisode(episode) }

    // ---- 性生活记录（type = SEX_TYPE，按天 toggle） ----
    fun toggleSex(date: LocalDate) = viewModelScope.launch {
        val existing = repository.getAllSymptoms().firstOrNull { it.date == date && it.type == SEX_TYPE }
        if (existing != null) repository.removeSymptom(existing)
        else repository.addSymptom(SymptomEntryEntity(date = date, type = SEX_TYPE, value = SEX_VALUE, intensity = 0))
    }

    // ---- 症状 ----
    fun symptomsForDate(date: LocalDate) = repository.symptomsForDate(date)
    fun toggleSymptom(date: LocalDate, type: Int, value: String, intensity: Int) = viewModelScope.launch {
        val existing = repository.getAllSymptoms().firstOrNull { it.date == date && it.type == type && it.value == value }
        if (existing != null) repository.removeSymptom(existing)
        else repository.addSymptom(SymptomEntryEntity(date = date, type = type, value = value, intensity = intensity))
    }

    // ---- 设置 ----
    fun setDefaultCycleLength(v: Int) = viewModelScope.launch { settings.setDefaultCycleLength(v) }
    fun setDefaultPeriodLength(v: Int) = viewModelScope.launch { settings.setDefaultPeriodLength(v) }
    fun setPeriodReminderOn(v: Boolean) = viewModelScope.launch { settings.setPeriodReminderOn(v) }
    fun setPeriodRemindDaysBefore(v: Int) = viewModelScope.launch { settings.setPeriodRemindDaysBefore(v) }
    fun setOvulationReminderOn(v: Boolean) = viewModelScope.launch { settings.setOvulationReminderOn(v) }

    suspend fun export(): File = exportManager.export(repository.getAllEpisodes(), repository.getAllSymptoms())

    companion object {
        /** 症状类型（保留）；性生活使用 SEX_TYPE，心情/分泌物已移除。 */
        const val SYMPTOM_TYPE = 0
        const val SEX_TYPE = 3
        const val SEX_VALUE = "有"
    }
}

class AppViewModelFactory(private val app: Application) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val a = app as MenstrualApplication
        return AppViewModel(a, a.repository, a.settings, a.predictUseCase, a.exportManager) as T
    }
}
