package com.cyanrain.baselibrary.debug

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Point
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.LinearLayout.LayoutParams
import androidx.core.view.isNotEmpty

class FloatingBall(private val context: Context) {

    // 悬浮球容器视图
    private lateinit var floatingContainer: FrameLayout

    // 窗口管理器
    private val windowManager: WindowManager by lazy {
        context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    }

    // 布局参数
    private val mLayoutParams: WindowManager.LayoutParams by lazy {
        WindowManager.LayoutParams().apply {
            width = ViewGroup.LayoutParams.WRAP_CONTENT
            height = ViewGroup.LayoutParams.WRAP_CONTENT
            gravity = Gravity.TOP or Gravity.START
            flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
            format = android.graphics.PixelFormat.TRANSLUCENT
            type = WindowManager.LayoutParams.TYPE_APPLICATION
        }
    }

    // 屏幕尺寸
    private val screenSize: Point by lazy {
        Point().apply { windowManager.defaultDisplay.getSize(this) }
    }

    // 处理贴边逻辑的Handler
    private val handler = Handler(Looper.getMainLooper())

    // 拖动相关变量
    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f

    // 是否正在拖动
    private var isDragging = false

    // 自动贴边延迟时间 (毫秒)
    private companion object {
        const val AUTO_ADSORB_DELAY = 100L
    }

    // 悬浮球配置
    data class Config(
        val width: Int = 60,  // 悬浮球宽(dp)
        val height: Int = 60,  // 悬浮球高(dp)
        val background: Drawable? = null, // 背景
        val contentView: View? = null,
    )

    private var config = Config()

    // 设置悬浮球配置
    fun setConfig(config: Config) {
        this.config = config
//        updateBallAppearance()
    }

    // 创建悬浮球
    fun show() {
        // 如果已经存在，先移除
        if (::floatingContainer.isInitialized) {
            close()
        }

        // 创建悬浮球视图
        floatingContainer = FrameLayout(context).apply {
            // 设置背景
            config.background?.let {
                background = it
            }

            layoutParams =
                FrameLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
            setOnTouchListener(ballTouchListener)

            if (config.contentView != null) {
                addView(config.contentView)
            }
        }
        // 初始位置（屏幕右下角）
        mLayoutParams.x = 0
        mLayoutParams.y = screenSize.y / 2
        val widthInPx = (config.width * context.resources.displayMetrics.density).toInt()
        Log.d("FloatingBall", "widthInPx: $widthInPx")
        val heightInPx = (config.height * context.resources.displayMetrics.density).toInt()
        Log.d("FloatingBall", "heightInPx: $heightInPx")
        mLayoutParams.width = widthInPx
        mLayoutParams.height = heightInPx
        // 添加到窗口
        windowManager.addView(floatingContainer, mLayoutParams)
    }

    // 创建圆角背景
    private fun createRoundRectDrawable(
        radiusDp: Float,
        color: Int,
    ): Drawable {
        val radiusPx = (radiusDp * context.resources.displayMetrics.density)
        return GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(color)
            cornerRadius = radiusPx
        }
    }

    // 更新悬浮球外观
//    private fun updateBallAppearance() {
//        if (::floatingContainer.isInitialized) {
//            floatingBall.text = config.text
//            floatingBall.setTextColor(config.textColor)
//            floatingBall.textSize = config.textSize

    // 设置背景
//            config.background?.let {
//                background = it
//            }

    // 设置大小
//            val sizeInPx = (config.size * context.resources.displayMetrics.density).toInt()
//            floatingContainer.layoutParams = FrameLayout.LayoutParams(sizeInPx, sizeInPx)

    // 刷新视图
//            windowManager.updateViewLayout(floatingContainer, layoutParams)
//        }
//    }

    // 悬浮球触摸监听器
    @SuppressLint("ClickableViewAccessibility")
    private val ballTouchListener = View.OnTouchListener { _, event ->
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                // 记录初始位置
                initialX = mLayoutParams.x
                initialY = mLayoutParams.y
                initialTouchX = event.rawX
                initialTouchY = event.rawY
                isDragging = false

                // 取消自动贴边
                handler.removeCallbacks(autoAdsorbRunnable)
                true
            }

            MotionEvent.ACTION_MOVE -> {
                // 计算偏移量
                val offsetX = (event.rawX - initialTouchX).toInt()
                val offsetY = (event.rawY - initialTouchY).toInt()

                // 更新位置
                mLayoutParams.x = initialX + offsetX
                mLayoutParams.y = initialY + offsetY

                // 确保位置在屏幕范围内
                constrainToScreenBounds()

                // 应用新位置
                windowManager.updateViewLayout(floatingContainer, mLayoutParams)
                isDragging = true
                true
            }

            MotionEvent.ACTION_UP -> {
                if (isDragging) {
                    // 启动延迟贴边
                    handler.postDelayed(autoAdsorbRunnable, AUTO_ADSORB_DELAY)
                }
                true
            }

            else -> false
        }
    }

    // 确保悬浮球在屏幕范围内
    private fun constrainToScreenBounds() {
        // 约束X坐标
        mLayoutParams.x = mLayoutParams.x.coerceIn(0, screenSize.x - floatingContainer.width)

        // 约束Y坐标
        mLayoutParams.y = mLayoutParams.y.coerceIn(0, screenSize.y - floatingContainer.height)
    }

    // 自动贴边逻辑
    private val autoAdsorbRunnable = Runnable {
        // 计算距离左右边的距离
        val distanceToLeft = mLayoutParams.x
        val distanceToRight = screenSize.x - mLayoutParams.x - floatingContainer.width

        // 判断最近边缘并吸附
        if (distanceToLeft < distanceToRight) {
            // 贴左
            mLayoutParams.x = 0
        } else {
            // 贴右
            mLayoutParams.x = screenSize.x - floatingContainer.width
        }

        // 应用新位置
        windowManager.updateViewLayout(floatingContainer, mLayoutParams)
    }

    // 移除悬浮球
    fun close() {
        if (::floatingContainer.isInitialized) {
            try {
                handler.removeCallbacks(autoAdsorbRunnable)
                windowManager.removeView(floatingContainer)
            } catch (e: Exception) {
                e.printStackTrace()
            }

        }
    }

    // 设置点击监听器
    fun setOnClickListener(listener: View.OnClickListener) {
        if (::floatingContainer.isInitialized) {
            floatingContainer.setOnClickListener(listener)
        }
    }
    
    // 获取内容视图
    fun getContent(): View? {
        return if (::floatingContainer.isInitialized && floatingContainer.isNotEmpty()) {
            floatingContainer.getChildAt(0)
        } else {
            null
        }
    }
}