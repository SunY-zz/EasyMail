# EasyMail 动态线程池使用指南

## 概述

EasyMail 框架内置了一个高性能的动态线程池组件，支持根据实际负载自动调整线程池参数。其他项目引入 EasyMail 后，不仅可以使用邮件功能，还可以直接使用这个强大的动态线程池。

## 核心特性

### 🚀 智能调整
- **自适应扩缩容**：根据队列使用率和线程活跃度自动调整
- **安全边界控制**：防止资源过度消耗或不足
- **频率限制**：避免频繁调整导致的系统抖动

### 📊 实时监控
- **多维度指标**：线程数、队列状态、任务处理速率等
- **性能分析**：等待时间、处理效率、负载等级
- **可视化日志**：详细的调整过程和性能指标

### 🛡️ 高可靠性
- **异常隔离**：监控和调整异常不影响业务执行
- **配置验证**：启动时验证配置参数合理性
- **优雅关闭**：支持线程池的优雅停机

## 快速开始

### 1. 添加依赖

在你的项目中添加 EasyMail 依赖：

```xml
<dependency>
    <groupId>cn.sunyblog.easymail</groupId>
    <artifactId>easymail-spring-boot-starter</artifactId>
    <version>1.0.2</version>
</dependency>
```

### 2. 配置文件

在 `application.yml` 中添加动态线程池配置：

```yaml
mail:
  # 动态线程池配置
  dynamic-thread-pool:
    enabled: true # 是否启用动态线程池
    # 基本配置
    basic:
      core-pool-size: 5 # 核心线程数
      maximum-pool-size: 20 # 最大线程数
      queue-capacity: 100 # 队列容量
      keep-alive-time: 60 # 线程存活时间(秒)
      thread-pool-name: MyApp-DynamicPool # 线程池名称前缀
    # 监控配置
    monitor:
      interval: 30 # 监控间隔(秒)
      enable-auto-adjustment: true # 是否启用自动调整
      enable-metrics-logging: true # 是否启用指标日志
    # 调整策略配置
    adjustment:
      strategy-class: cn.sunyblog.easymail.dynamicThreadPool.strategy.DefaultAdjustmentStrategy
      min-interval: 60 # 最小调整间隔(秒)
      max-per-hour: 10 # 每小时最大调整次数
      queue-usage-expand-threshold: 0.8 # 队列使用率扩容阈值
      queue-usage-shrink-threshold: 0.3 # 队列使用率缩容阈值
      thread-activity-expand-threshold: 0.9 # 线程池活跃度扩容阈值
      thread-activity-shrink-threshold: 0.5 # 线程池活跃度缩容阈值
      task-wait-time-threshold: 1000 # 任务等待时间阈值(毫秒)
      shrink-duration: 300 # 缩容持续时间要求(秒)
    # 安全边界配置
    safety:
      min-core-pool-size: 1 # 最小核心线程数
      max-core-pool-size: 50 # 最大核心线程数
      min-maximum-pool-size: 2 # 最小最大线程数
      max-maximum-pool-size: 100 # 最大最大线程数
      min-queue-capacity: 10 # 最小队列容量
      max-queue-capacity: 10000 # 最大队列容量
      max-adjustment-ratio: 0.5 # 单次调整最大幅度(百分比)
```

### 3. 启用 EasyMail

在主类上添加 `@EnableEasyMail` 注解：

```java
@SpringBootApplication
@EnableEasyMail
public class MyApplication {
    public static void main(String[] args) {
        SpringApplication.run(MyApplication.class, args);
    }
}
```

### 4. 使用动态线程池

#### 方式一：直接注入使用

```java
@Service
public class MyService {
    
    @Autowired
    @Qualifier("noticeThreadPool")
    private ThreadPoolExecutor threadPool;
    
    public void executeTask() {
        threadPool.execute(() -> {
            // 你的业务逻辑
            System.out.println("任务执行中...");
        });
    }
}
```

