package com.cryptopatternfinder.analysis

import com.cryptopatternfinder.core.Direction
import com.cryptopatternfinder.core.Observation
import com.cryptopatternfinder.core.RecurringPattern
import com.cryptopatternfinder.core.SimilarityResult
import java.time.DayOfWeek
import kotlin.math.abs
import kotlin.math.max

object AnalysisEngine {

    /*
     * ============================================================
     * خلاصه آماری یک ارز
     * ============================================================
     */

    data class SymbolAnalysis(
        val symbol: String,
        val observations: Int,
        val averageChange: Double,
        val positiveCount: Int,
        val negativeCount: Int,
        val flatCount: Int,
        val upPercent: Double,
        val downPercent: Double,
        val strongestRise: Double,
        val strongestFall: Double,
        val lastChange: Double?,
        val trend: Direction
    )

    /*
     * ============================================================
     * تحلیل یک ارز
     * ============================================================
     */

    fun analyzeSymbol(
        observations: List<Observation>,
        symbol: String
    ): SymbolAnalysis? {

        val data =
            observations
                .filter {
                    it.symbol.equals(
                        symbol,
                        ignoreCase = true
                    )
                }
                .sortedBy {
                    it.observedAt
                }

        if (data.isEmpty()) {
            return null
        }

        val positive =
            data.count {
                it.changePercent > 0
            }

        val negative =
            data.count {
                it.changePercent < 0
            }

        val flat =
            data.count {
                it.changePercent == 0.0
            }

        val average =
            data
                .map {
                    it.changePercent
                }
                .average()

        val strongestRise =
            data
                .maxOfOrNull {
                    it.changePercent
                }
                ?: 0.0

        val strongestFall =
            data
                .minOfOrNull {
                    it.changePercent
                }
                ?: 0.0

        val last =
            data.lastOrNull()
                ?.changePercent

        val trend =
            when {

                positive > negative ->
                    Direction.UP

                negative > positive ->
                    Direction.DOWN

                else ->
                    Direction.FLAT
            }

        return SymbolAnalysis(

            symbol = symbol,

            observations =
                data.size,

            averageChange =
                average,

            positiveCount =
                positive,

            negativeCount =
                negative,

            flatCount =
                flat,

            upPercent =
                positive * 100.0 /
                    data.size,

            downPercent =
                negative * 100.0 /
                    data.size,

            strongestRise =
                strongestRise,

            strongestFall =
                strongestFall,

            lastChange =
                last,

            trend =
                trend
        )
    }

    /*
     * ============================================================
     * تحلیل تمام ارزهای موجود
     * ============================================================
     */

    fun analyzeAll(
        observations: List<Observation>
    ): List<SymbolAnalysis> {

        return observations
            .map {
                it.symbol
            }
            .distinct()
            .mapNotNull {
                analyzeSymbol(
                    observations,
                    it
                )
            }
            .sortedByDescending {
                abs(it.averageChange)
            }
    }

    /*
     * ============================================================
     * پیدا کردن الگوهای تکرارشونده
     * ============================================================
     */

    fun findRecurringPatterns(
        observations: List<Observation>,
        minimumOccurrences: Int = 2
    ): List<RecurringPattern> {

        if (observations.isEmpty()) {
            return emptyList()
        }

        data class PatternKey(
            val symbol: String,
            val weekday: DayOfWeek,
            val minute: Int
        )

        val groups =
            observations.groupBy {

                PatternKey(
                    symbol =
                        it.symbol,

                    weekday =
                        it.weekday,

                    minute =
                        normalizeMinute(
                            it.minuteOfDay
                        )
                )
            }

        val result =
            mutableListOf<RecurringPattern>()

        for (
            (key, values) in groups
        ) {

            if (
                values.size <
                minimumOccurrences
            ) {
                continue
            }

            val up =
                values.count {
                    it.direction ==
                        Direction.UP
                }

            val down =
                values.count {
                    it.direction ==
                        Direction.DOWN
                }

            val flat =
                values.count {
                    it.direction ==
                        Direction.FLAT
                }

            val dominant =
                when {

                    up >= down &&
                        up >= flat ->
                        Direction.UP

                    down >= up &&
                        down >= flat ->
                        Direction.DOWN

                    else ->
                        Direction.FLAT
                }

            val dominantCount =
                max(
                    up,
                    max(
                        down,
                        flat
                    )
                )

            val consistency =
                dominantCount * 100.0 /
                    values.size

            result +=
                RecurringPattern(

                    symbol =
                        key.symbol,

                    weekday =
                        key.weekday,

                    startMinute =
                        key.minute,

                    occurrences =
                        values.size,

                    dominantDirection =
                        dominant,

                    consistencyPercent =
                        consistency
                )
        }

        return result
            .sortedWith(
                compareByDescending<RecurringPattern> {
                    it.consistencyPercent
                }.thenByDescending {
                    it.occurrences
                }
            )
    }

    /*
     * ============================================================
     * نرمال‌سازی دقیقه
     *
     * داده‌های نزدیک به هم را در یک بازه قرار می‌دهد.
     * مثال:
     * 10:01 و 10:03 و 10:04
     * می‌توانند در یک بازه تحلیل شوند.
     * ============================================================
     */

    private fun normalizeMinute(
        minute: Int,
        bucketSize: Int = 5
    ): Int {

        if (bucketSize <= 1) {
            return minute
        }

        return (
            minute /
                bucketSize
            ) * bucketSize
    }

    /*
     * ============================================================
     * مقایسه دو ارز
     * ============================================================
     */

