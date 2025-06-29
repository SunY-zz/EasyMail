package cn.sunyblog.easymail.dynamicThreadPool;

/**
 * 动态线程池配置类
 * 管理线程池的各种配置参数
 *
 * @author suny
 */
public class DynamicThreadPoolConfig {

    // 基本配置
    private volatile int corePoolSize = 5;
    private volatile int maximumPoolSize = 10;
    private volatile int queueCapacity = 20;
    private volatile long keepAliveTime = 60; // 秒

    // 监控配置
    private volatile long monitorInterval = 30; // 监控间隔，秒
    private volatile boolean enableAutoAdjustment = true; // 是否启用自动调整
    private volatile boolean enableMetricsLogging = true; // 是否启用指标日志

    // 调整策略配置
    private volatile String adjustmentStrategyClass = "cn.sunyblog.tool.threadpool.strategy.DefaultAdjustmentStrategy";

    // 安全边界配置
    private volatile int minCorePoolSize = 1;
    private volatile int maxCorePoolSize = 50;
    private volatile int minMaximumPoolSize = 2;
    private volatile int maxMaximumPoolSize = 100;
    private volatile int minQueueCapacity = 5;
    private volatile int maxQueueCapacity = 1000;

    // 调整频率限制
    private volatile long minAdjustmentInterval = 60; // 最小调整间隔，秒
    private volatile int maxAdjustmentPerHour = 10; // 每小时最大调整次数

    // 线程池名称
    private volatile String threadPoolName = "DynamicThreadPool";

    public DynamicThreadPoolConfig() {
    }

    public DynamicThreadPoolConfig(int corePoolSize, int maximumPoolSize, int queueCapacity) {
        this.corePoolSize = corePoolSize;
        this.maximumPoolSize = maximumPoolSize;
        this.queueCapacity = queueCapacity;
    }

    // Getter and Setter methods
    public int getCorePoolSize() {
        return corePoolSize;
    }

    public void setCorePoolSize(int corePoolSize) {
        if (corePoolSize < minCorePoolSize || corePoolSize > maxCorePoolSize) {
            throw new IllegalArgumentException(
                    String.format("核心线程数必须在 %d 到 %d 之间", minCorePoolSize, maxCorePoolSize));
        }
        this.corePoolSize = corePoolSize;
    }

    public int getMaximumPoolSize() {
        return maximumPoolSize;
    }

    public void setMaximumPoolSize(int maximumPoolSize) {
        if (maximumPoolSize < minMaximumPoolSize || maximumPoolSize > maxMaximumPoolSize) {
            throw new IllegalArgumentException(
                    String.format("最大线程数必须在 %d 到 %d 之间", minMaximumPoolSize, maxMaximumPoolSize));
        }
        if (maximumPoolSize < corePoolSize) {
            throw new IllegalArgumentException("最大线程数不能小于核心线程数");
        }
        this.maximumPoolSize = maximumPoolSize;
    }

    public int getQueueCapacity() {
        return queueCapacity;
    }

    public void setQueueCapacity(int queueCapacity) {
        if (queueCapacity < minQueueCapacity || queueCapacity > maxQueueCapacity) {
            throw new IllegalArgumentException(
                    String.format("队列容量必须在 %d 到 %d 之间", minQueueCapacity, maxQueueCapacity));
        }
        this.queueCapacity = queueCapacity;
    }

    public long getKeepAliveTime() {
        return keepAliveTime;
    }

    public void setKeepAliveTime(long keepAliveTime) {
        if (keepAliveTime < 0) {
            throw new IllegalArgumentException("线程存活时间不能为负数");
        }
        this.keepAliveTime = keepAliveTime;
    }

    public long getMonitorInterval() {
        return monitorInterval;
    }

    public void setMonitorInterval(long monitorInterval) {
        if (monitorInterval < 5) {
            throw new IllegalArgumentException("监控间隔不能小于5秒");
        }
        this.monitorInterval = monitorInterval;
    }

    public boolean isEnableAutoAdjustment() {
        return enableAutoAdjustment;
    }

    public void setEnableAutoAdjustment(boolean enableAutoAdjustment) {
        this.enableAutoAdjustment = enableAutoAdjustment;
    }

    public boolean isEnableMetricsLogging() {
        return enableMetricsLogging;
    }

    public void setEnableMetricsLogging(boolean enableMetricsLogging) {
        this.enableMetricsLogging = enableMetricsLogging;
    }

    public String getAdjustmentStrategyClass() {
        return adjustmentStrategyClass;
    }

    public void setAdjustmentStrategyClass(String adjustmentStrategyClass) {
        this.adjustmentStrategyClass = adjustmentStrategyClass;
    }

    public int getMinCorePoolSize() {
        return minCorePoolSize;
    }

    public void setMinCorePoolSize(int minCorePoolSize) {
        this.minCorePoolSize = minCorePoolSize;
    }

    public int getMaxCorePoolSize() {
        return maxCorePoolSize;
    }

    public void setMaxCorePoolSize(int maxCorePoolSize) {
        this.maxCorePoolSize = maxCorePoolSize;
    }

    public int getMinMaximumPoolSize() {
        return minMaximumPoolSize;
    }

