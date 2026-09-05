package com.cryptopatternfinder.ocr

import com.cryptopatternfinder.core.Observation
import java.time.LocalDateTime
import java.util.Locale
import kotlin.math.abs

object OcrParser {

    /*
     * ============================================================
     * شناسه‌ها و نام‌های شناخته‌شده ارزها
     * ============================================================
     */

    private val knownSymbols = linkedMapOf(

        "BITCOIN" to "BTC",
        "BTC" to "BTC",

        "ETHEREUM" to "ETH",
        "ETH" to "ETH",

        "BINANCECOIN" to "BNB",
        "BINANCE" to "BNB",
        "BNB" to "BNB",

        "SOLANA" to "SOL",
        "SOL" to "SOL",

        "RIPPLE" to "XRP",
        "XRP" to "XRP",

        "CARDANO" to "ADA",
        "ADA" to "ADA",

        "DOGECOIN" to "DOGE",
        "DOGE" to "DOGE",

        "TONCOIN" to "TON",
        "TON" to "TON",

        "TRON" to "TRX",
        "TRX" to "TRX",

        "AVALANCHE" to "AVAX",
        "AVAX" to "AVAX",

        "SHIBAINU" to "SHIB",
        "SHIBA INU" to "SHIB",
        "SHIB" to "SHIB",

        "POLKADOT" to "DOT",
        "DOT" to "DOT",

        "CHAINLINK" to "LINK",
        "LINK" to "LINK",

        "POLYGON" to "POL",
        "MATIC" to "MATIC",
        "POL" to "POL",

        "LITECOIN" to "LTC",
        "LTC" to "LTC",

        "BITCOINCASH" to "BCH",
        "BCH" to "BCH",

        "UNISWAP" to "UNI",
        "UNI" to "UNI",

        "COSMOS" to "ATOM",
        "COSMOSHUB" to "ATOM",
        "ATOM" to "ATOM",

        "ETHEREUMCLASSIC" to "ETC",
        "ETC" to "ETC",

        "STELLAR" to "XLM",
        "XLM" to "XLM",

        "NEARPROTOCOL" to "NEAR",
        "NEAR" to "NEAR",

        "APTOS" to "APT",
        "APT" to "APT",

        "FILECOIN" to "FIL",
        "FIL" to "FIL",

        "ARBITRUM" to "ARB",
        "ARB" to "ARB",

        "OPTIMISM" to "OP",
        "OP" to "OP",

        "SUI" to "SUI",

        "INJECTIVE" to "INJ",
        "INJ" to "INJ",

        "AAVE" to "AAVE",

        "ALGORAND" to "ALGO",
        "ALGO" to "ALGO",

        "VECHAIN" to "VET",
        "VET" to "VET",

        "INTERNETCOMPUTER" to "ICP",
        "ICP" to "ICP",

        "HEDERAHASHGRAPH" to "HBAR",
        "HBAR" to "HBAR",

        "MAKER" to "MKR",
        "MKR" to "MKR",

        "PEPE" to "PEPE",
        "FLOKI" to "FLOKI",
        "BONK" to "BONK",
        "SEI" to "SEI",
        "TIA" to "TIA",

        "THORCHAIN" to "RUNE",
        "RUNE" to "RUNE",

        "EOS" to "EOS",

        "CRONOS" to "CRO",
        "CRO" to "CRO",

        "KASPA" to "KAS",
        "KAS" to "KAS",

        "MONERO" to "XMR",
        "XMR" to "XMR",

        "QUANT" to "QNT",
        "QNT" to "QNT",

        "FANTOM" to "FTM",
        "FTM" to "FTM",

        "TEZOS" to "XTZ",
        "XTZ" to "XTZ",

        "FLOW" to "FLOW",

        "SAND" to "SAND",
        "MANA" to "MANA",

        "AXIEINFINITY" to "AXS",
        "AXS" to "AXS",

        "THEGRAPH" to "GRT",
        "GRT" to "GRT",

        "KAVA" to "KAVA",

        "ALPHA" to "ALPHA",

        "ZILLIQA" to "ZIL",
        "ZIL" to "ZIL",

        "IOTA" to "IOTA",
        "THETA" to "THETA",

        "NEO" to "NEO",
        "DASH" to "DASH",

        "LUNC" to "LUNC",

        "TERRA" to "LUNA",
        "LUNA" to "LUNA",

        "ORDI" to "ORDI",

        "STX" to "STX",
        "STACKS" to "STX",

        "MANTLE" to "MNT",
        "MNT" to "MNT",

        "WIF" to "WIF",

        "JUPITER" to "JUP",
        "JUP" to "JUP",

        "JASMY" to "JASMY",
        "JASMYCOIN" to "JASMY",

        "IMX" to "IMX",
        "IMMUTABLE" to "IMX",

        "ONDO" to "ONDO",
        "ONDOFINANCE" to "ONDO",

        "FET" to "FET",
        "FETCHAI" to "FET",

        "RENDER" to "RENDER",
        "RNDR" to "RNDR",

        "TAO" to "TAO",
        "BITTENSOR" to "TAO",

        "ENA" to "ENA",
        "ETHENA" to "ENA"
    )

