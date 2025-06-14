package cn.sunyblog.javaemaildemo.processor.annotation;

import org.springframework.stereotype.Component;

import java.lang.annotation.*;

/**
 * 邮件处理器组件注解
 * 用于标记包含邮件处理方法的类
 * 继承自Spring的@Component注解，自动注册为Spring Bean
 * 
 * @author suny
 * @version 1.0
 * @since 2025/06/14
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
public @interface EmailProcessor {
    
    /**
     * 处理器组名称
     * 用于分组管理多个处理器
     * @return 组名称
     */
    String group() default "default";
    
    /**
     * 处理器描述
     * @return 描述信息
     */
    String description() default "";
    
    /**
     * 是否启用该处理器
     * @return 是否启用
     */
    boolean enabled() default true;
    
    /**
     * Spring Bean名称
     * @return Bean名称
     */
    String value() default "";
}