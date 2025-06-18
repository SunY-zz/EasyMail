package cn.sunyblog.easymail.annotation;

import cn.sunyblog.easymail.config.EasyMailAutoConfiguration;
import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

/**
 * 启用EasyMail邮件服务的注解
 * 
 * <p>在Spring Boot主类上添加此注解可以自动配置和启动EasyMail邮件服务。</p>
 * 
 * <p>使用示例：</p>
 * <pre>
 * &#64;SpringBootApplication
 * &#64;EnableEasyMail
 * public class Application {
 *     public static void main(String[] args) {
 *         SpringApplication.run(Application.class, args);
 *     }
 * }
 * </pre>
 * 
 * <p>此注解会自动导入以下配置：</p>
 * <ul>
 *   <li>邮件监听器自动配置</li>
 *   <li>邮件发送服务自动配置</li>
 *   <li>邮件处理器自动配置</li>
 *   <li>线程池配置</li>
 *   <li>SSL信任配置</li>
 * </ul>
 * 
 * @author sunyblog
 * @since 1.0.0
 * @see EasyMailAutoConfiguration
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(EasyMailAutoConfiguration.class)
public @interface EnableEasyMail {
    
    /**
     * 是否自动启动邮件监听服务
     * 
     * @return 默认为true，自动启动邮件监听服务
     */
    boolean autoStart() default true;
    
    /**
     * 是否启用邮件发送服务
     * 
     * @return 默认为true，启用邮件发送服务
     */
    boolean enableSender() default true;
    
    /**
     * 是否启用邮件监听服务
     * 
     * @return 默认为true，启用邮件监听服务
     */
    boolean enableListener() default true;
    
    /**
     * 是否启用注解驱动的邮件处理器
     * 
     * @return 默认为true，启用注解驱动的邮件处理器
     */
    boolean enableProcessor() default true;
    
    /**
     * 扫描邮件处理器的包路径
     * 
     * @return 包路径数组，默认为空，表示扫描所有包
     */
    String[] scanPackages() default {};
    
    /**
     * 配置文件前缀
     * 
     * @return 默认为"mail"，对应application.yml中的mail配置节点
     */
    String configPrefix() default "mail";
}