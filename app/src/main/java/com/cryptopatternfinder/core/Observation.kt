package com.cryptopatternfinder.core

import java.time.LocalDateTime

data class Observation(
    val exchange: String,
    val symbol: String,
    val name: String,
    val observedAt: LocalDateTime,
    val changePercent: Double
)
