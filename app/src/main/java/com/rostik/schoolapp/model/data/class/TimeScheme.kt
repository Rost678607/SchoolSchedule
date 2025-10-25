package com.rostik.schoolapp.model.data

import android.os.Build
import androidx.annotation.RequiresApi
import java.io.Serializable
import java.time.LocalTime

@RequiresApi(Build.VERSION_CODES.O)
data class TimeScheme(
    var start: LocalTime = LocalTime.of(9, 0),
    var lessonLength: Int = 45,
    val breaks: MutableList<Int> = mutableListOf(15, 15, 15, 15, 15, 15, 15),
    var defaultBreak: Int = 15,
    var coupleMiddleBreakLength: Int = 10,
    var isPairMode: Boolean = false
) : Serializable {
    companion object {
        private const val serialVersionUID: Long = 8620614583866301058L
    }
}