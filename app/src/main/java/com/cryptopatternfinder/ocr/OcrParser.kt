package com.cryptopatternfinder.ocr

import com.cryptopatternfinder.core.Observation
import java.time.LocalDateTime
import java.util.Locale
import kotlin.math.abs

object OcrParser {

    /*
     * نام ارزها و Symbolها.
     *
     * علاوه بر ارزهای قبلی، مواردی که در اسکرین‌شات
     * واقعی Nobitex دیده شدند نیز اضافه شده‌اند.
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
        "ETHENA" to "ENA",

        /*
         * ارزهای دیده‌شده در اسکرین‌شات اخیر
         */
        "GAIA" to "GAIA",
        "XMN" to "XMN",
        "ELF" to "ELF",
        "GRVT" to "GRVT",
        "HNT" to "HNT",
        "HELIUM" to "HNT",
        "SCRT" to "SCRT",
        "SECRET" to "SCRT",
        "POP" to "POP",
        "GIGA" to "GIGA",
        "WIF" to "WIF",
        "ZYPHER" to "ZYPHER",
        "ZYPHERNETWORK" to "ZYPHER"
    )

    /*
     * تبدیل اعداد فارسی و عربی به انگلیسی.
     */
    private fun normalizeDigits(text: String): String {

        return buildString(text.length) {

            for (char in text) {

                when (char) {

                    '۰' -> append('0')
                    '۱' -> append('1')
                    '۲' -> append('2')
                    '۳' -> append('3')
                    '۴' -> append('4')
                    '۵' -> append('5')
                    '۶' -> append('6')
                    '۷' -> append('7')
                    '۸' -> append('8')
                    '۹' -> append('9')

                    '٠' -> append('0')
                    '١' -> append('1')
                    '٢' -> append('2')
                    '٣' -> append('3')
                    '٤' -> append('4')
                    '٥' -> append('5')
                    '٦' -> append('6')
                    '٧' -> append('7')
                    '٨' -> append('8')
                    '٩' -> append('9')

                    else -> append(char)
                }
            }
        }
    }

    private fun normalize(text: String): String {

        return normalizeDigits(text)
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
            .replace('\u200C', ' ')
            .replace('\u200F', ' ')
            .replace('\u200E', ' ')
            .replace(Regex("""[ \t]+"""), " ")
            .trim()
    }

    private fun compact(text: String): String {

        return normalize(text)
            .replace(" ", "")
    }

    /*
     * اصلاح خطاهای رایج Tesseract در اعداد.
     */
    private fun normalizeOcrNumber(value: String): String {

        return normalizeDigits(value)
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

    /*
     * درصد واقعی.
     *
     * نمونه:
     * +2.5%
     * -1.23%
     * ۲.۵٪
     */
    private val percentRegex = Regex(
        """([+-]?\s*[0-9OIl۰-۹٠-٩]{1,4}(?:[.,][0-9OIl۰-۹٠-٩]{1,4})?)\s*[%٪]"""
    )

    /*
     * OCR گاهی % را P می‌خواند.
     */
    private val percentOcrRegex = Regex(
        """([+-]?\s*[0-9OIl۰-۹٠-٩]{1,4}(?:[.,][0-9OIl۰-۹٠-٩]{1,4})?)\s*[Pp]"""
    )

    /*
     * عدد دارای + یا -
     */
    private val signedNumberRegex = Regex(
        """([+-])\s*([0-9OIl۰-۹٠-٩]{1,4}(?:[.,][0-9OIl۰-۹٠-٩]{1,4})?)"""
    )

    /*
     * درصد ناقص.
     */
    private val loosePercentRegex = Regex(
        """([+-]?\s*[0-9OIl۰-۹٠-٩]{1,4}(?:[.,][0-9OIl۰-۹٠-٩]{1,4})?)\s*(?:%|٪|P|p)"""
    )

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
            .count { it == '\n' }
    }

    /*
     * پیدا کردن تمام Symbolها.
     */
    private fun findAllSymbols(
        text: String
    ): List<SymbolHit> {

        val normalized = normalize(text)

        val hits = mutableListOf<SymbolHit>()

        val entries =
            knownSymbols.entries
                .sortedByDescending {
                    it.key
                        .replace(" ", "")
                        .length
                }

        val lines =
            normalized.split('\n')

        var lineStart = 0

        lines.forEachIndexed { lineIndex, line ->

            val compactLine =
                compact(line)

            if (compactLine.isNotBlank()) {

                for ((name, symbol) in entries) {

                    val key =
                        name
                            .replace(" ", "")
                            .uppercase(Locale.US)

                    var start = 0

                    while (true) {

                        val position =
                            compactLine.indexOf(
                                key,
                                start
                            )

                        if (position < 0) {
                            break
                        }

                        hits += SymbolHit(
                            symbol = symbol,
                            position =
                                lineStart + position,
                            line = lineIndex
                        )

                        start =
                            position + key.length
                    }
                }
            }

            lineStart += line.length + 1
        }

        /*
         * اگر newline خراب شده باشد،
         * کل متن نیز بررسی می‌شود.
         */
        if (hits.isEmpty()) {

            val all =
                compact(text)

            for ((name, symbol) in entries) {

                val key =
                    name
                        .replace(" ", "")
                        .uppercase(Locale.US)

                var start = 0

                while (true) {

                    val position =
                        all.indexOf(
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
            hits.sortedBy { it.position }
        )
    }

    private fun removeDuplicateSymbolHits(
        hits: List<SymbolHit>
    ): List<SymbolHit> {

        val result =
            mutableListOf<SymbolHit>()

        for (hit in hits) {

            val duplicate =
                result.any {

                    it.symbol == hit.symbol &&
                        abs(
                            it.position -
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
     * استخراج درصدها.
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
         * 2 ـ درصدی که % به P تبدیل شده.
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

                    if (
                        result.none {
                            abs(
                                it.position -
                                    position
                            ) <= 3
                        }
                    ) {

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
         * 3 ـ + و -
         */
        if (result.isEmpty()) {

            signedNumberRegex
                .findAll(normalized)
                .forEach { match ->

                    val raw =
                        match.groupValues[1] +
                            match.groupValues[2]

                    val value =
                        normalizeOcrNumber(
                            raw
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
         * 4 ـ حالت ناقص OCR
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
            .sortedBy { it.position }
            .distinctBy {
                Triple(
                    it.value,
                    it.position,
                    it.line
                )
            }
    }

    /*
     * تشخیص نام قابل نمایش ارز.
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
                .firstOrNull {

                    it.value == symbol &&
                        normalized.contains(
                            it.key.replace(
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
     * جفت‌کردن Symbol و درصد در یک خط.
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

            val candidates =
                percentages
                    .mapIndexed { index, percent ->
                        index to percent
                    }
                    .filter { (_, percent) ->

                        percent.line >= 0 &&
                            symbol.line >= 0 &&
                            percent.line ==
                                symbol.line &&
                            percentIndexNotUsed(
                                itIndex = 0,
                                used = used
                            )
                    }

            var bestIndex = -1
            var bestDistance =
                Int.MAX_VALUE

            for ((index, percent) in candidates) {

                if (index in used) {
                    continue
                }

                val distance =
                    abs(
                        symbol.position -
                            percent.position
                    )

                if (distance < bestDistance) {

                    bestDistance =
                        distance

                    bestIndex =
                        index
                }
            }

            if (bestIndex >= 0) {

                used += bestIndex

                result += createObservation(
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
     * فقط برای خوانایی pairSameLine.
     */
    private fun percentIndexNotUsed(
        itIndex: Int,
        used: Set<Int>
    ): Boolean {
        return itIndex !in used
    }

    /*
     * وقتی تعداد Symbol و درصد برابر است،
     * ترتیب آنها حفظ می‌شود.
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

            result += createObservation(
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
     * اگر تعدادها برابر نباشد،
     * نزدیک‌ترین درصد به Symbol انتخاب می‌شود.
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

            for (index in percentages.indices) {

                if (index in used) {
                    continue
                }

                val distance =
                    abs(
                        symbol.position -
                            percentages[index]
                                .position
                    )

                /*
                 * خیلی دور از هم نباشند.
                 */
                if (
                    distance < bestDistance &&
                    distance <= 100
                ) {

                    bestDistance =
                        distance

                    bestIndex =
                        index
                }
            }

            if (bestIndex >= 0) {

                used += bestIndex

                result += createObservation(
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

    private fun removeDuplicateObservations(
        observations: List<Observation>
    ): List<Observation> {

        /*
         * فقط یک رکورد برای هر Symbol در همان تصویر.
         */
        return observations
            .asReversed()
            .distinctBy {
                it.symbol
            }
            .asReversed()
    }

    /*
     * ورودی اصلی OCR.
     */
    fun parse(
        text: String,
        exchange: String,
        seen: LocalDateTime
    ): List<Observation> {

        if (text.isBlank()) {
            return emptyList()
        }

        val symbols =
            findAllSymbols(text)

        if (symbols.isEmpty()) {
            return emptyList()
        }

        val percentages =
            findAllPercents(text)

        if (percentages.isEmpty()) {
            return emptyList()
        }

        /*
         * اول جفت‌های داخل یک خط.
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
            sameLine.isNotEmpty() &&
            sameLine.size >=
                minOf(
                    symbols.size,
                    percentages.size
                )
        ) {

            return removeDuplicateObservations(
                sameLine
            )
        }

        /*
         * اگر تعدادها برابر باشند،
         * ترتیب OCR را حفظ می‌کنیم.
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
         * در غیر این صورت نزدیک‌ترین جفت.
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
