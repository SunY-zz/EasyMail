package cn.sunyblog.javaemaildemo.api;

import cn.sunyblog.javaemaildemo.mail.EmailTemplate;
import cn.sunyblog.javaemaildemo.mail.SendResult;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * 企业级邮件发送服务接口
 * 提供统一的邮件发送API，支持同步、异步、批量发送等多种模式
 * 
 * @author JavaEmailSpringBoot
 * @version 1.0.0
 */
public interface EmailSenderService {
    
    // ==================== 基础发送方法 ====================
    
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
     * @param isHtml 是否为HTML格式
     * @param attachments 附件列表
     * @return 发送结果
     */
    SendResult sendWithAttachments(String to, String subject, String content, boolean isHtml, List<File> attachments);
    
    // ==================== 多收件人发送方法 ====================
    
    /**
     * 发送邮件给多个收件人（TO）
     * 
     * @param toList 收件人列表
     * @param subject 邮件主题
     * @param content 邮件内容
     * @param isHtml 是否为HTML格式
     * @return 发送结果
     */
    SendResult sendToMultiple(List<String> toList, String subject, String content, boolean isHtml);
    
    /**
     * 发送邮件（支持TO、CC、BCC）
     * 
     * @param toList 收件人列表
     * @param ccList 抄送人列表
     * @param bccList 密送人列表
     * @param subject 邮件主题
     * @param content 邮件内容
     * @param isHtml 是否为HTML格式
     * @param attachments 附件列表
     * @return 发送结果
     */
    SendResult send(List<String> toList, List<String> ccList, List<String> bccList, 
                   String subject, String content, boolean isHtml, List<File> attachments);
    
    // ==================== 模板邮件发送方法 ====================
    
    /**
     * 使用模板发送邮件
     * 
     * @param to 收件人邮箱
     * @param template 邮件模板
     * @param variables 模板变量
     * @return 发送结果
     */
    SendResult sendWithTemplate(String to, EmailTemplate template, Map<String, Object> variables);
    
    /**
     * 批量使用模板发送邮件（每个收件人可以有不同的变量）
     * 
     * @param recipients 收件人和对应的模板变量映射
     * @param template 邮件模板
     * @return 批量发送结果
     */
    SendResult sendBatchWithTemplate(Map<String, Map<String, Object>> recipients, EmailTemplate template);
    
    // ==================== 异步发送方法 ====================
    
    /**
     * 异步发送简单文本邮件
     * 
     * @param to 收件人邮箱
     * @param subject 邮件主题
     * @param content 邮件内容
     * @return 异步发送结果
     */
    CompletableFuture<SendResult> sendTextAsync(String to, String subject, String content);
    
    /**
     * 异步发送HTML格式邮件
     * 
     * @param to 收件人邮箱
     * @param subject 邮件主题
     * @param htmlContent HTML格式的邮件内容
     * @return 异步发送结果
     */
    CompletableFuture<SendResult> sendHtmlAsync(String to, String subject, String htmlContent);
    
    /**
     * 异步发送邮件（完整参数）
     * 
     * @param toList 收件人列表
     * @param ccList 抄送人列表
     * @param bccList 密送人列表
     * @param subject 邮件主题
     * @param content 邮件内容
     * @param isHtml 是否为HTML格式
     * @param attachments 附件列表
     * @return 异步发送结果
     */
    CompletableFuture<SendResult> sendAsync(List<String> toList, List<String> ccList, List<String> bccList,
                                           String subject, String content, boolean isHtml, List<File> attachments);
    
    /**
     * 异步批量发送邮件
     * 
     * @param toList 收件人列表
     * @param subject 邮件主题
     * @param content 邮件内容
     * @param isHtml 是否为HTML格式
     * @param callback 发送完成回调（参数为成功发送的数量）
     * @return 异步发送结果
     */
    CompletableFuture<SendResult> sendBatchAsync(List<String> toList, String subject, String content, 
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
}