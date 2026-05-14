package com.example.touristapp.database

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
        const val COL_QUEST_ID = "quest_id"
        const val COL_CURRENT_STEP = "current_step"

        // Таблица общих данных квеста
        const val TABLE_QUEST_DATA = "quest_data"
        const val COL_NUM_OF_FRAGMENTS = "num_of_fragments"
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
}