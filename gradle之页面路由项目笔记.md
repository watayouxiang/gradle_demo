# gradle之页面路由项目笔记

## 第一节：Plugin开发步骤

### 1、创建插件工程

建立 buildSrc 目录，建立 build.gradle

```
// 引用 groovy 插件，编译插件工程中的代码
apply plugin: 'groovy'

// 声明仓库的地址
repositories {
    jcenter()
}

// 声明依赖的包
dependencies {
    implementation gradleApi()
    implementation localGroovy()
}
```

建立 src/main/groovy/com/watayouxiang/router/gradle/RouterExtension.groovy

```
package com.watayouxiang.router.gradle

class RouterExtension {
    String wikiDir
}
```

建立 src/main/groovy/com/watayouxiang/router/gradle/RouterPlugin.groovy

```
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
```

建立 src/main/resources/META-INF/gradle-plugins/com.watayouxiang.router.properties

```
implementation-class=com.watayouxiang.router.gradle.RouterPlugin
```

### 2、发布插件到本地仓库

拷贝一份插件工程 buildSrc，重命名为 router-gradle-plugin

router-gradle-plugin 工程的 build.gradle 添加如下

```
// 调用 maven 插件，用于发布自己的插件
apply plugin: 'maven'

// 配置 maven 插件中的 uploadArchives 任务
uploadArchives {
    repositories {
        mavenDeployer {
            // 设置发布路径为 工程根目录下面的 repo 文件夹
            repository(url: uri('../repo')) {
                // 设置groupId，通常为包名
                pom.groupId = 'com.watayouxiang.router'
                // 设置artifactId，为当前插件的名称
                pom.artifactId = 'router-gradle-plugin'
                // 设置插件版本号
                pom.version = '1.0.0'
            }
        }
    }
}
```

settings.gradle中添加如下：

```
include ':router-gradle-plugin'

// 执行发布命令：terminal 中输入
// $ ./gradlew :router-gradle-plugin:uploadArchives
```

### 3、使用buildSrc中的插件

未发布的插件是指 buildSrc 目录中的插件

app module 的 build.gradle 中写入

```
// 应用自己的插件
apply plugin: 'com.watayouxiang.router'

// 向路由插件传递参数
router {
    wikiDir getRootDir().absolutePath
}

// 控制台输入：
// $./gradlew clean -q
//
// 回显：
// i am from RouterPlugin, apply from app
// 用户设置的 wikiDir 路径：/Users/TaoWang/Desktop/gradle_demo/gradle_router
// 
// 说明应用 buildSrc 插件成功
```

### 4、使用本地仓库的插件

根目录 build.gradle 添加如下

```
buildscript {
    // gradle 插件所在的仓库
    repositories {
        /**
         * 配置maven仓库地址
         * 这里可以是相对路径地址，也可以是绝对路径地址
         */
        maven {
            url uri("/Users/TaoWang/Desktop/gradle_demo/gradle_router/repo")
        }
    }

    // gradle 插件
    dependencies {
        /**
         * 声明依赖的插件
         * 形式是：groupId : artifactId : version
         */
        classpath 'com.watayouxiang.router:router-gradle-plugin:1.0.0'
    }
}
```

app module 的 build.gradle 中写入

```
// 应用自己的插件
apply plugin: 'com.watayouxiang.router'

// 向路由插件传递参数
router {
    wikiDir getRootDir().absolutePath
}
```

### 5、kapt的使用

> apt只能收集java的注解，如果还要收集kotlin注解的话，需要使用kapt。

根目录的 build.gradle 编写

```
buildscript {
    dependencies {
        // 添加 kotlin 编译插件
        classpath 'org.jetbrains.kotlin:kotlin-gradle-plugin:1.5.31'
    }
}
```

app module 的 build.gradle 编写

```
// kotlin 插件
apply plugin: 'kotlin-android'
apply plugin: 'kotlin-kapt'

// 配置 kapt 参数
android {
    kapt {
        arguments {
            arg("root_project_dir", rootProject.projectDir.absolutePath)
        }
    }
}

dependencies {
//    implementation project(':router-annotations')
//    annotationProcessor project(':router-processor')

//    implementation 'com.watayouxiang.router:router-annotations:1.0.0'
//    annotationProcessor 'com.watayouxiang.router:router-processor:1.0.0'

    implementation project(':router-annotations')
    kapt project(':router-processor')
}
```

com.watayouxiang.router.processor.DestinationProcessor#process 中添加如下代码，用于获取 kapt 的参数 root_project_dir。

