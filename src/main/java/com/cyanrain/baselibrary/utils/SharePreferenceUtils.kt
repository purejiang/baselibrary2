package com.cyanrain.baselibrary.utils

import android.content.Context

class SharePreferenceUtils(private val mContext: Context, private val mName: String) {
    companion object {
        private const val TAG = "SharePreferenceUtils"
    }

    private val sharedPreferences by lazy {
        mContext.getSharedPreferences(mName, Context.MODE_PRIVATE)
    }

    fun getString(key: String, defaultValue: String): String? {
        return sharedPreferences.getString(key, defaultValue)
    }

    fun putString(key: String, value: String): Boolean {
        return sharedPreferences.edit().putString(key, value).commit()
    }

    fun getInt(key: String, defaultValue: Int): Int {
        return sharedPreferences.getInt(key, defaultValue)
    }

    fun putInt(key: String, value: Int): Boolean {
        return sharedPreferences.edit().putInt(key, value).commit()
    }

    fun getLong(key: String, defaultValue: Long): Long {
        return sharedPreferences.getLong(key, defaultValue)
    }

    fun putLong(key: String, value: Long): Boolean {
        return sharedPreferences.edit().putLong(key, value).commit()
    }

    fun getFloat(key: String, defaultValue: Float): Float {
        return sharedPreferences.getFloat(key, defaultValue)
    }

    fun putFloat(key: String, value: Float): Boolean {
        return sharedPreferences.edit().putFloat(key, value).commit()
    }

    fun getBoolean(key: String, defaultValue: Boolean): Boolean {
        return sharedPreferences.getBoolean(key, defaultValue)
    }

    fun putBoolean(key: String, value: Boolean): Boolean {
        return sharedPreferences.edit().putBoolean(key, value).commit()
    }

    fun remove(key: String): Boolean {
        return sharedPreferences.edit().remove(key).commit()
    }

    fun clear(): Boolean {
        return sharedPreferences.edit().clear().commit()
    }

    fun contains(key: String): Boolean {
        return sharedPreferences.contains(key)
    }

    fun getAll(): Map<String, *> {
        return sharedPreferences.all
    }

}