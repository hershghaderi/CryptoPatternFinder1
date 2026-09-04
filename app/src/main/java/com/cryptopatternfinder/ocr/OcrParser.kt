package com.cryptopatternfinder.ocr

import com.cryptopatternfinder.core.Observation
import java.time.LocalDateTime
import java.util.Locale

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
        "EOS" to "EOS"
    )

    private val percentRegex = Regex(
        """([+-]?\d+(?:[.,]\d+)?)\s*[%٪]"""
    )

    private val signedNumberRegex = Regex(
        """([+-])\s*(\d{1,3}(?:[.,]\d{1,2})?)"""
    )

    private fun normalize(text: String): String {
        return text
            .uppercase(Locale.US)
            .replace('٪', '%')
            .replace('−', '-')
            .replace('–', '-')
            .replace('—', '-')
            .replace('﹣', '-')
            .replace('|', 'I')
            .replace(Regex("""\s+"""), " ")
            .trim()
    }

    private fun compact(text: String): String {
        return normalize(text).replace(" ", "")
    }

    private data class SymbolHit(
        val symbol: String,
        val position: Int
    )

    private data class PercentHit(
        val value: Double,
        val position: Int
    )

    private fun findAllSymbols(text: String): List<SymbolHit> {

        val normalized = compact(text)

        val hits = mutableListOf<SymbolHit>()

        val entries = knownSymbols.entries
            .sortedByDescending {
                it.key.replace(" ", "").length
            }

        for ((name, symbol) in entries) {

            val key = name.replace(" ", "")

            var start = 0

            while (true) {

                val position =
                    normalized.indexOf(key, start)

                if (position < 0) {
                    break
                }

                hits += SymbolHit(
                    symbol = symbol,
                    position = position
                )

                start = position + key.length
            }
        }

        /*
         * اگر یک Symbol چند بار توسط نام و Symbol
         * خودش پیدا شده، بر اساس موقعیت مرتب می‌کنیم.
         */
        return hits
            .sortedBy { it.position }
            .filterIndexed { index, hit ->

                if (index == 0) {
                    true
                } else {
                    val previous = hits
                        .sortedBy { it.position }[index - 1]

                    hit.position != previous.position ||
                        hit.symbol != previous.symbol
                }
            }
    }

    private fun findAllPercents(text: String): List<PercentHit> {

        val normalized = normalize(text)

        val result = mutableListOf<PercentHit>()

        /*
         * اول درصدهایی که % دارند.
         */
        percentRegex.findAll(normalized).forEach { match ->

            val value = match.groupValues[1]
                .replace(",", ".")
                .replace(" ", "")
                .toDoubleOrNull()

            if (
                value != null &&
                value in -100.0..1000.0
            ) {
                result += PercentHit(
                    value = value,
                    position = match.range.first
                )
            }
        }

        /*
         * اگر درصد صریح پیدا نشد،
         * اعداد علامت‌دار را بررسی می‌کنیم.
         */
        if (result.isEmpty()) {

            signedNumberRegex
                .findAll(normalized)
                .forEach { match ->

                    val sign = match.groupValues[1]
                    val number = match.groupValues[2]

                    val value =
                        (sign + number)
                            .replace(",", ".")
                            .toDoubleOrNull()

                    if (
                        value != null &&
                        value in -100.0..1000.0
                    ) {
                        result += PercentHit(
                            value = value,
                            position = match.range.first
                        )
                    }
                }
        }

        return result.sortedBy { it.position }
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
                        entry.key.replace(" ", "")
                    )
            }

        return entry?.key
            ?.lowercase(Locale.US)
            ?.replaceFirstChar {
                it.uppercase()
            }
            ?: symbol
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
         * همه ارزهای موجود در کل OCR
         * پیدا می‌شوند؛ دیگر محدود به یک خط نیستیم.
         */
        val symbols = findAllSymbols(text)

        if (symbols.isEmpty()) {
            return emptyList()
        }

        /*
         * همه درصدهای موجود در کل OCR
         * پیدا می‌شوند.
         */
        val percentages = findAllPercents(text)

        if (percentages.isEmpty()) {
            return emptyList()
        }

        val result = mutableListOf<Observation>()

        /*
         * اگر تعداد ارزها و درصدها برابر باشد،
         * بهترین حالت است:
         *
         * BTC
         * ETH
         * SOL
         *
         * +2.1%
         * -1.4%
         * +3.2%
         *
         * را به ترتیب به هم وصل می‌کنیم.
         */
        if (symbols.size == percentages.size) {

            for (i in symbols.indices) {

                val symbolHit = symbols[i]
                val percentHit = percentages[i]

                result += Observation(
                    exchange = exchange.ifBlank {
                        "نامشخص"
                    },
                    symbol = symbolHit.symbol,
                    name = symbolHit.symbol,
                    observedAt = seen,
                    changePercent = percentHit.value
                )
            }

        } else {

            /*
             * اگر تعدادشان برابر نبود،
             * برای هر ارز نزدیک‌ترین درصد استفاده می‌شود.
             */
            val usedPercentIndexes =
                mutableSetOf<Int>()

            for (symbolHit in symbols) {

                var bestIndex = -1
                var bestDistance = Int.MAX_VALUE

                for (
                    percentIndex in percentages.indices
                ) {

                    if (
                        percentIndex in
                        usedPercentIndexes
                    ) {
                        continue
                    }

                    val distance =
                        kotlin.math.abs(
                            symbolHit.position -
                                percentages[percentIndex].position
                        )

                    if (distance < bestDistance) {
                        bestDistance = distance
                        bestIndex = percentIndex
                    }
                }

                if (bestIndex >= 0) {

                    usedPercentIndexes += bestIndex

                    val percent =
                        percentages[bestIndex]

                    result += Observation(
                        exchange = exchange.ifBlank {
                            "نامشخص"
                        },
                        symbol = symbolHit.symbol,
                        name = symbolHit.symbol,
                        observedAt = seen,
                        changePercent = percent.value
                    )
                }
            }
        }

        /*
         * هر ارز فقط یک بار ذخیره شود.
         * اما ارزهای مختلف همگی حفظ می‌شوند.
         */
        return result
            .asReversed()
            .distinctBy { it.symbol }
            .asReversed()
    }
}
