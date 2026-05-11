package com.example.touristapp.utils.csv

import android.content.Context
import com.example.touristapp.models.*

/**
 * Парсер CSV файла для загрузки данных квеста
 *
 * Поддерживаемые типы строк:
 * - INTRO   : вступительный текст для квестовой цепочки
 * - DIALOGUE: диалог с персонажем
 * - TASK    : задание/квест
 * - RESULT  : результат выполнения
 *
 * Формат CSV (4 колонки, разделитель ;;):
 * 1. тип (INTRO, DIALOGUE, TASK, RESULT)
 * 2. текст (данные, зависит от типа)
 * 3. результат (награда и переход)
 * 4. триггер (широта::долгота::радиус::условие)
 *
 * Разделитель частей внутри колонки: ::
 *
 * CSV файл должен лежать в: app/src/main/assets/ (любое имя)
 */
class CsvParser(
    private val context: Context,
    private val csvFileName: String = "quest_data.csv"  // имя файла передаётся при создании
) {

    companion object {
        private const val COLUMN_SEPARATOR = ";;"
        private const val PART_SEPARATOR = "::"
    }

    /**
     * Загружает данные из CSV файла
     * @return LoadedQuestData содержащий intro, диалоги, задания и результаты
     */
    fun loadQuestData(): LoadedQuestData {
        var intro: IntroModel? = null
        val dialogues = mutableMapOf<String, DialogueModel>()
        val tasks = mutableMapOf<String, TaskModel>()
        val results = mutableMapOf<String, ResultModel>()

        val csvContent = readCsvFile()
        if (csvContent == null) {
            println("CsvParser: ❌ CSV файл '$csvFileName' не найден в assets")
            return LoadedQuestData(intro, dialogues, tasks, results, null)
        }

        val lines = csvContent.split("\n")
        val dataLines = lines.drop(1) // пропускаем заголовок

        var introCounter = 0
        var dialogueCounter = 0
        var taskCounter = 0
        var resultCounter = 0

        dataLines.forEachIndexed { index, line ->
            val trimmedLine = line.trim()
            if (trimmedLine.isEmpty()) return@forEachIndexed

            val columns = trimmedLine.split(COLUMN_SEPARATOR)
            if (columns.size < 4) {
                println("CsvParser: ⚠️ Строка ${index + 2} имеет ${columns.size} колонок, ожидается 4")
                return@forEachIndexed
            }

            val type = columns[0].trim().uppercase()
            val dataParts = if (columns[1].isNotBlank()) columns[1].trim().split(PART_SEPARATOR) else emptyList()
            val rewardParts = if (columns[2].isNotBlank()) columns[2].trim().split(PART_SEPARATOR) else emptyList()
            val triggerParts = if (columns[3].isNotBlank()) columns[3].trim().split(PART_SEPARATOR) else emptyList()

            val trigger = parseTrigger(triggerParts)

            when (type) {
                "INTRO" -> {
                    val id = "intro_${introCounter++}"
                    intro = parseIntro(id, dataParts, rewardParts, trigger)
                    println("CsvParser: 📖 Загружен INTRO: ${intro?.title}")
                }
                "DIALOGUE" -> {
                    val id = "dialogue_${dialogueCounter++}"
                    val dialogue = parseDialogue(id, dataParts, rewardParts, trigger)
                    dialogues[id] = dialogue
                    println("CsvParser: 💬 Загружен DIALOGUE: ${dialogue.characterName} -> ${dialogue.text.take(30)}...")
                }
                "TASK" -> {
                    val id = "task_${taskCounter++}"
                    val task = parseTask(id, dataParts, rewardParts, trigger)
                    tasks[id] = task
                    println("CsvParser: ❓ Загружен TASK: ${task.text.take(30)}...")
                }
                "RESULT" -> {
                    val id = "result_${resultCounter++}"
                    val result = parseResult(id, dataParts, rewardParts, trigger)
                    results[id] = result
                    println("CsvParser: 🏆 Загружен RESULT: ${result.message.take(30)}...")
                }
                else -> {
                    println("CsvParser: ⚠️ Неизвестный тип '$type' в строке ${index + 2}")
                }
            }
        }

        // Определяем стартовый элемент (приоритет: INTRO > первый DIALOGUE)
        val startElementId = intro?.id ?: dialogues.keys.firstOrNull()

        println("CsvParser: ✅ Файл '$csvFileName' загружен - INTRO: ${if (intro != null) 1 else 0}, диалогов: ${dialogues.size}, заданий: ${tasks.size}, результатов: ${results.size}")
        println("CsvParser: 🚀 Стартовый элемент: $startElementId")

        return LoadedQuestData(intro, dialogues, tasks, results, startElementId)
    }

    /**
     * Чтение CSV файла из assets по указанному имени
     */
    private fun readCsvFile(): String? {
        return try {
            // Пробуем открыть файл по разным путям
            val paths = listOf(
                csvFileName,
                "csv/$csvFileName",
                "data/$csvFileName",
                "quests/$csvFileName"
            )

            for (path in paths) {
                try {
                    val inputStream = context.assets.open(path)
                    val content = inputStream.bufferedReader().use { it.readText() }
                    println("CsvParser: ✅ CSV найден по пути: $path, размер: ${content.length} символов")
                    return content
                } catch (e: Exception) {
                    // Продолжаем поиск
                }
            }

            // Выводим список доступных файлов для отладки
            try {
                val allFiles = context.assets.list("")
                println("CsvParser: 📁 Доступные файлы в assets: ${allFiles?.joinToString() ?: "нет"}")
            } catch (e: Exception) {
                println("CsvParser: ❌ Не удалось получить список assets: ${e.message}")
            }

            println("CsvParser: ❌ CSV файл '$csvFileName' не найден ни по одному из путей")
            null
        } catch (e: Exception) {
            println("CsvParser: ❌ Общая ошибка чтения CSV: ${e.message}")
            null
        }
    }

    /**
     * Парсинг INTRO (вступления)
     * Формат dataParts: заголовок::текст::изображение
     * Формат rewardParts: награда_искрами::следующий_элемент
     */
    private fun parseIntro(
        id: String,
        dataParts: List<String>,
        rewardParts: List<String>,
        trigger: Trigger?
    ): IntroModel {
        val title = dataParts.getOrNull(0) ?: "Вступление"
        val text = dataParts.getOrNull(1) ?: "..."
        val imageUrl = dataParts.getOrNull(2)
        val nextElementId = rewardParts.getOrNull(1)

        return IntroModel(
            id = id,
            title = title,
            text = text,
            imageUrl = imageUrl,
            nextElementId = nextElementId,
            trigger = trigger
        )
    }

    /**
     * Парсинг диалога
     * Формат dataParts: персонаж::текст::изображение
     * Формат rewardParts: награда_искрами::следующий_элемент
     */
    private fun parseDialogue(
        id: String,
        dataParts: List<String>,
        rewardParts: List<String>,
        trigger: Trigger?
    ): DialogueModel {
        val characterName = dataParts.getOrNull(0) ?: "Хранитель"
        val text = dataParts.getOrNull(1) ?: "..."
        val imageUrl = dataParts.getOrNull(2)
        val sparksReward = rewardParts.getOrNull(0)?.toIntOrNull() ?: 0
        val nextElementId = rewardParts.getOrNull(1)

        return DialogueModel(
            id = id,
            characterName = characterName,
            text = text,
            imageUrl = imageUrl,
            sparksReward = sparksReward,
            nextElementId = nextElementId,
            trigger = trigger
        )
    }

    /**
     * Парсинг задания
     * Формат dataParts: текст_задания::правильный_ответ::вариант1::вариант2::...
     * Формат rewardParts: тип_задания::награда::стоимость_подсказки::подсказка::бонусное::следующий_элемент
     */
    private fun parseTask(
        id: String,
        dataParts: List<String>,
        rewardParts: List<String>,
        trigger: Trigger?
    ): TaskModel {
        val text = dataParts.getOrNull(0) ?: "Задание"
        val correctAnswer = dataParts.getOrNull(1)?.lowercase() ?: ""
        val options = if (dataParts.size > 2) {
            dataParts.subList(2, dataParts.size)
        } else emptyList()

        val taskTypeStr = rewardParts.getOrNull(0)?.uppercase() ?: "TEXT_INPUT"
        val taskType = when (taskTypeStr) {
            "PHOTO" -> TaskType.PHOTO
            "NUMBER_INPUT" -> TaskType.NUMBER_INPUT
            "MULTIPLE_CHOICE" -> TaskType.MULTIPLE_CHOICE
            else -> TaskType.TEXT_INPUT
        }

        val sparkReward = rewardParts.getOrNull(1)?.toIntOrNull() ?: 1
        val hintCost = rewardParts.getOrNull(2)?.toIntOrNull() ?: 1
        val hint = rewardParts.getOrNull(3) ?: ""
        val isBonus = rewardParts.getOrNull(4)?.lowercase() == "bonus" || rewardParts.getOrNull(4) == "true"
        val nextElementId = rewardParts.getOrNull(5)

        return TaskModel(
            id = id,
            text = text,
            correctAnswer = correctAnswer,
            options = options,
            taskType = taskType,
            sparkReward = sparkReward,
            isBonus = isBonus,
            hint = hint,
            hintCost = hintCost,
            nextElementId = nextElementId,
            trigger = trigger
        )
    }

    /**
     * Парсинг результата
     * Формат dataParts: сообщение
     * Формат rewardParts: награда_искрами::следующая_локация::финальный::достижение1::достижение2
     */
    private fun parseResult(
        id: String,
        dataParts: List<String>,
        rewardParts: List<String>,
        trigger: Trigger?
    ): ResultModel {
        val message = dataParts.getOrNull(0) ?: "Результат"
        val sparksReward = rewardParts.getOrNull(0)?.toIntOrNull() ?: 0
        val nextLocationId = rewardParts.getOrNull(1)?.toIntOrNull()
        val isFinal = rewardParts.getOrNull(2)?.lowercase() == "final"
        val achievements = if (rewardParts.size > 3) {
            rewardParts.subList(3, rewardParts.size)
        } else emptyList()

        return ResultModel(
            id = id,
            message = message,
            sparksReward = sparksReward,
            nextLocationId = nextLocationId,
            isFinal = isFinal,
            achievements = achievements,
            trigger = trigger
        )
    }

    /**
     * Парсинг триггера (геопозиция)
     * Формат: широта::долгота::радиус::условие
     */
    private fun parseTrigger(parts: List<String>): Trigger? {
        if (parts.isEmpty() || (parts.size == 1 && parts[0].isBlank())) return null

        val latitude = parts.getOrNull(0)?.toDoubleOrNull()
        val longitude = parts.getOrNull(1)?.toDoubleOrNull()
        val radius = parts.getOrNull(2)?.toFloatOrNull() ?: 50f
        val condition = parts.getOrNull(3)

        return if (latitude != null && longitude != null) {
            Trigger(latitude, longitude, radius, condition)
        } else if (condition != null) {
            Trigger(condition = condition)
        } else null
    }
}