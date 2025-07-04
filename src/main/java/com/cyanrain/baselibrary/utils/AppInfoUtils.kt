package com.cyanrain.baselibrary.utils

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build

object AppInfoUtils {

    /**
     * 获取应用版本名称
     * @return
     */
    fun Context.getAppVersionName(): String = try {
        getPackageInfo()?.versionName ?: ""
    } catch (e: Exception) {
        ""
    }

    /**
     * 获取应用版本号（兼容不同Android版本）
     * @return
     */
    fun Context.getAppVersionCode(): Long = try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            getPackageInfo()?.longVersionCode ?: 0L
        } else {
            @Suppress("DEPRECATION")
            getPackageInfo()?.versionCode?.toLong() ?: 0L
        }
    } catch (e: Exception) {
        0L
    }

    /**
     * 获取应用名称
     * @return
     */
    fun Context.getAppName(): String = try {
        val appInfo = packageManager.getApplicationInfo(packageName, 0)
        packageManager.getApplicationLabel(appInfo).toString()
    } catch (e: PackageManager.NameNotFoundException) {
        ""
    } catch (e: Exception) {
        ""
    }

    /**
     * 获取安装时间（API >= 9）
     * @return
     */
    fun Context.getInstallTime(): Long = try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
            getPackageInfo()?.firstInstallTime ?: 0L
        } else 0L
    } catch (e: Exception) {
        e.printStackTrace()
        0L
    }
    /**
     * 获取最后更新时间（API >= 9）
     * @return
     */
    fun Context.getUpdateTime(): Long = try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
            getPackageInfo()?.lastUpdateTime ?: 0L
        } else 0L
    } catch (e: Exception) {
        e.printStackTrace()
        0L
    }

    /**
     * 获取签名信息（需要GET_SIGNATURES权限）
     * @return
     */
    fun Context.getSignatures(): Array<ByteArray>? = try {
        (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            packageManager.getPackageInfo(
                packageName,
                PackageManager.GET_SIGNING_CERTIFICATES
            ).signingInfo?.apkContentsSigners
        } else {
            @Suppress("DEPRECATION")
            getPackageInfo()?.signatures
        })?.map { it.toByteArray() }?.toTypedArray()

    } catch (e: Exception) {
        null
    }


    private fun Context.getPackageInfo(): PackageInfo? = try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            packageManager.getPackageInfo(packageName, 0)
        }
    } catch (e: Exception) {
        null
    }
}