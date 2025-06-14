package cn.sunyblog.javaemaildemo.processor;

import cn.sunyblog.javaemaildemo.processor.annotation.EmailHandler;
import cn.sunyblog.javaemaildemo.processor.annotation.EmailProcessor;
import cn.sunyblog.javaemaildemo.processor.handler.EmailContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 测试专用邮件处理器
 * 用于演示注解驱动的邮件处理功能
 * 
 * @author suny
 * @version 1.0
 * @date 2025/06/14
 */
@Slf4j
@Component
@EmailProcessor(
    group = "test",
    description = "测试专用邮件处理器，用于演示注解功能",
    enabled = true
)
public class TestEmailProcessor2 {
    
    /**
     * 处理测试验证码邮件
     * 匹配主题包含"验证码"的邮件
     */
    @EmailHandler(
        name = "testVerificationCodeHandler",
        description = "测试验证码邮件处理器",
        subject = "(?i).*(验证码|verification|code).*",
        subjectMatchType = EmailHandler.MatchType.REGEX,
        priority = 50,
        async = false
    )
    public void handleTestVerificationCode(EmailContext context) {
        log.info("🧪 === 测试验证码邮件处理器 ===");
        log.info("📧 邮件主题: {}", context.getSubject());
        log.info("👤 发件人: {}", context.getSender());
        log.info("📝 邮件内容: {}", context.getContent());
        
        // 提取验证码
        String verificationCode = extractVerificationCode(context.getContent());
        if (verificationCode != null) {
            log.info("✅ 成功提取验证码: {}", verificationCode);
            log.info("💾 模拟将验证码存储到缓存中...");
            
            // 模拟业务逻辑
            try {
                Thread.sleep(500);
                log.info("📤 模拟发送验证码通知完成");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("处理被中断", e);
            }
        } else {
            log.warn("⚠️ 未能从邮件中提取到验证码");
        }
    }
    
    /**
     * 处理测试订单邮件
     * 匹配主题包含"订单"的邮件
     */
    @EmailHandler(
        name = "testOrderHandler",
        description = "测试订单邮件处理器",
        subject = "(?i).*(订单|order).*",
        subjectMatchType = EmailHandler.MatchType.REGEX,
        priority = 60,
        async = true
    )
    public void handleTestOrder(EmailContext context) {
        log.info("🧪 === 测试订单邮件处理器 ===");
        log.info("📧 邮件主题: {}", context.getSubject());
        log.info("👤 发件人: {}", context.getSender());
        log.info("📏 邮件大小: {} 字节", context.getSizeAsLong());
        
        // 提取订单号
        String orderNumber = extractOrderNumber(context.getContent());
        if (orderNumber != null) {
            log.info("✅ 提取到订单号: {}", orderNumber);
        }
        
        // 模拟异步处理
        try {
            Thread.sleep(1000);
            log.info("📦 测试订单邮件处理完成");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("订单邮件处理被中断", e);
        }
    }
    
    /**
     * 默认测试处理器
     * 处理所有其他邮件
     */
    @EmailHandler(
        name = "testDefaultHandler",
        description = "测试默认邮件处理器",
        priority = 999,
        async = true
    )
    public void handleTestDefault(EmailContext context) {
        log.info("🧪 === 测试默认邮件处理器 ===");
        log.info("📧 邮件主题: {}", context.getSubject());
        log.info("👤 发件人: {}", context.getSender());
        log.info("📝 使用测试默认处理逻辑处理邮件");
    }
    
    /**
     * 提取验证码
     */
    private String extractVerificationCode(String content) {
        if (content == null || content.trim().isEmpty()) {
            return null;
        }
        
        // 验证码提取模式
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
        if (content == null || content.trim().isEmpty()) {
            return null;
        }
        
        // 订单号提取模式
        String[] patterns = {
            "(?i)(?:订单号|order\\s*number|order\\s*id)[：:：\\s]*([A-Z0-9]{6,20})",
            "(?i)(?:订单|order)[：:：\\s]*([A-Z0-9]{6,20})",
            "\\b(ORDER[A-Z0-9]{6,15})\\b"
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
}