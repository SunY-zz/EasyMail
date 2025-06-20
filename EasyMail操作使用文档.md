# EasyMail 操作使用文档

## 项目简介

EasyMail 是一个高度自定义的邮箱服务工具，基于 Spring Boot 开发，提供邮件监听、处理和发送的完整解决方案。该项目支持 IMAP 邮件监听、SMTP 邮件发送、注解驱动的邮件处理器、模板邮件、异步发送等企业级功能。

### 核心特性

- 🚀 **高性能邮件监听**：基于 IMAP IDLE 协议的实时邮件监听
- 📧 **企业级邮件发送**：支持同步/异步、批量发送、重试机制
- 🎯 **注解驱动处理器**：通过注解轻松定义邮件处理逻辑
- 📝 **模板邮件系统**：支持变量替换的邮件模板
- 🔧 **高度可配置**：丰富的配置选项，满足各种使用场景
- 🛡️ **异常处理机制**：完善的异常处理和重试策略
- 📊 **监控统计**：详细的发送统计和性能监控

## 快速开始

### 1. 添加依赖

在 `pom.xml` 中添加依赖：

```xml
<dependency>
    <groupId>cn.sunyblog</groupId>
    <artifactId>easymail-spring-boot-starter</artifactId>
    <version>1.0.0</version>
</dependency>
```

### 2. 配置邮件服务

在 `application.yml` 中配置邮件服务：

```yaml
server:
  port: 8088

# 邮件配置
mail:
  # IMAP 邮件接收配置
  imap:
    server: imap.qq.com
    port: 993
    protocol: imaps
    username: your-email@qq.com
    password: your-auth-code
    attachment-dir: D:\
    # 连接配置
    connection:
      timeout: 15000
      read-timeout: 30000
      write-timeout: 30000
    # 监控配置
    monitor:
      idle-timeout: 20000
      keep-alive-interval: 240
      reconnect-delay: 15
      short-delay: 5
      long-delay: 30
      task-timeout: 300
    listener:
      max-retries: 20
    # 日志配置
    log:
      debug-enabled: false

  # SMTP 邮件发送配置
  smtp:
    server: smtp.163.com
    port: 465
    protocol: smtp
    username: your-send-email@163.com
    password: your-send-auth-code
    connection:
      timeout: 15000
      read-timeout: 30000
      write-timeout: 30000
    properties:
      mail-smtp-auth: true
      mail-smtp-starttls-enable: false
    retry:
      enabled: true
      max-retries: 3
      initial-delay-ms: 1000
      max-delay-ms: 10000
      use-exponential-backoff: true
      backoff-multiplier: 2.0
    log:
      debug-enabled: false

# 注解驱动邮件处理器配置
annotation-driven-email-processor:
  enabled: true
  scan:
    packages:
      - com.yourpackage.processor
```

### 3. 启用邮件服务

在主类上添加注解：

```java
@SpringBootApplication
@EnableEasyMail
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
```

## 核心功能详解

### 1. 邮件发送功能

#### 1.1 基础邮件发送

**注入邮件发送服务：**

```java
@Service
public class EmailService {
    
    @Autowired
    private EasyMailSenderService easyMailSenderService;
    
    // 发送简单文本邮件
    public void sendTextEmail() {
        EasyMailSendResult result = easyMailSenderService.sendText(
            "recipient@example.com",
            "测试邮件",
            "这是一封测试邮件"
        );
        
        if (result.isSuccess()) {
            System.out.println("邮件发送成功，ID: " + result.getMessageId());
        } else {
            System.out.println("邮件发送失败: " + result.getErrorMessage());
        }
    }
    
    // 发送HTML邮件
    public void sendHtmlEmail() {
        String htmlContent = "<h1>欢迎</h1><p>这是一封<strong>HTML</strong>邮件</p>";
        EasyMailSendResult result = easyMailSenderService.sendHtml(
            "recipient@example.com",
            "HTML邮件测试",
            htmlContent
        );
    }
}
```

#### 1.2 Builder 模式发送（推荐）

```java
// 简单文本邮件
EasyMailRequest request = EasyMailRequest.builder()
    .to("recipient@example.com")
    .subject("测试邮件")
    .text("这是邮件内容")
    .build();

EasyMailSendResult result = easyMailSenderService.send(request);

// 多收件人HTML邮件
EasyMailRequest htmlRequest = EasyMailRequest.builder()
    .to("recipient1@example.com")
    .to("recipient2@example.com")
    .cc("cc@example.com")
    .bcc("bcc@example.com")
    .subject("多收件人邮件")
    .html("<h2>HTML邮件内容</h2>")
    .priority(1) // 高优先级
    .build();

result = easyMailSenderService.send(htmlRequest);

// 带附件的邮件
EasyMailRequest attachmentRequest = EasyMailRequest.builder()
    .to("recipient@example.com")
    .subject("带附件的邮件")
    .text("请查看附件")
    .attachment(new File("path/to/file1.pdf"))
    .attachment(new File("path/to/file2.jpg"))
    .build();

EasyMailSendResult result = easyMailSenderService.send(attachmentRequest);
```

