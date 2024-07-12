package com.iostyle.flow.middleware.model

import com.iostyle.flow.utils.DEBUG_MODE
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlin.math.max

abstract class AbstractMiddleware<T> : IMiddleware<T> {
    override fun handle(originData: T, callback: (T) -> Unit) {
        GlobalScope.launch(Dispatchers.IO) {
            current = System.currentTimeMillis()
            callback.invoke(transform(originData))
            debugLog((originData as ByteArray).size)
        }
    }

    abstract suspend fun transform(originData: T): T

    private var current: Long = 0

    private fun debugLog(size: Int) {
        if (!DEBUG_MODE) return
        val time = System.currentTimeMillis() - current
        com.iostyle.flow.utils.debugLog("性能: size:$size, ${size / max(1, time)} byte/ms, 耗时 $time ms")

    }
}