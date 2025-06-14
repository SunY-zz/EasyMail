# EasyMail 邮件发送服务

## 📖 概述

邮件发送服务是EasyMail SDK的另一个核心功能，提供了简单易用、功能强大的邮件发送能力。支持同步/异步发送、模板邮件、批量发送、附件处理等多种功能，让邮件发送变得简单而高效。

## ✨ 核心特性

- 📧 **多种发送方式**: 支持简单文本、HTML、模板邮件发送
- ⚡ **异步发送**: 支持同步和异步发送模式，提高应用性能
- 📎 **附件支持**: 支持多种类型附件，包括文件、字节数组、输入流
- 📊 **批量发送**: 高效的批量邮件发送功能
- 🎨 **模板引擎**: 内置模板引擎支持，支持变量替换和条件渲染
- 🔄 **重试机制**: 智能重试机制，提高发送成功率
- 📈 **发送统计**: 详细的发送统计和监控功能
- 🌐 **REST API**: 提供完整的邮件发送REST API
- 🎯 **发送策略**: 支持多种发送策略和负载均衡
- 📝 **事件监听**: 支持发送事件监听和回调

## 🚀 快速开始

### 1. 基础配置

在`application.yml`中添加邮件服务器配置：

```yaml
# 邮件服务器配置
spring:
  mail:
    host: smtp.gmail.com          # SMTP服务器地址
    port: 587                     # SMTP端口
    username: your-email@gmail.com # 邮箱账号
    password: your-app-password    # 邮箱密码或应用密码
    properties:
      mail:
        smtp:
          auth: true              # 启用认证
          starttls:
            enable: true          # 启用TLS
            required: true        # 要求TLS
        debug: false              # 调试模式

# EasyMail发送服务配置
email-sender:
  enabled: true                   # 启用邮件发送服务
  default:
    from: your-email@gmail.com    # 默认发件人
    from-name: "EasyMail System"  # 默认发件人名称
  async:
    enabled: true                 # 启用异步发送
    core-pool-size: 5            # 核心线程数
    max-pool-size: 20            # 最大线程数
    queue-capacity: 100          # 队列容量
  retry:
    enabled: true                # 启用重试
    max-attempts: 3              # 最大重试次数
    delay: 1000                  # 重试延迟（毫秒）
  template:
    enabled: true                # 启用模板功能
    cache-enabled: true          # 启用模板缓存
  monitoring:
    enabled: true                # 启用监控
```

### 2. 注入邮件发送服务

```java
@Service
public class NotificationService {
    
    @Autowired
    private EmailSenderService emailSenderService;
    
    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);
}
```

### 3. 基本邮件发送

```java
/**
 * 发送简单文本邮件
 */
public void sendSimpleEmail() {
    try {
        EmailRequest request = EmailRequest.builder()
            .to("recipient@example.com")
            .subject("欢迎使用EasyMail")
            .text("这是一封测试邮件，感谢您使用EasyMail！")
            .build();
            
        EmailResult result = emailSenderService.send(request);
        
        if (result.isSuccess()) {
            log.info("邮件发送成功: messageId={}", result.getMessageId());
        } else {
            log.error("邮件发送失败: {}", result.getErrorMessage());
        }
        
    } catch (Exception e) {
        log.error("邮件发送异常", e);
    }
}

/**
 * 发送HTML邮件
 */
public void sendHtmlEmail() {
    String htmlContent = """
        <html>
        <body>
            <h2 style="color: #2E86AB;">欢迎使用EasyMail</h2>
            <p>这是一封<strong>HTML格式</strong>的邮件。</p>
            <ul>
                <li>支持富文本格式</li>
                <li>支持图片和链接</li>
                <li>支持CSS样式</li>
            </ul>
            <p>访问我们的网站：<a href="https://example.com">点击这里</a></p>
        </body>
        </html>
        """;
        
    EmailRequest request = EmailRequest.builder()
        .to("recipient@example.com")
        .subject("HTML邮件示例")
        .html(htmlContent)
        .build();
        
    EmailResult result = emailSenderService.send(request);
    log.info("HTML邮件发送结果: {}", result.isSuccess());
}
```

