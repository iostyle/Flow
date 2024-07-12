package com.iostyle.flow.file

import com.iostyle.flow.file.constant.FileSecureEnum
import com.iostyle.flow.middleware.DecryptionMiddleware
import com.iostyle.flow.middleware.EncryptionMiddleware
import com.iostyle.flow.middleware.model.IMiddleware
import com.iostyle.flow.streaming.StreamingDispatcher
import com.iostyle.flow.streaming.constant.*
import com.iostyle.flow.utils.SECURE_HEAD_SIZE
import com.iostyle.flow.utils.debugSecureHead
import com.iostyle.flow.utils.getOriginHeadSize
import com.iostyle.flow.utils.getSecureHead
import com.iostyle.flow.utils.initSecureHead
import com.iostyle.flow.utils.readyEmptyFile
import com.iostyle.flow.utils.securePile
import com.iostyle.flow.utils.toByteArray
import com.iostyle.flow.utils.toInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.IOException

object FileDispatcher : IFileDispatcher {

    /**
     * @param chunkSize 仅加密时传入分段大小
     */
    override suspend fun handle(
        origin: File,
        outputPath: String,
        middleware: IMiddleware<ByteArray>,
        chunkSize: Int,
        originHeadSize: Int?,
        onProgress: ((String, Float) -> Unit)?,
        callback: (File?, FileSecureEnum) -> Unit
    ) {

        withContext(Dispatchers.IO) {

            val id = "${origin.hashCode()}"
            val outputFile = File(outputPath)

            // 避免重复任务
            StreamingDispatcher.release(id, RELEASE_NOW)

            // 预估处理次数
            var total = 0
            // 当前处理i次数
            var current = 0

            // 准备输出路径
            if (!readyEmptyFile(outputFile)) {
                callback.invoke(null, FileSecureEnum.OUTPUT_READY_FAILURE)
                return@withContext
            }

            // 处理文件加密头信息
            val head: ByteArray? = if (middleware == EncryptionMiddleware) {
                outputFile.initSecureHead(
                    originHeadSize = originHeadSize
                )
            } else if (middleware == DecryptionMiddleware) {
                origin.getSecureHead()
            } else null

            debugSecureHead(head)

            if (head == null) {
                callback.invoke(null, FileSecureEnum.SECURE_HEAD_MISSING)
                return@withContext
            }

            StreamingDispatcher.bind(
                id = id,
                processorLoopInterval = PROCESSOR_LOOPER_INTERVAL_FAST,
                middleware = middleware,
                callback = {
                    withContext(Dispatchers.IO) {
                        if (middleware == EncryptionMiddleware) {
                            outputFile.appendBytes(securePile)
                            outputFile.appendBytes(it.size.toByteArray())
                        }
                        outputFile.appendBytes(it)
//                        debugByteArray("append", it)
                        current++
                        if (total > 0) {
                            onProgress?.invoke(id, Math.min(1f, current / total.toFloat()))
                        }
                    }
                },
                onRelease = {
                    System.out.println("FileDispatcher release")
                    GlobalScope.launch(Dispatchers.Main) {
                        callback.invoke(outputFile, FileSecureEnum.SUCCESS)
                    }
                })

            withContext(Dispatchers.IO) {
                try {
                    val fis = FileInputStream(origin)
                    var countOffset = 0

                    if (middleware == DecryptionMiddleware) {
                        // 跳过加密头
                        fis.read(ByteArray(SECURE_HEAD_SIZE))
                        countOffset += SECURE_HEAD_SIZE

                        // 获取原始头
                        head.getOriginHeadSize().let { size ->
                            if (size > 0) {
                                val originHead = ByteArray(size)
                                val bytesRead = fis.read(originHead)
                                if (bytesRead > 0) {
                                    withContext(Dispatchers.IO) {
                                        outputFile.appendBytes(originHead)
                                    }
                                }
                                countOffset += size
                            }
                        }
                    }

                    if (middleware == EncryptionMiddleware) {
                        val readSize = chunkSize
                        total = (origin.length() / chunkSize).toInt()

                        while (true) {
                            if (!readBuffer(id, readSize, fis)) {
                                break
                            }
                        }
                    } else if (middleware == DecryptionMiddleware) {
                        val pileSize = securePile.size
                        val pileBuffer = ByteArray(pileSize)
                        var bytesRead: Int
                        while (true) {
                            bytesRead = fis.read(pileBuffer)
                            if (bytesRead == -1 || bytesRead < pileSize) {
                                release(id = id, timeout = RELEASE_NEVER_TIMEOUT)
                                break
                            }
                            if (pileBuffer.contentEquals(securePile)) {
                                val sizeBuffer = ByteArray(4)
                                fis.read(sizeBuffer)

                                val readSize = sizeBuffer.toInt()

                                if (total == 0) {
                                    total = ((origin.length() - countOffset) / readSize).toInt()
                                }

                                if (!readBuffer(id, readSize, fis)) {
                                    break
                                }
                            } else {
                                release(id = id, timeout = RELEASE_NEVER_TIMEOUT, false)
                                withContext(Dispatchers.Main) {
                                    callback.invoke(null, FileSecureEnum.SECURE_PILE_ERROR)
                                }
                                break
                            }
                        }
                    }
                    fis.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                }

            }

        }
    }

    override fun release(id: String, timeout: Long, invokeCallback: Boolean) {
        StreamingDispatcher.release(id, timeout, invokeCallback)
    }

    override fun release(origin: File, timeout: Long, invokeCallback: Boolean) {
        release(id = "${origin.hashCode()}", timeout = timeout, invokeCallback)
    }


    private suspend fun readBuffer(id: String, readSize: Int, fis: FileInputStream): Boolean {
        val buffer = ByteArray(readSize)
        val bytesRead = fis.read(buffer)
        if (bytesRead == -1) {
            release(id = id, timeout = RELEASE_NEVER_TIMEOUT)
            return false
        } else if (bytesRead < readSize) {
            val chunk = ByteArray(bytesRead)
            System.arraycopy(buffer, 0, chunk, 0, bytesRead)

            return StreamingDispatcher.safeOffer(id, chunk)
        } else {
            return StreamingDispatcher.safeOffer(id, buffer)
        }
    }
}