```
// com.watayouxiang.router.processor.DestinationProcessor#process 中添加如下代码：
// 获取 kapt 的参数 root_project_dir
String rootDir = processingEnv.getOptions().get("root_project_dir");
System.out.println(TAG + " >>> rootDir = " + rootDir);

// 打印结果：
// >>> rootDir = /Users/TaoWang/Desktop/gradle_demo/gradle_router
```

## 第二节：APT采集页面路由信息

> 目的是自动生成如下class文件：

```
package com.watayouxiang.gradlerouter.mapping;
import java.util.HashMap;
import java.util.Map;

public class RouterMapping_1636709905463 {
	public static Map<String, String> get() {
		Map<String, String> mapping = new HashMap<>();
		mapping.put("router://page-home", "com.watayouxiang.gradlerouter.MainActivity");
		return mapping;
	}
}
```

### 1、建立Annotation工程

建立 router-annotations 目录

```
// ----------------------------
// 新建 module 的 build.gradle
// ----------------------------
// 应用 java 插件
apply plugin: 'java'

// 设置源码兼容性
targetCompatibility = JavaVersion.VERSION_1_7
sourceCompatibility = JavaVersion.VERSION_1_7

// ----------------------------
// setttings.gradle 中添加
// ----------------------------
include ':router-annotations'
```

创建注解 src/main/java/com/watayouxiang/router/annotations/Destination.java

```
package com.watayouxiang.router.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE})// 说明当前注解可以修饰的元素，此处表示可以用于标记在类上面
@Retention(RetentionPolicy.CLASS)// 说明当前注解可以被保留的时间
public @interface Destination {
    /**
     * 当前页面的url，不能为空
     *
     * @return 页面的url
     */
    String url();

    /**
     * 对于当前页面的中文描述
     *
     * @return 例如："个人主页"
     */
    String description();
}
```

### 2、建立APT工程

建立 router-processor 目录

```
// ----------------------------
// 新建 module 的 build.gradle
// ----------------------------
apply plugin: 'java'

dependencies {
    implementation project(':router-annotations')

    // 使用google的注解处理器，@AutoService(Processor.class)
    // 会帮助自动创建 META-INF/services/javax.annotation.processing.Processor 文件
    implementation 'com.google.auto.service:auto-service:1.0-rc6'
    annotationProcessor 'com.google.auto.service:auto-service:1.0-rc6'
    
    implementation 'com.google.code.gson:gson:2.8.1'
}

// ----------------------------
// setttings.gradle 中添加
// ----------------------------
include ':router-processor'
```

创建注解处理器 src/main/java/com/watayouxiang/router/processor/DestinationProcessor.java

