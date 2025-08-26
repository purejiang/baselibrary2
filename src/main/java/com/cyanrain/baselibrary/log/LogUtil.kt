package com.cyanrain.baselibrary.log

import android.content.Context
import android.util.Log
import com.cyanrain.baselibrary.common.JsonConverter
import com.cyanrain.baselibrary.utils.CommonUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.BufferedWriter
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

/**
 * 日志工具类
 *
 * PS：不用实现顺序写入，避免死锁和线程等待耗时，读取的时候再进行排序即可
 * @author CyanRain
 */
object LogUtil {
    private const val TAG = "LogUtil"

    private const val LOG_DIR_NAME = "LogUtil"
    private const val LOG_PREFIX = "log_"
    private const val LOG_SUFFIX = ".txt"

    private lateinit var mConfig: LogConfig
    private lateinit var mConverter: JsonConverter<LogBean>

    private val mCoroutineScope = CoroutineScope(Dispatchers.IO)

    private val mLogChannel = Channel<LogBean>(Channel.UNLIMITED)

    //    private val mWriteLock = ReentrantLock()   // 线程里面用这个锁，协程不用
    private val mMutex = Mutex()
    private var mCurrentActiveFile: File? = null

    private var mCurrentWriter: BufferedWriter? = null
    private var mCurrentFileSize = 0L
    
    // 日志监听器列表
    private val logListeners = mutableSetOf<LogListener>()


    /**
     * 初始化
     * @param context 上下文
     * @param converter json转换器
     * @param block 配置
     *
     */
    fun init(
        context: Context,
        converter: JsonConverter<LogBean>,
        block: LogConfig.() -> Unit
    ) {
        mConfig = LogConfig(saveFileDir = getDefaultDir(context)).apply(block)
        mConverter = converter
        if (mConfig.isSaveFile) {
            mConfig.saveFileDir.let {
                if (!it.exists()) {
                    // 文件不存在，创建文件夹
                    it.mkdirs()
                }
            }
        }
        Log.d(TAG, "init success.")
        startCoroutine()
    }

    /**
     * 日志打印（如果不打印throwable出来，gson转换的时候就不会有stacktrace。类型擦除）
     * @param tag 标签
     * @param message 内容
     * @param throwable 异常
     */
    fun v(tag: String, message: String, throwable: Throwable? = null) {
        log(LEVEL.VERBOSE, tag, message, throwable)
    }

    fun d(tag: String, message: String, throwable: Throwable? = null) {
        log(LEVEL.DEBUG, tag, message, throwable)
    }

    fun i(tag: String, message: String, throwable: Throwable? = null) {
        log(LEVEL.INFO, tag, message, throwable)
    }

    fun w(tag: String, message: String, throwable: Throwable? = null) {
        log(LEVEL.WARN, tag, message, throwable)
    }

    fun e(tag: String, message: String, throwable: Throwable? = null) {
        log(LEVEL.ERROR, tag, message, throwable)
    }

    fun wtf(tag: String, message: String, throwable: Throwable? = null) {
        log(LEVEL.WFT, tag, message, throwable)
    }

    /**
     * 从文件中解析日志
     * @return List<LogBean> 日志对象集合
     */
    fun getLogListFromFile(): List<LogBean> {
        val logBeanList = ArrayList<LogBean>()
        mConfig.saveFileDir.listFiles()?.forEach { file ->
            file.bufferedReader().use { reader ->
                reader.forEachLine { line ->
                    if (line.isNotBlank()) logBeanList.add(str2Log(line))
                }
            }
        }
        return logBeanList
    }

    /**
     * 清理所有日志文件
     */
    fun clear() {
        mConfig.saveFileDir.listFiles()?.forEach {
            it.delete()
        }
    }

    /**
     * 停止所有协程
     */
    fun destroy() {
        mCurrentWriter?.flush()
        mCurrentWriter?.close()
        mCoroutineScope.cancel()
        mCurrentWriter = null
    }
    
    /**
     * 注册日志监听器
     * @param listener 日志监听器
     */
    fun registerLogListener(listener: LogListener) {
        logListeners.add(listener)
    }
    
    /**
     * 注销日志监听器
     * @param listener 日志监听器
     */
    fun unregisterLogListener(listener: LogListener) {
        logListeners.remove(listener)
    }
    
    /**
     * 通知所有监听器有新的日志产生
     * @param logBean 日志对象
     */
    private fun notifyLogListeners(logBean: LogBean) {
        // 在主线程中通知监听器
        CoroutineScope(Dispatchers.Main).launch {
            logListeners.forEach { listener ->
                listener.onLog(logBean)
            }
        }
    }

