# @EnableEasyMail 注解使用指南

## 概述

`@EnableEasyMail` 是 EasyMail 框架提供的一个便捷注解，用于在 Spring Boot 应用中快速启用邮件服务。通过在主类上添加此注解，可以自动配置和启动邮件监听、发送、处理等功能，无需手动调用 `EasyMailService` 的 `init()` 或 `start()` 方法。

## 快速开始

### 1. 基本使用

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

### 2. 配置文件

在 `application.yml` 中配置邮件服务器信息：

```yaml
mail:
  imap:
    server: imap.example.com
    port: 993
    protocol: imaps
    username: your-email@example.com
    password: your-password
    auto-start: true
  
  sender:
    enabled: true
  
  listener:
    enabled: true
```

## 注解属性详解

### @EnableEasyMail 属性说明

```java
@EnableEasyMail(
    autoStart = true,           // 是否自动启动邮件监听服务
    enableSender = true,        // 是否启用邮件发送服务
    enableListener = true,      // 是否启用邮件监听服务
    enableProcessor = true,     // 是否启用注解驱动的邮件处理器
    scanPackages = {            // 指定扫描包路径
        "com.example.mail.processor",
        "com.example.mail.handler"
    },
    configPrefix = "mail"       // 配置文件前缀
)
```

#### 属性详细说明

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `autoStart` | boolean | true | 应用启动后是否自动开启邮件监听服务 |
| `enableSender` | boolean | true | 是否启用邮件发送功能 |
| `enableListener` | boolean | true | 是否启用邮件监听功能 |
| `enableProcessor` | boolean | true | 是否启用注解驱动的邮件处理器 |
| `scanPackages` | String[] | {} | 指定扫描邮件处理器的包路径，为空时扫描主类所在包及子包 |
| `configPrefix` | String | "mail" | 配置文件中的属性前缀 |

## 使用示例

### 示例 1：最简配置

```java
@SpringBootApplication
@EnableEasyMail
public class SimpleMailApplication {
    public static void main(String[] args) {
        SpringApplication.run(SimpleMailApplication.class, args);
    }
}
```

### 示例 2：自定义配置

```java
@SpringBootApplication
@EnableEasyMail(
    autoStart = false,          // 不自动启动，手动控制
    enableSender = true,        // 启用发送功能
    enableListener = true,      // 启用监听功能
    enableProcessor = true,     // 启用处理器
    scanPackages = {
        "com.example.mail.processor",
        "com.example.mail.handler"
    }
)
public class CustomMailApplication {
    
    @Autowired
    private EasyMailService easyMailService;
    
    public static void main(String[] args) {
        SpringApplication.run(CustomMailApplication.class, args);
    }
    
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        // 应用启动完成后手动启动邮件服务
        easyMailService.startMailMonitoring();
    }
}
```

### 示例 3：仅启用发送功能

```java
@SpringBootApplication
@EnableEasyMail(
    autoStart = false,
    enableSender = true,
    enableListener = false,     // 禁用监听功能
    enableProcessor = false     // 禁用处理器
)
public class SenderOnlyApplication {
    public static void main(String[] args) {
        SpringApplication.run(SenderOnlyApplication.class, args);
    }
}
```

## 服务控制

### 程序化控制

通过注入 `EasyMailStarter` 可以程序化控制邮件服务：

```java
@RestController
@RequestMapping("/api/mail")
public class MailController {
    
    @Autowired
    private EasyMailAutoConfiguration.EasyMailStarter easyMailStarter;
    
    @PostMapping("/start")
    public ResponseEntity<String> startService() {
        boolean success = easyMailStarter.start();
        return ResponseEntity.ok(success ? "服务启动成功" : "服务启动失败");
    }
    
    @PostMapping("/stop")
    public ResponseEntity<String> stopService() {
        easyMailStarter.stop();
        return ResponseEntity.ok("服务已停止");
    }
    
    @PostMapping("/restart")
    public ResponseEntity<String> restartService() {
        boolean success = easyMailStarter.restart();
        return ResponseEntity.ok(success ? "服务重启成功" : "服务重启失败");
    }
    
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("running", easyMailStarter.isRunning());
        status.put("configuration", easyMailStarter.getConfiguration());
        return ResponseEntity.ok(status);
    }
}
```

