# EasyMail API 文档

## 目录

1. [核心接口](#核心接口)
2. [邮件发送API](#邮件发送api)
3. [邮件监听API](#邮件监听api)
4. [邮件处理API](#邮件处理api)
5. [配置API](#配置api)
6. [事件API](#事件api)
7. [工具类API](#工具类api)
8. [异常类API](#异常类api)
9. [示例代码](#示例代码)

---

## 核心接口

### EasyMailSenderService

邮件发送服务的核心接口，提供各种邮件发送功能。

```java
public interface EasyMailSenderService {
    
    /**
     * 发送邮件
     * @param request 邮件请求对象
     * @return 发送结果
     */
    EasyMailSendResult send(EasyMailRequest request);
    
    /**
     * 异步发送邮件
     * @param request 邮件请求对象
     * @return 异步发送结果
     */
    CompletableFuture<EasyMailSendResult> sendAsync(EasyMailRequest request);
    
    /**
     * 发送文本邮件
     * @param to 收件人
     * @param subject 主题
     * @param text 文本内容
     * @return 发送结果
     */
    EasyMailSendResult sendText(String to, String subject, String text);
    
    /**
     * 发送HTML邮件
     * @param to 收件人
     * @param subject 主题
     * @param html HTML内容
     * @return 发送结果
     */
    EasyMailSendResult sendHtml(String to, String subject, String html);
    
    /**
     * 批量发送邮件
     * @param recipients 收件人列表
     * @param subject 主题
     * @param content 内容
     * @param isHtml 是否为HTML格式
     * @return 发送结果
     */
    EasyMailSendResult sendToMultiple(List<String> recipients, String subject, String content, boolean isHtml);
    
    /**
     * 使用模板发送邮件
     * @param to 收件人
     * @param template 邮件模板
     * @param variables 模板变量
     * @return 发送结果
     */
    EasyMailSendResult sendWithTemplate(String to, EasyMailSendTemplate template, Map<String, Object> variables);
    
    /**
     * 检查连接状态
     * @return 连接是否正常
     */
    boolean checkConnection();
    
    /**
     * 获取发送统计信息
     * @return 统计信息字符串
     */
    String getSendingStats();
    
    /**
     * 获取详细统计信息
     * @return 详细统计信息Map
     */
    Map<String, Object> getDetailedStatistics();
    
    /**
     * 获取策略统计信息
     * @return 策略统计信息Map
     */
    Map<String, Object> getStrategyStatistics();
    
    /**
     * 获取健康状态
     * @return 健康状态Map
     */
    Map<String, Object> getHealthStatus();
    
    /**
     * 获取发送趋势
     * @return 发送趋势Map
     */
    Map<String, Object> getSendTrend();
    
    /**
     * 生成监控报告
     * @return 监控报告字符串
     */
    String generateMonitorReport();
    
    /**
     * 获取线程池状态
     * @return 线程池状态字符串
     */
    String getThreadPoolStatus();
}
```

### EasyMailListenerApi

邮件监听器接口，用于处理接收到的邮件。

```java
public interface EasyMailListenerApi {
    
    /**
     * 处理邮件
     * @param message 邮件消息对象
     * @param content 邮件内容
     * @param subject 邮件主题
     * @param from 发件人
     * @return 处理是否成功
     */
    boolean processEmail(Message message, String content, String subject, String from);
    
    /**
     * 获取处理器名称
     * @return 处理器名称
     */
    String getProcessorName();
}
```

---

## 邮件发送API

### EasyMailRequest

邮件请求对象，使用Builder模式构建。

```java
public class EasyMailRequest {
    
    // 基础字段
    private List<String> toList;           // 收件人列表
    private List<String> ccList;           // 抄送列表
    private List<String> bccList;          // 密送列表
    private String subject;                // 主题
    private String text;                   // 文本内容
    private String html;                   // HTML内容
    private List<File> attachments;        // 附件列表
    private Integer priority;              // 优先级 (1-5)
    private boolean async;                 // 是否异步发送
    
    // 模板相关
    private String templateId;             // 模板ID
    private Map<String, Object> templateVariables; // 模板变量
    
    // 发送配置
    private String charset;                // 字符编码
    private Date sendTime;                 // 发送时间
    private Map<String, String> headers;   // 自定义头部
    
    /**
     * 创建Builder
     * @return Builder实例
     */
    public static Builder builder() {
        return new Builder();
    }
    
    /**
     * 验证请求参数
     * @return 验证结果
     */
    public ValidationResult validate() {
        // 验证逻辑实现
    }
    
    /**
     * 验证并抛出异常
     * @throws EasyMailValidationException 验证失败时抛出
     */
    public void validateAndThrow() throws EasyMailValidationException {
        // 验证逻辑实现
    }
    
    /**
     * Builder类
     */
    public static class Builder {
        
        /**
         * 添加收件人
         * @param to 收件人邮箱
         * @return Builder实例
         */
        public Builder to(String to) {
            // 实现
        }
        
        /**
         * 添加收件人列表
         * @param toList 收件人列表
         * @return Builder实例
         */
        public Builder toList(List<String> toList) {
            // 实现
        }
        
        /**
         * 添加抄送
         * @param cc 抄送邮箱
         * @return Builder实例
         */
        public Builder cc(String cc) {
            // 实现
        }
        
        /**
         * 添加密送
         * @param bcc 密送邮箱
         * @return Builder实例
         */
        public Builder bcc(String bcc) {
            // 实现
        }
        
        /**
         * 设置主题
         * @param subject 邮件主题
         * @return Builder实例
         */
        public Builder subject(String subject) {
            // 实现
        }
        
        /**
         * 设置文本内容
         * @param text 文本内容
         * @return Builder实例
         */
        public Builder text(String text) {
            // 实现
        }
        
        /**
         * 设置HTML内容
         * @param html HTML内容
         * @return Builder实例
         */
        public Builder html(String html) {
            // 实现
        }
        
        /**
         * 添加附件
         * @param attachment 附件文件
         * @return Builder实例
         */
        public Builder attachment(File attachment) {
            // 实现
        }
        
        /**
         * 设置优先级
         * @param priority 优先级 (1-5，1为最高)
         * @return Builder实例
         */
        public Builder priority(Integer priority) {
            // 实现
        }
        
        /**
         * 设置是否异步发送
         * @param async 是否异步
         * @return Builder实例
         */
        public Builder async(boolean async) {
            // 实现
        }
        
        /**
         * 设置模板ID
         * @param templateId 模板ID
         * @return Builder实例
         */
        public Builder templateId(String templateId) {
            // 实现
        }
        
        /**
         * 添加模板变量
         * @param key 变量名
         * @param value 变量值
         * @return Builder实例
         */
        public Builder variable(String key, Object value) {
            // 实现
        }
        
        /**
         * 设置模板变量Map
         * @param variables 变量Map
         * @return Builder实例
         */
        public Builder templateVariables(Map<String, Object> variables) {
            // 实现
        }
        
        /**
         * 构建请求对象
         * @return EasyMailRequest实例
         */
        public EasyMailRequest build() {
            // 实现
        }
    }
    
    /**
     * 验证结果类
     */
    public static class ValidationResult {
        private boolean valid;
        private String errorMessage;
        private List<String> errors;
        
        public boolean isValid() { return valid; }
        public String getErrorMessage() { return errorMessage; }
        public List<String> getErrors() { return errors; }
    }
}
```

### EasyMailSendResult

邮件发送结果对象。

```java
public class EasyMailSendResult {
    
    private boolean success;               // 是否成功
    private String messageId;              // 消息ID
    private String errorMessage;           // 错误信息
    private long startTime;                // 开始时间
    private long endTime;                  // 结束时间
    private long duration;                 // 耗时（毫秒）
    private List<String> successRecipients; // 成功的收件人
    private List<String> failedRecipients;  // 失败的收件人
    private int successCount;              // 成功数量
    private int failedCount;               // 失败数量
    private String strategy;               // 使用的发送策略
    private Map<String, Object> metadata;  // 元数据
    
    /**
     * 创建Builder
     * @return Builder实例
     */
    public static Builder builder() {
        return new Builder();
    }
    
    /**
     * 创建成功结果
     * @param messageId 消息ID
     * @return 成功结果
     */
    public static EasyMailSendResult success(String messageId) {
        return builder()
            .success(true)
            .messageId(messageId)
            .build();
    }
    
    /**
     * 创建失败结果
     * @param errorMessage 错误信息
     * @return 失败结果
     */
    public static EasyMailSendResult failure(String errorMessage) {
        return builder()
            .success(false)
            .errorMessage(errorMessage)
            .build();
    }
    
    // Getter方法
    public boolean isSuccess() { return success; }
    public String getMessageId() { return messageId; }
    public String getErrorMessage() { return errorMessage; }
    public long getStartTime() { return startTime; }
    public long getEndTime() { return endTime; }
    public long getDuration() { return duration; }
    public List<String> getSuccessRecipients() { return successRecipients; }
    public List<String> getFailedRecipients() { return failedRecipients; }
    public int getSuccessCount() { return successCount; }
    public int getFailedCount() { return failedCount; }
    public String getStrategy() { return strategy; }
    public Map<String, Object> getMetadata() { return metadata; }
    
    /**
     * Builder类
     */
    public static class Builder {
        // Builder实现
    }
}
```

### EasyMailSendTemplate

邮件模板对象。

```java
public class EasyMailSendTemplate {
    
    private String templateId;             // 模板ID
    private String templateName;           // 模板名称
    private String description;            // 描述
    private String version;                // 版本
    private String subjectTemplate;        // 主题模板
    private String contentTemplate;        // 内容模板
    private boolean isHtml;                // 是否HTML格式
    private List<File> defaultAttachments; // 默认附件
    private Map<String, Object> defaultVariables; // 默认变量
    private Date createTime;               // 创建时间
    private Date updateTime;               // 更新时间
    
    /**
     * 创建Builder
     * @return Builder实例
     */
    public static Builder builder() {
        return new Builder();
    }
    
    /**
     * 处理模板
     * @param variables 变量Map
     * @return 处理后的模板内容
     */
    public ProcessedTemplate process(Map<String, Object> variables) {
        // 模板处理逻辑
    }
    
    // Getter方法
    public String getTemplateId() { return templateId; }
    public String getTemplateName() { return templateName; }
    public String getDescription() { return description; }
    public String getVersion() { return version; }
    public String getSubjectTemplate() { return subjectTemplate; }
    public String getContentTemplate() { return contentTemplate; }
    public boolean isHtml() { return isHtml; }
    public List<File> getDefaultAttachments() { return defaultAttachments; }
    public Map<String, Object> getDefaultVariables() { return defaultVariables; }
    public Date getCreateTime() { return createTime; }
    public Date getUpdateTime() { return updateTime; }
    
    /**
     * Builder类
     */
    public static class Builder {
        
        public Builder templateId(String templateId) { /* 实现 */ }
        public Builder templateName(String templateName) { /* 实现 */ }
        public Builder description(String description) { /* 实现 */ }
        public Builder version(String version) { /* 实现 */ }
        public Builder subjectTemplate(String subjectTemplate) { /* 实现 */ }
        public Builder contentTemplate(String contentTemplate) { /* 实现 */ }
        public Builder isHtml(boolean isHtml) { /* 实现 */ }
        public Builder defaultAttachment(File attachment) { /* 实现 */ }
        public Builder defaultVariable(String key, Object value) { /* 实现 */ }
        
        public EasyMailSendTemplate build() {
            // 构建逻辑
        }
    }
    
    /**
     * 处理后的模板类
     */
    public static class ProcessedTemplate {
        private String processedSubject;
        private String processedContent;
        
        public String getProcessedSubject() { return processedSubject; }
        public String getProcessedContent() { return processedContent; }
    }
}
```

---

## 邮件监听API

### EasyMailContext

邮件处理上下文对象，包含邮件的所有相关信息。

```java
public class EasyMailContext {
    
    private String messageId;              // 消息ID
    private String subject;                // 主题
    private String sender;                 // 发件人
    private List<String> recipients;       // 收件人列表
    private List<String> ccList;           // 抄送列表
    private List<String> bccList;          // 密送列表
    private String content;                // 邮件内容（文本）
    private String htmlContent;            // HTML内容
    private List<File> attachments;        // 附件列表
    private Date receivedDate;             // 接收时间
    private Date sentDate;                 // 发送时间
    private Map<String, String> headers;   // 邮件头部信息
    private Message originalMessage;       // 原始邮件对象
    private boolean isRead;                // 是否已读
    private boolean isImportant;           // 是否重要
    private String folder;                 // 所在文件夹
    
    // Getter方法
    public String getMessageId() { return messageId; }
    public String getSubject() { return subject; }
    public String getSender() { return sender; }
    public List<String> getRecipients() { return recipients; }
    public List<String> getCcList() { return ccList; }
    public List<String> getBccList() { return bccList; }
    public String getContent() { return content; }
    public String getHtmlContent() { return htmlContent; }
    public List<File> getAttachments() { return attachments; }
    public Date getReceivedDate() { return receivedDate; }
    public Date getSentDate() { return sentDate; }
    public Map<String, String> getHeaders() { return headers; }
    public Message getOriginalMessage() { return originalMessage; }
    public boolean isRead() { return isRead; }
    public boolean isImportant() { return isImportant; }
    public String getFolder() { return folder; }
    
    /**
     * 获取指定头部信息
     * @param headerName 头部名称
     * @return 头部值
     */
    public String getHeader(String headerName) {
        return headers.get(headerName);
    }
    
    /**
     * 检查是否包含附件
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
     * 根据文件名查找附件
     * @param fileName 文件名
     * @return 附件文件，未找到返回null
     */
    public File findAttachmentByName(String fileName) {
        if (attachments == null) return null;
        return attachments.stream()
            .filter(file -> file.getName().equals(fileName))
            .findFirst()
            .orElse(null);
    }
}
```

---

## 邮件处理API

### @EasyMailProcessor

邮件处理器类注解。

```java
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Component
public @interface EasyMailProcessor {
    
    /**
     * 处理器组名
     * @return 组名
     */
    String group() default "default";
    
    /**
     * 处理器描述
     * @return 描述
     */
    String description() default "";
    
    /**
     * 是否启用
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

### @EasyMailProcessorHandler

邮件处理方法注解。

```java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface EasyMailProcessorHandler {
    
    /**
     * 处理器名称
     * @return 名称
     */
    String name() default "";
    
    /**
     * 处理器描述
     * @return 描述
     */
    String description() default "";
    
    /**
     * 主题匹配模式
     * @return 主题模式
     */
    String subject() default "";
    
    /**
     * 主题匹配类型
     * @return 匹配类型
     */
    MatchType subjectMatchType() default MatchType.CONTAINS;
    
    /**
     * 发件人匹配模式
     * @return 发件人模式
     */
    String from() default "";
    
    /**
     * 发件人匹配类型
     * @return 匹配类型
     */
    MatchType fromMatchType() default MatchType.CONTAINS;
    
    /**
     * 收件人匹配模式
     * @return 收件人模式
     */
    String to() default "";
    
    /**
     * 收件人匹配类型
     * @return 匹配类型
     */
    MatchType toMatchType() default MatchType.CONTAINS;
    
    /**
     * 处理器优先级
     * @return 优先级（数值越大优先级越高）
     */
    int priority() default 50;
    
    /**
     * 是否异步处理
     * @return 是否异步
     */
    boolean async() default false;
    
    /**
     * 是否启用
     * @return 是否启用
     */
    boolean enabled() default true;
    
    /**
     * 匹配类型枚举
     */
    enum MatchType {
        EQUALS,         // 完全匹配
        CONTAINS,       // 包含
        STARTS_WITH,    // 开始于
        ENDS_WITH,      // 结束于
        REGEX,          // 正则表达式
        IGNORE_CASE     // 忽略大小写
    }
}
```

---

## 配置API

### EasyMailConfig

IMAP邮件配置类。

```java
@ConfigurationProperties(prefix = "mail.imap")
public class EasyMailConfig {
    
    private String server;                 // 服务器地址
    private int port = 993;                // 端口
    private String protocol = "imaps";     // 协议
    private String username;               // 用户名
    private String password;               // 密码
    private String attachmentDir;          // 附件保存目录
    
    private ConnectionConfig connection = new ConnectionConfig();
    private MonitorConfig monitor = new MonitorConfig();
    private ListenerConfig listener = new ListenerConfig();
    private LogConfig log = new LogConfig();
    
    // Getter和Setter方法
    public String getServer() { return server; }
    public void setServer(String server) { this.server = server; }
    
    public int getPort() { return port; }
    public void setPort(int port) { this.port = port; }
    
    public String getProtocol() { return protocol; }
    public void setProtocol(String protocol) { this.protocol = protocol; }
    
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    
    public String getAttachmentDir() { return attachmentDir; }
    public void setAttachmentDir(String attachmentDir) { this.attachmentDir = attachmentDir; }
    
    public ConnectionConfig getConnection() { return connection; }
    public void setConnection(ConnectionConfig connection) { this.connection = connection; }
    
    public MonitorConfig getMonitor() { return monitor; }
    public void setMonitor(MonitorConfig monitor) { this.monitor = monitor; }
    
    public ListenerConfig getListener() { return listener; }
    public void setListener(ListenerConfig listener) { this.listener = listener; }
    
    public LogConfig getLog() { return log; }
    public void setLog(LogConfig log) { this.log = log; }
    
    /**
     * 连接配置
     */
    public static class ConnectionConfig {
        private int timeout = 15000;           // 连接超时
        private int readTimeout = 30000;       // 读取超时
        private int writeTimeout = 30000;      // 写入超时
        
        // Getter和Setter方法
        public int getTimeout() { return timeout; }
        public void setTimeout(int timeout) { this.timeout = timeout; }
        
        public int getReadTimeout() { return readTimeout; }
        public void setReadTimeout(int readTimeout) { this.readTimeout = readTimeout; }
        
        public int getWriteTimeout() { return writeTimeout; }
        public void setWriteTimeout(int writeTimeout) { this.writeTimeout = writeTimeout; }
    }
    
    /**
     * 监控配置
     */
    public static class MonitorConfig {
        private int idleTimeout = 20000;       // IDLE超时
        private int keepAliveInterval = 240;   // 保活间隔
        private int reconnectDelay = 15;       // 重连延迟
        private int shortDelay = 5;            // 短延迟
        private int longDelay = 30;            // 长延迟
        private int taskTimeout = 300;         // 任务超时
        
        // Getter和Setter方法
        public int getIdleTimeout() { return idleTimeout; }
        public void setIdleTimeout(int idleTimeout) { this.idleTimeout = idleTimeout; }
        
        public int getKeepAliveInterval() { return keepAliveInterval; }
        public void setKeepAliveInterval(int keepAliveInterval) { this.keepAliveInterval = keepAliveInterval; }
        
        public int getReconnectDelay() { return reconnectDelay; }
        public void setReconnectDelay(int reconnectDelay) { this.reconnectDelay = reconnectDelay; }
        
        public int getShortDelay() { return shortDelay; }
        public void setShortDelay(int shortDelay) { this.shortDelay = shortDelay; }
        
        public int getLongDelay() { return longDelay; }
        public void setLongDelay(int longDelay) { this.longDelay = longDelay; }
        
        public int getTaskTimeout() { return taskTimeout; }
        public void setTaskTimeout(int taskTimeout) { this.taskTimeout = taskTimeout; }
    }
    
    /**
     * 监听器配置
     */
    public static class ListenerConfig {
        private int maxRetries = 20;           // 最大重试次数
        
        public int getMaxRetries() { return maxRetries; }
        public void setMaxRetries(int maxRetries) { this.maxRetries = maxRetries; }
    }
    
    /**
     * 日志配置
     */
    public static class LogConfig {
        private boolean debugEnabled = false;  // 是否启用调试日志
        
        public boolean isDebugEnabled() { return debugEnabled; }
        public void setDebugEnabled(boolean debugEnabled) { this.debugEnabled = debugEnabled; }
    }
}
```

### EasyMailSmtpConfig

SMTP邮件配置类。

```java
@ConfigurationProperties(prefix = "mail.smtp")
public class EasyMailSmtpConfig {
    
    private String server;                 // SMTP服务器
    private int port = 587;                // 端口
    private String protocol = "smtp";      // 协议
    private String username;               // 用户名
    private String password;               // 密码
    
    private ConnectionConfig connection = new ConnectionConfig();
    private Map<String, String> properties = new HashMap<>();
    private RetryConfig retry = new RetryConfig();
    private LogConfig log = new LogConfig();
    
    // Getter和Setter方法（类似EasyMailConfig）
    
    /**
     * 重试配置
     */
    public static class RetryConfig {
        private boolean enabled = true;        // 是否启用重试
        private int maxRetries = 3;           // 最大重试次数
        private long initialDelayMs = 1000;   // 初始延迟
        private long maxDelayMs = 10000;      // 最大延迟
        private boolean useExponentialBackoff = true; // 指数退避
        private double backoffMultiplier = 2.0; // 退避乘数
        
        // Getter和Setter方法
    }
}
```

---

## 事件API

### EasyMailEvent

邮件事件基类。

```java
public abstract class EasyMailEvent {
    
    private String eventId;                // 事件ID
    private long timestamp;                // 时间戳
    private String source;                 // 事件源
    private Map<String, Object> metadata;  // 元数据
    
    protected EasyMailEvent(String source) {
        this.eventId = UUID.randomUUID().toString();
        this.timestamp = System.currentTimeMillis();
        this.source = source;
        this.metadata = new HashMap<>();
    }
    
    // Getter方法
    public String getEventId() { return eventId; }
    public long getTimestamp() { return timestamp; }
    public String getSource() { return source; }
    public Map<String, Object> getMetadata() { return metadata; }
    
    /**
     * 添加元数据
     * @param key 键
     * @param value 值
     */
    public void addMetadata(String key, Object value) {
        this.metadata.put(key, value);
    }
    
    /**
     * 获取事件类型
     * @return 事件类型
     */
    public abstract String getEventType();
}
```

### EasyMailSendSuccessEvent

邮件发送成功事件。

```java
public class EasyMailSendSuccessEvent extends EasyMailEvent {
    
    private String messageId;              // 消息ID
    private List<String> recipients;       // 收件人列表
    private String subject;                // 主题
    private long duration;                 // 发送耗时
    private String strategy;               // 发送策略
    
    public EasyMailSendSuccessEvent(String source, String messageId, List<String> recipients, 
                                   String subject, long duration, String strategy) {
        super(source);
        this.messageId = messageId;
        this.recipients = recipients;
        this.subject = subject;
        this.duration = duration;
        this.strategy = strategy;
    }
    
    @Override
    public String getEventType() {
        return "EMAIL_SEND_SUCCESS";
    }
    
    // Getter方法
    public String getMessageId() { return messageId; }
    public List<String> getRecipients() { return recipients; }
    public String getSubject() { return subject; }
    public long getDuration() { return duration; }
    public String getStrategy() { return strategy; }
}
```

### EasyMailSendFailureEvent

邮件发送失败事件。

```java
public class EasyMailSendFailureEvent extends EasyMailEvent {
    
    private List<String> recipients;       // 收件人列表
    private String subject;                // 主题
    private String errorMessage;           // 错误信息
    private Throwable cause;               // 异常原因
    private int retryCount;                // 重试次数
    private Integer priority;              // 邮件优先级
    
    public EasyMailSendFailureEvent(String source, List<String> recipients, String subject, 
                                   String errorMessage, Throwable cause, int retryCount, Integer priority) {
        super(source);
        this.recipients = recipients;
        this.subject = subject;
        this.errorMessage = errorMessage;
        this.cause = cause;
        this.retryCount = retryCount;
        this.priority = priority;
    }
    
    @Override
    public String getEventType() {
        return "EMAIL_SEND_FAILURE";
    }
    
    // Getter方法
    public List<String> getRecipients() { return recipients; }
    public String getSubject() { return subject; }
    public String getErrorMessage() { return errorMessage; }
    public Throwable getCause() { return cause; }
    public int getRetryCount() { return retryCount; }
    public Integer getPriority() { return priority; }
}
```

### EasyMailReceiveEvent

邮件接收事件。

```java
public class EasyMailReceiveEvent extends EasyMailEvent {
    
    private String messageId;              // 消息ID
    private String subject;                // 主题
    private String sender;                 // 发件人
    private List<String> recipients;       // 收件人列表
    private Date receivedDate;             // 接收时间
    private boolean hasAttachments;        // 是否有附件
    private String folder;                 // 文件夹
    
    public EasyMailReceiveEvent(String source, String messageId, String subject, String sender, 
                               List<String> recipients, Date receivedDate, boolean hasAttachments, String folder) {
        super(source);
        this.messageId = messageId;
        this.subject = subject;
        this.sender = sender;
        this.recipients = recipients;
        this.receivedDate = receivedDate;
        this.hasAttachments = hasAttachments;
        this.folder = folder;
    }
    
    @Override
    public String getEventType() {
        return "EMAIL_RECEIVE";
    }
    
    // Getter方法
    public String getMessageId() { return messageId; }
    public String getSubject() { return subject; }
    public String getSender() { return sender; }
    public List<String> getRecipients() { return recipients; }
    public Date getReceivedDate() { return receivedDate; }
    public boolean isHasAttachments() { return hasAttachments; }
    public String getFolder() { return folder; }
}
```

---

## 工具类API

### EasyMailUtils

邮件工具类。

```java
public class EasyMailUtils {
    
    /**
     * 验证邮箱地址格式
     * @param email 邮箱地址
     * @return 是否有效
     */
    public static boolean isValidEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }
        String emailRegex = "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$";
        return email.matches(emailRegex);
    }
    
    /**
     * 批量验证邮箱地址
     * @param emails 邮箱地址列表
     * @return 验证结果Map，key为邮箱地址，value为是否有效
     */
    public static Map<String, Boolean> validateEmails(List<String> emails) {
        Map<String, Boolean> result = new HashMap<>();
        if (emails != null) {
            for (String email : emails) {
                result.put(email, isValidEmail(email));
            }
        }
        return result;
    }
    
    /**
     * 过滤有效的邮箱地址
     * @param emails 邮箱地址列表
     * @return 有效的邮箱地址列表
     */
    public static List<String> filterValidEmails(List<String> emails) {
        if (emails == null) {
            return new ArrayList<>();
        }
        return emails.stream()
            .filter(EasyMailUtils::isValidEmail)
            .collect(Collectors.toList());
    }
    
    /**
     * 提取邮件内容中的链接
     * @param content 邮件内容
     * @return 链接列表
     */
    public static List<String> extractLinks(String content) {
        List<String> links = new ArrayList<>();
        if (content == null) {
            return links;
        }
        
        String urlRegex = "https?://[\\w\\-._~:/?#\\[\\]@!$&'()*+,;=%]+";
        Pattern pattern = Pattern.compile(urlRegex);
        Matcher matcher = pattern.matcher(content);
        
        while (matcher.find()) {
            links.add(matcher.group());
        }
        
        return links;
    }
    
    /**
     * 提取邮件内容中的邮箱地址
     * @param content 邮件内容
     * @return 邮箱地址列表
     */
    public static List<String> extractEmails(String content) {
        List<String> emails = new ArrayList<>();
        if (content == null) {
            return emails;
        }
        
        String emailRegex = "[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}";
        Pattern pattern = Pattern.compile(emailRegex);
        Matcher matcher = pattern.matcher(content);
        
        while (matcher.find()) {
            emails.add(matcher.group());
        }
        
        return emails;
    }
    
    /**
     * 清理HTML标签
     * @param html HTML内容
     * @return 纯文本内容
     */
    public static String stripHtml(String html) {
        if (html == null) {
            return null;
        }
        return html.replaceAll("<[^>]+>", "").trim();
    }
    
    /**
     * 生成邮件ID
     * @return 唯一的邮件ID
     */
    public static String generateMessageId() {
        return UUID.randomUUID().toString() + "@easymail";
    }
    
    /**
     * 格式化文件大小
     * @param bytes 字节数
     * @return 格式化后的大小字符串
     */
    public static String formatFileSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return String.format("%.1f KB", bytes / 1024.0);
        } else if (bytes < 1024 * 1024 * 1024) {
            return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
        } else {
            return String.format("%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0));
        }
    }
    
    /**
     * 检查文件类型是否为图片
     * @param fileName 文件名
     * @return 是否为图片
     */
    public static boolean isImageFile(String fileName) {
        if (fileName == null) {
            return false;
        }
        String extension = getFileExtension(fileName).toLowerCase();
        return Arrays.asList("jpg", "jpeg", "png", "gif", "bmp", "webp").contains(extension);
    }
    
    /**
     * 获取文件扩展名
     * @param fileName 文件名
     * @return 扩展名
     */
    public static String getFileExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf(".") + 1);
    }
    
    /**
     * 转义HTML特殊字符
     * @param text 文本
     * @return 转义后的文本
     */
    public static String escapeHtml(String text) {
        if (text == null) {
            return null;
        }
        return text.replace("&", "&amp;")
                  .replace("<", "&lt;")
                  .replace(">", "&gt;")
                  .replace("\"", "&quot;")
                  .replace("'", "&#39;");
    }
}
```

---

## 异常类API

### EasyMailException

邮件异常基类。

```java
public class EasyMailException extends RuntimeException {
    
    private String errorCode;              // 错误代码
    private Map<String, Object> context;   // 异常上下文
    
    public EasyMailException(String message) {
        super(message);
        this.context = new HashMap<>();
    }
    
    public EasyMailException(String message, Throwable cause) {
        super(message, cause);
        this.context = new HashMap<>();
    }
    
    public EasyMailException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
        this.context = new HashMap<>();
    }
    
    public EasyMailException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.context = new HashMap<>();
    }
    
    // Getter和Setter方法
    public String getErrorCode() { return errorCode; }
    public void setErrorCode(String errorCode) { this.errorCode = errorCode; }
    
    public Map<String, Object> getContext() { return context; }
    public void setContext(Map<String, Object> context) { this.context = context; }
    
    /**
     * 添加上下文信息
     * @param key 键
     * @param value 值
     * @return 当前异常实例
     */
    public EasyMailException addContext(String key, Object value) {
        this.context.put(key, value);
        return this;
    }
}
```

### EasyMailSendException

邮件发送异常。

```java
public class EasyMailSendException extends EasyMailException {
    
    private List<String> failedRecipients;  // 失败的收件人
    private String messageId;               // 消息ID
    private int retryCount;                 // 重试次数
    
    public EasyMailSendException(String message) {
        super(message);
    }
    
    public EasyMailSendException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public EasyMailSendException(String errorCode, String message, List<String> failedRecipients) {
        super(errorCode, message);
        this.failedRecipients = failedRecipients;
    }
    
    // Getter和Setter方法
    public List<String> getFailedRecipients() { return failedRecipients; }
    public void setFailedRecipients(List<String> failedRecipients) { this.failedRecipients = failedRecipients; }
    
    public String getMessageId() { return messageId; }
    public void setMessageId(String messageId) { this.messageId = messageId; }
    
    public int getRetryCount() { return retryCount; }
    public void setRetryCount(int retryCount) { this.retryCount = retryCount; }
}
```

### EasyMailValidationException

邮件验证异常。

```java
public class EasyMailValidationException extends EasyMailException {
    
    private List<String> errors;            // 验证错误列表
    private String field;                   // 错误字段
    
    public EasyMailValidationException(String message) {
        super(message);
        this.errors = new ArrayList<>();
    }
    
    public EasyMailValidationException(String message, List<String> errors) {
        super(message);
        this.errors = errors != null ? errors : new ArrayList<>();
    }
    
    public EasyMailValidationException(String field, String message) {
        super(message);
        this.field = field;
        this.errors = new ArrayList<>();
    }
    
    // Getter和Setter方法
    public List<String> getErrors() { return errors; }
    public void setErrors(List<String> errors) { this.errors = errors; }
    
    public String getField() { return field; }
    public void setField(String field) { this.field = field; }
    
    /**
     * 添加验证错误
     * @param error 错误信息
     */
    public void addError(String error) {
        this.errors.add(error);
    }
    
    /**
     * 是否有验证错误
     * @return 是否有错误
     */
    public boolean hasErrors() {
        return errors != null && !errors.isEmpty();
    }
}
```

### EasyMailConfigException

邮件配置异常。

```java
public class EasyMailConfigException extends EasyMailException {
    
    private String configKey;               // 配置键
    private Object configValue;             // 配置值
    
    public EasyMailConfigException(String message) {
        super(message);
    }
    
    public EasyMailConfigException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public EasyMailConfigException(String configKey, String message) {
        super(message);
        this.configKey = configKey;
    }
    
    public EasyMailConfigException(String configKey, Object configValue, String message) {
        super(message);
        this.configKey = configKey;
        this.configValue = configValue;
    }
    
    // Getter和Setter方法
    public String getConfigKey() { return configKey; }
    public void setConfigKey(String configKey) { this.configKey = configKey; }
    
    public Object getConfigValue() { return configValue; }
    public void setConfigValue(Object configValue) { this.configValue = configValue; }
}
```

---

## 示例代码

### 基础邮件发送示例

```java
@RestController
@RequestMapping("/api/email")
public class EmailController {
    
    @Autowired
    private EasyMailSenderService easyMailSenderService;
    
    /**
     * 发送简单文本邮件
     */
    @PostMapping("/send-text")
    public ResponseEntity<String> sendTextEmail(@RequestBody EmailRequest request) {
        try {
            EasyMailSendResult result = easyMailSenderService.sendText(
                request.getTo(),
                request.getSubject(),
                request.getContent()
            );
            
            if (result.isSuccess()) {
                return ResponseEntity.ok("邮件发送成功，ID: " + result.getMessageId());
            } else {
                return ResponseEntity.badRequest().body("邮件发送失败: " + result.getErrorMessage());
            }
        } catch (Exception e) {
            return ResponseEntity.status(500).body("邮件发送异常: " + e.getMessage());
        }
    }
    
    /**
     * 发送HTML邮件
     */
    @PostMapping("/send-html")
    public ResponseEntity<String> sendHtmlEmail(@RequestBody EmailRequest request) {
        EasyMailRequest mailRequest = EasyMailRequest.builder()
            .to(request.getTo())
            .subject(request.getSubject())
            .html(request.getContent())
            .priority(request.getPriority())
            .build();
        
        EasyMailSendResult result = easyMailSenderService.send(mailRequest);
        
        return result.isSuccess() 
            ? ResponseEntity.ok("HTML邮件发送成功")
            : ResponseEntity.badRequest().body("HTML邮件发送失败: " + result.getErrorMessage());
    }
    
    /**
     * 异步发送邮件
     */
    @PostMapping("/send-async")
    public ResponseEntity<String> sendAsyncEmail(@RequestBody EmailRequest request) {
        EasyMailRequest mailRequest = EasyMailRequest.builder()
            .to(request.getTo())
            .subject(request.getSubject())
            .text(request.getContent())
            .async(true)
            .build();
        
        CompletableFuture<EasyMailSendResult> future = easyMailSenderService.sendAsync(mailRequest);
        
        future.thenAccept(result -> {
            if (result.isSuccess()) {
                log.info("异步邮件发送成功: {}", result.getMessageId());
            } else {
                log.error("异步邮件发送失败: {}", result.getErrorMessage());
            }
        });
        
        return ResponseEntity.ok("邮件已提交异步发送");
    }
    
    /**
     * 批量发送邮件
     */
    @PostMapping("/send-batch")
    public ResponseEntity<String> sendBatchEmail(@RequestBody BatchEmailRequest request) {
        EasyMailSendResult result = easyMailSenderService.sendToMultiple(
            request.getRecipients(),
            request.getSubject(),
            request.getContent(),
            request.isHtml()
        );
        
        return ResponseEntity.ok(String.format(
            "批量邮件发送完成，成功: %d, 失败: %d",
            result.getSuccessCount(),
            result.getFailedCount()
        ));
    }
}
```

### 邮件处理器示例

```java
@Component
@EasyMailProcessor(group = "business", description = "业务邮件处理器")
public class BusinessEmailProcessor {
    
    private static final Logger log = LoggerFactory.getLogger(BusinessEmailProcessor.class);
    
    @Autowired
    private OrderService orderService;
    
    @Autowired
    private NotificationService notificationService;
    
    /**
     * 处理订单确认邮件
     */
    @EasyMailProcessorHandler(
        name = "orderConfirmationHandler",
        description = "处理订单确认邮件",
        subject = "(?i).*(订单确认|order confirmation).*",
        subjectMatchType = EasyMailProcessorHandler.MatchType.REGEX,
        priority = 100
    )
    public void handleOrderConfirmation(EasyMailContext context) {
        log.info("处理订单确认邮件: {}", context.getSubject());
        
        try {
            // 提取订单号
            String orderNumber = extractOrderNumber(context.getContent());
            if (orderNumber != null) {
                // 确认订单
                orderService.confirmOrder(orderNumber);
                log.info("订单确认成功: {}", orderNumber);
            } else {
                log.warn("无法从邮件中提取订单号: {}", context.getSubject());
            }
        } catch (Exception e) {
            log.error("处理订单确认邮件失败", e);
        }
    }
    
    /**
     * 处理支付通知邮件
     */
    @EasyMailProcessorHandler(
        name = "paymentNotificationHandler",
        description = "处理支付通知邮件",
        subject = "(?i).*(支付|payment|付款).*",
        subjectMatchType = EasyMailProcessorHandler.MatchType.REGEX,
        from = ".*@payment\\..*",
        fromMatchType = EasyMailProcessorHandler.MatchType.REGEX,
        priority = 95,
        async = true
    )
    public void handlePaymentNotification(EasyMailContext context) {
        log.info("处理支付通知邮件: {}", context.getSubject());
        
        try {
            // 提取支付信息
            PaymentInfo paymentInfo = extractPaymentInfo(context.getContent());
            if (paymentInfo != null) {
                // 异步处理支付信息
                orderService.processPayment(paymentInfo);
                log.info("支付信息处理成功: {}", paymentInfo.getTransactionId());
            }
        } catch (Exception e) {
            log.error("处理支付通知邮件失败", e);
        }
    }
    
    /**
     * 处理客服邮件
     */
    @EasyMailProcessorHandler(
        name = "customerServiceHandler",
        description = "处理客服邮件",
        subject = "(?i).*(客服|support|help|问题).*",
        subjectMatchType = EasyMailProcessorHandler.MatchType.REGEX,
        priority = 90
    )
    public void handleCustomerService(EasyMailContext context) {
        log.info("收到客服邮件: {}", context.getSubject());
        
        // 创建客服工单
        CustomerTicket ticket = new CustomerTicket();
        ticket.setSubject(context.getSubject());
        ticket.setContent(context.getContent());
        ticket.setCustomerEmail(context.getSender());
        ticket.setReceivedDate(context.getReceivedDate());
        
        // 如果有附件，保存附件信息
        if (context.hasAttachments()) {
            ticket.setAttachments(context.getAttachments());
        }
        
        // 保存工单
        customerService.createTicket(ticket);
        
        // 发送自动回复
        sendAutoReply(context.getSender(), ticket.getTicketId());
    }
    
    private String extractOrderNumber(String content) {
        Pattern pattern = Pattern.compile("(?i)订单号[：:]?\\s*([A-Z0-9]{8,20})");
        Matcher matcher = pattern.matcher(content);
        return matcher.find() ? matcher.group(1) : null;
    }
    
    private PaymentInfo extractPaymentInfo(String content) {
        // 实现支付信息提取逻辑
        return null;
    }
    
    private void sendAutoReply(String customerEmail, String ticketId) {
        try {
            EasyMailRequest autoReply = EasyMailRequest.builder()
                .to(customerEmail)
                .subject("您的问题已收到 - 工单号: " + ticketId)
                .html(buildAutoReplyContent(ticketId))
                .build();
            
            easyMailSenderService.send(autoReply);
        } catch (Exception e) {
            log.error("发送自动回复失败", e);
        }
    }
    
    private String buildAutoReplyContent(String ticketId) {
        return "<html><body>" +
               "<h2>感谢您的咨询</h2>" +
               "<p>我们已收到您的问题，工单号为: <strong>" + ticketId + "</strong></p>" +
               "<p>我们会在24小时内回复您，请耐心等待。</p>" +
               "</body></html>";
    }
}
```

### 模板邮件示例

```java
@Service
public class TemplateEmailService {
    
    @Autowired
    private EasyMailSenderService easyMailSenderService;
    
    @Autowired
    private EasyMailSendTemplateManager templateManager;
    
    /**
     * 发送验证码邮件
     */
    public void sendVerificationCode(String userEmail, String userName, String code) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("userName", userName);
        variables.put("code", code);
        variables.put("expireMinutes", 10);
        variables.put("appName", "我的应用");
        
        EasyMailRequest request = EasyMailRequest.builder()
            .to(userEmail)
            .templateId("verification")
            .templateVariables(variables)
            .priority(1) // 高优先级
            .build();
        
        EasyMailSendResult result = easyMailSenderService.send(request);
        
        if (result.isSuccess()) {
            log.info("验证码邮件发送成功: {}", result.getMessageId());
        } else {
            log.error("验证码邮件发送失败: {}", result.getErrorMessage());
        }
    }
    
    /**
     * 发送欢迎邮件
     */
    public void sendWelcomeEmail(String userEmail, String userName, String loginUrl) {
        EasyMailSendTemplate template = templateManager.getTemplate("welcome");
        
        Map<String, Object> variables = new HashMap<>();
        variables.put("userName", userName);
        variables.put("companyName", "示例公司");
        variables.put("loginUrl", loginUrl);
        
        EasyMailSendResult result = easyMailSenderService.sendWithTemplate(
            userEmail, template, variables
        );
        
        log.info("欢迎邮件发送结果: {}", result.isSuccess() ? "成功" : "失败");
    }
    
    /**
     * 创建和注册模板
     */
    @PostConstruct
    public void initTemplates() {
        // 验证码模板
        EasyMailSendTemplate verificationTemplate = EasyMailSendTemplate.builder()
            .templateId("verification")
            .templateName("验证码邮件")
            .subjectTemplate("您的验证码 - ${appName}")
            .contentTemplate(
                "<html><body>" +
                "<div style='font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;'>" +
                "<h2 style='color: #333;'>验证码</h2>" +
                "<p>您好 <strong>${userName}</strong>，</p>" +
                "<p>您的验证码是：</p>" +
                "<div style='background: #f5f5f5; padding: 20px; text-align: center; margin: 20px 0;'>" +
                "<span style='font-size: 24px; font-weight: bold; color: #007bff;'>${code}</span>" +
                "</div>" +
                "<p>验证码有效期为 <strong>${expireMinutes}</strong> 分钟，请及时使用。</p>" +
                "<p style='color: #666; font-size: 12px;'>如果您没有请求此验证码，请忽略此邮件。</p>" +
                "</div>" +
                "</body></html>"
            )
            .isHtml(true)
            .description("用户验证码邮件模板")
            .build();
        
        templateManager.registerTemplate(verificationTemplate);
        
        // 欢迎邮件模板
        EasyMailSendTemplate welcomeTemplate = EasyMailSendTemplate.builder()
            .templateId("welcome")
            .templateName("欢迎邮件")
            .subjectTemplate("欢迎加入 ${companyName}")
            .contentTemplate(
                "<html><body>" +
                "<div style='font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;'>" +
                "<h1 style='color: #007bff;'>欢迎 ${userName}！</h1>" +
                "<p>感谢您注册 <strong>${companyName}</strong>，您的账号已成功激活。</p>" +
                "<div style='background: #e7f3ff; padding: 15px; border-radius: 5px; margin: 20px 0;'>" +
                "<p><strong>登录信息：</strong></p>" +
                "<p>登录地址：<a href='${loginUrl}' style='color: #007bff;'>${loginUrl}</a></p>" +
                "</div>" +
                "<p>如有任何问题，请随时联系我们的客服团队。</p>" +
                "<p>祝您使用愉快！</p>" +
                "</div>" +
                "</body></html>"
            )
            .isHtml(true)
            .description("用户欢迎邮件模板")
            .build();
        
        templateManager.registerTemplate(welcomeTemplate);
    }
}
```

### 邮件监听器示例

```java
@Component
public class CustomEmailListener implements EasyMailListenerApi {
    
    private static final Logger log = LoggerFactory.getLogger(CustomEmailListener.class);
    
    @Autowired
    private EmailProcessingService emailProcessingService;
    
    @Override
    public boolean processEmail(Message message, String content, String subject, String from) {
        log.info("收到新邮件 - 主题: {}, 发件人: {}", subject, from);
        
        try {
            // 根据邮件类型进行分类处理
            EmailType emailType = classifyEmail(subject, from, content);
            
            switch (emailType) {
                case VERIFICATION:
                    return handleVerificationEmail(message, content, subject, from);
                case ORDER:
                    return handleOrderEmail(message, content, subject, from);
                case SUPPORT:
                    return handleSupportEmail(message, content, subject, from);
                case NOTIFICATION:
                    return handleNotificationEmail(message, content, subject, from);
                default:
                    return handleDefaultEmail(message, content, subject, from);
            }
            
        } catch (Exception e) {
            log.error("邮件处理异常 - 主题: {}, 发件人: {}", subject, from, e);
            return false;
        }
    }
    
    @Override
    public String getProcessorName() {
        return "自定义邮件监听器";
    }
    
    private EmailType classifyEmail(String subject, String from, String content) {
        if (subject.contains("验证码") || subject.contains("verification")) {
            return EmailType.VERIFICATION;
        } else if (subject.contains("订单") || subject.contains("order")) {
            return EmailType.ORDER;
        } else if (subject.contains("客服") || subject.contains("support")) {
            return EmailType.SUPPORT;
        } else if (from.contains("noreply") || from.contains("notification")) {
            return EmailType.NOTIFICATION;
        }
        return EmailType.OTHER;
    }
    
    private boolean handleVerificationEmail(Message message, String content, String subject, String from) {
        log.info("处理验证码邮件: {}", subject);
        
        // 提取验证码
        String code = extractVerificationCode(content);
        if (code != null) {
            // 存储验证码到缓存
            emailProcessingService.storeVerificationCode(from, code);
            return true;
        }
        
        return false;
    }
    
    private boolean handleOrderEmail(Message message, String content, String subject, String from) {
        log.info("处理订单邮件: {}", subject);
        
        // 提取订单信息
        OrderInfo orderInfo = extractOrderInfo(content);
        if (orderInfo != null) {
            emailProcessingService.processOrderInfo(orderInfo);
            return true;
        }
        
        return false;
    }
    
    private boolean handleSupportEmail(Message message, String content, String subject, String from) {
        log.info("处理客服邮件: {}", subject);
        
        // 创建客服工单
        emailProcessingService.createSupportTicket(subject, content, from);
        return true;
    }
    
    private boolean handleNotificationEmail(Message message, String content, String subject, String from) {
        log.info("处理通知邮件: {}", subject);
        
        // 记录通知信息
        emailProcessingService.recordNotification(subject, content, from);
        return true;
    }
    
    private boolean handleDefaultEmail(Message message, String content, String subject, String from) {
        log.info("处理默认邮件: {}", subject);
        
        // 记录未分类邮件
        emailProcessingService.recordUnclassifiedEmail(subject, content, from);
        return true;
    }
    
    private String extractVerificationCode(String content) {
        Pattern pattern = Pattern.compile("\\b(\\d{4,6})\\b");
        Matcher matcher = pattern.matcher(content);
        return matcher.find() ? matcher.group(1) : null;
    }
    
    private OrderInfo extractOrderInfo(String content) {
        // 实现订单信息提取逻辑
        Pattern orderPattern = Pattern.compile("订单号[：:]?\\s*([A-Z0-9]{8,20})");
        Matcher matcher = orderPattern.matcher(content);
        
        if (matcher.find()) {
            OrderInfo orderInfo = new OrderInfo();
            orderInfo.setOrderNumber(matcher.group(1));
            // 提取其他订单信息...
            return orderInfo;
        }
        
        return null;
    }
    
    private enum EmailType {
        VERIFICATION, ORDER, SUPPORT, NOTIFICATION, OTHER
    }
}
```

### 邮件服务控制示例

```java
@RestController
@RequestMapping("/api/email-service")
public class EmailServiceController {
    
    @Autowired
    private EasyMailService easyMailService;
    
    @Autowired
    private EasyMailSenderService senderService;
    
    /**
     * 启动邮件监听服务
     */
    @PostMapping("/start")
    public ResponseEntity<Map<String, Object>> startEmailService() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            boolean success = easyMailService.startMailMonitoring();
            response.put("success", success);
            response.put("message", success ? "邮件服务启动成功" : "邮件服务启动失败");
            response.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "邮件服务启动异常: " + e.getMessage());
            response.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.status(500).body(response);
        }
    }
    
    /**
     * 停止邮件监听服务
     */
    @PostMapping("/stop")
    public ResponseEntity<Map<String, Object>> stopEmailService() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            easyMailService.stopMailMonitoring();
            response.put("success", true);
            response.put("message", "邮件服务已停止");
            response.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "停止邮件服务异常: " + e.getMessage());
            response.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.status(500).body(response);
        }
    }
    
    /**
     * 获取服务状态
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getServiceStatus() {
        Map<String, Object> status = new HashMap<>();
        
        try {
            // 基础状态信息
            status.put("running", easyMailService.isMailServiceRunning());
            status.put("connectionStatus", senderService.checkConnection());
            status.put("timestamp", System.currentTimeMillis());
            
            // 处理统计信息
            Map<String, Object> processingStats = easyMailService.getMailProcessingStats();
            status.put("processingStats", processingStats);
            
            // 发送统计信息
            Map<String, Object> sendingStats = senderService.getDetailedStatistics();
            status.put("sendingStats", sendingStats);
            
            // 健康状态
            Map<String, Object> healthStatus = senderService.getHealthStatus();
            status.put("healthStatus", healthStatus);
            
            return ResponseEntity.ok(status);
        } catch (Exception e) {
            status.put("error", "获取状态信息异常: " + e.getMessage());
            return ResponseEntity.status(500).body(status);
        }
    }
    
    /**
     * 获取详细统计信息
     */
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getDetailedStatistics() {
        Map<String, Object> statistics = new HashMap<>();
        
        try {
            // 发送统计
            statistics.put("sendingStats", senderService.getDetailedStatistics());
            statistics.put("strategyStats", senderService.getStrategyStatistics());
            statistics.put("sendTrend", senderService.getSendTrend());
            
            // 线程池状态
            statistics.put("threadPoolStatus", senderService.getThreadPoolStatus());
            
            // 监控报告
            statistics.put("monitorReport", senderService.generateMonitorReport());
            
            return ResponseEntity.ok(statistics);
        } catch (Exception e) {
            statistics.put("error", "获取统计信息异常: " + e.getMessage());
            return ResponseEntity.status(500).body(statistics);
        }
    }
    
    /**
     * 重启邮件服务
     */
    @PostMapping("/restart")
    public ResponseEntity<Map<String, Object>> restartEmailService() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // 先停止服务
            easyMailService.stopMailMonitoring();
            Thread.sleep(2000); // 等待2秒
            
            // 再启动服务
            boolean success = easyMailService.startMailMonitoring();
            
            response.put("success", success);
            response.put("message", success ? "邮件服务重启成功" : "邮件服务重启失败");
            response.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "邮件服务重启异常: " + e.getMessage());
            response.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.status(500).body(response);
        }
    }
}
```

---

## 常用配置示例

### 完整的application.yml配置

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
    attachment-dir: D:\\
    
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
    
    # 监听器配置
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
    
    # 连接配置
    connection:
      timeout: 15000
      read-timeout: 30000
      write-timeout: 30000
    
    # SMTP属性配置
    properties:
      mail-smtp-auth: true
      mail-smtp-starttls-enable: false
      mail-smtp-ssl-enable: true
      mail-smtp-ssl-trust: smtp.163.com
    
    # 重试配置
    retry:
      enabled: true
      max-retries: 3
      initial-delay-ms: 1000
      max-delay-ms: 10000
      use-exponential-backoff: true
      backoff-multiplier: 2.0
    
    # 日志配置
    log:
      debug-enabled: false

# 注解驱动邮件处理器配置
annotation-driven-email-processor:
  enabled: true
  scan:
    packages:
      - com.example.processor
      - com.example.handler
  
  # 处理器配置
  processor:
    default-async: false
    thread-pool:
      core-size: 5
      max-size: 20
      queue-capacity: 100
      keep-alive: 60

# EasyMail框架配置
easymail:
  # 线程池配置
  thread-pool:
    core-pool-size: 5
    maximum-pool-size: 20
    keep-alive-time: 60
    queue-capacity: 1000
    thread-name-prefix: "EasyMail-"
    rejection-policy: "CallerRuns"
  
  # 缓存配置
  cache:
    enabled: true
    type: "memory" # memory, redis
    expire-seconds: 3600
  
  # 监控配置
  monitor:
    enabled: true
    metrics-enabled: true
    health-check-interval: 30
  
  # 安全配置
  security:
    max-attachment-size: 10485760 # 10MB
    allowed-attachment-types:
      - pdf
      - doc
      - docx
      - xls
      - xlsx
      - jpg
      - jpeg
      - png
      - gif

# Spring Boot配置
spring:
  application:
    name: easymail-demo
  
  # 数据源配置（如果需要持久化）
  datasource:
    url: jdbc:mysql://localhost:3306/easymail?useUnicode=true&characterEncoding=utf8&useSSL=false
    username: root
    password: password
    driver-class-name: com.mysql.cj.jdbc.Driver
  
  # JPA配置
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: false
    properties:
      hibernate:
        dialect: org.hibernate.dialect.MySQL8Dialect
  
  # Redis配置（如果使用Redis缓存）
  redis:
    host: localhost
    port: 6379
    password:
    database: 0
    timeout: 3000
    jedis:
      pool:
        max-active: 20
        max-idle: 10
        min-idle: 0

# 日志配置
logging:
  level:
    cn.sunyblog.easymail: INFO
    org.springframework.mail: WARN
    javax.mail: WARN
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"
    file: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"
  file:
    name: logs/easymail.log
    max-size: 100MB
    max-history: 30
```

