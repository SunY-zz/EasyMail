# JavaEmailSpringBoot

一个功能强大、易于集成的Java邮件处理库，基于Spring Boot，提供邮件监听、处理和发送功能。

[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![Java](https://img.shields.io/badge/Java-8%2B-orange.svg)](https://www.oracle.com/java/technologies/javase-downloads.html)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-2.x-green.svg)](https://spring.io/projects/spring-boot)

## 功能特点

### 核心功能
- **邮件监听**：自动监听新邮件，支持IMAP协议，实时处理
- **邮件处理**：灵活的邮件内容解析，支持文本、HTML和附件处理
- **邮件发送**：支持发送文本邮件、HTML邮件和带附件的邮件

### 高级特性
- **多种处理方式**：接口实现、函数式处理、工具类方式，满足不同场景需求
- **自动重连**：智能重连和错误恢复机制，保证服务稳定性
- **线程池管理**：高效的线程池处理并发邮件，可配置线程数和队列容量
- **SSL支持**：内置SSL证书信任机制，轻松处理各类邮件服务器
- **事件机制**：基于Spring事件机制，支持邮件事件发布和订阅
- **缓存去重**：内置邮件缓存，避免重复处理同一邮件
- **批量发送**：支持批量发送邮件，提高处理效率
- **自动配置**：Spring Boot自动配置，开箱即用

## 快速开始

### 1. 添加依赖

在你的项目的`pom.xml`文件中添加以下依赖：

```xml
<dependency>
    <groupId>cn.sunyblog</groupId>
    <artifactId>email-listener-spring-boot-starter</artifactId>
    <version>1.0.0</version>
</dependency>
```

或者在`build.gradle`中：

```groovy
implementation 'cn.sunyblog:email-listener-spring-boot-starter:1.0.0'
```

### 2. 配置邮件服务器

在你的`application.yml`或`application.properties`文件中添加以下配置：

```yaml
email:
  listener:
    enabled: true
    server:
      host: imap.example.com
      port: 993
      protocol: imaps
      username: your-email@example.com
      password: your-password
      folder: INBOX
    attachment:
      save-dir: /path/to/save/attachments
```

### 3. 处理邮件（三种方式）

#### 方式一：实现接口

创建一个类实现`EmailListenerApi`接口，用于处理接收到的邮件：

```java
import cn.sunyblog.javaemaildemo.api.EmailListenerApi;
import org.springframework.stereotype.Component;

import javax.mail.Message;

@Component
public class MyEmailProcessor implements EmailListenerApi {
    
    @Override
    public boolean processEmail(Message message, String content, String subject, String from) {
        System.out.println("收到新邮件：" + subject);
        System.out.println("发件人：" + from);
        System.out.println("内容：" + content);
        
        // 在这里处理邮件内容，例如提取验证码、处理订单通知等
        
        return true; // 返回true表示处理成功
    }
    
    @Override
    public String getProcessorName() {
        return "MyEmailProcessor"; // 自定义处理器名称
    }
}
```

#### 方式二：函数式处理（更灵活）

使用函数式接口处理邮件，无需实现接口：

```java
import cn.sunyblog.javaemaildemo.mail.MailProcessor;
import cn.sunyblog.javaemaildemo.mail.EmailProcessorFunction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.mail.Message;

@Component
public class FunctionalEmailProcessor {

    @Autowired
    private MailProcessor mailProcessor;
    
    @PostConstruct
    public void init() {
        // 使用lambda表达式设置邮件处理函数
        mailProcessor.setEmailProcessorFunction((message, content, subject, from) -> {
            System.out.println("函数式处理邮件: " + subject);
            // 处理邮件逻辑
            return "处理结果";
        });
    }
}
```

#### 方式三：工具类方式

直接使用`MailService`作为工具类：

```java
import cn.sunyblog.javaemaildemo.mail.MailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class EmailProcessorExample {

    @Autowired
    private MailService mailService;
    
    // 启动邮件监听
    public boolean startEmailListener() {
        return mailService.startMailMonitoring();
    }
    
    // 停止邮件监听
    public void stopEmailListener() {
        mailService.stopMailMonitoring();
    }
    
    // 获取处理统计
    public String getEmailStats() {
        return mailService.getMailProcessingStats();
    }
}
```

### 4. 发送邮件

```java
import cn.sunyblog.javaemaildemo.mail.MailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.Arrays;
import java.util.List;

@Service
public class EmailService {

    @Autowired
    private MailService mailService;
    
    // 发送简单文本邮件
    public boolean sendTextEmail(String to, String subject, String content) {
        return mailService.sendSimpleEmail(to, subject, content);
    }
    
    // 发送HTML邮件
    public boolean sendHtmlEmail(String to, String subject, String htmlContent) {
        return mailService.sendHtmlEmail(to, subject, htmlContent);
    }
    
    // 发送带附件的邮件
    public boolean sendEmailWithAttachments(String to, String subject, String content, 
                                           List<File> attachments) {
        return mailService.sendEmailWithAttachments(to, subject, content, false, attachments);
    }
    
    // 批量发送邮件
    public int sendBatchEmails(List<String> recipients, String subject, String content) {
        return mailService.sendBatchEmails(recipients, subject, content, false);
    }
}
```

### 邮件发送重试机制

系统内置了智能重试机制，当邮件发送失败时会自动进行重试。重试机制支持以下特性：

- **自动重试**：当邮件发送失败时，系统会根据配置自动重试
- **指数退避**：每次重试的间隔时间会逐渐增加，避免对邮件服务器造成压力
- **可配置参数**：可以通过配置文件自定义重试次数、延迟时间等参数
- **特定异常重试**：只对特定的异常（如网络超时、服务器繁忙等）进行重试

重试机制完全透明，用户无需修改代码即可享受到这一功能。只需在配置文件中启用并设置相关参数：

```yaml
email:
  smtp:
    retry:
      enabled: true # 启用重试机制
      max-retries: 3 # 最大重试次数
      initial-delay-ms: 1000 # 初始重试延迟
      use-exponential-backoff: true # 使用指数退避策略
```

## 高级配置

### 完整配置选项

```yaml
email:
  listener:
    # 是否启用邮件监听器
    enabled: true
    
    # 邮件服务器配置
    server:
      # 邮件服务器地址
      host: imap.example.com
      # 邮件服务器端口
      port: 993
      # 邮件协议，例如imaps
      protocol: imaps
      # 邮件账户用户名
      username: your-email@example.com
      # 邮件账户授权码
      password: your-password
      # 邮件文件夹，默认为收件箱
      folder: INBOX
    
    # 连接配置
    connection:
      # 连接超时时间，默认15秒
      timeout: 15000
      # 读取超时时间，默认30秒
      read-timeout: 30000
      # 写入超时时间，默认30秒
      write-timeout: 30000
      # 是否信任所有SSL证书
      trust-all-certs: true
    
    # 监控配置
    monitor:
      # Idle状态超时时间，默认10秒
      idle-timeout: 10000
      # 保持连接的间隔时间，默认300秒
      keep-alive-interval: 300
      # 重新连接的延迟时间，默认15秒
      reconnect-delay: 15
      # 短延迟时间，默认5秒
      short-delay: 5
      # 长延迟时间，默认30秒
      long-delay: 30
      # 任务超时时间，默认300秒
      task-timeout: 300
    
    # 监听配置
    listener:
      # 最大重试次数，默认20次
      max-retries: 20
      # 是否自动启动
      auto-start: true
      # 是否处理现有未读邮件
      process-existing-unread: true
      # 线程池核心线程数
      core-pool-size: 16
      # 线程池最大线程数
      max-pool-size: 50
      # 线程池队列容量
      queue-capacity: 100
    
    # 日志配置
    log:
      # 是否启用debug日志，默认不启用
      debug-enabled: false
    
    # 附件配置
    attachment:
      # 附件存储目录
      save-dir: /path/to/save/attachments
      # 是否保存附件
      save-attachments: true
      # 是否使用唯一文件名
      use-unique-filename: true
      
  # SMTP邮件发送配置
  smtp:
    # SMTP服务器地址
    server: smtp.example.com
    # SMTP服务器端口
    port: 465
    # 邮件协议
    protocol: smtp
    # 邮件账户用户名
    username: your-email@example.com
    # 邮件账户授权码
    password: your-password
    # 连接配置
    connection:
      # 连接超时时间，默认15秒
      timeout: 15000
      # 读取超时时间，默认30秒
      read-timeout: 30000
      # 写入超时时间，默认30秒
      write-timeout: 30000
    # 邮件属性配置
    properties:
      # 是否启用SMTP认证
      mail-smtp-auth: true
      # 是否启用STARTTLS
      mail-smtp-starttls-enable: true
    # 重试配置
    retry:
      # 是否启用重试机制
      enabled: true
      # 最大重试次数（不包括第一次尝试）
      max-retries: 3
      # 初始重试延迟（毫秒）
      initial-delay-ms: 1000
      # 最大重试延迟（毫秒）
      max-delay-ms: 10000
      # 是否使用指数退避策略
      use-exponential-backoff: true
      # 退避乘数
      backoff-multiplier: 2.0
```

### 邮件过滤和条件处理

你可以在邮件处理器中实现自定义的过滤逻辑：

```java
@Component
public class FilteredEmailProcessor implements EmailListenerApi {
    
    @Override
    public boolean processEmail(Message message, String content, String subject, String from) {
        // 只处理特定发件人的邮件
        if (from.contains("important-sender.com")) {
            // 处理重要邮件
            return processImportantEmail(message, content, subject);
        }
        
        // 根据主题过滤
        if (subject.contains("[URGENT]")) {
            // 处理紧急邮件
            return processUrgentEmail(message, content);
        }
        
        // 默认处理
        return processRegularEmail(message, content);
    }
    
    private boolean processImportantEmail(Message message, String content, String subject) {
        // 处理重要邮件的逻辑
        return true;
    }
    
    private boolean processUrgentEmail(Message message, String content) {
        // 处理紧急邮件的逻辑
        return true;
    }
    
    private boolean processRegularEmail(Message message, String content) {
        // 处理普通邮件的逻辑
        return true;
    }
}
```

### 使用邮件事件机制

利用Spring的事件机制，可以在不同组件间传递邮件事件：

```java
// 发布邮件事件
@Component
public class EmailEventPublisher implements EmailListenerApi {
    
    private final ApplicationEventPublisher eventPublisher;
    
    public EmailEventPublisher(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }
    
    @Override
    public boolean processEmail(Message message, String content, String subject, String from) {
        // 创建邮件事件
        EmailEvent event = EmailEvent.builder()
                .message(message)
                .subject(subject)
                .from(from)
                .content(content)
                .receivedTime(System.currentTimeMillis())
                .build();
        
        // 发布事件
        eventPublisher.publishEvent(event);
        
        return true;
    }
}

// 监听邮件事件
@Component
public class EmailEventListener {
    
    @EventListener
    public void handleEmailEvent(EmailEvent event) {
        // 处理邮件事件
        System.out.println("收到邮件事件：" + event.getSubject());
        
        // 可以根据邮件内容执行不同的业务逻辑
        if (event.getSubject().contains("订单")) {
            processOrderEmail(event);
        } else if (event.getSubject().contains("注册")) {
            processRegistrationEmail(event);
        }
    }
    
    private void processOrderEmail(EmailEvent event) {
        // 处理订单相关邮件
    }
    
    private void processRegistrationEmail(EmailEvent event) {
        // 处理注册相关邮件
    }
}
```

## 高级用例

### 验证码提取和处理

```java
@Component
public class VerificationCodeProcessor implements EmailListenerApi {
    
    private static final Pattern VERIFICATION_CODE_PATTERN = Pattern.compile("验证码[：:\s]*(\d{4,6})");
    
    @Override
    public boolean processEmail(Message message, String content, String subject, String from) {
        // 提取验证码
        String code = extractVerificationCode(content);
        if (code != null) {
            // 处理验证码，例如存入Redis缓存
            saveVerificationCode(from, code);
            return true;
        }
        return false;
    }
    
    private String extractVerificationCode(String content) {
        if (content == null) {
            return null;
        }
        
        Matcher matcher = VERIFICATION_CODE_PATTERN.matcher(content);
        if (matcher.find()) {
            return matcher.group(1);
        }
        
        return null;
    }
    
    private void saveVerificationCode(String email, String code) {
        // 这里可以将验证码存入Redis或其他缓存系统
        System.out.println("保存验证码: " + email + " -> " + code);
    }
}
```

### 自定义邮件模板发送

```java
@Service
public class TemplateEmailService {

    @Autowired
    private MailService mailService;
    
    @Autowired
    private TemplateEngine templateEngine; // 例如Thymeleaf模板引擎
    
    /**
     * 使用模板发送邮件
     * 
     * @param to 收件人
     * @param subject 主题
     * @param templateName 模板名称
     * @param variables 模板变量
     * @return 是否发送成功
     */
    public boolean sendTemplateEmail(String to, String subject, String templateName, Map<String, Object> variables) {
        // 处理模板
        Context context = new Context();
        variables.forEach(context::setVariable);
        
        // 生成HTML内容
        String htmlContent = templateEngine.process(templateName, context);
        
        // 发送HTML邮件
        return mailService.sendHtmlEmail(to, subject, htmlContent);
    }
    
    /**
     * 发送欢迎邮件
     */
    public boolean sendWelcomeEmail(String to, String username) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("username", username);
        variables.put("currentYear", Calendar.getInstance().get(Calendar.YEAR));
        
        return sendTemplateEmail(to, "欢迎加入我们！", "welcome-email", variables);
    }
}
```

### 邮件附件处理

```java
@Component
public class AttachmentProcessor implements EmailListenerApi {
    
    @Autowired
    private FileStorageService fileStorageService; // 自定义文件存储服务
    
    @Override
    public boolean processEmail(Message message, String content, String subject, String from) {
        try {
            if (message.getContent() instanceof Multipart) {
                Multipart multipart = (Multipart) message.getContent();
                processAttachments(multipart, from);
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    
    private void processAttachments(Multipart multipart, String from) throws Exception {
        for (int i = 0; i < multipart.getCount(); i++) {
            BodyPart bodyPart = multipart.getBodyPart(i);
            
            // 检查是否为附件
            if (Part.ATTACHMENT.equalsIgnoreCase(bodyPart.getDisposition())) {
                String fileName = bodyPart.getFileName();
                
                // 处理附件，例如保存到云存储
                try (InputStream is = bodyPart.getInputStream()) {
                    // 保存附件
                    String fileUrl = fileStorageService.storeFile(fileName, is);
                    
                    // 记录附件信息
                    logAttachmentInfo(from, fileName, fileUrl);
                }
            }
            
            // 处理嵌套的Multipart
            if (bodyPart.getContent() instanceof Multipart) {
                processAttachments((Multipart) bodyPart.getContent(), from);
            }
        }
    }
    
    private void logAttachmentInfo(String from, String fileName, String fileUrl) {
        System.out.println("收到来自 " + from + " 的附件: " + fileName);
        System.out.println("附件已保存到: " + fileUrl);
    }
}
```

## 最佳实践

### 安全性建议

1. **不要硬编码密码**：使用环境变量或加密的配置文件存储敏感信息
2. **使用应用专用密码**：对于Gmail等服务，使用应用专用密码而非主密码
3. **限制权限**：使用具有最小必要权限的邮箱账户
4. **加密敏感数据**：对提取的敏感信息进行加密存储
5. **定期轮换密钥**：定期更新邮箱授权码
6. **启用SSL/TLS**：确保所有邮件传输都使用加密连接

### 性能优化

1. **调整线程池参数**：根据实际负载调整线程池大小
2. **批量处理**：使用批量发送API减少网络开销
3. **缓存优化**：定期清理邮件缓存，避免内存泄漏
4. **连接池复用**：复用邮件连接，减少连接建立开销
5. **超时设置**：合理设置连接超时和读写超时，避免资源浪费

## 最佳实践
### 可靠性保障

1. **重试机制**：对失败的操作进行智能重试
   - 邮件发送重试：自动对失败的邮件发送进行重试，支持指数退避策略
   - 连接重试：自动重试连接邮件服务器，确保连接可靠性
2. **熔断保护**：实现熔断机制，防止邮件服务器故障影响整个应用
3. **监控告警**：实现邮件处理状态监控和异常告警
4. **优雅降级**：在邮件服务不可用时提供降级策略
5. **定期健康检查**：定期检查邮件连接状态

## 常见问题解答

### 1. 如何处理不同邮件服务商的特殊要求？

不同邮件服务商可能有不同的连接要求和限制。对于常见的邮件服务商：

- **Gmail**: 需要开启"不够安全的应用访问权限"或使用应用专用密码
- **Outlook/Office365**: 可能需要特殊的认证设置
- **QQ邮箱**: 需要生成并使用授权码
- **163邮箱**: 需要在设置中开启IMAP/SMTP服务并使用授权码

### 2. 邮件监听器不工作，如何排查？

1. 检查网络连接是否正常
2. 验证邮箱凭据是否正确
3. 确认邮件服务器设置（主机、端口、协议）是否正确
4. 查看日志中的详细错误信息
5. 尝试手动启动监听器（`mailService.startMailMonitoring()`）
6. 检查防火墙设置是否阻止了邮件端口

### 3. 如何处理大量邮件的场景？

1. 增加线程池大小和队列容量
2. 实现分布式处理架构
3. 使用消息队列缓冲邮件处理任务
4. 实现邮件处理的优先级机制
5. 考虑使用专门的邮件处理服务

### 4. 如何实现邮件处理的幂等性？

使用`MailCache`类来记录已处理的邮件ID，避免重复处理：

```java
@Component
public class IdempotentEmailProcessor implements EmailListenerApi {
    
    @Autowired
    private MailCache mailCache;
    
    @Override
    public boolean processEmail(Message message, String content, String subject, String from) {
        // 获取邮件ID
        String messageId = mailCache.getMessageId(message);
        
        // 检查是否已处理
        if (!mailCache.checkAndMarkAsProcessed(messageId)) {
            // 已处理过，跳过
            return false;
        }
        
        // 处理邮件...
        return true;
    }
}
```

## 扩展与集成

### 与其他系统集成

1. **数据库集成**：将邮件内容存储到数据库中
2. **消息队列集成**：将邮件事件发送到消息队列
3. **REST API集成**：提供REST API接口暴露邮件功能
4. **WebSocket集成**：实时推送邮件通知
5. **微服务集成**：作为独立的邮件微服务

### 自定义扩展

1. **自定义邮件过滤器**：实现特定的邮件过滤逻辑
2. **自定义内容解析器**：处理特殊格式的邮件内容
3. **自定义存储策略**：实现不同的附件存储方式
4. **自定义重试策略**：实现更复杂的重试逻辑
5. **自定义监控指标**：收集和暴露邮件处理的监控指标

## 贡献指南

我们欢迎各种形式的贡献，包括但不限于：

- 报告问题和建议
- 提交代码改进
- 完善文档
- 添加新功能
- 修复bug

请遵循以下步骤：

1. Fork 项目
2. 创建特性分支 (`git checkout -b feature/amazing-feature`)
3. 提交更改 (`git commit -m 'Add some amazing feature'`)
4. 推送到分支 (`git push origin feature/amazing-feature`)
5. 创建Pull Request

## 版本历史

- **1.0.0** (2023-05-15): 首次发布
  - 基本的邮件监听、处理和发送功能
  - 支持IMAP协议
  - 支持附件处理
  - 支持多种邮件处理方式

## 许可证

本项目采用MIT许可证。详情请参阅[LICENSE](LICENSE)文件。

## 联系方式

- 项目维护者：[Your Name](mailto:your.email@example.com)
- 项目主页：[GitHub](https://github.com/yourusername/JavaEmailSpringBoot)

---

如果你觉得这个项目有用，请给它一个星标 ⭐️