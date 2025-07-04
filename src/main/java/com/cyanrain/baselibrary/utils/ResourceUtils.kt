package com.cyanrain.baselibrary.utils

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.drawable.Drawable

object ResourceUtils {

    private const val TAG = "ResourceUtils"


    @SuppressLint("DiscouragedApi")
    private fun getResourceIdByName(context: Context, type: String, resourcesName: String): Int {
        return context.resources.getIdentifier(resourcesName, type, context.packageName)
    }

    fun getString(context: Context, name: String): String? {
        return getResourceIdByName(context, "string", name).takeIf {
            it != 0
        }?.let { context.resources.getString(it) }

    }

    fun getDrawable(context: Context, name: String): Drawable? {
        return getResourceIdByName(context, "drawable", name).takeIf {
            it != 0
        }?.let { context.resources.getDrawable(it, null) }
    }

    fun getColor(context: Context, name: String): Int? {
        return getResourceIdByName(context, "color", name).takeIf {
            it != 0
        }?.let { context.resources.getColor(it, null) }
    }

    fun getDimension(context: Context, name: String): Float? {
        return getResourceIdByName(context, "dimen", name).takeIf {
            it != 0
        }?.let { context.resources.getDimension(it) }
    }

    fun getBoolean(context: Context, name: String): Boolean? {
        return getResourceIdByName(context, "bool", name).takeIf {
            it != 0
        }?.let { context.resources.getBoolean(it) }
    }

    fun getInteger(context: Context, name: String): Int? {
        return getResourceIdByName(context, "integer", name).takeIf {
            it != 0
        }?.let { context.resources.getInteger(it) }
    }
}