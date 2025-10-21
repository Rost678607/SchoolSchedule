package com.rostik.schoolapp.model.data

import android.content.Context
import androidx.datastore.preferences.core.byteArrayPreferencesKey
import androidx.datastore.preferences.core.edit
import com.rostik.schoolapp.model.data.DataStoreProvider.appDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.time.DayOfWeek

class SpecificLessonManager(private val context: Context) {
    private val specificLessons: MutableList<SpecificLesson> = mutableListOf()
    private val KEY = byteArrayPreferencesKey("specific_lessons")

    suspend fun load() {
        val byteArray = context.applicationContext.appDataStore.data.map { it[KEY] }.first()
        if (byteArray != null) {
            ObjectInputStream(ByteArrayInputStream(byteArray)).use { input ->
                val loadedSpecificLessons = input.readObject() as? MutableList<SpecificLesson>
                loadedSpecificLessons?.let { specificLessons.addAll(it) }
            }
        }
    }

    suspend fun apply() {
        context.applicationContext.appDataStore.edit { prefs ->
            ByteArrayOutputStream().use { byteOut ->
                ObjectOutputStream(byteOut).use { output ->
                    output.writeObject(specificLessons)
                    prefs[KEY] = byteOut.toByteArray()
                }
            }
        }
    }

    fun addSpecificLesson(day: DayOfWeek, lessonNumber: Int, lesson: Lesson, cabinet: String, additionalInfo: String): SpecificLesson {
        val newId = findMinimalFreeId()
        val specificLesson = SpecificLesson(newId, day, lessonNumber, lesson.id, cabinet, additionalInfo)
        specificLessons.add(specificLesson)
        return specificLesson
    }

    fun updateSpecificLesson(id: Int, day: DayOfWeek? = null, lessonNumber: Int? = null, lesson: Lesson? = null, cabinet: String? = null, additionalInfo: String? = null) {
        val specificLesson = specificLessons.find { it.id == id }
        specificLesson?.let {
            day?.let { specificLesson.day = it }
            lessonNumber?.let { specificLesson.lessonNumber = it }
            lesson?.let { specificLesson.lessonId = it.id }
            cabinet?.let { specificLesson.cabinet = it }
            additionalInfo?.let { specificLesson.additionalInfo = it }
        }
    }

    fun deleteSpecificLesson(id: Int) {
        specificLessons.removeIf { it.id == id }
    }

    fun getAllSpecificLessons(): List<SpecificLesson> = specificLessons.toList()

    fun getSpecificLessonsForDay(day: DayOfWeek): List<SpecificLesson> {
        return specificLessons.filter { it.day == day }.sortedBy { it.lessonNumber }
    }

    fun getSpecificLesson(day: DayOfWeek, lessonNumber: Int): SpecificLesson? {
        return specificLessons.find { it.day == day && it.lessonNumber == lessonNumber }
    }

    suspend fun cleanInvalid(lessonManager: LessonManager) {
        specificLessons.removeIf { lessonManager.getLessonById(it.lessonId) == null }
        apply()
    }

    private fun findMinimalFreeId(): Int {
        val usedIds = specificLessons.map { it.id }.toSet()
        var id = 0
        while (id in usedIds) id++
        return id
    }
}