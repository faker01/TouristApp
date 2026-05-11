package com.example.touristapp.models

import java.io.Serializable

/**
 * Базовый абстрактный класс для всех элементов квеста
 *
 * @property id Уникальный идентификатор элемента
 * @property type Тип элемента (диалог, задание, результат, вступление)
 * @property trigger Триггер активации (геопозиция или условие)
 */
abstract class BaseQuestModel(
    open val id: String,
    open val type: ElementType,
    open val trigger: Trigger? = null
) : Serializable

/**
 * Типы элементов квеста
 */
enum class ElementType {
    INTRO,      // Вступительный текст (для начала квестовой цепочки)
    DIALOGUE,   // Диалог с персонажем
    TASK,       // Задание/квест
    RESULT      // Результат выполнения
}

/**
 * Триггер для активации элемента
 *
 * @param latitude Широта точки
 * @param longitude Долгота точки
 * @param radius Радиус срабатывания в метрах
 * @param condition Дополнительное условие (например, "после_задания_3")
 */
data class Trigger(
    val latitude: Double? = null,
    val longitude: Double? = null,
    val radius: Float = 50f,
    val condition: String? = null
) : Serializable {

    /**
     * Проверяет, является ли триггер геолокационным (имеет координаты)
     */
    fun isLocationTrigger(): Boolean = latitude != null && longitude != null

    /**
     * Проверяет, есть ли у триггера дополнительное условие
     */
    fun hasCondition(): Boolean = !condition.isNullOrEmpty()
}