#### 1.3 异步邮件发送

```java
// 异步发送
CompletableFuture<EasyMailSendResult> future = easyMailSenderService.sendAsync(request);

future.thenAccept(result -> {
    if (result.isSuccess()) {
        log.info("异步邮件发送成功: {}", result.getMessageId());
    } else {
        log.error("异步邮件发送失败: {}", result.getErrorMessage());
    }
}).exceptionally(throwable -> {
    log.error("异步邮件发送异常", throwable);
    return null;
});
```

#### 1.4 批量邮件发送

```java
// 批量发送给多个收件人
List<String> recipients = Arrays.asList(
    "user1@example.com",
    "user2@example.com",
    "user3@example.com"
);

EasyMailSendResult result = easyMailSenderService.sendToMultiple(
    recipients,
    "批量邮件",
    "这是批量发送的邮件",
    false // 是否HTML格式
);

log.info("批量发送结果: 成功 {}, 失败 {}", 
    result.getSuccessCount(), result.getFailedCount());
```

### 2. 模板邮件系统

#### 2.1 创建邮件模板

```java
@Service
public class TemplateService {
    
    @Autowired
    private EasyMailSendTemplateManager templateManager;
    
    public void createTemplates() {
        // 创建验证码模板
        EasyMailSendTemplate verifyTemplate = EasyMailSendTemplate.builder()
            .templateId("verification")
            .templateName("验证码邮件")
            .subjectTemplate("您的验证码 - ${appName}")
            .contentTemplate(
                "<h2>验证码</h2>" +
                "<p>您好 ${userName}，</p>" +
                "<p>您的验证码是：<strong>${code}</strong></p>" +
                "<p>验证码有效期为 ${expireMinutes} 分钟。</p>"
            )
            .isHtml(true)
            .description("用户验证码邮件模板")
            .build();
        
        templateManager.registerTemplate(verifyTemplate);
        
        // 创建欢迎邮件模板
        EasyMailSendTemplate welcomeTemplate = EasyMailSendTemplate.builder()
            .templateId("welcome")
            .templateName("欢迎邮件")
            .subjectTemplate("欢迎加入 ${companyName}")
            .contentTemplate(
                "<h1>欢迎 ${userName}！</h1>" +
                "<p>感谢您注册 ${companyName}，您的账号已成功激活。</p>" +
                "<p>登录地址：<a href='${loginUrl}'>${loginUrl}</a></p>"
            )
            .isHtml(true)
            .build();
        
        templateManager.registerTemplate(welcomeTemplate);
    }
}
```

#### 2.2 使用模板发送邮件

```java
// 发送验证码邮件
Map<String, Object> variables = new HashMap<>();
variables.put("userName", "张三");
variables.put("appName", "我的应用");
variables.put("code", "123456");
variables.put("expireMinutes", 10);

EasyMailSendTemplate template = templateManager.getTemplate("verification");
EasyMailSendResult result = easyMailSenderService.sendWithTemplate(
    "user@example.com",
    template,
    variables
);

// 使用Builder模式发送模板邮件
EasyMailRequest templateRequest = EasyMailRequest.builder()
    .to("user@example.com")
    .templateId("welcome")
    .variable("userName", "李四")
    .variable("companyName", "示例公司")
    .variable("loginUrl", "https://example.com/login")
    .build();

EasyMailSendResult result = easyMailSenderService.send(templateRequest);
```

### 3. 邮件监听和处理

#### 3.1 注解驱动的邮件处理器

