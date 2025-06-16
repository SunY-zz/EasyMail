package cn.sunyblog.javaemaildemo.processor.annotation;

import java.lang.annotation.*;

/**
 * 邮件处理器注解
 * 用于标记邮件处理方法，支持基于主题、发件人、标签等条件的邮件路由
 * 
 * @author suny
 * @version 1.0
 * @since 2025/06/14
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface EmailHandler {
    
    /**
     * 邮件主题匹配模式
     * 支持精确匹配和包含匹配
     * @return 主题匹配模式数组
     */
    String[] subject() default {};
    
    /**
     * 主题匹配类型
     * @return 匹配类型
     */
    MatchType subjectMatchType() default MatchType.CONTAINS;
    
    /**
     * 发件人匹配模式
     * 支持邮箱地址或显示名称匹配
     * @return 发件人匹配模式数组
     */
    String[] from() default {};
    
    /**
     * 发件人匹配类型
     * @return 匹配类型
     */
    MatchType fromMatchType() default MatchType.CONTAINS;
    
    /**
     * 邮件标签匹配
     * 用于基于邮件标签的路由
     * @return 标签数组
     */
    String[] tags() default {};
    
    /**
     * 处理器优先级
     * 数值越小优先级越高
     * @return 优先级
     */
    int priority() default 100;
    
    /**
     * 处理器名称
     * 用于标识和日志记录
     * @return 处理器名称
     */
    String name() default "";
    
    /**
     * 是否异步处理
     * @return 是否异步
     */
    boolean async() default true;
    
    /**
     * 处理器描述
     * @return 描述信息
     */
    String description() default "";
    
    /**
     * 匹配类型枚举
     */
    enum MatchType {
        /** 精确匹配 */
        EXACT,
        /** 包含匹配 */
        CONTAINS,
        /** 正则表达式匹配 */
        REGEX,
        /** 前缀匹配 */
        PREFIX,
        /** 后缀匹配 */
        SUFFIX
    }
}