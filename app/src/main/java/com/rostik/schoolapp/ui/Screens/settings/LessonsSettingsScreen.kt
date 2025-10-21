package com.rostik.schoolapp.ui

import android.annotation.SuppressLint
import android.os.Build
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.rostik.schoolapp.model.data.Lesson
import com.rostik.schoolapp.model.data.LessonManager
import com.rostik.schoolapp.model.data.SpecificLesson
import com.rostik.schoolapp.model.data.SpecificLessonManager
import kotlinx.coroutines.launch
import java.time.DayOfWeek

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun LessonsSettingsScreen(
    specificLessonManager: SpecificLessonManager,
    lessonManager: LessonManager
) {
    val days = listOf(
        DayOfWeek.MONDAY,
        DayOfWeek.TUESDAY,
        DayOfWeek.WEDNESDAY,
        DayOfWeek.THURSDAY,
        DayOfWeek.FRIDAY,
        DayOfWeek.SATURDAY,
        DayOfWeek.SUNDAY
    )
    val dayNames = mapOf(
        DayOfWeek.MONDAY to "Пн",
        DayOfWeek.TUESDAY to "Вт",
        DayOfWeek.WEDNESDAY to "Ср",
        DayOfWeek.THURSDAY to "Чт",
        DayOfWeek.FRIDAY to "Пт",
        DayOfWeek.SATURDAY to "Сб",
        DayOfWeek.SUNDAY to "Вс"
    )

    val pagerState = rememberPagerState(pageCount = { days.size })
    var specificLessons by remember { mutableStateOf<List<SpecificLesson>>(emptyList()) }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    var showAddDialog by remember { mutableStateOf(false) }
    var editingSpecificLesson by remember { mutableStateOf<SpecificLesson?>(null) }
    var deletingSpecificLesson by remember { mutableStateOf<SpecificLesson?>(null) }

    LaunchedEffect(Unit) {
        lessonManager.load()
        specificLessonManager.load()
        specificLessonManager.cleanInvalid(lessonManager)
        specificLessons = specificLessonManager.getAllSpecificLessons()
    }

    Scaffold(
        topBar = {
            TabRow(
                selectedTabIndex = pagerState.currentPage,
                modifier = Modifier.fillMaxWidth()
            ) {
                days.forEachIndexed { index, day ->
                    Tab(
                        selected = pagerState.currentPage == index,
                        onClick = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(index)
                            }
                        },
                        text = { Text(dayNames[day] ?: day.toString()) }
                    )
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                content = {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = "Добавить конкретный урок",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                },
                containerColor = MaterialTheme.colorScheme.primary
            )
        }
    ) { innerPadding ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) { page ->
            val selectedDay = days[page]
            val daySpecificLessons = specificLessonManager.getSpecificLessonsForDay(selectedDay)

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
                items(daySpecificLessons) { specificLesson ->
                    val lesson = lessonManager.getLessonById(specificLesson.lessonId)
                    if (lesson != null) {
                        SpecificLessonItem(
                            specificLesson = specificLesson,
                            lesson = lesson,
                            onEdit = { editingSpecificLesson = it },
                            onDelete = { deletingSpecificLesson = it }
                        )
                    }
                }
                item {
                    Spacer(modifier = Modifier.height(84.dp))
                }
            }
        }
    }

    if (showAddDialog) {
        AddSpecificLessonDialog(
            day = days[pagerState.currentPage],
            lessons = lessonManager.getAllLessons(),
            onDismiss = { showAddDialog = false },
            onAdd = { lessonNumber, lesson, cabinet, additionalInfo ->
                val existing = specificLessonManager.getSpecificLesson(days[pagerState.currentPage], lessonNumber)
                if (existing != null) {
                    Toast.makeText(context, "Урок с таким номером уже существует", Toast.LENGTH_SHORT).show()
                } else {
                    specificLessonManager.addSpecificLesson(days[pagerState.currentPage], lessonNumber, lesson, cabinet, additionalInfo)
                    coroutineScope.launch {
                        specificLessonManager.apply()
                        specificLessons = specificLessonManager.getAllSpecificLessons()
                        showAddDialog = false
                    }
                }
            }
        )
    }

    editingSpecificLesson?.let { specificLesson ->
        EditSpecificLessonDialog(
            specificLesson = specificLesson,
            lessons = lessonManager.getAllLessons(),
            lessonManager = lessonManager,
            onDismiss = { editingSpecificLesson = null },
            onSave = { lessonNumber, lesson, cabinet, additionalInfo ->
                val existing = specificLessonManager.getSpecificLesson(days[pagerState.currentPage], lessonNumber)
                if (existing != null && existing.id != specificLesson.id) {
                    Toast.makeText(context, "Урок с таким номером уже существует", Toast.LENGTH_SHORT).show()
                } else {
                    specificLessonManager.updateSpecificLesson(
                        id = specificLesson.id,
                        day = days[pagerState.currentPage],
                        lessonNumber = lessonNumber,
                        lesson = lesson,
                        cabinet = cabinet,
                        additionalInfo = additionalInfo
                    )
                    coroutineScope.launch {
                        specificLessonManager.apply()
                        specificLessons = specificLessonManager.getAllSpecificLessons()
                        editingSpecificLesson = null
                    }
                }
            }
        )
    }

    deletingSpecificLesson?.let { specificLesson ->
        AlertDialog(
            onDismissRequest = { deletingSpecificLesson = null },
            title = { Text("Удалить урок из расписания?") },
            text = {
                val lesson = lessonManager.getLessonById(specificLesson.lessonId)
                Text("Вы уверены, что хотите удалить ${lesson?.name ?: "урок"} (${specificLesson.lessonNumber})?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        specificLessonManager.deleteSpecificLesson(specificLesson.id)
                        coroutineScope.launch {
                            specificLessonManager.apply()
                            specificLessons = specificLessonManager.getAllSpecificLessons()
                            deletingSpecificLesson = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Удалить")
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingSpecificLesson = null }) {
                    Text("Отмена")
                }
            }
        )
    }
}

