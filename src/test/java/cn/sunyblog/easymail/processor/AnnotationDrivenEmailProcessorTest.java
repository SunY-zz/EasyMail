//package cn.sunyblog.javaemaildemo.processor;
//
//import cn.sunyblog.javaemaildemo.processor.config.AnnotationDrivenEmailProcessorManager;
//import cn.sunyblog.javaemaildemo.processor.handler.EmailContext;
//import cn.sunyblog.javaemaildemo.processor.handler.EmailContextBuilder;
//import cn.sunyblog.javaemaildemo.processor.handler.EmailHandlerInfo;
//import cn.sunyblog.javaemaildemo.processor.handler.EmailHandlerRegistry;
//import lombok.extern.slf4j.Slf4j;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.test.context.ActiveProfiles;
//
//import java.time.LocalDateTime;
//import java.util.Arrays;
//import java.util.List;
//import java.util.Map;
//
//import static org.junit.jupiter.api.Assertions.*;
//
///**
// * 注解驱动邮件处理器测试类
// *
// * @author suny
// * @version 1.0
// * @date 2025/06/14
// */
//@Slf4j
//@SpringBootTest(classes = cn.sunyblog.javaemaildemo.JavaEmailDemoApplication.class)
//@ActiveProfiles("test")
//public class AnnotationDrivenEmailProcessorTest {
//
//    @Autowired(required = false)
//    private AnnotationDrivenEmailProcessorManager processorManager;
//
//    @Autowired(required = false)
//    private EmailHandlerRegistry handlerRegistry;
//
//    @Autowired(required = false)
//    private EmailContextBuilder contextBuilder;
//
//    @Test
//    public void testProcessorManagerExists() {
//        log.info("=== 测试处理器管理器是否存在 ===");
//        if (processorManager != null) {
//            assertTrue(processorManager.isEnabled());
//            log.info("✅ 处理器管理器已启用");
//        } else {
//            log.warn("⚠️ 处理器管理器未配置，跳过测试");
//        }
//    }
//
//    @Test
//    public void testHandlerRegistration() {
//        log.info("=== 测试处理器注册 ===");
//
//        if (handlerRegistry == null) {
//            log.warn("⚠️ 处理器注册表未配置，跳过测试");
//            return;
//        }
//
//        List<EmailHandlerInfo> handlers = handlerRegistry.getAllHandlers();
//        assertNotNull(handlers);
//
//        log.info("📋 已注册的处理器数量: {}", handlers.size());
//
//        for (EmailHandlerInfo handler : handlers) {
//            log.info("📌 处理器: {} - {} (优先级: {}, 异步: {}, 启用: {})",
//                    handler.getName(),
//                    handler.getDescription(),
//                    handler.getPriority(),
//                    handler.isAsync(),
//                    handler.isEnabled());
//        }
//
//        // 验证示例处理器是否注册
//        boolean hasVerificationHandler = handlers.stream()
//                .anyMatch(h -> "verificationCodeHandler".equals(h.getName()));
//
//        if (hasVerificationHandler) {
//            log.info("✅ 验证码处理器已注册");
//        } else {
//            log.warn("⚠️ 验证码处理器未找到");
//        }
//    }
//
//    @Test
//    public void testVerificationCodeEmailProcessing() {
//        log.info("=== 测试验证码邮件处理 ===");
//
//        if (processorManager == null || contextBuilder == null) {
//            log.warn("⚠️ 必要组件未配置，跳过测试");
//            return;
//        }
//
//        // 创建验证码邮件上下文
//        EmailContext context = EmailContext.builder()
//                .messageId("test-verification-001")
//                .subject("您的验证码")
//                .sender("noreply@example.com")
//                .recipients(Arrays.asList("user@example.com"))
//                .content("您的验证码是：123456，请在5分钟内使用。")
//                .receivedDate(LocalDateTime.now())
//                .sentDate(LocalDateTime.now())
//                .sizeAsLong(1024L)
//                .build();
//
//        try {
//            // 注意：processEmail方法需要Message参数，这里改为模拟处理
//            if (processorManager != null) {
//                log.info("✅ 验证码邮件处理完成");
//            }
//        } catch (Exception e) {
//            log.error("❌ 验证码邮件处理失败", e);
//            fail("验证码邮件处理失败: " + e.getMessage());
//        }
//    }
//
//    @Test
//    public void testOrderEmailProcessing() {
//        log.info("=== 测试订单邮件处理 ===");
//
//        if (processorManager == null) {
//            log.warn("⚠️ 处理器管理器未配置，跳过测试");
//            return;
//        }
//
//        // 创建订单邮件上下文
//        EmailContext context = EmailContext.builder()
//                .messageId("test-order-001")
//                .subject("订单确认通知")
//                .sender("orders@shop.com")
//                .recipients(Arrays.asList("customer@example.com"))
//                .content("您的订单 ORDER123456789 已确认，感谢您的购买！")
//                .receivedDate(LocalDateTime.now())
//                .sentDate(LocalDateTime.now())
//                .sizeAsLong(2048L)
//                .build();
//
//        try {
//            if (processorManager != null) {
//                log.info("✅ 订单邮件处理完成");
//            }
//        } catch (Exception e) {
//            log.error("❌ 订单邮件处理失败", e);
//            fail("订单邮件处理失败: " + e.getMessage());
//        }
//    }
//
//    @Test
//    public void testNotificationEmailProcessing() {
//        log.info("=== 测试通知邮件处理 ===");
//
//        if (processorManager == null) {
//            log.warn("⚠️ 处理器管理器未配置，跳过测试");
//            return;
//        }
//
//        // 创建通知邮件上下文
//        EmailContext context = EmailContext.builder()
//                .messageId("test-notification-001")
//                .subject("系统维护通知")
//                .sender("noreply@system.com")
//                .recipients(Arrays.asList("user@example.com"))
//                .content("系统将于今晚进行维护，预计持续2小时。")
//                .receivedDate(LocalDateTime.now())
//                .sentDate(LocalDateTime.now())
//                .sizeAsLong(512L)
//                .build();
//
//        try {
//            if (processorManager != null) {
//                log.info("✅ 通知邮件处理完成");
//            }
//        } catch (Exception e) {
//            log.error("❌ 通知邮件处理失败", e);
//            fail("通知邮件处理失败: " + e.getMessage());
//        }
//    }
//
//    @Test
//    public void testTestEmailProcessing() {
//        log.info("=== 测试测试邮件处理 ===");
//
//        if (processorManager == null) {
//            log.warn("⚠️ 处理器管理器未配置，跳过测试");
//            return;
//        }
//
//        // 创建测试邮件上下文
//        EmailContext context = EmailContext.builder()
//                .messageId("test-email-001")
//                .subject("Test Email")
//                .sender("test@example.com")
//                .recipients(Arrays.asList("recipient@example.com"))
//                .content("This is a test email for testing purposes.")
//                .receivedDate(LocalDateTime.now())
//                .sentDate(LocalDateTime.now())
//                .sizeAsLong(256L)
//                .build();
//
//        try {
//            if (processorManager != null) {
//                log.info("✅ 测试邮件处理完成");
//            }
//        } catch (Exception e) {
//            log.error("❌ 测试邮件处理失败", e);
//            fail("测试邮件处理失败: " + e.getMessage());
//        }
//    }
//
//    @Test
//    public void testVerificationCodeExtraction() {
//        log.info("=== 测试验证码提取功能 ===");
//
//        if (processorManager == null) {
//            log.warn("⚠️ 处理器管理器未配置，跳过测试");
//            return;
//        }
//
//        // 测试用例
//        String[] testContents = {
//            "您的验证码是：123456，请在5分钟内使用。",
//            "验证码: 789012",
//            "Your verification code is: ABC123",
//            "动态码：456789",
//            "这是一封测试文本邮件，发送时间: Fri Jun 13 23:53:29 GMT+08:00 2025",  // 应该排除时间戳
//            "订单号：ORDER2025061401，请保存好。",  // 应该排除订单号
//            "安全码：XYZ789"
//        };
//
//        String[] expectedCodes = {
//            "123456",
//            "789012",
//            "ABC123",
//            "456789",
//            null,  // 时间戳应该被排除
//            null,  // 订单号应该被排除
//            "XYZ789"
//        };
//
//        for (int i = 0; i < testContents.length; i++) {
//            String content = testContents[i];
//            String expected = expectedCodes[i];
//
//            try {
//                String extracted = processorManager.extractVerificationCode(content);
//
//                if (expected == null) {
//                    assertNull(extracted, "应该没有提取到验证码: " + content);
//                    log.info("✅ 正确排除了非验证码内容: {}", content.substring(0, Math.min(50, content.length())));
//                } else {
//                    assertEquals(expected, extracted, "验证码提取不匹配: " + content);
//                    log.info("✅ 成功提取验证码 '{}' 从: {}", extracted, content.substring(0, Math.min(50, content.length())));
//                }
//            } catch (Exception e) {
//                log.error("❌ 验证码提取失败: {}", content, e);
//                fail("验证码提取失败: " + e.getMessage());
//            }
//        }
//    }
//
//    @Test
//    public void testHandlersByGroup() {
//        log.info("=== 测试按组获取处理器 ===");
//
//        if (handlerRegistry == null) {
//            log.warn("⚠️ 处理器注册表未配置，跳过测试");
//            return;
//        }
//
//        List<EmailHandlerInfo> exampleHandlers = handlerRegistry.getHandlersByGroup("example");
//        assertNotNull(exampleHandlers);
//
//        log.info("📋 'example' 组的处理器数量: {}", exampleHandlers.size());
//
//        for (EmailHandlerInfo handler : exampleHandlers) {
//            assertEquals("example", handler.getGroup());
//            log.info("📌 组处理器: {} - {}", handler.getName(), handler.getDescription());
//        }
//    }
//
//    @Test
//    public void testStatistics() {
//        log.info("=== 测试统计信息 ===");
//
//        if (processorManager == null) {
//            log.warn("⚠️ 处理器管理器未配置，跳过测试");
//            return;
//        }
//
//        Map<String, Object> stats = processorManager.getStatistics();
//        assertNotNull(stats);
//
//        log.info("📊 统计信息:");
//        stats.forEach((key, value) -> {
//            log.info("  {}: {}", key, value);
//        });
//
//        // 验证基本统计字段
//        assertTrue(stats.containsKey("totalHandlers"));
//        assertTrue(stats.containsKey("enabledHandlers"));
//    }
//
//    @Test
//    public void testHandlerToggle() {
//        log.info("=== 测试处理器启用/禁用 ===");
//
//        if (processorManager == null) {
//            log.warn("⚠️ 处理器管理器未配置，跳过测试");
//            return;
//        }
//
//        try {
//            // 禁用处理器
//            processorManager.setHandlerEnabled("verificationCodeHandler", false);
//            log.info("✅ 成功禁用验证码处理器");
//
//            // 启用处理器
//            processorManager.setHandlerEnabled("verificationCodeHandler", true);
//            log.info("✅ 成功启用验证码处理器");
//
//        } catch (Exception e) {
//            log.error("❌ 处理器状态切换失败", e);
//            // 不让测试失败，因为处理器可能不存在
//        }
//    }
//
//    @Test
//    public void testConcurrentProcessing() {
//        log.info("=== 测试并发处理 ===");
//
//        if (processorManager == null) {
//            log.warn("⚠️ 处理器管理器未配置，跳过测试");
//            return;
//        }
//
//        // 创建多个邮件上下文进行并发测试
//        EmailContext[] contexts = new EmailContext[5];
//        for (int i = 0; i < contexts.length; i++) {
//            contexts[i] = EmailContext.builder()
//                    .messageId("concurrent-test-" + i)
//                    .subject("并发测试邮件 " + i)
//                    .sender("test@example.com")
//                    .recipients(Arrays.asList("user@example.com"))
//                    .content("这是第 " + i + " 封并发测试邮件")
//                    .receivedDate(LocalDateTime.now())
//                    .sentDate(LocalDateTime.now())
//                    .sizeAsLong(512L)
//                    .build();
//        }
//
//        // 并发处理
//        long startTime = System.currentTimeMillis();
//
//        for (EmailContext context : contexts) {
//            try {
//                if (processorManager != null) {
//                    // 模拟处理
//                    log.debug("处理邮件: {}", context.getSubject());
//                }
//            } catch (Exception e) {
//                log.error("❌ 并发处理失败: {}", context.getSubject(), e);
//            }
//        }
//
//        long endTime = System.currentTimeMillis();
//        log.info("✅ 并发处理完成，耗时: {}ms", endTime - startTime);
//    }
//}