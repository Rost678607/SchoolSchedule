package com.rostik.schoolapp.model.data

import java.io.Serializable
import java.time.DayOfWeek

data class Schedule(
    val schedule: Map<DayOfWeek, MutableMap<Int, SpecificLesson>>
) : Serializable