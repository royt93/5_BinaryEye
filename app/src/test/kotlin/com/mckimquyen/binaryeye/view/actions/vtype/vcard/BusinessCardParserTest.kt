package com.mckimquyen.binaryeye.view.actions.vtype.vcard

import com.mckimquyen.binaryeye.view.actions.vtype.VTypeParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BusinessCardParserTest {

    @Test
    fun parse_typicalCard_extractsAllFields() {
        val lines = listOf(
            "Nguyen Van A",
            "Sales Manager",
            "ABC Company Ltd",
            "0901 234 567",
            "a.nguyen@abc.com",
        )
        val card = BusinessCardParser.parse(lines)
        assertEquals("Nguyen Van A", card.name)
        assertEquals(listOf("0901 234 567"), card.phones)
        assertEquals(listOf("a.nguyen@abc.com"), card.emails)
        assertEquals("ABC Company Ltd", card.company)
        assertEquals("Sales Manager", card.title)
    }

    @Test
    fun parse_multiplePhonesAndEmails_capturesAll() {
        val lines = listOf(
            "John Doe",
            "+84 901 234 567",
            "028 3822 1234",
            "john@work.com",
            "john.doe@personal.com",
        )
        val card = BusinessCardParser.parse(lines)
        assertEquals(2, card.phones.size)
        assertEquals(2, card.emails.size)
    }

    @Test
    fun parse_emptyLines_areIgnored() {
        val card = BusinessCardParser.parse(listOf("", "  ", "Jane Smith", ""))
        assertEquals("Jane Smith", card.name)
    }

    @Test
    fun parse_noStructuredData_nameIsFirstLine_othersEmpty() {
        val card = BusinessCardParser.parse(listOf("Just some text", "more text"))
        assertEquals("Just some text", card.name)
        assertTrue(card.phones.isEmpty())
        assertTrue(card.emails.isEmpty())
    }

    @Test
    fun parse_emptyInput_returnsAllEmptyDefaults() {
        val card = BusinessCardParser.parse(emptyList())
        assertEquals("", card.name)
        assertTrue(card.phones.isEmpty())
        assertTrue(card.emails.isEmpty())
        assertEquals("", card.company)
        assertEquals("", card.title)
    }

    @Test
    fun parse_shortDigitSequence_notTreatedAsPhone() {
        // "Room 12" khong du 7 chu so nen khong bi bat nham la SDT
        val card = BusinessCardParser.parse(listOf("Room 12"))
        assertTrue(card.phones.isEmpty())
    }

    @Test
    fun toVCard_roundTrips_throughVTypeParser() {
        val card = BusinessCardParser.ParsedCard(
            name = "Nguyen Van A",
            phones = listOf("0901234567"),
            emails = listOf("a@b.com"),
            company = "ABC Ltd",
            title = "Manager",
        )
        val vcard = BusinessCardParser.toVCard(card)
        assertEquals("VCARD", VTypeParser.parseVType(vcard))
        val parsed = VTypeParser.parseMap(vcard)
        assertEquals("Nguyen Van A", parsed["FN"]?.single()?.value)
        assertEquals("0901234567", parsed["TEL"]?.single()?.value)
        assertEquals("a@b.com", parsed["EMAIL"]?.single()?.value)
        assertEquals("ABC Ltd", parsed["ORG"]?.single()?.value)
        assertEquals("Manager", parsed["TITLE"]?.single()?.value)
    }

    @Test
    fun toVCard_blankOptionalFields_areOmitted() {
        val card = BusinessCardParser.ParsedCard(name = "Solo Name")
        val vcard = BusinessCardParser.toVCard(card)
        assertTrue(!vcard.contains("ORG:"))
        assertTrue(!vcard.contains("TITLE:"))
        assertTrue(!vcard.contains("TEL:"))
        assertTrue(!vcard.contains("EMAIL:"))
        assertEquals("VCARD", VTypeParser.parseVType(vcard))
    }
}
