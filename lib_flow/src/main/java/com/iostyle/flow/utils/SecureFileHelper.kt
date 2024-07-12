package com.iostyle.flow.utils

import com.iostyle.flow.file.constant.FileSecureEnum
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.RandomAccessFile

private const val SUFFIX = "enc"

/**
 * 普通文件名转化为加密文件名
 */
fun String.toSecurePath(): String {
    return "$this$SUFFIX"
}

/**
 * 加密文件名转换为原始文件名
 */
fun String.toOriginPath(): String {
    if (this.endsWith(SUFFIX))
        return this.substring(0, this.length - SUFFIX.length)
    return this
}

/**
 * return 是否是加密文件
 */
fun File.isSecureFile(): Boolean {
    if (!this.exists()) throw IllegalArgumentException("file not exists")
    return this.path.endsWith(SUFFIX)
}

/**
 * return 是否是加密文件
 */
fun String?.isSecurePath(): Boolean {
    return !this.isNullOrBlank() && this.endsWith(SUFFIX)
}

suspend fun readyEmptyFile(file: File): Boolean {
    return withContext(Dispatchers.IO) {
        if (file.exists()) {
            file.delete().also {
                if (!it) {
                    return@withContext false
                }
            }
        }
        if (!file.exists()) {
            return@withContext file.createNewFile()
        }
        true
    }
}

fun bytesToShort(highByte: Byte, lowByte: Byte): Short {
    return (((highByte.toInt() and 0xFF) shl 8) or (lowByte.toInt() and 0xFF)).toShort()
}

fun shortToBytes(value: Short): Pair<Byte, Byte> {
    val highByte = (value.toInt() shr 8).toByte()
    val lowByte = value.toByte()
    return Pair(highByte, lowByte)
}

private const val SECURE_BYTE_HEAD = 4396.toShort()
private const val SECURE_BYTE_TAIL = 1557.toShort()
const val SECURE_HEAD_SIZE = 16

/**
 * ｜0｜1｜2｜3｜4｜5｜6｜7｜8｜9｜10｜11｜12｜13｜14｜15｜
 * ｜0,1头｜2原始文件头长度｜待定，密钥信息｜14,15尾｜
 * 112c 0615
 */
fun ByteArray.isSecureHead(): Boolean {
    if (this.isEmpty() || this.size < SECURE_HEAD_SIZE) return false
    return (bytesToShort(this[0], this[1]) == SECURE_BYTE_HEAD && bytesToShort(this[14], this[15]) == SECURE_BYTE_TAIL)
}

fun ByteArray.getOriginHeadSize(): Int {
    return this[2].toInt()
}

/**
 * 初始化私有头 确保在IO线程运行
 * 注意：如果 originHeadSize 原始头大小设置不为null 一定不要忘记手动调用 updateOriginHead 更新原始头信息
 */
fun File.initSecureHead(
    originHeadSize: Int? = null
): ByteArray {
    val head = ByteArray(SECURE_HEAD_SIZE).secureHeadBuild(originHeadSize).also {
        debugSecureHead(it)
    }
    this.writeBytes(head)
    if (originHeadSize != null && originHeadSize > 0) {
        val originHeadPlaceHolder = ByteArray(originHeadSize)
        this.appendBytes(originHeadPlaceHolder)
    }
    return head
}

fun ByteArray?.secureHeadBuild(
    originHeadSize: Int? = null
): ByteArray {
    return (this ?: ByteArray(SECURE_HEAD_SIZE)).also { bytes ->
        assert(bytes.size == SECURE_HEAD_SIZE)

        bytes[0] = 0x11
        bytes[1] = 0x2c
        bytes[14] = 0x06
        bytes[15] = 0x15

        originHeadSize?.let {
            bytes[2] = it.toByte()
        }
    }
}

/**
 * 更新加密文件的原始文件头信息
 *
 * @return 错误信息
 */
