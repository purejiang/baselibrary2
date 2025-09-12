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
//import android.view.DisplayCutout
//import android.view.Gravity
//import android.view.MotionEvent
//import android.view.View
//import android.view.View.OnTouchListener
//import android.view.WindowManager
//import android.widget.LinearLayout
//import android.widget.TextView
//import com.cyanrain.baselibrary.phone.ScreenUtils.getScreenHeight
//import com.cyanrain.baselibrary.phone.ScreenUtils.getScreenWidth
//import com.cyanrain.baselibrary.phone.ScreenUtils.getStatusBarHeight
//import kotlin.math.abs
//
///**
// * sdk悬浮窗
// *
// * @author purejiang
// * @date 2025/8/11
// */
//@SuppressLint("ClickableViewAccessibility")
//class DebugFloatView : LinearLayout, OnTouchListener {
//
//    companion object {
//        private const val TAG = "DebugFloatView"
//    }
//
//    constructor(context: Context) : super(context)
//    constructor(context: Context, attrs: android.util.AttributeSet) : super(context, attrs)
//    constructor(context: Context, attrs: android.util.AttributeSet, defStyleAttr: Int) : super(
//        context, attrs, defStyleAttr
//    )
//
//    private var mDownX = 0f
//    private var mDownY = 0f
//
//    // 此wmParams为获取的全局变量，用以保存悬浮窗口的属性
//    private var mWmParams: WindowManager.LayoutParams = WindowManager.LayoutParams()
//    private var mWindowManager: WindowManager? = null
//
//    // 是否已经隐藏
//    private var mIsHide = false
//
//    private val mHandler = Handler(Looper.getMainLooper())
//
//    private var mDebugTextView: TextView? = null
//
//    private var mCutoutHeight: Int = 0
//
//    init {
//        mWindowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
//
//        mWmParams.type = WindowManager.LayoutParams.TYPE_APPLICATION
//        //FLAG_FULLSCREEN充满屏幕，隐藏所有的装饰物(比如状态栏)
//        mWmParams.flags =
//            (WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_FULLSCREEN)
//
//        // 调整悬浮窗口至左中
//        mWmParams.gravity = Gravity.START or Gravity.CENTER
//        //设置悬浮窗口长宽
//        mWmParams.width = WindowManager.LayoutParams.WRAP_CONTENT
//        mWmParams.height = WindowManager.LayoutParams.WRAP_CONTENT
//        mWmParams.format = PixelFormat.RGBA_8888
//
//
//        mDebugTextView = TextView(context)
//        mDebugTextView?.setTextColor(Color.RED)
//        mDebugTextView?.setBackgroundColor(Color.GREEN)
//        mDebugTextView?.setPadding(10, 10, 10, 10)
//
//        addView(mDebugTextView)
//
//        updateFloat(true)
//
//        this.setOnTouchListener(this)
//    }
//
//
//    public fun show(cutoutHeight: Int) {
//        try {
//            mCutoutHeight = cutoutHeight
//            Log.d(TAG, "show:${mCutoutHeight}")
//            mWindowManager?.addView(this, mWmParams)
//            autoHide()
//        } catch (e: Exception) {
//            e.printStackTrace()
//        }
//    }
//
//    public fun dismiss() {
//        try {
//            mWindowManager?.removeViewImmediate(this)
//        } catch (e: Exception) {
//            e.printStackTrace()
//        }
//    }
//
//    private fun updateFloat(isFull: Boolean) {
//        mIsHide = !isFull
//        if (isFull) {
//            mDebugTextView?.text = "DebugView"
//        } else {
//            mDebugTextView?.text = "Debug"
//        }
//    }
//
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
//            mWmParams.x = (event.rawX - this.width).toInt()
//            mWmParams.y = (event.rawY - context.getScreenHeight() / 2 - context.getStatusBarHeight() + mCutoutHeight).toInt()
//        }
//        mWindowManager?.updateViewLayout(this, mWmParams)
//    }
//
//    /**
//     * 悬浮窗贴边
//     *
//     * @param event 手势事件
//     */
//    private fun jumpToSide(event: MotionEvent) {
//        // 超过一半就是右边
//        val isRight = event.rawX > context.getScreenWidth() / 2
//        mWmParams.x = if (isRight) context.getScreenWidth() else 0
//        mWindowManager?.updateViewLayout(this, mWmParams)
//        autoHide()
//    }
//
//    protected fun autoHide() {
//        mHandler.postDelayed(
//            {
//                updateFloat(false)
//            }, 3000
//        )
//    }
//
//
//    override fun onTouch(v: View, event: MotionEvent?): Boolean {
//        if (event == null) {
//            return false
//        }
//        var isTouch = false
//        when (event.action) {
//            MotionEvent.ACTION_DOWN -> {
//                mDownX = event.rawX
//                mDownY = event.rawY
//                mHandler.removeCallbacksAndMessages(null)
//                if (mIsHide) {
//                    updateFloat(true)
//                }
//            }
//
//            MotionEvent.ACTION_MOVE ->
//                // 滑动的距离太近就不做滑动的处理
//                if (abs(event.rawX - mDownX) > 20 || abs(event.rawY - mDownY) > 10) {
//                    updatePosition(event)
//                }
//
//            MotionEvent.ACTION_UP -> {
//                jumpToSide(event)
//                val upx = event.rawX
//                val upy = event.rawY
//                if (abs(mDownX - upx) > 5 || abs(mDownY - upy) > 5) isTouch = true
//            }
//
//            else -> {}
//        }
//        return isTouch
//    }
//}