    /**
     * 日志批量写入协程
     */
    private fun startCoroutine() {
        mCoroutineScope.launch {
            val batch = mutableListOf<LogBean>()

            while (true) {
                val logBean = try {
                    mLogChannel.receive()
                } catch (e: Exception) {
                    break
                }
                batch.add(logBean)
                if (batch.size >= mConfig.batchSize) {
                    saveBatchToFile(batch)
                    batch.clear()
                }
            }
        }
    }


    /**
     * 日志格式化
     * @param logBean 日志对象
     * @return String 日志格式化后的字符串
     */
    private fun log2Str(logBean: LogBean): String {
        return mConverter.toJson(logBean)
    }

    /**
     * 日志格式化
     * @param content 日志格式化后的字符串
     * @return LogBean 日志对象
     */
    private fun str2Log(content: String): LogBean {
        return mConverter.fromJson(content)
    }

    /**
     * 日志打印/写入文件
     * @param level 日志级别
     * @param tag 标签
     * @param message 内容
     * @param throwable 异常
     */
    private fun log(level: LEVEL, tag: String, message: String, throwable: Throwable?) {
        val logBean = LogBean(
            tag = tag,
            message = message,
            level = level,
            time = System.currentTimeMillis(),
            throwable = throwable,
            threadInfo = CommonUtils.getSimpleThreadInfo(),
            trackInfo = getSimpleTrackInfo()
        )
        if (mConfig.isDebug) {
            // debug模式才输出日志
            printLog(logBean)
            // 通知监听器
            notifyLogListeners(logBean)
        }
        if (mConfig.isSaveFile) {
            // 排进队列
            mCoroutineScope.launch {
                mLogChannel.send(logBean)
            }
        }
    }

    /**
     * 保存日志
     * @param logBatch 日志对象集合
     */
    private suspend fun saveBatchToFile(logBatch: List<LogBean>) {
//        val content = log2Str(logBean)
//        Log.d(TAG, "content: $content")
        // 加锁
//        mWriteLock.lock()
//        Log.d(TAG, "saveBatchToFile logBatch:$logBatch")
        mMutex.withLock {
            try {
                val content = logBatch.joinToString("\n") { log2Str(it) }

                val targetFile = getCurrentActiveFile()

                if (targetFile != mCurrentActiveFile) {
                    mCurrentWriter?.close()
                    mCurrentWriter = null
                    mCurrentActiveFile = targetFile
                    mCurrentFileSize = 0L
                }
//            writeFile(mCurrentActiveFile,  content)
                // 执行完会释放锁
                val writer =
                    mCurrentWriter ?: FileOutputStream(targetFile, true).bufferedWriter().also {
                        mCurrentWriter = it
                    }
                writer.append(content)
                writer.flush()

                mCurrentFileSize += content.length

                if (mCurrentFileSize > mConfig.maxFileSize) {
                    maintainFileCount()
                }
            } catch (e: IOException) {
                // 流异常时关闭并重置流，下次重新创建
                mCurrentWriter?.close()
                mCurrentWriter = null
                e.printStackTrace()
            }
//            mConfig.let { cf ->
//                val targetFile = getCurrentActiveFile().also {
//                    if (it.length() + content.length > cf.maxFileSize) {
//                        mCurrentActiveFile = createNewFile(cf.saveFileDir)
//                    }
//                }
//                // 执行实际写入
//                writeFile(targetFile, content)
//                //Log.d(TAG, "save file success: ${targetFile.absolutePath}")
//                // 维护文件数量
//                maintainFileCount()
//            }
        }
//        mMutex.lock()
//        try {
//
//        } finally {
        // 解锁
//            mWriteLock.unlock()
//            mMutex.unlock()
//        }
    }

//    /**
//     * 写入文件
//     * @param file 文件
//     * @param content 内容
//     */
//    private fun writeFile(file: File?, content: String) {
//        try {
//            // 高频情况下，用FileWriter会导致频繁访问磁盘进行写入文件
////            FileWriter(file, true).use { writer ->
////                writer.append(content)
////            }
//            // 优化点：用bufferWrite，先缓存在内存，满了之后再写入，减少频繁的IO操作，但是在缓存期间应用被关闭/崩了，就不会写入了
//            // 默认是8*1024
//            // use可以自动关闭流
//            val writer = mCurrentWriter ?: FileOutputStream(file, true).bufferedWriter().use {
//                it.append(content)
//            }
//
//        } catch (e: IOException) {
//            e.printStackTrace()
//        }
//    }

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
     * 设置当前日志文件
     * @return File 当前日志文件
     */
    private fun getCurrentActiveFile(): File {
        return mCurrentActiveFile ?: run {
            mConfig.saveFileDir.listFiles()
                ?.maxByOrNull { it.lastModified() }
                ?.takeIf { it.length() < mConfig.maxFileSize }
                ?: createNewFile(mConfig.saveFileDir).also {
                    mCurrentActiveFile = it
                }
        }
    }

