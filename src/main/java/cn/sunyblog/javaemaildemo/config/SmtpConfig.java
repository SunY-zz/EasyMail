package cn.sunyblog.javaemaildemo.config;

import cn.sunyblog.javaemaildemo.config.RetryConfig;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * SMTP邮件发送配置
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "mail.smtp")
public class SmtpConfig {
    /**
     * SMTP服务器地址
     */
    private String server;
    
    /**
     * SMTP服务器端口
     */
    private String port;
    
    /**
     * 邮件协议，例如smtp
     */
    private String protocol;
    
    /**
     * 邮件账户用户名
     */
    private String username;
    
    /**
     * 邮件账户授权码
     */
    private String password;
    
    /**
     * 连接配置
     */
    private Connection connection = new Connection();
    
    /**
     * 邮件属性配置
     */
    private Properties properties = new Properties();
    
    /**
     * 重试配置
     */
    private RetryConfig retry = new RetryConfig();

    /**
     * 日志配置
     */
    private Log log = new Log();
    
    /**
     * 连接配置类，用于配置邮件连接的超时参数
     */
    @Data
    public static class Connection {
        /**
         * 连接超时时间，默认15秒
         */
        private int timeout = 15000;
        
        /**
         * 读取超时时间，默认30秒
         */
        private int readTimeout = 30000;
        
        /**
         * 写入超时时间，默认30秒
         */
        private int writeTimeout = 30000;
    }
    
    /**
     * 邮件属性配置类
     */
    @Data
    public static class Properties {
        /**
         * 是否启用SMTP认证
         */
        private boolean mailSmtpAuth = true;
        
        /**
         * 是否启用STARTTLS
         */
        private boolean mailSmtpStarttlsEnable = true;
    }

    /**
     * 日志配置类
     */
    @Data
    public static class Log {
        /**
         * 是否启用调试日志
         */
        private boolean debugEnabled = false;
    }
}