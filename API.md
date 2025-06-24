# EasyMail API 文档

## 目录

- [核心 API 接口](#核心-api-接口)
- [邮件发送服务](#邮件发送服务)
- [邮件监听服务](#邮件监听服务)
- [配置属性](#配置属性)
- [最佳实践](#最佳实践)
- [常见问题](#常见问题)

## 核心 API 接口

### EasyMailSenderService

应用级邮件发送服务的核心接口，提供统一的邮件发送 API。

#### 主要方法

##### 1. 基础发送方法

```java
// 使用 EasyMailRequest 发送邮件（推荐）
EasyMailSendResult send(EasyMailRequest request);

// 异步发送邮件
CompletableFuture<EasyMailSendResult> sendAsync(EasyMailRequest request);
```

**示例：**

```java
@Service
public class EmailService {
    
    @Resource
    private EasyMailSenderService easyMailSenderService;
    
    public void sendSimpleEmail() {
        EasyMailRequest request = EasyMailRequest.builder()
            .to("recipient@example.com")
            .subject("测试邮件")
            .text("这是邮件内容")
            .build();
            
        EasyMailSendResult result = easyMailSenderService.send(request);
        
        if (result.isSuccess()) {
            System.out.println("发送成功，消息ID：" + result.getMessageId());
        } else {
            System.out.println("发送失败：" + result.getErrorMessage());
        }
    }
}
```

##### 2. 便捷发送方法

```java
// 发送文本邮件
default EasyMailSendResult sendText(String to, String subject, String content);

// 发送 HTML 邮件
default EasyMailSendResult sendHtml(String to, String subject, String htmlContent);

// 异步发送文本邮件
default CompletableFuture<EasyMailSendResult> sendTextAsync(String to, String subject, String content);

// 异步发送 HTML 邮件
default CompletableFuture<EasyMailSendResult> sendHtmlAsync(String to, String subject, String htmlContent);
```

**示例：**

```java
public void sendQuickEmail() {
    // 快速发送文本邮件
    EasyMailSendResult result = easyMailSenderService.sendText(
        "user@example.com", 
        "欢迎注册", 
        "感谢您注册我们的服务！"
    );
    
    // 快速发送 HTML 邮件
    String htmlContent = "<h1>欢迎</h1><p>感谢您的注册！</p>";
    easyMailSenderService.sendHtml(
        "user@example.com", 
        "欢迎注册", 
        htmlContent
    );
}
```

##### 3. 模板邮件方法

```java
// 使用模板发送邮件
EasyMailSendResult sendWithTemplate(String to, EasyMailSendTemplate template, Map<String, Object> variables);

// 批量使用模板发送邮件
EasyMailSendResult sendBatchWithTemplate(Map<String, Map<String, Object>> recipients, EasyMailSendTemplate template);
```

**示例：**

```java
public void sendTemplateEmail() {
    // 创建模板
    EasyMailSendTemplate template = EasyMailSendTemplate.builder()
        .templateId("welcome")
        .subjectTemplate("欢迎 ${username} 加入我们！")
        .contentTemplate("<h1>欢迎 ${username}！</h1><p>您的账号：${account}</p>")
        .isHtml(true)
        .build();
    
    // 准备变量
    Map<String, Object> variables = new HashMap<>();
    variables.put("username", "张三");
    variables.put("account", "zhangsan@example.com");
    
    // 发送模板邮件
    EasyMailSendResult result = easyMailSenderService.sendWithTemplate(
        "zhangsan@example.com", 
        template, 
        variables
    );
}
```

### EasyMailRequest

邮件请求构建器，提供链式调用的友好 API。

#### 构建方法

```java
// 基础构建器
EasyMailRequest.builder()
    .to("recipient@example.com")           // 收件人
    .toList(Arrays.asList("a@x.com", "b@x.com"))  // 多个收件人
    .cc("cc@example.com")                  // 抄送
    .bcc("bcc@example.com")                // 密送
    .subject("邮件主题")                    // 主题
    .text("文本内容")                       // 文本内容
    .html("<h1>HTML内容</h1>")              // HTML内容
    .attachment(new File("/path/to/file")) // 附件
    .priority(1)                           // 优先级（1-5）
    .async(true)                           // 是否异步
    .build();

// 快速构建方法
EasyMailRequest.simpleText("to@example.com", "主题", "内容");
EasyMailRequest.htmlEmail("to@example.com", "主题", "<h1>HTML内容</h1>");
```

#### 完整示例

```java
public void sendComplexEmail() {
    EasyMailRequest request = EasyMailRequest.builder()
        .to("primary@example.com")
        .cc("manager@example.com")
        .bcc("admin@example.com")
        .subject("项目报告 - " + LocalDate.now())
        .html("<h2>项目进度报告</h2><p>详情请查看附件。</p>")
        .attachment(new File("/reports/project-report.pdf"))
        .attachment(new File("/reports/charts.xlsx"))
        .priority(2)  // 高优先级
        .build();
        
    // 异步发送
    CompletableFuture<EasyMailSendResult> future = easyMailSenderService.sendAsync(request);
    
    future.thenAccept(result -> {
        if (result.isSuccess()) {
            System.out.println("报告邮件发送成功");
        } else {
            System.err.println("发送失败：" + result.getErrorMessage());
        }
    });
}
```
##### 4. 定时、延时发送单条、多条邮件
```java
public void sendScheduledMail() {
        EasyMailRequest request = EasyMailRequest.builder()
        .to("recipient@example.com")
        .subject("每日报告")
        .text("这是每日报告邮件")
        .build();

        String taskId = easyMailSenderService.sendScheduled(request, "0 0 9 * * ?", "每日报告任务");
}
```
详细使用参考[定时发送邮件](SCHEDULE_FEATURE.md)

### EasyMailListenerApi

邮件监听器接口，用于处理接收到的邮件。

```java
public interface EasyMailListenerApi {
    
    /**
     * 处理接收到的邮件
     * @param message 原始邮件消息
     * @param content 解析后的邮件内容
     * @param subject 邮件主题
     * @param from 发件人
     * @return 处理结果，true表示处理成功
     */
    boolean processEmail(Message message, String content, String subject, String from);
    
    /**
     * 获取处理器名称
     */
    default String getProcessorName() {
        return this.getClass().getSimpleName();
    }
}
```

### 注解驱动邮件处理器（推荐）

#### @EasyMailProcessor

用于标记邮件处理器类的注解。

```java
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Component
public @interface EasyMailProcessor {

    /**
     * 处理器组名称
     * 用于分组管理多个处理器
     */
    String group() default "default";

    /**
     * 处理器描述
     */
    String description() default "";

    /**
     * 是否启用该处理器
     */
    boolean enabled() default true;

    /**
     * Spring Bean名称
     */
    String value() default "";
}
```

#### @EasyMailProcessorHandler

用于标记邮件处理方法的注解。

```java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface EasyMailProcessorHandler {
    
    /**
     * 主题匹配模式
     */
    String[] subject() default {};
    
    /**
     * 主题匹配类型
     */
    MatchType subjectMatchType() default MatchType.EXACT;
    
    /**
     * 发件人匹配模式
     */
    String[] from() default {};
    
    /**
     * 发件人匹配类型
     */
    MatchType fromMatchType() default MatchType.EXACT;
    
    /**
     * 标签匹配模式
     */
    String[] tags() default {};
    
    /**
     * 标签匹配类型
     */
    MatchType tagsMatchType() default MatchType.EXACT;
    
    /**
     * 处理器优先级（数值越小优先级越高）
     */
    int priority() default 0;
    
    /**
     * 处理器名称
     */
    String name() default "";
    
    /**
     * 是否异步处理
     */
    boolean async() default false;
    
    /**
     * 处理器描述
     */
    String description() default "";
    
    /**
     * 匹配类型枚举
     */
    enum MatchType {
        EXACT,      // 精确匹配
        CONTAINS,   // 包含匹配
        REGEX,      // 正则表达式匹配
        PREFIX,     // 前缀匹配
        SUFFIX      // 后缀匹配
    }
}
```

#### EasyMailContext

邮件处理上下文，封装了邮件处理过程中的所有相关信息。

```java
public class EasyMailContext {
    
    /**
     * 获取原始邮件对象
     */
    public Message getMessage();
    
    /**
     * 获取邮件ID
     */
    public String getMessageId();
    
    /**
     * 获取邮件主题
     */
    public String getSubject();
    
    /**
     * 获取发件人
     */
    public String getFrom();
    
    /**
     * 获取收件人列表
     */
    public List<String> getTo();
    
    /**
     * 获取抄送列表
     */
    public List<String> getCc();
    
    /**
     * 获取密送列表
     */
    public List<String> getBcc();
    
    /**
     * 获取文本内容
     */
    public String getTextContent();
    
    /**
     * 获取HTML内容
     */
    public String getHtmlContent();
    
    /**
     * 获取标签列表
     */
    public List<String> getTags();
    
    /**
     * 获取附件信息
     */
    public List<AttachmentInfo> getAttachments();
    
    /**
     * 获取接收时间
     */
    public Date getReceivedDate();
    
    /**
     * 获取发送时间
     */
    public Date getSentDate();
}
```

#### 使用示例

```java
@EasyMailProcessor(group = "business", description = "业务邮件处理器")
public class BusinessEmailProcessor {
    
    @EasyMailProcessorHandler(
        subject = {"订单", "order"},
        subjectMatchType = EasyMailProcessorHandler.MatchType.CONTAINS,
        priority = 1,
        async = true,
        description = "处理订单相关邮件"
    )
    public void handleOrderEmail(EasyMailContext context) {
        String subject = context.getSubject();
        String content = context.getTextContent();
        
        // 提取订单号
        String orderNumber = extractOrderNumber(content);
        if (orderNumber != null) {
            // 处理订单逻辑
            processOrder(orderNumber);
        }
    }
    
    @EasyMailProcessorHandler(
        from = {".*@bank\\.com$"},
        fromMatchType = EasyMailProcessorHandler.MatchType.REGEX,
        priority = 2,
        description = "处理银行邮件"
    )
    public void handleBankEmail(EasyMailContext context) {
        // 处理银行邮件
        String from = context.getFrom();
        String content = context.getTextContent();
        
        // 银行邮件处理逻辑
        processBankNotification(from, content);
    }
    
    @EasyMailProcessorHandler(
        tags = {"urgent", "important"},
        tagsMatchType = EasyMailProcessorHandler.MatchType.CONTAINS,
        priority = 0,
        description = "处理紧急邮件"
    )
    public void handleUrgentEmail(EasyMailContext context) {
        // 处理紧急邮件
        sendAlert(context.getSubject(), context.getFrom());
    }
    
    private String extractOrderNumber(String content) {
        // 订单号提取逻辑
        return null;
    }
    
    private void processOrder(String orderNumber) {
        // 订单处理逻辑
    }
    
    private void processBankNotification(String from, String content) {
        // 银行通知处理逻辑
    }
    
    private void sendAlert(String subject, String from) {
        // 发送警报逻辑
    }
}
```

#### 实现示例

```java
@Component
public class VerificationCodeProcessor implements EasyMailListenerApi {
    
    @Override
    public boolean processEmail(Message message, String content, String subject, String from) {
        // 检查是否是验证码邮件
        if (subject.contains("验证码") || subject.contains("verification")) {
            String code = extractVerificationCode(content);
            if (code != null) {
                // 存储验证码到缓存或数据库
                saveVerificationCode(from, code);
                return true;
            }
        }
        return false;
    }
    
    private String extractVerificationCode(String content) {
        // 使用正则表达式提取验证码
        Pattern pattern = Pattern.compile("验证码[：:]?\\s*(\\d{4,6})");
        Matcher matcher = pattern.matcher(content);
        return matcher.find() ? matcher.group(1) : null;
    }
    
    private void saveVerificationCode(String email, String code) {
        // 实现验证码存储逻辑
        System.out.println("保存验证码：" + email + " -> " + code);
    }
}
```

## 邮件发送服务

### 发送结果处理

```java
public class EasyMailSendResult {
    private boolean success;           // 是否成功
    private String messageId;          // 消息ID
    private String errorMessage;       // 错误信息
    private long sendTime;            // 发送时间
    private int recipientCount;       // 收件人数量
    private List<String> failedRecipients; // 失败的收件人
}
```

#### 处理发送结果

```java
public void handleSendResult(EasyMailSendResult result) {
    if (result.isSuccess()) {
        System.out.println("邮件发送成功");
        System.out.println("消息ID: " + result.getMessageId());
        System.out.println("发送时间: " + new Date(result.getSendTime()));
        System.out.println("收件人数量: " + result.getRecipientCount());
    } else {
        System.err.println("邮件发送失败: " + result.getErrorMessage());
        
        // 处理部分失败的情况
        if (result.getFailedRecipients() != null && !result.getFailedRecipients().isEmpty()) {
            System.err.println("失败的收件人: " + result.getFailedRecipients());
        }
    }
}
```

### 批量发送

```java
public void sendBatchEmails() {
    List<String> recipients = Arrays.asList(
        "user1@example.com",
        "user2@example.com",
        "user3@example.com"
    );
    
    EasyMailRequest request = EasyMailRequest.builder()
        .toList(recipients)
        .subject("批量通知")
        .text("这是一条批量通知消息")
        .build();
        
    EasyMailSendResult result = easyMailSenderService.send(request);
    
    System.out.println("批量发送结果: " + result.isSuccess());
    System.out.println("成功发送: " + (result.getRecipientCount() - result.getFailedRecipients().size()));
    System.out.println("失败发送: " + result.getFailedRecipients().size());
}
```

### 异步发送处理

```java
public void sendAsyncWithCallback() {
    EasyMailRequest request = EasyMailRequest.builder()
        .to("user@example.com")
        .subject("异步邮件")
        .text("这是异步发送的邮件")
        .build();
        
    CompletableFuture<EasyMailSendResult> future = easyMailSenderService.sendAsync(request);
    
    // 设置回调
    future.thenAccept(result -> {
        if (result.isSuccess()) {
            System.out.println("异步发送成功: " + result.getMessageId());
        } else {
            System.err.println("异步发送失败: " + result.getErrorMessage());
        }
    }).exceptionally(throwable -> {
        System.err.println("异步发送异常: " + throwable.getMessage());
        return null;
    });
    
    // 或者等待结果
    try {
        EasyMailSendResult result = future.get(30, TimeUnit.SECONDS);
        handleSendResult(result);
    } catch (TimeoutException e) {
        System.err.println("发送超时");
    } catch (Exception e) {
        System.err.println("发送异常: " + e.getMessage());
    }
}
```

### 函数式邮件处理器

```java
@Configuration
public class EmailProcessorConfig {
    
    @Bean
    public EasyMailProcessorFunction orderProcessor() {
        return (message, content, subject, from) -> {
            if (subject.contains("订单")) {
                // 处理订单相关邮件
                processOrder(content);
                return true;
            }
            return false;
        };
    }
    
    @Bean
    public EasyMailProcessorFunction verificationProcessor() {
        return (message, content, subject, from) -> {
            if (from.contains("verification") || subject.contains("验证")) {
                // 处理验证码邮件
                String code = extractVerificationCode(content);
                if (code != null) {
                    saveVerificationCode(from, code);
                    return true;
                }
            }
            return false;
        };
    }
    
    private void processOrder(String content) {
        // 订单处理逻辑
    }
    
    private String extractVerificationCode(String content) {
        // 验证码提取逻辑
        return null;
    }
    
    private void saveVerificationCode(String from, String code) {
        // 验证码保存逻辑
    }
}
```

## 配置属性

### SMTP 发送配置

```yaml
mail:
  smtp:
    server: smtp.qq.com          # SMTP服务器地址
    port: 465                    # SMTP端口
    protocol: smtp               # 协议
    username: your@email.com     # 用户名
    password: your-auth-code     # 密码/授权码
    
    # 连接配置
    connection:
      timeout: 15000             # 连接超时(毫秒)
      read-timeout: 30000        # 读取超时(毫秒)
      write-timeout: 30000       # 写入超时(毫秒)
    
    # 协议属性
    properties:
      mail-smtp-auth: true                    # 启用认证
      mail-smtp-starttls-enable: true         # 启用STARTTLS
      mail-smtp-ssl-enable: false             # 启用SSL
    
    # 重试配置
    retry:
      enabled: true                          # 启用重试
      max-retries: 3                         # 最大重试次数
      initial-delay-ms: 1000                 # 初始延迟
      max-delay-ms: 10000                    # 最大延迟
      use-exponential-backoff: true          # 指数退避
      backoff-multiplier: 2.0                # 退避乘数
```

### IMAP 监听配置

```yaml
mail:
  imap:
    server: imap.qq.com          # IMAP服务器地址
    port: 993                    # IMAP端口
    protocol: imaps              # 协议
    username: your@email.com     # 用户名
    password: your-auth-code     # 密码/授权码
    attachment-dir: /path/to/attachments  # 附件保存目录
    
    # 连接配置
    connection:
      timeout: 15000             # 连接超时(毫秒)
      read-timeout: 30000        # 读取超时(毫秒)
      write-timeout: 30000       # 写入超时(毫秒)
    
    # 监控配置
    monitor:
      idle-timeout: 20000        # IDLE超时时间(毫秒)
      keep-alive-interval: 60    # 保活间隔(秒)
      reconnect-delay: 10        # 重连延迟(秒)
      short-delay: 5             # 短延迟(秒)
      long-delay: 30             # 长延迟(秒)
      task-timeout: 300          # 任务超时(秒)
    
    # 监听器配置
    listener:
      max-retries: 20            # 最大重试次数
      startup-process-strategy: MARK_AS_READ_ONLY  # 启动处理策略
```
邮件服务启动时处理策略配置说明请参考 [邮件服务启动时处理策略配置](STARTUP_PROCESS_STRATEGY.md)

## 最佳实践

### 1. 邮件发送最佳实践

#### 使用 Builder 模式

```java
// 推荐：使用 Builder 模式
EasyMailRequest request = EasyMailRequest.builder()
    .to("user@example.com")
    .subject("欢迎注册")
    .html(generateWelcomeHtml(user))
    .build();

// 不推荐：直接使用便捷方法发送复杂邮件
```

#### 异步发送长时间任务

```java
// 对于批量发送或大附件，使用异步发送
CompletableFuture<EasyMailSendResult> future = easyMailSenderService.sendAsync(request);
future.thenAccept(result -> {
    // 处理发送结果
    logSendResult(result);
});
```

#### 错误处理

```java
public void sendEmailWithErrorHandling(EasyMailRequest request) {
    try {
        EasyMailSendResult result = easyMailSenderService.send(request);
        
        if (result.isSuccess()) {
            // 记录成功日志
            log.info("邮件发送成功: {}", result.getMessageId());
        } else {
            // 记录失败日志
            log.error("邮件发送失败: {}", result.getErrorMessage());
            
            // 可以考虑重试或降级处理
            handleSendFailure(request, result);
        }
    } catch (Exception e) {
        log.error("邮件发送异常", e);
        // 异常处理逻辑
    }
}
```

### 2. 邮件监听最佳实践

#### 幂等性处理

```java
@Component
public class IdempotentEmailProcessor implements EasyMailListenerApi {
    
    private final Set<String> processedMessageIds = ConcurrentHashMap.newKeySet();
    
    @Override
    public boolean processEmail(Message message, String content, String subject, String from) {
        try {
            String messageId = message.getHeader("Message-ID")[0];
            
            // 检查是否已处理
            if (processedMessageIds.contains(messageId)) {
                log.debug("邮件已处理，跳过: {}", messageId);
                return true;
            }
            
            // 处理邮件
            boolean success = doProcessEmail(content, subject, from);
            
            if (success) {
                processedMessageIds.add(messageId);
            }
            
            return success;
        } catch (Exception e) {
            log.error("处理邮件异常", e);
            return false;
        }
    }
}
```

#### 分类处理

```java
@Component
public class ClassifiedEmailProcessor implements EasyMailListenerApi {
    
    @Override
    public boolean processEmail(Message message, String content, String subject, String from) {
        // 根据发件人分类处理
        if (from.contains("bank.com")) {
            return processBankEmail(content, subject);
        } else if (from.contains("shop.com")) {
            return processShopEmail(content, subject);
        } else if (subject.contains("验证码")) {
            return processVerificationEmail(content);
        }
        
        // 默认处理
        return processGeneralEmail(content, subject, from);
    }
}
```

### 3. 性能优化

#### 连接池配置

```yaml
mail:
  smtp:
    connection:
      pool-size: 10              # 连接池大小
      max-idle-time: 300         # 最大空闲时间(秒)
      validation-query: "NOOP"   # 连接验证查询
```

#### 批量发送优化

```java
public void sendBatchOptimized(List<String> recipients, String subject, String content) {
    // 分批发送，避免单次发送过多收件人
    int batchSize = 50;
    for (int i = 0; i < recipients.size(); i += batchSize) {
        List<String> batch = recipients.subList(i, Math.min(i + batchSize, recipients.size()));
        
        EasyMailRequest request = EasyMailRequest.builder()
            .toList(batch)
            .subject(subject)
            .text(content)
            .build();
            
        // 异步发送批次
        easyMailSenderService.sendAsync(request);
    }
}
```

### 4. 安全考虑

#### 敏感信息保护

```java
// 不要在日志中记录敏感信息
log.info("发送邮件给用户: {}", maskEmail(recipient));

private String maskEmail(String email) {
    if (email == null || !email.contains("@")) {
        return "***";
    }
    String[] parts = email.split("@");
    return parts[0].substring(0, 1) + "***@" + parts[1];
}
```

#### 输入验证

```java
public EasyMailSendResult sendEmailSafely(String to, String subject, String content) {
    // 验证邮箱格式
    if (!isValidEmail(to)) {
        throw new IllegalArgumentException("无效的邮箱地址: " + to);
    }
    
    // 验证内容长度
    if (content.length() > 10000) {
        throw new IllegalArgumentException("邮件内容过长");
    }
    
    // 过滤恶意内容
    String safeContent = sanitizeContent(content);
    
    return easyMailSenderService.sendText(to, subject, safeContent);
}
```

## 常见问题

### Q1: 邮件发送失败怎么办？

**A:** 检查以下几个方面：

1. **配置检查**：确认 SMTP 服务器地址、端口、用户名、密码是否正确
2. **网络连接**：确认网络连接正常，防火墙设置正确
3. **认证设置**：确认启用了 SMTP 认证，使用了正确的授权码
4. **SSL/TLS 设置**：根据邮件服务商要求配置 SSL 或 STARTTLS

```java
// 启用详细日志来诊断问题
mail:
  smtp:
    log:
      debug-enabled: true
```

### Q2: 如何处理大量邮件发送？

**A:** 使用以下策略：

1. **异步发送**：使用 `sendAsync()` 方法
2. **分批处理**：将大量收件人分成小批次
3. **限流控制**：控制发送频率，避免被邮件服务商限制
4. **监控统计**：监控发送成功率和性能指标

### Q3: 邮件监听器不工作怎么办？

**A:** 检查以下配置：

1. **IMAP 配置**：确认 IMAP 服务器配置正确
2. **权限设置**：确认邮箱启用了 IMAP 服务
3. **防火墙**：确认网络连接正常
4. **处理器注册**：确认邮件处理器正确注册为 Spring Bean

### Q4: 如何自定义邮件模板？

**A:** 参考高级文档中的自定义模板部分，或者：

```java
@Component
public class CustomTemplateEngine implements EasyMailSendTemplateEngine {
    
    @Override
    public String processTemplate(String template, Map<String, Object> variables) {
        // 实现自定义模板处理逻辑
        return processWithCustomEngine(template, variables);
    }
}
```

### Q5: 如何监控邮件服务状态？

**A:** 启用监控功能：

```yaml
email:
  sender:
    monitor:
      enabled: true
      health-check-interval: 5
```

然后通过 JMX 或自定义端点获取监控数据：

```java
@RestController
public class EmailMonitorController {
    
    @Resource
    private EasyMailSendMonitor sendMonitor;
    
    @GetMapping("/email/stats")
    public Map<String, Object> getEmailStats() {
        return sendMonitor.getStatistics();
    }
}
```

---

更多高级用法请参阅 [高级文档](ADVANCED.md)。