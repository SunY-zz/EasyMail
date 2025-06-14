# 企业级邮件发送服务

## 概述

本项目在原有邮件功能基础上，新增了企业级邮件发送服务，提供了更强大、更灵活、更可靠的邮件发送解决方案。

## 核心特性

### 🚀 高级功能
- **多种发送策略**：默认策略、批量策略、高优先级策略
- **智能重试机制**：可配置的重试次数和延迟
- **异步发送支持**：支持异步发送和回调处理
- **模板引擎**：支持变量替换的邮件模板
- **批量发送优化**：并行处理大量邮件发送

### 📊 监控与统计
- **实时监控**：发送成功率、失败率、响应时间
- **详细统计**：按小时统计、趋势分析、健康状态
- **性能指标**：线程池状态、策略使用情况
- **事件追踪**：完整的邮件发送生命周期事件

### 🔧 企业级特性
- **Spring Boot 自动配置**：开箱即用
- **灵活配置**：支持多种配置方式
- **扩展性强**：支持自定义策略和监听器
- **生产就绪**：包含完整的错误处理和日志记录

## 快速开始

### 1. 配置SMTP

在 `application.yml` 中配置SMTP服务器信息：

```yaml
email:
  smtp:
    host: smtp.example.com
    port: 587
    username: your-email@example.com
    password: your-password
    auth: true
    starttls: true
  retry:
    maxRetries: 3
    retryDelay: 1000
  sender:
    enabled: true
    batchThreshold: 10
    defaultStrategy: default
```

### 2. 基本使用

#### 注入服务

```java
@Service
public class YourService {
    
    @Resource
    private EmailSenderService emailSenderService;
    
    @Resource
    private EmailSenderStarter emailSenderStarter;
}
```

#### 发送简单邮件

```java
// 发送文本邮件
SendResult result = emailSenderService.sendText(
    "recipient@example.com", 
    "测试邮件", 
    "这是一封测试邮件");

if (result.isSuccess()) {
    log.info("邮件发送成功，消息ID: {}", result.getMessageId());
} else {
    log.error("邮件发送失败: {}", result.getErrorMessage());
}

// 发送HTML邮件
SendResult htmlResult = emailSenderService.sendHtml(
    "recipient@example.com", 
    "HTML邮件", 
    "<h1>欢迎</h1><p>这是HTML邮件</p>");
```

#### 便捷方法

```java
// 使用便捷方法
boolean success = emailSenderStarter.sendText(
    "recipient@example.com", 
    "测试邮件", 
    "邮件内容");
```

### 3. 模板邮件

#### 使用内置模板

```java
Map<String, Object> variables = new HashMap<>();
variables.put("code", "123456");
variables.put("expireMinutes", 10);

boolean success = emailSenderStarter.sendWithTemplate(
    "user@example.com", 
    "verification", 
    variables);
```

#### 创建自定义模板

```java
// 创建模板
EmailTemplate template = new EmailTemplate(
    "welcome",
    "欢迎加入{{company}}",
    "<h2>欢迎 {{name}}！</h2><p>感谢您加入{{company}}</p>",
    true);

// 注册模板
emailSenderStarter.getTemplateManager().registerTemplate(template);

// 使用模板
Map<String, Object> vars = new HashMap<>();
vars.put("name", "张三");
vars.put("company", "示例公司");

SendResult result = emailSenderService.sendWithTemplate(
    "user@example.com", 
    template, 
    vars);
```

### 4. 异步发送

```java
// 异步发送带回调
emailSenderService.sendAsync(
    "recipient@example.com", 
    "异步邮件", 
    "邮件内容",
    result -> {
        if (result.isSuccess()) {
            log.info("异步邮件发送成功");
        } else {
            log.error("异步邮件发送失败: {}", result.getErrorMessage());
        }
    });

// 便捷的异步发送
CompletableFuture<Boolean> future = emailSenderStarter.sendAsync(
    "recipient@example.com", 
    "异步邮件", 
    "邮件内容");

future.thenAccept(success -> {
    log.info("邮件发送结果: {}", success ? "成功" : "失败");
});
```

