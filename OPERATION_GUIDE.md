# JavaEmailSpringBoot 操作指南

## 项目简介

JavaEmailSpringBoot 是一个功能强大、易于使用的 Spring Boot 邮件处理框架，提供了完整的邮件收发解决方案。该项目采用现代化的设计模式，支持多种邮件处理方式，具有高度的可扩展性和灵活性。

### 核心特性

- **统一的邮件发送API**：提供 `EasyMailSender` 接口，支持多种发送方式
- **Builder模式支持**：通过 `EmailRequest` 提供链式调用的友好API
- **多种邮件处理方式**：支持接口实现、注解驱动、函数式处理
- **异步处理能力**：支持异步发送和异步邮件处理
- **模板邮件支持**：内置模板引擎，支持动态内容生成
- **监控和统计**：提供详细的发送统计和健康监控
- **高度可配置**：丰富的配置选项，适应不同场景需求
- **企业级特性**：支持重试机制、批量发送、优先级处理

## 环境搭建与配置

### 1. 项目依赖

在您的 `pom.xml` 中添加依赖：

```xml
<dependency>
    <groupId>cn.sunyblog</groupId>
    <artifactId>java-email-spring-boot-starter</artifactId>
    <version>1.0.0</version>
</dependency>
```

### 2. 基础配置

在 `application.yml` 中配置邮件服务：

```yaml
# SMTP发送配置
mail:
  smtp:
    server: smtp.qq.com
    port: 587
    protocol: smtp
    username: your-email@qq.com
    password: your-auth-code
    connection:
      timeout: 15000
      readTimeout: 30000
      writeTimeout: 30000
    properties:
      auth: true
      starttls:
        enable: true
        required: true
      ssl:
        enable: false
        trust: smtp.qq.com
    retry:
      maxAttempts: 3
      delay: 1000
      multiplier: 2.0
      maxDelay: 10000
    log:
      enabled: true
      level: INFO

# 邮件监听配置（用于接收邮件）
email:
  listener:
    enabled: true
    server:
      host: imap.qq.com
      port: 993
      protocol: imaps
      username: your-email@qq.com
      password: your-auth-code
      folder: INBOX
    connection:
      timeout: 30000
      readTimeout: 60000
    monitor:
      enabled: true
      interval: 30
      maxRetries: 3
    listener:
      enabled: true
      interval: 10
      batchSize: 10
      markAsRead: true
      deleteAfterProcess: false

# 邮件发送服务配置
  sender:
    enabled: true
    defaultStrategy: default
    batchThreshold: 10
    highPriorityKeywords: ["urgent", "紧急", "重要", "important"]
    monitor:
      enabled: true
      retentionHours: 24
      healthCheckInterval: 5
      failureRateThreshold: 0.1
      responseTimeThreshold: 5000
    template:
      enabled: true
      cacheSize: 100
      cacheExpireMinutes: 60
      defaultPath: classpath:templates/email/
    event:
      enabled: true
      async: true
      threadPoolSize: 5
```

### 3. 常用邮箱配置示例

#### QQ邮箱
```yaml
mail:
  smtp:
    server: smtp.qq.com
    port: 587  # 或 465 (SSL)
    username: your-email@qq.com
    password: your-auth-code  # QQ邮箱授权码
```

#### 163邮箱
```yaml
mail:
  smtp:
    server: smtp.163.com
    port: 25
    username: your-email@163.com
    password: your-auth-code
```

#### Gmail
```yaml
mail:
  smtp:
    server: smtp.gmail.com
    port: 587
    username: your-email@gmail.com
    password: your-app-password
```

## 核心功能使用

### 1. 邮件发送功能

#### 1.1 基础发送方式