    /*
     * ============================================================
     * الگوهای تشخیص درصد
     * ============================================================
     */

    private val percentRegex = Regex(
        """([+-]?\s*\d+(?:[.,]\d+)?)\s*[%٪]"""
    )

    private val percentOcrRegex = Regex(
        """([+-]?\s*[0-9OIlLZS]{1,4}(?:[.,][0-9OIlLZS]{1,4})?)\s*[Pp]"""
    )

    private val signedNumberRegex = Regex(
        """([+-])\s*([0-9OIlLZS]{1,4}(?:[.,][0-9OIlLZS]{1,4})?)"""
    )

    private val loosePercentRegex = Regex(
        """([+-]?\s*[0-9OIlLZS]{1,4}(?:[.,][0-9OIlLZS]{1,4})?)\s*(?:%|٪|P|p)"""
    )

    /*
     * ============================================================
     * ساختارهای داخلی Parser
     * ============================================================
     */

    private data class SymbolHit(
        val symbol: String,
        val position: Int,
        val line: Int = -1
    )

    private data class PercentHit(
        val value: Double,
        val position: Int,
        val line: Int = -1
    )

    /*
     * ============================================================
     * نرمال‌سازی متن
     *
     * نکته مهم:
     * newline را حفظ می‌کنیم.
     * ============================================================
     */

    private fun normalize(text: String): String {

        return text
            .uppercase(Locale.US)
            .replace('٪', '%')
            .replace('−', '-')
            .replace('–', '-')
            .replace('—', '-')
            .replace('﹣', '-')
            .replace('‐', '-')
            .replace('﹘', '-')
            .replace('|', 'I')
            .replace('«', '<')
            .replace('»', '>')
            .replace('\r', '\n')
            .replace(Regex("""[ \t]+"""), " ")
            .replace(
                Regex("""\n{2,}"""),
                "\n"
            )
            .trim()
    }

    /*
     * برای جستجوی Symbol،
     * فاصله‌های داخل متن حذف می‌شوند.
     */
    private fun compact(text: String): String {

        return normalize(text)
            .replace(" ", "")
    }

    /*
     * اصلاح خطاهای رایج OCR در اعداد.
     */

    private fun normalizeOcrNumber(
        value: String
    ): String {

        return value
            .uppercase(Locale.US)
            .replace("O", "0")
            .replace("I", "1")
            .replace("L", "1")
            .replace("Z", "2")
            .replace("S", "5")
            .replace(",", ".")
            .replace(" ", "")
    }

    /*
     * ============================================================
     * پیدا کردن Symbolها
     * ============================================================
     */