## 📧 邮件发送方式详解

### 1. 同步发送

同步发送会阻塞当前线程直到邮件发送完成，适用于需要立即知道发送结果的场景。

```java
/**
 * 同步发送邮件
 * 适用于：验证码邮件、重要通知等需要确保发送成功的场景
 */
public void sendSyncEmail() {
    EmailRequest request = EmailRequest.builder()
        .to("user@example.com")
        .subject("验证码")
        .text("您的验证码是：123456，5分钟内有效。")
        .build();
        
    // 同步发送
    EmailResult result = emailSenderService.send(request);
    
    if (result.isSuccess()) {
        log.info("验证码邮件发送成功，耗时: {}ms", result.getSendDuration());
        // 可以立即进行后续处理
        updateUserVerificationStatus(result.getMessageId());
    } else {
        log.error("验证码邮件发送失败: {}", result.getErrorMessage());
        throw new EmailSendException("验证码发送失败");
    }
}
```

### 2. 异步发送

异步发送不会阻塞当前线程，适用于批量发送或对发送时间不敏感的场景。

```java
/**
 * 异步发送邮件
 * 适用于：营销邮件、通知邮件等对实时性要求不高的场景
 */
public void sendAsyncEmail() {
    EmailRequest request = EmailRequest.builder()
        .to("user@example.com")
        .subject("产品更新通知")
        .html("<h2>我们的产品有新功能上线了！</h2><p>点击查看详情...</p>")
        .build();
        
    // 异步发送
    CompletableFuture<EmailResult> future = emailSenderService.sendAsync(request);
    
    // 设置回调处理
    future.thenAccept(result -> {
        if (result.isSuccess()) {
            log.info("异步邮件发送成功: messageId={}", result.getMessageId());
            // 更新发送记录
            updateEmailSendRecord(result);
        } else {
            log.error("异步邮件发送失败: {}", result.getErrorMessage());
            // 记录失败日志
            recordFailedEmail(request, result.getErrorMessage());
        }
    }).exceptionally(throwable -> {
        log.error("异步邮件发送异常", throwable);
        return null;
    });
    
    log.info("异步邮件已提交发送队列");
}

/**
 * 异步发送带回调
 */
public void sendAsyncWithCallback() {
    EmailRequest request = EmailRequest.builder()
        .to("user@example.com")
        .subject("订单确认")
        .text("您的订单已确认，订单号：ORD123456")
        .build();
        
    // 使用回调方式
    emailSenderService.sendAsync(request, new EmailSendCallback() {
        @Override
        public void onSuccess(EmailResult result) {
            log.info("订单确认邮件发送成功");
            // 更新订单状态
            updateOrderEmailStatus("ORD123456", true);
        }
        
        @Override
        public void onFailure(EmailResult result) {
            log.error("订单确认邮件发送失败: {}", result.getErrorMessage());
            // 标记需要重新发送
            markEmailForRetry("ORD123456");
        }
    });
}
```

### 3. 批量发送

高效的批量邮件发送功能，支持模板变量替换和发送状态跟踪。

