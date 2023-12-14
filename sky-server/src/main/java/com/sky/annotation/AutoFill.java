package com.sky.annotation;

import com.sky.enumeration.OperationType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 自定义注解，用于表示某个方法需要进行功能字段自动填充处理
 */
@Target(ElementType.METHOD) // 只能加载方法上
@Retention(RetentionPolicy.RUNTIME) // 运行时执行
public @interface AutoFill {
    // 指定当前数据库操作类型: update, insert
    OperationType value(); // 枚举类型，在update和insert操作都要执行自动填充

}
