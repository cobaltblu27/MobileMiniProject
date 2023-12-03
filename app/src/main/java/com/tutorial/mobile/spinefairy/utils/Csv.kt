package com.tutorial.mobile.spinefairy.utils

import android.app.Activity
import android.content.ContentValues
import android.os.Environment
import android.provider.MediaStore
import com.tutorial.mobile.spinefairy.filePath
import com.tutorial.mobile.spinefairy.model.CsvData
import com.tutorial.mobile.spinefairy.model.PoseMeasurement
import java.io.File

inline fun <reified T: CsvData<T>> List<T>.toCsv(): String {
    val header = firstOrNull()?.header ?: ""
    return this.map { it.toCsv() }.let {
        (listOf(header) + it).joinToString("\n")
    }
}

fun measurementsFromCsv(data: String): List<PoseMeasurement> {
    val rows = data.lines().drop(1)
    return rows.map {
        PoseMeasurement.fromCsv(it)
    }
}

fun Activity.saveCsv(content: String, fileName: String) {
    val relativeLocation = Environment.DIRECTORY_DOCUMENTS + File.separator + filePath
    val contentValues = ContentValues().apply {
        put(MediaStore.Downloads.DISPLAY_NAME, fileName)
        put(MediaStore.Downloads.MIME_TYPE, "text/csv")
        put(MediaStore.Downloads.RELATIVE_PATH, relativeLocation)
    }
    val uri = contentResolver.insert(MediaStore.Files.getContentUri("external"), contentValues)
        ?: throw Exception()
    contentResolver.openOutputStream(uri)?.use { outputStream ->
        outputStream.write(content.toByteArray())
    }
    contentValues.clear()
    contentValues.put(MediaStore.Downloads.IS_PENDING, 0)
    contentResolver.update(uri, contentValues, null, null)
}
