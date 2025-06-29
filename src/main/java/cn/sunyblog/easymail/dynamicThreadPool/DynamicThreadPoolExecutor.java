package cn.sunyblog.easymail.dynamicThreadPool;


import cn.sunyblog.easymail.dynamicThreadPool.monitor.ThreadPoolMetrics;
import cn.sunyblog.easymail.dynamicThreadPool.monitor.ThreadPoolMonitor;
import cn.sunyblog.easymail.dynamicThreadPool.queue.DynamicBlockingQueue;
import cn.sunyblog.easymail.dynamicThreadPool.strategy.AdjustmentStrategy;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 动态线程池执行器
 * 支持根据任务负载动态调整线程池参数
 *
 * @author suny
 */
public class DynamicThreadPoolExecutor extends ThreadPoolExecutor {

    private final DynamicThreadPoolConfig config;
    private final ThreadPoolMonitor monitor;
    private final AdjustmentStrategy adjustmentStrategy;
    private final AtomicBoolean adjusting = new AtomicBoolean(false);
    private final AtomicLong lastAdjustTime = new AtomicLong(System.currentTimeMillis());

    // 监控指标
    private final AtomicLong submittedTaskCount = new AtomicLong(0);
    private final AtomicLong completedTaskCount = new AtomicLong(0);

    public DynamicThreadPoolExecutor(DynamicThreadPoolConfig config) {
        super(
                config.getCorePoolSize(),
                config.getMaximumPoolSize(),
                config.getKeepAliveTime(),
                config.getTimeUnit(),
                new DynamicBlockingQueue<>(config.getQueueCapacity()),
                config.getThreadFactory(),
                config.getRejectedExecutionHandler()
        );

        this.config = config;
        this.monitor = new ThreadPoolMonitor(this);
        this.adjustmentStrategy = config.getAdjustmentStrategy();

        // 启动监控和调整任务
        startMonitoring();
    }

    @Override
    public void execute(Runnable command) {
        submittedTaskCount.incrementAndGet();
        super.execute(command);
    }

    @Override
    protected void afterExecute(Runnable r, Throwable t) {
        super.afterExecute(r, t);
        completedTaskCount.incrementAndGet();
    }

    /**
     * 启动监控任务
     */
    private void startMonitoring() {
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "DynamicThreadPool-Monitor");
            t.setDaemon(true);
            return t;
        });

        scheduler.scheduleAtFixedRate(this::monitorAndAdjust,
                config.getMonitorInterval(),
                config.getMonitorInterval(),
                TimeUnit.SECONDS);
    }

    /**
     * 监控并调整线程池参数
     */
    private void monitorAndAdjust() {
        try {
            // 获取当前指标
            ThreadPoolMetrics metrics = monitor.getMetrics();

            // 判断是否需要调整
            if (shouldAdjust(metrics)) {
                adjustThreadPool(metrics);
            }

            // 记录监控日志
            //logMetrics(metrics);

        } catch (Exception e) {
            System.err.println("监控调整过程中发生异常: " + e.getMessage());
            e.printStackTrace();
        }
    }


    /**
     * 判断是否需要调整
     */
    private boolean shouldAdjust(ThreadPoolMetrics metrics) {
        // 防止频繁调整
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastAdjustTime.get() < config.getMinAdjustmentInterval() * 1000) {
            return false;
        }

        // 如果正在调整中，跳过
        if (adjusting.get()) {
            return false;
        }

        AdjustmentStrategy.AdjustmentRecommendation recommendation = adjustmentStrategy.recommend(metrics);
        return recommendation != null;
    }

    /**
     * 调整线程池参数
     */
    private void adjustThreadPool(ThreadPoolMetrics metrics) {
        if (!adjusting.compareAndSet(false, true)) {
            return;
        }

        try {
            AdjustmentStrategy.AdjustmentRecommendation recommendation = adjustmentStrategy.recommend(metrics);

            if (recommendation != null) {
                boolean adjusted = false;

                // 调整最大线程数（必须先调整最大线程数）
                if (recommendation.getNewMaximumPoolSize() != getMaximumPoolSize()) {
                    int oldMax = getMaximumPoolSize();
                    setMaximumPoolSize(recommendation.getNewMaximumPoolSize());
                    System.out.println(String.format(
                            "[%s] 调整最大线程数: %d -> %d (%s)",
                            config.getThreadPoolName(), oldMax, recommendation.getNewMaximumPoolSize(),
                            recommendation.getReason()));
                    adjusted = true;
                }

                // 调整核心线程数
                if (recommendation.getNewCorePoolSize() != getCorePoolSize()) {
                    int oldCore = getCorePoolSize();
                    setCorePoolSize(recommendation.getNewCorePoolSize());
                    System.out.println(String.format(
                            "[%s] 调整核心线程数: %d -> %d (%s)",
                            config.getThreadPoolName(), oldCore, recommendation.getNewCorePoolSize(),
                            recommendation.getReason()));
                    adjusted = true;
                }

                // 调整队列大小
                if (recommendation.getNewQueueCapacity() > 0) {
                    DynamicBlockingQueue<?> queue = (DynamicBlockingQueue<?>) getQueue();
                    int oldCapacity = queue.getCapacity();
                    if (recommendation.getNewQueueCapacity() != oldCapacity) {
                        queue.setCapacity(recommendation.getNewQueueCapacity());
                        System.out.println(String.format(
                                "[%s] 调整队列大小: %d -> %d (%s)",
                                config.getThreadPoolName(), oldCapacity, recommendation.getNewQueueCapacity(),
                                recommendation.getReason()));
                        adjusted = true;
                    }
                }

                if (adjusted) {
                    lastAdjustTime.set(System.currentTimeMillis());

                    System.out.println(String.format(
                            "[%s] 线程池调整完成 - %s",
                            config.getThreadPoolName(), recommendation.getType().getDescription()));
                }
            }

        } catch (Exception e) {
            System.err.println("调整线程池参数时发生异常: " + e.getMessage());
            e.printStackTrace();
        } finally {
            adjusting.set(false);
        }
    }

    /**
     * 记录监控指标
     */
    private void logMetrics(ThreadPoolMetrics metrics) {
        if (config.isEnableMetricsLogging()) {
            System.out.println(String.format(
                    "[ThreadPool-Metrics] 核心线程数: %d, 最大线程数: %d, 活跃线程数: %d, 队列大小: %d/%d, " +
                            "队列使用率: %.2f%%, 线程活跃度: %.2f%%, 提交速率: %.2f/s, 完成速率: %.2f/s, 平均等待时间: %.2fms",
                    getCorePoolSize(),
                    getMaximumPoolSize(),
                    getActiveCount(),
                    getQueue().size(),
                    ((DynamicBlockingQueue<?>) getQueue()).getCapacity(),
                    metrics.getQueueUsageRate() * 100,
                    metrics.getThreadPoolActivity() * 100,
                    metrics.getTaskSubmitRate(),
                    metrics.getTaskCompleteRate(),
                    metrics.getAverageWaitTime()
            ));
        }
    }

    /**
     * 获取当前监控指标
     */
    public ThreadPoolMetrics getCurrentMetrics() {
        return monitor.getMetrics();
    }

    /**
     * 获取调整统计信息
     */
    public String getAdjustmentStats() {
        return String.format(
                "最后调整时间: %s",
                new java.util.Date(lastAdjustTime.get())
        );
    }

    /**
     * 获取配置信息
     */
    public DynamicThreadPoolConfig getConfig() {
        return config;
    }

    @Override
    public void shutdown() {
        super.shutdown();
        //System.out.println("动态线程池已关闭");
    }
}