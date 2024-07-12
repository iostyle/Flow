package com.iostyle.flow.middleware

import com.iostyle.flow.middleware.model.AbstractMiddleware

object EncryptionMiddleware : AbstractMiddleware<ByteArray>() {

    override suspend fun transform(originData: ByteArray): ByteArray {

        return originData
    }
}