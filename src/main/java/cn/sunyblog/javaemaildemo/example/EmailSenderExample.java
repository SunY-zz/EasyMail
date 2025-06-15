//package cn.sunyblog.javaemaildemo.example;
//
//import cn.sunyblog.javaemaildemo.EmailSenderStarter;
//import cn.sunyblog.javaemaildemo.api.EmailRequest;
//import cn.sunyblog.javaemaildemo.api.EmailSenderService;
//
//import cn.sunyblog.javaemaildemo.mail.MailService;
//import cn.sunyblog.javaemaildemo.send.SendResult;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.CommandLineRunner;
//import org.springframework.stereotype.Component;
//
//import javax.annotation.Resource;
//import java.util.*;
//
///**
// * 邮件发送示例类
// * 展示如何使用邮件发送功能（包含新的企业级邮件发送服务和Builder API）
// */
//@Slf4j
//@Component
//public class EmailSenderExample implements CommandLineRunner {
//
//    @Autowired
//    private MailService mailService;
//
//    @Resource
//    private EmailSenderService emailSenderService;
//
//    @Resource
//    private EmailSenderStarter emailSenderStarter;
//
//    @Override
//    public void run(String... args) {
//        // 注意：这里仅作为示例，实际运行时请注释掉不需要的示例代码
//        // 或者通过配置控制是否执行示例代码
//
//        log.info("=== 邮件发送服务示例 ===");
//
//        // 检查是否启用了示例代码
//        if (!isExampleEnabled()) {
//            log.info("示例代码已禁用，如需启用请设置 email.sender.example.enabled=true");
//            return;
//        }
//
//        try {
//            // 演示新的Builder API
//            demonstrateBuilderAPI();
//
//            // 新的企业级邮件发送服务示例
//            //enterpriseEmailServiceExamples();
//
//            log.info("=== 邮件发送服务示例完成 ===");
//        } catch (Exception e) {
//            log.error("示例执行异常: {}", e.getMessage(), e);
//        }
//    }
//
//    /**
//     * 检查是否启用了示例代码
//     */
//    private boolean isExampleEnabled() {
//        // 这里可以通过配置文件或环境变量来控制
//        // 为了演示，这里直接返回false，避免在生产环境中意外执行
//        return false; // 改为true来启用示例
//    }
////
////    /**
////     * 发送简单文本邮件示例
////     */
////    public void sendSimpleTextEmail() {
////        String to = "3379652824@qq.com";
////        String subject = "测试邮件 - 文本格式";
////        String content = "这是一封测试邮件，由JavaEmailDemo发送。\n\n祝好，\nJavaEmailDemo团队";
////
////        boolean success = mailService.sendSimpleEmail(to, subject, content);
////        log.info("简单文本邮件发送{}", success ? "成功" : "失败");
////    }
////
////    /**
////     * 发送HTML格式邮件示例
////     */
////    public void sendHtmlEmail() {
////        String to = "recipient@example.com";
////        String subject = "测试邮件 - HTML格式";
////        String htmlContent = ""
////                + "<html>"
////                + "<head><title>测试邮件</title></head>"
////                + "<body>"
////                + "<h1>JavaEmailDemo测试邮件</h1>"
////                + "<p>这是一封<strong>HTML格式</strong>的测试邮件。</p>"
////                + "<p>您可以在这里添加<span style='color:blue;'>各种样式</span>和<a href='https://github.com/yourusername/JavaEmailDemo'>链接</a>。</p>"
////                + "<hr/>"
////                + "<p>祝好，<br/>JavaEmailDemo团队</p>"
////                + "</body>"
////                + "</html>";
////
////        boolean success = mailService.sendHtmlEmail(to, subject, htmlContent);
////        log.info("HTML格式邮件发送{}", success ? "成功" : "失败");
////    }
////
//
//    /**
//     * 发送带附件的邮件示例 - 使用新的Builder API
//     */
//    public void sendEmailWithAttachments() {
//        // 使用新的Builder API发送带附件的邮件
//        EmailRequest request = EmailRequest.builder()
//                .to("recipient@example.com")
//                .subject("EasyMail 测试邮件 - 带附件")
//                .text("这是一封带附件的测试邮件，使用了新的Builder API，请查看附件。")
//                // 添加附件（实际使用时请确保文件存在）
//                // .attachment(new File("path/to/attachment1.pdf"))
//                // .attachment(new File("path/to/attachment2.jpg"))
//                .build();
//
//        SendResult success = emailSenderService.send(request);
//        //log.info("带附件的邮件发送{}", success ? "成功" : "失败");
//    }
//
//    /**
//     * 演示新的Builder API的各种用法
//     */
//    public void demonstrateBuilderAPI() {
//        log.info("=== Builder API 演示 ===");
//
//        try {
//            // 1. 简单文本邮件
//            EmailRequest textRequest = EmailRequest.builder()
//                    .to("recipient@example.com")
//                    .subject("EasyMail 简单文本邮件测试")
//                    .text("这是一封简单的文本邮件，用于测试EasyMail的基本发送功能。\n\n发送时间：" + new Date())
//                    .build();
//
//            SendResult textResult = emailSenderService.send(textRequest);
//            //log.info("文本邮件发送{}", textResult ? "成功" : "失败");
//
//            // 2. HTML邮件
//            EmailRequest htmlRequest = EmailRequest.builder()
//                    .to("recipient@example.com")
//                    .subject("EasyMail HTML邮件测试")
//                    .html("<html><body>" +
//                            "<h2 style='color: #2E86AB;'>EasyMail HTML邮件测试</h2>" +
//                            "<p>这是一封<strong>HTML格式</strong>的邮件，使用了新的Builder API。</p>" +
//                            "<p>支持<em>富文本</em>内容和<a href='https://example.com'>链接</a>。</p>" +
//                            "<p style='color: #666;'>发送时间：" + new Date() + "</p>" +
//                            "</body></html>")
//                    .build();
//
//            SendResult htmlResult = emailSenderService.send(htmlRequest);
//            // log.info("HTML邮件发送{}", htmlResult ? "成功" : "失败");
//
//            // 3. 多收件人邮件
//            EmailRequest multiRecipientRequest = EmailRequest.builder()
//                    .to("recipient1@example.com")
//                    .to("recipient2@example.com")
//                    .cc("cc@example.com")
//                    .bcc("bcc@example.com")
//                    .subject("EasyMail 多收件人邮件测试")
//                    .html("<h3>多收件人邮件测试</h3><p>这封邮件发送给了多个收件人。</p>")
//                    .build();
//
//            SendResult multiResult = emailSenderService.send(multiRecipientRequest);
//            //log.info("多收件人邮件发送{}", multiResult ? "成功" : "失败");
//
//            // 4. 异步发送
//            EmailRequest asyncRequest = EmailRequest.builder()
//                    .to("recipient@example.com")
//                    .subject("EasyMail 异步邮件测试")
//                    .text("这是一封异步发送的邮件。")
//                    .build();
//
//            emailSenderService.sendAsync(asyncRequest)
//                    .thenAccept(result -> log.info("异步邮件发送{}", result))
//                    .exceptionally(throwable -> {
//                        log.error("异步邮件发送失败", throwable);
//                        return null;
//                    });
//
//        } catch (Exception e) {
//            log.error("Builder API演示失败", e);
//        }
//    }
//}
////
////    /**
////     * 批量发送邮件示例
////     */
////    public void sendBatchEmails() {
////        List<String> recipients = Arrays.asList(
////                "recipient1@example.com",
////                "recipient2@example.com",
////                "recipient3@example.com"
////        );
////
////        String subject = "批量测试邮件";
////        String content = "这是一封批量发送的测试邮件。";
////
////        int successCount = mailService.sendBatchEmails(recipients, subject, content, false);
////        log.info("批量邮件发送完成，成功发送: {}/{}", successCount, recipients.size());
////    }
////
////    /**
////     * 企业级邮件发送服务示例
////     */
////    private void enterpriseEmailServiceExamples() {
////        log.info("=== 企业级邮件发送服务示例 ===");
////
////        // 检查连接
////        checkConnection();
////
////        // 基本发送示例
////        basicSendingExamples();
////
////        // 模板发送示例
////        templateSendingExamples();
////
////        // 高级功能示例
////        advancedFeaturesExamples();
////
////        // 统计信息示例
////        statisticsExamples();
////    }
////
////    /**
////     * 检查连接示例
////     */
////    private void checkConnection() {
////        log.info("检查邮件服务连接...");
////        boolean connected = emailSenderService.checkConnection();
////        log.info("连接状态: {}", connected ? "正常" : "异常");
////    }
////
////    /**
////     * 基本发送示例
////     */
////    private void basicSendingExamples() {
////        log.info("=== 基本发送示例 ===");
////
////        String recipient = "example@example.com"; // 替换为实际收件人
////
////        // 发送文本邮件
////        SendResult textResult = emailSenderService.sendText(
////                recipient,
////                "文本邮件测试",
////                "这是一封测试文本邮件，发送时间：" + System.currentTimeMillis());
////        log.info("文本邮件发送结果: {}", textResult.isSuccess() ? "成功" : "失败: " + textResult.getErrorMessage());
////
////        // 发送HTML邮件
////        SendResult htmlResult = emailSenderService.sendHtml(
////                recipient,
////                "HTML邮件测试",
////                "<h2>HTML邮件测试</h2><p>这是一封<strong>HTML格式</strong>的测试邮件</p>");
////        log.info("HTML邮件发送结果: {}", htmlResult.isSuccess() ? "成功" : "失败: " + htmlResult.getErrorMessage());
////
////        // 批量发送邮件
////        SendResult batchResult = emailSenderService.sendToMultiple(
////                Arrays.asList(recipient, "another@example.com"),
////                "批量邮件测试",
////                "这是一封批量发送的测试邮件", false);
////        log.info("批量邮件发送结果: {}", batchResult.isSuccess() ? "成功" : "失败: " + batchResult.getErrorMessage());
////    }
////
////    /**
////     * 模板发送示例
////     */
////    private void templateSendingExamples() {
////        log.info("=== 模板发送示例 ===");
////
////        String recipient = "example@example.com"; // 替换为实际收件人
////
////        // 使用内置模板
////        Map<String, Object> verifyVars = new HashMap<>();
////        verifyVars.put("code", "123456");
////        verifyVars.put("expireMinutes", 10);
////
////        boolean builtinResult = emailSenderStarter.sendWithTemplate(
////                recipient,
////                "verification",
////                verifyVars);
////        log.info("内置模板邮件发送结果: {}", builtinResult ? "成功" : "失败");
////
////        // 创建自定义模板
////        EmailTemplate welcomeTemplate = EmailTemplate.builder()
////                .templateId("custom-welcome")
////                .templateName("欢迎模板")
////                .subjectTemplate("欢迎加入{{company}}")
////                .contentTemplate("<h2>欢迎 {{name}}！</h2><p>感谢您加入{{company}}，您的账号已激活。</p>")
////                .isHtml(true)
////                .build();
////
////        // 注册模板
////        emailSenderStarter.getTemplateManager().registerTemplate(welcomeTemplate);
////
////        // 准备模板变量
////        Map<String, Object> variables = new HashMap<>();
////        variables.put("name", "张三");
////        variables.put("company", "示例公司");
////
////        // 使用模板发送邮件
////        SendResult templateResult = emailSenderService.sendWithTemplate(
////                recipient,
////                welcomeTemplate,
////                variables);
////        log.info("自定义模板邮件发送结果: {}", templateResult.isSuccess() ? "成功" : "失败: " + templateResult.getErrorMessage());
////    }
////
////    /**
////     * 高级功能示例
////     */
////    private void advancedFeaturesExamples() {
////        log.info("=== 高级功能示例 ===");
////
////        String recipient = "example@example.com"; // 替换为实际收件人
////
////        // 异步发送邮件
////        emailSenderService.sendAsync(
////                Arrays.asList(recipient),
////                null, null,
////                "异步邮件测试",
////                "这是一封异步发送的测试邮件",
////                false, null)
////                .thenAccept(result -> {
////                    log.info("异步邮件发送完成，结果: {}", result.isSuccess() ? "成功" : "失败: " + result.getErrorMessage());
////                    log.info("消息ID: {}, 耗时: {}ms", result.getMessageId(), result.getDuration());
////                });
////
////        // 使用便捷方法发送带回调的邮件
////        emailSenderStarter.sendWithCallback(
////                recipient,
////                "带回调的邮件测试",
////                "这是一封带回调的测试邮件",
////                success -> log.info("邮件发送成功"),
////                error -> log.error("邮件发送失败: {}", error));
////    }
////
////    /**
////     * 统计信息示例
////     */
////    private void statisticsExamples() {
////        log.info("=== 统计信息示例 ===");
////
////        // 获取发送统计信息
////        String stats = emailSenderService.getSendingStats();
////        log.info("发送统计: {}", stats);
////
////        // 获取线程池状态
////        String poolStatus = emailSenderService.getThreadPoolStatus();
////        log.info("线程池状态: {}", poolStatus);
////
////        // 获取模板统计信息
////        log.info("模板数量: {}", emailSenderStarter.getTemplateNames().size());
////        log.info("模板列表: {}", emailSenderStarter.getTemplateNames());
////    }
////
////    /**
////     * 发送验证码邮件示例
////     *
////     * @param email 收件人邮箱
////     * @param verificationCode 验证码
////     * @return 是否发送成功
////     */
////    public boolean sendVerificationCodeEmail(String email, String verificationCode) {
////        String subject = "您的验证码";
////        String htmlContent = ""
////                + "<html>"
////                + "<head><title>验证码</title></head>"
////                + "<body>"
////                + "<div style='background-color:#f7f7f7;padding:20px;'>"
////                + "<h2>验证码</h2>"
////                + "<p>您的验证码是：</p>"
////                + "<div style='background-color:#ffffff;padding:10px;font-size:24px;font-weight:bold;text-align:center;'>"
////                + verificationCode
////                + "</div>"
////                + "<p>验证码有效期为10分钟，请勿泄露给他人。</p>"
////                + "<p>如果这不是您的操作，请忽略此邮件。</p>"
////                + "</div>"
////                + "</body>"
////                + "</html>";
////
////        return mailService.sendHtmlEmail(email, subject, htmlContent);
////    }
////
////    /**
////     * 发送通知邮件示例
////     *
////     * @param email 收件人邮箱
////     * @param title 通知标题
////     * @param message 通知内容
////     * @return 是否发送成功
////     */
////    public boolean sendNotificationEmail(String email, String title, String message) {
////        String subject = "通知: " + title;
////        String htmlContent = ""
////                + "<html>"
////                + "<head><title>" + title + "</title></head>"
////                + "<body>"
////                + "<div style='background-color:#f7f7f7;padding:20px;'>"
////                + "<h2>" + title + "</h2>"
////                + "<div style='background-color:#ffffff;padding:15px;'>"
////                + message.replace("\n", "<br/>")
////                + "</div>"
////                + "<p style='font-size:12px;color:#888888;margin-top:20px;'>此邮件由系统自动发送，请勿回复。</p>"
////                + "</div>"
////                + "</body>"
////                + "</html>";
////
////        return mailService.sendHtmlEmail(email, subject, htmlContent);
////    }
////}