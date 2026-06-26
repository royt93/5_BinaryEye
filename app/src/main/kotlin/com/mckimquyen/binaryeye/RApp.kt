package com.mckimquyen.binaryeye

import android.app.Application
import android.util.Log
import com.mckimquyen.binaryeye.database.Db
import com.mckimquyen.binaryeye.pref.Pref
import com.roy.sdkadbmob.AdManager
import com.roy.sdkadbmob.AdSdkConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

val db = Db()
val prefs = Pref()

class RApp : Application() {
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        db.open(this)
        prefs.init(this)
        setupAdSystem()
    }

    override fun onTerminate() {
        super.onTerminate()
        appScope.cancel()
        db.close()
    }

    private fun setupAdSystem() {
        // Cấu hình AdmobApplovinWrapper cho AppLovin
        val adConfig = AdSdkConfig(
            isEnableAdmob = BuildConfig.IS_ENABLE_ADMOB, // false
            isDebug = BuildConfig.DEBUG,
            applovinSdkKey = BuildConfig.APPLOVIN_SDK_KEY,
            applovinBannerId = BuildConfig.APPLOVIN_BANNER_ID,
            applovinInterstitialId = BuildConfig.APPLOVIN_INTERSTITIAL_ID,
            applovinAppOpenId = BuildConfig.APPLOVIN_APP_OPEN_ID,
            applovinRewardedId = BuildConfig.APPLOVIN_REWARDED_ID,
            // Sử dụng Secret Key 30 ngày làm gốc
            vipKeySecret = "OWZBMHE3ZU4hMjdjTHgwNEAyMTk5M1kydTBJNyNRMA==",
            safety = if (BuildConfig.DEBUG) com.roy.sdkadbmob.AdSafetyLimits.TEST else com.roy.sdkadbmob.AdSafetyLimits()
        )

        AdManager.setConfig(adConfig)
        
        AdManager.initialize(this) { success, gaid ->
            if (BuildConfig.DEBUG) Log.d("roy93~", "AdManager init success=$success, gaid=$gaid")
        }
    }
}
