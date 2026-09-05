package com.cryptopatternfinder.ocr

import com.cryptopatternfinder.core.Observation
import java.time.LocalDateTime
import java.util.Locale
import kotlin.math.abs

object OcrParser {

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
        "COSMOSHUB" to "ATOM",
        "FANTOM" to "FTM",
        "FTM" to "FTM",
        "TEZOS" to "XTZ",
        "XTZ" to "XTZ",
        "EOS" to "EOS",
        "FLOW" to "FLOW",
        "FLOW" to "FLOW",
        "SAND" to "SAND",
        "MANA" to "MANA",
        "AXIEINFINITY" to "AXS",
        "AXS" to "AXS",
        "THEGRAPH" to "GRT",
        "GRT" to "GRT",
        "FILECOIN" to "FIL",
        "KAVA" to "KAVA",
        "KAVA" to "KAVA",
        "ALPHA" to "ALPHA",
        "ZILLIQA" to "ZIL",
        "ZIL" to "ZIL",
        "IOTA" to "IOTA",
        "IOTA" to "IOTA",
        "THETA" to "THETA",
        "THETA" to "THETA",
        "NEO" to "NEO",
        "NEO" to "NEO",
        "DASH" to "DASH",
        "DASH" to "DASH",
        "LUNC" to "LUNC",
        "TERRA" to "LUNA",
        "LUNA" to "LUNA",
        "ORDI" to "ORDI",
        "ORDI" to "ORDI",
        "STX" to "STX",
        "STACKS" to "STX",
        "MANTLE" to "MNT",
        "MNT" to "MNT",
        "APT" to "APT",
        "WIF" to "WIF",
        "BONK" to "BONK",
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

    private val percentRegex = Regex(
        """([+-]?\s*\d+(?:[.,]\d+)?)\s*[%٪]"""
    )

    private val percentOcrRegex = Regex(
        """([+-]?\s*[0-9OIl]{1,3}(?:[.,][0-9OIl]{1,3})?)\s*[Pp]"""
    )

    private val signedNumberRegex = Regex(
        """([+-])\s*([0-9OIl]{1,3}(?:[.,][0-9OIl]{1,3})?)"""
    )