```java
@Service
public class EmailService {
    
    @Autowired
    private EasyMailSender easyMailSender;
    
    /**
     * 发送简单文本邮件
     */
    public void sendSimpleText() {
        SendResult result = easyMailSender.sendText(
            "recipient@example.com",
            "测试邮件",
            "这是一封测试邮件内容"
        );
        
        if (result.isSuccess()) {
            System.out.println("邮件发送成功，ID: " + result.getMessageId());
        } else {
            System.out.println("邮件发送失败: " + result.getErrorMessage());
        }
    }
    
    /**
     * 发送HTML邮件
     */
    public void sendHtmlEmail() {
        String htmlContent = """
            <html>
            <body>
                <h1>欢迎使用JavaEmailSpringBoot</h1>
                <p>这是一封<strong>HTML格式</strong>的邮件。</p>
                <a href="https://github.com">访问GitHub</a>
            </body>
            </html>
            """;
            
        SendResult result = easyMailSender.sendHtml(
            "recipient@example.com",
            "HTML邮件测试",
            htmlContent
        );
    }
    
    /**
     * 发送带附件的邮件
     */
    public void sendWithAttachment() {
        File attachment = new File("path/to/your/file.pdf");
        
        SendResult result = easyMailSender.sendWithAttachment(
            "recipient@example.com",
            "带附件的邮件",
            "请查收附件",
            attachment
        );
    }
}
```

#### 1.2 Builder模式发送（推荐）

```java
@Service
public class AdvancedEmailService {
    
    @Autowired
    private EasyMailSender easyMailSender;
    
    /**
     * 使用Builder模式发送复杂邮件
     */
    public void sendComplexEmail() {
        EmailRequest request = EmailRequest.builder()
            .to("recipient1@example.com")
            .to("recipient2@example.com")
            .cc("cc@example.com")
            .bcc("bcc@example.com")
            .subject("复杂邮件示例")
            .html("<h1>这是HTML内容</h1>")
            .attachment(new File("file1.pdf"))
            .attachment(new File("file2.jpg"))
            .priority(1)  // 高优先级
            .strategy("high_priority")  // 使用高优先级发送策略
            .build();
            
        SendResult result = easyMailSender.send(request);
        
        // 处理发送结果
        handleSendResult(result);
    }
    
    /**
     * 模板邮件发送
     */
    public void sendTemplateEmail() {
        EmailRequest request = EmailRequest.builder()
            .to("user@example.com")
            .subject("欢迎注册")
            .templateId("welcome")
            .variable("username", "张三")
            .variable("activationLink", "https://example.com/activate?token=abc123")
            .variable("companyName", "我的公司")
            .build();
            
        SendResult result = easyMailSender.send(request);
    }
    
    /**
     * 批量发送邮件
     */
    public void sendBatchEmails() {
        List<EmailRequest> requests = Arrays.asList(
            EmailRequest.builder()
                .to("user1@example.com")
                .subject("批量邮件1")
                .text("内容1")
                .build(),
            EmailRequest.builder()
                .to("user2@example.com")
                .subject("批量邮件2")
                .text("内容2")
                .build()
        );
        
        SendResult result = easyMailSender.sendBatch(requests);
    }
    
    private void handleSendResult(SendResult result) {
        if (result.isSuccess()) {
            System.out.println("发送成功:");
            System.out.println("  消息ID: " + result.getMessageId());
            System.out.println("  耗时: " + result.getDuration() + "ms");
            System.out.println("  收件人: " + result.getRecipients());
        } else {
            System.out.println("发送失败:");
            System.out.println("  错误代码: " + result.getErrorCode());
            System.out.println("  错误信息: " + result.getErrorMessage());
        }
    }
}
```

#### 1.3 异步发送

```java
@Service
public class AsyncEmailService {
    
    @Autowired
    private EasyMailSender easyMailSender;
    
    /**
     * 异步发送文本邮件
     */
    public void sendTextAsync() {
        CompletableFuture<SendResult> future = easyMailSender.sendTextAsync(
            "recipient@example.com",
            "异步邮件",
            "这是异步发送的邮件"
        );
        
        // 异步处理结果
        future.thenAccept(result -> {
            if (result.isSuccess()) {
                System.out.println("异步发送成功: " + result.getMessageId());
            } else {
                System.out.println("异步发送失败: " + result.getErrorMessage());
            }
        }).exceptionally(throwable -> {
            System.err.println("异步发送异常: " + throwable.getMessage());
            return null;
        });
    }
    
    /**
     * 使用Builder模式异步发送
     */
    public void sendAsyncWithBuilder() {
        EmailRequest request = EmailRequest.builder()
            .to("recipient@example.com")
            .subject("Builder异步邮件")
            .html("<p>这是Builder模式的异步邮件</p>")
            .async(true)  // 标记为异步
            .build();
            
        CompletableFuture<SendResult> future = easyMailSender.sendAsync(request);
        
        future.whenComplete((result, throwable) -> {
            if (throwable != null) {
                System.err.println("发送异常: " + throwable.getMessage());
            } else if (result.isSuccess()) {
                System.out.println("发送成功: " + result.getMessageId());
            } else {
                System.out.println("发送失败: " + result.getErrorMessage());
            }
        });
    }
    
    /**
     * 带回调的异步发送
     */
    public void sendWithCallback() {
        CompletableFuture<SendResult> future = easyMailSender.sendWithCallback(
            "recipient@example.com",
            "回调邮件",
            "这是带回调的邮件",
            result -> {
                // 成功回调
                System.out.println("发送成功回调: " + result.getMessageId());
            },
            result -> {
                // 失败回调
                System.out.println("发送失败回调: " + result.getErrorMessage());
            }
        );
    }
}
```