#### 方式二：创建自定义线程池

```java
@Configuration
public class MyThreadPoolConfig {
    
    @Autowired
    private EasyMailDynamicThreadPoolConfig config;
    
    @Bean("myCustomThreadPool")
    public DynamicThreadPoolExecutor createCustomThreadPool() {
        // 复制配置并自定义
        DynamicThreadPoolConfig customConfig = config.copy();
        customConfig.setThreadPoolName("MyCustomPool");
        customConfig.setCorePoolSize(3);
        customConfig.setMaximumPoolSize(15);
        
        return new DynamicThreadPoolExecutor(customConfig);
    }
}
```

#### 方式三：编程式创建

```java
@Component
public class TaskProcessor {
    
    private DynamicThreadPoolExecutor executor;
    
    @PostConstruct
    public void init() {
        DynamicThreadPoolConfig config = new DynamicThreadPoolConfig();
        config.setThreadPoolName("TaskProcessor");
        config.setCorePoolSize(2);
        config.setMaximumPoolSize(10);
        config.setQueueCapacity(50);
        config.setEnableAutoAdjustment(true);
        config.setEnableMetricsLogging(true);
        
        this.executor = new DynamicThreadPoolExecutor(config);
    }
    
    public void processTask(Runnable task) {
        executor.execute(task);
    }
    
    @PreDestroy
    public void destroy() {
        if (executor != null) {
            executor.shutdown();
        }
    }
}
```

## 配置详解

### 基本配置 (basic)

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| core-pool-size | int | 5 | 核心线程数，始终保持活跃的线程数量 |
| maximum-pool-size | int | 20 | 最大线程数，线程池能创建的最大线程数 |
| queue-capacity | int | 100 | 队列容量，等待执行的任务队列大小 |
| keep-alive-time | long | 60 | 线程存活时间(秒)，非核心线程的最大空闲时间 |
| thread-pool-name | String | DynamicThreadPool | 线程池名称前缀，用于日志和监控 |

### 监控配置 (monitor)

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| interval | long | 30 | 监控间隔(秒)，多久检查一次线程池状态 |
| enable-auto-adjustment | boolean | true | 是否启用自动调整功能 |
| enable-metrics-logging | boolean | true | 是否启用指标日志输出 |

### 调整策略配置 (adjustment)

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| strategy-class | String | DefaultAdjustmentStrategy | 调整策略实现类 |
| min-interval | long | 60 | 最小调整间隔(秒)，防止频繁调整 |
| max-per-hour | int | 10 | 每小时最大调整次数限制 |
| queue-usage-expand-threshold | double | 0.8 | 队列使用率扩容阈值(80%) |
| queue-usage-shrink-threshold | double | 0.3 | 队列使用率缩容阈值(30%) |
| thread-activity-expand-threshold | double | 0.9 | 线程活跃度扩容阈值(90%) |
| thread-activity-shrink-threshold | double | 0.5 | 线程活跃度缩容阈值(50%) |
| task-wait-time-threshold | long | 1000 | 任务等待时间阈值(毫秒) |
| shrink-duration | long | 300 | 缩容持续时间要求(秒) |

### 安全边界配置 (safety)

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| min-core-pool-size | int | 1 | 最小核心线程数限制 |
| max-core-pool-size | int | 50 | 最大核心线程数限制 |
| min-maximum-pool-size | int | 2 | 最小最大线程数限制 |
| max-maximum-pool-size | int | 100 | 最大最大线程数限制 |
| min-queue-capacity | int | 10 | 最小队列容量限制 |
| max-queue-capacity | int | 10000 | 最大队列容量限制 |
| max-adjustment-ratio | double | 0.5 | 单次调整最大幅度(50%) |

## 监控操作

### 1. 日志监控

启用 `enable-metrics-logging: true` 后，系统会定期输出监控日志：

