package com.example.touristapp.models

import java.io.Serializable

// Тип задания
enum class TaskType {
    PHOTO,          // Сделать фото
    NUMBER_INPUT,   // Ввести число
    TEXT_INPUT,     // Ввести текст
    MULTIPLE_CHOICE // Выбрать из вариантов
}

data class QuestTask(
    val id: Int,
    val text: String,           // Текст загадки
    val type: TaskType,
    val correctAnswer: String,  // Правильный ответ (в нижнем регистре)
    val options: List<String> = emptyList(), // Варианты для MULTIPLE_CHOICE
    val sparkReward: Int = 1,   // Награда в искрах
    val isBonus: Boolean = false,
    val hint: String = "",
    val hintCost: Int = 1       // Стоимость подсказки в искрах
) : Serializable

data class QuestLocation(
    val id: Int,
    val homlinName: String,         // Имя хомлина
    val locationName: String,       // Название места
    val lat: Double,
    val lon: Double,
    val shardName: String,          // Название осколка
    val arrivalText: String,        // Текст при приближении
    val tasks: List<QuestTask>,
    val minTasksRequired: Int = 4,  // Минимум заданий для продолжения
    val sparkRewardForShard: Int = 3
) : Serializable

data class QuestProgress(
    val role: String = "Янтарный Детектив",
    val sparks: Int = 10,
    val completedLocations: List<Int> = emptyList(),   // id пройденных локаций
    val completedTasks: Map<Int, List<Int>> = emptyMap(), // locationId -> [taskId]
    val currentLocationIndex: Int = 0,
    val isCompleted: Boolean = false
) : Serializable
