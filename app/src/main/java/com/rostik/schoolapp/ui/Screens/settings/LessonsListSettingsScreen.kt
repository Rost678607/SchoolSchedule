package com.rostik.schoolapp.ui

import android.annotation.SuppressLint
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rostik.schoolapp.model.data.Lesson
import com.rostik.schoolapp.model.data.LessonManager
import kotlinx.coroutines.launch

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun LessonsListSettingsScreen(lessonManager: LessonManager) {
    var lessons by remember { mutableStateOf(lessonManager.getAllLessons()) }
    val coroutineScope = rememberCoroutineScope()
    var showAddDialog by remember { mutableStateOf(false) }
    var editingLesson by remember { mutableStateOf<Lesson?>(null) }
    var lessonToDelete by remember { mutableStateOf<Lesson?>(null) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        lessonManager.load()
        lessons = lessonManager.getAllLessons()
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                content = {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = "Добавить урок",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                },
                containerColor = MaterialTheme.colorScheme.primary
            )
        }
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
            items(lessons) { lesson ->
                LessonItem(
                    lesson = lesson,
                    onEdit = { editingLesson = it },
                    onDeleteRequest = { lessonToDelete = it }
                )
            }
            item {
                Spacer(modifier = Modifier.height(84.dp))
            }
        }
    }

    if (showDeleteConfirmDialog && lessonToDelete != null) {
        AlertDialog(
            onDismissRequest = {
                showDeleteConfirmDialog = false
                lessonToDelete = null
            },
            title = { Text("Удалить урок?") },
            text = {
                Text("Вы уверены, что хотите удалить урок \"${lessonToDelete!!.name}\"? Это действие нельзя отменить.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        lessonManager.deleteLesson(lessonToDelete!!.id)
                        coroutineScope.launch {
                            lessonManager.apply()
                            lessons = lessonManager.getAllLessons()
                        }
                        showDeleteConfirmDialog = false
                        lessonToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Удалить")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showDeleteConfirmDialog = false
                    lessonToDelete = null
                }) {
                    Text("Отмена")
                }
            }
        )
    }

    if (showAddDialog) {
        AddLessonDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { name, teacher ->
                lessonManager.addLesson(name, teacher)
                coroutineScope.launch {
                    lessonManager.apply()
                    lessons = lessonManager.getAllLessons()
                    showAddDialog = false
                }
            }
        )
    }

    editingLesson?.let { lesson ->
        EditLessonDialog(
            lesson = lesson,
            onDismiss = { editingLesson = null },
            onSave = { name, teacher ->
                lessonManager.updateLesson(lesson.id, name, teacher)
                coroutineScope.launch {
                    lessonManager.apply()
                    lessons = lessonManager.getAllLessons()
                    editingLesson = null
                }
            }
        )
    }

    LaunchedEffect(lessonToDelete) {
        if (lessonToDelete != null) {
            showDeleteConfirmDialog = true
        }
    }
}

@Composable
fun LessonItem(
    lesson: Lesson,
    onEdit: (Lesson) -> Unit,
    onDeleteRequest: (Lesson) -> Unit
) {
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
            Column {
                Text(
                    text = lesson.name,
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = lesson.teacher,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            IconButton(onClick = { onDeleteRequest(lesson) }) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    tint = MaterialTheme.colorScheme.error,
                    contentDescription = "Удалить урок"
                )
            }
        }
    }
}

@Composable
fun AddLessonDialog(
    onDismiss: () -> Unit,
    onAdd: (String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var teacher by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Добавить урок") },
        text = {
            Column {
                TextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Название урока") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                )
                TextField(
                    value = teacher,
                    onValueChange = { teacher = it },
                    label = { Text("Учитель") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank() && teacher.isNotBlank()) {
                        onAdd(name, teacher)
                    }
                },
                enabled = name.isNotBlank() && teacher.isNotBlank()
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

@Composable
fun EditLessonDialog(
    lesson: Lesson,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    var name by remember { mutableStateOf(lesson.name) }
    var teacher by remember { mutableStateOf(lesson.teacher) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Редактировать урок") },
        text = {
            Column {
                TextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Название урока") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                )
                TextField(
                    value = teacher,
                    onValueChange = { teacher = it },
                    label = { Text("Учитель") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank() && teacher.isNotBlank()) {
                        onSave(name, teacher)
                    }
                },
                enabled = name.isNotBlank() && teacher.isNotBlank()
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