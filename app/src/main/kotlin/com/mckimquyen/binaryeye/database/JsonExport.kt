package com.mckimquyen.binaryeye.database

import android.content.Context
import android.database.Cursor
import com.mckimquyen.binaryeye.view.io.writeExternalFile
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

fun Context.exportJson(
    name: String,
    cursor: Cursor,
) = writeExternalFile(name, "application/json") { outputStream ->
    cursor.exportJson()?.let {
        outputStream.write(it.toByteArray())
    }
}

@OptIn(ExperimentalStdlibApi::class)
fun Cursor.exportJson(): String? {
    if (!moveToFirst()) {
        return null
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
        Db.SCANS_GTIN_ISSUE_NUMBER,
        Db.SCANS_TAGS
    )
    val indices = columns.map {
        Pair(getColumnIndex(it), it)
    }
    val contentIndex = getColumnIndex(Db.SCANS_CONTENT)
    val rawIndex = getColumnIndex(Db.SCANS_RAW)
    val root = JSONArray()
    do {
        var deviation: Pair<Int, String>? = null
        if (getString(contentIndex)?.isEmpty() == true) {
            deviation = Pair(
                contentIndex,
                getBlob(/* columnIndex = */ rawIndex).toHexString()
            )
        }
        root.put(toJsonObject(indices, deviation))
    } while (moveToNext())
    return root.toString()
}

private fun Cursor.toJsonObject(
    indices: List<Pair<Int, String>>,
    deviation: Pair<Int, String>?,
): JSONObject {
    val obj = JSONObject()
    return try {
        indices.forEach {
            val value = if (deviation?.first == it.first) {
                deviation.second
            } else {
                this.getString(it.first)
            }
            obj.put(it.second, value ?: "")
        }
        obj
    } catch (e: JSONException) {
        obj
    }
}
