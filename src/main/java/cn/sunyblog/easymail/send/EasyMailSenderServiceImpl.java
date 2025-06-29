package cn.sunyblog.easymail.send;

import cn.sunyblog.easymail.api.EasyMailSenderService;
import cn.sunyblog.easymail.api.EasyMailRequest;
import cn.sunyblog.easymail.config.EasyMailSmtpConfig;
import cn.sunyblog.easymail.exception.EasyMailException;
import cn.sunyblog.easymail.exception.EasyMailExceptionHandler;
import cn.sunyblog.easymail.exception.EasyMailTemplateException;
import cn.sunyblog.easymail.exception.EasyMailValidationException;
import cn.sunyblog.easymail.send.template.EasyMailSendTemplate;
import cn.sunyblog.easymail.send.template.EasyMailSendTemplateManager;
import cn.sunyblog.easymail.send.event.EasyMailSendEventListener;
import cn.sunyblog.easymail.send.monitor.EasyMailSendMonitor;
import cn.sunyblog.easymail.send.strategy.EasyMailSendStrategyManager;
import cn.sunyblog.easymail.send.schedule.EasyMailScheduleManager;
import cn.sunyblog.easymail.send.schedule.EasyMailScheduledTask;
import cn.sunyblog.easymail.util.EasyMailRetryUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.mail.*;
import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/**
 * 企业级邮件发送服务实现类
 * 提供完整的邮件发送功能，包括重试机制、模板支持、异步发送等
 *
 * @author suny
 * @version 1.0.0
 */
@Slf4j
@Service
public class EasyMailSenderServiceImpl implements EasyMailSenderService {

    @Resource
    private EasyMailSender easyMailSender;

    @Resource
    private EasyMailSmtpConfig easyMailSmtpConfig;

    @Resource
    private ThreadPoolExecutor executor;

    @Resource
    private EasyMailSendStrategyManager strategyManager;

    @Resource
    private EasyMailSendTemplateManager templateManager;

    @Resource
    private EasyMailSendMonitor sendMonitor;

    @Resource
    private ApplicationEventPublisher eventPublisher;

    @Resource
    private EasyMailScheduleManager scheduleManager;

    // 统计信息
    private final AtomicLong totalSentCount = new AtomicLong(0);
    private final AtomicLong totalFailedCount = new AtomicLong(0);
    private final AtomicLong totalRetryCount = new AtomicLong(0);
    private final AtomicLong totalDuration = new AtomicLong(0);

    // ==================== 新的Builder模式API实现 ====================

    @Override
    public EasyMailSendResult send(EasyMailRequest request) {
        long startTime = System.currentTimeMillis();

        try {
            // 验证请求并抛出异常
            request.validateAndThrow();

            // 如果是模板邮件
            if (request.isTemplate()) {
                EasyMailSendTemplate template = templateManager.getTemplate(request.getTemplateId());
                if (template == null) {
                    throw EasyMailTemplateException.templateNotFound(request.getTemplateId());
                }

                try {
                    String subject = template.generateSubject(request.getTemplateVariables());
                    String content = template.generateContent(request.getTemplateVariables());

                    return sendInternal(request.getActualToList(), request.getCcList(), request.getBccList(),
                            subject, content, template.isHtml(),
                            mergeAttachments(request.getAttachments(), template.getDefaultAttachments()));
                } catch (Exception e) {
                    throw EasyMailTemplateException.renderError(request.getTemplateId(), e);
                }
            }

            // 普通邮件
            return sendInternal(request.getActualToList(), request.getCcList(), request.getBccList(),
                    request.getSubject(), request.getContent(), request.isHtml(),
                    request.getAttachments());

        } catch (EasyMailException e) {
            // 已经是EmailException，直接处理
            long duration = System.currentTimeMillis() - startTime;
            EasyMailExceptionHandler.logException(e, "邮件发送");
            return EasyMailSendResult.failure(request.getToList(), request.getSubject(),
                    e.getFullErrorMessage(), e.getErrorCode(), getStackTrace(e), duration);
        } catch (Exception e) {
            // 包装其他异常
            long duration = System.currentTimeMillis() - startTime;
            EasyMailException wrappedException =
                    EasyMailExceptionHandler.wrapException(e, "邮件发送");
            EasyMailExceptionHandler.logException(wrappedException, "邮件发送");
            return EasyMailSendResult.failure(request.getToList(), request.getSubject(),
                    wrappedException.getFullErrorMessage(), wrappedException.getErrorCode(), getStackTrace(e), duration);
        }
    }

