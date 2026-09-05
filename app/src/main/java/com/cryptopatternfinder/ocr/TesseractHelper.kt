package com.cryptopatternfinder.ocr

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.googlecode.tesseract.android.TessBaseAPI
import java.io.File
import java.io.FileOutputStream

object TesseractHelper {

    private const val TESS_DATA_FOLDER = "tessdata"
    private var tess: TessBaseAPI? = null

    fun init(context: Context) {
        if (tess != null) return

        val dataPath = File(context.filesDir, "tesseract").absolutePath
        val tessDataDir = File(dataPath, TESS_DATA_FOLDER)

        if (!tessDataDir.exists()) {
            tessDataDir.mkdirs()
        }

        // کپی فایل‌های زبان
        listOf("eng.traineddata", "fas.traineddata").forEach { fileName ->
            val outFile = File(tessDataDir, fileName)
            if (!outFile.exists()) {
                context.assets.open("$TESS_DATA_FOLDER/$fileName").use { input ->
                    FileOutputStream(outFile).use { output ->
                        input.copyTo(output)
                    }
                }
            }
        }

        tess = TessBaseAPI().apply {
            // ترکیب انگلیسی + فارسی
            if (!init(dataPath, "eng+fas")) {
                throw IllegalStateException("Tesseract init failed")
            }

            pageSegMode = TessBaseAPI.PageSegMode.PSM_AUTO
        }
    }

    fun recognize(context: Context, uri: Uri): String {
        init(context)

        val bitmap = context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it)
        } ?: return ""

        return try {
            tess?.setImage(bitmap)
            tess?.utF8Text ?: ""
        } finally {
            bitmap.recycle()
        }
    }

    fun release() {
        tess?.recycle()
        tess = null
    }
}
