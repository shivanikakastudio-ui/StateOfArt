package dev.abhinav.myapplication.quiz.data

data class Question(
    val id: String,
    val text: String,
    val options: List<String>,
    val correctIndex: Int,
)