    public void setMinMaximumPoolSize(int minMaximumPoolSize) {
        this.minMaximumPoolSize = minMaximumPoolSize;
    }

    public int getMaxMaximumPoolSize() {
        return maxMaximumPoolSize;
    }

    public void setMaxMaximumPoolSize(int maxMaximumPoolSize) {
        this.maxMaximumPoolSize = maxMaximumPoolSize;
    }

    public int getMinQueueCapacity() {
        return minQueueCapacity;
    }

    public void setMinQueueCapacity(int minQueueCapacity) {
        this.minQueueCapacity = minQueueCapacity;
    }

    public int getMaxQueueCapacity() {
        return maxQueueCapacity;
    }

    public void setMaxQueueCapacity(int maxQueueCapacity) {
        this.maxQueueCapacity = maxQueueCapacity;
    }

    public long getMinAdjustmentInterval() {
        return minAdjustmentInterval;
    }

    public void setMinAdjustmentInterval(long minAdjustmentInterval) {
        this.minAdjustmentInterval = minAdjustmentInterval;
    }

    public int getMaxAdjustmentPerHour() {
        return maxAdjustmentPerHour;
    }

    public void setMaxAdjustmentPerHour(int maxAdjustmentPerHour) {
        this.maxAdjustmentPerHour = maxAdjustmentPerHour;
    }

    public String getThreadPoolName() {
        return threadPoolName;
    }

    public void setThreadPoolName(String threadPoolName) {
        this.threadPoolName = threadPoolName;
    }

    public java.util.concurrent.TimeUnit getTimeUnit() {
        return java.util.concurrent.TimeUnit.SECONDS;
    }

    public java.util.concurrent.ThreadFactory getThreadFactory() {
        return java.util.concurrent.Executors.defaultThreadFactory();
    }

    public java.util.concurrent.RejectedExecutionHandler getRejectedExecutionHandler() {
        return new java.util.concurrent.ThreadPoolExecutor.AbortPolicy();
    }

    public cn.sunyblog.easymail.dynamicThreadPool.strategy.AdjustmentStrategy getAdjustmentStrategy() {
        try {
            Class<?> clazz = Class.forName(adjustmentStrategyClass);
            return (cn.sunyblog.easymail.dynamicThreadPool.strategy.AdjustmentStrategy) clazz.newInstance();
        } catch (Exception e) {
            return new cn.sunyblog.easymail.dynamicThreadPool.strategy.DefaultAdjustmentStrategy();
        }
    }

    /**
     * 验证配置的合法性
     */
    public void validate() {
        if (corePoolSize > maximumPoolSize) {
            throw new IllegalArgumentException("核心线程数不能大于最大线程数");
        }

        if (corePoolSize < minCorePoolSize || corePoolSize > maxCorePoolSize) {
            throw new IllegalArgumentException(
                    String.format("核心线程数必须在 %d 到 %d 之间", minCorePoolSize, maxCorePoolSize));
        }

        if (maximumPoolSize < minMaximumPoolSize || maximumPoolSize > maxMaximumPoolSize) {
            throw new IllegalArgumentException(
                    String.format("最大线程数必须在 %d 到 %d 之间", minMaximumPoolSize, maxMaximumPoolSize));
        }

        if (queueCapacity < minQueueCapacity || queueCapacity > maxQueueCapacity) {
            throw new IllegalArgumentException(
                    String.format("队列容量必须在 %d 到 %d 之间", minQueueCapacity, maxQueueCapacity));
        }
    }

    /**
     * 创建配置的副本
     */
    public DynamicThreadPoolConfig copy() {
        DynamicThreadPoolConfig copy = new DynamicThreadPoolConfig();
        copy.corePoolSize = this.corePoolSize;
        copy.maximumPoolSize = this.maximumPoolSize;
        copy.queueCapacity = this.queueCapacity;
        copy.keepAliveTime = this.keepAliveTime;
        copy.monitorInterval = this.monitorInterval;
        copy.enableAutoAdjustment = this.enableAutoAdjustment;
        copy.enableMetricsLogging = this.enableMetricsLogging;
        copy.adjustmentStrategyClass = this.adjustmentStrategyClass;
        copy.minCorePoolSize = this.minCorePoolSize;
        copy.maxCorePoolSize = this.maxCorePoolSize;
        copy.minMaximumPoolSize = this.minMaximumPoolSize;
        copy.maxMaximumPoolSize = this.maxMaximumPoolSize;
        copy.minQueueCapacity = this.minQueueCapacity;
        copy.maxQueueCapacity = this.maxQueueCapacity;
        copy.minAdjustmentInterval = this.minAdjustmentInterval;
        copy.maxAdjustmentPerHour = this.maxAdjustmentPerHour;
        copy.threadPoolName = this.threadPoolName;
        return copy;
    }

    @Override
    public String toString() {
        return String.format(
                "DynamicThreadPoolConfig{name='%s', core=%d, max=%d, queue=%d, " +
                        "keepAlive=%ds, monitorInterval=%ds, autoAdjust=%s}",
                threadPoolName, corePoolSize, maximumPoolSize, queueCapacity,
                keepAliveTime, monitorInterval, enableAutoAdjustment
        );
    }
}