```java
@Component
@EasyMailProcessor(
    group = "business",
    description = "业务邮件处理器",
    enabled = true
)
public class BusinessEmailProcessor {
    
    // 处理验证码邮件
    @EasyMailProcessorHandler(
        name = "verificationHandler",
        description = "处理验证码邮件",
        subject = "(?i).*(验证码|verification|code).*",
        subjectMatchType = EasyMailProcessorHandler.MatchType.REGEX,
        priority = 100,
        async = false
    )
    public void handleVerificationCode(EasyMailContext context) {
        log.info("收到验证码邮件: {}", context.getSubject());
        
        String code = extractVerificationCode(context.getContent());
        if (code != null) {
            log.info("提取到验证码: {}", code);
            // 存储验证码到缓存
            cacheService.storeVerificationCode(context.getSender(), code);
        }
    }
    
    // 处理订单邮件
    @EasyMailProcessorHandler(
        name = "orderHandler",
        description = "处理订单邮件",
        subject = "(?i).*(订单|order|购买).*",
        subjectMatchType = EasyMailProcessorHandler.MatchType.REGEX,
        priority = 90,
        async = true
    )
    public void handleOrderEmail(EasyMailContext context) {
        log.info("收到订单邮件: {}", context.getSubject());
        
        String orderNumber = extractOrderNumber(context.getContent());
        if (orderNumber != null) {
            // 异步处理订单信息
            orderService.processOrderEmail(orderNumber, context);
        }
    }
    
    // 处理特定发件人的邮件
    @EasyMailProcessorHandler(
        name = "systemNotificationHandler",
        description = "处理系统通知邮件",
        from = "(?i).*(noreply|system|notification).*",
        fromMatchType = EasyMailProcessorHandler.MatchType.REGEX,
        priority = 80
    )
    public void handleSystemNotification(EasyMailContext context) {
        log.info("收到系统通知: {}", context.getSubject());
        
        // 分析通知类型并处理
        String notificationType = analyzeNotificationType(context);
        notificationService.processNotification(notificationType, context);
    }
    
    // 默认处理器
    @EasyMailProcessorHandler(
        name = "defaultHandler",
        description = "默认邮件处理器",
        subject = ".*",
        subjectMatchType = EasyMailProcessorHandler.MatchType.REGEX,
        priority = 10
    )
    public void handleDefault(EasyMailContext context) {
        log.info("默认处理器处理邮件: {}", context.getSubject());
        // 记录未分类的邮件
        emailLogService.logUnclassifiedEmail(context);
    }
    
    private String extractVerificationCode(String content) {
        Pattern pattern = Pattern.compile("\\b(\\d{4,6})\\b");
        Matcher matcher = pattern.matcher(content);
        return matcher.find() ? matcher.group(1) : null;
    }
    
    private String extractOrderNumber(String content) {
        Pattern pattern = Pattern.compile("(?i)订单号[：:]?\\s*([A-Z0-9]{8,20})");
        Matcher matcher = pattern.matcher(content);
        return matcher.find() ? matcher.group(1) : null;
    }
}
```

#### 3.2 实现邮件监听器接口

```java
@Component
public class CustomEmailListener implements EasyMailListenerApi {
    
    @Override
    public boolean processEmail(Message message, String content, String subject, String from) {
        log.info("收到新邮件: {} 来自: {}", subject, from);
        
        try {
            // 自定义邮件处理逻辑
            if (subject.contains("重要")) {
                handleImportantEmail(message, content, subject, from);
            } else if (from.contains("@bank.com")) {
                handleBankEmail(message, content, subject, from);
            }
            
            return true;
        } catch (Exception e) {
            log.error("邮件处理失败", e);
            return false;
        }
    }
    
    @Override
    public String getProcessorName() {
        return "自定义邮件监听器";
    }
    
    private void handleImportantEmail(Message message, String content, String subject, String from) {
        // 处理重要邮件的逻辑
        notificationService.sendUrgentNotification(subject, from);
    }
    
    private void handleBankEmail(Message message, String content, String subject, String from) {
        // 处理银行邮件的逻辑
        bankService.processBankNotification(content, subject);
    }
}
```

#### 3.3 函数式邮件处理器

```java
@Component
public class FunctionalEmailProcessor {
    
    @Autowired
    private MailProcessor mailProcessor;
    
    @PostConstruct
    public void init() {
        // 设置函数式邮件处理器
        mailProcessor.setEasyMailProcessorFunction(this::processEmail);
    }
    
    public String processEmail(Message message, String content, String subject, String from) {
        log.info("函数式处理邮件: {}", subject);
        
        // 根据邮件内容执行不同的处理逻辑
        if (subject.contains("验证码")) {
            return extractAndStoreVerificationCode(content, from);
        } else if (subject.contains("账单")) {
            processBillEmail(content, from);
        }
        
        return null;
    }
    
    private String extractAndStoreVerificationCode(String content, String from) {
        String code = extractVerificationCode(content);
        if (code != null) {
            cacheService.storeVerificationCode(from, code);
        }
        return code;
    }
}
```

### 4. 邮件服务控制

#### 4.1 服务启动和停止

