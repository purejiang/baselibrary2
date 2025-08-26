//package com.cyanrain.baselibrary.debug
//
//import android.content.Context
//import android.graphics.Color
//import android.os.Handler
//import android.os.Looper
//import android.text.method.ScrollingMovementMethod
//import android.view.Gravity
//import android.view.View
//import android.view.ViewGroup
//import android.widget.Button
//import android.widget.LinearLayout
//import android.widget.ProgressBar
//import android.widget.TextView
//import kotlinx.coroutines.CoroutineScope
//import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.Job
//import kotlinx.coroutines.launch
//import kotlinx.coroutines.withContext
//import java.io.BufferedReader
//import java.io.InputStreamReader
//
//class LogDialog(context: Context) : LinearLayout(context), View.OnClickListener {
//    companion object{
//        private const val TAG = "LogDialog"
//        // 限制日志行数，避免内存溢出
//        private const val MAX_LOG_LINES = 2000
//        // 2 秒刷新一次
//        private const val REFRESH_INTERVAL = 2000L
//    }
//    private lateinit var logTextView: TextView
//    private lateinit var progressBar: ProgressBar
//    private lateinit var refreshHandler: Handler
//    private lateinit var refreshRunnable: Runnable
//
//    private var job: Job? = null
//    init {
//        orientation = VERTICAL
//        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
//        setBackgroundColor(Color.BLACK)
//
//        // 进度条
//        progressBar = ProgressBar(context).apply {
//            layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
//                gravity = Gravity.CENTER_HORIZONTAL
//            }
//            isIndeterminate = true
//            visibility = View.VISIBLE
//        }
//        addView(progressBar)
//
//        // 日志文本视图
//        logTextView = TextView(context).apply {
//            setTextColor(Color.WHITE)
//            textSize = 12f
//            setBackgroundColor(Color.BLACK)
//            setPadding(16, 16, 16, 16)
//            movementMethod = ScrollingMovementMethod()
//        }
//        addView(logTextView, LayoutParams(LayoutParams.MATCH_PARENT, 0).apply {
//            weight = 1f
//        })
//
//        // 清除按钮
//        Button(context).apply {
//            text = "清除"
//            setTextColor(Color.parseColor("#2196F3"))
//            setBackgroundColor(Color.parseColor("#E3F2FD"))
//            setOnClickListener(this@LogDialog)
//            layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
//                gravity = Gravity.END
//                topMargin = 16
//            }
//        }.also { addView(it) }
//        // 初始化刷新任务
//        refreshHandler = Handler(Looper.getMainLooper())
//        refreshRunnable = object : Runnable {
//            override fun run() {
//                loadLogcat()
//                refreshHandler.postDelayed(this, REFRESH_INTERVAL)
//            }
//        }
//        // 加载日志
//        loadLogcat()
//    }
//
//    private fun loadLogcat() {
//        job = CoroutineScope(Dispatchers.IO).launch {
//            try {
//                val process = Runtime.getRuntime().exec("logcat -d -v time")
//                val reader = BufferedReader(InputStreamReader(process.inputStream))
//                val errorReader = BufferedReader(InputStreamReader(process.errorStream))
//
//                val logOutput = StringBuilder()
//                var line: String?
//                var lineCount = 0
//
//                // 读取日志内容（限制行数）
//                while ((reader.readLine().also { line = it }) != null && lineCount < MAX_LOG_LINES) {
//                    logOutput.append(line).append("\n")
//                    lineCount++
//                }
//
//                // 错误流处理
//                while ((errorReader.readLine().also { line = it }) != null) {
//                    logOutput.append("[ERROR] ").append(line).append("\n")
//                }
//
//                reader.close()
//                errorReader.close()
//                process.destroy()
//
//                withContext(Dispatchers.Main) {
//                    progressBar.visibility = View.GONE
//                    logTextView.text = logOutput.toString()
//                }
//            } catch (e: Exception) {
//                withContext(Dispatchers.Main) {
//                    progressBar.visibility = View.GONE
//                    logTextView.text = "无法读取日志：${e.message}"
//                }
//            }
//        }
//    }
//
//    override fun onClick(v: View?) {
//        logTextView.text = ""
//    }
//
//    /**
//     * 当 LogDialog 被添加到窗口时启动定时刷新
//     */
//    override fun onAttachedToWindow() {
//        super.onAttachedToWindow()
//        refreshHandler.post(refreshRunnable)
//    }
//
//    /**
//     * 当 LogDialog 被移除时停止刷新
//     */
//    override fun onDetachedFromWindow() {
//        super.onDetachedFromWindow()
//        refreshHandler.removeCallbacks(refreshRunnable)
//        job?.cancel()
//    }
//
//    fun dismiss() {
//        (parent as? ViewGroup)?.removeView(this)
//    }
//}