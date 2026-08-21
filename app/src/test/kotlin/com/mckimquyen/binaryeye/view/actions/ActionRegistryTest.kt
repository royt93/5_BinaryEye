package com.mckimquyen.binaryeye.view.actions

import com.mckimquyen.binaryeye.view.actions.search.OpenOrSearchAction
import com.mckimquyen.binaryeye.view.actions.tel.TelAction
import com.mckimquyen.binaryeye.view.actions.web.WebAction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ActionRegistryTest {

    @Test
    fun webAction_isLastInRegistry() {
        // [FIX SEC-06] Bat buoc: URL matching cua WebAction rat rong (greedy),
        // phai la lua chon cuoi cung sau khi cac action cu the hon da thu.
        assertEquals(WebAction, ActionRegistry.REGISTRY.last())
    }

    @Test
    fun telScheme_resolvesToTelAction() {
        assertEquals(TelAction, ActionRegistry.getAction("tel:0123456789".toByteArray()))
    }

    @Test
    fun plainText_fallsBackToDefaultAction() {
        assertEquals(
            OpenOrSearchAction,
            ActionRegistry.getAction("just some random scanned text".toByteArray())
        )
        assertTrue(ActionRegistry.DEFAULT_ACTION === OpenOrSearchAction)
    }
}
