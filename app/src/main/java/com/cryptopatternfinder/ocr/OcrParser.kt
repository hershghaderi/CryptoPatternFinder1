package com.cryptopatternfinder.ocr

import com.cryptopatternfinder.core.Observation
import java.time.LocalDateTime
import java.util.Locale

object OcrParser {

    /*
     * نمادهای رایج ارزها.
     * OCR ممکن است بعضی نمادها را با فاصله یا حروف کوچک برگرداند،
     * بنابراین قبل از تشخیص متن نرمال می‌شود.
     */
    private val knownSymbols = setOf(
        "BTC", "ETH", "USDT", "BNB", "SOL", "XRP",
        "ADA", "DOGE", "TON", "TRX", "AVAX", "SHIB",
        "DOT", "LINK", "MATIC", "POL", "LTC", "BCH",
        "UNI", "ATOM", "ETC", "XLM", "NEAR", "APT",
        "FIL", "ARB", "OP", "SUI", "INJ", "AAVE",
        "ALGO", "VET", "ICP", "HBAR", "MKR", "PEPE",
        "FLOKI", "BONK", "SEI", "TIA", "RUNE", "EOS"
    )

    private val symbolRegex =
        Regex("""(?<![A-Z0-9])([A-Z]{2,12})(?![A-Z0-9])""")

    private val percentRegex =
        Regex("""([+-]?\d+(?:[.,]\d+)?)\s*%""")

    private val decimalRegex =
        Regex("""([+-]?\d+(?:[.,]\d+)?)""")

    /*
     * بعض OCRها علامت درصد را حذف می‌کنند.
     * این تابع اعداد یک خط را پیدا می‌کند و آخرین عدد مناسب
     * را به عنوان درصد تغییر در نظر می‌گیرد.
     */
    private fun findChangePercent(line: String): Double? {

        val percentMatches = percentRegex.findAll(line)
            .mapNotNull {
                it.groupValues[1]
                    .replace(",", ".")
                    .toDoubleOrNull()
            }
            .toList()

        if (percentMatches.isNotEmpty()) {
            return percentMatches.lastOrNull {
                it >= -100.0 && it <= 1000.0
            }
        }

        val numbers = decimalRegex.findAll(line)
            .mapNotNull {
                it.groupValues[1]
                    .replace(",", ".")
                    .toDoubleOrNull()
            }
            .filter {
                it >= -100.0 && it <= 1000.0
            }
            .toList()

        return numbers.lastOrNull()
    }

    private fun normalize(text: String): String {
        return text
            .uppercase(Locale.US)
            .replace('٪', '%')
            .replace('−', '-')
            .replace('–', '-')
            .replace('—', '-')
            .replace("O", "0")
    }

    private fun findSymbol(line: String): String? {

        val normalized = normalize(line)

        /*
         * ابتدا نمادهای شناخته‌شده را بررسی می‌کنیم.
         * این کار احتمال اشتباه OCR را کمتر می‌کند.
         */
        knownSymbols.forEach { symbol ->
            if (
                Regex("""(?<![A-Z0-9])$symbol(?![A-Z0-9])""")
                    .containsMatchIn(normalized)
            ) {
                return symbol
            }
        }

        /*
         * اگر نماد در لیست بالا نبود، یک نماد عمومی ۲ تا ۱۲ حرفی
         * هم قبول می‌کنیم.
         */
        return symbolRegex
            .find(normalized)
            ?.groupValues
            ?.getOrNull(1)
            ?.takeIf {
                it.length in 2..12 &&
                !it.all { ch -> ch.isDigit() }
            }
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

            if (change < -100.0 || change > 1000.0) {
                continue
            }

            result.add(
                Observation(
                    exchange = exchange.ifBlank { "نامشخص" },
                    symbol = symbol,
                    observedAt = seen,
                    changePercent = change
                )
            )
        }

        /*
         * اگر یک ارز در چند خط تکرار شده باشد،
         * آخرین تشخیص نگه داشته می‌شود.
         */
        return result
            .distinctBy { it.symbol }
    }
}