```
package com.watayouxiang.router.processor;

import com.google.auto.service.AutoService;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.watayouxiang.router.annotations.Destination;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.Writer;
import java.util.Collections;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.tools.JavaFileObject;

/**
 * 告诉 javac 加载注解处理器 DestinationProcessor
 * <p>
 * 会帮助自动创建 META-INF/services/javax.annotation.processing.Processor 文件
 */
@AutoService(Processor.class)
public class DestinationProcessor extends AbstractProcessor {

    private static final String TAG = "DestinationProcessor";

    /**
     * 告诉编译器，当前处理器支持的注解类型
     */
    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return Collections.singleton(
                Destination.class.getCanonicalName()
        );
    }

    /**
     * 编译器找到我们关心的注解后，会回调这个方法
     */
    @Override
    public boolean process(Set<? extends TypeElement> set, RoundEnvironment roundEnvironment) {
        // 避免多次调用 process
        if (roundEnvironment.processingOver()) {
            return false;
        }

        System.out.println(TAG + " >>> process start ...");

        // 获取所有标记了 @Destination 注解的 类的信息
        Set<Element> allDestinationElements = (Set<Element>) roundEnvironment.getElementsAnnotatedWith(Destination.class);

        System.out.println(TAG + " >>> all Destination elements count = " + allDestinationElements.size());

        // 当未收集到 @Destination 注解的时候，跳过后续流程
        if (allDestinationElements.size() < 1) {
            return false;
        }

        // 将要自动生成的类的类名
        String className = "RouterMapping_" + System.currentTimeMillis();

        StringBuilder builder = new StringBuilder();

        builder.append("package com.watayouxiang.gradlerouter.mapping;\n");
        builder.append("import java.util.HashMap;\n");
        builder.append("import java.util.Map;\n\n");
        builder.append("public class ").append(className).append(" {\n");
        builder.append("\tpublic static Map<String, String> get() {\n");
        builder.append("\t\tMap<String, String> mapping = new HashMap<>();\n");


        final JsonArray destinationJsonArray = new JsonArray();

        // 遍历所有 @Destination 注解信息，挨个获取详细信息
        for (Element element : allDestinationElements) {

            final TypeElement typeElement = (TypeElement) element;

            // 尝试在当前类上，获取 @Destination 的信息
            final Destination destination = typeElement.getAnnotation(Destination.class);

            if (destination == null) continue;

            final String url = destination.url();
            final String description = destination.description();
            // 获取注解当前类的全类名
            final String realPath = typeElement.getQualifiedName().toString();

            System.out.println(TAG + " >>> url = " + url);
            System.out.println(TAG + " >>> description = " + description);
            System.out.println(TAG + " >>> realPath = " + realPath);

            builder.append("\t\tmapping.put(")
                    .append("\"" + url + "\"")
                    .append(", ")
                    .append("\"" + realPath + "\"")
                    .append(");\n");

            // 组装json对象
            JsonObject item = new JsonObject();
            item.addProperty("url", url);
            item.addProperty("description", description);
            item.addProperty("realPath", realPath);

            destinationJsonArray.add(item);
        }

        builder.append("\t\treturn mapping;\n");
        builder.append("\t}\n");
        builder.append("}");

        String mappingFullClassName = "com.watayouxiang.gradlerouter.mapping." + className;

        System.out.println(TAG + " >>> mappingFullClassName = " + mappingFullClassName);
        System.out.println(TAG + " >>> class content = \n" + builder);


        // 写入自动生成的类到本地文件中
        try {
            JavaFileObject source = processingEnv.getFiler().createSourceFile(mappingFullClassName);
            Writer writer = source.openWriter();
            writer.write(builder.toString());
            writer.flush();
            writer.close();
        } catch (Exception e) {
            throw new RuntimeException("Error while create file", e);
        }

        System.out.println(TAG + " >>> process finish ...");

        return false;
    }
}
```

目的是生成这样的文件

```
package com.watayouxiang.gradlerouter.mapping;
import java.util.HashMap;
import java.util.Map;

public class RouterMapping_1636709905463 {
	public static Map<String, String> get() {
		Map<String, String> mapping = new HashMap<>();
		mapping.put("router://page-home", "com.watayouxiang.gradlerouter.MainActivity");
		return mapping;
	}
}
```

### 3、使用Annotation

app module 的 build.gradle 添加

```
dependencies {
    // 依赖自己的注解
    implementation project(':router-annotations')
    // 依赖自己的注解处理器
    annotationProcessor project(':router-processor')
}
```

使用注解

```
package com.watayouxiang.gradlerouter;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import com.watayouxiang.router.annotations.Destination;

@Destination(url = "router://page-home", description = "应用主页")
public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }
}
```

测试注解

```
/**
 * 测试注解处理器
 *
 * 异常处理：Mac OS 升级到11.0.1后 ./gradlew :androiddemo:assembleDebug -q 编译项目出错
 * 资源库中找到 Internet Plug-Ins 文件夹，将文件夹名改为 InternetPlug-Ins
 * 参考：https://www.jianshu.com/p/3c1ad32a1def
 *
 * 注意：com.imooc.router.processor.DestinationProcessor 类中的日志，仅在第一次编译时打印.
 *      如果需要再次打印，需要先清楚缓存 ./gradlew clean -q
 *
 * // 1、清除缓存
 * $ ./gradlew clean -q
 *
 * // 2、开始debug编译
 * $ ./gradlew :app:assembleDebug -q
 *
 * //3、查看生成文件
 * 生成的 RouterMapping_xxx.java 文件在:
 * app module 的 build/generated/ap_generated_sources/out/${packagename} 目录下
 */
```

### 4、发布工程到本地仓库

根目录下的 gradle.properties 添加：

```
POM_URL=../repo
GROUP_ID=com.watayouxiang.router
VERSION_NAME=1.0.0
```

router-annotations 项目下新建 gradle.properties 文件并写入：

```
POM_ARTIFACT_ID=router-annotations
```

router-processor 项目下新建 gradle.properties 写入

```
POM_ARTIFACT_ID=router-processor
```

根目录下新建 maven-publish.gradle 文件并写入：

