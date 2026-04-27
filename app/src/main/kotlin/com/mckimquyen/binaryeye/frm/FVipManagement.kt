package com.mckimquyen.binaryeye.frm

import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.CountDownTimer
import android.os.VibrationEffect
import android.os.Vibrator
import android.text.Editable
import android.text.TextWatcher
import android.text.format.DateFormat
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import android.widget.EditText
import com.mckimquyen.binaryeye.R
import com.mckimquyen.binaryeye.view.setPaddingFromWindowInsets
import com.roy.sdkadbmob.AdError
import com.roy.sdkadbmob.AdManager
import com.roy.sdkadbmob.RewardedAdListener
import com.roy.sdkadbmob.SafeLogger
import java.lang.ref.WeakReference
import java.text.NumberFormat
import java.util.Currency
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.random.Random

class FVipManagement : Fragment() {

    private companion object {
        const val TAG = "FVipManagement"
    }

    // Views
    private lateinit var tvVipStatus: TextView
    private lateinit var tvVipExpiry: TextView
    private lateinit var tvVipActivated: TextView
    private lateinit var tvCountdown: TextView
    private lateinit var progressVip: ProgressBar
    private lateinit var ivVipIcon: ImageView
    private lateinit var etVipKey: EditText
    private lateinit var ivClearKey: ImageView
    private lateinit var btnSubmitKey: Button
    private lateinit var btnCancelVip: Button
    private lateinit var rowWatchAd: View
    private lateinit var tvWatchAdStatus: TextView
    private lateinit var layoutVipDetail: View
    private lateinit var heroContainer: FrameLayout
    private lateinit var viewGlowSweep: View
    private lateinit var chipStatus: View
    private lateinit var tvChipText: TextView
    private lateinit var ivChipIcon: ImageView
    private lateinit var tvFreeStatusHint: TextView
    private lateinit var viewHeroGlow: View
    private lateinit var featuresCard: View
    private lateinit var planRow30: View
    private lateinit var planRow90: View
    private lateinit var planRow1Y: View
    private lateinit var planRowLifetime: View
    private lateinit var tvProgressCaption: TextView

    // Animators
    private var shimmerAnim: ObjectAnimator? = null
    private var progressAnim: ObjectAnimator? = null
    private var glowAnim: ObjectAnimator? = null
    private var heroGlowAnim: ObjectAnimator? = null
    private var watchAdPulseAnim: ObjectAnimator? = null
    private var countDownTimer: CountDownTimer? = null
    private var pendingVipReward = false
    private var hasPlayedEntrance = false

    private val VIP_SECRET  = "OWZBMHE3ZU4hMjdjTHgwNEAyMTk5M1kydTBJNyNRMA=="
    private val KEY_3_DAYS  = "eQ7@93L0f!2Y2707xN04021993u0I#2aK"
    private val KEY_30_DAYS = "9fA0q7eN!27cLx04@21993Y2u0I7#Q0"

    // Metadata cho progress (SDK chỉ lưu expiry, ko có start time)
    private val META_PREF = "vip_meta"
    private val KEY_VIP_STARTED_AT = "vip_started_at"
    private val KEY_VIP_DURATION_MS = "vip_duration_ms"

    private fun vipMeta() = requireContext().getSharedPreferences(META_PREF, Context.MODE_PRIVATE)
    private fun saveVipMeta(days: Int) {
        vipMeta().edit()
            .putLong(KEY_VIP_STARTED_AT, System.currentTimeMillis())
            .putLong(KEY_VIP_DURATION_MS, days * 86_400_000L)
            .apply()
        SafeLogger.d(TAG, "saveVipMeta — days=$days")
    }
    private fun clearVipMeta() {
        vipMeta().edit().clear().apply()
        SafeLogger.d(TAG, "clearVipMeta")
    }

    // Heuristic dùng cho VIP đã activate trước khi metadata tồn tại
    private fun estimateDurationMs(remainingMs: Long): Long {
        val remainingDays = (remainingMs.toFloat() / 86_400_000L).toInt()
        val totalDays = when {
            remainingDays > 365 -> 36500L
            remainingDays > 90 -> 365L
            remainingDays > 30 -> 90L
            remainingDays > 3 -> 30L
            else -> 3L
        }
        return totalDays * 86_400_000L
    }

