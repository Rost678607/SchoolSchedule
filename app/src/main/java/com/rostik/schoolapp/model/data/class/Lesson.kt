package com.rostik.schoolapp.model.data

import java.io.Serializable

data class Lesson(
    val id: Int,
    var name: String,
    var teacher: String,
    var homework: String = ""
) : Serializable