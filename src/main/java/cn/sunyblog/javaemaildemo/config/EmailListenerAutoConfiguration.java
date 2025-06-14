package cn.sunyblog.javaemaildemo.config;

import cn.sunyblog.javaemaildemo.api.EmailListenerApi;
import cn.sunyblog.javaemaildemo.api.EmailListenerProperties;
import cn.sunyblog.javaemaildemo.mail.*;
import cn.sunyblog.javaemaildemo.config.ThreadPoolConfig;
import cn.sunyblog.javaemaildemo.config.SSLTrustInitializer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import java.util.concurrent.ExecutorService;

/**
 * 邮件监听器自动配置类
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(EmailListenerProperties.class)
@ConditionalOnProperty(prefix = "email.listener", name = "enabled", havingValue = "true", matchIfMissing = true)
@Import({ThreadPoolConfig.class, SSLTrustInitializer.class})
public class EmailListenerAutoConfiguration {

    /**
     * 配置邮件缓存
     */
    @Bean
    @ConditionalOnMissingBean
    public MailCache mailCache() {
        return new MailCache();
    }

    /**
     * 配置邮件内容解析器
     */
    @Bean
    @ConditionalOnMissingBean
    public MailContentParser mailContentParser() {
        return new MailContentParser();
    }

    /**
     * 配置邮件服务器连接器
     */
    @Bean
    @ConditionalOnMissingBean
    public MailServerConnector mailServerConnector(EmailListenerProperties properties) {
        MailServerConnector connector = new MailServerConnector();
        connector.setMailConfig(convertToMailConfig(properties));
        return connector;
    }

    /**
     * 配置邮件处理器
     */
    @Bean
    @ConditionalOnMissingBean
    public MailProcessor mailProcessor(MailCache mailCache, MailContentParser contentParser, 
                                       EmailListenerProperties properties, EmailListenerApi emailListenerApi) {
        MailProcessor processor = new MailProcessor();
        processor.setMailCache(mailCache);
        processor.setContentParser(contentParser);
        processor.setMailConfig(convertToMailConfig(properties));
        processor.setEmailListenerApi(emailListenerApi);
        return processor;
    }

    /**
     * 配置邮件监听器
     */
    @Bean
    @ConditionalOnMissingBean
    public MailListener mailListener(MailServerConnector connector, MailProcessor processor, 
                                     EmailListenerProperties properties, MailCache mailCache,
                                     ExecutorService noticeThreadPool) {
        MailListener listener = new MailListener();
        listener.setMailServerConnector(connector);
        listener.setMailProcessor(processor);
        listener.setMailConfig(convertToMailConfig(properties));
        listener.setMailCache(mailCache);
        listener.setNoticeThreadPool(noticeThreadPool);
        return listener;
    }

    /**
     * 配置邮件发送器
     */
    @Bean
    @ConditionalOnMissingBean
    public MailSender mailSender() {
        return new MailSender();
    }

    /**
     * 配置邮件服务
     */
    @Bean
    @ConditionalOnMissingBean
    public MailService mailService(MailListener mailListener, MailServerConnector mailServerConnector, 
                                   MailProcessor mailProcessor, MailSender mailSender, EmailListenerProperties properties) {
        MailService service = new MailService();
        service.setMailListener(mailListener);
        service.setMailServerConnector(mailServerConnector);
        service.setMailProcessor(mailProcessor);
        service.setMailSender(mailSender);
        service.setAutoStart(properties.getListener().isAutoStart());
        return service;
    }

    /**
     * 默认的邮件监听器API实现
     * 如果用户没有提供自定义实现，则使用此默认实现
     */
    @Bean
    @ConditionalOnMissingBean(EmailListenerApi.class)
    public EmailListenerApi defaultEmailListenerApi() {
        return new EmailListenerApi() {
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
    private MailConfig convertToMailConfig(EmailListenerProperties properties) {
        MailConfig mailConfig = new MailConfig();
        
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
        // 我们不直接设置processExistingUnread，因为MailConfig.Listener中没有这个setter方法
        
        // 日志配置
        mailConfig.getLog().setDebugEnabled(properties.getLog().isDebugEnabled());
        
        // 附件配置
        mailConfig.setAttachmentDir(properties.getAttachment().getSaveDir());
        
        return mailConfig;
    }
}