```java
/**
 * 批量发送邮件
 * 适用于：营销活动、系统通知等需要发送给多个用户的场景
 */
public void sendBatchEmails() {
    // 准备收件人列表
    List<BatchEmailRecipient> recipients = Arrays.asList(
        BatchEmailRecipient.builder()
            .email("user1@example.com")
            .name("张三")
            .variable("orderNo", "ORD001")
            .variable("amount", "99.99")
            .build(),
        BatchEmailRecipient.builder()
            .email("user2@example.com")
            .name("李四")
            .variable("orderNo", "ORD002")
            .variable("amount", "199.99")
            .build(),
        BatchEmailRecipient.builder()
            .email("user3@example.com")
            .name("王五")
            .variable("orderNo", "ORD003")
            .variable("amount", "299.99")
            .build()
    );
    
    // 创建批量发送请求
    BatchEmailRequest batchRequest = BatchEmailRequest.builder()
        .subject("订单支付成功通知")
        .template("order-success")  // 使用模板
        .recipients(recipients)
        .batchSize(10)              // 每批发送10封
        .delayBetweenBatches(1000)  // 批次间延迟1秒
        .build();
        
    // 执行批量发送
    BatchEmailResult batchResult = emailSenderService.sendBatch(batchRequest);
    
    log.info("批量邮件发送完成: 总数={}, 成功={}, 失败={}, 耗时={}ms",
            batchResult.getTotalCount(),
            batchResult.getSuccessCount(),
            batchResult.getFailureCount(),
            batchResult.getTotalDuration());
            
    // 处理失败的邮件
    if (batchResult.getFailureCount() > 0) {
        List<EmailResult> failedResults = batchResult.getFailedResults();
        for (EmailResult failedResult : failedResults) {
            log.error("邮件发送失败: to={}, error={}",
                    failedResult.getRecipient(), failedResult.getErrorMessage());
        }
        
        // 可以选择重试失败的邮件
        retryFailedEmails(failedResults);
    }
}

/**
 * 异步批量发送
 */
public void sendBatchEmailsAsync() {
    List<BatchEmailRecipient> recipients = prepareRecipients();
    
    BatchEmailRequest batchRequest = BatchEmailRequest.builder()
        .subject("月度报告")
        .template("monthly-report")
        .recipients(recipients)
        .build();
        
    // 异步批量发送
    CompletableFuture<BatchEmailResult> future = emailSenderService.sendBatchAsync(batchRequest);
    
    future.thenAccept(result -> {
        log.info("异步批量发送完成: 成功率={}%", 
                result.getSuccessRate() * 100);
    });
    
    log.info("批量邮件已提交异步处理");
}
```

## 🎨 模板邮件

### 1. 创建邮件模板

在`src/main/resources/email-templates/`目录下创建模板文件：

**welcome.html**
```html
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>欢迎邮件</title>
    <style>
        .container { max-width: 600px; margin: 0 auto; font-family: Arial, sans-serif; }
        .header { background-color: #2E86AB; color: white; padding: 20px; text-align: center; }
        .content { padding: 20px; }
        .footer { background-color: #f5f5f5; padding: 10px; text-align: center; font-size: 12px; }
        .button { background-color: #2E86AB; color: white; padding: 10px 20px; text-decoration: none; border-radius: 5px; }
    </style>
</head>
<body>
    <div class="container">
        <div class="header">
            <h1>欢迎加入 {{companyName}}</h1>
        </div>
        <div class="content">
            <p>亲爱的 {{userName}}，</p>
            <p>欢迎您注册成为我们的用户！您的账号信息如下：</p>
            <ul>
                <li>用户名：{{username}}</li>
                <li>注册邮箱：{{email}}</li>
                <li>注册时间：{{registerTime}}</li>
            </ul>
            
            {{#if needVerification}}
            <p>请点击下面的按钮完成邮箱验证：</p>
            <p style="text-align: center;">
                <a href="{{verificationUrl}}" class="button">验证邮箱</a>
            </p>
            {{/if}}
            
            <p>如果您有任何问题，请随时联系我们的客服团队。</p>
        </div>
        <div class="footer">
            <p>&copy; 2024 {{companyName}}. All rights reserved.</p>
        </div>
    </div>
</body>
</html>
```

**verification-code.txt**
```text
亲爱的 {{userName}}，

您正在进行{{operationType}}操作，验证码为：

{{verificationCode}}

验证码有效期为 {{expireMinutes}} 分钟，请及时使用。

如果这不是您本人的操作，请忽略此邮件。

{{companyName}}
{{currentTime}}
```

### 2. 使用模板发送邮件

