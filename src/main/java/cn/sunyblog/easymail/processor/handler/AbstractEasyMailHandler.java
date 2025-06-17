package cn.sunyblog.easymail.processor.handler;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CompletableFuture;

import cn.sunyblog.easymail.exception.EasyMailProcessException;

/**
 * 抽象邮件处理器基类
 * 提供通用的邮件处理逻辑和扩展点，简化自定义处理器的开发
 * <p>
 * 使用示例：
 * <pre>
 * @Component
 * public class MyEmailHandler extends AbstractEmailHandler {
 *
 *     @Override
 *     protected boolean shouldHandle(EmailContext context) {
 *         return context.getSubject().contains("订单");
 *     }
 *
 *     @Override
 *     protected void doHandle(EmailContext context) {
 *         // 处理订单相关邮件
 *         log.info("处理订单邮件: {}", context.getSubject());
 *     }
 * }
 * </pre>
 *
 * @author suny
 * @version 1.0.0
 */
@Slf4j
public abstract class AbstractEasyMailHandler {

    /**
     * 处理器名称（默认为类名）
     */
    private String handlerName;

    /**
     * 是否启用异步处理
     */
    private boolean asyncEnabled = false;

    /**
     * 处理器优先级（数值越小优先级越高）
     */
    private int priority = 100;

    /**
     * 是否启用
     */
    private boolean enabled = true;

    public AbstractEasyMailHandler() {
        this.handlerName = this.getClass().getSimpleName();
    }

    public AbstractEasyMailHandler(String handlerName) {
        this.handlerName = handlerName;
    }

    /**
     * 处理邮件的主入口方法
     *
     * @param context 邮件上下文
     * @return 处理结果
     */
    public final EmailHandlerResult handle(EasyMailContext context) {
        if (!enabled) {
            log.debug("处理器 {} 已禁用，跳过处理", handlerName);
            return EmailHandlerResult.skipped(handlerName, "处理器已禁用");
        }

        try {
            // 前置处理
            if (!preHandle(context)) {
                log.debug("处理器 {} 前置检查失败，跳过处理", handlerName);
                return EmailHandlerResult.skipped(handlerName, "前置检查失败");
            }

            // 检查是否应该处理
            if (!shouldHandle(context)) {
                log.debug("处理器 {} 不匹配当前邮件，跳过处理", handlerName);
                return EmailHandlerResult.skipped(handlerName, "不匹配当前邮件");
            }

            long startTime = System.currentTimeMillis();
            log.debug("处理器 {} 开始处理邮件: {}", handlerName, context.getSubject());

            // 执行处理逻辑
            if (asyncEnabled) {
                return handleAsync(context);
            } else {
                doHandle(context);

                long duration = System.currentTimeMillis() - startTime;
                log.debug("处理器 {} 处理完成，耗时: {}ms", handlerName, duration);

                // 后置处理
                postHandle(context, true);

                return EmailHandlerResult.success(handlerName, duration);
            }

        } catch (Exception e) {
            EasyMailProcessException processEx = EasyMailProcessException.processingError("邮件处理异常", e);
            log.error(processEx.getFullErrorMessage(), processEx);

            // 异常后置处理
            try {
                postHandle(context, false);
            } catch (Exception postEx) {
                log.error("处理器 {} 后置处理异常: {}", handlerName, postEx.getMessage(), postEx);
            }

            // 错误处理
            handleError(context, e);

            return EmailHandlerResult.failure(handlerName, processEx.getMessage());
        }
    }

    /**
     * 异步处理邮件
     */
    private EmailHandlerResult handleAsync(EasyMailContext context) {
        CompletableFuture.runAsync(() -> {
            try {
                long startTime = System.currentTimeMillis();
                doHandle(context);
                long duration = System.currentTimeMillis() - startTime;
                log.debug("处理器 {} 异步处理完成，耗时: {}ms", handlerName, duration);
                postHandle(context, true);
            } catch (Exception e) {
                log.error("处理器 {} 异步处理异常: {}", handlerName, e.getMessage(), e);
                try {
                    postHandle(context, false);
                } catch (Exception postEx) {
                    log.error("处理器 {} 异步后置处理异常: {}", handlerName, postEx.getMessage(), postEx);
                }
                handleError(context, e);
            }
        });

        return EmailHandlerResult.async(handlerName);
    }

    /**
     * 前置处理（可重写）
     *
     * @param context 邮件上下文
     * @return 是否继续处理
     */
    protected boolean preHandle(EasyMailContext context) {
        return true;
    }

    /**
     * 判断是否应该处理此邮件（必须实现）
     *
     * @param context 邮件上下文
     * @return 是否应该处理
     */
    protected abstract boolean shouldHandle(EasyMailContext context);

    /**
     * 执行具体的处理逻辑（必须实现）
     *
     * @param context 邮件上下文
     * @throws Exception 处理异常
     */
    protected abstract void doHandle(EasyMailContext context) throws Exception;

    /**
     * 后置处理（可重写）
     *
     * @param context 邮件上下文
     * @param success 处理是否成功
     */
    protected void postHandle(EasyMailContext context, boolean success) {
        // 默认空实现
    }

    /**
     * 错误处理（可重写）
     *
     * @param context 邮件上下文
     * @param error   异常信息
     */
    protected void handleError(EasyMailContext context, Exception error) {
        // 默认空实现，子类可以重写进行自定义错误处理
    }

    // ==================== Getter/Setter ====================

    public String getHandlerName() {
        return handlerName;
    }

    public void setHandlerName(String handlerName) {
        this.handlerName = handlerName;
    }

    public boolean isAsyncEnabled() {
        return asyncEnabled;
    }

    public void setAsyncEnabled(boolean asyncEnabled) {
        this.asyncEnabled = asyncEnabled;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * 邮件处理结果
     */
    public static class EmailHandlerResult {
        private final String handlerName;
        private final boolean success;
        private final boolean skipped;
        private final boolean async;
        private final String message;
        private final long duration;

        private EmailHandlerResult(String handlerName, boolean success, boolean skipped,
                                   boolean async, String message, long duration) {
            this.handlerName = handlerName;
            this.success = success;
            this.skipped = skipped;
            this.async = async;
            this.message = message;
            this.duration = duration;
        }

        public static EmailHandlerResult success(String handlerName, long duration) {
            return new EmailHandlerResult(handlerName, true, false, false, "处理成功", duration);
        }

        public static EmailHandlerResult failure(String handlerName, String message) {
            return new EmailHandlerResult(handlerName, false, false, false, message, 0);
        }

        public static EmailHandlerResult skipped(String handlerName, String reason) {
            return new EmailHandlerResult(handlerName, false, true, false, reason, 0);
        }

        public static EmailHandlerResult async(String handlerName) {
            return new EmailHandlerResult(handlerName, true, false, true, "异步处理中", 0);
        }

        // Getters
        public String getHandlerName() {
            return handlerName;
        }

        public boolean isSuccess() {
            return success;
        }

        public boolean isSkipped() {
            return skipped;
        }

        public boolean isAsync() {
            return async;
        }

        public String getMessage() {
            return message;
        }

        public long getDuration() {
            return duration;
        }

        @Override
        public String toString() {
            return String.format("EmailHandlerResult{handler='%s', success=%s, skipped=%s, async=%s, message='%s', duration=%dms}",
                    handlerName, success, skipped, async, message, duration);
        }
    }
}