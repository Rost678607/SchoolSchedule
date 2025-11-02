package com.rostik.schoolapp

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import com.rostik.schoolapp.ui.NavigableApp

class MainActivity : ComponentActivity() {
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val initialUri = if (intent?.action == android.content.Intent.ACTION_VIEW) intent.data else null
        setContent {
            NavigableApp(applicationContext, initialUri)
        }
    }
}