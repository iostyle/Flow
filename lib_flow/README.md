## module_secure

### 接口设计

#### 一. 上下行双路缓存调度
一个保障插入数据有序处理，返回数据有序冷流传输，中间件可替换，不长期占用、阻塞线程资源，支持并发的多线程任务调度器
##### 1. Dispatcher 调度器使用方法

1.1 bind方法

| 参数                    | 释义                                                                                                                                   |
| --------------------- | ------------------------------------------------------------------------------------------------------------------------------------ |
| id                    | 主键                                                                                                                                   |
| processorLoopInterval | 下行无任务处理时，轮询间隔时间，可选参数PROCESSOR_LOOPER_INTERVAL_SLOW PROCESSOR_LOOPER_INTERVAL_FAST PROCESSOR_LOOPER_INTERVAL_NO                       |
| middleware            | 数据处理中间件，项目提供                                      EncryptionMiddleware  DecryptionMiddleware，可自定义继承AbsMiddleware<T> : IMiddleware 实现 |
| callback              | 处理完成回调，声明为suspend，会将下行Job挂起                                                                                                          |
| onRelease             | 上下行任务全部处理完毕后才会被调用，内部资源自动释放无需关注                                                                                                       |

```Kotlin
StreamingDispatcher.bind(  
    id = id,  
    processorLoopInterval = PROCESSOR_LOOPER_INTERVAL_SLOW,  
    middleware = EncryptionMiddleware,  
    callback = { output ->  
        
    },  
    onRelease = {  
        
    })
```
1.2 offer 插入数据
```Kotlin
StreamingDispatcher.offer(id, byteArray)
```
1.3 release 释放

| 参数                    | 释义                   |
| --------------------- | -------------------- |
| RELEASE_NEVER_TIMEOUT | 不设置最大超时时间，等待执行完毕后再释放 |
| RELEASE_NOW           | 马上释放                 |
```Kotlin
StreamingDispatcher.release(id, RELEASE_NEVER_TIMEOUT)
```

##### 2. Processor 下行任务调度器
外部使用无需过多关注，内部自动调度下行callback，自动释放资源

#### 二. 中间件

项目提供， 加密解密中间件，在需要传入的地方直接使用即可

| 参数                   | 释义    |
| -------------------- | ----- |
| EncryptionMiddleware | 加密中间件 |
| DecryptionMiddleware | 解密中间件 |
也可自定义，实现transform方法
```Kotlin
abstract class AbsMiddleware<T> : IMiddleware<T> {  
    override fun handle(data: T, callback: (T) -> Unit) {  
        GlobalScope.launch(Dispatchers.IO) {  
            callback.invoke(transform(data))  
        }  
    }  
  
    abstract fun transform(data: T): T  
}
```

#### 三. 文件相关

##### FileDispatcher
1. handle
```Kotlin
suspend fun handle(  
        origin: File, outputPath: String, middleware: IMiddleware<ByteArray>, callback: (File) -> Unit  
    )  
```

| 参数         | 释义          |
| ---------- | ----------- |
| origin     | 原始文件        |
| outputPath | 输出路径        |
| middleware | 对数据进行处理的中间件 |
| callback   | 完成回调        |
2. release
```Kotlin
fun release(origin: File, timeout: Long = RELEASE_NEVER_TIMEOUT)  
fun release(id: String, timeout: Long = RELEASE_NEVER_TIMEOUT)  
```

| 参数      | 释义                                             |
| ------- | ---------------------------------------------- |
| origin  | 原始文件                                           |
| id      | 原始文件的hashcode                                  |
| timeout | 释放超时时间，可选参数为 RELEASE_NEVER_TIMEOUT，RELEASE_NOW |
