package com.cryptopatternfinder.ocr
import com.cryptopatternfinder.core.Observation
import java.time.LocalDateTime

object OcrParser {
    private val symbol=Regex("\\b[A-Z]{2,12}\\b")
    private val number=Regex("([+-]?\\d+(?:[\\.,]\\d+)?)\\s*%?")
    fun parse(text:String,exchange:String,seen:LocalDateTime):List<Observation> =
        text.lines().mapNotNull { line ->
            val s=symbol.find(line)?.value ?: return@mapNotNull null
            val nums=number.findAll(line).mapNotNull {
                it.groupValues[1].replace(',','.').toDoubleOrNull()
            }.toList()
            val p=nums.lastOrNull() ?: return@mapNotNull null
            if(p < -100 || p > 1000) return@mapNotNull null
            Observation(exchange,s,seen,p)
        }.distinctBy { it.symbol }
}
