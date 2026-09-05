package com.cryptopatternfinder.core

import java.time.DayOfWeek
import java.time.LocalDateTime

data class Observation(
    val exchange: String,
    val symbol: String,
    val name: String = "",
    val observedAt: LocalDateTime,
    val changePercent: Double
) {

    val direction: Direction
        get() = when {
            changePercent > 0 -> Direction.UP
            changePercent < 0 -> Direction.DOWN
            else -> Direction.FLAT
        }

    val weekday: DayOfWeek
        get() = observedAt.dayOfWeek

    val minuteOfDay: Int
        get() = observedAt.hour * 60 + observedAt.minute
}

enum class Direction {
    UP,
    DOWN,
    FLAT
}

data class SimilarityResult(
    val symbol: String,
    val scorePercent: Double,
    val sameDirectionPercent: Double,
    val observations: Int
)

data class RecurringPattern(
    val symbol: String,
    val weekday: DayOfWeek,
    val startMinute: Int,
    val occurrences: Int,
    val dominantDirection: Direction,
    val consistencyPercent: Double
)