    // ── Context ────────────────────────────────────────────────────────

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        SafeLogger.d(TAG, "onCreateView")
        val themedCtx = android.view.ContextThemeWrapper(
            requireContext(),
            com.google.android.material.R.style.Theme_Material3_DayNight_NoActionBar
        )
        return inflater.cloneInContext(themedCtx).inflate(R.layout.roy_frm_vip_management, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        SafeLogger.d(TAG, "onViewCreated")
        activity?.setTitle(getString(R.string.vip_title))

        view.findViewById<ScrollView>(R.id.scrollVip).setPaddingFromWindowInsets()

        tvVipStatus       = view.findViewById(R.id.tvVipStatus)
        tvVipExpiry       = view.findViewById(R.id.tvVipExpiry)
        tvVipActivated    = view.findViewById(R.id.tvVipActivated)
        tvCountdown       = view.findViewById(R.id.tvCountdown)
        progressVip       = view.findViewById(R.id.progressVip)
        ivVipIcon         = view.findViewById(R.id.ivVipIcon)
        etVipKey          = view.findViewById(R.id.etVipKey)
        ivClearKey        = view.findViewById(R.id.ivClearKey)
        btnSubmitKey      = view.findViewById(R.id.btnSubmitKey)
        btnCancelVip      = view.findViewById(R.id.btnCancelVip)
        rowWatchAd        = view.findViewById(R.id.rowWatchAd)
        tvWatchAdStatus   = view.findViewById(R.id.tvWatchAdStatus)
        layoutVipDetail   = view.findViewById(R.id.layoutVipDetail)
        heroContainer     = view.findViewById(R.id.heroContainer)
        viewGlowSweep     = view.findViewById(R.id.viewGlowSweep)
        chipStatus        = view.findViewById(R.id.chipStatus)
        tvChipText        = view.findViewById(R.id.tvChipText)
        ivChipIcon        = view.findViewById(R.id.ivChipIcon)
        tvFreeStatusHint  = view.findViewById(R.id.tvFreeStatusHint)
        viewHeroGlow      = view.findViewById(R.id.viewHeroGlow)
        featuresCard      = view.findViewById(R.id.featuresCard)
        planRow30         = view.findViewById(R.id.planRow30)
        planRow90         = view.findViewById(R.id.planRow90)
        planRow1Y         = view.findViewById(R.id.planRow1Y)
        planRowLifetime   = view.findViewById(R.id.planRowLifetime)
        tvProgressCaption = view.findViewById(R.id.tvProgressCaption)

        setPriceLabels()
        setupKeyInput()
        setupCancelButton()
        setupWatchAdRow()
        setupRewardedListener()
        runStaggerEntrance()

        // Khi focus EditText: scroll để CẢ KHỐI key input (input + button "Kích hoạt") nằm trên keyboard.
        // initSystemBars dùng FLAG_LAYOUT_FULLSCREEN + setPaddingFromWindowInsets nên scrollVip
        // chỉ thực sự shrink khi IME đã bố trí xong → dùng OnGlobalLayoutListener listener pattern.
        val keyCard = view.findViewById<View>(R.id.keyInputCard)
        val scrollView = view.findViewById<ScrollView>(R.id.scrollVip)
        etVipKey.setOnFocusChangeListener { _, hasFocus ->
            SafeLogger.d(TAG, "roy93~ etVipKey focus=$hasFocus")
            if (!hasFocus) return@setOnFocusChangeListener
            installKeyboardScrollListener(scrollView, keyCard)
        }

        AdManager.loadRewarded(requireContext())
        SafeLogger.d(TAG, "loadRewarded called")

        refreshStatus(celebration = false)
    }

    override fun onCreateAnimation(transit: Int, enter: Boolean, nextAnim: Int): Animation? =
        if (enter) AnimationUtils.loadAnimation(requireContext(), R.anim.roy_item_anim_slide_up)
        else super.onCreateAnimation(transit, enter, nextAnim)

    override fun onResume() {
        super.onResume()
        SafeLogger.d(TAG, "onResume — isVip=${AdManager.isVipByKeyActive()}")
        refreshStatus(celebration = false)
        if (pendingVipReward) {
            pendingVipReward = false
            refreshStatus(celebration = true)
            showBottomSheetDialog(
                getString(R.string.vip_sheet_reward_title),
                getString(R.string.vip_sheet_reward_msg),
                true,
                getString(R.string.vip_btn_great),
                null,
                {},
            )
        }
        startCrownShimmer()
        startHeroGlowRotation()
    }

    override fun onPause() {
        super.onPause()
        SafeLogger.d(TAG, "onPause")
        shimmerAnim?.cancel()
        progressAnim?.cancel()
        glowAnim?.cancel()
        heroGlowAnim?.cancel()
        watchAdPulseAnim?.cancel()
        countDownTimer?.cancel()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        SafeLogger.d(TAG, "onDestroyView — clearing rewardedListener")
        shimmerAnim?.cancel()
        progressAnim?.cancel()
        glowAnim?.cancel()
        heroGlowAnim?.cancel()
        watchAdPulseAnim?.cancel()
        countDownTimer?.cancel()
        removeKeyboardScrollListener()
        AdManager.rewardedListener = null
    }

    // ── Keyboard scroll handler ───────────────────────────────────────

    private var keyboardScrollListener: android.view.ViewTreeObserver.OnGlobalLayoutListener? = null
    private var lastVisibleHeight = 0

    private fun installKeyboardScrollListener(scrollView: ScrollView, keyCard: View) {
        if (keyboardScrollListener != null) return // already installed
        val rootView = scrollView.rootView
        val r = android.graphics.Rect()
        keyboardScrollListener = android.view.ViewTreeObserver.OnGlobalLayoutListener {
            if (!isAdded) return@OnGlobalLayoutListener
            rootView.getWindowVisibleDisplayFrame(r)
            val visibleHeight = r.bottom - r.top
            if (visibleHeight == lastVisibleHeight) return@OnGlobalLayoutListener
            val rootHeight = rootView.height
            val isImeShown = rootHeight - visibleHeight > rootHeight * 0.15f
            SafeLogger.d(TAG, "roy93~ globalLayout — visibleH=$visibleHeight rootH=$rootHeight imeShown=$isImeShown")
            lastVisibleHeight = visibleHeight
            if (isImeShown) scrollKeyCardAboveKeyboard(scrollView, keyCard, r.bottom)
        }
        rootView.viewTreeObserver.addOnGlobalLayoutListener(keyboardScrollListener)
        SafeLogger.d(TAG, "roy93~ installKeyboardScrollListener")
    }

