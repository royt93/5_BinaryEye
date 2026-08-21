package com.mckimquyen.binaryeye.view.actions

import com.mckimquyen.binaryeye.view.actions.mail.MailAction
import com.mckimquyen.binaryeye.view.actions.mail.MatMsgAction
import com.mckimquyen.binaryeye.view.actions.otpauth.OtpauthAction
import com.mckimquyen.binaryeye.view.actions.search.OpenOrSearchAction
import com.mckimquyen.binaryeye.view.actions.sms.SmsAction
import com.mckimquyen.binaryeye.view.actions.tel.TelAction
import com.mckimquyen.binaryeye.view.actions.vtype.vcard.VCardAction
import com.mckimquyen.binaryeye.view.actions.vtype.vevent.VEventAction
import com.mckimquyen.binaryeye.view.actions.web.WebAction
import com.mckimquyen.binaryeye.view.actions.wifi.WifiAction
import com.roy.sdkadbmob.SafeLogger

private const val TAG = "ActionRegistry"

object ActionRegistry {
    val DEFAULT_ACTION: IAction = OpenOrSearchAction

    // [FIX SEC-06] List thay vi Set - thu tu la invariant bat buoc (WebAction
    // phai cuoi), truoc day chi dung nho hanh vi ngam cua setOf()=LinkedHashSet,
    // khong duoc compiler dam bao
    internal val REGISTRY: List<IAction> = listOf(
        MailAction,
        MatMsgAction,
        OtpauthAction,
        SmsAction,
        TelAction,
        VCardAction,
        VEventAction,
        WifiAction,
        // Try WebAction last because recognizing colloquial URLs is
        // very aggressive.
        WebAction
    )

    fun getAction(data: ByteArray): IAction {
        val action = REGISTRY.find { it.canExecuteOn(data) } ?: DEFAULT_ACTION
        SafeLogger.d(TAG, "getAction resolved to ${action::class.simpleName}")
        return action
    }
}
