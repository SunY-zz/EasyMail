package cn.sunyblog.easymail.send.monitor;

import cn.sunyblog.easymail.send.EasyMailSendResult;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

/**
 * 邮件发送监控服务
 * 提供邮件发送的监控、统计和报告功能
 *
 * @author suny
 * @version 1.0.0
 */
@Slf4j
@Component
public class EasyMailSendMonitor {

    /**
     * 发送统计信息
     */
    private final SendStatistics statistics = new SendStatistics();

    /**
     * 发送历史记录（最近1000条）
     */
    private final Queue<SendRecord> sendHistory = new LinkedList<>();
    private static final int MAX_HISTORY_SIZE = 1000;

    /**
     * 错误统计
     */
    private final Map<String, LongAdder> errorStats = new ConcurrentHashMap<>();

    /**
     * 每小时发送统计
     */
    private final Map<String, LongAdder> hourlyStats = new ConcurrentHashMap<>();

    /**
     * 监控开始时间
     */
    private LocalDateTime monitorStartTime;

    @PostConstruct
    public void init() {
        monitorStartTime = LocalDateTime.now();
        log.info("邮件发送监控服务已启动");
    }

    /**
     * 记录发送结果
     *
     * @param result 发送结果
     */
    public void recordSendResult(EasyMailSendResult result) {
        if (result == null) {
            return;
        }

        // 更新基础统计
        statistics.totalSent.increment();

        if (result.isSuccess()) {
            statistics.successCount.increment();
        } else {
            statistics.failureCount.increment();

            // 记录错误统计
            String errorType = result.getErrorCode() != null ? result.getErrorCode() : "UNKNOWN_ERROR";
            errorStats.computeIfAbsent(errorType, k -> new LongAdder()).increment();
        }

        // 更新耗时统计
        if (result.getDuration() > 0) {
            statistics.totalDuration.add(result.getDuration());
            statistics.maxDuration.updateAndGet(current -> Math.max(current, result.getDuration()));
            statistics.minDuration.updateAndGet(current ->
                    current == Long.MAX_VALUE ? result.getDuration() : Math.min(current, result.getDuration()));
        }

        // 更新收件人统计
        if (result.getRecipients() != null) {
            statistics.totalRecipients.add(result.getRecipients().size());
        }

        // 更新每小时统计
        String hourKey = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH"));
        hourlyStats.computeIfAbsent(hourKey, k -> new LongAdder()).increment();

        // 添加到历史记录
        addToHistory(result);

        // 批量发送统计
        if (result.isBatchSend()) {
            statistics.batchSendCount.increment();
            if (result.getSuccessCount() > 0) {
                statistics.batchSuccessCount.add(result.getSuccessCount());
            }
            if (result.getFailureCount() > 0) {
                statistics.batchFailureCount.add(result.getFailureCount());
            }
        }

        log.debug("记录邮件发送结果: 成功={}, 主题={}", result.isSuccess(), result.getSubject());
    }

    /**
     * 添加到历史记录
     */
    private synchronized void addToHistory(EasyMailSendResult result) {
        SendRecord record = new SendRecord();
        record.setTimestamp(LocalDateTime.now());
        record.setSuccess(result.isSuccess());
        record.setSubject(result.getSubject());
        record.setRecipientCount(result.getRecipients() != null ? result.getRecipients().size() : 0);
        record.setDuration(result.getDuration());
        record.setErrorMessage(result.getErrorMessage());
        record.setErrorCode(result.getErrorCode());
        record.setBatchSend(result.isBatchSend());

        sendHistory.offer(record);

        // 保持历史记录大小
        while (sendHistory.size() > MAX_HISTORY_SIZE) {
            sendHistory.poll();
        }
    }