    private fun scrollKeyCardAboveKeyboard(scrollView: ScrollView, keyCard: View, imeTopY: Int) {
        scrollView.post {
            if (!isAdded) return@post
            val cardLoc = IntArray(2)
            keyCard.getLocationInWindow(cardLoc)
            val cardBottomOnScreen = cardLoc[1] + keyCard.height
            val overlap = cardBottomOnScreen - imeTopY
            SafeLogger.d(TAG, "roy93~ scrollKeyCard — cardTop=${cardLoc[1]} cardBottom=$cardBottomOnScreen imeTopY=$imeTopY overlap=$overlap currentScrollY=${scrollView.scrollY}")
            if (overlap > 0) {
                val targetY = scrollView.scrollY + overlap + 24 // +24dp padding để button không sát keyboard
                scrollView.smoothScrollTo(0, targetY)
                SafeLogger.d(TAG, "roy93~ scrollKeyCard → smoothScrollTo y=$targetY")
            } else {
                SafeLogger.d(TAG, "roy93~ scrollKeyCard — no overlap, skip")
            }
        }
    }

    private fun removeKeyboardScrollListener() {
        keyboardScrollListener?.let {
            view?.rootView?.viewTreeObserver?.removeOnGlobalLayoutListener(it)
            keyboardScrollListener = null
            lastVisibleHeight = 0
            SafeLogger.d(TAG, "roy93~ removeKeyboardScrollListener")
        }
    }

    // ── Stagger entrance ───────────────────────────────────────────────

    private fun runStaggerEntrance() {
        if (hasPlayedEntrance) return
        hasPlayedEntrance = true
        val targets = listOf(featuresCard, rowWatchAd, planRow30, planRow90, planRow1Y, planRowLifetime)
        targets.forEachIndexed { i, v ->
            v.alpha = 0f
            v.translationY = 40f
            v.animate()
                .alpha(1f)
                .translationY(0f)
                .setStartDelay(120L + i * 70L)
                .setDuration(380L)
                .setInterpolator(android.view.animation.DecelerateInterpolator())
                .start()
        }
    }

    // ── Hero glow rotation ────────────────────────────────────────────

    private fun startHeroGlowRotation() {
        heroGlowAnim?.cancel()
        heroGlowAnim = ObjectAnimator.ofFloat(viewHeroGlow, "rotation", 0f, 360f).apply {
            duration = 12_000L
            repeatCount = ObjectAnimator.INFINITE
            repeatMode = ObjectAnimator.RESTART
            interpolator = android.view.animation.LinearInterpolator()
            start()
        }
    }

    // ── Watch ad pulse ────────────────────────────────────────────────

    private fun startWatchAdPulse() {
        watchAdPulseAnim?.cancel()
        if (!isAdded || rowWatchAd.visibility != View.VISIBLE) return
        watchAdPulseAnim = ObjectAnimator.ofPropertyValuesHolder(
            rowWatchAd,
            android.animation.PropertyValuesHolder.ofFloat("scaleX", 1f, 1.02f, 1f),
            android.animation.PropertyValuesHolder.ofFloat("scaleY", 1f, 1.02f, 1f),
        ).apply {
            duration = 1500L
            repeatCount = ObjectAnimator.INFINITE
            repeatMode = ObjectAnimator.RESTART
            start()
        }
    }

    // ── Status chip toggle ────────────────────────────────────────────

    private fun applyStatusChip(isVip: Boolean) {
        if (isVip) {
            chipStatus.setBackgroundResource(R.drawable.roy_bg_status_chip_gold)
            tvChipText.text = getString(R.string.vip_chip_active)
            tvChipText.setTextColor(0xFFFFD700.toInt())
            ivChipIcon.setImageResource(R.drawable.ic_sparkle)
        } else {
            chipStatus.setBackgroundResource(R.drawable.roy_bg_status_chip)
            tvChipText.text = getString(R.string.vip_chip_free)
            tvChipText.setTextColor(0xFFFFFFFF.toInt())
            ivChipIcon.setImageResource(R.drawable.ic_lock_outline)
        }
    }

    // ── Price labels ───────────────────────────────────────────────────

    private fun setPriceLabels() {
        view?.findViewById<TextView>(R.id.tvPrice30)?.text       = getString(R.string.vip_price_30, formatPrice(0.50))
        view?.findViewById<TextView>(R.id.tvPrice90)?.text       = getString(R.string.vip_price_90, formatPrice(1.00))
        view?.findViewById<TextView>(R.id.tvPrice1Y)?.text       = getString(R.string.vip_price_1y, formatPrice(2.00))
        view?.findViewById<TextView>(R.id.tvPriceLifetime)?.text = getString(R.string.vip_price_lifetime, formatPrice(3.00))
    }

