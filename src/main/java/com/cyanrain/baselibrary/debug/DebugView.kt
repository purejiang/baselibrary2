//package com.cyanrain.baselibrary.debug
//
//import android.annotation.SuppressLint
//import android.app.Activity
//import android.content.Context
//import android.content.res.Configuration
//import android.graphics.Color
//import android.graphics.PixelFormat
//import android.os.Handler
//import android.os.Looper
//import android.util.Log
//import android.view.Gravity
//import android.view.MotionEvent
//import android.view.View
//import android.view.View.OnTouchListener
//import android.view.ViewGroup
//import android.view.WindowManager
//import android.widget.BaseAdapter
//import android.widget.Button
//import android.widget.LinearLayout
//import android.widget.ListView
//import android.widget.TextView
//import com.cyanrain.baselibrary.log.LogUtil
//import com.cyanrain.baselibrary.phone.ScreenUtils.getScreenHeight
//import com.cyanrain.baselibrary.phone.ScreenUtils.getStatusBarHeight
//import com.cyanrain.baselibrary.utils.CommonUtils.Companion.formatTime
//import java.io.BufferedReader
//import java.io.InputStreamReader
//import java.util.LinkedList
//import kotlin.math.abs
//
//class DebugView : LinearLayout, LogUtil.LogListener, OnTouchListener {
//    companion object {
//        private const val TAG = "LogDialog"
//
//        // 限制日志行数，避免内存溢出
//        private const val MAX_LOG_LINES = 1000
//
//        // 1 秒刷新一次
//        private const val REFRESH_INTERVAL = 1000L
//    }
//
//    constructor(context: Context) : super(context)
//    constructor(context: Context, attrs: android.util.AttributeSet) : super(context, attrs)
//    constructor(context: Context, attrs: android.util.AttributeSet, defStyleAttr: Int) : super(
//        context, attrs, defStyleAttr
//    )
//
//    // 此wmParams为获取的全局变量，用以保存悬浮窗口的属性
//    private var mWmParams: WindowManager.LayoutParams = WindowManager.LayoutParams()
//    private var mWindowManager: WindowManager? = null
//    private var mDownX = 0f
//    private var mDownY = 0f
//
//    private lateinit var logListView: ListView
//    private lateinit var logAdapter: LogAdapter
//    private lateinit var refreshHandler: Handler
//    private lateinit var refreshRunnable: Runnable
//
//    // 存储应用内日志的缓冲区
//    private val appLogBuffer = LinkedList<LogUtil.LogBean>()
//
//    // 存储系统日志的缓冲区
//    private val systemLogBuffer = LinkedList<String>()
//
//    // 存储从文件读取的日志缓冲区
//    private val fileLogBuffer = LinkedList<LogUtil.LogBean>()
//
//    // 日志类型标识
//    private var logMode = LogMode.APP_LOG
//
//    init {
//        initView()
//    }
//
//    @SuppressLint("ClickableViewAccessibility")
//    private fun initView() {
//        orientation = VERTICAL
//        mWmParams.type = WindowManager.LayoutParams.TYPE_APPLICATION
//        //FLAG_FULLSCREEN充满屏幕，隐藏所有的装饰物(比如状态栏)
//        mWmParams.flags =
//            (WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_FULLSCREEN)
//
//
//        // 调整悬浮窗口至左中
//        mWmParams.gravity = Gravity.START or Gravity.TOP
//        mWmParams.x = 100
//        mWmParams.y = 300
//
//        // 创建 ListView 用于显示日志
//        logListView = ListView(context).apply {
//            // 半透明背景
//            setBackgroundColor(Color.argb(128, 0, 0, 0))
//        }
//        logAdapter = LogAdapter()
//        logListView.adapter = logAdapter
//
//        val listLayoutParams = LayoutParams(LayoutParams.MATCH_PARENT, 0).apply {
//            weight = 1f
//        }
//        logListView.layoutParams = listLayoutParams
//        addView(logListView)
//
//        val linearLayout = LinearLayout(context).apply {
//            orientation = HORIZONTAL
//            layoutParams =
//                LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
//                    gravity = Gravity.CENTER
//                }
//        }
//
//        // 切换日志按钮
//        Button(context).apply {
//            text = "系统日志"
//            setTextColor(Color.WHITE)
//            setBackgroundColor(Color.GRAY)
//            layoutParams =
//                LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
//                    gravity = Gravity.CENTER
//                    topMargin = 16
//                }
//        }.also { linearLayout.addView(it) }.setOnClickListener {
//            when (logMode) {
//                LogMode.APP_LOG -> {
//                    logMode = LogMode.SYSTEM_LOG
//                    (it as Button).text = "文件日志"
////                    loadSystemLogs()
//                }
//                LogMode.SYSTEM_LOG -> {
//                    logMode = LogMode.FILE_LOG
//                    (it as Button).text = "应用日志"
//                    loadFileLogs()
//                }
//                LogMode.FILE_LOG -> {
//                    logMode = LogMode.APP_LOG
//                    (it as Button).text = "系统日志"
//                }
//            }
//            refreshLogs()
//        }
//
//        // clear按钮
//        Button(context).apply {
//            text = "clear"
//            setTextColor(Color.WHITE)
//            setBackgroundColor(Color.GRAY)
//            layoutParams =
//                LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
//                    gravity = Gravity.CENTER
//                    topMargin = 16
//                    marginStart = 16
//                }
//        }.also { linearLayout.addView(it) }.setOnClickListener {
//            when (logMode) {
//                LogMode.APP_LOG -> {
//                    synchronized(appLogBuffer) {
//                        appLogBuffer.clear()
//                    }
//                }
//                LogMode.SYSTEM_LOG -> {
//                    synchronized(systemLogBuffer) {
//                        systemLogBuffer.clear()
//                    }
//                }
//                LogMode.FILE_LOG -> {
//                    synchronized(fileLogBuffer) {
//                        fileLogBuffer.clear()
//                    }
//                }
//            }
//            logAdapter.notifyDataSetChanged()
//        }
//        Button(context).apply {
//            text = "close"
//            setTextColor(Color.WHITE)
//            setBackgroundColor(Color.DKGRAY)
//            layoutParams =
//                LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
//                    gravity = Gravity.END
//                    topMargin = 16
//                    marginStart = 16
//                }
//        }.also { linearLayout.addView(it) }.setOnClickListener {
//            dismiss()
//        }
//        addView(linearLayout)
//
//        //设置悬浮窗口长宽
//        mWmParams.width = 800
//        mWmParams.height = 1000
//        mWmParams.format = PixelFormat.RGBA_8888
//
//        this.setOnTouchListener(this)
//    }
//
//    /**
//     * LogUtil.LogListener 回调方法
//     * 当有新的应用内日志产生时会调用此方法
//     */
//    override fun onLog(logBean: LogUtil.LogBean) {
//        // 添加到缓冲区
//        synchronized(appLogBuffer) {
//            appLogBuffer.add(logBean)
//            // 保持缓冲区大小在限制范围内
//            if (appLogBuffer.size > MAX_LOG_LINES) {
//                appLogBuffer.removeFirst()
//            }
//        }
//
//        // 更新UI
//        refreshLogs()
//    }
//
//    /**
//     * 日志适配器
//     */
//    inner class LogAdapter : BaseAdapter() {
//        override fun getCount(): Int {
//            return when (logMode) {
//                LogMode.APP_LOG -> synchronized(appLogBuffer) { appLogBuffer.size }
//                LogMode.SYSTEM_LOG -> synchronized(systemLogBuffer) { systemLogBuffer.size }
//                LogMode.FILE_LOG -> synchronized(fileLogBuffer) { fileLogBuffer.size }
//            }
//        }
//
//        override fun getItem(position: Int): Any {
//            return when (logMode) {
//                LogMode.APP_LOG -> synchronized(appLogBuffer) { appLogBuffer[position] }
//                LogMode.SYSTEM_LOG -> synchronized(systemLogBuffer) { systemLogBuffer[position] }
//                LogMode.FILE_LOG -> synchronized(fileLogBuffer) { fileLogBuffer[position] }
//            }
//        }
//
//        override fun getItemId(position: Int): Long {
//            return position.toLong()
//        }
//
//        @SuppressLint("SetTextI18n")
//        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
//            val view: TextView = if (convertView == null) {
//                val textView = TextView(context)
//                textView.setTextColor(Color.WHITE)
//                textView.textSize = 11f
//                textView.setPadding(10, 10, 10, 10)
//                textView
//            } else {
//                convertView as TextView
//            }
//
//            when (logMode) {
//                LogMode.APP_LOG -> {
//                    val logBean = getItem(position) as LogUtil.LogBean
//                    setLogItemStyle(view, logBean.level, logBean)
//                }
//                LogMode.FILE_LOG -> {
//                    val logBean = getItem(position) as LogUtil.LogBean
//                    setLogItemStyle(view, logBean.level, logBean)
//                }
//                LogMode.SYSTEM_LOG -> {
//                    val logLine = getItem(position) as String
//                    // 系统日志着色
//                    when {
//                        logLine.contains(" E/") || logLine.contains(" ERROR") -> {
//                            view.setTextColor(Color.RED)
//                        }
//                        logLine.contains(" W/") || logLine.contains(" WARN") -> {
//                            view.setTextColor(Color.YELLOW)
//                        }
//                        logLine.contains(" I/") || logLine.contains(" INFO") -> {
//                            view.setTextColor(Color.BLUE)
//                        }
//                        logLine.contains(" D/") || logLine.contains(" DEBUG") -> {
//                            view.setTextColor(Color.GREEN)
//                        }
//                        else -> {
//                            view.setTextColor(Color.WHITE)
//                        }
//                    }
//                    view.text = logLine
//                }
//            }
//
//            return view
//        }
//
//        private fun setLogItemStyle(textView: TextView, level: LogUtil.LEVEL, logBean: LogUtil.LogBean) {
//            val levelStr = when (level) {
//                LogUtil.LEVEL.VERBOSE -> "V"
//                LogUtil.LEVEL.DEBUG -> "D"
//                LogUtil.LEVEL.INFO -> "I"
//                LogUtil.LEVEL.WARN -> "W"
//                LogUtil.LEVEL.ERROR -> "E"
//                LogUtil.LEVEL.WFT -> "WTF"
//            }
//
//            // 设置不同级别日志的颜色
//            when (level) {
//                LogUtil.LEVEL.ERROR, LogUtil.LEVEL.WFT -> {
//                    textView.setTextColor(Color.RED)
//                }
//                LogUtil.LEVEL.WARN -> {
//                    textView.setTextColor(Color.YELLOW)
//                }
//                LogUtil.LEVEL.INFO -> {
//                    textView.setTextColor(Color.BLUE)
//                }
//                LogUtil.LEVEL.DEBUG -> {
//                    textView.setTextColor(Color.GREEN)
//                }
//                else -> {
//                    textView.setTextColor(Color.WHITE)
//                }
//            }
//
//            textView.text = "${formatTime(logBean.time)} ${levelStr}/${logBean.tag}: ${logBean.message}"
//        }
//    }
//
//    /**
//     * 刷新日志显示
//     */
//    private fun refreshLogs() {
//        logAdapter.notifyDataSetChanged()
//        // 自动滚动到底部
//        logListView.post {
//            logListView.setSelection(logAdapter.count - 1)
//        }
//    }
//
//    /**
//     * 从文件加载日志
//     */
//    private fun loadFileLogs() {
//        Thread {
//            try {
//                val logsFromFile = LogUtil.getLogListFromFile()
//
//                synchronized(fileLogBuffer) {
//                    fileLogBuffer.clear()
//                    // 只保留最新的MAX_LOG_LINES条日志
//                    val startIndex = maxOf(0, logsFromFile.size - MAX_LOG_LINES)
//                    fileLogBuffer.addAll(logsFromFile.subList(startIndex, logsFromFile.size))
//                }
//
//                // 更新UI
//                post {
//                    if (logMode == LogMode.FILE_LOG) {
//                        refreshLogs()
//                    }
//                }
//            } catch (e: Exception) {
//                Log.e(TAG, "Failed to load file logs", e)
//            }
//        }.start()
//    }
//
//    fun show() {
////        windowManager = activity.getSystemService(Context.WINDOW_SERVICE) as WindowManager
//        try {
//            Log.d("DebugView", "show")
////            windowManager?.addView(this, mWmParams)
//            // 注册日志监听器
//            LogUtil.registerLogListener(this)
//            // 初始化刷新任务
//            refreshHandler = Handler(Looper.getMainLooper())
//            refreshRunnable = object : Runnable {
//                override fun run() {
//                    when (logMode) {
////                        LogMode.SYSTEM_LOG -> loadSystemLogs()
//                        LogMode.FILE_LOG -> loadFileLogs()
//                        else -> {} // APP_LOG模式通过监听器实时更新，不需要定时刷新
//                    }
//                    refreshHandler.postDelayed(this, REFRESH_INTERVAL)
//                }
//            }
//            refreshHandler.post(refreshRunnable)
//        } catch (e: Exception) {
//            e.printStackTrace()
//        }
//    }
//
//    /**
//     * 更新悬浮窗的坐标位置
//     *
//     * @param event 手势事件
//     */
//    private fun updatePosition(event: MotionEvent) {
//        // 更新浮动窗口位置参数,x是鼠标在屏幕的位置，mTouchStartX是鼠标在图片的位置
//        val ori = context.resources.configuration.orientation //获取屏幕方向
//        if (ori == Configuration.ORIENTATION_LANDSCAPE) {
//            // 横屏调整
//            mWmParams.x = (event.rawX - context.getStatusBarHeight() - this.width / 2).toInt()
//            mWmParams.y = (event.rawY - context.getScreenHeight() / 2).toInt()
//        } else if (ori == Configuration.ORIENTATION_PORTRAIT) {
//            // 竖屏调整
//            mWmParams.x = (event.rawX - this.width / 2).toInt()
//            mWmParams.y = (event.rawY - context.getScreenHeight() / 2 - context.getStatusBarHeight()).toInt()
//        }
//        mWindowManager?.updateViewLayout(this, mWmParams)
//    }
//
//    override fun onTouch(view: View, event: MotionEvent?): Boolean {
//        if (event == null) {
//            return false
//        }
//        var isTouch = false
//        when (event.action) {
//            MotionEvent.ACTION_DOWN -> {
//                mDownX = event.rawX
//                mDownY = event.rawY
//            }
//
//            MotionEvent.ACTION_MOVE ->{
//                Log.d("DebugView", "onTouch:${event.rawX},${event.rawY}")
//                // 滑动的距离太近就不做滑动的处理
//                if (abs(event.rawX - mDownX) > this.width / 3 || abs(event.rawY - mDownY) > this.height / 3) {
//                    updatePosition(event)
//                }
//            }
//
//
//            MotionEvent.ACTION_UP -> {
//                val upx = event.rawX
//                val upy = event.rawY
//                if (abs(mDownX - upx) > 5 || abs(mDownY - upy) > 5) isTouch = true
//            }
//
//            else -> {}
//        }
//        return isTouch
//    }
//
//    private fun dismiss() {
//        refreshHandler.removeCallbacks(refreshRunnable)
//        try {
//            // 注销日志监听器
//            LogUtil.unregisterLogListener(this)
//            mWindowManager?.removeViewImmediate(this)
//        } catch (e: Exception) {
//            e.printStackTrace()
//        }
//        Log.d("DebugView", "dismiss")
//    }
//
//    /**
//     * 日志模式枚举
//     */
//    private enum class LogMode {
//        APP_LOG,     // 应用内日志（实时）
//        SYSTEM_LOG,  // 系统日志（logcat）
//        FILE_LOG     // 文件日志（从日志文件读取）
//    }
//}