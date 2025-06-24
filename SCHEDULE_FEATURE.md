# EasyMail 定时发送功能使用指南

## 功能概述

EasyMail 新增了强大的定时发送功能，支持多种定时策略，让邮件发送更加灵活和智能。该功能完全复用现有的 `EasyMailSenderService` 服务，无需额外配置，开箱即用。

## 支持的定时策略

### 1. Cron表达式定时发送
使用标准的Cron表达式来定义复杂的定时规则。

```java
// 每天上午9点发送邮件
EasyMailRequest request = EasyMailRequest.builder()
    .to("recipient@example.com")
    .subject("每日报告")
    .text("这是每日报告邮件")
    .build();

String taskId = easyMailSenderService.sendScheduled(request, "0 0 9 * * ?", "每日报告任务");
```

### 2. 延迟发送
指定延迟时间后发送邮件。

```java
// 5分钟后发送邮件
EasyMailRequest request = EasyMailRequest.builder()
    .to("recipient@example.com")
    .subject("延迟通知")
    .text("这是一封延迟发送的邮件")
    .build();

long delayMillis = 5 * 60 * 1000; // 5分钟
String taskId = easyMailSenderService.sendDelayed(request, delayMillis, "延迟通知任务");
```

### 3. 固定频率发送
按照固定的时间间隔重复发送邮件。

```java
// 每30秒发送一次
EasyMailRequest request = EasyMailRequest.builder()
    .to("recipient@example.com")
    .subject("定期监控")
    .text("系统监控报告")
    .build();

long fixedRateMillis = 30 * 1000; // 30秒
String taskId = easyMailSenderService.sendAtFixedRate(request, fixedRateMillis, "监控任务");
```

### 4. 固定延迟发送
上次发送完成后，延迟指定时间再发送下一封邮件。

```java
// 上次发送完成后延迟1分钟再发送
EasyMailRequest request = EasyMailRequest.builder()
    .to("recipient@example.com")
    .subject("固定延迟邮件")
    .text("这是固定延迟发送的邮件")
    .build();

long fixedDelayMillis = 60 * 1000; // 1分钟
String taskId = easyMailSenderService.sendWithFixedDelay(request, fixedDelayMillis, "固定延迟任务");
```

### 5. 指定时间发送
在具体的时间点发送邮件。

```java
// 在指定时间发送邮件
EasyMailRequest request = EasyMailRequest.builder()
    .to("recipient@example.com")
    .subject("定时邮件")
    .text("这是在指定时间发送的邮件")
    .build();

LocalDateTime executeTime = LocalDateTime.now().plusHours(2); // 2小时后
String taskId = easyMailSenderService.sendAtTime(request, executeTime, "指定时间任务");
```

## 任务管理功能

### 查询任务

```java
// 获取所有定时任务
List<EasyMailScheduledTask> allTasks = easyMailSenderService.getAllScheduledTasks();

// 获取运行中的任务
List<EasyMailScheduledTask> runningTasks = easyMailSenderService.getRunningScheduledTasks();

// 根据状态获取任务
List<EasyMailScheduledTask> pendingTasks = easyMailSenderService
    .getScheduledTasksByStatus(EasyMailScheduledTask.TaskStatus.PENDING);

// 获取指定任务
EasyMailScheduledTask task = easyMailSenderService.getScheduledTask(taskId);
```

### 取消和删除任务

```java
// 取消任务（任务仍保留在系统中）
boolean cancelled = easyMailSenderService.cancelScheduledTask(taskId);

// 删除任务（从系统中完全移除）
boolean removed = easyMailSenderService.removeScheduledTask(taskId);

// 取消所有任务
easyMailSenderService.cancelAllScheduledTasks();
```

### 任务统计

```java
// 获取任务统计信息
Map<String, Object> statistics = easyMailSenderService.getScheduledTaskStatistics();

// 获取任务数量
int taskCount = easyMailSenderService.getScheduledTaskCount();

// 检查任务是否存在
boolean exists = easyMailSenderService.scheduledTaskExists(taskId);
```

### 清理任务

```java
// 清理已完成的任务
easyMailSenderService.cleanupCompletedScheduledTasks();
```

## 任务状态

定时任务支持以下状态：

- `PENDING`: 待执行
- `RUNNING`: 执行中
- `COMPLETED`: 已完成
- `CANCELLED`: 已取消
- `FAILED`: 执行失败
- `PAUSED`: 已暂停

## 常用Cron表达式示例

| 表达式 | 说明 |
|--------|------|
| `0 0 9 * * ?` | 每天上午9点 |
| `0 0 9 * * MON-FRI` | 工作日上午9点 |
| `0 0 9 ? * MON` | 每周一上午9点 |
| `0 0 9 1 * ?` | 每月1号上午9点 |
| `0 0/30 * * * ?` | 每30分钟 |
| `0 0 9,18 * * ?` | 每天上午9点和下午6点 |

## 配置说明

定时发送功能无需额外配置，自动使用现有的SMTP配置：

```yaml
email:
  sender:
    smtp:
      server: smtp.163.com
      port: 465
      username: your-email@163.com
      password: your-password
      # ... 其他SMTP配置
```

## 最佳实践

### 1. 任务命名
为每个定时任务指定有意义的名称，便于管理和监控：

```java
String taskId = easyMailSenderService.sendScheduled(request, cronExpression, "每日销售报告");
```

### 2. 错误处理
定时任务执行失败时，系统会自动记录错误信息，可以通过任务状态查询：

```java
EasyMailScheduledTask task = easyMailSenderService.getScheduledTask(taskId);
if (task.getStatus() == EasyMailScheduledTask.TaskStatus.FAILED) {
    // 处理失败情况
    log.error("任务执行失败: {}", task.getErrorMessage());
}
```

### 3. 资源管理
定期清理已完成的任务，避免内存占用过多：

```java
// 建议定期执行
easyMailSenderService.cleanupCompletedScheduledTasks();
```

### 4. 监控统计
定期检查任务统计信息，监控系统运行状态：

```java
Map<String, Object> stats = easyMailSenderService.getScheduledTaskStatistics();
log.info("定时任务统计: {}", stats);
```

## 注意事项

1. **时区**: 所有时间都基于系统默认时区
2. **并发**: 定时任务使用独立的线程池，不会影响普通邮件发送
3. **持久化**: 当前版本的任务信息存储在内存中，应用重启后会丢失
4. **性能**: 大量定时任务可能会影响系统性能，建议合理控制任务数量
5. **重试**: 定时任务会复用现有的重试机制配置

## 完整示例

参考 `EasyMailScheduleExample.java` 文件，其中包含了所有功能的完整使用示例。

```java
@Component
public class MyScheduleService {
    
    @Resource
    private EasyMailSenderService easyMailSenderService;
    
    public void setupDailyReport() {
        EasyMailRequest request = EasyMailRequest.builder()
            .to("manager@company.com")
            .subject("每日销售报告")
            .html("<h1>销售报告</h1><p>详细内容...</p>")
            .build();
            
        // 每天上午9点发送
        String taskId = easyMailSenderService.sendScheduled(
            request, 
            "0 0 9 * * ?", 
            "每日销售报告"
        );
        
        log.info("已设置每日报告任务: {}", taskId);
    }
}
```

通过以上功能，您可以轻松实现各种复杂的邮件定时发送需求，提升系统的自动化水平。