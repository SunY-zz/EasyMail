package cn.sunyblog.easymail.api;

import cn.sunyblog.easymail.send.template.EasyMailSendTemplate;
import cn.sunyblog.easymail.send.EasyMailSendResult;
import cn.sunyblog.easymail.send.schedule.EasyMailScheduledTask;

import java.io.File;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * 企业级邮件发送服务接口
 * 提供统一的邮件发送API，支持同步、异步、批量发送等多种模式
 *
 * @author suny
 * @version 1.0.0
 */
public interface EasyMailSenderService {

    // ==================== 新的Builder模式API ====================

    /**
     * 使用EmailRequest发送邮件（推荐使用）
     * 提供更友好的链式调用API
     *
     * @param request 邮件请求对象
     * @return 发送结果
     */
    EasyMailSendResult send(EasyMailRequest request);

    /**
     * 异步发送邮件（使用EmailRequest）
     *
     * @param request 邮件请求对象
     * @return 异步发送结果
     */
    CompletableFuture<EasyMailSendResult> sendAsync(EasyMailRequest request);

    // ==================== 便捷发送方法（基于EasyMailRequest的简化API） ====================

    /**
     * 发送简单文本邮件（便捷方法）
     *
     * @param to      收件人邮箱
     * @param subject 邮件主题
     * @param content 邮件内容
     * @return 发送结果
     */
    default EasyMailSendResult sendText(String to, String subject, String content) {
        return send(EasyMailRequest.simpleText(to, subject, content));
    }

    /**
     * 发送HTML格式邮件（便捷方法）
     *
     * @param to          收件人邮箱
     * @param subject     邮件主题
     * @param htmlContent HTML格式的邮件内容
     * @return 发送结果
     */
    default EasyMailSendResult sendHtml(String to, String subject, String htmlContent) {
        return send(EasyMailRequest.htmlEmail(to, subject, htmlContent));
    }

    /**
     * 使用HTML模板文件发送邮件
     * 此方法会动态从文件系统加载HTML模板并创建临时模板
     *
     * @param to           收件人邮箱
     * @param subject      邮件主题
     * @param templatePath HTML模板文件路径（相对于classpath:static/templates/）
     * @param variables    模板变量
     * @return 发送结果
     */
    default EasyMailSendResult sendHtmlTemplate(String to, String subject, String templatePath, Map<String, Object> variables) {
        return send(EasyMailRequest.htmlTemplate(to, subject, templatePath, variables));
    }

    /**
     * 使用HTML模板文件发送邮件（无变量）
     *
     * @param to           收件人邮箱
     * @param subject      邮件主题
     * @param templatePath HTML模板文件路径（相对于classpath:static/templates/）
     * @return 发送结果
     */
    default EasyMailSendResult sendHtmlTemplate(String to, String subject, String templatePath) {
        return sendHtmlTemplate(to, subject, templatePath, new HashMap<>());
    }

    // ==================== 模板邮件发送方法 ====================

    /**
     * 使用模板发送邮件
     *
     * @param to        收件人邮箱
     * @param template  邮件模板
     * @param variables 模板变量
     * @return 发送结果
     */
    EasyMailSendResult sendWithTemplate(String to, EasyMailSendTemplate template, Map<String, Object> variables) throws Exception;

    /**
     * 批量使用模板发送邮件（每个收件人可以有不同的变量）
     *
     * @param recipients 收件人和对应的模板变量映射
     * @param template   邮件模板
     * @return 批量发送结果
     */
    EasyMailSendResult sendBatchWithTemplate(Map<String, Map<String, Object>> recipients, EasyMailSendTemplate template) throws Exception;

    // ==================== 异步发送方法（基于EasyMailRequest的简化API） ====================

    /**
     * 异步发送简单文本邮件（便捷方法）
     *
     * @param to      收件人邮箱
     * @param subject 邮件主题
     * @param content 邮件内容
     * @return 异步发送结果
     */
    default CompletableFuture<EasyMailSendResult> sendTextAsync(String to, String subject, String content) {
        return sendAsync(EasyMailRequest.simpleText(to, subject, content));
    }

