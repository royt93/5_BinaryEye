package com.mckimquyen.binaryeye.view.act

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Matrix
import android.graphics.Rect
import android.hardware.Camera
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.addCallback
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.mckimquyen.binaryeye.BaseActivity
import com.mckimquyen.binaryeye.BuildConfig
import com.mckimquyen.binaryeye.R
import com.mckimquyen.binaryeye.adapter.prettifyFormatName
import com.mckimquyen.binaryeye.database.toScan
import com.mckimquyen.binaryeye.db
import com.mckimquyen.binaryeye.ext.app.PERMISSION_CAMERA
import com.mckimquyen.binaryeye.ext.app.hasBluetoothPermission
import com.mckimquyen.binaryeye.ext.app.hasCameraPermission
import com.mckimquyen.binaryeye.ext.moreApp
import com.mckimquyen.binaryeye.ext.openBrowserPolicy
import com.mckimquyen.binaryeye.ext.rateApp
import com.mckimquyen.binaryeye.ext.rateAppInApp
import com.mckimquyen.binaryeye.ext.shareApp
import com.mckimquyen.binaryeye.prefs
import com.roy.sdkadbmob.AdManager
import com.roy.sdkadbmob.AdSdkConfig
import com.mckimquyen.binaryeye.view.bluetooth.sendBluetoothAsync
import com.mckimquyen.binaryeye.view.content.copyToClipboard
import com.mckimquyen.binaryeye.view.content.execShareIntent
import com.mckimquyen.binaryeye.view.content.openUrl
import com.mckimquyen.binaryeye.view.errorFeedback
import com.mckimquyen.binaryeye.view.graphics.FrameMetrics
import com.mckimquyen.binaryeye.view.graphics.mapPosition
import com.mckimquyen.binaryeye.view.graphics.setFrameRoi
import com.mckimquyen.binaryeye.view.graphics.setFrameToView
import com.mckimquyen.binaryeye.view.initSystemBars
import com.mckimquyen.binaryeye.view.media.releaseToneGenerators
import com.mckimquyen.binaryeye.view.net.sendAsync
import com.mckimquyen.binaryeye.view.net.urlEncode
import com.mckimquyen.binaryeye.view.scanFeedback
import com.mckimquyen.binaryeye.view.setPaddingFromWindowInsets
import com.mckimquyen.binaryeye.view.widget.DetectorView
import com.mckimquyen.binaryeye.view.widget.toast
import com.mckimquyen.binaryeye.frm.FLanguageDialog
import de.markusfisch.android.cameraview.widget.CameraView
import de.markusfisch.android.zxingcpp.ZxingCpp
import de.markusfisch.android.zxingcpp.ZxingCpp.Binarizer
import de.markusfisch.android.zxingcpp.ZxingCpp.ReaderOptions
import de.markusfisch.android.zxingcpp.ZxingCpp.Result
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

class CameraActivity : BaseActivity() {
    private val frameRoi = Rect()
    private val matrix = Matrix()

    private lateinit var cameraView: CameraView
    private lateinit var detectorView: DetectorView
    private lateinit var zoomBar: SeekBar
    private lateinit var flashFab: FloatingActionButton

    private var formatsToRead = setOf<String>()
    private var frameMetrics = FrameMetrics()
    private var decoding = true
    private var returnResult = false
    private var returnUrlTemplate: String? = null
    private var finishAfterShowingResult = false
    private var frontFacing = false
    private var bulkMode = prefs.bulkMode
    private var restrictFormat: String? = null
    private var ignoreNext: String? = null
    private var fallbackBuffer: IntArray? = null

    // [FIX M1] Handler de cancel pending back-press reset
    // resetDoubleBack da duoc inline vao setupDoubleBackToExit lambda
    private val doubleBackHandler = Handler(Looper.getMainLooper())

