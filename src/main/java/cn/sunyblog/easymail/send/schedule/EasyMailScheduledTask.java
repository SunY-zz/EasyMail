package cn.sunyblog.easymail.send.schedule;

import cn.sunyblog.easymail.api.EasyMailRequest;
import lombok.Data;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ScheduledFuture;

/**
 * 定时邮件任务实体
 * 封装定时发送邮件的所有信息
 *
 * @author suny
 * @version 1.0.0
 */
@Data
@Builder
public class EasyMailScheduledTask {

    /**
     * 任务唯一标识
     */
    private String taskId;

    /**
     * 任务名称
     */
    private String taskName;

    /**
     * 邮件请求对象（单个邮件）
     */
    private EasyMailRequest mailRequest;

    /**
     * 批量邮件请求列表（批量邮件）
     */
    private List<EasyMailRequest> batchRequests;

    /**
     * 定时表达式类型
     */
    private ScheduleType scheduleType;

    /**
     * Cron表达式（用于CRON类型）
     */
    private String cronExpression;

    /**
     * 延迟时间（毫秒，用于DELAY类型）
     */
    private Long delayMillis;

    /**
     * 固定间隔时间（毫秒，用于FIXED_RATE类型）
     */
    private Long fixedRateMillis;

    /**
     * 固定延迟时间（毫秒，用于FIXED_DELAY类型）
     */
    private Long fixedDelayMillis;

    /**
     * 指定执行时间（用于AT_TIME类型）
     */
    private LocalDateTime executeTime;

    /**
     * 任务状态
     */
    @Builder.Default
    private TaskStatus status = TaskStatus.PENDING;

    /**
     * 任务创建时间
     */
    @Builder.Default
    private LocalDateTime createTime = LocalDateTime.now();

    /**
     * 任务最后执行时间
     */
    private LocalDateTime lastExecuteTime;

    /**
     * 任务下次执行时间
     */
    private LocalDateTime nextExecuteTime;

    /**
     * 执行次数
     */
    @Builder.Default
    private Integer executeCount = 0;

    /**
     * 最大执行次数（-1表示无限制）
     */
    @Builder.Default
    private Integer maxExecuteCount = -1;

    /**
     * 任务描述
     */
    private String description;

    /**
     * Spring的ScheduledFuture对象（用于取消任务）
     */
    private ScheduledFuture<?> scheduledFuture;

    /**
     * 定时类型枚举
     */
    public enum ScheduleType {
        /**
         * Cron表达式
         */
        CRON,
        /**
         * 延迟执行（一次性）
         */
        DELAY,
        /**
         * 固定频率执行
         */
        FIXED_RATE,
        /**
         * 固定延迟执行
         */
        FIXED_DELAY,
        /**
         * 指定时间执行（一次性）
         */
        AT_TIME
    }

    /**
     * 任务状态枚举
     */
    public enum TaskStatus {
        /**
         * 等待执行
         */
        PENDING,
        /**
         * 运行中
         */
        RUNNING,
        /**
         * 已完成
         */
        COMPLETED,
        /**
         * 已取消
         */
        CANCELLED,
        /**
         * 执行失败
         */
        FAILED,
        /**
         * 已暂停
         */
        PAUSED
    }

    /**
     * 检查是否为批量任务
     *
     * @return 是否为批量任务
     */
    public boolean isBatchTask() {
        return batchRequests != null && !batchRequests.isEmpty();
    }

    /**
     * 获取邮件数量
     *
     * @return 邮件数量
     */
    public int getMailCount() {
        if (isBatchTask()) {
            return batchRequests.size();
        }
        return mailRequest != null ? 1 : 0;
    }

    /**
     * 检查任务是否可以执行
     *
     * @return 是否可以执行
     */
    public boolean canExecute() {
        if (status != TaskStatus.PENDING && status != TaskStatus.RUNNING) {
            return false;
        }

        // 检查最大执行次数
        if (maxExecuteCount > 0 && executeCount >= maxExecuteCount) {
            return false;
        }

        // 检查是否有邮件请求
        if (!isBatchTask() && mailRequest == null) {
            return false;
        }

        return true;
    }

    /**
     * 增加执行次数
     */
    public void incrementExecuteCount() {
        this.executeCount++;
        this.lastExecuteTime = LocalDateTime.now();
    }

    /**
     * 检查是否为一次性任务
     *
     * @return 是否为一次性任务
     */
    public boolean isOneTimeTask() {
        return scheduleType == ScheduleType.DELAY || scheduleType == ScheduleType.AT_TIME;
    }

    /**
     * 取消任务
     */
    public void cancel() {
        if (scheduledFuture != null && !scheduledFuture.isCancelled()) {
            scheduledFuture.cancel(false);
        }
        this.status = TaskStatus.CANCELLED;
    }

    /**
     * 暂停任务
     */
    public void pause() {
        if (scheduledFuture != null && !scheduledFuture.isCancelled()) {
            scheduledFuture.cancel(false);
        }
        this.status = TaskStatus.PAUSED;
    }
}