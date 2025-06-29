package cn.sunyblog.easymail.send.schedule;

import cn.sunyblog.easymail.api.EasyMailRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * 邮件定时任务管理器
 * 负责定时任务的统一管理，包括创建、删除、查询、统计等功能
 *
 * @author suny
 * @version 1.0.0
 */
@Slf4j
@Component
public class EasyMailScheduleManager {

    @Resource
    private EasyMailTaskScheduler taskScheduler;

    /**
     * 任务存储容器
     */
    private final Map<String, EasyMailScheduledTask> tasks = new ConcurrentHashMap<>();

    /**
     * 任务ID生成器
     */
    private final AtomicLong taskIdGenerator = new AtomicLong(1);

    @PostConstruct
    public void init() {
        log.debug("邮件定时任务管理器初始化完成");
    }

    @PreDestroy
    public void destroy() {
        log.debug("正在关闭邮件定时任务管理器...");
        cancelAllTasks();
        log.debug("邮件定时任务管理器已关闭");
    }

    /**
     * 添加定时任务
     *
     * @param task 定时任务
     * @return 任务ID
     */
    public String addTask(EasyMailScheduledTask task) {
        // 生成任务ID（如果没有指定）
        if (task.getTaskId() == null || task.getTaskId().trim().isEmpty()) {
            task.setTaskId("MAIL_TASK_" + taskIdGenerator.getAndIncrement());
        }

        // 检查任务ID是否已存在
        if (tasks.containsKey(task.getTaskId())) {
            throw new IllegalArgumentException("任务ID已存在: " + task.getTaskId());
        }

        // 验证任务配置
        validateTask(task);

        // 调度任务
        ScheduledFuture<?> scheduledFuture = scheduleTask(task);
        task.setScheduledFuture(scheduledFuture);

        // 存储任务
        tasks.put(task.getTaskId(), task);

        log.info("已添加定时邮件任务: {} - {}", task.getTaskId(), task.getTaskName());
        return task.getTaskId();
    }

    /**
     * 调度任务
     *
     * @param task 定时任务
     * @return ScheduledFuture对象
     */
    private ScheduledFuture<?> scheduleTask(EasyMailScheduledTask task) {
        switch (task.getScheduleType()) {
            case CRON:
                return taskScheduler.scheduleCronTask(task);
            case DELAY:
                return taskScheduler.scheduleDelayTask(task);
            case FIXED_RATE:
                return taskScheduler.scheduleFixedRateTask(task);
            case FIXED_DELAY:
                return taskScheduler.scheduleFixedDelayTask(task);
            case AT_TIME:
                return taskScheduler.scheduleAtTimeTask(task);
            default:
                throw new IllegalArgumentException("不支持的调度类型: " + task.getScheduleType());
        }
    }

    /**
     * 验证任务配置
     *
     * @param task 定时任务
     */
    private void validateTask(EasyMailScheduledTask task) {
        // 验证邮件请求
        if (!task.isBatchTask() && task.getMailRequest() == null) {
            throw new IllegalArgumentException("邮件请求对象不能为空");
        }
        if (task.isBatchTask() && (task.getBatchRequests() == null || task.getBatchRequests().isEmpty())) {
            throw new IllegalArgumentException("批量邮件请求列表不能为空");
        }

        switch (task.getScheduleType()) {
            case CRON:
                if (task.getCronExpression() == null || task.getCronExpression().trim().isEmpty()) {
                    throw new IllegalArgumentException("Cron表达式不能为空");
                }
                break;
            case DELAY:
                if (task.getDelayMillis() == null || task.getDelayMillis() <= 0) {
                    throw new IllegalArgumentException("延迟时间必须大于0");
                }
                break;
            case FIXED_RATE:
                if (task.getFixedRateMillis() == null || task.getFixedRateMillis() <= 0) {
                    throw new IllegalArgumentException("固定频率时间必须大于0");
                }
                break;
            case FIXED_DELAY:
                if (task.getFixedDelayMillis() == null || task.getFixedDelayMillis() <= 0) {
                    throw new IllegalArgumentException("固定延迟时间必须大于0");
                }
                break;
            case AT_TIME:
                if (task.getExecuteTime() == null) {
                    throw new IllegalArgumentException("执行时间不能为空");
                }
                if (task.getExecuteTime().isBefore(LocalDateTime.now())) {
                    throw new IllegalArgumentException("执行时间不能早于当前时间");
                }
                break;
            default:
                throw new IllegalArgumentException("不支持的调度类型: " + task.getScheduleType());
        }
    }

    /**
     * 取消任务
     *
     * @param taskId 任务ID
     * @return 是否成功取消
     */
    public boolean cancelTask(String taskId) {
        EasyMailScheduledTask task = tasks.get(taskId);
        if (task == null) {
            log.warn("任务不存在: {}", taskId);
            return false;
        }

        taskScheduler.cancelTask(task);
        log.info("已取消定时邮件任务: {}", taskId);
        return true;
    }

    /**
     * 删除任务
     *
     * @param taskId 任务ID
     * @return 是否成功删除
     */
    public boolean removeTask(String taskId) {
        EasyMailScheduledTask task = tasks.get(taskId);
        if (task == null) {
            log.warn("任务不存在: {}", taskId);
            return false;
        }

        // 先取消任务
        taskScheduler.cancelTask(task);
        
        // 从容器中移除
        tasks.remove(taskId);
        
        log.info("已删除定时邮件任务: {}", taskId);
        return true;
    }