    fun compareSymbols(
        observations: List<Observation>,
        firstSymbol: String,
        secondSymbol: String
    ): Double {

        val first =
            observations
                .filter {
                    it.symbol.equals(
                        firstSymbol,
                        ignoreCase = true
                    )
                }
                .sortedBy {
                    it.observedAt
                }

        val second =
            observations
                .filter {
                    it.symbol.equals(
                        secondSymbol,
                        ignoreCase = true
                    )
                }
                .sortedBy {
                    it.observedAt
                }

        if (
            first.isEmpty() ||
            second.isEmpty()
        ) {
            return 0.0
        }

        val firstByTime =
            first.associateBy {
                it.observedAt
            }

        val secondByTime =
            second.associateBy {
                it.observedAt
            }

        val commonTimes =
            firstByTime.keys
                .intersect(
                    secondByTime.keys
                )

        if (commonTimes.isEmpty()) {
            return 0.0
        }

        var sameDirection = 0

        for (time in commonTimes) {

            val a =
                firstByTime[time]
                    ?: continue

            val b =
                secondByTime[time]
                    ?: continue

            if (
                a.direction ==
                b.direction
            ) {
                sameDirection++
            }
        }

        return sameDirection * 100.0 /
            commonTimes.size
    }

    /*
     * ============================================================
     * شباهت دو سری داده
     * ============================================================
     */

    fun similarity(
        first: List<Observation>,
        second: List<Observation>
    ): SimilarityResult? {

        if (
            first.isEmpty() ||
            second.isEmpty()
        ) {
            return null
        }

        val firstSorted =
            first.sortedBy {
                it.observedAt
            }

        val secondSorted =
            second.sortedBy {
                it.observedAt
            }

        val count =
            minOf(
                firstSorted.size,
                secondSorted.size
            )

        if (count == 0) {
            return null
        }

        var directionMatches = 0

        var totalDifference = 0.0

        for (i in 0 until count) {

            val a =
                firstSorted[i]

            val b =
                secondSorted[i]

            if (
                a.direction ==
                b.direction
            ) {
                directionMatches++
            }

            totalDifference +=
                abs(
                    a.changePercent -
                        b.changePercent
                )
        }

        val sameDirectionPercent =
            directionMatches * 100.0 /
                count

        val averageDifference =
            totalDifference /
                count

        /*
         * هرچه اختلاف درصد کمتر باشد،
         * شباهت بیشتر است.
         */

        val score =
            (
                sameDirectionPercent *
                    0.7
                +
                max(
                    0.0,
                    100.0 -
                        averageDifference * 10.0
                ) *
                    0.3
            ).coerceIn(
                0.0,
                100.0
            )

        return SimilarityResult(

            symbol =
                "${firstSorted.first().symbol} ↔ " +
                    secondSorted.first().symbol,

            scorePercent =
                score,

            sameDirectionPercent =
                sameDirectionPercent,

            observations =
                count
        )
    }

    /*
     * ============================================================
     * پیدا کردن ارزهایی که بیشترین شباهت را دارند
     * ============================================================
     */

    fun findSimilarSymbols(
        observations: List<Observation>,
        targetSymbol: String
    ): List<SimilarityResult> {

        val target =
            observations.filter {
                it.symbol.equals(
                    targetSymbol,
                    ignoreCase = true
                )
            }

        if (target.isEmpty()) {
            return emptyList()
        }

        return observations
            .map {
                it.symbol
            }
            .distinct()
            .filter {
                !it.equals(
                    targetSymbol,
                    ignoreCase = true
                )
            }
            .mapNotNull { symbol ->

                val other =
                    observations.filter {
                        it.symbol == symbol
                    }

                similarity(
                    target,
                    other
                )
            }
            .sortedByDescending {
                it.scorePercent
            }
    }

    /*
     * ============================================================
     * قوی‌ترین حرکت‌ها
     * ============================================================
     */

    fun strongestMoves(
        observations: List<Observation>,
        limit: Int = 10
    ): List<Observation> {

        return observations
            .sortedByDescending {
                abs(it.changePercent)
            }
            .take(
                limit.coerceAtLeast(1)
            )
    }

    /*
     * ============================================================
     * بیشترین صعودها
     * ============================================================
     */

    fun strongestRisers(
        observations: List<Observation>,
        limit: Int = 10
    ): List<Observation> {

        return observations
            .filter {
                it.changePercent > 0
            }
            .sortedByDescending {
                it.changePercent
            }
            .take(
                limit.coerceAtLeast(1)
            )
    }

    /*
     * ============================================================
     * بیشترین نزول‌ها
     * ============================================================
     */

    fun strongestFallers(
        observations: List<Observation>,
        limit: Int = 10
    ): List<Observation> {

        return observations
            .filter {
                it.changePercent < 0
            }
            .sortedBy {
                it.changePercent
            }
            .take(
                limit.coerceAtLeast(1)
            )
    }

    /*
     * ============================================================
     * امتیاز جهت بازار
     *
     * خروجی:
     * +100 = همه صعودی
     *   0  = متعادل
     * -100 = همه نزولی
     * ============================================================
     */

    fun marketDirectionScore(
        observations: List<Observation>
    ): Double {

        if (observations.isEmpty()) {
            return 0.0
        }

        val up =
            observations.count {
                it.direction ==
                    Direction.UP
            }

        val down =
            observations.count {
                it.direction ==
                    Direction.DOWN
            }

        return (
            up - down
        ) * 100.0 /
            observations.size
    }
}
