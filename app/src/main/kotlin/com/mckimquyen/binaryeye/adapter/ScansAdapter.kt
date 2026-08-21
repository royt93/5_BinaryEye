package com.mckimquyen.binaryeye.adapter

import android.content.Context
import android.database.Cursor
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CursorAdapter
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.mckimquyen.binaryeye.R
import com.mckimquyen.binaryeye.database.Db
import java.text.DateFormat
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*

private const val VIEW_TYPE_ITEM = 0
private const val VIEW_TYPE_HEADER = 1

// [FEAT E2] Vi tri trong ListView khong con khop 1-1 voi vi tri cursor sau khi
// chen header - moi entry la 1 dong that (Cursor) hoac 1 header ngay (khong co
// cursor backing).
private sealed class Row {
    data class Item(val cursorPosition: Int) : Row()
    data class Header(val group: DateGroup) : Row()
}

class ScansAdapter(context: Context, cursor: Cursor) :
    CursorAdapter(context, cursor, false) {
    private val idIndex = cursor.getColumnIndex(Db.SCANS_ID)
    private val timeIndex = cursor.getColumnIndex(Db.SCANS_DATETIME)
    private val nameIndex = cursor.getColumnIndex(Db.SCANS_NAME)
    private val contentIndex = cursor.getColumnIndex(Db.SCANS_CONTENT)
    private val formatIndex = cursor.getColumnIndex(Db.SCANS_FORMAT)
    private val selections = mutableMapOf<Long, Int>()
    private val selectedColor = ContextCompat.getColor(context, R.color.selectedRow)

    // [FEAT E2] Tinh 1 lan khi tao adapter (FHistory luon tao adapter moi cho
    // moi lan cursor thay doi, khong goi lai changeCursor voi cursor khac null)
    private val rows: List<Row> = buildRows(cursor, System.currentTimeMillis())

    private fun buildRows(cursor: Cursor, nowMs: Long): List<Row> = buildList {
        if (!cursor.moveToFirst()) return@buildList
        var lastGroup: DateGroup? = null
        var cursorPosition = 0
        do {
            val group = classifyDateGroup(cursor.getString(timeIndex), nowMs)
            if (group != lastGroup) {
                add(Row.Header(group))
                lastGroup = group
            }
            add(Row.Item(cursorPosition))
            cursorPosition++
        } while (cursor.moveToNext())
    }

    fun select(
        view: View,
        id: Long,
        position: Int,
    ) {
        view.select(
            if (selections[id] == null) {
                selections[id] = position
                true
            } else {
                selections.remove(id)
                false
            }
        )
    }

    fun clearSelection() {
        selections.clear()
    }

    fun forSelection(callback: (Long, Int) -> Unit) {
        selections.forEach {
            callback.invoke(it.key, it.value)
        }
    }

    fun getSelectedIds(): List<Long> = mutableListOf<Long>().apply {
        forSelection { id, _ ->
            add(id)
        }
    }

    fun getSelectedContent(separator: String): String {
        val sb = StringBuilder()
        forSelection { _, position ->
            getContent(position)?.let {
                if (sb.isNotEmpty()) {
                    sb.append(separator)
                }
                sb.append(it)
            }
        }
        return sb.toString()
    }

    fun getName(
        position: Int,
    ) = (getItem(position) as Cursor?)?.getString(nameIndex)

    fun getContent(
        position: Int,
    ) = (getItem(position) as Cursor?)?.getString(contentIndex)

    // [FEAT E2] Header khong the/khong nen bam duoc (click mo detail,
    // long-click chon nhieu) - bao ListView bo qua dispatch cho cac vi tri nay.
    override fun areAllItemsEnabled(): Boolean = false

    override fun isEnabled(position: Int): Boolean = rows.getOrNull(position) is Row.Item

    override fun getCount(): Int = rows.size

    override fun getViewTypeCount(): Int = 2

    override fun getItemViewType(position: Int): Int = when (rows.getOrNull(position)) {
        is Row.Header -> VIEW_TYPE_HEADER
        else -> VIEW_TYPE_ITEM
    }

    override fun getItem(position: Int): Any? = when (val row = rows.getOrNull(position)) {
        is Row.Item -> cursor?.takeIf { it.moveToPosition(row.cursorPosition) }
        else -> null
    }

    override fun getItemId(position: Int): Long = when (val row = rows.getOrNull(position)) {
        is Row.Item -> {
            cursor?.moveToPosition(row.cursorPosition)
            cursor?.getLong(idIndex) ?: -1L
        }
        // Header khong co _id that - dung gia tri am on dinh theo position de
        // khong bao gio trung voi id thuc (luon >= 0) tu DB.
        else -> -(position + 1).toLong()
    }

    override fun getView(
        position: Int,
        convertView: View?,
        parent: ViewGroup,
    ): View = when (val row = rows.getOrNull(position)) {
        is Row.Header -> bindHeaderView(row.group, convertView, parent)
        is Row.Item -> {
            val c = cursor
            checkNotNull(c) { "cursor is null" }
            c.moveToPosition(row.cursorPosition)
            val view = convertView ?: newView(parent.context, c, parent)
            bindView(view, parent.context, c)
            view
        }
        null -> checkNotNull(convertView) { "invalid position $position with no convertView" }
    }

    private fun bindHeaderView(
        group: DateGroup,
        convertView: View?,
        parent: ViewGroup,
    ): View {
        val view = convertView ?: LayoutInflater.from(parent.context).inflate(
            R.layout.roy_v_row_scan_header, parent, false
        )
        (view as? TextView)?.setText(group.labelResId)
        return view
    }

    override fun newView(
        context: Context,
        cursor: Cursor,
        parent: ViewGroup,
    ): View = LayoutInflater.from(parent.context).inflate(
        R.layout.roy_v_row_scan, parent, false
    )

    override fun bindView(
        view: View,
        context: Context,
        cursor: Cursor,
    ) {
        getViewHolder(view).apply {
            timeView.text = formatDateTime(cursor.getString(timeIndex))
            val name = cursor.getString(nameIndex)
            val content = cursor.getString(contentIndex)
            var icon = 0
            contentView.text = when {
                name?.isNotEmpty() == true -> {
                    icon = R.drawable.ic_label
                    name
                }

                content?.isEmpty() == true -> context.getString(
                    R.string.binary_data
                )

                else -> content
            }
            contentView.setCompoundDrawablesWithIntrinsicBounds(
                /* left = */ icon, /* top = */ 0, /* right = */ 0, /* bottom = */ 0
            )
            formatView.text = prettifyFormatName(cursor.getString(formatIndex))
        }
        val selected = selections[cursor.getLong(idIndex)] != null
        // [FIX BUG-8] Goi truc tiep thay vi view.post {} de tranh stale execution
        // sau khi view da bi detach khoi adapter
        view.select(selected)
    }

    private fun View.select(selected: Boolean) {
        setBackgroundColor(if (selected) selectedColor else 0)
    }

    private fun getViewHolder(
        view: View,
    ): ViewHolder = view.tag as ViewHolder? ?: ViewHolder(
        view.findViewById(R.id.time),
        view.findViewById(R.id.content),
        view.findViewById(R.id.format)
    ).also {
        view.tag = it
    }

    private data class ViewHolder(
        val timeView: TextView,
        val contentView: TextView,
        val formatView: TextView,
    )
}

fun prettifyFormatName(format: String) = format.replace("_", " ")

private fun formatDateTime(rfc: String): String {
    try {
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).parse(rfc)?.let {
            return DateFormat.getDateTimeInstance(
                /* dateStyle = */ DateFormat.LONG,
                /* timeStyle = */ DateFormat.SHORT
            ).format(it)
        }
    } catch (e: ParseException) {
        // Ignore and fall through.
    }
    return rfc
}
