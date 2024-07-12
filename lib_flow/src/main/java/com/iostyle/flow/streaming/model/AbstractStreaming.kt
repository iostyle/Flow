package com.iostyle.flow.streaming.model

import com.iostyle.flow.middleware.model.IMiddleware
import java.util.concurrent.ConcurrentLinkedDeque

abstract class AbstractStreaming(
    var outputBuffer: ConcurrentLinkedDeque<ByteArray>?, var middleware: IMiddleware<ByteArray>?, var onRelease: (() -> Unit)? = null
) {

    abstract suspend fun safeOffer(byteArray: ByteArray): Boolean
    abstract fun offer(byteArray: ByteArray): Boolean
    abstract fun release(invokeCallback: Boolean = true)
    abstract fun canRelease(): Boolean
    protected abstract fun tryWakeUp()

}