    /**
     * 异步发送HTML格式邮件（便捷方法）
     *
     * @param to          收件人邮箱
     * @param subject     邮件主题
     * @param htmlContent HTML格式的邮件内容
     * @return 异步发送结果
     */
    default CompletableFuture<EasyMailSendResult> sendHtmlAsync(String to, String subject, String htmlContent) {
        return sendAsync(EasyMailRequest.htmlEmail(to, subject, htmlContent));
    }

    /**
     * 批量发送邮件（便捷方法）
     *
     * @param requests 邮件请求列表
     * @return 批量发送结果
     */
    List<EasyMailSendResult> sendBatch(List<EasyMailRequest> requests);

    /**
     * 异步批量发送邮件
     *
     * @param requests 邮件请求列表
     * @param callback 发送完成回调（参数为成功发送的数量）
     * @return 异步发送结果
     */
    CompletableFuture<List<EasyMailSendResult>> sendBatchAsync(List<EasyMailRequest> requests, Consumer<Integer> callback);

    // ==================== 状态查询方法 ====================

    /**
     * 获取发送统计信息
     *
     * @return 发送统计信息
     */
    String getSendingStats();

    /**
     * 获取线程池状态
     *
     * @return 线程池状态信息
     */
    String getThreadPoolStatus();

    /**
     * 检查邮件服务器连接状态
     *
     * @return 连接状态
     */
    boolean checkConnection();

    /**
     * 获取详细统计信息
     *
     * @return 详细统计信息
     */
    Map<String, Object> getDetailedStatistics();

    /**
     * 获取策略统计信息
     *
     * @return 策略统计信息
     */
    Map<String, Object> getStrategyStatistics();

    /**
     * 获取健康状态信息
     *
     * @return 健康状态信息
     */
    Map<String, Object> getHealthStatus();

    /**
     * 获取邮件发送趋势信息
     *
     * @return 邮件发送趋势信息
     */
    Map<String, Object> getSendTrend();

    /**
     * 生成监控报告
     *
     * @return 监控报告
     */
    String generateMonitorReport();

    /**
     * 重置统计信息
     */
    void resetStatistics();

    // ==================== 定时发送方法 ====================

    /**
     * 使用Cron表达式定时发送邮件
     *
     * @param request        邮件请求对象
     * @param cronExpression Cron表达式
     * @return 定时任务ID
     */
    String sendScheduled(EasyMailRequest request, String cronExpression);

    /**
     * 使用Cron表达式定时发送邮件（带任务名称）
     *
     * @param request        邮件请求对象
     * @param cronExpression Cron表达式
     * @param taskName       任务名称
     * @return 定时任务ID
     */
    String sendScheduled(EasyMailRequest request, String cronExpression, String taskName);

    /**
     * 延迟发送邮件
     *
     * @param request     邮件请求对象
     * @param delayMillis 延迟时间（毫秒）
     * @return 定时任务ID
     */
    String sendDelayed(EasyMailRequest request, long delayMillis);

    /**
     * 延迟发送邮件（带任务名称）
     *
     * @param request     邮件请求对象
     * @param delayMillis 延迟时间（毫秒）
     * @param taskName    任务名称
     * @return 定时任务ID
     */
    String sendDelayed(EasyMailRequest request, long delayMillis, String taskName);

    /**
     * 固定频率发送邮件
     *
     * @param request         邮件请求对象
     * @param fixedRateMillis 固定频率时间（毫秒）
     * @return 定时任务ID
     */
    String sendAtFixedRate(EasyMailRequest request, long fixedRateMillis);

    /**
     * 固定频率发送邮件（带任务名称）
     *
     * @param request         邮件请求对象
     * @param fixedRateMillis 固定频率时间（毫秒）
     * @param taskName        任务名称
     * @return 定时任务ID
     */
    String sendAtFixedRate(EasyMailRequest request, long fixedRateMillis, String taskName);

    /**
     * 固定延迟发送邮件
     *
     * @param request          邮件请求对象
     * @param fixedDelayMillis 固定延迟时间（毫秒）
     * @return 定时任务ID
     */
    String sendWithFixedDelay(EasyMailRequest request, long fixedDelayMillis);

    /**
     * 固定延迟发送邮件（带任务名称）
     *
     * @param request          邮件请求对象
     * @param fixedDelayMillis 固定延迟时间（毫秒）
     * @param taskName         任务名称
     * @return 定时任务ID
     */
    String sendWithFixedDelay(EasyMailRequest request, long fixedDelayMillis, String taskName);