### 5. 批量发送

```java
List<String> recipients = Arrays.asList(
    "user1@example.com",
    "user2@example.com",
    "user3@example.com");

SendResult batchResult = emailSenderService.sendBatch(
    recipients, 
    "批量邮件", 
    "这是批量发送的邮件");

log.info("批量发送结果: 成功 {}/{}", 
    batchResult.getSuccessCount(), 
    batchResult.getTotalCount());
```

## 高级功能

### 发送策略

系统内置了三种发送策略：

1. **默认策略** (`DefaultEmailSendStrategy`)
   - 适用于普通邮件发送
   - 包含重试机制
   - 优先级：100

2. **批量策略** (`BatchEmailSendStrategy`)
   - 适用于大量邮件发送（>10封）
   - 并行处理提高效率
   - 优先级：200

3. **高优先级策略** (`HighPriorityEmailSendStrategy`)
   - 适用于紧急邮件
   - 更短的超时时间
   - 优先级：300

策略会根据邮件内容和数量自动选择，也可以手动指定。

### 监控和统计

```java
// 获取发送统计
String stats = emailSenderService.getSendingStats();
log.info("发送统计: {}", stats);

// 获取详细统计信息
Map<String, Object> detailedStats = emailSenderServiceImpl.getDetailedStatistics();

// 获取健康状态
Map<String, Object> health = emailSenderServiceImpl.getHealthStatus();

// 获取发送趋势
Map<String, Object> trend = emailSenderServiceImpl.getSendTrend();

// 生成监控报告
String report = emailSenderServiceImpl.generateMonitorReport();
```

### 事件监听

系统会发布以下事件：
- `EmailSendStartEvent`：邮件发送开始
- `EmailSendCompleteEvent`：邮件发送完成
- `EmailSendFailureEvent`：邮件发送失败
- `EmailSendRetryEvent`：邮件重试
- `BatchSendProgressEvent`：批量发送进度
- `TemplateUsageEvent`：模板使用

可以通过实现 `ApplicationListener` 来监听这些事件。

## 配置说明

### 完整配置示例

```yaml
email:
  smtp:
    host: smtp.example.com
    port: 587
    username: your-email@example.com
    password: your-password
    auth: true
    starttls: true
    connectionTimeout: 10000
    timeout: 10000
  
  retry:
    maxRetries: 3
    retryDelay: 1000
    backoffMultiplier: 2.0
  
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
      defaultPath: "classpath:templates/email/"
      autoLoadDefaults: true
    
    event:
      enabled: true
      async: true
      queueSize: 1000
      threadPoolSize: 2
    
    strategy:
      autoSelect: true
      selectionAlgorithm: performance
      performanceWindowSize: 100
```

### 配置项说明

| 配置项 | 说明 | 默认值 |
|--------|------|--------|
| `email.sender.enabled` | 是否启用邮件发送服务 | `true` |
| `email.sender.batchThreshold` | 批量发送阈值 | `10` |
| `email.sender.defaultStrategy` | 默认发送策略 | `default` |
| `email.sender.monitor.enabled` | 是否启用监控 | `true` |
| `email.sender.template.enabled` | 是否启用模板功能 | `true` |
| `email.sender.event.enabled` | 是否启用事件发布 | `true` |

## 内置模板

系统提供了以下内置模板：

1. **welcome** - 欢迎邮件模板
   - 变量：`{{username}}`

2. **verification** - 验证码邮件模板
   - 变量：`{{code}}`, `{{expireMinutes}}`

3. **password-reset** - 密码重置模板
   - 变量：`{{resetLink}}`

4. **notification** - 通知邮件模板
   - 变量：`{{title}}`, `{{message}}`, `{{timestamp}}`

## 最佳实践

