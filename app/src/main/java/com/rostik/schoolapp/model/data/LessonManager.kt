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

class LessonManager(private val context: Context) {
    private val lessons: MutableList<Lesson> = mutableListOf()
    private val KEY = byteArrayPreferencesKey("lessons")

    suspend fun load() {
        try {
            val byteArray = context.applicationContext.appDataStore.data.map { it[KEY] }.first()
            if (byteArray != null) {
                ObjectInputStream(ByteArrayInputStream(byteArray)).use { input ->
                    val loadedLessons = input.readObject() as? MutableList<Lesson>
                    loadedLessons?.let { lessons.addAll(it) }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun apply() {
        try {
            context.applicationContext.appDataStore.edit { prefs ->
                ByteArrayOutputStream().use { byteOut ->
                    ObjectOutputStream(byteOut).use { output ->
                        output.writeObject(lessons)
                        prefs[KEY] = byteOut.toByteArray()
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun addLesson(name: String, teacher: String): Lesson {
        val newId = findMinimalFreeId()
        val lesson = Lesson(newId, name, teacher)
        lessons.add(lesson)
        return lesson
    }

    fun updateLesson(id: Int, name: String? = null, teacher: String? = null) {
        val lesson = lessons.find { it.id == id }
        lesson?.let {
            name?.let { lesson.name = it }
            teacher?.let { lesson.teacher = it }
        }
    }

    fun updateLessonHomework(id: Int, homework: String?) {
        val lesson = lessons.find { it.id == id }
        lesson?.let {
            homework?.let { lesson.homework = it }
        }
    }

    fun deleteLesson(id: Int) {
        lessons.removeIf { it.id == id }
    }

    fun replaceAll(newLessons: List<Lesson>) {
        lessons.clear()
        lessons.addAll(newLessons)
    }

    fun getAllLessons(): List<Lesson> = lessons.toList()

    fun getLessonById(id: Int): Lesson? {
        return lessons.find { it.id == id }
    }

    private fun findMinimalFreeId(): Int {
        val usedIds = lessons.map { it.id }.toSet()
        var id = 0
        while (id in usedIds) id++
        return id
    }
}