    private var adView: android.view.View? = null

//    private var interstitialAd: MaxInterstitialAd? = null

//    private fun createAdInter() {
//        val enableAdInter = getString(R.string.EnableAdInter) == "true"
//        if (enableAdInter) {
//            interstitialAd = MaxInterstitialAd(getString(R.string.INTER), this)
//            interstitialAd?.let { ad ->
//                ad.setListener(object : MaxAdListener {
//                    override fun onAdLoaded(p0: MaxAd) {
////                        logI("onAdLoaded")
////                        retryAttempt = 0
//                    }
//
//                    override fun onAdDisplayed(p0: MaxAd) {
////                        logI("onAdDisplayed")
//                    }
//
//                    override fun onAdHidden(p0: MaxAd) {
////                        logI("onAdHidden")
//                        // Interstitial Ad is hidden. Pre-load the next ad
//                        interstitialAd?.loadAd()
//                    }
//
//                    override fun onAdClicked(p0: MaxAd) {
////                        logI("onAdClicked")
//                    }
//
//                    override fun onAdLoadFailed(p0: String, p1: MaxError) {
////                        logI("onAdLoadFailed")
////                        retryAttempt++
////                        val delayMillis =
////                            TimeUnit.SECONDS.toMillis(2.0.pow(min(6, retryAttempt)).toLong())
////
////                        Handler(Looper.getMainLooper()).postDelayed(
////                            {
////                                interstitialAd?.loadAd()
////                            }, delayMillis
////                        )
//                    }
//
//                    override fun onAdDisplayFailed(p0: MaxAd, p1: MaxError) {
////                        logI("onAdDisplayFailed")
//                        // Interstitial ad failed to display. We recommend loading the next ad.
//                        interstitialAd?.loadAd()
//                    }
//
//                })
//                ad.setRevenueListener {
////                    logI("onAdDisplayed")
//                }
//
//                // Load the first ad.
//                ad.loadAd()
//            }
//        }
//    }
//
//    fun showAd(runnable: Runnable? = null) {
//        val enableAdInter = getString(R.string.EnableAdInter) == "true"
//        if (enableAdInter) {
//            if (interstitialAd == null) {
//                runnable?.run()
//            } else {
//                interstitialAd?.let { ad ->
//                    if (ad.isReady) {
////                        showDialogProgress()
////                        setDelay(500.getRandomNumber() + 500) {
////                            hideDialogProgress()
////                            ad.showAd()
////                            runnable?.run()
////                        }
//                        ad.showAd()
//                        runnable?.run()
//                    } else {
//                        runnable?.run()
//                    }
//                }
//            }
//        } else {
//            Toast.makeText(this, "Applovin show ad Inter in debug mode", Toast.LENGTH_SHORT).show()
//            runnable?.run()
//        }
//    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            PERMISSION_CAMERA -> if (grantResults.isNotEmpty() && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                toast(R.string.cameraError)
            }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        resultData: Intent?,
    ) {
        super.onActivityResult(requestCode, resultCode, resultData)
        when (requestCode) {
            PICK_FILE_RESULT_CODE -> {
                if (resultCode == RESULT_OK && resultData != null) {
                    val pick = Intent(this, ActivityPick::class.java)
                    pick.action = Intent.ACTION_VIEW
                    pick.setDataAndType(resultData.data, "image/*")
                    startActivity(pick)
                }
            }
        }
    }

//    override fun attachBaseContext(base: Context?) {
//        base?.applyLocale(prefs.customLocale)
//        super.attachBaseContext(base)
//    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.roy_a_camera)

        AdManager.setCurrentActivity(this)

        // Necessary to get the right translation after setting a
        // custom locale.
        setTitle(R.string.scan_code)

        initSystemBars(this)
        setSupportActionBar(findViewById(R.id.toolbar))

        cameraView = findViewById(R.id.cameraView)
        detectorView = findViewById(R.id.detectorView)
        zoomBar = findViewById(R.id.zoom)
        flashFab = findViewById(R.id.flash)

        initCameraView()
        initZoomBar()
        initDetectorView()

        if (intent?.action == Intent.ACTION_SEND && intent.type == "text/plain") {
            handleSendText(intent)
        }