### 2. 邮件接收与处理功能

#### 2.1 接口实现方式

```java
@Component
public class MyEmailProcessor implements EmailListenerApi {
    
    @Override
    public boolean processEmail(Message message, String content, String subject, String from) {
        System.out.println("收到邮件: " + subject + " 来自: " + from);
        
        // 处理验证码邮件
        if (subject.contains("验证码") || subject.contains("verification")) {
            String code = extractVerificationCode(content);
            if (code != null) {
                System.out.println("提取到验证码: " + code);
                // 存储验证码到缓存或数据库
                saveVerificationCode(from, code);
                return true;
            }
        }
        
        // 处理订单确认邮件
        if (subject.contains("订单") || subject.contains("order")) {
            processOrderEmail(content);
            return true;
        }
        
        return false;
    }
    
    @Override
    public String getProcessorName() {
        return "通用邮件处理器";
    }
    
    private String extractVerificationCode(String content) {
        Pattern pattern = Pattern.compile("\\b(\\d{4,6})\\b");
        Matcher matcher = pattern.matcher(content);
        return matcher.find() ? matcher.group(1) : null;
    }
    
    private void saveVerificationCode(String email, String code) {
        // 实现验证码存储逻辑
    }
    
    private void processOrderEmail(String content) {
        // 实现订单邮件处理逻辑
    }
}
```

#### 2.2 注解驱动方式（推荐）

