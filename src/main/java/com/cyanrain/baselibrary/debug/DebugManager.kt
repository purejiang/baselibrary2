package com.cyanrain.baselibrary.debug

import android.app.Activity
import android.util.Log
import com.cyanrain.baselibrary.log.LogUtil


object DebugManager : LogUtil.LogListener {
    const val TAG = "DebugManager"

    private var mDebugFloatBall: FloatingBall? = null

    private var mDebugInfoBall: FloatingBall? = null

    private val mLogBeanList by lazy {
        mutableListOf<LogUtil.LogBean>()
    }

    init {
        Log.d(TAG, "init: $DebugManager")
        LogUtil.registerLogListener(this)

        // 先加载历史日志
        LogUtil.getLogListFromFile().forEach {
            mLogBeanList.add(it)
        }
    }



    override fun onLog(logBean: LogUtil.LogBean) {
        Log.d(TAG, "onLog: $logBean")
        mLogBeanList.add(logBean)
        // 将实时日志发送到 mDebugInfoBall
        mDebugInfoBall?.getContent()?.let {
            if (it is DebugView2) {
                it.addLog(logBean)
            }
        }
    }

    fun showFloat(activity: Activity) {
        Log.d(TAG, "showFloat：$mDebugFloatBall")
        if (mDebugFloatBall == null) {
            mDebugFloatBall = FloatingBall(activity)
        }
        val debugView = DebugFloatView2(activity)
        // 配置悬浮球
        FloatingBall.Config(
            // 设置大小
            width = 50,
            height = 30,
            contentView = debugView
        ).let {
            mDebugFloatBall?.setConfig(it)
            // 展示悬浮球
            mDebugFloatBall?.show()
        }


        // 设置点击事件
        debugView.setOnClickListener {
            showDebugView(activity)
        }
    }

    fun showDebugView(activity: Activity) {
        Log.d(TAG, "showDebugView：$mDebugInfoBall")
        if (mDebugInfoBall == null) {
            mDebugInfoBall = FloatingBall(activity)
        }
        val debugInfoView = DebugView2(activity)

        debugInfoView.setOnItemClickListener(object : DebugView2.OnItemClickListener {
            override fun onClose() {
                mDebugInfoBall?.close()
                mDebugFloatBall = null
            }

        })
        // 添加已缓存的实时日志
        mLogBeanList.forEach {
            debugInfoView.addLog(it)
        }

        // 配置悬浮球
        FloatingBall.Config(
            width = 300,
            height = 450,
            contentView = debugInfoView
        ).let {
            mDebugInfoBall?.setConfig(it)
            // 显示悬浮球
            mDebugInfoBall?.show()
        }
    }


    fun destroy() {
        mDebugInfoBall?.close()
        mDebugFloatBall?.close()

        mDebugFloatBall = null
        mDebugInfoBall = null
    }


}