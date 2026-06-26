package com.mckimquyen.binaryeye.frm

private val UNSAFE_ENTRY_CHARS = "[^A-Za-z0-9._-]".toRegex()

/**
 * Builds a safe, collision-free ZIP entry file name for a batch QR item.
 * Pure function (no Android dependency) so it can be unit-tested on the JVM.
 *
 * The source [name] is truncated, stripped of filesystem-unsafe characters,
 * and suffixed with the 1-based [index] to guarantee uniqueness even when two
 * items sanitize to the same string (or are empty).
 */
fun zipEntryName(name: String, index: Int): String {
    val safe = name.take(24)
        .replace(UNSAFE_ENTRY_CHARS, "_")
        .trim('_')
        .ifEmpty { "qr" }
    return "${safe}_${index + 1}.png"
}