```java
@RestController
@RequestMapping("/api/email")
public class EmailController {
    
    @Autowired
    private EasyMailService easyMailService;
    
    @Autowired
    private EasyMailSenderService senderService;
    
    // 启动邮件监听服务
    @PostMapping("/start")
    public ResponseEntity<String> startEmailService() {
        boolean success = easyMailService.startMailMonitoring();
        return ResponseEntity.ok(success ? "邮件服务启动成功" : "邮件服务启动失败");
    }
    
    // 停止邮件监听服务
    @PostMapping("/stop")
    public ResponseEntity<String> stopEmailService() {
        easyMailService.stopMailMonitoring();
        return ResponseEntity.ok("邮件服务已停止");
    }
    
    // 获取服务状态
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getServiceStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("running", easyMailService.isMailServiceRunning());
        status.put("stats", easyMailService.getMailProcessingStats());
        status.put("connection", senderService.checkConnection());
        return ResponseEntity.ok(status);
    }
    
    // 获取发送统计
    @GetMapping("/send-stats")
    public ResponseEntity<Map<String, Object>> getSendStats() {
        return ResponseEntity.ok(senderService.getDetailedStatistics());
    }
    
    // 获取健康状态
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> getHealthStatus() {
        return ResponseEntity.ok(senderService.getHealthStatus());
    }
}
```

## 高级功能

### 1. 自定义发送策略

```java
@Component
public class CustomSendStrategy implements EasyMailSendStrategy {
    
    @Override
    public String getStrategyName() {
        return "custom";
    }
    
    @Override
    public boolean canHandle(EasyMailRequest request) {
        // 自定义策略适用条件
        return request.getPriority() != null && request.getPriority() == 1;
    }
    
    @Override
    public EasyMailSendResult execute(EasyMailRequest request, EasyMailSender sender) {
        // 自定义发送逻辑
        log.info("使用自定义策略发送高优先级邮件");
        
        // 可以在这里实现特殊的发送逻辑
        // 比如使用不同的SMTP服务器、特殊的重试机制等
        
        return sender.sendEmail(request);
    }
    
    @Override
    public int getPriority() {
        return 100; // 高优先级策略
    }
}
```

### 2. 自定义模板引擎

```java
@Component
public class CustomTemplateEngine implements EasyMailSendTemplateEngine {
    
    @Override
    public String processTemplate(String template, Map<String, Object> variables) {
        // 实现自定义的模板处理逻辑
        // 比如支持更复杂的模板语法、条件判断、循环等
        
        String result = template;
        for (Map.Entry<String, Object> entry : variables.entrySet()) {
            String placeholder = "${" + entry.getKey() + "}";
            String value = String.valueOf(entry.getValue());
            result = result.replace(placeholder, value);
        }
        
        return result;
    }
    
    @Override
    public boolean supports(String templateType) {
        return "custom".equals(templateType);
    }
}
```

### 3. 邮件发送事件监听

```java
@Component
public class EmailSendEventListener {
    
    @EventListener
    public void handleEmailSendSuccess(EasyMailSendSuccessEvent event) {
        log.info("邮件发送成功: {}", event.getMessageId());
        
        // 记录发送成功的邮件
        emailLogService.logSuccessfulSend(event);
        
        // 更新用户通知状态
        userService.updateNotificationStatus(event.getRecipients(), true);
    }
    
    @EventListener
    public void handleEmailSendFailure(EasyMailSendFailureEvent event) {
        log.error("邮件发送失败: {}", event.getErrorMessage());
        
        // 记录发送失败的邮件
        emailLogService.logFailedSend(event);
        
        // 如果是重要邮件，发送告警
        if (event.getPriority() <= 2) {
            alertService.sendEmailFailureAlert(event);
        }
    }
}
```

### 4. 自定义缓存管理器

```java
@Component
public class CustomCacheManager implements EasyMailCacheManager {
    
    private final RedisTemplate<String, Object> redisTemplate;
    
    public CustomCacheManager(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }
    
    @Override
    public void put(String key, Object value, long expireSeconds) {
        redisTemplate.opsForValue().set(key, value, expireSeconds, TimeUnit.SECONDS);
    }
    
    @Override
    public <T> T get(String key, Class<T> type) {
        Object value = redisTemplate.opsForValue().get(key);
        return type.cast(value);
    }
    
    @Override
    public void remove(String key) {
        redisTemplate.delete(key);
    }
    
    @Override
    public boolean exists(String key) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }
}
```

## 配置详解

### 1. IMAP 配置

```yaml
mail:
  imap:
    server: imap.qq.com          # IMAP服务器地址
    port: 993                    # IMAP端口
    protocol: imaps              # 协议（imap/imaps）
    username: your@email.com     # 邮箱账号
    password: auth-code          # 授权码
    attachment-dir: /tmp/        # 附件保存目录
    
    # 连接配置
    connection:
      timeout: 15000             # 连接超时（毫秒）
      read-timeout: 30000        # 读取超时（毫秒）
      write-timeout: 30000       # 写入超时（毫秒）
    
    # 监控配置
    monitor:
      idle-timeout: 20000        # IDLE超时时间（毫秒）
      keep-alive-interval: 240   # 保活间隔（秒）
      reconnect-delay: 15        # 重连延迟（秒）
      short-delay: 5             # 短延迟（秒）
      long-delay: 30             # 长延迟（秒）
      task-timeout: 300          # 任务超时（秒）
    
    # 监听器配置
    listener:
      max-retries: 20            # 最大重试次数
    
    # 日志配置
    log:
      debug-enabled: false       # 是否启用调试日志
```

