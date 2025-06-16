package cn.sunyblog.easymail.send.event;

import cn.sunyblog.easymail.send.SendResult;
import cn.sunyblog.easymail.send.monitor.EmailSendMonitor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 邮件发送事件监听器
 * 监听邮件发送过程中的各种事件，提供日志记录、监控统计等功能
 *
 * @author suny
 * @version 1.0.0
 */
@Slf4j
@Component
public class EmailSendEventListener {

    @Resource
    private EmailSendMonitor emailSendMonitor;

    /**
     * 监听邮件发送开始事件
     */
    @EventListener
    @Async
    public void handleEmailSendStartEvent(EmailSendStartEvent event) {
        log.info("邮件发送开始 - 主题: {}, 收件人数: {}, 策略: {}",
                event.getSubject(),
                event.getRecipientCount(),
                event.getStrategy());

        // 可以在这里添加发送前的预处理逻辑
        if (event.getRecipientCount() > 100) {
            log.warn("大批量邮件发送警告 - 收件人数: {}", event.getRecipientCount());
        }
    }

    /**
     * 监听邮件发送完成事件
     */
    @EventListener
    @Async
    public void handleEmailSendCompleteEvent(EmailSendCompleteEvent event) {
        SendResult result = event.getResult();

        if (result.isSuccess()) {
            log.info("邮件发送成功 - 主题: {}, 耗时: {}ms, 策略: {}",
                    result.getSubject(),
                    result.getDuration(),
                    event.getStrategy());
        } else {
            log.error("邮件发送失败 - 主题: {}, 错误: {}, 策略: {}",
                    result.getSubject(),
                    result.getErrorMessage(),
                    event.getStrategy());
        }

        // 记录到监控系统
        emailSendMonitor.recordSendResult(result);

        // 批量发送特殊处理
        if (result.isBatchSend()) {
            log.info("批量邮件发送完成 - 成功: {}/{}, 总耗时: {}ms",
                    result.getSuccessCount(),
                    result.getTotalCount(),
                    result.getDuration());
        }
    }

    /**
     * 监听邮件发送失败事件
     */
    @EventListener
    @Async
    public void handleEmailSendFailureEvent(EmailSendFailureEvent event) {
        log.error("邮件发送失败事件 - 主题: {}, 错误类型: {}, 错误信息: {}, 重试次数: {}",
                event.getSubject(),
                event.getErrorType(),
                event.getErrorMessage(),
                event.getRetryCount());

        // 如果是严重错误，可以发送告警
        if (isCriticalError(event.getErrorType())) {
            sendAlert(event);
        }

        // 记录失败详情
        recordFailureDetails(event);
    }

    /**
     * 监听邮件重试事件
     */
    @EventListener
    @Async
    public void handleEmailRetryEvent(EmailRetryEvent event) {
        log.warn("邮件发送重试 - 主题: {}, 重试次数: {}/{}, 延迟: {}ms, 原因: {}",
                event.getSubject(),
                event.getCurrentRetry(),
                event.getMaxRetries(),
                event.getRetryDelay(),
                event.getRetryReason());

        // 如果重试次数过多，记录警告
        if (event.getCurrentRetry() >= event.getMaxRetries() * 0.8) {
            log.warn("邮件重试次数接近上限 - 主题: {}, 当前重试: {}/{}",
                    event.getSubject(),
                    event.getCurrentRetry(),
                    event.getMaxRetries());
        }
    }

    /**
     * 监听批量邮件发送进度事件
     */
    @EventListener
    @Async
    public void handleBatchSendProgressEvent(BatchSendProgressEvent event) {
        log.info("批量邮件发送进度 - 已完成: {}/{}, 成功: {}, 失败: {}, 进度: {:.1f}%",
                event.getCompletedCount(),
                event.getTotalCount(),
                event.getSuccessCount(),
                event.getFailureCount(),
                event.getProgressPercentage());

        // 每完成25%记录一次里程碑
        double progress = event.getProgressPercentage();
        if (progress >= 25 && progress < 26 ||
                progress >= 50 && progress < 51 ||
                progress >= 75 && progress < 76 ||
                progress >= 100) {
            log.info("批量发送里程碑 - {}% 完成", (int) progress);
        }
    }

    /**
     * 监听邮件模板使用事件
     */
    @EventListener
    @Async
    public void handleTemplateUsageEvent(TemplateUsageEvent event) {
        log.debug("邮件模板使用 - 模板: {}, 变量数: {}",
                event.getTemplateName(),
                event.getVariableCount());

        // 可以在这里统计模板使用情况
    }

    /**
     * 判断是否为严重错误
     */
    private boolean isCriticalError(String errorType) {
        return errorType != null && (
                errorType.contains("AUTHENTICATION") ||
                        errorType.contains("CONNECTION") ||
                        errorType.contains("TIMEOUT") ||
                        errorType.contains("SSL")
        );
    }

