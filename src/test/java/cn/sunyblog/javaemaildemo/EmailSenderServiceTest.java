package cn.sunyblog.javaemaildemo;

import cn.sunyblog.javaemaildemo.api.EmailSenderService;
import cn.sunyblog.javaemaildemo.mail.EmailTemplateManager;
import cn.sunyblog.javaemaildemo.monitor.EmailSendMonitor;
import cn.sunyblog.javaemaildemo.strategy.EmailSendStrategyManager;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * 邮件发送服务测试类
 * 测试新的企业级邮件发送服务的各项功能
 * 
 * @author JavaEmailSpringBoot
 * @version 1.0.0
 */
@Slf4j
@SpringBootTest
class EmailSenderServiceTest {

    @Autowired
    private EmailSenderStarter emailSenderStarter;
    
    @Autowired
    private EmailSenderService emailSenderService;
    
    @Autowired
    private EmailTemplateManager templateManager;
    
    @Autowired
    private EmailSendStrategyManager strategyManager;
    
    @Autowired
    private EmailSendMonitor sendMonitor;

    /**
     * 测试连接状态
     */
    @Test
    public void testConnection() {
        log.info("=== 测试邮件服务器连接状态 ===");
        boolean connected = emailSenderStarter.checkConnection();
        log.info("邮件服务器连接状态: {}", connected ? "正常" : "异常");
        
        if (!connected) {
            log.warn("邮件服务器连接失败，请检查配置");
        }
    }

    /**
     * 测试基础邮件发送功能
     */
    @Test
    public void testBasicEmailSending() {
        log.info("=== 测试基础邮件发送功能 ===");
        
        String testEmail = "3379652824@qq.com";
        
        // 测试文本邮件
        log.info("1. 测试文本邮件发送");
        boolean textResult = emailSenderStarter.sendText(testEmail, 
                "测试文本邮件 - 新服务", 
                "这是使用新邮件发送服务发送的文本邮件。\n\n测试时间: " + new Date());
        log.info("文本邮件发送结果: {}", textResult ? "成功" : "失败");
        
        // 测试HTML邮件
        log.info("2. 测试HTML邮件发送");
        String htmlContent = ""
                + "<html><body>"
                + "<h2 style='color: #2E86AB;'>新邮件服务测试</h2>"
                + "<p>这是使用新邮件发送服务发送的HTML邮件。</p>"
                + "<div style='background-color: #f0f0f0; padding: 10px; margin: 10px 0;'>"
                + "<strong>测试时间:</strong> " + new Date()
                + "</div>"
                + "<p style='color: #666;'>如果您收到此邮件，说明新服务运行正常！</p>"
                + "</body></html>";
        
        boolean htmlResult = emailSenderStarter.sendHtml(testEmail, 
                "测试HTML邮件 - 新服务", htmlContent);
        log.info("HTML邮件发送结果: {}", htmlResult ? "成功" : "失败");
        
        // 测试批量发送
        log.info("3. 测试批量邮件发送");
        List<String> recipients = Arrays.asList(
                "3379652824@qq.com",
                "daliyuan702@sina.com"
        );
        boolean batchResult = emailSenderStarter.sendBatch(recipients, 
                "批量测试邮件 - 新服务", 
                "这是批量发送的测试邮件。发送时间: " + new Date());
        log.info("批量邮件发送结果: {}", batchResult ? "成功" : "失败");
    }

    /**
     * 测试模板邮件功能
     */
    @Test
    public void testTemplateEmail() {
        log.info("=== 测试模板邮件功能 ===");
        
        // 1. 测试内置模板
        log.info("1. 测试内置欢迎模板");
        Map<String, Object> welcomeVars = new HashMap<>();
        welcomeVars.put("username", "张三");
        
        boolean welcomeResult = emailSenderStarter.sendWithTemplate(
                "3379652824@qq.com", "welcome", welcomeVars);
        log.info("欢迎邮件发送结果: {}", welcomeResult ? "成功" : "失败");
        
        // 2. 测试验证码模板
        log.info("2. 测试验证码模板");
        Map<String, Object> codeVars = new HashMap<>();
        codeVars.put("code", "123456");
        codeVars.put("expireMinutes", "10");
        
        boolean codeResult = emailSenderStarter.sendWithTemplate(
                "3379652824@qq.com", "verification", codeVars);
        log.info("验证码邮件发送结果: {}", codeResult ? "成功" : "失败");
        
        // 3. 创建并测试自定义模板
        log.info("3. 测试自定义模板");
        emailSenderStarter.registerTemplate("test-custom", 
                "自定义测试邮件 - {{type}}", 
                "<h3>{{title}}</h3><p>{{message}}</p><p>发送时间: {{timestamp}}</p>", 
                true);
        
        Map<String, Object> customVars = new HashMap<>();
        customVars.put("type", "功能测试");
        customVars.put("title", "自定义模板测试");
        customVars.put("message", "这是一个自定义模板的测试邮件，用于验证模板功能是否正常。");
        customVars.put("timestamp", new Date().toString());
        
        boolean customResult = emailSenderStarter.sendWithTemplate(
                "3379652824@qq.com", "test-custom", customVars);
        log.info("自定义模板邮件发送结果: {}", customResult ? "成功" : "失败");
        
        // 4. 显示所有可用模板
        log.info("4. 当前可用模板: {}", emailSenderStarter.getTemplateNames());
    }

