package com.watayouxiang.gradle.router.runtime

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log

object Router {

    private const val TAG = "RouterTAG"

    // 编译期间生成的总映射表
    private const val GENERATED_MAPPING = "com.watayouxiang.gradlerouter.mapping.RouterMapping"

    // 存储所有映射表信息
    private val mapping: HashMap<String, String> = HashMap()

    /**
     * 初始化 Router
     */
    fun init() {
        try {
            val clazz = Class.forName(GENERATED_MAPPING)
            val method = clazz.getMethod("get")
            val allMapping = method.invoke(null) as Map<String, String>

            if (allMapping?.size > 0) {
                Log.i(TAG, "init: get all mapping:")
                allMapping.onEach {
                    Log.i(TAG, "        ${it.key} -> ${it.value}")
                }
                mapping.putAll(allMapping)
            }
        } catch (e: Throwable) {
            Log.i(TAG, "init: error while init router: $e")
        }
    }

    /**
     * 路由跳转
     */
    fun go(context: Context, url: String) {
        if (context == null || url == null) {
            Log.i(TAG, "go: param error")
            return
        }

        // 1、匹配URL，找到目标页面
        // router://watayouxiang/profile?name=imooc&message=hello

        val uri = Uri.parse(url)
        val scheme = uri.scheme
        val host = uri.host
        val path = uri.path

        var targetActivityClass = ""
        mapping.onEach {
            val rUri = Uri.parse(it.key)
            val rScheme = rUri.scheme
            val rHost = rUri.host
            val rPath = rUri.path

            if (rScheme == scheme && rHost == host && rPath == path) {
                targetActivityClass = it.value
            }
        }

        if (targetActivityClass == "") {
            Log.e(TAG, "go:     no destination found")
            return
        }

        // 2、解析URL里的参数，封装成一个 Bundle
        val bundle = Bundle()
        val query = uri.query
        query?.let {
            if (it.length >= 3) {// a=b 至少三个字符
                val args = it.split("&")
                args.onEach { arg ->
                    val splits = arg.split("=")
                    bundle.putSerializable(splits[0], splits[1])
                }
            }
        }

        // 3、打开对应的Activity，并传入参数
        try {
            val activity = Class.forName(targetActivityClass)
            val intent = Intent(context, activity)
            intent.putExtras(bundle)
            context.startActivity(intent)
        } catch (e: Throwable) {
            Log.e(TAG, "go: error while start activity: $targetActivityClass, e = $e")
        }
    }

}