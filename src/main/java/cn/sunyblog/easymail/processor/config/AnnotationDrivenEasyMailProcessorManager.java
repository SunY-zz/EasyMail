package cn.sunyblog.easymail.processor.config;

import cn.sunyblog.easymail.processor.handler.EasyMailContext;
import cn.sunyblog.easymail.processor.handler.EasyMailContextBuilder;
import cn.sunyblog.easymail.processor.handler.EasyMailHandlerExecutor;
import cn.sunyblog.easymail.processor.handler.EasyMailHandlerRegistry;
import lombok.extern.slf4j.Slf4j;

import javax.mail.Message;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 注解驱动邮件处理器管理器
 * 作为新邮件处理系统的核心管理组件
 *
 * @author suny
 * @version 1.0
 * @date 2025/06/14
 */
@Slf4j
// 移除@Component注解，避免与AutoConfiguration中的@Bean定义冲突
// @Component
public class AnnotationDrivenEasyMailProcessorManager {

    private final EasyMailHandlerRegistry handlerRegistry;
    private final EasyMailHandlerExecutor handlerExecutor;
    private final EasyMailContextBuilder contextBuilder;
    private final AnnotationDrivenEasyMailProcessorProperties properties;

    /**
     * 处理统计
     */
    private final AtomicLong totalProcessed = new AtomicLong(0);
    private final AtomicLong totalSuccess = new AtomicLong(0);
    private final AtomicLong totalFailed = new AtomicLong(0);

    public AnnotationDrivenEasyMailProcessorManager(
            EasyMailHandlerRegistry handlerRegistry,
            EasyMailHandlerExecutor handlerExecutor,
            EasyMailContextBuilder contextBuilder,
            AnnotationDrivenEasyMailProcessorProperties properties) {

        this.handlerRegistry = handlerRegistry;
        this.handlerExecutor = handlerExecutor;
        this.contextBuilder = contextBuilder;
        this.properties = properties;

        log.debug("注解驱动邮件处理器管理器初始化完成");
        logConfiguration();
    }

    /**
     * 处理邮件
     *
     * @param message       邮件消息
     * @param attachmentDir 附件保存目录
     * @return 处理结果
     */
    public boolean processEmail(Message message, String attachmentDir) {
        long startTime = System.currentTimeMillis();
        totalProcessed.incrementAndGet();

        try {
            // 构建邮件上下文
            EasyMailContext context = contextBuilder.buildContext(message, attachmentDir);

            log.debug("开始处理邮件 - 主题: {}, 发件人: {}",
                    context.getSubject(), context.getFrom());

            // 执行处理器
            boolean result = handlerExecutor.executeHandlers(context);

            // 更新统计
            if (result) {
                totalSuccess.incrementAndGet();
            } else {
                totalFailed.incrementAndGet();
            }

            long duration = System.currentTimeMillis() - startTime;

            log.debug("邮件处理完成 - 结果: {}, 耗时: {}ms", result ? "成功" : "失败", duration);

            return result;

        } catch (Exception e) {
            totalFailed.incrementAndGet();
            long duration = System.currentTimeMillis() - startTime;
            log.error("邮件处理异常 - 耗时: {}ms", duration, e);
            return false;
        }
    }

    /**
     * 处理邮件（兼容旧接口）
     *
     * @param message 邮件消息
     * @param content 邮件内容
     * @param subject 邮件主题
     * @param from    发件人
     * @return 处理结果
     */
    public boolean processEmail(Message message, String content, String subject, String from) {
        try {
            // 构建简化的邮件上下文
            EasyMailContext context = EasyMailContext.builder()
                    .message(message)
                    .textContent(content)
                    .subject(subject)
                    .from(from)
                    .build();

            return handlerExecutor.executeHandlers(context);

        } catch (Exception e) {
            log.error("邮件处理异常", e);
            return false;
        }
    }

    /**
     * 按组处理邮件
     *
     * @param message       邮件消息
     * @param attachmentDir 附件保存目录
     * @param group         处理器组名
     * @return 处理结果
     */
    public boolean processEmailByGroup(Message message, String attachmentDir, String group) {
        try {
            EasyMailContext context = contextBuilder.buildContext(message, attachmentDir);
            return handlerExecutor.executeHandlersByGroup(context, group);
        } catch (Exception e) {
            log.error("按组处理邮件异常", e);
            return false;
        }
    }

    /**
     * 获取处理器统计信息
     *
     * @return 统计信息
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = handlerRegistry.getStatistics();
        stats.put("totalProcessed", totalProcessed.get());
        stats.put("totalSuccess", totalSuccess.get());
        stats.put("totalFailed", totalFailed.get());

        long total = totalProcessed.get();
        if (total > 0) {
            stats.put("successRate", String.format("%.2f%%", (totalSuccess.get() * 100.0) / total));
            stats.put("failureRate", String.format("%.2f%%", (totalFailed.get() * 100.0) / total));
        }

        return stats;
    }

    /**
     * 重置统计信息
     */
    public void resetStatistics() {
        totalProcessed.set(0);
        totalSuccess.set(0);
        totalFailed.set(0);
        log.info("处理统计信息已重置");
    }

    /**
     * 启用/禁用处理器
     *
     * @param handlerName 处理器名称
     * @param enabled     是否启用
     */
    public void setHandlerEnabled(String handlerName, boolean enabled) {
        handlerRegistry.setHandlerEnabled(handlerName, enabled);
    }

    /**
     * 获取配置信息
     *
     * @return 配置信息
     */
    public AnnotationDrivenEasyMailProcessorProperties getProperties() {
        return properties;
    }

    /**
     * 检查是否启用
     *
     * @return 是否启用
     */
    public boolean isEnabled() {
        return properties.isEnabled();
    }

    /**
     * 提取验证码
     *
     * @param content 邮件内容
     * @return 验证码
     */
    public String extractVerificationCode(String content) {
        return contextBuilder.extractVerificationCode(content);
    }

    /**
     * 记录配置信息
     */
    private void logConfiguration() {
        log.debug("注解驱动邮件处理器配置:");
        log.debug("  - 启用状态: {}", properties.isEnabled());

        if (properties.getScan().getPackages().length > 0) {
            log.debug("  - 扫描包路径: {}", String.join(", ", properties.getScan().getPackages()));
        }
    }
}