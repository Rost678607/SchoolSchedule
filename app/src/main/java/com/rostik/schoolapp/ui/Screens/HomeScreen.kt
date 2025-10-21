package com.rostik.schoolapp.ui

import android.annotation.SuppressLint
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import com.rostik.schoolapp.model.data.LessonManager
import com.rostik.schoolapp.model.data.SpecificLesson
import com.rostik.schoolapp.model.data.SpecificLessonManager
import com.rostik.schoolapp.model.data.TimeScheme
import com.rostik.schoolapp.model.data.TimeSchemeManager
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.launch

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun HomeScreen(navController: NavController = rememberNavController()) {
    val context = LocalContext.current
    val lessonManager = remember { LessonManager(context) }
    val specificLessonManager = remember { SpecificLessonManager(context) }
    val timeSchemeManager = remember { TimeSchemeManager(context) }
    val loaded = remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        coroutineScope.launch {
            timeSchemeManager.load()
            lessonManager.load()
            specificLessonManager.load()
            specificLessonManager.cleanInvalid(lessonManager)
            loaded.value = true
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate(Screen.Homework.route) },
            ) {
                Icon(Icons.Filled.AutoStories, contentDescription = "Add Homework")
            }
        }
    ) {
        if (!loaded.value) {
            Text(
                text = "Загрузка...",
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                fontSize = 24.sp
            )
        } else {
            val nowDate = LocalDate.now()
            val nowTime = LocalTime.now()
            val today = nowDate.dayOfWeek
            val tomorrow = if (today == DayOfWeek.SUNDAY) DayOfWeek.MONDAY else DayOfWeek.of(today.value + 1)

            val timeScheme = timeSchemeManager.getTimeScheme()
            val specificLessonsToday = specificLessonManager.getSpecificLessonsForDay(today)

            val showTomorrow = specificLessonsToday.isEmpty() || lessonsHaveEnded(specificLessonsToday, timeScheme, nowTime)
            val dayToShow = if (showTomorrow) tomorrow else today
            val scheduleTitle = if (showTomorrow) "Расписание на завтра" else "Расписание на сегодня"

            val lessonsForDay = specificLessonManager.getSpecificLessonsForDay(dayToShow).sortedBy { it.lessonNumber }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        elevation = CardDefaults.cardElevation(8.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(8.dp)
                        ) {
                            Text(
                                text = scheduleTitle,
                                fontSize = 24.sp
                            )
                            if (lessonsForDay.isEmpty()) {
                                Text(
                                    text = "Нет уроков на завтра",
                                    fontSize = 16.sp,
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                            } else {
                                lessonsForDay.forEach { specificLesson ->
                                    val lesson = lessonManager.getLessonById(specificLesson.lessonId)
                                    if (lesson != null) {
                                        val startTime = calculateStartTime(specificLesson, timeScheme, lessonsForDay)
                                        val endTime = startTime.plusMinutes(timeScheme.lessonLength.toLong())
                                        Text(
                                            text = buildAnnotatedString {
                                                withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                                                    append("${specificLesson.lessonNumber}. ${lesson.name}")
                                                }
                                                append(" (${lesson.teacher}) - ${specificLesson.cabinet} ${specificLesson.additionalInfo} (${startTime} - ${endTime})")
                                            },
                                            fontSize = 16.sp,
                                            modifier = Modifier.padding(top = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        elevation = CardDefaults.cardElevation(8.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(8.dp)
                        ) {
                            Text(
                                text = "Домашние задания",
                                fontSize = 24.sp
                            )
                            val homeworkEntries = lessonManager.getAllLessons()
                                .filter { !it.homework.isNullOrBlank() }
                                .mapNotNull { lesson ->
                                    val daysToNext = calculateDaysToNextLesson(lesson.id, specificLessonManager, nowDate, nowTime, timeScheme)
                                    if (daysToNext >= 0) lesson to daysToNext else null
                                }
                                .sortedBy { it.second }

                            if (homeworkEntries.isEmpty()) {
                                Text(
                                    text = "Нет домашнего задания",
                                    fontSize = 16.sp,
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                            } else {
                                homeworkEntries.forEach { (lesson, days) ->
                                    Text(
                                        text = "${lesson.name}: ${lesson.homework} (через $days дней)",
                                        fontSize = 16.sp,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
private fun lessonsHaveEnded(
    specificLessons: List<SpecificLesson>,
    timeScheme: TimeScheme,
    nowTime: LocalTime
): Boolean {
    if (specificLessons.isEmpty()) return true
    val lastLesson = specificLessons.maxByOrNull { it.lessonNumber } ?: return true
    val startTime = calculateStartTime(lastLesson, timeScheme, specificLessons)
    val endTime = startTime.plusMinutes(timeScheme.lessonLength.toLong())
    return nowTime.isAfter(endTime)
}

@RequiresApi(Build.VERSION_CODES.O)
private fun calculateStartTime(
    specificLesson: SpecificLesson,
    timeScheme: TimeScheme,
    lessonsForDay: List<SpecificLesson>
): LocalTime {
    val index = lessonsForDay.sortedBy { it.lessonNumber }.indexOfFirst { it.id == specificLesson.id }
    var currentTime = timeScheme.start
    for (j in 0 until index) {
        currentTime = currentTime.plusMinutes(timeScheme.lessonLength.toLong())
        val breakIndex = j
        val breakDuration = if (breakIndex < timeScheme.breaks.size) timeScheme.breaks[breakIndex] else timeScheme.defaultBreak
        currentTime = currentTime.plusMinutes(breakDuration.toLong())
    }
    return currentTime
}

@RequiresApi(Build.VERSION_CODES.O)
private fun calculateDaysToNextLesson(
    lessonId: Int,
    specificLessonManager: SpecificLessonManager,
    nowDate: LocalDate,
    nowTime: LocalTime,
    timeScheme: TimeScheme
): Int {
    val today = nowDate.dayOfWeek
    val todaySpecs = specificLessonManager.getSpecificLessonsForDay(today).filter { it.lessonId == lessonId }
    if (todaySpecs.isNotEmpty()) {
        val futureToday = todaySpecs.any { spec ->
            val startTime = calculateStartTime(spec, timeScheme, specificLessonManager.getSpecificLessonsForDay(today))
            nowTime.isBefore(startTime)
        }
        if (futureToday) return 0
    }

    for (d in 1..7) {
        val futureDate = nowDate.plusDays(d.toLong())
        val futureDay = futureDate.dayOfWeek
        if (specificLessonManager.getSpecificLessonsForDay(futureDay).any { it.lessonId == lessonId }) {
            return d
        }
    }
    return -1
}