package com.cyanrain.baselibrary.log

import java.io.File

data class LogConfig(
    var isDebug: Boolean = false,  // 是否是调试模式
    var isSaveFile: Boolean = false,  // 是否保存日志文件
    var isFormat:Boolean = false,  // 是否格式化日志
    var prefixTag:String = "",  // 日志前缀
    var saveFileDir: File,  // 保存日志文件的目录
    var maxFileSize: Long = 2 * 1024 * 1024L,  // 日志文件最大大小
    var maxFileCount: Int = 5,  // 日志文件最大数量
    var batchSize: Int = 5  // 日志文件批量写入数量
)
