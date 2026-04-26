package com.mckimquyen.binaryeye.view

import android.graphics.Rect
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.appcompat.widget.Toolbar
import android.view.View

private var toolbarHeight = 0
fun recordToolbarHeight(toolbar: Toolbar) {
    val lp = toolbar.layoutParams as? android.view.ViewGroup.MarginLayoutParams
    val height = if (toolbar.layoutParams.height > 0) toolbar.layoutParams.height else 0
    toolbarHeight = height + (lp?.topMargin ?: 0) + (lp?.bottomMargin ?: 0)
}

fun View.setPaddingFromWindowInsets() {
    doOnApplyWindowInsets { v, insets -> v.setPadding(insets) }
}

// A slight variation of the idea from Google's Chris Banes to avoid
// adding the toolbar height for every view that needs to be inset.
// For the original post see here:
// https://medium.com/androiddevelopers/windowinsets-listeners-to-layouts-8f9ccc8fa4d1
fun View.doOnApplyWindowInsets(f: (View, Rect) -> Unit) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
        f(this, insetsWithToolbar())
    } else {
        ViewCompat.setOnApplyWindowInsetsListener(this) { v, insets ->
            f(v, insetsWithToolbar(insets))
            insets
        }
        // It's important to explicitly request the insets (again) in
        // case the view was created in Fragment.onCreateView() because
        // setOnApplyWindowInsetsListener() won't fire when the view
        // isn't attached.
        requestApplyInsetsWhenAttached()
    }
}

private fun insetsWithToolbar(insets: WindowInsetsCompat? = null) = Rect(
    /* left = */ insets?.systemWindowInsetLeft ?: 0,
    /* top = */ (insets?.systemWindowInsetTop ?: 0) + toolbarHeight,
    /* right = */ insets?.systemWindowInsetRight ?: 0,
    /* bottom = */ insets?.systemWindowInsetBottom ?: 0
)

@RequiresApi(Build.VERSION_CODES.KITKAT)
private fun View.requestApplyInsetsWhenAttached() {
    if (isAttachedToWindow) {
        ViewCompat.requestApplyInsets(this)
    } else {
        addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(v: View) {
                v.removeOnAttachStateChangeListener(this)
                ViewCompat.requestApplyInsets(v)
            }

            override fun onViewDetachedFromWindow(v: View) = Unit
        })
    }
}