### 1. 错误处理

```java
SendResult result = emailSenderService.sendText(to, subject, content);
if (!result.isSuccess()) {
    // 记录错误日志
    log.error("邮件发送失败: {}, 错误码: {}, 重试次数: {}", 
        result.getErrorMessage(), 
        result.getErrorCode(), 
        result.getRetryCount());
    
    // 根据错误类型进行处理
    if (result.getErrorCode() == 550) {
        // 邮箱地址无效
        handleInvalidEmail(to);
    } else if (result.getErrorCode() == 421) {
        // 服务器繁忙，稍后重试
        scheduleRetry(to, subject, content);
    }
}
```

### 2. 性能优化

```java
// 对于大量邮件，使用批量发送
if (recipients.size() > 10) {
    SendResult result = emailSenderService.sendBatch(recipients, subject, content);
} else {
    // 少量邮件使用普通发送
    for (String recipient : recipients) {
        emailSenderService.sendText(recipient, subject, content);
    }
}
```

### 3. 监控告警

```java
// 定期检查健康状态
@Scheduled(fixedRate = 300000) // 每5分钟检查一次
public void checkEmailServiceHealth() {
    Map<String, Object> health = emailSenderServiceImpl.getHealthStatus();
    boolean isHealthy = (Boolean) health.get("healthy");
    
    if (!isHealthy) {
        // 发送告警
        alertService.sendAlert("邮件服务健康状态异常: " + health);
    }
}
```

## 故障排除

### 常见问题

1. **邮件发送失败**
   - 检查SMTP配置是否正确
   - 确认网络连接正常
   - 查看错误日志获取详细信息

2. **发送速度慢**
   - 调整线程池大小
   - 使用批量发送策略
   - 检查SMTP服务器性能

3. **模板不生效**
   - 确认模板已正确注册
   - 检查变量名称是否匹配
   - 验证模板语法

### 调试技巧

```java
// 启用详细日志
logging:
  level:
    cn.sunyblog.javaemaildemo: DEBUG

// 获取详细的发送结果
SendResult result = emailSenderService.sendText(to, subject, content);
log.debug("发送结果详情: {}", result.toString());

// 检查连接状态
boolean connected = emailSenderService.checkConnection();
log.debug("SMTP连接状态: {}", connected);
```

## 扩展开发

### 自定义发送策略

```java
@Component
public class CustomEmailSendStrategy implements EmailSendStrategy {
    
    @Override
    public SendResult send(List<String> toList, List<String> ccList, 
                          List<String> bccList, String subject, 
                          String content, boolean isHtml, 
                          List<File> attachments) {
        // 实现自定义发送逻辑
        return null;
    }
    
    @Override
    public String getStrategyName() {
        return "custom";
    }
    
    @Override
    public int getPriority() {
        return 400;
    }
    
    @Override
    public boolean supports(List<String> toList, String subject, String content) {
        // 定义策略适用条件
        return subject.contains("custom");
    }
}
```

### 自定义事件监听器

```java
@Component
public class CustomEmailEventListener {
    
    @EventListener
    public void handleEmailSendStart(EmailSendEventListener.EmailSendStartEvent event) {
        log.info("邮件开始发送: {}", event.getSubject());
    }
    
    @EventListener
    public void handleEmailSendComplete(EmailSendEventListener.EmailSendCompleteEvent event) {
        log.info("邮件发送完成: {}", event.getResult().isSuccess());
    }
}
```

## 版本历史

- **v1.0.0** - 初始版本，包含基本邮件发送功能
- **v2.0.0** - 新增企业级邮件发送服务
  - 多策略支持
  - 模板引擎
  - 监控统计
  - 事件系统
  - Spring Boot 自动配置

## 许可证

本项目采用 MIT 许可证。

## 贡献

欢迎提交 Issue 和 Pull Request 来改进这个项目。

## 联系方式

如有问题，请联系：sunyblog@example.com