package com.example.touristapp.data

import com.example.touristapp.models.Attraction

/**
 * Конфигурация квестовых цепочек и готовых маршрутов
 *
 * Здесь хранятся:
 * 1. Квестовые цепочки — открываются последовательно
 * 2. Готовые маршруты — всегда доступны
 */
object RouteQuestData {

    // ============================================================
    // 1. КВЕСТОВЫЕ ЦЕПОЧКИ (открываются по порядку)
    // ============================================================
    private val questChainsList = listOf(
        QuestChainConfig(
            id = 0,
            name = "Тайна Янтарной комнаты",
            description = "Раскрой тайну исчезновения Янтарной комнаты",
            duration = "~30 мин",
            distance = "1.2 км",
            count = "5 заданий",
            csvFileName = "Homlini_1_dedKarl.csv",
            isLocked = false
        ),
        QuestChainConfig(
            id = 1,
            name = "Проклятие Королевского замка",
            description = "Пройдите первую цепочку, чтобы открыть",
            duration = "~40 мин",
            distance = "1.5 км",
            count = "3 задания",
            csvFileName = "Homlini_2_babMarta.csv",
            isLocked = true
        )
    )
    // ============================================================
    // ПУБЛИЧНЫЕ МЕТОДЫ ДОСТУПА
    // ============================================================

    fun getQuestChains(): List<QuestChainConfig> = questChainsList
    fun getQuestChainById(id: Int): QuestChainConfig? {
        return questChainsList.find { it.id == id }
    }

    fun getCsvFileNameForChain(chainId: Int): String? {
        return questChainsList.find { it.id == chainId }?.csvFileName
    }

    /**
     * Получить разблокированные квестовые цепочки
     * @param completedChainIds ID цепочек, которые уже пройдены
     */
    fun getUnlockedQuestChains(completedChainIds: Set<Int>): List<QuestChainConfig> {
        return questChainsList.map { chain ->
            val isUnlocked = chain.id == 0 || (chain.id - 1) in completedChainIds
            chain.copy(isLocked = !isUnlocked)
        }
    }
}

// ============================================================
// DATA CLASSES
// ============================================================

/**
 * Конфигурация квестовой цепочки
 */
data class QuestChainConfig(
    val id: Int,
    val name: String,
    val description: String,
    val duration: String,
    val distance: String,
    val count: String,
    val csvFileName: String,
    val isLocked: Boolean = true
)

/**
 * Конфигурация готового маршрута
 */
data class RegularRouteConfig(
    val id: Int,
    val name: String,
    val duration: String,
    val distance: String,
    val count: String,
    val attractions: List<Attraction>
)