### 2. SMTP 配置

```yaml
mail:
  smtp:
    server: smtp.163.com         # SMTP服务器地址
    port: 465                    # SMTP端口
    protocol: smtp               # 协议
    username: send@email.com     # 发送邮箱
    password: send-auth-code     # 发送邮箱授权码
    
    # 连接配置
    connection:
      timeout: 15000
      read-timeout: 30000
      write-timeout: 30000
    
    # SMTP属性配置
    properties:
      mail-smtp-auth: true              # 启用SMTP认证
      mail-smtp-starttls-enable: false  # 启用STARTTLS
      mail-smtp-ssl-enable: true        # 启用SSL
    
    # 重试配置
    retry:
      enabled: true                     # 启用重试
      max-retries: 3                   # 最大重试次数
      initial-delay-ms: 1000           # 初始延迟
      max-delay-ms: 10000              # 最大延迟
      use-exponential-backoff: true    # 指数退避
      backoff-multiplier: 2.0          # 退避乘数
    
    # 日志配置
    log:
      debug-enabled: false
```

### 3. 注解处理器配置

```yaml
annotation-driven-email-processor:
  enabled: true                 # 启用注解处理器
  scan:
    packages:                   # 扫描包路径
      - com.example.processor
      - com.example.handler
  
  # 处理器配置
  processor:
    default-async: false        # 默认是否异步处理
    thread-pool:
      core-size: 5             # 核心线程数
      max-size: 20             # 最大线程数
      queue-capacity: 100      # 队列容量
      keep-alive: 60           # 线程存活时间（秒）
```

### 4. 线程池配置

```yaml
easymail:
  thread-pool:
    core-pool-size: 5           # 核心线程数
    maximum-pool-size: 20       # 最大线程数
    keep-alive-time: 60         # 线程存活时间（秒）
    queue-capacity: 1000        # 队列容量
    thread-name-prefix: "EasyMail-"  # 线程名前缀
    rejection-policy: "CallerRuns"   # 拒绝策略
```

## 监控和统计

### 1. 获取发送统计

```java
@Service
public class MonitorService {
    
    @Autowired
    private EasyMailSenderService senderService;
    
    public void printStatistics() {
        // 获取基础统计信息
        String basicStats = senderService.getSendingStats();
        log.info("基础统计: {}", basicStats);
        
        // 获取详细统计信息
        Map<String, Object> detailedStats = senderService.getDetailedStatistics();
        log.info("详细统计: {}", detailedStats);
        
        // 获取策略统计
        Map<String, Object> strategyStats = senderService.getStrategyStatistics();
        log.info("策略统计: {}", strategyStats);
        
        // 获取健康状态
        Map<String, Object> healthStatus = senderService.getHealthStatus();
        log.info("健康状态: {}", healthStatus);
        
        // 获取发送趋势
        Map<String, Object> sendTrend = senderService.getSendTrend();
        log.info("发送趋势: {}", sendTrend);
        
        // 生成监控报告
        String report = senderService.generateMonitorReport();
        log.info("监控报告:\n{}", report);
    }
}
```

### 2. 自定义监控指标

```java
@Component
public class CustomEmailMonitor {
    
    private final MeterRegistry meterRegistry;
    private final Counter emailSentCounter;
    private final Counter emailFailedCounter;
    private final Timer emailSendTimer;
    
    public CustomEmailMonitor(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.emailSentCounter = Counter.builder("email.sent")
            .description("Number of emails sent")
            .register(meterRegistry);
        this.emailFailedCounter = Counter.builder("email.failed")
            .description("Number of emails failed")
            .register(meterRegistry);
        this.emailSendTimer = Timer.builder("email.send.duration")
            .description("Email send duration")
            .register(meterRegistry);
    }
    
    @EventListener
    public void onEmailSent(EasyMailSendSuccessEvent event) {
        emailSentCounter.increment();
        emailSendTimer.record(event.getDuration(), TimeUnit.MILLISECONDS);
    }
    
    @EventListener
    public void onEmailFailed(EasyMailSendFailureEvent event) {
        emailFailedCounter.increment();
    }
}
```

## 最佳实践

### 1. 邮件发送最佳实践

