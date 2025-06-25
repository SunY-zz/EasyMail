package cn.sunyblog.easymail.config;

import cn.sunyblog.easymail.api.EasyMailListenerApi;
import cn.sunyblog.easymail.mail.*;
import cn.sunyblog.easymail.send.EasyMailSender;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.Order;

import javax.annotation.Resource;
import java.util.concurrent.ExecutorService;

/**
 * 邮件监听器自动配置类
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(EasyMailImapConfig.class)
@ConditionalOnProperty(prefix = "mail.imap", name = "server", matchIfMissing = false)
@Import({EasyMailThreadPoolConfig.class, EasyMailSSLTrustInitializer.class})
@Order(0) // 确保优先级高于MailConfigCompatibilityAutoConfiguration
public class EasyMailListenerAutoConfiguration {

    @Autowired
    private EasyMailImapConfig easyMailImapConfig;

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
    public EasyMailServerConnector mailServerConnector() {
        EasyMailServerConnector connector = new EasyMailServerConnector();
        connector.setMailConfig(easyMailImapConfig);
        return connector;
    }

    /**
     * 配置邮件处理器
     */
    @Bean
    @ConditionalOnMissingBean
    public EasyMailProcessor mailProcessor(EasyMailCache easyMailCache, EasyMailContentParser contentParser,
                                           EasyMailListenerApi easyMailListenerApi) {
        EasyMailProcessor processor = new EasyMailProcessor();
        processor.setEasyMailCache(easyMailCache);
        processor.setContentParser(contentParser);
        processor.setEasyMailImapConfig(easyMailImapConfig);
        processor.setEasyMailListenerApi(easyMailListenerApi);
        return processor;
    }

    /**
     * 配置邮件监听器
     */
    @Bean
    @ConditionalOnMissingBean
    public EasyMailListener mailListener(EasyMailServerConnector connector, EasyMailProcessor processor,
                                         EasyMailCache easyMailCache, ExecutorService noticeThreadPool) {
        EasyMailListener listener = new EasyMailListener();
        listener.setEasyMailServerConnector(connector);
        listener.setEasyMailProcessor(processor);
        listener.setEasyMailImapConfig(easyMailImapConfig);
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
                                       EasyMailProcessor easyMailProcessor, EasyMailSender easyMailSender) {
        EasyMailService service = new EasyMailService();
        service.setEasyMailListener(easyMailListener);
        service.setEasyMailServerConnector(easyMailServerConnector);
        service.setEasyMailProcessor(easyMailProcessor);
        service.setEasyMailSender(easyMailSender);
        service.setAutoStart(easyMailImapConfig.isAutoStart());
        return service;
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


    

}