package cn.sunyblog.easymail.dynamicThreadPool.strategy;


import cn.sunyblog.easymail.dynamicThreadPool.monitor.ThreadPoolMetrics;

/**
 * 线程池调整策略接口
 * 定义根据监控指标调整线程池参数的策略
 *
 * @author suny
 */
public interface AdjustmentStrategy {

    /**
     * 根据监控指标计算调整建议
     *
     * @param metrics 当前线程池监控指标
     * @return 调整建议，如果不需要调整则返回null
     */
    AdjustmentRecommendation recommend(ThreadPoolMetrics metrics);

    /**
     * 获取策略名称
     */
    String getStrategyName();

    /**
     * 获取策略描述
     */
    String getDescription();

    /**
     * 调整建议
     */
    class AdjustmentRecommendation {
        private final int newCorePoolSize;
        private final int newMaximumPoolSize;
        private final int newQueueCapacity;
        private final String reason;
        private final AdjustmentType type;

        public AdjustmentRecommendation(int newCorePoolSize, int newMaximumPoolSize,
                                        int newQueueCapacity, String reason, AdjustmentType type) {
            this.newCorePoolSize = newCorePoolSize;
            this.newMaximumPoolSize = newMaximumPoolSize;
            this.newQueueCapacity = newQueueCapacity;
            this.reason = reason;
            this.type = type;
        }

        public int getNewCorePoolSize() {
            return newCorePoolSize;
        }

        public int getNewMaximumPoolSize() {
            return newMaximumPoolSize;
        }

        public int getNewQueueCapacity() {
            return newQueueCapacity;
        }

        public String getReason() {
            return reason;
        }

        public AdjustmentType getType() {
            return type;
        }

        @Override
        public String toString() {
            return String.format(
                    "AdjustmentRecommendation{type=%s, coreSize=%d, maxSize=%d, queueCapacity=%d, reason='%s'}",
                    type, newCorePoolSize, newMaximumPoolSize, newQueueCapacity, reason
            );
        }
    }

    /**
     * 调整类型枚举
     */
    enum AdjustmentType {
        SCALE_UP("扩容"),
        SCALE_DOWN("缩容"),
        OPTIMIZE("优化"),
        EMERGENCY("紧急调整");

        private final String description;

        AdjustmentType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }
}