```java
@Service
public class EmailBestPracticeService {
    
    @Autowired
    private EasyMailSenderService senderService;
    
    // 1. 使用Builder模式，代码更清晰
    public void sendWelcomeEmail(String userEmail, String userName) {
        EasyMailRequest request = EasyMailRequest.builder()
            .to(userEmail)
            .subject("欢迎加入我们的平台")
            .html(buildWelcomeEmailContent(userName))
            .priority(2) // 中等优先级
            .build();
        
        // 验证请求
        EasyMailRequest.ValidationResult validation = request.validate();
        if (!validation.isValid()) {
            log.error("邮件请求验证失败: {}", validation.getErrorMessage());
            return;
        }
        
        // 发送邮件
        EasyMailSendResult result = senderService.send(request);
        handleSendResult(result, "欢迎邮件");
    }
    
    // 2. 批量发送时使用异步
    public void sendBatchNotifications(List<String> recipients, String subject, String content) {
        EasyMailRequest request = EasyMailRequest.builder()
            .subject(subject)
            .html(content)
            .async(true) // 异步发送
            .build();
        
        // 分批发送，避免一次发送过多
        int batchSize = 50;
        for (int i = 0; i < recipients.size(); i += batchSize) {
            List<String> batch = recipients.subList(i, Math.min(i + batchSize, recipients.size()));
            
            EasyMailRequest batchRequest = request.toBuilder()
                .clearTo()
                .toList(batch)
                .build();
            
            senderService.sendAsync(batchRequest)
                .thenAccept(result -> handleSendResult(result, "批量通知"))
                .exceptionally(throwable -> {
                    log.error("批量邮件发送异常", throwable);
                    return null;
                });
        }
    }
    
    // 3. 使用模板提高复用性
    public void sendVerificationCode(String userEmail, String code) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("code", code);
        variables.put("expireMinutes", 10);
        variables.put("userEmail", userEmail);
        
        EasyMailRequest request = EasyMailRequest.builder()
            .to(userEmail)
            .templateId("verification")
            .templateVariables(variables)
            .priority(1) // 高优先级
            .build();
        
        EasyMailSendResult result = senderService.send(request);
        handleSendResult(result, "验证码邮件");
    }
    
    private void handleSendResult(EasyMailSendResult result, String emailType) {
        if (result.isSuccess()) {
            log.info("{}发送成功，ID: {}, 耗时: {}ms", 
                emailType, result.getMessageId(), result.getDuration());
        } else {
            log.error("{}发送失败: {}", emailType, result.getErrorMessage());
            // 可以在这里添加失败处理逻辑，比如重试、告警等
        }
    }
    
    private String buildWelcomeEmailContent(String userName) {
        return "<html><body>" +
               "<h1>欢迎 " + userName + "！</h1>" +
               "<p>感谢您注册我们的平台...</p>" +
               "</body></html>";
    }
}
```

### 2. 邮件处理最佳实践

```java
@Component
@EasyMailProcessor(group = "business", description = "业务邮件处理器")
public class EmailProcessorBestPractice {
    
    // 1. 使用具体的匹配条件，避免误匹配
    @EasyMailProcessorHandler(
        name = "orderConfirmationHandler",
        subject = "^订单确认.*|^Order Confirmation.*",
        subjectMatchType = EasyMailProcessorHandler.MatchType.REGEX,
        from = ".*@shop\\.example\\.com",
        fromMatchType = EasyMailProcessorHandler.MatchType.REGEX,
        priority = 100
    )
    public void handleOrderConfirmation(EasyMailContext context) {
        try {
            log.info("处理订单确认邮件: {}", context.getSubject());
            
            // 提取订单信息
            OrderInfo orderInfo = extractOrderInfo(context.getContent());
            if (orderInfo != null) {
                orderService.confirmOrder(orderInfo);
                log.info("订单确认处理完成: {}", orderInfo.getOrderNumber());
            }
            
        } catch (Exception e) {
            log.error("订单确认邮件处理失败", e);
            // 发送告警或记录到错误队列
            errorHandlingService.handleProcessingError(context, e);
        }
    }
    
    // 2. 异步处理耗时操作
    @EasyMailProcessorHandler(
        name = "reportHandler",
        subject = ".*报告.*|.*report.*",
        subjectMatchType = EasyMailProcessorHandler.MatchType.REGEX,
        priority = 80,
        async = true // 异步处理
    )
    public void handleReport(EasyMailContext context) {
        log.info("异步处理报告邮件: {}", context.getSubject());
        
        // 处理附件中的报告文件
        if (context.getAttachments() != null && !context.getAttachments().isEmpty()) {
            for (File attachment : context.getAttachments()) {
                if (attachment.getName().endsWith(".pdf") || attachment.getName().endsWith(".xlsx")) {
                    reportService.processReportFile(attachment);
                }
            }
        }
    }
    
    // 3. 使用优先级控制处理顺序
    @EasyMailProcessorHandler(
        name = "urgentHandler",
        subject = ".*紧急.*|.*urgent.*|.*URGENT.*",
        subjectMatchType = EasyMailProcessorHandler.MatchType.REGEX,
        priority = 200, // 最高优先级
        async = false
    )
    public void handleUrgentEmail(EasyMailContext context) {
        log.warn("收到紧急邮件: {}", context.getSubject());
        
        // 立即处理紧急邮件
        urgentEmailService.processUrgentEmail(context);
        
        // 发送即时通知
        notificationService.sendUrgentNotification(context);
    }
    
    private OrderInfo extractOrderInfo(String content) {
        // 实现订单信息提取逻辑
        Pattern pattern = Pattern.compile("订单号[：:]?\\s*([A-Z0-9]{10,20})");
        Matcher matcher = pattern.matcher(content);
        
        if (matcher.find()) {
            OrderInfo orderInfo = new OrderInfo();
            orderInfo.setOrderNumber(matcher.group(1));
            return orderInfo;
        }
        
        return null;
    }
}
```

