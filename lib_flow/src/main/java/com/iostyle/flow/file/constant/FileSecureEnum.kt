package com.iostyle.flow.file.constant

enum class FileSecureEnum {
    SUCCESS,
    OUTPUT_READY_FAILURE, // 写入路径初始化失败
    SECURE_HEAD_MISSING, // 解密文件没有私有头
    SECURE_PILE_ERROR, // 读取插桩异常

    // 添加及修改加密头相关错误
    FILE_NOT_EXIST, // 文件不存在
    FILE_WITHOUT_SECURE_HEAD, // 文件没有加密头
    FILE_WITHOUT_ORIGIN_HEAD_PLACEHOLDER,  // 文件原始头没有占位符或占位符错误
    ORIGIN_HEAD_PLACEHOLDER_HAS_DATA, // 原始头占位符有数据
    ORIGIN_HEAD_SIZE_ZERO, // 修改文件原始头数据时，加密数据头里存储原始数据头长度为0
    ORIGIN_HEAD_SIZE_MISMATCH, // 修改文件原始头数据时，加密数据头里存储原始数据头长度+加密新增量 与存入数据长度不匹配

    EXCEPTION, // 未知异常

}