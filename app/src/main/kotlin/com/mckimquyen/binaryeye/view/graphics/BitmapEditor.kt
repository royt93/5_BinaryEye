package com.mckimquyen.binaryeye.view.graphics

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.RectF
import android.net.Uri
import java.io.IOException
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

fun ContentResolver.loadImageUri(uri: Uri): Bitmap? = try {
    val options = BitmapFactory.Options()
    openInputStream(uri)?.use {
        options.inJustDecodeBounds = true
        BitmapFactory.decodeStream(it, null, options)
        options.inSampleSize = calculateInSampleSize(
            width = options.outWidth,
            height = options.outHeight
        )
        options.inJustDecodeBounds = false
    }
    openInputStream(uri)?.use {
        BitmapFactory.decodeStream(
            /* is = */ it,
            /* outPadding = */ null,
            /* opts = */ options
        )
    }
} catch (e: IOException) {
    null
}

private fun calculateInSampleSize(
    width: Int,
    height: Int,
    reqWidth: Int = 1024,
    reqHeight: Int = 1024,
): Int {
    var inSampleSize = 1
    if (height > reqHeight || width > reqWidth) {
        val halfHeight = height / 2
        val halfWidth = width / 2
        while (
            halfHeight / inSampleSize >= reqHeight ||
            halfWidth / inSampleSize >= reqWidth
        ) {
            inSampleSize *= 2
        }
    }
    return inSampleSize
}

fun Bitmap.crop(
    rect: RectF,
    rotation: Float,
    pivotX: Float,
    pivotY: Float,
) = try {
    val erected = erect(rotation, pivotX, pivotY)
    val w = erected.width
    val h = erected.height
    val x = max(0, (rect.left * w).roundToInt())
    val y = max(0, (rect.top * h).roundToInt())
    try {
        Bitmap.createBitmap(
            /* source = */ erected,
            /* x = */ x,
            /* y = */ y,
            /* width = */ min(w - x, (rect.right * w).roundToInt() - x),
            /* height = */ min(h - y, (rect.bottom * h).roundToInt() - y)
        )
    } finally {
        // [FIX BUG-9] Recycle intermediate bitmap neu no khac original
        // Tranh OOM voi anh lon tu gallery
        if (erected !== this) erected.recycle()
    }
} catch (e: OutOfMemoryError) {
    null
} catch (e: IllegalArgumentException) {
    null
}

private fun Bitmap.erect(
    rotation: Float,
    pivotX: Float,
    pivotY: Float,
): Bitmap = if (
    rotation % 360f != 0f
) {
    Bitmap.createBitmap(
        /* source = */ this,
        /* x = */ 0,
        /* y = */ 0,
        /* width = */ width,
        /* height = */ height,
        /* m = */ Matrix().apply {
            setRotate(rotation, pivotX, pivotY)
        },
        /* filter = */ true
    )
} else {
    this
}
