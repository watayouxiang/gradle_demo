# gradle基础集合

## 1、gradle环境配置

```
// 下载 grdle-6.5-all，并解压到 /Users/TaoWang/.gradle/wrapper/dists
// gradle下载地址：https://gradle.org/releases/

// 打开环境变量配置文件：/Users/TaoWang/.bash_profile，添加如下配置信息：
------------------------------------------
# Gradle
GRADE_HOME=/Users/TaoWang/.gradle/wrapper/dists/gradle-6.5-all/2oz4ud9k3tuxjg84bbf55q0tn/gradle-6.5
export GRADE_HOME
export PATH=${PATH}:/Users/TaoWang/.gradle/wrapper/dists/gradle-6.5-all/2oz4ud9k3tuxjg84bbf55q0tn/gradle-6.5/bin 
# Gradle END
------------------------------------------

// 环境变量立即生效
$ source ~/.bash_profile

// 查看所有环境变量
$ echo $PATH

// 检验gradle是否配置成功
$ gradle -version
```

## 2、gradle-wapper命令

```
// 生成gradle-wrapper
$ gradle wrapper

// 清理
$ ./gradlew clean

// 查看所有子工程
$ ./gradlew projects

// 查看所有子任务
$ ./gradlew tasks

// gradle的升级
$ ./gradlew wapper --gradle-version 6.7

// 查看 app module 依赖树
$ ./gradlew :app:dependencies

// 查看 huawei构建变体 的依赖树
$ ./gradlew :app:dependencies --configuration huaweiDebugCompileClasspath
```

## 3、groovy语法

> Android Studio > Tools > Groovy Console 即可以开始编码

闭包

```groovy
def c = { println("hello Closure") }
c()

// 有一个参数的时候，参数可以忽略，默认名称为 it
def c2 = { println("c2: it = $it") }
c2("watayouxiang")

def c4 = { name1, name2 ->
    println("c4: name1 = $name1")
    println("c4: name2 = $name2")
}
c4("test_1", "test_2")

def list = [1, 3, 5, 7, 9]
list.each { println("item = $it") }
```

DSL

```groovy
// 希望输出
// 
// Android { 
// 		compileSdkVersion = 27, 
//		defaultConfig = DefaultConfig { 
//				versionName = 1.0 
//		} 
// }

class DefaultConfig {
    private String versionName

    def versionName(String versionName) {
        this.versionName = versionName
    }

    @Override
    String toString() {
        return "DefaultConfig{ versionName = $versionName }"
    }
}

class Android {
    private int compileSdkVersion
    private DefaultConfig defaultConfig

    Android() {
        this.defaultConfig = new DefaultConfig()
    }

    def compileSdkVersion(int compileSdkVersion) {
        this.compileSdkVersion = compileSdkVersion
    }

    def defaultConfig(Closure closure) {
        // 将闭包和具体对象关联起来
        closure.setDelegate(defaultConfig)
        closure.call()
    }

    @Override
    String toString() {
        return "Android { compileSdkVersion = $compileSdkVersion, " +
                "defaultConfig = $defaultConfig }"
    }
}

// 实现自定义 DSL
def android = {
    compileSdkVersion 27
    defaultConfig {
        versionName "1.0"
    }
}

Android a = new Android()
// 将闭包与具体对象关联起来
android.delegate = a
android.call()

println("android = $a")
```

## 4、gradle插件分类

> 插件分为 “二进制插件” 和 “脚本插件”

二进制插件

```groovy
// --------------------------------------------
// 在 project 的 build.gradle 中
// --------------------------------------------

// 1、引用插件
buildscript { 
    // 插件所在的仓库
    repositories {
        google()
        jcenter()
    }

    // gradle 插件
    dependencies {
        // 声明插件的 ID 和 版本号
        classpath 'com.android.tools.build:gradle:4.1.2'
    }
}

// --------------------------------------------
// 在 module 的 build.gradle 中
// --------------------------------------------

// 2、应用插件
apply plugin: 'com.android.application'

// 3、配置插件
android {
    compileSdkVersion 30
    buildToolsVersion "30.0.1"

    defaultConfig {
        applicationId "com.watayouxiang.androiddemo"
        minSdkVersion 16
        targetSdkVersion 30
        versionCode 1
        versionName "1.0"
    }

    buildTypes {
        release {
            minifyEnabled false
            proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
        }
    }
}
```

