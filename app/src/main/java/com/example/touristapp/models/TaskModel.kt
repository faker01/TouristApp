package com.example.touristapp.models

import java.io.Serializable

/**
 * Модель задания для загрузки из CSV
 *
 * Формат из CSV (колонка "текст"): условие::правильный_ответ::вариант1::вариант2::...
 * Формат из CSV (колонка "результат"): тип_задания::награда::стоимость_подсказки::подсказка::бонусное::следующий_ID
 *
 * @property id Уникальный ID задания
 * @property text Условие/текст задания
 * @property correctAnswer Правильный ответ
 * @property options Варианты для MULTIPLE_CHOICE
 * @property taskType Тип задания
 * @property sparkReward Награда в искрах
 * @property isBonus Бонусное задание?
 * @property hint Подсказка
 * @property hintCost Стоимость подсказки
 * @property nextElementId ID следующего элемента (диалога или задания)
 */
data class TaskModel(
    override val id: String,
    override val type: ElementType = ElementType.TASK,
    val text: String,
    val correctAnswer: String,
    val options: List<String> = emptyList(),
    val taskType: TaskType,
    val sparkReward: Int = 1,
    val isBonus: Boolean = false,
    val hint: String = "",
    val hintCost: Int = 1,
    val nextElementId: String? = null,
    override val trigger: Trigger? = null
) : BaseQuestModel(id, type, trigger), Serializable {

    /**
     * Проверяет ответ пользователя
     */
    fun checkAnswer(userAnswer: String): Boolean {
        return userAnswer.trim().lowercase() == correctAnswer.trim().lowercase()
    }

    /**
     * Конвертирует TaskModel в существующий QuestTask
     *
     * @param taskIndex Порядковый номер задания в локации (для id)
     */
    fun toQuestTask(taskIndex: Int): QuestTask {
        return QuestTask(
            id = taskIndex,
            text = text,
            type = taskType,
            correctAnswer = correctAnswer,
            options = options,
            sparkReward = sparkReward,
            isBonus = isBonus,
            hint = hint,
            hintCost = hintCost
        )
    }
}