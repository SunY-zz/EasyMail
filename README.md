# EasyMail - 企业级邮件处理SDK

[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![Java](https://img.shields.io/badge/Java-8%2B-orange.svg)](https://www.oracle.com/java/technologies/javase-downloads.html)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-2.x-green.svg)](https://spring.io/projects/spring-boot)
[![Maven Central](https://img.shields.io/badge/Maven%20Central-1.0.0-brightgreen.svg)](https://search.maven.org/artifact/cn.sunyblog/easymail-spring-boot-starter)

一个功能强大、易于集成的企业级Java邮件处理SDK，基于Spring Boot，提供邮件监听、智能处理和批量发送的完整解决方案。

## ✨ 核心特性

### 📧 邮件监听与处理
- **实时监听**：支持IMAP/IMAPS协议，实时监听新邮件
- **注解驱动**：使用`@EmailProcessor`和`@EmailHandler`注解轻松定义处理逻辑
- **智能匹配**：支持主题、发件人、内容的精确匹配、包含匹配和正则表达式匹配
- **优先级控制**：支持处理器优先级设置，确保重要邮件优先处理
- **异步处理**：支持同步和异步处理模式，提高系统性能

### 🚀 邮件发送服务
- **多种策略**：默认策略、批量策略、高优先级策略
- **模板引擎**：支持变量替换的邮件模板系统
- **批量发送**：并行处理大量邮件发送，性能优异
- **智能重试**：可配置的重试机制和失败处理
- **异步发送**：支持异步发送和回调处理

### 🛡️ 企业级特性
- **自动配置**：Spring Boot自动配置，开箱即用
- **监控统计**：完整的性能监控和统计信息
- **健康检查**：内置健康检查和故障恢复机制
- **线程安全**：高并发环境下的线程安全保证
- **扩展性强**：支持自定义策略和监听器

## 🚀 快速开始

### 1. 添加依赖

**Maven:**
```xml
<dependency>
    <groupId>cn.sunyblog</groupId>
    <artifactId>easymail-spring-boot-starter</artifactId>
    <version>1.0.0</version>
</dependency>
```

**Gradle:**
```groovy
implementation 'cn.sunyblog:easymail-spring-boot-starter:1.0.0'
```

### 重要说明

**版本 1.0.0 已修复 "No qualifying bean of type 'MailConfig'" 错误**

- ✅ 修复了 MailConfig Bean 无法正确注册的问题
- ✅ 确保在任何配置情况下都能创建 MailConfig Bean
- ✅ 支持两种配置格式：`mail.imap.*` 和 `email.listener.*`
- ✅ 提供向后兼容性，无需修改现有配置

### 2. 基础配置

在`application.yml`中添加邮件服务器配置。SDK支持两种配置格式：

#### 方式一：推荐的新配置格式

```yaml
email:
  # 邮件监听配置
  listener:
    enabled: true
    server:
      host: imap.gmail.com
      port: 993
      protocol: imaps
      username: your-email@gmail.com
      password: your-app-password
      folder: INBOX
    connection:
      timeout: 15000
      read-timeout: 30000
      write-timeout: 30000
    monitor:
      idle-timeout: 10000
      keep-alive-interval: 300
      reconnect-delay: 15
    listener:
      max-retries: 20
      auto-start: true
    attachment:
      save-dir: ./attachments
  
  # 邮件发送配置
  smtp:
    host: smtp.gmail.com
    port: 587
    username: your-email@gmail.com
    password: your-app-password
    auth: true
    starttls: true
  
  # 注解驱动处理器配置
  annotation-driven-email-processor:
    enabled: true
    management:
      enabled: true
    scan:
      packages:
        - com.yourcompany.email.processors
```

#### 方式二：兼容的传统配置格式

```yaml
mail:
  imap:
    server: imap.gmail.com
    port: 993
    protocol: imaps
    username: your-email@gmail.com
    password: your-app-password
    attachment-dir: ./attachments
    connection:
      timeout: 15000
      read-timeout: 30000
      write-timeout: 30000
    monitor:
      idle-timeout: 10000
      keep-alive-interval: 300
      reconnect-delay: 15
    listener:
      max-retries: 20
    log:
      debug-enabled: false

# 注解驱动处理器配置
annotation-driven-email-processor:
  enabled: true
  scan:
    packages:
      - com.yourcompany.email.processors
```

> **注意**：两种配置格式都支持，但推荐使用新的`email.listener`格式，它提供了更好的结构化配置和更多的配置选项。

### 3. 创建邮件处理器

使用注解驱动的方式创建邮件处理器：

```java
@Component
@EmailProcessor(
    group = "business",
    description = "业务邮件处理器",
    enabled = true
)
public class BusinessEmailProcessor {
    
    private static final Logger log = LoggerFactory.getLogger(BusinessEmailProcessor.class);
    
    /**
     * 处理验证码邮件
     * 支持中英文验证码邮件的智能识别
     */
    @EmailHandler(
        name = "verificationCodeHandler",
        description = "处理验证码邮件",
        subjectPattern = "(?i).*(验证码|verification|code).*",
        matchType = EmailHandler.MatchType.REGEX,
        priority = 100,
        async = false
    )
    public void handleVerificationCode(EmailContext context) {
        log.info("收到验证码邮件: {}", context.getSubject());
        
        // 智能提取验证码
        String code = extractVerificationCode(context.getContent());
        if (code != null) {
            log.info("提取到验证码: {}", code);
            // 处理验证码逻辑
            processVerificationCode(code, context);
        }
    }
    
    /**
     * 处理订单邮件
     * 异步处理提高性能
     */
    @EmailHandler(
        name = "orderHandler",
        description = "处理订单邮件",
        subjectPattern = "(?i).*(订单|order|购买).*",
        matchType = EmailHandler.MatchType.REGEX,
        priority = 90,
        async = true
    )
    public void handleOrderEmail(EmailContext context) {
        log.info("收到订单邮件: {}", context.getSubject());
        // 异步处理订单逻辑
        processOrderAsync(context);
    }
    
    /**
     * 处理重要邮件
     * 基于发件人匹配
     */
    @EmailHandler(
        name = "importantHandler",
        description = "处理重要邮件",
        senderPattern = ".*@important-company\\.com",
        matchType = EmailHandler.MatchType.REGEX,
        priority = 200,
        async = false
    )
    public void handleImportantEmail(EmailContext context) {
        log.warn("收到重要邮件: {} from {}", context.getSubject(), context.getSender());
        // 立即处理重要邮件
        processImportantEmail(context);
    }
    
    private String extractVerificationCode(String content) {
        // 使用内置的智能验证码提取功能
        // 支持多种验证码格式的自动识别
        return VerificationCodeExtractor.extract(content);
    }
    
    private void processVerificationCode(String code, EmailContext context) {
        // 实现验证码处理逻辑
    }
    
    private void processOrderAsync(EmailContext context) {
        // 实现订单处理逻辑
    }
    
    private void processImportantEmail(EmailContext context) {
        // 实现重要邮件处理逻辑
    }
}
```

### 4. 发送邮件

#### 基本发送

```java
@Service
public class NotificationService {
    
    @Resource
    private EmailSenderService emailSenderService;
    
    @Resource
    private EmailSenderStarter emailSenderStarter;
    
    /**
     * 发送简单文本邮件
     */
    public void sendWelcomeEmail(String userEmail, String userName) {
        SendResult result = emailSenderService.sendText(
            userEmail,
            "欢迎加入我们！",
            String.format("亲爱的 %s，欢迎加入我们的平台！", userName)
        );
        
        if (result.isSuccess()) {
            log.info("欢迎邮件发送成功，消息ID: {}", result.getMessageId());
        } else {
            log.error("欢迎邮件发送失败: {}", result.getErrorMessage());
        }
    }
    
    /**
     * 发送HTML邮件
     */
    public void sendHtmlNotification(String userEmail, String content) {
        String htmlContent = String.format(
            "<html><body><h2>系统通知</h2><p>%s</p></body></html>",
            content
        );
        
        boolean success = emailSenderStarter.sendHtml(
            userEmail,
            "系统通知",
            htmlContent
        );
        
        log.info("HTML邮件发送结果: {}", success ? "成功" : "失败");
    }
}
```

#### 模板邮件

```java
/**
 * 使用内置模板发送验证码邮件
 */
public void sendVerificationCode(String userEmail, String code) {
    Map<String, Object> variables = new HashMap<>();
    variables.put("code", code);
    variables.put("expireMinutes", 10);
    variables.put("userName", "用户");
    
    boolean success = emailSenderStarter.sendWithTemplate(
        userEmail,
        "verification",  // 内置验证码模板
        variables
    );
    
    log.info("验证码邮件发送结果: {}", success ? "成功" : "失败");
}

/**
 * 创建和使用自定义模板
 */
public void sendCustomTemplate(String userEmail) {
    // 创建自定义模板
    EmailTemplate template = new EmailTemplate(
        "welcome",
        "欢迎加入{{company}}",
        "<h2>欢迎 {{name}}！</h2><p>感谢您加入{{company}}，我们很高兴为您服务。</p>",
        true  // 是否为HTML模板
    );
    
    // 注册模板
    emailSenderStarter.getTemplateManager().registerTemplate(template);
    
    // 使用模板发送邮件
    Map<String, Object> vars = new HashMap<>();
    vars.put("name", "张三");
    vars.put("company", "示例科技有限公司");
    
    SendResult result = emailSenderService.sendWithTemplate(
        userEmail,
        template,
        vars
    );
    
    log.info("自定义模板邮件发送结果: {}", result.isSuccess());
}
```

#### 异步和批量发送

```java
/**
 * 异步发送邮件
 */
public void sendAsyncEmail(String userEmail, String subject, String content) {
    // 方式1：带回调的异步发送
    emailSenderService.sendAsync(
        userEmail,
        subject,
        content,
        result -> {
            if (result.isSuccess()) {
                log.info("异步邮件发送成功: {}", result.getMessageId());
            } else {
                log.error("异步邮件发送失败: {}", result.getErrorMessage());
            }
        }
    );
    
    // 方式2：返回Future的异步发送
    CompletableFuture<Boolean> future = emailSenderStarter.sendAsync(
        userEmail,
        subject,
        content
    );
    
    future.thenAccept(success -> {
        log.info("邮件发送结果: {}", success ? "成功" : "失败");
    }).exceptionally(throwable -> {
        log.error("邮件发送异常", throwable);
        return null;
    });
}

/**
 * 批量发送邮件
 */
public void sendBatchEmails(List<String> recipients, String subject, String content) {
    SendResult batchResult = emailSenderService.sendBatch(
        recipients,
        subject,
        content
    );
    
    log.info("批量发送完成: 成功 {}/{}, 失败 {}",
        batchResult.getSuccessCount(),
        batchResult.getTotalCount(),
        batchResult.getFailureCount()
    );
    
    // 获取失败的邮件地址
    if (batchResult.getFailureCount() > 0) {
        List<String> failedRecipients = batchResult.getFailedRecipients();
        log.warn("发送失败的邮件地址: {}", failedRecipients);
    }
}
```

## 📖 详细配置说明

### 邮件监听配置

```yaml
email:
  listener:
    enabled: true                    # 是否启用邮件监听
    server:
      host: imap.gmail.com          # IMAP服务器地址
      port: 993                     # IMAP端口
      protocol: imaps               # 协议 (imap/imaps)
      username: your-email@gmail.com # 邮箱用户名
      password: your-app-password    # 邮箱密码或应用密码
      folder: INBOX                 # 监听的文件夹
    attachment:
      save-dir: ./attachments       # 附件保存目录
      max-size: 10MB               # 最大附件大小
    polling:
      interval: 30000              # 轮询间隔(毫秒)
      batch-size: 50               # 批处理大小
    thread-pool:
      core-size: 5                 # 核心线程数
      max-size: 20                 # 最大线程数
      queue-capacity: 100          # 队列容量
```

### 邮件发送配置

```yaml
email:
  smtp:
    host: smtp.gmail.com           # SMTP服务器地址
    port: 587                      # SMTP端口
    username: your-email@gmail.com # 发送邮箱
    password: your-app-password    # 邮箱密码
    auth: true                     # 是否需要认证
    starttls: true                 # 是否启用STARTTLS
    ssl: false                     # 是否使用SSL
  retry:
    max-retries: 3                 # 最大重试次数
    retry-delay: 1000              # 重试延迟(毫秒)
    backoff-multiplier: 2.0        # 退避倍数
  sender:
    enabled: true                  # 是否启用发送服务
    batch-threshold: 10            # 批量发送阈值
    default-strategy: default      # 默认发送策略
    thread-pool:
      core-size: 10                # 核心线程数
      max-size: 50                 # 最大线程数
```

### 注解驱动处理器配置

```yaml
annotation-driven-email-processor:
  enabled: true                    # 启用注解驱动处理器
  management:
    enabled: true                  # 启用管理功能
  default:
    priority: 50                   # 默认优先级
    async: false                   # 默认同步执行
  execution:
    timeout: 30000                 # 执行超时时间(毫秒)
  performance:
    monitoring-enabled: true       # 启用性能监控
    logging-enabled: true          # 启用日志记录
  concurrency:
    max-concurrent: 10             # 最大并发处理数
  scan:
    packages:                      # 扫描包路径
      - com.yourcompany.processors
  verification-code:
    smart-extraction: true         # 启用智能验证码提取
    config:
      min-length: 4                # 验证码最小长度
      max-length: 8                # 验证码最大长度
      allow-alphanumeric: true     # 允许字母数字组合
      exclude-timestamp: true      # 排除时间戳
```

## 🎯 最佳实践

### 1. 邮件处理器设计原则

```java
@Component
@EmailProcessor(group = "core", description = "核心业务处理器")
public class CoreEmailProcessor {
    
    // ✅ 好的做法：明确的处理器命名和描述
    @EmailHandler(
        name = "loginVerificationHandler",
        description = "处理登录验证码邮件",
        subjectPattern = "(?i).*登录验证码.*",
        matchType = EmailHandler.MatchType.REGEX,
        priority = 100
    )
    public void handleLoginVerification(EmailContext context) {
        // 处理逻辑
    }
    
    // ✅ 好的做法：使用异步处理耗时操作
    @EmailHandler(
        name = "reportHandler",
        description = "处理报表邮件",
        subjectPattern = ".*报表.*",
        matchType = EmailHandler.MatchType.CONTAINS,
        async = true,  // 异步处理
        priority = 50
    )
    public void handleReport(EmailContext context) {
        // 耗时的报表处理逻辑
    }
}
```

### 2. 错误处理和日志记录

```java
@EmailHandler(name = "robustHandler", subjectPattern = ".*")
public void handleEmailRobustly(EmailContext context) {
    try {
        // 业务逻辑
        processEmail(context);
        
        // 记录成功日志
        log.info("邮件处理成功: subject={}, sender={}", 
                context.getSubject(), context.getSender());
                
    } catch (BusinessException e) {
        // 业务异常处理
        log.warn("业务处理失败: {}", e.getMessage());
        // 可以选择重试或者记录到失败队列
        
    } catch (Exception e) {
        // 系统异常处理
        log.error("邮件处理异常: subject={}", context.getSubject(), e);
        // 发送告警通知
        sendAlert("邮件处理异常", e.getMessage());
    }
}
```

### 3. 性能优化建议

```java
// ✅ 使用异步处理提高吞吐量
@EmailHandler(async = true, priority = 50)
public void handleBulkEmail(EmailContext context) {
    // 批量处理逻辑
}

// ✅ 合理设置优先级
@EmailHandler(priority = 200)  // 高优先级
public void handleUrgentEmail(EmailContext context) {
    // 紧急邮件处理
}

@EmailHandler(priority = 10)   // 低优先级
public void handleBulkEmail(EmailContext context) {
    // 批量邮件处理
}

// ✅ 使用精确匹配提高性能
@EmailHandler(
    subjectPattern = "验证码",
    matchType = EmailHandler.MatchType.EXACT  // 精确匹配比正则表达式快
)
public void handleExactMatch(EmailContext context) {
    // 处理逻辑
}
```

### 4. 邮件发送最佳实践

```java
@Service
public class EmailService {
    
    @Resource
    private EmailSenderService emailSenderService;
    
    /**
     * ✅ 好的做法：使用模板和参数化
     */
    public void sendWelcomeEmail(User user) {
        Map<String, Object> params = new HashMap<>();
        params.put("userName", user.getName());
        params.put("activationLink", generateActivationLink(user));
        
        emailSenderService.sendWithTemplate(
            user.getEmail(),
            "welcome",
            params
        );
    }
    
    /**
     * ✅ 好的做法：批量发送优化
     */
    public void sendBulkNotification(List<User> users, String message) {
        List<String> emails = users.stream()
            .map(User::getEmail)
            .collect(Collectors.toList());
            
        // 分批发送，避免一次性发送过多
        int batchSize = 50;
        for (int i = 0; i < emails.size(); i += batchSize) {
            List<String> batch = emails.subList(
                i, Math.min(i + batchSize, emails.size())
            );
            
            emailSenderService.sendBatch(batch, "通知", message);
        }
    }
    
    /**
     * ✅ 好的做法：异步发送非关键邮件
     */
    public void sendNewsletterAsync(List<String> subscribers, String content) {
        CompletableFuture.runAsync(() -> {
            emailSenderService.sendBatch(subscribers, "Newsletter", content);
        });
    }
}
```

## 🔧 高级功能

### 自定义发送策略

```java
@Component
public class CustomEmailSendStrategy implements EmailSendStrategy {
    
    @Override
    public String getStrategyName() {
        return "custom";
    }
    
    @Override
    public int getPriority() {
        return 150;  // 优先级
    }
    
    @Override
    public boolean canHandle(EmailSendRequest request) {
        // 自定义处理条件
        return request.getRecipients().size() > 100;
    }
    
    @Override
    public SendResult send(EmailSendRequest request) {
        // 自定义发送逻辑
        return customSendLogic(request);
    }
}
```

### 邮件事件监听

```java
@Component
public class EmailEventListener {
    
    @EventListener
    public void handleEmailReceived(EmailReceivedEvent event) {
        log.info("收到新邮件: {}", event.getSubject());
        // 自定义处理逻辑
    }
    
    @EventListener
    public void handleEmailSent(EmailSentEvent event) {
        log.info("邮件发送完成: {}", event.getMessageId());
        // 发送统计或通知
    }
}
```

## 📊 监控和管理

### REST API接口

如果你的项目包含了`spring-boot-starter-web`依赖，EasyMail会自动提供管理API：

```bash
# 获取所有邮件处理器
GET /api/email/processor/handlers

# 获取指定组的处理器
GET /api/email/processor/handlers/group/{groupName}

# 启用/禁用处理器
POST /api/email/processor/handlers/{handlerName}/enable
POST /api/email/processor/handlers/{handlerName}/disable

# 获取处理器统计信息
GET /api/email/processor/statistics

# 获取邮件发送统计
GET /api/email/sender/statistics
```

### 健康检查

```java
@Component
public class EmailHealthIndicator implements HealthIndicator {
    
    @Resource
    private MailListener mailListener;
    
    @Override
    public Health health() {
        if (mailListener.isConnected()) {
            return Health.up()
                .withDetail("status", "connected")
                .withDetail("lastCheck", new Date())
                .build();
        } else {
            return Health.down()
                .withDetail("status", "disconnected")
                .withDetail("error", "Mail server connection failed")
                .build();
        }
    }
}
```

## 🔍 故障排除

### 常见问题

1. **邮件监听不工作**
   - 检查IMAP服务器配置是否正确
   - 确认邮箱密码或应用密码是否有效
   - 检查防火墙和网络连接

2. **邮件发送失败**
   - 验证SMTP服务器配置
   - 检查认证信息是否正确
   - 确认是否需要启用"不够安全的应用访问权限"

3. **处理器不生效**
   - 确认包扫描路径配置正确
   - 检查处理器类是否添加了`@Component`注解
   - 验证匹配模式是否正确

### 调试配置

```yaml
logging:
  level:
    cn.sunyblog.javaemaildemo: DEBUG
    javax.mail: DEBUG
    org.springframework.mail: DEBUG
```

## 📄 许可证

本项目采用 [MIT License](LICENSE) 许可证。

## 🤝 贡献

欢迎提交Issue和Pull Request来帮助改进这个项目！

## 📞 支持

如果你在使用过程中遇到问题，可以通过以下方式获取帮助：

- 提交 [GitHub Issue](https://github.com/sunyblog/easymail/issues)
- 发送邮件至：contact@sunyblog.cn
- 查看 [Wiki文档](https://github.com/sunyblog/easymail/wiki)

---

**EasyMail** - 让邮件处理变得简单高效！ 🚀