    /**
     * 获取发送统计信息
     *
     * @return 统计信息映射
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();

        long totalSent = statistics.totalSent.sum();
        long successCount = statistics.successCount.sum();
        long failureCount = statistics.failureCount.sum();
        long totalDuration = statistics.totalDuration.sum();

        stats.put("totalSent", totalSent);
        stats.put("successCount", successCount);
        stats.put("failureCount", failureCount);
        stats.put("successRate", totalSent > 0 ? (double) successCount / totalSent : 0.0);
        stats.put("failureRate", totalSent > 0 ? (double) failureCount / totalSent : 0.0);

        stats.put("totalRecipients", statistics.totalRecipients.sum());
        stats.put("averageRecipientsPerEmail", totalSent > 0 ? (double) statistics.totalRecipients.sum() / totalSent : 0.0);

        stats.put("totalDuration", totalDuration);
        stats.put("averageDuration", totalSent > 0 ? (double) totalDuration / totalSent : 0.0);
        stats.put("maxDuration", statistics.maxDuration.get() == 0 ? 0 : statistics.maxDuration.get());
        stats.put("minDuration", statistics.minDuration.get() == Long.MAX_VALUE ? 0 : statistics.minDuration.get());

        stats.put("batchSendCount", statistics.batchSendCount.sum());
        stats.put("batchSuccessCount", statistics.batchSuccessCount.sum());
        stats.put("batchFailureCount", statistics.batchFailureCount.sum());

        stats.put("monitorStartTime", monitorStartTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        stats.put("monitorDuration", java.time.Duration.between(monitorStartTime, LocalDateTime.now()).toMinutes() + " 分钟");

        return stats;
    }

    /**
     * 获取错误统计
     *
     * @return 错误统计映射
     */
    public Map<String, Long> getErrorStatistics() {
        Map<String, Long> errors = new HashMap<>();
        errorStats.forEach((errorType, count) -> errors.put(errorType, count.sum()));
        return errors;
    }

    /**
     * 获取每小时发送统计
     *
     * @return 每小时统计映射
     */
    public Map<String, Long> getHourlyStatistics() {
        Map<String, Long> hourly = new HashMap<>();
        hourlyStats.forEach((hour, count) -> hourly.put(hour, count.sum()));
        return hourly;
    }

    /**
     * 获取最近的发送记录
     *
     * @param limit 限制数量
     * @return 发送记录列表
     */
    public List<SendRecord> getRecentSendHistory(int limit) {
        List<SendRecord> recent = new ArrayList<>();
        synchronized (this) {
            Iterator<SendRecord> iterator = ((LinkedList<SendRecord>) sendHistory).descendingIterator();
            int count = 0;
            while (iterator.hasNext() && count < limit) {
                recent.add(iterator.next());
                count++;
            }
        }
        return recent;
    }

    /**
     * 获取发送趋势（最近24小时）
     *
     * @return 发送趋势数据
     */
    public Map<String, Object> getSendTrend() {
        Map<String, Object> trend = new HashMap<>();

        LocalDateTime now = LocalDateTime.now();
        Map<String, Integer> hourlyTrend = new LinkedHashMap<>();

        // 生成最近24小时的数据
        for (int i = 23; i >= 0; i--) {
            LocalDateTime hour = now.minusHours(i);
            String hourKey = hour.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH"));
            String displayKey = hour.format(DateTimeFormatter.ofPattern("HH:mm"));

            LongAdder count = hourlyStats.get(hourKey);
            hourlyTrend.put(displayKey, count != null ? (int) count.sum() : 0);
        }

        trend.put("hourlyTrend", hourlyTrend);
        trend.put("peakHour", findPeakHour(hourlyTrend));

        return trend;
    }

    /**
     * 查找发送高峰时段
     */
    private String findPeakHour(Map<String, Integer> hourlyTrend) {
        return hourlyTrend.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("无数据");
    }

