package cn.sunyblog.easymail.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * SMTP邮件发送配置
 */
@Data
@ConfigurationProperties(prefix = "mail.smtp")
public class EasyMailSmtpConfig {
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
    private Retry retry = new Retry();

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
     * 重试配置类
     */
    @Data
    public static class Retry {
        /**
         * 是否启用重试机制
         */
        private boolean enabled = true;

        /**
         * 最大重试次数（不包括第一次尝试）
         */
        private int maxRetries = 3;

        /**
         * 初始重试延迟（毫秒）
         */
        private long initialDelayMs = 1000;

        /**
         * 最大重试延迟（毫秒）
         */
        private long maxDelayMs = 10000;

        /**
         * 是否使用指数退避策略
         * 如果为true，则每次重试的延迟时间会按照指数增长
         */
        private boolean useExponentialBackoff = true;

        /**
         * 退避乘数
         * 当使用指数退避策略时，每次重试的延迟时间会乘以这个系数
         */
        private double backoffMultiplier = 2.0;
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