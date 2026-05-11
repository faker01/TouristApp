package com.example.touristapp.utils.csv

import android.content.Context
import com.example.touristapp.models.*

/**
 * Загрузчик данных квеста из CSV
 *
 * Отвечает за:
 * 1. Загрузку данных через CsvParser
 * 2. Сборку связей между элементами
 * 3. Предоставление удобного доступа к данным
 *
 * @param context Контекст приложения
 * @param csvFileName Имя CSV файла (по умолчанию "quest_data.csv")
 */
class QuestDataLoader(
    private val context: Context,
    private val csvFileName: String = "quest_data.csv"
) {

    private val csvParser = CsvParser(context, csvFileName)

    /**
     * Загружает и собирает все данные квеста
     *
     * @return LoadedQuestData содержащий intro, диалоги, задания и результаты
     */
    fun loadQuest(): LoadedQuestData {
        val questData = csvParser.loadQuestData()

        println("QuestDataLoader: 📁 Файл '$csvFileName' загружен")
        println("QuestDataLoader: 📖 INTRO: ${if (questData.intro != null) "есть" else "нет"}")
        println("QuestDataLoader: 💬 Диалогов: ${questData.dialogues.size}")
        println("QuestDataLoader: ❓ Заданий: ${questData.tasks.size}")
        println("QuestDataLoader: 🏆 Результатов: ${questData.results.size}")
        println("QuestDataLoader: 🚀 Стартовый элемент: ${questData.startElementId}")

        return questData
    }

    /**
     * Получить intro (вступление) если есть
     */
    fun getIntro(): IntroModel? {
        return loadQuest().intro
    }

    /**
     * Получить все диалоги (без учёта триггеров)
     */
    fun getAllDialogues(): List<DialogueModel> {
        return loadQuest().dialogues.values.toList()
    }

    /**
     * Получить все задания
     */
    fun getAllTasks(): List<TaskModel> {
        return loadQuest().tasks.values.toList()
    }

    /**
     * Получить все результаты
     */
    fun getAllResults(): List<ResultModel> {
        return loadQuest().results.values.toList()
    }

    /**
     * Получить элемент по ID
     */
    fun getElementById(id: String): BaseQuestModel? {
        return loadQuest().getElementById(id)
    }

    /**
     * Получить следующий элемент за текущим
     */
    fun getNextElement(currentId: String): BaseQuestModel? {
        return loadQuest().getNextElement(currentId)
    }

    /**
     * Получить диалоги с гео-триггером для конкретной локации
     * @param latitude Широта
     * @param longitude Долгота
     * @param radius Радиус срабатывания (метры)
     */
    fun getDialoguesByLocation(latitude: Double, longitude: Double, radius: Float = 50f): List<DialogueModel> {
        val questData = loadQuest()
        return questData.dialogues.values.filter { dialogue ->
            dialogue.trigger?.isLocationTrigger() == true &&
                    dialogue.trigger?.latitude == latitude &&
                    dialogue.trigger?.longitude == longitude &&
                    (dialogue.trigger?.radius ?: 50f) <= radius
        }
    }

    /**
     * Получить задания с гео-триггером для конкретной локации
     */
    fun getTasksByLocation(latitude: Double, longitude: Double, radius: Float = 50f): List<TaskModel> {
        val questData = loadQuest()
        return questData.tasks.values.filter { task ->
            task.trigger?.isLocationTrigger() == true &&
                    task.trigger?.latitude == latitude &&
                    task.trigger?.longitude == longitude &&
                    (task.trigger?.radius ?: 50f) <= radius
        }
    }

    /**
     * Проверить, является ли элемент первым в цепочке
     */
    fun isFirstElement(elementId: String): Boolean {
        val questData = loadQuest()
        return questData.startElementId == elementId
    }

    /**
     * Проверить, является ли элемент последним (ведёт к финальному результату)
     */
    fun isLastElement(elementId: String): Boolean {
        val next = getNextElement(elementId)
        return next == null || (next is ResultModel && next.isFinal)
    }

    /**
     * Получить следующий элемент после текущего с учётом триггеров
     * Если триггеры совпадают, возвращает следующий элемент без открытия карты
     */
    fun getNextElementWithTriggerCheck(currentId: String): Pair<BaseQuestModel?, Boolean> {
        val currentElement = getElementById(currentId)
        val nextElement = getNextElement(currentId)

        // Если нет следующего элемента
        if (nextElement == null) {
            return Pair(null, false)
        }

        // Сравниваем триггеры
        val currentTrigger = when (currentElement) {
            is TaskModel -> currentElement.trigger
            is DialogueModel -> currentElement.trigger
            else -> null
        }

        val nextTrigger = when (nextElement) {
            is TaskModel -> nextElement.trigger
            is DialogueModel -> nextElement.trigger
            else -> null
        }

        // Если координаты триггеров совпадают, карту не показываем
        val shouldShowMap = !areTriggersEqual(currentTrigger, nextTrigger)

        return Pair(nextElement, shouldShowMap)
    }

    /**
     * Сравнивает два триггера по координатам
     * @return true если координаты совпадают (в пределах радиуса)
     */
    private fun areTriggersEqual(t1: Trigger?, t2: Trigger?): Boolean {
        if (t1 == null && t2 == null) return true
        if (t1 == null || t2 == null) return false

        // Если нет гео-координат, считаем что совпадают
        if (!t1.isLocationTrigger() && !t2.isLocationTrigger()) return true
        if (!t1.isLocationTrigger() || !t2.isLocationTrigger()) return false

        // Проверяем расстояние между точками
        val distance = calculateDistance(
            t1.latitude ?: 0.0, t1.longitude ?: 0.0,
            t2.latitude ?: 0.0, t2.longitude ?: 0.0
        )

        // Если расстояние меньше суммы радиусов, считаем что в одной зоне
        val radiusSum = (t1.radius ?: 50f) + (t2.radius ?: 50f)
        return distance <= radiusSum
    }

    /**
     * Расчёт расстояния между двумя точками в метрах (формула гаверсинуса)
     */
    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
        val R = 6371000f // радиус Земли в метрах
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return (R * c).toFloat()
    }
}