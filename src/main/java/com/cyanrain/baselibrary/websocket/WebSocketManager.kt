//package com.cyanrain.baselibrary.network.websocket
//
//import android.os.Handler
//import android.os.Looper
//import android.util.Log
//
//
//class WebSocketManager  constructor(
//    private val client: OkHttpClient
//) {
//
//    companion object {
//        const val TAG = "WebSocketManager"
//        const val MAX_RECONNECT_ATTEMPTS = 5
//        const val INITIAL_RETRY_DELAY_MS = 1000L
//    }
//
//    // 回调接口
//    interface Listener {
//        fun onOpen()
//        fun onMessage(message: String)
//        fun onMessage(bytes: ByteString)
//        fun onClosing(code: Int, reason: String)
//        fun onClosed(code: Int, reason: String)
//        fun onError(t: Throwable?)
//    }
//
//    private var webSocket: WebSocket? = null
//    private var reconnectAttempts = 0
//    private var isReconnecting = false
//    private var listener: Listener? = null
//
//    private val handler by lazy { Handler(Looper.getMainLooper()) }
//
//    private var currentUrl: String = ""
//
//    init {
////        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
//    }
//
//    // 初始化并连接
//    fun connect(url: String, listener: Listener) {
//        this.listener = listener
//        this.currentUrl = url
//        doConnect(url)
//    }
//
//    // 发送文本消息
//    fun sendMessage(message: String): Boolean {
//        Log.d(TAG, "发送消息: $message")
//        return webSocket?.send(message) ?: false
//    }
//
//    // 发送二进制消息
//    fun sendByteMessage(bytes: ByteString): Boolean {
//        return webSocket?.send(bytes) ?: false
//    }
//
//    // 关闭连接
//    fun disconnect() {
//        Log.d(TAG, "关闭连接")
//        isReconnecting = false
//        webSocket?.close(1000, "User disconnected")
//        webSocket = null
//    }
//
//    // 实际建立连接
//    private fun doConnect(url: String) {
//        if (webSocket != null) return
//        Log.d(TAG, "正在连接 $url")
//        val request = Request.Builder().url(url).build()
//
//        webSocket = client.newWebSocket(request, object : WebSocketListener() {
//            override fun onOpen(webSocket: WebSocket, response: Response) {
//                super.onOpen(webSocket, response)
//                Log.d(TAG, "连接已建立")
//                this@WebSocketManager.webSocket = webSocket
//                reconnectAttempts = 0
//                isReconnecting = false
//                listener?.onOpen()
//            }
//
//            override fun onMessage(webSocket: WebSocket, text: String) {
//                super.onMessage(webSocket, text)
//                Log.d(TAG, "收到消息: $text")
//                listener?.onMessage(text)
//            }
//
//            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
//                super.onMessage(webSocket, bytes)
//                Log.d(TAG, "收到二进制消息: ${bytes.hex()}")
//                listener?.onMessage(bytes)
//            }
//
//            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
//                super.onClosing(webSocket, code, reason)
//                Log.d(TAG, "正在关闭连接: $code, $reason")
//                listener?.onClosing(code, reason)
//            }
//
//            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
//                super.onClosed(webSocket, code, reason)
//                Log.d(TAG, "连接已关闭: $code, $reason");
//                webSocket.cancel()
//                this@WebSocketManager.webSocket = null
//                listener?.onClosed(code, reason)
//                maybeReconnect()
//            }
//
//            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
//                super.onFailure(webSocket, t, response)
//                Log.d(TAG, "连接失败: ${t.message}")
//                listener?.onError(t)
//                webSocket.cancel()
//                this@WebSocketManager.webSocket = null
//                maybeReconnect()
//            }
//        })
//    }
//
//    // 自动重连机制
//    private fun maybeReconnect() {
//        if (isReconnecting || reconnectAttempts >= MAX_RECONNECT_ATTEMPTS) return
//        isReconnecting = true
//
//        reconnectAttempts++
//        val delayMillis = INITIAL_RETRY_DELAY_MS * reconnectAttempts
//
//        handler.postDelayed({
//            Log.d(TAG, "尝试第 $reconnectAttempts 次重连...")
//            doConnect(currentUrl)
//        }, delayMillis)
//    }
//}