    private fun formatPrice(usd: Double): String = try {
        val country = Locale.getDefault().country
        val (amount, prefix, suffix) = when (country) {
            "VN" -> Triple(usd * 25_400,  "",   "đ")
            "JP" -> Triple(usd * 150.0,   "¥",  "")
            "KR" -> Triple(usd * 1_350.0, "₩",  "")
            "IN" -> Triple(usd * 83.0,    "₹",  "")
            "TH" -> Triple(usd * 35.0,    "฿",  "")
            "CN" -> Triple(usd * 7.2,     "¥",  "")
            "GB" -> Triple(usd * 0.79,    "£",  "")
            "DE","FR","IT","ES","AT","NL","PT","BE" -> Triple(usd * 0.92, "€", "")
            else -> Triple(usd, "$", "")
        }
        val fmt = NumberFormat.getNumberInstance(Locale.getDefault()).apply {
            maximumFractionDigits = if (amount >= 100) 0 else 2
            minimumFractionDigits = 0
        }
        "~$prefix${fmt.format(amount)}$suffix"
    } catch (e: Exception) {
        SafeLogger.w(TAG, "formatPrice error: ${e.message}")
        "~\$${"%.2f".format(usd)}"
    }

    // ── Inline Key Input ───────────────────────────────────────────────

    private fun setupKeyInput() {
        btnSubmitKey.isEnabled = false
        etVipKey.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val hasText = !s.isNullOrBlank()
                btnSubmitKey.isEnabled = hasText
                
                if (hasText && ivClearKey.visibility == View.GONE) {
                    ivClearKey.alpha = 0f
                    ivClearKey.visibility = View.VISIBLE
                    ivClearKey.animate().alpha(1f).setDuration(200).start()
                } else if (!hasText && ivClearKey.visibility == View.VISIBLE) {
                    ivClearKey.animate().alpha(0f).setDuration(200).withEndAction {
                        ivClearKey.visibility = View.GONE
                    }.start()
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        ivClearKey.setOnClickListener {
            etVipKey.setText("")
        }

        etVipKey.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH) {
                btnSubmitKey.performClick()
                true
            } else {
                false
            }
        }

