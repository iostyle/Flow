package com.iostyle.flow.utils

import java.util.concurrent.ConcurrentHashMap
import kotlin.math.max
import kotlin.math.min

object SecureSpeedCache {

    private val SPEED = 20000L

    private val DELAY_MIN = 1L
    private val DELAY_MAX = 50L

    val speedCacheMap by lazy(LazyThreadSafetyMode.SYNCHRONIZED) { ConcurrentHashMap<Int, Long>() }

    fun getSpeedDelay(size: Int): Long {
        return speedCacheMap[size] ?: run {
            min(DELAY_MAX, max(size / SPEED, DELAY_MIN)).also {
                speedCacheMap[size] = it
            }
        }
    }
}