package com.rostik.schoolapp.ui

import android.annotation.SuppressLint
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.rostik.schoolapp.model.data.Lesson
import com.rostik.schoolapp.model.data.LessonManager
import kotlinx.coroutines.launch

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter", "UnrememberedMutableState")
@Composable
fun HomeworkScreen(lessonManager: LessonManager) {
    var lessons by remember { mutableStateOf(lessonManager.getAllLessons()) }
    val coroutineScope = rememberCoroutineScope()
    var editingLesson by remember { mutableStateOf<Lesson?>(null) }

    LaunchedEffect(Unit) {
        lessonManager.load()
        lessons = lessonManager.getAllLessons()
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
        items(lessons, key = { it.id }) { lesson ->
            HomeworkItem(
                lesson = lesson,
                onEdit = { editingLesson = it },
                onDelete = {
                    lessonManager.updateLessonHomework(lesson.id, "")
                    coroutineScope.launch {
                        lessonManager.apply()
                        lessons = lessons.map {
                            if (it.id == lesson.id) it.copy(homework = "") else it
                        }
                    }
                }
            )
        }
        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    editingLesson?.let { lesson ->
        EditHomeworkDialog(
            lesson = lesson,
            onDismiss = { editingLesson = null },
            onSave = { homework ->
                lessonManager.updateLessonHomework(lesson.id, homework)
                coroutineScope.launch {
                    lessonManager.apply()
                    lessons = lessons.map {
                        if (it.id == lesson.id) it.copy(homework = homework) else it
                    }
                    editingLesson = null
                }
            }
        )
    }
}

@Composable
fun HomeworkItem(
    lesson: Lesson,
    onEdit: (Lesson) -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteConfirmation by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEdit(lesson) }
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
                    text = lesson.name,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = lesson.homework,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            IconButton(onClick = { showDeleteConfirmation = true }) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    tint = MaterialTheme.colorScheme.error,
                    contentDescription = "Delete homework"
                )
            }
        }
    }

    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("Удалить домашнее задание") },
            text = { Text("Вы уверены, что хотите удалить домашнее задание для ${lesson.name}?") },
            confirmButton = {
                Button(
                    onClick = {
                        onDelete()
                        showDeleteConfirmation = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Удалить")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) {
                    Text("Отмена")
                }
            }
        )
    }
}

@Composable
fun EditHomeworkDialog(
    lesson: Lesson,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var homework by remember { mutableStateOf(lesson.homework) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Изменить домашнее задание по ${lesson.name}") },
        text = {
            Column {
                TextField(
                    value = homework,
                    onValueChange = { homework = it },
                    label = { Text("Домашнее задание") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (homework.isNotBlank()) {
                        onSave(homework)
                    }
                },
                enabled = homework.isNotBlank()
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