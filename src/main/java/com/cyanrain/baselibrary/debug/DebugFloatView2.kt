package com.cyanrain.baselibrary.debug

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.view.Gravity
import androidx.appcompat.widget.AppCompatTextView

/**
 *
 */
@SuppressLint("ClickableViewAccessibility")
class DebugFloatView2: AppCompatTextView{
    companion object {
        private const val TAG = "DebugView2"
    }

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: android.util.AttributeSet) : super(context, attrs)
    constructor(context: Context, attrs: android.util.AttributeSet, defStyleAttr: Int) : super(
        context, attrs, defStyleAttr
    )

    init {
        text = "debug"
        textSize = 16f // 文本大小(sp)
        gravity = Gravity.CENTER
        setTextColor(Color.WHITE)
        setBackgroundColor(0x88000000.toInt())
    }
}