//        flAd = findViewById(R.id.flAd)
        val bannerContainer = findViewById<ViewGroup>(R.id.bannerContainer)
        val tvLabelAd = findViewById<TextView>(R.id.tvLabelAd)
        adView = AdManager.loadBanner(
            context = this,
            container = bannerContainer,
            tvLabelAd = tvLabelAd,
            adSize = AdManager.getAdaptiveBannerSize(this),
        )

        AdManager.loadInterstitial(this)

        // [FIX BUG-6] Migrate tu deprecated onBackPressed sang OnBackPressedDispatcher
        setupDoubleBackToExit()
    }

    // [FIX BUG-6] Double-back to exit dung OnBackPressedCallback
    private fun setupDoubleBackToExit() {
        var doubleBackToExitPressedOnce = false
        onBackPressedDispatcher.addCallback(this) {
            if (doubleBackToExitPressedOnce) {
                finish()
                return@addCallback
            }
            doubleBackToExitPressedOnce = true
            // [FIX BUG-6] Dung R.string thay vi hard-coded string
            toast(R.string.press_back_again_to_exit)
            doubleBackHandler.removeCallbacksAndMessages(null)
            doubleBackHandler.postDelayed({
                doubleBackToExitPressedOnce = false
            }, 2000)
        }
    }

    override fun onDestroy() {
//        flAd?.destroyAdBanner(adView)
//        adView?.destroy()
        super.onDestroy()
        fallbackBuffer = null
        saveZoom()
        detectorView.saveCropHandlePos()
        releaseToneGenerators()
        // [FIX M1] Cancel tat ca pending Handler callbacks de tranh leak
        doubleBackHandler.removeCallbacksAndMessages(null)
        // [FIX ML-2] Null out listener de singleton khong giu Activity reference
//        AdMobManager.interstitialListener = null
    }

    override fun onResume() {
        super.onResume()
//        adView?.resume()
        System.gc()
        updateHints()
        if (prefs.bulkMode && bulkMode != prefs.bulkMode) {
            bulkMode = prefs.bulkMode
            invalidateOptionsMenu()
            ignoreNext = null
        }
        setReturnTarget(intent)
        if (hasCameraPermission()) {
            openCamera()
        }
        rateAppInApp(BuildConfig.DEBUG)
        // Show language selection dialog on first launch only
        showLanguageDialogIfNeeded()
    }

    private fun showLanguageDialogIfNeeded() {
        if (prefs.hasShownLanguageDialog) return
        // Check dialog chua duoc show truoc do (tranh duplicate khi recreate)
        val existing = supportFragmentManager.findFragmentByTag(FLanguageDialog.TAG)
        if (existing != null) return
        FLanguageDialog().show(supportFragmentManager, FLanguageDialog.TAG)
    }

    private fun updateHints() {
        val restriction = restrictFormat
        formatsToRead = if (restriction != null) {
            title = getString(
                R.string.scan_format, prettifyFormatName(restriction)
            )
            setOf(restriction)
        } else {
            setTitle(R.string.scan_code)
            prefs.barcodeFormats
        }
    }

    private fun setReturnTarget(intent: Intent?) {
        when {
            intent?.action == "com.google.zxing.client.android.SCAN" -> {
                returnResult = true
            }

            intent?.dataString?.isReturnUrl() == true -> {
                finishAfterShowingResult = true
                returnUrlTemplate = intent.data?.getQueryParameter("ret")
            }
        }
    }

    private fun openCamera() {
        cameraView.openAsync(
            CameraView.findCameraId(
                @Suppress("DEPRECATION") if (frontFacing) {
                    Camera.CameraInfo.CAMERA_FACING_FRONT
                } else {
                    Camera.CameraInfo.CAMERA_FACING_BACK
                }
            )
        )
    }

    override fun onPause() {
//        adView?.pause()
        super.onPause()
        closeCamera()
    }

    // [FIX BUG-6] Da xu ly qua setupDoubleBackToExit() – xoa deprecated override

    private fun closeCamera() {
        // [FIX BUG-4] Explicit null callback truoc khi dong camera
        // Tranh camera thread giu strong ref cua Activity sau onDestroy
        cameraView.camera?.setPreviewCallback(null)
        cameraView.close()
    }

    override fun onRestoreInstanceState(savedState: Bundle) {
        super.onRestoreInstanceState(savedState)
        zoomBar.max = savedState.getInt(ZOOM_MAX)
        zoomBar.progress = savedState.getInt(ZOOM_LEVEL)
        frontFacing = savedState.getBoolean(FRONT_FACING)
        bulkMode = savedState.getBoolean(BULK_MODE)
        restrictFormat = savedState.getString(RESTRICT_FORMAT)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt(ZOOM_MAX, zoomBar.max)
        outState.putInt(ZOOM_LEVEL, zoomBar.progress)
        outState.putBoolean(FRONT_FACING, frontFacing)
        outState.putBoolean(BULK_MODE, bulkMode)
        outState.putString(RESTRICT_FORMAT, restrictFormat)
        super.onSaveInstanceState(outState)
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        // Always give crop handle precedence over other controls
        // because it can easily overlap and would then be inaccessible.
        if (detectorView.onTouchEvent(ev)) {
            return true
        }
        return super.dispatchTouchEvent(ev)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_a_camera, menu)
        menu.findItem(R.id.bulkMode).isChecked = bulkMode
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_vip -> {
                startActivity(ActivityMain.getVipManagementIntent(this))
                true
            }

            R.id.create -> {
                AdManager.showInterstitial(this) { success ->
                    if (success) {
                        Log.d("roy93~", "Ad đã hiển thị và đóng thành công")
                    } else {
                        Log.d("roy93~", "Ad không hiển thị được hoặc có lỗi")
                    }
                    createBarcode()
                }
                true
            }

            R.id.history -> {
                AdManager.showInterstitial(this) { success ->
                    if (success) {
                        Log.d("roy93~", "Ad đã hiển thị và đóng thành công")
                    } else {
                        Log.d("roy93~", "Ad không hiển thị được hoặc có lỗi")
                    }
                    startActivity(ActivityMain.getHistoryIntent(this))
                }
                true
            }

            R.id.pickFile -> {
                startActivityForResult(
                    Intent.createChooser(
                        Intent(Intent.ACTION_GET_CONTENT).apply {
                            type = "image/*"
                        }, getString(R.string.pick_file)
                    ), PICK_FILE_RESULT_CODE
                )
                true
            }

            R.id.switchCamera -> {
                switchCamera()
                true
            }

            R.id.bulkMode -> {
                bulkMode = bulkMode xor true
                item.isChecked = bulkMode
                ignoreNext = null
                true
            }

            R.id.restrictFormat -> {
                showRestrictionDialog()
                true
            }

            R.id.preferences -> {
                AdManager.showInterstitial(this) { success ->
                    if (success) {
                        Log.d("roy93~", "Ad đã hiển thị và đóng thành công")
                    } else {
                        Log.d("roy93~", "Ad không hiển thị được hoặc có lỗi")
                    }
                    startActivity(ActivityMain.getPreferencesIntent(this))
                }
                true
            }

            R.id.info -> {
                toast("Version ${BuildConfig.VERSION_NAME}")
                true
            }

            R.id.menuRate -> {
                rateApp(packageName)
                true
            }

            R.id.menuMore -> {
                moreApp()
                true
            }

            R.id.menuShare -> {
                shareApp()
                true
            }

            R.id.menuPolicy -> {
                openBrowserPolicy()
                true
            }

            R.id.menuBetaTester -> {
//                openUrlInBrowser("https://github.com/gj-loitp/20-TESTER-FOR-CLOSED-TESTING")
                rateApp("com.mckimquyen.bemytester")
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun createBarcode() {
        startActivity(ActivityMain.getEncodeIntent(this))
    }

    private fun switchCamera() {
        closeCamera()
        frontFacing = frontFacing xor true
        openCamera()
    }

    private fun showRestrictionDialog() {
        val names = resources.getStringArray(
            R.array.barcodeFormatsNames
        ).toMutableList()
        val formats = resources.getStringArray(
            R.array.barcodeFormatsValues
        ).toMutableList()
        if (restrictFormat != null) {
            names.add(0, getString(R.string.remove_restriction))
            formats.add(0, null)
        }
        AlertDialog.Builder(this).apply {
            setTitle(R.string.restrict_format)
            setItems(names.toTypedArray()) { _, which ->
                restrictFormat = formats[which]
                updateHints()
            }
            show()
        }
    }

//	private fun openReadme() {
//		val intent = Intent(
//			Intent.ACTION_VIEW,
//			Uri.parse(getString(R.string.project_url))
//		)
//		execShareIntent(intent)
//	}

    private fun handleSendText(intent: Intent) {
        val text = intent.getStringExtra(Intent.EXTRA_TEXT)
        if (text?.isEmpty() == false) {
            startActivity(ActivityMain.getEncodeIntent(this, text, true))
            finish()
        }
    }

    private fun initCameraView() {
        cameraView.setUseOrientationListener(true)
        @Suppress("ClickableViewAccessibility") cameraView.setOnTouchListener(object : View.OnTouchListener {
            var focus = true
            var offset = -1f
            var progress = 0

            override fun onTouch(v: View?, event: MotionEvent?): Boolean {
                event ?: return false
                val pos = event.y
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        offset = pos
                        progress = zoomBar.progress
                        return true
                    }

                    MotionEvent.ACTION_MOVE -> {
                        if (prefs.zoomBySwiping) {
                            v ?: return false
                            val dist = offset - pos
                            val maxValue = zoomBar.max
                            val change = maxValue / v.height.toFloat() * 2f * dist
                            zoomBar.progress = min(
                                maxValue, max(progress + change.roundToInt(), 0)
                            )
                            return true
                        }
                    }

                    MotionEvent.ACTION_UP -> {
                        // Stop calling focusTo() as soon as it returns false
                        // to avoid throwing and catching future exceptions.
                        if (focus) {
                            focus = cameraView.focusTo(v, event.x, event.y)
                            if (focus) {
                                v?.performClick()
                                return true
                            }
                        }
                    }
                }
                return false
            }
        })
        @Suppress("DEPRECATION") cameraView.setOnCameraListener(object : CameraView.OnCameraListener {
            override fun onConfigureParameters(
                parameters: Camera.Parameters,
            ) {
                zoomBar.visibility = if (parameters.isZoomSupported) {
                    val max = parameters.maxZoom
                    if (zoomBar.max != max) {
                        zoomBar.max = max
                        zoomBar.progress = max / 10
                        saveZoom()
                    }
                    parameters.zoom = zoomBar.progress
                    View.VISIBLE
                } else {
                    View.GONE
                }
                val sceneModes = parameters.supportedSceneModes
                sceneModes?.let {
                    for (mode in sceneModes) {
                        if (mode == Camera.Parameters.SCENE_MODE_BARCODE) {
                            parameters.sceneMode = mode
                            break
                        }
                    }
                }
                CameraView.setAutoFocus(parameters)
                updateFlashFab(parameters.flashMode == null)
            }

            override fun onCameraError() {
                this@CameraActivity.toast(R.string.cameraError)
            }

            override fun onCameraReady(camera: Camera) {
                frameMetrics = FrameMetrics(
                    cameraView.frameWidth, cameraView.frameHeight, cameraView.frameOrientation
                )
                updateFrameRoiAndMappingMatrix()
                ignoreNext = null
                decoding = true
                // These settings can't change while the camera is open.
                val readerOptions = ReaderOptions()
                readerOptions.tryHarder = prefs.tryHarder
                readerOptions.tryRotate = prefs.autoRotate
                readerOptions.tryInvert = true
                readerOptions.tryDownscale = true
                readerOptions.maxNumberOfSymbols = 1
                readerOptions.formats = formatsToRead.mapNotNull {
                    try {
                        ZxingCpp.BarcodeFormat.valueOf(it)
                    } catch (e: Exception) {
                        null
                    }
                }.toSet()

                var useLocalAverage = false
                camera.setPreviewCallback { frameData, _ ->
                    if (decoding) {
                        useLocalAverage = useLocalAverage xor true
                        // By default, ZXing uses LOCAL_AVERAGE, but
                        // this does not work well with inverted
                        // barcodes on low-contrast backgrounds.
                        readerOptions.binarizer = if (useLocalAverage) {
                            Binarizer.LOCAL_AVERAGE
                        } else {
                            Binarizer.GLOBAL_HISTOGRAM
                        }

                        ZxingCpp.readByteArray(
                            yuvData = frameData,
                            rowStride = frameMetrics.width,
                            left = frameRoi.left,
                            top = frameRoi.top,
                            width = frameRoi.width(),
                            height = frameRoi.height(),
                            rotation = frameMetrics.orientation,
                            options = readerOptions
                        )?.let { results ->
                            val result = results.first()
                            if (result.text != ignoreNext) {
                                postResult(result)
                                decoding = false
                            }
                        }
                    }
                }
            }

            override fun onPreviewStarted(camera: Camera) {
            }

            override fun onCameraStopping(camera: Camera) {
                camera.setPreviewCallback(null)
            }
        })
    }

    private fun initZoomBar() {
        zoomBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(
                seekBar: SeekBar,
                progress: Int,
                fromUser: Boolean,
            ) {
                cameraView.camera?.setZoom(progress)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {}

            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })
        restoreZoom()
    }

    @Suppress("DEPRECATION")
    private fun Camera.setZoom(zoom: Int) {
        try {
            val params = parameters
            params.zoom = zoom
            parameters = params
        } catch (e: RuntimeException) {
            e.printStackTrace()
            // Ignore. There's nothing we can do.
        }
    }

    private fun saveZoom() {
        val editor = prefs.preferences.edit()
        editor.putInt(ZOOM_MAX, zoomBar.max)
        editor.putInt(ZOOM_LEVEL, zoomBar.progress)
        editor.apply()
    }

    private fun restoreZoom() {
        zoomBar.max = prefs.preferences.getInt(ZOOM_MAX, zoomBar.max)
        zoomBar.progress = prefs.preferences.getInt(/* key = */ ZOOM_LEVEL,/* defValue = */ zoomBar.progress
        )
    }

    private fun initDetectorView() {
        detectorView.onRoiChange = {
            decoding = false
        }
        detectorView.onRoiChanged = {
            decoding = true
            updateFrameRoiAndMappingMatrix()
        }
        detectorView.setPaddingFromWindowInsets()
        detectorView.restoreCropHandlePos()
    }

    private fun updateFrameRoiAndMappingMatrix() {
        val viewRect = cameraView.previewRect
        val viewRoi = if (detectorView.roi.width() < 1) {
            viewRect
        } else {
            detectorView.roi
        }
        frameRoi.setFrameRoi(
            frameMetrics = frameMetrics, viewRect = viewRect, viewRoi = viewRoi
        )
        matrix.setFrameToView(
            frameMetrics = frameMetrics, viewRect = viewRect, viewRoi = viewRoi
        )
    }

    private fun updateFlashFab(unavailable: Boolean) {
        if (unavailable) {
            flashFab.setImageResource(R.drawable.ic_action_create)
            flashFab.setOnClickListener { createBarcode() }
        } else {
            flashFab.setImageResource(R.drawable.ic_action_flash)
            flashFab.setOnClickListener { toggleTorchMode() }
        }
    }

    @Suppress("DEPRECATION")
    private fun toggleTorchMode() {
        val camera = cameraView.camera ?: return
        val parameters = camera.parameters ?: return
        parameters.flashMode = if (parameters.flashMode != Camera.Parameters.FLASH_MODE_OFF) {
            Camera.Parameters.FLASH_MODE_OFF
        } else {
            Camera.Parameters.FLASH_MODE_TORCH
        }
        try {
            camera.parameters = parameters
        } catch (e: RuntimeException) {
            toast(e.message ?: getString(R.string.error_flash))
        }
    }

    private fun postResult(result: Result) {
        cameraView.post {
            detectorView.update(
                matrix.mapPosition(
                    position = result.position, coords = detectorView.coordinates
                )
            )
            scanFeedback()
            val returnUri = returnUrlTemplate?.let {
                try {
                    completeUrl(it, result)
                } catch (e: Exception) {
                    e.message?.let { message ->
                        toast(message)
                    }
                    null
                }
            }
            when {
                returnResult -> {
                    setResult(RESULT_OK, getReturnIntent(result))
                    finish()
                }

                returnUri != null -> execShareIntent(
                    Intent(Intent.ACTION_VIEW, returnUri)
                )

                else -> {
                    showResult(result, bulkMode)
                    // If this app was invoked via a deep link but without
                    // a return URI, we probably don't want to return to
                    // the camera screen after scanning, but to the caller.
                    if (finishAfterShowingResult) {
                        finish()
                    }
                }
            }
            if (bulkMode) {
                if (prefs.ignoreConsecutiveDuplicates) {
                    ignoreNext = result.text
                }
                if (prefs.showToastInBulkMode) {
                    toast(result.text)
                }
                detectorView.postDelayed({
                    decoding = true
                }, prefs.bulkModeDelay.toLong())
            }
        }
    }

    companion object {
        private const val PICK_FILE_RESULT_CODE = 1
        private const val ZOOM_MAX = "zoom_max"
        private const val ZOOM_LEVEL = "zoom_level"
        private const val FRONT_FACING = "front_facing"
        private const val BULK_MODE = "bulk_mode"
        private const val RESTRICT_FORMAT = "restrict_format"
    }

}