    @Override
    public CompletableFuture<EasyMailSendResult> sendAsync(EasyMailRequest request) {
        if (request.isAsync()) {
            return CompletableFuture.supplyAsync(() -> send(request), executor);
        }
        return CompletableFuture.completedFuture(send(request));
    }

    /**
     * 合并附件列表
     */
    private List<File> mergeAttachments(List<File> requestAttachments, List<File> templateAttachments) {
        List<File> merged = new ArrayList<>();
        if (requestAttachments != null) {
            merged.addAll(requestAttachments);
        }
        if (templateAttachments != null) {
            merged.addAll(templateAttachments);
        }
        return merged.isEmpty() ? null : merged;
    }

    // ==================== 批量发送方法 ====================

    @Override
    public List<EasyMailSendResult> sendBatch(List<EasyMailRequest> requests) {
        List<EasyMailSendResult> results = new ArrayList<>();
        for (EasyMailRequest request : requests) {
            results.add(send(request));
        }
        return results;
    }

    @Override
    public CompletableFuture<List<EasyMailSendResult>> sendBatchAsync(List<EasyMailRequest> requests, Consumer<Integer> callback) {
        return CompletableFuture.supplyAsync(() -> {
            List<EasyMailSendResult> results = sendBatch(requests);
            if (callback != null) {
                int successCount = (int) results.stream().mapToInt(r -> r.isSuccess() ? 1 : 0).sum();
                callback.accept(successCount);
            }
            return results;
        }, executor);
    }

    // ==================== 核心发送方法 ====================

    /**
     * 内部发送方法（核心实现）
     */
    private EasyMailSendResult sendInternal(List<String> toList, List<String> ccList, List<String> bccList,
                                            String subject, String content, boolean isHtml, List<File> attachments) {
        long startTime = System.currentTimeMillis();

        try {
            // 参数验证
            if (toList == null || toList.isEmpty()) {
                throw EasyMailValidationException.emptyRecipients();
            }
            if (subject == null || subject.trim().isEmpty()) {
                throw EasyMailValidationException.emptySubject();
            }
            if (content == null || content.trim().isEmpty()) {
                throw EasyMailValidationException.emptyContent();
            }

            // 验证邮箱格式
            for (String email : toList) {
                if (!EasyMailExceptionHandler.isValidEmail(email)) {
                    throw EasyMailValidationException.invalidEmailAddresses(Collections.singletonList(email));
                }
            }
            if (ccList != null) {
                for (String email : ccList) {
                    if (!EasyMailExceptionHandler.isValidEmail(email)) {
                        throw EasyMailValidationException.invalidEmailAddresses(Collections.singletonList(email));
                    }
                }
            }
            if (bccList != null) {
                for (String email : bccList) {
                    if (!EasyMailExceptionHandler.isValidEmail(email)) {
                        throw EasyMailValidationException.invalidEmailAddresses(Collections.singletonList(email));
                    }
                }
            }

            // 验证附件
            if (attachments != null) {
                for (File attachment : attachments) {
                    if (attachment == null) {
                        throw EasyMailValidationException.missingAttachments(Collections.singletonList("附件不能为null"));
                    }
                    if (!attachment.exists()) {
                        throw EasyMailValidationException.missingAttachments(Collections.singletonList(attachment.getPath()));
                    }
                    if (!attachment.isFile()) {
                        throw EasyMailValidationException.missingAttachments(Collections.singletonList(attachment.getPath() + " 不是文件"));
                    }
                    if (!attachment.canRead()) {
                        throw EasyMailValidationException.missingAttachments(Collections.singletonList(attachment.getPath() + " 无法读取"));
                    }
                }
            }

            // 发布邮件发送开始事件
            eventPublisher.publishEvent(new EasyMailSendEventListener.EmailSendStartEvent(subject, toList, "StrategyManager"));

            // 使用策略管理器发送邮件
            EasyMailSendResult result = strategyManager.sendEmail(toList, ccList, bccList, subject, content, isHtml, attachments);

            // 更新统计信息
            if (result.isSuccess()) {
                totalSentCount.incrementAndGet();
            } else {
                totalFailedCount.incrementAndGet();
            }
            totalDuration.addAndGet(result.getDuration());

            // 发布邮件发送完成事件
            eventPublisher.publishEvent(new EasyMailSendEventListener.EmailSendCompleteEvent(result, "StrategyManager"));

            return result;

        } catch (EasyMailException e) {
            // 已经是EmailException，直接处理
            long duration = System.currentTimeMillis() - startTime;
            totalFailedCount.incrementAndGet();
            totalDuration.addAndGet(duration);
            EasyMailExceptionHandler.logException(e, "邮件发送");

            EasyMailSendResult failureResult = EasyMailSendResult.failure(toList, subject, e.getFullErrorMessage(),
                    e.getErrorCode(), getStackTrace(e), duration);
            eventPublisher.publishEvent(new EasyMailSendEventListener.EmailSendFailureEvent(
                    subject, toList, e.getErrorCode(), e.getFullErrorMessage(), getStackTrace(e), 0));
            return failureResult;

        } catch (Exception e) {
            // 包装其他异常
            long duration = System.currentTimeMillis() - startTime;
            totalFailedCount.incrementAndGet();
            totalDuration.addAndGet(duration);

            EasyMailException wrappedException =
                    EasyMailExceptionHandler.wrapException(e, "邮件发送");
            EasyMailExceptionHandler.logException(wrappedException, "邮件发送");

            EasyMailSendResult failureResult = EasyMailSendResult.failure(toList, subject, wrappedException.getFullErrorMessage(),
                    wrappedException.getErrorCode(), getStackTrace(e), duration);
            eventPublisher.publishEvent(new EasyMailSendEventListener.EmailSendFailureEvent(
                    subject, toList, wrappedException.getErrorCode(), wrappedException.getFullErrorMessage(), getStackTrace(e), 0));
            return failureResult;
        }
    }

