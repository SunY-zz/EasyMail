package cn.sunyblog.easymail.dynamicThreadPool.monitor;

import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

/**
 * 线程池监控器
 * 负责收集线程池运行时的各种指标数据
 *
 * @author suny
 */
public class ThreadPoolMonitor {

    private final ThreadPoolExecutor threadPool;

    // 任务提交统计
    private final LongAdder totalSubmittedTasks = new LongAdder();
    private final LongAdder totalCompletedTasks = new LongAdder();
    private final LongAdder totalRejectedTasks = new LongAdder();

    // 时间统计
    private volatile long lastSubmitTime = System.currentTimeMillis();
    private volatile long lastCompleteTime = System.currentTimeMillis();

    // 任务等待时间统计
    private final LongAdder totalWaitTime = new LongAdder();
    private final AtomicLong maxWaitTime = new AtomicLong(0);

    // 监控周期内的统计
    private volatile long periodStartTime = System.currentTimeMillis();
    private volatile long periodSubmittedTasks = 0;
    private volatile long periodCompletedTasks = 0;

    public ThreadPoolMonitor(ThreadPoolExecutor threadPool) {
        this.threadPool = threadPool;
    }

    /**
     * 记录任务提交
     */
    public void recordTaskSubmission() {
        totalSubmittedTasks.increment();
        lastSubmitTime = System.currentTimeMillis();
    }

    /**
     * 记录任务完成
     */
    public void recordTaskCompletion() {
        totalCompletedTasks.increment();
        lastCompleteTime = System.currentTimeMillis();
    }

    /**
     * 记录任务拒绝
     */
    public void recordTaskRejection() {
        totalRejectedTasks.increment();
    }

    /**
     * 记录任务等待时间
     */
    public void recordTaskWaitTime(long waitTimeMs) {
        totalWaitTime.add(waitTimeMs);

        // 更新最大等待时间
        long currentMax = maxWaitTime.get();
        while (waitTimeMs > currentMax) {
            if (maxWaitTime.compareAndSet(currentMax, waitTimeMs)) {
                break;
            }
            currentMax = maxWaitTime.get();
        }
    }

    /**
     * 获取线程池监控指标
     */
    public ThreadPoolMetrics getMetrics() {
        long currentTime = System.currentTimeMillis();

        // 计算任务提交速率 (任务/秒)
        double submitRate = calculateSubmitRate(currentTime);

        // 计算任务完成速率 (任务/秒)
        double completeRate = calculateCompleteRate(currentTime);

        // 计算平均等待时间
        double avgWaitTime = calculateAverageWaitTime();

        // 获取线程池基本信息
        int corePoolSize = threadPool.getCorePoolSize();
        int maximumPoolSize = threadPool.getMaximumPoolSize();
        int currentPoolSize = threadPool.getPoolSize();
        int activeThreads = threadPool.getActiveCount();
        long completedTaskCount = threadPool.getCompletedTaskCount();
        int queueSize = threadPool.getQueue().size();

        // 计算线程池活跃度
        double threadPoolActivity = currentPoolSize > 0 ? (double) activeThreads / currentPoolSize : 0;

        // 计算队列使用率
        double queueUsageRate = 0;
        if (threadPool.getQueue() instanceof cn.sunyblog.easymail.dynamicThreadPool.queue.DynamicBlockingQueue) {
            cn.sunyblog.easymail.dynamicThreadPool.queue.DynamicBlockingQueue<?> dynamicQueue =
                    (cn.sunyblog.easymail.dynamicThreadPool.queue.DynamicBlockingQueue<?>) threadPool.getQueue();
            queueUsageRate = dynamicQueue.getUsageRate();
        } else {
            // 对于普通队列，估算使用率
            int queueCapacity = queueSize + threadPool.getQueue().remainingCapacity();
            queueUsageRate = queueCapacity > 0 ? (double) queueSize / queueCapacity : 0;
        }

        return new ThreadPoolMetrics(
                corePoolSize,
                maximumPoolSize,
                currentPoolSize,
                activeThreads,
                queueSize,
                queueUsageRate,
                threadPoolActivity,
                submitRate,
                completeRate,
                avgWaitTime,
                maxWaitTime.get(),
                totalSubmittedTasks.sum(),
                totalCompletedTasks.sum(),
                totalRejectedTasks.sum(),
                completedTaskCount
        );
    }

    /**
     * 计算任务提交速率
     */
    private double calculateSubmitRate(long currentTime) {
        long timeDiff = currentTime - periodStartTime;
        if (timeDiff <= 0) {
            return 0;
        }

        long currentSubmitted = totalSubmittedTasks.sum();
        long submittedInPeriod = currentSubmitted - periodSubmittedTasks;

        return (double) submittedInPeriod / (timeDiff / 1000.0);
    }

    /**
     * 计算任务完成速率
     */
    private double calculateCompleteRate(long currentTime) {
        long timeDiff = currentTime - periodStartTime;
        if (timeDiff <= 0) {
            return 0;
        }

        long currentCompleted = totalCompletedTasks.sum();
        long completedInPeriod = currentCompleted - periodCompletedTasks;

        return (double) completedInPeriod / (timeDiff / 1000.0);
    }

    /**
     * 计算平均等待时间
     */
    private double calculateAverageWaitTime() {
        long totalTasks = totalCompletedTasks.sum();
        if (totalTasks == 0) {
            return 0;
        }
        return (double) totalWaitTime.sum() / totalTasks;
    }

    /**
     * 重置周期统计
     */
    public void resetPeriodStats() {
        periodStartTime = System.currentTimeMillis();
        periodSubmittedTasks = totalSubmittedTasks.sum();
        periodCompletedTasks = totalCompletedTasks.sum();
    }

    /**
     * 重置所有统计数据
     */
    public void resetAllStats() {
        totalSubmittedTasks.reset();
        totalCompletedTasks.reset();
        totalRejectedTasks.reset();
        totalWaitTime.reset();
        maxWaitTime.set(0);
        resetPeriodStats();
    }
}