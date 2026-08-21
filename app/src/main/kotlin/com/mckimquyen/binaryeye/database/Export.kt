package com.mckimquyen.binaryeye.database

import android.app.Activity
import android.os.Environment
import com.mckimquyen.binaryeye.db
import com.mckimquyen.binaryeye.view.io.writeExternalFile
import java.io.File
import java.io.FileInputStream

fun Activity.exportDatabase(fileName: String): Boolean {
    val dbFile = File(
        Environment.getDataDirectory(),
        "//data//${packageName}//databases//${Db.FILE_NAME}"
    )
    if (!dbFile.exists()) {
        return false
    }
    // [FIX BUG-13] Flush WAL truoc khi copy - tranh thieu du lieu moi nhat
    db.checkpoint()
    return writeExternalFile(
        fileName,
        "application/vnd.sqlite3"
    ) {
        FileInputStream(dbFile).copyTo(it)
    }
}
