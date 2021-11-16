package com.watayouxiang.gradlerouter

import android.app.Application
import com.watayouxiang.gradle.router.runtime.Router

/**
 * <pre>
 *     author : TaoWang
 *     e-mail : watayouxiang@qq.com
 *     time   : 2021/11/16
 *     desc   :
 * </pre>
 */
class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()

        // 初始化 路由器
        Router.init()
    }
}