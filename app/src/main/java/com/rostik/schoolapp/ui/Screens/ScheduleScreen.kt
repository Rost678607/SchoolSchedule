package com.rostik.schoolapp.ui

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.rostik.schoolapp.model.data.LessonManager
import com.rostik.schoolapp.model.data.SpecificLesson
import com.rostik.schoolapp.model.data.SpecificLessonManager
import com.rostik.schoolapp.model.data.TimeScheme
import com.rostik.schoolapp.model.data.TimeSchemeManager
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun LessonItem(lesson: SpecificLesson, timeScheme: TimeScheme, lessonManager: LessonManager) {
    val lessonObj = lessonManager.getLessonById(lesson.lessonId) ?: return
    val lessonStartTime = calculateLessonStartTime(lesson.lessonNumber, timeScheme)
    val lessonEndTime = lessonStartTime.plusMinutes(timeScheme.lessonLength.toLong())

    val formatter = DateTimeFormatter.ofPattern("HH:mm")
    val startTimeText = lessonStartTime.format(formatter)
    val endTimeText = lessonEndTime.format(formatter)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "${lesson.lessonNumber}. ${lessonObj.name}",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "Учитель: ${lessonObj.teacher}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "Кабинет: ${lesson.cabinet}",
                    style = MaterialTheme.typography.bodyMedium
                )
                lesson.additionalInfo.takeIf { it.isNotBlank() }?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                lessonObj.homework.takeIf { it.isNotBlank() }?.let {
                    Text(
                        text = "Д/З: $it",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column(
                modifier = Modifier
                    .padding(start = 8.dp),
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = startTimeText,
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = endTimeText,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
fun calculateLessonStartTime(lessonNumber: Int, timeScheme: TimeScheme): LocalTime {
    var currentTime = timeScheme.start
    for (i in 0 until lessonNumber - 1) {
        currentTime = currentTime.plusMinutes(timeScheme.lessonLength.toLong())
        val breakDuration = if (i < timeScheme.breaks.size) {
            timeScheme.breaks[i].toLong()
        } else {
            timeScheme.defaultBreak.toLong()
        }
        currentTime = currentTime.plusMinutes(breakDuration)
    }
    return currentTime
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun ScheduleScreen() {
    val context = LocalContext.current
    val specificLessonManager = remember { SpecificLessonManager(context) }
    val timeSchemeManager = remember { TimeSchemeManager(context) }
    val lessonManager = remember { LessonManager(context) }
    var specificLessons by remember { mutableStateOf<List<SpecificLesson>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        lessonManager.load()
        specificLessonManager.load()
        specificLessonManager.cleanInvalid(lessonManager)
        timeSchemeManager.load()
        specificLessons = specificLessonManager.getAllSpecificLessons()
        isLoading = false

        if (specificLessons.isEmpty()) {
            val lesson = lessonManager.addLesson("Математика", "Иванов И.И.")
            specificLessonManager.addSpecificLesson(
                day = DayOfWeek.MONDAY,
                lessonNumber = 1,
                lesson = lesson,
                cabinet = "101",
                additionalInfo = "Тест"
            )
            specificLessonManager.apply()
            specificLessons = specificLessonManager.getAllSpecificLessons()
        }
    }

    if (isLoading) {
        Text(
            text = "Загрузка...",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(16.dp)
        )
        return
    }

    val days = listOf(
        DayOfWeek.MONDAY to "Понедельник",
        DayOfWeek.TUESDAY to "Вторник",
        DayOfWeek.WEDNESDAY to "Среда",
        DayOfWeek.THURSDAY to "Четверг",
        DayOfWeek.FRIDAY to "Пятница",
        DayOfWeek.SATURDAY to "Суббота",
        DayOfWeek.SUNDAY to "Воскресенье"
    )
    val lessonsByDay = specificLessons.groupBy { it.day }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
    ) {
        days.forEach { (dayEnum, dayName) ->
            item {
                Text(
                    text = dayName,
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(16.dp)
                )
            }
            val dayLessons = lessonsByDay[dayEnum] ?: emptyList()
            if (dayLessons.isEmpty()) {
                item {
                    Text(
                        text = "Нет уроков",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            } else {
                items(dayLessons.sortedBy { it.lessonNumber }) { lesson ->
                    LessonItem(lesson, timeSchemeManager.getTimeScheme(), lessonManager)
                }
            }
        }
    }
}