脚本插件

```groovy
// 1、编写脚本插件
// 在工程根目录下，新建 other.gradle，内容如下
println("我是脚本插件的代码")

// 2、应用脚本插件
// 在 module 的 build.gradle 中应用，内容如下
apply from: project.rootProject.file("other.gradle")

// 3、运行插件
$ ./gradlew clean -q
```

## 5、gradle构建

### 1）gradle构建的脚本基础

- setting.gradle
  - 声明当前project包含了哪些module
- build.grade
  - project 下的 build.gradle
    - 所有子工程都可以共用的配置
  - module 下的 build.gradle
    - 针对当前子工程的构建行为
- grade.properties
  - 配置开关型参数的文件

### 2）gradle构建的生命周期

**1）初始化阶段：**收集本次参加构建的所有子工程，创建一个项目的层次结构，为每一个项目创建一个project实例

```groovy
// 在 setting.gradle 文件添加如下代码
// 添加 构建的生命周期 监听
gradle.addBuildListener(new BuildAdapter(){
    @Override
    void settingsEvaluated(Settings settings) {
        super.settingsEvaluated(settings)
        println("project 初始化阶段完成")
    }

    @Override
    void projectsEvaluated(Gradle gradle) {
        super.projectsEvaluated(gradle)
        println("project 配置阶段完成")
    }

    @Override
    void buildFinished(BuildResult result) {
        super.buildFinished(result)
        println("project 构建结束")
    }
})

// terminal中执行
$./gradlew clean -q

// 可以得出结论：setting.gradle 是在 初始化阶段之前 执行的
```

**2）配置阶段：**执行各个module下的build.gradle脚本，来完成project对象的配置。并且根据项目自己的配置去构建出一个项目依赖图，以便在下一个执行阶段去执行

```groovy
// 在project的build.gradle中写入
println("我是project的build.gradle")

// 在module的build.gradle中写入
println("我是module的build.gradle")

// terminal中执行
$ ./gradlew clean -q

// 可以得出结论：“project的build.gradle” 和 “module的build.gradle” 是在 初始化阶段之后，配置阶段之前 执行的
```

**3）执行阶段：**把配置阶段生成的一个任务依赖图，依次去执行

```groovy
// 在app module的build.gradle中写入
task testTask() {
	println("我是app module中的任务")
}

// terminal中执行
$ ./gradlew :app:testTask -q

// 可以得出结论：task是在 配置阶段之后，构建结束之前 执行的
```

### 3）gradle几个主要角色

**1）初始化阶段  rootProject：**在初始化阶段之前，就能拿到 rootProject 对象了

```groovy
// 在setting.gradle写入
println("我的项目路径：${rootProject.projectDir}")

$ ./gradlew clean -q
```

**2）配置阶段 project：**在配置阶段完成后，能拿到 project 对象

```groovy
// 在 setting.gradle 中使用 project对象
gradle.addBuildListener(new BuildAdapter(){
    @Override
    void projectsEvaluated(Gradle gradle) {
        super.projectsEvaluated(gradle)
        println("project 配置阶段完成")
        
        gradle.rootProject.childProjects.each {	name, proj ->
        	println("module名称是 $name, 路径是 ${proj.getProjectDir()}")
        }
    }
})

// 执行如下命令
$ ./gradlew clean -q
```

```groovy
// 在 app module 的 build.gradle 中使用 project对象
println("我是app module，我的路径是：${project.projectDir}")

// 执行如下命令
$ ./gradlew clean -q
```

**3）执行阶段 task：**gradle最小的执行单元，一个project可以有多个task，task之间可以相互依赖的，靠相互依赖的关系来串成一个有向无环图

执行任务

```groovy
// 在app module 的 build.gradle中写入
task testTask(){
	doLast{
		println("我是 testTask 任务")
	}
}

$ ./gradlew :app:testTask -q
```

任务的依赖

```groovy
// 在app module 的 build.gradle中写入
task testTask(){
    doLast{
        println("我是 testTask 任务")
    }
}

task test2(){
    dependsOn testTask
    doLast{
        println("我是 test2 任务")
    }
}

$ ./gradlew :app:test2 -q

// 结论：test2 依赖于 testTask
```

### 4）监听构建生命周期回调