### 3. 错误处理和重试策略

```java
@Service
public class EmailErrorHandlingService {
    
    @Autowired
    private EasyMailSenderService senderService;
    
    @Retryable(
        value = {EasyMailSendException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public EasyMailSendResult sendWithRetry(EasyMailRequest request) {
        try {
            return senderService.send(request);
        } catch (EasyMailSendException e) {
            log.warn("邮件发送失败，准备重试: {}", e.getMessage());
            throw e; // 重新抛出异常以触发重试
        }
    }
    
    @Recover
    public EasyMailSendResult recover(EasyMailSendException e, EasyMailRequest request) {
        log.error("邮件发送最终失败，已达到最大重试次数: {}", e.getMessage());
        
        // 记录失败的邮件到数据库或消息队列
        failedEmailService.recordFailedEmail(request, e.getMessage());
        
        // 返回失败结果
        return EasyMailSendResult.builder()
            .success(false)
            .errorMessage("邮件发送失败: " + e.getMessage())
            .build();
    }
}
```

## 故障排查

### 1. 常见问题及解决方案

#### 问题1：邮件监听器无法启动

**可能原因：**
- IMAP配置错误
- 网络连接问题
- 授权码错误

**解决方案：**
```java
// 检查配置
@Component
public class EmailConfigValidator {
    
    @Autowired
    private EasyMailConfig config;
    
    @PostConstruct
    public void validateConfig() {
        if (StringUtils.isEmpty(config.getServer())) {
            throw new EasyMailConfigException("IMAP服务器地址不能为空");
        }
        
        if (StringUtils.isEmpty(config.getUsername()) || StringUtils.isEmpty(config.getPassword())) {
            throw new EasyMailConfigException("邮箱用户名和密码不能为空");
        }
        
        // 测试连接
        try {
            boolean connected = easyMailSenderService.checkConnection();
            if (!connected) {
                log.warn("邮件服务器连接测试失败，请检查配置");
            }
        } catch (Exception e) {
            log.error("邮件服务器连接测试异常", e);
        }
    }
}
```

#### 问题2：邮件发送失败

**可能原因：**
- SMTP配置错误
- 收件人地址无效
- 邮件内容被拒绝

**解决方案：**
```java
// 详细的错误处理
public void sendEmailWithDetailedErrorHandling(EasyMailRequest request) {
    try {
        // 验证请求
        request.validateAndThrow();
        
        EasyMailSendResult result = senderService.send(request);
        
        if (!result.isSuccess()) {
            handleSendFailure(result, request);
        }
        
    } catch (EasyMailValidationException e) {
        log.error("邮件请求验证失败: {}", e.getErrors());
    } catch (EasyMailSendException e) {
        log.error("邮件发送异常: {}", e.getMessage());
        // 根据异常类型进行不同处理
        if (e.getCause() instanceof AuthenticationFailedException) {
            log.error("SMTP认证失败，请检查用户名和密码");
        } else if (e.getCause() instanceof MessagingException) {
            log.error("邮件协议异常，请检查SMTP配置");
        }
    }
}

private void handleSendFailure(EasyMailSendResult result, EasyMailRequest request) {
    String errorMessage = result.getErrorMessage();
    
    if (errorMessage.contains("Invalid Addresses")) {
        log.error("收件人地址无效: {}", request.getToList());
    } else if (errorMessage.contains("Authentication failed")) {
        log.error("SMTP认证失败，请检查发送邮箱配置");
    } else if (errorMessage.contains("Message rejected")) {
        log.error("邮件内容被拒绝，可能包含敏感内容");
    } else {
        log.error("邮件发送失败，未知错误: {}", errorMessage);
    }
}
```

#### 问题3：邮件处理器不生效

**可能原因：**
- 注解扫描路径配置错误
- 匹配条件不正确
- 处理器被禁用

