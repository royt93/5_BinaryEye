package com.mckimquyen.binaryeye.view.act

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.animation.AnimationUtils
import android.widget.TextView
import com.mckimquyen.binaryeye.BaseActivity
import com.mckimquyen.binaryeye.BuildConfig
import com.mckimquyen.binaryeye.R
import com.mckimquyen.binaryeye.sdkadbmob.AdMobManager
import com.mckimquyen.binaryeye.sdkadbmob.UIUtils

class ActivitySplash : BaseActivity() {
    // [FIX L2] Dùng Handler có thể cancel để tránh giữ Activity reference
    private val handler = Handler(Looper.getMainLooper())
    private val finishRunnable = Runnable { finish() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        UIUtils.setupEdgeToEdge1(window)
        setContentView(R.layout.roy_a_splash)
        UIUtils.setupEdgeToEdge2(findViewById(R.id.layoutRoot))

        // Apply animations to splash elements
        applySplashAnimations()

        AdMobManager.initSplashScreen(activity = this, onAdLoaded = {
            goToMain()
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        // [FIX L2] Cancel pending callbacks để tránh memory leak
        handler.removeCallbacks(finishRunnable)
    }

    private fun applySplashAnimations() {
        // Version text fade in
        findViewById<TextView>(R.id.tvVersion)?.apply {
            text = "v${BuildConfig.VERSION_NAME}"
            startAnimation(AnimationUtils.loadAnimation(context, R.anim.splash_fade_in))
        }

        // Logo scale in
//        findViewById<ImageView>(R.id.ivLogo)?.apply {
//            startAnimation(AnimationUtils.loadAnimation(context, R.anim.splash_scale_in))
//        }

        // App name fade in
        findViewById<TextView>(R.id.tvAppName)?.apply {
            startAnimation(AnimationUtils.loadAnimation(context, R.anim.splash_fade_in))
        }

        // Bottom info slide up
        findViewById<android.view.View>(R.id.layoutBottom)?.apply {
            startAnimation(AnimationUtils.loadAnimation(context, R.anim.splash_slide_up))
        }
    }

    private fun goToMain() {
        val intent = Intent(this@ActivitySplash, CameraActivity::class.java)
        startActivity(intent)
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        // [FIX L2] Dùng handler có thể cancel thay vì decorView.postDelayed
        handler.postDelayed(finishRunnable, 300)
    }
}

