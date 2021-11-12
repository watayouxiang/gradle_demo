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
