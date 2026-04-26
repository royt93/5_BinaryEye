package com.mckimquyen.binaryeye.view

import android.content.Context
import android.content.ContextWrapper
import android.graphics.drawable.ColorDrawable
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.view.View
import android.view.WindowManager
import android.widget.AbsListView
import com.mckimquyen.binaryeye.R

val systemBarListViewScrollListener = object : AbsListView.OnScrollListener {
    override fun onScroll(
        view: AbsListView,
        firstVisibleItem: Int,
        visibleItemCount: Int,
        totalItemCount: Int,
    ) {
        // Give Android some time to settle down before running this,
        // not putting it on the queue makes it only work sometimes.
        view.post {
            val scrolled = firstVisibleItem > 0 ||
                    (totalItemCount > 0 && firstChildScrolled(view))
            val scrollable = scrolled || totalItemCount > 0 && lastChildOutOfView(view)
            colorSystemAndToolBars(
                context = view.context,
                scrolled = scrolled,
                scrollable = scrollable
            )
        }
    }

    override fun onScrollStateChanged(view: AbsListView, scrollState: Int) {
    }
}

val systemBarRecyclerViewScrollListener = object : RecyclerView.OnScrollListener() {
    override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
        val layoutManager = recyclerView.layoutManager as LinearLayoutManager
        val scrolled = layoutManager.findFirstCompletelyVisibleItemPosition() != 0
        val scrollable = scrolled || layoutManager.findLastVisibleItemPosition() <
                (recyclerView.adapter?.itemCount ?: (0 - 1))
        colorSystemAndToolBars(
            context = recyclerView.context,
            scrolled = scrolled,
            scrollable = scrollable
        )
    }

    override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
    }
}

private fun firstChildScrolled(listView: AbsListView): Boolean {
    val child = listView.getChildAt(0)
    return child != null && child.top < listView.paddingTop
}

private fun lastChildOutOfView(listView: AbsListView): Boolean {
    val child = listView.getChildAt(listView.lastVisiblePosition)
    return child != null && child.bottom >= listView.height
}

fun initSystemBars(activity: AppCompatActivity) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        // Keeps the soft keyboard from repositioning the layout.
        val window = activity.window
        window.setSoftInputMode(
            WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
        )
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                    View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
    }
    colorSystemAndToolBars(activity)
}

private var statusBarColorLocked = false
fun lockStatusBarColor() {
    statusBarColorLocked = true
}

fun unlockStatusBarColor() {
    statusBarColorLocked = false
}

private var translucentPrimaryColor = 0
private val actionBarBackground = ColorDrawable()
fun colorSystemAndToolBars(
    context: Context,
    scrolled: Boolean = false,
    scrollable: Boolean = false,
) {
    if (translucentPrimaryColor == 0) {
        translucentPrimaryColor = ContextCompat.getColor(
            /* context = */ context,
            /* id = */ R.color.primary_translucent
        )
    }
    val topColor = if (scrolled) translucentPrimaryColor else 0
    val activity = getAppCompatActivity(context) ?: return
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        val window = activity.window
        if (!statusBarColorLocked) {
            window.statusBarColor = topColor
        }
        window.navigationBarColor = if (scrolled || scrollable) {
            translucentPrimaryColor
        } else {
            0
        }
    }
    // We no longer overwrite the Action Bar background with ColorDrawable
    // because it ruins the custom frosted glass (bg_liquid_glass_dark) and margin styling.
    /*
    activity.supportActionBar?.setBackgroundDrawable(
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            // Avoid allocation on Honeycomb and better.
            actionBarBackground.color = topColor
            actionBarBackground
        } else {
            // ColorDrawable.setColor() doesn't exist pre Honeycomb.
            ColorDrawable(topColor)
        }
    )
    */
}

private fun getAppCompatActivity(context: Context): AppCompatActivity? {
    var c = context
    while (c is ContextWrapper) {
        if (c is AppCompatActivity) {
            return c
        }
        c = c.baseContext ?: break
    }
    return null
}
