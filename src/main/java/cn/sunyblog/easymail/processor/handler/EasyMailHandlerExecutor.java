package cn.sunyblog.easymail.processor.handler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.ReflectionUtils;

import javax.annotation.Resource;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

import cn.sunyblog.easymail.exception.EasyMailProcessException;

/**
 * 邮件处理器执行器
 * 负责调用匹配的处理器方法
 *
 * @author suny
 * @version 1.0
 * @date 2025/06/14
 */
@Slf4j
@Component
public class EasyMailHandlerExecutor {

    @Resource
    private EasyMailHandlerRegistry handlerRegistry;

    @Resource
    private ExecutorService noticeThreadPool;

    /**
     * 执行邮件处理
     *
     * @param context 邮件上下文
     * @return 处理结果
     */
    public boolean executeHandlers(EasyMailContext context) {
        List<EasyMailHandlerInfo> matchingHandlers = handlerRegistry.getMatchingHandlers(context);

        if (matchingHandlers.isEmpty()) {
            log.debug("没有找到匹配的邮件处理器，主题: {}, 发件人: {}",
                    context.getSubject(), context.getFrom());
            return false;
        }

        log.debug("找到 {} 个匹配的邮件处理器，主题: {}", matchingHandlers.size(), context.getSubject());

        boolean hasSuccess = false;

        for (EasyMailHandlerInfo handlerInfo : matchingHandlers) {
            try {
                context.setProcessStartTime(LocalDateTime.now());
                context.setProcessorName(handlerInfo.getName());

                if (handlerInfo.isAsync()) {
                    // 异步执行
                    executeHandlerAsync(handlerInfo, context);
                    hasSuccess = true;
                } else {
                    // 同步执行
                    boolean result = executeHandlerSync(handlerInfo, context);
                    if (result) {
                        hasSuccess = true;
                    }
                }
            } catch (Exception e) {
                EasyMailProcessException processEx = EasyMailProcessException.processingError("执行邮件处理器失败", e);
                log.error(processEx.getFullErrorMessage(), processEx);
            }
        }

        return hasSuccess;
    }

    /**
     * 同步执行处理器
     *
     * @param handlerInfo 处理器信息
     * @param context     邮件上下文
     * @return 执行结果
     */
    private boolean executeHandlerSync(EasyMailHandlerInfo handlerInfo, EasyMailContext context) {
        long startTime = System.currentTimeMillis();

        try {
            Object result = invokeHandler(handlerInfo, context);

            long duration = System.currentTimeMillis() - startTime;
            log.debug("同步执行邮件处理器 [{}] 完成，耗时: {}ms，结果: {}",
                    handlerInfo.getName(), duration, result);

            return result != null;
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            EasyMailProcessException processEx = EasyMailProcessException.processingError("同步执行邮件处理器失败", e);
            log.error("同步执行邮件处理器 [{}] 失败，耗时: {}ms - {}",
                    handlerInfo.getName(), duration, processEx.getFullErrorMessage(), processEx);
            return false;
        }
    }

    /**
     * 异步执行处理器
     *
     * @param handlerInfo 处理器信息
     * @param context     邮件上下文
     */
    private void executeHandlerAsync(EasyMailHandlerInfo handlerInfo, EasyMailContext context) {
        CompletableFuture.runAsync(() -> {
            long startTime = System.currentTimeMillis();

            try {
                Object result = invokeHandler(handlerInfo, context);

                long duration = System.currentTimeMillis() - startTime;
                log.debug("异步执行邮件处理器 [{}] 完成，耗时: {}ms，结果: {}",
                        handlerInfo.getName(), duration, result);

            } catch (Exception e) {
                long duration = System.currentTimeMillis() - startTime;
                EasyMailProcessException processEx = EasyMailProcessException.processingError("异步执行邮件处理器失败", e);
                log.error("异步执行邮件处理器 [{}] 失败，耗时: {}ms - {}",
                        handlerInfo.getName(), duration, processEx.getFullErrorMessage(), processEx);
            }
        }, noticeThreadPool);
    }

    /**
     * 调用处理器方法
     *
     * @param handlerInfo 处理器信息
     * @param context     邮件上下文
     * @return 调用结果
     */
    private Object invokeHandler(EasyMailHandlerInfo handlerInfo, EasyMailContext context) {
        Method method = handlerInfo.getMethod();
        Object bean = handlerInfo.getBean();

        // 确保方法可访问
        ReflectionUtils.makeAccessible(method);

        Class<?>[] paramTypes = method.getParameterTypes();
        Object[] args;

        if (paramTypes.length == 1 && paramTypes[0] == EasyMailContext.class) {
            // 新的上下文方式
            args = new Object[]{context};
        } else if (paramTypes.length == 4) {
            // 兼容旧的方式
            args = new Object[]{
                    context.getMessage(),
                    context.getTextContent(),
                    context.getSubject(),
                    context.getFrom()
            };
        } else {
            throw new IllegalArgumentException("不支持的方法签名: " + method.getName());
        }

        try {
            return ReflectionUtils.invokeMethod(method, bean, args);
        } catch (Exception e) {
            EasyMailProcessException processEx = EasyMailProcessException.processingError("调用处理器方法失败", e);
            log.error("调用处理器方法失败: {}.{} - {}",
                    bean.getClass().getSimpleName(), method.getName(), processEx.getFullErrorMessage(), processEx);
            throw processEx;
        }
    }

    /**
     * 执行指定组的处理器
     *
     * @param context 邮件上下文
     * @param group   组名
     * @return 处理结果
     */
    public boolean executeHandlersByGroup(EasyMailContext context, String group) {
        List<EasyMailHandlerInfo> groupHandlers = handlerRegistry.getHandlersByGroup(group);

        if (groupHandlers.isEmpty()) {
            log.debug("组 [{}] 中没有找到邮件处理器", group);
            return false;
        }

        boolean hasSuccess = false;

        for (EasyMailHandlerInfo handlerInfo : groupHandlers) {
            if (handlerInfo.matches(context)) {
                try {
                    context.setProcessStartTime(LocalDateTime.now());
                    context.setProcessorName(handlerInfo.getName());

                    if (handlerInfo.isAsync()) {
                        executeHandlerAsync(handlerInfo, context);
                        hasSuccess = true;
                    } else {
                        boolean result = executeHandlerSync(handlerInfo, context);
                        if (result) {
                            hasSuccess = true;
                        }
                    }
                } catch (Exception e) {
                    EasyMailProcessException processEx = EasyMailProcessException.processingError("执行组中的邮件处理器失败", e);
                    log.error("执行组 [{}] 中的邮件处理器失败: {} - {}", group, handlerInfo.getName(), processEx.getFullErrorMessage(), processEx);
                }
            }
        }

        return hasSuccess;
    }

    /**
     * 获取处理器执行统计
     *
     * @return 统计信息
     */
    public String getExecutionStats() {
        return handlerRegistry.getStatistics().toString();
    }
}