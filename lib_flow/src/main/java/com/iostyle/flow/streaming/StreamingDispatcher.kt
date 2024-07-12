package com.iostyle.flow.streaming

import com.iostyle.flow.middleware.model.IMiddleware
import com.iostyle.flow.streaming.impl.StreamingImpl
import com.iostyle.flow.streaming.model.AbstractStreaming
import com.iostyle.flow.streaming.model.IStreamingDispatcher
import com.iostyle.flow.utils.debugLog
import java.util.concurrent.ConcurrentHashMap

/**
 * 流式传输分发器
 * 支持上行、下行双路缓存
 * 支持自动延迟释放资源
 */
object StreamingDispatcher : IStreamingDispatcher<ByteArray> {

    private val dispatchers by lazy(LazyThreadSafetyMode.SYNCHRONIZED) { ConcurrentHashMap<String, AbstractStreaming>() }

    /**
     * StreamingDispatcher.bind(
     *             id = "",
     *             middleware = EncryptionMiddleware,
     *             callback = {
     *                 System.out.println(it.toString())
     *             },
     *             onRelease = {
     *                 System.out.println("release")
     *             })
     */
    override fun bind(
        id: String,
        middleware: IMiddleware<ByteArray>,
        processorLoopInterval: Long,
        onRelease: (() -> Unit)?,
        callback: suspend (output: ByteArray) -> Unit,
    ) {
        dispatchers.remove(id)?.also {
            debugLog("bind init release $id")
            it.release(false)
        }
        dispatchers[id] =
            StreamingImpl(StreamingProcessor.build(id, processorLoopInterval, callback), middleware, onRelease)
    }

    override fun offer(id: String, byteArray: ByteArray): Boolean {
        return try {
            dispatchers[id]?.offer(byteArray) ?: false
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    override suspend fun safeOffer(id: String, byteArray: ByteArray): Boolean {
        return try {
            dispatchers[id]?.safeOffer(byteArray) ?: false
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    override fun find(id: String): AbstractStreaming? {
        return dispatchers[id]
    }

    override fun release(id: String, timeout: Long, invokeCallback: Boolean) {
        StreamingProcessor.tryRelease(id, timeout, invokeCallback)
    }
}