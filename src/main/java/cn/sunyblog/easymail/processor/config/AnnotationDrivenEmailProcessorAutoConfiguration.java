package cn.sunyblog.easymail.processor.config;

import cn.sunyblog.easymail.processor.handler.EmailContextBuilder;
import cn.sunyblog.easymail.processor.handler.EmailHandlerExecutor;
import cn.sunyblog.easymail.processor.handler.EmailHandlerRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * 注解驱动邮件处理器自动配置
 *
 * @author suny
 * @version 1.0
 * @date 2025/06/14
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(AnnotationDrivenEmailProcessorProperties.class)
@ConditionalOnProperty(prefix = "annotation-driven-email-processor", name = "enabled", havingValue = "true", matchIfMissing = true)
@Import({EmailHandlerRegistry.class, EmailHandlerExecutor.class, EmailContextBuilder.class})
public class AnnotationDrivenEmailProcessorAutoConfiguration {

    /**
     * 注解驱动邮件处理器管理器
     */
    @Bean
    public AnnotationDrivenEmailProcessorManager annotationDrivenEmailProcessorManager(
            EmailHandlerRegistry handlerRegistry,
            EmailHandlerExecutor handlerExecutor,
            EmailContextBuilder contextBuilder,
            AnnotationDrivenEmailProcessorProperties properties) {

        log.info("初始化注解驱动邮件处理器管理器");

        return new AnnotationDrivenEmailProcessorManager(
                handlerRegistry, handlerExecutor, contextBuilder, properties);
    }


}