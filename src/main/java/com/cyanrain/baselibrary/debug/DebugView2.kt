package com.cyanrain.baselibrary.debug

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView
import com.cyanrain.baselibrary.log.LogUtil
import com.cyanrain.baselibrary.utils.CommonUtils.Companion.formatTime
import java.util.LinkedList

class DebugView2 : LinearLayout {
    companion object {
        private const val TAG = "DebugView2"

        // 限制日志行数，避免内存溢出
        private const val MAX_LOG_LINES = 1000
    }

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: android.util.AttributeSet) : super(context, attrs)
    constructor(context: Context, attrs: android.util.AttributeSet, defStyleAttr: Int) : super(
        context, attrs, defStyleAttr
    )


    private lateinit var logListView: ListView
    private lateinit var logAdapter: LogAdapter
    private val appLogBuffer = LinkedList<LogUtil.LogBean>()

    private var onItemClickListener: OnItemClickListener? = null

    init {
        orientation = VERTICAL
        setBackgroundColor(0x88000000.toInt())
        initContentView()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun initContentView() {
        // 创建 ListView 用于显示日志
        logListView = ListView(context)
        logAdapter = LogAdapter()
        logListView.adapter = logAdapter

        val listLayoutParams = LayoutParams(LayoutParams.MATCH_PARENT, 0).apply {
            weight = 1.0f
        }
        logListView.layoutParams = listLayoutParams
        addView(logListView)

        val linearLayout = LinearLayout(context).apply {
            orientation = HORIZONTAL
            layoutParams =
                LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
                    gravity = Gravity.CENTER
                }
        }
        // clear按钮
        Button(context).apply {
            text = "clear"
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.GRAY)
            layoutParams =
                LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
                    gravity = Gravity.CENTER
                    topMargin = 16
                    marginStart = 16
                }
        }.also { linearLayout.addView(it) }.setOnClickListener {
            appLogBuffer.clear()
            logAdapter.notifyDataSetChanged()
        }
        Button(context).apply {
            text = "X"
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.DKGRAY)
            layoutParams =
                LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
                    gravity = Gravity.CENTER
                    topMargin = 16
                    marginStart = 16
                }
        }.also { linearLayout.addView(it) }.setOnClickListener {
            appLogBuffer.clear()
            onItemClickListener?.onClose()
        }
        addView(linearLayout)

        // 默认滑动到最底部
        logListView.post {
            logListView.setSelection(logAdapter.count - 1)
        }
    }


    /**
     * 日志适配器
     */
    inner class LogAdapter : BaseAdapter() {
        override fun getCount(): Int {
            return synchronized(appLogBuffer) { appLogBuffer.size }
        }

        override fun getItem(position: Int): Any {
            return appLogBuffer[position]
        }

        override fun getItemId(position: Int): Long {
            return position.toLong()
        }

        @SuppressLint("SetTextI18n")
        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            val view: TextView = if (convertView == null) {
                val textView = TextView(context)
                textView.setTextColor(Color.WHITE)
                textView.textSize = 11f
                textView.setPadding(10, 10, 10, 10)
                textView
            } else {
                convertView as TextView
            }

            val logBean = getItem(position) as LogUtil.LogBean
            setLogItemStyle(view, logBean.level, logBean)

            return view
        }

        private fun setLogItemStyle(
            textView: TextView,
            level: LogUtil.LEVEL,
            logBean: LogUtil.LogBean,
        ) {
            val levelStr = when (level) {
                LogUtil.LEVEL.VERBOSE -> "V"
                LogUtil.LEVEL.DEBUG -> "D"
                LogUtil.LEVEL.INFO -> "I"
                LogUtil.LEVEL.WARN -> "W"
                LogUtil.LEVEL.ERROR -> "E"
                LogUtil.LEVEL.WFT -> "WTF"
            }

            // 设置不同级别日志的颜色
            when (level) {
                LogUtil.LEVEL.ERROR, LogUtil.LEVEL.WFT -> {
                    textView.setTextColor(Color.RED)
                }

                LogUtil.LEVEL.WARN -> {
                    textView.setTextColor(Color.YELLOW)
                }

                LogUtil.LEVEL.INFO -> {
                    textView.setTextColor(Color.WHITE)
                }

                LogUtil.LEVEL.DEBUG -> {
                    textView.setTextColor(Color.GREEN)
                }

                else -> {
                    textView.setTextColor(Color.BLUE)
                }
            }

            textView.text =
                "${formatTime(logBean.time)} ${levelStr}/${logBean.tag}: ${logBean.message}"
        }
    }

    fun setOnItemClickListener(listener: OnItemClickListener) {
        onItemClickListener = listener
    }

    fun addLog(logBean: LogUtil.LogBean) {
        synchronized(appLogBuffer) {
            appLogBuffer.add(logBean)
            // 保持缓冲区大小在限制范围内
            if (appLogBuffer.size > MAX_LOG_LINES) {
                appLogBuffer.removeFirst()
            }
        }

        // 更新UI
        refreshLogs()
    }

    /**
     * 刷新日志显示
     */
    private fun refreshLogs() {
        logAdapter.notifyDataSetChanged()
        // 自动滚动到底部
        logListView.post {
            logListView.setSelection(logAdapter.count - 1)
        }
    }

    interface OnItemClickListener {
        fun onClose()
    }
}