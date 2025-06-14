package cn.sunyblog.javaemaildemo.example;

import cn.sunyblog.javaemaildemo.processor.annotation.EmailHandler;
import cn.sunyblog.javaemaildemo.processor.annotation.EmailProcessor;
import cn.sunyblog.javaemaildemo.processor.handler.EmailContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.mail.Message;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 注解驱动邮件处理器示例
 * 展示如何使用 @EmailProcessor 和 @EmailHandler 注解来处理不同类型的邮件
 * 
 * @author suny
 * @version 1.0
 * @date 2025/06/14
 */
@Slf4j
@Component
@EmailProcessor(
    group = "example",
    description = "示例邮件处理器，展示注解驱动的邮件处理功能",
    enabled = true
)
public class AnnotationEmailProcessorExample {
    
    /**
     * 处理验证码邮件
     * 匹配主题包含"验证码"或"verification"的邮件
     */
    @EmailHandler(
        name = "verificationCodeHandler",
        description = "处理验证码邮件",
        subject = "(?i).*(验证码|verification|code).*",
        subjectMatchType = EmailHandler.MatchType.REGEX,
        priority = 100,
        async = false
    )
    public void handleVerificationCode(EmailContext context) {
        log.info("=== 验证码邮件处理器 ===");
        log.info("邮件主题: {}", context.getSubject());
        log.info("发件人: {}", context.getSender());
        
        // 提取验证码
        String verificationCode = extractVerificationCode(context.getContent());
        if (verificationCode != null) {
            log.info("✅ 成功提取验证码: {}", verificationCode);
            
            // 这里可以添加业务逻辑，比如：
            // - 将验证码存储到缓存中
            // - 发送通知给相关用户
            // - 触发后续的业务流程
            
        } else {
            log.warn("⚠️ 未能从邮件中提取到验证码");
        }
    }
    
    /**
     * 处理订单相关邮件
     * 匹配主题包含"订单"、"order"或"购买"的邮件
     */
    @EmailHandler(
        name = "orderHandler",
        description = "处理订单相关邮件",
        subject = "(?i).*(订单|order|购买|purchase|支付|payment).*",
        subjectMatchType = EmailHandler.MatchType.REGEX,
        priority = 90,
        async = true
    )
    public void handleOrderEmail(EmailContext context) {
        log.info("=== 订单邮件处理器 ===");
        log.info("邮件主题: {}", context.getSubject());
        log.info("发件人: {}", context.getSender());
        log.info("邮件内容长度: {} 字符", context.getContent().length());
        
        // 提取订单号
        String orderNumber = extractOrderNumber(context.getContent());
        if (orderNumber != null) {
            log.info("✅ 提取到订单号: {}", orderNumber);
        }
        
        // 模拟异步处理
        try {
            Thread.sleep(1000);
            log.info("📦 订单邮件处理完成");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("订单邮件处理被中断", e);
        }
    }
    
    /**
     * 处理通知类邮件
     * 匹配发件人包含"noreply"或"notification"的邮件
     */
    @EmailHandler(
        name = "notificationHandler",
        description = "处理系统通知邮件",
        from = "(?i).*(noreply|notification|system|admin).*",
        fromMatchType = EmailHandler.MatchType.REGEX,
        priority = 80,
        async = true
    )
    public void handleNotification(EmailContext context) {
        log.info("=== 通知邮件处理器 ===");
        log.info("邮件主题: {}", context.getSubject());
        log.info("发件人: {}", context.getSender());
        
        // 分析通知类型
        String notificationType = analyzeNotificationType(context.getSubject(), context.getContent());
        log.info("📢 通知类型: {}", notificationType);
        
        // 这里可以根据不同的通知类型执行不同的处理逻辑
        switch (notificationType) {
            case "安全警告":
                log.warn("🔒 检测到安全相关通知");
                break;
            case "账户变更":
                log.info("👤 检测到账户变更通知");
                break;
            case "系统维护":
                log.info("🔧 检测到系统维护通知");
                break;
            default:
                log.info("📋 普通系统通知");
                break;
        }
    }
    
    /**
     * 处理测试邮件
     * 使用传统的Message参数方式
     */
    @EmailHandler(
        name = "testHandler",
        description = "处理测试邮件（传统参数方式）",
        subject = "(?i).*test.*",
        subjectMatchType = EmailHandler.MatchType.REGEX,
        priority = 70,
        async = false
    )
    public void handleTestEmail(Message message, String subject, String sender, String content) {
        log.info("=== 测试邮件处理器（传统方式） ===");
        log.info("邮件主题: {}", subject);
        log.info("发件人: {}", sender);
        log.info("内容预览: {}", content.substring(0, Math.min(100, content.length())));
        
        try {
            log.info("📧 邮件接收时间: {}", message.getReceivedDate());
            log.info("📤 邮件发送时间: {}", message.getSentDate());
        } catch (Exception e) {
            log.error("获取邮件时间信息失败", e);
        }
    }
    
    /**
     * 默认处理器 - 处理所有其他邮件
     * 优先级最低，作为兜底处理
     */
    @EmailHandler(
        name = "defaultHandler",
        description = "默认邮件处理器",
        subject = ".*",
        subjectMatchType = EmailHandler.MatchType.REGEX,
        priority = 10,
        async = true
    )
    public void handleDefaultEmail(EmailContext context) {
        log.info("=== 默认邮件处理器 ===");
        log.info("邮件主题: {}", context.getSubject());
        log.info("发件人: {}", context.getSender());
        log.info("邮件大小: {} 字节", context.getSize());
        
        if (context.getAttachments() != null && !context.getAttachments().isEmpty()) {
            log.info("📎 包含 {} 个附件", context.getAttachments().size());
        }
        
        log.info("📝 使用默认处理逻辑处理邮件");
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
     * 提取订单号
     */
    private String extractOrderNumber(String content) {
        String[] patterns = {
            "(?i)(?:订单号|order\\s*number|order\\s*id)[：:：\\s]*([A-Z0-9]{8,20})",
            "(?i)(?:订单|order)[：:：\\s]*([A-Z0-9]{10,20})"
        };
        
        for (String pattern : patterns) {
            Pattern regex = Pattern.compile(pattern);
            Matcher matcher = regex.matcher(content);
            if (matcher.find()) {
                return matcher.group(1);
            }
        }
        
        return null;
    }
    
    /**
     * 分析通知类型
     */
    private String analyzeNotificationType(String subject, String content) {
        String fullText = (subject + " " + content).toLowerCase();
        
        if (fullText.contains("安全") || fullText.contains("security") || fullText.contains("警告") || fullText.contains("warning")) {
            return "安全警告";
        } else if (fullText.contains("账户") || fullText.contains("account") || fullText.contains("profile")) {
            return "账户变更";
        } else if (fullText.contains("维护") || fullText.contains("maintenance") || fullText.contains("升级") || fullText.contains("upgrade")) {
            return "系统维护";
        } else {
            return "其他通知";
        }
    }
}