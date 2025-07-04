package com.cyanrain.baselibrary.phone

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.os.Build
import android.view.WindowInsets
import android.view.WindowManager

object ScreenUtils {
    private const val TAG = "ScreenUtils"

    /**
     * 获取屏幕宽度
     * @return
     */
    fun Context.getScreenWidth(): Int = this.resources.displayMetrics.widthPixels

    /**
     * 获取屏幕高度
     * @return
     */
    fun Context.getScreenHeight(): Int = this.resources.displayMetrics.heightPixels

    /**
     * 获取导航栏高度
     * @return
     */
    @SuppressLint("DiscouragedApi", "InternalInsetResource")
    fun Context.getNavigationBarHeight(): Int = try {
        val resourceId = resources.getIdentifier("navigation_bar_height", "dimen", "android")
        resources.getDimensionPixelSize(resourceId)
    } catch (e: Exception) {
        0
    }

    /**
     * 获取状态栏高度
     * @return
     */
    @SuppressLint("DiscouragedApi", "InternalInsetResource")
    fun Context.getStatusBarHeight(): Int = try {
        val resourceId = resources.getIdentifier("status_bar_height", "dimen", "android")
        resources.getDimensionPixelSize(resourceId)
    } catch (e: Exception) {
        e.printStackTrace()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val windowMetrics = (getSystemService(Context.WINDOW_SERVICE) as WindowManager)
                .currentWindowMetrics
            windowMetrics.windowInsets.getInsets(WindowInsets.Type.statusBars()).top
        } else {
            dpToPx(24, this).toInt() // 默认近似值
        }
    }

    /**
     * 获取刘海屏高度
     */
    fun Activity.getDisplayCutoutHeight(): Int {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            return window.decorView.rootWindowInsets?.displayCutout?.run {
                safeInsetTop.coerceAtLeast(safeInsetBottom)
            } ?: 0
        }
        return 0
    }

    /**
     * dp转px
     * @param dp
     * @param context
     * @return
     */
    fun dpToPx(dp: Int, context: Context): Float {
        return dp * context.resources.displayMetrics.density
    }

}