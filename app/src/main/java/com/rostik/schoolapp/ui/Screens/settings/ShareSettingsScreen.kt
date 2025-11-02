package com.rostik.schoolapp.ui

import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.rostik.schoolapp.model.ShareManager
import com.rostik.schoolapp.model.data.LessonManager
import com.rostik.schoolapp.model.data.SpecificLessonManager
import com.rostik.schoolapp.model.data.TimeSchemeManager
import kotlinx.coroutines.launch

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun ShareSettingsScreen(
    lessonManager: LessonManager,
    specificLessonManager: SpecificLessonManager,
    timeSchemeManager: TimeSchemeManager,
    shareManager: ShareManager,
    initialUri: Uri? = null
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var showExportDialog by remember { mutableStateOf(false) }
    var showImportConfirmation by remember { mutableStateOf(false) }
    var selectedImportUri by remember { mutableStateOf<Uri?>(null) }

    LaunchedEffect(initialUri) {
        if (initialUri != null) {
            selectedImportUri = initialUri
            showImportConfirmation = true
        }
    }

    val saveLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("*/*")
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                try {
                    lessonManager.load()
                    specificLessonManager.load()
                    timeSchemeManager.load()

                    val json = shareManager.getExportJson()
                    context.contentResolver.openOutputStream(uri)?.use { os ->
                        os.write(json.toByteArray())
                    }
                    Toast.makeText(context, "Сохранено", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(context, "Ошибка", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            if (shareManager.isValidFile(uri)) {
                selectedImportUri = uri
                showImportConfirmation = true
            } else {
                Toast.makeText(context, "Неверный формат файла", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        ListItem("Импорт") {
            importLauncher.launch("*/*")
        }
        ListItem("Экспорт") {
            showExportDialog = true
        }
    }

    if (showExportDialog) {
        ExportDialog(
            onDismiss = { showExportDialog = false },
            onSave = { filename ->
                val finalName = if (filename.endsWith(".schoe")) filename else "$filename.schoe"
                saveLauncher.launch(finalName)
                showExportDialog = false
            },
            onShare = { filename ->
                val baseName = filename.substringBeforeLast(".schoe", filename)
                coroutineScope.launch {
                    lessonManager.load()
                    specificLessonManager.load()
                    timeSchemeManager.load()
                    shareManager.exportAndShare(baseName)
                }
                showExportDialog = false
            }
        )
    }

    if (showImportConfirmation && selectedImportUri != null) {
        ImportConfirmationDialog(
            onDismiss = {
                showImportConfirmation = false
                selectedImportUri = null
            },
            onConfirm = {
                val uri = selectedImportUri!!
                showImportConfirmation = false
                selectedImportUri = null
                coroutineScope.launch {
                    try {
                        shareManager.importFromUri(uri)
                        Toast.makeText(context, "Импорт завершён", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        e.printStackTrace()
                        Toast.makeText(context, "Ошибка импорта: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        )
    }
}

@Composable
fun ListItem(text: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 8.dp)
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Text(
            modifier = Modifier.padding(16.dp),
            text = text,
            style = MaterialTheme.typography.titleLarge
        )
    }
}

@Composable
fun ExportDialog(
    onDismiss: () -> Unit,
    onSave: (filename: String) -> Unit,
    onShare: (filename: String) -> Unit
) {
    var filename by remember { mutableStateOf("schedule") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Экспорт настроек") },
        text = {
            Column {
                Text("Имя файла")
                Spacer(modifier = Modifier.height(8.dp))
                TextField(
                    value = filename,
                    onValueChange = { filename = it },
                    label = { Text("Имя файла") },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (filename.isNotBlank()) {
                        onSave(filename.trim())
                    }
                },
                enabled = filename.isNotBlank()
            ) {
                Text("Сохранить в файлы")
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    if (filename.isNotBlank()) {
                        onShare(filename.trim())
                    }
                }
            ) {
                Text("Поделиться")
            }
        }
    )
}

@Composable
fun ImportConfirmationDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Импорт настроек") },
        text = {
            Text(
                "Все текущие настройки будут заменены на новые из файла.\n" +
                        "Текущие данные не сохранятся. Продолжить?"
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Подтвердить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}