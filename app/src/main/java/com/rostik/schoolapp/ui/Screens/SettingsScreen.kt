package com.rostik.schoolapp.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController

@Composable
fun SettingsScreen(navController: NavHostController) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
    ) {
        item {
            SettingItem(
                text = "Уроки",
                description = "Изменение списка уроков",
                onClick = { navController.navigate("settings/lessons_list") }
            )
        }
        item {
            SettingItem(
                text = "Расписание уроков",
                description = "Изменение расписания уроков",
                onClick = { navController.navigate("settings/lessons") }
            )
        }
        item {
            SettingItem(
                text = "Расписание звонков",
                description = "Изменение расписания звонков",
                onClick = { navController.navigate("settings/time_scheme") }
            )
        }
    }
}

@Composable
fun SettingItem(text: String, description: String = "", onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 8.dp)
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(6.dp)
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.titleLarge
            )
            if (description.isNotEmpty()) {
                Text(
                    modifier = Modifier.padding(top = 4.dp),
                    text = description,
                    color = MaterialTheme.colorScheme.tertiary,
                    style = MaterialTheme.typography.titleSmall
                )
            }
        }
    }
}