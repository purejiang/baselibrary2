package com.cyanrain.baselibrary.crash

import android.content.Context
import android.util.Log
import com.cyanrain.baselibrary.utils.AppInfoUtils.getAppName
import com.cyanrain.baselibrary.utils.AppInfoUtils.getAppVersionCode
import com.cyanrain.baselibrary.utils.AppInfoUtils.getAppVersionName
import com.cyanrain.baselibrary.utils.CommonUtils
import java.io.File
import java.io.FileOutputStream
import java.lang.Thread.sleep
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

object CrashManager {
    private const val CRASH_DIR_NAME = "Crash"
    private const val CRASH_PREFIX = "crash_"
    private const val CRASH_SUFFIX = ".txt"
    private const val TAG = "CrashManager"

    private var mDefaultHandler: Thread.UncaughtExceptionHandler? = null
    private var mThreadPool: ExecutorService
    private lateinit var mCrashDir: File
    private lateinit var mPackageName: String
    private lateinit var mAppVersionName: String
    private lateinit var mAppVersionCode: String

    init {
        mDefaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        mThreadPool = Executors.newSingleThreadExecutor()
    }

    /**
     * 初始化
     * @param context
     */
    fun init(context: Context) {
        mPackageName = context.getAppName()
        mAppVersionName = context.getAppVersionName()
        mAppVersionCode = context.getAppVersionCode().toString()
        mCrashDir = getDefaultDir(context)
        mCrashDir.let {
            if (!it.exists()) {
                // 文件不存在，创建文件夹
                it.mkdirs()
            }
        }
        Thread.setDefaultUncaughtExceptionHandler { t, e ->
            saveCrashInfo(t, e)
            // 给日志写入一点时间
            sleep(500)
            // 交给默认处理器处理
            mDefaultHandler?.uncaughtException(t, e)
        }
    }

    /**
     * 保存崩溃信息
     * @param thread 线程
     * @param throwable 异常
     */
    private fun saveCrashInfo(thread: Thread, throwable: Throwable) {
        mThreadPool.execute {
            // 保存崩溃信息
            val crashData = CrashData(
                exception = throwable,
                threadName = thread.name,
                timestamp = System.currentTimeMillis(),
                isCoroutineCrash = false,
                processId = android.os.Process.myPid(),
                threadId = thread.id,
                userData = collectUserData(),
                deviceData = collectDeviceData(),
                memoryState = collectMemoryInfo()
            )
            Log.d(TAG, "write start")
            writeCrashToFile(crashData)
        }
    }

    /**
     * 获取默认日志保存路径
     * @param context 上下文
     * @return File 日志保存路径
     */
    private fun getDefaultDir(context: Context): File {
        return context.getExternalFilesDir(CRASH_DIR_NAME) ?: File(
            context.filesDir,
            CRASH_DIR_NAME
        )
    }

    /**
     * 创建日志文件名
     * @return
     */
    private fun createLogFileName(): String {
        return "$CRASH_PREFIX${System.currentTimeMillis()}$CRASH_SUFFIX"
    }

    /**
     * 获取新文件
     * @param dir 文件夹
     * @return File 新文件
     */
    private fun createNewFile(dir: File): File {
        val newFile = File(dir, createLogFileName())
        if (newFile.exists()) {
            newFile.delete()
        }
        newFile.createNewFile()
        return newFile
    }

    /**
     * 保存崩溃信息
     * @param crashData 崩溃数据
     */
    private fun writeCrashToFile(crashData: CrashData) {
        val logFile = createNewFile(mCrashDir)

        try {
            FileOutputStream(logFile, true).use { stream ->
                stream.write(buildLogContent(crashData).toByteArray())
                stream.flush()
            }
            Log.d(TAG, "write finish")
        } catch (e: Exception) {
            System.err.println("Failed to write crash log: ${e.message}")
        }
    }

    /**
     * 格式化日志
     * @param crashData 崩溃数据
     * @return 格式化内容
     */
    private fun buildLogContent(crashData: CrashData): String {
        return """
            ======= CRASH REPORT =======
            App: $mPackageName v$mAppVersionName ($mAppVersionCode)
            
            Time: ${CommonUtils.formatTime(crashData.timestamp)}
            Process: PID: ${crashData.processId}
            Thread: ${crashData.threadName} (TID: ${crashData.threadId})
            ${if (crashData.isCoroutineCrash) "!! COROUTINE CRASH !!" else ""}
            
            === DEVICE INFO ===
            ${crashData.deviceData}
            
            === MEMORY INFO ===
            ${crashData.memoryState}
            
            === EXCEPTION ===
            Type: ${crashData.exception.javaClass.name}
            Message: ${crashData.exception.message}
            
            === STACK TRACE ===
            ${CommonUtils.getStackTrace(crashData.exception)}
            
            === USER DATA ===
            ${crashData.userData}
            ============================
        """.trimIndent()
    }

    /**
     * 收集设备信息
     * @return
     */
    private fun collectDeviceData(): String {
        return """
            Brand: ${android.os.Build.BRAND}
            Device: ${android.os.Build.DEVICE}
            Board: ${android.os.Build.BOARD}
            Hardware: ${android.os.Build.HARDWARE}
            Available processors: ${Runtime.getRuntime().availableProcessors()}
            Model: ${android.os.Build.MODEL}
            Manufacturer: ${android.os.Build.MANUFACTURER}
            Android: ${android.os.Build.VERSION.RELEASE} (SDK ${android.os.Build.VERSION.SDK_INT})
        """.trimIndent()
    }

    /**
     * 收集内存状态
     * @return
     */
    private fun collectMemoryInfo(): String {
        val runtime = Runtime.getRuntime()
        return """
            Total memory: ${runtime.totalMemory() / (1024 * 1024)} MB
            Free memory: ${runtime.freeMemory() / (1024 * 1024)} MB
            Max memory: ${runtime.maxMemory() / (1024 * 1024)} MB
        """.trimIndent()
    }

    /**
     * 收集应用自定义数据
     * @return
     */
    private fun collectUserData(): String {
        // 实际应用中可替换为：
        // - 当前用户信息
        // - 应用状态数据
        // - 关键操作历史
        return "Current time: ${CommonUtils.formatTime(System.currentTimeMillis())}"
    }

    data class CrashData(
        val exception: Throwable,
        val threadName: String,
        val timestamp: Long,
        val isCoroutineCrash: Boolean,
        val processId: Int,
        val threadId: Long,
        val userData: String,
        val deviceData: String,
        val memoryState: String
    )
}