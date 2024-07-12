package com.iostyle.flow.streaming

import com.iostyle.flow.utils.debugLog
import com.iostyle.flow.streaming.constant.RELEASE_INTERVAL
import com.iostyle.flow.streaming.constant.RELEASE_NEVER_TIMEOUT
import com.iostyle.flow.streaming.model.AbstractStreaming
import com.iostyle.flow.streaming.model.IStreamingProcessor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.atomic.AtomicBoolean

object StreamingProcessor : IStreamingProcessor<ByteArray> {

    private val jobMap by lazy(LazyThreadSafetyMode.SYNCHRONIZED) { ConcurrentHashMap<String, Job>() }
    private val bufferMap by lazy(LazyThreadSafetyMode.SYNCHRONIZED) { ConcurrentHashMap<String, ConcurrentLinkedDeque<ByteArray>>() }
    private val inProcessMap by lazy(LazyThreadSafetyMode.SYNCHRONIZED) { ConcurrentHashMap<String, AtomicBoolean>() }

    override fun build(
        id: String,
        processorLoopInterval: Long,
        callback: suspend (output: ByteArray) -> Unit
    ): ConcurrentLinkedDeque<ByteArray> {
        bufferMap[id] = ConcurrentLinkedDeque()
        jobMap[id] = GlobalScope.launch(Dispatchers.IO) {
            while (this.isActive) {
//                debugLog("Looper $id ${!bufferMap[id].isNullOrEmpty()} ${bufferMap[id]?.size}")
                if (!bufferMap[id].isNullOrEmpty()) {
                    bufferMap[id]?.poll()?.let {
                        setProcessStatus(id, true)
                        callback(it)
                        setProcessStatus(id, false)
                    }
                } else {
                    if (processorLoopInterval > 0) delay(processorLoopInterval)
                }
            }
        }
        return bufferMap[id]!!
    }

    // inputBuffer outputBuffer 都需要处理完成
    override fun tryRelease(id: String, timeout: Long, invokeCallback: Boolean, callback: (() -> Unit)?) {
        StreamingDispatcher.find(id)?.let {
            GlobalScope.launch(Dispatchers.IO) {
                var loopTime = 0L
                while (!bothCanRelease(it, id)
                    // 设置不超时 或者设置了超时时间，并且还没有超时
                    && (timeout == RELEASE_NEVER_TIMEOUT || (timeout > 0 && loopTime < timeout))
                ) {
                    delay(RELEASE_INTERVAL)
                    // 如果设置了具体的超时时间，就开始计时
                    if (timeout != RELEASE_NEVER_TIMEOUT) loopTime += RELEASE_INTERVAL
                }
                jobMap.remove(id)?.cancel()
                bufferMap.remove(id)
                inProcessMap.remove(id)
                it.release(invokeCallback)
                callback?.invoke()
            }
        }
    }

    // 校验上行、下行处理状态，返回均可释放
    private fun bothCanRelease(streaming: AbstractStreaming, id: String): Boolean {
        return (streaming.canRelease() && !isWorking(id)).also {
            debugLog("bothCanRelease $it")
        }
    }

    // 即判断buffer 也判断尾帧处理状态
    override fun isWorking(id: String): Boolean {
        return !bufferMap[id].isNullOrEmpty() || getProcessStatus(id)
    }

    private fun setProcessStatus(id: String, inProcess: Boolean) {
        inProcessMap[id]?.set(inProcess) ?: run {
            inProcessMap.put(id, AtomicBoolean(inProcess))
        }
    }

    private fun getProcessStatus(id: String): Boolean {
        return inProcessMap[id]?.get() == true
    }
}