```java
/**
 * 发送欢迎邮件
 */
public void sendWelcomeEmail(String userEmail, String userName) {
    // 准备模板变量
    Map<String, Object> variables = new HashMap<>();
    variables.put("userName", userName);
    variables.put("username", userEmail);
    variables.put("email", userEmail);
    variables.put("companyName", "EasyMail科技");
    variables.put("registerTime", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
    variables.put("needVerification", true);
    variables.put("verificationUrl", "https://example.com/verify?token=abc123");
    
    EmailRequest request = EmailRequest.builder()
        .to(userEmail)
        .subject("欢迎加入EasyMail科技")
        .template("welcome")        // 模板名称
        .variables(variables)       // 模板变量
        .build();
        
    EmailResult result = emailSenderService.send(request);
    
    if (result.isSuccess()) {
        log.info("欢迎邮件发送成功: user={}", userName);
    }
}

/**
 * 发送验证码邮件
 */
public void sendVerificationCode(String userEmail, String userName, String code) {
    Map<String, Object> variables = new HashMap<>();
    variables.put("userName", userName);
    variables.put("operationType", "登录验证");
    variables.put("verificationCode", code);
    variables.put("expireMinutes", 5);
    variables.put("companyName", "EasyMail科技");
    variables.put("currentTime", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
    
    EmailRequest request = EmailRequest.builder()
        .to(userEmail)
        .subject("登录验证码")
        .template("verification-code")  // 使用文本模板
        .variables(variables)
        .build();
        
    // 验证码邮件建议同步发送，确保及时性
    EmailResult result = emailSenderService.send(request);
    
    if (!result.isSuccess()) {
        throw new EmailSendException("验证码发送失败: " + result.getErrorMessage());
    }
}

/**
 * 动态模板内容
 */
public void sendDynamicTemplate(String userEmail, String templateContent) {
    Map<String, Object> variables = new HashMap<>();
    variables.put("userName", "用户");
    variables.put("currentDate", LocalDate.now().toString());
    
    EmailRequest request = EmailRequest.builder()
        .to(userEmail)
        .subject("动态模板邮件")
        .templateContent(templateContent)  // 直接使用模板内容
        .variables(variables)
        .build();
        
    emailSenderService.sendAsync(request);
}
```

## 📎 附件处理

### 1. 发送文件附件

```java
/**
 * 发送带文件附件的邮件
 */
public void sendEmailWithFileAttachment() {
    try {
        // 准备附件文件
        File reportFile = new File("/path/to/monthly-report.pdf");
        File imageFile = new File("/path/to/chart.png");
        
        EmailRequest request = EmailRequest.builder()
            .to("manager@example.com")
            .subject("月度报告")
            .html("<h2>月度报告</h2><p>请查看附件中的详细报告。</p>")
            .attachment(reportFile)                    // 添加文件附件
            .attachment(imageFile, "报表图表.png")      // 添加文件附件并指定显示名称
            .build();
            
        EmailResult result = emailSenderService.send(request);
        
        if (result.isSuccess()) {
            log.info("带附件邮件发送成功，附件数量: {}", request.getAttachments().size());
        }
        
    } catch (Exception e) {
        log.error("发送附件邮件失败", e);
    }
}

/**
 * 发送字节数组附件
 */
public void sendEmailWithByteArrayAttachment() {
    try {
        // 生成CSV报告数据
        String csvContent = generateCsvReport();
        byte[] csvBytes = csvContent.getBytes(StandardCharsets.UTF_8);
        
        // 生成图片数据
        byte[] imageBytes = generateChartImage();
        
        EmailRequest request = EmailRequest.builder()
            .to("analyst@example.com")
            .subject("数据分析报告")
            .text("请查看附件中的数据分析报告和图表。")
            .attachment(csvBytes, "data-report.csv", "text/csv")           // CSV附件
            .attachment(imageBytes, "analysis-chart.png", "image/png")     // 图片附件
            .build();
            
        emailSenderService.sendAsync(request);
        
    } catch (Exception e) {
        log.error("发送数据报告邮件失败", e);
    }
}

/**
 * 发送输入流附件
 */
public void sendEmailWithStreamAttachment() {
    try (InputStream inputStream = getClass().getResourceAsStream("/templates/user-manual.pdf")) {
        
        EmailRequest request = EmailRequest.builder()
            .to("newuser@example.com")
            .subject("用户手册")
            .html("<p>欢迎使用我们的产品！请查看附件中的用户手册。</p>")
            .attachment(inputStream, "用户手册.pdf", "application/pdf")
            .build();
            
        EmailResult result = emailSenderService.send(request);
        log.info("用户手册邮件发送结果: {}", result.isSuccess());
        
    } catch (Exception e) {
        log.error("发送用户手册失败", e);
    }
}
```