@Composable
fun SpecificLessonItem(
    specificLesson: SpecificLesson,
    lesson: Lesson,
    onEdit: (SpecificLesson) -> Unit,
    onDelete: (SpecificLesson) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEdit(specificLesson) }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp)
            ) {
                Text(
                    text = "${specificLesson.lessonNumber}. ${lesson.name}",
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Кабинет: ${specificLesson.cabinet}",
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Инфо: ${specificLesson.additionalInfo}",
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            IconButton(onClick = { onDelete(specificLesson) }) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    tint = MaterialTheme.colorScheme.error,
                    contentDescription = "Удалить"
                )
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSpecificLessonDialog(
    day: DayOfWeek,
    lessons: List<Lesson>,
    onDismiss: () -> Unit,
    onAdd: (Int, Lesson, String, String) -> Unit
) {
    var lessonNumber by remember { mutableStateOf("") }
    var selectedLesson by remember { mutableStateOf<Lesson?>(null) }
    var cabinet by remember { mutableStateOf("") }
    var additionalInfo by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val dayNames = mapOf(
        DayOfWeek.MONDAY to "Пн",
        DayOfWeek.TUESDAY to "Вт",
        DayOfWeek.WEDNESDAY to "Ср",
        DayOfWeek.THURSDAY to "Чт",
        DayOfWeek.FRIDAY to "Пт",
        DayOfWeek.SATURDAY to "Сб",
        DayOfWeek.SUNDAY to "Вс"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Добавить урок на ${dayNames[day] ?: day.toString()}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = lessonNumber,
                    onValueChange = { if (it.all { char -> char.isDigit() } || it.isEmpty()) lessonNumber = it },
                    label = { Text("Номер урока") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                var expanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    TextField(
                        value = selectedLesson?.name ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Урок") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        colors = ExposedDropdownMenuDefaults.textFieldColors(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        lessons.forEach { lesson ->
                            DropdownMenuItem(
                                text = { Text(lesson.name) },
                                onClick = {
                                    selectedLesson = lesson
                                    expanded = false
                                }
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = cabinet,
                    onValueChange = { cabinet = it },
                    label = { Text("Кабинет") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = additionalInfo,
                    onValueChange = { additionalInfo = it },
                    label = { Text("Дополнительная информация") },
                    modifier = Modifier.fillMaxWidth()
                )
                errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val num = lessonNumber.toIntOrNull()
                    if (num == null || num <= 0) {
                        errorMessage = "Неверный номер урока"
                        return@Button
                    }
                    if (selectedLesson == null) {
                        errorMessage = "Выберите урок"
                        return@Button
                    }
                    errorMessage = null
                    onAdd(num, selectedLesson!!, cabinet, additionalInfo)
                }
            ) {
                Text("Добавить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditSpecificLessonDialog(
    specificLesson: SpecificLesson,
    lessons: List<Lesson>,
    lessonManager: LessonManager,
    onDismiss: () -> Unit,
    onSave: (Int, Lesson, String, String) -> Unit
) {
    val currentLesson = lessonManager.getLessonById(specificLesson.lessonId)
    var lessonNumber by remember { mutableStateOf(specificLesson.lessonNumber.toString()) }
    var selectedLesson by remember { mutableStateOf(currentLesson) }
    var cabinet by remember { mutableStateOf(specificLesson.cabinet) }
    var additionalInfo by remember { mutableStateOf(specificLesson.additionalInfo) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Редактировать урок") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = lessonNumber,
                    onValueChange = { if (it.all { char -> char.isDigit() } || it.isEmpty()) lessonNumber = it },
                    label = { Text("Номер урока") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                var expanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    TextField(
                        value = selectedLesson?.name ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Урок") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        colors = ExposedDropdownMenuDefaults.textFieldColors(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        lessons.forEach { lesson ->
                            DropdownMenuItem(
                                text = { Text(lesson.name) },
                                onClick = {
                                    selectedLesson = lesson
                                    expanded = false
                                }
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = cabinet,
                    onValueChange = { cabinet = it },
                    label = { Text("Кабинет") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = additionalInfo,
                    onValueChange = { additionalInfo = it },
                    label = { Text("Дополнительная информация") },
                    modifier = Modifier.fillMaxWidth()
                )
                errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val num = lessonNumber.toIntOrNull()
                    if (num == null || num <= 0) {
                        errorMessage = "Неверный номер урока"
                        return@Button
                    }
                    if (selectedLesson == null) {
                        errorMessage = "Выберите урок"
                        return@Button
                    }
                    errorMessage = null
                    onSave(num, selectedLesson!!, cabinet, additionalInfo)
                }
            ) {
                Text("Сохранить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}