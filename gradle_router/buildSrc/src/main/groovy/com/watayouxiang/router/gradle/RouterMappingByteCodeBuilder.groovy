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