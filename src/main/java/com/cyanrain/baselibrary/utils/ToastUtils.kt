package com.cyanrain.baselibrary.utils

import android.content.Context
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.TextView
import android.widget.Toast


object ToastUtils {
    private var mIsDebug = false
    fun init(isDebug: Boolean = false) {
        mIsDebug = isDebug
    }
    @Deprecated("Deprecated")
    fun show(context: Context, text: String) {
        val toast: Toast = Toast.makeText(context, text, Toast.LENGTH_LONG)
        toast.setGravity(Gravity.CENTER_HORIZONTAL or Gravity.BOTTOM, 0, 0) //设置显示位置

        val v = toast.view?.findViewById<View>(android.R.id.message) as TextView
        v.isSingleLine = false //设置TextView可以显示多行文本
        v.ellipsize = null
        v.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 18f)
        v.gravity = Gravity.LEFT
        toast.show()
    }
}