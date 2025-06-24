# EasyMail - 应用级邮件服务框架

## 项目简介

EasyMail 是一个基于 Spring Boot 的应用级邮件服务框架，提供了完整的邮件发送和监听功能。该框架采用模块化设计，支持多种发送策略、模板引擎、异步处理等高级特性，适用于各种企业级应用场景。

## 主要功能

### 📧 邮件发送服务
- **多种发送方式**：支持文本、HTML、带附件的邮件发送
- **Builder 模式 API**：提供友好的链式调用接口
- **异步发送**：支持同步和异步两种发送模式
- **批量发送**：高效的批量邮件发送功能
- **模板支持**：内置模板引擎，支持变量替换
- **发送策略**：可插拔的发送策略，支持自定义
- **重试机制**：智能重试机制，提高发送成功率
- **监控统计**：完整的发送监控和统计功能

### 📨 邮件监听服务
- **实时监听**：基于 IMAP IDLE 的实时邮件监听（如果邮箱提供商支持，
但大部分邮箱服务商不支持IMAP IDLE模式，所有大部分情况使用轮询方式监听新邮件）
- **自动处理**：支持自定义邮件处理器
- **注解驱动**：基于 `@EasyMailProcessor` 和 `@EasyMailProcessorHandler` 注解形式的邮件处理方式
- **智能路由**：支持基于主题、发件人、标签等条件的邮件路由
- **上下文处理**：提供丰富的 `EasyMailContext` 邮件上下文信息
- **缓存机制**：避免重复处理同一邮件
- **异常处理**：完善的异常处理和恢复机制
- **连接管理**：自动重连和连接池管理

### 🔧 高级特性
- **自动配置**：基于 Spring Boot 的自动配置
- **配置灵活**：丰富的配置选项，支持多环境
- **扩展性强**：支持自定义处理器、策略、模板
- **线程安全**：完全线程安全的设计
- **SSL 支持**：完整的 SSL/TLS 支持
- **日志记录**：详细的日志记录和调试信息

## 项目优点

### 🚀 易于使用
- **一键启用**：通过 `@EnableEasyMail` 注解即可启用所有功能
- **低配置**：只需要提供邮箱账户以及授权码配置，开箱即用
- **友好 API**：Builder 模式的 API 设计，代码简洁易读

### 🏗️ 架构优秀
- **模块化设计**：清晰的模块划分，职责分离
- **策略模式**：可插拔的发送策略，易于扩展
- **事件驱动**：基于 Spring 事件机制的松耦合设计
- **依赖注入**：充分利用 Spring 的依赖注入特性

### 🔒 企业级特性
- **高可用性**：自动重连、异常恢复等机制
- **性能优化**：连接池、缓存、异步处理等优化
- **监控支持**：完整的监控指标和健康检查
- **安全性**：SSL/TLS 加密，安全的认证机制

### 🔧 高度可定制
- **自定义处理器**：支持自定义邮件处理逻辑
- **自定义策略**：支持自定义发送策略
- **自定义模板**：支持自定义模板引擎
- **配置灵活**：丰富的配置选项，适应不同需求

## 快速开始

### 1. 添加依赖

```xml
<dependency>
    <groupId>cn.sunyblog.easymail</groupId>
    <artifactId>easymail-spring-boot-starter</artifactId>
    <version>1.0.1</version>
</dependency>
```

### 2. 启用 EasyMail

在 Spring Boot 主类上添加 `@EnableEasyMail` 注解：

```java
@SpringBootApplication
@EnableEasyMail
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
```

### 3. 配置邮件服务

在 `application.yml` 中添加邮件配置：

```yaml
mail:
  # SMTP 发送配置
  smtp:
    server: smtp.139.com
    port: 465
    username: your-email@139.com
    password: your-auth-code
    
  # IMAP 监听配置（可选）
  imap:
    server: imap.139.com
    port: 993
    username: your-email@139.com
    password: your-auth-code
    attachment-dir: /path/to/attachments（可选）
```

### 4. 发送邮件

```java
@Service
public class EmailService {
    
    @Resource
    private EasyMailSenderService easyMailSenderService;
    
    public void sendEmail() {
        // 使用 Builder API 发送邮件
        EasyMailSendResult result = easyMailSenderService.send(
            EasyMailRequest.builder()
                .to("recipient@example.com")
                .subject("测试邮件")
                .text("这是一封测试邮件")
                .build()
        );
        
        if (result.isSuccess()) {
            System.out.println("邮件发送成功！");
        }
    }
}
```

### 5. 监听邮件（可选）

#### 方式一：实现接口

```java
@Component
public class MyEmailProcessor implements EasyMailListenerApi {
    
    @Override
    public boolean processEmail(Message message, String content, String subject, String from) {
        System.out.println("收到邮件：" + subject + " 来自：" + from);
        // 处理邮件逻辑
        return true;
    }
}
```

#### 方式二：注解驱动（推荐）

```java
@EasyMailProcessor(group = "verification", description = "验证码处理器")
public class VerificationCodeProcessor {
    
    @EasyMailProcessorHandler(
        subject = {"验证码", "verification"},
        subjectMatchType = EasyMailProcessorHandler.MatchType.CONTAINS,
        priority = 1,
        async = true
    )
    public void handleVerificationCode(EasyMailContext context) {
        String code = extractVerificationCode(context.getTextContent());
        if (code != null) {
            System.out.println("提取到验证码：" + code);
            // 存储验证码到缓存或数据库
        }
    }
    
    @EasyMailProcessorHandler(
        from = {"bank.com", "payment.com"},
        fromMatchType = EasyMailProcessorHandler.MatchType.CONTAINS,
        priority = 2
    )
    public void handleBankNotification(EasyMailContext context) {
        System.out.println("处理银行通知：" + context.getSubject());
        // 处理银行通知逻辑
    }
    
    private String extractVerificationCode(String content) {
        // 验证码提取逻辑
        return null;
    }
}
```

## 项目结构

```
easymail/
├── annotation/          # 注解定义
├── api/                # 核心 API 接口
├── config/             # 自动配置类
├── exception/          # 异常定义
├── mail/               # 邮件监听核心
├── processor/          # 注解驱动处理器
├── receiver/           # 邮件接收器
├── send/               # 邮件发送核心
│   ├── event/          # 发送事件
│   ├── monitor/        # 发送监控
│   ├── schedule/       # 定时发送
│   ├── strategy/       # 发送策略
│   └── template/       # 模板管理
├── template/           # 模板引擎
└── util/               # 工具类
```

## 注意
**目前测试发现：**

1. qq邮箱会使用idle模式进行实时监听，但是这种连接不太稳定，导致新邮件接收不及时，如果有实时监听的需求请使用其他服务商邮箱

2. 163邮箱连接时长较短，连接建立三分钟左右，163服务商就会提示认证错误，重试也无法成功，如果有长连接的需求，也请使用其他服务商邮箱


## 贡献
个人能力有限，如有错误请见谅并诚恳希望能向我提出错误。持续更新中~

欢迎提交 Issue 和 Pull Request 来帮助改进这个项目。

## 联系方式

- 作者：suny
- 邮箱：3379652824@qq.com
- 博客：https://www.sunyblog.cn/

---

更多详细信息请参阅：
- [API 文档](API.md)
- [高级文档](ADVANCED.md)