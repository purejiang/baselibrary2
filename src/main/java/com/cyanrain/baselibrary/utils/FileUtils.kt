package com.cyanrain.baselibrary.utils

import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

object FileUtils {
    private const val TAG = "FileUtils"

    enum class WriteMode { APPEND, OVERWRITE }

    inline fun <reified T> writeFile(block: FileWriterBuilder<T>.() -> Unit): Result<Unit> {
        return FileWriterBuilder<T>().apply(block).execute()
    }


    class FileWriterBuilder<T> {
        lateinit var target: File
        var data: T? = null
        var mode: WriteMode = WriteMode.APPEND
        var encoder: ((T) -> ByteArray)? = null
        fun execute(): Result<Unit> = runCatching {
            check(::target.isInitialized) { "Target file must be specified" }
            check(data != null) { "Write data cannot be null" }
            target.parentFile?.mkdirs()

            data?.let {
                when (it) {
                    is String -> writeText(target, it, mode)
                    is ByteArray -> writeBytes(target, it, mode)
                    is InputStream -> writeStream(target, it, mode)
                    else -> encoder?.invoke(it)?.let { coder -> writeBytes(target, coder, mode) }
                        ?: error("Unsupported data type: ${it::class.java}")
                }
            }

        }


        private fun writeText(file: File, text: String, mode: WriteMode) {

            file.writer().use { writer ->
                when (mode) {
                    WriteMode.APPEND -> writer.append(text)
                    WriteMode.OVERWRITE -> writer.write(text)
                }
            }
        }

        private fun writeBytes(file: File, bytes: ByteArray, mode: WriteMode) {
            FileOutputStream(file, mode == WriteMode.APPEND).use { os ->
                os.buffered().use { bf ->
                    when (mode) {
                        WriteMode.APPEND -> bf.write(bytes)
                        WriteMode.OVERWRITE -> {
                            os.channel.truncate(0)
                            bf.write(bytes)
                        }
                    }
                }
            }
        }

        private fun writeStream(file: File, stream: InputStream, mode: WriteMode) {
            FileOutputStream(file, mode == WriteMode.APPEND).use { os ->
                os.buffered().use { bf ->
                    stream.buffered().use { it.copyTo(bf) }
                }
            }
        }
    }
}