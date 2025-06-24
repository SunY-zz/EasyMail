package cn.sunyblog.easymail.config;

import cn.sunyblog.easymail.api.EasyMailSenderService;
import cn.sunyblog.easymail.send.event.EasyMailSendEventListener;
import cn.sunyblog.easymail.send.EasyMailSenderServiceImpl;
import cn.sunyblog.easymail.send.template.EasyMailSendTemplateManager;
import cn.sunyblog.easymail.send.monitor.EasyMailSendMonitor;
import cn.sunyblog.easymail.send.strategy.BatchEasyMailSendStrategy;
import cn.sunyblog.easymail.send.strategy.DefaultEasyMailSendStrategy;
import cn.sunyblog.easymail.send.strategy.EasyMailSendStrategyManager;
import cn.sunyblog.easymail.send.strategy.HighPriorityEasyMailSendStrategy;
import cn.sunyblog.easymail.send.schedule.EasyMailScheduleManager;
import cn.sunyblog.easymail.send.schedule.EasyMailTaskScheduler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

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
@EnableScheduling
@EnableConfigurationProperties({EasyMailSmtpConfig.class, EasyMailRetryConfig.class})
@ConditionalOnProperty(prefix = "email.sender", name = "enabled", havingValue = "true", matchIfMissing = true)
@Import({EasyMailThreadPoolConfig.class})
public class EasyMailSenderAutoConfiguration {

    @Resource
    private EasyMailSmtpConfig easyMailSmtpConfig;

    @Resource
    private EasyMailRetryConfig easyMailRetryConfig;

    @PostConstruct
    public void init() {
        log.info("EmailSender 自动配置已启用");
        log.info("SMTP配置: host={}, port={}, username={}",
                easyMailSmtpConfig.getServer(), easyMailSmtpConfig.getPort(), easyMailSmtpConfig.getUsername());
        log.info("重试配置: maxRetries={}, retryDelay={}",
                easyMailRetryConfig.getMaxRetries(), easyMailRetryConfig.getInitialDelayMs());
    }

    /**
     * 邮件模板管理器
     */
    @Bean
    @ConditionalOnMissingBean
    public EasyMailSendTemplateManager easyMailSendTemplateManager() {
        EasyMailSendTemplateManager manager = new EasyMailSendTemplateManager();
        log.info("EmailTemplateManager 已创建");
        return manager;
    }

    /**
     * 邮件发送监控器
     */
    @Bean
    @ConditionalOnMissingBean
    public EasyMailSendMonitor easyMailSendMonitor() {
        EasyMailSendMonitor monitor = new EasyMailSendMonitor();
        log.info("EmailSendMonitor 已创建");
        return monitor;
    }

    /**
     * 邮件发送事件监听器
     */
    @Bean
    @ConditionalOnMissingBean
    public EasyMailSendEventListener easyMailSendEventListener() {
        return new EasyMailSendEventListener();
    }

    /**
     * 默认邮件发送策略
     */
    @Bean
    @ConditionalOnMissingBean
    public DefaultEasyMailSendStrategy defaultEasyMailSendStrategy() {
        DefaultEasyMailSendStrategy strategy = new DefaultEasyMailSendStrategy();
        log.info("DefaultEmailSendStrategy 已创建");
        return strategy;
    }

    /**
     * 批量邮件发送策略
     */
    @Bean
    @ConditionalOnMissingBean
    public BatchEasyMailSendStrategy batchEasyMailSendStrategy() {
        BatchEasyMailSendStrategy strategy = new BatchEasyMailSendStrategy();
        log.info("BatchEmailSendStrategy 已创建");
        return strategy;
    }

    /**
     * 高优先级邮件发送策略
     */
    @Bean
    @ConditionalOnMissingBean
    public HighPriorityEasyMailSendStrategy highPriorityEasyMailSendStrategy() {
        HighPriorityEasyMailSendStrategy strategy = new HighPriorityEasyMailSendStrategy();
        log.info("HighPriorityEmailSendStrategy 已创建");
        return strategy;
    }

    /**
     * 邮件发送策略管理器
     */
    @Bean
    @ConditionalOnMissingBean
    public EasyMailSendStrategyManager easyMailSendStrategyManager() {
        return new EasyMailSendStrategyManager();
    }

    /**
     * 邮件发送服务实现
     */
    @Bean(name = {"emailSenderService", "easyMailSenderService", "mailSenderService"})
    @ConditionalOnMissingBean
    public EasyMailSenderService emailSenderService() {
        EasyMailSenderServiceImpl service = new EasyMailSenderServiceImpl();
        log.info("EmailSenderService 已创建");
        return service;
    }

    /**
     * 定时任务调度器
     */
    @Bean
    @Primary
    @ConditionalOnMissingBean
    public ThreadPoolTaskScheduler threadPoolTaskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(5);
        scheduler.setThreadNamePrefix("EasyMail-Schedule-");
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.setAwaitTerminationSeconds(60);
        scheduler.initialize();
        log.info("EasyMail定时任务调度器已创建，线程池大小: 5");
        return scheduler;
    }

    /**
     * 邮件任务调度器
     */
    @Bean
    @ConditionalOnMissingBean
    public EasyMailTaskScheduler easyMailTaskScheduler(ThreadPoolTaskScheduler threadPoolTaskScheduler) {
        EasyMailTaskScheduler taskScheduler = new EasyMailTaskScheduler();
        taskScheduler.setTaskScheduler(threadPoolTaskScheduler);
        log.info("EasyMailTaskScheduler 已创建");
        return taskScheduler;
    }

    /**
     * 邮件定时任务管理器
     */
    @Bean
    @ConditionalOnMissingBean
    public EasyMailScheduleManager easyMailScheduleManager() {
        log.info("EasyMailScheduleManager 已创建");
        return new EasyMailScheduleManager();
    }
}