```java
@Slf4j
@Component
@EmailProcessor(
    group = "business",
    description = "业务邮件处理器",
    enabled = true
)
public class BusinessEmailProcessor {
    
    /**
     * 处理验证码邮件
     */
    @EmailHandler(
        name = "verificationCodeHandler",
        description = "处理验证码邮件",
        subject = {"(?i).*(验证码|verification|code).*"},
        subjectMatchType = EmailHandler.MatchType.REGEX,
        priority = 100,
        async = false
    )
    public void handleVerificationCode(EmailContext context) {
        log.info("处理验证码邮件: {}", context.getSubject());
        
        String code = extractVerificationCode(context.getContent());
        if (code != null) {
            log.info("提取到验证码: {}", code);
            // 存储验证码
            saveVerificationCode(context.getSender(), code);
        }
    }
    
    /**
     * 处理订单邮件
     */
    @EmailHandler(
        name = "orderHandler",
        description = "处理订单相关邮件",
        subject = {"订单", "order", "购买", "purchase"},
        subjectMatchType = EmailHandler.MatchType.CONTAINS,
        priority = 200,
        async = true
    )
    public void handleOrderEmail(EmailContext context) {
        log.info("处理订单邮件: {}", context.getSubject());
        
        // 提取订单信息
        String orderNumber = extractOrderNumber(context.getContent());
        if (orderNumber != null) {
            log.info("提取到订单号: {}", orderNumber);
            // 处理订单逻辑
            processOrder(orderNumber, context);
        }
    }
    
    /**
     * 处理特定发件人的邮件
     */
    @EmailHandler(
        name = "specificSenderHandler",
        description = "处理特定发件人邮件",
        from = {"noreply@example.com", "service@company.com"},
        fromMatchType = EmailHandler.MatchType.EXACT,
        priority = 50,
        async = true
    )
    public void handleSpecificSender(EmailContext context) {
        log.info("处理特定发件人邮件: {} from {}", 
                context.getSubject(), context.getSender());
        
        // 根据发件人进行不同处理
        if (context.getSender().contains("noreply@example.com")) {
            handleSystemNotification(context);
        } else if (context.getSender().contains("service@company.com")) {
            handleServiceEmail(context);
        }
    }
    
    /**
     * 处理带附件的邮件
     */
    @EmailHandler(
        name = "attachmentHandler",
        description = "处理带附件的邮件",
        subject = {".*附件.*", ".*attachment.*"},
        subjectMatchType = EmailHandler.MatchType.REGEX,
        priority = 300,
        async = true
    )
    public void handleEmailWithAttachment(EmailContext context) {
        log.info("处理带附件邮件: {}", context.getSubject());
        
        if (context.getAttachments() != null && !context.getAttachments().isEmpty()) {
            for (EmailContext.AttachmentInfo attachment : context.getAttachments()) {
                log.info("处理附件: {} ({})", 
                        attachment.getFileName(), attachment.getSize());
                // 处理附件逻辑
                processAttachment(attachment);
            }
        }
    }
    
    // 辅助方法
    private String extractVerificationCode(String content) {
        Pattern pattern = Pattern.compile("\\b(\\d{4,6})\\b");
        Matcher matcher = pattern.matcher(content);
        return matcher.find() ? matcher.group(1) : null;
    }
    
    private String extractOrderNumber(String content) {
        Pattern pattern = Pattern.compile("订单号[：:]?\\s*([A-Z0-9]{10,20})");
        Matcher matcher = pattern.matcher(content);
        return matcher.find() ? matcher.group(1) : null;
    }
    
    private void saveVerificationCode(String email, String code) {
        // 实现验证码存储逻辑
    }
    
    private void processOrder(String orderNumber, EmailContext context) {
        // 实现订单处理逻辑
    }
    
    private void handleSystemNotification(EmailContext context) {
        // 处理系统通知邮件
    }
    
    private void handleServiceEmail(EmailContext context) {
        // 处理客服邮件
    }
    
    private void processAttachment(EmailContext.AttachmentInfo attachment) {
        // 处理附件逻辑
    }
}
```

#### 2.3 函数式处理方式

```java
@Slf4j
@Component
public class FunctionalEmailProcessor {
    
    @Autowired
    private MailProcessor mailProcessor;
    
    @PostConstruct
    public void init() {
        // 设置函数式邮件处理器
        mailProcessor.setEmailProcessorFunction(this::processEmail);
        log.info("函数式邮件处理器已注册");
    }
    
    /**
     * 函数式邮件处理方法
     */
    public String processEmail(Message message, String content, String subject, String from) {
        log.info("函数式处理邮件: {} from {}", subject, from);
        
        try {
            // 根据邮件类型进行不同处理
            if (isVerificationEmail(subject)) {
                return handleVerificationEmail(content, from);
            } else if (isOrderEmail(subject)) {
                return handleOrderEmail(content, from);
            } else if (isNewsletterEmail(subject, from)) {
                return handleNewsletterEmail(content, from);
            }
            
            // 默认处理
            return handleDefaultEmail(content, subject, from);
            
        } catch (Exception e) {
            log.error("邮件处理异常: {}", e.getMessage(), e);
            return null;
        }
    }
    
    private boolean isVerificationEmail(String subject) {
        return subject.matches("(?i).*(验证码|verification|code).*");
    }
    
    private boolean isOrderEmail(String subject) {
        return subject.matches("(?i).*(订单|order|购买|purchase).*");
    }
    
    private boolean isNewsletterEmail(String subject, String from) {
        return from.contains("newsletter") || subject.contains("订阅");
    }
    
    private String handleVerificationEmail(String content, String from) {
        String code = extractVerificationCode(content);
        if (code != null) {
            log.info("提取到验证码: {}", code);
            // 存储验证码
            saveVerificationCode(from, code);
            return code;
        }
        return null;
    }
    
    private String handleOrderEmail(String content, String from) {
        String orderNumber = extractOrderNumber(content);
        if (orderNumber != null) {
            log.info("提取到订单号: {}", orderNumber);
            // 处理订单
            processOrder(orderNumber, content);
            return orderNumber;
        }
        return null;
    }
    
    private String handleNewsletterEmail(String content, String from) {
        log.info("处理订阅邮件来自: {}", from);
        // 处理订阅邮件逻辑
        return "newsletter_processed";
    }
    
    private String handleDefaultEmail(String content, String subject, String from) {
        log.info("默认处理邮件: {} from {}", subject, from);
        // 默认处理逻辑
        return "default_processed";
    }
    
    // 辅助方法（与注解方式相同）
    private String extractVerificationCode(String content) {
        Pattern pattern = Pattern.compile("\\b(\\d{4,6})\\b");
        Matcher matcher = pattern.matcher(content);
        return matcher.find() ? matcher.group(1) : null;
    }
    
    private String extractOrderNumber(String content) {
        Pattern pattern = Pattern.compile("订单号[：:]?\\s*([A-Z0-9]{10,20})");
        Matcher matcher = pattern.matcher(content);
        return matcher.find() ? matcher.group(1) : null;
    }
    
    private void saveVerificationCode(String email, String code) {
        // 实现验证码存储逻辑
    }
    
    private void processOrder(String orderNumber, String content) {
        // 实现订单处理逻辑
    }
}
```

