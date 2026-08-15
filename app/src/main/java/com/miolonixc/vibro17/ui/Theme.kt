package com.miolonixc.vibro17.ui

import android.content.Context
import androidx.annotation.AttrRes

object Theme {
    fun accent(context: Context, @AttrRes attr: Int = R.attr.vibroAccent, fallback: Int = 0xFF00E5FF.toInt()): Int {
        val ta = context.theme.obtainStyledAttributes(intArrayOf(attr))
        val value = ta.getColor(0, fallback)
        ta.recycle()
        return value
    }

    fun accentDim(context: Context): Int =
        accent(context, R.attr.vibroAccentDim, 0xFF0093A6.toInt())
}
