# 配置迁移指南

## 📖 概述

本指南帮助您解决在其他项目中集成EasyMail SDK时遇到的配置问题，并提供从旧配置格式迁移到新配置格式的详细说明。

## 🚨 常见问题解决

### 问题1：启动时报错 "No qualifying bean of type 'MailConfig'"

**错误信息：**
```
org.springframework.beans.factory.NoSuchBeanDefinitionException: 
No qualifying bean of type 'cn.sunyblog.javaemaildemo.mail.MailConfig' available
```

**解决方案：**

1. **确保配置正确**：检查您的`application.yml`配置文件，确保使用了正确的配置格式。

2. **使用兼容配置格式**：如果您想继续使用原有的配置格式，请确保配置如下：

```yaml
mail:
  imap:
    server: your-imap-server.com
    port: 993
    protocol: imaps
    username: your-email@example.com
    password: your-password
    # 其他配置...
```

3. **使用新的推荐配置格式**：

```yaml
email:
  listener:
    enabled: true
    server:
      host: your-imap-server.com
      port: 993
      protocol: imaps
      username: your-email@example.com
      password: your-password
    # 其他配置...
```

## 🔄 配置格式对比

### 旧配置格式 (mail.imap.*)

```yaml
mail:
  imap:
    server: imap.qq.com
    port: 993
    protocol: imaps
    username: your-email@qq.com
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
      short-delay: 5
      long-delay: 30
      task-timeout: 300
    listener:
      max-retries: 20
    log:
      debug-enabled: false
```

### 新配置格式 (email.listener.*)

```yaml
email:
  listener:
    enabled: true
    server:
      host: imap.qq.com
      port: 993
      protocol: imaps
      username: your-email@qq.com
      password: your-app-password
      folder: INBOX
    connection:
      timeout: 15000
      read-timeout: 30000
      write-timeout: 30000
      trust-all-certs: true
    monitor:
      idle-timeout: 10000
      keep-alive-interval: 300
      reconnect-delay: 15
      short-delay: 5
      long-delay: 30
      task-timeout: 300
    listener:
      max-retries: 20
      auto-start: true
      process-existing-unread: true
      core-pool-size: 16
      max-pool-size: 50
      queue-capacity: 100
    log:
      debug-enabled: false
    attachment:
      save-dir: ./attachments
      save-attachments: true
      use-unique-filename: true
```

## 📋 配置字段映射表

| 旧配置路径 | 新配置路径 | 说明 |
|-----------|-----------|------|
| `mail.imap.server` | `email.listener.server.host` | 邮件服务器地址 |
| `mail.imap.port` | `email.listener.server.port` | 邮件服务器端口 |
| `mail.imap.protocol` | `email.listener.server.protocol` | 邮件协议 |
| `mail.imap.username` | `email.listener.server.username` | 用户名 |
| `mail.imap.password` | `email.listener.server.password` | 密码 |
| `mail.imap.attachment-dir` | `email.listener.attachment.save-dir` | 附件保存目录 |
| `mail.imap.connection.*` | `email.listener.connection.*` | 连接配置 |
| `mail.imap.monitor.*` | `email.listener.monitor.*` | 监控配置 |
| `mail.imap.listener.*` | `email.listener.listener.*` | 监听配置 |
| `mail.imap.log.*` | `email.listener.log.*` | 日志配置 |

## 🚀 迁移步骤

### 步骤1：备份现有配置

首先备份您当前的`application.yml`配置文件。

### 步骤2：选择配置格式

**选项A：继续使用旧格式（推荐用于快速修复）**
- 保持现有的`mail.imap.*`配置不变
- SDK会自动兼容并创建所需的Bean

**选项B：迁移到新格式（推荐用于新项目）**
- 将配置从`mail.imap.*`迁移到`email.listener.*`
- 享受更丰富的配置选项和更好的结构化

### 步骤3：验证配置

启动应用程序，检查是否有配置错误：

```bash
# 启用debug模式查看详细信息
java -jar your-app.jar --debug
```

### 步骤4：测试功能

确保邮件监听和处理功能正常工作。

## 🔧 高级配置

### 自定义MailConfig Bean

如果您需要完全自定义MailConfig，可以在您的配置类中定义：

```java
@Configuration
public class CustomMailConfiguration {
    
    @Bean
    @Primary
    public MailConfig customMailConfig() {
        MailConfig config = new MailConfig();
        config.setServer("your-server.com");
        config.setPort("993");
        config.setProtocol("imaps");
        config.setUsername("your-username");
        config.setPassword("your-password");
        // 设置其他配置...
        return config;
    }
}
```

### 条件化配置

您可以根据环境或条件来选择不同的配置：

```java
@Configuration
public class ConditionalMailConfiguration {
    
    @Bean
    @ConditionalOnProperty(name = "app.mail.provider", havingValue = "qq")
    public MailConfig qqMailConfig() {
        // QQ邮箱配置
    }
    
    @Bean
    @ConditionalOnProperty(name = "app.mail.provider", havingValue = "gmail")
    public MailConfig gmailConfig() {
        // Gmail配置
    }
}
```

## 📞 技术支持

如果您在迁移过程中遇到问题，请：

1. 检查配置格式是否正确
2. 确保所有必需的配置项都已设置
3. 查看应用程序启动日志中的详细错误信息
4. 参考本项目的示例配置文件

## 📝 注意事项

1. **向后兼容性**：SDK同时支持新旧两种配置格式，您可以根据需要选择
2. **配置优先级**：如果同时存在两种格式的配置，新格式的优先级更高
3. **性能考虑**：新配置格式提供了更多的性能调优选项
4. **功能完整性**：某些新功能只在新配置格式中可用

---

**版本信息**：本指南适用于EasyMail SDK 1.0.0及以上版本。