### 3. 监控和统计功能

```java
@Service
public class EmailMonitorService {
    
    @Autowired
    private EasyMailSender easyMailSender;
    
    /**
     * 获取发送统计信息
     */
    public void showSendingStats() {
        Map<String, Object> stats = easyMailSender.getSendingStats();
        
        System.out.println("=== 邮件发送统计 ===");
        System.out.println("总发送数量: " + stats.get("totalSent"));
        System.out.println("成功发送数量: " + stats.get("successCount"));
        System.out.println("失败发送数量: " + stats.get("failureCount"));
        System.out.println("成功率: " + stats.get("successRate") + "%");
        System.out.println("平均响应时间: " + stats.get("avgResponseTime") + "ms");
        System.out.println("最后发送时间: " + stats.get("lastSendTime"));
    }
    
    /**
     * 检查连接状态
     */
    public void checkConnectionStatus() {
        boolean isConnected = easyMailSender.checkConnection();
        
        if (isConnected) {
            System.out.println("✅ 邮件服务连接正常");
        } else {
            System.out.println("❌ 邮件服务连接异常");
        }
    }
    
    /**
     * 获取线程池状态
     */
    public void showThreadPoolStatus() {
        Map<String, Object> status = easyMailSender.getThreadPoolStatus();
        
        System.out.println("=== 线程池状态 ===");
        System.out.println("核心线程数: " + status.get("corePoolSize"));
        System.out.println("最大线程数: " + status.get("maximumPoolSize"));
        System.out.println("当前线程数: " + status.get("poolSize"));
        System.out.println("活跃线程数: " + status.get("activeCount"));
        System.out.println("队列大小: " + status.get("queueSize"));
        System.out.println("已完成任务数: " + status.get("completedTaskCount"));
    }
    
    /**
     * 定期健康检查
     */
    @Scheduled(fixedRate = 300000) // 每5分钟检查一次
    public void healthCheck() {
        boolean isHealthy = easyMailSender.checkConnection();
        Map<String, Object> stats = easyMailSender.getSendingStats();
        
        if (!isHealthy) {
            System.err.println("⚠️ 邮件服务健康检查失败");
            // 发送告警通知
            sendHealthAlert("邮件服务连接异常");
        }
        
        // 检查失败率
        Double failureRate = (Double) stats.get("failureRate");
        if (failureRate != null && failureRate > 10.0) {
            System.err.println("⚠️ 邮件发送失败率过高: " + failureRate + "%");
            sendHealthAlert("邮件发送失败率过高: " + failureRate + "%");
        }
    }
    
    private void sendHealthAlert(String message) {
        // 发送健康告警邮件
        try {
            easyMailSender.sendText(
                "admin@example.com",
                "邮件服务告警",
                "告警信息: " + message + "\n时间: " + new Date()
            );
        } catch (Exception e) {
            System.err.println("发送告警邮件失败: " + e.getMessage());
        }
    }
}
```

## 自定义扩展