```
// ------------------------------------------------------------------------
// 使用maven插件中的发布功能
// ------------------------------------------------------------------------

apply plugin: 'maven'

// ------------------------------------------------------------------------
// 读取工程配置
// ------------------------------------------------------------------------

Properties rootProjectProperties = new Properties()
rootProjectProperties.load(project.rootProject.file('gradle.properties').newDataInputStream())
def POM_URL = rootProjectProperties.getProperty("POM_URL")
def GROUP_ID = rootProjectProperties.getProperty("GROUP_ID")
def VERSION_NAME = rootProjectProperties.getProperty("VERSION_NAME")

Properties childProjectProperties = new Properties()
childProjectProperties.load(project.file('gradle.properties').newDataInputStream())
def POM_ARTIFACT_ID = childProjectProperties.getProperty("POM_ARTIFACT_ID")

println("maven-publish POM_URL = $POM_URL")
println("maven-publish GROUP_ID = $GROUP_ID")
println("maven-publish VERSION_NAME = $VERSION_NAME")
println("maven-publish POM_ARTIFACT_ID = $POM_ARTIFACT_ID")

// ------------------------------------------------------------------------
// 发布到本地 maven 仓库的任务
// ------------------------------------------------------------------------

uploadArchives {
    repositories {
        mavenDeployer {

            // 填入发布信息
            repository(url: uri(POM_URL)) {
                pom.groupId = GROUP_ID
                pom.artifactId = POM_ARTIFACT_ID
                pom.version = VERSION_NAME
            }

            // 修改 router-processor 的 build.gradle 内容
            // 原本内容：dependencies { implementation project(':router-annotations') }
            // 修改后的内容：dependencies { implementation 'com.watayouxiang.router:router-annotations:1.0.0' }
            pom.whenConfigured { pom ->
                pom.dependencies.forEach { dep ->
                    if (dep.getVersion() == "unspecified") {
                        dep.setGroupId(GROUP_ID)
                        dep.setVersion(VERSION_NAME)
                    }
                }
            }
        }
    }
}
```

router-annotations 项目下 build.gradle 应用 maven-publish.gradle 插件

```
apply from : rootProject.file("maven-publish.gradle")
```

router-processor 项目下 build.gradle 应用 maven-publish.gradle 插件

```
apply from : rootProject.file("maven-publish.gradle")
```

执行发布命令

```
// 清理build文件
$ ./gradlew clean -q

// 项目上传到maven本地仓库
$ ./gradlew :router-annotations:uploadArchives
$ ./gradlew :router-processor:uploadArchives
```

### 5、应用maven仓库的aar

rootProject 的 build.gradle 写入仓库地址

```
buildscript {
    // 插件所在的仓库
    repositories {
        // 本地 maven 仓库地址
        maven {
            url uri("/Users/TaoWang/Desktop/gradle_demo/gradle_router/repo")
        }
    }
}

allprojects {
    // 工程依赖所在的仓库
    repositories {
        // 本地 maven 仓库地址
        maven {
            url uri("/Users/TaoWang/Desktop/gradle_demo/gradle_router/repo")
        }
    }
}
```

app module 的 build.gradle 中引用 注解和注解处理器

```
dependencies {
//    implementation project(':router-annotations')
//    annotationProcessor project(':router-processor')

    implementation 'com.watayouxiang.router:router-annotations:1.0.0'
    annotationProcessor 'com.watayouxiang.router:router-processor:1.0.0'
}
```

验证是否引用成功

```
// 清空build文件
$ ./gradlew clean -q

// 测试打包
$ ./gradlew :app:assembleDebug

// 查看 app module 的 build/generated/ap_generated_sources/debug/out 目录下生成的代码是否正确
```

## 第三节：ASM实现路由组件自动注册

> 目的是将多个 RouterMapping_xxx 合并汇总成 RouterMapping。为了生成如下代码：

```
public class RouterMapping {
    public static Map<String, String> get() {
        Map<String, String> map = new HashMap<>();
        map.putAll(RouterMapping_1636803627725.get());
        map.putAll(RouterMapping_6434692469264.get());
        //...
        return map;
    }
}
```

### 1、ASM插件使用

通过字节码插桩技术（ASM技术），在.class打包成.dex文件前对其进行修改。

- 安装 ASM Bytecode Viewer Support Kotlin 插件，帮助写ASM代码
- 在RouterMapping文件中，右键选择 ASM Bytecode Viewer 就能查看RouterMapping的二进制代码，再点击 ASMMified 选项卡，就能查看RouterMapping的ASM代码

### 2、引用Transform插件

在 buildSrc/build.gradle 添加如下

