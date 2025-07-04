package com.cyanrain.baselibrary.common

interface JsonConverter<T> {
    /**
     * 将对象转换为json字符串
     * @param any 需要转换的对象
     * @return json
     */

    fun toJson(any: T): String

    /**
     * 将json字符串转换为对象
     * @param json 需要转换的json字符串
     * @return 对象
     */

    fun fromJson(json: String): T
}