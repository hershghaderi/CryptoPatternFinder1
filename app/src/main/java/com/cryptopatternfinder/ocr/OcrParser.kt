package com.cryptopatternfinder.ocr

import com.cryptopatternfinder.core.Observation
import java.time.LocalDateTime
import java.util.Locale

object OcrParser {

    private val knownSymbols = mapOf(
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

    private val validSymbols =
        knownSymbols.values.distinct().toSet()

    private val percentRegex = Regex(
        """([+-]?\d+(?:[.,]\d+)?)\s*[%٪]"""
    )

    private val signedNumberRegex = Regex(
        """([+-]\s*\d+(?:[.,]\d+)?)"""
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

    private fun findSymbol(text: String): String? {

        val normalized = normalize(text)

        val entries =
            knownSymbols.entries.sortedByDescending { it.key.length }

        for ((name, symbol) in entries) {

            val pattern =
                Regex(
                    """(?<![A-Z0-9])${Regex.escape(name)}(?![A-Z0-9])"""
                )

            if (pattern.containsMatchIn(normalized)) {
                return symbol
            }
        }

        for (symbol in validSymbols.sortedByDescending { it.length }) {

            val pattern =
                Regex(
                    """(?<![A-Z0-9])${Regex.escape(symbol)}(?![A-Z0-9])"""
                )

            if (pattern.containsMatchIn(normalized)) {
                return symbol
            }
        }

        return null
    }

    private fun findChangePercent(text: String): Double? {

        val normalized = normalize(text)

        val percentages =
            percentRegex
                .findAll(normalized)
                .mapNotNull { match ->
                    match.groupValues[1]
                        .replace(",", ".")
                        .replace(" ", "")
                        .toDoubleOrNull()
                }
                .filter { it in -100.0..1000.0 }
                .toList()

        if (percentages.isNotEmpty()) {
            return percentages.last()
        }

        val signed =
            signedNumberRegex
                .findAll(normalized)
                .mapNotNull { match ->
                    match.groupValues[1]
                        .replace(" ", "")
                        .replace(",", ".")
                        .toDoubleOrNull()
                }
                .filter { it in -100.0..1000.0 }
                .toList()

        return signed.lastOrNull()
    }

    private fun findName(
        line: String,
        symbol: String
    ): String {

        val normalized = normalize(line)

        val entry =
            knownSymbols.entries
                .sortedByDescending { it.key.length }
                .firstOrNull { (name, mappedSymbol) ->
                    mappedSymbol == symbol &&
                        Regex(
                            """(?<![A-Z0-9])${Regex.escape(name)}(?![A-Z0-9])"""
                        ).containsMatchIn(normalized)
                }

        return entry?.key
            ?.lowercase(Locale.US)
            ?.replaceFirstChar { it.uppercase() }
            ?: symbol
    }

    fun parse(
        text: String,
        exchange: String,
        seen: LocalDateTime
    ): List<Observation> {

        val result = mutableListOf<Observation>()

        val lines =
            text.lines()
                .map { normalize(it) }
                .filter { it.isNotBlank() }

        for (index in lines.indices) {

            val currentLine = lines[index]

            val currentSymbol =
                findSymbol(currentLine)

            val currentChange =
                findChangePercent(currentLine)

            if (
                currentSymbol != null &&
                currentChange != null &&
                currentChange in -100.0..1000.0
            ) {

                result += Observation(
                    exchange = exchange.ifBlank { "نامشخص" },
                    symbol = currentSymbol,
                    name = findName(currentLine, currentSymbol),
                    observedAt = seen,
                    changePercent = currentChange
                )

                continue
            }

            if (currentSymbol != null) {

                val nextLine =
                    lines.getOrNull(index + 1)

                if (nextLine != null) {

                    val nextChange =
                        findChangePercent(nextLine)

                    if (
                        nextChange != null &&
                        nextChange in -100.0..1000.0
                    ) {

                        result += Observation(
                            exchange = exchange.ifBlank { "نامشخص" },
                            symbol = currentSymbol,
                            name = findName(
                                "$currentLine $nextLine",
                                currentSymbol
                            ),
                            observedAt = seen,
                            changePercent = nextChange
                        )
                    }
                }
            }
        }

        return result
            .asReversed()
            .distinctBy { it.symbol }
            .asReversed()
    }
}
