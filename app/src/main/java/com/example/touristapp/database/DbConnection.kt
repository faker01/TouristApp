package com.example.touristapp.database

import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.example.touristapp.models.Attraction
import com.example.touristapp.models.Route
import kotlin.collections.plusAssign

class DbConnection(context: Context) : SQLiteOpenHelper(
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

        // Связующая таблица маршрут ↔ место (many-to-many)
        const val TABLE_ROUTE_ATTRACTIONS = "route_attractions"
        const val COL_RA_ROUTE_ID = "route_id"
        const val COL_RA_ATT_ID = "attraction_id"
        const val COL_RA_ORDER = "sort_order"

        const val TABLE_QUEST_PROGRESS = "quest_progress"
        const val COL_QUEST_ID = "quest_id"
        const val COL_CURRENT_STEP = "current_step"
    }

    // ────────────────────────────────────────────────────────────────────────
    // Создание таблиц (вызывается один раз при первом запуске)
    // ────────────────────────────────────────────────────────────────────────

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS $TABLE_ATTRACTIONS (
                $COL_ATT_ID          INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_ATT_TITLE       TEXT    NOT NULL,
                $COL_ATT_LAT         REAL    NOT NULL,
                $COL_ATT_LON         REAL    NOT NULL,
                $COL_ATT_DESCRIPTION TEXT    DEFAULT ''
            )
        """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS $TABLE_ROUTES (
                $COL_RT_ID       INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_RT_NAME     TEXT NOT NULL,
                $COL_RT_DURATION TEXT DEFAULT '—',
                $COL_RT_DISTANCE TEXT DEFAULT '—',
                $COL_RT_COUNT    TEXT DEFAULT '0 мест'
            )
        """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS $TABLE_ROUTE_ATTRACTIONS (
                $COL_RA_ROUTE_ID INTEGER NOT NULL
                    REFERENCES $TABLE_ROUTES($COL_RT_ID) ON DELETE CASCADE,
                $COL_RA_ATT_ID   INTEGER NOT NULL
                    REFERENCES $TABLE_ATTRACTIONS($COL_ATT_ID) ON DELETE CASCADE,
                $COL_RA_ORDER    INTEGER DEFAULT 0,
                PRIMARY KEY ($COL_RA_ROUTE_ID, $COL_RA_ATT_ID)
            )
        """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS $TABLE_QUEST_PROGRESS (
                $COL_QUEST_ID TEXT NOT NULL UNIQUE,
                $COL_CURRENT_STEP INTEGER DEFAULT 0,
                PRIMARY KEY ($COL_QUEST_ID)
            )
        """.trimIndent()
        )

        // Включаем поддержку внешних ключей (CASCADE работает только с этим)
        db.execSQL("PRAGMA foreign_keys = ON;")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Простейшая стратегия: удалить и пересоздать.
        // В реальном приложении лучше писать миграции ALTER TABLE.
        db.execSQL("DROP TABLE IF EXISTS $TABLE_ROUTE_ATTRACTIONS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_ROUTES")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_ATTRACTIONS")
        onCreate(db)
    }

    // Включаем foreign keys при каждом открытии соединения
    override fun onOpen(db: SQLiteDatabase) {
        super.onOpen(db)
        if (!db.isReadOnly) db.execSQL("PRAGMA foreign_keys = ON;")
    }

    // ────────────────────────────────────────────────────────────────────────
    // Достопримечательности
    // ────────────────────────────────────────────────────────────────────────

    fun getAllAttractions(): List<Attraction> {
        val list = mutableListOf<Attraction>()
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
        return list
    }

    fun getAttractionById(id: Long): Attraction? {
        val cursor = readableDatabase.query(
            TABLE_ATTRACTIONS, null,
            "$COL_ATT_ID = ?", arrayOf(id.toString()),
            null, null, null
        )
        return cursor.use {
            if (it.moveToFirst()) Attraction(
                id = it.getInt(it.getColumnIndexOrThrow(COL_ATT_ID)),
                title = it.getString(it.getColumnIndexOrThrow(COL_ATT_TITLE)),
                lat = it.getDouble(it.getColumnIndexOrThrow(COL_ATT_LAT)),
                lon = it.getDouble(it.getColumnIndexOrThrow(COL_ATT_LON)),
                description = it.getString(it.getColumnIndexOrThrow(COL_ATT_DESCRIPTION))
            ) else null
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    // Маршруты
    // ────────────────────────────────────────────────────────────────────────
    fun getAllRoutes(): List<Route> {
        val routes = mutableListOf<Route>()
        val cursor = readableDatabase.query(
            TABLE_ROUTES, null, null, null, null, null, "$COL_RT_ID ASC"
        )
        cursor.use {
            while (it.moveToNext()) {
                val routeId = it.getLong(it.getColumnIndexOrThrow(COL_RT_ID))
                routes += Route(
                    name = it.getString(it.getColumnIndexOrThrow(COL_RT_NAME)),
                    duration = it.getString(it.getColumnIndexOrThrow(COL_RT_DURATION)),
                    distance = it.getString(it.getColumnIndexOrThrow(COL_RT_DISTANCE)),
                    count = it.getString(it.getColumnIndexOrThrow(COL_RT_COUNT)),
                    attractions = getAttractionsForRoute(routeId)
                )
            }
        }
        return routes
    }

    fun getAttractionsForRoute(routeId: Long): List<Attraction> {
        val sql = """
            SELECT a.* FROM $TABLE_ATTRACTIONS a
            INNER JOIN $TABLE_ROUTE_ATTRACTIONS ra
                ON a.$COL_ATT_ID = ra.$COL_RA_ATT_ID
            WHERE ra.$COL_RA_ROUTE_ID = ?
            ORDER BY ra.$COL_RA_ORDER ASC
        """.trimIndent()
        val list = mutableListOf<Attraction>()
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
        return list
    }
}