package cn.sunyblog.javaemaildemo.mail;
import cn.sunyblog.javaemaildemo.api.EmailListenerApi;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import javax.mail.Address;
import javax.mail.Flags;
import javax.mail.Message;
import javax.mail.MessagingException;
import java.io.IOException;

/**
 * @author suny
 * @version 1.0
 * @description: 邮件处理器  负责处理单个邮件，包括获取内容、保存附件等
 * @date 2025/05/12 16:24
 */
@Slf4j
@Data
@Component
public class MailProcessor {

    @Resource
    private MailConfig mailConfig;
    @Resource
    private MailContentParser contentParser;
    @Resource
    private MailCache mailCache;
    @Resource
    private EmailListenerApi emailListenerApi;
    private EmailProcessorFunction emailProcessorFunction;

    /**
     * 处理单封邮件
     *
     * @param message 邮件消息
     * @return 是否成功处理
     */
    public boolean processMessage(Message message) {
        try {
            // 获取邮件ID
            String messageId = mailCache.getMessageId(message);

            // 原子性检查并标记为已处理
            if (!mailCache.checkAndMarkAsProcessed(messageId)) {
                log.debug("邮件已处理，跳过: {}", messageId);
                return false;
            }

            long startTime = System.currentTimeMillis();
            String subject = mailCache.getSubjectSafely(message);
            log.info("开始处理邮件，主题: {}", subject);

            // 获取发件人
            String from = "(未知发件人)";
            Address[] fromAddresses = message.getFrom();
            if (fromAddresses != null && fromAddresses.length > 0 && fromAddresses[0] != null) {
                from = contentParser.decodeText(fromAddresses[0].toString());
            }

            // 解析邮件内容
            String emailContent = contentParser.parseContent(message, mailConfig.getAttachmentDir());

            // 处理邮件
            boolean processed = false;
            
            // 优先使用函数式处理方式
            if (emailProcessorFunction != null) {
                try {
                    Object result = emailProcessorFunction.process(message, emailContent, subject, from);
                    processed = (result != null);
                    log.info("函数式邮件处理结果: {}", result);
                } catch (Exception e) {
                    log.error("函数式邮件处理异常: {}", e.getMessage(), e);
                }
            } 
            // 其次使用接口方式
            else if (emailListenerApi != null) {
                try {
                    processed = emailListenerApi.processEmail(message, emailContent, subject, from);
                    log.info("邮件处理器[{}]处理结果: {}", emailListenerApi.getProcessorName(), processed);
                } catch (Exception e) {
                    log.error("邮件处理器[{}]处理异常: {}", emailListenerApi.getProcessorName(), e.getMessage(), e);
                }
            } 
            // 最后使用默认处理方法
            else {
                processed = processVerificationCodeEmail(subject, emailContent);
            }

            long endTime = System.currentTimeMillis();
            long processDuration = endTime - startTime;
            log.info("邮件处理完成，耗时: {}毫秒", processDuration);

            try {
                // 标记为已读
                message.setFlag(Flags.Flag.SEEN, true);
            } catch (MessagingException ex) {
                log.error("标记邮件为已读失败: {}", ex.getMessage(), ex);
            }

            return processed;
        } catch (MessagingException | IOException e) {
            log.error("处理邮件异常: {}", e.getMessage(), e);
            return false;
        } catch (Exception e) {
            log.error("处理邮件未预期异常: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * 处理验证码邮件
     *
     * @param subject 邮件主题
     * @param content 邮件内容
     * @return 处理结果
     */
    private boolean processVerificationCodeEmail(String subject, String content) {
        // 默认实现，可以在这里添加验证码邮件的处理逻辑
        log.info("默认邮件处理: 主题={}", subject);
        log.debug("邮件内容: {}", content);
        return true;
    }


    /**
     * 获取邮件处理统计
     *
     * @return 处理统计信息
     */
    public String getProcessingStats() {
        return "已处理邮件数: " + mailCache.getProcessedCount();
    }
    
    /**
     * 设置邮件处理函数
     * 这种方式比实现接口更灵活，可以直接传入lambda表达式处理邮件
     *
     * @param processorFunction 邮件处理函数
     */
    public void setEmailProcessorFunction(EmailProcessorFunction processorFunction) {
        this.emailProcessorFunction = processorFunction;
        log.info("已设置函数式邮件处理器");
    }
}
