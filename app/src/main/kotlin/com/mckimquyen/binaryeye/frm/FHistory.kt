package com.mckimquyen.binaryeye.frm

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.SearchManager
import android.content.Context
import android.database.Cursor
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import android.view.*
import android.widget.AbsListView
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ListView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.SwitchCompat
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.material.chip.ChipGroup
import com.mckimquyen.binaryeye.R
import com.mckimquyen.binaryeye.adapter.ScansAdapter
import com.mckimquyen.binaryeye.database.Db
import com.mckimquyen.binaryeye.database.ScanFilter
import com.mckimquyen.binaryeye.database.exportCsv
import com.mckimquyen.binaryeye.database.exportDatabase
import com.mckimquyen.binaryeye.database.exportJson
import com.mckimquyen.binaryeye.database.joinTags
import com.mckimquyen.binaryeye.database.parseTags
import com.mckimquyen.binaryeye.db
import com.mckimquyen.binaryeye.ext.app.addFragment
import com.mckimquyen.binaryeye.ext.app.alertDialog
import com.mckimquyen.binaryeye.ext.app.hasWritePermission
import com.mckimquyen.binaryeye.prefs
import com.mckimquyen.binaryeye.view.content.copyToClipboard
import com.mckimquyen.binaryeye.view.content.shareText
import com.mckimquyen.binaryeye.view.io.askForFileName
import com.mckimquyen.binaryeye.view.io.toSaveResult
import com.mckimquyen.binaryeye.view.lockStatusBarColor
import com.mckimquyen.binaryeye.view.setPaddingFromWindowInsets
import com.mckimquyen.binaryeye.view.systemBarListViewScrollListener
import com.mckimquyen.binaryeye.view.unlockStatusBarColor
import com.mckimquyen.binaryeye.view.useVisibility
import com.mckimquyen.binaryeye.view.widget.toast
import de.markusfisch.android.zxingcpp.ZxingCpp
import kotlinx.coroutines.*

private const val SEARCH_DEBOUNCE_MS = 300L

// [FEAT-NEW-05] Phan trang History - tranh load het toan bo scan 1 lan khi
// lich su lon, anh huong hieu nang thuc te.
private const val HISTORY_PAGE_SIZE = 100
private const val LOAD_MORE_THRESHOLD = 15

// [FEAT F3] Key luu trong DB/dung de filter - CO DINH, khong localize, khac
// voi nhan hien thi tren Chip/CheckBox (lay tu string resource) de doi ngon
// ngu khong lam vo tag da gan cho scan cu.
private const val TAG_KEY_WORK = "Work"
private const val TAG_KEY_PERSONAL = "Personal"
private const val TAG_KEY_SHOPPING = "Shopping"
private const val TAG_KEY_TRAVEL = "Travel"
private val TAG_PRESET_KEYS = setOf(TAG_KEY_WORK, TAG_KEY_PERSONAL, TAG_KEY_SHOPPING, TAG_KEY_TRAVEL)

class FHistory : Fragment() {
    private lateinit var useHistorySwitch: SwitchCompat
    private lateinit var listView: ListView
    private lateinit var fab: View
    private lateinit var progressView: View

    private lateinit var chipGroupDate: ChipGroup
    private lateinit var chipGroupFormat: ChipGroup
    private lateinit var chipGroupTag: ChipGroup

    // [FEAT F8] Lock History (VIP-exclusive)
    private lateinit var lockOverlay: View
    private var authenticated = false

    private val parentJob = Job()
    private val scope = CoroutineScope(Dispatchers.IO + parentJob)
    private var searchJob: Job? = null

