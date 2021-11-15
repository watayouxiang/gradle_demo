package com.watayouxiang.router.gradle

import com.android.build.api.transform.*
import com.android.build.gradle.internal.pipeline.TransformManager
import com.android.utils.FileUtils

import java.util.jar.JarOutputStream
import java.util.zip.ZipEntry

class RouterMappingTransform extends Transform {
    /**
     * 当前 Transform 的名称
     */
    @Override
    String getName() {
        return "RouterMappingTransform"
    }

    /**
     * 返回告知编译器，当前 Transform 需要消费的输入类型
     * 在这里是 CLASS 类型
     */
    @Override
    Set<QualifiedContent.ContentType> getInputTypes() {
        return TransformManager.CONTENT_CLASS
    }

    /**
     * 告诉编译器，当前 Transform 需要收集的范围
     */
    @Override
    Set<? super QualifiedContent.Scope> getScopes() {
        return TransformManager.SCOPE_FULL_PROJECT
    }

    /**
     * 告诉编译器，当前 Transform 是否支持增量
     * 通常返回 false
     */
    @Override
    boolean isIncremental() {
        return false
    }

    /**
     * 所有 class 收集好以后，会被打包传入此方法
     */
    @Override
    void transform(TransformInvocation transformInvocation) throws TransformException, InterruptedException, IOException {
        //super.transform(transformInvocation)
        // 1、遍历所有的Input
        // 2、对Input进行二次处理
        // 3、将Input拷贝到目标目录

        RouterMappingCollector collector = new RouterMappingCollector()

        // 遍历所有的输入
        // 如果 app module 有：build/intermediates/transforms/RouterMappingTransform，说明拷贝成功
        transformInvocation.inputs.each {
            // 把文件夹类型的输入，拷贝到目标目录
            it.directoryInputs.each { dirInput ->
                def destDir = transformInvocation.outputProvider.getContentLocation(
                        dirInput.name,
                        dirInput.contentTypes,
                        dirInput.scopes,
                        Format.DIRECTORY)
                collector.collect(dirInput.file)
                FileUtils.copyDirectory(dirInput.file, destDir)
            }
            // 把 JAR 类型的输入，拷贝到目标目录
            it.jarInputs.each { jarInput ->
                def dest = transformInvocation.outputProvider.getContentLocation(
                        jarInput.name,
                        jarInput.contentTypes,
                        jarInput.scopes,
                        Format.JAR)
                collector.collectFromJarFile(jarInput.file)
                FileUtils.copyFile(jarInput.file, dest)
            }
        }

        // RouterMappingTransform all mapping class name = [RouterMapping_1617693731656, RouterMapping_1617693732873]
        println("${getName()} all mapping class name = " + collector.mappingClassName)

        //-------------------------------- 使用 RouterMappingByteCodeBuilder
        File mappingJarFile = transformInvocation.outputProvider.getContentLocation(
                "router_mapping",
                getOutputTypes(),
                getScopes(),
                Format.JAR)

        println("${getName()} mappingJarFile = $mappingJarFile")

        if (mappingJarFile.getParentFile().exists()) {
            mappingJarFile.getParentFile().mkdirs()
        }
        if (mappingJarFile.exists()) {
            mappingJarFile.delete()
        }

        // 将生成的字节码写入本地文件
        FileOutputStream fos = new FileOutputStream(mappingJarFile)
        JarOutputStream jarOutputStream = new JarOutputStream(fos)
        ZipEntry zipEntry = new ZipEntry(RouterMappingByteCodeBuilder.CLASS_NAME + ".class")
        jarOutputStream.putNextEntry(zipEntry)
        jarOutputStream.write(RouterMappingByteCodeBuilder.get(collector.mappingClassName))

        jarOutputStream.closeEntry()
        jarOutputStream.close()
        fos.close()

        /*
        验证：
        $ ./gradlew clean
        $ ./gradlew :app:assembleDebug -q

        日志输出：RouterMappingTransform mappingJarFile = app/build/intermediates/transforms/RouterMappingTransform/debug/48.jar
        查看build目录：app/build/intermediates/transforms/RouterMappingTransform/debug/48.jar

        $ cd /Users/TaoWang/Desktop/gradle_demo/gradle_router/app/build/intermediates/transforms/RouterMappingTransform/debug
        // 将 48.jar 解压到 48
        $ unzip 48.jar -d 48

        查看解压后的 RouterMapping.class 代码，验证正确性
         */
    }
}