fun File.updateOriginHead(byteArray: ByteArray): FileSecureEnum {
    try {
        checkOriginHeadWriteable(byteArray).let {
            if (it != FileSecureEnum.SUCCESS) return it
        }

        val file = RandomAccessFile(this.path, "rw")
        file.seek(SECURE_HEAD_SIZE.toLong())
        file.write(byteArray)
        file.close()
        return FileSecureEnum.SUCCESS
    } catch (e: Exception) {
        e.printStackTrace()
        return FileSecureEnum.EXCEPTION
    }
}

fun File.checkOriginHeadWriteable(byteArray: ByteArray): FileSecureEnum {
    // 文件不存在
    if (!this.exists()) return FileSecureEnum.FILE_NOT_EXIST
    // 文件私有头不存在
    val head: ByteArray
    val originHead: ByteArray

    this.getSecureAndOriginHead().let {
        head = it.first ?: return FileSecureEnum.FILE_WITHOUT_SECURE_HEAD
        originHead = it.second ?: return FileSecureEnum.FILE_WITHOUT_ORIGIN_HEAD_PLACEHOLDER
    }

    head.getOriginHeadSize().let { originHeadSize ->
        // 私有头中原始头长度为 0
        if (originHeadSize == 0) return FileSecureEnum.ORIGIN_HEAD_SIZE_ZERO
        // 原始头长度和传入的数组长度不一致
        if (originHeadSize != byteArray.size) return FileSecureEnum.ORIGIN_HEAD_SIZE_MISMATCH

        originHead.forEach {
            // 原始头占位不为 0
            if (it != 0.toByte()) return FileSecureEnum.ORIGIN_HEAD_PLACEHOLDER_HAS_DATA
        }
    }
    return FileSecureEnum.SUCCESS
}

fun File.getSecureHead(): ByteArray? {
    val fis = FileInputStream(this)
    val buffer = ByteArray(SECURE_HEAD_SIZE)
    val bytesRead = fis.read(buffer)
    fis.close()

    if (bytesRead < SECURE_HEAD_SIZE || !buffer.isSecureHead()) {
        return null
    } else {
        return buffer
    }
}

fun File.getSecureAndOriginHead(): Pair<ByteArray?, ByteArray?> {
    val fis = FileInputStream(this)
    val buffer = ByteArray(SECURE_HEAD_SIZE)
    val bytesRead = fis.read(buffer)

    if (bytesRead < SECURE_HEAD_SIZE || !buffer.isSecureHead()) {
        fis.close()
        return Pair(null, null)
    } else {
        val originBuffer = ByteArray(buffer.getOriginHeadSize())
        val originBytesRead = fis.read(originBuffer)
        fis.close()
        return Pair(buffer, if (originBytesRead < originBuffer.size) null else originBuffer)
    }
}

val securePile by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
    ByteArray(4).also {
        it[0] = 0x11
        it[1] = 0x2c
        it[2] = 0x06
        it[3] = 0x15
    }
}

fun Int.toByteArray(): ByteArray {
    return byteArrayOf(
        (this shr 24).toByte(),
        (this shr 16).toByte(),
        (this shr 8).toByte(),
        this.toByte()
    )
}

fun Short.toByteArrayBE(): ByteArray {
    return byteArrayOf(this.toByte(), (this.toInt() shr 8).toByte())
}

fun Int.toByteArrayBE(): ByteArray {
    return byteArrayOf(
        this.toByte(),
        (this shr 8).toByte(),
        (this shr 16).toByte(),
        (this shr 24).toByte(),
    )
}

fun ByteArray.toInt(): Int {
    require(this.size == 4) { "ByteArray must be of size 4" }
    return (this[3].toInt() and 0xFF) or
            ((this[2].toInt() and 0xFF) shl 8) or
            ((this[1].toInt() and 0xFF) shl 16) or
            ((this[0].toInt() and 0xFF) shl 24)
}


fun modifyFileExtension(filePath: String, newExtension: String): String {
    val lastDotIndex = filePath.lastIndexOf('.')
    return if (lastDotIndex != -1) {
        filePath.substring(0, lastDotIndex) + "." + newExtension
    } else {
        filePath + "." + newExtension
    }
}
