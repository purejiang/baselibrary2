package com.cyanrain.baselibrary.audio

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder

class AudioHelper {
    companion object {
        private const val TAG = "AudioHelper"

        /**
         * 创建AudioRecord对象
         * @param audioSource 默认音频源，MIC为麦克风
         * @param sampleRate 采样率，默认16000
         * @param channelConfig 默认单声道
         * @param audioFormat 默认16位PCM编码
         * @param bufferSize 缓冲区大小，默认根据采样率、通道数、音频格式计算最小值
         * @return the audio record
         */
        @SuppressLint("MissingPermission")
        fun createAudioRecord(
            audioSource: Int = MediaRecorder.AudioSource.MIC,
            sampleRate: Int = 16000,
            channelConfig: Int = AudioFormat.CHANNEL_IN_MONO,
            audioFormat: Int = AudioFormat.ENCODING_PCM_16BIT,
            bufferSize: Int = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
        ): AudioRecord {
            return AudioRecord(
                audioSource,
                sampleRate,
                channelConfig,
                audioFormat,
                bufferSize * 2 // a sample has two bytes as we are using 16-bit PCM
            )
        }

        /**
         * 录音
         * @param audioRecord the audio record
         * @param processListener the process listener
         */
        fun createProcessAudioRunnable(
            audioRecord: AudioRecord, processListener: ProcessListener
        ): SafeRunnable {
            val interval = 0.1 // i.e., 100 ms
            val buffer = ShortArray((interval * audioRecord.sampleRate).toInt())
            return object : SafeRunnable() {
                override fun doWork() {
//                    while (running) {
                    audioRecord.read(buffer, 0, buffer.size).let { read ->
                        if (read > 0) {
                            processListener.onProcess(FloatArray(read) { buffer[it] / 32768.0f })
                        }
                    }
//                    }
                }
            }
        }

        interface ProcessListener {
            /**
             * 是否停止录音
             * @return true if you want to stop the audio record
             */
            fun toStop(): Boolean

            /**
             * 回调音频输入的数据
             * @param floatArray the audio data
             */
            fun onProcess(floatArray: FloatArray)
        }

        open class SafeRunnable : Runnable {
            // 使用 volatile 确保可见性
            @Volatile
            protected var running = true;

            public fun stop() {
                running = false;
            }

            override fun run() {
                try {
                    while (running) {
                        // 执行任务
                        doWork();

                        // 可选的检查点
                        if (!running) break;
                    }
                } finally {
                    // 清理资源
                    cleanUp();
                }
            }

            protected open fun doWork() {
                // 实际工作代码
            }

            protected open fun cleanUp() {
                // 释放资源
            }
        }
    }
}