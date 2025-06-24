# EasyMail 高级文档

## 目录

- [架构概述](#架构概述)
- [自定义发送策略](#自定义发送策略)
- [自定义模板引擎](#自定义模板引擎)
- [自定义邮件处理器](#自定义邮件处理器)
- [自定义缓存管理器](#自定义缓存管理器)
- [自定义异常处理器](#自定义异常处理器)
- [扩展配置](#扩展配置)
- [性能调优](#性能调优)
- [监控与诊断](#监控与诊断)
- [集成示例](#集成示例)

## 架构概述

EasyMail 采用模块化设计，主要包含以下核心组件：

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   发送服务模块   │    │   监听服务模块   │    │   处理器模块     │
│                │    │                │    │                │
│ ┌─────────────┐ │    │ ┌─────────────┐ │    │ ┌─────────────┐ │
│ │ 发送策略管理 │ │    │ │ 邮件监听器   │ │    │ │ 注解处理器   │ │
│ └─────────────┘ │    │ └─────────────┘ │    │ └─────────────┘ │
│ ┌─────────────┐ │    │ ┌─────────────┐ │    │ ┌─────────────┐ │
│ │ 模板管理器   │ │    │ │ 内容解析器   │ │    │ │ 函数式处理器 │ │
│ └─────────────┘ │    │ └─────────────┘ │    │ └─────────────┘ │
│ ┌─────────────┐ │    │ ┌─────────────┐ │    │ ┌─────────────┐ │
│ │ 发送监控器   │ │    │ │ 缓存管理器   │ │    │ │ 上下文构建器 │ │
│ └─────────────┘ │    │ └─────────────┘ │    │ └─────────────┘ │
└─────────────────┘    └─────────────────┘    └─────────────────┘
```

## 自定义发送策略

### 1. 实现发送策略接口

```java
@Component
public class CustomEasyMailSendStrategy implements EasyMailSendStrategy {
    
    @Resource
    private EasyMailSender easyMailSender;
    
    @Override
    public EasyMailSendResult send(List<String> toList, List<String> ccList, List<String> bccList,
                                   String subject, String content, boolean isHtml, List<File> attachments) {
        
        // 自定义发送逻辑
        try {
            // 1. 预处理
            preprocessEmail(toList, subject, content);
            
            // 2. 发送邮件
            EasyMailSendResult result = doCustomSend(toList, ccList, bccList, subject, content, isHtml, attachments);
            
            // 3. 后处理
            postprocessEmail(result);
            
            return result;
            
        } catch (Exception e) {
            return EasyMailSendResult.failure("自定义策略发送失败: " + e.getMessage());
        }
    }
    
    @Override
    public String getStrategyName() {
        return "custom-strategy";
    }
    
    @Override
    public String getDescription() {
        return "自定义邮件发送策略，支持特殊业务逻辑";
    }
    
    @Override
    public boolean supports(int recipientCount, boolean hasAttachments, boolean isHtml) {
        // 定义策略适用条件
        return recipientCount <= 100 && !hasAttachments; // 仅支持小批量无附件邮件
    }
    
    @Override
    public int getPriority() {
        return 1; // 高优先级
    }
    
    private void preprocessEmail(List<String> toList, String subject, String content) {
        // 预处理逻辑：验证、过滤、转换等
        log.info("预处理邮件，收件人数量: {}, 主题: {}", toList.size(), subject);
        
        // 例如：过滤无效邮箱
        toList.removeIf(email -> !isValidEmail(email));
    }
    
    private EasyMailSendResult doCustomSend(List<String> toList, List<String> ccList, List<String> bccList,
                                            String subject, String content, boolean isHtml, List<File> attachments) {
        
        // 实现自定义发送逻辑
        long startTime = System.currentTimeMillis();
        
        try {
            // 使用底层发送器
            boolean success = easyMailSender.sendEmail(toList, ccList, bccList, subject, content, isHtml, attachments);
            
            long duration = System.currentTimeMillis() - startTime;
            
            if (success) {
                return EasyMailSendResult.success("CUSTOM-" + System.currentTimeMillis(), toList.size(), duration);
            } else {
                return EasyMailSendResult.failure("发送失败");
            }
            
        } catch (Exception e) {
            return EasyMailSendResult.failure("发送异常: " + e.getMessage());
        }
    }
    
    private void postprocessEmail(EasyMailSendResult result) {
        // 后处理逻辑：记录日志、统计、通知等
        if (result.isSuccess()) {
            log.info("自定义策略发送成功，消息ID: {}", result.getMessageId());
        } else {
            log.error("自定义策略发送失败: {}", result.getErrorMessage());
        }
    }
    
    private boolean isValidEmail(String email) {
        return email != null && email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    }
}
```

### 2. 高级发送策略示例

#### 智能重试策略

```java
@Component
public class SmartRetryEasyMailSendStrategy implements EasyMailSendStrategy {
    
    private static final int MAX_RETRIES = 3;
    private static final long[] RETRY_DELAYS = {1000, 3000, 5000}; // 毫秒
    
    @Resource
    private EasyMailSender easyMailSender;
    
    @Override
    public EasyMailSendResult send(List<String> toList, List<String> ccList, List<String> bccList,
                                   String subject, String content, boolean isHtml, List<File> attachments) {
        
        Exception lastException = null;
        
        for (int attempt = 0; attempt < MAX_RETRIES; attempt++) {
            try {
                // 尝试发送
                boolean success = easyMailSender.sendEmail(toList, ccList, bccList, subject, content, isHtml, attachments);
                
                if (success) {
                    return EasyMailSendResult.success("RETRY-" + System.currentTimeMillis(), toList.size());
                }
                
            } catch (Exception e) {
                lastException = e;
                log.warn("发送尝试 {} 失败: {}", attempt + 1, e.getMessage());
                
                // 如果不是最后一次尝试，等待后重试
                if (attempt < MAX_RETRIES - 1) {
                    try {
                        Thread.sleep(RETRY_DELAYS[attempt]);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }
        
        return EasyMailSendResult.failure("重试 " + MAX_RETRIES + " 次后仍然失败: " + 
                                          (lastException != null ? lastException.getMessage() : "未知错误"));
    }
    
    @Override
    public String getStrategyName() {
        return "smart-retry";
    }
    
    @Override
    public String getDescription() {
        return "智能重试策略，支持指数退避重试";
    }
    
    @Override
    public boolean supports(int recipientCount, boolean hasAttachments, boolean isHtml) {
        return true; // 支持所有场景
    }
    
    @Override
    public int getPriority() {
        return 5; // 低优先级，作为兜底策略
    }
}
```

#### 负载均衡策略

```java
@Component
public class LoadBalancedEasyMailSendStrategy implements EasyMailSendStrategy {
    
    private final List<EasyMailSender> senderPool;
    private final AtomicInteger currentIndex = new AtomicInteger(0);
    
    public LoadBalancedEasyMailSendStrategy(@Qualifier("primarySender") EasyMailSender primarySender,
                                            @Qualifier("secondarySender") EasyMailSender secondarySender) {
        this.senderPool = Arrays.asList(primarySender, secondarySender);
    }
    
    @Override
    public EasyMailSendResult send(List<String> toList, List<String> ccList, List<String> bccList,
                                   String subject, String content, boolean isHtml, List<File> attachments) {
        
        // 轮询选择发送器
        EasyMailSender sender = getNextSender();
        
        try {
            boolean success = sender.sendEmail(toList, ccList, bccList, subject, content, isHtml, attachments);
            
            if (success) {
                return EasyMailSendResult.success("LB-" + System.currentTimeMillis(), toList.size());
            } else {
                // 如果当前发送器失败，尝试下一个
                return tryWithNextSender(toList, ccList, bccList, subject, content, isHtml, attachments);
            }
            
        } catch (Exception e) {
            log.warn("发送器 {} 发送失败，尝试下一个: {}", sender.getClass().getSimpleName(), e.getMessage());
            return tryWithNextSender(toList, ccList, bccList, subject, content, isHtml, attachments);
        }
    }
    
    private EasyMailSender getNextSender() {
        int index = currentIndex.getAndIncrement() % senderPool.size();
        return senderPool.get(index);
    }
    
    private EasyMailSendResult tryWithNextSender(List<String> toList, List<String> ccList, List<String> bccList,
                                                  String subject, String content, boolean isHtml, List<File> attachments) {
        EasyMailSender nextSender = getNextSender();
        
        try {
            boolean success = nextSender.sendEmail(toList, ccList, bccList, subject, content, isHtml, attachments);
            
            if (success) {
                return EasyMailSendResult.success("LB-FALLBACK-" + System.currentTimeMillis(), toList.size());
            }
        } catch (Exception e) {
            log.error("备用发送器也失败: {}", e.getMessage());
        }
        
        return EasyMailSendResult.failure("所有发送器都失败");
    }
    
    @Override
    public String getStrategyName() {
        return "load-balanced";
    }
    
    @Override
    public String getDescription() {
        return "负载均衡策略，在多个发送器之间轮询";
    }
    
    @Override
    public boolean supports(int recipientCount, boolean hasAttachments, boolean isHtml) {
        return recipientCount > 10; // 适用于大批量发送
    }
    
    @Override
    public int getPriority() {
        return 2;
    }
}
```

### 3. 注册自定义策略

```java
@Configuration
public class CustomStrategyConfig {
    
    @Bean
    @Primary
    public EasyMailSendStrategyManager customStrategyManager(
            List<EasyMailSendStrategy> strategies) {
        
        EasyMailSendStrategyManager manager = new EasyMailSendStrategyManager();
        
        // 注册所有策略
        strategies.forEach(manager::registerStrategy);
        
        // 设置默认策略选择逻辑
        manager.setStrategySelector(this::selectBestStrategy);
        
        return manager;
    }
    
    private EasyMailSendStrategy selectBestStrategy(List<EasyMailSendStrategy> availableStrategies,
                                                     int recipientCount, boolean hasAttachments, boolean isHtml) {
        
        // 自定义策略选择逻辑
        return availableStrategies.stream()
                .filter(strategy -> strategy.supports(recipientCount, hasAttachments, isHtml))
                .min(Comparator.comparingInt(EasyMailSendStrategy::getPriority))
                .orElse(availableStrategies.get(0)); // 默认策略
    }
}
```

## 自定义模板引擎

### 1. 实现模板引擎接口

```java
@Component
public class ThymeleafEasyMailTemplateEngine implements EasyMailSendTemplateEngine {
    
    private final TemplateEngine templateEngine;
    
    public ThymeleafEasyMailTemplateEngine() {
        // 配置 Thymeleaf 模板引擎
        SpringTemplateEngine engine = new SpringTemplateEngine();
        
        // 设置模板解析器
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/email/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCharacterEncoding("UTF-8");
        resolver.setCacheable(true);
        
        engine.setTemplateResolver(resolver);
        this.templateEngine = engine;
    }
    
    @Override
    public String processTemplate(String templateName, Map<String, Object> variables) {
        try {
            Context context = new Context();
            context.setVariables(variables);
            
            return templateEngine.process(templateName, context);
            
        } catch (Exception e) {
            throw new EasyMailTemplateException("Thymeleaf 模板处理失败: " + e.getMessage(), e);
        }
    }
    
    @Override
    public boolean supports(String templateName) {
        // 检查模板是否存在
        try {
            return templateEngine.getTemplateResolver().resolveTemplate(
                    templateEngine.getConfiguration(), null, templateName, null) != null;
        } catch (Exception e) {
            return false;
        }
    }
    
    @Override
    public String getEngineName() {
        return "thymeleaf";
    }
}
```

### 2. Freemarker 模板引擎

```java
@Component
public class FreemarkerEasyMailTemplateEngine implements EasyMailSendTemplateEngine {
    
    private final Configuration freemarkerConfig;
    
    public FreemarkerEasyMailTemplateEngine() {
        freemarkerConfig = new Configuration(Configuration.VERSION_2_3_31);
        freemarkerConfig.setClassForTemplateLoading(this.getClass(), "/templates/email");
        freemarkerConfig.setDefaultEncoding("UTF-8");
        freemarkerConfig.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
    }
    
    @Override
    public String processTemplate(String templateName, Map<String, Object> variables) {
        try {
            Template template = freemarkerConfig.getTemplate(templateName + ".ftl");
            
            StringWriter writer = new StringWriter();
            template.process(variables, writer);
            
            return writer.toString();
            
        } catch (Exception e) {
            throw new EasyMailTemplateException("Freemarker 模板处理失败: " + e.getMessage(), e);
        }
    }
    
    @Override
    public boolean supports(String templateName) {
        try {
            freemarkerConfig.getTemplate(templateName + ".ftl");
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    @Override
    public String getEngineName() {
        return "freemarker";
    }
}
```

### 3. 自定义模板管理器

```java
@Component
public class CustomEasyMailTemplateManager extends EasyMailSendTemplateManager {
    
    private final Map<String, EasyMailSendTemplateEngine> engines = new HashMap<>();
    private final Map<String, EasyMailSendTemplate> templateCache = new ConcurrentHashMap<>();
    
    @Autowired
    public CustomEasyMailTemplateManager(List<EasyMailSendTemplateEngine> templateEngines) {
        // 注册所有模板引擎
        templateEngines.forEach(engine -> 
            engines.put(engine.getEngineName(), engine));
    }
    
    @Override
    public EasyMailSendTemplate getTemplate(String templateId) {
        // 先从缓存获取
        EasyMailSendTemplate template = templateCache.get(templateId);
        if (template != null) {
            return template;
        }
        
        // 从数据库或文件系统加载
        template = loadTemplateFromStorage(templateId);
        if (template != null) {
            templateCache.put(templateId, template);
        }
        
        return template;
    }
    
    @Override
    public String processTemplate(String templateId, Map<String, Object> variables) {
        EasyMailSendTemplate template = getTemplate(templateId);
        if (template == null) {
            throw new EasyMailTemplateException("模板不存在: " + templateId);
        }
        
        // 根据模板类型选择引擎
        String engineName = template.getEngineName();
        EasyMailSendTemplateEngine engine = engines.get(engineName);
        
        if (engine == null) {
            // 使用默认的简单替换引擎
            return EasyMailSendTemplate.replaceVariables(template.getContentTemplate(), variables);
        }
        
        return engine.processTemplate(template.getContentTemplate(), variables);
    }
    
    private EasyMailSendTemplate loadTemplateFromStorage(String templateId) {
        // 实现从数据库或文件系统加载模板的逻辑
        // 这里是示例实现
        
        if ("welcome".equals(templateId)) {
            return EasyMailSendTemplate.builder()
                    .templateId(templateId)
                    .templateName("欢迎邮件")
                    .subjectTemplate("欢迎 ${username} 加入我们！")
                    .contentTemplate(loadTemplateContent("welcome"))
                    .isHtml(true)
                    .engineName("thymeleaf")
                    .build();
        }
        
        return null;
    }
    
    private String loadTemplateContent(String templateName) {
        // 从文件系统加载模板内容
        try {
            return Files.readString(Paths.get("templates/email/" + templateName + ".html"));
        } catch (Exception e) {
            log.warn("加载模板文件失败: {}", templateName, e);
            return null;
        }
    }
}
```

## 自定义邮件处理器

### 1. 传统方式：实现 EasyMailListenerApi

```java
@Component
public class CustomEmailProcessor implements EasyMailListenerApi {
    
    @Override
    public boolean processEmail(Message message, String content, String subject, String from) {
        // 自定义处理逻辑
        return handleCustomLogic(message, content, subject, from);
    }
    
    @Override
    public String getProcessorName() {
        return "CustomEmailProcessor";
    }
    
    private boolean handleCustomLogic(Message message, String content, String subject, String from) {
        // 实现具体的处理逻辑
        return true;
    }
}
```

### 2. 注解驱动方式（推荐）

#### 2.1 基础注解处理器

```java
@EasyMailProcessor(
    group = "notification",
    description = "通知邮件处理器",
    enabled = true
)
public class NotificationEmailProcessor {
    
    private static final Logger logger = LoggerFactory.getLogger(NotificationEmailProcessor.class);
    
    @EasyMailProcessorHandler(
        subject = {"通知", "notification", "alert"},
        subjectMatchType = EasyMailProcessorHandler.MatchType.CONTAINS,
        priority = 1,
        async = true,
        description = "处理通知类邮件"
    )
    public void handleNotification(EasyMailContext context) {
        logger.info("处理通知邮件：{} 来自：{}", context.getSubject(), context.getFrom());
        
        // 根据邮件内容进行分类处理
        String content = context.getTextContent();
        if (content.contains("系统")) {
            handleSystemNotification(context);
        } else if (content.contains("用户")) {
            handleUserNotification(context);
        }
    }
    
    @EasyMailProcessorHandler(
        from = {".*@system\\.com$", ".*@admin\\.com$"},
        fromMatchType = EasyMailProcessorHandler.MatchType.REGEX,
        priority = 0,
        description = "处理系统管理员邮件"
    )
    public void handleAdminEmail(EasyMailContext context) {
        logger.warn("收到管理员邮件：{}", context.getSubject());
        
        // 高优先级处理
        sendImmediateAlert(context);
    }
    
    @EasyMailProcessorHandler(
        tags = {"urgent", "emergency"},
        tagsMatchType = EasyMailProcessorHandler.MatchType.CONTAINS,
        priority = -1,  // 最高优先级
        async = false,  // 同步处理
        description = "处理紧急邮件"
    )
    public void handleUrgentEmail(EasyMailContext context) {
        logger.error("收到紧急邮件：{} 来自：{}", context.getSubject(), context.getFrom());
        
        // 紧急邮件处理逻辑
        handleEmergency(context);
    }
    
    private void handleSystemNotification(EasyMailContext context) {
        // 系统通知处理逻辑
    }
    
    private void handleUserNotification(EasyMailContext context) {
        // 用户通知处理逻辑
    }
    
    private void sendImmediateAlert(EasyMailContext context) {
        // 立即发送警报
    }
    
    private void handleEmergency(EasyMailContext context) {
        // 紧急情况处理
    }
}
```

#### 2.2 高级匹配模式

```java
@EasyMailProcessor(group = "advanced", description = "高级邮件处理器")
public class AdvancedEmailProcessor {
    
    // 正则表达式匹配
    @EasyMailProcessorHandler(
        subject = {"订单[0-9]{8,12}", "Order\\s+#[A-Z0-9]+"},
        subjectMatchType = EasyMailProcessorHandler.MatchType.REGEX,
        description = "处理订单邮件"
    )
    public void handleOrderEmail(EasyMailContext context) {
        String orderNumber = extractOrderNumber(context.getSubject());
        processOrder(orderNumber, context);
    }
    
    // 前缀匹配
    @EasyMailProcessorHandler(
        from = {"noreply@", "no-reply@", "donotreply@"},
        fromMatchType = EasyMailProcessorHandler.MatchType.PREFIX,
        description = "处理自动发送邮件"
    )
    public void handleAutomatedEmail(EasyMailContext context) {
        // 自动邮件处理逻辑
        processAutomatedEmail(context);
    }
    
    // 后缀匹配
    @EasyMailProcessorHandler(
        from = {".gov", ".edu", ".org"},
        fromMatchType = EasyMailProcessorHandler.MatchType.SUFFIX,
        description = "处理机构邮件"
    )
    public void handleInstitutionalEmail(EasyMailContext context) {
        // 机构邮件处理逻辑
        processInstitutionalEmail(context);
    }
    
    // 多条件组合匹配
    @EasyMailProcessorHandler(
        subject = {"验证码", "verification"},
        subjectMatchType = EasyMailProcessorHandler.MatchType.CONTAINS,
        from = {".*@bank\\.com$", ".*@payment\\.com$"},
        fromMatchType = EasyMailProcessorHandler.MatchType.REGEX,
        priority = 1,
        async = true,
        description = "处理银行验证码邮件"
    )
    public void handleBankVerificationCode(EasyMailContext context) {
        String code = extractVerificationCode(context.getTextContent());
        if (code != null) {
            storeBankVerificationCode(context.getFrom(), code);
        }
    }
    
    private String extractOrderNumber(String subject) {
        // 订单号提取逻辑
        return null;
    }
    
    private void processOrder(String orderNumber, EasyMailContext context) {
        // 订单处理逻辑
    }
    
    private void processAutomatedEmail(EasyMailContext context) {
        // 自动邮件处理逻辑
    }
    
    private void processInstitutionalEmail(EasyMailContext context) {
        // 机构邮件处理逻辑
    }
    
    private String extractVerificationCode(String content) {
        // 验证码提取逻辑
        return null;
    }
    
    private void storeBankVerificationCode(String from, String code) {
        // 银行验证码存储逻辑
    }
}
```

#### 2.3 处理器管理和控制

```java
@RestController
@RequestMapping("/api/mail/processor")
public class EmailProcessorController {
    
    @Autowired
    private AnnotationDrivenEasyMailProcessorManager processorManager;
    
    /**
     * 获取所有处理器信息
     */
    @GetMapping("/list")
    public ResponseEntity<Map<String, Object>> listProcessors() {
        Map<String, Object> result = processorManager.getProcessorInfo();
        return ResponseEntity.ok(result);
    }
    
    /**
     * 获取处理器统计信息
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        Map<String, Object> stats = processorManager.getProcessorStats();
        return ResponseEntity.ok(stats);
    }
    
    /**
     * 重置统计信息
     */
    @PostMapping("/stats/reset")
    public ResponseEntity<String> resetStats() {
        processorManager.resetStats();
        return ResponseEntity.ok("统计信息已重置");
    }
    
    /**
     * 启用/禁用处理器
     */
    @PostMapping("/toggle/{handlerId}")
    public ResponseEntity<String> toggleProcessor(@PathVariable String handlerId, 
                                                  @RequestParam boolean enabled) {
        boolean result = processorManager.setProcessorEnabled(handlerId, enabled);
        if (result) {
            return ResponseEntity.ok("处理器状态已更新");
        } else {
            return ResponseEntity.badRequest().body("处理器不存在或更新失败");
        }
    }
    
    /**
     * 按组处理邮件（测试用）
     */
    @PostMapping("/process/group/{group}")
    public ResponseEntity<String> processEmailByGroup(@PathVariable String group,
                                                      @RequestBody EasyMailContext context) {
        try {
            processorManager.processEmailByGroup(context, group);
            return ResponseEntity.ok("邮件处理完成");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("邮件处理失败：" + e.getMessage());
        }
    }
}
```

#### 2.4 配置属性

```yaml
# application.yml
mail:
  processor:
    annotation-driven:
      enabled: true
      scan-packages:
        - com.example.mail.processor
        - com.example.business.email
      async:
        core-pool-size: 5
        max-pool-size: 20
        queue-capacity: 100
        thread-name-prefix: "email-processor-"
      metrics:
        enabled: true
        export-interval: 60s
```

对应的配置类：

```java
@ConfigurationProperties(prefix = "mail.processor.annotation-driven")
@Data
public class AnnotationDrivenEasyMailProcessorProperties {
    
    /**
     * 是否启用注解驱动邮件处理器
     */
    private boolean enabled = true;
    
    /**
     * 扫描包路径
     */
    private List<String> scanPackages = Arrays.asList("com.example");
    
    /**
     * 异步处理配置
     */
    private AsyncConfig async = new AsyncConfig();
    
    /**
     * 指标配置
     */
    private MetricsConfig metrics = new MetricsConfig();
    
    @Data
    public static class AsyncConfig {
        private int corePoolSize = 5;
        private int maxPoolSize = 20;
        private int queueCapacity = 100;
        private String threadNamePrefix = "email-processor-";
    }
    
    @Data
    public static class MetricsConfig {
        private boolean enabled = true;
        private Duration exportInterval = Duration.ofSeconds(60);
    }
}
```

### 3. 高级注解处理器示例

```java
@Component
public class AdvancedEmailProcessor {
    
    @Resource
    private RedisTemplate<String, String> redisTemplate;
    
    @Resource
    private NotificationService notificationService;
    
    /**
     * 处理银行通知邮件
     */
    @EasyMailProcessor(
        from = ".*@bank\\.com",
        subjectPattern = "账户.*通知",
        priority = 1
    )
    public void handleBankNotification(Message message, String content, String subject, String from) {
        try {
            // 解析银行通知内容
            BankNotification notification = parseBankNotification(content);
            
            // 存储到数据库
            saveBankNotification(notification);
            
            // 发送实时通知
            notificationService.sendRealTimeNotification(notification);
            
            log.info("处理银行通知成功: {}", notification.getTransactionId());
            
        } catch (Exception e) {
            log.error("处理银行通知失败", e);
        }
    }
    
    /**
     * 处理验证码邮件
     */
    @EasyMailProcessor(
        subjectPattern = ".*(验证码|verification|code).*",
        contentPattern = ".*\\b(\\d{4,6})\\b.*",
        priority = 2
    )
    public void handleVerificationCode(Message message, String content, String subject, String from) {
        try {
            // 提取验证码
            String code = extractVerificationCode(content);
            if (code == null) {
                log.warn("未能提取验证码: {}", subject);
                return;
            }
            
            // 提取邮箱地址
            String email = extractEmailFromContent(content);
            if (email == null) {
                email = extractEmailFromFrom(from);
            }
            
            // 存储验证码到 Redis（5分钟过期）
            String key = "verification_code:" + email;
            redisTemplate.opsForValue().set(key, code, Duration.ofMinutes(5));
            
            log.info("保存验证码成功: {} -> {}", email, code);
            
        } catch (Exception e) {
            log.error("处理验证码邮件失败", e);
        }
    }
    
    /**
     * 处理订单通知邮件
     */
    @EasyMailProcessor(
        subjectPattern = ".*(订单|order).*",
        priority = 3
    )
    public void handleOrderNotification(Message message, String content, String subject, String from) {
        try {
            // 解析订单信息
            OrderInfo orderInfo = parseOrderInfo(content);
            
            if (orderInfo != null) {
                // 更新订单状态
                updateOrderStatus(orderInfo);
                
                // 发送推送通知
                sendPushNotification(orderInfo);
                
                log.info("处理订单通知成功: {}", orderInfo.getOrderId());
            }
            
        } catch (Exception e) {
            log.error("处理订单通知失败", e);
        }
    }
    
    // 辅助方法
    private BankNotification parseBankNotification(String content) {
        // 实现银行通知解析逻辑
        return new BankNotification();
    }
    
    private String extractVerificationCode(String content) {
        Pattern pattern = Pattern.compile("\\b(\\d{4,6})\\b");
        Matcher matcher = pattern.matcher(content);
        return matcher.find() ? matcher.group(1) : null;
    }
    
    private String extractEmailFromContent(String content) {
        Pattern pattern = Pattern.compile("\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Z|a-z]{2,}\\b");
        Matcher matcher = pattern.matcher(content);
        return matcher.find() ? matcher.group() : null;
    }
    
    private String extractEmailFromFrom(String from) {
        // 从发件人字段提取邮箱地址
        Pattern pattern = Pattern.compile("<([^>]+)>");
        Matcher matcher = pattern.matcher(from);
        return matcher.find() ? matcher.group(1) : from;
    }
}
```

### 2. 函数式处理器

```java
@Configuration
public class FunctionalProcessorConfig {
    
    @Bean
    public EasyMailProcessorFunction smartFilterProcessor() {
        return (message, content, subject, from) -> {
            // 智能过滤垃圾邮件
            if (isSpamEmail(content, subject, from)) {
                log.info("检测到垃圾邮件，已过滤: {}", subject);
                return true; // 标记为已处理，避免其他处理器处理
            }
            return false; // 继续交给其他处理器
        };
    }
    
    @Bean
    public EasyMailProcessorFunction contentAnalysisProcessor() {
        return (message, content, subject, from) -> {
            try {
                // 内容分析和分类
                EmailCategory category = analyzeEmailContent(content, subject);
                
                // 根据分类进行不同处理
                switch (category) {
                    case FINANCIAL:
                        handleFinancialEmail(content, subject, from);
                        break;
                    case SOCIAL:
                        handleSocialEmail(content, subject, from);
                        break;
                    case PROMOTIONAL:
                        handlePromotionalEmail(content, subject, from);
                        break;
                    default:
                        handleGeneralEmail(content, subject, from);
                }
                
                return true;
                
            } catch (Exception e) {
                log.error("内容分析处理失败", e);
                return false;
            }
        };
    }
    
    @Bean
    public EasyMailProcessorFunction attachmentProcessor() {
        return (message, content, subject, from) -> {
            try {
                // 处理附件
                if (message instanceof MimeMessage) {
                    MimeMessage mimeMessage = (MimeMessage) message;
                    List<File> attachments = extractAttachments(mimeMessage);
                    
                    for (File attachment : attachments) {
                        processAttachment(attachment, from, subject);
                    }
                }
                
                return true;
                
            } catch (Exception e) {
                log.error("附件处理失败", e);
                return false;
            }
        };
    }
    
    private boolean isSpamEmail(String content, String subject, String from) {
        // 实现垃圾邮件检测逻辑
        String[] spamKeywords = {"中奖", "免费", "赚钱", "点击这里"};
        String lowerContent = content.toLowerCase();
        String lowerSubject = subject.toLowerCase();
        
        return Arrays.stream(spamKeywords)
                .anyMatch(keyword -> lowerContent.contains(keyword) || lowerSubject.contains(keyword));
    }
    
    private EmailCategory analyzeEmailContent(String content, String subject) {
        // 实现邮件内容分析逻辑
        if (content.contains("银行") || content.contains("账户") || content.contains("转账")) {
            return EmailCategory.FINANCIAL;
        } else if (content.contains("朋友") || content.contains("社交")) {
            return EmailCategory.SOCIAL;
        } else if (content.contains("优惠") || content.contains("促销")) {
            return EmailCategory.PROMOTIONAL;
        }
        return EmailCategory.GENERAL;
    }
    
    enum EmailCategory {
        FINANCIAL, SOCIAL, PROMOTIONAL, GENERAL
    }
}
```

### 3. 链式处理器

```java
@Component
public class ChainedEmailProcessor implements EasyMailListenerApi {
    
    private final List<EmailProcessorChain> processorChains;
    
    public ChainedEmailProcessor() {
        this.processorChains = Arrays.asList(
            new ValidationChain(),
            new FilterChain(),
            new ExtractionChain(),
            new StorageChain(),
            new NotificationChain()
        );
    }
    
    @Override
    public boolean processEmail(Message message, String content, String subject, String from) {
        EmailContext context = new EmailContext(message, content, subject, from);
        
        // 依次执行处理链
        for (EmailProcessorChain chain : processorChains) {
            try {
                ChainResult result = chain.process(context);
                
                if (result == ChainResult.STOP) {
                    log.info("处理链中断: {}", chain.getClass().getSimpleName());
                    break;
                } else if (result == ChainResult.SKIP) {
                    log.debug("跳过处理链: {}", chain.getClass().getSimpleName());
                    continue;
                }
                
            } catch (Exception e) {
                log.error("处理链执行失败: {}", chain.getClass().getSimpleName(), e);
                
                // 根据配置决定是否继续
                if (chain.isFailureFatal()) {
                    return false;
                }
            }
        }
        
        return true;
    }
    
    // 处理链接口
    interface EmailProcessorChain {
        ChainResult process(EmailContext context);
        boolean isFailureFatal();
    }
    
    enum ChainResult {
        CONTINUE, STOP, SKIP
    }
    
    // 邮件上下文
    @Data
    static class EmailContext {
        private final Message message;
        private final String content;
        private final String subject;
        private final String from;
        private final Map<String, Object> attributes = new HashMap<>();
        
        public EmailContext(Message message, String content, String subject, String from) {
            this.message = message;
            this.content = content;
            this.subject = subject;
            this.from = from;
        }
    }
    
    // 验证链
    static class ValidationChain implements EmailProcessorChain {
        @Override
        public ChainResult process(EmailContext context) {
            // 验证邮件格式和内容
            if (context.getContent() == null || context.getContent().trim().isEmpty()) {
                log.warn("邮件内容为空，跳过处理");
                return ChainResult.STOP;
            }
            return ChainResult.CONTINUE;
        }
        
        @Override
        public boolean isFailureFatal() {
            return true;
        }
    }
    
    // 过滤链
    static class FilterChain implements EmailProcessorChain {
        @Override
        public ChainResult process(EmailContext context) {
            // 过滤垃圾邮件或不需要的邮件
            if (isSpamOrUnwanted(context)) {
                log.info("过滤不需要的邮件: {}", context.getSubject());
                return ChainResult.STOP;
            }
            return ChainResult.CONTINUE;
        }
        
        @Override
        public boolean isFailureFatal() {
            return false;
        }
        
        private boolean isSpamOrUnwanted(EmailContext context) {
            // 实现过滤逻辑
            return false;
        }
    }
}
```

## 自定义缓存管理器

### 1. Redis 缓存管理器

```java
@Component
public class RedisEasyMailCacheManager implements EasyMailCacheManager {
    
    @Resource
    private RedisTemplate<String, String> redisTemplate;
    
    private static final String CACHE_PREFIX = "easymail:cache:";
    private static final String PROCESSED_PREFIX = "easymail:processed:";
    
    @Override
    public boolean checkAndMarkAsProcessed(String messageId) {
        String key = PROCESSED_PREFIX + messageId;
        
        // 使用 Redis 的 SETNX 命令实现原子性检查和设置
        Boolean success = redisTemplate.opsForValue().setIfAbsent(key, "1", Duration.ofDays(7));
        
        return Boolean.TRUE.equals(success);
    }
    
    @Override
    public boolean isProcessed(String messageId) {
        String key = PROCESSED_PREFIX + messageId;
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }
    
    @Override
    public void markAsProcessed(String messageId) {
        String key = PROCESSED_PREFIX + messageId;
        redisTemplate.opsForValue().set(key, "1", Duration.ofDays(7));
    }
    
    @Override
    public String getMessageId(Message message) {
        try {
            // 尝试从缓存获取
            String cacheKey = CACHE_PREFIX + "msgid:" + message.hashCode();
            String cachedId = redisTemplate.opsForValue().get(cacheKey);
            
            if (cachedId != null) {
                return cachedId;
            }
            
            // 生成消息ID
            String messageId = generateMessageId(message);
            
            // 缓存消息ID
            redisTemplate.opsForValue().set(cacheKey, messageId, Duration.ofHours(24));
            
            return messageId;
            
        } catch (Exception e) {
            log.error("获取消息ID失败", e);
            return "unknown-" + System.currentTimeMillis();
        }
    }
    
    @Override
    public String getSubjectSafely(Message message) {
        try {
            // 尝试从缓存获取
            String cacheKey = CACHE_PREFIX + "subject:" + message.hashCode();
            String cachedSubject = redisTemplate.opsForValue().get(cacheKey);
            
            if (cachedSubject != null) {
                return cachedSubject;
            }
            
            // 获取主题
            String subject = message.getSubject();
            if (subject == null) {
                subject = "(无主题)";
            }
            
            // 缓存主题
            redisTemplate.opsForValue().set(cacheKey, subject, Duration.ofHours(24));
            
            return subject;
            
        } catch (Exception e) {
            log.error("获取邮件主题失败", e);
            return "(获取失败)";
        }
    }
    
    @Override
    public void clearCache() {
        try {
            Set<String> keys = redisTemplate.keys(CACHE_PREFIX + "*");
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
            }
        } catch (Exception e) {
            log.error("清理缓存失败", e);
        }
    }
    
    @Override
    public void clearProcessedMessages() {
        try {
            Set<String> keys = redisTemplate.keys(PROCESSED_PREFIX + "*");
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
            }
        } catch (Exception e) {
            log.error("清理已处理消息记录失败", e);
        }
    }
    
    private String generateMessageId(Message message) throws MessagingException {
        // 尝试获取邮件的 Message-ID 头
        String[] messageIds = message.getHeader("Message-ID");
        if (messageIds != null && messageIds.length > 0 && messageIds[0] != null) {
            return messageIds[0];
        }
        
        // 如果没有 Message-ID，使用其他信息生成
        StringBuilder idBuilder = new StringBuilder();
        idBuilder.append(message.getSentDate() != null ? message.getSentDate().getTime() : System.currentTimeMillis());
        idBuilder.append("-");
        idBuilder.append(message.getSubject() != null ? message.getSubject().hashCode() : 0);
        idBuilder.append("-");
        idBuilder.append(message.getFrom() != null ? Arrays.hashCode(message.getFrom()) : 0);
        
        return idBuilder.toString();
    }
}
```

### 2. 多级缓存管理器

```java
@Component
public class MultiLevelEasyMailCacheManager implements EasyMailCacheManager {
    
    // L1 缓存：本地内存缓存
    private final Cache<String, String> l1Cache;
    
    // L2 缓存：Redis 缓存
    @Resource
    private RedisTemplate<String, String> redisTemplate;
    
    // L3 缓存：数据库
    @Resource
    private ProcessedMessageRepository processedMessageRepository;
    
    public MultiLevelEasyMailCacheManager() {
        this.l1Cache = Caffeine.newBuilder()
                .maximumSize(10000)
                .expireAfterWrite(Duration.ofMinutes(30))
                .build();
    }
    
    @Override
    public boolean checkAndMarkAsProcessed(String messageId) {
        // L1 缓存检查
        if (l1Cache.getIfPresent(messageId) != null) {
            return false; // 已处理
        }
        
        // L2 缓存检查
        String redisKey = "processed:" + messageId;
        if (Boolean.TRUE.equals(redisTemplate.hasKey(redisKey))) {
            // 回填 L1 缓存
            l1Cache.put(messageId, "1");
            return false; // 已处理
        }
        
        // L3 数据库检查
        if (processedMessageRepository.existsByMessageId(messageId)) {
            // 回填缓存
            l1Cache.put(messageId, "1");
            redisTemplate.opsForValue().set(redisKey, "1", Duration.ofDays(7));
            return false; // 已处理
        }
        
        // 标记为已处理
        l1Cache.put(messageId, "1");
        redisTemplate.opsForValue().set(redisKey, "1", Duration.ofDays(7));
        processedMessageRepository.save(new ProcessedMessage(messageId, Instant.now()));
        
        return true; // 首次处理
    }
    
    @Override
    public boolean isProcessed(String messageId) {
        // 依次检查各级缓存
        if (l1Cache.getIfPresent(messageId) != null) {
            return true;
        }
        
        String redisKey = "processed:" + messageId;
        if (Boolean.TRUE.equals(redisTemplate.hasKey(redisKey))) {
            l1Cache.put(messageId, "1");
            return true;
        }
        
        if (processedMessageRepository.existsByMessageId(messageId)) {
            l1Cache.put(messageId, "1");
            redisTemplate.opsForValue().set(redisKey, "1", Duration.ofDays(7));
            return true;
        }
        
        return false;
    }
    
    @Override
    public void clearCache() {
        l1Cache.invalidateAll();
        
        Set<String> redisKeys = redisTemplate.keys("easymail:*");
        if (redisKeys != null && !redisKeys.isEmpty()) {
            redisTemplate.delete(redisKeys);
        }
    }
}
```

## 自定义异常处理器

### 1. 全局异常处理器

```java
@Component
public class CustomEasyMailExceptionHandler implements EasyMailExceptionHandler {
    
    @Resource
    private NotificationService notificationService;
    
    @Resource
    private MetricsService metricsService;
    
    @Override
    public void handleSendException(EasyMailSendException exception, EasyMailRequest request) {
        log.error("邮件发送异常", exception);
        
        // 记录指标
        metricsService.incrementCounter("email.send.error", 
                "error_type", exception.getClass().getSimpleName());
        
        // 根据异常类型进行不同处理
        if (exception instanceof EasyMailConnectionException) {
            handleConnectionException((EasyMailConnectionException) exception, request);
        } else if (exception instanceof EasyMailValidationException) {
            handleValidationException((EasyMailValidationException) exception, request);
        } else {
            handleGeneralSendException(exception, request);
        }
    }
    
    @Override
    public void handleProcessException(EasyMailProcessException exception, Message message) {
        log.error("邮件处理异常", exception);
        
        // 记录指标
        metricsService.incrementCounter("email.process.error",
                "error_type", exception.getClass().getSimpleName());
        
        // 尝试恢复处理
        if (exception.isRecoverable()) {
            scheduleRetryProcessing(message, exception.getRetryDelay());
        } else {
            // 发送告警
            sendAlert("邮件处理不可恢复异常", exception, message);
        }
    }
    
    @Override
    public void handleConfigException(EasyMailConfigException exception) {
        log.error("邮件配置异常", exception);
        
        // 配置异常通常是致命的，需要立即告警
        sendCriticalAlert("邮件服务配置异常", exception);
        
        // 尝试使用默认配置
        if (exception.canUseDefaultConfig()) {
            log.warn("使用默认配置继续运行");
            applyDefaultConfig();
        }
    }
    
    private void handleConnectionException(EasyMailConnectionException exception, EasyMailRequest request) {
        // 连接异常处理
        if (exception.isTemporary()) {
            // 临时连接问题，安排重试
            scheduleRetry(request, exception.getRetryAfter());
        } else {
            // 持久连接问题，发送告警
            sendAlert("邮件服务连接异常", exception, request);
        }
    }
    
    private void handleValidationException(EasyMailValidationException exception, EasyMailRequest request) {
        // 验证异常处理
        log.warn("邮件请求验证失败: {}", exception.getMessage());
        
        // 记录无效请求
        recordInvalidRequest(request, exception);
        
        // 如果是用户输入错误，不需要重试
        if (exception.isUserInputError()) {
            notifyUser(request, "邮件发送失败：" + exception.getMessage());
        }
    }
    
    private void handleGeneralSendException(EasyMailSendException exception, EasyMailRequest request) {
        // 通用发送异常处理
        if (exception.getRetryCount() < 3) {
            // 重试
            scheduleRetry(request, Duration.ofMinutes(1));
        } else {
            // 超过重试次数，记录失败
            recordFailedEmail(request, exception);
            sendAlert("邮件发送最终失败", exception, request);
        }
    }
    
    private void scheduleRetry(EasyMailRequest request, Duration delay) {
        // 实现重试调度逻辑
        log.info("安排邮件重试，延迟: {}", delay);
    }
    
    private void scheduleRetryProcessing(Message message, Duration delay) {
        // 实现处理重试调度逻辑
        log.info("安排邮件处理重试，延迟: {}", delay);
    }
    
    private void sendAlert(String title, Exception exception, Object context) {
        // 发送告警通知
        notificationService.sendAlert(title, exception.getMessage(), context);
    }
    
    private void sendCriticalAlert(String title, Exception exception) {
        // 发送严重告警
        notificationService.sendCriticalAlert(title, exception.getMessage());
    }
}
```

### 2. 重试机制

```java
@Component
public class EasyMailRetryHandler {
    
    private final ScheduledExecutorService retryExecutor;
    
    @Resource
    private EasyMailSenderService easyMailSenderService;
    
    public EasyMailRetryHandler() {
        this.retryExecutor = Executors.newScheduledThreadPool(5);
    }
    
    public void scheduleRetry(EasyMailRequest request, Duration delay, int maxRetries) {
        retryExecutor.schedule(() -> {
            executeRetry(request, maxRetries - 1);
        }, delay.toMillis(), TimeUnit.MILLISECONDS);
    }
    
    private void executeRetry(EasyMailRequest request, int remainingRetries) {
        try {
            EasyMailSendResult result = easyMailSenderService.send(request);
            
            if (result.isSuccess()) {
                log.info("邮件重试发送成功: {}", result.getMessageId());
            } else if (remainingRetries > 0) {
                // 继续重试
                Duration nextDelay = calculateNextDelay(3 - remainingRetries);
                scheduleRetry(request, nextDelay, remainingRetries);
            } else {
                log.error("邮件重试最终失败: {}", result.getErrorMessage());
            }
            
        } catch (Exception e) {
            if (remainingRetries > 0) {
                Duration nextDelay = calculateNextDelay(3 - remainingRetries);
                scheduleRetry(request, nextDelay, remainingRetries);
            } else {
                log.error("邮件重试异常，已达最大重试次数", e);
            }
        }
    }
    
    private Duration calculateNextDelay(int attemptNumber) {
        // 指数退避算法
        long delayMs = (long) (1000 * Math.pow(2, attemptNumber));
        return Duration.ofMillis(Math.min(delayMs, 30000)); // 最大30秒
    }
}
```

## 扩展配置

### 1. 自定义自动配置

```java
@Configuration
@ConditionalOnProperty(prefix = "easymail.custom", name = "enabled", havingValue = "true")
public class CustomEasyMailAutoConfiguration {
    
    @Bean
    @ConditionalOnMissingBean
    public CustomEasyMailSendStrategy customSendStrategy() {
        return new CustomEasyMailSendStrategy();
    }
    
    @Bean
    @ConditionalOnProperty(prefix = "easymail.template", name = "engine", havingValue = "thymeleaf")
    public ThymeleafEasyMailTemplateEngine thymeleafTemplateEngine() {
        return new ThymeleafEasyMailTemplateEngine();
    }
    
    @Bean
    @ConditionalOnProperty(prefix = "easymail.cache", name = "type", havingValue = "redis")
    public RedisEasyMailCacheManager redisCacheManager() {
        return new RedisEasyMailCacheManager();
    }
    
    @Bean
    @ConditionalOnProperty(prefix = "easymail.monitoring", name = "enabled", havingValue = "true")
    public EasyMailMonitoringService monitoringService() {
        return new EasyMailMonitoringService();
    }
}
```

### 2. 配置属性

```java
@Data
@ConfigurationProperties(prefix = "easymail.custom")
public class CustomEasyMailProperties {
    
    /**
     * 是否启用自定义功能
     */
    private boolean enabled = false;
    
    /**
     * 模板配置
     */
    private Template template = new Template();
    
    /**
     * 缓存配置
     */
    private Cache cache = new Cache();
    
    /**
     * 监控配置
     */
    private Monitoring monitoring = new Monitoring();
    
    @Data
    public static class Template {
        private String engine = "simple";
        private String basePath = "classpath:templates/email";
        private boolean cacheEnabled = true;
        private Duration cacheExpire = Duration.ofHours(1);
    }
    
    @Data
    public static class Cache {
        private String type = "memory";
        private int maxSize = 10000;
        private Duration expireAfterWrite = Duration.ofMinutes(30);
        private Duration expireAfterAccess = Duration.ofMinutes(10);
    }
    
    @Data
    public static class Monitoring {
        private boolean enabled = false;
        private Duration metricsRetention = Duration.ofDays(7);
        private boolean alertEnabled = true;
        private String alertWebhook;
    }
}
```

## 性能调优

### 1. 连接池优化

```java
@Configuration
public class EasyMailPerformanceConfig {
    
    @Bean
    @Primary
    public ThreadPoolExecutor easyMailThreadPool() {
        return new ThreadPoolExecutor(
            10,  // 核心线程数
            50,  // 最大线程数
            60L, TimeUnit.SECONDS, // 空闲时间
            new LinkedBlockingQueue<>(1000), // 队列大小
            new ThreadFactoryBuilder()
                .setNameFormat("easymail-executor-%d")
                .setDaemon(true)
                .build(),
            new ThreadPoolExecutor.CallerRunsPolicy() // 拒绝策略
        );
    }
    
    @Bean
    public ScheduledThreadPoolExecutor easyMailScheduledExecutor() {
        ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(
            5,
            new ThreadFactoryBuilder()
                .setNameFormat("easymail-scheduled-%d")
                .setDaemon(true)
                .build()
        );
        
        executor.setRemoveOnCancelPolicy(true);
        return executor;
    }
}
```

### 2. 批量处理优化

```java
@Component
public class BatchOptimizedEasyMailSender {
    
    @Resource
    private EasyMailSenderService easyMailSenderService;
    
    private final BlockingQueue<EasyMailRequest> emailQueue = new LinkedBlockingQueue<>();
    private final ScheduledExecutorService batchProcessor = Executors.newSingleThreadScheduledExecutor();
    
    @PostConstruct
    public void startBatchProcessing() {
        batchProcessor.scheduleWithFixedDelay(this::processBatch, 1, 5, TimeUnit.SECONDS);
    }
    
    public void queueEmail(EasyMailRequest request) {
        emailQueue.offer(request);
    }
    
    private void processBatch() {
        List<EasyMailRequest> batch = new ArrayList<>();
        emailQueue.drainTo(batch, 100); // 每批最多100封
        
        if (!batch.isEmpty()) {
            processBatchEmails(batch);
        }
    }
    
    private void processBatchEmails(List<EasyMailRequest> batch) {
        // 按收件人分组
        Map<String, List<EasyMailRequest>> groupedByRecipient = batch.stream()
            .collect(Collectors.groupingBy(req -> req.getTo()));
        
        // 并行处理每组
        groupedByRecipient.entrySet().parallelStream().forEach(entry -> {
            String recipient = entry.getKey();
            List<EasyMailRequest> requests = entry.getValue();
            
            // 合并同一收件人的邮件
            EasyMailRequest mergedRequest = mergeRequests(requests);
            
            try {
                easyMailSenderService.send(mergedRequest);
                log.info("批量发送成功，收件人: {}, 邮件数: {}", recipient, requests.size());
            } catch (Exception e) {
                log.error("批量发送失败，收件人: {}", recipient, e);
            }
        });
    }
    
    private EasyMailRequest mergeRequests(List<EasyMailRequest> requests) {
        // 实现邮件合并逻辑
        if (requests.size() == 1) {
            return requests.get(0);
        }
        
        // 合并多个邮件为一个
        StringBuilder content = new StringBuilder();
        content.append("您有 ").append(requests.size()).append(" 条新消息：\n\n");
        
        for (int i = 0; i < requests.size(); i++) {
            EasyMailRequest req = requests.get(i);
            content.append(i + 1).append(". ").append(req.getSubject()).append("\n");
            content.append(req.getText()).append("\n\n");
        }
        
        return EasyMailRequest.builder()
            .to(requests.get(0).getTo())
            .subject("您有 " + requests.size() + " 条新消息")
            .text(content.toString())
            .build();
    }
}
```

## 监控与诊断

### 1. 指标收集

```java
@Component
public class EasyMailMetricsCollector {
    
    private final MeterRegistry meterRegistry;
    private final Counter sendSuccessCounter;
    private final Counter sendFailureCounter;
    private final Timer sendDurationTimer;
    private final Gauge queueSizeGauge;
    
    public EasyMailMetricsCollector(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        
        this.sendSuccessCounter = Counter.builder("easymail.send.success")
            .description("成功发送的邮件数量")
            .register(meterRegistry);
            
        this.sendFailureCounter = Counter.builder("easymail.send.failure")
            .description("发送失败的邮件数量")
            .register(meterRegistry);
            
        this.sendDurationTimer = Timer.builder("easymail.send.duration")
            .description("邮件发送耗时")
            .register(meterRegistry);
            
        this.queueSizeGauge = Gauge.builder("easymail.queue.size")
            .description("邮件队列大小")
            .register(meterRegistry, this, EasyMailMetricsCollector::getQueueSize);
    }
    
    public void recordSendSuccess(String strategy, long duration) {
        sendSuccessCounter.increment(Tags.of("strategy", strategy));
        sendDurationTimer.record(duration, TimeUnit.MILLISECONDS);
    }
    
    public void recordSendFailure(String strategy, String errorType) {
        sendFailureCounter.increment(Tags.of("strategy", strategy, "error_type", errorType));
    }
    
    private double getQueueSize() {
        // 返回当前队列大小
        return 0; // 实际实现中应该返回真实的队列大小
    }
}
```

### 2. 健康检查

```java
@Component
public class EasyMailHealthIndicator implements HealthIndicator {
    
    @Resource
    private EasyMailSenderService easyMailSenderService;
    
    @Resource
    private EasyMailListenerService easyMailListenerService;
    
    @Override
    public Health health() {
        Health.Builder builder = new Health.Builder();
        
        try {
            // 检查发送服务
            boolean senderHealthy = checkSenderHealth();
            
            // 检查监听服务
            boolean listenerHealthy = checkListenerHealth();
            
            if (senderHealthy && listenerHealthy) {
                builder.up()
                    .withDetail("sender", "UP")
                    .withDetail("listener", "UP");
            } else {
                builder.down()
                    .withDetail("sender", senderHealthy ? "UP" : "DOWN")
                    .withDetail("listener", listenerHealthy ? "UP" : "DOWN");
            }
            
        } catch (Exception e) {
            builder.down(e);
        }
        
        return builder.build();
    }
    
    private boolean checkSenderHealth() {
        try {
            // 发送测试邮件或检查连接
            return easyMailSenderService.isHealthy();
        } catch (Exception e) {
            return false;
        }
    }
    
    private boolean checkListenerHealth() {
        try {
            // 检查监听器状态
            return easyMailListenerService.isRunning();
        } catch (Exception e) {
            return false;
        }
    }
}
```

### 3. 诊断工具

```java
@RestController
@RequestMapping("/easymail/diagnostics")
public class EasyMailDiagnosticsController {
    
    @Resource
    private EasyMailSenderService easyMailSenderService;
    
    @Resource
    private EasyMailMetricsCollector metricsCollector;
    
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        Map<String, Object> status = new HashMap<>();
        
        status.put("timestamp", Instant.now());
        status.put("sender_status", getSenderStatus());
        status.put("listener_status", getListenerStatus());
        status.put("metrics", getMetrics());
        
        return ResponseEntity.ok(status);
    }
    
    @PostMapping("/test-send")
    public ResponseEntity<EasyMailSendResult> testSend(@RequestParam String to) {
        try {
            EasyMailRequest testRequest = EasyMailRequest.builder()
                .to(to)
                .subject("EasyMail 测试邮件")
                .text("这是一封测试邮件，发送时间：" + Instant.now())
                .build();
                
            EasyMailSendResult result = easyMailSenderService.send(testRequest);
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            return ResponseEntity.status(500)
                .body(EasyMailSendResult.failure("测试发送失败：" + e.getMessage()));
        }
    }
    
    @GetMapping("/config")
    public ResponseEntity<Map<String, Object>> getConfig() {
        Map<String, Object> config = new HashMap<>();
        
        // 返回当前配置信息（脱敏）
        config.put("strategies", getAvailableStrategies());
        config.put("templates", getAvailableTemplates());
        config.put("processors", getRegisteredProcessors());
        
        return ResponseEntity.ok(config);
    }
    
    private Map<String, Object> getSenderStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("healthy", true);
        status.put("last_send", Instant.now().minusMinutes(5));
        return status;
    }
    
    private Map<String, Object> getListenerStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("running", true);
        status.put("last_check", Instant.now().minusSeconds(30));
        return status;
    }
    
    private Map<String, Object> getMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("send_success_total", 100);
        metrics.put("send_failure_total", 5);
        metrics.put("avg_send_duration_ms", 1500);
        return metrics;
    }
}
```

## 集成示例

### 1. Spring Boot 完整集成

```java
@SpringBootApplication
@EnableEasyMail
public class EasyMailDemoApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(EasyMailDemoApplication.class, args);
    }
    
    @Bean
    public CustomEasyMailSendStrategy customStrategy() {
        return new CustomEasyMailSendStrategy();
    }
    
    @Bean
    public ThymeleafEasyMailTemplateEngine thymeleafEngine() {
        return new ThymeleafEasyMailTemplateEngine();
    }
}
```

### 2. 微服务集成

```java
@Service
public class UserNotificationService {
    
    @Resource
    private EasyMailSenderService easyMailSenderService;
    
    @Async
    public CompletableFuture<Void> sendWelcomeEmail(User user) {
        return CompletableFuture.runAsync(() -> {
            try {
                Map<String, Object> variables = new HashMap<>();
                variables.put("username", user.getUsername());
                variables.put("activationLink", generateActivationLink(user));
                
                EasyMailRequest request = EasyMailRequest.builder()
                    .to(user.getEmail())
                    .templateId("welcome")
                    .templateVariables(variables)
                    .build();
                    
                EasyMailSendResult result = easyMailSenderService.send(request);
                
                if (result.isSuccess()) {
                    log.info("欢迎邮件发送成功: {}", user.getEmail());
                } else {
                    log.error("欢迎邮件发送失败: {}", result.getErrorMessage());
                }
                
            } catch (Exception e) {
                log.error("发送欢迎邮件异常", e);
            }
        });
    }
    
    private String generateActivationLink(User user) {
        return "https://example.com/activate?token=" + user.getActivationToken();
    }
}
```

### 3. 事件驱动集成

```java
@Component
public class EmailEventHandler {
    
    @Resource
    private EasyMailSenderService easyMailSenderService;
    
    @EventListener
    public void handleUserRegistered(UserRegisteredEvent event) {
        sendWelcomeEmail(event.getUser());
    }
    
    @EventListener
    public void handleOrderCreated(OrderCreatedEvent event) {
        sendOrderConfirmationEmail(event.getOrder());
    }
    
    @EventListener
    public void handlePasswordReset(PasswordResetEvent event) {
        sendPasswordResetEmail(event.getUser(), event.getResetToken());
    }
    
    private void sendWelcomeEmail(User user) {
        EasyMailRequest request = EasyMailRequest.builder()
            .to(user.getEmail())
            .templateId("user-welcome")
            .templateVariable("username", user.getUsername())
            .build();
            
        easyMailSenderService.sendAsync(request);
    }
    
    private void sendOrderConfirmationEmail(Order order) {
        EasyMailRequest request = EasyMailRequest.builder()
            .to(order.getCustomerEmail())
            .templateId("order-confirmation")
            .templateVariable("orderNumber", order.getOrderNumber())
            .templateVariable("orderItems", order.getItems())
            .templateVariable("totalAmount", order.getTotalAmount())
            .build();
            
        easyMailSenderService.sendAsync(request);
    }
    
    private void sendPasswordResetEmail(User user, String resetToken) {
        String resetLink = "https://example.com/reset-password?token=" + resetToken;
        
        EasyMailRequest request = EasyMailRequest.builder()
            .to(user.getEmail())
            .templateId("password-reset")
            .templateVariable("username", user.getUsername())
            .templateVariable("resetLink", resetLink)
            .build();
            
        easyMailSenderService.sendAsync(request);
    }
}
```

### 4. 配置文件示例

```yaml
# application.yml
mail:
  smtp:
    host: smtp.example.com
    port: 587
    username: your-email@example.com
    password: your-app-password
    protocol: smtp
    auth: true
    starttls:
      enable: true
  
  imap:
    host: imap.example.com
    port: 993
    username: your-email@example.com
    password: your-app-password
    protocol: imaps
    
  sender:
    enabled: true
    default-strategy: smart-retry
    batch-size: 50
    retry:
      max-attempts: 3
      initial-delay: 1s
      max-delay: 30s
    monitoring:
      enabled: true
    
  listener:
    enabled: true
    auto-start: true
    folder: INBOX
    startup-process-strategy: MARK_AS_READ_ONLY
    idle-timeout: 30m
    keep-alive-interval: 5m
    reconnect-delay: 10s
    
easymail:
  custom:
    enabled: true
    template:
      engine: thymeleaf
      base-path: classpath:templates/email
      cache-enabled: true
      cache-expire: 1h
    cache:
      type: redis
      max-size: 10000
      expire-after-write: 30m
    monitoring:
      enabled: true
      metrics-retention: 7d
      alert-enabled: true
      alert-webhook: https://hooks.slack.com/services/YOUR/WEBHOOK/URL
```

## 最佳实践总结

### 1. 性能优化
- 使用连接池管理邮件连接
- 实现批量发送减少网络开销
- 合理配置线程池大小
- 启用缓存减少重复计算

### 2. 可靠性保障
- 实现重试机制处理临时故障
- 使用多级缓存提高可用性
- 配置健康检查监控服务状态
- 实现优雅降级策略

### 3. 安全考虑
- 使用应用密码而非账户密码
- 启用 TLS/SSL 加密传输
- 验证邮件地址格式
- 防止邮件注入攻击

### 4. 监控运维
- 收集关键指标数据
- 配置告警通知
- 定期检查服务健康状态
- 保留足够的日志信息

### 5. 扩展开发
- 遵循接口设计原则
- 使用依赖注入提高可测试性
- 实现配置外部化
- 提供丰富的扩展点

通过以上高级功能和最佳实践，您可以构建一个功能强大、性能优异、高度可定制的企业级邮件服务系统。