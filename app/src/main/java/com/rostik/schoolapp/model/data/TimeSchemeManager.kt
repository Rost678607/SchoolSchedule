package com.rostik.schoolapp.model.data

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.datastore.preferences.core.byteArrayPreferencesKey
import androidx.datastore.preferences.core.edit
import com.rostik.schoolapp.model.data.DataStoreProvider.appDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.time.LocalTime

@RequiresApi(Build.VERSION_CODES.O)
class TimeSchemeManager(private val context: Context) {
    private var timeScheme: TimeScheme = TimeScheme()
    private val KEY = byteArrayPreferencesKey("time_scheme")

    suspend fun load() {
        val byteArray = context.applicationContext.appDataStore.data.map { it[KEY] }.first()
        if (byteArray != null) {
            ObjectInputStream(ByteArrayInputStream(byteArray)).use { input ->
                val loadedTimeScheme = input.readObject() as? TimeScheme
                loadedTimeScheme?.let { timeScheme = it }
            }
        }
    }

    suspend fun apply() {
        context.applicationContext.appDataStore.edit { prefs ->
            ByteArrayOutputStream().use { byteOut ->
                ObjectOutputStream(byteOut).use { output ->
                    output.writeObject(timeScheme)
                    prefs[KEY] = byteOut.toByteArray()
                }
            }
        }
    }

    fun getTimeScheme(): TimeScheme = timeScheme

    fun updateTimeScheme(
        start: LocalTime? = null,
        lessonLength: Int? = null,
        breaks: MutableList<Int>? = null,
        defaultBreak: Int? = null,
        coupleMiddleBreak: Int? = null,
        isPairMode: Boolean? = null
    ) {
        start?.let { timeScheme.start = it }
        lessonLength?.let { timeScheme.lessonLength = it }
        breaks?.let { timeScheme.breaks.clear(); timeScheme.breaks.addAll(it) }
        defaultBreak?.let { timeScheme.defaultBreak = it }
        coupleMiddleBreak?.let { timeScheme.coupleMiddleBreakLength = it }
        isPairMode?.let { timeScheme.isPairMode = it }
    }

    fun resetToDefault() {
        timeScheme = TimeScheme()
    }

    fun setBreaks(breaks: List<Int>) {
        timeScheme.breaks.clear()
        timeScheme.breaks.addAll(breaks)
    }

    fun getFullLessonDuration(): Int {
        return if (timeScheme.isPairMode) {
            timeScheme.lessonLength * 2 + timeScheme.coupleMiddleBreakLength
        } else {
            timeScheme.lessonLength
        }
    }

    fun getLessonStartTime(lessonNumber: Int): LocalTime {
        var currentTime = timeScheme.start
        for (i in 0 until lessonNumber - 1) {
            currentTime = currentTime.plusMinutes(getFullLessonDuration().toLong())
            val breakDuration = if (i < timeScheme.breaks.size) {
                timeScheme.breaks[i].toLong()
            } else {
                timeScheme.defaultBreak.toLong()
            }
            currentTime = currentTime.plusMinutes(breakDuration)
        }
        return currentTime
    }
}