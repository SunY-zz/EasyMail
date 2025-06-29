package cn.sunyblog.easymail.dynamicThreadPool.strategy;


import cn.sunyblog.easymail.dynamicThreadPool.monitor.ThreadPoolMetrics;

/**
 * 默认的线程池调整策略实现
 * 基于负载情况、队列使用率、线程活跃度等指标进行智能调整
 *
 * @author suny
 */
public class DefaultAdjustmentStrategy implements AdjustmentStrategy {

    // 调整阈值配置
    private final double highLoadThreshold = 0.8;      // 高负载阈值
    private final double lowLoadThreshold = 0.3;       // 低负载阈值
    private final double queueHighThreshold = 0.7;     // 队列高使用率阈值
    private final double queueLowThreshold = 0.2;      // 队列低使用率阈值
    private final double activityHighThreshold = 0.9;  // 线程高活跃度阈值
    private final double activityLowThreshold = 0.4;   // 线程低活跃度阈值

    // 调整幅度配置
    private final double scaleUpFactor = 1.5;          // 扩容倍数
    private final double scaleDownFactor = 0.8;        // 缩容倍数
    private final int minCorePoolSize = 2;              // 最小核心线程数
    private final int maxCorePoolSize = 50;             // 最大核心线程数
    private final int minMaxPoolSize = 4;               // 最小最大线程数
    private final int maxMaxPoolSize = 100;             // 最大最大线程数
    private final int minQueueCapacity = 10;            // 最小队列容量
    private final int maxQueueCapacity = 1000;          // 最大队列容量

    @Override
    public AdjustmentRecommendation recommend(ThreadPoolMetrics metrics) {
        // 检查是否需要紧急扩容
        AdjustmentRecommendation emergency = checkEmergencyScaleUp(metrics);
        if (emergency != null) {
            return emergency;
        }

        // 检查是否需要常规扩容
        AdjustmentRecommendation scaleUp = checkScaleUp(metrics);
        if (scaleUp != null) {
            return scaleUp;
        }

        // 检查是否需要缩容
        AdjustmentRecommendation scaleDown = checkScaleDown(metrics);
        if (scaleDown != null) {
            return scaleDown;
        }

        // 检查是否需要优化调整
        return checkOptimization(metrics);
    }

    /**
     * 检查是否需要紧急扩容
     */
    private AdjustmentRecommendation checkEmergencyScaleUp(ThreadPoolMetrics metrics) {
        boolean isOverloaded = metrics.getQueueUsageRate() > 0.95 ||
                metrics.getThreadPoolActivity() > 0.98;
        boolean hasLongWaitTime = metrics.hasLongWaitTime();
        boolean hasTaskBacklog = metrics.hasTaskBacklog();

        if (isOverloaded && (hasLongWaitTime || hasTaskBacklog)) {
            int newCoreSize = Math.min(
                    (int) (metrics.getCorePoolSize() * 2),
                    maxCorePoolSize
            );
            int newMaxSize = Math.min(
                    (int) (metrics.getMaximumPoolSize() * 2),
                    maxMaxPoolSize
            );
            int newQueueCapacity = Math.min(
                    (int) (getCurrentQueueCapacity(metrics) * 1.5),
                    maxQueueCapacity
            );

            return new AdjustmentRecommendation(
                    newCoreSize, newMaxSize, newQueueCapacity,
                    String.format("紧急扩容: 队列使用率%.1f%%, 线程活跃度%.1f%%, 平均等待时间%.1fms",
                            metrics.getQueueUsageRate() * 100,
                            metrics.getThreadPoolActivity() * 100,
                            metrics.getAverageWaitTime()),
                    AdjustmentType.EMERGENCY
            );
        }

        return null;
    }

    /**
     * 检查是否需要常规扩容
     */
    private AdjustmentRecommendation checkScaleUp(ThreadPoolMetrics metrics) {
        boolean highQueueUsage = metrics.getQueueUsageRate() > queueHighThreshold;
        boolean highActivity = metrics.getThreadPoolActivity() > activityHighThreshold;
        boolean submitRateHigher = metrics.getTaskSubmitRate() > metrics.getTaskCompleteRate() * 1.2;

        if ((highQueueUsage && highActivity) || submitRateHigher) {
            int newCoreSize = Math.min(
                    (int) (metrics.getCorePoolSize() * scaleUpFactor),
                    maxCorePoolSize
            );
            int newMaxSize = Math.min(
                    Math.max(newCoreSize * 2, (int) (metrics.getMaximumPoolSize() * scaleUpFactor)),
                    maxMaxPoolSize
            );
            int newQueueCapacity = getCurrentQueueCapacity(metrics);

            // 如果队列使用率很高，也适当增加队列容量
            if (metrics.getQueueUsageRate() > 0.8) {
                newQueueCapacity = Math.min(
                        (int) (newQueueCapacity * 1.3),
                        maxQueueCapacity
                );
            }

            return new AdjustmentRecommendation(
                    newCoreSize, newMaxSize, newQueueCapacity,
                    String.format("扩容: 队列使用率%.1f%%, 线程活跃度%.1f%%, 提交速率%.1f/s > 完成速率%.1f/s",
                            metrics.getQueueUsageRate() * 100,
                            metrics.getThreadPoolActivity() * 100,
                            metrics.getTaskSubmitRate(),
                            metrics.getTaskCompleteRate()),
                    AdjustmentType.SCALE_UP
            );
        }

        return null;
    }

