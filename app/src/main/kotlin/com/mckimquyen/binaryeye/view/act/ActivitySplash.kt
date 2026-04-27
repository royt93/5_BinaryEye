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
import com.roy.sdkadbmob.awaitSplashComplete
import kotlinx.coroutines.launch

class ActivitySplash : BaseActivity() {
    // [FIX L2] Dùng Handler có thể cancel để tránh giữ Activity reference
    private val handler = Handler(Looper.getMainLooper())
    private val finishRunnable = Runnable { finish() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.roy_a_splash)

        // Apply animations to splash elements
        applySplashAnimations()

        com.roy.sdkadbmob.AdManager.requestConsentInfoUpdate(
            activity = this,
            tagForUnderAgeOfConsent = false
        ) { canRequestAds ->
            if (canRequestAds) {
                runSplashAdFlow()
            } else {
                goToMain()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // [FIX L2] Cancel pending callbacks để tránh memory leak
        splashJob?.cancel()
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
    
    private var splashJob: kotlinx.coroutines.Job? = null

    @OptIn(com.roy.sdkadbmob.ExperimentalAdApi::class)
    private fun runSplashAdFlow() {
        splashJob = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
            com.roy.sdkadbmob.AdManager.awaitSplashComplete(this@ActivitySplash)
            if (!isDestroyed && !isFinishing) {
                goToMain()
            }
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

