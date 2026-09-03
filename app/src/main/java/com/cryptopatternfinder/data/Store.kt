package com.cryptopatternfinder.data

import android.content.ContentValues
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
                coin_name TEXT NOT NULL DEFAULT '',
                observed_at INTEGER NOT NULL,
                change_percent REAL NOT NULL
            )
            """.trimIndent()
        )

        db.execSQL(
            "CREATE INDEX idx_obs_symbol_time ON observations(symbol, observed_at)"
        )

        db.execSQL(
            "CREATE INDEX idx_obs_exchange ON observations(exchange_name)"
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
                    "ALTER TABLE observations ADD COLUMN coin_name TEXT NOT NULL DEFAULT ''"
                )
            } catch (_: Exception) {
                // ستون از قبل وجود دارد.
            }
        }
    }

    fun insert(o: Observation) {

        val values = ContentValues().apply {

            put("exchange_name", o.exchange)

            put("symbol", o.symbol)

            put("coin_name", o.name)

            put(
                "observed_at",
                o.observedAt
                    .toInstant(ZoneOffset.UTC)
                    .toEpochMilli()
            )

            put(
                "change_percent",
                o.changePercent
            )
        }

        writableDatabase.insert(
            "observations",
            null,
            values
        )
    }

    fun all(): List<Observation> {

        val result = mutableListOf<Observation>()

        readableDatabase.rawQuery(
            """
            SELECT
                exchange_name,
                symbol,
                coin_name,
                observed_at,
                change_percent
            FROM observations
            ORDER BY observed_at DESC
            """.trimIndent(),
            null
        ).use { cursor ->

            while (cursor.moveToNext()) {

                val exchange =
                    cursor.getString(0)

                val symbol =
                    cursor.getString(1)

                val name =
                    cursor.getString(2)

                val time =
                    LocalDateTime.ofInstant(
                        Instant.ofEpochMilli(
                            cursor.getLong(3)
                        ),
                        ZoneOffset.UTC
                    )

                val change =
                    cursor.getDouble(4)

                result += Observation(
                    exchange = exchange,
                    symbol = symbol,
                    name = name,
                    observedAt = time,
                    changePercent = change
                )
            }
        }

        return result
    }

    fun symbols(): List<String> {

        val result = mutableListOf<String>()

        readableDatabase.rawQuery(
            """
            SELECT DISTINCT symbol
            FROM observations
            ORDER BY symbol
            """.trimIndent(),
            null
        ).use { cursor ->

            while (cursor.moveToNext()) {
                result += cursor.getString(0)
            }
        }

        return result
    }

    fun exchanges(): List<String> {

        val result = mutableListOf<String>()

        readableDatabase.rawQuery(
            """
            SELECT DISTINCT exchange_name
            FROM observations
            WHERE exchange_name <> ''
            ORDER BY exchange_name
            """.trimIndent(),
            null
        ).use { cursor ->

            while (cursor.moveToNext()) {
                result += cursor.getString(0)
            }
        }

        return result
    }

    fun observationsForSymbol(
        symbol: String
    ): List<Observation> {

        return all()
            .filter { it.symbol == symbol }
            .sortedBy { it.observedAt }
    }

    fun observationsForExchange(
        exchange: String
    ): List<Observation> {

        return all()
            .filter { it.exchange == exchange }
            .sortedBy { it.observedAt }
    }

    fun observationsForDate(
        date: LocalDate
    ): List<Observation> {

        return all()
            .filter {
                it.observedAt.toLocalDate() == date
            }
            .sortedBy { it.observedAt }
    }

    fun countAll(): Int {

        readableDatabase.rawQuery(
            "SELECT COUNT(*) FROM observations",
            null
        ).use { cursor ->

            return if (cursor.moveToFirst()) {
                cursor.getInt(0)
            } else {
                0
            }
        }
    }

    fun countToday(): Int {

        val start =
            LocalDate
                .now()
                .atStartOfDay()
                .toInstant(ZoneOffset.UTC)
                .toEpochMilli()

        val end =
            LocalDate
                .now()
                .plusDays(1)
                .atStartOfDay()
                .toInstant(ZoneOffset.UTC)
                .toEpochMilli()

        readableDatabase.rawQuery(
            """
            SELECT COUNT(*)
            FROM observations
            WHERE observed_at >= ?
            AND observed_at < ?
            """.trimIndent(),
            arrayOf(
                start.toString(),
                end.toString()
            )
        ).use { cursor ->

            return if (cursor.moveToFirst()) {
                cursor.getInt(0)
            } else {
                0
            }
        }
    }
}