### 2. 内嵌图片

```java
/**
 * 发送带内嵌图片的HTML邮件
 */
public void sendEmailWithInlineImages() {
    try {
        // 准备内嵌图片
        File logoFile = new File("/path/to/company-logo.png");
        File bannerFile = new File("/path/to/banner.jpg");
        
        // HTML内容，使用cid引用内嵌图片
        String htmlContent = """
            <html>
            <body>
                <div style="text-align: center;">
                    <img src="cid:logo" alt="Company Logo" style="width: 200px;"/>
                </div>
                <h2>产品推广</h2>
                <p>我们很高兴向您介绍我们的新产品：</p>
                <div style="text-align: center;">
                    <img src="cid:banner" alt="Product Banner" style="width: 100%; max-width: 600px;"/>
                </div>
                <p>点击<a href="https://example.com/product">这里</a>了解更多详情。</p>
            </body>
            </html>
            """;
            
        EmailRequest request = EmailRequest.builder()
            .to("customer@example.com")
            .subject("新产品推广")
            .html(htmlContent)
            .inlineImage(logoFile, "logo")      // 内嵌图片，cid为logo
            .inlineImage(bannerFile, "banner")  // 内嵌图片，cid为banner
            .build();
            
        EmailResult result = emailSenderService.send(request);
        log.info("内嵌图片邮件发送结果: {}", result.isSuccess());
        
    } catch (Exception e) {
        log.error("发送内嵌图片邮件失败", e);
    }
}
```

## ⚙️ 完整配置选项

```yaml
email-sender:
  enabled: true                     # 启用邮件发送服务
  
  # 默认配置
  default:
    from: noreply@example.com       # 默认发件人邮箱
    from-name: "System"             # 默认发件人名称
    reply-to: support@example.com   # 默认回复邮箱
    charset: UTF-8                  # 默认字符编码
    
  # 异步发送配置
  async:
    enabled: true                   # 启用异步发送
    core-pool-size: 5              # 核心线程数
    max-pool-size: 20              # 最大线程数
    queue-capacity: 100            # 队列容量
    keep-alive-seconds: 60         # 线程保活时间
    thread-name-prefix: "email-"   # 线程名前缀
    
  # 重试配置
  retry:
    enabled: true                   # 启用重试机制
    max-attempts: 3                # 最大重试次数
    delay: 1000                    # 重试延迟（毫秒）
    multiplier: 2.0                # 延迟倍数
    max-delay: 10000               # 最大延迟
    
  # 模板配置
  template:
    enabled: true                   # 启用模板功能
    path: classpath:email-templates/ # 模板路径
    cache-enabled: true            # 启用模板缓存
    cache-size: 100                # 缓存大小
    encoding: UTF-8                # 模板编码
    
  # 批量发送配置
  batch:
    default-batch-size: 50         # 默认批次大小
    max-batch-size: 200            # 最大批次大小
    delay-between-batches: 1000    # 批次间延迟
    max-concurrent-batches: 3      # 最大并发批次数
    
  # 附件配置
  attachment:
    max-size: 25MB                 # 单个附件最大大小
    max-total-size: 50MB           # 总附件最大大小
    allowed-types:                 # 允许的附件类型
      - "application/pdf"
      - "image/*"
      - "text/*"
      - "application/vnd.ms-excel"
      - "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
    
  # 监控配置
  monitoring:
    enabled: true                   # 启用监控
    metrics-enabled: true          # 启用指标收集
    
  # 发送策略配置
  strategy:
    default: "round-robin"          # 默认发送策略
    strategies:
      round-robin:
        enabled: true
      random:
        enabled: true
      weighted:
        enabled: true
        weights:
          smtp1: 70
          smtp2: 30
          
  # 事件配置
  events:
    enabled: true                   # 启用事件发布
    async-events: true             # 异步事件处理
```

**配置参数详解：**

