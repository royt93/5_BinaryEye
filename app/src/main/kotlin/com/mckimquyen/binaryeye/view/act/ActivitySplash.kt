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
import com.mckimquyen.binaryeye.prefs
import com.roy.sdkadbmob.awaitSplashComplete
import kotlinx.coroutines.launch

class ActivitySplash : BaseActivity() {
    private val handler = Handler(Looper.getMainLooper())
    private val finishRunnable = Runnable { finish() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.roy_a_splash)

        applySplashAnimations()

        // [FEAT E8] Neu app vua duoc mo trong 30 phut gan day (cold start lai
        // do he thong kill), bo qua App Open ad de vao Main nhanh hon
        val recentlyForegrounded =
            System.currentTimeMillis() - prefs.lastForegroundMs < RECENT_FOREGROUND_WINDOW_MS

        com.roy.sdkadbmob.AdManager.requestConsentInfoUpdate(
            activity = this,
            tagForUnderAgeOfConsent = false
        ) { canRequestAds ->
            if (canRequestAds && !recentlyForegrounded) {
                runSplashAdFlow()
            } else {
                goToMain()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        splashJob?.cancel()
        handler.removeCallbacks(finishRunnable)
    }

    private fun applySplashAnimations() {
        findViewById<TextView>(R.id.tvVersion)?.apply {
            text = "v${BuildConfig.VERSION_NAME}"
            startAnimation(AnimationUtils.loadAnimation(context, R.anim.splash_fade_in))
        }
        findViewById<TextView>(R.id.tvAppName)?.apply {
            startAnimation(AnimationUtils.loadAnimation(context, R.anim.splash_fade_in))
        }
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
        prefs.lastForegroundMs = System.currentTimeMillis()
        val intent = Intent(this@ActivitySplash, ActivityCamera::class.java)
        startActivity(intent)
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        handler.postDelayed(finishRunnable, 300)
    }

    private companion object {
        const val RECENT_FOREGROUND_WINDOW_MS = 30 * 60_000L
    }
}

