package com.example.touristapp.models

import java.io.Serializable

/**
 * Одно действие скрипта квеста.
 *
 * Типы (type):
 *   "toast"   — показать короткое всплывающее сообщение (text)
 *   "dialog"  — показать диалог с заголовком (title) и текстом (text)
 *   "navigate"— перейти на другой экран (destination: id ресурса навигации в виде строки)
 */
data class ScriptAction(
    val type: String,
    val title: String = "",
    val text: String  = "",
    val destination: String = ""
) : Serializable

/**
 * Скрипт квеста — список действий, которые выполняются последовательно
 * когда пользователь достигает финальной точки маршрута.
 *
 * Пример создания:
 *
 *   val script = QuestScript(listOf(
 *       ScriptAction("toast",  text = "Вы завершили маршрут!"),
 *       ScriptAction("dialog", title = "Награда", text = "Вы получили 100 очков!")
 *   ))
 */
data class QuestScript(
    val actions: List<ScriptAction>
) : Serializable
