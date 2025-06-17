package cn.sunyblog.easymail.processor.config;

import cn.sunyblog.easymail.processor.handler.EasyMailContextBuilder;
import cn.sunyblog.easymail.processor.handler.EasyMailHandlerExecutor;
import cn.sunyblog.easymail.processor.handler.EasyMailHandlerRegistry;
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
@EnableConfigurationProperties(AnnotationDrivenEasyMailProcessorProperties.class)
@ConditionalOnProperty(prefix = "annotation-driven-email-processor", name = "enabled", havingValue = "true", matchIfMissing = true)
@Import({EasyMailHandlerRegistry.class, EasyMailHandlerExecutor.class, EasyMailContextBuilder.class})
public class AnnotationDrivenEasyMailProcessorAutoConfiguration {

    /**
     * 注解驱动邮件处理器管理器
     */
    @Bean
    public AnnotationDrivenEasyMailProcessorManager annotationDrivenEmailProcessorManager(
            EasyMailHandlerRegistry handlerRegistry,
            EasyMailHandlerExecutor handlerExecutor,
            EasyMailContextBuilder contextBuilder,
            AnnotationDrivenEasyMailProcessorProperties properties) {

        log.info("初始化注解驱动邮件处理器管理器");

        return new AnnotationDrivenEasyMailProcessorManager(
                handlerRegistry, handlerExecutor, contextBuilder, properties);
    }


}