```
// 声明仓库的地址
repositories {
    // 包含 'com.android.tools.build:gradle:xxx'
    google()
}

// 声明依赖的包
dependencies {
    // 包含 com.android.build.api.transform.Transform
    implementation 'com.android.tools.build:gradle:4.1.3'
}
```

### 3、收集所有RouterMapping_xxx.class

创建 com.watayouxiang.router.gradle.RouterMappingCollector。

- 用于收集所有RouterMapping_xxx。

```
package com.watayouxiang.router.gradle

import java.util.jar.JarEntry
import java.util.jar.JarFile

class RouterMappingCollector {

    private static final String PACKAGE_NAME = 'com/watayouxiang/gradlerouter/mapping'
    private static final String CLASS_NAME_PREFIX = 'RouterMapping_'
    private static final String CLASS_FILE_SUFFIX = '.class'

    private final Set<String> mappingClassNames = new HashSet<>()

    /**
     * 获取收集好的映射表类名
     */
    Set<String> getMappingClassName() {
        return mappingClassNames
    }

    /**
     * 收集class文件或者class文件目录中的映射表
     */
    void collect(File classFile) {
        if (classFile == null || !classFile.exists()) return
        if (classFile.isFile()) {
            if (classFile.absolutePath.contains(PACKAGE_NAME)
                    && classFile.name.startsWith(CLASS_NAME_PREFIX)
                    && classFile.name.endsWith(CLASS_FILE_SUFFIX)) {
                String className = classFile.name.replace(CLASS_FILE_SUFFIX, "")
                mappingClassNames.add(className)
            }
        } else {
            classFile.listFiles().each {
                collect(it)
            }
        }
    }

    /**
     * 收集JAR包中的目标类
     */
    void collectFromJarFile(File jarFile) {
        Enumeration enumeration = new JarFile(jarFile).entries()

        while (enumeration.hasMoreElements()) {
            JarEntry jarEntry = (JarEntry) enumeration.nextElement()
            String entryName = jarEntry.getName()
            if (entryName.contains(PACKAGE_NAME)
                    && entryName.contains(CLASS_NAME_PREFIX)
                    && entryName.contains(CLASS_FILE_SUFFIX)) {
                String className = entryName.replace(PACKAGE_NAME, "")
                        .replace("/", "")
                        .replace(CLASS_FILE_SUFFIX, "")
                mappingClassNames.add(className)
            }
        }
    }
}
```

创建 com.watayouxiang.router.gradle.RouterMappingTransform。
- 实现类的拷贝逻辑
- 收集所有RouterMapping_xxx

```
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

        // --------------- 收集 RouterMapping_xxx.class ---------------

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

        // --------------- 生成 RouterMapping.class ---------------

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
    }
}
```

### 4、ASM生成RouterMapping.class

创建 com.watayouxiang.router.gradle.RouterMappingByteCodeBuilder。
- 用asm编写RouterMapping字节码

