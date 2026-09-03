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

        "NEAR" to "NEAR",
        "NEARPROTOCOL" to "NEAR",

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

    private val tokenRegex =
        Regex("""[A-Z][A-Z0-9]{1,11}""")

    private val percentRegex =
        Regex("""([+-]?\d+(?:[.,]\d+)?)\s*%""")

    private val numberRegex =
        Regex("""([+-]?\d+(?:[.,]\d+)?)""")

    private fun normalize(text: String): String {
        return text
            .uppercase(Locale.US)
            .replace('٪', '%')
            .replace('−', '-')
            .replace('–', '-')
            .replace('—', '-')
            .replace('|', 'I')
    }

    private fun findSymbol(line: String): String? {
        val normalized = normalize(line)

        // ابتدا نام کامل ارزها
        for ((name, symbol) in knownSymbols) {
            if (
                Regex("""(?<![A-Z0-9])$name(?![A-Z0-9])""")
                    .containsMatchIn(normalized)
            ) {
                return symbol
            }
        }

        // سپس نمادهای شناخته‌شده
        for (symbol in knownSymbols.values.distinct()) {
            if (
                Regex("""(?<![A-Z0-9])$symbol(?![A-Z0-9])""")
                    .containsMatchIn(normalized)
            ) {
                return symbol
            }
        }

        return null
    }

    private fun findChangePercent(line: String): Double? {

        val percentages = percentRegex
            .findAll(line)
            .mapNotNull {
                it.groupValues[1]
                    .replace(",", ".")
                    .toDoubleOrNull()
            }
            .filter { it in -100.0..1000.0 }
            .toList()

        if (percentages.isNotEmpty()) {
            return percentages.last()
        }

        // اگر OCR علامت % را حذف کرده باشد
        val numbers = numberRegex
            .findAll(line)
            .mapNotNull {
                it.groupValues[1]
                    .replace(",", ".")
                    .toDoubleOrNull()
            }
            .filter { it in -100.0..1000.0 }
            .toList()

        return numbers.lastOrNull()
    }

    private fun findName(line: String, symbol: String): String {
        val normalized = normalize(line)

        for ((name, mappedSymbol) in knownSymbols) {
            if (
                mappedSymbol == symbol &&
                Regex("""(?<![A-Z0-9])$name(?![A-Z0-9])""")
                    .containsMatchIn(normalized)
            ) {
                return name
                    .lowercase()
                    .replaceFirstChar { it.uppercase() }
            }
        }

        return symbol
    }

    fun parse(
        text: String,
        exchange: String,
        seen: LocalDateTime
    ): List<Observation> {

        val result = mutableListOf<Observation>()

        for (rawLine in text.lines()) {

            val line = normalize(rawLine).trim()

            if (line.isBlank()) continue

            val symbol = findSymbol(line) ?: continue

            val change = findChangePercent(line) ?: continue

            if (change !in -100.0..1000.0) continue

            result += Observation(
                exchange = exchange.ifBlank { "نامشخص" },
                symbol = symbol,
                name = findName(line, symbol),
                observedAt = seen,
                changePercent = change
            )
        }

        // اگر یک ارز در یک اسکرین‌شات چند بار OCR شد،
        // فقط آخرین رکورد همان ارز نگه داشته می‌شود.
        return result
            .distinctBy { it.symbol }
    }
}
