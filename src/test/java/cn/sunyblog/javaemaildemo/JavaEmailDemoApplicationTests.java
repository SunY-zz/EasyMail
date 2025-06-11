package cn.sunyblog.javaemaildemo;

import cn.sunyblog.javaemaildemo.mail.MailService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.PostConstruct;
import java.util.Arrays;
import java.util.List;

@Slf4j
@SpringBootTest
class JavaEmailDemoApplicationTests {
    @Autowired
    private MailService mailService;

    @PostConstruct
    public void startMailMonitoring() {
        mailService.init();
    }

    /**
     * 发送简单文本邮件示例
     */
    @Test
    public void sendSimpleTextEmail() {
        while (mailService.isMailServiceRunning()) {
            String to = "3379652824@qq.com";
            String subject = "测试邮件 - 文本格式";
            String content = "这是一封测试邮件，由JavaEmailDemo发送。\n\n祝好，\nJavaEmailDemo团队";

            boolean success = mailService.sendSimpleEmail(to, subject, content);
            log.info("简单文本邮件发送{}", success ? "成功" : "失败");
            break;
        }
    }

    /**
     * 批量发送邮件示例
     */
    @Test
    public void sendBatchEmails() {
        List<String> recipients = Arrays.asList(
                "3379652824@qq.com",
                "daliyuan702@sina.com",
                "daliyuan702@gmail.com"
        );

        String subject = "批量测试邮件";
        String content = "这是一封批量发送的测试邮件。";

        int successCount = mailService.sendBatchEmails(recipients, subject, content, false);
        log.info("批量邮件发送完成，成功发送: {}/{}", successCount, recipients.size());
    }
    /**
     * 发送验证码邮件示例
     * @return 是否发送成功
     */
    @Test
    public void sendVerificationCodeEmail() {
        String email = "3379652824@qq.com";
        String verificationCode = "123456";
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
        mailService.sendHtmlEmail(email, subject, htmlContent);
    }

    /**
     * 发送通知邮件示例
     *
     * @return 是否发送成功
     */
    @Test
    public void sendNotificationEmail() {
        String email = "3379652824@qq.com";
        String title = "系统通知";
        String message = "您有一个新的系统通知。\n\n"
                + "标题: " + title + "\n"
                + "内容: \n"
                + "这是一封系统通知，请勿回复。";
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

        mailService.sendHtmlEmail(email, subject, htmlContent);
    }
}
