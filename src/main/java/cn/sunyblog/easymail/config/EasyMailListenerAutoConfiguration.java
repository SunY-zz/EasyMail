package cn.sunyblog.easymail.config;

import cn.sunyblog.easymail.api.EasyMailListenerApi;
import cn.sunyblog.easymail.mail.*;
import cn.sunyblog.easymail.send.EasyMailSender;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;

import java.util.concurrent.ExecutorService;

/**
 * 邮件监听器自动配置类
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(EasyMailListenerProperties.class)
@ConditionalOnProperty(prefix = "email.listener", name = "enabled", havingValue = "true", matchIfMissing = true)
@Import({EasyMailThreadPoolConfig.class, EasyMailSSLTrustInitializer.class})
@Order(0) // 确保优先级高于MailConfigCompatibilityAutoConfiguration
public class EasyMailListenerAutoConfiguration {

    /**
     * 配置邮件缓存
     */
    @Bean
    @ConditionalOnMissingBean
    public EasyMailCache mailCache() {
        return new EasyMailCache();
    }

    /**
     * 配置邮件内容解析器
     */
    @Bean
    @ConditionalOnMissingBean
    public EasyMailContentParser mailContentParser() {
        return new EasyMailContentParser();
    }

    /**
     * 配置邮件服务器连接器
     */
    @Bean
    @ConditionalOnMissingBean
    public EasyMailServerConnector mailServerConnector(EasyMailListenerProperties properties) {
        EasyMailServerConnector connector = new EasyMailServerConnector();
        connector.setMailConfig(convertToMailConfig(properties));
        return connector;
    }

    /**
     * 配置邮件处理器
     */
    @Bean
    @ConditionalOnMissingBean
    public EasyMailProcessor mailProcessor(EasyMailCache easyMailCache, EasyMailContentParser contentParser,
                                           EasyMailListenerProperties properties, EasyMailListenerApi easyMailListenerApi) {
        EasyMailProcessor processor = new EasyMailProcessor();
        processor.setEasyMailCache(easyMailCache);
        processor.setContentParser(contentParser);
        processor.setMailConfig(convertToMailConfig(properties));
        processor.setEasyMailListenerApi(easyMailListenerApi);
        return processor;
    }

    /**
     * 配置邮件监听器
     */
    @Bean
    @ConditionalOnMissingBean
    public EasyMailListener mailListener(EasyMailServerConnector connector, EasyMailProcessor processor,
                                         EasyMailListenerProperties properties, EasyMailCache easyMailCache,
                                         ExecutorService noticeThreadPool) {
        EasyMailListener listener = new EasyMailListener();
        listener.setEasyMailServerConnector(connector);
        listener.setEasyMailProcessor(processor);
        listener.setMailConfig(convertToMailConfig(properties));
        listener.setEasyMailCache(easyMailCache);
        listener.setNoticeThreadPool(noticeThreadPool);
        return listener;
    }

    /**
     * 配置邮件发送器
     */
    @Bean
    @ConditionalOnMissingBean
    public EasyMailSender mailSender() {
        return new EasyMailSender();
    }

    /**
     * 配置邮件服务
     */
    @Bean
    @ConditionalOnMissingBean
    public EasyMailService mailService(EasyMailListener easyMailListener, EasyMailServerConnector easyMailServerConnector,
                                       EasyMailProcessor easyMailProcessor, EasyMailSender easyMailSender, EasyMailListenerProperties properties) {
        EasyMailService service = new EasyMailService();
        service.setEasyMailListener(easyMailListener);
        service.setEasyMailServerConnector(easyMailServerConnector);
        service.setEasyMailProcessor(easyMailProcessor);
        service.setEasyMailSender(easyMailSender);
        service.setAutoStart(properties.getListener().isAutoStart());
        return service;
    }

    /**
     * 配置MailConfig Bean
     * 将EmailListenerProperties转换为MailConfig并注册为Bean
     * 只有在没有其他MailConfig bean且使用email.listener配置时才创建
     */
    @Bean("mailConfig")
    @ConditionalOnMissingBean(name = "mailConfig")
    @ConditionalOnProperty(prefix = "email.listener", name = "server.host")
    public EasyMailConfig mailConfig(EasyMailListenerProperties properties) {
        log.info("使用新配置格式加载 MailConfig: email.listener.*");
        EasyMailConfig config = convertToMailConfig(properties);
        log.info("MailConfig配置: server={}, port={}, protocol={}, username={}",
                config.getServer(), config.getPort(), config.getProtocol(), config.getUsername());
        return config;
    }

    /**
     * 默认的邮件监听器API实现
     * 如果用户没有提供自定义实现，则使用此默认实现
     */
    @Bean
    @ConditionalOnMissingBean(EasyMailListenerApi.class)
    public EasyMailListenerApi defaultEmailListenerApi() {
        return new EasyMailListenerApi() {
            @Override
            public boolean processEmail(javax.mail.Message message, String content, String subject, String from) {
                log.info("收到邮件: 主题={}, 发件人={}", subject, from);
                log.debug("邮件内容: {}", content);
                return true;
            }

            @Override
            public String getProcessorName() {
                return "DefaultEmailProcessor";
            }
        };
    }

    /**
     * 将EmailListenerProperties转换为MailConfig
     */
    private EasyMailConfig convertToMailConfig(EasyMailListenerProperties properties) {
        EasyMailConfig mailConfig = new EasyMailConfig();

        // 服务器配置
        mailConfig.setServer(properties.getServer().getHost());
        mailConfig.setPort(properties.getServer().getPort());
        mailConfig.setProtocol(properties.getServer().getProtocol());
        mailConfig.setUsername(properties.getServer().getUsername());
        mailConfig.setPassword(properties.getServer().getPassword());
        // 我们不直接设置folder，因为MailConfig中没有这个setter方法

        // 连接配置
        mailConfig.getConnection().setTimeout(properties.getConnection().getTimeout());
        mailConfig.getConnection().setReadTimeout(properties.getConnection().getReadTimeout());
        mailConfig.getConnection().setWriteTimeout(properties.getConnection().getWriteTimeout());

        // 监控配置
        mailConfig.getMonitor().setIdleTimeout(properties.getMonitor().getIdleTimeout());
        mailConfig.getMonitor().setKeepAliveInterval(properties.getMonitor().getKeepAliveInterval());
        mailConfig.getMonitor().setReconnectDelay(properties.getMonitor().getReconnectDelay());
        mailConfig.getMonitor().setShortDelay(properties.getMonitor().getShortDelay());
        mailConfig.getMonitor().setLongDelay(properties.getMonitor().getLongDelay());
        mailConfig.getMonitor().setTaskTimeout(properties.getMonitor().getTaskTimeout());

        // 监听配置
        mailConfig.getListener().setMaxRetries(properties.getListener().getMaxRetries());
        
        mailConfig.getListener().setStartupProcessStrategy(properties.getListener().getStartupProcessStrategy());

        // 日志配置
        mailConfig.getLog().setDebugEnabled(properties.getLog().isDebugEnabled());

        // 附件配置
        mailConfig.setAttachmentDir(properties.getAttachment().getSaveDir());

        return mailConfig;
    }
    

}