---

## API响应格式

### 统一响应格式

```java
public class ApiResponse<T> {
    private boolean success;
    private String message;
    private T data;
    private String errorCode;
    private long timestamp;
    
    // 成功响应
    public static <T> ApiResponse<T> success(T data) {
        ApiResponse<T> response = new ApiResponse<>();
        response.setSuccess(true);
        response.setData(data);
        response.setTimestamp(System.currentTimeMillis());
        return response;
    }
    
    // 失败响应
    public static <T> ApiResponse<T> error(String message) {
        ApiResponse<T> response = new ApiResponse<>();
        response.setSuccess(false);
        response.setMessage(message);
        response.setTimestamp(System.currentTimeMillis());
        return response;
    }
    
    // Getter和Setter方法
}
```

### 常见响应示例

```json
// 邮件发送成功响应
{
  "success": true,
  "message": "邮件发送成功",
  "data": {
    "messageId": "abc123-def456-ghi789",
    "duration": 1250,
    "successCount": 1,
    "failedCount": 0,
    "strategy": "default"
  },
  "timestamp": 1640995200000
}

// 邮件发送失败响应
{
  "success": false,
  "message": "邮件发送失败: 收件人地址无效",
  "errorCode": "INVALID_RECIPIENT",
  "timestamp": 1640995200000
}

// 服务状态响应
{
  "success": true,
  "data": {
    "running": true,
    "connectionStatus": true,
    "processingStats": {
      "totalProcessed": 1250,
      "successCount": 1200,
      "failedCount": 50,
      "averageProcessingTime": 150
    },
    "sendingStats": {
      "totalSent": 980,
      "totalFailed": 20,
      "averageDuration": 1200,
      "successRate": 98.0
    },
    "healthStatus": {
      "status": "HEALTHY",
      "lastCheck": 1640995200000,
      "issues": []
    }
  },
  "timestamp": 1640995200000
}
```

---

## 总结

本API文档详细介绍了EasyMail框架的所有核心接口和类，包括：

1. **核心接口**：邮件发送服务和监听器接口
2. **邮件发送API**：请求对象、结果对象、模板对象
3. **邮件监听API**：上下文对象和处理器注解
4. **配置API**：IMAP和SMTP配置类
5. **事件API**：各种邮件事件类
6. **工具类API**：邮件处理工具方法
7. **异常类API**：各种异常类型
8. **示例代码**：完整的使用示例

通过本文档，开发者可以：
- 快速了解所有可用的API
- 学习如何正确使用各个接口
- 参考示例代码进行开发
- 了解配置选项和最佳实践

建议开发者在使用时结合操作使用文档一起参考，以获得最佳的开发体验。