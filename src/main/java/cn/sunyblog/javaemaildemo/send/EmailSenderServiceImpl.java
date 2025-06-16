package cn.sunyblog.javaemaildemo.send;

import cn.sunyblog.javaemaildemo.api.EmailSenderService;
import cn.sunyblog.javaemaildemo.api.EmailRequest;
import cn.sunyblog.javaemaildemo.config.SmtpConfig;
import cn.sunyblog.javaemaildemo.send.template.EmailTemplate;
import cn.sunyblog.javaemaildemo.send.template.EmailTemplateManager;
import cn.sunyblog.javaemaildemo.send.event.EmailSendEventListener;
import cn.sunyblog.javaemaildemo.send.monitor.EmailSendMonitor;
import cn.sunyblog.javaemaildemo.send.strategy.EmailSendStrategyManager;
import cn.sunyblog.javaemaildemo.util.RetryUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.mail.*;
import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
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
 * @author JavaEmailSpringBoot
 * @version 1.0.0
 */
@Slf4j
@Service
public class EmailSenderServiceImpl implements EmailSenderService {
    
    @Resource
    private MailSender mailSender;
    
    @Resource
    private SmtpConfig smtpConfig;
    
    @Resource
    private ThreadPoolExecutor executor;
    
    @Resource
    private EmailSendStrategyManager strategyManager;
    
    @Resource
    private EmailTemplateManager templateManager;
    
    @Resource
    private EmailSendMonitor sendMonitor;
    
    @Resource
    private ApplicationEventPublisher eventPublisher;
    
    // 统计信息
    private final AtomicLong totalSentCount = new AtomicLong(0);
    private final AtomicLong totalFailedCount = new AtomicLong(0);
    private final AtomicLong totalRetryCount = new AtomicLong(0);
    private final AtomicLong totalDuration = new AtomicLong(0);
    
    // ==================== 新的Builder模式API实现 ====================
    
    @Override
    public SendResult send(EmailRequest request) {
        // 验证请求
        EmailRequest.ValidationResult validation = request.validate();
        if (!validation.isValid()) {
            return SendResult.failure(request.getToList(), request.getSubject(), 
                    validation.getErrorMessage(), 0);
        }
        
        try {
            // 如果是模板邮件
            if (request.isTemplate()) {
                EmailTemplate template = templateManager.getTemplate(request.getTemplateId());
                if (template == null) {
                    return SendResult.failure(request.getToList(), request.getSubject(), 
                            "模板不存在: " + request.getTemplateId(), 0);
                }
                
                String subject = template.generateSubject(request.getTemplateVariables());
                String content = template.generateContent(request.getTemplateVariables());
                
                return send(request.getToList(), request.getCcList(), request.getBccList(),
                        subject, content, template.isHtml(), 
                        mergeAttachments(request.getAttachments(), template.getDefaultAttachments()));
            }
            
            // 普通邮件
            return send(request.getToList(), request.getCcList(), request.getBccList(),
                    request.getSubject(), request.getContent(), request.isHtml(), 
                    request.getAttachments());
                    
        } catch (Exception e) {
            log.error("使用EmailRequest发送邮件失败: {}", e.getMessage(), e);
            return SendResult.failure(request.getToList(), request.getSubject(), 
                    e.getMessage(), 0);
        }
    }
    
