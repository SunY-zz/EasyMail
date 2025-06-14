# EasyMail 注解驱动邮件处理器

## 📖 概述

注解驱动邮件处理器是EasyMail SDK的核心功能之一，它提供了一种简单而强大的方式来处理不同类型的邮件。通过使用`@EmailProcessor`和`@EmailHandler`注解，开发者可以轻松定义邮件处理逻辑，类似于Spring MVC的控制器模式。

## ✨ 核心特性

- 🎯 **注解驱动**: 使用简单的注解定义邮件处理逻辑
- 🔍 **智能匹配**: 支持精确匹配、包含匹配、正则表达式匹配
- ⚡ **异步处理**: 支持同步和异步处理模式
- 🎚️ **优先级控制**: 支持处理器优先级设置
- 📊 **监控统计**: 提供详细的处理统计信息
- 🔧 **动态管理**: 支持运行时启用/禁用处理器
- 🧠 **智能验证码提取**: 优化的验证码识别算法
- 🌐 **REST API**: 提供完整的管理和监控API

## 🚀 快速开始

### 1. 启用注解驱动邮件处理器

在`application.yml`中添加配置：

```yaml
annotation-driven-email-processor:
  enabled: true                    # 启用处理器
  management:
    enabled: true                  # 启用管理功能
  scan:
    packages:                      # 扫描包路径
      - com.yourcompany.email.processors
```

### 2. 创建邮件处理器

```java
@Component
@EmailProcessor(
    group = "business",           // 处理器组名
    description = "业务邮件处理器",  // 描述信息
    enabled = true               // 是否启用
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
    
    private String extractVerificationCode(String content) {
        // 使用内置的智能验证码提取功能
        return VerificationCodeExtractor.extract(content);
    }
    
    private void processVerificationCode(String code, EmailContext context) {
        // 实现验证码处理逻辑
        // 例如：存储到Redis、发送到消息队列等
    }
    
    private void processOrderAsync(EmailContext context) {
        // 实现订单处理逻辑
        // 例如：解析订单信息、更新数据库等
    }
}
```

## 📋 注解详解

### @EmailProcessor

用于标记包含邮件处理方法的类，必须与`@Component`一起使用。

```java
@EmailProcessor(
    group = "business",           // 处理器组名，用于分类管理
    description = "业务处理器",    // 描述信息，用于监控和管理
    enabled = true,              // 是否启用，默认为true
    beanName = "customName"      // 自定义Bean名称（可选）
)
```

**参数说明：**

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| `group` | String | 否 | "default" | 处理器组名，用于分类管理 |
| `description` | String | 否 | "" | 处理器描述信息 |
| `enabled` | boolean | 否 | true | 是否启用处理器 |
| `beanName` | String | 否 | "" | 自定义Bean名称 |

### @EmailHandler

用于标记具体的邮件处理方法，支持多种匹配方式和配置选项。

```java
@EmailHandler(
    name = "handlerName",                    // 处理器名称（必须唯一）
    description = "处理器描述",               // 描述信息
    subjectPattern = ".*验证码.*",           // 主题匹配模式
    senderPattern = ".*@company.com",       // 发件人匹配模式
    tagPattern = "urgent",                  // 标签匹配模式
    matchType = EmailHandler.MatchType.REGEX, // 匹配类型
    priority = 100,                         // 优先级（数值越大优先级越高）
    async = false,                          // 是否异步执行
    enabled = true                          // 是否启用
)
```

**参数说明：**

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| `name` | String | 是 | - | 处理器名称，必须唯一 |
| `description` | String | 否 | "" | 处理器描述信息 |
| `subjectPattern` | String | 否 | "" | 主题匹配模式 |
| `senderPattern` | String | 否 | "" | 发件人匹配模式 |
| `tagPattern` | String | 否 | "" | 标签匹配模式 |
| `matchType` | MatchType | 否 | CONTAINS | 匹配类型 |
| `priority` | int | 否 | 50 | 优先级，数值越大优先级越高 |
| `async` | boolean | 否 | false | 是否异步执行 |
| `enabled` | boolean | 否 | true | 是否启用 |

## 🎯 匹配类型详解

### EXACT - 精确匹配

完全匹配指定的字符串，性能最高。

```java
@EmailHandler(
    name = "exactHandler",
    subjectPattern = "验证码",
    matchType = EmailHandler.MatchType.EXACT
)
public void handleExactMatch(EmailContext context) {
    // 只有主题完全等于"验证码"的邮件才会被处理
}
```

**适用场景：**
- 已知确切的邮件主题
- 需要最高性能的匹配
- 简单的字符串匹配

### CONTAINS - 包含匹配

检查字符串是否包含指定的子字符串，性能较好。