```
[ThreadPool-Metrics] 核心线程数: 5, 最大线程数: 20, 活跃线程数: 3, 队列大小: 15/100, 
队列使用率: 15.00%, 线程活跃度: 60.00%, 提交速率: 2.5/s, 完成速率: 2.3/s, 平均等待时间: 120.50ms
```

### 2. 调整日志

当线程池参数发生调整时，会输出详细的调整日志：

```
[MyApp-DynamicPool] 调整核心线程数: 5 -> 8 (队列使用率过高，需要扩容)
[MyApp-DynamicPool] 线程池调整完成 - 扩容调整
```

### 3. 编程式监控

```java
@Component
public class ThreadPoolMonitorService {
    
    @Autowired
    @Qualifier("noticeThreadPool")
    private DynamicThreadPoolExecutor threadPool;
    
    @Scheduled(fixedRate = 60000) // 每分钟检查一次
    public void monitorThreadPool() {
        ThreadPoolMetrics metrics = threadPool.getCurrentMetrics();
        
        // 检查负载等级
        int loadLevel = metrics.getLoadLevel();
        if (loadLevel >= 3) {
            // 高负载告警
            sendAlert("线程池负载过高: " + metrics.getStatusSummary());
        }
        
        // 检查任务积压
        if (metrics.hasTaskBacklog()) {
            sendAlert("检测到任务积压: " + metrics.toString());
        }
        
        // 记录关键指标到监控系统
        recordMetrics(metrics);
    }
    
    private void sendAlert(String message) {
        // 发送告警通知
        System.err.println("[ALERT] " + message);
    }
    
    private void recordMetrics(ThreadPoolMetrics metrics) {
        // 记录到监控系统（如 Prometheus、InfluxDB 等）
        // metricsCollector.record("threadpool.core.size", metrics.getCorePoolSize());
        // metricsCollector.record("threadpool.queue.usage", metrics.getQueueUsageRate());
    }
}
```

### 4. JMX 监控

可以通过 JMX 获取线程池状态：

```java
@Component
@ManagedResource(objectName = "cn.sunyblog.easymail:type=DynamicThreadPool")
public class ThreadPoolJMXBean {
    
    @Autowired
    @Qualifier("noticeThreadPool")
    private DynamicThreadPoolExecutor threadPool;
    
    @ManagedAttribute
    public int getCorePoolSize() {
        return threadPool.getCorePoolSize();
    }
    
    @ManagedAttribute
    public int getActiveCount() {
        return threadPool.getActiveCount();
    }
    
    @ManagedAttribute
    public int getQueueSize() {
        return threadPool.getQueue().size();
    }
    
    @ManagedOperation
    public String getCurrentMetrics() {
        return threadPool.getCurrentMetrics().toString();
    }
}
```

## 实现原理

### 1. 架构设计

```
┌─────────────────────────────────────────────────────────────┐
│                    DynamicThreadPoolExecutor                │
├─────────────────────────────────────────────────────────────┤
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐ │
│  │ ThreadPoolMonitor│  │AdjustmentStrategy│  │DynamicBlockingQueue│ │
│  │                 │  │                 │  │                 │ │
│  │ • 指标收集       │  │ • 调整决策       │  │ • 动态队列       │ │
│  │ • 性能统计       │  │ • 策略算法       │  │ • 容量调整       │ │
│  │ • 监控任务       │  │ • 安全检查       │  │ • 使用率计算     │ │
│  └─────────────────┘  └─────────────────┘  └─────────────────┘ │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
                    ┌─────────────────┐
                    │ScheduledExecutor │
                    │                 │
                    │ • 定时监控       │
                    │ • 异步调整       │
                    │ • 守护线程       │
                    └─────────────────┘
```

### 2. 核心组件

#### DynamicThreadPoolExecutor
- **继承关系**：继承自 `ThreadPoolExecutor`
- **核心功能**：在标准线程池基础上增加动态调整能力
- **监控集成**：内置监控器，实时收集性能指标
- **调整机制**：基于策略模式的参数调整