        btnSubmitKey.setOnClickListener {
            val key = etVipKey.text.toString().trim()
            SafeLogger.d(TAG, "btnSubmitKey clicked — key length=${key.length}")
            if (key.isEmpty()) return@setOnClickListener
            hideKeyboard()
            val days = when (key) {
                KEY_30_DAYS -> 30
                KEY_3_DAYS  -> 3
                else -> {
                    SafeLogger.w(TAG, "btnSubmitKey — key không hợp lệ")
                    showBottomSheetDialog(
                        getString(R.string.vip_sheet_invalid_title),
                        getString(R.string.vip_sheet_invalid_msg),
                        false,
                        getString(R.string.vip_btn_close),
                        null,
                        {},
                    )
                    return@setOnClickListener
                }
            }
            SafeLogger.d(TAG, "btnSubmitKey — activating VIP $days days")
            val ok = AdManager.activateVipByKey(requireContext(), VIP_SECRET, days)
            if (ok) {
                saveVipMeta(days)
                etVipKey.setText("")
                refreshStatus(celebration = true)

                val expiryStr = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.US)
                    .format(java.util.Date(AdManager.getVipByKeyExpiry()))
                showBottomSheetDialog(
                    getString(R.string.vip_sheet_activated_title),
                    getString(R.string.vip_sheet_activated_msg, days, expiryStr),
                    true,
                    getString(R.string.vip_btn_ok),
                    null,
                    {},
                )
            } else {
                SafeLogger.w(TAG, "btnSubmitKey — activateVipByKey returned false")
                showBottomSheetDialog(
                    getString(R.string.vip_sheet_system_error_title),
                    getString(R.string.vip_sheet_system_error_msg),
                    false,
                    getString(R.string.vip_btn_close),
                    null,
                    {},
                )
            }
        }
    }

    // ── Cancel VIP ─────────────────────────────────────────────────────

    private fun setupCancelButton() {
        btnCancelVip.setOnClickListener {
            SafeLogger.d(TAG, "btnCancelVip clicked")
            showBottomSheet(
                getString(R.string.vip_sheet_cancel_title),
                getString(R.string.vip_sheet_cancel_msg),
                SheetType.WARNING,
                getString(R.string.vip_btn_confirm_cancel),
                getString(R.string.vip_btn_keep),
                {
                    SafeLogger.d(TAG, "clearVipByKey confirmed")
                    AdManager.clearVipByKey()
                    clearVipMeta()
                    AdManager.loadRewarded(requireContext())
                    refreshStatus(celebration = false)
                    showBottomSheetDialog(
                        getString(R.string.vip_sheet_cancelled_title),
                        getString(R.string.vip_sheet_cancelled_msg),
                        true,
                        getString(R.string.vip_btn_ok),
                        null,
                        {},
                    )
                }
            )
        }
    }

    // ── Watch Ad Row ───────────────────────────────────────────────────
    // FIX: SDK skip showRewarded khi isVIPMember=true → fallback sang showInterstitial
    // nên set click listener ở đây, không đặt trong onAdLoaded

    private fun setupWatchAdRow() {
        SafeLogger.d(TAG, "setupWatchAdRow — isVip=${AdManager.isVipByKeyActive()}")
        rowWatchAd.isClickable = false
        rowWatchAd.alpha = 0.6f

        rowWatchAd.setOnClickListener {
            SafeLogger.d(TAG, "rowWatchAd clicked — isVip=${AdManager.isVipByKeyActive()}")
            rowWatchAd.isClickable = false
            tvWatchAdStatus.text = getString(R.string.vip_plan_watch_sub_opening)

            if (AdManager.isVipByKeyActive()) {
                SafeLogger.d(TAG, "rowWatchAd — VIP active, fallback to interstitial")
                AdManager.showInterstitial(requireActivity()) { shown ->
                    SafeLogger.d(TAG, "rowWatchAd — interstitial shown=$shown")
                    if (isAdded) {
                        if (shown) {
                            val ok = AdManager.activateVipByKey(requireContext(), VIP_SECRET, 3)
                            SafeLogger.d(TAG, "rowWatchAd — fallback activate ok=$ok")
                            if (ok) {
                                saveVipMeta(3)
                                refreshStatus(celebration = true)
                                showBottomSheetDialog(
                                    getString(R.string.vip_sheet_extra_3_title),
                                    getString(R.string.vip_sheet_extra_3_msg),
                                    true,
                                    getString(R.string.vip_btn_ok),
                                    null,
                                    {},
                                )
                            }
                        }
                        rowWatchAd.isClickable = true
                        tvWatchAdStatus.text = getString(R.string.vip_plan_watch_sub_ready)
                    }
                }
            } else {
                SafeLogger.d(TAG, "rowWatchAd — showRewarded")
                AdManager.showRewarded(requireActivity()) { earned ->
                    SafeLogger.d(TAG, "rowWatchAd — showRewarded callback earned=$earned")
                    if (!isAdded) return@showRewarded
                    if (!earned) {
                        tvWatchAdStatus.text = getString(R.string.vip_plan_watch_sub_retry)
                        rowWatchAd.isClickable = true
                    }
                }
            }
        }
    }

    // ── Rewarded Ad Listener (WeakReference → chống leak) ─────────────

    private fun setupRewardedListener() {
        val weakSelf = WeakReference(this)
        AdManager.rewardedListener = object : RewardedAdListener {
            override fun onAdLoaded() {
                SafeLogger.d(TAG, "rewardedListener.onAdLoaded")
                val f = weakSelf.get() ?: return
                if (!f.isAdded) return
                f.tvWatchAdStatus.text = f.getString(R.string.vip_plan_watch_sub_ready)
                f.rowWatchAd.isClickable = true
                f.rowWatchAd.alpha = 1f
            }

            override fun onAdFailedToLoad(error: AdError) {
                SafeLogger.w(TAG, "rewardedListener.onAdFailedToLoad: ${error}")
                val f = weakSelf.get() ?: return
                if (!f.isAdded) return
                f.tvWatchAdStatus.text = f.getString(R.string.vip_plan_watch_sub_error)
                f.rowWatchAd.isClickable = true
                f.rowWatchAd.alpha = 0.8f
                f.rowWatchAd.setOnClickListener {
                    SafeLogger.d(TAG, "rowWatchAd retry loadRewarded")
                    f.tvWatchAdStatus.text = f.getString(R.string.vip_plan_watch_sub_reloading)
                    f.rowWatchAd.isClickable = false
                    AdManager.loadRewarded(f.requireContext())
                    f.setupWatchAdRow()
                }
            }

            override fun onAdFailedToShow(error: AdError) {
                SafeLogger.w(TAG, "rewardedListener.onAdFailedToShow: ${error}")
                val f = weakSelf.get() ?: return
                if (!f.isAdded) return
                f.tvWatchAdStatus.text = f.getString(R.string.vip_plan_watch_sub_failed_show)
                f.rowWatchAd.isClickable = true
                f.rowWatchAd.alpha = 1f
            }

            override fun onAdNotAvailable() {
                SafeLogger.w(TAG, "rewardedListener.onAdNotAvailable")
                val f = weakSelf.get() ?: return
                if (!f.isAdded) return
                f.tvWatchAdStatus.text = f.getString(R.string.vip_plan_watch_sub_unavailable)
                f.rowWatchAd.isClickable = true
                f.rowWatchAd.alpha = 0.8f
                f.rowWatchAd.setOnClickListener {
                    SafeLogger.d(TAG, "rowWatchAd retry loadRewarded (not available)")
                    f.tvWatchAdStatus.text = f.getString(R.string.vip_plan_watch_sub_loading)
                    f.rowWatchAd.isClickable = false
                    AdManager.loadRewarded(f.requireContext())
                    f.setupWatchAdRow()
                }
            }

            override fun onAdDismissed() {
                SafeLogger.d(TAG, "rewardedListener.onAdDismissed")
                val f = weakSelf.get() ?: return
                if (!f.isAdded) return
                f.tvWatchAdStatus.text = f.getString(R.string.vip_plan_watch_sub_reloading)
                f.rowWatchAd.isClickable = false
                f.rowWatchAd.alpha = 0.6f
                AdManager.loadRewarded(f.requireContext())
            }

            override fun onUserEarnedReward(type: String, amount: Int) {
                SafeLogger.d(TAG, "rewardedListener.onUserEarnedReward type=$type amount=$amount")
                val f = weakSelf.get() ?: return
                if (!f.isAdded) return
                val ok = AdManager.activateVipByKey(f.requireContext(), f.VIP_SECRET, 3)
                SafeLogger.d(TAG, "onUserEarnedReward — activateVipByKey ok=$ok")
                if (ok) f.saveVipMeta(3)
                f.pendingVipReward = ok
            }
        }
    }

    // ── UI Refresh ─────────────────────────────────────────────────────

    private fun refreshStatus(celebration: Boolean) {
        if (!isAdded) return
        val isVip = AdManager.isVipByKeyActive()
        val expiryMs = AdManager.getVipByKeyExpiry()
        SafeLogger.d(TAG, "refreshStatus — isVip=$isVip celebration=$celebration expiryMs=$expiryMs")

        applyStatusChip(isVip)

        if (isVip) {
            val fmt = DateFormat.getMediumDateFormat(requireContext())
            tvVipStatus.text = "✦  ${getString(R.string.vip_status_active)}"
            tvVipStatus.setTextColor(0xFFFFD700.toInt())
            tvFreeStatusHint.visibility = View.GONE

            val meta = vipMeta()
            val startedAtPref = meta.getLong(KEY_VIP_STARTED_AT, 0L)
            val durationMsPref = meta.getLong(KEY_VIP_DURATION_MS, 0L)
            tvVipActivated.visibility = View.VISIBLE
            tvVipExpiry.text = fmt.format(Date(expiryMs))

            layoutVipDetail.visibility = View.VISIBLE
            ivVipIcon.visibility = View.VISIBLE
            view?.findViewById<View>(R.id.lottieCrown)?.visibility = View.VISIBLE

            btnCancelVip.visibility = View.VISIBLE
            rowWatchAd.visibility = View.GONE
            watchAdPulseAnim?.cancel()

            val now = System.currentTimeMillis()
            val remaining = (expiryMs - now).coerceAtLeast(0L)

            // Use real start+duration if available, otherwise heuristic
            val realDurationMs = if (durationMsPref > 0L) durationMsPref
                                 else estimateDurationMs(remaining)
            val realStart = if (startedAtPref > 0L) startedAtPref else (expiryMs - realDurationMs)
            val elapsed = (now - realStart).coerceIn(0L, realDurationMs)
            val ratioRemaining = remaining.toFloat() / realDurationMs.toFloat()
            val target = (ratioRemaining * 10_000).toInt().coerceIn(0, 10_000)
            tvVipActivated.text = fmt.format(Date(realStart))
            animateProgress(target)

            // Caption: "Đã dùng X / Y ngày · còn Z%"
            val totalDays = (realDurationMs / 86_400_000L).toInt().coerceAtLeast(1)
            val usedDays = (elapsed / 86_400_000L).toInt().coerceIn(0, totalDays)
            val remainingPct = (ratioRemaining * 100f).toInt().coerceIn(0, 100)
            tvProgressCaption.text = getString(R.string.vip_progress_caption, usedDays, totalDays, remainingPct)
            startCountdown(expiryMs)
            startGlowAnimation()
            if (celebration) { showConfetti(); playCelebration() }
        } else {
            tvVipStatus.text = getString(R.string.vip_status_free)
            tvVipStatus.setTextColor(0xFFFFFFFF.toInt())
            tvFreeStatusHint.visibility = View.VISIBLE
            layoutVipDetail.visibility = View.GONE
            ivVipIcon.visibility = View.VISIBLE
            view?.findViewById<View>(R.id.lottieCrown)?.visibility = View.GONE
            countDownTimer?.cancel()
            glowAnim?.cancel()
            tvCountdown.text = ""
            animateProgress(0)
            btnCancelVip.visibility = View.GONE
            // Hiện lại Watch Ad row khi free
            rowWatchAd.visibility = View.VISIBLE
            startWatchAdPulse()
        }
    }

    // ── Animated Progress ──────────────────────────────────────────────

    private fun animateProgress(target: Int) {
        progressAnim?.cancel()
        progressAnim = ObjectAnimator.ofInt(progressVip, "progress", progressVip.progress, target).apply {
            duration = 1200
            interpolator = android.view.animation.DecelerateInterpolator()
            start()
        }
    }

    // ── Crown shimmer ──────────────────────────────────────────────────

    private fun startCrownShimmer() {
        shimmerAnim?.cancel()
        shimmerAnim = ObjectAnimator.ofFloat(ivVipIcon, "rotation", -5f, 5f).apply {
            duration = 800; repeatCount = ObjectAnimator.INFINITE; repeatMode = ObjectAnimator.REVERSE; start()
        }
    }

    // ── Glow animation ─────────────────────────────────────────────────

    private fun startGlowAnimation() {
        glowAnim?.cancel()
        viewGlowSweep.visibility = View.VISIBLE
        viewGlowSweep.post {
            val parentWidth = (viewGlowSweep.parent as? View)?.width?.toFloat() ?: 1000f
            glowAnim = ObjectAnimator.ofFloat(viewGlowSweep, "translationX", -150f, parentWidth + 150f).apply {
                duration = 1800
                repeatCount = ObjectAnimator.INFINITE
                repeatMode = ObjectAnimator.RESTART
                start()
            }
        }
    }

    // ── Celebration ────────────────────────────────────────────────────

    private fun playCelebration() {
        ivVipIcon.scaleX = 0f; ivVipIcon.scaleY = 0f; ivVipIcon.alpha = 0f
        ivVipIcon.animate().scaleX(1.25f).scaleY(1.25f).alpha(1f).setDuration(400)
            .withEndAction { ivVipIcon.animate().scaleX(1f).scaleY(1f).setDuration(250).start() }
            .start()
        haptic()
    }

    // ── Confetti ───────────────────────────────────────────────────────

    private fun showConfetti() {
        SafeLogger.d(TAG, "showConfetti")
        val root = requireActivity().window.decorView as? ViewGroup ?: run {
            SafeLogger.w(TAG, "showConfetti — decorView not ViewGroup, skip")
            return
        }
        val colors = intArrayOf(
            Color.parseColor("#FF4081"), Color.parseColor("#FFD700"),
            Color.parseColor("#4CAF50"), Color.parseColor("#2196F3"),
            Color.parseColor("#FF5722"), Color.parseColor("#E91E63"),
            Color.parseColor("#00BCD4"), Color.parseColor("#9C27B0")
        )
        val dp = resources.displayMetrics.density
        val screenW = root.width.toFloat().takeIf { it > 0f } ?: 400f

        repeat(50) { i ->
            val size = ((8 + Random.nextInt(10)) * dp).toInt()
            val dot = View(requireContext())
            GradientDrawable().apply {
                shape = if (i % 2 == 0) GradientDrawable.OVAL else GradientDrawable.RECTANGLE
                setColor(colors[i % colors.size])
                if (i % 2 != 0) cornerRadius = 4 * dp
            }.also { dot.background = it }

            root.addView(dot, FrameLayout.LayoutParams(size, size).apply { gravity = Gravity.TOP or Gravity.START })
            dot.x = Random.nextFloat() * screenW
            dot.y = -size.toFloat()
            dot.rotation = Random.nextFloat() * 360f

            val fallDuration = 1500L + Random.nextLong(1500)
            val delay = i * 40L

            ObjectAnimator.ofFloat(dot, "y", -size.toFloat(), root.height.toFloat() + size).apply {
                duration = fallDuration; startDelay = delay; start()
            }
            ObjectAnimator.ofFloat(dot, "rotation", dot.rotation, dot.rotation + Random.nextFloat() * 360f).apply {
                duration = fallDuration; startDelay = delay; start()
            }
            ObjectAnimator.ofFloat(dot, "alpha", 1f, 0f).apply {
                duration = 500; startDelay = delay + fallDuration - 500; start()
            }
            dot.postDelayed({ runCatching { root.removeView(dot) } }, delay + fallDuration + 100)
        }
    }

    // ── Countdown ─────────────────────────────────────────────────────

    private fun startCountdown(expiryMs: Long) {
        countDownTimer?.cancel()
        val remaining = expiryMs - System.currentTimeMillis()
        if (remaining <= 0) {
            SafeLogger.w(TAG, "startCountdown — already expired")
            tvCountdown.text = getString(R.string.vip_status_expired); refreshStatus(false); return
        }
        countDownTimer = object : CountDownTimer(remaining, 1000L) {
            override fun onTick(ms: Long) {
                if (!isAdded) return
                val d = TimeUnit.MILLISECONDS.toDays(ms)
                val h = TimeUnit.MILLISECONDS.toHours(ms) % 24
                val m = TimeUnit.MILLISECONDS.toMinutes(ms) % 60
                val s = TimeUnit.MILLISECONDS.toSeconds(ms) % 60
                tvCountdown.text = "${d}d ${h.toString().padStart(2,'0')}h ${m.toString().padStart(2,'0')}m ${s.toString().padStart(2,'0')}s"
                if (s == 0L) {
                    val totalMs = vipMeta().getLong(KEY_VIP_DURATION_MS, 0L).takeIf { it > 0L } ?: (30L * 86_400_000L)
                    animateProgress(((ms.toFloat() / totalMs) * 10_000).toInt().coerceIn(0, 10_000))
                }
            }
            override fun onFinish() {
                SafeLogger.d(TAG, "startCountdown — onFinish, VIP expired")
                if (!isAdded) return
                tvCountdown.text = getString(R.string.vip_status_expired); refreshStatus(false)
            }
        }.start()
    }

    // ── Bottom Sheet Dialogs ──────────────────────────────────────────

    enum class SheetType { SUCCESS, ERROR, WARNING, QUESTION }

    private fun showBottomSheetDialog(
        title: String,
        message: String,
        isSuccess: Boolean,
        positiveText: String,
        negativeText: String?,
        onPositive: () -> Unit,
    ) {
        // Map legacy boolean → enum: success=true → SUCCESS, success=false + có 2 nút → QUESTION,
        // success=false + 1 nút → ERROR. Các caller có thể được upgrade trực tiếp sau.
        val type = when {
            isSuccess -> SheetType.SUCCESS
            negativeText != null -> SheetType.QUESTION
            else -> SheetType.ERROR
        }
        showBottomSheet(title, message, type, positiveText, negativeText, onPositive)
    }

    private fun showBottomSheet(
        title: String,
        message: String,
        type: SheetType,
        positiveText: String,
        negativeText: String?,
        onPositive: () -> Unit,
    ) {
        if (!isAdded || activity?.isFinishing == true) return
        val sheet = com.google.android.material.bottomsheet.BottomSheetDialog(requireContext(), R.style.Theme_Design_BottomSheetDialog)
        val view = layoutInflater.inflate(R.layout.roy_bottom_sheet_vip, null)
        sheet.setContentView(view)

        // Fix viền xám ở 2 góc top: bottomSheet container có default theme background → set transparent
        sheet.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))
        (view.parent as? View)?.setBackgroundColor(android.graphics.Color.TRANSPARENT)

        val tvTitle = view.findViewById<TextView>(R.id.tvSheetTitle)
        val tvMsg = view.findViewById<TextView>(R.id.tvSheetMessage)
        val ivIcon = view.findViewById<ImageView>(R.id.ivSheetIcon)
        val iconCircle = view.findViewById<View>(R.id.sheetIconCircle)
        val halo = view.findViewById<View>(R.id.sheetHalo)
        val lottie = view.findViewById<com.airbnb.lottie.LottieAnimationView>(R.id.lottieSheet)
        val btnPos = view.findViewById<Button>(R.id.btnSheetPositive)
        val btnNeg = view.findViewById<Button>(R.id.btnSheetNegative)

        tvTitle.text = title
        tvMsg.text = message
        btnPos.text = positiveText

        // Icon + colors theo type
        when (type) {
            SheetType.SUCCESS -> {
                ivIcon.setImageResource(R.drawable.ic_check_circle_filled)
                iconCircle.setBackgroundResource(R.drawable.roy_bg_sheet_icon_success)
                btnPos.setBackgroundResource(R.drawable.roy_bg_btn_gradient_gold)
                btnPos.setTextColor(0xFF000000.toInt())
                lottie.visibility = View.VISIBLE
            }
            SheetType.ERROR -> {
                ivIcon.setImageResource(R.drawable.ic_error_filled)
                iconCircle.setBackgroundResource(R.drawable.roy_bg_sheet_icon_error)
                btnPos.setBackgroundResource(R.drawable.roy_bg_btn_gradient_red)
                btnPos.setTextColor(0xFFFFFFFF.toInt())
                lottie.visibility = View.GONE
            }
            SheetType.WARNING -> {
                ivIcon.setImageResource(R.drawable.ic_warning_filled)
                iconCircle.setBackgroundResource(R.drawable.roy_bg_sheet_icon_warning)
                btnPos.setBackgroundResource(R.drawable.roy_bg_btn_gradient_red)
                btnPos.setTextColor(0xFFFFFFFF.toInt())
                lottie.visibility = View.GONE
            }
            SheetType.QUESTION -> {
                ivIcon.setImageResource(R.drawable.ic_help_filled)
                iconCircle.setBackgroundResource(R.drawable.roy_bg_sheet_icon_question)
                btnPos.setBackgroundResource(R.drawable.roy_bg_btn_gradient_red)
                btnPos.setTextColor(0xFFFFFFFF.toInt())
                lottie.visibility = View.GONE
            }
        }

        // Animation: scale + fade-in cho icon stack, slide-up cho text + buttons
        animateSheetIn(iconCircle, halo, tvTitle, tvMsg, btnPos, btnNeg)

        btnPos.setOnClickListener {
            sheet.dismiss()
            onPositive()
        }

        if (negativeText != null) {
            btnNeg.visibility = View.VISIBLE
            btnNeg.text = negativeText
            btnNeg.setOnClickListener { sheet.dismiss() }
        } else {
            btnNeg.visibility = View.GONE
        }

        sheet.show()
        if (type == SheetType.SUCCESS) haptic()
    }

    private fun animateSheetIn(
        iconCircle: View,
        halo: View,
        tvTitle: View,
        tvMsg: View,
        btnPos: View,
        btnNeg: View,
    ) {
        // Icon: scale 0 → 1.15 → 1 (over-shoot) + alpha
        iconCircle.scaleX = 0f; iconCircle.scaleY = 0f; iconCircle.alpha = 0f
        iconCircle.animate()
            .scaleX(1.15f).scaleY(1.15f).alpha(1f)
            .setStartDelay(80L)
            .setDuration(360L)
            .setInterpolator(android.view.animation.OvershootInterpolator(1.6f))
            .withEndAction {
                iconCircle.animate().scaleX(1f).scaleY(1f).setDuration(220L).start()
            }
            .start()

        // Halo: slow rotation 360°
        halo.alpha = 0f
        halo.animate().alpha(1f).setDuration(500L).start()
        ObjectAnimator.ofFloat(halo, "rotation", 0f, 360f).apply {
            duration = 8000L
            repeatCount = ObjectAnimator.INFINITE
            interpolator = android.view.animation.LinearInterpolator()
            start()
        }

        // Text + buttons: slide-up + fade
        listOf(tvTitle to 200L, tvMsg to 260L, btnPos to 320L, btnNeg to 320L).forEach { (v, delay) ->
            v.alpha = 0f
            v.translationY = 30f
            v.animate()
                .alpha(1f)
                .translationY(0f)
                .setStartDelay(delay)
                .setDuration(320L)
                .setInterpolator(android.view.animation.DecelerateInterpolator())
                .start()
        }
    }

    // ── Haptic ────────────────────────────────────────────────────────

    private fun haptic() {
        try {
            val v = requireContext().getSystemService(android.content.Context.VIBRATOR_SERVICE) as? Vibrator
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O)
                v?.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
            else @Suppress("DEPRECATION") v?.vibrate(30)
        } catch (e: Exception) {
            SafeLogger.w(TAG, "haptic error: ${e.message}")
        }
    }

    // Fix #4: ẩn bàn phím
    private fun hideKeyboard() {
        try {
            val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
            val focused = requireActivity().currentFocus ?: etVipKey
            imm?.hideSoftInputFromWindow(focused.windowToken, 0)
            SafeLogger.d(TAG, "hideKeyboard OK")
        } catch (e: Exception) {
            SafeLogger.w(TAG, "hideKeyboard error: ${e.message}")
        }
    }
}