    @Override
    public CompletableFuture<SendResult> sendAsync(EmailRequest request) {
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
    
    // ==================== 基础发送方法 ====================
    
    @Override
    public SendResult sendText(String to, String subject, String content) {
        return send(Collections.singletonList(to), null, null, subject, content, false, null);
    }
    
    @Override
    public SendResult sendHtml(String to, String subject, String htmlContent) {
        return send(Collections.singletonList(to), null, null, subject, htmlContent, true, null);
    }
    
    @Override
    public SendResult sendWithAttachments(String to, String subject, String content, boolean isHtml, List<File> attachments) {
        return send(Collections.singletonList(to), null, null, subject, content, isHtml, attachments);
    }
    
    // ==================== 多收件人发送方法 ====================
    
    @Override
    public SendResult sendToMultiple(List<String> toList, String subject, String content, boolean isHtml) {
        return send(toList, null, null, subject, content, isHtml, null);
    }
    
    @Override
    public SendResult send(List<String> toList, List<String> ccList, List<String> bccList, 
                          String subject, String content, boolean isHtml, List<File> attachments) {
        long startTime = System.currentTimeMillis();
        
        try {
            // 参数验证
            if (toList == null || toList.isEmpty()) {
                return SendResult.failure(toList, subject, "收件人列表不能为空", 0);
            }
            
            // 发布邮件发送开始事件
            eventPublisher.publishEvent(new EmailSendEventListener.EmailSendStartEvent(subject, toList, "StrategyManager"));
            
            // 使用策略管理器发送邮件
            SendResult result = strategyManager.sendEmail(toList, ccList, bccList, subject, content, isHtml, attachments);
            
            // 更新统计信息
            if (result.isSuccess()) {
                totalSentCount.incrementAndGet();
            } else {
                totalFailedCount.incrementAndGet();
            }
            totalDuration.addAndGet(result.getDuration());
            
            // 发布邮件发送完成事件
            eventPublisher.publishEvent(new EmailSendEventListener.EmailSendCompleteEvent(result, "StrategyManager"));
            
            return result;
            
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            totalFailedCount.incrementAndGet();
            totalDuration.addAndGet(duration);
            
            log.error("邮件发送异常: {}", e.getMessage(), e);
            
            SendResult failureResult = SendResult.failure(toList, subject, e.getMessage(), getErrorCode(e), getStackTrace(e), duration);
            
            // 发布邮件发送失败事件
            eventPublisher.publishEvent(new EmailSendEventListener.EmailSendFailureEvent(
                subject, toList, getErrorCode(e), e.getMessage(), getStackTrace(e), 0));
            
            return failureResult;
        }
    }
    
    // ==================== 模板邮件发送方法 ====================
    
    @Override
    public SendResult sendWithTemplate(String to, EmailTemplate template, Map<String, Object> variables) {
        if (template == null || !template.isValid()) {
            return SendResult.failure(Collections.singletonList(to), "模板邮件", "邮件模板无效", 0);
        }
        
        // 发布模板使用事件
        eventPublisher.publishEvent(new EmailSendEventListener.TemplateUsageEvent(
            template.getTemplateName(), variables != null ? variables.size() : 0));
        
        String subject = template.generateSubject(variables);
        String content = template.generateContent(variables);
        
        return send(Collections.singletonList(to), null, null, subject, content, template.isHtml(), template.getDefaultAttachments());
    }
    
    @Override
    public SendResult sendBatchWithTemplate(Map<String, Map<String, Object>> recipients, EmailTemplate template) {
        if (template == null || !template.isValid()) {
            return SendResult.failure(new ArrayList<>(recipients.keySet()), "批量模板邮件", "邮件模板无效", 0);
        }
        
        long startTime = System.currentTimeMillis();
        Map<String, Boolean> batchResults = new HashMap<>();
        
        for (Map.Entry<String, Map<String, Object>> entry : recipients.entrySet()) {
            String to = entry.getKey();
            Map<String, Object> variables = entry.getValue();
            
            try {
                String subject = template.generateSubject(variables);
                String content = template.generateContent(variables);
                
                SendResult result = send(Collections.singletonList(to), null, null, subject, content,
                                       template.isHtml(), template.getDefaultAttachments());
                batchResults.put(to, result.isSuccess());
                
            } catch (Exception e) {
                log.error("发送模板邮件给 {} 失败: {}", to, e.getMessage(), e);
                batchResults.put(to, false);
            }
        }
        
        long duration = System.currentTimeMillis() - startTime;
        return SendResult.batchResult(batchResults, "批量模板邮件", duration);
    }
    
    // ==================== 异步发送方法 ====================
    
    @Override
    public CompletableFuture<SendResult> sendTextAsync(String to, String subject, String content) {
        return CompletableFuture.supplyAsync(() -> sendText(to, subject, content), executor);
    }
    
    @Override
    public CompletableFuture<SendResult> sendHtmlAsync(String to, String subject, String htmlContent) {
        return CompletableFuture.supplyAsync(() -> sendHtml(to, subject, htmlContent), executor);
    }
    
    @Override
    public CompletableFuture<SendResult> sendAsync(List<String> toList, List<String> ccList, List<String> bccList,
                                                  String subject, String content, boolean isHtml, List<File> attachments) {
        return CompletableFuture.supplyAsync(() -> 
            send(toList, ccList, bccList, subject, content, isHtml, attachments), executor);
    }
    
    @Override
    public CompletableFuture<SendResult> sendBatchAsync(List<String> toList, String subject, String content, 
                                                       boolean isHtml, Consumer<Integer> callback) {
        return CompletableFuture.supplyAsync(() -> {
            SendResult result = sendToMultiple(toList, subject, content, isHtml);
            if (callback != null) {
                callback.accept(result.getSuccessCount());
            }
            return result;
        }, executor);
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
        return mailSender.getThreadPoolStatus();
    }
    
    /**
     * 获取详细统计信息
     */
    public Map<String, Object> getDetailedStatistics() {
        return sendMonitor.getStatistics();
    }
    
    /**
     * 获取策略管理器统计信息
     */
    public Map<String, Object> getStrategyStatistics() {
        return strategyManager.getStrategyInfo();
    }
    
    /**
     * 获取监控健康状态
     */
    public Map<String, Object> getHealthStatus() {
        return sendMonitor.getHealthStatus();
    }
    
    /**
     * 获取发送趋势
     */
    public Map<String, Object> getSendTrend() {
        return sendMonitor.getSendTrend();
    }
    
    /**
     * 生成监控报告
     */
    public String generateMonitorReport() {
        return sendMonitor.generateReport();
    }
    
    /**
     * 重置统计信息
     */
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
            Session session = mailSender.createSession();
            Transport transport = session.getTransport("smtp");
            transport.connect(smtpConfig.getServer(), Integer.parseInt(smtpConfig.getPort()), 
                            smtpConfig.getUsername(), smtpConfig.getPassword());
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
    private SendResult sendSingle(String to, List<String> ccList, List<String> bccList, 
                                 String subject, String content, boolean isHtml, 
                                 List<File> attachments, long startTime) {
        
        // 构建收件人列表
        List<String> allRecipients = new ArrayList<>();
        allRecipients.add(to);
        if (ccList != null) allRecipients.addAll(ccList);
        if (bccList != null) allRecipients.addAll(bccList);
        
        // 配置重试策略
        RetryUtil.RetryConfig retryConfig = buildRetryConfig();
        
        try {
            boolean success = RetryUtil.executeWithRetry(() -> mailSender.sendEmailInternal(to,
                ccList != null && !ccList.isEmpty() ? String.join(",", ccList) : null,
                bccList != null && !bccList.isEmpty() ? String.join(",", bccList) : null,
                subject, content, isHtml, attachments), retryConfig);
            
            long duration = System.currentTimeMillis() - startTime;
            
            if (success) {
                totalSentCount.incrementAndGet();
                totalDuration.addAndGet(duration);
                
                String messageId = generateMessageId();
                return SendResult.success(messageId, allRecipients, subject, duration);
            } else {
                totalFailedCount.incrementAndGet();
                totalDuration.addAndGet(duration);
                
                return SendResult.failure(allRecipients, subject, "邮件发送失败", duration);
            }
            
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            totalFailedCount.incrementAndGet();
            totalDuration.addAndGet(duration);
            
            return SendResult.failure(allRecipients, subject, e.getMessage(), 
                                    getErrorCode(e), getStackTrace(e), duration);
        }
    }
    
    /**
     * 批量发送邮件
     */
    private SendResult sendBatch(List<String> toList, List<String> ccList, List<String> bccList,
                                String subject, String content, boolean isHtml, 
                                List<File> attachments, long startTime) {
        
        Map<String, Boolean> batchResults = new HashMap<>();
        AtomicInteger successCount = new AtomicInteger(0);
        
        // 并行发送
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        
        for (String to : toList) {
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                try {
                    SendResult result = sendSingle(to, ccList, bccList, subject, content, isHtml, attachments, System.currentTimeMillis());
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
        return SendResult.batchResult(batchResults, subject, duration);
    }
    
    /**
     * 构建重试配置
     */
    private RetryUtil.RetryConfig buildRetryConfig() {
        if (!smtpConfig.getRetry().isEnabled()) {
            return RetryUtil.RetryConfig.defaults().maxAttempts(1);
        }
        
        return RetryUtil.RetryConfig.defaults()
                .maxAttempts(smtpConfig.getRetry().getMaxRetries() + 1)
                .initialDelayMs(smtpConfig.getRetry().getInitialDelayMs())
                .maxDelayMs(smtpConfig.getRetry().getMaxDelayMs())
                .useExponentialBackoff(smtpConfig.getRetry().isUseExponentialBackoff())
                .backoffMultiplier(smtpConfig.getRetry().getBackoffMultiplier())
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
}