### REST API 控制

如果使用了示例应用 `EnableEasyMailExample`，可以通过以下 REST API 控制服务：

- `GET /api/email/status` - 获取服务状态
- `POST /api/email/start` - 启动邮件服务
- `POST /api/email/stop` - 停止邮件服务
- `POST /api/email/restart` - 重启邮件服务

## 配置文件示例

### 完整配置示例

```yaml
# 邮件服务配置
mail:
  # IMAP 服务器配置
  imap:
    server: imap.gmail.com
    port: 993
    protocol: imaps
    username: your-email@gmail.com
    password: your-app-password
    auto-start: true
    
    # 连接配置
    connection:
      timeout: 30000
      read-timeout: 60000
      write-timeout: 60000
      keep-alive: true
    
    # 监控配置
    monitor:
      check-interval: 30
      max-idle-time: 300
      reconnect-attempts: 3
    
    # 日志配置
    log:
      enabled: true
      level: INFO
      include-content: false
  
  # 发送服务配置
  sender:
    enabled: true
    smtp:
      server: smtp.gmail.com
      port: 587
      protocol: smtp
      username: your-email@gmail.com
      password: your-app-password
      auth: true
      starttls: true
  
  # 监听服务配置
  listener:
    enabled: true
    folders:
      - INBOX
      - Sent
    
  # 处理器配置
  processor:
    enabled: true
    scan-packages:
      - com.example.mail.processor
      - com.example.mail.handler
```

## 与传统方式的对比

### 传统方式

```java
@SpringBootApplication
public class TraditionalApplication {
    
    @Autowired
    private EasyMailService easyMailService;
    
    public static void main(String[] args) {
        SpringApplication.run(TraditionalApplication.class, args);
    }
    
    @PostConstruct
    public void init() {
        // 手动初始化和启动
        easyMailService.init();
        easyMailService.startMailMonitoring();
    }
}
```

### 注解方式

```java
@SpringBootApplication
@EnableEasyMail  // 一个注解搞定
public class AnnotationApplication {
    public static void main(String[] args) {
        SpringApplication.run(AnnotationApplication.class, args);
    }
}
```

## 优势

1. **简化配置**：一个注解即可启用所有邮件功能
2. **自动装配**：自动配置所有必要的 Bean 和服务
3. **灵活控制**：通过注解属性精确控制启用的功能
4. **向后兼容**：与现有配置方式完全兼容
5. **程序化控制**：提供 `EasyMailStarter` 用于运行时控制
6. **配置验证**：自动验证配置的有效性

## 注意事项

1. **配置文件**：确保 `application.yml` 中有正确的邮件服务器配置
2. **包扫描**：如果邮件处理器不在主类包下，需要指定 `scanPackages`
3. **依赖注入**：所有相关的 Bean 都会自动注册到 Spring 容器中
4. **服务控制**：可以通过 `autoStart=false` 禁用自动启动，然后手动控制
5. **配置前缀**：默认使用 `mail` 前缀，可以通过 `configPrefix` 自定义

## 故障排除

### 常见问题

1. **服务未启动**
   - 检查 `autoStart` 是否为 `true`
   - 检查配置文件中的邮件服务器信息
   - 查看应用日志中的错误信息

2. **Bean 注入失败**
   - 确保在主类上添加了 `@EnableEasyMail` 注解
   - 检查包扫描路径是否正确

3. **配置不生效**
   - 检查配置文件格式是否正确
   - 确认配置前缀是否匹配

### 调试技巧

1. **启用调试日志**：
   ```yaml
   logging:
     level:
       cn.sunyblog.easymail: DEBUG
   ```

2. **检查 Bean 注册**：
   ```java
   @Autowired
   private ApplicationContext applicationContext;
   
   public void checkBeans() {
       String[] beanNames = applicationContext.getBeanNamesForType(EasyMailService.class);
       System.out.println("EasyMailService beans: " + Arrays.toString(beanNames));
   }
   ```

## 总结

`@EnableEasyMail` 注解提供了一种简洁、优雅的方式来启用 EasyMail 邮件服务。通过合理配置注解属性和配置文件，可以快速构建功能完整的邮件处理应用。这种方式不仅简化了配置过程，还提供了灵活的控制选项，是推荐的 EasyMail 使用方式。