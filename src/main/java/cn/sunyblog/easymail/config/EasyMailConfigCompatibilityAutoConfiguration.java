package cn.sunyblog.easymail.config;

import cn.sunyblog.easymail.mail.EasyMailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.annotation.Order;

/**
 * EasyMail配置兼容性自动配置类
 * 
 * <p>此配置类确保@EnableEasyMail注解方式与传统配置方式的兼容性。</p>
 * <p>支持原有的 mail.imap 配置格式，确保向后兼容。</p>
 * <p>提供配置属性的统一管理和服务的自动初始化。</p>
 * 
 * @author sunyblog
 * @since 1.0.0
 */
@Slf4j
@Configuration
@Order(1) // 确保优先级高于EmailListenerAutoConfiguration
public class EasyMailConfigCompatibilityAutoConfiguration {
    
    /**
     * 邮件服务Bean
     * 确保EasyMailService在Spring容器中可用
     */
    @Bean
    @ConditionalOnBean(EasyMailImapConfig.class)
    @ConditionalOnMissingBean(EasyMailService.class)
    public EasyMailService easyMailService(EasyMailImapConfig config) {
        log.info("创建EasyMailService实例");
        EasyMailService service = new EasyMailService();
        
        // 根据配置设置自动启动
        if (config != null && config.isAutoStart()) {
            log.info("配置了自动启动，将在应用启动后自动开启邮件服务");
            service.setAutoStart(true);
        }
        
        return service;
    }
    
    /**
     * 配置验证器
     * 验证配置的有效性
     */
    @Bean
    @ConditionalOnMissingBean(EasyMailConfigValidator.class)
    public EasyMailConfigValidator easyMailConfigValidator() {
        return new EasyMailConfigValidator();
    }
    
    /**
     * EasyMail配置验证器
     */
    public static class EasyMailConfigValidator {
        
        /**
         * 验证配置
         */
        public boolean validateConfig(EasyMailImapConfig config) {
            if (config == null) {
                log.warn("EasyMail配置为空");
                return false;
            }
            
            // 验证基本配置
            if (config.getServer() == null || config.getServer().trim().isEmpty()) {
                log.warn("邮件服务器地址未配置");
                return false;
            }
            
            if (Integer.parseInt(config.getPort()) <= 0 || Integer.parseInt(config.getPort()) > 65535) {
                log.warn("邮件服务器端口配置无效: {}", config.getPort());
                return false;
            }
            
            if (config.getUsername() == null || config.getUsername().trim().isEmpty()) {
                log.warn("邮件服务器用户名未配置");
                return false;
            }
            
            if (config.getPassword() == null || config.getPassword().trim().isEmpty()) {
                log.warn("邮件服务器密码未配置");
                return false;
            }
            
            log.info("EasyMail配置验证通过");
            return true;
        }
        
        /**
         * 获取配置摘要
         */
        public String getConfigSummary(EasyMailImapConfig config) {
            if (config == null) {
                return "配置为空";
            }
            
            return String.format("服务器: %s:%d, 协议: %s, 用户: %s, 自动启动: %s",
                config.getServer(),
                config.getPort(),
                config.getProtocol(),
                config.getUsername(),
                config.isAutoStart());
        }
    }
}