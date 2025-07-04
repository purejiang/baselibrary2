package com.cyanrain.baselibrary.phone

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.telephony.TelephonyManager

object PhoneUtils {
    /**
     * 内存相关
     */
    private const val MEGABYTE = 1024 * 1024L

    /**
     * 获取当前网络状态
     * @return 网络状态 描述 WIFI, CELLULAR, ETHERNET, BLUETOOTH, VPN, WIFI_AWARE, LOWPAN, USB, THREAD, SATELLITE, UNKNOWN, DISCONNECTED, ERROR
     */
    @SuppressLint("MissingPermission")
    fun Context.getNetworkStatus(): String {
        return try {
            val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            cm.getNetworkCapabilities(cm.activeNetwork)?.run {
                when {
                    hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "WIFI"
                    hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "CELLULAR"
                    //for other device how are able to connect with Ethernet
                    hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "ETHERNET"
                    //for check internet over Bluetooth
                    hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH) -> "BLUETOOTH"
                    hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> "VPN"
                    hasTransport(NetworkCapabilities.TRANSPORT_WIFI_AWARE) -> "WIFI_AWARE"
                    hasTransport(NetworkCapabilities.TRANSPORT_LOWPAN) -> "LOWPAN"
                    hasTransport(NetworkCapabilities.TRANSPORT_USB) -> "USB"
                    hasTransport(NetworkCapabilities.TRANSPORT_THREAD) -> "THREAD"
                    hasTransport(NetworkCapabilities.TRANSPORT_SATELLITE) -> "SATELLITE"
                    else -> "UNKNOWN"
                }
            } ?: "DISCONNECTED"
        } catch (e: Exception) {
            e.printStackTrace()
            "ERROR"
        }
    }

    /**
     * 获取电池状态
     * @return 电池状态
     */
    fun Context.getBatteryStatus(): BatteryStatus {
        return registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))?.run {
            BatteryStatus(
                level = getIntExtra(BatteryManager.EXTRA_LEVEL, -1),
                isCharging = getIntExtra(BatteryManager.EXTRA_PLUGGED, -1) != 0,
                chargeType = when (getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)) {
                    BatteryManager.BATTERY_PLUGGED_USB -> "USB"
                    BatteryManager.BATTERY_PLUGGED_AC -> "AC"
                    BatteryManager.BATTERY_PLUGGED_WIRELESS -> "WIRELESS"
                    else -> "UNKNOWN"
                }
            )
        } ?: BatteryStatus(-1, false, "UNKNOWN")
    }

    /**
     * 电池状态
     * @property level 电池电量
     * @property isCharging 是否正在充电
     * @property chargeType 充电类型
     */
    data class BatteryStatus(
        val level: Int,
        val isCharging: Boolean,
        val chargeType: String
    )


    // 重构后的内存方法
    fun Context.getAvailableMemory(): Long = getMemoryData { it.availMem / MEGABYTE } as Long
    fun Context.getTotalMemory(): Long = getMemoryData { it.totalMem / MEGABYTE } as Long
    fun Context.getMemoryThreshold(): Long = getMemoryData { it.threshold / MEGABYTE } as Long
    fun Context.isLowMemory(): Boolean = getMemoryData { it.lowMemory } as Boolean


    /**
     * 内存获取方法（优化后的，合并重复代码）
     * @param block 内存信息处理逻辑
     * @return 处理结果
     */
    private inline fun Context.getMemoryData(crossinline block: (ActivityManager.MemoryInfo) -> Any): Any {
        return try {
            val am = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val memInfo = ActivityManager.MemoryInfo()
            am.getMemoryInfo(memInfo)
            block(memInfo)
        } catch (e: Exception) {
            -1
        }
    }


    /**
     * 判断是否有SIM卡
     * @return
     */
    fun Context.hasSimCard(): Boolean = try {
        (getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager).simState == TelephonyManager.SIM_STATE_READY
    } catch (e: Exception) {
        false
    }

}