| 配置项 | 类型 | 默认值 | 说明 |
|--------|------|--------|------|
| `enabled` | boolean | true | 是否启用邮件发送服务 |
| `default.from` | String | - | 默认发件人邮箱 |
| `default.from-name` | String | "" | 默认发件人名称 |
| `async.enabled` | boolean | true | 是否启用异步发送 |
| `async.core-pool-size` | int | 5 | 异步发送核心线程数 |
| `async.max-pool-size` | int | 20 | 异步发送最大线程数 |
| `retry.enabled` | boolean | true | 是否启用重试机制 |
| `retry.max-attempts` | int | 3 | 最大重试次数 |
| `template.enabled` | boolean | true | 是否启用模板功能 |
| `batch.default-batch-size` | int | 50 | 默认批次大小 |
| `attachment.max-size` | String | "25MB" | 单个附件最大大小 |
| `monitoring.enabled` | boolean | true | 是否启用监控 |

## 🎯 最佳实践

### 1. 邮件发送策略选择

```java
@Service
public class SmartEmailService {
    
    @Autowired
    private EmailSenderService emailSenderService;
    
    /**
     * 根据邮件类型选择发送策略
     */
    public void sendEmailByType(EmailType type, EmailRequest request) {
        switch (type) {
            case VERIFICATION_CODE:
                // 验证码邮件：同步发送，确保及时性
                sendVerificationEmail(request);
                break;
                
            case NOTIFICATION:
                // 通知邮件：异步发送，不影响主流程
                sendNotificationEmail(request);
                break;
                
            case MARKETING:
                // 营销邮件：批量异步发送
                sendMarketingEmail(request);
                break;
                
            case REPORT:
                // 报告邮件：异步发送，可能包含大附件
                sendReportEmail(request);
                break;
        }
    }
    
    private void sendVerificationEmail(EmailRequest request) {
        // 验证码邮件必须同步发送
        EmailResult result = emailSenderService.send(request);
        if (!result.isSuccess()) {
            throw new EmailSendException("验证码发送失败");
        }
        log.info("验证码邮件发送成功: {}", result.getMessageId());
    }
    
    private void sendNotificationEmail(EmailRequest request) {
        // 通知邮件异步发送
        emailSenderService.sendAsync(request)
            .thenAccept(result -> {
                if (result.isSuccess()) {
                    updateNotificationStatus(request.getTo().get(0), "SENT");
                } else {
                    updateNotificationStatus(request.getTo().get(0), "FAILED");
                }
            });
    }
    
    private void sendMarketingEmail(EmailRequest request) {
        // 营销邮件使用批量发送
        // 实现批量发送逻辑
    }
    
    private void sendReportEmail(EmailRequest request) {
        // 报告邮件异步发送，设置较长超时时间
        CompletableFuture<EmailResult> future = emailSenderService.sendAsync(request);
        future.orTimeout(5, TimeUnit.MINUTES)
            .thenAccept(result -> {
                log.info("报告邮件发送完成: {}", result.isSuccess());
            })
            .exceptionally(throwable -> {
                log.error("报告邮件发送超时或失败", throwable);
                return null;
            });
    }
}
```

### 2. 错误处理和重试机制

```java
@Service
public class RobustEmailService {
    
    @Autowired
    private EmailSenderService emailSenderService;
    
    @Retryable(
        value = {EmailSendException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public void sendEmailWithRetry(EmailRequest request) {
        try {
            EmailResult result = emailSenderService.send(request);
            
            if (!result.isSuccess()) {
                // 根据错误类型决定是否重试
                if (isRetryableError(result.getErrorMessage())) {
                    throw new EmailSendException("可重试的发送失败: " + result.getErrorMessage());
                } else {
                    log.error("不可重试的发送失败: {}", result.getErrorMessage());
                    handleNonRetryableError(request, result);
                }
            }
            
        } catch (Exception e) {
            log.error("邮件发送异常: to={}", request.getTo(), e);
            throw new EmailSendException("邮件发送失败", e);
        }
    }
    
    @Recover
    public void recoverFromEmailSendFailure(EmailSendException ex, EmailRequest request) {
        log.error("邮件发送最终失败，已达最大重试次数: to={}", request.getTo());
        
        // 记录到失败队列
        recordFailedEmail(request, ex.getMessage());
        
        // 发送告警通知
        sendAlertToAdmin("邮件发送失败", request, ex);
    }
    
    private boolean isRetryableError(String errorMessage) {
        // 判断是否为可重试的错误
        return errorMessage.contains("timeout") ||
               errorMessage.contains("connection") ||
               errorMessage.contains("temporary");
    }
    
    private void handleNonRetryableError(EmailRequest request, EmailResult result) {
        // 处理不可重试的错误，如邮箱地址无效等
        log.warn("邮箱地址可能无效: {}", request.getTo());
        markEmailAddressAsInvalid(request.getTo().get(0));
    }
}
```