#### ThreadPoolMonitor
- **指标收集**：任务提交/完成/拒绝统计
- **性能分析**：等待时间、处理速率计算
- **状态评估**：负载等级、使用率分析

#### AdjustmentStrategy
- **策略接口**：定义调整决策的标准接口
- **默认实现**：基于阈值的智能调整策略
- **扩展性**：支持自定义调整算法

#### DynamicBlockingQueue
- **动态容量**：支持运行时调整队列大小
- **使用率监控**：实时计算队列使用率
- **线程安全**：保证并发环境下的数据一致性

### 3. 调整算法

#### 扩容条件
```java
// 队列使用率过高
if (queueUsageRate > expandThreshold) {
    // 扩容队列或增加线程
}

// 线程活跃度过高
if (threadActivity > activityThreshold) {
    // 增加线程数
}

// 任务等待时间过长
if (averageWaitTime > waitTimeThreshold) {
    // 优先增加线程，其次扩容队列
}
```

#### 缩容条件
```java
// 队列使用率持续较低
if (queueUsageRate < shrinkThreshold && 
    lowUsageDuration > shrinkDuration) {
    // 缩容队列或减少线程
}

// 线程活跃度持续较低
if (threadActivity < activityShrinkThreshold && 
    lowActivityDuration > shrinkDuration) {
    // 减少线程数
}
```

#### 安全检查
```java
// 调整幅度限制
int maxChange = (int) (currentSize * maxAdjustmentRatio);
newSize = Math.min(newSize, currentSize + maxChange);
newSize = Math.max(newSize, currentSize - maxChange);

// 边界值检查
newSize = Math.max(newSize, minSize);
newSize = Math.min(newSize, maxSize);

// 频率限制
if (System.currentTimeMillis() - lastAdjustTime < minInterval) {
    return; // 跳过本次调整
}
```

### 4. 监控指标

#### 基础指标
- **线程数量**：核心线程数、最大线程数、当前线程数、活跃线程数
- **队列状态**：队列大小、队列容量、队列使用率
- **任务统计**：提交总数、完成总数、拒绝总数

#### 性能指标
- **处理速率**：任务提交速率、任务完成速率
- **等待时间**：平均等待时间、最大等待时间
- **活跃度**：线程池活跃度、队列活跃度

#### 负载评估
- **负载等级**：1-低负载、2-中等负载、3-高负载、4-过载
- **利用率**：线程池利用率、队列利用率
- **处理效率**：完成率/提交率

### 5. 线程安全

#### 并发控制
- **原子操作**：使用 `AtomicLong`、`AtomicBoolean` 等原子类
- **CAS 操作**：无锁的比较交换操作
- **volatile 变量**：保证配置变更的可见性

#### 调整同步
```java
private final AtomicBoolean adjusting = new AtomicBoolean(false);

private void adjustThreadPool(ThreadPoolMetrics metrics) {
    if (!adjusting.compareAndSet(false, true)) {
        return; // 已有调整在进行中
    }
    
    try {
        // 执行调整逻辑
    } finally {
        adjusting.set(false);
    }
}
```

## 最佳实践

### 1. 配置建议

#### 生产环境配置
```yaml
mail:
  dynamic-thread-pool:
    enabled: true
    basic:
      core-pool-size: 10
      maximum-pool-size: 50
      queue-capacity: 200
      keep-alive-time: 300
    monitor:
      interval: 60
      enable-auto-adjustment: true
      enable-metrics-logging: false # 生产环境建议关闭详细日志
    adjustment:
      min-interval: 120 # 生产环境建议更长的调整间隔
      max-per-hour: 5
    safety:
      max-adjustment-ratio: 0.3 # 生产环境建议更保守的调整幅度
```