    /**
     * 在指定时间发送邮件
     *
     * @param request     邮件请求对象
     * @param executeTime 执行时间
     * @return 定时任务ID
     */
    String sendAtTime(EasyMailRequest request, LocalDateTime executeTime);

    /**
     * 在指定时间发送邮件（带任务名称）
     *
     * @param request     邮件请求对象
     * @param executeTime 执行时间
     * @param taskName    任务名称
     * @return 定时任务ID
     */
    String sendAtTime(EasyMailRequest request, LocalDateTime executeTime, String taskName);

    // ==================== 批量定时发送方法 ====================

    /**
     * 批量定时发送邮件（Cron表达式）
     *
     * @param requests       邮件请求列表
     * @param cronExpression Cron表达式
     * @return 定时任务ID
     */
    String sendBatchScheduled(List<EasyMailRequest> requests, String cronExpression);

    /**
     * 批量定时发送邮件（Cron表达式，带任务名称）
     *
     * @param requests       邮件请求列表
     * @param cronExpression Cron表达式
     * @param taskName       任务名称
     * @return 定时任务ID
     */
    String sendBatchScheduled(List<EasyMailRequest> requests, String cronExpression, String taskName);

    /**
     * 批量延迟发送邮件
     *
     * @param requests    邮件请求列表
     * @param delayMillis 延迟时间（毫秒）
     * @return 定时任务ID
     */
    String sendBatchDelayed(List<EasyMailRequest> requests, long delayMillis);

    /**
     * 批量延迟发送邮件（带任务名称）
     *
     * @param requests    邮件请求列表
     * @param delayMillis 延迟时间（毫秒）
     * @param taskName    任务名称
     * @return 定时任务ID
     */
    String sendBatchDelayed(List<EasyMailRequest> requests, long delayMillis, String taskName);

    /**
     * 批量在指定时间发送邮件
     *
     * @param requests    邮件请求列表
     * @param executeTime 执行时间
     * @return 定时任务ID
     */
    String sendBatchAtTime(List<EasyMailRequest> requests, LocalDateTime executeTime);

    /**
     * 批量在指定时间发送邮件（带任务名称）
     *
     * @param requests    邮件请求列表
     * @param executeTime 执行时间
     * @param taskName    任务名称
     * @return 定时任务ID
     */
    String sendBatchAtTime(List<EasyMailRequest> requests, LocalDateTime executeTime, String taskName);

    // ==================== 定时任务管理方法 ====================

    /**
     * 取消定时任务
     *
     * @param taskId 任务ID
     * @return 是否成功取消
     */
    boolean cancelScheduledTask(String taskId);

    /**
     * 删除定时任务
     *
     * @param taskId 任务ID
     * @return 是否成功删除
     */
    boolean removeScheduledTask(String taskId);

    /**
     * 获取定时任务
     *
     * @param taskId 任务ID
     * @return 定时任务
     */
    EasyMailScheduledTask getScheduledTask(String taskId);

    /**
     * 获取所有定时任务
     *
     * @return 所有定时任务列表
     */
    List<EasyMailScheduledTask> getAllScheduledTasks();

    /**
     * 根据状态获取定时任务
     *
     * @param status 任务状态
     * @return 任务列表
     */
    List<EasyMailScheduledTask> getScheduledTasksByStatus(EasyMailScheduledTask.TaskStatus status);

    /**
     * 获取运行中的定时任务
     *
     * @return 运行中的任务列表
     */
    List<EasyMailScheduledTask> getRunningScheduledTasks();

    /**
     * 取消所有定时任务
     */
    void cancelAllScheduledTasks();

    /**
     * 清理已完成的定时任务
     */
    void cleanupCompletedScheduledTasks();

    /**
     * 获取定时任务统计信息
     *
     * @return 统计信息
     */
    Map<String, Object> getScheduledTaskStatistics();

    /**
     * 检查定时任务是否存在
     *
     * @param taskId 任务ID
     * @return 是否存在
     */
    boolean scheduledTaskExists(String taskId);

    /**
     * 获取定时任务数量
     *
     * @return 任务数量
     */
    int getScheduledTaskCount();
}