    private fun findAllSymbols(
        text: String
    ): List<SymbolHit> {

        val normalized =
            normalize(text)

        val hits =
            mutableListOf<SymbolHit>()

        val entries =
            knownSymbols.entries
                .sortedByDescending {
                    it.key
                        .replace(" ", "")
                        .length
                }

        normalized
            .split("\n")
            .forEachIndexed { lineIndex, line ->

                val normalizedLine =
                    compact(line)

                if (normalizedLine.isBlank()) {
                    return@forEachIndexed
                }

                for ((name, symbol) in entries) {

                    val key =
                        name
                            .replace(" ", "")
                            .uppercase(
                                Locale.US
                            )

                    var start = 0

                    while (true) {

                        val position =
                            normalizedLine.indexOf(
                                key,
                                start
                            )

                        if (position < 0) {
                            break
                        }

                        hits += SymbolHit(
                            symbol = symbol,
                            position = position,
                            line = lineIndex
                        )

                        start =
                            position + key.length
                    }
                }
            }

        /*
         * اگر OCR newline مناسبی نداشت،
         * کل متن نیز بررسی می‌شود.
         */

        if (hits.isEmpty()) {

            val full =
                compact(text)

            for ((name, symbol) in entries) {

                val key =
                    name
                        .replace(" ", "")
                        .uppercase(
                            Locale.US
                        )

                var start = 0

                while (true) {

                    val position =
                        full.indexOf(
                            key,
                            start
                        )

                    if (position < 0) {
                        break
                    }

                    hits += SymbolHit(
                        symbol = symbol,
                        position = position,
                        line = -1
                    )

                    start =
                        position + key.length
                }
            }
        }

        return removeDuplicateSymbolHits(
            hits.sortedBy {
                it.position
            }
        )
    }

    private fun removeDuplicateSymbolHits(
        hits: List<SymbolHit>
    ): List<SymbolHit> {

        val result =
            mutableListOf<SymbolHit>()

        for (hit in hits) {

            val duplicate =
                result.any { previous ->

                    previous.symbol ==
                        hit.symbol &&
                        previous.line ==
                        hit.line &&
                        abs(
                            previous.position -
                                hit.position
                        ) <= 3
                }

            if (!duplicate) {
                result += hit
            }
        }

        return result
    }

    /*
     * ============================================================
     * پیدا کردن درصدها
     * ============================================================
     */

    private fun findAllPercents(
        text: String
    ): List<PercentHit> {

        val normalized =
            normalize(text)

        val result =
            mutableListOf<PercentHit>()

        /*
         * 1 ـ درصد واقعی
         */

        percentRegex
            .findAll(normalized)
            .forEach { match ->

                val value =
                    normalizeOcrNumber(
                        match.groupValues[1]
                    ).toDoubleOrNull()

                if (
                    value != null &&
                    value in -100.0..1000.0
                ) {

                    result += PercentHit(
                        value = value,
                        position =
                            match.range.first,
                        line =
                            findLineNumber(
                                normalized,
                                match.range.first
                            )
                    )
                }
            }

        /*
         * 2 ـ OCR که % را P خوانده است.
         */

        percentOcrRegex
            .findAll(normalized)
            .forEach { match ->

                val value =
                    normalizeOcrNumber(
                        match.groupValues[1]
                    ).toDoubleOrNull()

                if (
                    value != null &&
                    value in -100.0..1000.0
                ) {

                    val position =
                        match.range.first

                    val duplicate =
                        result.any {
                            abs(
                                it.position -
                                    position
                            ) <= 3
                        }

                    if (!duplicate) {

                        result += PercentHit(
                            value = value,
                            position = position,
                            line =
                                findLineNumber(
                                    normalized,
                                    position
                                )
                        )
                    }
                }
            }

        /*
         * 3 ـ عدد دارای + یا -
         */

        if (result.isEmpty()) {

            signedNumberRegex
                .findAll(normalized)
                .forEach { match ->

                    val value =
                        normalizeOcrNumber(
                            match.groupValues[1] +
                                match.groupValues[2]
                        ).toDoubleOrNull()

                    if (
                        value != null &&
                        value in -100.0..1000.0
                    ) {

                        result += PercentHit(
                            value = value,
                            position =
                                match.range.first,
                            line =
                                findLineNumber(
                                    normalized,
                                    match.range.first
                                )
                        )
                    }
                }
        }

        /*
         * 4 ـ درصد ناقص
         */

        if (result.isEmpty()) {

            loosePercentRegex
                .findAll(normalized)
                .forEach { match ->

                    val value =
                        normalizeOcrNumber(
                            match.groupValues[1]
                        ).toDoubleOrNull()

                    if (
                        value != null &&
                        value in -100.0..1000.0
                    ) {

                        result += PercentHit(
                            value = value,
                            position =
                                match.range.first,
                            line =
                                findLineNumber(
                                    normalized,
                                    match.range.first
                                )
                        )
                    }
                }
        }

        return result
            .sortedBy {
                it.position
            }
            .distinctBy {
                Triple(
                    it.value,
                    it.position,
                    it.line
                )
            }
    }