在 setting.gradle 配置如下代码监听构建生命周期回调

Terminal中输入 `./gradlew clean`

```groovy
gradle.buildStarted {
    println "项目构建开始..."
}

// 1、初始化阶段：执行项目根目录下的 setting.gradle 文件，分析哪些 project 参与本次构建
gradle.projectsLoaded {
    println "从 setting.gradle 解析完成参与构建的所有项目"
}

// 2、配置阶段：加载所有参与本次构建项目下的 build.gradle 文件，会将 build.gradle 文件解析
//    并实例化为一个 Gradle 的 Project 对象，然后分析 Project 之间的依赖关系，分析 Project 下的
//    Task 之间的依赖关系，生成有向无环拓扑结构图 TaskGraph
gradle.beforeProject { proj ->
    println "${proj.name} build.gradle 解析之前"
}
gradle.afterProject { proj ->
    println "${proj.name} build.gradle 解析完成"
}
gradle.projectsEvaluated {
    println "所有项目的 build.gradle 解析配置完成"
}

// 3、执行阶段：这是 Task 真正被执行的阶段，Gradle 会根据依赖关系决定哪些 Task 需要被执行，以及执行的先后顺序。
//    Task 是 Gradle 中的最小执行单元，我们所有的构建、编译、打包、debug 都是执行了一个或者多个 Task，
//    一个 Project 可以有多个 Task，Task 之间可以互相依赖。
gradle.getTaskGraph().addTaskExecutionListener(new TaskExecutionListener() {
    @Override
    void beforeExecute(Task task) {
        println("任务执行：start" + task.name)
    }

    @Override
    void afterExecute(Task task, TaskState taskState) {
        println("任务执行：end" + task.name)
    }
})

gradle.buildFinished {
    println "项目构建结束..."
}
```

### 5）打印构建阶段task依赖关系及输出输入

- 在根 project 的 build.gradle 配置如下

```groovy
// 打印构建阶段task依赖关系及输出输入
afterEvaluate { project ->
    // 收集所有project的task集合
    Map<Project, Set<Task>> allTasks = project.getAllTasks(true)
    // 遍历每一个project下的task集合
    allTasks.entrySet().each { projTask ->
        projTask.value.each { task ->
            // 输出task的名称 和dependOn依赖
            System.out.println(task.getName())
            for (Object o : task.getDependsOn()) {
                System.out.println("dependOn-->" + o.toString())
            }

            // 打印每个任务的输入，输出
            for (File file : task.getInputs().getFiles().getFiles()) {
                System.out.println("input-->" + file.getAbsolutePath())
            }
            for (File file : task.getOutputs().getFiles().getFiles()) {
                System.out.println("output-->" + file.getAbsolutePath())
            }

            System.out.println("----------------------------------------")
        }
    }
}

// 相当于上面写法
this.project.afterEvaluate {
}
```

### 6）Project工程树

- RootProject (AsProj)
  - SubProject (biz_home)
  - SubProject (biz_detail)
  - SubProject (service_provider)
    - SubProject (app_module)
    - SubProject (service_module)
    - SubProject (core_module)

> 1、此时 service_provider 称为下面三个 project 的 parentProject
>
> 2、RootProject 永远指的是 ASPProj

## 6、应用构建实用技能

### - 版本号的统一管理

project 的 build.gradle 

```groovy
ext {
    MIN_SDK_VERSION = 16
    TARGET_SDK_VERSION = 30

    APP_COMPAT = 'androidx.appcompat:appcompat:1.2.0'
    MATERIAL = 'com.google.android.material:material:1.2.1'
    CONSTRAINT_LAYOUT = 'androidx.constraintlayout:constraintlayout:2.0.4'
}
```

module 的 build.gradle 

```groovy
android {
    defaultConfig {
        applicationId "com.watayouxiang.androiddemo"
        minSdkVersion MIN_SDK_VERSION
        targetSdkVersion TARGET_SDK_VERSION
    }
}

dependencies {
    implementation APP_COMPAT
    implementation MATERIAL
    implementation CONSTRAINT_LAYOUT
}
```

### - 维护敏感信息

- local.properties 添加如下

```properties
KEY_ALIAS=test_wt666
KEY_PASSWORD=test_wt666
STORE_PASSWORD=test_wt666
```

- app module 的 build.gradle