```java
@EmailHandler(
    name = "containsHandler",
    subjectPattern = "验证码",
    matchType = EmailHandler.MatchType.CONTAINS
)
public void handleContainsMatch(EmailContext context) {
    // 主题包含"验证码"的邮件都会被处理
    // 例如："您的验证码"、"登录验证码"、"验证码已发送"等
}
```

**适用场景：**
- 主题包含特定关键词的邮件
- 不需要复杂的模式匹配
- 平衡性能和灵活性

### REGEX - 正则表达式匹配

使用正则表达式进行复杂的模式匹配，功能最强大但性能相对较低。

```java
@EmailHandler(
    name = "regexHandler",
    subjectPattern = "(?i).*(验证码|verification|code).*",
    matchType = EmailHandler.MatchType.REGEX
)
public void handleRegexMatch(EmailContext context) {
    // 支持复杂的匹配模式
    // (?i) 表示忽略大小写
    // .* 表示任意字符
    // (验证码|verification|code) 表示匹配其中任一词汇
}
```

**常用正则表达式示例：**

```java
// 忽略大小写匹配
"(?i).*verification.*"

// 匹配多个关键词
"(?i).*(验证码|code|verification).*"

// 匹配特定格式
"订单号：\\d{10,}"

// 匹配邮箱地址
".*@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}.*"

// 匹配数字验证码
"验证码[：:\\s]*(\\d{4,6})"
```

**适用场景：**
- 需要复杂的模式匹配
- 支持多语言或多种格式
- 需要提取特定信息

## 🔧 方法参数支持

邮件处理方法支持多种参数类型，系统会自动注入相应的值：

### EmailContext 参数（推荐）

```java
@EmailHandler(name = "contextHandler")
public void handleWithContext(EmailContext context) {
    // 获取邮件信息
    String subject = context.getSubject();
    String sender = context.getSender();
    String content = context.getContent();
    Message message = context.getMessage();
    
    // 获取附件信息
    List<AttachmentInfo> attachments = context.getAttachments();
    
    // 获取接收时间
    Date receivedDate = context.getReceivedDate();
}
```

### 传统参数方式

```java
@EmailHandler(name = "traditionalHandler")
public void handleTraditional(Message message, String subject, String sender, String content) {
    // 使用传统的参数方式
    log.info("收到邮件: {} from {}", subject, sender);
}
```

### 混合参数方式

```java
@EmailHandler(name = "mixedHandler")
public void handleMixed(EmailContext context, String subject) {
    // 可以混合使用不同类型的参数
    log.info("处理邮件: {}", subject);
    // 使用context获取更多信息
}
```

## ⚙️ 完整配置选项

```yaml
annotation-driven-email-processor:
  enabled: true                    # 启用处理器
  management:
    enabled: true                  # 启用管理功能
  default:
    priority: 50                   # 默认优先级
    async: false                   # 默认同步执行
  execution:
    timeout: 30000                 # 执行超时时间（毫秒）
  performance:
    monitoring-enabled: true       # 启用性能监控
    logging-enabled: true          # 启用日志记录
  concurrency:
    max-concurrent: 10             # 最大并发处理数
  scan:
    packages:                      # 扫描包路径
      - com.yourcompany.processors
      - com.yourcompany.handlers
  verification-code:
    smart-extraction: true         # 启用智能验证码提取
    config:
      min-length: 4                # 最小长度
      max-length: 8                # 最大长度
      allow-alphanumeric: true     # 允许字母数字组合
      exclude-timestamp: true      # 排除时间戳
      custom-patterns:             # 自定义模式
        - "(?i)(?:验证码|code)[：:：\\s]*([A-Z0-9]{4,8})"
        - "(?i)(?:pin|密码)[：:：\\s]*([0-9]{4,6})"
      exclude-keywords:            # 排除关键词
        - "年"
        - "月"
        - "日"
        - "GMT"
        - "UTC"
```

**配置参数详解：**

| 配置项 | 类型 | 默认值 | 说明 |
|--------|------|--------|------|
| `enabled` | boolean | true | 是否启用注解驱动处理器 |
| `management.enabled` | boolean | true | 是否启用管理功能 |
| `default.priority` | int | 50 | 默认优先级 |
| `default.async` | boolean | false | 默认是否异步执行 |
| `execution.timeout` | long | 30000 | 执行超时时间（毫秒） |
| `performance.monitoring-enabled` | boolean | true | 是否启用性能监控 |
| `performance.logging-enabled` | boolean | true | 是否启用日志记录 |
| `concurrency.max-concurrent` | int | 10 | 最大并发处理数 |
| `scan.packages` | List<String> | [] | 扫描包路径 |

## 🎯 最佳实践

