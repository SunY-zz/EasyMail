package cn.sunyblog.easymail.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 邮件发送重试配置
 * 用于配置邮件发送失败时的重试策略
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "mail.smtp.retry")
public class EasyMailRetryConfig {
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