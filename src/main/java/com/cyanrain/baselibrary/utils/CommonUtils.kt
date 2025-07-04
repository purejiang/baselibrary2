package com.cyanrain.baselibrary.utils

import android.os.Build
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CommonUtils {
    companion object {
        /**
         * 时间格式化
         * @param timestamp 时间戳
         * @param pattern 时间格式
         * @return
         */
        fun formatTime(timestamp: Long, pattern: String = "yyyy-MM-dd HH:mm:ss.SSS"): String {
            return SimpleDateFormat(pattern, Locale.getDefault()).format(Date(timestamp))
        }

        /**
         * 获取堆栈信息
         * @param throwable
         * @return
         */
        fun getStackTrace(throwable: Throwable): String {
            return StringWriter().use { sw ->
                PrintWriter(sw).use { pw ->
                    throwable.printStackTrace(pw)
                    sw.toString()
                }
            }
        }

        /**
         * 获取当前线程堆栈信息
         * @return
         */
        fun getThreadTraceInfo(): String {
            val threadInfo = Thread.currentThread().stackTrace
            val sb = StringBuilder()
            for (i in threadInfo.indices) {
                sb.append(threadInfo[i].toString())
                sb.append("\n")
            }
            return sb.toString()
        }

        /**
         * 获取当前线程的名称和id
         * @return
         */
        fun getSimpleThreadInfo(): String {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
                Thread.currentThread().name + ":" + Thread.currentThread().threadId()
            } else {
                Thread.currentThread().name + ":" + Thread.currentThread().id
            }
        }

    }
}