package com.example.touristapp.models

import java.io.Serializable

/**
 * Модель диалога с персонажем
 *
 * Формат из CSV (колонка "текст"): персонаж::текст::ссылка_на_изображение
 * Формат из CSV (колонка "результат"): награда_искрами::следующий_ID
 *
 * @property id Уникальный ID диалога
 * @property characterName Имя персонажа
 * @property text Текст диалога
 * @property imageUrl Ссылка на изображение персонажа (или resource ID)
 * @property sparksReward Награда искрами после диалога
 * @property nextElementId ID следующего элемента (диалога или задания)
 */
data class DialogueModel(
    override val id: String,
    override val type: ElementType = ElementType.DIALOGUE,
    val characterName: String,
    val text: String,
    val imageUrl: String? = null,
    val sparksReward: Int = 0,
    val nextElementId: String? = null,
    override val trigger: Trigger? = null
) : BaseQuestModel(id, type, trigger), Serializable {

    fun isFinal(): Boolean = nextElementId == null || nextElementId == "END"

    fun formatDisplay(): String = "$characterName: $text"
}