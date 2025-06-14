package cn.sunyblog.javaemaildemo;

import cn.sunyblog.javaemaildemo.mail.MailService;
import cn.sunyblog.javaemaildemo.processor.config.AnnotationDrivenEmailProcessorManager;
import cn.sunyblog.javaemaildemo.processor.handler.EmailHandlerInfo;
import cn.sunyblog.javaemaildemo.processor.handler.EmailHandlerRegistry;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.PostConstruct;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@SpringBootTest
class JavaEmailDemoApplicationTests {
    @Autowired
    private MailService mailService;
    
    @Autowired(required = false)
    private AnnotationDrivenEmailProcessorManager processorManager;
    
    @Autowired(required = false)
    private EmailHandlerRegistry handlerRegistry;

    @PostConstruct
    public void startMailMonitoring() {
        mailService.init();
    }

    /**
     * 测试注解驱动邮件处理器功能
     * 通过发送邮件来触发注解处理器
     */
    @Test
    public void testAnnotationDrivenEmailProcessor() throws InterruptedException {
        log.info("=== 测试注解驱动邮件处理器 ===");
        
        // 发送验证码邮件，触发验证码处理器
        String to = "3379652824@qq.com";
        String subject = "您的验证码";
        String content = "您的验证码是：123456，请在5分钟内使用。";
        
        boolean success = mailService.sendSimpleEmail(to, subject, content);
        log.info("验证码邮件发送{}", success ? "成功" : "失败");
        
        // 等待邮件处理
        Thread.sleep(3000);
        
        // 发送订单邮件，触发订单处理器
        String orderSubject = "订单确认通知";
        String orderContent = "您的订单 ORDER123456789 已确认，感谢您的购买！";
        
        success = mailService.sendSimpleEmail(to, orderSubject, orderContent);
        log.info("订单邮件发送{}", success ? "成功" : "失败");
        
        // 等待邮件处理
        Thread.sleep(500000);

        log.info("✅ 注解驱动邮件处理器测试完成");
    }

    @Test
    public void testSender() {
        log.info("=== 测试注解驱动邮件处理器 ===");

        // 发送验证码邮件，触发验证码处理器
        String to = "3379652824@qq.com";
        String subject = "您的验证码";
        String content = "您的验证码是：123456，请在5分钟内使用。";

        boolean success = mailService.sendSimpleEmail(to, subject, content);
        log.info("验证码邮件发送{}", success ? "成功" : "失败");
    }

    /**
     * 测试注解处理器注册情况
     */
    @Test
    public void testEmailHandlerRegistration() {
        log.info("=== 测试邮件处理器注册情况 ===");
        
        if (handlerRegistry == null) {
            log.warn("⚠️ 处理器注册表未配置，跳过测试");
            return;
        }
        
        List<EmailHandlerInfo> handlers = handlerRegistry.getAllHandlers();
        log.info("📋 已注册的处理器总数: {}", handlers.size());
        
        for (EmailHandlerInfo handler : handlers) {
            log.info("📌 处理器: {} - {} (优先级: {}, 异步: {}, 启用: {}, 组: {})", 
                    handler.getName(), 
                    handler.getDescription(),
                    handler.getPriority(),
                    handler.isAsync(),
                    handler.isEnabled(),
                    handler.getGroup());
        }
        
        // 检查测试处理器是否注册
        boolean hasTestVerificationHandler = handlers.stream()
                .anyMatch(h -> "testVerificationCodeHandler".equals(h.getName()));
        
        boolean hasTestOrderHandler = handlers.stream()
                .anyMatch(h -> "testOrderHandler".equals(h.getName()));
        
        if (hasTestVerificationHandler) {
            log.info("✅ 测试验证码处理器已注册");
        } else {
            log.warn("⚠️ 测试验证码处理器未找到");
        }
        
        if (hasTestOrderHandler) {
            log.info("✅ 测试订单处理器已注册");
        } else {
            log.warn("⚠️ 测试订单处理器未找到");
        }
        
        // 测试按组获取处理器
        List<EmailHandlerInfo> testHandlers = handlerRegistry.getHandlersByGroup("test");
        log.info("📋 'test' 组的处理器数量: {}", testHandlers.size());
        
        List<EmailHandlerInfo> exampleHandlers = handlerRegistry.getHandlersByGroup("example");
        log.info("📋 'example' 组的处理器数量: {}", exampleHandlers.size());
    }
    
    /**
     * 测试处理器管理器统计信息
     */
    @Test
    public void testProcessorManagerStatistics() {
        log.info("=== 测试处理器管理器统计信息 ===");
        
        if (processorManager == null) {
            log.warn("⚠️ 处理器管理器未配置，跳过测试");
            return;
        }
        
        Map<String, Object> stats = processorManager.getStatistics();
        log.info("📊 统计信息:");
        stats.forEach((key, value) -> {
            log.info("  {}: {}", key, value);
        });
        
        log.info("🔧 处理器管理器状态: {}", processorManager.isEnabled() ? "已启用" : "已禁用");
    }

    /**
     * 提取验证码
     */
    private String extractVerificationCode(String content) {
        if (content == null || content.trim().isEmpty()) {
            return null;
        }

        // 优化的验证码提取模式
        String[] patterns = {
                "(?i)(?:验证码|verification\\s*code|auth\\s*code)[：:：\\s]*([A-Z0-9]{4,8})",
                "(?i)(?:动态码|dynamic\\s*code)[：:：\\s]*([A-Z0-9]{4,8})",
                "(?i)(?:安全码|security\\s*code)[：:：\\s]*([A-Z0-9]{4,8})",
                "\\b([0-9]{6})\\b",
                "\\b([0-9]{4})\\b"
        };

        for (String pattern : patterns) {
            Pattern regex = Pattern.compile(pattern);
            Matcher matcher = regex.matcher(content);
            if (matcher.find()) {
                String code = matcher.group(1);
                // 排除年份格式
                if (!code.matches("^(19|20)\\d{2}$")) {
                    return code;
                }
            }
        }

        return null;
    }
    /**
     * 发送简单文本邮件示例
     */
    @Test
    public void sendSimpleTextEmail() throws InterruptedException {
            String to = "3379652824@qq.com";
            String subject = "测试邮件 - 文本格式";
            String content = "这是一封测试邮件，由JavaEmailDemo发送。\n\n祝好，\nJavaEmailDemo团队";

            boolean success = mailService.sendSimpleEmail(to, subject, content);
            log.info("简单文本邮件发送{}", success ? "成功" : "失败");
            //不停止程序
            Thread.sleep(500000);
    }

    /**
     * 批量发送邮件示例
     */
    @Test
    public void sendBatchEmails() throws InterruptedException {
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