**解决方案：**
```java
// 调试邮件处理器
@Component
public class EmailProcessorDebugger {
    
    @Autowired
    private AnnotationDrivenEasyMailProcessorManager processorManager;
    
    @PostConstruct
    public void debugProcessors() {
        // 列出所有注册的处理器
        List<EasyMailHandlerInfo> handlers = processorManager.getAllHandlers();
        log.info("已注册的邮件处理器数量: {}", handlers.size());
        
        for (EasyMailHandlerInfo handler : handlers) {
            log.info("处理器: {}, 优先级: {}, 启用: {}", 
                handler.getName(), handler.getPriority(), handler.isEnabled());
        }
    }
    
    // 测试处理器匹配
    public void testProcessorMatching(String subject, String from) {
        List<EasyMailHandlerInfo> matchedHandlers = processorManager.findMatchingHandlers(subject, from);
        log.info("匹配的处理器数量: {}", matchedHandlers.size());
        
        for (EasyMailHandlerInfo handler : matchedHandlers) {
            log.info("匹配的处理器: {}", handler.getName());
        }
    }
}
```

### 2. 性能优化建议

```java
// 性能监控和优化
@Component
public class EmailPerformanceOptimizer {
    
    @Autowired
    private EasyMailSenderService senderService;
    
    // 1. 监控发送性能
    @Scheduled(fixedRate = 60000) // 每分钟执行一次
    public void monitorPerformance() {
        Map<String, Object> stats = senderService.getDetailedStatistics();
        
        Long avgDuration = (Long) stats.get("averageDuration");
        Long totalSent = (Long) stats.get("totalSent");
        Long totalFailed = (Long) stats.get("totalFailed");
        
        if (avgDuration > 5000) { // 平均发送时间超过5秒
            log.warn("邮件发送性能较差，平均耗时: {}ms", avgDuration);
        }
        
        if (totalFailed > 0) {
            double failureRate = (double) totalFailed / (totalSent + totalFailed) * 100;
            if (failureRate > 5) { // 失败率超过5%
                log.warn("邮件发送失败率较高: {:.2f}%", failureRate);
            }
        }
    }
    
    // 2. 优化建议
    public void optimizationSuggestions() {
        String threadPoolStatus = senderService.getThreadPoolStatus();
        log.info("线程池状态: {}", threadPoolStatus);
        
        // 根据线程池状态给出优化建议
        if (threadPoolStatus.contains("queue full")) {
            log.warn("建议增加线程池大小或队列容量");
        }
        
        if (threadPoolStatus.contains("high utilization")) {
            log.warn("建议考虑异步发送或批量发送优化");
        }
    }
}
```

## 项目实现原理

### 1. 架构设计

EasyMail 采用分层架构设计：

- **API层**：提供统一的接口定义
- **服务层**：实现核心业务逻辑
- **配置层**：管理各种配置和自动装配
- **工具层**：提供通用工具和异常处理

### 2. 核心组件

#### 2.1 邮件监听器 (EasyMailListener)
- 基于 IMAP IDLE 协议实现实时邮件监听
- 支持自动重连和异常恢复
- 多线程处理邮件事件

#### 2.2 邮件发送器 (EasyMailSender)
- 支持多种发送策略
- 内置重试机制和失败处理
- 异步发送和批量发送优化

#### 2.3 注解处理器 (AnnotationProcessor)
- 基于 Spring AOP 实现
- 支持条件匹配和优先级排序
- 动态方法调用和参数注入

#### 2.4 模板引擎 (TemplateEngine)
- 支持变量替换和条件渲染
- 可扩展的模板语法
- 缓存机制提升性能

### 3. 设计模式应用

- **Builder模式**：EasyMailRequest 构建
- **策略模式**：邮件发送策略
- **观察者模式**：邮件事件监听
- **模板方法模式**：邮件处理流程
- **工厂模式**：组件创建和管理

## 项目优点

### 1. 易用性
- 配置简单，开箱即用
- 注解驱动，简化开发
- Builder模式，链式调用
- 丰富的示例和文档

### 2. 可扩展性
- 插件化架构设计
- 自定义处理器支持
- 可替换的组件实现
- 灵活的配置选项

### 3. 可靠性
- 完善的异常处理机制
- 自动重试和故障恢复
- 连接池和资源管理
- 详细的日志和监控

### 4. 性能
- 异步处理机制
- 连接复用和池化
- 批量操作优化
- 内存和CPU效率优化

### 5. 企业级特性
- 多租户支持
- 安全认证机制
- 监控和统计功能
- 高可用性设计

---

## 总结

EasyMail 是一个功能完整、易于使用的邮件服务框架，适用于各种规模的项目。通过本文档的介绍，您应该能够：

1. 快速集成和配置 EasyMail
2. 使用各种邮件发送功能
3. 实现自定义的邮件处理逻辑
4. 进行性能优化和故障排查
5. 扩展和定制框架功能

如果您在使用过程中遇到问题，请参考故障排查章节或查看项目的 GitHub 仓库获取更多帮助。