### 1. 处理器设计原则

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
    
    // ❌ 不好的做法：模糊的命名和描述
    @EmailHandler(name = "handler1", description = "处理邮件")
    public void badHandler(EmailContext context) {
        // 不清楚具体处理什么类型的邮件
    }
}
```

### 2. 优先级设置策略

```java
// 高优先级：紧急或重要邮件
@EmailHandler(priority = 200, name = "urgentHandler")
public void handleUrgentEmail(EmailContext context) {
    // 处理紧急邮件
}

// 中等优先级：业务邮件
@EmailHandler(priority = 100, name = "businessHandler")
public void handleBusinessEmail(EmailContext context) {
    // 处理业务邮件
}

// 低优先级：通知邮件
@EmailHandler(priority = 50, name = "notificationHandler")
public void handleNotification(EmailContext context) {
    // 处理通知邮件
}

// 最低优先级：垃圾邮件或其他
@EmailHandler(priority = 10, name = "otherHandler")
public void handleOther(EmailContext context) {
    // 处理其他邮件
}
```

### 3. 错误处理和日志记录

```java
@EmailHandler(name = "robustHandler", subjectPattern = ".*")
public void handleEmailRobustly(EmailContext context) {
    try {
        // 业务逻辑
        processEmail(context);
        
        // 记录成功日志
        log.info("邮件处理成功: subject={}, sender={}, messageId={}", 
                context.getSubject(), context.getSender(), context.getMessageId());
                
    } catch (BusinessException e) {
        // 业务异常处理
        log.warn("业务处理失败: subject={}, error={}", 
                context.getSubject(), e.getMessage());
        // 可以选择重试或者记录到失败队列
        handleBusinessError(context, e);
        
    } catch (Exception e) {
        // 系统异常处理
        log.error("邮件处理异常: subject={}, messageId={}", 
                context.getSubject(), context.getMessageId(), e);
        // 发送告警通知
        sendAlert("邮件处理异常", context, e);
    }
}

private void handleBusinessError(EmailContext context, BusinessException e) {
    // 记录到失败队列或重试机制
}

private void sendAlert(String title, EmailContext context, Exception e) {
    // 发送告警通知给运维人员
}
```

### 4. 性能优化建议

```java
// ✅ 使用精确匹配提高性能
@EmailHandler(
    name = "exactMatchHandler",
    subjectPattern = "验证码",
    matchType = EmailHandler.MatchType.EXACT  // 精确匹配比正则表达式快
)
public void handleExactMatch(EmailContext context) {
    // 处理逻辑
}

// ✅ 合理使用异步处理
@EmailHandler(
    name = "asyncHandler",
    async = true,  // 异步处理耗时操作
    priority = 50
)
public void handleAsyncEmail(EmailContext context) {
    // 耗时的处理逻辑
    processLongRunningTask(context);
}

// ✅ 避免在处理器中进行阻塞操作
@EmailHandler(name = "nonBlockingHandler")
public void handleNonBlocking(EmailContext context) {
    // ❌ 避免这样做
    // Thread.sleep(1000);
    // synchronousHttpCall();
    
    // ✅ 推荐这样做
    CompletableFuture.runAsync(() -> {
        // 异步执行耗时操作
        processInBackground(context);
    });
}
```

### 5. 智能验证码提取

```java
@EmailHandler(
    name = "smartCodeHandler",
    subjectPattern = "(?i).*(验证码|code|verification).*",
    matchType = EmailHandler.MatchType.REGEX
)
public void handleVerificationCode(EmailContext context) {
    // 使用内置的智能验证码提取
    String code = VerificationCodeExtractor.extract(context.getContent());
    
    if (code != null) {
        log.info("提取到验证码: {}", code);
        
        // 验证码处理逻辑
        storeVerificationCode(context.getSender(), code);
        notifyCodeReceived(context.getSender(), code);
    } else {
        log.warn("未能提取验证码: subject={}", context.getSubject());
    }
}

