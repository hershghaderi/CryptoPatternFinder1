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

            // انواع
