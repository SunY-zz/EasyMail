# JavaEmailSpringBoot API 参考文档

## 目录

1. [核心接口](#核心接口)
2. [邮件发送API](#邮件发送api)
3. [邮件接收API](#邮件接收api)
4. [注解驱动API](#注解驱动api)
5. [配置API](#配置api)
6. [监控API](#监控api)
7. [扩展接口](#扩展接口)
8. [事件API](#事件api)
9. [工具类API](#工具类api)
10. [错误代码](#错误代码)

## 核心接口

### EasyMailSender

统一的邮件发送接口，提供所有邮件发送功能。

```java
public interface EasyMailSender {
    
    // === 基础发送方法 ===
    
    /**
     * 发送简单文本邮件
     * @param to 收件人
     * @param subject 主题
     * @param text 文本内容
     * @return 发送结果
     */
    SendResult sendText(String to, String subject, String text);
    
    /**
     * 发送HTML邮件
     * @param to 收件人
     * @param subject 主题
     * @param html HTML内容
     * @return 发送结果
     */
    SendResult sendHtml(String to, String subject, String html);
    
    /**
     * 发送带附件的邮件
     * @param to 收件人
     * @param subject 主题
     * @param text 文本内容
     * @param attachment 附件
     * @return 发送结果
     */
    SendResult sendWithAttachment(String to, String subject, String text, File attachment);
    
    /**
     * 使用模板发送邮件
     * @param to 收件人
     * @param subject 主题
     * @param templateId 模板ID
     * @param variables 模板变量
     * @return 发送结果
     */
    SendResult sendWithTemplate(String to, String subject, String templateId, Map<String, Object> variables);
    
    // === Builder模式发送 ===
    
    /**
     * 使用EmailRequest发送邮件（同步）
     * @param request 邮件请求
     * @return 发送结果
     */
    SendResult send(EmailRequest request);
    
    /**
     * 使用EmailRequest发送邮件（异步）
     * @param request 邮件请求
     * @return 异步发送结果
     */
    CompletableFuture<SendResult> sendAsync(EmailRequest request);
    
    // === 批量发送 ===
    
    /**
     * 批量发送邮件
     * @param requests 邮件请求列表
     * @return 发送结果
     */
    SendResult sendBatch(List<EmailRequest> requests);
    
    // === 异步发送方法 ===
    
    /**
     * 异步发送文本邮件
     * @param to 收件人
     * @param subject 主题
     * @param text 文本内容
     * @return 异步发送结果
     */
    CompletableFuture<SendResult> sendTextAsync(String to, String subject, String text);
    
    /**
     * 异步发送HTML邮件
     * @param to 收件人
     * @param subject 主题
     * @param html HTML内容
     * @return 异步发送结果
     */
    CompletableFuture<SendResult> sendHtmlAsync(String to, String subject, String html);
    
    /**
     * 带回调的异步发送
     * @param to 收件人
     * @param subject 主题
     * @param text 文本内容
     * @param successCallback 成功回调
     * @param failureCallback 失败回调
     * @return 异步发送结果
     */
    CompletableFuture<SendResult> sendWithCallback(
        String to, String subject, String text,
        Consumer<SendResult> successCallback,
        Consumer<SendResult> failureCallback
    );
    
    // === 状态查询方法 ===
    
    /**
     * 检查邮件服务连接状态
     * @return 连接是否正常
     */
    boolean checkConnection();
    
    /**
     * 获取发送统计信息
     * @return 统计信息Map
     */
    Map<String, Object> getSendingStats();
    
    /**
     * 获取线程池状态
     * @return 线程池状态Map
     */
    Map<String, Object> getThreadPoolStatus();
}
```

## 邮件发送API

### EmailRequest

邮件请求构建器，使用Builder模式创建邮件请求。

```java
@Data
@Builder
public class EmailRequest {
    
    // === 基础字段 ===
    
    /** 收件人列表（TO） */
    @Singular("to")
    private List<String> toList;
    
    /** 抄送人列表（CC） */
    @Singular("cc")
    private List<String> ccList;
    
    /** 密送人列表（BCC） */
    @Singular("bcc")
    private List<String> bccList;
    
    /** 邮件主题 */
    private String subject;
    
    /** 文本内容（纯文本格式） */
    private String text;
    
    /** HTML内容（HTML格式） */
    private String html;
    
    /** 附件列表 */
    @Singular("attachment")
    private List<File> attachments;
    
    // === 模板相关 ===
    
    /** 模板ID（用于模板邮件） */
    private String templateId;
    
    /** 模板变量（用于模板邮件） */
    @Singular("variable")
    private Map<String, Object> templateVariables;
    
    // === 发送选项 ===
    
    /** 邮件优先级（1-5，1为最高优先级） */
    @Builder.Default
    private Integer priority = 3;
    
    /** 是否异步发送 */
    @Builder.Default
    private boolean async = false;
    
    /** 发送策略（可选：default, batch, high_priority, custom） */
    private String strategy;
    
    /** 重试次数 */
    @Builder.Default
    private Integer retryCount = 3;
    
    /** 发送延迟（毫秒） */
    @Builder.Default
    private Long delay = 0L;
    
    // === 验证方法 ===
    
    /**
     * 验证邮件请求是否有效
     * @return 验证结果
     */
    public ValidationResult validate() {
        List<String> errors = new ArrayList<>();
        
        // 验证收件人
        if (toList == null || toList.isEmpty()) {
            errors.add("收件人不能为空");
        } else {
            for (String email : toList) {
                if (!isValidEmail(email)) {
                    errors.add("无效的收件人邮箱: " + email);
                }
            }
        }
        
        // 验证主题
        if (subject == null || subject.trim().isEmpty()) {
            errors.add("邮件主题不能为空");
        }
        
        // 验证内容
        if ((text == null || text.trim().isEmpty()) && 
            (html == null || html.trim().isEmpty()) && 
            (templateId == null || templateId.trim().isEmpty())) {
            errors.add("邮件内容、HTML内容或模板ID至少需要提供一个");
        }
        
        // 验证模板
        if (templateId != null && !templateId.trim().isEmpty()) {
            if (templateVariables == null || templateVariables.isEmpty()) {
                errors.add("使用模板时必须提供模板变量");
            }
        }
        
        return new ValidationResult(errors.isEmpty(), errors);
    }
    
    private boolean isValidEmail(String email) {
        return email != null && email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    }
    
    @Data
    @AllArgsConstructor
    public static class ValidationResult {
        private boolean valid;
        private List<String> errors;
    }
}
```

#### EmailRequest 使用示例

```java
// 简单邮件
EmailRequest simple = EmailRequest.builder()
    .to("user@example.com")
    .subject("测试邮件")
    .text("这是测试内容")
    .build();

// 复杂邮件
EmailRequest complex = EmailRequest.builder()
    .to("user1@example.com")
    .to("user2@example.com")
    .cc("manager@example.com")
    .bcc("admin@example.com")
    .subject("重要通知")
    .html("<h1>重要通知</h1><p>请查看附件</p>")
    .attachment(new File("report.pdf"))
    .attachment(new File("data.xlsx"))
    .priority(1)
    .strategy("high_priority")
    .retryCount(5)
    .build();

// 模板邮件
EmailRequest template = EmailRequest.builder()
    .to("newuser@example.com")
    .subject("欢迎注册")
    .templateId("welcome")
    .variable("username", "张三")
    .variable("activationLink", "https://example.com/activate?token=abc123")
    .variable("companyName", "我的公司")
    .build();
```

### SendResult

邮件发送结果类，提供详细的发送状态和错误信息。

```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SendResult {
    
    // === 基础状态 ===
    
    /** 发送是否成功 */
    private boolean success;
    
    /** 发送的邮件ID */
    private String messageId;
    
    // === 时间信息 ===
    
    /** 发送开始时间 */
    private long startTime;
    
    /** 发送结束时间 */
    private long endTime;
    
    /** 发送耗时（毫秒） */
    private long duration;
    
    // === 收件人信息 ===
    
    /** 收件人列表 */
    private List<String> recipients;
    
    /** 抄送人列表 */
    private List<String> ccRecipients;
    
    /** 密送人列表 */
    private List<String> bccRecipients;
    
    /** 邮件主题 */
    private String subject;
    
    // === 错误信息 ===
    
    /** 错误信息 */
    private String errorMessage;
    
    /** 错误代码 */
    private String errorCode;
    
    /** 异常堆栈信息 */
    private String stackTrace;
    
    // === 发送详情 ===
    
    /** 使用的发送策略 */
    private String strategy;
    
    /** 重试次数 */
    private int retryCount;
    
    /** 服务器响应信息 */
    private String serverResponse;
    
    /** 附件数量 */
    private int attachmentCount;
    
    /** 邮件大小（字节） */
    private long messageSize;
    
    // === 批量发送相关 ===
    
    /** 批量发送的总数量 */
    private int batchTotal;
    
    /** 批量发送的成功数量 */
    private int batchSuccess;
    
    /** 批量发送的失败数量 */
    private int batchFailure;
    
    /** 批量发送的详细结果 */
    private List<SendResult> batchResults;
    
    // === 便捷方法 ===
    
    /**
     * 获取成功率（百分比）
     * @return 成功率
     */
    public double getSuccessRate() {
        if (batchTotal > 0) {
            return (double) batchSuccess / batchTotal * 100;
        }
        return success ? 100.0 : 0.0;
    }
    
    /**
     * 是否为批量发送
     * @return 是否批量发送
     */
    public boolean isBatchSend() {
        return batchTotal > 1;
    }
    
    /**
     * 获取格式化的耗时
     * @return 格式化耗时字符串
     */
    public String getFormattedDuration() {
        if (duration < 1000) {
            return duration + "ms";
        } else if (duration < 60000) {
            return String.format("%.2fs", duration / 1000.0);
        } else {
            return String.format("%.2fm", duration / 60000.0);
        }
    }
    
    /**
     * 获取格式化的邮件大小
     * @return 格式化大小字符串
     */
    public String getFormattedMessageSize() {
        if (messageSize < 1024) {
            return messageSize + "B";
        } else if (messageSize < 1024 * 1024) {
            return String.format("%.2fKB", messageSize / 1024.0);
        } else {
            return String.format("%.2fMB", messageSize / (1024.0 * 1024.0));
        }
    }
    
    // === 静态工厂方法 ===
    
    /**
     * 创建成功结果
     * @param messageId 消息ID
     * @return 成功结果
     */
    public static SendResult success(String messageId) {
        return SendResult.builder()
            .success(true)
            .messageId(messageId)
            .endTime(System.currentTimeMillis())
            .build();
    }
    
    /**
     * 创建失败结果
     * @param errorCode 错误代码
     * @param errorMessage 错误信息
     * @return 失败结果
     */
    public static SendResult failure(String errorCode, String errorMessage) {
        return SendResult.builder()
            .success(false)
            .errorCode(errorCode)
            .errorMessage(errorMessage)
            .endTime(System.currentTimeMillis())
            .build();
    }
}
```

## 邮件接收API

### EmailListenerApi

邮件监听器接口，用于处理接收到的邮件。

```java
public interface EmailListenerApi {
    
    /**
     * 处理接收到的邮件
     * @param message 原始邮件消息
     * @param content 解析后的邮件内容
     * @param subject 邮件主题
     * @param from 发件人
     * @return 处理结果，true表示处理成功，false表示处理失败
     */
    boolean processEmail(Message message, String content, String subject, String from);
    
    /**
     * 获取处理器名称
     * @return 处理器名称
     */
    default String getProcessorName() {
        return this.getClass().getSimpleName();
    }
    
    /**
     * 获取处理器优先级
     * @return 优先级（数值越小优先级越高）
     */
    default int getPriority() {
        return 100;
    }
    
    /**
     * 是否支持异步处理
     * @return 是否异步
     */
    default boolean isAsync() {
        return true;
    }
    
    /**
     * 是否启用该处理器
     * @return 是否启用
     */
    default boolean isEnabled() {
        return true;
    }
}
```

### EmailProcessorFunction

函数式邮件处理器接口。

```java
@FunctionalInterface
public interface EmailProcessorFunction {
    
    /**
     * 处理邮件的函数式接口
     * @param message 原始邮件消息
     * @param content 邮件内容
     * @param subject 邮件主题
     * @param from 发件人
     * @return 处理结果
     */
    String processEmail(Message message, String content, String subject, String from);
}
```

## 注解驱动API

### @EmailProcessor

邮件处理器类注解，用于标记包含邮件处理方法的类。

```java
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
public @interface EmailProcessor {
    
    /**
     * 处理器组名称
     * @return 组名称
     */
    String group() default "default";
    
    /**
     * 处理器描述
     * @return 描述信息
     */
    String description() default "";
    
    /**
     * 是否启用该处理器
     * @return 是否启用
     */
    boolean enabled() default true;
    
    /**
     * Spring Bean名称
     * @return Bean名称
     */
    String value() default "";
}
```

### @EmailHandler

邮件处理方法注解，用于标记具体的邮件处理方法。

```java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface EmailHandler {
    
    /**
     * 邮件主题匹配模式
     * @return 主题匹配模式数组
     */
    String[] subject() default {};
    
    /**
     * 主题匹配类型
     * @return 匹配类型
     */
    MatchType subjectMatchType() default MatchType.CONTAINS;
    
    /**
     * 发件人匹配模式
     * @return 发件人匹配模式数组
     */
    String[] from() default {};
    
    /**
     * 发件人匹配类型
     * @return 匹配类型
     */
    MatchType fromMatchType() default MatchType.CONTAINS;
    
    /**
     * 邮件标签匹配
     * @return 标签数组
     */
    String[] tags() default {};
    
    /**
     * 处理器优先级（数值越小优先级越高）
     * @return 优先级
     */
    int priority() default 100;
    
    /**
     * 处理器名称
     * @return 处理器名称
     */
    String name() default "";
    
    /**
     * 是否异步处理
     * @return 是否异步
     */
    boolean async() default true;
    
    /**
     * 处理器描述
     * @return 描述信息
     */
    String description() default "";
    
    /**
     * 匹配类型枚举
     */
    enum MatchType {
        /** 精确匹配 */
        EXACT,
        /** 包含匹配 */
        CONTAINS,
        /** 正则表达式匹配 */
        REGEX,
        /** 前缀匹配 */
        PREFIX,
        /** 后缀匹配 */
        SUFFIX
    }
}
```

### EmailContext

邮件处理上下文，封装邮件处理过程中的所有相关信息。

```java
@Data
@Builder
public class EmailContext {
    
    // === 基础信息 ===
    
    /** 原始邮件消息 */
    private Message message;
    
    /** 邮件ID */
    private String messageId;
    
    /** 邮件主题 */
    private String subject;
    
    /** 发件人 */
    private String from;
    
    /** 发件人（别名） */
    private String sender;
    
    // === 收件人信息 ===
    
    /** 收件人列表 */
    private List<String> to;
    
    /** 收件人列表（别名） */
    private List<String> recipients;
    
    /** 抄送列表 */
    private List<String> cc;
    
    /** 密送列表 */
    private List<String> bcc;
    
    // === 内容信息 ===
    
    /** 邮件内容（纯文本） */
    private String textContent;
    
    /** 邮件内容（HTML） */
    private String htmlContent;
    
    /** 邮件内容（通用） */
    private String content;
    
    /** 邮件标签 */
    private List<String> tags;
    
    // === 附件信息 ===
    
    /** 附件信息列表 */
    private List<AttachmentInfo> attachments;
    
    // === 时间信息 ===
    
    /** 邮件接收时间 */
    private LocalDateTime receivedTime;
    
    /** 邮件发送时间 */
    private LocalDateTime sentTime;
    
    /** 邮件接收日期（别名） */
    private LocalDateTime receivedDate;
    
    /** 邮件发送日期（别名） */
    private LocalDateTime sentDate;
    
    // === 其他信息 ===
    
    /** 邮件大小（字节） */
    private Long size;
    
    /** 邮件大小（Long类型） */
    private Long messageSize;
    
    /** 邮件优先级 */
    private Integer priority;
    
    /** 邮件标志 */
    private List<String> flags;
    
    /** 邮件头信息 */
    private Map<String, String> headers;
    
    /** 处理上下文数据 */
    private Map<String, Object> contextData;
    
    // === 附件信息类 ===
    
    @Data
    @Builder
    public static class AttachmentInfo {
        /** 附件文件名 */
        private String fileName;
        
        /** 附件大小 */
        private long size;
        
        /** 附件类型 */
        private String contentType;
        
        /** 附件内容 */
        private byte[] content;
        
        /** 是否为内联附件 */
        private boolean inline;
        
        /** 内容ID（用于内联附件） */
        private String contentId;
    }
    
    // === 便捷方法 ===
    
    /**
     * 是否包含附件
     * @return 是否有附件
     */
    public boolean hasAttachments() {
        return attachments != null && !attachments.isEmpty();
    }
    
    /**
     * 获取附件数量
     * @return 附件数量
     */
    public int getAttachmentCount() {
        return attachments != null ? attachments.size() : 0;
    }
    
    /**
     * 是否为HTML邮件
     * @return 是否HTML格式
     */
    public boolean isHtmlEmail() {
        return htmlContent != null && !htmlContent.trim().isEmpty();
    }
    
    /**
     * 获取格式化的邮件大小
     * @return 格式化大小
     */
    public String getFormattedSize() {
        if (size == null) return "未知";
        
        if (size < 1024) {
            return size + "B";
        } else if (size < 1024 * 1024) {
            return String.format("%.2fKB", size / 1024.0);
        } else {
            return String.format("%.2fMB", size / (1024.0 * 1024.0));
        }
    }
    
    /**
     * 添加上下文数据
     * @param key 键
     * @param value 值
     */
    public void putContextData(String key, Object value) {
        if (contextData == null) {
            contextData = new HashMap<>();
        }
        contextData.put(key, value);
    }
    
    /**
     * 获取上下文数据
     * @param key 键
     * @param type 类型
     * @return 值
     */
    public <T> T getContextData(String key, Class<T> type) {
        if (contextData == null) return null;
        Object value = contextData.get(key);
        return type.isInstance(value) ? type.cast(value) : null;
    }
}
```

## 配置API

### EmailSenderProperties

邮件发送服务配置属性。

```yaml
# 配置示例
email:
  sender:
    enabled: true                    # 是否启用邮件发送服务
    defaultStrategy: default         # 默认发送策略
    batchThreshold: 10              # 批量发送阈值
    highPriorityKeywords:           # 高优先级关键词
      - urgent
      - 紧急
      - 重要
      - important
    monitor:                        # 监控配置
      enabled: true
      retentionHours: 24
      healthCheckInterval: 5
      failureRateThreshold: 0.1
      responseTimeThreshold: 5000
    template:                       # 模板配置
      enabled: true
      cacheSize: 100
      cacheExpireMinutes: 60
      defaultPath: classpath:templates/email/
    event:                          # 事件配置
      enabled: true
      async: true
      threadPoolSize: 5
```

### EmailListenerProperties

邮件监听器配置属性。

```yaml
# 配置示例
email:
  listener:
    enabled: true                   # 是否启用邮件监听器
    server:                         # 邮件服务器配置
      host: imap.qq.com
      port: 993
      protocol: imaps
      username: your-email@qq.com
      password: your-auth-code
      folder: INBOX
    connection:                     # 连接配置
      timeout: 30000
      readTimeout: 60000
      writeTimeout: 30000
      poolSize: 5
    monitor:                        # 监控配置
      enabled: true
      interval: 30
      maxRetries: 3
      healthCheckInterval: 300
    listener:                       # 监听配置
      enabled: true
      interval: 10
      batchSize: 10
      markAsRead: true
      deleteAfterProcess: false
      maxProcessTime: 300
    log:                           # 日志配置
      enabled: true
      level: INFO
      logContent: false
      logAttachment: false
    attachment:                    # 附件配置
      enabled: true
      maxSize: 10485760
      allowedTypes:
        - pdf
        - doc
        - docx
        - xls
        - xlsx
        - jpg
        - png
      savePath: /tmp/email-attachments
```

### SmtpConfig

SMTP邮件发送配置。

```yaml
# 配置示例
mail:
  smtp:
    server: smtp.qq.com             # SMTP服务器地址
    port: 587                       # SMTP服务器端口
    protocol: smtp                  # 邮件协议
    username: your-email@qq.com     # 邮件账户用户名
    password: your-auth-code        # 邮件账户授权码
    connection:                     # 连接配置
      timeout: 15000
      readTimeout: 30000
      writeTimeout: 30000
    properties:                     # 邮件属性配置
      auth: true
      starttls:
        enable: true
        required: true
      ssl:
        enable: false
        trust: smtp.qq.com
      debug: false
    retry:                          # 重试配置
      maxAttempts: 3
      delay: 1000
      multiplier: 2.0
      maxDelay: 10000
    log:                           # 日志配置
      enabled: true
      level: INFO
```

## 监控API

### EmailSendMonitor

邮件发送监控器，提供发送统计和健康检查功能。

```java
public interface EmailSendMonitor {
    
    /**
     * 记录发送开始
     * @param request 邮件请求
     * @return 发送记录ID
     */
    String recordSendStart(EmailRequest request);
    
    /**
     * 记录发送成功
     * @param recordId 记录ID
     * @param result 发送结果
     */
    void recordSendSuccess(String recordId, SendResult result);
    
    /**
     * 记录发送失败
     * @param recordId 记录ID
     * @param result 发送结果
     */
    void recordSendFailure(String recordId, SendResult result);
    
    /**
     * 获取发送统计
     * @return 统计信息
     */
    Map<String, Object> getStatistics();
    
    /**
     * 获取健康状态
     * @return 健康状态
     */
    Map<String, Object> getHealthStatus();
    
    /**
     * 重置统计数据
     */
    void resetStatistics();
}
```

### 统计信息字段

```java
// getStatistics() 返回的Map包含以下字段：
Map<String, Object> stats = {
    "totalSent": 1000,              // 总发送数量
    "successCount": 950,            // 成功发送数量
    "failureCount": 50,             // 失败发送数量
    "successRate": 95.0,            // 成功率（百分比）
    "failureRate": 5.0,             // 失败率（百分比）
    "avgResponseTime": 1500,        // 平均响应时间（毫秒）
    "maxResponseTime": 5000,        // 最大响应时间（毫秒）
    "minResponseTime": 200,         // 最小响应时间（毫秒）
    "lastSendTime": "2025-01-15T10:30:00", // 最后发送时间
    "totalSize": 52428800,          // 总发送大小（字节）
    "avgSize": 52428,               // 平均邮件大小（字节）
    "hourlyStats": [...],           // 小时统计数据
    "dailyStats": [...],            // 日统计数据
    "strategyStats": {...}          // 策略统计数据
};

// getHealthStatus() 返回的Map包含以下字段：
Map<String, Object> health = {
    "healthy": true,                 // 整体健康状态
    "connectionStatus": "UP",       // 连接状态
    "lastCheckTime": "2025-01-15T10:30:00", // 最后检查时间
    "responseTime": 1200,           // 当前响应时间
    "errorRate": 2.5,               // 当前错误率
    "threadPoolStatus": {...},      // 线程池状态
    "memoryUsage": 65.5,            // 内存使用率
    "queueSize": 5                  // 队列大小
};
```

## 扩展接口

### EmailSendStrategy

邮件发送策略接口，支持自定义发送策略。

```java
public interface EmailSendStrategy {
    
    /**
     * 获取策略名称
     * @return 策略名称
     */
    String getStrategyName();
    
    /**
     * 发送邮件
     * @param request 邮件请求
     * @return 发送结果
     */
    SendResult send(EmailRequest request);
    
    /**
     * 是否支持该请求
     * @param request 邮件请求
     * @return 是否支持
     */
    default boolean supports(EmailRequest request) {
        return true;
    }
    
    /**
     * 获取策略优先级
     * @return 优先级（数值越小优先级越高）
     */
    default int getPriority() {
        return 100;
    }
}
```

### EmailTemplateEngine

邮件模板引擎接口，支持自定义模板处理。

```java
public interface EmailTemplateEngine {
    
    /**
     * 处理模板
     * @param templateId 模板ID
     * @param variables 模板变量
     * @return 处理后的内容
     */
    String processTemplate(String templateId, Map<String, Object> variables);
    
    /**
     * 是否支持该模板
     * @param templateId 模板ID
     * @return 是否支持
     */
    default boolean supports(String templateId) {
        return true;
    }
    
    /**
     * 获取引擎名称
     * @return 引擎名称
     */
    default String getEngineName() {
        return this.getClass().getSimpleName();
    }
}
```

### EmailCacheManager

邮件缓存管理器接口，支持自定义缓存实现。

```java
public interface EmailCacheManager {
    
    /**
     * 存储缓存
     * @param key 键
     * @param value 值
     * @param expireSeconds 过期时间（秒）
     */
    void put(String key, Object value, long expireSeconds);
    
    /**
     * 获取缓存
     * @param key 键
     * @param type 类型
     * @return 值
     */
    <T> T get(String key, Class<T> type);
    
    /**
     * 删除缓存
     * @param key 键
     */
    void remove(String key);
    
    /**
     * 清空缓存
     */
    void clear();
    
    /**
     * 检查缓存是否存在
     * @param key 键
     * @return 是否存在
     */
    boolean exists(String key);
}
```

## 事件API

### EmailEvent

邮件事件类，定义了邮件处理过程中的各种事件。

```java
public class EmailEvent {
    
    /**
     * 邮件发送前事件
     */
    @Data
    @AllArgsConstructor
    public static class EmailSendingEvent {
        private EmailRequest emailRequest;
        private long timestamp;
        
        public EmailSendingEvent(EmailRequest emailRequest) {
            this.emailRequest = emailRequest;
            this.timestamp = System.currentTimeMillis();
        }
    }
    
    /**
     * 邮件发送成功事件
     */
    @Data
    @AllArgsConstructor
    public static class EmailSentEvent {
        private EmailRequest emailRequest;
        private SendResult sendResult;
        private long timestamp;
        
        public EmailSentEvent(EmailRequest emailRequest, SendResult sendResult) {
            this.emailRequest = emailRequest;
            this.sendResult = sendResult;
            this.timestamp = System.currentTimeMillis();
        }
    }
    
    /**
     * 邮件发送失败事件
     */
    @Data
    @AllArgsConstructor
    public static class EmailSendFailedEvent {
        private EmailRequest emailRequest;
        private SendResult sendResult;
        private Throwable exception;
        private long timestamp;
        
        public EmailSendFailedEvent(EmailRequest emailRequest, SendResult sendResult, Throwable exception) {
            this.emailRequest = emailRequest;
            this.sendResult = sendResult;
            this.exception = exception;
            this.timestamp = System.currentTimeMillis();
        }
    }
    
    /**
     * 邮件接收事件
     */
    @Data
    @AllArgsConstructor
    public static class EmailReceivedEvent {
        private EmailContext emailContext;
        private long timestamp;
        
        public EmailReceivedEvent(EmailContext emailContext) {
            this.emailContext = emailContext;
            this.timestamp = System.currentTimeMillis();
        }
    }
    
    /**
     * 邮件处理完成事件
     */
    @Data
    @AllArgsConstructor
    public static class EmailProcessedEvent {
        private EmailContext emailContext;
        private boolean success;
        private String processorName;
        private long processingTime;
        private long timestamp;
        
        public EmailProcessedEvent(EmailContext emailContext, boolean success, String processorName, long processingTime) {
            this.emailContext = emailContext;
            this.success = success;
            this.processorName = processorName;
            this.processingTime = processingTime;
            this.timestamp = System.currentTimeMillis();
        }
    }
}
```

### 事件监听示例

```java
@Component
public class EmailEventListener {
    
    @EventListener
    public void handleEmailSending(EmailEvent.EmailSendingEvent event) {
        // 处理邮件发送前事件
    }
    
    @EventListener
    public void handleEmailSent(EmailEvent.EmailSentEvent event) {
        // 处理邮件发送成功事件
    }
    
    @EventListener
    public void handleEmailSendFailed(EmailEvent.EmailSendFailedEvent event) {
        // 处理邮件发送失败事件
    }
    
    @EventListener
    public void handleEmailReceived(EmailEvent.EmailReceivedEvent event) {
        // 处理邮件接收事件
    }
    
    @EventListener
    public void handleEmailProcessed(EmailEvent.EmailProcessedEvent event) {
        // 处理邮件处理完成事件
    }
}
```

## 工具类API

### RetryUtil

重试工具类，提供重试机制支持。

```java
public class RetryUtil {
    
    /**
     * 执行带重试的操作
     * @param operation 操作
     * @param maxAttempts 最大重试次数
     * @param delay 重试延迟（毫秒）
     * @param multiplier 延迟倍数
     * @param maxDelay 最大延迟（毫秒）
     * @return 操作结果
     */
    public static <T> T executeWithRetry(
        Supplier<T> operation,
        int maxAttempts,
        long delay,
        double multiplier,
        long maxDelay
    ) throws Exception;
    
    /**
     * 执行带重试的操作（异步）
     * @param operation 操作
     * @param maxAttempts 最大重试次数
     * @param delay 重试延迟（毫秒）
     * @param multiplier 延迟倍数
     * @param maxDelay 最大延迟（毫秒）
     * @return 异步操作结果
     */
    public static <T> CompletableFuture<T> executeWithRetryAsync(
        Supplier<T> operation,
        int maxAttempts,
        long delay,
        double multiplier,
        long maxDelay
    );
}
```

### SSLTrustUtil

SSL信任工具类，处理SSL证书信任问题。

```java
public class SSLTrustUtil {
    
    /**
     * 信任所有SSL证书（仅用于开发环境）
     */
    public static void trustAllCertificates();
    
    /**
     * 信任指定主机的SSL证书
     * @param hosts 主机列表
     */
    public static void trustHosts(String... hosts);
    
    /**
     * 验证SSL证书
     * @param host 主机
     * @param port 端口
     * @return 验证结果
     */
    public static boolean verifyCertificate(String host, int port);
}
```

## 错误代码

### 发送错误代码

| 错误代码 | 描述 | 解决方案 |
|---------|------|----------|
| `SMTP_CONNECTION_FAILED` | SMTP连接失败 | 检查服务器地址和端口 |
| `SMTP_AUTH_FAILED` | SMTP认证失败 | 检查用户名和密码 |
| `INVALID_RECIPIENT` | 无效的收件人 | 检查邮箱地址格式 |
| `MESSAGE_TOO_LARGE` | 邮件过大 | 减少附件大小或内容 |
| `ATTACHMENT_NOT_FOUND` | 附件文件不存在 | 检查附件文件路径 |
| `TEMPLATE_NOT_FOUND` | 模板不存在 | 检查模板ID和路径 |
| `TEMPLATE_PROCESS_ERROR` | 模板处理错误 | 检查模板语法和变量 |
| `SEND_TIMEOUT` | 发送超时 | 增加超时时间或检查网络 |
| `QUOTA_EXCEEDED` | 发送配额超限 | 等待配额重置或联系服务商 |
| `STRATEGY_NOT_FOUND` | 发送策略不存在 | 检查策略名称或注册策略 |

### 接收错误代码

| 错误代码 | 描述 | 解决方案 |
|---------|------|----------|
| `IMAP_CONNECTION_FAILED` | IMAP连接失败 | 检查服务器地址和端口 |
| `IMAP_AUTH_FAILED` | IMAP认证失败 | 检查用户名和密码 |
| `FOLDER_NOT_FOUND` | 邮件文件夹不存在 | 检查文件夹名称 |
| `MESSAGE_PARSE_ERROR` | 邮件解析错误 | 检查邮件格式 |
| `ATTACHMENT_SAVE_ERROR` | 附件保存失败 | 检查保存路径权限 |
| `PROCESSOR_ERROR` | 处理器执行错误 | 检查处理器逻辑 |
| `HANDLER_NOT_FOUND` | 处理器未找到 | 检查注解配置 |

### 配置错误代码

| 错误代码 | 描述 | 解决方案 |
|---------|------|----------|
| `CONFIG_MISSING` | 配置缺失 | 添加必要的配置项 |
| `CONFIG_INVALID` | 配置无效 | 检查配置格式和值 |
| `BEAN_NOT_FOUND` | Bean未找到 | 检查组件扫描和注解 |
| `CIRCULAR_DEPENDENCY` | 循环依赖 | 重构依赖关系 |

## 最佳实践

### 1. 错误处理

```java
// 推荐的错误处理方式
SendResult result = easyMailSender.send(request);
if (!result.isSuccess()) {
    switch (result.getErrorCode()) {
        case "SMTP_CONNECTION_FAILED":
            // 处理连接失败
            handleConnectionError(result);
            break;
        case "INVALID_RECIPIENT":
            // 处理收件人错误
            handleRecipientError(result);
            break;
        default:
            // 处理其他错误
            handleGenericError(result);
    }
}
```

### 2. 异步处理

```java
// 推荐的异步处理方式
CompletableFuture<SendResult> future = easyMailSender.sendAsync(request);
future.whenComplete((result, throwable) -> {
    if (throwable != null) {
        log.error("异步发送异常", throwable);
    } else if (result.isSuccess()) {
        log.info("异步发送成功: {}", result.getMessageId());
    } else {
        log.warn("异步发送失败: {}", result.getErrorMessage());
    }
});
```

### 3. 资源管理

```java
// 推荐的资源管理方式
try (FileInputStream fis = new FileInputStream(attachmentFile)) {
    EmailRequest request = EmailRequest.builder()
        .to("user@example.com")
        .subject("带附件邮件")
        .text("请查收附件")
        .attachment(attachmentFile)
        .build();
    
    SendResult result = easyMailSender.send(request);
    // 处理结果
} catch (IOException e) {
    log.error("文件处理异常", e);
}
```

### 4. 性能优化

```java
// 批量发送优化
List<EmailRequest> requests = buildEmailRequests();
if (requests.size() > 10) {
    // 使用批量发送
    SendResult result = easyMailSender.sendBatch(requests);
} else {
    // 使用普通发送
    for (EmailRequest request : requests) {
        easyMailSender.sendAsync(request);
    }
}
```

## 技术支持

如果您在使用过程中遇到问题，可以通过以下方式获取帮助：

1. **查看日志**: 启用详细日志记录，分析错误信息
2. **检查配置**: 确认所有配置项都正确设置
3. **测试连接**: 使用 `checkConnection()` 方法测试连接
4. **查看统计**: 使用监控API查看发送统计和健康状态
5. **参考示例**: 查看项目中的示例代码

---

*本文档基于 JavaEmailSpringBoot v1.0.0 编写，如有更新请参考最新版本文档。*