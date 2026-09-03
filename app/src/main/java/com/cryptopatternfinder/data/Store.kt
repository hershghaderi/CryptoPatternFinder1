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
    SQLiteOpenHelper(appContext, "patterns.db", null, 2) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE observations(
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                exchange_name TEXT NOT NULL,
                symbol TEXT NOT NULL,
                observed_at INTEGER NOT NULL,
                change_percent REAL NOT NULL
            )
            """.trimIndent()
        )

        db.execSQL(
            "CREATE INDEX idx_obs ON observations(symbol, observed_at)"
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            try {
                db.execSQL("ALTER TABLE observations DROP COLUMN image_uri")
            } catch (_: Exception) {
                // اگر ستون وجود نداشته باشد، کاری انجام نمی‌شود.
            }
        }
    }

    fun insert(o: Observation) {
        writableDatabase.execSQL(
            """
            INSERT INTO observations(
                exchange_name,
                symbol,
                observed_at,
                change_percent
            ) VALUES(?,?,?,?)
            """.trimIndent(),
            arrayOf(
                o.exchange,
                o.symbol,
                o.observedAt.toInstant(ZoneOffset.UTC).toEpochMilli(),
                o.changePercent
            )
        )
    }

    fun all(): List<Observation> {
        val out = mutableListOf<Observation>()

        readableDatabase.rawQuery(
            """
            SELECT exchange_name, symbol, observed_at, change_percent
            FROM observations
            ORDER BY observed_at
            """.trimIndent(),
            null
        ).use { c ->
            while (c.moveToNext()) {
                out += Observation(
                    c.getString(0),
                    c.getString(1),
                    LocalDateTime.ofInstant(
                        Instant.ofEpochMilli(c.getLong(2)),
                        ZoneOffset.UTC
                    ),
                    c.getDouble(3)
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
            "SELECT COUNT(*) FROM observations WHERE observed_at >= ?",
            arrayOf(start.toString())
        ).use { c ->
            if (c.moveToFirst()) c.getInt(0) else 0
        }
    }
}
