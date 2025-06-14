package cn.sunyblog.javaemaildemo.example;

import cn.sunyblog.javaemaildemo.EmailSenderStarter;
import cn.sunyblog.javaemaildemo.api.EmailSenderService;
import cn.sunyblog.javaemaildemo.mail.EmailTemplate;
import cn.sunyblog.javaemaildemo.mail.MailService;
import cn.sunyblog.javaemaildemo.mail.SendResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.File;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * 邮件发送示例类
 * 展示如何使用邮件发送功能（包含新的企业级邮件发送服务）
 */
@Slf4j
@Component
public class EmailSenderExample implements CommandLineRunner {

    @Autowired
    private MailService mailService;
    
    @Resource
    private EmailSenderService emailSenderService;
    
    @Resource
    private EmailSenderStarter emailSenderStarter;

    @Override
    public void run(String... args) {
        // 注意：这里仅作为示例，实际运行时请注释掉不需要的示例代码
        // 或者通过配置控制是否执行示例代码
        
        log.info("=== 邮件发送服务示例 ===");
        
        // 检查是否启用了示例代码
        if (!isExampleEnabled()) {
            log.info("示例代码已禁用，如需启用请设置 email.sender.example.enabled=true");
            return;
        }
        
        try {
            // 新的企业级邮件发送服务示例
            enterpriseEmailServiceExamples();
            
            log.info("=== 邮件发送服务示例完成 ===");
        } catch (Exception e) {
            log.error("示例执行异常: {}", e.getMessage(), e);
        }
    }

    /**
     * 检查是否启用了示例代码
     */
    private boolean isExampleEnabled() {
        // 实际项目中可以通过配置控制
        // 这里为了简化示例，默认返回false，避免意外发送邮件
        return false;
    }

    /**
     * 发送简单文本邮件示例
     */
    public void sendSimpleTextEmail() {
        String to = "3379652824@qq.com";
        String subject = "测试邮件 - 文本格式";
        String content = "这是一封测试邮件，由JavaEmailDemo发送。\n\n祝好，\nJavaEmailDemo团队";
        
        boolean success = mailService.sendSimpleEmail(to, subject, content);
        log.info("简单文本邮件发送{}", success ? "成功" : "失败");
    }

    /**
     * 发送HTML格式邮件示例
     */
    public void sendHtmlEmail() {
        String to = "recipient@example.com";
        String subject = "测试邮件 - HTML格式";
        String htmlContent = ""
                + "<html>"
                + "<head><title>测试邮件</title></head>"
                + "<body>"
                + "<h1>JavaEmailDemo测试邮件</h1>"
                + "<p>这是一封<strong>HTML格式</strong>的测试邮件。</p>"
                + "<p>您可以在这里添加<span style='color:blue;'>各种样式</span>和<a href='https://github.com/yourusername/JavaEmailDemo'>链接</a>。</p>"
                + "<hr/>"
                + "<p>祝好，<br/>JavaEmailDemo团队</p>"
                + "</body>"
                + "</html>";
        
        boolean success = mailService.sendHtmlEmail(to, subject, htmlContent);
        log.info("HTML格式邮件发送{}", success ? "成功" : "失败");
    }

    /**
     * 发送带附件的邮件示例
     */
    public void sendEmailWithAttachments() {
        String to = "recipient@example.com";
        String subject = "测试邮件 - 带附件";
        String content = "这是一封带附件的测试邮件，请查看附件。";
        
        // 创建附件列表
        List<File> attachments = new ArrayList<>();
        attachments.add(new File("path/to/attachment1.pdf"));
        attachments.add(new File("path/to/attachment2.jpg"));
        
        boolean success = mailService.sendEmailWithAttachments(to, subject, content, false, attachments);
        log.info("带附件的邮件发送{}", success ? "成功" : "失败");
    }

    /**
     * 批量发送邮件示例
     */
    public void sendBatchEmails() {
        List<String> recipients = Arrays.asList(
                "recipient1@example.com",
                "recipient2@example.com",
                "recipient3@example.com"
        );
        
        String subject = "批量测试邮件";
        String content = "这是一封批量发送的测试邮件。";
        
        int successCount = mailService.sendBatchEmails(recipients, subject, content, false);
        log.info("批量邮件发送完成，成功发送: {}/{}", successCount, recipients.size());
    }

    /**
     * 企业级邮件发送服务示例
     */
    private void enterpriseEmailServiceExamples() {
        log.info("=== 企业级邮件发送服务示例 ===");
        
        // 检查连接
        checkConnection();
        
        // 基本发送示例
        basicSendingExamples();
        
        // 模板发送示例
        templateSendingExamples();
        
        // 高级功能示例
        advancedFeaturesExamples();
        
        // 统计信息示例
        statisticsExamples();
    }

    /**
     * 检查连接示例
     */
    private void checkConnection() {
        log.info("检查邮件服务连接...");
        boolean connected = emailSenderService.checkConnection();
        log.info("连接状态: {}", connected ? "正常" : "异常");
    }

    /**
     * 基本发送示例
     */
    private void basicSendingExamples() {
        log.info("=== 基本发送示例 ===");
        
        String recipient = "example@example.com"; // 替换为实际收件人
        
        // 发送文本邮件
        SendResult textResult = emailSenderService.sendText(
                recipient, 
                "文本邮件测试", 
                "这是一封测试文本邮件，发送时间：" + System.currentTimeMillis());
        log.info("文本邮件发送结果: {}", textResult.isSuccess() ? "成功" : "失败: " + textResult.getErrorMessage());
        
        // 发送HTML邮件
        SendResult htmlResult = emailSenderService.sendHtml(
                recipient, 
                "HTML邮件测试", 
                "<h2>HTML邮件测试</h2><p>这是一封<strong>HTML格式</strong>的测试邮件</p>");
        log.info("HTML邮件发送结果: {}", htmlResult.isSuccess() ? "成功" : "失败: " + htmlResult.getErrorMessage());
        
        // 批量发送邮件
        SendResult batchResult = emailSenderService.sendToMultiple(
                Arrays.asList(recipient, "another@example.com"), 
                "批量邮件测试", 
                "这是一封批量发送的测试邮件", false);
        log.info("批量邮件发送结果: {}", batchResult.isSuccess() ? "成功" : "失败: " + batchResult.getErrorMessage());
    }

