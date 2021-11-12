package com.watayouxiang.router.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project

class RouterPlugin implements Plugin<Project> {
    // 实现apply方法，注入插件的逻辑
    @Override
    void apply(Project project) {
        println("i am from RouterPlugin, apply from ${project.name}")

        // 创建 Extension
        project.getExtensions().create("router", RouterExtension)

        // 获取 Extension
        project.afterEvaluate {
            RouterExtension extension = project["router"]
            println("用户设置的 wikiDir 路径：${extension.wikiDir}")
        }
    }
}