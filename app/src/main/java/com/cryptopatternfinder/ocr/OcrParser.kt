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
     * فقط درصدهایی که علامت % یا ٪ دارند.
     *
     * مثال‌های معتبر:
     * +2.5%
     * -4.21%
     * 2,5%
     * +10 ٪
     */
    private val percentRegex = Regex(
        """([+-]?\d+(?:[.,]\d+)?)\s*[%٪]"""
    )

    /*
     * برای حالتی که OCR علامت درصد را حذف کرده باشد.
     *
     * عمداً فقط اعداد نسبتاً ساده را قبول می‌کنیم
     * و از قبول هر عدد تصادفی جلوگیری می‌کنیم.
     */
    private val fallbackSignedNumberRegex = Regex(
        """(?<![A-Z0-9])([+-])\s*(\d{1,3}(?:[.,]\d{1,2})?)(?![A-Z0-9])"""
    )

    private fun normalize(text: String): String {
        return text
            .uppercase(Locale.US)

            // درصد فارسی
            .replace('٪', '%')

            // انواع خط تیره OCR
            .replace('−', '-')
            .replace('–', '-')
            .replace('—', '-')
            .replace('﹣', '-')

            // چند خطای رایج OCR
            .replace('|', 'I')

            // حذف فاصله‌های اضافی
            .replace(Regex("""\s+"""), " ")
            .trim()
    }

    private fun compact(text: String): String {
        return normalize(text)
            .replace(" ", "")
    }

    /**
     * پیدا کردن نماد ارز
     */
    private fun findSymbol(text: String): String? {

        val normalized = compact(text)

        /*
         * اول نام‌ها و نمادهای طولانی‌تر بررسی می‌شوند
         * تا مثلاً ETHEREUM قبل از ETH پیدا شود.
         */
        val entries = knownSymbols.entries
            .sortedByDescending { it.key.replace(" ", "").length }

        for ((name, symbol) in entries) {

            val key = name.replace(" ", "")

            if (normalized.contains(key)) {
                return symbol
            }
        }

        /*
         * تعدادی خطای رایج OCR برای نمادهای مهم
         *
         * BТC / BTC
         * ЕТН / ETH
         * S0L / SOL
         */
        val ocrText = normalized
            .replace('0', 'O')
            .replace('1', 'I')

        val commonOcrSymbols = mapOf(
            "BTC" to "BTC",
            "BТC" to "BTC",
            "ETH" to "ETH",
            "SOL" to "SOL",
            "XRP" to "XRP",
            "BNB" to "BNB",
            "ADA" to "ADA",
            "DOGE" to "DOGE",
            "TON" to "TON",
            "TRX" to "TRX",
            "AVAX" to "AVAX",
            "SHIB" to "SHIB",
            "DOT" to "DOT",
            "LINK" to "LINK",
            "LTC" to "LTC",
            "BCH" to "BCH",
            "UNI" to "UNI",
            "ATOM" to "ATOM",
            "ETC" to "ETC",
            "XLM" to "XLM",
            "NEAR" to "NEAR",
            "APT" to "APT",
            "FIL" to "FIL",
            "ARB" to "ARB",
            "OP" to "OP",
            "SUI" to "SUI",
            "INJ" to "INJ",
            "AAVE" to "AAVE",
            "ALGO" to "ALGO",
            "VET" to "VET",
            "ICP" to "ICP",
            "HBAR" to "HBAR",
            "MKR" to "MKR",
            "PEPE" to "PEPE",
            "FLOKI" to "FLOKI",
            "BONK" to "BONK",
            "SEI" to "SEI",
            "TIA" to "TIA",
            "RUNE" to "RUNE",
            "EOS" to "EOS"
        )

        for ((key, symbol) in commonOcrSymbols) {
            if (ocrText.contains(key)) {
                return symbol
            }
        }

        return null
    }

    /**
     * پیدا کردن درصد تغییر
     */
    private fun findChangePercent(text: String): Double? {

        val normalized = normalize(text)

        /*
         * روش اول:
         * فقط عددی که واقعاً کنار % قرار گرفته.
         */
        val explicitPercent = percentRegex
            .findAll(normalized)
            .mapNotNull { match ->

                match.groupValues[1]
                    .replace(",", ".")
                    .replace(" ", "")
                    .toDoubleOrNull()
            }
            .lastOrNull { value ->
                value in -100.0..1000.0
            }

        if (explicitPercent != null) {
            return explicitPercent
        }

        /*
         * روش دوم:
         * اگر OCR علامت % را حذف کرده باشد.
         *
         * فقط اعداد دارای + یا - را بررسی می‌کنیم.
         * اعداد بدون علامت اصلاً قبول نمی‌شوند.
         */
        val fallback = fallbackSignedNumberRegex
            .findAll(normalized)
            .mapNotNull { match ->

                val sign = match.groupValues[1]
                val number = match.groupValues[2]

                val value =
                    (sign + number)
                        .replace(",", ".")
                        .toDoubleOrNull()

                value
            }
            .lastOrNull { value ->
                value in -100.0..1000.0
            }

        return fallback
    }

    /**
     * نام قابل نمایش ارز
     */
    private fun findName(
        line: String,
        symbol: String
    ): String {

        val normalized = compact(line)

        val entry = knownSymbols.entries
            .sortedByDescending {
                it.key.replace(" ", "").length
            }
            .firstOrNull {

                it.value == symbol &&
                    normalized.contains(
                        it.key.replace(" ", "")
                    )
            }

        if (entry != null) {

            return entry.key
                .lowercase(Locale.US)
                .replaceFirstChar {
                    it.uppercase()
                }
        }

        return symbol
    }

    /**
     * تبدیل متن OCR به Observation
     */
    fun parse(
        text: String,
        exchange: String,
        seen: LocalDateTime
    ): List<Observation> {

        val result = mutableListOf<Observation>()

        /*
         * متن را خط‌به‌خط تمیز می‌کنیم.
         */
        val lines = text
            .lines()
            .map { normalize(it) }
            .filter { it.isNotBlank() }

        for (index in lines.indices) {

            val currentLine = lines[index]

            val symbol =
                findSymbol(currentLine)
                    ?: continue

            /*
             * اول همان خط را بررسی می‌کنیم.
             */
            var change =
                findChangePercent(currentLine)

            /*
             * اگر درصد در همان خط نبود،
             * دو خط بعدی را هم بررسی می‌کنیم.
             */
            if (change == null) {

                for (offset in 1..2) {

                    val nextLine =
                        lines.getOrNull(index + offset)
                            ?: continue

                    /*
                     * اگر خط بعدی خودش یک ارز دیگر باشد،
                     * دیگر به آن خط برای درصد این ارز اعتماد نمی‌کنیم.
                     */
                    val anotherSymbol =
                        findSymbol(nextLine)

                    if (
                        anotherSymbol != null &&
                        anotherSymbol != symbol
                    ) {
                        break
                    }

                    change =
                        findChangePercent(nextLine)

                    if (change != null) {
                        break
                    }
                }
            }

            /*
             * بدون درصد معتبر، رکورد ذخیره نمی‌کنیم.
             */
            if (change == null) {
                continue
            }

            result += Observation(
                exchange = exchange.ifBlank {
                    "نامشخص"
                },
                symbol = symbol,
                name = findName(
                    currentLine,
                    symbol
                ),
                observedAt = seen,
                changePercent = change
            )
        }

        /*
         * اگر یک نماد چند بار در OCR دیده شد،
         * فقط آخرین نتیجه آن نماد را نگه می‌داریم.
         */
        return result
            .asReversed()
            .distinctBy { it.symbol }
            .asReversed()
    }
}
