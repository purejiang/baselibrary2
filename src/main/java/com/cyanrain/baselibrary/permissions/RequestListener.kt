package com.cyanrain.baselibrary.permissions

public interface RequestListener {
    /**
     * 请求权限
     * @param requestCode 请求码
     * @param permissions 请求的权限
     */
    fun onRequestPermission(requestCode: Int, permissions: Array<out String>)

    /**
     * 权限请求成功
     */
    fun onGranted()

    /**
     * 权限请求失败
     */
    fun onDenied()
}