### 3. 性能优化

```java
@Service
public class OptimizedEmailService {
    
    @Autowired
    private EmailSenderService emailSenderService;
    
    /**
     * 批量发送优化
     */
    public void sendBulkEmailsOptimized(List<EmailRequest> requests) {
        // 按收件人域名分组，提高发送效率
        Map<String, List<EmailRequest>> groupedByDomain = requests.stream()
            .collect(Collectors.groupingBy(this::extractDomain));
            
        // 并行处理不同域名的邮件
        groupedByDomain.entrySet().parallelStream().forEach(entry -> {
            String domain = entry.getKey();
            List<EmailRequest> domainRequests = entry.getValue();
            
            log.info("开始发送{}域名邮件，数量: {}", domain, domainRequests.size());
            
            // 分批发送，避免被限流
            List<List<EmailRequest>> batches = partition(domainRequests, 10);
            
            for (List<EmailRequest> batch : batches) {
                sendBatchWithDelay(batch, 1000); // 批次间延迟1秒
            }
        });
    }
    
    /**
     * 模板缓存优化
     */
    @Cacheable(value = "emailTemplates", key = "#templateName")
    public String getTemplate(String templateName) {
        // 模板内容会被缓存，避免重复读取文件
        return loadTemplateFromFile(templateName);
    }
    
    /**
     * 连接池优化
     */
    @EventListener
    public void onApplicationReady(ApplicationReadyEvent event) {
        // 预热邮件连接池
        warmUpEmailConnections();
    }
    
    private String extractDomain(EmailRequest request) {
        String email = request.getTo().get(0);
        return email.substring(email.indexOf('@') + 1);
    }
    
    private void sendBatchWithDelay(List<EmailRequest> batch, long delayMs) {
        batch.forEach(request -> {
            emailSenderService.sendAsync(request);
            try {
                Thread.sleep(delayMs / batch.size()); // 平均分配延迟
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
    }
}
```

### 4. 监控和统计

```java
@Component
public class EmailMetricsCollector {
    
    private final MeterRegistry meterRegistry;
    private final Counter emailSentCounter;
    private final Counter emailFailedCounter;
    private final Timer emailSendTimer;
    
    public EmailMetricsCollector(MeterRegistry meterRegistry) {
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
    public void onEmailSent(EmailSentEvent event) {
        emailSentCounter.increment(
            Tags.of(
                "type", event.getEmailType(),
                "template", event.getTemplateName()
            )
        );
        
        emailSendTimer.record(event.getDuration(), TimeUnit.MILLISECONDS);
    }
    
    @EventListener
    public void onEmailFailed(EmailFailedEvent event) {
        emailFailedCounter.increment(
            Tags.of(
                "type", event.getEmailType(),
                "error", event.getErrorType()
            )
        );
    }
}
```

## 🌐 REST API 接口

当项目包含`spring-boot-starter-web`依赖时，系统会自动提供REST API接口：

### 发送简单邮件

```http
POST /api/email/send
Content-Type: application/json

{
  "to": ["user@example.com"],
  "subject": "测试邮件",
  "text": "这是一封测试邮件",
  "from": "sender@example.com"
}
```

### 发送模板邮件

```http
POST /api/email/send/template
Content-Type: application/json

{
  "to": ["user@example.com"],
  "subject": "欢迎邮件",
  "template": "welcome",
  "variables": {
    "userName": "张三",
    "companyName": "EasyMail科技"
  }
}
```

