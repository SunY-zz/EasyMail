package cn.sunyblog.javaemaildemo.api;

import cn.sunyblog.javaemaildemo.send.SendResult;
import cn.sunyblog.javaemaildemo.send.template.EmailTemplate;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * EasyMail 统一对外API接口
 * 这是推荐给外部用户使用的主要接口，提供简洁易用的邮件发送功能
 * 
 * 使用示例：
 * <pre>
 * // 1. 简单文本邮件
 * SendResult result = easyMailSender.sendText("user@example.com", "测试", "内容");
 * 
 * // 2. 使用Builder模式（推荐）
 * SendResult result = easyMailSender.send(
 *     EmailRequest.builder()
 *         .to("user@example.com")
 *         .subject("测试邮件")
 *         .text("邮件内容")
 *         .build()
 * );
 * 
 * // 3. 异步发送
 * CompletableFuture<SendResult> future = easyMailSender.sendAsync(
 *     EmailRequest.builder()
 *         .to("user@example.com")
 *         .subject("异步邮件")
 *         .html("<h1>HTML内容</h1>")
 *         .build()
 * );
 * </pre>
 * 
 * @author JavaEmailSpringBoot
 * @version 1.0.0
 * @since 1.0.0
 */
public interface EasyMailSender {
    
    // ==================== 推荐使用的Builder模式API ====================
    
    /**
     * 发送邮件（推荐使用）
     * 使用EmailRequest构建器模式，提供最灵活的邮件发送方式
     * 
     * @param request 邮件请求对象
     * @return 发送结果
     */
    SendResult send(EmailRequest request);
    
    /**
     * 异步发送邮件（推荐使用）
     * 
     * @param request 邮件请求对象
     * @return 异步发送结果
     */
    CompletableFuture<SendResult> sendAsync(EmailRequest request);
    
    // ==================== 便捷发送方法 ====================
    
    /**
     * 发送简单文本邮件
     * 
     * @param to 收件人邮箱
     * @param subject 邮件主题
     * @param content 邮件内容
     * @return 发送结果
     */
    SendResult sendText(String to, String subject, String content);
    
    /**
     * 发送HTML格式邮件
     * 
     * @param to 收件人邮箱
     * @param subject 邮件主题
     * @param htmlContent HTML格式的邮件内容
     * @return 发送结果
     */
    SendResult sendHtml(String to, String subject, String htmlContent);
    
    /**
     * 发送带附件的邮件
     * 
     * @param to 收件人邮箱
     * @param subject 邮件主题
     * @param content 邮件内容
     * @param attachments 附件列表
     * @return 发送结果
     */
    SendResult sendWithAttachment(String to, String subject, String content, File... attachments);
    
    /**
     * 批量发送邮件
     * 
     * @param toList 收件人列表
     * @param subject 邮件主题
     * @param content 邮件内容
     * @return 发送结果
     */
    SendResult sendBatch(List<String> toList, String subject, String content);
    
    /**
     * 使用模板发送邮件
     * 
     * @param to 收件人邮箱
     * @param templateName 模板名称
     * @param variables 模板变量
     * @return 发送结果
     */
    SendResult sendWithTemplate(String to, String templateName, Map<String, Object> variables);
    
    // ==================== 异步发送方法 ====================
    
    /**
     * 异步发送文本邮件
     * 
     * @param to 收件人邮箱
     * @param subject 邮件主题
     * @param content 邮件内容
     * @return 异步发送结果
     */
    CompletableFuture<SendResult> sendTextAsync(String to, String subject, String content);
    
    /**
     * 异步发送HTML邮件
     * 
     * @param to 收件人邮箱
     * @param subject 邮件主题
     * @param htmlContent HTML格式的邮件内容
     * @return 异步发送结果
     */
    CompletableFuture<SendResult> sendHtmlAsync(String to, String subject, String htmlContent);
    
    /**
     * 发送邮件并处理结果回调
     * 
     * @param to 收件人邮箱
     * @param subject 邮件主题
     * @param content 邮件内容
     * @param successCallback 成功回调
     * @param errorCallback 错误回调
     * @return 异步发送结果
     */
    CompletableFuture<SendResult> sendWithCallback(String to, String subject, String content,
                                                   Consumer<SendResult> successCallback,
                                                   Consumer<SendResult> errorCallback);
    
    // ==================== 状态查询方法 ====================
    
    /**
     * 检查邮件服务器连接状态
     * 
     * @return 连接状态
     */
    boolean checkConnection();
    
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
}