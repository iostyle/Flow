package com.iostyle.flow.utils

const val DEBUG_MODE = true

fun debugLog(log: String) {
    if (DEBUG_MODE) {
        System.out.println(log)
    }
}

fun debugByteArray(tag: String, byteArray: ByteArray?) {
    try {
        if (byteArray == null) {
            debugLog("SecureUtil 【$tag】｜ size = 0")

        } else {
            debugLog("SecureUtil 【$tag】｜ size = ${byteArray.size} ｜ ${byteArray[0]} ${byteArray[1]} ${byteArray[2]} ${byteArray[3]} /  ${byteArray[byteArray.size - 4]} ${byteArray[byteArray.size - 3]} ${byteArray[byteArray.size - 2]} ${byteArray[byteArray.size - 1]}")
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

fun debugSecureHead(byteArray: ByteArray?) {
    try {
        if (byteArray == null) {
            debugLog("SecureUtil analyze head｜ size = 0")

        } else {
            debugLog("SecureUtil analyze head｜ size = ${byteArray.size} | ${byteArray.getOriginHeadSize()}")
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}