package com.mckimquyen.binaryeye.view.audit

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AuditSessionTest {

    @Test
    fun tallyOnlyMode_noExpectedList_firstScanIsNewExpected() {
        val session = AuditSession()
        assertFalse(session.hasExpectedList)
        assertEquals(AuditSession.Outcome.NEW_EXPECTED, session.recordScan("A001", "QR_CODE"))
        assertEquals(1, session.uniqueCount)
        assertEquals(1, session.totalScans)
        assertEquals(0, session.unexpectedCount)
    }

    @Test
    fun tallyOnlyMode_rescan_isDuplicate() {
        val session = AuditSession()
        session.recordScan("A001", "QR_CODE")
        val outcome = session.recordScan("A001", "QR_CODE")
        assertEquals(AuditSession.Outcome.DUPLICATE, outcome)
        assertEquals(1, session.uniqueCount)
        assertEquals(2, session.totalScans)
        assertEquals(1, session.duplicateScanCount)
    }

    @Test
    fun expectedList_scanInList_isNewExpected() {
        val session = AuditSession(listOf("A001", "A002"))
        assertEquals(AuditSession.Outcome.NEW_EXPECTED, session.recordScan("A001", "QR_CODE"))
        assertEquals(0, session.unexpectedCount)
    }

    @Test
    fun expectedList_scanNotInList_isNewUnexpected() {
        val session = AuditSession(listOf("A001", "A002"))
        assertEquals(AuditSession.Outcome.NEW_UNEXPECTED, session.recordScan("X999", "QR_CODE"))
        assertEquals(1, session.unexpectedCount)
    }

    @Test
    fun expectedList_rescanOfUnexpected_isStillDuplicateNotUnexpectedAgain() {
        val session = AuditSession(listOf("A001"))
        session.recordScan("X999", "QR_CODE")
        val outcome = session.recordScan("X999", "QR_CODE")
        assertEquals(AuditSession.Outcome.DUPLICATE, outcome)
        // unexpectedCount dem theo ma duy nhat, khong tang them khi quet lai
        assertEquals(1, session.unexpectedCount)
    }

    @Test
    fun expectedList_codeNeverScanned_appearsInMissing() {
        val session = AuditSession(listOf("A001", "A002", "A003"))
        session.recordScan("A001", "QR_CODE")
        assertEquals(listOf("A002", "A003"), session.missingCodes)
    }

    @Test
    fun expectedList_allScanned_missingIsEmpty() {
        val session = AuditSession(listOf("A001", "A002"))
        session.recordScan("A001", "QR_CODE")
        session.recordScan("A002", "QR_CODE")
        assertTrue(session.missingCodes.isEmpty())
    }

    @Test
    fun rows_statusReflectsOkDuplicateUnexpected() {
        val session = AuditSession(listOf("A001", "A002"))
        session.recordScan("A001", "QR_CODE") // OK
        session.recordScan("A001", "QR_CODE") // duplicate -> DUPLICATE
        session.recordScan("X999", "QR_CODE") // UNEXPECTED

        val rows = session.rows().associateBy { it.content }
        assertEquals("DUPLICATE", rows.getValue("A001").status)
        assertEquals(2, rows.getValue("A001").count)
        assertEquals("UNEXPECTED", rows.getValue("X999").status)
    }

    @Test
    fun rows_tallyOnlyMode_statusIsOkOrDuplicate_neverUnexpected() {
        val session = AuditSession()
        session.recordScan("A001", "QR_CODE")
        session.recordScan("A001", "QR_CODE")
        session.recordScan("B002", "EAN_13")

        val rows = session.rows().associateBy { it.content }
        assertEquals("DUPLICATE", rows.getValue("A001").status)
        assertEquals("OK", rows.getValue("B002").status)
    }

    @Test
    fun missingRows_haveZeroCountAndMissingStatus() {
        val session = AuditSession(listOf("A001", "A002"))
        session.recordScan("A001", "QR_CODE")
        val missing = session.missingRows()
        assertEquals(1, missing.size)
        assertEquals("A002", missing[0].content)
        assertEquals(0, missing[0].count)
        assertEquals("MISSING", missing[0].status)
    }

    @Test
    fun toCsv_headerAndRowCountMatchScannedPlusMissing() {
        val session = AuditSession(listOf("A001", "A002", "A003"))
        session.recordScan("A001", "QR_CODE")
        session.recordScan("X999", "EAN_13")

        val lines = session.toCsv().trim().split("\n")
        assertEquals(
            "content,format,count,status,gtin,lot,expiry,serial,cross_session_alert",
            lines[0]
        )
        // header + A001 + X999 (scanned) + A002 + A003 (missing) = 5
        assertEquals(5, lines.size)
        assertTrue(lines.any { it.contains("\"A001\"") && it.contains("\"OK\"") })
        assertTrue(lines.any { it.contains("\"X999\"") && it.contains("\"UNEXPECTED\"") })
        assertTrue(lines.any { it.contains("\"A002\"") && it.contains("\"MISSING\"") })
    }

    @Test
    fun rowsPreserveInsertionOrder() {
        val session = AuditSession()
        session.recordScan("C003", "QR_CODE")
        session.recordScan("A001", "QR_CODE")
        session.recordScan("B002", "QR_CODE")
        assertEquals(listOf("C003", "A001", "B002"), session.rows().map { it.content })
    }

    @Test
    fun snapshotAndRestore_reproducesSameCountsAndOutcomes() {
        val original = AuditSession(listOf("A001", "A002"))
        original.recordScan("A001", "QR_CODE")
        original.recordScan("A001", "QR_CODE") // duplicate
        original.recordScan("X999", "EAN_13") // unexpected

        val restored = AuditSession(original.expectedCodesList)
        restored.restoreEntries(original.snapshotEntries())

        assertEquals(original.totalScans, restored.totalScans)
        assertEquals(original.uniqueCount, restored.uniqueCount)
        assertEquals(original.unexpectedCount, restored.unexpectedCount)
        assertEquals(original.missingCodes, restored.missingCodes)
        assertEquals(original.rows(), restored.rows())
        // Sau khi khoi phuc, quet lai A001 phai van la DUPLICATE (khong bi coi la moi)
        assertEquals(AuditSession.Outcome.DUPLICATE, restored.recordScan("A001", "QR_CODE"))
    }

    @Test
    fun rows_gs1Content_exposesParsedGtinLotExpirySerial() {
        val session = AuditSession()
        session.recordScan("(01)09501101530003(17)191231(10)LOT1(21)SN1", "DATA_MATRIX")
        session.recordScan("plain-non-gs1-code", "QR_CODE")

        val rows = session.rows().associateBy { it.content }
        val gs1Row = rows.getValue("(01)09501101530003(17)191231(10)LOT1(21)SN1")
        assertEquals("09501101530003", gs1Row.gtin)
        assertEquals("LOT1", gs1Row.lot)
        assertEquals("2019-12-31", gs1Row.expiryDate)
        assertEquals("SN1", gs1Row.serial)

        val plainRow = rows.getValue("plain-non-gs1-code")
        assertEquals(null, plainRow.gtin)
        assertEquals(null, plainRow.serial)
    }

    @Test
    fun toCsv_includesGs1Columns() {
        val session = AuditSession()
        session.recordScan("(01)09501101530003(21)SN1", "DATA_MATRIX")
        val csv = session.toCsv()
        assertTrue(csv.contains("\"09501101530003\""))
        assertTrue(csv.contains("\"SN1\""))
    }

    @Test
    fun toCsv_crossSessionAlertCallback_marksMatchingRowsOnly() {
        val session = AuditSession()
        session.recordScan("(21)FLAGGED", "QR_CODE")
        session.recordScan("(21)CLEAN", "QR_CODE")

        val csv = session.toCsv(isCrossSessionAlert = { it.serial == "FLAGGED" })
        val lines = csv.trim().split("\n")
        val flaggedLine = lines.first { it.contains("FLAGGED") }
        val cleanLine = lines.first { it.contains("CLEAN") }
        assertTrue(flaggedLine.endsWith("\"YES\""))
        assertTrue(cleanLine.endsWith("\"\""))
    }
}
