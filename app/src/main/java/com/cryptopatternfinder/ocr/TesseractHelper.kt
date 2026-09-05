package com.cryptopatternfinder.ocr

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Rect
import android.net.Uri
import com.googlecode.tesseract.android.TessBaseAPI
import java.io.File
import java.io.FileOutputStream
import java.util.Locale

object TesseractHelper {

    private const val TESS_DATA_FOLDER = "tessdata"

    private var tess: TessBaseAPI? = null

    data class OcrWord(
        val text: String,
        val rect: Rect,
        val confidence: Float,
        val centerX: Int,
        val centerY: Int
    )

    data class OcrResult(
        val text: String,
        val words: List<OcrWord>,
        val imageWidth: Int,
        val imageHeight: Int
    )

    fun init(context: Context) {

        if (tess != null) return

        val dataPath =
            File(
                context.filesDir,
                "tesseract"
            ).absolutePath

        val tessDataDir =
            File(
                dataPath,
                TESS_DATA_FOLDER
            )

        if (!tessDataDir.exists()) {
            tessDataDir.mkdirs()
        }

        /*
         * مدل‌های فارسی و انگلیسی
         */
        listOf(
            "eng.traineddata",
            "fas.traineddata"
        ).forEach { fileName ->

            val outFile =
                File(
                    tessDataDir,
                    fileName
                )

            if (!outFile.exists()) {

                context.assets
                    .open(
                        "$TESS_DATA_FOLDER/$fileName"
                    )
                    .use { input ->

                        FileOutputStream(
                            outFile
                        ).use { output ->

                            input.copyTo(output)
                        }
                    }
            }
        }

        tess =
            TessBaseAPI().apply {

                if (
                    !init(
                        dataPath,
                        "eng+fas"
                    )
                ) {

                    throw IllegalStateException(
                        "Tesseract init failed"
                    )
                }

                /*
                 * برای جدول‌های چندردیفه
                 */
                pageSegMode =
                    TessBaseAPI.PageSegMode.PSM_AUTO
            }
    }

    /**
     * OCR قدیمی برنامه.
     *
     * این تابع عمداً حفظ شده تا قسمت‌های
     * فعلی برنامه خراب نشوند.
     */
    fun recognize(
        context: Context,
        uri: Uri
    ): String {

        init(context)

        val bitmap =
            context.contentResolver
                .openInputStream(uri)
                ?.use {
                    BitmapFactory.decodeStream(it)
                }
                ?: return ""

        return try {

            tess?.setImage(bitmap)

            tess?.getUTF8Text()

        } finally {

            bitmap.recycle()
        }
    }

    /**
     * OCR پیشرفته:
     *
     * متن + مختصات + confidence
     *
     * این تابع برای تشخیص ردیف‌های
     * صرافی استفاده خواهد شد.
     */
    fun recognizeDetailed(
        context: Context,
        uri: Uri
    ): OcrResult {

        init(context)

        val bitmap =
            context.contentResolver
                .openInputStream(uri)
                ?.use {
                    BitmapFactory.decodeStream(it)
                }
                ?: return OcrResult(
                    text = "",
                    words = emptyList(),
                    imageWidth = 0,
                    imageHeight = 0
                )

        return try {

            val api = tess
                ?: return OcrResult(
                    text = "",
                    words = emptyList(),
                    imageWidth = bitmap.width,
                    imageHeight = bitmap.height
                )

            api.setImage(bitmap)

            /*
             * اول recognition را اجرا می‌کنیم.
             */
            val text =
                api.utF8Text ?: ""

            val words =
                mutableListOf<OcrWord>()

            /*
             * نتیجه Tesseract را
             * کلمه به کلمه می‌خوانیم.
             *
             * RIL_WORD = سطح کلمه
             */
            val iterator =
                api.resultIterator

            if (iterator != null) {

                try {

                    iterator.begin()

                    do {

                        val word =
                            iterator
                                .getUTF8Text(
                                    TessBaseAPI.PageIteratorLevel.RIL_WORD
                                )
                                ?.trim()
                                ?: ""

                        if (word.isNotBlank()) {

                            val confidence =
                                iterator.confidence(
                                    TessBaseAPI.PageIteratorLevel.RIL_WORD
                                )

                            val rect =
                                iterator.boundingBox(
                                    TessBaseAPI.PageIteratorLevel.RIL_WORD
                                )

                            if (
                                rect != null &&
                                rect.width() > 0 &&
                                rect.height() > 0
                            ) {

                                words +=
                                    OcrWord(
                                        text =
                                            normalizeWord(
                                                word
                                            ),

                                        rect = rect,

                                        confidence =
                                            confidence,

                                        centerX =
                                            rect.centerX(),

                                        centerY =
                                            rect.centerY()
                                    )
                            }
                        }

                    } while (
                        iterator.next(
                            TessBaseAPI.PageIteratorLevel.RIL_WORD
                        )
                    )

                } finally {

                    iterator.recycle()
                }
            }

            OcrResult(
                text = text,
                words = words,
                imageWidth = bitmap.width,
                imageHeight = bitmap.height
            )

        } finally {

            bitmap.recycle()
        }
    }

    /**
     * پاک‌سازی کوچک خروجی OCR
     */
    private fun normalizeWord(
        value: String
    ): String {

        return value
            .replace("\n", " ")
            .replace("\r", " ")
            .replace(
                Regex("\\s+"),
                " "
            )
            .trim()
    }

    /**
     * فقط کلمات با confidence مناسب
     */
    fun filterWordsByConfidence(
        result: OcrResult,
        minimumConfidence: Float = 25f
    ): List<OcrWord> {

        return result.words
            .filter {
                it.confidence >=
                    minimumConfidence
            }
    }

    /**
     * مرتب‌سازی تقریبی از بالا به پایین
     * و داخل هر ردیف از راست به چپ.
     *
     * برای جدول فارسی بسیار مهم است.
     */
    fun sortForPersianTable(
        words: List<OcrWord>
    ): List<OcrWord> {

        if (words.isEmpty()) {
            return emptyList()
        }

        val rows =
            mutableListOf<MutableList<OcrWord>>()

        val sorted =
            words.sortedBy {
                it.centerY
            }

        for (word in sorted) {

            val existingRow =
                rows.firstOrNull { row ->

                    val averageY =
                        row.map {
                            it.centerY
                        }.average()

                    kotlin.math.abs(
                        word.centerY -
                            averageY
                    ) <=
                        maxOf(
                            12,
                            word.rect.height() / 2
                        )
                }

            if (existingRow != null) {

                existingRow += word

            } else {

                rows +=
                    mutableListOf(word)
            }
        }

        return rows
            .sortedBy {
                it.map { word ->
                    word.centerY
                }.average()
            }
            .flatMap { row ->

                /*
                 * رابط کاربری فارسی است،
                 * بنابراین ستون‌های سمت راست
                 * اهمیت بیشتری دارند.
                 */
                row.sortedByDescending {
                    it.centerX
                }
            }
    }

    fun release() {

        tess?.recycle()
        tess = null
    }
}
