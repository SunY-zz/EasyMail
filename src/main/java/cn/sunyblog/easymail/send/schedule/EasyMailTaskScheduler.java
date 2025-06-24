package cn.sunyblog.easymail.send.schedule;

import cn.sunyblog.easymail.api.EasyMailSenderService;
import cn.sunyblog.easymail.send.EasyMailSendResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ScheduledFuture;

/**
 * 邮件定时任务调度器
 * 负责实际的任务调度和执行
 *
 * @author suny
 * @version 1.0.0
 */
@Slf4j
@Component
public class EasyMailTaskScheduler {

    @Resource
    private TaskScheduler taskScheduler;

    @Resource
    @Lazy
    private EasyMailSenderService easyMailSenderService;

    /**
     * 设置任务调度器
     *
     * @param taskScheduler 任务调度器
     */
    public void setTaskScheduler(TaskScheduler taskScheduler) {
        this.taskScheduler = taskScheduler;
    }

    /**
     * 调度Cron任务
     *
     * @param task 定时任务
     * @return ScheduledFuture对象
     */
    public ScheduledFuture<?> scheduleCronTask(EasyMailScheduledTask task) {
        log.info("调度Cron任务: {} - {}", task.getTaskId(), task.getCronExpression());
        
        CronTrigger cronTrigger = new CronTrigger(task.getCronExpression());
        
        return taskScheduler.schedule(() -> executeTask(task), cronTrigger);
    }

    /**
     * 调度延迟任务（一次性）
     *
     * @param task 定时任务
     * @return ScheduledFuture对象
     */
    public ScheduledFuture<?> scheduleDelayTask(EasyMailScheduledTask task) {
        log.info("调度延迟任务: {} - 延迟{}毫秒", task.getTaskId(), task.getDelayMillis());
        
        Instant startTime = Instant.now().plusMillis(task.getDelayMillis());
        
        return taskScheduler.schedule(() -> executeTask(task), startTime);
    }

    /**
     * 调度固定频率任务
     *
     * @param task 定时任务
     * @return ScheduledFuture对象
     */
    public ScheduledFuture<?> scheduleFixedRateTask(EasyMailScheduledTask task) {
        log.info("调度固定频率任务: {} - 间隔{}毫秒", task.getTaskId(), task.getFixedRateMillis());
        
        Instant startTime = Instant.now().plusMillis(1000); // 1秒后开始
        Duration period = Duration.ofMillis(task.getFixedRateMillis());
        
        return taskScheduler.scheduleAtFixedRate(() -> executeTask(task), startTime, period);
    }

    /**
     * 调度固定延迟任务
     *
     * @param task 定时任务
     * @return ScheduledFuture对象
     */
    public ScheduledFuture<?> scheduleFixedDelayTask(EasyMailScheduledTask task) {
        log.info("调度固定延迟任务: {} - 延迟{}毫秒", task.getTaskId(), task.getFixedDelayMillis());
        
        Instant startTime = Instant.now().plusMillis(1000); // 1秒后开始
        Duration delay = Duration.ofMillis(task.getFixedDelayMillis());
        
        return taskScheduler.scheduleWithFixedDelay(() -> executeTask(task), startTime, delay);
    }

    /**
     * 调度指定时间任务（一次性）
     *
     * @param task 定时任务
     * @return ScheduledFuture对象
     */
    public ScheduledFuture<?> scheduleAtTimeTask(EasyMailScheduledTask task) {
        log.info("调度指定时间任务: {} - 执行时间{}", task.getTaskId(), task.getExecuteTime());
        
        Instant executeInstant = task.getExecuteTime().atZone(ZoneId.systemDefault()).toInstant();
        
        return taskScheduler.schedule(() -> executeTask(task), executeInstant);
    }

    /**
     * 执行任务
     *
     * @param task 定时任务
     */
    private void executeTask(EasyMailScheduledTask task) {
        if (!task.canExecute()) {
            log.warn("任务 {} 不能执行，当前状态: {}", task.getTaskId(), task.getStatus());
            return;
        }

        log.info("开始执行定时邮件任务: {} (邮件数量: {})", task.getTaskId(), task.getMailCount());
        
        try {
            // 更新任务状态
            task.setStatus(EasyMailScheduledTask.TaskStatus.RUNNING);
            task.incrementExecuteCount();
            
            boolean success = false;
            
            // 执行邮件发送
            if (task.isBatchTask()) {
                // 批量发送
                List<EasyMailSendResult> results = easyMailSenderService.sendBatch(task.getBatchRequests());
                int successCount = (int) results.stream().mapToInt(r -> r.isSuccess() ? 1 : 0).sum();
                success = successCount > 0;
                log.info("批量定时邮件任务 {} 执行完成，成功发送 {}/{} 封邮件", 
                        task.getTaskId(), successCount, results.size());
            } else {
                // 单个发送
                EasyMailSendResult result = easyMailSenderService.send(task.getMailRequest());
                success = result.isSuccess();
                if (success) {
                    log.info("定时邮件任务 {} 执行成功，第{}次执行", task.getTaskId(), task.getExecuteCount());
                } else {
                    log.error("定时邮件任务 {} 执行失败: {}", task.getTaskId(), result.getErrorMessage());
                }
            }
            
            if (success) {
                // 如果是一次性任务，标记为完成
                if (task.isOneTimeTask()) {
                    task.setStatus(EasyMailScheduledTask.TaskStatus.COMPLETED);
                } else {
                    // 检查是否达到最大执行次数
                    if (task.getMaxExecuteCount() > 0 && task.getExecuteCount() >= task.getMaxExecuteCount()) {
                        task.setStatus(EasyMailScheduledTask.TaskStatus.COMPLETED);
                        task.cancel();
                        log.info("定时邮件任务 {} 达到最大执行次数，任务完成", task.getTaskId());
                    } else {
                        task.setStatus(EasyMailScheduledTask.TaskStatus.PENDING);
                    }
                }
            } else {
                task.setStatus(EasyMailScheduledTask.TaskStatus.FAILED);
            }
            
        } catch (Exception e) {
            log.error("定时邮件任务 {} 执行异常", task.getTaskId(), e);
            task.setStatus(EasyMailScheduledTask.TaskStatus.FAILED);
        }
    }

    /**
     * 取消任务
     *
     * @param task 定时任务
     */
    public void cancelTask(EasyMailScheduledTask task) {
        if (task.getScheduledFuture() != null && !task.getScheduledFuture().isCancelled()) {
            task.getScheduledFuture().cancel(false);
            log.info("已取消定时邮件任务: {}", task.getTaskId());
        }
        task.setStatus(EasyMailScheduledTask.TaskStatus.CANCELLED);
    }

    /**
     * 检查任务是否已完成
     *
     * @param task 定时任务
     * @return 是否已完成
     */
    public boolean isTaskCompleted(EasyMailScheduledTask task) {
        return task.getStatus() == EasyMailScheduledTask.TaskStatus.COMPLETED ||
               task.getStatus() == EasyMailScheduledTask.TaskStatus.CANCELLED;
    }

    /**
     * 检查任务是否正在运行
     *
     * @param task 定时任务
     * @return 是否正在运行
     */
    public boolean isTaskRunning(EasyMailScheduledTask task) {
        return task.getScheduledFuture() != null && 
               !task.getScheduledFuture().isCancelled() && 
               !task.getScheduledFuture().isDone();
    }
}