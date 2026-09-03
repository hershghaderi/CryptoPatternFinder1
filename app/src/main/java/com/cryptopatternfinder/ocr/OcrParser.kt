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

    /*
     * فقط نمادهای واقعی را قبول می‌کنیم.
     * این باعث می‌شود چیزهایی مثل USD، 24H، PRICE و متن‌های تصادفی
     * به عنوان ارز ذخیره نشوند.
     */
    private val validSymbols = knownSymbols.values
        .distinct()
        .toSet()

    private val percentRegex = Regex(
        """([+-]?\d+(?:[.,]\d+)?)\s*[%٪]"""
    )

    /*
     * حالت‌هایی که OCR علامت درصد را حذف کرده است.
     * فقط اعداد دارای علامت + یا - را بررسی می‌کنیم.
     */
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
            .replace('O', '0')
            .replace(Regex("""\s+"""), " ")
            .trim()
    }

    private fun findSymbol(line: String): String? {
        val normalized = normalize(line)

        // اول نام کامل ارزها؛ نام‌های طولانی‌تر اول بررسی شوند.
        val entries = knownSymbols.entries.sortedByDescending { it.key.length }

        for ((name, symbol) in entries) {
            val escaped = Regex.escape(name)

            if (
                Regex("""(?<![A-Z0-9])$escaped(?![A-Z0-9])""")
                    .containsMatchIn(normalized)
            ) {
                return symbol
            }
        }

        // سپس Symbolهای شناخته‌شده
        for (symbol in validSymbols.sortedByDescending { it.length }) {
            val escaped = Regex.escape(symbol)

            if (
                Regex("""(?<![A-Z0-9])$escaped(?![A-Z0-9])""")
                    .containsMatchIn(normalized)
            ) {
                return symbol
            }
        }

        return null
    }

    private fun findChangePercent(line: String): Double? {
        val normalized = normalize(line)

        // حالت مطمئن: عدد همراه با %
        val percentages = percentRegex
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

        /*
         * اگر OCR علامت % را حذف کرده باشد، فقط عدد علامت‌دار را قبول کن.
         *
         * بنابراین:
         * BTC  +2.35   -> قبول
         * BTC  -1.72   -> قبول
         * BTC  104532  -> رد
         * BTC  0.00012 -> رد
         */
        val signed = signedNumberRegex
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

    private fun findName(line: String, symbol: String): String {
        val normalized = normalize(line)

        val entry = knownSymbols.entries
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

        for (rawLine in text.lines()) {

            val line = normalize(rawLine)

            if (line.isBlank()) continue

            val symbol = findSymbol(line) ?: continue

            val change = findChangePercent(line) ?: continue

            /*
             * تغییرات غیرمنطقی حذف شوند.
             */
            if (change !in -100.0..1000.0) continue

            result += Observation(
                exchange = exchange.ifBlank { "نامشخص" },
                symbol = symbol,
                name = findName(line, symbol),
                observedAt = seen,
                changePercent = change
            )
        }

        /*
         * اگر OCR یک ارز را چند بار تشخیص داد،
         * آخرین رکورد همان Symbol نگه داشته می‌شود.
         */
        return result
            .asReversed()
            .distinctBy { it.symbol }
            .asReversed()
    }
}
