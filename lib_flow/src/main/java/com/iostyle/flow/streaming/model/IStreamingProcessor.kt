package com.iostyle.flow.streaming.model

import com.iostyle.flow.streaming.constant.PROCESSOR_LOOPER_INTERVAL_SLOW
import com.iostyle.flow.streaming.constant.ProcessorLooperIntervalEnum
import com.iostyle.flow.streaming.constant.RELEASE_NEVER_TIMEOUT
import java.util.concurrent.ConcurrentLinkedDeque

interface IStreamingProcessor<T> {
    /**
     * @param callback 内部保障有序处理
     */
    fun build(
        id: String,
        @ProcessorLooperIntervalEnum processorLoopInterval: Long = PROCESSOR_LOOPER_INTERVAL_SLOW,
        callback: suspend (output: T) -> Unit,
    ): ConcurrentLinkedDeque<T>

    /**
     * @param timeout 最大等待时间，默认为 RELEASE_NEVER_TIMEOUT
     */
    fun tryRelease(
        id: String,
        timeout: Long = RELEASE_NEVER_TIMEOUT,
        invokeCallback: Boolean = true,
        callback: (() -> Unit)? = null
    )

    fun isWorking(id: String): Boolean
}