    // [FEAT-NEW-05] Phan trang History
    private var loadedCount = HISTORY_PAGE_SIZE
    private var hasMorePages = true
    private var isLoadingMore = false
    private val actionModeCallback = object : ActionMode.Callback {
        override fun onCreateActionMode(
            mode: ActionMode,
            menu: Menu,
        ): Boolean {
            mode.menuInflater.inflate(
                R.menu.menu_f_history_edit,
                menu
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                lockStatusBarColor()
                val ac = activity ?: return false
                ac.window.statusBarColor = ContextCompat.getColor(
                    ac,
                    R.color.accentDark
                )
            }
            return true
        }

        override fun onPrepareActionMode(
            mode: ActionMode,
            menu: Menu,
        ): Boolean {
            return false
        }

        override fun onActionItemClicked(
            mode: ActionMode,
            item: MenuItem,
        ): Boolean {
            val ac = activity ?: return false
            return when (item.itemId) {
                R.id.copyScan -> {
                    scansAdapter?.getSelectedContent("\n")?.let {
                        ac.copyToClipboard(it)
                        ac.toast(R.string.copied_to_clipboard)
                    }
                    closeActionMode()
                    true
                }

                R.id.editScan -> {
                    scansAdapter?.forSelection { id, position ->
                        ac.askForName(
                            id,
                            scansAdapter?.getName(position),
                            scansAdapter?.getContent(position)
                        )
                    }
                    closeActionMode()
                    true
                }

                R.id.removeScan -> {
                    scansAdapter?.getSelectedIds()?.let {
                        if (it.isNotEmpty()) {
                            ac.askToRemoveScans(it)
                        }
                    }
                    closeActionMode()
                    true
                }

                R.id.manageTags -> {
                    val selectedIds = scansAdapter?.getSelectedIds() ?: emptyList()
                    if (selectedIds.size == 1) {
                        manageTags(selectedIds[0])
                    } else {
                        ac.toast(R.string.manage_tags_requires_single_selection)
                    }
                    closeActionMode()
                    true
                }

                R.id.showScanAsQr -> {
                    // [Feature 2] Only works with single selection
                    val selectedIds = scansAdapter?.getSelectedIds() ?: emptyList()
                    if (selectedIds.size == 1) {
                        scope.launch {
                            // Run DB query off Main thread
                            val scan = db.getScan(selectedIds[0])
                            withContext(Dispatchers.Main) {
                                val ac2 = activity ?: return@withContext
                                if (scan != null && scan.content.isNotEmpty()) {
                                    try {
                                        fragmentManager?.addFragment(
                                            FBarcode.newInstance(
                                                content = scan.content,
                                                format = ZxingCpp.BarcodeFormat.valueOf(scan.format),
                                                size = 640,
                                            )
                                        )
                                    } catch (e: IllegalArgumentException) {
                                        ac2.toast(R.string.cannot_show_as_qr)
                                    }
                                } else {
                                    ac2.toast(R.string.cannot_show_as_qr)
                                }
                            }
                        }
                    } else {
                        ac.toast(R.string.cannot_show_as_qr)
                    }
                    closeActionMode()
                    true
                }

                else -> false
            }
        }

        override fun onDestroyActionMode(mode: ActionMode) {
            closeActionMode()
        }
    }

