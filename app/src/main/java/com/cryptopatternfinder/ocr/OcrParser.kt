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

    private val percentRegex =
        Regex("""([+-]?\d+(?:[.,]\d+)?)\s*[%٪]""")

    private val signedNumberRegex =
        Regex("""([+-]\s*\d+(?:[.,]\d+)?)""")

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

        for ((name, symbol) in
            knownSymbols.entries.sortedByDescending { it.key.length }) {

            if (normalized.contains(name)) {
                return symbol
            }
        }

        return null
    }

    private fun findChangePercent(text: String): Double? {

        val normalized = normalize(text)

        val percent =
            percentRegex
                .findAll(normalized)
                .mapNotNull {
                    it.groupValues[1]
                        .replace(",", ".")
                        .replace(" ", "")
                        .toDoubleOrNull()
                }
                .lastOrNull {
                    it in -100.0..1000.0
                }

        if (percent != null) {
            return percent
        }

        return signedNumberRegex
            .findAll(normalized)
            .mapNotNull {
                it.groupValues[1]
                    .replace(",", ".")
                    .replace(" ", "")
                    .toDoubleOrNull()
            }
            .lastOrNull {
                it in -100.0..1000.0
            }
    }

    private fun findName(
        line: String,
        symbol: String
    ): String {

        val entry =
            knownSymbols.entries.firstOrNull {
                it.value == symbol &&
                    normalize(line).contains(it.key)
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

        val result =
            mutableListOf<Observation>()

        val lines =
            text.lines()
                .map { normalize(it) }
                .filter { it.isNotBlank() }

        for (index in lines.indices) {

            val line = lines[index]

            val symbol =
                findSymbol(line)

            if (symbol == null) {
                continue
            }

            var change =
                findChangePercent(line)

            if (change == null) {

                for (offset in 1..2) {

                    val next =
                        lines.getOrNull(index + offset)

                    if (next != null) {

                        change =
                            findChangePercent(next)

                        if (change != null) {
                            break
                        }
                    }
                }
            }

            if (change == null) {
                continue
            }

            result += Observation(
                exchange = exchange.ifBlank { "نامشخص" },
                symbol = symbol,
                name = findName(line, symbol),
                observedAt = seen,
                changePercent = change
            )
        }

        return result
            .asReversed()
            .distinctBy { it.symbol }
            .asReversed()
    }
}