    /**
     * 检查是否需要缩容
     */
    private AdjustmentRecommendation checkScaleDown(ThreadPoolMetrics metrics) {
        boolean lowQueueUsage = metrics.getQueueUsageRate() < queueLowThreshold;
        boolean lowActivity = metrics.getThreadPoolActivity() < activityLowThreshold;
        boolean submitRateLower = metrics.getTaskSubmitRate() < metrics.getTaskCompleteRate() * 0.7;
        boolean hasIdleThreads = metrics.getCurrentPoolSize() > metrics.getActiveThreads() + 2;

        // 只有在持续低负载的情况下才缩容
        if (lowQueueUsage && lowActivity && (submitRateLower || hasIdleThreads)) {
            int newCoreSize = Math.max(
                    (int) (metrics.getCorePoolSize() * scaleDownFactor),
                    minCorePoolSize
            );
            int newMaxSize = Math.max(
                    Math.max(newCoreSize * 2, (int) (metrics.getMaximumPoolSize() * scaleDownFactor)),
                    minMaxPoolSize
            );
            int newQueueCapacity = getCurrentQueueCapacity(metrics);

            // 如果队列使用率很低，可以适当减少队列容量
            if (metrics.getQueueUsageRate() < 0.1) {
                newQueueCapacity = Math.max(
                        (int) (newQueueCapacity * 0.8),
                        minQueueCapacity
                );
            }

            return new AdjustmentRecommendation(
                    newCoreSize, newMaxSize, newQueueCapacity,
                    String.format("缩容: 队列使用率%.1f%%, 线程活跃度%.1f%%, 空闲线程%d个",
                            metrics.getQueueUsageRate() * 100,
                            metrics.getThreadPoolActivity() * 100,
                            metrics.getCurrentPoolSize() - metrics.getActiveThreads()),
                    AdjustmentType.SCALE_DOWN
            );
        }

        return null;
    }

    /**
     * 检查是否需要优化调整
     */
    private AdjustmentRecommendation checkOptimization(ThreadPoolMetrics metrics) {
        // 检查线程池配置是否合理
        boolean coreMaxRatioUnreasonable = metrics.getMaximumPoolSize() < metrics.getCorePoolSize() * 1.5;
        boolean queueTooSmall = getCurrentQueueCapacity(metrics) < metrics.getCorePoolSize();
        boolean queueTooLarge = getCurrentQueueCapacity(metrics) > metrics.getMaximumPoolSize() * 10;

        if (coreMaxRatioUnreasonable || queueTooSmall || queueTooLarge) {
            int newCoreSize = metrics.getCorePoolSize();
            int newMaxSize = Math.max(newCoreSize * 2, metrics.getMaximumPoolSize());
            int newQueueCapacity;

            if (queueTooSmall) {
                newQueueCapacity = newCoreSize * 2;
            } else if (queueTooLarge) {
                newQueueCapacity = newMaxSize * 5;
            } else {
                newQueueCapacity = getCurrentQueueCapacity(metrics);
            }

            return new AdjustmentRecommendation(
                    newCoreSize, newMaxSize, newQueueCapacity,
                    "优化配置: 调整核心线程数与最大线程数比例，优化队列容量",
                    AdjustmentType.OPTIMIZE
            );
        }

        return null;
    }

    /**
     * 获取当前队列容量
     */
    private int getCurrentQueueCapacity(ThreadPoolMetrics metrics) {
        // 根据队列使用率和当前队列大小估算容量
        if (metrics.getQueueUsageRate() > 0) {
            return (int) (metrics.getQueueSize() / metrics.getQueueUsageRate());
        }
        // 如果队列为空，返回默认容量
        return Math.max(metrics.getCorePoolSize() * 2, 20);
    }

    @Override
    public String getStrategyName() {
        return "DefaultAdjustmentStrategy";
    }

    @Override
    public String getDescription() {
        return "基于负载情况、队列使用率、线程活跃度等指标的默认调整策略";
    }
}