package com.mckimquyen.binaryeye.view.act

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.lifecycle.lifecycleScope
import com.mckimquyen.binaryeye.BaseActivity
import com.mckimquyen.binaryeye.BuildConfig
import com.mckimquyen.binaryeye.R
import com.mckimquyen.binaryeye.sdkadbmob.AdMobManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ActivitySplash : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.roy_a_splash)
        // It's important _not_ to inflate a layout file here
        // because that would happen after the app is fully
        // initialized what is too late.

        AdMobManager.loadAppOpenAd(
            context = this@ActivitySplash,
            adUnitId = BuildConfig.ADMOB_APP_OPEN_ID,
            onAdLoaded = { result ->
                Log.d("roy93~", "onAdLoaded result $result")
                goToMain()
                AdMobManager.showAppOpenAd(this@ActivitySplash)
            },
        )

//        lifecycleScope.launch {
//            var hasCalledGoToMain = false
//            val job = launch {
//                delay(3_000)
//                if (!hasCalledGoToMain) {
//                    hasCalledGoToMain = true
//                    Log.d("roy93~", "goToMain #1")
//                    goToMain()
//                }
//            }
//            AdMobManager.loadAppOpenAd(
//                context = this@ActivitySplash,
//                adUnitId = BuildConfig.ADMOB_APP_OPEN_ID,
//                onAdLoaded = {
//                    if (!hasCalledGoToMain) {
//                        hasCalledGoToMain = true
//                        job.cancel()
//                        Log.d("roy93~", "goToMain #2")
//                        goToMain()
//                        AdMobManager.showAppOpenAd(this@ActivitySplash)
//                    }
//                },
//            )
//        }
    }


    private fun goToMain() {
        val intent = Intent(this@ActivitySplash, CameraActivity::class.java)
        startActivity(intent)
        overridePendingTransition(0, 0)
        finishAffinity()
    }
}