    private var scansAdapter: ScansAdapter? = null
    private var listViewState: Parcelable? = null
    private var actionMode: ActionMode? = null
    private var scanFilter = ScanFilter()
    private var clearListMenuItem: MenuItem? = null
    private var exportHistoryMenuItem: MenuItem? = null

    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        state: Bundle?,
    ): View? {
        val ac = activity ?: return null
        ac.setTitle(R.string.history)

        val view = inflater.inflate(
            R.layout.roy_f_history,
            container,
            false
        )

        useHistorySwitch = view.findViewById(R.id.useHistory)
        initHistorySwitch(useHistorySwitch)

        listView = view.findViewById(R.id.scans)
        listView.setOnItemClickListener { _, _, _, id ->
            showScan(id)
        }
        listView.setOnItemLongClickListener { _, v, position, id ->
            scansAdapter?.select(v, id, position)
            if (actionMode == null && ac is AppCompatActivity) {
                actionMode = ac.delegate.startSupportActionMode(
                    actionModeCallback
                )
            }
            true
        }
        // [FEAT-NEW-05] Kem theo systemBarListViewScrollListener (cosmetic,
        // co san) - setOnScrollListener chi nhan 1 listener nen phai goi ca 2
        // thu cong thay vi thay the.
        listView.setOnScrollListener(object : AbsListView.OnScrollListener {
            override fun onScroll(
                view: AbsListView,
                firstVisibleItem: Int,
                visibleItemCount: Int,
                totalItemCount: Int,
            ) {
                systemBarListViewScrollListener.onScroll(
                    view, firstVisibleItem, visibleItemCount, totalItemCount
                )
                if (HistoryPaging.shouldLoadMore(
                        firstVisibleItem, visibleItemCount, totalItemCount, LOAD_MORE_THRESHOLD
                    )
                ) {
                    loadMore()
                }
            }

            override fun onScrollStateChanged(view: AbsListView, scrollState: Int) {
                systemBarListViewScrollListener.onScrollStateChanged(view, scrollState)
            }
        })

        fab = view.findViewById(R.id.share)
        fab.setOnClickListener { v ->
            v.context.pickListSeparatorAndShare()
        }

        progressView = view.findViewById(R.id.progressView)

        lockOverlay = view.findViewById(R.id.lockOverlay)
        view.findViewById<View>(R.id.btnUnlock).setOnClickListener { showBiometricPrompt() }

        // Áp window-inset (status bar + toolbar + navbar) cho cả cột để thanh
        // filter chips ở trên cùng không bị status bar/toolbar che, và list/FAB
        // không bị navbar che. Tránh double-pad ở từng con.
        (view.findViewById<View>(R.id.historyInsetRoot)).setPaddingFromWindowInsets()

        chipGroupDate = view.findViewById(R.id.chipGroupDate)
        chipGroupFormat = view.findViewById(R.id.chipGroupFormat)
        chipGroupTag = view.findViewById(R.id.chipGroupTag)
        setupFilterChips()

        update()

        return view
    }

    override fun onDestroy() {
        super.onDestroy()
        scansAdapter?.changeCursor(null)
        parentJob.cancel()
    }

    override fun onResume() {
        super.onResume()
        if (isLockRequired() && !authenticated) {
            lockOverlay.visibility = View.VISIBLE
            showBiometricPrompt()
        }
    }

    override fun onPause() {
        super.onPause()
        listViewState = listView.onSaveInstanceState()
        // [FEAT F8] Bat lai xac thuc moi lan roi man hinh (background app,
        // chuyen fragment khac...) de dam bao khoa that su co tac dung
        if (isLockRequired()) {
            authenticated = false
        }
    }

    private fun isLockRequired() =
        prefs.lockHistory && com.roy.sdkadbmob.AdManager.isVipByKeyActive()

    private fun showBiometricPrompt() {
        val ac = activity ?: return
        val biometricManager = BiometricManager.from(ac)
        val allowedAuthenticators = BiometricManager.Authenticators.BIOMETRIC_WEAK or
            BiometricManager.Authenticators.DEVICE_CREDENTIAL
        if (biometricManager.canAuthenticate(allowedAuthenticators) !=
            BiometricManager.BIOMETRIC_SUCCESS
        ) {
            // Khong co van tay/PIN/pattern nao duoc thiet lap tren may - khong
            // the khoa duoc, cho qua thay vi khoa cung user ra khoi History
            authenticated = true
            lockOverlay.visibility = View.GONE
            return
        }
        val prompt = BiometricPrompt(
            this,
            ContextCompat.getMainExecutor(ac),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(
                    result: BiometricPrompt.AuthenticationResult,
                ) {
                    authenticated = true
                    lockOverlay.visibility = View.GONE
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    // User huy hoac lockout - giu overlay, nut "Unlock" cho phep thu lai
                    if (errorCode != BiometricPrompt.ERROR_USER_CANCELED &&
                        errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON &&
                        errorCode != BiometricPrompt.ERROR_CANCELED
                    ) {
                        ac.toast("${getString(R.string.lock_history_auth_failed)}: $errString")
                    }
                }
            }
        )
        prompt.authenticate(
            BiometricPrompt.PromptInfo.Builder()
                .setTitle(getString(R.string.lock_history_prompt_title))
                .setAllowedAuthenticators(allowedAuthenticators)
                .build()
        )
    }

    override fun onCreateOptionsMenu(
        menu: Menu,
        inflater: MenuInflater,
    ) {
        inflater.inflate(R.menu.menu_f_history, menu)
        initSearchView(menu.findItem(R.id.search))
        menu.setGroupVisible(R.id.scansAvailable, scansAdapter?.count != 0)
        clearListMenuItem = menu.findItem(R.id.clear)
        exportHistoryMenuItem = menu.findItem(R.id.exportHistory)
    }

    private fun initSearchView(item: MenuItem?) {
        item ?: return
        val ac = activity ?: return
        val searchView = item.actionView as SearchView
        val searchManager = ac.getSystemService(
            Context.SEARCH_SERVICE
        ) as SearchManager
        searchView.setSearchableInfo(
            searchManager.getSearchableInfo(ac.componentName)
        )
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean {
                update(query)
                return false
            }

            override fun onQueryTextChange(query: String): Boolean {
                update(query)
                return false
            }
        })
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.clear -> {
                context?.askToRemoveScans()
                true
            }

            R.id.exportHistory -> {
                askToExportToFile()
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun initHistorySwitch(switchView: SwitchCompat) {
        switchView.setOnCheckedChangeListener { _, isChecked ->
            prefs.useHistory = isChecked
        }
        if (prefs.useHistory) {
            switchView.toggle()
        }
    }

    private fun updateAndClearFilter() {
        scanFilter = ScanFilter()
        chipGroupDate.check(R.id.chipDateAll)
        chipGroupFormat.check(R.id.chipFormatAll)
        chipGroupTag.check(R.id.chipTagAll)
        update()
    }


    private fun setupFilterChips() {
        chipGroupDate.setOnCheckedStateChangeListener { _, checkedIds ->
            val dateRange = when (checkedIds.firstOrNull()) {
                R.id.chipDateToday -> ScanFilter.DateRange.TODAY
                R.id.chipDateWeek  -> ScanFilter.DateRange.WEEK
                R.id.chipDateMonth -> ScanFilter.DateRange.MONTH
                else               -> ScanFilter.DateRange.ALL
            }
            scanFilter = scanFilter.copy(dateRange = dateRange)
            update()
        }
        chipGroupFormat.setOnCheckedStateChangeListener { _, checkedIds ->
            val formatGroup = when (checkedIds.firstOrNull()) {
                R.id.chipFormatQr   -> ScanFilter.FormatGroup.QR
                R.id.chipFormat1d   -> ScanFilter.FormatGroup.BARCODE_1D
                R.id.chipFormat2d   -> ScanFilter.FormatGroup.OTHER_2D
                else                -> ScanFilter.FormatGroup.ALL
            }
            scanFilter = scanFilter.copy(formatGroup = formatGroup)
            update()
        }
        chipGroupTag.setOnCheckedStateChangeListener { _, checkedIds ->
            val tag = when (checkedIds.firstOrNull()) {
                R.id.chipTagWork     -> TAG_KEY_WORK
                R.id.chipTagPersonal -> TAG_KEY_PERSONAL
                R.id.chipTagShopping -> TAG_KEY_SHOPPING
                R.id.chipTagTravel   -> TAG_KEY_TRAVEL
                else                 -> null
            }
            scanFilter = scanFilter.copy(tag = tag)
            update()
        }
    }

    // [FEAT-NEW-05] resetPaging=false danh cho loadMore() - giu nguyen so
    // dong da tai (da duoc tang truoc do), khong quay ve trang dau.
    private fun update(query: String? = null, resetPaging: Boolean = true) {
        if (query != null) scanFilter = scanFilter.copy(query = query)
        if (resetPaging) {
            loadedCount = HISTORY_PAGE_SIZE
            hasMorePages = true
        }
        val requestedLimit = loadedCount
        // [FIX BUG-12] Huy job tim kiem truoc do de tranh race - ket qua cu
        // co the ve sau va de len ket qua moi neu khong huy
        searchJob?.cancel()
        searchJob = scope.launch {
            delay(SEARCH_DEBOUNCE_MS)
            val cursor = db.getScans(scanFilter, limit = requestedLimit)
            withContext(Dispatchers.Main) {
                // [FIX BUG-11] Dong cursor truoc khi return neu fragment da
                // detach - truoc day cursor mo o background thread bi ro ri
                val ac = activity ?: run {
                    cursor?.close()
                    return@withContext
                }
                val hasScans = cursor != null && cursor.count > 0
                hasMorePages = HistoryPaging.hasMorePages(cursor?.count ?: 0, requestedLimit)
                isLoadingMore = false
                if (scanFilter.isDefault) {
                    if (!hasScans) {
                        listView.emptyView = useHistorySwitch
                    }
                    ActivityCompat.invalidateOptionsMenu(ac)
                }
                enableMenuItems(hasScans)
                fab.visibility = if (hasScans) {
                    View.VISIBLE
                } else {
                    View.GONE
                }
                cursor?.let { cursor ->
                    // [FEAT-NEW-05] Khi load them trang (khong reset), giu
                    // nguyen vi tri cuon hien tai thay vi restore vi tri cu
                    // tu listViewState (chi cap nhat luc onPause, co the cu)
                    val keepPosition = listView.firstVisiblePosition
                    val keepTop = listView.getChildAt(0)?.top ?: 0
                    // Close previous cursor.
                    scansAdapter?.also { it.changeCursor(null) }
                    scansAdapter = ScansAdapter(ac, cursor)
                    listView.adapter = scansAdapter
                    if (resetPaging) {
                        listViewState?.also {
                            listView.onRestoreInstanceState(it)
                        }
                    } else {
                        listView.setSelectionFromTop(keepPosition, keepTop)
                    }
                }
            }
        }
    }

    // [FEAT-NEW-05] Goi khi cuon gan den cuoi danh sach - tai them 1 trang
    // nua thay vi da load het tu dau.
    private fun loadMore() {
        if (isLoadingMore || !hasMorePages) return
        isLoadingMore = true
        loadedCount += HISTORY_PAGE_SIZE
        update(resetPaging = false)
    }

    private fun enableMenuItems(enabled: Boolean) {
        clearListMenuItem?.isEnabled = enabled
        exportHistoryMenuItem?.isEnabled = enabled
    }

    private fun closeActionMode() {
        unlockStatusBarColor()
        scansAdapter?.clearSelection()
        actionMode?.finish()
        actionMode = null
        scansAdapter?.notifyDataSetChanged()
    }

    // [FIX BUG-3] db.getScan() la SQLite IO, khong duoc goi tren Main thread
    // Chuyen sang background thread de tranh ANR
    private fun showScan(id: Long) {
        closeActionMode()
        scope.launch {
            val scan = db.getScan(id) ?: return@launch
            withContext(Dispatchers.Main) {
                val ac = activity ?: return@withContext
                try {
                    fragmentManager?.addFragment(FDecode.newInstance(scan))
                } catch (e: IllegalArgumentException) {
                    e.printStackTrace()
                }
            }
        }
    }

    // Dialogs don't have a parent layout.
    @SuppressLint("InflateParams")
    private fun Context.askForName(
        id: Long,
        text: String?,
        content: String?,
    ) {
        val view = LayoutInflater.from(this).inflate(
            R.layout.roy_dlg_enter_name, null
        )
        val nameView = view.findViewById<EditText>(R.id.name)
        nameView.setText(text)
        AlertDialog.Builder(this)
            .setTitle(
                if (content.isNullOrEmpty()) {
                    getString(R.string.binary_data)
                } else {
                    content
                }
            )
            .setView(view)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                val name = nameView.text.toString()
                db.renameScan(id, name)
                update()
            }
            .setNegativeButton(android.R.string.cancel) { _, _ -> }
            .show()
    }

    // [FEAT F3] Chi ho tro 1 scan/lan (giong showScanAsQr) - don gian hoa vi
    // khong can hop nhat tag hien co cua nhieu scan khac nhau.
    @SuppressLint("InflateParams")
    private fun manageTags(id: Long) {
        scope.launch {
            val existingTags = parseTags(db.getScan(id)?.tags)
            withContext(Dispatchers.Main) {
                val ac = activity ?: return@withContext
                val view = LayoutInflater.from(ac).inflate(R.layout.roy_dlg_manage_tags, null)
                val workBox = view.findViewById<CheckBox>(R.id.tagWork).apply {
                    isChecked = TAG_KEY_WORK in existingTags
                }
                val personalBox = view.findViewById<CheckBox>(R.id.tagPersonal).apply {
                    isChecked = TAG_KEY_PERSONAL in existingTags
                }
                val shoppingBox = view.findViewById<CheckBox>(R.id.tagShopping).apply {
                    isChecked = TAG_KEY_SHOPPING in existingTags
                }
                val travelBox = view.findViewById<CheckBox>(R.id.tagTravel).apply {
                    isChecked = TAG_KEY_TRAVEL in existingTags
                }
                // [FEAT F3] Tag tuy chon la VIP-exclusive - free user chi duoc
                // chon trong 4 preset o tren, khong tao tag rieng
                val isVip = com.roy.sdkadbmob.AdManager.isVipByKeyActive()
                val existingCustomTags = existingTags.filterNot { it in TAG_PRESET_KEYS }
                val customView = view.findViewById<EditText>(R.id.tagCustom)
                customView.setText(existingCustomTags.joinToString(", "))
                if (!isVip) {
                    customView.isEnabled = false
                    customView.hint = ac.getString(R.string.tag_custom_vip_hint)
                }
                AlertDialog.Builder(ac)
                    .setTitle(R.string.manage_tags)
                    .setView(view)
                    .setPositiveButton(android.R.string.ok) { _, _ ->
                        val selectedPresets = mutableListOf<String>().apply {
                            if (workBox.isChecked) add(TAG_KEY_WORK)
                            if (personalBox.isChecked) add(TAG_KEY_PERSONAL)
                            if (shoppingBox.isChecked) add(TAG_KEY_SHOPPING)
                            if (travelBox.isChecked) add(TAG_KEY_TRAVEL)
                        }
                        // [FIX] Non-VIP khong duoc SUA custom tag (field disabled) nhung
                        // khong duoc XOA custom tag da co tu truoc (vd luc con VIP) -
                        // giu nguyen existingCustomTags thay vi emptyList().
                        val customTags = if (isVip) {
                            parseTags(customView.text.toString())
                        } else {
                            existingCustomTags
                        }
                        val tagsCsv = joinTags(selectedPresets + customTags)
                        scope.launch {
                            db.setTags(id, tagsCsv)
                            withContext(Dispatchers.Main) { update() }
                        }
                    }
                    .setNegativeButton(android.R.string.cancel) { _, _ -> }
                    .show()
            }
        }
    }

    private fun Context.askToRemoveScans(ids: List<Long>) {
        AlertDialog.Builder(this)
            .setMessage(
                if (ids.size > 1) {
                    R.string.reallyRemoveSelectedScans
                } else {
                    R.string.really_remove_scan
                }
            )
            .setPositiveButton(android.R.string.ok) { _, _ ->
                ids.forEach { db.removeScan(it) }
                if (scansAdapter?.count == 1) {
                    updateAndClearFilter()
                } else {
                    update()
                }
            }
            .setNegativeButton(android.R.string.cancel) { _, _ ->
            }
            .show()
    }

    private fun Context.askToRemoveScans() {
        AlertDialog.Builder(this)
            .setMessage(
                if (scanFilter.isDefault) {
                    R.string.reallyRemoveAllScans
                } else {
                    R.string.reallyRemoveSelectedScans
                }
            )
            .setPositiveButton(android.R.string.ok) { _, _ ->
                db.removeScans(scanFilter)
                updateAndClearFilter()
            }
            .setNegativeButton(android.R.string.cancel) { _, _ ->
            }
            .show()
    }

    private fun askToExportToFile() {
        scope.launch {
            val ac = activity ?: return@launch
            progressView.useVisibility {
                // Write permission is only required before Android Q.
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q &&
                    !ac.hasWritePermission { askToExportToFile() }
                ) {
                    return@useVisibility
                }
                val options = ac.resources.getStringArray(
                    R.array.exportOptionsValues
                )
                val delimiter = alertDialog<String>(ac) { resume ->
                    setTitle(R.string.exportAs)
                    setItems(R.array.exportOptionsNames) { _, which ->
                        resume(options[which])
                    }
                } ?: return@useVisibility
                // [FEAT E5] Neu dang co filter active va export CSV/JSON (khong
                // ap dung cho "db" - luon la ban sao toan bo file), hoi user
                // muon export dung phan da loc hay toan bo lich su
                val exportFilter = if (delimiter != "db" && !scanFilter.isDefault) {
                    val filteredCount = db.getScansDetailed(scanFilter)?.use { it.count } ?: 0
                    val exportAll = alertDialog<Boolean>(ac) { resume ->
                        setTitle(R.string.export_filter_prompt_title)
                        setMessage(
                            ac.getString(R.string.export_filter_prompt_message, filteredCount)
                        )
                        setPositiveButton(R.string.export_filter_prompt_filtered) { _, _ ->
                            resume(false)
                        }
                        setNegativeButton(R.string.export_filter_prompt_all) { _, _ ->
                            resume(true)
                        }
                    } ?: false
                    if (exportAll) ScanFilter() else scanFilter
                } else {
                    scanFilter
                }
                val name = withContext(Dispatchers.Main) {
                    ac.askForFileName(
                        when (delimiter) {
                            "db" -> ".db"
                            "json" -> ".json"
                            else -> ".csv"
                        }
                    )
                } ?: return@useVisibility
                val message = when (delimiter) {
                    "db" -> ac.exportDatabase(name)
                    else -> db.getScansDetailed(exportFilter)?.use {
                        when (delimiter) {
                            "json" -> ac.exportJson(name, it)
                            else -> ac.exportCsv(name, it, delimiter)
                        }
                    } ?: false
                }.toSaveResult()
                withContext(Dispatchers.Main) {
                    ac.toast(message)
                }
            }
        }
    }

    private fun Context.pickListSeparatorAndShare() {
        val separators = resources.getStringArray(
            R.array.listSeparatorsValues
        )
        AlertDialog.Builder(this)
            .setTitle(R.string.pickListSeparator)
            .setItems(R.array.listSeparatorsNames) { _, which ->
                shareScans(separators[which])
            }
            .show()
    }

    private fun shareScans(format: String) = scope.launch {
        progressView.useVisibility {
            var text: String? = null
            db.getScansDetailed(scanFilter)?.use { cursor ->
                val details = format.split(":")
                text = when (details[0]) {
                    "text" -> cursor.exportText(details[1])
                    "csv" -> cursor.exportCsv(details[1])
                    else -> cursor.exportJson()
                }
            }
            text?.let {
                withContext(Dispatchers.Main) {
                    context?.shareText(it)
                }
            }
        }
    }
}

private fun Cursor.exportText(separator: String): String {
    val sb = StringBuilder()
    val contentIndex = getColumnIndex(Db.SCANS_CONTENT)
    if (contentIndex > -1 && moveToFirst()) {
        do {
            val content = getString(contentIndex)
            if (content?.isNotEmpty() == true) {
                sb.append(content)
                sb.append(separator)
            }
        } while (moveToNext())
    }
    return sb.toString()
}