    /**
     * 发送告警
     */
    private void sendAlert(EmailSendFailureEvent event) {
        // 这里可以实现告警逻辑，比如发送钉钉消息、短信等
        log.error("严重邮件发送错误告警 - 错误类型: {}, 时间: {}",
                event.getErrorType(),
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
    }

    /**
     * 记录失败详情
     */
    private void recordFailureDetails(EmailSendFailureEvent event) {
        // 可以将失败详情记录到数据库或文件中，用于后续分析
        log.debug("记录邮件发送失败详情 - 主题: {}, 收件人: {}, 堆栈: {}",
                event.getSubject(),
                event.getRecipients(),
                event.getStackTrace());
    }

    /**
     * 邮件发送开始事件
     */
    public static class EmailSendStartEvent {
        private final String subject;
        private final List<String> recipients;
        private final int recipientCount;
        private final String strategy;
        private final LocalDateTime timestamp;

        public EmailSendStartEvent(String subject, List<String> recipients, String strategy) {
            this.subject = subject;
            this.recipients = recipients;
            this.recipientCount = recipients != null ? recipients.size() : 0;
            this.strategy = strategy;
            this.timestamp = LocalDateTime.now();
        }

        // Getters
        public String getSubject() {
            return subject;
        }

        public List<String> getRecipients() {
            return recipients;
        }

        public int getRecipientCount() {
            return recipientCount;
        }

        public String getStrategy() {
            return strategy;
        }

        public LocalDateTime getTimestamp() {
            return timestamp;
        }
    }

    /**
     * 邮件发送完成事件
     */
    public static class EmailSendCompleteEvent {
        private final SendResult result;
        private final String strategy;
        private final LocalDateTime timestamp;

        public EmailSendCompleteEvent(SendResult result, String strategy) {
            this.result = result;
            this.strategy = strategy;
            this.timestamp = LocalDateTime.now();
        }

        // Getters
        public SendResult getResult() {
            return result;
        }

        public String getStrategy() {
            return strategy;
        }

        public LocalDateTime getTimestamp() {
            return timestamp;
        }
    }

    /**
     * 邮件发送失败事件
     */
    public static class EmailSendFailureEvent {
        private final String subject;
        private final List<String> recipients;
        private final String errorType;
        private final String errorMessage;
        private final String stackTrace;
        private final int retryCount;
        private final LocalDateTime timestamp;

        public EmailSendFailureEvent(String subject, List<String> recipients, String errorType,
                                     String errorMessage, String stackTrace, int retryCount) {
            this.subject = subject;
            this.recipients = recipients;
            this.errorType = errorType;
            this.errorMessage = errorMessage;
            this.stackTrace = stackTrace;
            this.retryCount = retryCount;
            this.timestamp = LocalDateTime.now();
        }

        // Getters
        public String getSubject() {
            return subject;
        }

        public List<String> getRecipients() {
            return recipients;
        }

        public String getErrorType() {
            return errorType;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        public String getStackTrace() {
            return stackTrace;
        }

        public int getRetryCount() {
            return retryCount;
        }

        public LocalDateTime getTimestamp() {
            return timestamp;
        }
    }

    /**
     * 邮件重试事件
     */
    public static class EmailRetryEvent {
        private final String subject;
        private final int currentRetry;
        private final int maxRetries;
        private final long retryDelay;
        private final String retryReason;
        private final LocalDateTime timestamp;

        public EmailRetryEvent(String subject, int currentRetry, int maxRetries,
                               long retryDelay, String retryReason) {
            this.subject = subject;
            this.currentRetry = currentRetry;
            this.maxRetries = maxRetries;
            this.retryDelay = retryDelay;
            this.retryReason = retryReason;
            this.timestamp = LocalDateTime.now();
        }

        // Getters
        public String getSubject() {
            return subject;
        }

        public int getCurrentRetry() {
            return currentRetry;
        }

        public int getMaxRetries() {
            return maxRetries;
        }

        public long getRetryDelay() {
            return retryDelay;
        }

        public String getRetryReason() {
            return retryReason;
        }

        public LocalDateTime getTimestamp() {
            return timestamp;
        }
    }

    /**
     * 批量发送进度事件
     */
    public static class BatchSendProgressEvent {
        private final int completedCount;
        private final int totalCount;
        private final int successCount;
        private final int failureCount;
        private final double progressPercentage;
        private final LocalDateTime timestamp;

        public BatchSendProgressEvent(int completedCount, int totalCount,
                                      int successCount, int failureCount) {
            this.completedCount = completedCount;
            this.totalCount = totalCount;
            this.successCount = successCount;
            this.failureCount = failureCount;
            this.progressPercentage = totalCount > 0 ? (double) completedCount / totalCount * 100 : 0;
            this.timestamp = LocalDateTime.now();
        }

        // Getters
        public int getCompletedCount() {
            return completedCount;
        }

        public int getTotalCount() {
            return totalCount;
        }

        public int getSuccessCount() {
            return successCount;
        }

        public int getFailureCount() {
            return failureCount;
        }

        public double getProgressPercentage() {
            return progressPercentage;
        }

        public LocalDateTime getTimestamp() {
            return timestamp;
        }
    }

    /**
     * 模板使用事件
     */
    public static class TemplateUsageEvent {
        private final String templateName;
        private final int variableCount;
        private final LocalDateTime timestamp;

        public TemplateUsageEvent(String templateName, int variableCount) {
            this.templateName = templateName;
            this.variableCount = variableCount;
            this.timestamp = LocalDateTime.now();
        }

        // Getters
        public String getTemplateName() {
            return templateName;
        }

        public int getVariableCount() {
            return variableCount;
        }

        public LocalDateTime getTimestamp() {
            return timestamp;
        }
    }
}