    /*
     * ============================================================
     * شماره خط
     * ============================================================
     */

    private fun findLineNumber(
        text: String,
        position: Int
    ): Int {

        if (position <= 0) {
            return 0
        }

        return text
            .substring(
                0,
                position.coerceAtMost(
                    text.length
                )
            )
            .count {
                it == '\n'
            }
    }

    /*
     * ============================================================
     * نام کامل ارز
     * ============================================================
     */

    private fun findName(
        text: String,
        symbol: String
    ): String {

        val normalized =
            compact(text)

        val entry =
            knownSymbols.entries
                .sortedByDescending {
                    it.key
                        .replace(" ", "")
                        .length
                }
                .firstOrNull { entry ->

                    entry.value == symbol &&
                        normalized.contains(
                            entry.key
                                .replace(
                                    " ",
                                    ""
                                )
                        )
                }

        return entry
            ?.key
            ?.lowercase(Locale.US)
            ?.replaceFirstChar {
                it.uppercase()
            }
            ?: symbol
    }

    /*
     * ============================================================
     * ساخت Observation
     * ============================================================
     */

    private fun createObservation(
        symbol: String,
        text: String,
        exchange: String,
        seen: LocalDateTime,
        percent: Double
    ): Observation {

        return Observation(

            exchange =
                exchange.ifBlank {
                    "نامشخص"
                },

            symbol = symbol,

            name =
                findName(
                    text,
                    symbol
                ),

            observedAt = seen,

            changePercent = percent
        )
    }

    /*
     * ============================================================
     * جفت‌کردن Symbol و درصد در یک خط
     * ============================================================
     */

    private fun pairSameLine(
        text: String,
        symbols: List<SymbolHit>,
        percentages: List<PercentHit>,
        exchange: String,
        seen: LocalDateTime
    ): List<Observation> {

        val result =
            mutableListOf<Observation>()

        val used =
            mutableSetOf<Int>()

        for (symbol in symbols) {

            var bestIndex = -1
            var bestDistance =
                Int.MAX_VALUE

            for (
                index in percentages.indices
            ) {

                if (index in used) {
                    continue
                }

                val percent =
                    percentages[index]

                if (
                    percent.line < 0 ||
                    symbol.line < 0
                ) {
                    continue
                }

                if (
                    percent.line !=
                    symbol.line
                ) {
                    continue
                }

                val distance =
                    abs(
                        percent.position -
                            symbol.position
                    )

                if (
                    distance <
                    bestDistance
                ) {

                    bestDistance =
                        distance

                    bestIndex =
                        index
                }
            }

            if (bestIndex >= 0) {

                used += bestIndex

                result +=
                    createObservation(
                        symbol =
                            symbol.symbol,
                        text = text,
                        exchange = exchange,
                        seen = seen,
                        percent =
                            percentages[
                                bestIndex
                            ].value
                    )
            }
        }

        return result
    }

