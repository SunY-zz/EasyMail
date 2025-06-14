package cn.sunyblog.javaemaildemo;

import cn.sunyblog.javaemaildemo.api.EmailSenderService;
import cn.sunyblog.javaemaildemo.config.EmailSenderAutoConfiguration;
import cn.sunyblog.javaemaildemo.config.EmailSenderProperties;
import cn.sunyblog.javaemaildemo.mail.EmailTemplate;
import cn.sunyblog.javaemaildemo.mail.EmailTemplateManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfigurationPackage;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * 邮件发送服务启动器
 * 提供便捷的邮件发送功能，支持开箱即用
 * 
 * 使用方式：
 * 1. 引入依赖或复制代码到项目中
 * 2. 配置SMTP相关参数
 * 3. 注入EmailSenderStarter或EmailSenderService使用
 * 
 * @author sunyblog
 * @since 1.0.0
 */
@Slf4j
@Component
@AutoConfigurationPackage
@Import(EmailSenderAutoConfiguration.class)
public class EmailSenderStarter {

    @Resource
    private EmailSenderService emailSenderService;
    
    @Resource
    private EmailTemplateManager templateManager;
    
    @Resource
    private EmailSenderProperties properties;

    @PostConstruct
    public void init() {
        log.info("EmailSenderStarter 初始化完成");
        log.info("邮件发送服务已启用: {}", properties.isEnabled());
        
        // 初始化默认模板
        if (properties.getTemplate().isAutoLoadDefaults()) {
            initDefaultTemplates();
        }
    }

    // ==================== 便捷发送方法 ====================

    /**
     * 发送简单文本邮件
     */
    public boolean sendText(String to, String subject, String content) {
        return emailSenderService.sendText(to, subject, content).isSuccess();
    }

    /**
     * 发送HTML邮件
     */
    public boolean sendHtml(String to, String subject, String htmlContent) {
        return emailSenderService.sendHtml(to, subject, htmlContent).isSuccess();
    }

    /**
     * 发送带附件的邮件
     */
    public boolean sendWithAttachment(String to, String subject, String content, File... attachments) {
        return emailSenderService.sendWithAttachments(to, subject, content, false, Arrays.asList(attachments)).isSuccess();
    }

    /**
     * 批量发送邮件
     */
    public boolean sendBatch(List<String> toList, String subject, String content) {
        return emailSenderService.sendToMultiple(toList, subject, content, false).isSuccess();
    }

    /**
     * 异步发送邮件
     */
    public CompletableFuture<Boolean> sendAsync(String to, String subject, String content) {
        return emailSenderService.sendTextAsync(to, subject, content)
                .thenApply(result -> result.isSuccess());
    }

    /**
     * 使用模板发送邮件
     */
    public boolean sendWithTemplate(String to, String templateName, Map<String, Object> variables) {
        EmailTemplate template = templateManager.getTemplate(templateName);
        if (template == null) {
            log.warn("模板不存在: {}", templateName);
            return false;
        }
        return emailSenderService.sendWithTemplate(to, template, variables).isSuccess();
    }

    // ==================== 高级功能 ====================

    /**
     * 发送邮件并处理结果
     */
    public void sendWithCallback(String to, String subject, String content, 
                                Consumer<Boolean> successCallback, 
                                Consumer<String> errorCallback) {
        emailSenderService.sendTextAsync(to, subject, content)
                .thenAccept(result -> {
                    if (result.isSuccess()) {
                        if (successCallback != null) {
                            successCallback.accept(true);
                        }
                    } else {
                        if (errorCallback != null) {
                            errorCallback.accept(result.getErrorMessage());
                        }
                    }
                });
    }

    /**
     * 检查邮件服务连接状态
     */
    public boolean checkConnection() {
        return emailSenderService.checkConnection();
    }

    /**
     * 获取发送统计信息
     */
    public String getStats() {
        return emailSenderService.getSendingStats();
    }

    /**
     * 获取线程池状态
     */
    public String getThreadPoolStatus() {
        return emailSenderService.getThreadPoolStatus();
    }

    // ==================== 模板管理 ====================

    /**
     * 注册邮件模板
     */
    public void registerTemplate(String name, String subject, String content, boolean isHtml) {
        EmailTemplate template = EmailTemplate.builder()
                .templateId(name)
                .templateName(name)
                .subjectTemplate(subject)
                .contentTemplate(content)
                .isHtml(isHtml)
                .build();
        templateManager.registerTemplate(template);
    }

    /**
     * 注册邮件模板（带默认附件）
     */
    public void registerTemplate(String name, String subject, String content, boolean isHtml, List<File> defaultAttachments) {
        EmailTemplate template = EmailTemplate.builder()
                .templateId(name)
                .templateName(name)
                .subjectTemplate(subject)
                .contentTemplate(content)
                .isHtml(isHtml)
                .defaultAttachments(defaultAttachments)
                .build();
        templateManager.registerTemplate(template);
    }

    /**
     * 获取所有模板名称
     */
    public List<String> getTemplateNames() {
        return new ArrayList<>(templateManager.getAllTemplates().keySet());
    }

    /**
     * 移除模板
     */
    public boolean removeTemplate(String name) {
        return templateManager.removeTemplate(name);
    }

    // ==================== 内部方法 ====================

    /**
     * 初始化默认模板
     */
    private void initDefaultTemplates() {
        try {
            // 欢迎邮件模板
            registerTemplate("welcome", 
                "欢迎使用我们的服务", 
                "<h2>欢迎 {{username}}！</h2><p>感谢您注册我们的服务，您的账号已激活。</p>", 
                true);

            // 验证码邮件模板
            registerTemplate("verification", 
                "验证码", 
                "<h3>您的验证码是：{{code}}</h3><p>验证码有效期为{{expireMinutes}}分钟，请及时使用。</p>", 
                true);

            // 密码重置模板
            registerTemplate("password-reset", 
                "密码重置", 
                "<h3>密码重置</h3><p>点击以下链接重置您的密码：</p><a href='{{resetLink}}'>重置密码</a>", 
                true);

            // 通知邮件模板
            registerTemplate("notification", 
                "系统通知", 
                "<h3>{{title}}</h3><p>{{message}}</p><p>发送时间：{{timestamp}}</p>", 
                true);

            log.info("默认邮件模板初始化完成");
        } catch (Exception e) {
            log.warn("初始化默认模板失败: {}", e.getMessage());
        }
    }

    // ==================== 获取服务实例 ====================

    /**
     * 获取邮件发送服务实例（用于高级功能）
     */
    public EmailSenderService getEmailSenderService() {
        return emailSenderService;
    }

    /**
     * 获取模板管理器实例
     */
    public EmailTemplateManager getTemplateManager() {
        return templateManager;
    }
}