```groovy
////////////////////////////////////////// 获取 local.properties 参数

Properties properties = new Properties()
properties.load(project.rootProject.file("local.properties").newDataInputStream())

def KEY_ALIAS = properties.getProperty('KEY_ALIAS')
def KEY_PASSWORD = properties.getProperty('KEY_PASSWORD')
def STORE_PASSWORD = properties.getProperty('STORE_PASSWORD')

println("KEY_ALIAS = $KEY_ALIAS, KEY_PASSWORD = $KEY_PASSWORD, STORE_PASSWORD = $STORE_PASSWORD")

////////////////////////////////////////// 获取 local.properties 参数

android {
    signingConfigs {
        release {
            keyAlias KEY_ALIAS
            keyPassword KEY_PASSWORD
            storeFile file('../wt_apkkey/test_wt666.key')
            storePassword STORE_PASSWORD
        }
    }
}
```

### - 限制依赖库的使用

- 在 app module 的 build.gradle 添加如下

```groovy
// 限制依赖库的使用
// $ ./gradlew :app:assembleDebug
configurations.all {
    resolutionStrategy.eachDependency { detail ->
        String dependency = detail.requested.toString()
        if (dependency.contains("com.blankj:utilcodex")) {
            throw new RuntimeException("不允许使用 $dependency")
        }
    }
}
```

### - buildTypes那些事

- app module 的 build.gradle

```groovy
android {
    buildTypes {
        debug {
            // 往 BuildConfig.java 中添加一个字段
            // 生成目录：build/generated/source/buildConfig/...BuildConfig.java
            buildConfigField("String", "BUILD_TIME", "\"${System.currentTimeMillis()}\"")
            
            // 生成目录：build/generated/res/resValues/...gradleResValues.xml
            resValue("string", "greeting", "hello!")
        }
        release {
            // 往 BuildConfig.java 中添加一个字段
            // 生成目录：build/generated/source/buildConfig/...BuildConfig.java
            // 这里之所以也添加，目的是预防编译报错
            buildConfigField("String", "BUILD_TIME", "\"0\"")
        }
    }
}
```

### - BuildVariants那些事

app module 的 build.gradle 中添加

```
// 产品维度
flavorDimensions "channel"
// 产品风味
productFlavors {
    // 开发专用的构建变体
    dev {
        minSdkVersion 21
    }
    huawei {
        manifestPlaceholders = [WT_CHANNEL_VALUE: "huawei"]
    }
    xiaomi {
        manifestPlaceholders = [WT_CHANNEL_VALUE: "baidu"]
    }
}

applicationVariants.all { variant ->
    variant.outputs.each { output ->
        if (variant.buildType.name.equals('release')) {
            // ${variant.productFlavors[0].name} 渠道名称
            // ${defaultConfig.versionName} 版本号
            def fileName = "wtApp_${variant.productFlavors[0].name}_${defaultConfig.versionName}.apk"
            output.outputFileName = fileName
        }
    }
}
```

app module 的 AndroidManifest.xml 添加：

```
<application>
    <meta-data
        android:name="WT_CHANNEL"
        android:value="${WT_CHANNEL_VALUE}" />
</application>
```

app module 的 代码中获取渠道信息：

```
// 测试：获取渠道信息
try {
    ApplicationInfo appInfo = getPackageManager().getApplicationInfo(getPackageName(), PackageManager.GET_META_DATA);
    String wtChannel = appInfo.metaData.getString("WT_CHANNEL");
    tv_text.setText("wtChannel = " + wtChannel);
} catch (Exception ignored) {
}
```

### - 分析构建性能

```groovy
// 分析构建任务的耗时
// --offline 开始离线模式，避免网络因素干扰
// --rerun-tasks 不使用任何缓存
// --profile 生成一份性能报告
// 性能报告所在位置：build/reports/profile/...xxx.html
$ ./gradlew app:assembleHuaweiDebug --offline --rerun-tasks --profile
```

### - 分析app页面的布局树

```groovy
// 进入到uiautomatorviewer目录
$ cd /Users/TaoWang/Library/Android/sdk/tools/bin

// 运行 uiautomatorviewer
$ ./uiautomatorviewer

// 点击 Device ScreenShot 按钮
```

### - 代码反编译

- dex2jar 工具
- Java Decompiler
