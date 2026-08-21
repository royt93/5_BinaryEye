package com.mckimquyen.binaryeye.view.actions.search

import android.content.Context
import com.mckimquyen.binaryeye.R
import com.mckimquyen.binaryeye.ext.app.alertDialog
import com.mckimquyen.binaryeye.prefs
import com.mckimquyen.binaryeye.view.actions.IAction
import com.mckimquyen.binaryeye.view.content.openUrl
import com.mckimquyen.binaryeye.view.net.urlEncode

object OpenOrSearchAction : IAction {
    override val iconResId: Int = R.drawable.ic_action_search
    override val titleResId: Int = R.string.searchWeb

    override fun canExecuteOn(data: ByteArray): Boolean = false

    override suspend fun execute(
        context: Context,
        data: ByteArray,
    ) {
        view(context = context, url = String(data), search = true)
    }

    private suspend fun view(
        context: Context,
        url: String,
        search: Boolean,
    ) {
        if (!context.openUrl(url, silent = true) && search) {
            openSearch(context, url)
        }
    }

    private suspend fun openSearch(
        context: Context,
        query: String,
    ) {
        val defaultSearchUrl = prefs.defaultSearchUrl
        if (defaultSearchUrl.isNotEmpty()) {
            view(
                context = context,
                url = defaultSearchUrl + query.urlEncode(),
                search = false
            )
            return
        }
        val names = context.resources.getStringArray(
            R.array.searchEnginesNames
        ).toMutableList()
        val urls = context.resources.getStringArray(
            R.array.searchEnginesValues
        ).toMutableList()
        // Remove the "Always ask" entry. The arrays search_engines_*
        // are used in the preferences too.
        names.removeAt(0)
        urls.removeAt(0)
        if (prefs.openWithUrl.isNotEmpty()) {
            names.add(prefs.openWithUrl)
            urls.add(prefs.openWithUrl)
        }
        val queryUri = alertDialog<String>(context) { resume ->
            setTitle(R.string.pick_search_engine)
            setItems(names.toTypedArray()) { _, which ->
                resume(urls[which] + query.urlEncode())
            }
        } ?: return
        view(
            context = context,
            url = queryUri,
            search = false
        )
    }
}
