package com.mckimquyen.binaryeye.database

import org.junit.Assert.assertEquals
import org.junit.Test

class TagUtilsTest {

    @Test
    fun parseTags_nullOrEmpty_returnsEmptyList() {
        assertEquals(emptyList<String>(), parseTags(null))
        assertEquals(emptyList<String>(), parseTags(""))
    }

    @Test
    fun parseTags_splitsAndTrims() {
        assertEquals(listOf("Work", "Personal"), parseTags("Work,Personal"))
        assertEquals(listOf("Work", "Personal"), parseTags(" Work , Personal "))
    }

    @Test
    fun parseTags_dropsBlankEntries() {
        assertEquals(listOf("Work", "Personal"), parseTags("Work,,Personal,"))
    }

    @Test
    fun joinTags_dedupesAndTrims() {
        assertEquals("Work,Personal", joinTags(listOf(" Work ", "Personal", "Work")))
    }

    @Test
    fun joinTags_emptyList_returnsEmptyString() {
        assertEquals("", joinTags(emptyList()))
    }

    @Test
    fun joinTags_dropsBlankEntries() {
        assertEquals("Work", joinTags(listOf("Work", "", "  ")))
    }

    @Test
    fun roundTrip_parseThenJoin_isStable() {
        val csv = "Work,Personal,Shopping"
        assertEquals(csv, joinTags(parseTags(csv)))
    }
}
