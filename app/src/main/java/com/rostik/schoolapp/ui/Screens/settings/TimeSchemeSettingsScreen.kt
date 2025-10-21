package com.rostik.schoolapp.ui

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.TimePickerDialog
import android.content.Context
import android.os.Build
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.rostik.schoolapp.model.data.TimeScheme
import com.rostik.schoolapp.model.data.TimeSchemeManager
import kotlinx.coroutines.launch
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun TimeSchemeSettingsScreen(timeSchemeManager: TimeSchemeManager) {
    var timeScheme by remember { mutableStateOf<TimeScheme?>(null) }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showResetDialog by remember { mutableStateOf(false) }
    var showAddBreakDialog by remember { mutableStateOf(false) }

    var startTimeSeconds by remember { mutableStateOf(0) }
    var lessonLengthMinutes by remember { mutableStateOf("") }
    val breaksMinutes: SnapshotStateList<Int> = remember { mutableStateListOf() }

    LaunchedEffect(Unit) {
        coroutineScope.launch {
            timeSchemeManager.load()
            timeScheme = timeSchemeManager.getTimeScheme()
            timeScheme?.let { scheme ->
                startTimeSeconds = scheme.start.toSecondOfDay()
                lessonLengthMinutes = scheme.lessonLength.toString()
                breaksMinutes.clear()
                breaksMinutes.addAll(scheme.breaks)
            }
        }
    }

    if (showResetDialog) {
        AlertDialog.Builder(context)
            .setTitle("Сброс настроек")
            .setMessage("Вы уверены, что хотите сбросить настройки к значениям по умолчанию?")
            .setPositiveButton("Подтвердить") { _, _ ->
                coroutineScope.launch {
                    timeSchemeManager.resetToDefault()
                    timeSchemeManager.apply()
                    timeScheme = timeSchemeManager.getTimeScheme()
                    timeScheme?.let { scheme ->
                        startTimeSeconds = scheme.start.toSecondOfDay()
                        lessonLengthMinutes = scheme.lessonLength.toString()
                        breaksMinutes.clear()
                        breaksMinutes.addAll(scheme.breaks)
                    }
                    errorMessage = null
                    showResetDialog = false
                    Toast.makeText(context, "Настройки сброшены к умолчанию", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Отмена") { _, _ ->
                showResetDialog = false
            }
            .setOnCancelListener {
                showResetDialog = false
            }
            .show()
    }

    if (showAddBreakDialog) {
        AddBreakDialog(
            onDismiss = { showAddBreakDialog = false },
            onAdd = { minutes ->
                breaksMinutes.add(minutes)
                coroutineScope.launch {
                    timeSchemeManager.setBreaks(breaksMinutes)
                    timeSchemeManager.apply()
                    showAddBreakDialog = false
                }
            }
        )
    }

    timeScheme?.let { scheme ->
        Scaffold(
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { showAddBreakDialog = true },
                    content = {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = "Добавить перерыв",
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
                    .padding(horizontal = 16.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    TimePickerField(
                        label = "Начало занятий",
                        seconds = startTimeSeconds,
                        context = context,
                        onTimeSelected = { newSeconds ->
                            startTimeSeconds = newSeconds
                            coroutineScope.launch {
                                timeSchemeManager.updateTimeScheme(
                                    start = LocalTime.ofSecondOfDay(newSeconds.toLong()),
                                    lessonLength = lessonLengthMinutes.toIntOrNull() ?: scheme.lessonLength
                                )
                                timeSchemeManager.apply()
                            }
                        }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = lessonLengthMinutes,
                        onValueChange = { value ->
                            if (value.all { char -> char.isDigit() } || value.isEmpty()) {
                                lessonLengthMinutes = value
                                errorMessage = null
                                if (value.isNotEmpty() && value.toIntOrNull() != null && value.toInt() > 0) {
                                    coroutineScope.launch {
                                        timeSchemeManager.updateTimeScheme(
                                            start = LocalTime.ofSecondOfDay(startTimeSeconds.toLong()),
                                            lessonLength = value.toInt()
                                        )
                                        timeSchemeManager.apply()
                                    }
                                }
                            } else {
                                errorMessage = "Только цифры"
                            }
                        },
                        label = { Text("Длительность урока (минуты)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        isError = errorMessage != null,
                        supportingText = {
                            errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Перерывы",
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Start,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
                itemsIndexed(breaksMinutes) { index, minutes ->
                    BreakItem(
                        minutes = minutes,
                        index = index,
                        onEdit = { newMinutes ->
                            breaksMinutes[index] = newMinutes
                            coroutineScope.launch {
                                timeSchemeManager.setBreaks(breaksMinutes)
                                timeSchemeManager.apply()
                            }
                        },
                        onDelete = {
                            breaksMinutes.removeAt(index)
                            coroutineScope.launch {
                                timeSchemeManager.setBreaks(breaksMinutes)
                                timeSchemeManager.apply()
                            }
                        }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Button(
                            onClick = { showResetDialog = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Сбросить")
                        }
                        Spacer(modifier = Modifier.height(64.dp))
                    }
                }
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun TimePickerField(
    label: String,
    seconds: Int,
    context: Context,
    onTimeSelected: (Int) -> Unit
) {
    val time = LocalTime.ofSecondOfDay(seconds.toLong())
    val formattedTime = time.format(DateTimeFormatter.ofPattern("HH:mm"))

    Button(
        onClick = {
            val timePicker = TimePickerDialog(
                context,
                { _, hour, minute ->
                    onTimeSelected(hour * 3600 + minute * 60)
                },
                time.hour,
                time.minute,
                true
            )
            timePicker.show()
        },
        modifier = Modifier.fillMaxWidth()
    ) {
        Text("$label: $formattedTime")
    }
}

@Composable
fun BreakItem(
    minutes: Int,
    index: Int,
    onEdit: (Int) -> Unit,
    onDelete: () -> Unit
) {
    var showEditDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { showEditDialog = true }
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
                    text = "Перерыв ${index + 1}",
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = "$minutes мин",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    tint = MaterialTheme.colorScheme.error,
                    contentDescription = "Удалить перерыв"
                )
            }
        }
    }

    if (showEditDialog) {
        EditBreakDialog(
            minutes = minutes,
            index = index,
            onDismiss = { showEditDialog = false },
            onSave = { newMinutes ->
                onEdit(newMinutes)
                showEditDialog = false
            }
        )
    }
}

@Composable
fun AddBreakDialog(
    onDismiss: () -> Unit,
    onAdd: (Int) -> Unit
) {
    var minutes by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Добавить перерыв") },
        text = {
            Column {
                OutlinedTextField(
                    value = minutes,
                    onValueChange = { value ->
                        if (value.all { it.isDigit() } || value.isEmpty()) {
                            minutes = value
                            errorMessage = null
                        } else {
                            errorMessage = "Только цифры"
                        }
                    },
                    label = { Text("Длительность перерыва (минуты)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = errorMessage != null || (minutes.isNotEmpty() && minutes.toIntOrNull() == 0),
                    supportingText = {
                        when {
                            errorMessage != null -> Text(errorMessage!!, color = MaterialTheme.colorScheme.error)
                            minutes.isNotEmpty() && minutes.toIntOrNull() == 0 -> Text("Должно быть больше 0", color = MaterialTheme.colorScheme.error)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val minutesInt = minutes.toIntOrNull()
                    if (minutesInt != null && minutesInt > 0) {
                        onAdd(minutesInt)
                    } else {
                        errorMessage = "Введите корректное значение"
                    }
                },
                enabled = minutes.isNotEmpty() && minutes.toIntOrNull() != null && minutes.toInt() > 0
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
fun EditBreakDialog(
    minutes: Int,
    index: Int,
    onDismiss: () -> Unit,
    onSave: (Int) -> Unit
) {
    var inputMinutes by remember { mutableStateOf(minutes.toString()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Редактировать перерыв ${index + 1}") },
        text = {
            Column {
                OutlinedTextField(
                    value = inputMinutes,
                    onValueChange = { value ->
                        if (value.all { it.isDigit() } || value.isEmpty()) {
                            inputMinutes = value
                            errorMessage = null
                        } else {
                            errorMessage = "Только цифры"
                        }
                    },
                    label = { Text("Длительность перерыва (минуты)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = errorMessage != null || (inputMinutes.isNotEmpty() && inputMinutes.toIntOrNull() == 0),
                    supportingText = {
                        when {
                            errorMessage != null -> Text(errorMessage!!, color = MaterialTheme.colorScheme.error)
                            inputMinutes.isNotEmpty() && inputMinutes.toIntOrNull() == 0 -> Text("Должно быть больше 0", color = MaterialTheme.colorScheme.error)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val minutesInt = inputMinutes.toIntOrNull()
                    if (minutesInt != null && minutesInt > 0) {
                        onSave(minutesInt)
                    } else {
                        errorMessage = "Введите корректное значение"
                    }
                },
                enabled = inputMinutes.isNotEmpty() && inputMinutes.toIntOrNull() != null && inputMinutes.toInt() > 0
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