    /**
     * 获取任务
     *
     * @param taskId 任务ID
     * @return 定时任务
     */
    public EasyMailScheduledTask getTask(String taskId) {
        return tasks.get(taskId);
    }

    /**
     * 获取所有任务
     *
     * @return 所有任务列表
     */
    public List<EasyMailScheduledTask> getAllTasks() {
        return new ArrayList<>(tasks.values());
    }

    /**
     * 根据状态获取任务
     *
     * @param status 任务状态
     * @return 任务列表
     */
    public List<EasyMailScheduledTask> getTasksByStatus(EasyMailScheduledTask.TaskStatus status) {
        return tasks.values().stream()
                .filter(task -> task.getStatus() == status)
                .collect(Collectors.toList());
    }

    /**
     * 获取运行中的任务
     *
     * @return 运行中的任务列表
     */
    public List<EasyMailScheduledTask> getRunningTasks() {
        return tasks.values().stream()
                .filter(task -> taskScheduler.isTaskRunning(task))
                .collect(Collectors.toList());
    }

    /**
     * 取消所有任务
     */
    public void cancelAllTasks() {
        log.info("正在取消所有定时邮件任务...");
        
        for (EasyMailScheduledTask task : tasks.values()) {
            taskScheduler.cancelTask(task);
        }
        
        log.info("已取消所有定时邮件任务，共{}个", tasks.size());
    }

    /**
     * 清理已完成的任务
     */
    public void cleanupCompletedTasks() {
        List<String> completedTaskIds = tasks.values().stream()
                .filter(task -> taskScheduler.isTaskCompleted(task))
                .map(EasyMailScheduledTask::getTaskId)
                .collect(Collectors.toList());

        for (String taskId : completedTaskIds) {
            tasks.remove(taskId);
        }

        if (!completedTaskIds.isEmpty()) {
            log.info("已清理{}个已完成的定时邮件任务", completedTaskIds.size());
        }
    }

    /**
     * 获取任务统计信息
     *
     * @return 统计信息
     */
    public Map<String, Object> getTaskStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        stats.put("totalTasks", tasks.size());
        stats.put("pendingTasks", getTasksByStatus(EasyMailScheduledTask.TaskStatus.PENDING).size());
        stats.put("runningTasks", getTasksByStatus(EasyMailScheduledTask.TaskStatus.RUNNING).size());
        stats.put("completedTasks", getTasksByStatus(EasyMailScheduledTask.TaskStatus.COMPLETED).size());
        stats.put("cancelledTasks", getTasksByStatus(EasyMailScheduledTask.TaskStatus.CANCELLED).size());
        stats.put("failedTasks", getTasksByStatus(EasyMailScheduledTask.TaskStatus.FAILED).size());
        stats.put("pausedTasks", getTasksByStatus(EasyMailScheduledTask.TaskStatus.PAUSED).size());
        
        return stats;
    }

    /**
     * 检查任务是否存在
     *
     * @param taskId 任务ID
     * @return 是否存在
     */
    public boolean taskExists(String taskId) {
        return tasks.containsKey(taskId);
    }

    /**
     * 获取任务数量
     *
     * @return 任务数量
     */
    public int getTaskCount() {
        return tasks.size();
    }

    // ==================== 批量发送相关方法 ====================

    /**
     * 创建批量定时任务（Cron表达式）
     *
     * @param requests 邮件请求列表
     * @param cronExpression Cron表达式
     * @param taskName 任务名称
     * @return 任务ID
     */
    public String createBatchScheduledTask(List<EasyMailRequest> requests, String cronExpression, String taskName) {
        EasyMailScheduledTask task = EasyMailScheduledTask.builder()
                .taskName(taskName)
                .scheduleType(EasyMailScheduledTask.ScheduleType.CRON)
                .cronExpression(cronExpression)
                .batchRequests(requests)
                .build();
        return addTask(task);
    }

    /**
     * 创建批量延迟任务
     *
     * @param requests 邮件请求列表
     * @param delayMillis 延迟毫秒数
     * @param taskName 任务名称
     * @return 任务ID
     */
    public String createBatchDelayedTask(List<EasyMailRequest> requests, long delayMillis, String taskName) {
        EasyMailScheduledTask task = EasyMailScheduledTask.builder()
                .taskName(taskName)
                .scheduleType(EasyMailScheduledTask.ScheduleType.DELAY)
                .delayMillis(delayMillis)
                .batchRequests(requests)
                .build();
        return addTask(task);
    }

    /**
     * 创建批量指定时间任务
     *
     * @param requests 邮件请求列表
     * @param executeTime 执行时间
     * @param taskName 任务名称
     * @return 任务ID
     */
    public String createBatchAtTimeTask(List<EasyMailRequest> requests, LocalDateTime executeTime, String taskName) {
        EasyMailScheduledTask task = EasyMailScheduledTask.builder()
                .taskName(taskName)
                .scheduleType(EasyMailScheduledTask.ScheduleType.AT_TIME)
                .executeTime(executeTime)
                .batchRequests(requests)
                .build();
        return addTask(task);
    }
}