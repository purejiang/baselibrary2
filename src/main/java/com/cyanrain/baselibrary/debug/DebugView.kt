package com.cyanrain.baselibrary.debug

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.BaseAdapter
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView
import com.cyanrain.baselibrary.log.LogUtil
import com.cyanrain.baselibrary.utils.CommonUtils.Companion.formatTime
import java.util.LinkedList

class DebugView : LinearLayout, LogUtil.LogListener {
    companion object {
        private const val TAG = "LogDialog"

        // 限制日志行数，避免内存溢出
        private const val MAX_LOG_LINES = 1000

        // 1 秒刷新一次
        private const val REFRESH_INTERVAL = 1000L
    }

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: android.util.AttributeSet) : super(context, attrs)
    constructor(context: Context, attrs: android.util.AttributeSet, defStyleAttr: Int) : super(
        context, attrs, defStyleAttr
    )

    // 此wmParams为获取的全局变量，用以保存悬浮窗口的属性
    private var mWmParams: WindowManager.LayoutParams = WindowManager.LayoutParams()


    private lateinit var logListView: ListView
    private lateinit var logAdapter: LogAdapter
    private lateinit var refreshHandler: Handler
    private lateinit var refreshRunnable: Runnable
    private var windowManager: WindowManager? = null

    // 存储应用内日志的缓冲区
    private val logBuffer = LinkedList<LogUtil.LogBean>()

    init {
        initView()
    }

    private fun initView() {
        orientation = VERTICAL
        mWmParams.type = WindowManager.LayoutParams.TYPE_APPLICATION
        //FLAG_FULLSCREEN充满屏幕，隐藏所有的装饰物(比如状态栏)
        mWmParams.flags =
            (WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_FULLSCREEN)

        // 调整悬浮窗口至左中
        mWmParams.gravity = Gravity.START or Gravity.CENTER

        // 创建 ListView 用于显示日志
        logListView = ListView(context).apply {
            // 半透明背景
            setBackgroundColor(Color.argb(128, 0, 0, 0))
        }
        logAdapter = LogAdapter()
        logListView.adapter = logAdapter
        
        val listLayoutParams = LayoutParams(LayoutParams.MATCH_PARENT, 0).apply {
            weight = 1f
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
                    gravity = Gravity.END
                    topMargin = 16
                }
        }.also { linearLayout.addView(it) }.setOnClickListener {
            synchronized(logBuffer) {
                logBuffer.clear()
            }
            logAdapter.notifyDataSetChanged()
        }
        Button(context).apply {
            text = "X"
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.DKGRAY)
            layoutParams =
                LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
                    gravity = Gravity.END
                    topMargin = 16
                }
        }.also { linearLayout.addView(it) }.setOnClickListener {
            dismiss()
        }
        addView(linearLayout)

        //设置悬浮窗口长宽
        mWmParams.width = 600
        mWmParams.height = 900
        mWmParams.format = PixelFormat.RGBA_8888
    }

    /**
     * LogUtil.LogListener 回调方法
     * 当有新的应用内日志产生时会调用此方法
     */
    override fun onLog(logBean: LogUtil.LogBean) {
        // 添加到缓冲区
        synchronized(logBuffer) {
            logBuffer.add(logBean)
            // 保持缓冲区大小在限制范围内
            if (logBuffer.size > MAX_LOG_LINES) {
                logBuffer.removeFirst()
            }
        }

        // 更新UI
        refreshLogs()
    }

    /**
     * 日志适配器
     */
    inner class LogAdapter : BaseAdapter() {
        override fun getCount(): Int {
            return synchronized(logBuffer) { logBuffer.size }
        }

        override fun getItem(position: Int): Any {
            return synchronized(logBuffer) { logBuffer[position] }
        }

        override fun getItemId(position: Int): Long {
            return position.toLong()
        }

        @SuppressLint("SetTextI18n")
        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            val view: TextView = if (convertView == null) {
                val textView = TextView(context)
                textView.setTextColor(Color.BLACK)
                textView.textSize = 11f
                textView.setPadding(10, 10, 10, 10)
                textView
            } else {
                convertView as TextView
            }

            val logBean = getItem(position) as LogUtil.LogBean
            val levelStr = when (logBean.level) {
                LogUtil.LEVEL.VERBOSE -> "V"
                LogUtil.LEVEL.DEBUG -> "D"
                LogUtil.LEVEL.INFO -> "I"
                LogUtil.LEVEL.WARN -> "W"
                LogUtil.LEVEL.ERROR -> "E"
                LogUtil.LEVEL.WFT -> "WTF"
            }

            // 设置不同级别日志的颜色
            when (logBean.level) {
                LogUtil.LEVEL.ERROR, LogUtil.LEVEL.WFT -> {
                    view.setTextColor(Color.RED)
                }
                LogUtil.LEVEL.WARN -> {
                    view.setTextColor(Color.YELLOW)
                }
                LogUtil.LEVEL.INFO -> {
                    view.setTextColor(Color.BLUE)
                }
                LogUtil.LEVEL.DEBUG -> {
                    view.setTextColor(Color.GREEN)
                }
                else -> {
                    view.setTextColor(Color.BLACK)
                }
            }

            view.text = "${formatTime(logBean.time)} ${levelStr}/${logBean.tag}: ${logBean.message}"
            return view
        }
    }

    /**
     * 刷新日志显示
     */
    private fun refreshLogs() {
        logAdapter.notifyDataSetChanged()
        // 自动滚动到底部
//        logListView.post {
//            logListView.setSelection(logAdapter.count - 1)
//        }
    }

    fun show(activity: Activity) {
        windowManager = activity.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        try {
            Log.d("DebugView", "show")
            windowManager?.addView(this, mWmParams)
            // 注册日志监听器
            LogUtil.registerLogListener(this)
            // 初始化刷新任务
            refreshHandler = Handler(Looper.getMainLooper())
            refreshRunnable = object : Runnable {
                override fun run() {
                    refreshLogs()
                    refreshHandler.postDelayed(this, REFRESH_INTERVAL)
                }
            }
            refreshHandler.post(refreshRunnable)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun dismiss() {
        refreshHandler.removeCallbacks(refreshRunnable)
//        job?.cancel()
        try {
            // 注销日志监听器
            LogUtil.unregisterLogListener(this)
            windowManager?.removeViewImmediate(this)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        Log.d("DebugView", "dismiss")
    }
}