### 1. 自定义邮件发送策略

```java
@Component
public class CustomEmailSendStrategy implements EmailSendStrategy {
    
    @Override
    public String getStrategyName() {
        return "custom";
    }
    
    @Override
    public SendResult send(EmailRequest request) {
        // 实现自定义发送逻辑
        System.out.println("使用自定义策略发送邮件: " + request.getSubject());
        
        try {
            // 自定义的发送前处理
            preprocessEmail(request);
            
            // 执行实际发送
            SendResult result = doSend(request);
            
            // 自定义的发送后处理
            postprocessEmail(request, result);
            
            return result;
            
        } catch (Exception e) {
            return SendResult.builder()
                .success(false)
                .errorMessage("自定义策略发送失败: " + e.getMessage())
                .errorCode("CUSTOM_SEND_ERROR")
                .build();
        }
    }
    
    private void preprocessEmail(EmailRequest request) {
        // 发送前预处理逻辑
        System.out.println("自定义策略预处理邮件");
    }
    
    private SendResult doSend(EmailRequest request) {
        // 实际发送逻辑
        return SendResult.builder()
            .success(true)
            .messageId("custom-" + System.currentTimeMillis())
            .startTime(System.currentTimeMillis())
            .endTime(System.currentTimeMillis() + 1000)
            .duration(1000)
            .recipients(request.getToList())
            .subject(request.getSubject())
            .build();
    }
    
    private void postprocessEmail(EmailRequest request, SendResult result) {
        // 发送后处理逻辑
        System.out.println("自定义策略后处理邮件，结果: " + result.isSuccess());
    }
}
```

### 2. 自定义模板引擎

```java
@Component
public class CustomEmailTemplateEngine implements EmailTemplateEngine {
    
    @Override
    public String processTemplate(String templateId, Map<String, Object> variables) {
        // 实现自定义模板处理逻辑
        System.out.println("使用自定义模板引擎处理模板: " + templateId);
        
        try {
            // 加载模板
            String template = loadTemplate(templateId);
            
            // 处理变量替换
            return processVariables(template, variables);
            
        } catch (Exception e) {
            throw new RuntimeException("模板处理失败: " + e.getMessage(), e);
        }
    }
    
    private String loadTemplate(String templateId) {
        // 从文件系统、数据库或其他地方加载模板
        switch (templateId) {
            case "welcome":
                return "欢迎 ${username} 加入我们！激活链接: ${activationLink}";
            case "notification":
                return "通知: ${message}";
            default:
                throw new IllegalArgumentException("未知模板ID: " + templateId);
        }
    }
    
    private String processVariables(String template, Map<String, Object> variables) {
        String result = template;
        
        for (Map.Entry<String, Object> entry : variables.entrySet()) {
            String placeholder = "${" + entry.getKey() + "}";
            String value = String.valueOf(entry.getValue());
            result = result.replace(placeholder, value);
        }
        
        return result;
    }
}
```

### 3. 自定义事件监听器

```java
@Component
public class CustomEmailEventListener {
    
    /**
     * 监听邮件发送前事件
     */
    @EventListener
    public void handleEmailSendingEvent(EmailEvent.EmailSendingEvent event) {
        System.out.println("邮件即将发送: " + event.getEmailRequest().getSubject());
        
        // 可以在这里进行发送前的处理，如日志记录、权限检查等
        logEmailSending(event.getEmailRequest());
    }
    
    /**
     * 监听邮件发送成功事件
     */
    @EventListener
    public void handleEmailSentEvent(EmailEvent.EmailSentEvent event) {
        System.out.println("邮件发送成功: " + event.getSendResult().getMessageId());
        
        // 发送成功后的处理，如更新统计、通知等
        updateSendingStatistics(event.getSendResult());
    }
    
    /**
     * 监听邮件发送失败事件
     */
    @EventListener
    public void handleEmailSendFailedEvent(EmailEvent.EmailSendFailedEvent event) {
        System.err.println("邮件发送失败: " + event.getSendResult().getErrorMessage());
        
        // 发送失败后的处理，如重试、告警等
        handleSendingFailure(event.getSendResult());
    }
    
    /**
     * 监听邮件接收事件
     */
    @EventListener
    public void handleEmailReceivedEvent(EmailEvent.EmailReceivedEvent event) {
        System.out.println("收到新邮件: " + event.getEmailContext().getSubject());
        
        // 邮件接收后的处理
        processReceivedEmail(event.getEmailContext());
    }
    
    private void logEmailSending(EmailRequest request) {
        // 记录发送日志
    }
    
    private void updateSendingStatistics(SendResult result) {
        // 更新发送统计
    }
    
    private void handleSendingFailure(SendResult result) {
        // 处理发送失败
    }
    
    private void processReceivedEmail(EmailContext context) {
        // 处理接收到的邮件
    }
}
```

