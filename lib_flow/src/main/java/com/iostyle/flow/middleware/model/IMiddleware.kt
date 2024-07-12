package com.iostyle.flow.middleware.model

interface IMiddleware<T> {
    fun handle(originData: T, callback: (T) -> Unit)
}