```
package com.watayouxiang.router.gradle

import jdk.internal.org.objectweb.asm.ClassWriter
import jdk.internal.org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes

/*
// 将多个 RouterMapping_xxx 合并汇总成 RouterMapping
public class RouterMapping {
    public static Map<String, String> get() {
        Map<String, String> map = new HashMap<>();
        map.putAll(RouterMapping_1636803627725.get());
        map.putAll(RouterMapping_6434692469264.get());
        //...
        return map;
    }
}

// 通过字节码插桩技术，在.class打包成.dex文件前对其进行修改。
// 修改字节码，通过ASM技术
1）安装 ASM Bytecode Viewer Support Kotlin 插件，帮助写ASM代码
2）在RouterMapping文件中，右键选择 ASM Bytecode Viewer 就能查看RouterMapping的二进制代码，
    再点击 ASMMified 选项卡，就能查看RouterMapping的ASM代码
 */
class RouterMappingByteCodeBuilder implements Opcodes {

    public static final String CLASS_NAME = "com/watayouxiang/gradlerouter/mapping/RouterMapping"

    static byte[] get(Set<String> allMappingNames) {
        // 1、创建一个类
        // 2、创建构造方法
        // 3、创建get方法
        //      1）创建一个Map
        //      2）塞入所有映射表的内容
        //      3）返回map

        // ClassWriter.COMPUTE_MAXS 自动计算局部变量需要的栈针大小
        ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS)
        // 1、创建一个类
        classWriter.visit(V1_8,
                ACC_PUBLIC | ACC_SUPER,
                CLASS_NAME, // 类名
                null,
                "java/lang/Object",// 父类
                null
        )

        MethodVisitor methodVisitor

        // 2、创建构造方法
        methodVisitor = classWriter.visitMethod(ACC_PUBLIC,
                "<init>",
                "()V",
                null,
                null)
        // 开启字节码的生成和访问
        methodVisitor.visitCode()
        methodVisitor.visitVarInsn(ALOAD, 0)
        methodVisitor.visitMethodInsn(INVOKESPECIAL,
                "java/lang/Object",
                "<init>",
                "()V",
                false)
        methodVisitor.visitInsn(RETURN)
        methodVisitor.visitMaxs(1, 1)
        methodVisitor.visitEnd()

        // 3、创建get方法
        methodVisitor = classWriter.visitMethod(ACC_PUBLIC | ACC_STATIC,
                "get",
                "()Ljava/util/Map;",
                "()Ljava/util/Map<Ljava/lang/String;Ljava/lang/String;>;",
                null)
        methodVisitor.visitCode()

        // 1）创建一个Map
        methodVisitor.visitTypeInsn(NEW, "java/util/HashMap")
        methodVisitor.visitInsn(DUP)
        methodVisitor.visitMethodInsn(INVOKESPECIAL,
                "java/util/HashMap",
                "<init>",
                "()V",
                false)
        methodVisitor.visitVarInsn(ASTORE, 0)
        // 2）塞入所有映射表的内容
        allMappingNames.each {
            methodVisitor.visitVarInsn(ALOAD, 0);
            methodVisitor.visitMethodInsn(INVOKESTATIC,
                    "com/watayouxiang/gradlerouter/mapping/$it",
                    "get", "()Ljava/util/Map;",
                    false)
            methodVisitor.visitMethodInsn(INVOKEINTERFACE,
                    "java/util/Map",
                    "putAll",
                    "(Ljava/util/Map;)V",
                    true)
        }
        // 3）返回map
        methodVisitor.visitVarInsn(ALOAD, 0)
        methodVisitor.visitInsn(ARETURN)
        methodVisitor.visitMaxs(2, 1)

        classWriter.visitEnd()

        return classWriter.toByteArray()
    }
}
```

### 5、验证生成结果

```
验证：
$ ./gradlew clean
$ ./gradlew :app:assembleDebug -q

日志输出：RouterMappingTransform mappingJarFile = app/build/intermediates/transforms/RouterMappingTransform/debug/48.jar
查看build目录：app/build/intermediates/transforms/RouterMappingTransform/debug/48.jar

$ cd /Users/TaoWang/Desktop/gradle_demo/gradle_router/app/build/intermediates/transforms/RouterMappingTransform/debug
// 将 48.jar 解压到 48
$ unzip 48.jar -d 48

查看解压后的 RouterMapping.class 代码，验证正确性
```

## 第四节：为gradle插件添加文档生成功能

### 1、思路解析

1、将路径参数 root_project_dir 传递到 kapt 注解处理器中。从而避免需要手动在app module的 build.gradle 中配置如下信息。

```
kapt {
	arguments {
		arg("root_project_dir", rootProject.projectDir.absolutePath)
	}
}
```

2、实现旧的构建产物的自动清理：删除上一次构建生成的 router_mapping 目录

3、在 javac 任务 (compileDebugJavaWithJavac) 后，汇总生成文档

```
// 可以在 setting.gradle 中打印本次构建执行的所有任务的名称，从而知晓 javac 任务的名称
gradle.taskGraph.beforeTask { task ->
 println("[all-task] " + task.name)
}

// $ ./gradlew :app:assembleDebug -q
```

### 2、根据RouterMapping_xxx.class生成mapping_xxx.json

项目 com.watayouxiang.router.processor.DestinationProcessor 编码修改如下：

