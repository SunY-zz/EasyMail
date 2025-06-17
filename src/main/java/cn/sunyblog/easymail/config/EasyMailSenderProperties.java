package cn.sunyblog.easymail.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 邮件发送服务配置属性
 *
 * @author sunyblog
 * @since 1.0.0
 */
@Data
@Component
@ConfigurationProperties(prefix = "email.sender")
public class EasyMailSenderProperties {

    /**
     * 是否启用邮件发送服务
     */
    private boolean enabled = true;

    /**
     * 默认发送策略
     */
    private String defaultStrategy = "default";

    /**
     * 批量发送阈值（超过此数量使用批量策略）
     */
    private int batchThreshold = 10;

    /**
     * 高优先级关键词（包含这些关键词的邮件使用高优先级策略）
     */
    private String[] highPriorityKeywords = {"urgent", "紧急", "重要", "important"};

    /**
     * 监控配置
     */
    private Monitor monitor = new Monitor();

    /**
     * 模板配置
     */
    private Template template = new Template();

    /**
     * 事件配置
     */
    private Event event = new Event();

    @Data
    public static class Monitor {
        /**
         * 是否启用监控
         */
        private boolean enabled = true;

        /**
         * 统计数据保留时间（小时）
         */
        private int retentionHours = 24;

        /**
         * 健康检查间隔（分钟）
         */
        private int healthCheckInterval = 5;

        /**
         * 失败率阈值（超过此阈值认为不健康）
         */
        private double failureRateThreshold = 0.1;

        /**
         * 响应时间阈值（毫秒，超过此时间认为响应慢）
         */
        private long responseTimeThreshold = 5000;
    }

    @Data
    public static class Template {
        /**
         * 是否启用模板功能
         */
        private boolean enabled = true;

        /**
         * 模板缓存大小
         */
        private int cacheSize = 100;

        /**
         * 模板缓存过期时间（分钟）
         */
        private int cacheExpireMinutes = 60;

        /**
         * 默认模板路径
         */
        private String defaultPath = "classpath:templates/email/";

        /**
         * 是否自动加载默认模板
         */
        private boolean autoLoadDefaults = true;
    }

    @Data
    public static class Event {
        /**
         * 是否启用事件发布
         */
        private boolean enabled = true;

        /**
         * 是否异步发布事件
         */
        private boolean async = true;

        /**
         * 事件队列大小
         */
        private int queueSize = 1000;

        /**
         * 事件处理线程数
         */
        private int threadPoolSize = 2;
    }

    /**
     * 策略选择配置
     */
    @Data
    public static class Strategy {
        /**
         * 是否启用自动策略选择
         */
        private boolean autoSelect = true;

        /**
         * 策略选择算法（round_robin, weighted, performance）
         */
        private String selectionAlgorithm = "performance";

        /**
         * 性能统计窗口大小
         */
        private int performanceWindowSize = 100;
    }

    private Strategy strategy = new Strategy();
}