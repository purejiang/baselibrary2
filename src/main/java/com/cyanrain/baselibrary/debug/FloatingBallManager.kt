//package com.cyanrain.baselibrary.debug
//
//import android.annotation.SuppressLint
//import android.app.Activity
//import android.content.Context
//import android.graphics.Point
//import android.os.Handler
//import android.os.Looper
//import android.util.Log
//import android.view.Gravity
//import android.view.MotionEvent
//import android.view.View
//import android.view.ViewGroup
//import android.view.WindowManager
//import android.widget.FrameLayout
//import android.widget.TextView
//
//class FloatingBallManager(private val context: Context) {
//
//    // 悬浮球视图
//    private lateinit var floatingBall: TextView
//
//    // 窗口管理器
//    private val windowManager: WindowManager by lazy {
//        context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
//    }
//
//    // 布局参数
//    private val layoutParams: WindowManager.LayoutParams by lazy {
//        WindowManager.LayoutParams().apply {
//            width = ViewGroup.LayoutParams.WRAP_CONTENT
//            height = ViewGroup.LayoutParams.WRAP_CONTENT
//            gravity = Gravity.TOP or Gravity.START
//            flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
//            format = android.graphics.PixelFormat.TRANSLUCENT
//            type = WindowManager.LayoutParams.TYPE_APPLICATION
//        }
//    }
//
//    // 屏幕尺寸
//    private val screenSize: Point by lazy {
//        Point().apply { windowManager.defaultDisplay.getSize(this) }
//    }
//
//
//
//    // 处理贴边逻辑的Handler
//    private val handler = Handler(Looper.getMainLooper())
//
//    // 拖动相关变量
//    private var initialX = 0
//    private var initialY = 0
//    private var initialTouchX = 0f
//    private var initialTouchY = 0f
//    // 是否正在拖动
//    private var isDragging = false
//
//    // 自动贴边延迟时间 (毫秒)
//    private companion object {
//        const val AUTO_ADSORB_DELAY = 100L
//    }
//
//    // 悬浮球配置
//    data class Config(
//        val size: Int = 60, // 悬浮球大小(dp)
//        val text: String = "⚪", // 悬浮球文本
//        val textSize: Float = 24f, // 文本大小(sp)
//        val backgroundColor: Int = 0x88000000.toInt(), // 背景颜色 (带透明度)
//        val textColor: Int = 0xFFFFFFFF.toInt(), // 文本颜色
//        val cornerRadius: Float = 30f // 圆角半径(dp)
//    )
//
//    private var config = Config()
//
//    // 设置悬浮球配置
//    fun setConfig(config: Config) {
//        this.config = config
//        updateBallAppearance()
//    }
//
//    // 创建悬浮球
//    fun createFloatingBall() {
//        // 如果已经存在，先移除
//        if (::floatingBall.isInitialized) {
//            removeFloatingBall()
//        }
//
//        // 创建悬浮球视图
//        floatingBall = TextView(context).apply {
//            text = config.text
//            textSize = config.textSize
//            setTextColor(config.textColor)
//            gravity = Gravity.CENTER
//
//            // 设置背景（带圆角）
//            background = createRoundRectDrawable(config.cornerRadius, config.backgroundColor)
//
//            // 设置大小
//            val sizeInPx = (config.size * context.resources.displayMetrics.density).toInt()
//            layoutParams = FrameLayout.LayoutParams(sizeInPx, sizeInPx)
//
//            // 设置触摸监听
//            this.setOnTouchListener(ballTouchListener)
//        }
//        // 初始位置（屏幕右下角）
//        layoutParams.x = 0
//        layoutParams.y = screenSize.y/2
//
//        // 添加到窗口
//        windowManager.addView(floatingBall, layoutParams)
//    }
//
//    // 创建圆角背景
//    private fun createRoundRectDrawable(radiusDp: Float, color: Int): android.graphics.drawable.Drawable {
//        val radiusPx = (radiusDp * context.resources.displayMetrics.density)
//        return android.graphics.drawable.GradientDrawable().apply {
//            shape = android.graphics.drawable.GradientDrawable.OVAL
//            setColor(color)
//            cornerRadius = radiusPx
//        }
//    }
//
//    // 更新悬浮球外观
//    private fun updateBallAppearance() {
//        if (::floatingBall.isInitialized) {
//            floatingBall.text = config.text
//            floatingBall.setTextColor(config.textColor)
//            floatingBall.textSize = config.textSize
//
//            // 设置背景
//            floatingBall.background = createRoundRectDrawable(config.cornerRadius, config.backgroundColor)
//
//            // 设置大小
//            val sizeInPx = (config.size * context.resources.displayMetrics.density).toInt()
//            floatingBall.layoutParams = FrameLayout.LayoutParams(sizeInPx, sizeInPx)
//
//            // 刷新视图
//            windowManager.updateViewLayout(floatingBall, layoutParams)
//        }
//    }
//
//    // 悬浮球触摸监听器
//    @SuppressLint("ClickableViewAccessibility")
//    private val ballTouchListener = View.OnTouchListener { _, event ->
//        when (event.action) {
//            MotionEvent.ACTION_DOWN -> {
//                // 记录初始位置
//                initialX = layoutParams.x
//                initialY = layoutParams.y
//                initialTouchX = event.rawX
//                initialTouchY = event.rawY
//                isDragging = false
//
//                // 取消自动贴边
//                handler.removeCallbacks(autoAdsorbRunnable)
//                true
//            }
//            MotionEvent.ACTION_MOVE -> {
//                // 计算偏移量
//                val offsetX = (event.rawX - initialTouchX).toInt()
//                val offsetY = (event.rawY - initialTouchY).toInt()
//
//                // 更新位置
//                layoutParams.x = initialX + offsetX
//                layoutParams.y = initialY + offsetY
//
//                // 确保位置在屏幕范围内
//                constrainToScreenBounds()
//
//                // 应用新位置
//                windowManager.updateViewLayout(floatingBall, layoutParams)
//                isDragging = true
//                true
//            }
//            MotionEvent.ACTION_UP -> {
//                if (isDragging) {
//                    // 启动延迟贴边
//                    handler.postDelayed(autoAdsorbRunnable, AUTO_ADSORB_DELAY)
//                }
//                true
//            }
//            else -> false
//        }
//    }
//
//    // 确保悬浮球在屏幕范围内
//    private fun constrainToScreenBounds() {
//        // 约束X坐标
//        layoutParams.x = layoutParams.x.coerceIn(0, screenSize.x - floatingBall.width)
//
//        // 约束Y坐标
//        layoutParams.y = layoutParams.y.coerceIn(0, screenSize.y - floatingBall.height)
//    }
//
//    // 自动贴边逻辑
//    private val autoAdsorbRunnable = Runnable {
//        // 计算距离左右边的距离
//        val distanceToLeft = layoutParams.x
//        val distanceToRight = screenSize.x - layoutParams.x - floatingBall.width
//
//        // 判断最近边缘并吸附
//        if (distanceToLeft < distanceToRight) {
//            // 贴左
//            layoutParams.x = 0
//        } else {
//            // 贴右
//            layoutParams.x = screenSize.x - floatingBall.width
//        }
//
//        // 应用新位置
//        windowManager.updateViewLayout(floatingBall, layoutParams)
//    }
//
//    // 移除悬浮球
//    fun removeFloatingBall() {
//        if (::floatingBall.isInitialized) {
//            windowManager.removeView(floatingBall)
//            handler.removeCallbacks(autoAdsorbRunnable)
//        }
//    }
//
//    // 设置点击监听器
//    fun setOnClickListener(listener: View.OnClickListener) {
//        if (::floatingBall.isInitialized) {
//            floatingBall.setOnClickListener(listener)
//        }
//    }
//}