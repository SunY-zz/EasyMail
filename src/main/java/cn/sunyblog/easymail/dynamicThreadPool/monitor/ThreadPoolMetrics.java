package cn.sunyblog.easymail.dynamicThreadPool.monitor;

/**
 * 线程池监控指标数据
 * 封装线程池运行时的各种性能指标
 *
 * @author suny
 */
public class ThreadPoolMetrics {

    // 线程池基本配置
    private final int corePoolSize;
    private final int maximumPoolSize;
    private final int currentPoolSize;
    private final int activeThreads;

    // 队列相关指标
    private final int queueSize;
    private final double queueUsageRate;

    // 性能指标
    private final double threadPoolActivity;
    private final double taskSubmitRate;
    private final double taskCompleteRate;
    private final double averageWaitTime;
    private final long maxWaitTime;

    // 累计统计
    private final long totalSubmittedTasks;
    private final long totalCompletedTasks;
    private final long totalRejectedTasks;
    private final long completedTaskCount;

    public ThreadPoolMetrics(int corePoolSize, int maximumPoolSize, int currentPoolSize,
                             int activeThreads, int queueSize, double queueUsageRate,
                             double threadPoolActivity, double taskSubmitRate,
                             double taskCompleteRate, double averageWaitTime, long maxWaitTime,
                             long totalSubmittedTasks, long totalCompletedTasks,
                             long totalRejectedTasks, long completedTaskCount) {
        this.corePoolSize = corePoolSize;
        this.maximumPoolSize = maximumPoolSize;
        this.currentPoolSize = currentPoolSize;
        this.activeThreads = activeThreads;
        this.queueSize = queueSize;
        this.queueUsageRate = queueUsageRate;
        this.threadPoolActivity = threadPoolActivity;
        this.taskSubmitRate = taskSubmitRate;
        this.taskCompleteRate = taskCompleteRate;
        this.averageWaitTime = averageWaitTime;
        this.maxWaitTime = maxWaitTime;
        this.totalSubmittedTasks = totalSubmittedTasks;
        this.totalCompletedTasks = totalCompletedTasks;
        this.totalRejectedTasks = totalRejectedTasks;
        this.completedTaskCount = completedTaskCount;
    }

    public int getCorePoolSize() {
        return corePoolSize;
    }

    public int getMaximumPoolSize() {
        return maximumPoolSize;
    }

    public int getCurrentPoolSize() {
        return currentPoolSize;
    }

    public int getActiveThreads() {
        return activeThreads;
    }

    public int getQueueSize() {
        return queueSize;
    }

    public double getQueueUsageRate() {
        return queueUsageRate;
    }

    public double getThreadPoolActivity() {
        return threadPoolActivity;
    }

    public double getTaskSubmitRate() {
        return taskSubmitRate;
    }

    public double getTaskCompleteRate() {
        return taskCompleteRate;
    }

    public double getAverageWaitTime() {
        return averageWaitTime;
    }

    public long getMaxWaitTime() {
        return maxWaitTime;
    }

    public long getTotalSubmittedTasks() {
        return totalSubmittedTasks;
    }

    public long getTotalCompletedTasks() {
        return totalCompletedTasks;
    }

    public long getTotalRejectedTasks() {
        return totalRejectedTasks;
    }

    public long getCompletedTaskCount() {
        return completedTaskCount;
    }

    /**
     * 判断线程池是否处于高负载状态
     */
    public boolean isHighLoad() {
        return queueUsageRate > 0.8 || threadPoolActivity > 0.9;
    }

    /**
     * 判断线程池是否处于低负载状态
     */
    public boolean isLowLoad() {
        return queueUsageRate < 0.3 && threadPoolActivity < 0.5;
    }

    /**
     * 判断是否存在任务积压
     */
    public boolean hasTaskBacklog() {
        return queueUsageRate > 0.7 && taskSubmitRate > taskCompleteRate;
    }

    /**
     * 判断等待时间是否过长
     */
    public boolean hasLongWaitTime() {
        return averageWaitTime > 1000; // 超过1秒认为等待时间过长
    }

    /**
     * 获取负载等级
     *
     * @return 1-低负载, 2-中等负载, 3-高负载, 4-过载
     */
    public int getLoadLevel() {
        if (queueUsageRate > 0.9 || threadPoolActivity > 0.95) {
            return 4; // 过载
        } else if (queueUsageRate > 0.7 || threadPoolActivity > 0.8) {
            return 3; // 高负载
        } else if (queueUsageRate > 0.3 || threadPoolActivity > 0.5) {
            return 2; // 中等负载
        } else {
            return 1; // 低负载
        }
    }

    /**
     * 计算线程池利用率
     */
    public double getUtilizationRate() {
        return maximumPoolSize > 0 ? (double) currentPoolSize / maximumPoolSize : 0;
    }

    /**
     * 计算任务处理效率 (完成率/提交率)
     */
    public double getTaskProcessingEfficiency() {
        return taskSubmitRate > 0 ? taskCompleteRate / taskSubmitRate : 1.0;
    }

    @Override
    public String toString() {
        return String.format(
                "ThreadPoolMetrics{" +
                        "coreSize=%d, maxSize=%d, currentSize=%d, active=%d, " +
                        "queueSize=%d, queueUsage=%.2f%%, activity=%.2f%%, " +
                        "submitRate=%.2f/s, completeRate=%.2f/s, " +
                        "avgWait=%.2fms, maxWait=%dms, " +
                        "submitted=%d, completed=%d, rejected=%d, loadLevel=%d}",
                corePoolSize, maximumPoolSize, currentPoolSize, activeThreads,
                queueSize, queueUsageRate * 100, threadPoolActivity * 100,
                taskSubmitRate, taskCompleteRate,
                averageWaitTime, maxWaitTime,
                totalSubmittedTasks, totalCompletedTasks, totalRejectedTasks,
                getLoadLevel()
        );
    }

    /**
     * 获取简化的状态描述
     */
    public String getStatusSummary() {
        String loadDesc;
        switch (getLoadLevel()) {
            case 1:
                loadDesc = "低负载";
                break;
            case 2:
                loadDesc = "中等负载";
                break;
            case 3:
                loadDesc = "高负载";
                break;
            case 4:
                loadDesc = "过载";
                break;
            default:
                loadDesc = "未知";
                break;
        }

        return String.format(
                "[%s] 线程:%d/%d, 队列:%d(%.1f%%), 活跃度:%.1f%%, 效率:%.2f",
                loadDesc, currentPoolSize, maximumPoolSize,
                queueSize, queueUsageRate * 100, threadPoolActivity * 100,
                getTaskProcessingEfficiency()
        );
    }
}