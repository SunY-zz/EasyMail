package cn.sunyblog.easymail.config;

import cn.sunyblog.easymail.api.EmailSenderService;
import cn.sunyblog.easymail.send.event.EmailSendEventListener;
import cn.sunyblog.easymail.send.EmailSenderServiceImpl;
import cn.sunyblog.easymail.send.template.EmailTemplateManager;
import cn.sunyblog.easymail.send.monitor.EmailSendMonitor;
import cn.sunyblog.easymail.send.strategy.BatchEmailSendStrategy;
import cn.sunyblog.easymail.send.strategy.DefaultEmailSendStrategy;
import cn.sunyblog.easymail.send.strategy.EmailSendStrategyManager;
import cn.sunyblog.easymail.send.strategy.HighPriorityEmailSendStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

/**
 * 邮件发送服务自动配置类
 * 自动配置所有邮件发送相关的组件
 *
 * @author sunyblog
 * @since 1.0.0
 */
@Slf4j
@Configuration
@EnableConfigurationProperties({SmtpConfig.class, RetryConfig.class})
@ConditionalOnProperty(prefix = "email.sender", name = "enabled", havingValue = "true", matchIfMissing = true)
@Import({ThreadPoolConfig.class})
public class EmailSenderAutoConfiguration {

    @Resource
    private SmtpConfig smtpConfig;

    @Resource
    private RetryConfig retryConfig;

    @Resource
    private ApplicationEventPublisher eventPublisher;

    @PostConstruct
    public void init() {
        log.info("EmailSender 自动配置已启用");
        log.info("SMTP配置: host={}, port={}, username={}",
                smtpConfig.getServer(), smtpConfig.getPort(), smtpConfig.getUsername());
        log.info("重试配置: maxRetries={}, retryDelay={}",
                retryConfig.getMaxRetries(), retryConfig.getInitialDelayMs());
    }

    /**
     * 邮件模板管理器
     */
    @Bean
    @ConditionalOnMissingBean
    public EmailTemplateManager emailTemplateManager() {
        EmailTemplateManager manager = new EmailTemplateManager();
        log.info("EmailTemplateManager 已创建");
        return manager;
    }

    /**
     * 邮件发送监控器
     */
    @Bean
    @ConditionalOnMissingBean
    public EmailSendMonitor emailSendMonitor() {
        EmailSendMonitor monitor = new EmailSendMonitor();
        log.info("EmailSendMonitor 已创建");
        return monitor;
    }

    /**
     * 邮件发送事件监听器
     */
    @Bean
    @ConditionalOnMissingBean
    public EmailSendEventListener emailSendEventListener() {
        return new EmailSendEventListener();
    }

    /**
     * 默认邮件发送策略
     */
    @Bean
    @ConditionalOnMissingBean(name = "defaultEmailSendStrategy")
    public DefaultEmailSendStrategy defaultEmailSendStrategy() {
        DefaultEmailSendStrategy strategy = new DefaultEmailSendStrategy();
        log.info("DefaultEmailSendStrategy 已创建");
        return strategy;
    }

    /**
     * 批量邮件发送策略
     */
    @Bean
    @ConditionalOnMissingBean(name = "batchEmailSendStrategy")
    public BatchEmailSendStrategy batchEmailSendStrategy() {
        BatchEmailSendStrategy strategy = new BatchEmailSendStrategy();
        log.info("BatchEmailSendStrategy 已创建");
        return strategy;
    }

    /**
     * 高优先级邮件发送策略
     */
    @Bean
    @ConditionalOnMissingBean(name = "highPriorityEmailSendStrategy")
    public HighPriorityEmailSendStrategy highPriorityEmailSendStrategy() {
        HighPriorityEmailSendStrategy strategy = new HighPriorityEmailSendStrategy();
        log.info("HighPriorityEmailSendStrategy 已创建");
        return strategy;
    }

    /**
     * 邮件发送策略管理器
     */
    @Bean
    @ConditionalOnMissingBean
    public EmailSendStrategyManager emailSendStrategyManager() {
        return new EmailSendStrategyManager();
    }

    /**
     * 邮件发送服务实现
     */
    @Bean
    @ConditionalOnMissingBean
    public EmailSenderService emailSenderService() {
        EmailSenderServiceImpl service = new EmailSenderServiceImpl();
        log.info("EmailSenderService 已创建");
        return service;
    }
}