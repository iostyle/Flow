package com.iostyle.flow.streaming.constant

import androidx.annotation.LongDef

const val RELEASE_INTERVAL = 500L // 尝试释放间隔时间，防止内存泄露，无需过短

const val RELEASE_NEVER_TIMEOUT = 0L // 不设置最大超时时间，等待执行完毕后再释放

const val RELEASE_NOW = -1L // 马上释放，不计后果

// 当下行buffer无数据时，轮询间隔时间
const val PROCESSOR_LOOPER_INTERVAL_SLOW = 500L
const val PROCESSOR_LOOPER_INTERVAL_FAST = 50L
const val PROCESSOR_LOOPER_INTERVAL_NO = 0L // 不建议使用，占用协程资源

@LongDef(
    value = longArrayOf(
        PROCESSOR_LOOPER_INTERVAL_SLOW,
        PROCESSOR_LOOPER_INTERVAL_FAST,
        PROCESSOR_LOOPER_INTERVAL_NO
    )
)
@Retention(AnnotationRetention.SOURCE)
annotation class ProcessorLooperIntervalEnum
