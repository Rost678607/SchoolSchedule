package com.rostik.schoolapp.model

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import androidx.annotation.RequiresApi
import androidx.core.content.FileProvider
import com.rostik.schoolapp.model.data.Lesson
import com.rostik.schoolapp.model.data.LessonManager
import com.rostik.schoolapp.model.data.SpecificLesson
import com.rostik.schoolapp.model.data.SpecificLessonManager
import com.rostik.schoolapp.model.data.TimeScheme
import com.rostik.schoolapp.model.data.TimeSchemeManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.time.DayOfWeek
import java.time.LocalTime

class ShareManager(
    private val context: Context,
    private val lessonManager: LessonManager,
    private val specificLessonManager: SpecificLessonManager,
    private val timeSchemeManager: TimeSchemeManager
) {

    @RequiresApi(Build.VERSION_CODES.O)
    fun exportAndShare(filename: String) {
        val file = createExportFile(filename)
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/octet-stream"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(Intent.createChooser(shareIntent, "Поделиться расписанием"))
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createExportFile(filename: String): File {
        val json = getExportJson()
        val file = File(context.cacheDir, "$filename.schoe")
        file.writeText(json)
        return file
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun getExportJson(): String {
        val root = JSONObject()

        val lessonsArray = JSONArray()
        lessonManager.getAllLessons().forEach { lesson ->
            val obj = JSONObject()
            obj.put("id", lesson.id)
            obj.put("name", lesson.name)
            obj.put("teacher", lesson.teacher)
            lessonsArray.put(obj)
        }
        root.put("lessons", lessonsArray)

        val specificArray = JSONArray()
        specificLessonManager.getAllSpecificLessons().forEach { sl ->
            val obj = JSONObject()
            obj.put("id", sl.id)
            obj.put("day", sl.day.name)
            obj.put("lessonNumber", sl.lessonNumber)
            obj.put("lessonId", sl.lessonId)
            obj.put("cabinet", sl.cabinet)
            obj.put("additionalInfo", sl.additionalInfo)
            specificArray.put(obj)
        }
        root.put("specificLessons", specificArray)

        val ts = timeSchemeManager.getTimeScheme()
        val tsObj = JSONObject()
        tsObj.put("start", ts.start.toString())
        tsObj.put("lessonLength", ts.lessonLength)
        val breaksArray = JSONArray()
        ts.breaks.forEach { breaksArray.put(it) }
        tsObj.put("breaks", breaksArray)
        tsObj.put("defaultBreak", ts.defaultBreak)
        tsObj.put("coupleMiddleBreakLength", ts.coupleMiddleBreakLength)
        tsObj.put("isPairMode", ts.isPairMode)
        root.put("timeScheme", tsObj)

        return root.toString(2)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun importFromUri(uri: Uri) {
        val cachedFile = withContext(Dispatchers.IO) {
            val inputStream = context.contentResolver.openInputStream(uri)
                ?: throw Exception("Не удалось открыть файл")

            val fileName = "import_${System.currentTimeMillis()}.schoe"
            val file = File(context.cacheDir, fileName)

            inputStream.use { input ->
                file.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            file
        }

        val jsonString = withContext(Dispatchers.IO) {
            cachedFile.readText()
        }

        try {
            val root = JSONObject(jsonString)

            val lessonsArray = root.getJSONArray("lessons")
            val newLessons = mutableListOf<Lesson>()
            for (i in 0 until lessonsArray.length()) {
                val obj = lessonsArray.getJSONObject(i)
                newLessons.add(
                    Lesson(
                        id = obj.getInt("id"),
                        name = obj.getString("name"),
                        teacher = obj.getString("teacher"),
                        homework = ""
                    )
                )
            }
            lessonManager.replaceAll(newLessons)
            lessonManager.apply()

            val specificArray = root.getJSONArray("specificLessons")
            val newSpecific = mutableListOf<SpecificLesson>()
            for (i in 0 until specificArray.length()) {
                val obj = specificArray.getJSONObject(i)
                newSpecific.add(
                    SpecificLesson(
                        id = obj.getInt("id"),
                        day = DayOfWeek.valueOf(obj.getString("day")),
                        lessonNumber = obj.getInt("lessonNumber"),
                        lessonId = obj.getInt("lessonId"),
                        cabinet = obj.getString("cabinet"),
                        additionalInfo = obj.getString("additionalInfo")
                    )
                )
            }
            specificLessonManager.replaceAll(newSpecific)
            specificLessonManager.apply()
            specificLessonManager.cleanInvalid(lessonManager)

            val tsObj = root.getJSONObject("timeScheme")
            val newTimeScheme = TimeScheme(
                start = LocalTime.parse(tsObj.getString("start")),
                lessonLength = tsObj.getInt("lessonLength"),
                breaks = mutableListOf<Int>().apply {
                    val breaksArray = tsObj.getJSONArray("breaks")
                    for (i in 0 until breaksArray.length()) {
                        add(breaksArray.getInt(i))
                    }
                },
                defaultBreak = tsObj.getInt("defaultBreak"),
                coupleMiddleBreakLength = tsObj.getInt("coupleMiddleBreakLength"),
                isPairMode = tsObj.getBoolean("isPairMode")
            )
            timeSchemeManager.replace(newTimeScheme)
            timeSchemeManager.apply()

            withContext(Dispatchers.IO) {
                cachedFile.delete()
            }
        } catch (e: Exception) {
            withContext(Dispatchers.IO) { cachedFile.delete() }
            throw e
        }
    }

    fun isValidFile(uri: Uri): Boolean {
        return try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val displayNameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (displayNameIndex != -1) {
                        val fileName = cursor.getString(displayNameIndex)
                        return fileName.endsWith(".schoe", ignoreCase = true)
                    }
                }
            }
            false
        } catch (_: Exception) {
            false
        }
    }
}