    // ==================== 模板邮件发送方法 ====================

    @Override
    public EasyMailSendResult sendWithTemplate(String to, EasyMailSendTemplate template, Map<String, Object> variables) throws Exception {
        if (template == null || !template.isValid()) {
            throw EasyMailTemplateException.invalidEmailTemplate();
        }

        // 发布模板使用事件
        eventPublisher.publishEvent(new EasyMailSendEventListener.TemplateUsageEvent(
                template.getTemplateName(), variables != null ? variables.size() : 0));

        String subject = template.generateSubject(variables);
        String content = template.generateContent(variables);

        return sendInternal(Collections.singletonList(to), null, null, subject, content, template.isHtml(), template.getDefaultAttachments());
    }

    @Override
    public EasyMailSendResult sendBatchWithTemplate(Map<String, Map<String, Object>> recipients, EasyMailSendTemplate template) throws Exception {
        if (template == null || !template.isValid()) {
            throw EasyMailTemplateException.invalidBathEmailTemplate();
        }

        long startTime = System.currentTimeMillis();
        Map<String, Boolean> batchResults = new HashMap<>();

        for (Map.Entry<String, Map<String, Object>> entry : recipients.entrySet()) {
            String to = entry.getKey();
            Map<String, Object> variables = entry.getValue();

            try {
                String subject = template.generateSubject(variables);
                String content = template.generateContent(variables);

                EasyMailSendResult result = sendInternal(Collections.singletonList(to), null, null, subject, content,
                        template.isHtml(), template.getDefaultAttachments());
                batchResults.put(to, result.isSuccess());

            } catch (Exception e) {
                log.error("发送模板邮件给 {} 失败: {}", to, e.getMessage(), e);
                batchResults.put(to, false);
            }
        }

        long duration = System.currentTimeMillis() - startTime;
        return EasyMailSendResult.batchResult(batchResults, "批量模板邮件", duration);
    }

    // ==================== 批量定时发送方法实现 ====================