// 自定义验证码提取逻辑
@EmailHandler(name = "customCodeHandler")
public void handleCustomCode(EmailContext context) {
    String content = context.getContent();
    
    // 自定义提取逻辑
    Pattern pattern = Pattern.compile("验证码[：:\\s]*(\\d{6})");
    Matcher matcher = pattern.matcher(content);
    
    if (matcher.find()) {
        String code = matcher.group(1);
        log.info("自定义提取验证码: {}", code);
        processCustomCode(code, context);
    }
}
```

## 🌐 REST API 管理接口

当项目包含`spring-boot-starter-web`依赖时，系统会自动提供REST API接口：

### 获取所有处理器

```http
GET /api/email/processor/handlers
```

**响应示例：**
```json
{
  "success": true,
  "data": [
    {
      "name": "verificationCodeHandler",
      "description": "处理验证码邮件",
      "group": "business",
      "priority": 100,
      "async": false,
      "enabled": true,
      "matchType": "REGEX",
      "subjectPattern": "(?i).*(验证码|verification|code).*",
      "statistics": {
        "processedCount": 156,
        "successCount": 154,
        "failureCount": 2,
        "averageProcessingTime": 45
      }
    }
  ]
}
```

### 获取指定组的处理器

```http
GET /api/email/processor/handlers/group/{groupName}
```

### 启用/禁用处理器

```http
POST /api/email/processor/handlers/{handlerName}/enable
POST /api/email/processor/handlers/{handlerName}/disable
```

### 获取处理器统计信息

```http
GET /api/email/processor/statistics
```

**响应示例：**
```json
{
  "success": true,
  "data": {
    "totalHandlers": 5,
    "enabledHandlers": 4,
    "totalProcessed": 1250,
    "totalSuccess": 1235,
    "totalFailure": 15,
    "averageProcessingTime": 67,
    "processingRate": 98.8
  }
}
```

### 获取处理器详细信息

```http
GET /api/email/processor/handlers/{handlerName}
```

## 📊 监控和统计

### 性能监控

系统自动收集以下性能指标：

- **处理次数**：总处理次数、成功次数、失败次数
- **处理时间**：平均处理时间、最大处理时间、最小处理时间
- **处理速率**：每分钟处理数量、成功率
- **并发情况**：当前并发数、最大并发数

### 日志记录

```yaml
# 启用详细日志
logging:
  level:
    cn.sunyblog.javaemaildemo.processor: DEBUG
    cn.sunyblog.javaemaildemo.annotation: DEBUG
```

### 自定义监控

```java
@Component
public class EmailProcessorMonitor {
    
    @EventListener
    public void onEmailProcessed(EmailProcessedEvent event) {
        // 自定义监控逻辑
        log.info("邮件处理完成: handler={}, success={}, duration={}ms",
                event.getHandlerName(), event.isSuccess(), event.getDuration());
        
        // 发送到监控系统
        sendToMonitoringSystem(event);
    }
    
    @EventListener
    public void onEmailProcessingFailed(EmailProcessingFailedEvent event) {
        // 处理失败事件
        log.error("邮件处理失败: handler={}, error={}",
                event.getHandlerName(), event.getError());
        
        // 发送告警
        sendAlert(event);
    }
}
```

## 🔍 故障排除

### 常见问题

1. **处理器不生效**
   ```yaml
   # 检查包扫描配置
   annotation-driven-email-processor:
     scan:
       packages:
         - com.yourcompany.processors  # 确保包路径正确
   ```

2. **匹配模式不工作**
   ```java
   // 检查正则表达式语法
   @EmailHandler(
       subjectPattern = "(?i).*(验证码|code).*",  // 确保正则表达式正确
       matchType = EmailHandler.MatchType.REGEX
   )
   ```

3. **性能问题**
   ```yaml
   # 调整并发配置
   annotation-driven-email-processor:
     concurrency:
       max-concurrent: 20  # 增加并发数
   ```

### 调试配置

```yaml
logging:
  level:
    cn.sunyblog.javaemaildemo: DEBUG
    root: INFO

annotation-driven-email-processor:
  performance:
    logging-enabled: true  # 启用性能日志
    monitoring-enabled: true  # 启用监控
```

## 🚀 高级用法

### 条件处理器

```java
@Component
@EmailProcessor(group = "conditional")
@ConditionalOnProperty(name = "email.processor.advanced.enabled", havingValue = "true")
public class ConditionalEmailProcessor {
    
    @EmailHandler(name = "conditionalHandler")
    public void handleConditional(EmailContext context) {
        // 只有在配置启用时才会注册此处理器
    }
}
```

### 动态处理器注册

```java
@Service
public class DynamicProcessorService {
    
    @Autowired
    private AnnotationDrivenEmailProcessorManager processorManager;
    
    public void registerDynamicHandler() {
        // 动态注册处理器
        EmailHandlerInfo handlerInfo = EmailHandlerInfo.builder()
            .name("dynamicHandler")
            .description("动态注册的处理器")
            .subjectPattern(".*dynamic.*")
            .matchType(EmailHandler.MatchType.CONTAINS)
            .priority(80)
            .async(false)
            .enabled(true)
            .build();
            
        processorManager.registerHandler(handlerInfo, this::handleDynamic);
    }
    
    private void handleDynamic(EmailContext context) {
        log.info("动态处理器处理邮件: {}", context.getSubject());
    }
}
```

---

通过以上详细的文档和示例，你可以充分利用EasyMail的注解驱动邮件处理器功能，构建强大而灵活的邮件处理系统。如果有任何问题，请参考主文档或联系技术支持。