package com.example.touristapp.utils

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.example.touristapp.models.Attraction
import com.example.touristapp.models.Route

class DbConnection(context: Context) : SQLiteOpenHelper(
    context,
    DATABASE_NAME,
    null,
    DATABASE_VERSION
) {

    companion object {
        private const val DATABASE_NAME    = "tourist_app.db"
        private const val DATABASE_VERSION = 1

        // Таблица мест
        const val TABLE_ATTRACTIONS        = "attractions"
        const val COL_ATT_ID               = "id"
        const val COL_ATT_TITLE            = "title"
        const val COL_ATT_LAT              = "lat"
        const val COL_ATT_LON              = "lon"
        const val COL_ATT_DESCRIPTION      = "description"

        // Таблица маршрутов
        const val TABLE_ROUTES             = "routes"
        const val COL_RT_ID                = "id"
        const val COL_RT_NAME              = "name"
        const val COL_RT_DURATION          = "duration"
        const val COL_RT_DISTANCE          = "distance"
        const val COL_RT_COUNT             = "count"

        // Связующая таблица маршрут ↔ место (many-to-many)
        const val TABLE_ROUTE_ATTRACTIONS  = "route_attractions"
        const val COL_RA_ROUTE_ID          = "route_id"
        const val COL_RA_ATT_ID            = "attraction_id"
        const val COL_RA_ORDER             = "sort_order"
    }

    // ────────────────────────────────────────────────────────────────────────
    // Создание таблиц (вызывается один раз при первом запуске)
    // ────────────────────────────────────────────────────────────────────────

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS $TABLE_ATTRACTIONS (
                $COL_ATT_ID          INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_ATT_TITLE       TEXT    NOT NULL,
                $COL_ATT_LAT         REAL    NOT NULL,
                $COL_ATT_LON         REAL    NOT NULL,
                $COL_ATT_DESCRIPTION TEXT    DEFAULT ''
            )
        """.trimIndent())

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS $TABLE_ROUTES (
                $COL_RT_ID       INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_RT_NAME     TEXT NOT NULL,
                $COL_RT_DURATION TEXT DEFAULT '—',
                $COL_RT_DISTANCE TEXT DEFAULT '—',
                $COL_RT_COUNT    TEXT DEFAULT '0 мест'
            )
        """.trimIndent())

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS $TABLE_ROUTE_ATTRACTIONS (
                $COL_RA_ROUTE_ID INTEGER NOT NULL
                    REFERENCES $TABLE_ROUTES($COL_RT_ID) ON DELETE CASCADE,
                $COL_RA_ATT_ID   INTEGER NOT NULL
                    REFERENCES $TABLE_ATTRACTIONS($COL_ATT_ID) ON DELETE CASCADE,
                $COL_RA_ORDER    INTEGER DEFAULT 0,
                PRIMARY KEY ($COL_RA_ROUTE_ID, $COL_RA_ATT_ID)
            )
        """.trimIndent())

        // Включаем поддержку внешних ключей (CASCADE работает только с этим)
        db.execSQL("PRAGMA foreign_keys = ON;")

        // Заполняем стартовыми данными
        seedData(db)
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
    // Предзаполнение данными
    // ────────────────────────────────────────────────────────────────────────

    private fun seedData(db: SQLiteDatabase) {
        val attractions = listOf(
            Triple("Собор",              54.7065 to 20.5090, "Кафедральный собор XIV века на острове Канта"),
            Triple("Музей океана",       54.7044 to 20.4994, "Единственный в России музей Мирового океана"),
            Triple("Рыбная деревня",     54.7030 to 20.5095, "Этнографический комплекс на берегу Преголи"),
            Triple("Форт №5",            54.7240 to 20.4550, "Оборонительный форт XIX века"),
            Triple("Королевские ворота", 54.7210 to 20.5155, "Одни из семи сохранившихся ворот Кёнигсберга"),
            Triple("Форт №3",            54.7310 to 20.4720, "Форт «Король Фридрих Вильгельм I»"),
            Triple("Исторический музей", 54.7180 to 20.5080, "История Калининградского края")
        )

        val ids = attractions.map { (title, coords, desc) ->
            val cv = ContentValues().apply {
                put(COL_ATT_TITLE,       title)
                put(COL_ATT_LAT,         coords.first)
                put(COL_ATT_LON,         coords.second)
                put(COL_ATT_DESCRIPTION, desc)
            }
            db.insert(TABLE_ATTRACTIONS, null, cv)
        }
        // ids[0]=Собор, [1]=МузейОкеана, [2]=РыбнаяДеревня, [3]=Форт5, [4]=КоролевскиеВорота, [5]=Форт3, [6]=ИстМузей

        fun insertRoute(name: String, duration: String, distance: String, count: String, attIds: List<Long>) {
            val cv = ContentValues().apply {
                put(COL_RT_NAME,     name)
                put(COL_RT_DURATION, duration)
                put(COL_RT_DISTANCE, distance)
                put(COL_RT_COUNT,    count)
            }
            val routeId = db.insert(TABLE_ROUTES, null, cv)
            attIds.forEachIndexed { index, attId ->
                val ref = ContentValues().apply {
                    put(COL_RA_ROUTE_ID, routeId)
                    put(COL_RA_ATT_ID,   attId)
                    put(COL_RA_ORDER,    index)
                }
                db.insert(TABLE_ROUTE_ATTRACTIONS, null, ref)
            }
        }

        insertRoute("История", "2-3 ч", "4.5 км", "3 места",  listOf(ids[0], ids[2], ids[4]))
        insertRoute("Форты",   "4-5 ч", "8.2 км", "2 места",  listOf(ids[3], ids[5]))
        insertRoute("Музеи",   "3-4 ч", "3.8 км", "2 места",  listOf(ids[1], ids[6]))
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
                    id          = it.getInt(it.getColumnIndexOrThrow(COL_ATT_ID)),
                    title       = it.getString(it.getColumnIndexOrThrow(COL_ATT_TITLE)),
                    lat         = it.getDouble(it.getColumnIndexOrThrow(COL_ATT_LAT)),
                    lon         = it.getDouble(it.getColumnIndexOrThrow(COL_ATT_LON)),
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
                id          = it.getInt(it.getColumnIndexOrThrow(COL_ATT_ID)),
                title       = it.getString(it.getColumnIndexOrThrow(COL_ATT_TITLE)),
                lat         = it.getDouble(it.getColumnIndexOrThrow(COL_ATT_LAT)),
                lon         = it.getDouble(it.getColumnIndexOrThrow(COL_ATT_LON)),
                description = it.getString(it.getColumnIndexOrThrow(COL_ATT_DESCRIPTION))
            ) else null
        }
    }

    /*fun insertAttraction(title: String, lat: Double, lon: Double, description: String = ""): Long {
        val cv = ContentValues().apply {
            put(COL_ATT_TITLE,       title)
            put(COL_ATT_LAT,         lat)
            put(COL_ATT_LON,         lon)
            put(COL_ATT_DESCRIPTION, description)
        }
        return writableDatabase.insert(TABLE_ATTRACTIONS, null, cv)
    }*/

    /*fun updateAttraction(id: Long, title: String, lat: Double, lon: Double, description: String = "") {
        val cv = ContentValues().apply {
            put(COL_ATT_TITLE,       title)
            put(COL_ATT_LAT,         lat)
            put(COL_ATT_LON,         lon)
            put(COL_ATT_DESCRIPTION, description)
        }
        writableDatabase.update(TABLE_ATTRACTIONS, cv, "$COL_ATT_ID = ?", arrayOf(id.toString()))
    }*/

    /*fun deleteAttraction(id: Long) {
        writableDatabase.delete(TABLE_ATTRACTIONS, "$COL_ATT_ID = ?", arrayOf(id.toString()))
    }*/

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
                    name        = it.getString(it.getColumnIndexOrThrow(COL_RT_NAME)),
                    duration    = it.getString(it.getColumnIndexOrThrow(COL_RT_DURATION)),
                    distance    = it.getString(it.getColumnIndexOrThrow(COL_RT_DISTANCE)),
                    count       = it.getString(it.getColumnIndexOrThrow(COL_RT_COUNT)),
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
                    id          = it.getInt(it.getColumnIndexOrThrow(COL_ATT_ID)),
                    title       = it.getString(it.getColumnIndexOrThrow(COL_ATT_TITLE)),
                    lat         = it.getDouble(it.getColumnIndexOrThrow(COL_ATT_LAT)),
                    lon         = it.getDouble(it.getColumnIndexOrThrow(COL_ATT_LON)),
                    description = it.getString(it.getColumnIndexOrThrow(COL_ATT_DESCRIPTION))
                )
            }
        }
        return list
    }


    /*fun insertRoute(
        name: String,
        duration: String = "—",
        distance: String = "—",
        count: String,
        attractionIds: List<Long>
    ): Long {
        val db = writableDatabase
        db.beginTransaction()
        return try {
            val cv = ContentValues().apply {
                put(COL_RT_NAME,     name)
                put(COL_RT_DURATION, duration)
                put(COL_RT_DISTANCE, distance)
                put(COL_RT_COUNT,    count)
            }
            val routeId = db.insert(TABLE_ROUTES, null, cv)
            attractionIds.forEachIndexed { index, attId ->
                val ref = ContentValues().apply {
                    put(COL_RA_ROUTE_ID, routeId)
                    put(COL_RA_ATT_ID,   attId)
                    put(COL_RA_ORDER,    index)
                }
                db.insert(TABLE_ROUTE_ATTRACTIONS, null, ref)
            }
            db.setTransactionSuccessful()
            routeId
        } finally {
            db.endTransaction()
        }
    }*/

    /*fun updateRoute(id: Long, name: String, duration: String, distance: String, count: String) {
        val cv = ContentValues().apply {
            put(COL_RT_NAME,     name)
            put(COL_RT_DURATION, duration)
            put(COL_RT_DISTANCE, distance)
            put(COL_RT_COUNT,    count)
        }
        writableDatabase.update(TABLE_ROUTES, cv, "$COL_RT_ID = ?", arrayOf(id.toString()))
    }*/

    /*fun replaceRouteAttractions(routeId: Long, attractionIds: List<Long>) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            db.delete(TABLE_ROUTE_ATTRACTIONS, "$COL_RA_ROUTE_ID = ?", arrayOf(routeId.toString()))
            attractionIds.forEachIndexed { index, attId ->
                val cv = ContentValues().apply {
                    put(COL_RA_ROUTE_ID, routeId)
                    put(COL_RA_ATT_ID,   attId)
                    put(COL_RA_ORDER,    index)
                }
                db.insert(TABLE_ROUTE_ATTRACTIONS, null, cv)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }*/

    /*fun deleteRoute(id: Long) {
        writableDatabase.delete(TABLE_ROUTES, "$COL_RT_ID = ?", arrayOf(id.toString()))
    }*/
}