    @Override
    public String sendBatchScheduled(List<EasyMailRequest> requests, String cronExpression) {
        return sendBatchScheduled(requests, cronExpression, "BatchScheduledTask-" + System.currentTimeMillis());
    }

    @Override
    public String sendBatchScheduled(List<EasyMailRequest> requests, String cronExpression, String taskName) {
        return scheduleManager.createBatchScheduledTask(requests, cronExpression, taskName);
    }

    @Override
    public String sendBatchDelayed(List<EasyMailRequest> requests, long delayMillis) {
        return sendBatchDelayed(requests, delayMillis, "BatchDelayedTask-" + System.currentTimeMillis());
    }

    @Override
    public String sendBatchDelayed(List<EasyMailRequest> requests, long delayMillis, String taskName) {
        return scheduleManager.createBatchDelayedTask(requests, delayMillis, taskName);
    }

    @Override
    public String sendBatchAtTime(List<EasyMailRequest> requests, LocalDateTime executeTime) {
        return sendBatchAtTime(requests, executeTime, "BatchAtTimeTask-" + System.currentTimeMillis());
    }

    @Override
    public String sendBatchAtTime(List<EasyMailRequest> requests, LocalDateTime executeTime, String taskName) {
        return scheduleManager.createBatchAtTimeTask(requests, executeTime, taskName);
    }

    // ==================== 状态查询方法 ====================

    @Override
    public String getSendingStats() {
        Map<String, Object> stats = sendMonitor.getStatistics();
        return String.format(
                "邮件发送统计 - 成功: %s, 失败: %s, 平均耗时: %.2fms, 成功率: %.2f%%",
                stats.get("successCount"), stats.get("failureCount"),
                stats.get("averageDuration"), (Double) stats.get("successRate") * 100
        );
    }

    @Override
    public String getThreadPoolStatus() {
        return easyMailSender.getThreadPoolStatus();
    }

    /**
     * 获取详细统计信息
     */
    @Override
    public Map<String, Object> getDetailedStatistics() {
        return sendMonitor.getStatistics();
    }

    /**
     * 获取策略管理器统计信息
     */
    @Override
    public Map<String, Object> getStrategyStatistics() {
        return strategyManager.getStrategyInfo();
    }

    /**
     * 获取监控健康状态
     */
    @Override
    public Map<String, Object> getHealthStatus() {
        return sendMonitor.getHealthStatus();
    }

    /**
     * 获取发送趋势
     */
    @Override
    public Map<String, Object> getSendTrend() {
        return sendMonitor.getSendTrend();
    }

    /**
     * 生成监控报告
     */
    @Override
    public String generateMonitorReport() {
        return sendMonitor.generateReport();
    }

    /**
     * 重置统计信息
     */
    @Override
    public void resetStatistics() {
        sendMonitor.resetStatistics();
        strategyManager.resetStatistics();
        totalSentCount.set(0);
        totalFailedCount.set(0);
        totalRetryCount.set(0);
        totalDuration.set(0);
    }

