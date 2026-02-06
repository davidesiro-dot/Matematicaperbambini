package com.example.matematicaperbambini

import kotlinx.serialization.Serializable

@Serializable
data class TeacherHomeworkCode(
    val code: String,
    val description: String,
    val createdAt: Long,
    val seed: Long
)