#### 开发环境配置
```yaml
mail:
  dynamic-thread-pool:
    enabled: true
    basic:
      core-pool-size: 2
      maximum-pool-size: 10
      queue-capacity: 20
    monitor:
      interval: 10
      enable-metrics-logging: true # 开发环境启用详细日志
    adjustment:
      min-interval: 30
```

### 2. 性能调优

#### CPU 密集型任务
```yaml
basic:
  core-pool-size: 8 # 接近 CPU 核心数
  maximum-pool-size: 16
  queue-capacity: 50 # 较小的队列
adjustment:
  thread-activity-expand-threshold: 0.95 # 更高的扩容阈值
```

#### IO 密集型任务
```yaml
basic:
  core-pool-size: 20 # 大于 CPU 核心数
  maximum-pool-size: 100
  queue-capacity: 500 # 较大的队列
adjustment:
  queue-usage-expand-threshold: 0.7 # 更低的扩容阈值
```

### 3. 监控告警

#### 关键指标告警
```java
@Component
public class ThreadPoolAlertService {
    
    private static final double HIGH_LOAD_THRESHOLD = 0.8;
    private static final long HIGH_WAIT_TIME_THRESHOLD = 2000;
    
    @EventListener
    public void handleMetrics(ThreadPoolMetrics metrics) {
        // 高负载告警
        if (metrics.getQueueUsageRate() > HIGH_LOAD_THRESHOLD) {
            sendAlert(AlertLevel.WARNING, "队列使用率过高: " + 
                     String.format("%.2f%%", metrics.getQueueUsageRate() * 100));
        }
        
        // 等待时间告警
        if (metrics.getAverageWaitTime() > HIGH_WAIT_TIME_THRESHOLD) {
            sendAlert(AlertLevel.ERROR, "任务等待时间过长: " + 
                     metrics.getAverageWaitTime() + "ms");
        }
        
        // 任务拒绝告警
        if (metrics.getTotalRejectedTasks() > 0) {
            sendAlert(AlertLevel.CRITICAL, "检测到任务拒绝: " + 
                     metrics.getTotalRejectedTasks() + " 个任务被拒绝");
        }
    }
}
```

### 4. 故障排查

#### 常见问题

1. **线程池不调整**
   - 检查 `enable-auto-adjustment` 是否为 true
   - 确认调整间隔设置是否合理
   - 查看是否触发了频率限制

2. **调整过于频繁**
   - 增加 `min-interval` 值
   - 降低 `max-per-hour` 值
   - 调整阈值参数

3. **性能不佳**
   - 检查线程数配置是否合理
   - 确认队列容量设置
   - 分析任务特性（CPU/IO 密集型）

#### 调试技巧

```java
// 启用详细日志
config.setEnableMetricsLogging(true);

// 获取调整统计
String stats = threadPool.getAdjustmentStats();
System.out.println(stats);

// 手动触发指标收集
ThreadPoolMetrics metrics = threadPool.getCurrentMetrics();
System.out.println(metrics.toString());
```

## 示例项目

完整的示例代码可以参考 `DynamicThreadPoolExample.java`，该示例演示了：

- 不同负载场景的模拟
- 线程池参数的动态调整
- 监控指标的实时输出
- 异常情况的处理

运行示例：

```bash
# 编译项目
mvn clean compile

# 运行示例
java -cp target/classes cn.sunyblog.easymail.dynamicThreadPool.example.DynamicThreadPoolExample
```

## 总结

EasyMail 的动态线程池是一个功能强大、易于使用的线程池解决方案。通过智能的监控和调整机制，它能够：

- **自动适应负载变化**，无需人工干预
- **提供丰富的监控指标**，便于性能分析
- **保证系统稳定性**，避免资源过度消耗
- **支持灵活配置**，适应不同业务场景

无论是在邮件处理、异步任务执行，还是其他需要线程池的场景中，都能发挥重要作用，显著提升应用的性能和稳定性。