    /**
     * 测试异步发送功能
     */
    @Test
    public void testAsyncSending() throws Exception {
        log.info("=== 测试异步邮件发送功能 ===");
        
        // 测试异步发送
        log.info("1. 测试异步发送");
        CompletableFuture<Boolean> asyncResult = emailSenderStarter.sendAsync(
                "3379652824@qq.com", 
                "异步测试邮件 - 新服务", 
                "这是异步发送的测试邮件。发送时间: " + new Date());
        
        // 等待异步结果
        Boolean result = asyncResult.get(30, TimeUnit.SECONDS);
        log.info("异步邮件发送结果: {}", result ? "成功" : "失败");
        
        // 测试带回调的发送
        log.info("2. 测试带回调的异步发送");
        emailSenderStarter.sendWithCallback(
                "3379652824@qq.com", 
                "回调测试邮件 - 新服务", 
                "这是带回调的异步发送测试邮件。发送时间: " + new Date(),
                success -> log.info("回调 - 邮件发送成功!"),
                error -> log.error("回调 - 邮件发送失败: {}", error)
        );
        
        // 等待回调执行
        Thread.sleep(5000);
    }

    /**
     * 测试高级功能
     */
    @Test
    public void testAdvancedFeatures() {
        log.info("=== 测试高级功能 ===");
        
        // 1. 测试发送统计
        log.info("1. 发送统计信息:");
        log.info(emailSenderStarter.getStats());
        
        // 2. 测试线程池状态
        log.info("2. 线程池状态:");
        log.info(emailSenderStarter.getThreadPoolStatus());
        
        // 3. 测试监控功能
        log.info("3. 监控统计:");
        Map<String, Object> monitorStats = sendMonitor.getStatistics();
        log.info("总发送数: {}", monitorStats.get("totalSent"));
        log.info("成功数: {}", monitorStats.get("successCount"));
        log.info("失败数: {}", monitorStats.get("failureCount"));
        log.info("成功率: {:.2f}%", monitorStats.get("successRate"));
        log.info("平均耗时: {:.2f}ms", monitorStats.get("averageDuration"));
        
        // 4. 测试策略管理
        log.info("4. 策略统计:");
        Map<String, Long> strategyStats = strategyManager.getStrategyUsageStats();
        strategyStats.forEach((strategy, count) -> 
                log.info("策略 {} 使用次数: {}", strategy, count));
    }

    /**
     * 测试错误处理
     */
    @Test
    public void testErrorHandling() {
        log.info("=== 测试错误处理 ===");
        
        // 1. 测试无效邮箱地址
        log.info("1. 测试无效邮箱地址");
        boolean invalidEmailResult = emailSenderStarter.sendText(
                "invalid-email", "测试邮件", "测试内容");
        log.info("无效邮箱发送结果: {}", invalidEmailResult ? "成功" : "失败");
        
        // 2. 测试不存在的模板
        log.info("2. 测试不存在的模板");
        boolean invalidTemplateResult = emailSenderStarter.sendWithTemplate(
                "3379652824@qq.com", "non-existent-template", new HashMap<>());
        log.info("不存在模板发送结果: {}", invalidTemplateResult ? "成功" : "失败");
        
        // 3. 查看错误统计
        log.info("3. 错误统计:");
        Map<String, Object> errorMonitorStats = sendMonitor.getStatistics();
        log.info("失败数: {}", errorMonitorStats.get("failureCount"));
        Map<String, Long> errorStats = sendMonitor.getErrorStatistics();
        errorStats.forEach((errorType, count) -> 
                log.info("错误类型 {} 出现次数: {}", errorType, count));
    }

    /**
     * 综合测试
     */
    @Test
    public void testComprehensive() throws Exception {
        log.info("=== 综合功能测试 ===");
        
        String testEmail = "3379652824@qq.com";
        
        // 发送多种类型的邮件
        log.info("发送多种类型的邮件进行综合测试...");
        
        // 文本邮件
        emailSenderStarter.sendText(testEmail, "综合测试 - 文本", "文本邮件测试");
        
        // HTML邮件
        emailSenderStarter.sendHtml(testEmail, "综合测试 - HTML", 
                "<h3>HTML邮件测试</h3><p>测试时间: " + new Date() + "</p>");
        
        // 模板邮件
        Map<String, Object> vars = new HashMap<>();
        vars.put("username", "测试用户");
        emailSenderStarter.sendWithTemplate(testEmail, "welcome", vars);
        
        // 异步邮件
        emailSenderStarter.sendAsync(testEmail, "综合测试 - 异步", "异步邮件测试");
        
        // 等待所有邮件发送完成
        Thread.sleep(10000);
        
        // 输出最终统计
        log.info("=== 综合测试完成，最终统计 ===");
        log.info(emailSenderStarter.getStats());
        Map<String, Object> finalStats = sendMonitor.getStatistics();
        log.info("总发送数: {}", finalStats.get("totalSent"));
        log.info("成功率: {:.2f}%", finalStats.get("successRate"));
        
        // 输出详细统计
        log.info("详细统计: {}", finalStats);
    }
}