fun Activity.showResult(
    result: Result,
    bulkMode: Boolean = false,
) {
    if (prefs.copyImmediately) {
        copyToClipboard(result.text)
    }
    val scan = result.toScan()
    if (prefs.useHistory) {
        scan.id = db.insertScan(scan)
    }
    if (prefs.sendScanActive && prefs.sendScanUrl.isNotEmpty()) {
        if (prefs.sendScanType == "4") {
            openUrl(
                prefs.sendScanUrl + scan.content.urlEncode()
            )
            return
        }
        // [FIX ML-4] Dung lifecycleScope thay vi GlobalScope
        // Request se bi cancel khi Activity bi destroy
        val scope = (this as? androidx.lifecycle.LifecycleOwner)?.lifecycleScope
            ?: kotlinx.coroutines.MainScope()
        scan.sendAsync(
            prefs.sendScanUrl, prefs.sendScanType, scope
        ) { code, body ->
            if (code == null || code < 200 || code > 299) {
                errorFeedback()
            }
            if (!body.isNullOrEmpty()) {
                toast(body)
            } else if (code == null || code > 299) {
                toast(R.string.background_request_failed)
            }
        }
    }
    if (prefs.sendScanBluetooth && prefs.sendScanBluetoothHost.isNotEmpty() && hasBluetoothPermission()) {
        val scope = (this as? androidx.lifecycle.LifecycleOwner)?.lifecycleScope
            ?: kotlinx.coroutines.MainScope()
        scan.sendBluetoothAsync(
            prefs.sendScanBluetoothHost, scope
        ) { connected, sent ->
            toast(
                when {
                    !connected -> {
                        errorFeedback()
                        R.string.bluetooth_connect_fail
                    }

                    !sent -> {
                        errorFeedback()
                        R.string.bluetooth_send_fail
                    }

                    else -> R.string.bluetooth_send_success
                }
            )
        }
    }
    if (!bulkMode) {
        startActivity(
            ActivityMain.getDecodeIntent(this, scan)
        )
    }
}

private fun getReturnIntent(result: Result) = Intent().apply {
    putExtra("SCAN_RESULT", result.text)
    putExtra("SCAN_RESULT_FORMAT", result.format)
    putExtra("SCAN_RESULT_ORIENTATION", result.orientation)
    putExtra("SCAN_RESULT_ERROR_CORRECTION_LEVEL", result.ecLevel)
    if (result.rawBytes.isNotEmpty()) {
        putExtra("SCAN_RESULT_BYTES", result.rawBytes)
    }
}

private fun String.isReturnUrl() = listOf(
    "binaryeye://scan", "http://markusfisch.de/BinaryEye", "https://markusfisch.de/BinaryEye"
).firstOrNull { startsWith(it) } != null

@OptIn(ExperimentalStdlibApi::class)
private fun completeUrl(urlTemplate: String, result: Result) = Uri.parse(
    urlTemplate.replace("{RESULT}", result.text.urlEncode()).replace("{RESULT_BYTES}", result.rawBytes.toHexString())
        .replace(
            "{FORMAT}", result.format.name.urlEncode()
        )
        // And support {CODE} from the old ZXing app, too.
        .replace("{CODE}", result.text.urlEncode())
)
