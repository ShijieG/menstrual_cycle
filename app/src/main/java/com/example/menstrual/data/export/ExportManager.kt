package com.example.menstrual.data.export

import android.content.Context
import com.example.menstrual.data.local.entity.PeriodEpisodeEntity
import com.example.menstrual.data.local.entity.SymptomEntryEntity
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * 将数据导出为 JSON 文件（应用私有目录 /exports），供用户分享备份。
 * 使用平台内置 org.json，无需额外依赖。
 */
class ExportManager(private val context: Context) {
    fun export(
        episodes: List<PeriodEpisodeEntity>,
        symptoms: List<SymptomEntryEntity>
    ): File {
        val root = JSONObject().apply {
            put("app", "menstrual-tracker")
            put("version", 1)
            put("exportedAt", System.currentTimeMillis())
            put("episodes", JSONArray().apply {
                episodes.forEach { e ->
                    put(JSONObject().apply {
                        put("id", e.id)
                        put("startDate", e.startDate.toString())
                        put("endDate", e.endDate?.toString() ?: JSONObject.NULL)
                        put("flowLevel", e.flowLevel)
                        put("note", e.note)
                    })
                }
            })
            put("symptoms", JSONArray().apply {
                symptoms.forEach { s ->
                    put(JSONObject().apply {
                        put("id", s.id)
                        put("date", s.date.toString())
                        put("type", s.type)
                        put("value", s.value)
                        put("intensity", s.intensity)
                    })
                }
            })
        }

        val dir = File(context.getExternalFilesDir(null), "exports")
        dir.mkdirs()
        val file = File(dir, "menstrual-export-${System.currentTimeMillis()}.json")
        file.writeText(root.toString(2))
        return file
    }
}
