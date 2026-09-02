package com.cryptopatternfinder.data
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.cryptopatternfinder.core.Observation
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset

class Store(val appContext:Context):SQLiteOpenHelper(appContext,"patterns.db",null,1) {
    override fun onCreate(db:SQLiteDatabase) {
        db.execSQL("CREATE TABLE observations(id INTEGER PRIMARY KEY AUTOINCREMENT, exchange_name TEXT NOT NULL, symbol TEXT NOT NULL, observed_at INTEGER NOT NULL, change_percent REAL NOT NULL, image_uri TEXT)")
        db.execSQL("CREATE INDEX idx_obs ON observations(symbol,observed_at)")
    }
    override fun onUpgrade(db:SQLiteDatabase,o:Int,n:Int) {}
    fun insert(o:Observation,imageUri:String?) {
        writableDatabase.execSQL(
            "INSERT INTO observations(exchange_name,symbol,observed_at,change_percent,image_uri) VALUES(?,?,?,?,?)",
            arrayOf(o.exchange,o.symbol,o.observedAt.toInstant(ZoneOffset.UTC).toEpochMilli(),o.changePercent,imageUri)
        )
    }
    fun all():List<Observation> {
        val out=mutableListOf<Observation>()
        readableDatabase.rawQuery("SELECT exchange_name,symbol,observed_at,change_percent FROM observations ORDER BY observed_at",null).use { c ->
            while(c.moveToNext()) out += Observation(
                c.getString(0),c.getString(1),
                LocalDateTime.ofInstant(Instant.ofEpochMilli(c.getLong(2)),ZoneOffset.UTC),
                c.getDouble(3)
            )
        }
        return out
    }
}
