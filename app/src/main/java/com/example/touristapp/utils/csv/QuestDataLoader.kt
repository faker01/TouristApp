package com.example.touristapp.utils.csv

import android.content.Context
import com.example.touristapp.models.DialogueModel
import com.example.touristapp.models.LoadedQuestData

/**
 * Загрузчик данных квеста из CSV
 *
 * Отвечает за:
 * 1. Загрузку данных через CsvParser
 * 2. Сборку связей между элементами
 * 3. Предоставление удобного доступа к данным
 */
class QuestDataLoader(private val context: Context, private val csvFileName: String? = null) {

    private val csvParser = CsvParser(context, csvFileName)

    /**
     * Загружает и собирает все данные квеста
     *
     * @return LoadedQuestData содержащий все диалоги, задания и результаты
     */
    fun loadQuest(): LoadedQuestData {
        val (dialogues, tasks, results) = csvParser.loadQuestData()

        val startDialogueId = findStartDialogueId(dialogues)

        println("QuestDataLoader: Загружено ${dialogues.size} диалогов, ${tasks.size} заданий, ${results.size} результатов")
        println("QuestDataLoader: Стартовый диалог: $startDialogueId")

        return LoadedQuestData(
            dialogues = dialogues,
            tasks = tasks,
            results = results,
            startDialogueId = startDialogueId
        )
    }

    /**
     * Загружает только пролог (диалоги без триггера)
     */
    fun loadPrologue(): List<DialogueModel> {
        val (dialogues, _, _) = csvParser.loadQuestData()
        return dialogues.values
            .filter { it.trigger == null }
            .sortedBy { it.id }
    }

    /**
     * Загружает диалоги с гео-триггером для конкретной локации
     */
    fun loadDialoguesByLocation(latitude: Double, longitude: Double, radius: Float = 50f): List<DialogueModel> {
        val (dialogues, _, _) = csvParser.loadQuestData()
        return dialogues.values.filter { dialogue ->
            dialogue.trigger?.isLocationTrigger() == true &&
                    dialogue.trigger?.latitude == latitude &&
                    dialogue.trigger?.longitude == longitude
        }
    }

    /**
     * Находит начальный диалог
     * Приоритет: диалог без триггера -> первый диалог
     */
    private fun findStartDialogueId(dialogues: Map<String, DialogueModel>): String? {
        // Ищем диалог без триггера (пролог)
        val prologueDialogue = dialogues.values.find { it.trigger == null }
        if (prologueDialogue != null) {
            println("QuestDataLoader: Найден пролог-диалог: ${prologueDialogue.id}")
            return prologueDialogue.id
        }

        // Или первый по порядку
        val firstDialogue = dialogues.values.minByOrNull { it.id }
        println("QuestDataLoader: Стартовый диалог по умолчанию: ${firstDialogue?.id}")
        return firstDialogue?.id
    }
}