package cn.sunyblog.easymail.api;

import cn.sunyblog.easymail.send.template.EasyMailSendTemplate;
import cn.sunyblog.easymail.send.EasyMailSendResult;

import java.io.File;
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

    // ==================== 基础发送方法 ====================

    /**
     * 发送简单文本邮件
     *
     * @param to      收件人邮箱
     * @param subject 邮件主题
     * @param content 邮件内容
     * @return 发送结果
     */
    EasyMailSendResult sendText(String to, String subject, String content);

    /**
     * 发送HTML格式邮件
     *
     * @param to          收件人邮箱
     * @param subject     邮件主题
     * @param htmlContent HTML格式的邮件内容
     * @return 发送结果
     */
    EasyMailSendResult sendHtml(String to, String subject, String htmlContent);

    /**
     * 发送带附件的邮件
     *
     * @param to          收件人邮箱
     * @param subject     邮件主题
     * @param content     邮件内容
     * @param isHtml      是否为HTML格式
     * @param attachments 附件列表
     * @return 发送结果
     */
    EasyMailSendResult sendWithAttachments(String to, String subject, String content, boolean isHtml, List<File> attachments);

    // ==================== 多收件人发送方法 ====================

    EasyMailSendResult send(List<String> toList, String subject, String content);

    EasyMailSendResult send(List<String> toList, String subject, String content, boolean isHtml);

    EasyMailSendResult send(List<String> toList, String subject, String content, List<File> attachments);

    /**
     * 发送邮件给多个收件人（TO）
     *
     * @param toList  收件人列表
     * @param subject 邮件主题
     * @param content 邮件内容
     * @param isHtml  是否为HTML格式
     * @return 发送结果
     */
    EasyMailSendResult sendToMultiple(List<String> toList, String subject, String content, boolean isHtml);

    /**
     * 发送邮件（支持TO、CC、BCC）
     *
     * @param toList      收件人列表
     * @param ccList      抄送人列表
     * @param bccList     密送人列表
     * @param subject     邮件主题
     * @param content     邮件内容
     * @param isHtml      是否为HTML格式
     * @param attachments 附件列表
     * @return 发送结果
     */
    EasyMailSendResult send(List<String> toList, List<String> ccList, List<String> bccList,
                            String subject, String content, boolean isHtml, List<File> attachments);

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

    // ==================== 异步发送方法 ====================

    /**
     * 异步发送简单文本邮件
     *
     * @param to      收件人邮箱
     * @param subject 邮件主题
     * @param content 邮件内容
     * @return 异步发送结果
     */
    CompletableFuture<EasyMailSendResult> sendTextAsync(String to, String subject, String content);

    /**
     * 异步发送HTML格式邮件
     *
     * @param to          收件人邮箱
     * @param subject     邮件主题
     * @param htmlContent HTML格式的邮件内容
     * @return 异步发送结果
     */
    CompletableFuture<EasyMailSendResult> sendHtmlAsync(String to, String subject, String htmlContent);

    /**
     * 异步发送邮件（完整参数）
     *
     * @param toList      收件人列表
     * @param ccList      抄送人列表
     * @param bccList     密送人列表
     * @param subject     邮件主题
     * @param content     邮件内容
     * @param isHtml      是否为HTML格式
     * @param attachments 附件列表
     * @return 异步发送结果
     */
    CompletableFuture<EasyMailSendResult> sendAsync(List<String> toList, List<String> ccList, List<String> bccList,
                                                    String subject, String content, boolean isHtml, List<File> attachments);

    /**
     * 异步批量发送邮件
     *
     * @param toList   收件人列表
     * @param subject  邮件主题
     * @param content  邮件内容
     * @param isHtml   是否为HTML格式
     * @param callback 发送完成回调（参数为成功发送的数量）
     * @return 异步发送结果
     */
    CompletableFuture<EasyMailSendResult> sendBatchAsync(List<String> toList, String subject, String content,
                                                         boolean isHtml, Consumer<Integer> callback);

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
}