    /**
     * 获取健康状态
     *
     * @return 健康状态信息
     */
    public Map<String, Object> getHealthStatus() {
        Map<String, Object> health = new HashMap<>();

        long totalSent = statistics.totalSent.sum();
        long successCount = statistics.successCount.sum();
        double successRate = totalSent > 0 ? (double) successCount / totalSent : 0.0;

        // 健康状态评估
        String status;
        if (successRate >= 0.95) {
            status = "HEALTHY";
        } else if (successRate >= 0.85) {
            status = "WARNING";
        } else {
            status = "CRITICAL";
        }

        health.put("status", status);
        health.put("successRate", successRate);
        health.put("totalSent", totalSent);
        health.put("recentErrors", getRecentErrors());

        return health;
    }

    /**
     * 获取最近的错误信息
     */
    private List<String> getRecentErrors() {
        List<String> errors = new ArrayList<>();
        synchronized (this) {
            sendHistory.stream()
                    .filter(record -> !record.isSuccess())
                    .limit(5)
                    .forEach(record -> {
                        if (record.getErrorMessage() != null) {
                            errors.add(record.getErrorMessage());
                        }
                    });
        }
        return errors;
    }

    /**
     * 重置统计信息
     */
    public void resetStatistics() {
        statistics.reset();
        errorStats.clear();
        hourlyStats.clear();
        synchronized (this) {
            sendHistory.clear();
        }
        monitorStartTime = LocalDateTime.now();
        log.info("邮件发送监控统计信息已重置");
    }

    /**
     * 生成监控报告
     *
     * @return 监控报告字符串
     */
    public String generateReport() {
        StringBuilder report = new StringBuilder();
        report.append("=== 邮件发送监控报告 ===\n");
        report.append("生成时间: ").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("\n");
        report.append("监控开始时间: ").append(monitorStartTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("\n\n");

        Map<String, Object> stats = getStatistics();
        report.append("基础统计:\n");
        report.append(String.format("  总发送数: %d\n", stats.get("totalSent")));
        report.append(String.format("  成功数: %d\n", stats.get("successCount")));
        report.append(String.format("  失败数: %d\n", stats.get("failureCount")));
        report.append(String.format("  成功率: %.2f%%\n", (Double) stats.get("successRate") * 100));
        report.append(String.format("  平均耗时: %.2f ms\n", stats.get("averageDuration")));
        report.append("\n");

        Map<String, Long> errorStats = getErrorStatistics();
        if (!errorStats.isEmpty()) {
            report.append("错误统计:\n");
            errorStats.forEach((errorType, count) ->
                    report.append(String.format("  %s: %d\n", errorType, count)));
            report.append("\n");
        }

        Map<String, Object> health = getHealthStatus();
        report.append("健康状态: ").append(health.get("status")).append("\n");

        return report.toString();
    }

    /**
     * 发送统计信息内部类
     */
    @Data
    private static class SendStatistics {
        private final LongAdder totalSent = new LongAdder();
        private final LongAdder successCount = new LongAdder();
        private final LongAdder failureCount = new LongAdder();
        private final LongAdder totalRecipients = new LongAdder();
        private final LongAdder totalDuration = new LongAdder();
        private final AtomicLong maxDuration = new AtomicLong(0);
        private final AtomicLong minDuration = new AtomicLong(Long.MAX_VALUE);
        private final LongAdder batchSendCount = new LongAdder();
        private final LongAdder batchSuccessCount = new LongAdder();
        private final LongAdder batchFailureCount = new LongAdder();

        public void reset() {
            totalSent.reset();
            successCount.reset();
            failureCount.reset();
            totalRecipients.reset();
            totalDuration.reset();
            maxDuration.set(0);
            minDuration.set(Long.MAX_VALUE);
            batchSendCount.reset();
            batchSuccessCount.reset();
            batchFailureCount.reset();
        }
    }

    /**
     * 发送记录内部类
     */
    @Data
    public static class SendRecord {
        private LocalDateTime timestamp;
        private boolean success;
        private String subject;
        private int recipientCount;
        private long duration;
        private String errorMessage;
        private String errorCode;
        private boolean batchSend;
    }
}