```
package com.watayouxiang.router.processor;

import com.google.auto.service.AutoService;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.watayouxiang.router.annotations.Destination;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.Writer;
import java.util.Collections;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.tools.JavaFileObject;

/**
 * 告诉 javac 加载注解处理器 DestinationProcessor
 * <p>
 * 会帮助自动创建 META-INF/services/javax.annotation.processing.Processor 文件
 */
@AutoService(Processor.class)
public class DestinationProcessor extends AbstractProcessor {

    private static final String TAG = "DestinationProcessor";

    /**
     * 告诉编译器，当前处理器支持的注解类型
     */
    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return Collections.singleton(
                Destination.class.getCanonicalName()
        );
    }

    /**
     * 编译器找到我们关心的注解后，会回调这个方法
     */
    @Override
    public boolean process(Set<? extends TypeElement> set, RoundEnvironment roundEnvironment) {
        // 避免多次调用 process
        if (roundEnvironment.processingOver()) {
            return false;
        }

        System.out.println(TAG + " >>> process start ...");

        // 获取所有标记了 @Destination 注解的 类的信息
        Set<Element> allDestinationElements = (Set<Element>) roundEnvironment.getElementsAnnotatedWith(Destination.class);

        System.out.println(TAG + " >>> all Destination elements count = " + allDestinationElements.size());

        // 当未收集到 @Destination 注解的时候，跳过后续流程
        if (allDestinationElements.size() < 1) {
            return false;
        }

        // ------------------ 生成 RouterMapping_xxx.class ------------------

        // 将要自动生成的类的类名
        String className = "RouterMapping_" + System.currentTimeMillis();

        StringBuilder builder = new StringBuilder();

        builder.append("package com.watayouxiang.gradlerouter.mapping;\n");
        builder.append("import java.util.HashMap;\n");
        builder.append("import java.util.Map;\n\n");
        builder.append("public class ").append(className).append(" {\n");
        builder.append("\tpublic static Map<String, String> get() {\n");
        builder.append("\t\tMap<String, String> mapping = new HashMap<>();\n");


        final JsonArray destinationJsonArray = new JsonArray();

        // 遍历所有 @Destination 注解信息，挨个获取详细信息
        for (Element element : allDestinationElements) {

            final TypeElement typeElement = (TypeElement) element;

            // 尝试在当前类上，获取 @Destination 的信息
            final Destination destination = typeElement.getAnnotation(Destination.class);

            if (destination == null) continue;

            final String url = destination.url();
            final String description = destination.description();
            // 获取注解当前类的全类名
            final String realPath = typeElement.getQualifiedName().toString();

            System.out.println(TAG + " >>> url = " + url);
            System.out.println(TAG + " >>> description = " + description);
            System.out.println(TAG + " >>> realPath = " + realPath);

            builder.append("\t\tmapping.put(")
                    .append("\"" + url + "\"")
                    .append(", ")
                    .append("\"" + realPath + "\"")
                    .append(");\n");

            // 组装json对象
            JsonObject item = new JsonObject();
            item.addProperty("url", url);
            item.addProperty("description", description);
            item.addProperty("realPath", realPath);

            destinationJsonArray.add(item);
        }

        builder.append("\t\treturn mapping;\n");
        builder.append("\t}\n");
        builder.append("}");

        String mappingFullClassName = "com.watayouxiang.gradlerouter.mapping." + className;

        System.out.println(TAG + " >>> mappingFullClassName = " + mappingFullClassName);
        System.out.println(TAG + " >>> class content = \n" + builder);


        // 写入自动生成的类到本地文件中
        try {
            JavaFileObject source = processingEnv.getFiler().createSourceFile(mappingFullClassName);
            Writer writer = source.openWriter();
            writer.write(builder.toString());
            writer.flush();
            writer.close();
        } catch (Exception e) {
            throw new RuntimeException("Error while create file", e);
        }

        // ------------------ 生成 mapping_xxx.json ------------------

        // 获取 kapt 的参数 root_project_dir
        String rootDir = processingEnv.getOptions().get("root_project_dir");

        // 写入json到本地文件中
        File rootDirFile = new File(rootDir);
        if (!rootDirFile.exists()) {
            throw new RuntimeException("root_project_dir not exist!");
        }

        File routerFileDir = new File(rootDirFile, "router_mapping");
        if (!routerFileDir.exists()) {
            routerFileDir.mkdir();
        }

        File mappingFile = new File(routerFileDir, "mapping_" + System.currentTimeMillis() + ".json");

        try {
            BufferedWriter out = new BufferedWriter(new FileWriter(mappingFile));
            String jsonStr = destinationJsonArray.toString();
            out.write(jsonStr);
            out.flush();
            out.close();
        } catch (Exception e) {
            throw new RuntimeException("Error while writing json", e);
        }

        System.out.println(TAG + " >>> process finish ...");

        return false;
    }
}
```

### 3、汇总mapping_xxx.json生成RouterMapping.md

 项目 com.watayouxiang.router.gradle.RouterPlugin 编码修改如下：

