package com.example.touristapp.models

import java.io.Serializable

/**
 * Модель результата выполнения миссии/задания/диалога
 *
 * Формат из CSV (колонка "текст"): сообщение
 * Формат из CSV (колонка "результат"): награда_искрами::следующая_локация::финальный::достижение1::достижение2
 *
 * @property id Уникальный ID результата
 * @property message Сообщение о результате
 * @property sparksReward Награда искрами
 * @property nextLocationId ID следующей локации
 * @property isFinal Флаг финального результата
 * @property achievements Полученные достижения
 */
data class ResultModel(
    override val id: String,
    override val type: ElementType = ElementType.RESULT,
    val message: String,
    val sparksReward: Int = 0,
    val nextLocationId: Int? = null,
    val isFinal: Boolean = false,
    val achievements: List<String> = emptyList(),
    override val trigger: Trigger? = null
) : BaseQuestModel(id, type, trigger), Serializable {

    companion object {
        fun success(message: String, sparks: Int = 0): ResultModel {
            return ResultModel(
                id = "success_${System.currentTimeMillis()}",
                message = message,
                sparksReward = sparks
            )
        }

        fun failure(message: String): ResultModel {
            return ResultModel(
                id = "failure_${System.currentTimeMillis()}",
                message = message,
                sparksReward = 0
            )
        }
    }
}