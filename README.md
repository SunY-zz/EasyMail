```
# JavaEmailDemo

一个简单易用的Java邮件监听、处理和发送库，基于Spring Boot。

## 功能特点

- 自动监听新邮件，支持IMAP协议
- 支持邮件内容解析，包括文本和HTML格式
- 支持附件处理
- 提供多种邮件处理方式：接口实现、函数式处理、工具类方式
- 支持发送普通文本邮件、HTML格式邮件和带附件的邮件
- 支持批量发送邮件
- 自动重连和错误恢复机制
- 支持手动控制邮件监听器的启动和停止

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

### 4. 启动应用

启动你的Spring Boot应用，邮件监听器将自动启动并开始监听新邮件。

## 配置选项

以下是所有可用的配置选项：

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
```

## 高级用法

### 手动控制邮件监听器

如果你需要手动控制邮件监听器的启动和停止，可以注入`MailService`并调用相应的方法：

```java
import cn.sunyblog.javaemaildemo.mail.MailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class EmailController {
    
    @Autowired
    private MailService mailService;
    
    @GetMapping("/email/start")
    public String startEmailListener() {
        boolean result = mailService.startMailMonitoring();
        return "邮件监听器启动" + (result ? "成功" : "失败");
    }
    
    @GetMapping("/email/stop")
    public String stopEmailListener() {
        mailService.stopMailMonitoring();
        return "邮件监听器已停止";
    }
    
    @GetMapping("/email/status")
    public String getEmailListenerStatus() {
        boolean running = mailService.isMailServiceRunning();
        return "邮件监听器状态: " + (running ? "运行中" : "已停止");
    }
    
    @GetMapping("/email/stats")
    public String getEmailStats() {
        return mailService.getMailProcessingStats();
    }
}
```

### 使用邮件事件

你可以使用`EmailEvent`类来传递邮件事件信息：

```java
import cn.sunyblog.javaemaildemo.api.EmailEvent;
import cn.sunyblog.javaemaildemo.api.EmailListenerApi;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import javax.mail.Message;

@Component
public class MyEmailProcessor implements EmailListenerApi {
    
    private final ApplicationEventPublisher eventPublisher;
    
    public MyEmailProcessor(ApplicationEventPublisher eventPublisher) {
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
```

然后，你可以在其他组件中监听这个事件：

```java
import cn.sunyblog.javaemaildemo.api.EmailEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class EmailEventListener {
    
    @EventListener
    public void handleEmailEvent(EmailEvent event) {
        System.out.println("收到邮件事件：" + event.getSubject());
        // 处理邮件事件
    }
}
```

## 常见问题

### 邮件发送功能

除了监听和处理邮件，JavaEmailDemo还提供了强大的邮件发送功能：

#### 发送简单文本邮件

```java
// 通过MailService发送简单文本邮件
mailService.sendSimpleEmail("recipient@example.com", "邮件主题", "邮件内容");
```

#### 发送HTML格式邮件

```java
String htmlContent = "<html><body><h1>HTML邮件</h1><p>这是一封HTML格式的邮件</p></body></html>";
mailService.sendHtmlEmail("recipient@example.com", "HTML邮件", htmlContent);
```

#### 发送带附件的邮件

```java
List<File> attachments = new ArrayList<>();
attachments.add(new File("path/to/file.pdf"));
attachments.add(new File("path/to/image.jpg"));

mailService.sendEmailWithAttachments(
    "recipient@example.com", 
    "带附件的邮件", 
    "请查看附件", 
    false, // 是否HTML格式
    attachments
);
```

#### 批量发送邮件

```java
List<String> recipients = Arrays.asList(
    "user1@example.com",
    "user2@example.com",
    "user3@example.com"
);

int successCount = mailService.sendBatchEmails(
    recipients,
    "批量邮件",
    "这是一封批量发送的邮件",
    false // 是否HTML格式
);
```

### 常见问题

### 1. 如何处理特定类型的邮件？

你可以在`EmailListenerApi`的实现类中根据邮件的主题、发件人或内容来判断邮件类型，然后进行相应的处理。如果使用函数式处理方式，可以在lambda表达式中进行判断。

### 2. 如何保存附件？

附件会自动保存到配置的`email.listener.attachment.save-dir`目录中。如果你需要自定义附件保存逻辑，可以修改`MailContentParser`类。

### 3. 如何处理HTML邮件？

HTML邮件会自动转换为纯文本，你可以在`EmailListenerApi`的实现类中处理转换后的文本内容。

### 4. 如何选择合适的邮件处理方式？

- **接口实现方式**：适合需要在多个地方复用邮件处理逻辑的场景，或者处理逻辑比较复杂的场景。
- **函数式处理方式**：适合处理逻辑简单，或者需要动态切换处理逻辑的场景，更加灵活。
- **工具类方式**：适合只需要控制邮件监听器启停，而不需要自定义处理逻辑的场景。

### 5. 如何提取邮件中的验证码？

可以使用正则表达式提取验证码，示例代码：

```java
private String extractVerificationCode(String content) {
    if (content == null) {
        return null;
    }
    
    // 使用正则表达式匹配4-6位数字验证码
    Pattern pattern = Pattern.compile("\\b(\\d{4,6})\\b");
    Matcher matcher = pattern.matcher(content);
    
    if (matcher.find()) {
        return matcher.group(1);
    }
    
    return null;
}
```

### 6. 如何配置邮件发送功能？

在`application.properties`或`application.yml`中添加以下配置：

```properties
# 邮件发送配置
email.listener.server.host=smtp.example.com
email.listener.server.port=465
email.listener.server.protocol=smtp
email.listener.server.username=your-email@example.com
email.listener.server.password=your-password-or-app-password
```

## 许可证

本项目采用MIT许可证。详情请参阅[LICENSE](LICENSE)文件。