    /**
     * 维护日志文件数量
     */
    private fun maintainFileCount() {
        mConfig.saveFileDir.listFiles()?.let { files ->
            if (files.size > mConfig.maxFileCount) {
                files.sortedBy { it.lastModified() }
                    .take(files.size - mConfig.maxFileCount)
                    .forEach { it.delete() }
            }
        }
    }


    /**
     * 获取默认日志保存路径
     * @param context 上下文
     * @return File 日志保存路径
     */
    private fun getDefaultDir(context: Context): File {
        return context.getExternalFilesDir(LOG_DIR_NAME) ?: File(
            context.filesDir,
            LOG_DIR_NAME
        )
    }

    /**
     * 创建日志名称
     * @return
     */
    private fun createLogFileName(): String {
        return "$LOG_PREFIX${System.currentTimeMillis()}$LOG_SUFFIX"
    }


    private fun printLog(logBean: LogBean) {
        val message = if (mConfig.isFormat) {
            formatLogMessage(logBean)
        } else {
            logBean.message
        }
        val tag = if (mConfig.prefixTag.isNotEmpty()) {
            "${mConfig.prefixTag}-${logBean.tag}"
        } else {
            logBean.tag
        }
        when (logBean.level) {
            LEVEL.VERBOSE -> Log.v(tag, message, logBean.throwable)
            LEVEL.DEBUG -> Log.d(tag, message, logBean.throwable)
            LEVEL.INFO -> Log.i(tag, message, logBean.throwable)
            LEVEL.WARN -> Log.w(tag, message, logBean.throwable)
            LEVEL.ERROR -> Log.e(tag, message, logBean.throwable)
            LEVEL.WFT -> Log.wtf(tag, message, logBean.throwable)
        }
    }

    /**
     * 获取简单堆栈信息
     * @return
     */
    private fun getSimpleTrackInfo(): String {
        val dropStr = listOf(
            "dalvik.",
            "android.",
            "java.lang.",
            "com.android.internal."
        )
        // 带类名验证的堆栈获取方法
        val thisClassName = LogUtil::class.java.name
        val element = Thread.currentThread().stackTrace// 跳过虚拟机内部调用
            .dropWhile { element ->
                element.className.startsWith(thisClassName) ||
                        dropStr.any { element.className.startsWith(it) }
            }
            // 跳过所有连续的内部调用
            .dropWhile { element ->
                element.methodName == "getThreadStackTrace" || element.methodName == "getStackTrace"
            }
            .firstOrNull()
        // 类名+方法名+行号
        val className = element?.className?.substringAfterLast('.')
        return "$className.${element?.methodName}(${element?.lineNumber})"
    }

    /**
     * 日志格式化
     * @param logBean 日志
     * @return
     */
    private fun formatLogMessage(logBean: LogBean): String {
        return StringBuilder().apply {
            // 时间戳
            append("[${CommonUtils.formatTime(logBean.time)}] ")
            // 线程信息
            append("[${logBean.threadInfo}] ")
            // 堆栈信息
            append("[${logBean.trackInfo}] ")
            // 消息
            append(" - ${logBean.message}")
        }.toString()
    }

    /**
     * 日志对象
     * @param tag 标签
     * @param message 内容
     * @param level 日志级别
     * @param throwable 异常
     * @param threadInfo 线程信息
     * @param trackInfo 堆栈信息
     */
    data class LogBean(
        var tag: String,
        var message: String,
        var level: LEVEL,
        var time: Long,
        var throwable: Throwable?,
        var threadInfo: String,
        var trackInfo: String
    )

    /**
     * 日志级别
     * @see VERBOSE 详细
     * @see DEBUG 调试
     * @see INFO 信息
     * @see WARN 警告
     * @see ERROR 错误
     * @see WFT 严重错误
     */
    enum class LEVEL {
        VERBOSE,
        DEBUG,
        INFO,
        WARN,
        ERROR,
        WFT
    }
    
    /**
     * 日志监听器接口
     */
    interface LogListener {
        /**
         * 当有新的日志产生时回调
         * @param logBean 日志对象
         */
        fun onLog(logBean: LogBean)
    }
}