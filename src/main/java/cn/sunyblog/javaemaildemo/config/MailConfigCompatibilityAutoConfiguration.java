package cn.sunyblog.javaemaildemo.config;

import cn.sunyblog.javaemaildemo.mail.MailConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

/**
 * 邮件配置兼容性自动配置类
 * 支持原有的 mail.imap 配置格式，确保向后兼容
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(MailConfig.class)
@Order(1) // 确保优先级高于EmailListenerAutoConfiguration
public class MailConfigCompatibilityAutoConfiguration {

    /**
     * 当使用原有的 mail.imap 配置格式时，直接注册 MailConfig Bean
     */
    @Bean("mailConfig")
    @ConditionalOnMissingBean(name = "mailConfig")
    public MailConfig mailConfigFromLegacyProperties(MailConfig mailConfig) {
        if (mailConfig.getServer() != null && !mailConfig.getServer().trim().isEmpty()) {
            log.info("使用兼容性配置加载 MailConfig: mail.imap.*");
            log.info("MailConfig配置: server={}, port={}, protocol={}, username={}", 
                    mailConfig.getServer(), mailConfig.getPort(), mailConfig.getProtocol(), mailConfig.getUsername());
        } else {
            log.info("创建默认空的 MailConfig，请检查配置文件中的 mail.imap.* 配置");
        }
        return mailConfig;
    }
}