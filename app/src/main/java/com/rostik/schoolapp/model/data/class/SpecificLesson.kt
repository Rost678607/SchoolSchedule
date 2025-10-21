package com.rostik.schoolapp.model.data

import java.io.Serializable
import java.time.DayOfWeek

data class SpecificLesson(
    val id: Int,
    var day: DayOfWeek,
    var lessonNumber: Int,
    var lessonId: Int,
    var cabinet: String,
    var additionalInfo: String
) : Serializable