### 4. 自定义缓存管理器

```java
@Component
public class CustomEmailCacheManager implements EmailCacheManager {
    
    private final Map<String, Object> cache = new ConcurrentHashMap<>();
    private final Map<String, Long> expireTime = new ConcurrentHashMap<>();
    
    @Override
    public void put(String key, Object value, long expireSeconds) {
        cache.put(key, value);
        expireTime.put(key, System.currentTimeMillis() + expireSeconds * 1000);
    }
    
    @Override
    public <T> T get(String key, Class<T> type) {
        // 检查是否过期
        Long expire = expireTime.get(key);
        if (expire != null && System.currentTimeMillis() > expire) {
            remove(key);
            return null;
        }
        
        Object value = cache.get(key);
        return type.isInstance(value) ? type.cast(value) : null;
    }
    
    @Override
    public void remove(String key) {
        cache.remove(key);
        expireTime.remove(key);
    }
    
    @Override
    public void clear() {
        cache.clear();
        expireTime.clear();
    }
    
    @Override
    public boolean exists(String key) {
        return cache.containsKey(key) && !isExpired(key);
    }
    
    private boolean isExpired(String key) {
        Long expire = expireTime.get(key);
        return expire != null && System.currentTimeMillis() > expire;
    }
    
    /**
     * 定期清理过期缓存
     */
    @Scheduled(fixedRate = 60000) // 每分钟清理一次
    public void cleanExpiredCache() {
        long now = System.currentTimeMillis();
        
        expireTime.entrySet().removeIf(entry -> {
            if (now > entry.getValue()) {
                cache.remove(entry.getKey());
                return true;
            }
            return false;
        });
    }
}
```

## 常见问题与故障排除

### 1. 连接问题

**问题**: 无法连接到SMTP服务器

**解决方案**:
```yaml
# 检查配置
mail:
  smtp:
    server: smtp.qq.com  # 确保服务器地址正确
    port: 587           # 确保端口正确
    properties:
      auth: true        # 启用认证
      starttls:
        enable: true    # 启用TLS
        required: true
      ssl:
        trust: smtp.qq.com  # 信任服务器证书
```

**调试方法**:
```java
@Service
public class EmailDebugService {
    
    @Autowired
    private EasyMailSender easyMailSender;
    
    public void debugConnection() {
        try {
            boolean connected = easyMailSender.checkConnection();
            System.out.println("连接状态: " + (connected ? "成功" : "失败"));
        } catch (Exception e) {
            System.err.println("连接异常: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
```

### 2. 认证问题

**问题**: 认证失败

**解决方案**:
- 确保使用正确的用户名和密码/授权码
- 对于QQ邮箱，需要使用授权码而不是登录密码
- 检查邮箱是否开启了SMTP服务

### 3. 发送失败

**问题**: 邮件发送失败

**调试代码**:
```java
public void debugSending() {
    SendResult result = easyMailSender.sendText(
        "test@example.com",
        "测试邮件",
        "测试内容"
    );
    
    if (!result.isSuccess()) {
        System.err.println("发送失败:");
        System.err.println("错误代码: " + result.getErrorCode());
        System.err.println("错误信息: " + result.getErrorMessage());
        System.err.println("堆栈信息: " + result.getStackTrace());
    }
}
```

### 4. 性能优化

**批量发送优化**:
```yaml
email:
  sender:
    batchThreshold: 50  # 调整批量发送阈值
    monitor:
      responseTimeThreshold: 3000  # 调整响应时间阈值
```

**线程池优化**:
```yaml
thread:
  pool:
    coreSize: 10      # 核心线程数
    maxSize: 50       # 最大线程数
    queueCapacity: 200 # 队列容量
```

