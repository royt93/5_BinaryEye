package com.mckimquyen.binaryeye.ext.app

import android.app.AlertDialog
import android.content.Context
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

suspend inline fun <T : Any> alertDialog(
    context: Context,
    crossinline build: AlertDialog.Builder.(resume: (T?) -> Unit) -> Unit,
): T? = withContext(Dispatchers.Main) {
    suspendCoroutine { continuation ->
        AlertDialog.Builder(context).apply {
            setOnCancelListener {
                continuation.resume(null)
            }
            build(continuation::resume)
            show()
        }
    }
}
