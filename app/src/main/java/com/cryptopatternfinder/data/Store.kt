package com.cryptopatternfinder.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.cryptopatternfinder.core.Observation
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset

class Store(val appContext: Context) :
    SQLiteOpenHelper(appContext, "patterns.db", null, 3) {

    override fun onCreate(db: SQLiteDatabase) {

        db.execSQL(
            """
            CREATE TABLE observations(
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                exchange_name TEXT NOT NULL,
                symbol TEXT NOT NULL,
                observed_at INTEGER NOT NULL,
                change_percent REAL NOT NULL,
                screenshot_id INTEGER
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE screenshots(
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                exchange_name TEXT NOT NULL,
                captured_at INTEGER NOT NULL,
                image_uri TEXT
            )
            """.trimIndent()
        )

        db.execSQL(
            "CREATE INDEX idx_obs_symbol_time ON observations(symbol, observed_at)"
        )

        db.execSQL(
            "CREATE INDEX idx_obs_exchange_time ON observations(exchange_name, observed_at)"
        )

        db.execSQL(
            "CREATE INDEX idx_screenshot_time ON screenshots(captured_at)"
        )
    }

    override fun onUpgrade(
        db: SQLiteDatabase,
        oldVersion: Int,
        newVersion: Int
    ) {

        if (oldVersion < 3) {

            try {
                db.execSQL(
                    "ALTER TABLE observations ADD COLUMN screenshot_id INTEGER"
                )
            } catch (_: Exception) {
            }

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS screenshots(
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    exchange_name TEXT NOT NULL,
                    captured_at INTEGER NOT NULL,
                    image_uri TEXT
                )
                """.trimIndent()
            )

            db.execSQL(
                "CREATE INDEX IF NOT EXISTS idx_obs_symbol_time ON observations(symbol, observed_at)"
            )

            db.execSQL(
                "CREATE INDEX IF NOT EXISTS idx_obs_exchange_time ON observations(exchange_name, observed_at)"
            )

            db.execSQL(
                "CREATE INDEX IF NOT EXISTS idx_screenshot_time ON screenshots(captured_at)"
            )
        }
    }

    fun createScreenshot(
        exchange: String,
        capturedAt: LocalDateTime,
        imageUri: String?
    ): Long {

        val values = android.content.ContentValues().apply {
            put("exchange_name", exchange.ifBlank { "نامشخص" })
            put(
                "captured_at",
                capturedAt.toInstant(ZoneOffset.UTC).toEpochMilli()
            )

            if (imageUri != null) {
                put("image_uri", imageUri)
            }
        }

        return writableDatabase.insert(
            "screenshots",
            null,
            values
        )
    }

    fun insert(
        o: Observation,
        screenshotId: Long? = null
    ) {

        val values = android.content.ContentValues().apply {

            put("exchange_name", o.exchange.ifBlank { "نامشخص" })
            put("symbol", o.symbol)
            put(
                "observed_at",
                o.observedAt.toInstant(ZoneOffset.UTC).toEpochMilli()
            )
            put("change_percent", o.changePercent)

            if (screenshotId != null) {
                put("screenshot_id", screenshotId)
            }
        }

        writableDatabase.insert(
            "observations",
            null,
            values
        )
    }

    fun all(): List<Observation> {

        val out = mutableListOf<Observation>()

        readableDatabase.rawQuery(
            """
            SELECT
                exchange_name,
                symbol,
                observed_at,
                change_percent
            FROM observations
            ORDER BY observed_at ASC
            """.trimIndent(),
            null
        ).use { c ->

            while (c.moveToNext()) {

                out += Observation(
                    exchange = c.getString(0),
                    symbol = c.getString(1),
                    observedAt = LocalDateTime.ofInstant(
                        Instant.ofEpochMilli(c.getLong(2)),
                        ZoneOffset.UTC
                    ),
                    changePercent = c.getDouble(3)
                )
            }
        }

        return out
    }

    fun observationsForSymbol(
        symbol: String
    ): List<Observation> {

        val out = mutableListOf<Observation>()

        readableDatabase.rawQuery(
            """
            SELECT
                exchange_name,
                symbol,
                observed_at,
                change_percent
            FROM observations
            WHERE symbol = ?
            ORDER BY observed_at ASC
            """.trimIndent(),
            arrayOf(symbol)
        ).use { c ->

            while (c.moveToNext()) {

                out += Observation(
                    exchange = c.getString(0),
                    symbol = c.getString(1),
                    observedAt = LocalDateTime.ofInstant(
                        Instant.ofEpochMilli(c.getLong(2)),
                        ZoneOffset.UTC
                    ),
                    changePercent = c.getDouble(3)
                )
            }
        }

        return out
    }

    fun observationsForDate(
        date: LocalDate
    ): List<Observation> {

        val start = date
            .atStartOfDay()
            .toInstant(ZoneOffset.UTC)
            .toEpochMilli()

        val end = date
            .plusDays(1)
            .atStartOfDay()
            .toInstant(ZoneOffset.UTC)
            .toEpochMilli()

        val out = mutableListOf<Observation>()

        readableDatabase.rawQuery(
            """
            SELECT
                exchange_name,
                symbol,
                observed_at,
                change_percent
            FROM observations
            WHERE observed_at >= ?
              AND observed_at < ?
            ORDER BY observed_at ASC
            """.trimIndent(),
            arrayOf(
                start.toString(),
                end.toString()
            )
        ).use { c ->

            while (c.moveToNext()) {

                out += Observation(
                    exchange = c.getString(0),
                    symbol = c.getString(1),
                    observedAt = LocalDateTime.ofInstant(
                        Instant.ofEpochMilli(c.getLong(2)),
                        ZoneOffset.UTC
                    ),
                    changePercent = c.getDouble(3)
                )
            }
        }

        return out
    }

    fun countToday(): Int {

        val start = LocalDate.now()
            .atStartOfDay()
            .toInstant(ZoneOffset.UTC)
            .toEpochMilli()

        return readableDatabase.rawQuery(
            """
            SELECT COUNT(*)
            FROM observations
            WHERE observed_at >= ?
            """.trimIndent(),
            arrayOf(start.toString())
        ).use { c ->

            if (c.moveToFirst()) {
                c.getInt(0)
            } else {
                0
            }
        }
    }

    fun deleteOldScreenshotRecords(
        olderThan: LocalDateTime
    ) {

        val time = olderThan
            .toInstant(ZoneOffset.UTC)
            .toEpochMilli()

        /*
         * فقط رکورد مربوط به تصویر حذف می‌شود.
         *
         * اطلاعات observations حذف نمی‌شوند.
         */
        writableDatabase.delete(
            "screenshots",
            "captured_at < ?",
            arrayOf(time.toString())
        )
    }
}