    /**
     * 模板发送示例
     */
    private void templateSendingExamples() {
        log.info("=== 模板发送示例 ===");
        
        String recipient = "example@example.com"; // 替换为实际收件人
        
        // 使用内置模板
        Map<String, Object> verifyVars = new HashMap<>();
        verifyVars.put("code", "123456");
        verifyVars.put("expireMinutes", 10);
        
        boolean builtinResult = emailSenderStarter.sendWithTemplate(
                recipient, 
                "verification", 
                verifyVars);
        log.info("内置模板邮件发送结果: {}", builtinResult ? "成功" : "失败");
        
        // 创建自定义模板
        EmailTemplate welcomeTemplate = EmailTemplate.builder()
                .templateId("custom-welcome")
                .templateName("欢迎模板")
                .subjectTemplate("欢迎加入{{company}}")
                .contentTemplate("<h2>欢迎 {{name}}！</h2><p>感谢您加入{{company}}，您的账号已激活。</p>")
                .isHtml(true)
                .build();
        
        // 注册模板
        emailSenderStarter.getTemplateManager().registerTemplate(welcomeTemplate);
        
        // 准备模板变量
        Map<String, Object> variables = new HashMap<>();
        variables.put("name", "张三");
        variables.put("company", "示例公司");
        
        // 使用模板发送邮件
        SendResult templateResult = emailSenderService.sendWithTemplate(
                recipient, 
                welcomeTemplate, 
                variables);
        log.info("自定义模板邮件发送结果: {}", templateResult.isSuccess() ? "成功" : "失败: " + templateResult.getErrorMessage());
    }

    /**
     * 高级功能示例
     */
    private void advancedFeaturesExamples() {
        log.info("=== 高级功能示例 ===");
        
        String recipient = "example@example.com"; // 替换为实际收件人
        
        // 异步发送邮件
        emailSenderService.sendAsync(
                Arrays.asList(recipient), 
                null, null,
                "异步邮件测试", 
                "这是一封异步发送的测试邮件",
                false, null)
                .thenAccept(result -> {
                    log.info("异步邮件发送完成，结果: {}", result.isSuccess() ? "成功" : "失败: " + result.getErrorMessage());
                    log.info("消息ID: {}, 耗时: {}ms", result.getMessageId(), result.getDuration());
                });
        
        // 使用便捷方法发送带回调的邮件
        emailSenderStarter.sendWithCallback(
                recipient, 
                "带回调的邮件测试", 
                "这是一封带回调的测试邮件",
                success -> log.info("邮件发送成功"),
                error -> log.error("邮件发送失败: {}", error));
    }

    /**
     * 统计信息示例
     */
    private void statisticsExamples() {
        log.info("=== 统计信息示例 ===");
        
        // 获取发送统计信息
        String stats = emailSenderService.getSendingStats();
        log.info("发送统计: {}", stats);
        
        // 获取线程池状态
        String poolStatus = emailSenderService.getThreadPoolStatus();
        log.info("线程池状态: {}", poolStatus);
        
        // 获取模板统计信息
        log.info("模板数量: {}", emailSenderStarter.getTemplateNames().size());
        log.info("模板列表: {}", emailSenderStarter.getTemplateNames());
    }

    /**
     * 发送验证码邮件示例
     * 
     * @param email 收件人邮箱
     * @param verificationCode 验证码
     * @return 是否发送成功
     */
    public boolean sendVerificationCodeEmail(String email, String verificationCode) {
        String subject = "您的验证码";
        String htmlContent = ""
                + "<html>"
                + "<head><title>验证码</title></head>"
                + "<body>"
                + "<div style='background-color:#f7f7f7;padding:20px;'>"
                + "<h2>验证码</h2>"
                + "<p>您的验证码是：</p>"
                + "<div style='background-color:#ffffff;padding:10px;font-size:24px;font-weight:bold;text-align:center;'>"
                + verificationCode
                + "</div>"
                + "<p>验证码有效期为10分钟，请勿泄露给他人。</p>"
                + "<p>如果这不是您的操作，请忽略此邮件。</p>"
                + "</div>"
                + "</body>"
                + "</html>";
        
        return mailService.sendHtmlEmail(email, subject, htmlContent);
    }

    /**
     * 发送通知邮件示例
     * 
     * @param email 收件人邮箱
     * @param title 通知标题
     * @param message 通知内容
     * @return 是否发送成功
     */
    public boolean sendNotificationEmail(String email, String title, String message) {
        String subject = "通知: " + title;
        String htmlContent = ""
                + "<html>"
                + "<head><title>" + title + "</title></head>"
                + "<body>"
                + "<div style='background-color:#f7f7f7;padding:20px;'>"
                + "<h2>" + title + "</h2>"
                + "<div style='background-color:#ffffff;padding:15px;'>"
                + message.replace("\n", "<br/>")
                + "</div>"
                + "<p style='font-size:12px;color:#888888;margin-top:20px;'>此邮件由系统自动发送，请勿回复。</p>"
                + "</div>"
                + "</body>"
                + "</html>";
        
        return mailService.sendHtmlEmail(email, subject, htmlContent);
    }
}