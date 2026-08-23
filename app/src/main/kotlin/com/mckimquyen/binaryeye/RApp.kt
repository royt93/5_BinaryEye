package com.mckimquyen.binaryeye

import android.app.Application
import android.util.Log
import com.mckimquyen.binaryeye.database.Db
import com.mckimquyen.binaryeye.pref.Pref
import com.mckimquyen.binaryeye.view.act.ActivitySplash
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
        // Cấu hình AdmobApplovinWrapper — provider AdMob (IS_ENABLE_ADMOB=true), AppLovin giữ làm mediation/fallback
        val adConfig = AdSdkConfig(
            isEnableAdmob = BuildConfig.IS_ENABLE_ADMOB, // true
            isDebug = BuildConfig.DEBUG,
            admobBannerId = BuildConfig.ADMOB_BANNER_ID,
            admobInterstitialId = BuildConfig.ADMOB_INTERSTITIAL_ID,
            admobAppOpenId = BuildConfig.ADMOB_APP_OPEN_ID,
            admobRewardedId = BuildConfig.ADMOB_REWARDED_ID,
            applovinSdkKey = BuildConfig.APPLOVIN_SDK_KEY,
            applovinBannerId = BuildConfig.APPLOVIN_BANNER_ID,
            applovinInterstitialId = BuildConfig.APPLOVIN_INTERSTITIAL_ID,
            applovinAppOpenId = BuildConfig.APPLOVIN_APP_OPEN_ID,
            applovinRewardedId = BuildConfig.APPLOVIN_REWARDED_ID,
            // Sử dụng Secret Key 30 ngày làm gốc
            vipKeySecret = BuildConfig.VIP_KEY_SECRET,
            // FVipManagement dùng AdManager.activateVipByKey(...) (plaintext key), nhánh này SDK
            // >=1.2.0 mặc định TẮT — bật lại để giữ nguyên hành vi VIP hiện có (chưa migrate token ECDSA).
            allowLegacyPlaintextVipKey = true,
            // App Open không được tự-động đè lên Splash trong lúc awaitSplashComplete() đang chạy flow riêng.
            appOpenExcludedActivities = listOf(ActivitySplash::class.java),
            safety = if (BuildConfig.DEBUG) com.roy.sdkadbmob.AdSafetyLimits.TEST else com.roy.sdkadbmob.AdSafetyLimits()
        )

        AdManager.setConfig(adConfig)
        
        AdManager.initialize(this) { success, gaid ->
            if (BuildConfig.DEBUG) Log.d("roy93~", "AdManager init success=$success, gaid=$gaid")
        }
    }
}
