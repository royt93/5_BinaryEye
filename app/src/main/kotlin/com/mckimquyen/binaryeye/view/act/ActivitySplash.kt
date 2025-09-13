package com.mckimquyen.binaryeye.view.act

import android.content.Intent
import android.os.Bundle
import com.mckimquyen.binaryeye.BaseActivity
import com.mckimquyen.binaryeye.R
import com.mckimquyen.binaryeye.sdkadbmob.AdMobManager
import com.mckimquyen.binaryeye.sdkadbmob.UIUtils

class ActivitySplash : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        UIUtils.setupEdgeToEdge1(window)
        setContentView(R.layout.roy_a_splash)
        UIUtils.setupEdgeToEdge2(findViewById(R.id.layoutRoot))
        // It's important _not_ to inflate a layout file here
        // because that would happen after the app is fully
        // initialized what is too late.

        AdMobManager.initSplashScreen(activity = this, onAdLoaded = {
            goToMain()
        })
    }


    private fun goToMain() {
        val intent = Intent(this@ActivitySplash, CameraActivity::class.java)
        startActivity(intent)
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        // Trì hoãn finish để đợi animation hoàn tất
        window.decorView.postDelayed({
            finish() // Finish sau animation
        }, 300) // delay khoảng 300ms (hoặc đúng thời gian của animation)
    }
}