## 项目实现原理

### 1. 架构设计

项目采用分层架构设计：

- **API层**: 提供统一的邮件操作接口
- **服务层**: 实现具体的业务逻辑
- **策略层**: 支持多种发送策略
- **配置层**: 提供灵活的配置管理
- **监控层**: 提供性能监控和统计

### 2. 核心组件

#### 2.1 邮件发送组件

- `EasyMailSender`: 统一的邮件发送接口
- `EmailSenderStarter`: 主要实现类
- `EmailRequest`: Builder模式的请求构建器
- `SendResult`: 详细的发送结果

#### 2.2 邮件接收组件

- `EmailListenerApi`: 邮件监听接口
- `MailProcessor`: 邮件处理器
- `EmailContext`: 邮件处理上下文

#### 2.3 注解驱动组件

- `@EmailProcessor`: 处理器类注解
- `@EmailHandler`: 处理方法注解
- `AnnotationDrivenEmailProcessorManager`: 注解处理管理器

### 3. 设计模式应用

#### 3.1 Builder模式

`EmailRequest`使用Builder模式，提供链式调用：

```java
EmailRequest.builder()
    .to("user@example.com")
    .subject("测试")
    .text("内容")
    .build();
```

#### 3.2 策略模式

支持多种发送策略：
- `DefaultEmailSendStrategy`: 默认策略
- `BatchEmailSendStrategy`: 批量发送策略
- `HighPriorityEmailSendStrategy`: 高优先级策略

#### 3.3 观察者模式

通过Spring事件机制实现邮件发送的生命周期监听：
- `EmailSendingEvent`: 发送前事件
- `EmailSentEvent`: 发送成功事件
- `EmailSendFailedEvent`: 发送失败事件

#### 3.4 模板方法模式

`AbstractEmailHandler`定义了邮件处理的模板流程。

### 4. 异步处理机制

项目使用`CompletableFuture`和线程池实现异步处理：

```java
CompletableFuture<SendResult> future = CompletableFuture.supplyAsync(() -> {
    return doSend(request);
}, threadPoolExecutor);
```

### 5. 配置管理

使用Spring Boot的`@ConfigurationProperties`实现配置管理：

- `EmailSenderProperties`: 发送配置
- `EmailListenerProperties`: 监听配置
- `SmtpConfig`: SMTP配置

## 项目优点

### 1. 易用性

- **统一API**: 提供`EasyMailSender`统一接口
- **Builder模式**: 链式调用，代码简洁
- **注解驱动**: 声明式邮件处理
- **自动配置**: Spring Boot自动配置

### 2. 功能丰富

- **多种发送方式**: 文本、HTML、附件、模板、批量
- **多种处理方式**: 接口、注解、函数式
- **异步支持**: 异步发送和处理
- **监控统计**: 详细的性能监控

### 3. 高度可扩展

- **策略模式**: 支持自定义发送策略
- **模板引擎**: 支持自定义模板引擎
- **事件机制**: 支持自定义事件监听
- **缓存管理**: 支持自定义缓存实现

### 4. 生产就绪

- **错误处理**: 完善的异常处理机制
- **重试机制**: 自动重试失败的发送
- **连接池**: 高效的连接管理
- **监控告警**: 健康检查和告警机制

### 5. 性能优化

- **批量发送**: 自动批量处理
- **异步处理**: 非阻塞操作
- **缓存机制**: 模板和配置缓存
- **线程池**: 高效的线程管理

### 6. 安全性

- **SSL/TLS支持**: 加密传输
- **认证机制**: 安全的身份验证
- **配置加密**: 敏感信息保护

## 总结

JavaEmailSpringBoot是一个功能完整、设计优雅的邮件处理框架。它不仅提供了简单易用的API，还具备了企业级应用所需的各种特性。通过合理的架构设计和丰富的扩展点，开发者可以轻松地集成邮件功能，并根据具体需求进行定制化开发。

无论是简单的邮件发送需求，还是复杂的邮件处理场景，该框架都能提供优雅的解决方案。其注解驱动的处理方式、Builder模式的API设计、以及完善的监控机制，都体现了现代Java开发的最佳实践。