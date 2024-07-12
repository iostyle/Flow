package com.iostyle.flow.streaming.model

import com.iostyle.flow.middleware.model.IMiddleware
import com.iostyle.flow.streaming.constant.PROCESSOR_LOOPER_INTERVAL_SLOW
import com.iostyle.flow.streaming.constant.ProcessorLooperIntervalEnum
import com.iostyle.flow.streaming.constant.RELEASE_NEVER_TIMEOUT

interface IStreamingDispatcher<T> {
    /**
     * @param middleware 需要调用的中间件
     * @param processorLoopInterval 下行无数据时轮询间隔时间
     * @param callback 内部保障有序处理
     * @param onRelease 真正被释放资源时被调用
     */
    fun bind(
        id: String,
        middleware: IMiddleware<T>,
        @ProcessorLooperIntervalEnum processorLoopInterval: Long = PROCESSOR_LOOPER_INTERVAL_SLOW,
        onRelease: (() -> Unit)? = null,
        callback: suspend (output: T) -> Unit,
    )

    /**
     * 添加待处理数据
     *
     * @return 是否添加成功
     */
    fun offer(id: String, byteArray: T): Boolean

    suspend fun safeOffer(id: String, byteArray: T): Boolean

    /**
     * 根据id获取操作实体
     */
    fun find(id: String): AbstractStreaming?

    /**
     * @param timeout 最大等待时间，默认为 RELEASE_NEVER_TIMEOUT
     */
    fun release(id: String, timeout: Long = RELEASE_NEVER_TIMEOUT, invokeCallback: Boolean = true)
}