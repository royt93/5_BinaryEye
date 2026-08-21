package com.mckimquyen.binaryeye.database

import android.content.Context
import android.database.Cursor
import com.mckimquyen.binaryeye.view.io.writeExternalFile
import com.roy.sdkadbmob.SafeLogger
import java.io.ByteArrayOutputStream
import java.io.OutputStream

private const val TAG = "CsvExport"

fun Context.exportCsv(
	name: String,
	cursor: Cursor,
	delimiter: String,
) = writeExternalFile(name, "text/csv") { outputStream ->
    exportCsv(outputStream, cursor, delimiter)
}

fun Cursor.exportCsv(delimiter: String): String {
    val outputStream = ByteArrayOutputStream()
    exportCsv(outputStream, this, delimiter)
    return outputStream.toString()
}

@OptIn(ExperimentalStdlibApi::class)
private fun exportCsv(
	outputStream: OutputStream,
	cursor: Cursor,
	delimiter: String,
) {
    if (!cursor.moveToFirst()) {
        return
    }
    val columns = arrayOf(
        Db.SCANS_DATETIME,
        Db.SCANS_FORMAT,
        Db.SCANS_CONTENT,
        Db.SCANS_ERROR_CORRECTION_LEVEL,
        Db.SCANS_VERSION,
        Db.SCANS_SEQUENCE_SIZE,
        Db.SCANS_SEQUENCE_INDEX,
        Db.SCANS_SEQUENCE_ID,
        Db.SCANS_GTIN_COUNTRY,
        Db.SCANS_GTIN_ADD_ON,
        Db.SCANS_GTIN_PRICE,
        Db.SCANS_GTIN_ISSUE_NUMBER
    )
    val indices = columns.map {
        cursor.getColumnIndex(it)
    }
    val contentIndex = cursor.getColumnIndex(Db.SCANS_CONTENT)
    val rawIndex = cursor.getColumnIndex(Db.SCANS_RAW)
    outputStream.write(
        columns.joinToString(
            separator = delimiter,
            postfix = "\n"
        ).toByteArray()
    )
    do {
        var deviation: Pair<Int, String>? = null
        if (cursor.getString(contentIndex)?.isEmpty() == true) {
            deviation = Pair(
                first = contentIndex,
                second = cursor.getBlob(rawIndex).toHexString()
            )
        }
        outputStream.write(
            cursor.toCsvRecord(
                indices = indices,
                delimiter = delimiter,
                deviation = deviation
            )
        )
    } while (cursor.moveToNext())
}

private fun Cursor.toCsvRecord(
	indices: List<Int>,
	delimiter: String,
	deviation: Pair<Int, String>?,
): ByteArray {
    // [FIX BUG-10] Dung joinToString giong header - truoc day moi cot deu bi
    // them delimiter thua o cuoi (ke ca cot cuoi), lech so cot voi header
    val row = indices.joinToString(separator = delimiter) {
        val value = if (deviation?.first == it) {
            deviation.second
        } else {
            this.getString(it)
        }
        value?.quoteAndEscape() ?: ""
    }
    return "$row\n".toByteArray()
}

private fun String.quoteAndEscape() = "\"${
    this
        .escapeFormulaInjection()
        .replace("\n", " ")
        .replace("\"", "\"\"")
}\""

// [FIX SEC-05] Noi dung scan (vd tu QR doc hai) co the bat dau bang =/+/-/@
// khien Excel/Sheets dien giai thanh cong thuc khi mo file CSV - them tien
// to nhay don de ep hien thi nhu text thuan
internal fun String.escapeFormulaInjection(): String {
    if (isEmpty() || first() !in FORMULA_INJECTION_PREFIXES) {
        return this
    }
    SafeLogger.w(TAG, "CSV formula injection prefix sanitized")
    return "'$this"
}

private val FORMULA_INJECTION_PREFIXES = charArrayOf('=', '+', '-', '@')