    @Override
    public boolean checkConnection() {
        try {
            Session session = easyMailSender.createSession();
            Transport transport = session.getTransport("smtp");
            transport.connect(easyMailSmtpConfig.getServer(), Integer.parseInt(easyMailSmtpConfig.getPort()),
                    easyMailSmtpConfig.getUsername(), easyMailSmtpConfig.getPassword());
            transport.close();
            return true;
        } catch (Exception e) {
            log.error("检查邮件服务器连接失败: {}", e.getMessage(), e);
            return false;
        }
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 发送单封邮件
     */
    private EasyMailSendResult sendSingle(String to, List<String> ccList, List<String> bccList,
                                          String subject, String content, boolean isHtml,
                                          List<File> attachments, long startTime) {

        // 构建收件人列表
        List<String> allRecipients = new ArrayList<>();
        allRecipients.add(to);
        if (ccList != null) allRecipients.addAll(ccList);
        if (bccList != null) allRecipients.addAll(bccList);

        // 配置重试策略
        EasyMailRetryUtil.RetryConfig retryConfig = buildRetryConfig();

        try {
            boolean success = EasyMailRetryUtil.executeWithRetry(() -> easyMailSender.sendEmailInternal(to,
                    ccList != null && !ccList.isEmpty() ? String.join(",", ccList) : null,
                    bccList != null && !bccList.isEmpty() ? String.join(",", bccList) : null,
                    subject, content, isHtml, attachments), retryConfig);

            long duration = System.currentTimeMillis() - startTime;

            if (success) {
                totalSentCount.incrementAndGet();
                totalDuration.addAndGet(duration);

                String messageId = generateMessageId();
                return EasyMailSendResult.success(messageId, allRecipients, subject, duration);
            } else {
                totalFailedCount.incrementAndGet();
                totalDuration.addAndGet(duration);

                return EasyMailSendResult.failure(allRecipients, subject, "邮件发送失败", duration);
            }

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            totalFailedCount.incrementAndGet();
            totalDuration.addAndGet(duration);

            return EasyMailSendResult.failure(allRecipients, subject, e.getMessage(),
                    getErrorCode(e), getStackTrace(e), duration);
        }
    }

    /**
     * 批量发送邮件
     */
    private EasyMailSendResult sendBatch(List<String> toList, List<String> ccList, List<String> bccList,
                                         String subject, String content, boolean isHtml,
                                         List<File> attachments, long startTime) {

        Map<String, Boolean> batchResults = new HashMap<>();
        AtomicInteger successCount = new AtomicInteger(0);

        // 并行发送
        List<CompletableFuture<Void>> futures = new ArrayList<>();

        for (String to : toList) {
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                try {
                    EasyMailSendResult result = sendSingle(to, ccList, bccList, subject, content, isHtml, attachments, System.currentTimeMillis());
                    batchResults.put(to, result.isSuccess());
                    if (result.isSuccess()) {
                        successCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    log.error("批量发送邮件给 {} 失败: {}", to, e.getMessage(), e);
                    batchResults.put(to, false);
                }
            }, executor);

            futures.add(future);
        }

        // 等待所有发送完成
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        long duration = System.currentTimeMillis() - startTime;
        return EasyMailSendResult.batchResult(batchResults, subject, duration);
    }

    /**
     * 构建重试配置
     */
    private EasyMailRetryUtil.RetryConfig buildRetryConfig() {
        if (!easyMailSmtpConfig.getRetry().isEnabled()) {
            return EasyMailRetryUtil.RetryConfig.defaults().maxAttempts(1);
        }

        return EasyMailRetryUtil.RetryConfig.defaults()
                .maxAttempts(easyMailSmtpConfig.getRetry().getMaxRetries() + 1)
                .initialDelayMs(easyMailSmtpConfig.getRetry().getInitialDelayMs())
                .maxDelayMs(easyMailSmtpConfig.getRetry().getMaxDelayMs())
                .useExponentialBackoff(easyMailSmtpConfig.getRetry().isUseExponentialBackoff())
                .backoffMultiplier(easyMailSmtpConfig.getRetry().getBackoffMultiplier())
                .retryableExceptions(
                        MessagingException.class,
                        SendFailedException.class,
                        javax.mail.MessagingException.class
                );
    }

    /**
     * 生成邮件ID
     */
    private String generateMessageId() {
        return "msg_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8);
    }

    /**
     * 获取错误代码
     */
    private String getErrorCode(Exception e) {
        if (e instanceof MessagingException) {
            return "MESSAGING_ERROR";
        } else {
            return "UNKNOWN_ERROR";
        }
    }

