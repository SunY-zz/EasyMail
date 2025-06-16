//package cn.sunyblog.javaemaildemo;
//
//import cn.sunyblog.javaemaildemo.api.EmailSenderService;
//import cn.sunyblog.javaemaildemo.send.template.EmailTemplate;
//import cn.sunyblog.javaemaildemo.send.template.EmailTemplateManager;
//import cn.sunyblog.javaemaildemo.send.SendResult;
//import cn.sunyblog.javaemaildemo.monitor.EmailSendMonitor;
//import lombok.extern.slf4j.Slf4j;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.context.SpringBootTest;
//
//import java.io.File;
//import java.util.*;
//import java.util.concurrent.CompletableFuture;
//import java.util.concurrent.TimeUnit;
//
///**
// * 邮件发送集成测试类
// * 测试完整的邮件发送流程和各种功能
// *
// * @author JavaEmailSpringBoot
// * @version 1.0.0
// */
//@Slf4j
//@SpringBootTest
//class EmailIntegrationTest {
//
//    @Autowired
//    private EmailSenderService emailSenderService;
//
//    @Autowired
//    private EmailTemplateManager templateManager;
//
//    @Autowired
//    private EmailSendMonitor emailSendMonitor;
//
//    /**
//     * 测试基本文本邮件发送
//     */
//    @Test
//    public void testSendTextEmail() {
//        log.info("=== 测试基本文本邮件发送 ===");
//
//        try {
//            SendResult result = emailSenderService.sendText(
//                    "3379652824@qq.com",
//                    "测试文本邮件",
//                    "这是一封测试文本邮件，用于验证邮件发送功能是否正常。\n\n发送时间: " + new Date()
//            );
//
//            log.info("文本邮件发送结果: {}", result.isSuccess() ? "成功" : "失败");
//            if (result.isSuccess()) {
//                log.info("消息ID: {}", result.getMessageId());
//                log.info("发送耗时: {}ms", result.getDuration());
//            } else {
//                log.error("发送失败: {}", result.getErrorMessage());
//            }
//        } catch (Exception e) {
//            log.error("发送文本邮件时出现异常", e);
//        }
//    }
//
//    /**
//     * 测试HTML邮件发送
//     */
//    @Test
//    public void testSendHtmlEmail() {
//        log.info("=== 测试HTML邮件发送 ===");
//
//        String htmlContent = ""
//                + "<html><body>"
//                + "<h2 style='color: #2c3e50;'>HTML邮件测试</h2>"
//                + "<p>这是一封<strong>HTML格式</strong>的测试邮件。</p>"
//                + "<div style='background: #ecf0f1; padding: 15px; border-radius: 5px;'>"
//                + "<h3>功能特点:</h3>"
//                + "<ul>"
//                + "<li>支持HTML格式</li>"
//                + "<li>支持CSS样式</li>"
//                + "<li>支持图片和链接</li>"
//                + "</ul>"
//                + "</div>"
//                + "<p>发送时间: <em>" + new Date() + "</em></p>"
//                + "<hr>"
//                + "<p><small>这是一封自动生成的测试邮件</small></p>"
//                + "</body></html>";
//
//        try {
//            SendResult result = emailSenderService.sendHtml(
//                    "3379652824@qq.com",
//                    "测试HTML邮件",
//                    htmlContent
//            );
//
//            log.info("HTML邮件发送结果: {}", result.isSuccess() ? "成功" : "失败");
//            if (result.isSuccess()) {
//                log.info("消息ID: {}", result.getMessageId());
//                log.info("发送耗时: {}ms", result.getDuration());
//            } else {
//                log.error("发送失败: {}", result.getErrorMessage());
//            }
//        } catch (Exception e) {
//            log.error("发送HTML邮件时出现异常", e);
//        }
//    }
//
//    /**
//     * 测试多收件人邮件发送
//     */
//    @Test
//    public void testSendToMultipleRecipients() {
//        log.info("=== 测试多收件人邮件发送 ===");
//
//        List<String> toList = Arrays.asList("3379652824@qq.com", "daliyuan702@gamil.com");
//        List<String> ccList = Arrays.asList("2189463356@qq.com");
//        List<String> bccList = Arrays.asList("daliyuan702@gamil.com");
//
//        try {
//            SendResult result = emailSenderService.send(
//                    toList,
//                    ccList,
//                    bccList,
//                    "多收件人测试邮件",
//                    "这是一封发送给多个收件人的测试邮件。\n\n" +
//                    "TO: " + String.join(", ", toList) + "\n" +
//                    "CC: " + String.join(", ", ccList) + "\n" +
//                    "BCC: " + String.join(", ", bccList) + "\n\n" +
//                    "发送时间: " + new Date(),
//                    false,
//                    null
//            );
//
//            log.info("多收件人邮件发送结果: {}", result.isSuccess() ? "成功" : "失败");
//            if (result.isSuccess()) {
//                log.info("消息ID: {}", result.getMessageId());
//                log.info("总收件人数: {}", result.getTotalRecipientCount());
//                log.info("发送耗时: {}ms", result.getDuration());
//            } else {
//                log.error("发送失败: {}", result.getErrorMessage());
//            }
//        } catch (Exception e) {
//            log.error("发送多收件人邮件时出现异常", e);
//        }
//    }
//
//    /**
//     * 测试异步邮件发送
//     */
//    @Test
//    public void testSendAsyncEmail() {
//        log.info("=== 测试异步邮件发送 ===");
//
//        try {
//            CompletableFuture<SendResult> future = emailSenderService.sendTextAsync(
//                    "3379652824@qq.com",
//                    "异步邮件测试",
//                    "这是一封异步发送的测试邮件。\n\n发送时间: " + new Date()
//            );
//
//            log.info("异步邮件发送请求已提交");
//
//            // 等待异步结果
//            SendResult result = future.get(30, TimeUnit.SECONDS);
//
//            log.info("异步邮件发送结果: {}", result.isSuccess() ? "成功" : "失败");
//            if (result.isSuccess()) {
//                log.info("消息ID: {}", result.getMessageId());
//                log.info("发送耗时: {}ms", result.getDuration());
//            } else {
//                log.error("发送失败: {}", result.getErrorMessage());
//            }
//        } catch (Exception e) {
//            log.error("异步发送邮件时出现异常", e);
//        }
//    }
//
//    /**
//     * 测试模板邮件发送
//     */
//    @Test
//    public void testSendTemplateEmail() {
//        log.info("=== 测试模板邮件发送 ===");
//
//        // 创建测试模板
//        EmailTemplate testTemplate = EmailTemplate.builder()
//                .templateId("integration-test")
//                .templateName("集成测试模板")
//                .subjectTemplate("${type}通知 - ${title}")
//                .contentTemplate(""
//                        + "<html><body>"
//                        + "<h2>尊敬的 ${username}:</h2>"
//                        + "<p>您有一条新的${type}通知:</p>"
//                        + "<div style='border: 1px solid #ddd; padding: 15px; margin: 10px 0;'>"
//                        + "<h3>${title}</h3>"
//                        + "<p>${content}</p>"
//                        + "<p><strong>优先级:</strong> ${priority}</p>"
//                        + "<p><strong>时间:</strong> ${timestamp}</p>"
//                        + "</div>"
//                        + "<p>请及时处理。</p>"
//                        + "<hr>"
//                        + "<p><small>此邮件由系统自动发送，请勿回复。</small></p>"
//                        + "</body></html>")
//                .isHtml(true)
//                .build();
//
//        // 注册模板
//        templateManager.registerTemplate(testTemplate);
//
//        // 准备模板变量
//        Map<String, Object> variables = new HashMap<>();
//        variables.put("username", "测试用户");
//        variables.put("type", "系统");
//        variables.put("title", "重要更新通知");
//        variables.put("content", "系统将在今晚22:00-24:00进行维护升级，期间可能影响正常使用。");
//        variables.put("priority", "高");
//        variables.put("timestamp", new Date());
//
//        try {
//            EmailTemplate template = templateManager.getTemplate("integration-test");
//            SendResult result = emailSenderService.sendWithTemplate(
//                    "3379652824@qq.com",
//                    template,
//                    variables
//            );
//
//            log.info("模板邮件发送结果: {}", result.isSuccess() ? "成功" : "失败");
//            if (result.isSuccess()) {
//                log.info("消息ID: {}", result.getMessageId());
//                log.info("发送耗时: {}ms", result.getDuration());
//            } else {
//                log.error("发送失败: {}", result.getErrorMessage());
//            }
//        } catch (Exception e) {
//            log.error("发送模板邮件时出现异常", e);
//        }
//    }
//
//    /**
//     * 测试批量模板邮件发送
//     */
//    @Test
//    public void testSendBatchTemplateEmail() {
//        log.info("=== 测试批量模板邮件发送 ===");
//
//        // 准备批量发送数据
//        List<Map<String, Object>> batchData = new ArrayList<>();
//
//        // 第一个收件人
//        Map<String, Object> data1 = new HashMap<>();
//        data1.put("email", "3379652824@qq.com");
//        data1.put("username", "张三");
//        data1.put("type", "账单");
//        data1.put("title", "月度账单通知");
//        data1.put("content", "您的1月份账单已生成，总金额: ¥1,234.56");
//        data1.put("priority", "中");
//        data1.put("timestamp", new Date());
//        batchData.add(data1);
//
//        // 第二个收件人
//        Map<String, Object> data2 = new HashMap<>();
//        data2.put("email", "3379652824@qq.com");
//        data2.put("username", "李四");
//        data2.put("type", "安全");
//        data2.put("title", "登录异常提醒");
//        data2.put("content", "检测到您的账号在异地登录，如非本人操作请及时修改密码。");
//        data2.put("priority", "高");
//        data2.put("timestamp", new Date());
//        batchData.add(data2);
//
//        // 第三个收件人
//        Map<String, Object> data3 = new HashMap<>();
//        data3.put("email", "3379652824@qq.com");
//        data3.put("username", "王五");
//        data3.put("type", "活动");
//        data3.put("title", "新春活动邀请");
//        data3.put("content", "新春佳节即将到来，我们为您准备了丰厚的活动奖品，快来参与吧！");
//        data3.put("priority", "低");
//        data3.put("timestamp", new Date());
//        batchData.add(data3);
//
//        try {
//            // 准备批量发送数据映射
//            Map<String, Map<String, Object>> recipients = new HashMap<>();
//            for (Map<String, Object> data : batchData) {
//                String email = (String) data.get("email");
//                Map<String, Object> variables = new HashMap<>(data);
//                variables.remove("email"); // 移除email字段，只保留模板变量
//                recipients.put(email, variables);
//            }
//
//            EmailTemplate template = templateManager.getTemplate("integration-test");
//            SendResult result = emailSenderService.sendBatchWithTemplate(
//                    recipients,
//                    template
//            );
//
//            log.info("批量模板邮件发送结果: {}", result.isSuccess() ? "成功" : "失败");
//            if (result.isSuccess()) {
//                log.info("批量发送ID: {}", result.getMessageId());
//                log.info("总收件人数: {}", result.getTotalRecipientCount());
//                log.info("成功率: {}%", String.format("%.2f", result.getSuccessRate() * 100));
//                log.info("发送耗时: {}ms", result.getDuration());
//
//                // 显示批量结果详情
//                if (result.getBatchResults() != null) {
//                    log.info("批量发送详情:");
//                    result.getBatchResults().forEach((email, success) -> {
//                        log.info("  {}: {}", email, success ? "成功" : "失败");
//                    });
//                }
//            } else {
//                log.error("批量发送失败: {}", result.getErrorMessage());
//            }
//        } catch (Exception e) {
//            log.error("批量发送模板邮件时出现异常", e);
//        }
//    }
//
//    /**
//     * 测试连接检查
//     */
//    @Test
//    public void testConnectionCheck() {
//        log.info("=== 测试连接检查 ===");
//
//        try {
//            boolean connected = emailSenderService.checkConnection();
//            log.info("SMTP连接检查结果: {}", connected ? "连接正常" : "连接失败");
//        } catch (Exception e) {
//            log.error("检查SMTP连接时出现异常", e);
//        }
//    }
//
//    /**
//     * 测试发送统计
//     */
//    @Test
//    public void testSendingStats() {
//        log.info("=== 测试发送统计 ===");
//
//        try {
//            String stats = emailSenderService.getSendingStats();
//
//            log.info("邮件发送统计信息: {}", stats);
//        } catch (Exception e) {
//            log.error("获取发送统计时出现异常", e);
//        }
//    }
//
//    /**
//     * 测试监控功能
//     */
//    @Test
//    public void testMonitoring() {
//        log.info("=== 测试监控功能 ===");
//
//        try {
//            // 获取监控统计
//            Map<String, Object> monitorStats = emailSendMonitor.getStatistics();
//
//            log.info("邮件发送监控统计:");
//            monitorStats.forEach((key, value) -> {
//                log.info("  {}: {}", key, value);
//            });
//
//            // 获取最近的发送记录
//            log.info("最近发送记录:");
//            // 这里可以添加获取最近发送记录的代码
//
//        } catch (Exception e) {
//            log.error("获取监控信息时出现异常", e);
//        }
//    }
//
//    /**
//     * 测试错误处理
//     */
//    @Test
//    public void testErrorHandling() {
//        log.info("=== 测试错误处理 ===");
//
//        // 测试无效邮箱地址
//        try {
//            SendResult result = emailSenderService.sendText(
//                    "invalid-email",
//                    "错误测试",
//                    "测试无效邮箱地址的错误处理"
//            );
//
//            log.info("无效邮箱测试结果: {}", result.isSuccess() ? "成功" : "失败");
//            if (!result.isSuccess()) {
//                log.info("预期的错误信息: {}", result.getErrorMessage());
//            }
//        } catch (Exception e) {
//            log.info("捕获到预期的异常: {}", e.getMessage());
//        }
//
//        // 测试空内容
//        try {
//            SendResult result = emailSenderService.sendText(
//                    "3379652824@qq.com",
//                    "",
//                    ""
//            );
//
//            log.info("空内容测试结果: {}", result.isSuccess() ? "成功" : "失败");
//            if (!result.isSuccess()) {
//                log.info("预期的错误信息: {}", result.getErrorMessage());
//            }
//        } catch (Exception e) {
//            log.info("捕获到预期的异常: {}", e.getMessage());
//        }
//
//        // 测试不存在的模板
//        try {
//            Map<String, Object> vars = new HashMap<>();
//            vars.put("test", "value");
//
//            EmailTemplate nonExistentTemplate = templateManager.getTemplate("non-existent-template");
//            SendResult result = emailSenderService.sendWithTemplate(
//                    "test@example.com",
//                    nonExistentTemplate,
//                    vars
//            );
//
//            log.info("不存在模板测试结果: {}", result.isSuccess() ? "成功" : "失败");
//            if (!result.isSuccess()) {
//                log.info("预期的错误信息: {}", result.getErrorMessage());
//            }
//        } catch (Exception e) {
//            log.info("捕获到预期的异常: {}", e.getMessage());
//        }
//    }
//
//    /**
//     * 测试性能
//     */
//    @Test
//    public void testPerformance() {
//        log.info("=== 测试性能 ===");
//
//        int testCount = 5;
//        long totalTime = 0;
//        int successCount = 0;
//
//        for (int i = 0; i < testCount; i++) {
//            try {
//                long startTime = System.currentTimeMillis();
//
//                SendResult result = emailSenderService.sendText(
//                        "3379652824@qq.com",
//                        "性能测试邮件 #" + (i + 1),
//                        "这是第 " + (i + 1) + " 封性能测试邮件。\n发送时间: " + new Date()
//                );
//
//                long endTime = System.currentTimeMillis();
//                long duration = endTime - startTime;
//                totalTime += duration;
//
//                if (result.isSuccess()) {
//                    successCount++;
//                    log.info("第{}封邮件发送成功，耗时: {}ms", i + 1, duration);
//                } else {
//                    log.warn("第{}封邮件发送失败: {}", i + 1, result.getErrorMessage());
//                }
//
//                // 间隔一秒
//                Thread.sleep(1000);
//
//            } catch (Exception e) {
//                log.error("第{}封邮件发送异常", i + 1, e);
//            }
//        }
//
//        double avgTime = (double) totalTime / testCount;
//        double successRate = (double) successCount / testCount * 100;
//
//        log.info("性能测试结果:");
//        log.info("  总发送数: {}", testCount);
//        log.info("  成功数: {}", successCount);
//        log.info("  成功率: {:.2f}%", successRate);
//        log.info("  平均耗时: {:.2f}ms", avgTime);
//        log.info("  总耗时: {}ms", totalTime);
//    }
//}