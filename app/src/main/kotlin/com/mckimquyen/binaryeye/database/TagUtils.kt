package com.mckimquyen.binaryeye.database

private const val TAG_SEPARATOR = ","

/** Splits a stored CSV tag string (e.g. "Work,Personal") into a clean list. Pure function. */
fun parseTags(csv: String?): List<String> =
    csv?.split(TAG_SEPARATOR)
        ?.map { it.trim() }
        ?.filter { it.isNotEmpty() }
        ?: emptyList()

/** Builds the CSV string to store, deduping and dropping blanks. Pure function. */
fun joinTags(tags: List<String>): String =
    tags.map { it.trim() }
        .filter { it.isNotEmpty() }
        .distinct()
        .joinToString(TAG_SEPARATOR)