    private val loosePercentRegex = Regex(
        """([+-]?\s*[0-9OIl]{1,3}(?:[.,][0-9OIl]{1,3})?)\s*(?:%|٪|P|p)"""
    )

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
            .replace(Regex("""\s+"""), " ")
            .trim()
    }

    private fun compact(text: String): String {
        return normalize(text).replace(" ", "")
    }

    private fun normalizeOcrNumber(value: String): String {
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

    private fun findAllSymbols(text: String): List<SymbolHit> {

        val normalizedFull = normalize(text)

        val hits = mutableListOf<SymbolHit>()

        val entries = knownSymbols.entries
            .sortedByDescending {
                it.key.replace(" ", "").length
            }

        var lineStart = 0

        normalizedFull
            .split("\n")
            .forEachIndexed { lineIndex, line ->

                val normalizedLine = compact(line)

                if (normalizedLine.isBlank()) {
                    lineStart += line.length + 1
                    return@forEachIndexed
                }

                for ((name, symbol) in entries) {

                    val key = name
                        .replace(" ", "")
                        .uppercase(Locale.US)

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
                            position = lineStart + position,
                            line = lineIndex
                        )

                        start = position + key.length
                    }
                }

                lineStart += line.length + 1
            }

        /*
         * بعضی OCRها کل متن را بدون newline صحیح برمی‌گردانند.
         * در این حالت یک بار دیگر روی کل متن جستجو می‌کنیم.
         */
        if (hits.isEmpty()) {

            val normalized = compact(text)

            for ((name, symbol) in entries) {

                val key = name
                    .replace(" ", "")
                    .uppercase(Locale.US)

                var start = 0

                while (true) {

                    val position =
                        normalized.indexOf(
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

                    start = position + key.length
                }
            }
        }

        return removeDuplicateSymbolHits(
            hits.sortedBy { it.position }
        )
    }

    private fun removeDuplicateSymbolHits(
        hits: List<SymbolHit>
    ): List<SymbolHit> {

        if (hits.isEmpty()) {
            return emptyList()
        }

        val result = mutableListOf<SymbolHit>()

        for (hit in hits) {

            val duplicate =
                result.any { previous ->

                    previous.symbol == hit.symbol &&
                        abs(
                            previous.position -
                                hit.position
                        ) <= 2
                }

            if (!duplicate) {
                result += hit
            }
        }

        return result
    }

    private fun findAllPercents(text: String): List<PercentHit> {

        val normalized = normalize(text)

        val result = mutableListOf<PercentHit>()

        /*
         * مرحله اول:
         * درصدهایی که علامت % واقعی دارند.
         */
        percentRegex
            .findAll(normalized)
            .forEach { match ->

                val raw =
                    match.groupValues[1]

                val value =
                    normalizeOcrNumber(raw)
                        .toDoubleOrNull()

                if (
                    value != null &&
                    value in -100.0..1000.0
                ) {

                    result += PercentHit(
                        value = value,
                        position = match.range.first,
                        line = findLineNumber(
                            normalized,
                            match.range.first
                        )
                    )
                }
            }

        /*
         * مرحله دوم:
         * OCR گاهی % را به حرف P تبدیل می‌کند.
         *
         * مثال:
         * -1P.AS7
         * -1P.9F7
         */
        percentOcrRegex
            .findAll(normalized)
            .forEach { match ->

                val raw =
                    match.groupValues[1]

                val value =
                    normalizeOcrNumber(raw)
                        .toDoubleOrNull()

                if (
                    value != null &&
                    value in -100.0..1000.0
                ) {

                    val position =
                        match.range.first

                    val alreadyExists =
                        result.any {
                            abs(
                                it.position -
                                    position
                            ) <= 3
                        }

                    if (!alreadyExists) {

                        result += PercentHit(
                            value = value,
                            position = position,
                            line = findLineNumber(
                                normalized,
                                position
                            )
                        )
                    }
                }
            }

        /*
         * مرحله سوم:
         * اگر هنوز چیزی پیدا نشد،
         * اعداد دارای + یا - را بررسی می‌کنیم.
         */
        if (result.isEmpty()) {

            signedNumberRegex
                .findAll(normalized)
                .forEach { match ->

                    val sign =
                        match.groupValues[1]

                    val number =
                        match.groupValues[2]

                    val value =
                        normalizeOcrNumber(
                            sign + number
                        ).toDoubleOrNull()

                    if (
                        value != null &&
                        value in -100.0..1000.0
                    ) {

                        result += PercentHit(
                            value = value,
                            position = match.range.first,
                            line = findLineNumber(
                                normalized,
                                match.range.first
                            )
                        )
                    }
                }
        }

        /*
         * مرحله چهارم:
         * تشخیص درصد با فاصله یا OCR ناقص.
         */
        if (result.isEmpty()) {

            loosePercentRegex
                .findAll(normalized)
                .forEach { match ->

                    val raw =
                        match.groupValues[1]

                    val value =
                        normalizeOcrNumber(raw)
                            .toDoubleOrNull()

                    if (
                        value != null &&
                        value in -100.0..1000.0
                    ) {

                        result += PercentHit(
                            value = value,
                            position = match.range.first,
                            line = findLineNumber(
                                normalized,
                                match.range.first
                            )
                        )
                    }
                }
        }

        return result
            .sortedBy { it.position }
            .distinctBy {
                Triple(
                    it.value,
                    it.position,
                    it.line
                )
            }
    }

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
                position.coerceAtMost(text.length)
            )
            .count {
                it == '\n'
            }
    }

    private fun findName(
        text: String,
        symbol: String
    ): String {

        val normalized = compact(text)

        val entry = knownSymbols.entries
            .sortedByDescending {
                it.key.replace(" ", "").length
            }
            .firstOrNull { entry ->

                entry.value == symbol &&
                    normalized.contains(
                        entry.key.replace(
                            " ",
                            ""
                        )
                    )
            }

        return entry?.key
            ?.lowercase(Locale.US)
            ?.replaceFirstChar {
                it.uppercase()
            }
            ?: symbol
    }

    private fun createObservation(
        symbol: String,
        text: String,
        exchange: String,
        seen: LocalDateTime,
        percent: Double
    ): Observation {

        return Observation(
            exchange = exchange.ifBlank {
                "نامشخص"
            },
            symbol = symbol,
            name = findName(
                text,
                symbol
            ),
            observedAt = seen,
            changePercent = percent
        )
    }

    private fun pairSameLine(
        text: String,
        symbols: List<SymbolHit>,
        percentages: List<PercentHit>,
        exchange: String,
        seen: LocalDateTime
    ): List<Observation> {

        val result = mutableListOf<Observation>()

        for (symbol in symbols) {

            val sameLine =
                percentages
                    .filter {
                        it.line >= 0 &&
                            symbol.line >= 0 &&
                            it.line == symbol.line
                    }
                    .minByOrNull {
                        abs(
                            it.position -
                                symbol.position
                        )
                    }

            if (sameLine != null) {

                result += createObservation(
                    symbol = symbol.symbol,
                    text = text,
                    exchange = exchange,
                    seen = seen,
                    percent = sameLine.value
                )
            }
        }

        return result
    }

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

            val symbol =
                symbols[i]

            val percent =
                percentages[i]

            result += createObservation(
                symbol = symbol.symbol,
                text = text,
                exchange = exchange,
                seen = seen,
                percent = percent.value
            )
        }

        return result
    }

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

                if (
                    index in used
                ) {
                    continue
                }

                val distance =
                    abs(
                        symbol.position -
                            percentages[index]
                                .position
                    )

                if (
                    distance < bestDistance
                ) {

                    bestDistance =
                        distance

                    bestIndex =
                        index
                }
            }

            if (bestIndex >= 0) {

                used += bestIndex

                val percent =
                    percentages[bestIndex]

                result += createObservation(
                    symbol = symbol.symbol,
                    text = text,
                    exchange = exchange,
                    seen = seen,
                    percent = percent.value
                )
            }
        }

        return result
    }

    private fun removeDuplicateObservations(
        observations: List<Observation>
    ): List<Observation> {

        /*
         * اگر یک ارز چند بار در OCR آمده باشد،
         * آخرین مورد معتبر آن نگه داشته می‌شود.
         */
        return observations
            .asReversed()
            .distinctBy {
                it.symbol
            }
            .asReversed()
    }

    fun parse(
        text: String,
        exchange: String,
        seen: LocalDateTime
    ): List<Observation> {

        if (text.isBlank()) {
            return emptyList()
        }

        /*
         * تمام نمادهای شناخته‌شده از OCR استخراج می‌شوند.
         */
        val symbols =
            findAllSymbols(text)

        if (symbols.isEmpty()) {
            return emptyList()
        }

        /*
         * تمام درصدها و اعداد دارای علامت
         * از OCR استخراج می‌شوند.
         */
        val percentages =
            findAllPercents(text)

        if (percentages.isEmpty()) {
            return emptyList()
        }

        /*
         * مرحله اول:
         * اگر ارز و درصد در یک خط باشند،
         * همان جفت را ترجیح می‌دهیم.
         */
        val sameLineResult =
            pairSameLine(
                text = text,
                symbols = symbols,
                percentages = percentages,
                exchange = exchange,
                seen = seen
            )

        if (
            sameLineResult.isNotEmpty() &&
            sameLineResult.size >=
                minOf(
                    symbols.size,
                    percentages.size
                )
        ) {

            return removeDuplicateObservations(
                sameLineResult
            )
        }

        /*
         * مرحله دوم:
         * اگر OCR ستون‌ها را جداگانه خوانده باشد،
         * ترتیب استخراج را استفاده می‌کنیم.
         *
         * این حالت برای اسکرین‌شات‌هایی مهم است
         * که ابتدا همه درصدها و سپس همه Symbolها
         * توسط ML Kit خوانده می‌شوند.
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
         * مرحله سوم:
         * اگر تعداد ارزها و درصدها برابر نباشد،
         * نزدیک‌ترین درصد به هر ارز انتخاب می‌شود.
         */
        val nearestResult =
            pairNearest(
                text = text,
                symbols = symbols,
                percentages = percentages,
                exchange = exchange,
                seen = seen
            )

        return removeDuplicateObservations(
            nearestResult
        )
    }
}
