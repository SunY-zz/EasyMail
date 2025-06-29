package cn.sunyblog.easymail.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * EasyMail动态线程池配置类
 * 用于Spring Boot配置绑定
 *
 * @author suny
 */
@Data
@ConfigurationProperties(prefix = "mail.dynamic-thread-pool")
public class EasyMailDynamicThreadPoolConfig {

    /**
     * 是否启用动态线程池，默认启用
     */
    private boolean enabled = true;

    /**
     * 基本配置
     */
    private Basic basic = new Basic();

    /**
     * 监控配置
     */
    private Monitor monitor = new Monitor();

    /**
     * 调整策略配置
     */
    private Adjustment adjustment = new Adjustment();

    /**
     * 安全边界配置
     */
    private Safety safety = new Safety();

    /**
     * 基本配置类
     */
    @Data
    public static class Basic {
        /**
         * 核心线程数，默认5
         */
        private int corePoolSize = 5;

        /**
         * 最大线程数，默认20
         */
        private int maximumPoolSize = 20;

        /**
         * 队列容量，默认100
         */
        private int queueCapacity = 100;

        /**
         * 线程存活时间（秒），默认60秒
         */
        private long keepAliveTime = 60;

        /**
         * 线程池名称前缀，默认EasyMail-DynamicPool
         */
        private String threadPoolName = "EasyMail-DynamicPool";
    }

    /**
     * 监控配置类
     */
    @Data
    public static class Monitor {
        /**
         * 监控间隔（秒），默认30秒
         */
        private long interval = 30;

        /**
         * 是否启用自动调整，默认启用
         */
        private boolean enableAutoAdjustment = true;

        /**
         * 是否启用指标日志，默认启用
         */
        private boolean enableMetricsLogging = true;
    }

    /**
     * 调整策略配置类
     */
    @Data
    public static class Adjustment {
        /**
         * 调整策略类名，默认使用DefaultAdjustmentStrategy
         */
        private String strategyClass = "cn.sunyblog.easymail.dynamicThreadPool.strategy.DefaultAdjustmentStrategy";

        /**
         * 最小调整间隔（秒），默认60秒
         */
        private long minInterval = 60;

        /**
         * 每小时最大调整次数，默认10次
         */
        private int maxPerHour = 10;

        /**
         * 队列使用率扩容阈值，默认80%
         */
        private double queueUsageExpandThreshold = 0.8;

        /**
         * 队列使用率缩容阈值，默认30%
         */
        private double queueUsageShrinkThreshold = 0.3;

        /**
         * 线程池活跃度扩容阈值，默认90%
         */
        private double threadActivityExpandThreshold = 0.9;

        /**
         * 线程池活跃度缩容阈值，默认50%
         */
        private double threadActivityShrinkThreshold = 0.5;

        /**
         * 任务等待时间阈值（毫秒），默认1000毫秒
         */
        private long taskWaitTimeThreshold = 1000;

        /**
         * 缩容持续时间要求（秒），默认300秒（5分钟）
         */
        private long shrinkDuration = 300;
    }

    /**
     * 安全边界配置类
     */
    @Data
    public static class Safety {
        /**
         * 最小核心线程数，默认1
         */
        private int minCorePoolSize = 1;

        /**
         * 最大核心线程数，默认50
         */
        private int maxCorePoolSize = 50;

        /**
         * 最小最大线程数，默认2
         */
        private int minMaximumPoolSize = 2;

        /**
         * 最大最大线程数，默认100
         */
        private int maxMaximumPoolSize = 100;

        /**
         * 最小队列容量，默认10
         */
        private int minQueueCapacity = 10;

        /**
         * 最大队列容量，默认10000
         */
        private int maxQueueCapacity = 10000;

        /**
         * 单次调整最大幅度（百分比），默认50%
         */
        private double maxAdjustmentRatio = 0.5;
    }

    /**
     * 转换为DynamicThreadPoolConfig
     */
    public cn.sunyblog.easymail.dynamicThreadPool.DynamicThreadPoolConfig toDynamicThreadPoolConfig() {
        cn.sunyblog.easymail.dynamicThreadPool.DynamicThreadPoolConfig config =
                new cn.sunyblog.easymail.dynamicThreadPool.DynamicThreadPoolConfig();

        // 基本配置
        config.setCorePoolSize(basic.corePoolSize);
        config.setMaximumPoolSize(basic.maximumPoolSize);
        config.setQueueCapacity(basic.queueCapacity);
        config.setKeepAliveTime(basic.keepAliveTime);
        config.setThreadPoolName(basic.threadPoolName);

        // 监控配置
        config.setMonitorInterval(monitor.interval);
        config.setEnableAutoAdjustment(monitor.enableAutoAdjustment);
        config.setEnableMetricsLogging(monitor.enableMetricsLogging);

        // 调整策略配置
        config.setAdjustmentStrategyClass(adjustment.strategyClass);
        config.setMinAdjustmentInterval(adjustment.minInterval);
        config.setMaxAdjustmentPerHour(adjustment.maxPerHour);

        // 安全边界配置
        config.setMinCorePoolSize(safety.minCorePoolSize);
        config.setMaxCorePoolSize(safety.maxCorePoolSize);
        config.setMinMaximumPoolSize(safety.minMaximumPoolSize);
        config.setMaxMaximumPoolSize(safety.maxMaximumPoolSize);
        config.setMinQueueCapacity(safety.minQueueCapacity);
        config.setMaxQueueCapacity(safety.maxQueueCapacity);

        return config;
    }
}