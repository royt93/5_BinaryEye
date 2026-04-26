package com.mckimquyen.binaryeye

import android.app.Application
import android.util.Log
import com.google.android.gms.ads.MobileAds
import com.mckimquyen.binaryeye.database.Db
import com.mckimquyen.binaryeye.pref.Pref
import com.mckimquyen.binaryeye.sdkadbmob.AdMobManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

//TODO firebase

//done mckimquyen
//review in app bingo
//120hz
//font scale
//ad id
//leak canary
//proguard
//ui switch
//ic_launcher
//app version
//rate app
//more app
//share app
//policy
//double tap to exit
//keystore
//20 tester
//ad applovin

val db = Db()
val prefs = Pref()

class RApp : Application() {
    // [FIX H1] Scope có lifecycle — dùng SupervisorJob để có thể cancel
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        db.open(this)
        prefs.init(this)
//        this.setupApplovinAd()
        setupAdmob()
    }

    override fun onTerminate() {
        super.onTerminate()
        // [FIX H1] Cancel scope khi app ket thuc
        appScope.cancel()
        // [FIX BUG-7] Dong SQLiteDatabase de flush WAL journal
        db.close()
    }

    private fun setupAdmob() {
        // [FIX H1] Dùng appScope thay vì CoroutineScope(Dispatchers.IO) vô danh
        appScope.launch {
            MobileAds.initialize(this@RApp) {}
            AdMobManager.init(this@RApp) { success, gaidCurrent ->
                Log.d("roy93~", "AdMobManager init success $success, gaidCurrent $gaidCurrent")
            }
        }
//        registerActivityLifecycleCallbacks(
//            AppLifecycleListener(
//                { isForeground, activity ->
//                    if (isForeground) {
//                        Log.d("roy93~", "App moved to Foreground")
//                        Log.d("roy93~", "activity.localClassName ${activity.localClassName}")
//                        Log.d(
//                            "roy93~",
//                            "SplashActivity::class.java.simpleName ${ActivitySplash::class.java.simpleName}"
//                        )
//                        if (activity.localClassName == ActivitySplash::class.java.simpleName) {
//                            //do nothing
//                        } else {
////                            AdMobManager.showAppOpenAd(activity)
//                        }
//                    } else {
//                        Log.d("roy93~", "App moved to Background")
//                    }
//                }, { activity ->
//                    Log.d("roy93~", "callbackActivityCreated ${activity.localClassName}")
//                    if (activity.localClassName == ActivitySplash::class.java.simpleName) {
//                        //do nothing
//                    } else {
////                        AdMobManager.loadAppOpenAd(
////                            context = this,
////                            adUnitId = BuildConfig.ADMOB_APP_OPEN_ID,
////                            onAdLoaded = {},
////                        )
//                    }
//                }
//            )
//        )
    }
}
