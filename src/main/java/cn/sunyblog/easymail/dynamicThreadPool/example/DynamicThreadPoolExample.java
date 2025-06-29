package cn.sunyblog.easymail.dynamicThreadPool.example;


import cn.sunyblog.easymail.dynamicThreadPool.DynamicThreadPoolConfig;
import cn.sunyblog.easymail.dynamicThreadPool.DynamicThreadPoolExecutor;
import cn.sunyblog.easymail.dynamicThreadPool.monitor.ThreadPoolMetrics;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 动态线程池使用示例
 * 演示如何使用动态线程池以及其自动调整功能
 *
 * @author suny
 */
public class DynamicThreadPoolExample {

    public static void main(String[] args) throws InterruptedException {
        // 创建动态线程池配置
        DynamicThreadPoolConfig config = new DynamicThreadPoolConfig();
        config.setThreadPoolName("ExamplePool");
        config.setCorePoolSize(2);
        config.setMaximumPoolSize(8);
        config.setQueueCapacity(10);
        config.setKeepAliveTime(60);
        config.setMonitorInterval(10); // 10秒监控一次
        config.setEnableAutoAdjustment(true);
        config.setEnableMetricsLogging(true);

        // 创建动态线程池
        DynamicThreadPoolExecutor executor = new DynamicThreadPoolExecutor(config);

        System.out.println("=== 动态线程池示例开始 ===");
        System.out.println("初始配置: " + config);

        // 模拟不同的负载场景
        simulateLoadScenarios(executor);

        // 关闭线程池
        executor.shutdown();
        try {
            if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }

        System.out.println("=== 动态线程池示例结束 ===");
    }

    /**
     * 模拟不同的负载场景
     */
    private static void simulateLoadScenarios(DynamicThreadPoolExecutor executor) throws InterruptedException {
        AtomicInteger taskCounter = new AtomicInteger(0);

        // 场景1: 低负载 - 少量任务
        System.out.println("\n--- 场景1: 低负载测试 ---");
        submitTasks(executor, 5, 100, taskCounter, "低负载任务");
        Thread.sleep(15000); // 等待15秒观察调整
        printCurrentStatus(executor);

        // 场景2: 中等负载 - 适中任务量
        System.out.println("\n--- 场景2: 中等负载测试 ---");
        submitTasks(executor, 30, 200, taskCounter, "中等负载任务");
        Thread.sleep(15000); // 等待15秒观察调整
        printCurrentStatus(executor);

        // 场景3: 高负载 - 大量任务
        System.out.println("\n--- 场景3: 高负载测试 ---");
        submitTasks(executor, 80, 300, taskCounter, "高负载任务");
        Thread.sleep(20000); // 等待20秒观察调整
        printCurrentStatus(executor);

        // 场景4: 极限负载 - 超大量任务测试线程池耗尽
        System.out.println("\n--- 场景4: 极限负载测试 (测试线程池耗尽) ---");
        submitTasksWithRejectionHandling(executor, 150, 400, taskCounter, "极限负载任务");
        Thread.sleep(25000); // 等待25秒观察调整
        printCurrentStatus(executor);

        // 场景5: 突发负载 - 短时间大量任务
        System.out.println("\n--- 场景5: 突发负载测试 ---");
        for (int i = 0; i < 5; i++) {
            submitTasksWithRejectionHandling(executor, 50, 50, taskCounter, "突发任务-批次" + (i + 1));
            Thread.sleep(1000); // 短间隔
        }
        Thread.sleep(25000); // 等待25秒观察调整
        printCurrentStatus(executor);

        // 场景6: 持续高压测试
        System.out.println("\n--- 场景6: 持续高压测试 ---");
        for (int i = 0; i < 10; i++) {
            submitTasksWithRejectionHandling(executor, 20, 100, taskCounter, "持续高压任务-轮次" + (i + 1));
            Thread.sleep(500); // 很短间隔
        }
        Thread.sleep(30000); // 等待30秒观察调整
        printCurrentStatus(executor);

        // 场景7: 负载下降 - 任务量减少
        System.out.println("\n--- 场景7: 负载下降测试 ---");
        submitTasks(executor, 3, 500, taskCounter, "负载下降任务");
        Thread.sleep(30000); // 等待30秒观察缩容
        printCurrentStatus(executor);
    }

    /**
     * 提交任务
     */
    private static void submitTasks(DynamicThreadPoolExecutor executor, int taskCount,
                                    long taskDurationMs, AtomicInteger taskCounter, String taskType) {
        System.out.println(String.format("提交 %d 个 %s，每个任务耗时 %dms", taskCount, taskType, taskDurationMs));

        for (int i = 0; i < taskCount; i++) {
            final int taskId = taskCounter.incrementAndGet();
            executor.execute(() -> {
                try {
                    System.out.println(String.format(
                            "[%s-%d] 开始执行，线程: %s",
                            taskType, taskId, Thread.currentThread().getName()));

                    Thread.sleep(taskDurationMs);

                    System.out.println(String.format(
                            "[%s-%d] 执行完成", taskType, taskId));

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    System.out.println(String.format(
                            "[%s-%d] 被中断", taskType, taskId));
                }
            });
        }
    }

    /**
     * 提交任务并处理拒绝异常
     */
    private static void submitTasksWithRejectionHandling(DynamicThreadPoolExecutor executor, int taskCount,
                                                         long taskDurationMs, AtomicInteger taskCounter, String taskType) {
        System.out.println(String.format("提交 %d 个 %s，每个任务耗时 %dms (带拒绝处理)", taskCount, taskType, taskDurationMs));

        int rejectedCount = 0;
        for (int i = 0; i < taskCount; i++) {
            final int taskId = taskCounter.incrementAndGet();
            try {
                executor.execute(() -> {
                    try {
                        System.out.println(String.format(
                                "[%s-%d] 开始执行，线程: %s",
                                taskType, taskId, Thread.currentThread().getName()));

                        Thread.sleep(taskDurationMs);

                        System.out.println(String.format(
                                "[%s-%d] 执行完成", taskType, taskId));

                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        System.out.println(String.format(
                                "[%s-%d] 被中断", taskType, taskId));
                    }
                });
            } catch (Exception e) {
                rejectedCount++;
                System.out.println(String.format(
                        "[%s-%d] 任务被拒绝: %s", taskType, taskId, e.getMessage()));
            }
        }

        if (rejectedCount > 0) {
            System.out.println(String.format("*** %s: 总共 %d 个任务被拒绝 ***", taskType, rejectedCount));
        }
    }

    /**
     * 打印当前状态
     */
    private static void printCurrentStatus(DynamicThreadPoolExecutor executor) {
        ThreadPoolMetrics metrics = executor.getCurrentMetrics();
        System.out.println("\n=== 当前线程池状态 ===");
        System.out.println(metrics.toString());
        System.out.println(executor.getAdjustmentStats());
        System.out.println("========================\n");
    }
}