package cn.sunyblog.javaemaildemo.example;

import cn.sunyblog.javaemaildemo.mail.MailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 邮件发送示例类
 * 展示如何使用邮件发送功能
 */
@Slf4j
@Component
public class EmailSenderExample {

    @Autowired
    private MailService mailService;

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