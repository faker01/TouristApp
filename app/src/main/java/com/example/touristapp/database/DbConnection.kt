package com.example.touristapp.database

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import com.example.touristapp.models.Attraction
import com.example.touristapp.models.Route

class DbConnection(private val context: Context) : SQLiteOpenHelper(
    context,
    DATABASE_NAME,
    null,
    DATABASE_VERSION
) {

    companion object {
        private const val DATABASE_NAME = "tourist_app.db"
        private const val DATABASE_VERSION = 1

        // Таблица мест
        const val TABLE_ATTRACTIONS = "attractions"
        const val COL_ATT_ID = "id"
        const val COL_ATT_TITLE = "title"
        const val COL_ATT_LAT = "lat"
        const val COL_ATT_LON = "lon"
        const val COL_ATT_DESCRIPTION = "description"

        // Таблица маршрутов
        const val TABLE_ROUTES = "routes"
        const val COL_RT_ID = "id"
        const val COL_RT_NAME = "name"
        const val COL_RT_DURATION = "duration"
        const val COL_RT_DISTANCE = "distance"
        const val COL_RT_COUNT = "count"

        // Связующая таблица маршрут ↔ место
        const val TABLE_ROUTE_ATTRACTIONS = "route_attractions"
        const val COL_RA_ROUTE_ID = "route_id"
        const val COL_RA_ATT_ID = "attraction_id"
        const val COL_RA_ORDER = "sort_order"

        // Таблица сохранений квеста
        const val TABLE_QUEST_PROGRESS = "quest_progress"
        const val COL_QUEST_QUEST_LINE_ID = "quest_line"
        const val COL_QUEST_QUEST_ID = "quest_id"
        const val COL_QUEST_CURRENT_STEP = "current_step"

        // Таблица общих данных квеста
        const val TABLE_QUEST_DATA = "quest_data"
        const val COL_QUEST_DATA_QUEST_LINE_ID = "quest_line_id"
        const val COL_QUEST_DATA_NUM_OF_HINTS = "num_of_hints"
    }

    // --------------------------------------------------------
    // Копирование БД из assets при первом запуске
    // --------------------------------------------------------

    init {
        copyDatabaseIfNeeded()
    }

    private fun copyDatabaseIfNeeded() {
        val dbFile = context.getDatabasePath(DATABASE_NAME)

        val prefs = context.getSharedPreferences("db_prefs", Context.MODE_PRIVATE)
        val savedVersion = prefs.getInt("db_version", 0)

        // Удаляем старую БД если версия изменилась
        if (dbFile.exists() && savedVersion < DATABASE_VERSION) {
            dbFile.delete()
            Log.d("DbConnection", "Старая БД удалена, будет скопирована новая")
        }

        if (!dbFile.exists()) {
            dbFile.parentFile?.mkdirs()
            try {
                context.assets.open(DATABASE_NAME).use { input ->
                    dbFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                prefs.edit().putInt("db_version", DATABASE_VERSION).apply()
                Log.d("DbConnection", "БД успешно скопирована из assets")
            } catch (e: Exception) {
                Log.e("DbConnection", "Ошибка копирования БД: ${e.message}")
            }
        }
    }

    override fun onCreate(db: SQLiteDatabase) {}

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {}

    override fun onOpen(db: SQLiteDatabase) {
        super.onOpen(db)
        if (!db.isReadOnly) db.execSQL("PRAGMA foreign_keys = ON;")
    }

    // --------------------------------------------------------
    // Достопримечательности
    // --------------------------------------------------------

    fun getAllAttractions(): List<Attraction> {
        val list = mutableListOf<Attraction>()
        try {
            val cursor: Cursor = readableDatabase.query(
                TABLE_ATTRACTIONS, null, null, null, null, null, "$COL_ATT_TITLE ASC"
            )
            cursor.use {
                while (it.moveToNext()) {
                    list += Attraction(
                        id = it.getInt(it.getColumnIndexOrThrow(COL_ATT_ID)),
                        title = it.getString(it.getColumnIndexOrThrow(COL_ATT_TITLE)),
                        lat = it.getDouble(it.getColumnIndexOrThrow(COL_ATT_LAT)),
                        lon = it.getDouble(it.getColumnIndexOrThrow(COL_ATT_LON)),
                        description = it.getString(it.getColumnIndexOrThrow(COL_ATT_DESCRIPTION))
                    )
                }
            }
        } catch (e: Exception) {
            Log.e("DbConnection", "Ошибка getAllAttractions: ${e.message}")
        }
        return list
    }

    fun getAttractionById(id: Long): Attraction? {
        return try {
            val cursor = readableDatabase.query(
                TABLE_ATTRACTIONS, null,
                "$COL_ATT_ID = ?", arrayOf(id.toString()),
                null, null, null
            )
            cursor.use {
                if (it.moveToFirst()) Attraction(
                    id = it.getInt(it.getColumnIndexOrThrow(COL_ATT_ID)),
                    title = it.getString(it.getColumnIndexOrThrow(COL_ATT_TITLE)),
                    lat = it.getDouble(it.getColumnIndexOrThrow(COL_ATT_LAT)),
                    lon = it.getDouble(it.getColumnIndexOrThrow(COL_ATT_LON)),
                    description = it.getString(it.getColumnIndexOrThrow(COL_ATT_DESCRIPTION))
                ) else null
            }
        } catch (e: Exception) {
            Log.e("DbConnection", "Ошибка getAttractionById: ${e.message}")
            null
        }
    }

    // --------------------------------------------------------
    // Маршруты
    // --------------------------------------------------------

    fun getAllRoutes(): List<Route> {
        val routes = mutableListOf<Route>()
        try {
            val cursor = readableDatabase.query(
                TABLE_ROUTES, null, null, null, null, null, "$COL_RT_ID ASC"
            )
            cursor.use {
                while (it.moveToNext()) {
                    val routeId = it.getLong(it.getColumnIndexOrThrow(COL_RT_ID))
                    routes += Route(
                        id = routeId,
                        name = it.getString(it.getColumnIndexOrThrow(COL_RT_NAME)),
                        duration = it.getString(it.getColumnIndexOrThrow(COL_RT_DURATION)),
                        distance = it.getString(it.getColumnIndexOrThrow(COL_RT_DISTANCE)),
                        count = it.getString(it.getColumnIndexOrThrow(COL_RT_COUNT)),
                        attractions = getAttractionsForRoute(routeId)
                    )
                }
            }
        } catch (e: Exception) {
            Log.e("DbConnection", "Ошибка getAllRoutes: ${e.message}")
        }
        return routes
    }

    fun getAttractionsForRoute(routeId: Long): List<Attraction> {
        val list = mutableListOf<Attraction>()
        try {
            val sql = """
                SELECT a.* FROM $TABLE_ATTRACTIONS a
                INNER JOIN $TABLE_ROUTE_ATTRACTIONS ra
                    ON a.$COL_ATT_ID = ra.$COL_RA_ATT_ID
                WHERE ra.$COL_RA_ROUTE_ID = ?
                ORDER BY ra.$COL_RA_ORDER ASC
            """.trimIndent()
            val cursor = readableDatabase.rawQuery(sql, arrayOf(routeId.toString()))
            cursor.use {
                while (it.moveToNext()) {
                    list += Attraction(
                        id = it.getInt(it.getColumnIndexOrThrow(COL_ATT_ID)),
                        title = it.getString(it.getColumnIndexOrThrow(COL_ATT_TITLE)),
                        lat = it.getDouble(it.getColumnIndexOrThrow(COL_ATT_LAT)),
                        lon = it.getDouble(it.getColumnIndexOrThrow(COL_ATT_LON)),
                        description = it.getString(it.getColumnIndexOrThrow(COL_ATT_DESCRIPTION))
                    )
                }
            }
        } catch (e: Exception) {
            Log.e("DbConnection", "Ошибка getAttractionsForRoute: ${e.message}")
        }
        return list
    }

    fun getQuestProgressById(questLineId: String, questId: String): Int{
        try {
            val cursor = readableDatabase.query(
                TABLE_QUEST_PROGRESS, null,
                "$COL_QUEST_QUEST_ID = ? AND $COL_QUEST_QUEST_LINE_ID = ?", arrayOf(questId, questLineId),
                null, null, null
            )
            cursor.use {
                if (it.moveToFirst())
                {
                    return it.getInt(it.getColumnIndexOrThrow(COL_QUEST_CURRENT_STEP))
                }
                else null
            }
        } catch (e: Exception) {
            Log.e("DbConnection", "Ошибка getQuestProgressById: ${e.message}")
        }
        return 0
    }

    fun updateQuestProgress(questLineId: String, questId: String, step: Int): Int{
        val cv = ContentValues()
        cv.put(COL_QUEST_CURRENT_STEP, step)

        val result = writableDatabase.update(
            TABLE_QUEST_PROGRESS, cv, "$COL_QUEST_QUEST_LINE_ID = ? AND $COL_QUEST_QUEST_ID = ?", arrayOf(questLineId, questId),
        )
        return result
    }

    fun addQuestProgress(questLineId: String, questId: String): Long{
        val cv = ContentValues()
        cv.put(COL_QUEST_QUEST_LINE_ID, questLineId)
        cv.put(COL_QUEST_QUEST_ID, questId)
        cv.put(COL_QUEST_CURRENT_STEP, 0)

        val result = writableDatabase.insert(TABLE_QUEST_PROGRESS, null,cv)

        return result
    }

    fun getQuestData(questLineId: String): Int{
        try {
            val cursor = readableDatabase.query(
                TABLE_QUEST_DATA, null,
                "$COL_QUEST_DATA_QUEST_LINE_ID = ?", arrayOf(questLineId),
                null, null, null
            )
            cursor.use {
                if (it.moveToFirst())
                {
                    return it.getInt(it.getColumnIndexOrThrow(COL_QUEST_DATA_NUM_OF_HINTS))
                }
                else null
            }
        } catch (e: Exception) {
            Log.e("DbConnection", "Ошибка getQuestData: ${e.message}")
        }
        return 0
    }

    fun updateQuestData(questLineId: String, hintCount: Int): Int{
        val cv = ContentValues()
        cv.put(COL_QUEST_DATA_NUM_OF_HINTS, hintCount)

        val result = writableDatabase.update(
            TABLE_QUEST_DATA, cv, "$COL_QUEST_DATA_QUEST_LINE_ID = ?", arrayOf(questLineId),
        )
        return result
    }

    fun addQuestData(questLineId: String, hintCount: Int): Long {
        val cv = ContentValues()
        cv.put(COL_QUEST_DATA_QUEST_LINE_ID, questLineId)
        cv.put(COL_QUEST_DATA_NUM_OF_HINTS, hintCount)

        val result = writableDatabase.insert(
            TABLE_QUEST_DATA, null, cv
        )

        return result
    }
}