```
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

        // -------------------- 生成 RouterMapping.md --------------------

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
        println("RouterPlugin >>> 旧的构建产物的自动清理")

        // 容错处理，只处理App module，lib module则不处理
        if (!project.plugins.hasPlugin(AppPlugin)) {
            return
        }

        // 创建 Extension
        project.getExtensions().create("router", RouterExtension)

        // 获取 Extension
        project.afterEvaluate {
            RouterExtension extension = project["router"]
            println("RouterPlugin >>> 用户设置的wikiDir路径：${extension.wikiDir}")

            // 3、在 javac 任务 (compileDebugJavaWithJavac) 后，汇总生成文档
            project.tasks.findAll { task ->
                task.name.startsWith('compile') && task.name.endsWith('JavaWithJavac')
            } each { task ->
                task.doLast {
                    File routerMappingDir = new File(project.rootProject.projectDir, "router_mapping")
                    println("RouterPlugin >>> routerMappingDir：${routerMappingDir.absolutePath}")
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
                    println("RouterPlugin >>> markdownBuilder：${markdownBuilder.toString()}")
                }
            }
        }
    }
}
```

 测试 MD文档 是否生成

```
$ ./gradlew clean -q
$ ./gradlew :app:assembleDebug -q
```

### 4、使用注意事项

```
// --------------------------
// 在 app module 的 build.gradle 中，“router插件” 需要在 “kotlin插件” 之后声明使用
// 
// 因为：需要在 com.imooc.router.gradle.RouterPlugin 中获取 kapt 的 extension
// 所以：“router插件” 需要在 “kotlin插件” 之后声明
// --------------------------

// kotlin 插件
apply plugin: 'kotlin-android'
apply plugin: 'kotlin-kapt'
apply plugin: 'com.watayouxiang.router'

router {
    wikiDir getRootDir().absolutePath
}

// --------------------------
// 在 project module 的 build.gradle 中
//
// 注释掉 router-gradle-plugin，以便 app module 引用的是 buildSrc，而不是 router-gradle-plugin
// --------------------------
dependencies {
    // classpath 'com.watayouxiang.router:router-gradle-plugin:1.0.0'
}
```

## 第五节：运行时功能的实现

### 1、创建router-runtime工程

> 创建 android library 类型的 router-runtime。
>
> 解析路由参数，实现路由跳转，并传参。

1）创建 build.gradle 内容如下：

```
plugins {
    id 'com.android.library'
    id 'kotlin-android'
}

android {
    compileSdk 31

    defaultConfig {
        minSdk 21
        targetSdk 31
        versionCode 1
        versionName "1.0"
    }

    compileOptions {
        sourceCompatibility JavaVersion.VERSION_1_8
        targetCompatibility JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = '1.8'
    }
}

apply from : rootProject.file("maven-publish.gradle")
```

2）router-runtime module 根目录下，创建 gradle.properties，内容如下：

> 用于将 router-runtime module 上传到本地maven库

```
POM_ARTIFACT_ID=router-runtime
```

3）创建 com.watayouxiang.gradle.router.runtime.Router 内容如下：

```
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
```

### 2、应用router-runtime工程

1）app module 应用 router-runtime 库

```
// app module 的 build.gradle 添加如下：
dependencies {
    implementation project(':router-runtime')
}
```

2）创建 com.watayouxiang.gradlerouter.MyApp.kt，用于初始化 Router

```
package com.watayouxiang.gradlerouter

import android.app.Application
import com.watayouxiang.gradle.router.runtime.Router

// app module 的 AndroidManifest.xml 注册 MyApp
class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()

        // 初始化 路由器
        Router.init()
    }
}
```

2）创建 com.watayouxiang.gradlerouter.ProfileActivity.kt，用于测试 Router

```
package com.watayouxiang.gradlerouter

import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.TextView
import com.watayouxiang.router.annotations.Destination

@Destination(url = "router://watayouxiang/profile", description = "个人信息")
class ProfileActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val textView = TextView(this)
        textView.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        textView.setBackgroundColor(Color.WHITE)
        textView.setTextColor(Color.BLACK)
        textView.textSize = 16f
        textView.gravity = Gravity.CENTER

        setContentView(textView)

        val name = intent.getStringExtra("name")
        val message = intent.getStringExtra("message")

        textView.text = "ProfileActivity: name=$name, message = $message"
    }
}
```

创建 com.watayouxiang.gradlerouter.MainActivity.java

```
package com.watayouxiang.gradlerouter;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;

import com.watayouxiang.gradle.router.runtime.Router;
import com.watayouxiang.router.annotations.Destination;

@Destination(url = "router://page-home", description = "应用主页")
public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.tv_text).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Router.INSTANCE.go(MainActivity.this, "router://watayouxiang/profile?name=imooc&message=hello");
            }
        });
    }
}
```



