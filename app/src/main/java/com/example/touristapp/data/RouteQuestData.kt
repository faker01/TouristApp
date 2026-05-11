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
    // 2. ГОТОВЫЕ МАРШРУТЫ (всегда доступны)
    // ============================================================
    private val regularRoutesList = listOf(
        RegularRouteConfig(
            id = 100,
            name = "Исторический маршрут",
            duration = "2-3 ч",
            distance = "4.5 км",
            count = "8 мест",
            attractions = listOf(
                Attraction("Кафедральный собор", 54.7065, 20.5090),
                Attraction("Рыбная деревня", 54.7030, 20.5095),
                Attraction("Королевские ворота", 54.7210, 20.5155)
            )
        ),
        RegularRouteConfig(
            id = 101,
            name = "Форты Кёнигсберга",
            duration = "4-5 ч",
            distance = "8.2 км",
            count = "6 мест",
            attractions = listOf(
                Attraction("Форт №5", 54.7240, 20.4550),
                Attraction("Форт №3", 54.7000, 20.4800)
            )
        ),
        RegularRouteConfig(
            id = 102,
            name = "Музейный маршрут",
            duration = "3-4 ч",
            distance = "3.8 км",
            count = "5 мест",
            attractions = listOf(
                Attraction("Музей Мирового океана", 54.7044, 20.4994),
                Attraction("Музей изобразительных искусств", 54.7108, 20.5118),
                Attraction("Музей янтаря", 54.7200, 20.5000)
            )
        )
    )

    // ============================================================
    // ПУБЛИЧНЫЕ МЕТОДЫ ДОСТУПА
    // ============================================================

    fun getQuestChains(): List<QuestChainConfig> = questChainsList

    fun getRegularRoutes(): List<RegularRouteConfig> = regularRoutesList

    fun getQuestChainById(id: Int): QuestChainConfig? {
        return questChainsList.find { it.id == id }
    }

    fun getRegularRouteById(id: Int): RegularRouteConfig? {
        return regularRoutesList.find { it.id == id }
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