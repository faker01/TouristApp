package com.example.touristapp.utils.csv

import android.content.Context
import com.example.touristapp.models.*

/**
 * Парсер CSV файла для загрузки данных квеста
 */
class CsvParser(private val context: Context, private val csvFileName: String? = null) {

    companion object {
        private const val COLUMN_SEPARATOR = ";;"
        private const val PART_SEPARATOR = "::"
        private const val DEFAULT_CSV_FILE_NAME = "Homlini_1_dedKarl.csv"
    }

    private val actualFileName: String
        get() = csvFileName ?: DEFAULT_CSV_FILE_NAME

    fun loadQuestData(): Triple<Map<String, DialogueModel>, Map<String, TaskModel>, Map<String, ResultModel>> {
        val dialogues = mutableMapOf<String, DialogueModel>()
        val tasks = mutableMapOf<String, TaskModel>()
        val results = mutableMapOf<String, ResultModel>()

        val csvContent = readCsvFile()
        if (csvContent == null) {
            println("CsvParser: CSV файл '$actualFileName' не найден в assets")
            return Triple(dialogues, tasks, results)
        }

        val lines = csvContent.split("\n")
        val dataLines = lines.drop(1)

        var dialogueCounter = 0
        var taskCounter = 0
        var resultCounter = 0

        dataLines.forEachIndexed { index, line ->
            val trimmedLine = line.trim()
            if (trimmedLine.isEmpty()) return@forEachIndexed

            val columns = trimmedLine.split(COLUMN_SEPARATOR)
            if (columns.size < 4) {
                println("CsvParser: Строка ${index + 2} имеет ${columns.size} колонок, ожидается 4")
                return@forEachIndexed
            }

            val type = columns[0].trim().uppercase()
            val textParts = if (columns[1].isNotBlank()) columns[1].trim().split(PART_SEPARATOR) else emptyList()
            val resultParts = if (columns[2].isNotBlank()) columns[2].trim().split(PART_SEPARATOR) else emptyList()
            val triggerParts = if (columns[3].isNotBlank()) columns[3].trim().split(PART_SEPARATOR) else emptyList()

            val trigger = parseTrigger(triggerParts)

            when (type) {
                "DIALOGUE" -> {
                    val id = "dialogue_${dialogueCounter++}"
                    val dialogue = parseDialogue(id, textParts, resultParts, trigger)
                    dialogues[id] = dialogue
                }
                "TASK" -> {
                    val id = "task_${taskCounter++}"
                    val task = parseTask(id, textParts, resultParts, trigger)
                    tasks[id] = task
                }
                "RESULT" -> {
                    val id = "result_${resultCounter++}"
                    val result = parseResult(id, textParts, resultParts, trigger)
                    results[id] = result
                }
                else -> {
                    println("CsvParser: Неизвестный тип '$type' в строке ${index + 2}")
                }
            }
        }

        println("CsvParser: Загружено - диалогов: ${dialogues.size}, заданий: ${tasks.size}, результатов: ${results.size}")
        return Triple(dialogues, tasks, results)
    }

    private fun readCsvFile(): String? {
        return try {
            val inputStream = context.assets.open(actualFileName)
            val content = inputStream.bufferedReader().use { it.readText() }
            println("CsvParser: Файл '$actualFileName' прочитан, размер: ${content.length} символов")
            content
        } catch (e: Exception) {
            println("CsvParser: Ошибка чтения файла '$actualFileName' - ${e.message}")
            null
        }
    }

    private fun parseTrigger(parts: List<String>): Trigger? {
        if (parts.isEmpty() || (parts.size == 1 && parts[0].isBlank())) return null
        val latitude = parts.getOrNull(0)?.toDoubleOrNull()
        val longitude = parts.getOrNull(1)?.toDoubleOrNull()
        val radius = parts.getOrNull(2)?.toFloatOrNull() ?: 50f
        val condition = parts.getOrNull(3)
        return Trigger(latitude, longitude, radius, condition)
    }

    private fun parseDialogue(
        id: String,
        textParts: List<String>,
        resultParts: List<String>,
        trigger: Trigger?
    ): DialogueModel {
        return DialogueModel(
            id = id,
            characterName = textParts.getOrNull(0) ?: "Хранитель",
            text = textParts.getOrNull(1) ?: "...",
            imageUrl = textParts.getOrNull(2),
            sparksReward = resultParts.getOrNull(0)?.toIntOrNull() ?: 0,
            nextElementId = resultParts.getOrNull(1),
            trigger = trigger
        )
    }

    private fun parseTask(
        id: String,
        textParts: List<String>,
        resultParts: List<String>,
        trigger: Trigger?
    ): TaskModel {
        val taskTypeStr = resultParts.getOrNull(0)?.uppercase() ?: "TEXT_INPUT"
        val taskType = when (taskTypeStr) {
            "PHOTO" -> TaskType.PHOTO
            "NUMBER_INPUT" -> TaskType.NUMBER_INPUT
            "MULTIPLE_CHOICE" -> TaskType.MULTIPLE_CHOICE
            else -> TaskType.TEXT_INPUT
        }

        return TaskModel(
            id = id,
            text = textParts.getOrNull(0) ?: "Задание",
            correctAnswer = textParts.getOrNull(1)?.lowercase() ?: "",
            options = if (textParts.size > 2) textParts.subList(2, textParts.size) else emptyList(),
            taskType = taskType,
            sparkReward = resultParts.getOrNull(1)?.toIntOrNull() ?: 1,
            isBonus = resultParts.getOrNull(4)?.lowercase() == "bonus" || resultParts.getOrNull(4) == "true",
            hint = resultParts.getOrNull(3) ?: "",
            hintCost = resultParts.getOrNull(2)?.toIntOrNull() ?: 1,
            nextElementId = resultParts.getOrNull(5),
            trigger = trigger
        )
    }

    private fun parseResult(
        id: String,
        textParts: List<String>,
        resultParts: List<String>,
        trigger: Trigger?
    ): ResultModel {
        return ResultModel(
            id = id,
            message = textParts.getOrNull(0) ?: "Результат",
            sparksReward = resultParts.getOrNull(0)?.toIntOrNull() ?: 0,
            nextLocationId = resultParts.getOrNull(1)?.toIntOrNull(),
            isFinal = resultParts.getOrNull(2)?.lowercase() == "final",
            achievements = if (resultParts.size > 3) resultParts.subList(3, resultParts.size) else emptyList(),
            trigger = trigger
        )
    }
}