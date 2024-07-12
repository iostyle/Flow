package com.iostyle.flow.file

import com.iostyle.flow.file.constant.FileSecureEnum
import com.iostyle.flow.middleware.model.IMiddleware
import com.iostyle.flow.streaming.constant.RELEASE_NEVER_TIMEOUT
import java.io.File

interface IFileDispatcher {
    suspend fun handle(
        origin: File,
        outputPath: String,
        middleware: IMiddleware<ByteArray>,
        chunkSize: Int = 16 * 1024,
        originHeadSize: Int? = null,
        onProgress: ((String, Float) -> Unit)? = null,
        callback: (File?, FileSecureEnum) -> Unit
    )

    fun release(origin: File, timeout: Long = RELEASE_NEVER_TIMEOUT, invokeCallback: Boolean = true)
    fun release(id: String, timeout: Long = RELEASE_NEVER_TIMEOUT, invokeCallback: Boolean = true)
}