### 异步发送邮件

```http
POST /api/email/send/async
Content-Type: application/json

{
  "to": ["user@example.com"],
  "subject": "异步邮件",
  "html": "<h1>这是异步发送的邮件</h1>"
}
```

### 批量发送邮件

```http
POST /api/email/send/batch
Content-Type: application/json

{
  "subject": "批量通知",
  "template": "notification",
  "recipients": [
    {
      "email": "user1@example.com",
      "name": "用户1",
      "variables": {
        "orderNo": "ORD001"
      }
    },
    {
      "email": "user2@example.com",
      "name": "用户2",
      "variables": {
        "orderNo": "ORD002"
      }
    }
  ]
}
```

### 获取发送统计

```http
GET /api/email/statistics
```

**响应示例：**
```json
{
  "success": true,
  "data": {
    "totalSent": 1250,
    "totalFailed": 15,
    "successRate": 98.8,
    "averageSendTime": 245,
    "todaySent": 89,
    "queueSize": 5
  }
}
```

### 获取发送历史

```http
GET /api/email/history?page=0&size=20&status=SUCCESS
```

## 📊 监控和管理

### 健康检查

```http
GET /actuator/health/email
```

### 指标监控

```http
GET /actuator/metrics/email.sent
GET /actuator/metrics/email.failed
GET /actuator/metrics/email.send.duration
```

### 配置管理

```http
# 获取当前配置
GET /api/email/config

# 更新配置
PUT /api/email/config
Content-Type: application/json

{
  "async.enabled": true,
  "retry.maxAttempts": 5
}
```

## 🔍 故障排除

### 常见问题

1. **邮件发送失败**
   ```yaml
   # 检查SMTP配置
   spring:
     mail:
       host: smtp.gmail.com
       port: 587
       username: your-email@gmail.com
       password: your-app-password  # 使用应用密码，不是登录密码
   ```

2. **附件过大**
   ```yaml
   # 调整附件大小限制
   email-sender:
     attachment:
       max-size: 50MB
       max-total-size: 100MB
   ```

3. **发送速度慢**
   ```yaml
   # 增加线程池大小
   email-sender:
     async:
       core-pool-size: 10
       max-pool-size: 50
   ```

### 调试配置

```yaml
logging:
  level:
    cn.sunyblog.javaemaildemo.sender: DEBUG
    org.springframework.mail: DEBUG
    
email-sender:
  monitoring:
    enabled: true
    metrics-enabled: true
```

## 🚀 高级特性

### 1. 自定义发送策略

```java
@Component
public class CustomEmailSendStrategy implements EmailSendStrategy {
    
    @Override
    public EmailResult send(EmailRequest request, JavaMailSender mailSender) {
        // 实现自定义发送逻辑
        // 例如：根据收件人域名选择不同的SMTP服务器
        return doCustomSend(request, mailSender);
    }
    
    @Override
    public boolean supports(EmailRequest request) {
        // 判断是否支持此请求
        return request.getTo().stream()
            .anyMatch(email -> email.endsWith("@vip.com"));
    }
}
```

### 2. 事件监听

```java
@Component
public class EmailEventListener {
    
    @EventListener
    public void onEmailSending(EmailSendingEvent event) {
        log.info("邮件开始发送: to={}", event.getRequest().getTo());
    }
    
    @EventListener
    public void onEmailSent(EmailSentEvent event) {
        log.info("邮件发送成功: messageId={}, duration={}ms",
                event.getResult().getMessageId(), event.getDuration());
    }
    
    @EventListener
    public void onEmailFailed(EmailFailedEvent event) {
        log.error("邮件发送失败: to={}, error={}",
                event.getRequest().getTo(), event.getError());
        
        // 可以在这里实现失败处理逻辑
        handleEmailFailure(event);
    }
}
```

---

通过以上详细的文档和示例，你可以充分利用EasyMail的邮件发送服务功能，构建高效、可靠的邮件发送系统。如果有任何问题，请参考主文档或联系技术支持。