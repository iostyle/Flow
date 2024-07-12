package com.iostyle.flow.streaming.impl

import com.iostyle.flow.streaming.model.AbstractStreaming
import com.iostyle.flow.middleware.model.IMiddleware
import com.iostyle.flow.utils.DEBUG_MODE
import com.iostyle.flow.utils.SecureSpeedCache
import com.iostyle.flow.utils.debugLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.atomic.AtomicBoolean

class StreamingImpl(
    outputBuffer: ConcurrentLinkedDeque<ByteArray>?,
    middleware: IMiddleware<ByteArray>?,
    onRelease: (() -> Unit)? = null
) : AbstractStreaming(outputBuffer, middleware, onRelease) {

    val inputBuffer by lazy(LazyThreadSafetyMode.SYNCHRONIZED) { ConcurrentLinkedDeque<ByteArray>() }
    val inProcess by lazy(LazyThreadSafetyMode.SYNCHRONIZED) { AtomicBoolean() }

    private val BUFFER_MAX_SIZE = 10

    override fun offer(byteArray: ByteArray): Boolean {
        inputBuffer.offer(byteArray)
//        debugLog("offer inputBuffer.size:${inputBuffer.size}")
        debugLogBufferSize("offer")
        tryWakeUp()
        return true
    }

    override suspend fun safeOffer(byteArray: ByteArray): Boolean {
        if (inputBuffer.size < BUFFER_MAX_SIZE) {
            return offer(byteArray)
        } else {
            return GlobalScope.async(Dispatchers.IO) {
                while (inputBuffer.size >= BUFFER_MAX_SIZE) {
                    delay(SecureSpeedCache.getSpeedDelay(byteArray.size))
                }
                return@async offer(byteArray)
            }.await()
        }
    }

    /**
     * 外部需要保障outputBuffer被读取完毕，否则通道会一直保留
     */
    override fun release(invokeCallback: Boolean) {
        if (canRelease()) {
            outputBuffer = null
            middleware = null
            if (invokeCallback)
                onRelease?.invoke()
            onRelease = null
        }
    }

    override fun tryWakeUp() {
        if (!inProcess.get() && inputBuffer.isNotEmpty()) {
            inProcess.set(true)
            inputBuffer.poll().also { inputData ->
                debugLogBufferSize("poll")
                if (inputData != null) {
//                    System.out.println("FileDispatcher handle start ${inputData[0]} ${inputData[1]} ${inputData[2]}")
                    middleware?.handle(inputData) { outputData ->
//                        System.out.println("FileDispatcher handle to ${outputData[0]} ${outputData[1]} ${outputData[2]}")
                        outputBuffer?.offer(outputData)
                        inProcess.set(false)
                        tryWakeUp()
                    }
                } else {
//                    System.out.println("FileDispatcher error")
                    inProcess.set(false)
                    tryWakeUp()
                }
            }
        }
    }

    override fun canRelease(): Boolean {
        return (inputBuffer.isEmpty() && outputBuffer.isNullOrEmpty() && !inProcess.get())
    }

    private fun debugLogBufferSize(tag: String) {
        if (!DEBUG_MODE) return
        var size = 0L
        inputBuffer.forEach {
            size += it.size
        }
        debugLog("性能: debugLogBufferSize[$tag]: ${formatSize(size)}")
    }

    private fun formatSize(param: Long? = 0L): String {
        val size = param ?: 0L
        val kb = size / 1024
        val mb = kb / 1024
        val gb = mb / 1024
        return when {
            gb > 0 -> "$gb GB"
            mb > 0 -> "$mb MB"
            kb > 0 -> "$kb KB"
            else -> "$size B"
        }
    }
}