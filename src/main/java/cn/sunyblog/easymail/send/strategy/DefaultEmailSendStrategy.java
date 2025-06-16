package cn.sunyblog.easymail.send.strategy;

import cn.sunyblog.easymail.send.MailSender;
import cn.sunyblog.easymail.send.SendResult;
import cn.sunyblog.easymail.config.SmtpConfig;
import cn.sunyblog.easymail.util.RetryUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import javax.mail.MessagingException;
import javax.mail.SendFailedException;
import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

/**
 * 默认邮件发送策略实现
 * 适用于大多数常规邮件发送场景
 *
 * @author suny
 * @version 1.0.0
 */
@Slf4j
@Component
public class DefaultEmailSendStrategy implements EmailSendStrategy {

    @Resource
    private MailSender mailSender;

    @Resource
    private SmtpConfig smtpConfig;

    @Override
    public SendResult send(List<String> toList, List<String> ccList, List<String> bccList,
                           String subject, String content, boolean isHtml, List<File> attachments) {

        long startTime = System.currentTimeMillis();

        try {
            // 参数验证
            if (toList == null || toList.isEmpty()) {
                return SendResult.failure(toList, subject, "收件人列表不能为空", 0);
            }

            // 构建收件人列表
            List<String> allRecipients = new ArrayList<>(toList);
            if (ccList != null) allRecipients.addAll(ccList);
            if (bccList != null) allRecipients.addAll(bccList);

            // 配置重试策略
            RetryUtil.RetryConfig retryConfig = buildRetryConfig();

            // 发送邮件
            boolean success = RetryUtil.executeWithRetry(() -> mailSender.sendEmailInternal(
                    String.join(",", toList),
                    ccList != null && !ccList.isEmpty() ? String.join(",", ccList) : null,
                    bccList != null && !bccList.isEmpty() ? String.join(",", bccList) : null,
                    subject, content, isHtml, attachments), retryConfig);

            long duration = System.currentTimeMillis() - startTime;

            if (success) {
                String messageId = generateMessageId();
                SendResult result = SendResult.success(messageId, allRecipients, subject, duration);
                result.setRetried(retryConfig.getMaxAttempts() > 1);
                result.setRetryCount(retryConfig.getMaxAttempts() - 1);
                return result;
            } else {
                SendResult result = SendResult.failure(allRecipients, subject, "邮件发送失败", duration);
                result.setRetried(retryConfig.getMaxAttempts() > 1);
                result.setRetryCount(retryConfig.getMaxAttempts() - 1);
                return result;
            }

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("邮件发送异常: {}", e.getMessage(), e);
            return SendResult.failure(toList, subject, e.getMessage(), getErrorCode(e), getStackTrace(e), duration);
        }
    }

    @Override
    public String getStrategyName() {
        return "DefaultStrategy";
    }

    @Override
    public String getDescription() {
        return "默认邮件发送策略，适用于大多数常规邮件发送场景";
    }

    @Override
    public boolean supports(int recipientCount, boolean hasAttachments, boolean isHtml) {
        // 默认策略支持所有场景，但优先级较低
        return true;
    }

    @Override
    public int getPriority() {
        // 默认策略优先级最低
        return Integer.MAX_VALUE;
    }

    /**
     * 构建重试配置
     *
     * @return 重试配置
     */
    protected RetryUtil.RetryConfig buildRetryConfig() {
        if (!smtpConfig.getRetry().isEnabled()) {
            return RetryUtil.RetryConfig.defaults().maxAttempts(1);
        }

        return RetryUtil.RetryConfig.defaults()
                .maxAttempts(smtpConfig.getRetry().getMaxRetries() + 1)
                .initialDelayMs(smtpConfig.getRetry().getInitialDelayMs())
                .maxDelayMs(smtpConfig.getRetry().getMaxDelayMs())
                .useExponentialBackoff(smtpConfig.getRetry().isUseExponentialBackoff())
                .backoffMultiplier(smtpConfig.getRetry().getBackoffMultiplier())
                .retryableExceptions(
                        MessagingException.class,
                        SendFailedException.class,
                        javax.mail.MessagingException.class
                );
    }

    /**
     * 生成邮件ID
     *
     * @return 邮件ID
     */
    protected String generateMessageId() {
        return "msg_" + System.currentTimeMillis() + "_" + java.util.UUID.randomUUID().toString().substring(0, 8);
    }

    /**
     * 获取错误代码
     *
     * @param e 异常
     * @return 错误代码
     */
    protected String getErrorCode(Exception e) {
        if (e instanceof MessagingException) {
            return "MESSAGING_ERROR";
        } else if (e instanceof UnsupportedEncodingException) {
            return "ENCODING_ERROR";
        } else {
            return "UNKNOWN_ERROR";
        }
    }

    /**
     * 获取异常堆栈信息
     *
     * @param e 异常
     * @return 堆栈信息字符串
     */
    protected String getStackTrace(Exception e) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        return sw.toString();
    }
}