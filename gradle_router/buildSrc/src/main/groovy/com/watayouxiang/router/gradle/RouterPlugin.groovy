package com.watayouxiang.router.gradle

import com.android.build.api.transform.Transform
import com.android.build.gradle.AppExtension
import com.android.build.gradle.AppPlugin
import groovy.json.JsonSlurper
import org.gradle.api.Plugin
import org.gradle.api.Project

class RouterPlugin implements Plugin<Project> {
    // 实现apply方法，注入插件的逻辑
    @Override
    void apply(Project project) {

        // 注册Transform
        if (project.plugins.hasPlugin(AppPlugin)) {
            AppExtension appExtension = project.extensions.getByType(AppExtension)
            Transform transform = new RouterMappingTransform()
            appExtension.registerTransform(transform)
        }

        // 1、自动帮助用户传递路径参数到注解处理器中
        //     kapt {
        //        arguments {
        //            arg("root_project_dir", rootProject.projectDir.absolutePath)
        //        }
        //    }
        if (project.extensions.findByName("kapt") != null) {
            project.extensions.findByName("kapt").arguments {
                arg("root_project_dir", project.rootProject.projectDir.absolutePath)
            }
        }

        // 2、实现旧的构建产物的自动清理
        project.clean.doFirst {
            // 删除上一次构建生成的 router_mapping 目录
            File routerMappingDir = new File(project.rootProject.projectDir, "router_mapping")
            if (routerMappingDir.exists()) {
                routerMappingDir.deleteDir()
            }
        }

        // 容错处理，只处理App module，lib module则不处理
        if (!project.plugins.hasPlugin(AppPlugin)) {
            return
        }

        println("i am from RouterPlugin, apply from ${project.name}")

        // 创建 Extension
        project.getExtensions().create("router", RouterExtension)

        // 获取 Extension
        project.afterEvaluate {
            RouterExtension extension = project["router"]
            println("用户设置的 wikiDir 路径：${extension.wikiDir}")

            // 3、在 javac 任务 (compileDebugJavaWithJavac) 后，汇总生成文档
            project.tasks.findAll { task ->
                task.name.startsWith('compile') && task.name.endsWith('JavaWithJavac')
            } each { task ->
                task.doLast {
                    File routerMappingDir = new File(project.rootProject.projectDir, "router_mapping")
                    if (!routerMappingDir.exists()) {
                        return
                    }
                    File[] allChildFiles = routerMappingDir.listFiles()
                    if (allChildFiles.length < 1) {
                        return
                    }

                    StringBuilder markdownBuilder = new StringBuilder()
                    markdownBuilder.append("# 页面文档\n\n")
                    allChildFiles.each { child ->
                        if (child.name.endsWith(".json")) {
                            JsonSlurper jsonSlurper = new JsonSlurper()
                            def content = jsonSlurper.parse(child)
                            content.each { innerContent ->
                                def url = innerContent['url']
                                def description = innerContent['description']
                                def realPath = innerContent['realPath']
                                markdownBuilder.append("## $description\n")
                                markdownBuilder.append("- url: $url\n")
                                markdownBuilder.append("- realPath: $realPath\n\n")
                            }
                        }
                    }

                    File wikiFileDir = new File(extension.wikiDir)
                    if (!wikiFileDir.exists()) {
                        wikiFileDir.mkdir()
                    }
                    File wikiFile = new File(wikiFileDir, "RouterMapping.md")
                    if (wikiFile.exists()) {
                        wikiFile.delete()
                    }
                    wikiFile.write(markdownBuilder.toString())
                }
            }
            // 测试 MD文档 是否生成
            // $ ./gradlew clean -q
            // $ ./gradlew :app:assembleDebug -q
        }
    }
}