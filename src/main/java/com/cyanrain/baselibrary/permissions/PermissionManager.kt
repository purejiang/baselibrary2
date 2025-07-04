package com.cyanrain.baselibrary.permissions

import android.content.pm.PackageManager
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

object PermissionManager {
    private val mPermissionMap: ConcurrentHashMap<Int, PermissionInfo> = ConcurrentHashMap()

    // 默认的权限请求码
    private var mCurrentCode = AtomicInteger(10010)

    /**
     * 请求权限
     * @param permission 权限
     * @param permissionDocs 权限说明
     * @param permissionDeniedMsg 权限被拒绝时的提示信息
     * @param requestListener 权限回调
     */
    fun requestPermission(
        permission: String,
        permissionDocs: String,
        permissionDeniedMsg: String,
        requestListener: RequestListener
    ) {
        val code = mCurrentCode.getAndIncrement()
        val permissionInfo =
            PermissionInfo(permission, permissionDocs, permissionDeniedMsg, requestListener)
        mPermissionMap[code] = permissionInfo
        requestListener.onRequestPermission(code, arrayOf(permission))
    }

    /**
     * 权限返回
     * @param requestCode 请求码
     * @param permissions 请求的权限
     * @param grantResults 权限请求结果
     */
    fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        mPermissionMap[requestCode]?.let { info ->
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                info.requestListener.onGranted()
            } else {
                info.requestListener.onDenied()
            }
            mPermissionMap.remove(requestCode)
        }
    }

    /**
     * 权限信息
     * @param permission 权限
     * @param permissionDocs 权限说明
     * @param permissionDeniedMsg 权限被拒绝时的提示信息
     * @param requestListener 权限回调
     */
    data class PermissionInfo(
        val permission: String,
        val permissionDocs: String,
        val permissionDeniedMsg: String,
        val requestListener: RequestListener
    )
}
