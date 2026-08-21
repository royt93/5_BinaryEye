package com.mckimquyen.binaryeye.frm

import android.content.Context
import android.net.wifi.WifiManager
import android.net.wifi.WifiNetworkSuggestion
import android.os.Build
import android.os.Bundle
import android.view.*
import android.widget.ArrayAdapter
import android.widget.ListView
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import com.mckimquyen.binaryeye.R
import com.mckimquyen.binaryeye.view.setPaddingFromWindowInsets
import com.mckimquyen.binaryeye.view.systemBarListViewScrollListener
import com.mckimquyen.binaryeye.view.widget.toast

class FNetworkSuggestions : Fragment() {
    @RequiresApi(Build.VERSION_CODES.R)
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        state: Bundle?,
    ): View? {
        val ac = activity ?: return null
        ac.setTitle(R.string.network_suggestions)

        val view = inflater.inflate(
            /* resource = */ R.layout.roy_f_network_suggestions,
            /* root = */ container,
            /* attachToRoot = */ false
        )

        val wm = ac.applicationContext.getSystemService(
            Context.WIFI_SERVICE
        ) as WifiManager
        val suggestionArrayAdapter = ArrayAdapter(
            ac,
            android.R.layout.simple_list_item_checked,
            wm.networkSuggestions.map {
                Suggestion(
                    label = it.ssid ?: it.toString(),
                    suggestion = it
                )
            }
        )

        val listView = view.findViewById<ListView>(R.id.suggestions)
        listView.emptyView = view.findViewById(R.id.noSuggestions)
        listView.adapter = suggestionArrayAdapter
        listView.setOnScrollListener(systemBarListViewScrollListener)
        (view.findViewById<View>(R.id.insetLayout)).setPaddingFromWindowInsets()
        listView.setPaddingFromWindowInsets()

        view.findViewById<View>(R.id.remove).setOnClickListener {
            val removeList = ArrayList<WifiNetworkSuggestion>()
            val removeFromAdapter = ArrayList<Suggestion>()
            val checked = listView.checkedItemPositions
            val size = checked.size()
            for (i in 0 until size) {
                if (checked.valueAt(i)) {
                    val pos = checked.keyAt(i)
                    listView.setItemChecked(pos, false)
                    val suggestion = listView.getItemAtPosition(pos) as Suggestion
                    removeList.add(suggestion.suggestion)
                    removeFromAdapter.add(suggestion)
                }
            }
            if (removeList.isNotEmpty()) {
                removeFromAdapter.forEach {
                    suggestionArrayAdapter.remove(it)
                }
                wm.removeNetworkSuggestions(removeList)
            } else {
                ac.toast(
                    R.string.clear_network_suggestions_nothing_to_remove
                )
            }
        }

        return view
    }
}

private data class Suggestion(
    val label: String,
    val suggestion: WifiNetworkSuggestion,
) {
    override fun toString() = label
}
