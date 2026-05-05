package com.example.touristapp.models

import java.io.Serializable

// Тип задания
enum class TaskType {
    PHOTO,          // Сделать фото
    NUMBER_INPUT,   // Ввести число
    TEXT_INPUT,     // Ввести текст
    MULTIPLE_CHOICE // Выбрать из вариантов
}

/**
 * Задание квеста
 *
 * Существующая модель — НЕ МЕНЯЕТСЯ, только добавлены новые поля
 */
data class QuestTask(
    val id: Int,
    val text: String,           // Текст загадки
    val type: TaskType,
    val correctAnswer: String,  // Правильный ответ (в нижнем регистре)
    val options: List<String> = emptyList(), // Варианты для MULTIPLE_CHOICE
    val sparkReward: Int = 1,   // Награда в искрах
    val isBonus: Boolean = false,
    val hint: String = "",
    val hintCost: Int = 1,      // Стоимость подсказки в искрах
    // НОВЫЕ ПОЛЯ (опционально, для связи с новой системой)
    val sourceId: String? = null // ID исходного TaskModel из CSV
) : Serializable

/**
 * Локация квеста
 *
 * Существующая модель — НЕ МЕНЯЕТСЯ, только добавлены новые поля
 */
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
    val sparkRewardForShard: Int = 3,
    // НОВЫЕ ПОЛЯ (опционально, для связи с новой системой)
    val dialogues: List<DialogueModel> = emptyList(), // Диалоги для этой локации
    val result: ResultModel? = null // Результат после прохождения локации
) : Serializable

/**
 * Прогресс квеста
 *
 * Существующая модель — РАСШИРЕНА для поддержки новой системы
 */
data class QuestProgress(
    val role: String = "Янтарный Детектив",
    val sparks: Int = 10,
    val completedLocations: List<Int> = emptyList(),   // id пройденных локаций
    val completedTasks: Map<Int, List<Int>> = emptyMap(), // locationId -> [taskId]
    val currentLocationIndex: Int = 0,
    val isCompleted: Boolean = false,
    // НОВЫЕ ПОЛЯ (для поддержки диалогов)
    val currentDialogueId: String? = null,   // Текущий диалог (из CSV)
    val currentTaskId: String? = null,       // Текущее задание (из CSV)
    val completedDialogues: List<String> = emptyList(), // Пройденные диалоги
    val completedResults: List<String> = emptyList()   // Полученные результаты
) : Serializable {

    /**
     * Добавить пройденный диалог
     */
    fun addCompletedDialogue(dialogueId: String): QuestProgress {
        return copy(completedDialogues = completedDialogues + dialogueId)
    }

    /**
     * Добавить полученный результат
     */
    fun addCompletedResult(resultId: String): QuestProgress {
        return copy(completedResults = completedResults + resultId)
    }

    /**
     * Обновить текущий диалог
     */
    fun setCurrentDialogue(dialogueId: String?): QuestProgress {
        return copy(currentDialogueId = dialogueId, currentTaskId = null)
    }

    /**
     * Обновить текущее задание
     */
    fun setCurrentTask(taskId: String?): QuestProgress {
        return copy(currentTaskId = taskId, currentDialogueId = null)
    }

    /**
     * Добавить искры
     */
    fun addSparks(amount: Int): QuestProgress {
        return copy(sparks = sparks + amount)
    }

    /**
     * Завершить локацию
     */
    fun completeLocation(locationId: Int): QuestProgress {
        return copy(completedLocations = completedLocations + locationId)
    }

    /**
     * Завершить задание в локации
     */
    fun completeTask(locationId: Int, taskId: Int): QuestProgress {
        val currentCompleted = completedTasks[locationId] ?: emptyList()
        val newCompleted = currentCompleted + taskId
        return copy(completedTasks = completedTasks + (locationId to newCompleted))
    }
}

/**
 * Фабрика для создания QuestProgress из новой системы
 */
object QuestProgressFactory {

    /**
     * Создаёт прогресс из данных загруженных из CSV
     */
    fun fromQuestData(questData: LoadedQuestData): QuestProgress {
        return QuestProgress(
            role = "Янтарный Детектив",
            sparks = 10,
            currentDialogueId = questData.startDialogueId
        )
    }
}

/**
 * Собранные данные квеста из CSV
 */
data class LoadedQuestData(
    val dialogues: Map<String, DialogueModel>,
    val tasks: Map<String, TaskModel>,
    val results: Map<String, ResultModel>,
    val startDialogueId: String?
) {
    fun getStartDialogue(): DialogueModel? = startDialogueId?.let { dialogues[it] }

    fun getElementById(id: String): BaseQuestModel? {
        return dialogues[id] ?: tasks[id] ?: results[id]
    }

    fun getNextElement(currentId: String): BaseQuestModel? {
        val element = getElementById(currentId) ?: return null
        val nextId = when (element) {
            is DialogueModel -> element.nextElementId
            is TaskModel -> element.nextElementId
            else -> null
        }
        return nextId?.let { getElementById(it) }
    }
}