    /*
     * ============================================================
     * جفت‌کردن ترتیبی
     * ============================================================
     */

    private fun pairSequentially(
        text: String,
        symbols: List<SymbolHit>,
        percentages: List<PercentHit>,
        exchange: String,
        seen: LocalDateTime
    ): List<Observation> {

        val result =
            mutableListOf<Observation>()

        val count =
            minOf(
                symbols.size,
                percentages.size
            )

        for (i in 0 until count) {

            result +=
                createObservation(
                    symbol =
                        symbols[i].symbol,
                    text = text,
                    exchange = exchange,
                    seen = seen,
                    percent =
                        percentages[i].value
                )
        }

        return result
    }

    /*
     * ============================================================
     * جفت‌کردن بر اساس نزدیک‌ترین موقعیت
     * ============================================================
     */

    private fun pairNearest(
        text: String,
        symbols: List<SymbolHit>,
        percentages: List<PercentHit>,
        exchange: String,
        seen: LocalDateTime
    ): List<Observation> {

        val result =
            mutableListOf<Observation>()

        val used =
            mutableSetOf<Int>()

        for (symbol in symbols) {

            var bestIndex = -1

            var bestDistance =
                Int.MAX_VALUE

            for (
                index in percentages.indices
            ) {

                if (index in used) {
                    continue
                }

                val distance =
                    abs(
                        symbol.position -
                            percentages[index]
                                .position
                    )

                if (
                    distance <
                    bestDistance
                ) {

                    bestDistance =
                        distance

                    bestIndex =
                        index
                }
            }

            if (bestIndex >= 0) {

                used += bestIndex

                result +=
                    createObservation(
                        symbol =
                            symbol.symbol,
                        text = text,
                        exchange = exchange,
                        seen = seen,
                        percent =
                            percentages[
                                bestIndex
                            ].value
                    )
            }
        }

        return result
    }

    /*
     * ============================================================
     * حذف رکوردهای تکراری
     * ============================================================
     */

    private fun removeDuplicateObservations(
        observations: List<Observation>
    ): List<Observation> {

        return observations
            .asReversed()
            .distinctBy {
                it.symbol
            }
            .asReversed()
    }

    /*
     * ============================================================
     * نقطه ورود اصلی Parser
     * ============================================================
     */

    fun parse(
        text: String,
        exchange: String,
        seen: LocalDateTime
    ): List<Observation> {

        if (text.isBlank()) {
            return emptyList()
        }

        /*
         * 1 ـ پیدا کردن ارزها
         */

        val symbols =
            findAllSymbols(text)

        if (symbols.isEmpty()) {
            return emptyList()
        }

        /*
         * 2 ـ پیدا کردن درصدها
         */

        val percentages =
            findAllPercents(text)

        if (percentages.isEmpty()) {
            return emptyList()
        }

        /*
         * 3 ـ اولویت:
         * ارز و درصد در یک خط
         */

        val sameLine =
            pairSameLine(
                text = text,
                symbols = symbols,
                percentages = percentages,
                exchange = exchange,
                seen = seen
            )

        if (
            sameLine.isNotEmpty()
        ) {

            return removeDuplicateObservations(
                sameLine
            )
        }

        /*
         * 4 ـ اگر تعداد برابر باشد:
         * جفت‌کردن ترتیبی
         */

        if (
            symbols.size ==
            percentages.size
        ) {

            return removeDuplicateObservations(
                pairSequentially(
                    text = text,
                    symbols = symbols,
                    percentages = percentages,
                    exchange = exchange,
                    seen = seen
                )
            )
        }

        /*
         * 5 ـ در غیر این صورت:
         * نزدیک‌ترین درصد
         */

        return removeDuplicateObservations(
            pairNearest(
                text = text,
                symbols = symbols,
                percentages = percentages,
                exchange = exchange,
                seen = seen
            )
        )
    }
}