    /**
     * 获取异常堆栈信息
     */
    private String getStackTrace(Exception e) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        return sw.toString();
    }

    // ==================== 定时发送方法实现 ====================

    @Override
    public String sendScheduled(EasyMailRequest request, String cronExpression) {
        return sendScheduled(request, cronExpression, null);
    }

    @Override
    public String sendScheduled(EasyMailRequest request, String cronExpression, String taskName) {
        EasyMailScheduledTask task = EasyMailScheduledTask.builder()
                .taskName(taskName != null ? taskName : "Cron邮件任务")
                .mailRequest(request)
                .scheduleType(EasyMailScheduledTask.ScheduleType.CRON)
                .cronExpression(cronExpression)
                .build();

        return scheduleManager.addTask(task);
    }

    @Override
    public String sendDelayed(EasyMailRequest request, long delayMillis) {
        return sendDelayed(request, delayMillis, null);
    }

    @Override
    public String sendDelayed(EasyMailRequest request, long delayMillis, String taskName) {
        EasyMailScheduledTask task = EasyMailScheduledTask.builder()
                .taskName(taskName != null ? taskName : "延迟邮件任务")
                .mailRequest(request)
                .scheduleType(EasyMailScheduledTask.ScheduleType.DELAY)
                .delayMillis(delayMillis)
                .build();

        return scheduleManager.addTask(task);
    }

    @Override
    public String sendAtFixedRate(EasyMailRequest request, long fixedRateMillis) {
        return sendAtFixedRate(request, fixedRateMillis, null);
    }

    @Override
    public String sendAtFixedRate(EasyMailRequest request, long fixedRateMillis, String taskName) {
        EasyMailScheduledTask task = EasyMailScheduledTask.builder()
                .taskName(taskName != null ? taskName : "固定频率邮件任务")
                .mailRequest(request)
                .scheduleType(EasyMailScheduledTask.ScheduleType.FIXED_RATE)
                .fixedRateMillis(fixedRateMillis)
                .build();

        return scheduleManager.addTask(task);
    }

    @Override
    public String sendWithFixedDelay(EasyMailRequest request, long fixedDelayMillis) {
        return sendWithFixedDelay(request, fixedDelayMillis, null);
    }

    @Override
    public String sendWithFixedDelay(EasyMailRequest request, long fixedDelayMillis, String taskName) {
        EasyMailScheduledTask task = EasyMailScheduledTask.builder()
                .taskName(taskName != null ? taskName : "固定延迟邮件任务")
                .mailRequest(request)
                .scheduleType(EasyMailScheduledTask.ScheduleType.FIXED_DELAY)
                .fixedDelayMillis(fixedDelayMillis)
                .build();

        return scheduleManager.addTask(task);
    }

    @Override
    public String sendAtTime(EasyMailRequest request, LocalDateTime executeTime) {
        return sendAtTime(request, executeTime, null);
    }

    @Override
    public String sendAtTime(EasyMailRequest request, LocalDateTime executeTime, String taskName) {
        EasyMailScheduledTask task = EasyMailScheduledTask.builder()
                .taskName(taskName != null ? taskName : "定时邮件任务")
                .mailRequest(request)
                .scheduleType(EasyMailScheduledTask.ScheduleType.AT_TIME)
                .executeTime(executeTime)
                .build();

        return scheduleManager.addTask(task);
    }

    // ==================== 定时任务管理方法实现 ====================

    @Override
    public boolean cancelScheduledTask(String taskId) {
        return scheduleManager.cancelTask(taskId);
    }

    @Override
    public boolean removeScheduledTask(String taskId) {
        return scheduleManager.removeTask(taskId);
    }

    @Override
    public EasyMailScheduledTask getScheduledTask(String taskId) {
        return scheduleManager.getTask(taskId);
    }

    @Override
    public List<EasyMailScheduledTask> getAllScheduledTasks() {
        return scheduleManager.getAllTasks();
    }

    @Override
    public List<EasyMailScheduledTask> getScheduledTasksByStatus(EasyMailScheduledTask.TaskStatus status) {
        return scheduleManager.getTasksByStatus(status);
    }

    @Override
    public List<EasyMailScheduledTask> getRunningScheduledTasks() {
        return scheduleManager.getRunningTasks();
    }

    @Override
    public void cancelAllScheduledTasks() {
        scheduleManager.cancelAllTasks();
    }

    @Override
    public void cleanupCompletedScheduledTasks() {
        scheduleManager.cleanupCompletedTasks();
    }

    @Override
    public Map<String, Object> getScheduledTaskStatistics() {
        return scheduleManager.getTaskStatistics();
    }

    @Override
    public boolean scheduledTaskExists(String taskId) {
        return scheduleManager.taskExists(taskId);
    }

    @Override
    public int getScheduledTaskCount() {
        return scheduleManager.getTaskCount();
    }
}