package com.cyanrain.baselibrary.phone

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager
import android.os.Build
import android.util.Log
import java.lang.reflect.InvocationTargetException
import kotlin.Boolean
import kotlin.Exception
import kotlin.Int

object FoldUtils {
    private const val TAG = "FoldUtils"

    /**
     * OPPO折叠屏识别
     * @return
     */
    private val isOPPOFold: Boolean
        get() {
            var isFold = false
            try {
                val cls = Class.forName("com.oplus.content.OplusFeatureConfigManager")

                val configManager = cls.getMethod("getInstance").invoke(null)

                val hasFeature = cls.getDeclaredMethod("hasFeature", String::class.java)
                val obj = hasFeature.invoke(configManager, "oplus.hardware.type.fold")
                if (obj is Boolean) {
                    isFold = obj
                }
            } catch (e: ClassNotFoundException) {
                e.printStackTrace()
            } catch (e: NoSuchMethodException) {
                e.printStackTrace()
            } catch (e: InvocationTargetException) {
                e.printStackTrace()
            } catch (e: IllegalAccessException) {
                e.printStackTrace()
            }

            return isFold
        }

    /**
     * 华为折叠屏识别
     * @param activity
     * @return
     */
    private fun isHuaweiFold(activity: Activity): Boolean = try {
        activity.packageManager.hasSystemFeature("com.huawei.hardware.sensor.posture")
    } catch (e: Exception) {
        e.printStackTrace()
        false
    }


    /**
     * 荣耀折叠屏识别
     * @param activity
     * @return
     */
    private fun isHonorFold(activity: Activity): Boolean {
        // 首先判断是否是荣耀设备
        if (!Build.MANUFACTURER.equals("HONOR", ignoreCase = true)) {
            return false
        }

        // 荣耀折叠屏Magic V的产品名是HNMGI
        if (Build.DEVICE.equals("HNMGI", ignoreCase = true)) {
            return true
        }
        return activity.packageManager.hasSystemFeature("com.hihonor.hardware.sensor.posture")
    }

    /**
     * vivo折叠屏识别
     * @return
     */

    private val isVivoFold: Boolean
        @SuppressLint("PrivateApi")
        get() {
            var isVivoFold = false
            try {
                val cls = Class.forName("android.util.FtDeviceInfo")
                val dType = cls.getMethod("getDeviceType").invoke(cls)
                isVivoFold = "foldable" == dType
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return isVivoFold
        }

    /**
     * 小米折叠屏识别
     * @return
     */
    private val isXiaomiFold: Boolean
        get() {
            var isXiaomiFold = false
            try {
                // 通过反射获取systemProperties类
                val systemProperties = Class.forName("android.os.SystemProperties")
                //获取SystemProperties 的getInt方法
                val method = systemProperties.getMethod(
                    "getInt",
                    String::class.java,
                    Int::class.javaPrimitiveType
                )
                // 调用 getInt方法对persist.sys.muiltdisplay_type 属性值来进行判断
                isXiaomiFold = (method.invoke(null, "persist.sys.muiltdisplay_type", 0) as? Int) == 2
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return isXiaomiFold
        }

    private fun isSysFold(activity: Activity): Boolean {
        return when (val sensorManager = activity.getSystemService(Context.SENSOR_SERVICE)){
            is SensorManager ->{
                sensorManager.getDefaultSensor(Sensor.TYPE_HINGE_ANGLE)!=null
            }
            else -> false
        }
    }

    /**
     * 判断是否是折叠屏
     * @param activity
     * @return
     */
    fun isFold(activity: Activity): Boolean {
        Log.d(TAG, "isOppoFold:$isOPPOFold")
        Log.d(TAG, "isHuaweiFold:" + isHuaweiFold(activity))
        Log.d(TAG, "isHonorFold:" + isHonorFold(activity))
        Log.d(TAG, "isVivoFold:$isVivoFold")
        Log.d(TAG, "isXiaomiFold:$isXiaomiFold")
        Log.d(TAG, "isSysFold:" + isSysFold(activity))
        return isOPPOFold || isHuaweiFold(activity) || isHonorFold(activity) || isVivoFold || isXiaomiFold || isSysFold(activity)
    }

}