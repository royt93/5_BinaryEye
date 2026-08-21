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
import android.widget.EditText
import android.widget.ListView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.SwitchCompat
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

class FHistory : Fragment() {
    private lateinit var useHistorySwitch: SwitchCompat
    private lateinit var listView: ListView
    private lateinit var fab: View
    private lateinit var progressView: View

    private lateinit var chipGroupDate: ChipGroup
    private lateinit var chipGroupFormat: ChipGroup

    private val parentJob = Job()
    private val scope = CoroutineScope(Dispatchers.IO + parentJob)
    private var searchJob: Job? = null
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
        listView.setOnScrollListener(systemBarListViewScrollListener)

        fab = view.findViewById(R.id.share)
        fab.setOnClickListener { v ->
            v.context.pickListSeparatorAndShare()
        }

        progressView = view.findViewById(R.id.progressView)

        // Áp window-inset (status bar + toolbar + navbar) cho cả cột để thanh
        // filter chips ở trên cùng không bị status bar/toolbar che, và list/FAB
        // không bị navbar che. Tránh double-pad ở từng con.
        (view.findViewById<View>(R.id.historyInsetRoot)).setPaddingFromWindowInsets()

        chipGroupDate = view.findViewById(R.id.chipGroupDate)
        chipGroupFormat = view.findViewById(R.id.chipGroupFormat)
        setupFilterChips()

        update()

        return view
    }

    override fun onDestroy() {
        super.onDestroy()
        scansAdapter?.changeCursor(null)
        parentJob.cancel()
    }

    override fun onPause() {
        super.onPause()
        listViewState = listView.onSaveInstanceState()
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
    }

    private fun update(query: String? = null) {
        if (query != null) scanFilter = scanFilter.copy(query = query)
        // [FIX BUG-12] Huy job tim kiem truoc do de tranh race - ket qua cu
        // co the ve sau va de len ket qua moi neu khong huy
        searchJob?.cancel()
        searchJob = scope.launch {
            delay(SEARCH_DEBOUNCE_MS)
            val cursor = db.getScans(scanFilter)
            withContext(Dispatchers.Main) {
                // [FIX BUG-11] Dong cursor truoc khi return neu fragment da
                // detach - truoc day cursor mo o background thread bi ro ri
                val ac = activity ?: run {
                    cursor?.close()
                    return@withContext
                }
                val hasScans = cursor != null && cursor.count > 0
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
                    // Close previous cursor.
                    scansAdapter?.also { it.changeCursor(null) }
                    scansAdapter = ScansAdapter(ac, cursor)
                    listView.adapter = scansAdapter
                    listViewState?.also {
                        listView.onRestoreInstanceState(it)
                    }
                }
            }
        }
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
                    else -> db.getScansDetailed(scanFilter)?.use {
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
